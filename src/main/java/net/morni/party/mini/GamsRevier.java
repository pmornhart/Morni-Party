package net.morni.party.mini;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Das Gamsrevier der Gamsjagd, ohne Minecraft: ein Karstboden mit Felsbuckeln (jede Stufe höchstens einen Block hoch –
 * alles lässt sich erklettern), Latschen zum Verstecken, Geröll, Schneeflecken und ein Hochsitz in einer Ecke. Immer
 * gleich (fester Startwert), damit Bau und Spiel dieselbe Fläche kennen. Startplätze bleiben frei.
 */
final class GamsRevier {

    static final int HALF = 16;
    /** Hochsitz in der Nordost-Ecke. */
    static final int SEAT_X = HALF - 3;
    static final int SEAT_Z = -(HALF - 3);
    static final int SEAT_Y = 4;
    /** Abstand der Gämsen-Startplätze von der Mitte. */
    static final double SPAWN_R = HALF - 2.5;

    /** Ein Block über dem Boden (y ≥ 0; y = 0 ersetzt den Boden). */
    record Cell(int x, int y, int z, String block) {
    }

    private static final Map<Long, Cell> CELLS = new LinkedHashMap<>();
    private static final int[][] HEIGHT = new int[2 * HALF + 1][2 * HALF + 1];

    static {
        make();
    }

    private GamsRevier() {
    }

    private static long key(int x, int y, int z) {
        return ((long) (x + 64) << 16) | ((long) (y + 64) << 8) | (z + 64);
    }

    /** Startplätze: Mitte (Jäger) und drei am Rand (Gämsen) – {x, z}. */
    static List<double[]> spawns() {
        List<double[]> out = new ArrayList<>();
        out.add(new double[] {0, 0});
        for (int k = 0; k < 3; k++) {
            double a = 2 * Math.PI * k / 3 + Math.PI / 6;
            out.add(new double[] {Math.cos(a) * SPAWN_R, Math.sin(a) * SPAWN_R});
        }
        return out;
    }

    private static boolean reserved(int x, int z) {
        for (double[] s : spawns()) {
            if (Math.hypot(x - s[0], z - s[1]) < 3.2) {
                return true;
            }
        }
        return Math.abs(x - SEAT_X) <= 2 && Math.abs(z - SEAT_Z) <= 2;
    }

    private static boolean inside(int x, int z) {
        return Math.abs(x) <= HALF && Math.abs(z) <= HALF;
    }

    /** Höhe des Felsens an einer Stelle (0 = Boden). */
    static int height(int x, int z) {
        return inside(x, z) ? HEIGHT[x + HALF][z + HALF] : 0;
    }

    static Cell at(int x, int y, int z) {
        return CELLS.get(key(x, y, z));
    }

    static List<Cell> cells() {
        return List.copyOf(CELLS.values());
    }

    private static void put(int x, int y, int z, String block) {
        if (inside(x, z)) {
            CELLS.put(key(x, y, z), new Cell(x, y, z, block));
        }
    }

