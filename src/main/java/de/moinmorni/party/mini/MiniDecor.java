package de.moinmorni.party.mini;

import de.moinmorni.party.world.Decor;
import de.moinmorni.party.world.Plan;
import java.util.Random;

/**
 * Kulissen der Minispiel-Plätze: alles nur rund um die Spielfläche, nichts darüber oder darunter, wo jemand
 * hinfallen oder getroffen werden könnte. Die Spielfläche selbst legt jedes Minispiel vor jeder Runde neu
 * ({@link MiniType#plan()}); die Kulisse entsteht nur beim Bau.
 */
final class MiniDecor {

    private static final String GRASS = "grass_block[snowy=false]";

    private MiniDecor() {
    }

    static void build(MiniType type, Plan p) {
        Random r = new Random(500 + type.ordinal());
        switch (type) {
            case BROECKELBODEN -> glacier(p, r);
            case TALERREGEN -> market(p, r);
            case BERGSPRINT -> trail(p, r);
            case PFEILHAGEL -> castleYard(p, r);
            case HEISSE_BREZN -> bakery(p, r);
            case TRACHTENFARBEN -> dancefloor(p, r);
            case GIPFELKOENIG -> summit(p, r);
            case KUHGLOCKE -> pasture(p, r);
            case SCHAEFCHEN -> farm(p, r);
            case HOLZHACKER -> sawmill(p, r);
            case REMPELEI -> gorge(p, r);
            case SCHEIBENSCHIESSEN -> fair(p, r);
        }
    }

    // ---------- Bausteine ----------

    /** Ring aus Boden (oben {@code top}) mit Fels darunter, der nach unten spitz zuläuft. */
    private static void ring(Plan p, double in, double out, int y, String top, Random r) {
        int e = (int) Math.ceil(out) + 1;
        for (int x = -e; x <= e; x++) {
            for (int z = -e; z <= e; z++) {
                double d = Math.hypot(x, z);
                if (d < in || d > out + r.nextDouble() * 0.8) {
                    continue;
                }
                p.setIfEmpty(x, y, z, top);
                int depth = 2 + (int) Math.round(6 * Math.sin(Math.PI * Math.min(1, (d - in) / Math.max(1, out - in))))
                        + r.nextInt(2);
                for (int k = 1; k <= depth; k++) {
                    p.setIfEmpty(x, y - k, z, k == 1 && top.startsWith("grass") ? "dirt" : Decor.rock(r, k + 3));
                }
            }
        }
    }

