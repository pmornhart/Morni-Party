package de.moinmorni.party.world;

import de.moinmorni.party.board.BoardId;
import de.moinmorni.party.board.BoardLayout;
import de.moinmorni.party.board.Field;
import de.moinmorni.party.board.FieldType;
import java.util.Random;

/**
 * Baupläne der drei Spielbretter aus dem Weg ({@link BoardLayout}): Gelände, das den Weg trägt, Felder als bunte
 * 3×3-Platten und die Ausstattung je Brett – Almhütte und Gipfelkreuz am Morni-Kogel, Garten, Mauern und Bergfried
 * auf Schloss Weißenstein, Erzadern, Stützbalken, Förderturm und Goldkammer im Goldstollen.
 */
public final class BoardPlans {

    private static final int EDGE = 7;
    private static final double PATH = 2.3;
    private static final double PATH_CORE = 1.2;

    private BoardPlans() {
    }

    /** Farbe eines Felds. */
    public static String fieldBlock(FieldType t) {
        return switch (t) {
            case START -> "white_concrete";
            case TALER -> "light_blue_concrete";
            case VERLUST -> "red_concrete";
            case GLUECK -> "lime_concrete";
            case PECH -> "black_concrete";
            case DUELL -> "orange_concrete";
            case LADEN -> "yellow_concrete";
            case GEGENSTAND -> "magenta_concrete";
            case ZIEL -> "gold_block";
        };
    }

