package de.moinmorni.party.world;

import de.moinmorni.party.board.BoardId;
import de.moinmorni.party.board.BoardLayout;
import de.moinmorni.party.board.Field;
import de.moinmorni.party.board.FieldType;
import java.util.List;
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
    /** Schloss: ab diesem Anteil der Höhe beginnt die Burg (darunter Schlossgarten). */
    private static final double CASTLE = 0.42;
    /** Schloss: Höhe einer Terrassenstufe. */
    private static final int STEP = 3;

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
            case FAEHRE -> "cyan_concrete";
            case RIESENRAD -> "pink_concrete";
            case FAHRGESCHAEFT -> "purple_concrete";
            case LAWINE -> "light_gray_concrete";
            case EIS -> "packed_ice";
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
            case KOENIGSSEE -> BoardPlans2.lake(p, layout, t, r);
            case WIESN -> BoardPlans2.wiesn(p, layout, t, r);
            case WINTERSONNWEND -> BoardPlans2.winter(p, layout, t, r);
        }
        p.connect();
        return p;
    }

    private static String border(BoardId id) {
        return switch (id) {
            case ALMSOMMER -> "stripped_spruce_wood[axis=y]";
            case SCHLOSS -> "polished_diorite";
            case GOLDSTOLLEN, KOENIGSSEE -> "spruce_planks";
            case WIESN -> "stripped_oak_wood[axis=y]";
            case WINTERSONNWEND -> "snow_block";
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

        /** Nächstes Feld (Index) in der Nähe, oder -1. */
        int nearestField(int x, int z) {
            int best = -1;
            double bd = 30;
            for (Field f : layout.fields()) {
                double d = Math.hypot(f.x() - x, f.z() - z);
                if (d < bd) {
                    bd = d;
                    best = f.index();
                }
            }
            return best;
        }

        /** Höhe des nächsten Wegpunkts (nur in der Nähe des Wegs sinnvoll). */
        int pathHeight(int x, int z) {
            return (int) Math.round(pathY[x + size][z + size]);
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
                case KOENIGSSEE -> 0.5;
                case WIESN -> 0.35;
                case WINTERSONNWEND -> 1.5;
            };
            if (layout.id() == BoardId.KOENIGSSEE) {
                return BoardPlans2.lakeTop(x, z, d, s, dist[x + size][z + size]);
            }
            if (layout.id() == BoardId.SCHLOSS && base >= s.height() * CASTLE) {
                // Burg: Terrassen mit senkrechten Mauern statt glattem Hang
                return (int) (Math.round(base / STEP) * STEP);
            }
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
                boolean castle = top >= h * CASTLE;
                if (surface && path) {
                    yield castle ? (r.nextInt(5) == 0 ? "polished_andesite" : "stone_bricks")
                            : dist <= PATH_CORE ? "gravel" : "dirt_path";
                }
                if (castle && surface) {
                    // Terrassen: Rasen im Burghof, weiße Mauerkrone an der Kante
                    yield steep ? "smooth_quartz" : r.nextInt(9) == 0 ? "moss_block" : "grass_block[snowy=false]";
                }
                if (castle) {
                    // Mauern: weißer Stein mit einem Band aus Steinziegeln
                    yield Math.floorMod(y, STEP) == 0 ? "stone_bricks" : r.nextInt(8) == 0 ? "polished_diorite" : "calcite";
                }
                if (surface) {
                    yield "grass_block[snowy=false]";
                }
                if (steep) {
                    yield r.nextInt(5) == 0 ? "mossy_stone_bricks" : "stone_bricks";
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
            case KOENIGSSEE -> BoardPlans2.lakeBlock(r, y, top, dist, x, z, t);
            case WIESN -> {
                if (surface && path) {
                    yield dist <= PATH_CORE ? "dirt_path" : r.nextInt(4) == 0 ? "gravel" : "dirt_path";
                }
                if (surface) {
                    yield r.nextInt(14) == 0 ? "moss_block" : "grass_block[snowy=false]";
                }
                yield depth <= 2 ? "dirt" : Decor.rock(r, depth);
            }
            case WINTERSONNWEND -> {
                if (surface && path) {
                    int field = t.nearestField(x, z);
                    boolean icy = field >= 0 && t.layout.field(field).type() == FieldType.EIS;
                    yield icy ? (dist <= PATH_CORE ? "blue_ice" : "packed_ice")
                            : dist <= PATH_CORE ? "gravel" : r.nextInt(3) == 0 ? "andesite" : "light_gray_concrete_powder";
                }
                if (surface) {
                    double band = Math.sin(x * 0.27 + z * 0.13) + Math.cos(x * 0.11 - z * 0.31);
                    if (steep || band > 1.45) {
                        yield Decor.pick(r, "stone", "andesite", "stone", "tuff", "snow_block");
                    }
                    if (band < -1.6) {
                        yield r.nextBoolean() ? "packed_ice" : "snow_block";
                    }
                    yield "snow_block";
                }
                yield depth <= 1 && !steep ? "snow_block" : Decor.rock(r, depth);
            }
        };
    }

    private static void decorate(Plan p, BoardId id, Random r, int x, int top, int z, double dist, double h,
            Terrain t) {
        if (id == BoardId.SCHLOSS) {
            castleColumn(p, r, x, top, z, dist, h, t);
            return;
        }
        if (id == BoardId.KOENIGSSEE) {
            BoardPlans2.lakeColumn(p, r, x, top, z, dist, t);
            return;
        }
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
                // eigene Methode (castleColumn)
            }
            case GOLDSTOLLEN -> {
                if (!steep && dist > 3 && r.nextInt(60) == 0) {
                    p.setIfEmpty(x, top + 1, z, Decor.pick(r, "cobblestone_slab[type=bottom]", "brown_mushroom",
                            "dead_bush"));
                }
            }
            case KOENIGSSEE -> {
                // eigene Methode (lakeColumn)
            }
            case WIESN -> {
                if (steep || Math.hypot(x, z) > t.island - 2) {
                    return;
                }
                int roll = r.nextInt(100);
                if (roll < 7) {
                    p.setIfEmpty(x, top + 1, z, Decor.pick(r, "short_grass", "short_grass", "dandelion", "poppy",
                            "oxeye_daisy", "red_tulip", "white_tulip"));
                }
            }
            case WINTERSONNWEND -> {
                if (Math.hypot(x, z) > t.island - 2) {
                    return;
                }
                int roll = r.nextInt(100);
                if (!steep && roll < 18) {
                    p.setIfEmpty(x, top + 1, z, "snow[layers=" + (1 + r.nextInt(2)) + "]");
                } else if (!steep && roll < 20 && dist > 5 && top < h * 0.78) {
                    BoardPlans2.snowySpruce(p, x, top + 1, z, 5 + r.nextInt(4), r);
                }
            }
        }
    }

    /** Schloss, je Säule: Brustwehr am Weg, Zinnen an den Terrassenkanten, Hecken und Blumen im Garten. */
    private static void castleColumn(Plan p, Random r, int x, int top, int z, double dist, double h, Terrain t) {
        boolean castle = top >= h * CASTLE || (dist <= 4 && t.pathHeight(x, z) >= h * CASTLE);
        boolean steep = top - t.lowestNeighbour(x, z) >= 3;
        boolean edge = Math.hypot(x, z) > t.island - 2;
        if (dist <= PATH) {
            return;
        }
        if (castle && dist <= 3.4 && t.outerSide(x, z)) {
            // Brustwehr außen am Weg: Mauer bis über den Weg, oben Zinnen
            int wy = t.pathHeight(x, z);
            for (int y = top + 1; y <= wy + 1; y++) {
                p.set(x, y, z, Math.floorMod(y, STEP) == 0 ? "stone_bricks" : "calcite");
            }
            p.set(x, Math.max(top, wy + 1), z, "smooth_quartz");
            if (Math.floorMod(x + z, 2) == 0) {
                p.set(x, Math.max(top, wy + 1) + 1, z, "polished_diorite");
            }
            return;
        }
        if (castle && steep && dist > 3.6 && !edge && Math.floorMod(x + 2 * z, 3) != 0) {
            p.setIfEmpty(x, top + 1, z, "diorite_wall");
            return;
        }
        if (castle && !steep && dist > 4.5 && !edge) {
            int roll = r.nextInt(70);
            if (roll == 0) {
                p.setIfEmpty(x, top + 1, z, "potted_red_tulip");
            } else if (roll == 1) {
                p.setIfEmpty(x, top + 1, z, "azalea_leaves[persistent=true]");
            } else if (roll < 6) {
                p.setIfEmpty(x, top + 1, z, Decor.pick(r, "short_grass", "lily_of_the_valley", "white_tulip"));
            }
            return;
        }
        if (castle || steep || edge) {
            return;
        }
        // Schlossgarten: Hecken beidseits des Wegs, Blumenbeete
        if (dist < 3.8) {
            p.setIfEmpty(x, top + 1, z, "azalea_leaves[persistent=true]");
            return;
        }
        if (Math.floorMod(x * 3 + z * 7, 23) < 3) {
            p.setIfEmpty(x, top + 1, z, Decor.pick(r, "red_tulip", "white_tulip", "pink_tulip", "orange_tulip"));
        } else if (r.nextInt(18) == 0) {
            p.setIfEmpty(x, top + 1, z, Decor.pick(r, "lily_of_the_valley", "short_grass", "cornflower", "allium"));
        }
    }

    // ---------- Ausstattung je Brett ----------

    /** Ort in Polarkoordinaten (Abstand, Winkel) auf ganze Blöcke gerundet. */
    static int[] polar(double r, double a) {
        return new int[] {(int) Math.round(Math.cos(a) * r), (int) Math.round(Math.sin(a) * r)};
    }

    static void foundation(Plan p, Terrain t, int x0, int z0, int x1, int z1, int floor, String block) {
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
        double line = s.height() * CASTLE;
        keep(p, t.top(0, 0));
        List<BoardLayout.Sample> sm = layout.samples();
        // Ecktürme außen an der Burgmauer, in gleichen Abständen entlang des Wegs
        for (int i = 0; i < sm.size(); i += 230) {
            BoardLayout.Sample a = sm.get(i);
            if (a.y() < line + 1 || a.r() < s.inner() + 6) {
                continue;
            }
            double out = (a.r() + 5.5) / a.r();
            int cx = (int) Math.round(a.x() * out);
            int cz = (int) Math.round(a.z() * out);
            if (free(layout, t, cx, cz, 2.5)) {
                tower(p, t, cx, cz, (int) Math.round(a.y()) + 7, 2, i / 230);
            }
        }
        // Torhaus, wo der Weg vom Garten in die Burg führt
        for (int i = 0; i < sm.size() - 40; i++) {
            if (sm.get(i).y() >= line - 1 && fieldDistance(layout, sm.get(i)) > 4.5) {
                gatehouse(p, t, sm.get(i - 1 < 0 ? 0 : i - 1), sm.get(Math.min(sm.size() - 1, i + 20)));
                break;
            }
        }
        // Rosenbögen über dem Gartenweg
        for (int i = 60; i < sm.size(); i += 150) {
            BoardLayout.Sample a = sm.get(i);
            if (a.y() >= line - 3 || fieldDistance(layout, a) < 4.5) {
                continue;
            }
            arch(p, a, sm.get(Math.min(sm.size() - 1, i + 15)));
        }
        // Zwei Brunnen und Formschnitt-Bäumchen im Garten
        fountain(p, t, layout, s.outer() - 9, s.angle() - 0.5);
        fountain(p, t, layout, s.outer() - 8, s.angle() + 2.2);
        for (int k = 0; k < 14; k++) {
            double a = s.angle() + k * 0.45;
            double rr = s.outer() - 4 - (k % 3) * 6;
            int[] c = polar(rr, a);
            if (Math.abs(c[0]) <= t.size && Math.abs(c[1]) <= t.size && t.trail(c[0], c[1]) > 4.5
                    && t.top(c[0], c[1]) < line - 2 && t.top(c[0], c[1]) - t.lowestNeighbour(c[0], c[1]) < 2) {
                int y = t.top(c[0], c[1]);
                p.set(c[0], y + 1, c[1], "oak_log[axis=y]");
                p.set(c[0], y + 2, c[1], "oak_log[axis=y]");
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dy = 3; dy <= 4; dy++) {
                            if (Math.abs(dx) + Math.abs(dz) + (dy == 4 ? 1 : 0) <= 2) {
                                p.setIfEmpty(c[0] + dx, y + dy, c[1] + dz, k % 2 == 0
                                        ? "flowering_azalea_leaves[persistent=true]" : "azalea_leaves[persistent=true]");
                            }
                        }
                    }
                }
            }
        }
    }

    /** Abstand zum nächsten Feld. */
    private static double fieldDistance(BoardLayout layout, BoardLayout.Sample a) {
        double best = 99;
        for (Field f : layout.fields()) {
            best = Math.min(best, Math.hypot(f.x() - a.x(), f.z() - a.z()));
        }
        return best;
    }

    /** Ist hier im Umkreis {@code radius} kein Weg und kein Feld? */
    private static boolean free(BoardLayout layout, Terrain t, int cx, int cz, double radius) {
        for (int dx = (int) -Math.ceil(radius); dx <= Math.ceil(radius); dx++) {
            for (int dz = (int) -Math.ceil(radius); dz <= Math.ceil(radius); dz++) {
                int x = cx + dx;
                int z = cz + dz;
                if (Math.hypot(dx, dz) > radius + 0.4) {
                    continue;
                }
                if (Math.abs(x) > t.size || Math.abs(z) > t.size || !t.inside(x, z) || t.trail(x, z) <= PATH + 0.8) {
                    return false;
                }
            }
        }
        for (Field f : layout.fields()) {
            if (Math.hypot(f.x() - cx, f.z() - cz) < radius + 3) {
                return false;
            }
        }
        return true;
    }

    /** Bergfried: runder weißer Turm mit Zinnenkranz, blauem Kegeldach und Fahnen. */
    private static void keep(Plan p, int top) {
        int rad = 4;
        int height = 18;
        for (int x = -rad - 1; x <= rad + 1; x++) {
            for (int z = -rad - 1; z <= rad + 1; z++) {
                double d = Math.hypot(x, z);
                if (d > rad + 0.5) {
                    continue;
                }
                boolean shell = d > rad - 0.6;
                for (int y = top - 3; y <= top + height; y++) {
                    if (shell) {
                        int rel = y - top;
                        boolean window = rel % 6 == 4 && (x == 0 || z == 0);
                        p.set(x, y, z, window ? "light_blue_stained_glass_pane"
                                : rel % 6 == 0 ? "stone_bricks" : (x + y + z) % 7 == 0 ? "polished_diorite" : "calcite");
                    } else if (y == top || y == top + height) {
                        p.set(x, y, z, "polished_diorite");
                    } else {
                        p.clear(x, y, z);
                    }
                }
            }
        }
        // Tor zum Bergfried (zum Ziel hin offen)
        for (int y = top + 1; y <= top + 3; y++) {
            for (int dz = -1; dz <= 1; dz++) {
                p.clear(rad, y, dz);
            }
        }
        // Zinnenkranz auf Kragsteinen
        for (int a = 0; a < 360; a += 8) {
            int x = (int) Math.round(Math.cos(Math.toRadians(a)) * (rad + 1));
            int z = (int) Math.round(Math.sin(Math.toRadians(a)) * (rad + 1));
            p.set(x, top + height, z, "smooth_quartz");
            p.set(x, top + height - 1, z, "quartz_stairs[facing=" + face(x, z) + ",half=top,shape=straight]");
            if ((a / 8) % 2 == 0) {
                p.set(x, top + height + 1, z, "polished_diorite");
            }
        }
        // Kegeldach (blau) mit Spitze
        int roofBase = top + height + 2;
        for (int layer = 0; layer <= 9; layer++) {
            double rr = rad + 0.5 - layer * 0.5;
            for (int x = -rad; x <= rad; x++) {
                for (int z = -rad; z <= rad; z++) {
                    double d = Math.hypot(x, z);
                    if (d <= rr + 0.3 && (d > rr - 1.1 || layer == 0)) {
                        p.set(x, roofBase + layer, z, layer % 3 == 2 ? "light_blue_concrete" : "blue_concrete");
                    }
                }
            }
        }
        p.fill(0, roofBase + 10, 0, 0, roofBase + 12, 0, "spruce_fence");
        p.banner(0, roofBase + 13, 0, Decor.bannerBlock(2, false, "4"), Decor.bannerPatterns(2));
        // Fahnen an allen vier Seiten
        String[][] sides = {{"east", String.valueOf(rad + 1), "0"}, {"west", String.valueOf(-rad - 1), "0"},
            {"south", "0", String.valueOf(rad + 1)}, {"north", "0", String.valueOf(-rad - 1)}};
        for (String[] f : sides) {
            int x = Integer.parseInt(f[1]);
            int z = Integer.parseInt(f[2]);
            p.banner(x, top + height - 3, z, Decor.bannerBlock(2, true, f[0]), Decor.bannerPatterns(2));
            p.banner(x, top + 8, z, Decor.bannerBlock(2, true, f[0]), Decor.bannerPatterns(2));
        }
    }

    /** Blickrichtung nach außen (für Treppen und Wandbanner). */
    private static String face(int x, int z) {
        if (Math.abs(x) >= Math.abs(z)) {
            return x > 0 ? "west" : "east";
        }
        return z > 0 ? "north" : "south";
    }

    private static String outward(int x, int z) {
        if (Math.abs(x) >= Math.abs(z)) {
            return x > 0 ? "east" : "west";
        }
        return z > 0 ? "south" : "north";
    }

    /** Runder Eckturm: Mauer vom Hang bis über den Weg, Zinnen, blaues Spitzdach, weiß-blaue Fahne. */
    private static void tower(Plan p, Terrain t, int cx, int cz, int topY, int rad, int n) {
        int base = topY;
        for (int x = -rad; x <= rad; x++) {
            for (int z = -rad; z <= rad; z++) {
                if (Math.hypot(x, z) <= rad + 0.5) {
                    base = Math.min(base, t.top(cx + x, cz + z) - 2);
                }
            }
        }
        for (int x = -rad - 1; x <= rad + 1; x++) {
            for (int z = -rad - 1; z <= rad + 1; z++) {
                double d = Math.hypot(x, z);
                if (d <= rad + 0.5) {
                    for (int y = base; y <= topY; y++) {
                        boolean window = d > rad - 0.6 && (topY - y) % 5 == 3 && (x == 0 || z == 0);
                        p.set(cx + x, y, cz + z, window ? "light_blue_stained_glass_pane"
                                : Math.floorMod(y, 6) == 0 ? "stone_bricks" : "calcite");
                    }
                }
                if (d > rad + 0.5 && d <= rad + 1.5) {
                    p.set(cx + x, topY, cz + z, "smooth_quartz");
                    if (Math.floorMod(x + z, 2) == 0) {
                        p.set(cx + x, topY + 1, cz + z, "polished_diorite");
                    }
                }
            }
        }
        for (int layer = 0; layer <= 6; layer++) {
            double rr = rad + 1 - layer * 0.5;
            for (int x = -rad - 1; x <= rad + 1; x++) {
                for (int z = -rad - 1; z <= rad + 1; z++) {
                    if (Math.hypot(x, z) <= rr + 0.2) {
                        p.set(cx + x, topY + 1 + layer, cz + z, layer % 3 == 1 ? "light_blue_concrete" : "blue_concrete");
                    }
                }
            }
        }
        p.fill(cx, topY + 8, cz, cx, topY + 9, cz, "spruce_fence");
        p.banner(cx, topY + 10, cz, Decor.bannerBlock(2, false, String.valueOf((n * 5) % 16)), Decor.bannerPatterns(2));
        String dir = outward(cx, cz);
        int ox = "east".equals(dir) ? rad + 1 : "west".equals(dir) ? -rad - 1 : 0;
        int oz = "south".equals(dir) ? rad + 1 : "north".equals(dir) ? -rad - 1 : 0;
        p.banner(cx + ox, topY - 2, cz + oz, Decor.bannerBlock(2, true, dir), Decor.bannerPatterns(2));
        p.set(cx + ox, topY - 4, cz + oz, "lantern[hanging=false]");
    }

    /** Torhaus über dem Weg: zwei Türme, Bogen mit Fallgitter, Fahnen. */
    private static void gatehouse(Plan p, Terrain t, BoardLayout.Sample a, BoardLayout.Sample b) {
        double tx = b.x() - a.x();
        double tz = b.z() - a.z();
        double len = Math.hypot(tx, tz);
        double nx = -tz / len;
        double nz = tx / len;
        int y = (int) Math.round(a.y());
        for (int side : new int[] {-1, 1}) {
            int cx = (int) Math.round(a.x() + nx * 4.2 * side);
            int cz = (int) Math.round(a.z() + nz * 4.2 * side);
            int base = y;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    base = Math.min(base, t.top(cx + dx, cz + dz) - 2);
                }
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int yy = base; yy <= y + 8; yy++) {
                        p.set(cx + dx, yy, cz + dz, Math.floorMod(yy, 4) == 0 ? "stone_bricks" : "calcite");
                    }
                    if ((dx + dz) % 2 == 0) {
                        p.set(cx + dx, y + 9, cz + dz, "polished_diorite");
                    }
                }
            }
            Decor.towerRoof(p, cx, y + 10, cz, 1, "dark_prismarine_stairs", "dark_prismarine", 2);
        }
        // Bogen über dem Weg (Durchgang 4 Blöcke hoch)
        for (double k = -3.4; k <= 3.4; k += 0.25) {
            int x = (int) Math.round(a.x() + nx * k);
            int z = (int) Math.round(a.z() + nz * k);
            p.set(x, y + 5, z, "stone_bricks");
            p.set(x, y + 6, z, "calcite");
            p.set(x, y + 7, z, Math.abs(k) % 1.0 < 0.5 ? "polished_diorite" : "smooth_quartz");
            if (Math.abs(k) < 2.6) {
                p.set(x, y + 4, z, "iron_bars");
            }
        }
        String dir = Math.abs(tx) >= Math.abs(tz) ? (tx > 0 ? "west" : "east") : (tz > 0 ? "north" : "south");
        int bx = (int) Math.round(a.x() - tx / len * 1.0);
        int bz = (int) Math.round(a.z() - tz / len * 1.0);
        p.banner(bx, y + 6, bz, Decor.bannerBlock(2, true, dir), Decor.bannerPatterns(2));
    }

    /** Rosenbogen über dem Gartenweg. */
    private static void arch(Plan p, BoardLayout.Sample a, BoardLayout.Sample b) {
        double tx = b.x() - a.x();
        double tz = b.z() - a.z();
        double len = Math.hypot(tx, tz);
        double nx = -tz / len;
        double nz = tx / len;
        int y = (int) Math.round(a.y());
        for (int side : new int[] {-1, 1}) {
            int x = (int) Math.round(a.x() + nx * 3.1 * side);
            int z = (int) Math.round(a.z() + nz * 3.1 * side);
            p.fill(x, y + 1, z, x, y + 3, z, "birch_fence");
            p.set(x, y + 4, z, "flowering_azalea_leaves[persistent=true]");
        }
        for (double k = -2.8; k <= 2.8; k += 0.3) {
            int x = (int) Math.round(a.x() + nx * k);
            int z = (int) Math.round(a.z() + nz * k);
            p.set(x, y + 5, z, Math.abs(k) < 1.5 ? "flowering_azalea_leaves[persistent=true]"
                    : "azalea_leaves[persistent=true]");
        }
    }

    /** Brunnen im Schlossgarten. */
    private static void fountain(Plan p, Terrain t, BoardLayout layout, double rr, double angle) {
        int[] c = polar(rr, angle);
        int floor = (int) Math.round(layout.terrainHeight(rr));
        foundation(p, t, c[0] - 3, c[1] - 3, c[0] + 3, c[1] + 3, floor, "stone_bricks");
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                int ring = Math.max(Math.abs(x), Math.abs(z));
                p.set(c[0] + x, floor, c[1] + z, ring == 3 ? "polished_diorite" : "gravel");
                if (ring == 2) {
                    p.set(c[0] + x, floor + 1, c[1] + z, "quartz_slab[type=bottom]");
                } else if (ring < 2) {
                    p.set(c[0] + x, floor + 1, c[1] + z, "water");
                }
            }
        }
        p.fill(c[0], floor + 1, c[1], c[0], floor + 3, c[1], "quartz_pillar[axis=y]");
        p.set(c[0], floor + 4, c[1], "sea_lantern");
        for (int[] d : new int[][] {{-3, -3}, {3, 3}, {-3, 3}, {3, -3}}) {
            Decor.lanternPost(p, c[0] + d[0], floor + 1, c[1] + d[1], "birch_fence");
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