    /** Rechteckige Fläche (Boden) mit Fels darunter. */
    private static void pad(Plan p, int x1, int z1, int x2, int z2, int y, String top, Random r) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                p.setIfEmpty(x, y, z, top);
                int depth = 2 + r.nextInt(3);
                for (int k = 1; k <= depth; k++) {
                    p.setIfEmpty(x, y - k, z, k == 1 ? "dirt" : Decor.rock(r, k + 3));
                }
            }
        }
    }

    /** Blumen und Gras auf freien Grasblöcken einer Fläche. */
    private static void flowers(Plan p, int x1, int z1, int x2, int z2, int y, int chance, Random r) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                var d = p.get(x, y, z);
                if (d != null && d.getMaterial().name().equals("GRASS_BLOCK") && p.get(x, y + 1, z) == null
                        && r.nextInt(100) < chance) {
                    p.set(x, y + 1, z, Decor.pick(r, "short_grass", "short_grass", "dandelion", "poppy", "cornflower",
                            "oxeye_daisy", "azure_bluet", "allium"));
                }
            }
        }
    }

    /** Marktstand: Theke, vier Pfosten, gestreiftes Dach. */
    private static void stall(Plan p, int cx, int y, int cz, boolean alongX, String colour, String goods) {
        int a = 2;
        for (int i = -a; i <= a; i++) {
            int x = alongX ? cx + i : cx;
            int z = alongX ? cz : cz + i;
            p.set(x, y + 1, z, Math.abs(i) == a ? "barrel[facing=up]" : "spruce_planks");
            p.set(x, y + 2, z, Math.abs(i) == a ? goods : (i % 2 == 0 ? goods : "air"));
        }
        for (int i : new int[] {-a, a}) {
            for (int j : new int[] {-1, 1}) {
                int x = alongX ? cx + i : cx + j;
                int z = alongX ? cz + j : cz + i;
                p.fill(x, y + 1, z, x, y + 3, z, "spruce_fence");
            }
        }
        for (int i = -a - 1; i <= a + 1; i++) {
            for (int j = -2; j <= 2; j++) {
                int x = alongX ? cx + i : cx + j;
                int z = alongX ? cz + j : cz + i;
                p.set(x, y + 4, z, Math.floorMod(i, 2) == 0 ? colour + "_wool" : "white_wool");
            }
        }
    }

    /** Wimpelkette zwischen zwei Masten (bunte Wolle). */
    private static void bunting(Plan p, int x1, int z1, int x2, int z2, int y, String... colours) {
        p.fill(x1, y - 4, z1, x1, y, z1, "spruce_fence");
        p.fill(x2, y - 4, z2, x2, y, z2, "spruce_fence");
        int n = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
        for (int i = 1; i < n; i++) {
            int x = x1 + Math.round((float) (x2 - x1) * i / n);
            int z = z1 + Math.round((float) (z2 - z1) * i / n);
            int sag = (int) Math.round(Math.sin(Math.PI * i / n) * 1.2);
            p.set(x, y - sag, z, colours[i % colours.length] + "_wool");
        }
    }

    /** Fahne an einem Mast (weiß-blau). */
    private static void flag(Plan p, int x, int y, int z, int height, int rotation) {
        p.fill(x, y, z, x, y + height - 1, z, "spruce_fence");
        p.banner(x, y + height, z, Decor.bannerBlock(2, false, String.valueOf(rotation)), Decor.bannerPatterns(2));
    }

    /** Holzhütte mit Satteldach. */
    private static void hut(Plan p, int cx, int y, int cz, int hx, int hz, String wall, String roof, String roofSlab) {
        for (int x = -hx; x <= hx; x++) {
            for (int z = -hz; z <= hz; z++) {
                boolean edge = Math.abs(x) == hx || Math.abs(z) == hz;
                p.set(cx + x, y, cz + z, "spruce_planks");
                for (int h = 1; h <= 3; h++) {
                    if (edge) {
                        boolean corner = Math.abs(x) == hx && Math.abs(z) == hz;
                        p.set(cx + x, y + h, cz + z, corner ? "stripped_spruce_log[axis=y]"
                                : h == 2 && (x == 0 || z == 0) ? "glass_pane" : wall);
                    } else {
                        p.clear(cx + x, y + h, cz + z);
                    }
                }
            }
        }
        for (int layer = 0; layer <= hz; layer++) {
            for (int x = -hx - 1; x <= hx + 1; x++) {
                int yy = y + 4 + layer;
                if (layer < hz) {
                    p.set(cx + x, yy, cz - hz - 1 + layer, roof + "[facing=south,half=bottom,shape=straight]");
                    p.set(cx + x, yy, cz + hz + 1 - layer, roof + "[facing=north,half=bottom,shape=straight]");
                    if (Math.abs(x) == hx) {
                        for (int z = -hz + layer; z <= hz - layer; z++) {
                            p.set(cx + x, yy, cz + z, wall);
                        }
                    }
                } else {
                    p.set(cx + x, yy, cz, roofSlab + "[type=bottom]");
                }
            }
        }
    }

    // ---------- Die zwölf Kulissen ----------

    /** Bröckelboden: Gletscher – Schneeplateau rundherum, Eiszacken, verschneite Gipfel dahinter. */
    private static void glacier(Plan p, Random r) {
        ring(p, 14, 26, 0, "snow_block", r);
        for (int a = 0; a < 360; a += 14) {
            double rr = 16 + r.nextInt(8);
            int x = (int) Math.round(Math.cos(Math.toRadians(a)) * rr);
            int z = (int) Math.round(Math.sin(Math.toRadians(a)) * rr);
            if (z < -10 && Math.abs(x) < 6) {
                continue; // Blick der Zuschauer frei lassen
            }
            int h = 2 + r.nextInt(6);
            String ice = r.nextBoolean() ? "packed_ice" : "blue_ice";
            p.fill(x, 1, z, x, h, z, ice);
            if (h > 4) {
                p.set(x + 1, 1, z, ice);
                p.set(x, 1, z + 1, ice);
            }
        }
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(30 + k * 24 + r.nextInt(10));
            int x = (int) Math.round(Math.cos(a) * 21);
            int z = (int) Math.round(Math.sin(a) * 21);
            Decor.spruce(p, x, 1, z, 6 + r.nextInt(3), r);
            p.set(x, 0, z, "snow_block");
        }
        Decor.mountain(p, 0, 42, -14, 16, 46, r);
        Decor.mountain(p, -36, 22, -14, 13, 36, r);
        Decor.mountain(p, 36, 24, -14, 13, 38, r);
        Decor.mountain(p, -28, 44, -14, 10, 28, r);
        Decor.mountain(p, 30, 44, -14, 11, 30, r);
        flag(p, -14, 1, -14, 4, 2);
        flag(p, 14, 1, -14, 4, 14);
    }

    /** Talerregen: Marktplatz – Pflaster, Marktstände, Schatzhaus mit Goldschmuck, Brunnen. */
    private static void market(Plan p, Random r) {
        ring(p, 12, 26, 0, "stone_bricks", r);
        for (int x = -26; x <= 26; x++) {
            for (int z = -26; z <= 26; z++) {
                double d = Math.hypot(x, z);
                if (d >= 12 && d <= 25 && Math.floorMod(x * 7 + z * 3, 11) == 0) {
                    p.set(x, 0, z, Decor.pick(r, "cobblestone", "andesite", "mossy_stone_bricks"));
                }
            }
        }
        stall(p, -17, 0, 0, false, "red", "pumpkin");
        stall(p, 17, 0, 0, false, "blue", "melon");
        stall(p, -9, 0, 17, true, "yellow", "hay_block[axis=y]");
        // Schatzhaus
        int cz = 19;
        for (int x = 6; x <= 14; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) {
                boolean edge = x == 6 || x == 14 || z == cz - 3 || z == cz + 3;
                for (int h = 1; h <= 5; h++) {
                    if (edge) {
                        p.set(x, h, z, h == 5 ? "gold_block" : (x + z + h) % 5 == 0 ? "chiseled_stone_bricks" : "stone_bricks");
                    }
                }
                p.set(x, 6, z, "smooth_stone_slab[type=bottom]");
            }
        }
        p.fill(9, 1, cz - 3, 11, 3, cz - 3, "air");
        p.set(10, 1, cz - 1, "chest[facing=north]");
        p.set(9, 1, cz, "gold_block");
        p.set(11, 1, cz, "raw_gold_block");
        p.set(10, 4, cz - 4, "lantern[hanging=true]");
        p.set(10, 5, cz - 4, "stone_brick_slab[type=top]");
        flag(p, 6, 7, cz - 3, 3, 0);
        flag(p, 14, 7, cz - 3, 3, 0);
        for (int[] c : new int[][] {{-20, -12}, {20, -12}, {-20, 14}, {21, 12}}) {
            Decor.lanternPost(p, c[0], 1, c[1], "spruce_fence");
        }
        for (int[] c : new int[][] {{-15, -15}, {15, -16}, {-22, 6}, {22, -4}}) {
            p.set(c[0], 1, c[1], "barrel[facing=up]");
            p.set(c[0] + 1, 1, c[1], "chest[facing=north]");
        }
    }

    /** Bergsprint: Almweg zwischen Felsen, Hütte am Start, Gipfelkreuz und Zuschauer am Ziel. */
    private static void trail(Plan p, Random r) {
        for (int z = BergSprint.START_Z - 6; z <= BergSprint.GOAL_Z + 10; z++) {
            int h = BergSprint.height(Math.max(BergSprint.START_Z, Math.min(BergSprint.GOAL_Z, z)));
            for (int side : new int[] {-1, 1}) {
                for (int k = BergSprint.HALF_WIDTH + 6; k <= BergSprint.HALF_WIDTH + 18; k++) {
                    int x = side * k;
                    double far = (k - BergSprint.HALF_WIDTH - 6) / 12.0;
                    int top = h + (int) Math.round(far * (4 + 3 * Math.sin(z * 0.2 + side))) ;
                    for (int y = top - 4; y <= top; y++) {
                        p.setIfEmpty(x, y, z, y == top ? (top > h + 4 ? "stone" : GRASS) : Decor.rock(r, top - y + 1));
                    }
                    if (y0(top, h) && r.nextInt(30) == 0) {
                        Decor.spruce(p, x, top + 1, z, 5 + r.nextInt(4), r);
                    } else if (r.nextInt(12) == 0 && top <= h + 4) {
                        p.setIfEmpty(x, top + 1, z, Decor.pick(r, "short_grass", "dandelion", "cornflower", "oxeye_daisy"));
                    }
                }
            }
        }
        // Hütte am Start
        hut(p, 0, 0, BergSprint.START_Z - 11, 4, 3, "spruce_planks", "dark_oak_stairs", "dark_oak_slab");
        pad(p, -6, BergSprint.START_Z - 16, 6, BergSprint.START_Z - 5, 0, GRASS, r);
        // Gipfel hinter dem Ziel: Fels, Kreuz, Fahnen
        int gh = BergSprint.height(BergSprint.GOAL_Z);
        Decor.mountain(p, 0, BergSprint.GOAL_Z + 22, gh - 10, 14, 30, r);
        for (int side : new int[] {-1, 1}) {
            int x = side * (BergSprint.HALF_WIDTH + 3);
            p.fill(x, gh - 3, BergSprint.GOAL_Z - 2, x, gh, BergSprint.GOAL_Z - 2, "cobblestone");
            flag(p, x, gh + 1, BergSprint.GOAL_Z - 2, 5, side < 0 ? 4 : 12);
        }
    }

    private static boolean y0(int top, int h) {
        return top <= h + 5;
    }

    /** Pfeilhagel: Burghof – Mauern mit Zinnen und Schießscharten, Ecktürme, Banner. */
    private static void castleYard(Plan p, Random r) {
        int w = 13;
        pad(p, -w, -w, w, w, 0, "cobblestone", r);
        for (int x = -w; x <= w; x++) {
            for (int z = -w; z <= w; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) >= 9 && r.nextInt(4) == 0) {
                    p.set(x, 0, z, Decor.pick(r, "gravel", "mossy_cobblestone", "stone"));
                }
            }
        }
        for (int i = -w; i <= w; i++) {
            for (int[] c : new int[][] {{i, -w}, {i, w}, {-w, i}, {w, i}}) {
                boolean front = c[1] == -w && Math.abs(c[0]) <= 3;
                int top = front ? 4 : 8;
                for (int y = 1; y <= top; y++) {
                    boolean slit = !front && y == 5 && Math.floorMod(i, 4) == 0;
                    p.set(c[0], y, c[1], slit ? "air" : r.nextInt(6) == 0 ? "mossy_stone_bricks" : "stone_bricks");
                }
                if (Math.floorMod(i, 2) == 0) {
                    p.set(c[0], top + 1, c[1], "stone_bricks");
                }
            }
        }
        // Tor in der Vordermauer
        for (int x = -2; x <= 2; x++) {
            for (int y = 1; y <= 3; y++) {
                p.clear(x, y, -w);
            }
            p.set(x, 4, -w, "iron_bars");
        }
        for (int sx : new int[] {-w, w}) {
            for (int sz : new int[] {-w, w}) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        p.fill(sx + dx, 1, sz + dz, sx + dx, 11, sz + dz, "stone_bricks");
                    }
                }
                Decor.towerRoof(p, sx, 12, sz, 2, "dark_oak_stairs", "dark_oak_planks", 2);
            }
        }
        for (int i = -8; i <= 8; i += 4) {
            p.banner(i, 6, w - 1, Decor.bannerBlock(2, true, "north"), Decor.bannerPatterns(2));
            p.banner(-w + 1, 6, i, Decor.bannerBlock(0, true, "east"), Decor.bannerPatterns(0));
            p.banner(w - 1, 6, i, Decor.bannerBlock(0, true, "west"), Decor.bannerPatterns(0));
        }
        // Zielscheiben und Strohpuppen an der Mauer
        for (int x = -8; x <= 8; x += 8) {
            p.set(x, 1, w - 1, "hay_block[axis=y]");
            p.set(x, 2, w - 1, "carved_pumpkin[facing=north]");
        }
        Decor.island(p, w, 10, r, null, 0);
    }

    /** Heiße Brezn: Bäckerei mit Ofen und Kamin, Brezn-Stand, Biertische. */
    private static void bakery(Plan p, Random r) {
        ring(p, 9, 22, 0, GRASS, r);
        pad(p, -10, 11, 10, 20, 0, "cobblestone", r);
        // Backstube
        int cz = 17;
        for (int x = -7; x <= 7; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) {
                boolean edge = Math.abs(x) == 7 || z == cz - 3 || z == cz + 3;
                for (int h = 1; h <= 4; h++) {
                    if (edge) {
                        boolean frame = Math.abs(x) == 7 || Math.floorMod(x, 4) == 3 || h == 4;
                        p.set(x, h, z, frame ? "dark_oak_log[axis=y]" : h == 2 ? "glass_pane" : "white_terracotta");
                    } else {
                        p.clear(x, h, z);
                    }
                }
            }
        }
        for (int layer = 0; layer <= 4; layer++) {
            for (int x = -8; x <= 8; x++) {
                int y = 5 + layer;
                if (layer < 4) {
                    p.set(x, y, cz - 4 + layer, "red_nether_brick_stairs[facing=south,half=bottom,shape=straight]");
                    p.set(x, y, cz + 4 - layer, "red_nether_brick_stairs[facing=north,half=bottom,shape=straight]");
                    if (Math.abs(x) == 7) {
                        for (int z = cz - 3 + layer; z <= cz + 3 - layer; z++) {
                            p.set(x, y, z, "white_terracotta");
                        }
                    }
                } else {
                    p.set(x, y, cz, "red_nether_brick_slab[type=bottom]");
                }
            }
        }
        // Backofen mit Kamin (raucht)
        p.fill(3, 1, cz, 6, 3, cz + 2, "bricks");
        p.set(4, 1, cz - 1, "furnace[facing=north,lit=true]");
        p.set(5, 1, cz - 1, "blast_furnace[facing=north,lit=true]");
        p.fill(5, 4, cz + 1, 5, 11, cz + 1, "bricks");
        p.set(5, 12, cz + 1, "campfire[lit=true,signal_fire=true]");
        // Theke mit Brezn (Heuballen = Teig, Kuchen) unter einer Markise
        for (int x = -5; x <= 2; x++) {
            p.set(x, 1, cz - 4, "spruce_planks");
            p.set(x, 2, cz - 4, x % 2 == 0 ? "cake" : "hay_block[axis=x]");
            p.set(x, 4, cz - 5, Math.floorMod(x, 2) == 0 ? "light_blue_wool" : "white_wool");
            p.set(x, 4, cz - 6, Math.floorMod(x, 2) == 0 ? "light_blue_wool" : "white_wool");
        }
        p.fill(-6, 1, cz - 6, -6, 3, cz - 6, "spruce_fence");
        p.fill(3, 1, cz - 6, 3, 3, cz - 6, "spruce_fence");
        p.banner(-1, 3, cz - 5, Decor.bannerBlock(1, true, "north"), Decor.bannerPatterns(1));
        // Biertische links und rechts
        for (int side : new int[] {-1, 1}) {
            int x = side * 15;
            for (int z = -4; z <= 4; z++) {
                p.set(x, 1, z, "spruce_fence");
                p.set(x, 2, z, "spruce_slab[type=bottom]");
                p.set(x - 2, 1, z, "spruce_stairs[facing=east,half=bottom,shape=straight]");
                p.set(x + 2, 1, z, "spruce_stairs[facing=west,half=bottom,shape=straight]");
            }
            p.set(x, 3, 0, "lantern[hanging=false]");
            Decor.broadTree(p, side * 18, 1, -10, "oak_log", "oak_leaves", 4, 3, r);
        }
        flowers(p, -22, -22, 22, 10, 0, 10, r);
    }

    /** Trachtenfarben: Tanzplatz beim Trachtenfest – Wiese, Maibaum, Musikbühne, Wimpelketten. */
    private static void dancefloor(Plan p, Random r) {
        ring(p, 14, 26, 0, GRASS, r);
        // Maibaum hinter dem Tanzboden
        int mz = 19;
        for (int y = 1; y <= 22; y++) {
            p.set(0, y, mz, Math.floorMod(y, 4) < 2 ? "white_concrete" : "light_blue_concrete");
        }
        for (int a = 0; a < 360; a += 20) {
            int x = (int) Math.round(Math.cos(Math.toRadians(a)) * 2);
            int z = (int) Math.round(Math.sin(Math.toRadians(a)) * 2);
            p.setIfEmpty(x, 17, mz + z, "spruce_leaves[persistent=true]");
        }
        p.fill(-1, 23, mz - 1, 1, 23, mz + 1, "spruce_leaves[persistent=true]");
        p.set(0, 24, mz, "spruce_leaves[persistent=true]");
        // Bänder vom Maibaum zu den Masten
        String[] colours = {"red", "yellow", "lime", "light_blue", "blue", "orange", "pink", "white"};
        bunting(p, -14, 14, 14, 14, 6, colours);
        bunting(p, -16, -10, -16, 12, 6, colours);
        bunting(p, 16, -10, 16, 12, 6, colours);
        // Musikbühne rechts
        for (int x = 18; x <= 23; x++) {
            for (int z = -5; z <= 5; z++) {
                p.set(x, 1, z, "spruce_planks");
                if (x == 23) {
                    p.fill(x, 2, z, x, 5, z, "spruce_planks");
                }
                p.set(x, 6, z, Math.floorMod(z, 2) == 0 ? "light_blue_wool" : "white_wool");
            }
        }
        p.fill(18, 2, -5, 18, 5, -5, "spruce_log[axis=y]");
        p.fill(18, 2, 5, 18, 5, 5, "spruce_log[axis=y]");
        p.set(21, 2, -2, "note_block");
        p.set(21, 2, 0, "jukebox");
        p.set(21, 2, 2, "note_block");
        // Bänke links
        for (int z = -6; z <= 6; z += 3) {
            p.set(-19, 1, z, "spruce_stairs[facing=east,half=bottom,shape=straight]");
            p.set(-19, 1, z + 1, "spruce_stairs[facing=east,half=bottom,shape=straight]");
        }
        flowers(p, -26, -26, 26, 26, 0, 14, r);
    }

    /** Gipfelkönig: verschneiter Gipfel über den Wolken – Berg darunter, Nachbargipfel, Gipfelkreuz. */
    private static void summit(Plan p, Random r) {
        // Der Berg, auf dem die Spielfläche steht
        for (int x = -30; x <= 30; x++) {
            for (int z = -30; z <= 30; z++) {
                double d = Math.hypot(x, z);
                if (d < 12.6 || d > 30) {
                    continue;
                }
                int top = (int) Math.round(-2 - (d - 12.6) * 1.15 + Math.sin(x * 0.4) * Math.cos(z * 0.3) * 1.5);
                for (int y = top - 5; y <= top; y++) {
                    p.setIfEmpty(x, y, z, y == top ? (d < 19 ? "snow_block" : r.nextInt(3) == 0 ? "snow_block" : "stone")
                            : Decor.rock(r, top - y + 4));
                }
            }
        }
        // Gipfelkreuz neben der Spielfläche
        p.fill(-1, -7, 14, 1, -1, 16, "cobblestone");
        p.fill(0, 0, 15, 0, 7, 15, "spruce_log[axis=y]");
        p.fill(-2, 5, 15, 2, 5, 15, "spruce_log[axis=x]");
        // Wolken unter dem Gipfel
        for (int k = 0; k < 9; k++) {
            double a = Math.toRadians(k * 40 + r.nextInt(20));
            int cx = (int) Math.round(Math.cos(a) * (36 + r.nextInt(8)));
            int cz = (int) Math.round(Math.sin(a) * (36 + r.nextInt(8)));
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (dx * dx / 25.0 + dz * dz / 9.0 <= 1 && r.nextInt(5) > 0) {
                        p.setIfEmpty(cx + dx, -16 - r.nextInt(2), cz + dz, "white_wool");
                    }
                }
            }
        }
        Decor.mountain(p, 2, 50, -30, 16, 46, r);
        Decor.mountain(p, -44, 18, -30, 13, 38, r);
        Decor.mountain(p, 44, 22, -30, 13, 40, r);
        flag(p, -9, 1, 9, 5, 2);
    }

    /** Kuhglocke: Almweide mit Zaun und Kühen, Stall, Milchkannen, Berge dahinter. */
    private static void pasture(Plan p, Random r) {
        ring(p, 9, 28, 0, GRASS, r);
        pad(p, -10, 8, 10, 12, 0, GRASS, r);
        // Weide mit Zaun rechts und links
        for (int side : new int[] {-1, 1}) {
            int x1 = side * 13;
            int x2 = side * 24;
            for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
                p.set(x, 1, -10, "spruce_fence");
                p.set(x, 1, 10, "spruce_fence");
            }
            for (int z = -10; z <= 10; z++) {
                p.set(x1, 1, z, z == 0 ? "spruce_fence_gate[facing=east,open=false]" : "spruce_fence");
                p.set(x2, 1, z, "spruce_fence");
            }
            for (int k = 0; k < 4; k++) {
                p.creature(side * (16 + r.nextInt(6)) + 0.5, 1, -7 + k * 4 + 0.5, "cow", r.nextInt(360));
            }
            p.set(side * 22, 1, -7, "hay_block[axis=y]");
            p.set(side * 22, 1, -6, "water_cauldron[level=3]");
        }
        // Stall hinter der Glocke
        hut(p, 0, 0, 18, 7, 4, "spruce_planks", "spruce_stairs", "spruce_slab");
        p.fill(-1, 1, 14, 1, 3, 14, "air");
        p.set(-3, 1, 13, "cauldron");
        p.set(3, 1, 13, "cauldron");
        p.set(-4, 1, 13, "hay_block[axis=y]");
        p.set(4, 1, 13, "hay_block[axis=y]");
        p.creature(0.5, 1, 16.5, "cow", 180);
        p.banner(-3, 3, 13, Decor.bannerBlock(2, true, "north"), Decor.bannerPatterns(2));
        Decor.mountain(p, -10, 48, -16, 16, 46, r);
        Decor.mountain(p, 24, 44, -16, 14, 38, r);
        Decor.mountain(p, -38, 30, -16, 12, 32, r);
        flowers(p, -28, -28, 28, 28, 0, 12, r);
        for (int[] c : new int[][] {{-26, -14}, {26, -14}, {-26, 12}, {27, 14}}) {
            Decor.spruce(p, c[0], 1, c[1], 7, r);
        }
    }

    /** Schäfchen zählen: Bauernhof – Steinmauer, Bauernhaus, Heuschober, Obstbäume (keine Schafe!). */
    private static void farm(Plan p, Random r) {
        ring(p, 9, 24, 0, GRASS, r);
        for (int a = 0; a < 360; a += 3) {
            int x = (int) Math.round(Math.cos(Math.toRadians(a)) * 20);
            int z = (int) Math.round(Math.sin(Math.toRadians(a)) * 20);
            if (z < -14 && Math.abs(x) < 5) {
                continue;
            }
            p.set(x, 1, z, "mossy_cobblestone_wall");
        }
        hut(p, 15, 0, 6, 3, 4, "white_terracotta", "dark_oak_stairs", "dark_oak_slab");
        p.set(15, 1, 2, "air");
        p.set(15, 2, 2, "air");
        for (int[] c : new int[][] {{-14, 8}, {-15, 10}, {-13, 10}}) {
            p.fill(c[0], 1, c[1], c[0], 2, c[1], "hay_block[axis=y]");
        }
        p.set(-14, 3, 10, "hay_block[axis=y]");
        for (int[] c : new int[][] {{-13, -6}, {-16, 1}, {12, -10}, {6, 15}}) {
            Decor.broadTree(p, c[0], 1, c[1], "oak_log", "oak_leaves", 3, 2, r);
        }
        // Leiterwagen
        p.fill(-8, 1, 14, -5, 1, 14, "spruce_slab[type=top]");
        p.set(-8, 1, 13, "spruce_trapdoor[facing=north,half=bottom,open=true]");
        p.set(-5, 1, 13, "spruce_trapdoor[facing=north,half=bottom,open=true]");
        Decor.mountain(p, 6, 42, -14, 14, 40, r);
        Decor.mountain(p, -30, 32, -14, 12, 30, r);
        flowers(p, -24, -24, 24, 24, 0, 10, r);
    }

    /** Holzhacker: Sägewerk mit Wasserrad, Holzstapel, dichter Fichtenwald (kein Eichenholz in Spielhöhe). */
    private static void sawmill(Plan p, Random r) {
        ring(p, 12, 30, 0, GRASS, r);
        // Sägewerk
        int cz = 22;
        for (int x = -6; x <= 6; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) {
                p.set(x, 0, z, "spruce_planks");
                if (z == cz + 3 || Math.abs(x) == 6) {
                    for (int h = 1; h <= 4; h++) {
                        p.set(x, h, z, Math.abs(x) == 6 && (z == cz - 3 || z == cz + 3) ? "stripped_spruce_log[axis=y]"
                                : "spruce_planks");
                    }
                }
                p.set(x, 5, z, "dark_oak_slab[type=bottom]");
            }
        }
        for (int x : new int[] {-6, 6}) {
            p.fill(x, 1, cz - 3, x, 4, cz - 3, "stripped_spruce_log[axis=y]");
        }
        p.set(0, 1, cz, "stonecutter[facing=north]");
        p.fill(-4, 1, cz, -1, 1, cz, "stripped_spruce_log[axis=x]");
        p.fill(1, 1, cz, 4, 1, cz, "spruce_slab[type=bottom]");
        p.set(3, 1, cz + 1, "grindstone[face=floor,facing=north]");
        // Wasserrad an der Seite
        for (int a = 0; a < 360; a += 15) {
            int y = 4 + (int) Math.round(Math.sin(Math.toRadians(a)) * 3.5);
            int z = cz + (int) Math.round(Math.cos(Math.toRadians(a)) * 3.5);
            p.set(8, y, z, a % 45 == 0 ? "stripped_spruce_wood[axis=x]" : "spruce_planks");
        }
        p.fill(7, 4, cz, 9, 4, cz, "spruce_log[axis=x]");
        // Holzstapel (Fichte und Birke) links und rechts
        for (int side : new int[] {-1, 1}) {
            int x0 = side * 19;
            for (int dz = -4; dz <= 4; dz++) {
                for (int y = 1; y <= 3; y++) {
                    for (int dx = 0; dx < 3 - (y - 1); dx++) {
                        p.set(x0 + side * dx, y, dz, (dz + y) % 3 == 0 ? "birch_log[axis=z]" : "spruce_log[axis=z]");
                    }
                }
            }
            p.set(x0 - side, 1, 6, "chipped_anvil[facing=north]");
        }
        // Hackklotz mit Axt-Halter
        p.set(-12, 1, -15, "stripped_spruce_log[axis=y]");
        p.set(-12, 2, -15, "spruce_trapdoor[facing=north,half=bottom,open=false]");
        // Wald rundherum
        for (int k = 0; k < 30; k++) {
            double a = Math.toRadians(r.nextInt(360));
            double rr = 18 + r.nextInt(11);
            int x = (int) Math.round(Math.cos(a) * rr);
            int z = (int) Math.round(Math.sin(a) * rr);
            if ((z < -12 && Math.abs(x) < 8) || (z > 12 && Math.abs(x) < 11) || (Math.abs(z) < 7 && Math.abs(x) < 24)) {
                continue;
            }
            Decor.spruce(p, x, 1, z, 7 + r.nextInt(5), r);
        }
    }

    /** Rempelei: Felsnadel über der Schlucht – Felswände, schwebende Felsen weit unten, Wimpel auf Masten. */
    private static void gorge(Plan p, Random r) {
        // Felsnadel unter der Plattform (erst unterhalb der Aus-Linie, damit niemand darauf landet)
        for (int y = -10; y >= -46; y--) {
            double rr = 2.5 + (-10 - y) * 0.12;
            for (int x = -9; x <= 9; x++) {
                for (int z = -9; z <= 9; z++) {
                    if (Math.hypot(x, z) <= rr) {
                        p.setIfEmpty(x, y, z, Decor.rock(r, -y));
                    }
                }
            }
        }
        // Schluchtwände links und rechts (weit genug weg)
        for (int side : new int[] {-1, 1}) {
            for (int z = -34; z <= 34; z++) {
                int top = 4 + (int) Math.round(Math.sin(z * 0.18 + side) * 4);
                for (int k = 24; k <= 32; k++) {
                    int x = side * k;
                    int t = top + (k - 24);
                    for (int y = -40; y <= t; y++) {
                        if (y == t || y > t - 3 || r.nextInt(3) > 0) {
                            p.setIfEmpty(x, y, z, y == t ? (t > 8 ? "stone" : GRASS) : Decor.rock(r, t - y));
                        }
                    }
                    if (y0(t, 8) && r.nextInt(25) == 0) {
                        Decor.spruce(p, x, t + 1, z, 6 + r.nextInt(3), r);
                    }
                }
            }
        }
        // Schwebende Felsen weit unten
        for (int k = 0; k < 7; k++) {
            double a = Math.toRadians(k * 51);
            int cx = (int) Math.round(Math.cos(a) * 15);
            int cz = (int) Math.round(Math.sin(a) * 15);
            int cy = -18 - r.nextInt(14);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = -2; dy <= 0; dy++) {
                        if (Math.abs(dx) + Math.abs(dz) - dy <= 3) {
                            p.setIfEmpty(cx + dx, cy + dy, cz + dz, dy == 0 ? "moss_block" : Decor.rock(r, 4));
                        }
                    }
                }
            }
        }
        for (int a = 0; a < 360; a += 60) {
            int x = (int) Math.round(Math.cos(Math.toRadians(a + 30)) * 21);
            int z = (int) Math.round(Math.sin(Math.toRadians(a + 30)) * 21);
            if (z < -15) {
                continue;
            }
            p.fill(x, -3, z, x, -1, z, "cobblestone");
            flag(p, x, 0, z, 6, (a / 22) % 16);
        }
    }

    /** Scheibenschießen: Schützenfest – Bierzelt, Riesenrad, Preisregal, Lampions. */
    private static void fair(Plan p, Random r) {
        pad(p, -24, -18, 24, 32, 0, GRASS, r);
        Decor.island(p, 24, 10, r, null, 0);
        // Preisregal hinter dem Schützenstand
        for (int x = -7; x <= 7; x++) {
            p.set(x, 1, -9, "spruce_planks");
            p.set(x, 2, -9, Decor.pick(r, "cake", "flower_pot", "potted_red_tulip", "candle_cake", "gold_block"));
            p.set(x, 3, -9, "spruce_slab[type=bottom]");
            p.set(x, 4, -9, Decor.pick(r, "potted_poppy", "potted_dandelion", "pink_candle[candles=3,lit=true]",
                    "potted_blue_orchid"));
        }
        // Bierzelt links
        for (int x = -22; x <= -13; x++) {
            for (int z = -6; z <= 10; z++) {
                boolean edge = x == -22 || x == -13 || z == -6 || z == 10;
                int roofY = 6 - (int) Math.round(Math.abs(x + 17.5) * 0.7);
                p.set(x, roofY, z, Math.floorMod(z, 2) == 0 ? "light_blue_wool" : "white_wool");
                if (edge && (z == -6 || z == 10) && Math.floorMod(x, 3) == 0) {
                    p.fill(x, 1, z, x, roofY - 1, z, "spruce_fence");
                }
            }
        }
        for (int z = -3; z <= 8; z += 3) {
            p.fill(-19, 1, z, -16, 1, z, "spruce_fence");
            p.fill(-19, 2, z, -16, 2, z, "spruce_slab[type=bottom]");
        }
        // Riesenrad hinter der Scheibenwand
        int wz = 26;
        int wy = 13;
        for (int a = 0; a < 360; a += 4) {
            int x = (int) Math.round(Math.cos(Math.toRadians(a)) * 10);
            int y = wy + (int) Math.round(Math.sin(Math.toRadians(a)) * 10);
            p.set(x, y, wz, "white_concrete");
        }
        for (int a = 0; a < 360; a += 45) {
            for (double k = 1; k < 10; k += 0.5) {
                int x = (int) Math.round(Math.cos(Math.toRadians(a)) * k);
                int y = wy + (int) Math.round(Math.sin(Math.toRadians(a)) * k);
                p.set(x, y, wz, "iron_bars");
            }
            int gx = (int) Math.round(Math.cos(Math.toRadians(a)) * 10);
            int gy = wy + (int) Math.round(Math.sin(Math.toRadians(a)) * 10);
            String[] col = {"red", "yellow", "lime", "light_blue", "orange", "pink", "blue", "magenta"};
            p.set(gx, gy - 1, wz + 1, col[(a / 45) % col.length] + "_concrete");
            p.set(gx, gy - 1, wz - 1, col[(a / 45) % col.length] + "_concrete");
        }
        p.set(0, wy, wz, "gold_block");
        for (int y = 1; y < wy; y++) {
            p.set(-3 + y / 4, y, wz, "white_concrete");
            p.set(3 - y / 4, y, wz, "white_concrete");
        }
        // Lampions an Masten rechts
        String[] colours = {"red", "white", "light_blue", "yellow"};
        bunting(p, 13, -6, 13, 14, 6, colours);
        bunting(p, 13, 14, 22, 14, 6, colours);
        stall(p, 19, 0, 2, false, "red", "cake");
        for (int[] c : new int[][] {{-12, -12}, {12, -12}, {-23, 14}, {23, -10}}) {
            Decor.lanternPost(p, c[0], 1, c[1], "spruce_fence");
        }
    }
}
