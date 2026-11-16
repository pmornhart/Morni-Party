package de.moinmorni.party.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Statistik als YAML-Dateien ({@code spieler/<uuid>.yml}) und eine kleine Rangliste ({@code rangliste.yml}, Siege und
 * Edelweiß je Person). Geschrieben wird im Hintergrund, jede Datei erst als Kopie und dann umbenannt.
 */
public final class Store {

    private static final long CLOSE_WAIT_SECONDS = 10;

    /** Zeile der Rangliste. */
    public record Entry(UUID uuid, String name, int wins, int edelweiss) {
    }

    private final Logger log;
    private final Path dir;
    private final Path rankingFile;
    private final ConcurrentHashMap<UUID, Entry> ranking = new ConcurrentHashMap<>();
    /** Schon gelesene Werte (die Datei wird im Hintergrund geschrieben und kann kurz älter sein). */
    private final ConcurrentHashMap<UUID, PlayerStats> cache = new ConcurrentHashMap<>();
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Morni-Party-Speichern");
        t.setDaemon(true);
        return t;
    });

    public Store(Logger log, Path dataFolder) {
        this.log = log;
        this.dir = dataFolder.resolve("spieler");
        this.rankingFile = dataFolder.resolve("rangliste.yml");
        loadRanking();
    }

    public Optional<PlayerStats> read(UUID uuid) {
        PlayerStats cached = cache.get(uuid);
        if (cached != null) {
            return Optional.of(cached);
        }
        Path file = dir.resolve(uuid + ".yml");
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(PlayerStats.fromYaml(uuid, Files.readString(file, StandardCharsets.UTF_8)));
        } catch (IOException | IllegalArgumentException e) {
            log.log(Level.WARNING, "Spielerdatei " + file.getFileName() + " ließ sich nicht lesen", e);
            return Optional.empty();
        }
    }

    /** Partie für jemanden zählen (liest, ändert, schreibt – auf dem Hauptthread aufrufen). */
    public PlayerStats record(UUID uuid, String name, String mode, boolean won, int edelweiss, int minigames,
            boolean summit) {
        PlayerStats s = read(uuid).orElseGet(() -> new PlayerStats(uuid, name));
        s.name(name);
        s.record(mode, won, edelweiss, minigames, summit);
        cache.put(uuid, s);
        write(dir.resolve(uuid + ".yml"), s.toYaml());
        ranking.put(uuid, new Entry(uuid, s.name(), s.wins(), s.edelweiss()));
        saveRanking();
        return s;
    }

    /** Beste nach Siegen (dann Edelweiß). */
    public List<Entry> top(int n) {
        List<Entry> all = new ArrayList<>(ranking.values());
        all.sort(Comparator.comparingInt((Entry e) -> -e.wins()).thenComparingInt(e -> -e.edelweiss())
                .thenComparing(Entry::name));
        return all.subList(0, Math.min(n, all.size()));
    }

    /** Name → UUID (aus der Rangliste), für /stats ohne Online-Spieler. */
    public Optional<UUID> find(String name) {
        try (Stream<Entry> s = ranking.values().stream()) {
            return s.filter(e -> e.name().equalsIgnoreCase(name)).map(Entry::uuid).findFirst();
        }
    }

    private void loadRanking() {
        if (!Files.isRegularFile(rankingFile)) {
            return;
        }
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(Files.readString(rankingFile, StandardCharsets.UTF_8));
        } catch (IOException | InvalidConfigurationException e) {
            log.warning("rangliste.yml ließ sich nicht lesen: " + e.getMessage());
            return;
        }
        for (String key : y.getKeys(false)) {
            try {
                UUID u = UUID.fromString(key);
                ranking.put(u, new Entry(u, y.getString(key + ".name", "?"), y.getInt(key + ".siege"),
                        y.getInt(key + ".edelweiss")));
            } catch (IllegalArgumentException e) {
                // kaputte Zeile überspringen
            }
        }
    }

    private void saveRanking() {
        YamlConfiguration y = new YamlConfiguration();
        ranking.values().forEach(e -> {
            y.set(e.uuid() + ".name", e.name());
            y.set(e.uuid() + ".siege", e.wins());
            y.set(e.uuid() + ".edelweiss", e.edelweiss());
        });
        write(rankingFile, y.saveToString());
    }

    private void write(Path file, String text) {
        writer.execute(() -> {
            try {
                Files.createDirectories(file.getParent());
                Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
                Files.writeString(tmp, text, StandardCharsets.UTF_8);
                try {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                log.log(Level.WARNING, "Konnte " + file.getFileName() + " nicht speichern", e);
            }
        });
    }

    public void close() {
        writer.shutdown();
        try {
            if (!writer.awaitTermination(CLOSE_WAIT_SECONDS, TimeUnit.SECONDS)) {
                log.warning("Speichern dauert zu lange – einige Werte könnten fehlen.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