    private static void make() {
        Random r = new Random(31);
        // Felsbuckel: Höhe fällt nach außen um je einen Block – so kommt man überall hinauf
        for (int k = 0; k < 15; k++) {
            int cx = r.nextInt(2 * HALF - 5) - HALF + 3;
            int cz = r.nextInt(2 * HALF - 5) - HALF + 3;
            int top = 2 + r.nextInt(2);
            double rad = 1.6 + r.nextDouble() * 1.4;
            for (int x = cx - 4; x <= cx + 4; x++) {
                for (int z = cz - 4; z <= cz + 4; z++) {
                    if (!inside(x, z) || reserved(x, z) || Math.abs(x) == HALF || Math.abs(z) == HALF) {
                        continue;
                    }
                    double d = Math.hypot(x - cx, (z - cz) * 1.25);
                    int h = (int) Math.round(top - Math.max(0, d - rad + 1));
                    if (h > 0) {
                        HEIGHT[x + HALF][z + HALF] = Math.max(HEIGHT[x + HALF][z + HALF], h);
                    }
                }
            }
        }
        smooth();
        String[] stones = {"stone", "andesite", "stone", "tuff", "mossy_cobblestone", "cobblestone"};
        for (int x = -HALF; x <= HALF; x++) {
            for (int z = -HALF; z <= HALF; z++) {
                int h = height(x, z);
                for (int y = 1; y <= h; y++) {
                    boolean top = y == h;
                    String b = top && r.nextInt(5) == 0 ? "moss_block" : stones[r.nextInt(stones.length)];
                    put(x, y, z, b);
                }
                if (h > 0 && r.nextInt(7) == 0) {
                    put(x, h + 1, z, r.nextBoolean() ? "snow[layers=1]" : "short_grass");
                }
            }
        }
        // Geröll und Schneeflecken am Boden (begehbar)
        for (int x = -HALF; x <= HALF; x++) {
            for (int z = -HALF; z <= HALF; z++) {
                if (height(x, z) > 0) {
                    continue;
                }
                boolean nearRock = height(x + 1, z) > 0 || height(x - 1, z) > 0 || height(x, z + 1) > 0
                        || height(x, z - 1) > 0;
                int roll = r.nextInt(100);
                if (nearRock && roll < 35) {
                    put(x, 0, z, roll < 15 ? "gravel" : roll < 28 ? "coarse_dirt" : "andesite");
                } else if (roll < 4) {
                    put(x, 0, z, "moss_block");
                }
            }
        }
        // Wildwechsel: ausgetretene Pfade von den Ecken zur Mitte
        for (int[] c : new int[][] {{-HALF + 1, -HALF + 1}, {HALF - 1, HALF - 1}, {-HALF + 1, HALF - 1}}) {
            double x = c[0];
            double z = c[1];
            for (int step = 0; step < 40; step++) {
                int ix = (int) Math.round(x);
                int iz = (int) Math.round(z);
                if (height(ix, iz) == 0 && at(ix, 0, iz) == null) {
                    put(ix, 0, iz, r.nextInt(3) == 0 ? "podzol" : "coarse_dirt");
                }
                x += -Math.signum(x) * 0.7 + (r.nextDouble() - 0.5) * 0.8;
                z += -Math.signum(z) * 0.7 + (r.nextDouble() - 0.5) * 0.8;
                if (Math.abs(x) < 2 && Math.abs(z) < 2) {
                    break;
                }
            }
        }
        // Kleine Zirbe: Stamm und Krone (zum Verstecken, nicht zum Draufklettern)
        for (int k = 0; k < 5; k++) {
            int x = r.nextInt(2 * HALF - 5) - HALF + 3;
            int z = r.nextInt(2 * HALF - 5) - HALF + 3;
            if (reserved(x, z) || height(x, z) > 0) {
                continue;
            }
            put(x, 1, z, "spruce_log[axis=y]");
            put(x, 2, z, "spruce_log[axis=y]");
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if ((dx != 0 || dz != 0) && !reserved(x + dx, z + dz) && height(x + dx, z + dz) <= 1) {
                        put(x + dx, 3, z + dz, "spruce_leaves[persistent=true]");
                    }
                }
            }
            put(x, 3, z, "spruce_leaves[persistent=true]");
            put(x, 4, z, "spruce_leaves[persistent=true]");
        }
        // Latschen (Bergkiefern) zum Verstecken
        for (int k = 0; k < 30; k++) {
            int x = r.nextInt(2 * HALF - 1) - HALF + 1;
            int z = r.nextInt(2 * HALF - 1) - HALF + 1;
            if (reserved(x, z) || height(x, z) > 0 || at(x, 1, z) != null) {
                continue;
            }
            put(x, 1, z, "spruce_leaves[persistent=true]");
            if (r.nextBoolean() && !reserved(x + 1, z) && height(x + 1, z) == 0) {
                put(x + 1, 1, z, "spruce_leaves[persistent=true]");
            }
            if (r.nextInt(3) == 0) {
                put(x, 2, z, "spruce_leaves[persistent=true]");
            }
        }
        // Almrosen und Enzian (rot, blau) im Gras
        for (int k = 0; k < 40; k++) {
            int x = r.nextInt(2 * HALF - 1) - HALF + 1;
            int z = r.nextInt(2 * HALF - 1) - HALF + 1;
            if (height(x, z) == 0 && at(x, 1, z) == null && at(x, 0, z) == null && !reserved(x, z)) {
                put(x, 1, z, r.nextInt(3) == 0 ? "short_grass" : r.nextBoolean() ? "poppy" : "allium");
            }
        }
        hochsitz();
    }

    /** Höhe so glätten, dass Nachbarn höchstens einen Block auseinander liegen (alles erkletterbar). */
    private static void smooth() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int x = -HALF; x <= HALF; x++) {
                for (int z = -HALF; z <= HALF; z++) {
                    int h = height(x, z);
                    int lowest = Math.min(Math.min(height(x + 1, z), height(x - 1, z)),
                            Math.min(height(x, z + 1), height(x, z - 1)));
                    if (h > lowest + 1) {
                        HEIGHT[x + HALF][z + HALF] = lowest + 1;
                        changed = true;
                    }
                }
            }
        }
    }

    /** Hochsitz: vier Pfosten, Leiter, Plattform mit Geländer und Dach. */
    private static void hochsitz() {
        for (int dx : new int[] {-1, 1}) {
            for (int dz : new int[] {-1, 1}) {
                for (int y = 1; y <= SEAT_Y + 2; y++) {
                    put(SEAT_X + dx, y, SEAT_Z + dz, "stripped_spruce_log[axis=y]");
                }
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) == 1 && Math.abs(dz) == 1) {
                    continue;
                }
                put(SEAT_X + dx, SEAT_Y, SEAT_Z + dz, "spruce_planks");
                if (dx != 0 || dz != 0) {
                    boolean ladderSide = dx == -1 && dz == 0;
                    if (!ladderSide) {
                        put(SEAT_X + dx, SEAT_Y + 1, SEAT_Z + dz, "spruce_fence");
                    }
                }
            }
        }
        // Leiter auf der Westseite, an einem Balken
        for (int y = 1; y < SEAT_Y; y++) {
            put(SEAT_X - 1, y, SEAT_Z, "spruce_log[axis=y]");
            put(SEAT_X - 2, y, SEAT_Z, "ladder[facing=west]");
        }
        put(SEAT_X - 1, SEAT_Y, SEAT_Z, "spruce_planks");
        put(SEAT_X - 2, SEAT_Y, SEAT_Z, "ladder[facing=west]");
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                put(SEAT_X + dx, SEAT_Y + 3, SEAT_Z + dz, "spruce_slab[type=bottom]");
            }
        }
    }
}
