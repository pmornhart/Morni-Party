package net.morni.party.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.morni.party.board.BoardLayout;
import net.morni.party.board.Field;
import net.morni.party.board.FieldType;

/**
 * Baupläne der drei neuen Bretter: Königssee (Stege über einem Bergsee, Fähren, Kapelle mit roten Zwiebeltürmen auf der
 * Insel, Bootshaus, Ruderboote, Felswände), Wiesn (Festwiese mit Bierzelten, Riesenrad, Kettenkarussell, Achterbahn,
 * Buden, Maibaum, Lichtern) und Wintersonnwend (verschneiter Berg mit Eisflächen, Lawinenverbau, Iglu, Skihütte und
 * dem großen Sonnwendfeuer oben). Gelände und Felder kommen aus {@link BoardPlans}.
 */
final class BoardPlans2 {

    /** Königssee: Wasserspiegel (oberster Wasserblock). */
    static final int WATER = -1;

    private BoardPlans2() {
    }

    // ---------- Königssee ----------

    /** Gelände am See abseits des Wegs: Insel in der Mitte, Seegrund, steile Felswände außen. */
    static int lakeTop(int x, int z, double d, BoardLayout.Shape s, double trail) {
        double noise = Math.sin(x * 0.31 + z * 0.17) * 0.6 + Math.cos((x - z) * 0.23) * 0.7;
        if (d < s.inner() + 4) {
            return 1 + (int) Math.round(2 * (1 - d / (s.inner() + 4)) + noise * 0.4);
        }
        if (d < s.outer() - 2) {
            return -5 + (int) Math.round(noise);
        }
        return (int) Math.round((d - (s.outer() - 2)) * 2.6 + noise * 2);
    }

    static String lakeBlock(Random r, int y, int top, double dist, int x, int z, BoardPlans.Terrain t) {
        boolean surface = y == top;
        double d = Math.hypot(x, z);
        BoardLayout.Shape s = t.layout.shape();
        if (dist <= 2.3) {
            // Steg: Bretter oben, darunter Pfähle im Wasser
            if (surface) {
                return d < s.inner() + 3 ? "dirt_path" : "spruce_planks";
            }
            boolean post = Math.floorMod(x + 2 * z, 4) == 0 && dist > 1.4;
            if (y > WATER) {
                return post ? "spruce_log[axis=y]" : "air";
            }
            if (y > -5) {
                return post ? "spruce_log[axis=y]" : "water";
            }
            return Decor.pick(r, "gravel", "sand", "clay");
        }
        if (d < s.inner() + 4) {
            // Insel
            return surface ? (r.nextInt(10) == 0 ? "moss_block" : "grass_block[snowy=false]") : top - y <= 2 ? "dirt"
                    : Decor.rock(r, top - y);
        }
        if (top < WATER) {
            return surface ? Decor.pick(r, "gravel", "sand", "clay", "gravel", "mud") : Decor.rock(r, top - y + 2);
        }
        // Ufer und Felswände
        if (surface) {
            return top > 18 ? (r.nextInt(3) == 0 ? "stone" : "snow_block") : top > 8 ? Decor.pick(r, "stone", "andesite",
                    "stone", "mossy_cobblestone") : top <= 0 ? "sand" : r.nextInt(5) == 0 ? "moss_block"
                    : "grass_block[snowy=false]";
        }
        return Decor.rock(r, top - y);
    }

    /** Je Säule am See: Wasser über dem Seegrund, Seerosen, Seegras, Schilf, Fichten am Ufer. */
    static void lakeColumn(Plan p, Random r, int x, int top, int z, double dist, BoardPlans.Terrain t) {
        if (dist <= 2.3) {
            return;
        }
        if (top < WATER) {
            for (int y = top + 1; y <= WATER; y++) {
                p.set(x, y, z, "water");
            }
            if (dist > 4 && r.nextInt(38) == 0) {
                p.setIfEmpty(x, WATER + 1, z, "lily_pad");
            } else if (top < WATER - 2 && r.nextInt(9) == 0) {
                p.set(x, top + 1, z, "seagrass");
            }
            return;
        }
        double d = Math.hypot(x, z);
        if (top >= 0 && top <= 1 && d > t.layout.shape().inner() + 4 && r.nextInt(5) == 0) {
            p.setIfEmpty(x, top + 1, z, "sugar_cane");
            p.setIfEmpty(x, top + 2, z, "sugar_cane");
        } else if (top > 1 && top < 14 && dist > 4 && d > 11 && r.nextInt(26) == 0) {
            Decor.spruce(p, x, top + 1, z, 6 + r.nextInt(5), r);
        } else if (top >= 1 && top < 8 && r.nextInt(12) == 0) {
            p.setIfEmpty(x, top + 1, z, Decor.pick(r, "short_grass", "fern", "lily_of_the_valley", "blue_orchid"));
        }
    }

