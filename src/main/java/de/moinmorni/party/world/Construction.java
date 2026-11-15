package de.moinmorni.party.world;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Baut Festplatz, Spielbretter und Minispiel-Plätze aus den Bauplänen – nur wenn sich etwas geändert hat (Bau-Version
 * oder Zahl der Tische, gemerkt in {@code bau.yml}). Der Festplatz entsteht sofort, der Rest Stück für Stück
 * (höchstens {@link #BUDGET_NANOS} pro Tick), damit der Server dabei flüssig bleibt. Vorher wird abgeräumt, was der
 * alte Bau belegt hatte.
 */
public final class Construction {

    /** Bau-Version: erhöhen, wenn sich Festplatz, Bretter oder Plätze ändern – dann wird beim nächsten Start neu gebaut. */
    public static final int VERSION = 2;
    private static final long BUDGET_NANOS = 25_000_000L;
    private static final int CHECK_EVERY = 256;
    private static final int HUB_RANGE = 200;

    /** Ein Arbeitsschritt; {@code true} = fertig. */
    private interface Job {
        boolean step(long deadline);
    }

    private final Plugin plugin;
    private final Logger log;
    private final World world;
    private final Path stateFile;
    private final Deque<Job> jobs = new ArrayDeque<>();
    private BukkitTask task;
    private int placed;

    public Construction(Plugin plugin, World world) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.world = world;
        this.stateFile = plugin.getDataFolder().toPath().resolve("bau.yml");
    }

    public boolean running() {
        return task != null;
    }

    /**
     * Baut, falls nötig. {@code done} läuft auf dem Hauptthread, sobald alles steht (auch sofort, wenn nichts zu tun
     * ist).
     */
    public void ensure(int tables, Map<de.moinmorni.party.board.BoardId, Plan> boards,
            Map<de.moinmorni.party.mini.MiniType, Plan> minis, boolean force, Runnable done) {
        YamlConfiguration state = readState();
        boolean current = !force && state.getInt("version") == VERSION && state.getInt("tische") == tables;
        if (current) {
            done.run();
            return;
        }
        log.info("Festplatz, Spielbretter und Minispiel-Plätze werden gebaut (Bau-Version " + VERSION + ", " + tables
                + " Tische) …");
        long start = System.currentTimeMillis();
        List<String> boxes = new ArrayList<>();
        List<int[]> later = new ArrayList<>();
        for (String box : state.getStringList("bereiche")) {
            int[] b = parseBox(box);
            if (b == null) {
                continue;
            }
            if (Math.abs(b[0]) < HUB_RANGE && Math.abs(b[2]) < HUB_RANGE) {
                jobs.add(clearJob(b));
            } else {
                later.add(b);
            }
        }
        // Festplatz zuerst (sofort, damit niemand ins Leere fällt)
        Plan hub = HubPlan.build();
        boxes.add(boxOf(hub, 0, Places.FLOOR_Y, 0));
        jobs.add(placeJob(hub, 0, Places.FLOOR_Y, 0));
        runNow();
        later.forEach(b -> jobs.add(clearJob(b)));
        for (int t = 0; t < tables; t++) {
            for (var e : boards.entrySet()) {
                int[] o = Places.board(t, e.getKey());
                boxes.add(boxOf(e.getValue(), o[0], o[1], o[2]));
                jobs.add(placeJob(e.getValue(), o[0], o[1], o[2]));
            }
            for (var e : minis.entrySet()) {
                int[] o = Places.mini(t, e.getKey());
                boxes.add(boxOf(e.getValue(), o[0], o[1], o[2]));
                jobs.add(placeJob(e.getValue(), o[0], o[1], o[2]));
            }
        }
        jobs.add(deadline -> {
            YamlConfiguration fresh = new YamlConfiguration();
            fresh.set("version", VERSION);
            fresh.set("tische", tables);
            fresh.set("bereiche", boxes);
            writeState(fresh);
            log.info("Fertig gebaut: " + placed + " Blöcke in " + (System.currentTimeMillis() - start) / 1000 + " s.");
            done.run();
            return true;
        });
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    /** Erste Schritte (abräumen + Halle) gleich ausführen. */
    private void runNow() {
        while (!jobs.isEmpty()) {
            Job job = jobs.peekFirst();
            if (job.step(Long.MAX_VALUE)) {
                jobs.pollFirst();
            }
        }
    }

    private void tick() {
        long deadline = System.nanoTime() + BUDGET_NANOS;
        while (!jobs.isEmpty() && System.nanoTime() < deadline) {
            Job job = jobs.peekFirst();
            boolean finished;
            try {
                finished = job.step(deadline);
            } catch (RuntimeException e) {
                log.log(Level.WARNING, "Bauschritt fehlgeschlagen – wird übersprungen", e);
                finished = true;
            }
            if (finished) {
                jobs.pollFirst();
            }
        }
        if (jobs.isEmpty() && task != null) {
            task.cancel();
            task = null;
        }
    }

    private Job placeJob(Plan plan, int ox, int oy, int oz) {
        List<Long> keys = new ArrayList<>(plan.size());
        List<BlockData> data = new ArrayList<>(plan.size());
        plan.forEach((k, d) -> {
            keys.add(k);
            data.add(d);
        });
        List<Plan.Banner> banners = plan.banners();
        int[] index = {0};
        return deadline -> {
            int i = index[0];
            int n = 0;
            while (i < keys.size()) {
                long k = keys.get(i);
                world.getBlockAt(ox + Plan.keyX(k), oy + Plan.keyY(k), oz + Plan.keyZ(k)).setBlockData(data.get(i), false);
                i++;
                placed++;
                if (++n % CHECK_EVERY == 0 && System.nanoTime() > deadline) {
                    index[0] = i;
                    return false;
                }
            }
            index[0] = i;
            for (Plan.Banner b : banners) {
                Block block = world.getBlockAt(ox + b.x(), oy + b.y(), oz + b.z());
                BlockState st = block.getState();
                if (st instanceof org.bukkit.block.Banner banner) {
                    banner.setPatterns(b.patterns());
                    banner.update(true, false);
                }
            }
            for (Plan.Creature c : plan.creatures()) {
                spawnCreature(c, ox, oy, oz);
            }
            return true;
        };
    }

    /** Deko-Tier: steht still, ist unverwundbar und still, bleibt erhalten. */
    private void spawnCreature(Plan.Creature c, int ox, int oy, int oz) {
        org.bukkit.entity.EntityType type = org.bukkit.Registry.ENTITY_TYPE.get(org.bukkit.NamespacedKey.minecraft(c.type()));
        if (type == null) {
            return;
        }
        org.bukkit.Location l = new org.bukkit.Location(world, ox + c.x(), oy + c.y(), oz + c.z(), c.yaw(), 0f);
        world.spawnEntity(l, type, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
            e.setPersistent(true);
            e.setInvulnerable(true);
            e.setSilent(true);
            e.addScoreboardTag("morniparty_deko");
            if (e instanceof org.bukkit.entity.LivingEntity le) {
                le.setAI(false);
                le.setRemoveWhenFarAway(false);
            }
        });
    }

    private Job clearJob(int[] b) {
        int[] cursor = {b[0], b[1], b[2]};
        boolean[] entitiesGone = {false};
        return deadline -> {
            if (!entitiesGone[0]) {
                for (Entity e : world.getEntities()) {
                    if (!(e instanceof Player)) {
                        org.bukkit.Location l = e.getLocation();
                        if (l.getX() >= b[0] && l.getX() <= b[3] + 1 && l.getZ() >= b[2] && l.getZ() <= b[5] + 1) {
                            e.remove();
                        }
                    }
                }
                entitiesGone[0] = true;
            }
            int n = 0;
            while (cursor[0] <= b[3]) {
                Block block = world.getBlockAt(cursor[0], cursor[1], cursor[2]);
                if (block.getType() != Material.AIR) {
                    block.setType(Material.AIR, false);
                }
                if (++cursor[2] > b[5]) {
                    cursor[2] = b[2];
                    if (++cursor[1] > b[4]) {
                        cursor[1] = b[1];
                        cursor[0]++;
                    }
                }
                if (++n % CHECK_EVERY == 0 && System.nanoTime() > deadline) {
                    return false;
                }
            }
            return true;
        };
    }

    private static String boxOf(Plan plan, int ox, int oy, int oz) {
        int[] b = plan.bounds();
        return (ox + b[0]) + " " + (oy + b[1]) + " " + (oz + b[2]) + " " + (ox + b[3]) + " " + (oy + b[4]) + " "
                + (oz + b[5]);
    }

    private static int[] parseBox(String s) {
        String[] parts = s.trim().split("\\s+");
        if (parts.length != 6) {
            return null;
        }
        int[] b = new int[6];
        try {
            for (int i = 0; i < 6; i++) {
                b[i] = Integer.parseInt(parts[i]);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return b;
    }

    private YamlConfiguration readState() {
        YamlConfiguration y = new YamlConfiguration();
        if (Files.isRegularFile(stateFile)) {
            try {
                y.loadFromString(Files.readString(stateFile, StandardCharsets.UTF_8));
            } catch (IOException | InvalidConfigurationException e) {
                log.warning("bau.yml ließ sich nicht lesen – es wird neu gebaut: " + e.getMessage());
            }
        }
        return y;
    }

    private void writeState(YamlConfiguration y) {
        try {
            Files.createDirectories(stateFile.getParent());
            Files.writeString(stateFile, y.saveToString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.log(Level.WARNING, "bau.yml ließ sich nicht schreiben", e);
        }
    }
}
