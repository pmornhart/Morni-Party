package net.morni.party.mini;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Die Almwiese der Enzian-Suche, ohne Minecraft: hohes Gras, Farne, Büsche, Felsbrocken und bunte Blumen (nie blau)
 * – und die Verstecke, an denen ein blauer Enzian wachsen kann (frei, aber gleich neben etwas, das ihn verdeckt).
 * Immer gleich (fester Startwert), damit Bau und Spiel dieselbe Wiese kennen.
 */
final class EnzianWiese {

    static final int HALF = 14;
    /** Die Blume, die gesucht wird. */
    static final String ENZIAN = "cornflower";
    private static final String[] FLOWERS = {"dandelion", "poppy", "oxeye_daisy", "azure_bluet", "allium",
        "red_tulip", "orange_tulip", "white_tulip", "pink_tulip", "lily_of_the_valley"};

    /** Ein Block der Wiese über dem Boden (y = 1 oder 2). */
    record Cell(int x, int y, int z, String block) {
    }

    private static final Map<Long, Cell> CELLS = new LinkedHashMap<>();
    private static final List<int[]> SPOTS = new ArrayList<>();

    static {
        make();
    }

    private EnzianWiese() {
    }

    private static long key(int x, int y, int z) {
        return ((long) (x + 64) << 16) | ((long) (y + 64) << 8) | (z + 64);
    }

    private static void put(int x, int y, int z, String block) {
        if (Math.abs(x) <= HALF && Math.abs(z) <= HALF) {
            CELLS.putIfAbsent(key(x, y, z), new Cell(x, y, z, block));
        }
    }

    static Cell at(int x, int y, int z) {
        return CELLS.get(key(x, y, z));
    }

    /** Verdeckt etwas die Sicht (Fels, Busch, hohes Gras, Farn)? */
    static boolean hides(Cell c) {
        return c != null && !c.block().endsWith("[half=upper]") && (c.block().contains("tall_grass")
                || c.block().contains("large_fern") || c.block().contains("azalea") || c.block().contains("cobblestone")
                || c.block().equals("stone") || c.block().equals("andesite"));
    }

    private static void make() {
        Random r = new Random(77);
        // Felsbrocken
        for (int k = 0; k < 9; k++) {
            int cx = r.nextInt(2 * HALF - 3) - HALF + 2;
            int cz = r.nextInt(2 * HALF - 3) - HALF + 2;
            put(cx, 1, cz, r.nextBoolean() ? "mossy_cobblestone" : "stone");
            put(cx + 1, 1, cz, r.nextBoolean() ? "andesite" : "mossy_cobblestone");
            if (r.nextBoolean()) {
                put(cx, 2, cz, "mossy_cobblestone");
            }
        }
        // Büsche
        for (int k = 0; k < 14; k++) {
            int x = r.nextInt(2 * HALF - 1) - HALF + 1;
            int z = r.nextInt(2 * HALF - 1) - HALF + 1;
            put(x, 1, z, r.nextInt(3) == 0 ? "flowering_azalea" : "azalea");
        }
        // Hohes Gras und Farne in Büscheln
        for (int k = 0; k < 26; k++) {
            int cx = r.nextInt(2 * HALF - 1) - HALF + 1;
            int cz = r.nextInt(2 * HALF - 1) - HALF + 1;
            for (int i = 0; i < 5; i++) {
                int x = cx + r.nextInt(3) - 1;
                int z = cz + r.nextInt(3) - 1;
                if (at(x, 1, z) == null && at(x, 2, z) == null) {
                    String plant = r.nextInt(4) == 0 ? "large_fern" : "tall_grass";
                    put(x, 1, z, plant + "[half=lower]");
                    put(x, 2, z, plant + "[half=upper]");
                }
            }
        }
        // Bunte Blumen und kurzes Gras überall (nie blau)
        for (int x = -HALF; x <= HALF; x++) {
            for (int z = -HALF; z <= HALF; z++) {
                if (at(x, 1, z) == null && r.nextInt(100) < 22) {
                    put(x, 1, z, r.nextInt(3) == 0 ? "short_grass" : FLOWERS[r.nextInt(FLOWERS.length)]);
                }
            }
        }
        // Verstecke: frei, darüber frei, gleich neben etwas Verdeckendem, nicht am Rand
        for (int x = -HALF + 1; x <= HALF - 1; x++) {
            for (int z = -HALF + 1; z <= HALF - 1; z++) {
                if (at(x, 1, z) != null || at(x, 2, z) != null) {
                    continue;
                }
                boolean hidden = false;
                for (int dx = -1; dx <= 1 && !hidden; dx++) {
                    for (int dz = -1; dz <= 1 && !hidden; dz++) {
                        hidden = (dx != 0 || dz != 0) && hides(at(x + dx, 1, z + dz));
                    }
                }
                if (hidden) {
                    SPOTS.add(new int[] {x, z});
                }
            }
        }
    }

    static List<Cell> cells() {
        return List.copyOf(CELLS.values());
    }

    /** Verstecke (x, z) für einen Enzian auf y = 1. */
    static List<int[]> spots() {
        return List.copyOf(SPOTS);
    }
}