    static void lake(Plan p, BoardLayout layout, BoardPlans.Terrain t, Random r) {
        BoardLayout.Shape s = layout.shape();
        // Kapelle auf der Insel (weiße Mauern, rote Zwiebeltürme)
        chapel(p, t, 0, 0);
        // Fähren an den Fähr-Stegen: Boot an der Seeseite des Felds
        for (Field f : layout.fields()) {
            if (f.type() != FieldType.FAEHRE) {
                continue;
            }
            double d = Math.hypot(f.x(), f.z());
            double ux = f.x() / d;
            double uz = f.z() / d;
            int bx = (int) Math.round(f.x() - ux * 5);
            int bz = (int) Math.round(f.z() - uz * 5);
            boat(p, bx, WATER + 1, bz, Math.abs(ux) > Math.abs(uz), true);
            Field to = layout.field(layout.ferryTarget(f.index()));
            // Bojen zeigen die Route
            for (int k = 1; k < 6; k++) {
                double a = k / 6.0;
                int ox = (int) Math.round(f.x() + (to.x() - f.x()) * a);
                int oz = (int) Math.round(f.z() + (to.z() - f.z()) * a);
                if (t.trail(ox, oz) > 3.5 && t.top(ox, oz) < WATER) {
                    p.set(ox, WATER + 1, oz, k % 2 == 0 ? "red_wool" : "white_wool");
                }
            }
        }
        // Bootshaus am Ufer beim Start und Ruderboote
        int[] c = BoardPlans.polar(s.outer() - 1, s.angle() - 0.35);
        boathouse(p, c[0], c[1]);
        for (int k = 0; k < 5; k++) {
            int[] b = BoardPlans.polar(s.outer() - 6 - r.nextInt(18), s.angle() + 0.6 + k * 1.1);
            if (t.trail(b[0], b[1]) > 5 && t.top(b[0], b[1]) < WATER - 1) {
                boat(p, b[0], WATER + 1, b[1], r.nextBoolean(), false);
            }
        }
        // Steile Berge rund um den See
        for (int k = 0; k < 7; k++) {
            double a = 2 * Math.PI * k / 7 + 0.3;
            int dist = 74 + r.nextInt(12);
            Decor.mountain(p, (int) (Math.cos(a) * dist), (int) (Math.sin(a) * dist), -8, 24 + r.nextInt(8), 58 + r.nextInt(22), r);
        }
    }

    /** Kapelle: Langhaus mit Satteldach, zwei Zwiebeltürme, Apsis rund. */
    private static void chapel(Plan p, BoardPlans.Terrain t, int cx, int cz) {
        int floor = t.top(cx, cz);
        for (int x = -3; x <= 3; x++) {
            for (int z = -4; z <= 4; z++) {
                if (t.trail(cx + x, cz + z) <= 2.6) {
                    continue;
                }
                boolean wall = Math.abs(x) == 3 || Math.abs(z) == 4;
                p.set(cx + x, floor, cz + z, "polished_andesite");
                for (int y = 1; y <= 5; y++) {
                    p.set(cx + x, floor + y, cz + z, wall ? (y == 3 && Math.abs(x) == 3 && z % 2 == 0 ? "light_blue_stained_glass"
                            : "calcite") : "air");
                }
            }
        }
        for (int layer = 0; layer <= 3; layer++) {
            for (int z = -5; z <= 5; z++) {
                p.set(cx - 4 + layer, floor + 6 + layer, cz + z, "red_nether_brick_stairs[facing=east,half=bottom,shape=straight]");
                p.set(cx + 4 - layer, floor + 6 + layer, cz + z, "red_nether_brick_stairs[facing=west,half=bottom,shape=straight]");
            }
        }
        for (int z = -5; z <= 5; z++) {
            p.set(cx, floor + 9, cz + z, "red_nether_brick_slab[type=bottom]");
        }
        onionTower(p, cx - 4, floor, cz - 5);
        onionTower(p, cx + 4, floor, cz - 5);
        p.set(cx, floor + 1, cz + 4, "air");
        p.set(cx, floor + 2, cz + 4, "air");
        p.set(cx, floor + 1, cz + 5, "spruce_stairs[facing=north,half=bottom,shape=straight]");
    }

