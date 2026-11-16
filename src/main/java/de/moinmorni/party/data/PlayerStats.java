package de.moinmorni.party.data;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/** Statistik einer Person (lokal in {@code spieler/<uuid>.yml}). */
public final class PlayerStats {

    private final UUID uuid;
    private String name;
    private int games;
    private int wins;
    private int edelweiss;
    private int minigamesWon;
    private int bestEdelweiss;
    private int summits;
    /** Je Spielart (Kennung wie „marathon“): {Partien, Siege}. */
    private final Map<String, int[]> modes = new TreeMap<>();

    public PlayerStats(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String n) {
        if (n != null && !n.isBlank()) {
            name = n;
        }
    }

    public int games() {
        return games;
    }

    public int wins() {
        return wins;
    }

    public int edelweiss() {
        return edelweiss;
    }

    public int minigamesWon() {
        return minigamesWon;
    }

    public int bestEdelweiss() {
        return bestEdelweiss;
    }

    public int summits() {
        return summits;
    }

    /** Partien in einer Spielart. */
    public int modeGames(String mode) {
        return modes.getOrDefault(mode, new int[2])[0];
    }

    /** Siege in einer Spielart. */
    public int modeWins(String mode) {
        return modes.getOrDefault(mode, new int[2])[1];
    }

    /** Eine Partie zählen (mit Spielart). */
    public void record(String mode, boolean won, int edelweissCount, int minigames, boolean summit) {
        int[] m = modes.computeIfAbsent(mode, k -> new int[2]);
        m[0]++;
        if (won) {
            m[1]++;
        }
        record(won, edelweissCount, minigames, summit);
    }

    /** Eine Partie zählen. */
    public void record(boolean won, int edelweissCount, int minigames, boolean summit) {
        games++;
        if (won) {
            wins++;
        }
        edelweiss += Math.max(0, edelweissCount);
        minigamesWon += Math.max(0, minigames);
        bestEdelweiss = Math.max(bestEdelweiss, edelweissCount);
        if (summit) {
            summits++;
        }
    }

    public String toYaml() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("name", name);
        y.set("partien", games);
        y.set("siege", wins);
        y.set("edelweiss", edelweiss);
        y.set("minispiele-gewonnen", minigamesWon);
        y.set("edelweiss-bestwert", bestEdelweiss);
        y.set("gipfel", summits);
        modes.forEach((k, v) -> {
            y.set("spielarten." + k + ".partien", v[0]);
            y.set("spielarten." + k + ".siege", v[1]);
        });
        return y.saveToString();
    }

    public static PlayerStats fromYaml(UUID uuid, String text) {
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(text);
        } catch (InvalidConfigurationException e) {
            throw new IllegalArgumentException(e);
        }
        PlayerStats s = new PlayerStats(uuid, y.getString("name", "?"));
        s.games = y.getInt("partien");
        s.wins = y.getInt("siege");
        s.edelweiss = y.getInt("edelweiss");
        s.minigamesWon = y.getInt("minispiele-gewonnen");
        s.bestEdelweiss = y.getInt("edelweiss-bestwert");
        s.summits = y.getInt("gipfel");
        ConfigurationSection m = y.getConfigurationSection("spielarten");
        if (m != null) {
            for (String k : m.getKeys(false)) {
                s.modes.put(k, new int[] {m.getInt(k + ".partien"), m.getInt(k + ".siege")});
            }
        }
        return s;
    }
}