    /** Ein Feld: 3×3 in seiner Farbe mit Rand, darüber frei. */
    public static void pad(Plan p, int cx, int y, int cz, FieldType t, String border) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean ring = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                p.set(cx + dx, y, cz + dz, ring ? border : fieldBlock(t));
                for (int h = 1; h <= 3; h++) {
                    if (!ring || h > 1) {
                        p.clear(cx + dx, y + h, cz + dz);
                    }
                }
            }
        }
        if (t == FieldType.START) {
            p.set(cx, y, cz, "quartz_block");
        }
    }

    public static Plan build(BoardLayout layout) {
        Plan p = new Plan();
        Terrain t = new Terrain(layout);
        Random r = new Random(1000 + layout.id().ordinal());
        BoardId id = layout.id();
        double h = layout.shape().height();
        for (int x = -t.size; x <= t.size; x++) {
            for (int z = -t.size; z <= t.size; z++) {
                if (!t.inside(x, z)) {
                    continue;
                }
                int top = t.top(x, z);
                int from = Math.min(top - 3, t.lowestNeighbour(x, z) - 1);
                double d = Math.hypot(x, z);
                if (d > t.island - 2.5) {
                    from = Math.min(from, Math.min(top - 3, -4));
                }
                double dist = t.trail(x, z);
                for (int y = from; y <= top; y++) {
                    p.set(x, y, z, block(id, r, y, top, dist, h, t, x, z));
                }
                // Unterseite: Fels läuft nach unten spitz zu (die Insel schwebt)
                double f = Math.max(0, 1 - (d / t.island) * (d / t.island));
                int below = 1 + (int) Math.round(9 * Math.sqrt(f)) + r.nextInt(2);
                for (int y = from - below; y < from; y++) {
                    p.setIfEmpty(x, y, z, Decor.rock(r, from - y + 6));
                }
                decorate(p, id, r, x, top, z, dist, h, t);
            }
        }
        for (Field f : layout.fields()) {
            pad(p, f.x(), f.y(), f.z(), f.type(), border(id));
        }
        switch (id) {
            case ALMSOMMER -> alm(p, layout, t, r);
            case SCHLOSS -> castle(p, layout, t, r);
            case GOLDSTOLLEN -> mine(p, layout, t, r);
        }
        p.connect();
        return p;
    }

    private static String border(BoardId id) {
        return switch (id) {
            case ALMSOMMER -> "stripped_spruce_wood[axis=y]";
            case SCHLOSS -> "polished_diorite";
            case GOLDSTOLLEN -> "spruce_planks";
        };
    }

    // ---------- Gelände ----------

    /** Höhen, Abstand zum Weg und Höhe des Wegs je Säule. */
    static final class Terrain {
        final BoardLayout layout;
        final int size;
        final double island;
        private final double[][] dist;
        private final double[][] pathY;
        private final double[][] pathR;
        private final int[][] tops;

        Terrain(BoardLayout layout) {
            this.layout = layout;
            this.island = layout.shape().outer() + EDGE;
            this.size = (int) Math.ceil(island) + 2;
            int n = 2 * size + 1;
            dist = new double[n][n];
            pathY = new double[n][n];
            pathR = new double[n][n];
            tops = new int[n][n];
            for (double[] row : dist) {
                java.util.Arrays.fill(row, 99);
            }
            for (BoardLayout.Sample s : layout.samples()) {
                int cx = (int) Math.round(s.x());
                int cz = (int) Math.round(s.z());
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        int x = cx + dx;
                        int z = cz + dz;
                        if (Math.abs(x) > size || Math.abs(z) > size) {
                            continue;
                        }
                        double d = Math.hypot(s.x() - x, s.z() - z);
                        if (d < dist[x + size][z + size]) {
                            dist[x + size][z + size] = d;
                            pathY[x + size][z + size] = s.y();
                            pathR[x + size][z + size] = s.r();
                        }
                    }
                }
            }
            for (int x = -size; x <= size; x++) {
                for (int z = -size; z <= size; z++) {
                    tops[x + size][z + size] = computeTop(x, z);
                }
            }
        }

        boolean inside(int x, int z) {
            return Math.hypot(x, z) <= island + 0.3 * Math.sin(x * 0.7 + z * 0.4);
        }

        double trail(int x, int z) {
            return dist[x + size][z + size];
        }

        /** Liegt die Säule außen neben dem Weg (weiter weg von der Mitte als der Weg)? */
        boolean outerSide(int x, int z) {
            return Math.hypot(x, z) > pathR[x + size][z + size];
        }

        int top(int x, int z) {
            return tops[x + size][z + size];
        }

        int lowestNeighbour(int x, int z) {
            int low = top(x, z);
            for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = x + d[0];
                int nz = z + d[1];
                if (Math.abs(nx) > size || Math.abs(nz) > size || !inside(nx, nz)) {
                    low = Math.min(low, top(x, z) - 6);
                } else {
                    low = Math.min(low, top(nx, nz));
                }
            }
            return low;
        }

        private int computeTop(int x, int z) {
            double d = Math.hypot(x, z);
            if (dist[x + size][z + size] <= PATH) {
                return (int) Math.round(pathY[x + size][z + size]);
            }
            BoardLayout.Shape s = layout.shape();
            double base;
            if (d > s.outer()) {
                base = s.up() ? -(d - s.outer()) * 0.9 : s.height() + Math.min(3, (d - s.outer()) * 0.8);
            } else {
                base = layout.terrainHeight(d);
            }
            double amp = switch (layout.id()) {
                case ALMSOMMER -> 1.4;
                case SCHLOSS -> 0.4;
                case GOLDSTOLLEN -> 1.0;
            };
            double far = Math.min(1, Math.max(0, (dist[x + size][z + size] - PATH) / 3));
            double noise = Math.sin(x * 0.31 + z * 0.17) * 0.6 + Math.sin(x * 0.13 - z * 0.29) * 0.8
                    + Math.cos((x + z) * 0.21) * 0.5;
            return (int) Math.round(base + noise * amp * far);
        }
    }

    // ---------- Blöcke ----------

    private static String block(BoardId id, Random r, int y, int top, double dist, double h, Terrain t, int x, int z) {
        boolean surface = y == top;
        int depth = top - y;
        boolean path = dist <= PATH;
        boolean steep = top - t.lowestNeighbour(x, z) >= 3;
        return switch (id) {
            case ALMSOMMER -> {
                if (surface && path) {
                    yield dist <= PATH_CORE ? "dirt_path" : r.nextInt(3) == 0 ? "coarse_dirt" : "dirt_path";
                }
                if (surface) {
                    if (top > h * 0.82) {
                        yield r.nextInt(5) == 0 ? "stone" : "snow_block";
                    }
                    if (steep) {
                        yield Decor.pick(r, "stone", "andesite", "stone", "mossy_cobblestone");
                    }
                    yield r.nextInt(12) == 0 ? "moss_block" : "grass_block[snowy=false]";
                }
                yield depth <= 2 && !steep ? "dirt" : Decor.rock(r, depth);
            }
            case SCHLOSS -> {
                boolean castle = top >= h * 0.42;
                if (surface && path) {
                    yield castle ? (r.nextInt(4) == 0 ? "cracked_stone_bricks" : "stone_bricks")
                            : dist <= PATH_CORE ? "gravel" : "dirt_path";
                }
                if (surface) {
                    yield castle ? (r.nextInt(6) == 0 ? "smooth_quartz" : "calcite") : "grass_block[snowy=false]";
                }
                if (castle || steep) {
                    yield r.nextInt(7) == 0 ? "mossy_stone_bricks" : r.nextInt(9) == 0 ? "cracked_stone_bricks"
                            : "stone_bricks";
                }
                yield depth <= 2 ? "dirt" : Decor.rock(r, depth);
            }
            case GOLDSTOLLEN -> {
                if (surface && path) {
                    yield dist <= PATH_CORE ? "coarse_dirt" : "gravel";
                }
                boolean deep = top < h * 0.4;
                int roll = r.nextInt(100);
                if (!path && roll < (surface ? 3 : 2)) {
                    yield deep ? "deepslate_gold_ore" : "gold_ore";
                }
                if (!path && roll < 6) {
                    yield deep ? "deepslate_iron_ore" : "iron_ore";
                }
                if (!path && roll < 9) {
                    yield deep ? "deepslate_coal_ore" : "coal_ore";
                }
                if (deep) {
                    yield Decor.pick(r, "deepslate", "tuff", "cobbled_deepslate", "deepslate", "polished_tuff");
                }
                yield surface ? Decor.pick(r, "stone", "andesite", "tuff", "stone", "cobblestone", "gravel")
                        : Decor.rock(r, depth);
            }
        };
    }

    private static void decorate(Plan p, BoardId id, Random r, int x, int top, int z, double dist, double h,
            Terrain t) {
        if (dist <= PATH + 0.7) {
            return;
        }
        boolean steep = top - t.lowestNeighbour(x, z) >= 3;
        switch (id) {
            case ALMSOMMER -> {
                if (steep || top > h * 0.82 || Math.hypot(x, z) > t.island - 2) {
                    return;
                }
                int roll = r.nextInt(100);
                if (roll < 9) {
                    p.setIfEmpty(x, top + 1, z, Decor.pick(r, "short_grass", "short_grass", "dandelion", "poppy",
                            "cornflower", "oxeye_daisy", "allium", "azure_bluet"));
                } else if (roll < 11 && dist > 5 && top < h * 0.7) {
                    Decor.spruce(p, x, top + 1, z, 5 + r.nextInt(4), r);
                }
            }
            case SCHLOSS -> {
                boolean castle = top >= h * 0.42;
                if (castle && dist < 3.6 && t.outerSide(x, z) && t.top(x, z) >= t.lowestNeighbour(x, z)) {
                    p.setIfEmpty(x, top + 1, z, "stone_brick_wall");
                    return;
                }
                if (!castle && !steep && dist < 3.8 && t.outerSide(x, z)) {
                    p.setIfEmpty(x, top + 1, z, "azalea_leaves[persistent=true]");
                    return;
                }
                if (!castle && !steep && r.nextInt(14) == 0 && Math.hypot(x, z) < t.island - 2) {
                    p.setIfEmpty(x, top + 1, z, Decor.pick(r, "red_tulip", "white_tulip", "pink_tulip",
                            "lily_of_the_valley", "short_grass"));
                }
            }
            case GOLDSTOLLEN -> {
                if (!steep && dist > 3 && r.nextInt(60) == 0) {
                    p.setIfEmpty(x, top + 1, z, Decor.pick(r, "cobblestone_slab[type=bottom]", "brown_mushroom",
                            "dead_bush"));
                }
            }
        }
    }

    // ---------- Ausstattung je Brett ----------

    /** Ort in Polarkoordinaten (Abstand, Winkel) auf ganze Blöcke gerundet. */
    private static int[] polar(double r, double a) {
        return new int[] {(int) Math.round(Math.cos(a) * r), (int) Math.round(Math.sin(a) * r)};
    }

    private static void foundation(Plan p, Terrain t, int x0, int z0, int x1, int z1, int floor, String block) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int low = Math.abs(x) <= t.size && Math.abs(z) <= t.size ? t.top(x, z) : floor - 4;
                for (int y = Math.min(low, floor) - 2; y <= floor; y++) {
                    p.set(x, y, z, block);
                }
                for (int y = floor + 1; y <= floor + 6; y++) {
                    p.clear(x, y, z);
                }
            }
        }
    }

    private static void alm(Plan p, BoardLayout layout, Terrain t, Random r) {
        BoardLayout.Shape s = layout.shape();
        // Gipfelkreuz in der Mitte
        int top = t.top(0, 0);
        p.fill(-1, top - 1, -1, 1, top, 1, "cobblestone");
        for (int y = 1; y <= 8; y++) {
            p.set(0, top + y, 0, "spruce_log[axis=y]");
        }
        for (int dx = -2; dx <= 2; dx++) {
            if (dx != 0) {
                p.set(dx, top + 6, 0, "spruce_log[axis=x]");
            }
        }
        p.set(0, top + 9, 0, "lightning_rod");
        // Almhütte zwischen den Wegschleifen, unweit vom Start
        int[] c = polar(s.outer() - 9, s.angle() - 0.55);
        int floor = (int) Math.round(layout.terrainHeight(s.outer() - 9));
        hut(p, t, c[0], c[1], floor);
        // Weide mit Heuballen daneben
        int[] w = polar(s.outer() - 10, s.angle() - 0.95);
        int wy = t.top(w[0], w[1]);
        for (int i = -3; i <= 3; i++) {
            p.setIfEmpty(w[0] + i, wy + 1, w[1] - 3, "spruce_fence");
            p.setIfEmpty(w[0] + i, wy + 1, w[1] + 3, "spruce_fence");
            p.setIfEmpty(w[0] - 3, wy + 1, w[1] + i, "spruce_fence");
            p.setIfEmpty(w[0] + 3, wy + 1, w[1] + i, "spruce_fence");
        }
        p.setIfEmpty(w[0], wy + 1, w[1], "hay_block[axis=y]");
        p.setIfEmpty(w[0] + 1, wy + 1, w[1], "hay_block[axis=x]");
    }

    /** Kleine Almhütte (Holz auf Steinsockel, Satteldach). */
    private static void hut(Plan p, Terrain t, int cx, int cz, int floor) {
        foundation(p, t, cx - 3, cz - 3, cx + 3, cz + 3, floor, "cobblestone");
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                boolean wall = Math.abs(x) == 3 || Math.abs(z) == 3;
                p.set(cx + x, floor, cz + z, wall ? "cobblestone" : "spruce_planks");
                if (wall) {
                    boolean corner = Math.abs(x) == 3 && Math.abs(z) == 3;
                    for (int y = 1; y <= 3; y++) {
                        p.set(cx + x, floor + y, cz + z, corner ? "stripped_spruce_log[axis=y]"
                                : y == 2 && (x == 0 || z == 0) ? "glass_pane" : "spruce_planks");
                    }
                }
            }
        }
        p.clear(cx, floor + 1, cz + 3);
        p.set(cx, floor + 1, cz + 3, "spruce_door[facing=south,half=lower,hinge=left,open=false]");
        p.set(cx, floor + 2, cz + 3, "spruce_door[facing=south,half=upper,hinge=left,open=false]");
        for (int layer = 0; layer <= 4; layer++) {
            for (int x = -4; x <= 4; x++) {
                int y = floor + 4 + layer;
                if (layer < 4) {
                    p.set(cx + x, y, cz - 4 + layer, "dark_oak_stairs[facing=south,half=bottom,shape=straight]");
                    p.set(cx + x, y, cz + 4 - layer, "dark_oak_stairs[facing=north,half=bottom,shape=straight]");
                    for (int z = -3 + layer; z <= 3 - layer; z++) {
                        if (Math.abs(x) == 3) {
                            p.set(cx + x, y, cz + z, "spruce_planks");
                        }
                    }
                } else {
                    p.set(cx + x, y, cz, "dark_oak_slab[type=bottom]");
                }
            }
        }
        p.set(cx + 2, floor + 1, cz - 2, "crafting_table");
        p.set(cx - 2, floor + 1, cz - 2, "barrel[facing=up]");
        p.set(cx - 2, floor + 2, cz - 2, "lantern[hanging=false]");
        p.set(cx + 4, floor + 1, cz + 1, "flower_pot");
        p.set(cx - 4, floor + 1, cz + 1, "spruce_stairs[facing=east,half=bottom,shape=straight]");
    }

    private static void castle(Plan p, BoardLayout layout, Terrain t, Random r) {
        BoardLayout.Shape s = layout.shape();
        int top = t.top(0, 0);
        // Bergfried: runder Turm in der Mitte mit blauem Kegeldach
        int rad = 3;
        for (int x = -rad; x <= rad; x++) {
            for (int z = -rad; z <= rad; z++) {
                double d = Math.hypot(x, z);
                if (d > rad + 0.4) {
                    continue;
                }
                for (int y = top - 2; y <= top + 12; y++) {
                    boolean shell = d > rad - 0.6;
                    if (shell) {
                        boolean window = (y - top) % 5 == 3 && (x == 0 || z == 0);
                        p.set(x, y, z, window ? "light_blue_stained_glass_pane" : "calcite");
                    } else if (y == top) {
                        p.set(x, y, z, "polished_diorite");
                    } else {
                        p.clear(x, y, z);
                    }
                }
            }
        }
        for (int layer = 0; layer <= 6; layer++) {
            double rr = rad + 1 - layer * 0.65;
            for (int x = -rad - 1; x <= rad + 1; x++) {
                for (int z = -rad - 1; z <= rad + 1; z++) {
                    if (Math.hypot(x, z) <= rr + 0.3) {
                        p.set(x, top + 13 + layer, z, layer % 2 == 0 ? "blue_terracotta" : "light_blue_terracotta");
                    }
                }
            }
        }
        p.set(0, top + 20, 0, "lightning_rod");
        p.banner(rad + 1, top + 10, 0, Decor.bannerBlock(2, true, "east"), Decor.bannerPatterns(2));
        p.banner(-rad - 1, top + 10, 0, Decor.bannerBlock(0, true, "west"), Decor.bannerPatterns(0));
        // Brunnen im Schlossgarten
        int[] c = polar(s.outer() - 9, s.angle() - 0.5);
        int floor = (int) Math.round(layout.terrainHeight(s.outer() - 9));
        foundation(p, t, c[0] - 3, c[1] - 3, c[0] + 3, c[1] + 3, floor, "stone_bricks");
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                boolean rim = Math.max(Math.abs(x), Math.abs(z)) == 3;
                p.set(c[0] + x, floor, c[1] + z, rim ? "polished_diorite" : "gravel");
                if (Math.max(Math.abs(x), Math.abs(z)) == 2) {
                    p.set(c[0] + x, floor + 1, c[1] + z, "quartz_slab[type=bottom]");
                } else if (Math.max(Math.abs(x), Math.abs(z)) < 2) {
                    p.set(c[0] + x, floor + 1, c[1] + z, "water");
                }
            }
        }
        p.fill(c[0], floor + 1, c[1], c[0], floor + 3, c[1], "quartz_pillar[axis=y]");
        p.set(c[0], floor + 4, c[1], "sea_lantern");
        // Laubengang (Rosenbögen) entlang des unteren Wegs
        for (int i = 0; i < layout.samples().size(); i += 220) {
            BoardLayout.Sample sm = layout.samples().get(i);
            if (sm.y() >= s.height() * 0.42) {
                continue;
            }
            int bx = (int) Math.round(sm.x() * (sm.r() + 3) / sm.r());
            int bz = (int) Math.round(sm.z() * (sm.r() + 3) / sm.r());
            int by = t.top(bx, bz);
            p.fill(bx, by + 1, bz, bx, by + 3, bz, "birch_fence");
            p.set(bx, by + 4, bz, "flowering_azalea_leaves[persistent=true]");
        }
    }

    private static void mine(Plan p, BoardLayout layout, Terrain t, Random r) {
        BoardLayout.Shape s = layout.shape();
        // Goldkammer unten in der Mitte
        int bottom = t.top(0, 0);
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                double d = Math.hypot(x, z);
                if (d > 5.4 || t.trail(x, z) <= PATH + 0.5) {
                    continue;
                }
                int roll = r.nextInt(10);
                if (roll < 2) {
                    p.set(x, t.top(x, z) + 1, z, "raw_gold_block");
                } else if (roll < 3) {
                    p.set(x, t.top(x, z) + 1, z, "gold_block");
                    if (r.nextBoolean()) {
                        p.set(x, t.top(x, z) + 2, z, "raw_gold_block");
                    }
                } else if (roll < 4) {
                    p.set(x, t.top(x, z) + 1, z, "lantern[hanging=false]");
                }
            }
        }
        p.set(0, bottom + 1, 0, "chest[facing=south]");
        p.set(1, bottom + 1, 0, "gold_block");
        p.set(-1, bottom + 1, 0, "gold_block");
        p.set(0, bottom + 2, 0, "gold_block");
        // Stützbalken mit Laternen am Außenrand des Wegs
        int n = 0;
        for (BoardLayout.Sample sm : layout.samples()) {
            if (n++ % 140 != 0) {
                continue;
            }
            double out = (sm.r() + 3) / sm.r();
            int bx = (int) Math.round(sm.x() * out);
            int bz = (int) Math.round(sm.z() * out);
            if (Math.abs(bx) > t.size || Math.abs(bz) > t.size) {
                continue;
            }
            int by = (int) Math.round(sm.y());
            for (int y = by + 1; y <= by + 4; y++) {
                p.set(bx, y, bz, "stripped_spruce_log[axis=y]");
            }
            double in = (sm.r() - 0.5) / sm.r();
            int ix = (int) Math.round(sm.x() * in);
            int iz = (int) Math.round(sm.z() * in);
            p.set(ix, by + 5, iz, "spruce_slab[type=bottom]");
            p.set(bx, by + 5, bz, "spruce_planks");
            p.set(bx, by + 4, bz, "stripped_spruce_log[axis=y]");
            int lx = (int) Math.round(sm.x() * (sm.r() + 1.8) / sm.r());
            int lz = (int) Math.round(sm.z() * (sm.r() + 1.8) / sm.r());
            p.set(lx, by + 4, lz, "lantern[hanging=true]");
            p.set(lx, by + 5, lz, "spruce_slab[type=bottom]");
        }
        // Förderturm am Rand, hinter dem Start
        int[] c = polar(s.outer() + 4, s.angle() - 0.12);
        int floor = t.top(c[0], c[1]);
        for (int[] leg : new int[][] {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            for (int y = 1; y <= 12; y++) {
                p.set(c[0] + leg[0], floor + y, c[1] + leg[1], "dark_oak_log[axis=y]");
            }
        }
        for (int y : new int[] {5, 9, 12}) {
            for (int i = -2; i <= 2; i++) {
                p.set(c[0] + i, floor + y, c[1] - 2, "dark_oak_planks");
                p.set(c[0] + i, floor + y, c[1] + 2, "dark_oak_planks");
                p.set(c[0] - 2, floor + y, c[1] + i, "dark_oak_planks");
                p.set(c[0] + 2, floor + y, c[1] + i, "dark_oak_planks");
            }
        }
        p.fill(c[0] - 1, floor + 13, c[1], c[0] + 1, floor + 13, c[1], "dark_oak_slab[type=bottom]");
        for (int y = 3; y <= 12; y++) {
            p.set(c[0], floor + y, c[1], "iron_chain[axis=y]");
        }
        p.set(c[0], floor + 2, c[1], "lantern[hanging=true]");
        p.set(c[0], floor + 1, c[1], "barrel[facing=up]");
    }
}