    /** Weißer Turm mit roter Zwiebelhaube und goldener Spitze. */
    static void onionTower(Plan p, int cx, int floor, int cz) {
        for (int y = 0; y <= 10; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    boolean window = y == 7 && (dx == 0 || dz == 0) && !(dx == 0 && dz == 0);
                    p.set(cx + dx, floor + y, cz + dz, window && (dx == 0) != (dz == 0) ? "light_blue_stained_glass_pane"
                            : "calcite");
                }
            }
        }
        // Gesims, dann die Zwiebel: schmal, bauchig, zur Spitze hin schlank
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= 3) {
                    p.set(cx + dx, floor + 10, cz + dz, "smooth_quartz_slab[type=bottom]");
                }
            }
        }
        double[] radius = {1.5, 2.1, 2.4, 2.2, 1.6, 0.9, 0.4};
        for (int i = 0; i < radius.length; i++) {
            int y = floor + 11 + i;
            int e = (int) Math.ceil(radius[i]);
            for (int dx = -e; dx <= e; dx++) {
                for (int dz = -e; dz <= e; dz++) {
                    if (Math.hypot(dx, dz) <= radius[i] + 0.2) {
                        p.set(cx + dx, y, cz + dz, "red_terracotta");
                    }
                }
            }
        }
        p.set(cx, floor + 18, cz, "gold_block");
        p.set(cx, floor + 19, cz, "end_rod[facing=up]");
        p.set(cx, floor + 20, cz, "end_rod[facing=up]");
    }

    /** Boot auf dem Wasser: Rumpf aus Holz, bei der Fähre mit weißem Aufbau und Laterne. */
    private static void boat(Plan p, int cx, int y, int cz, boolean alongX, boolean ferry) {
        int len = ferry ? 4 : 2;
        int wid = ferry ? 1 : 0;
        for (int a = -len; a <= len; a++) {
            for (int b = -wid - 1; b <= wid + 1; b++) {
                int x = alongX ? cx + a : cx + b;
                int z = alongX ? cz + b : cz + a;
                boolean edge = Math.abs(b) == wid + 1 || Math.abs(a) == len;
                p.set(x, y - 1, z, "spruce_planks");
                if (edge) {
                    p.set(x, y, z, "spruce_slab[type=bottom]");
                }
            }
        }
        if (ferry) {
            for (int a = -1; a <= 1; a++) {
                for (int b = -wid; b <= wid; b++) {
                    int x = alongX ? cx + a : cx + b;
                    int z = alongX ? cz + b : cz + a;
                    p.set(x, y, z, "white_concrete");
                    p.set(x, y + 1, z, b == 0 ? "glass_pane" : "white_concrete");
                    p.set(x, y + 2, z, "red_concrete");
                }
            }
            p.set(cx, y + 3, cz, "lantern[hanging=false]");
        }
    }

    /** Bootshaus auf Pfählen mit dunklem Dach. */
    private static void boathouse(Plan p, int cx, int cz) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                boolean wall = Math.abs(x) == 3 || Math.abs(z) == 3;
                p.set(cx + x, WATER + 1, cz + z, "spruce_planks");
                if (Math.abs(x) == 3 && Math.abs(z) == 3) {
                    for (int y = WATER - 3; y <= WATER; y++) {
                        p.set(cx + x, y, cz + z, "spruce_log[axis=y]");
                    }
                }
                for (int y = 2; y <= 4; y++) {
                    p.set(cx + x, WATER + y, cz + z, wall ? (y == 3 && (x == 0 || z == 0) ? "glass_pane" : "dark_oak_planks")
                            : "air");
                }
            }
        }
        for (int layer = 0; layer <= 4; layer++) {
            for (int x = -4; x <= 4; x++) {
                int y = WATER + 5 + layer;
                if (layer < 4) {
                    p.set(cx + x, y, cz - 4 + layer, "dark_oak_stairs[facing=south,half=bottom,shape=straight]");
                    p.set(cx + x, y, cz + 4 - layer, "dark_oak_stairs[facing=north,half=bottom,shape=straight]");
                } else {
                    p.set(cx + x, y, cz, "dark_oak_slab[type=bottom]");
                }
            }
        }
        p.set(cx, WATER + 2, cz + 3, "air");
        p.set(cx, WATER + 3, cz + 3, "air");
        p.set(cx + 2, WATER + 2, cz - 2, "barrel[facing=up]");
        p.set(cx - 2, WATER + 2, cz - 2, "lantern[hanging=false]");
    }

    // ---------- Wiesn ----------

    /** Freie Stelle auf dem Brett: mindestens {@code clear} Blöcke vom Weg und von schon belegten Stellen weg. */
    private static int[] spot(BoardPlans.Terrain t, Random r, double clear, double minR, double maxR, List<int[]> used) {
        for (int tries = 0; tries < 400; tries++) {
            double a = r.nextDouble() * 2 * Math.PI;
            double d = minR + r.nextDouble() * (maxR - minR);
            int x = (int) Math.round(Math.cos(a) * d);
            int z = (int) Math.round(Math.sin(a) * d);
            if (Math.abs(x) + clear > t.size || Math.abs(z) + clear > t.size) {
                continue;
            }
            boolean ok = true;
            for (int dx = (int) -clear; dx <= clear && ok; dx += 2) {
                for (int dz = (int) -clear; dz <= clear && ok; dz += 2) {
                    if (t.trail(x + dx, z + dz) < 2.8 || !t.inside(x + dx, z + dz)) {
                        ok = false;
                    }
                }
            }
            for (int[] u : used) {
                if (Math.hypot(u[0] - x, u[1] - z) < clear + u[2] + 2) {
                    ok = false;
                }
            }
            if (ok) {
                used.add(new int[] {x, z, (int) Math.ceil(clear)});
                return new int[] {x, z};
            }
        }
        return null;
    }

    static void wiesn(Plan p, BoardLayout layout, BoardPlans.Terrain t, Random r) {
        BoardLayout.Shape s = layout.shape();
        List<int[]> used = new ArrayList<>();
        // Riesenrad in der Mitte, hinter dem Ziel
        ferrisWheel(p, t.top(0, 0), 0, 0);
        used.add(new int[] {0, 0, 7});
        // Zwei große Bierzelte, Kettenkarussell, Achterbahn, Buden, Maibaum
        for (int k = 0; k < 2; k++) {
            int[] c = spot(t, r, 9, 18, s.outer() - 4, used);
            if (c != null) {
                beerTent(p, t, c[0], c[1], k);
            }
        }
        int[] carousel = spot(t, r, 6, 14, s.outer() - 2, used);
        if (carousel != null) {
            chainCarousel(p, t.top(carousel[0], carousel[1]), carousel[0], carousel[1]);
        }
        int[] coaster = spot(t, r, 9, 16, s.outer() + 2, used);
        if (coaster != null) {
            coaster(p, t, coaster[0], coaster[1]);
        }
        String[] awnings = {"red", "white", "blue", "yellow"};
        for (int k = 0; k < 6; k++) {
            int[] c = spot(t, r, 3, 12, s.outer() + 3, used);
            if (c != null) {
                stall(p, t.top(c[0], c[1]), c[0], c[1], awnings[k % awnings.length], k);
            }
        }
        int[] pole = spot(t, r, 2, 12, s.outer(), used);
        if (pole != null) {
            maypole(p, t.top(pole[0], pole[1]), pole[0], pole[1]);
        }
        // Lichter entlang des Wegs (außen)
        int n = 0;
        for (BoardLayout.Sample sm : layout.samples()) {
            if (n++ % 110 != 0) {
                continue;
            }
            double out = (sm.r() + 3.2) / sm.r();
            int lx = (int) Math.round(sm.x() * out);
            int lz = (int) Math.round(sm.z() * out);
            if (Math.abs(lx) <= t.size && Math.abs(lz) <= t.size && t.trail(lx, lz) > 2.6) {
                int ly = t.top(lx, lz);
                for (int y = 1; y <= 3; y++) {
                    p.set(lx, ly + y, lz, "oak_fence");
                }
                p.set(lx, ly + 4, lz, n % 220 == 1 ? "sea_lantern" : "lantern[hanging=false]");
            }
        }
        // Kastanien am Rand und Berge in der Ferne
        for (int k = 0; k < 22; k++) {
            int[] c = spot(t, r, 2, s.outer() + 1, s.outer() + 6, used);
            if (c != null) {
                Decor.broadTree(p, c[0], t.top(c[0], c[1]) + 1, c[1], "oak_log", "oak_leaves", 4,
                        3, r);
            }
        }
        for (int k = 0; k < 8; k++) {
            double a = 2 * Math.PI * k / 8 + 0.7;
            int dist = 76 + r.nextInt(10);
            Decor.mountain(p, (int) (Math.cos(a) * dist), (int) (Math.sin(a) * dist), -14, 22 + r.nextInt(8), 44 + r.nextInt(20), r);
        }
    }

    /** Riesenrad: doppelter Ring mit weißen Speichen, Lichtern, acht bunten Gondeln und zwei A-Stützen. */
    private static void ferrisWheel(Plan p, int ground, int cx, int cz) {
        int radius = 10;
        int cy = ground + radius + 5;
        for (int side : new int[] {-1, 1}) {
            for (int a = 0; a < 720; a++) {
                double rad = Math.toRadians(a / 2.0);
                for (double rr : new double[] {radius - 0.4, radius + 0.4}) {
                    p.set(cx + (int) Math.round(Math.cos(rad) * rr), cy + (int) Math.round(Math.sin(rad) * rr), cz + side,
                            "white_concrete");
                }
            }
            for (int k = 0; k < 16; k++) {
                double rad = Math.toRadians(k * 22.5);
                for (double i = 1; i < radius; i += 0.5) {
                    p.set(cx + (int) Math.round(Math.cos(rad) * i), cy + (int) Math.round(Math.sin(rad) * i), cz + side,
                            k % 2 == 0 ? "white_concrete" : "light_gray_concrete");
                }
            }
            // A-Stützen vom Boden zur Nabe
            for (int leg : new int[] {-1, 1}) {
                for (double t = 0; t <= 1; t += 0.02) {
                    int x = cx + (int) Math.round(leg * 7 * (1 - t));
                    int y = ground + 1 + (int) Math.round((cy - ground - 1) * t);
                    p.set(x, y, cz + 2 * side, "white_concrete");
                }
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                p.set(cx + dx, cy + dy, cz, "gold_block");
            }
        }
        for (int z = -2; z <= 2; z++) {
            p.set(cx, cy, cz + z, "gold_block");
        }
        String[] colours = {"red", "yellow", "lime", "light_blue", "magenta", "orange", "cyan", "pink"};
        for (int k = 0; k < 8; k++) {
            double rad = Math.toRadians(k * 45 + 22.5);
            int gx = cx + (int) Math.round(Math.cos(rad) * radius);
            int gy = cy + (int) Math.round(Math.sin(rad) * radius);
            p.set(gx, gy, cz, "iron_chain[axis=y]");
            p.set(gx, gy - 1, cz, "iron_chain[axis=y]");
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    p.set(gx + dx, gy - 2, cz + dz, colours[k] + "_concrete");
                    p.set(gx + dx, gy - 3, cz + dz, dx == 0 && dz == 0 ? "air" : Math.abs(dx) + Math.abs(dz) == 1
                            ? colours[k] + "_stained_glass" : colours[k] + "_concrete");
                    p.set(gx + dx, gy - 4, cz + dz, colours[k] + "_concrete");
                }
            }
        }
        // Lichter außen am Ring
        for (int a = 0; a < 360; a += 15) {
            double rad = Math.toRadians(a);
            p.set(cx + (int) Math.round(Math.cos(rad) * (radius + 1)), cy + (int) Math.round(Math.sin(rad) * (radius + 1)), cz,
                    a % 30 == 0 ? "sea_lantern" : "glowstone");
        }
    }

    /** Bierzelt: weiß-blau gestreiftes Dach, Bänke und Tische, Fässer, Musikbühne. */
    private static void beerTent(Plan p, BoardPlans.Terrain t, int cx, int cz, int k) {
        int hx = 8;
        int hz = 5;
        int floor = t.top(cx, cz);
        BoardPlans.foundation(p, t, cx - hx, cz - hz, cx + hx, cz + hz, floor, "oak_planks");
        for (int x = -hx; x <= hx; x++) {
            for (int z = -hz; z <= hz; z++) {
                p.set(cx + x, floor, cz + z, "spruce_planks");
                boolean wall = Math.abs(x) == hx || Math.abs(z) == hz;
                for (int y = 1; y <= 4; y++) {
                    p.set(cx + x, floor + y, cz + z, wall && !(z == hz && Math.abs(x) <= 1 && y <= 3)
                            ? (y == 4 ? "white_wool" : Math.abs(x) % 4 == 0 ? "stripped_oak_log[axis=y]" : "white_wool") : "air");
                }
            }
        }
        // Dach: Streifen quer
        for (int layer = 0; layer <= hz; layer++) {
            for (int x = -hx - 1; x <= hx + 1; x++) {
                String c = Math.floorMod(x, 2) == 0 ? "blue_wool" : "white_wool";
                int y = floor + 5 + layer;
                p.set(cx + x, y, cz - hz - 1 + layer, c);
                p.set(cx + x, y, cz + hz + 1 - layer, c);
                for (int z = -hz + layer; z <= hz - layer; z++) {
                    if (layer == hz) {
                        p.set(cx + x, y, cz + z, c);
                    }
                }
            }
        }
        for (int x = -hx + 2; x <= hx - 2; x += 3) {
            for (int z = -hz + 2; z <= hz - 2; z++) {
                p.set(cx + x, floor + 1, cz + z, z == 0 ? "spruce_slab[type=top]" : "oak_slab[type=bottom]");
            }
        }
        p.set(cx - hx + 1, floor + 1, cz - hz + 1, "barrel[facing=up]");
        p.set(cx - hx + 1, floor + 2, cz - hz + 1, "barrel[facing=up]");
        p.set(cx + hx - 1, floor + 1, cz - hz + 1, "barrel[facing=up]");
        p.set(cx, floor + 1, cz - hz + 1, "note_block");
        p.set(cx, floor + 4, cz, "lantern[hanging=true]");
        p.set(cx, floor + 5 + hz + 1, cz, k == 0 ? "light_blue_wool" : "blue_wool");
    }

    /** Kettenkarussell: Mast, bunter Schirm, Sitze an Ketten. */
    private static void chainCarousel(Plan p, int ground, int cx, int cz) {
        for (int y = 1; y <= 10; y++) {
            p.set(cx, ground + y, cz, y <= 9 ? "stripped_birch_log[axis=y]" : "gold_block");
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                double d = Math.hypot(dx, dz);
                if (d <= 4.3) {
                    p.set(cx + dx, ground + 9 + (d < 2 ? 1 : 0), cz + dz, Math.floorMod((int) Math.round(Math.atan2(dz, dx) * 3),
                            2) == 0 ? "red_wool" : "yellow_wool");
                }
            }
        }
        for (int k = 0; k < 8; k++) {
            double a = 2 * Math.PI * k / 8;
            int x = cx + (int) Math.round(Math.cos(a) * 4);
            int z = cz + (int) Math.round(Math.sin(a) * 4);
            for (int y = 5; y <= 8; y++) {
                p.set(x, ground + y, z, "iron_chain[axis=y]");
            }
            p.set(x, ground + 4, z, "oak_slab[type=top]");
        }
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                if (Math.hypot(dx, dz) <= 5.3) {
                    p.set(cx + dx, ground, cz + dz, Math.hypot(dx, dz) > 4.5 ? "white_concrete" : "smooth_stone");
                }
            }
        }
    }

    /** Achterbahn: Schienen auf Holzstützen, rauf und runter in einer Schleife. */
    private static void coaster(Plan p, BoardPlans.Terrain t, int cx, int cz) {
        int ground = t.top(cx, cz);
        for (int a = 0; a < 360; a += 4) {
            double rad = Math.toRadians(a);
            int x = cx + (int) Math.round(Math.cos(rad) * 8);
            int z = cz + (int) Math.round(Math.sin(rad) * 5);
            int h = 3 + (int) Math.round(4 + 4 * Math.sin(rad * 2));
            for (int y = 1; y < h; y++) {
                if (a % 12 == 0) {
                    p.set(x, ground + y, z, "oak_fence");
                }
            }
            p.set(x, ground + h, z, "oak_planks");
            p.set(x, ground + h + 1, z, "rail");
        }
        // kleiner Wagen
        p.set(cx + 8, ground + 8, cz, "red_concrete");
    }

    /** Bude mit gestreifter Markise; abwechselnd Lebkuchenherzen, Brezn, Lose. */
    private static void stall(Plan p, int ground, int cx, int cz, String colour, int k) {
        for (int x = -2; x <= 2; x++) {
            p.set(cx + x, ground + 1, cz, "spruce_slab[type=top]");
            for (int z = -2; z <= 0; z++) {
                p.set(cx + x, ground + 4, cz + z, Math.floorMod(x, 2) == 0 ? colour + "_wool" : "white_wool");
            }
        }
        for (int[] post : new int[][] {{-2, 0}, {2, 0}, {-2, -2}, {2, -2}}) {
            for (int y = 1; y <= 3; y++) {
                p.set(cx + post[0], ground + y, cz + post[1], "spruce_fence");
            }
        }
        String goods = k % 3 == 0 ? "red_terracotta" : k % 3 == 1 ? "orange_terracotta" : "barrel[facing=up]";
        p.set(cx - 1, ground + 2, cz, goods);
        p.set(cx + 1, ground + 2, cz, goods);
        p.set(cx, ground + 3, cz - 2, "lantern[hanging=true]");
    }

    /** Maibaum: weiß-blau gestreift, Kranz und Zunftzeichen. */
    private static void maypole(Plan p, int ground, int cx, int cz) {
        for (int y = 1; y <= 18; y++) {
            p.set(cx, ground + y, cz, Math.floorMod(y, 2) == 0 ? "white_concrete" : "light_blue_concrete");
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    p.set(cx + dx, ground + 15, cz + dz, "spruce_leaves[persistent=true]");
                }
            }
        }
        for (int y : new int[] {7, 11}) {
            p.set(cx + 1, ground + y, cz, "light_blue_wall_banner[facing=east]");
            p.set(cx - 1, ground + y, cz, "white_wall_banner[facing=west]");
        }
        p.set(cx, ground + 19, cz, "spruce_leaves[persistent=true]");
    }

    // ---------- Wintersonnwend ----------

    /** Verschneite Fichte: Stamm, Nadeln, Schnee obendrauf. */
    static void snowySpruce(Plan p, int x, int y, int z, int h, Random r) {
        Decor.spruce(p, x, y, z, h, r);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int yy = y + h + 2; yy >= y; yy--) {
                    var d = p.get(x + dx, yy, z + dz);
                    if (d != null && d.getMaterial().name().endsWith("LEAVES")) {
                        p.setIfEmpty(x + dx, yy + 1, z + dz, "snow[layers=1]");
                        break;
                    }
                }
            }
        }
    }

    static void winter(Plan p, BoardLayout layout, BoardPlans.Terrain t, Random r) {
        BoardLayout.Shape s = layout.shape();
        List<int[]> used = new ArrayList<>();
        // Sonnwendfeuer oben neben dem Ziel: Holzstoß aus Stämmen, Lagerfeuer, Glut
        int top = t.top(0, 0);
        bonfire(p, 0, top, 0, true);
        used.add(new int[] {0, 0, 5});
        // Lawinenverbau über den Lawinenfeldern
        for (Field f : layout.fields()) {
            if (f.type() != FieldType.LAWINE) {
                continue;
            }
            double d = Math.hypot(f.x(), f.z());
            double ux = f.x() / d;
            double uz = f.z() / d;
            for (int row = 0; row < 3; row++) {
                double rr = d - 5 - row * 3;
                for (int k = -4; k <= 4; k++) {
                    double a = Math.atan2(f.z(), f.x()) + k * 0.6 / Math.max(5, rr);
                    int x = (int) Math.round(Math.cos(a) * rr);
                    int z = (int) Math.round(Math.sin(a) * rr);
                    if (t.trail(x, z) > 2.6 && t.inside(x, z)) {
                        int y = t.top(x, z);
                        p.set(x, y + 1, z, "dark_oak_fence");
                        p.set(x, y + 2, z, "dark_oak_fence");
                    }
                }
            }
            // Warnschild am Feld
            int wx = (int) Math.round(f.x() + ux * 3);
            int wz = (int) Math.round(f.z() + uz * 3);
            if (t.trail(wx, wz) > 2.4) {
                int y = t.top(wx, wz);
                p.set(wx, y + 1, wz, "dark_oak_fence");
                p.set(wx, y + 2, wz, "yellow_concrete");
                p.set(wx, y + 3, wz, "black_concrete");
            }
        }
        // Skihütte und Iglu beim Start, gefrorener See mit Bänken
        int[] hut = spot(t, r, 5, s.outer() - 12, s.outer() + 2, used);
        if (hut != null) {
            skiHut(p, t, hut[0], hut[1]);
        }
        int[] igloo = spot(t, r, 4, 14, s.outer() + 2, used);
        if (igloo != null) {
            igloo(p, t.top(igloo[0], igloo[1]), igloo[0], igloo[1]);
        }
        int[] pond = spot(t, r, 5, 14, s.outer(), used);
        if (pond != null) {
            int py = t.top(pond[0], pond[1]);
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (dx * dx / 25.0 + dz * dz / 16.0 <= 1) {
                        p.set(pond[0] + dx, py, pond[1] + dz, "ice");
                        p.set(pond[0] + dx, py + 1, pond[1] + dz, "air");
                    }
                }
            }
        }
        // kleine Sonnwendfeuer auf Felsspitzen ringsum
        for (int k = 0; k < 4; k++) {
            int[] c = spot(t, r, 2, 18, s.outer() + 3, used);
            if (c != null) {
                bonfire(p, c[0], t.top(c[0], c[1]), c[1], false);
            }
        }
        for (int k = 0; k < 6; k++) {
            double a = 2 * Math.PI * k / 6 + 0.2;
            Decor.mountain(p, (int) (Math.cos(a) * 88), (int) (Math.sin(a) * 88), -10, 26, 66 + r.nextInt(20), r);
        }
    }

    /**
     * Sonnwendfeuer: kegelförmiger Holzstoß aus Stämmen (mit Lücken), innen Glut und Lagerfeuer, deren Rauch oben
     * herauszieht, außen ein Steinkreis.
     */
    private static void bonfire(Plan p, int cx, int ground, int cz, boolean big) {
        int rad = big ? 3 : 1;
        int height = big ? 8 : 3;
        for (int dx = -rad - 2; dx <= rad + 2; dx++) {
            for (int dz = -rad - 2; dz <= rad + 2; dz++) {
                double d = Math.hypot(dx, dz);
                if (d > rad + 0.9 && d <= rad + 1.8) {
                    p.set(cx + dx, ground + 1, cz + dz, (dx + dz) % 2 == 0 ? "cobblestone_wall" : "mossy_cobblestone_wall");
                }
            }
        }
        for (int y = 1; y <= height; y++) {
            double rr = rad * (1 - (y - 1.0) / height) + 0.3;
            int e = (int) Math.ceil(rr);
            for (int dx = -e; dx <= e; dx++) {
                for (int dz = -e; dz <= e; dz++) {
                    double d = Math.hypot(dx, dz);
                    boolean shell = d <= rr + 0.3 && d > rr - 0.8;
                    boolean gap = Math.floorMod(dx * 3 + dz * 5 + y, 4) == 0 && y < height - 1;
                    if (shell && !gap) {
                        p.set(cx + dx, ground + y, cz + dz, Math.floorMod(dx + dz + y, 3) == 0 ? "stripped_spruce_log[axis=y]"
                                : "spruce_log[axis=y]");
                    } else if (d < rr - 0.8) {
                        p.set(cx + dx, ground + y, cz + dz, y == 1 ? "campfire[lit=true,facing=north]" : y == 2 && big
                                ? "shroomlight" : "air");
                    }
                }
            }
        }
        p.set(cx, ground, cz, "magma_block");
        if (big) {
            for (int[] o : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                p.set(cx + o[0], ground, cz + o[1], "magma_block");
            }
        }
    }

    private static void skiHut(Plan p, BoardPlans.Terrain t, int cx, int cz) {
        int floor = t.top(cx, cz);
        BoardPlans.foundation(p, t, cx - 3, cz - 3, cx + 3, cz + 3, floor, "stone_bricks");
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                boolean wall = Math.abs(x) == 3 || Math.abs(z) == 3;
                p.set(cx + x, floor, cz + z, "spruce_planks");
                for (int y = 1; y <= 3; y++) {
                    p.set(cx + x, floor + y, cz + z, wall ? (y == 2 && (x == 0 || z == 0) ? "glass_pane" : "spruce_log[axis=y]")
                            : "air");
                }
            }
        }
        for (int layer = 0; layer <= 4; layer++) {
            for (int x = -4; x <= 4; x++) {
                int y = floor + 4 + layer;
                if (layer < 4) {
                    p.set(cx + x, y, cz - 4 + layer, "spruce_stairs[facing=south,half=bottom,shape=straight]");
                    p.set(cx + x, y, cz + 4 - layer, "spruce_stairs[facing=north,half=bottom,shape=straight]");
                    p.setIfEmpty(cx + x, y + 1, cz - 4 + layer, "snow[layers=2]");
                    p.setIfEmpty(cx + x, y + 1, cz + 4 - layer, "snow[layers=2]");
                } else {
                    p.set(cx + x, y, cz, "snow_block");
                }
            }
        }
        p.set(cx, floor + 1, cz + 3, "air");
        p.set(cx, floor + 2, cz + 3, "air");
        // Skier an der Wand (Holzfalltüren) und ein Fass
        p.set(cx + 4, floor + 1, cz + 1, "birch_trapdoor[facing=east,half=bottom,open=true]");
        p.set(cx + 4, floor + 1, cz + 2, "birch_trapdoor[facing=east,half=bottom,open=true]");
        p.set(cx + 2, floor + 1, cz - 2, "barrel[facing=up]");
        p.set(cx - 2, floor + 1, cz - 2, "lantern[hanging=false]");
    }

    private static void igloo(Plan p, int ground, int cx, int cz) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int y = 0; y <= 3; y++) {
                    double d = Math.sqrt(dx * dx + dz * dz + (y * 1.2) * (y * 1.2));
                    if (d <= 3.3 && d > 2.3) {
                        p.set(cx + dx, ground + 1 + y, cz + dz, "snow_block");
                    } else if (d <= 2.3) {
                        p.set(cx + dx, ground + 1 + y, cz + dz, "air");
                    }
                }
            }
        }
        p.set(cx, ground + 1, cz + 3, "air");
        p.set(cx, ground + 2, cz + 3, "air");
        p.set(cx, ground + 1, cz - 2, "lantern[hanging=false]");
        p.set(cx, ground, cz, "white_wool");
    }
}
