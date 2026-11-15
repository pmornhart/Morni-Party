package de.moinmorni.party.world;

import de.moinmorni.party.board.FieldType;
import java.util.Random;

/**
 * Der Festplatz – Wartebereich von Morni Party: eine schwebende Wiese mit Maibaum in der Mitte, einem Ring aus bunten
 * Spielfeldern (zeigt, welche Farbe was bedeutet), Biergarten-Tischen, Siegertreppchen und Fichten am Rand.
 */
public final class HubPlan {

    public static final int RADIUS = 24;
    /** Ring der Beispiel-Felder. */
    public static final int RING = 11;
    /** Siegertreppchen (Mitte, Platz 1). */
    public static final int[] PODIUM = {0, 2, -17};

    private HubPlan() {
    }

    public static Plan build() {
        Plan p = new Plan();
        Random r = new Random(2026);
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                double d = Math.hypot(x, z);
                if (d > RADIUS + 0.5) {
                    continue;
                }
                String top = d < 4 ? "moss_block" : d < 8.5 ? (r.nextInt(4) == 0 ? "gravel" : "polished_andesite")
                        : r.nextInt(10) == 0 ? "moss_block" : "grass_block[snowy=false]";
                p.set(x, 0, z, top);
                p.set(x, -1, z, "dirt");
                if (d > 8.5 && d < RADIUS - 1 && r.nextInt(16) == 0) {
                    p.setIfEmpty(x, 1, z, Decor.pick(r, "short_grass", "dandelion", "poppy", "cornflower",
                            "oxeye_daisy", "azure_bluet"));
                }
            }
        }
        Decor.island(p, RADIUS - 2, 14, r, "dirt", 2);
        maypole(p);
        fieldRing(p);
        beerGarden(p, r);
        podium(p);
        // Rand: Fichten und unsichtbare Wand
        for (int a = 0; a < 360; a += 24) {
            int x = (int) Math.round(Math.cos(Math.toRadians(a + 7)) * (RADIUS - 3));
            int z = (int) Math.round(Math.sin(Math.toRadians(a + 7)) * (RADIUS - 3));
            if (z > 10 && Math.abs(x) < 8) {
                continue; // Blick vom Ankunftsplatz frei lassen
            }
            Decor.spruce(p, x, 1, z, 6 + r.nextInt(4), r);
        }
        for (int a = 0; a < 360; a += 2) {
            int x = (int) Math.round(Math.cos(Math.toRadians(a)) * (RADIUS + 1));
            int z = (int) Math.round(Math.sin(Math.toRadians(a)) * (RADIUS + 1));
            for (int h = 0; h <= 4; h++) {
                p.setIfEmpty(x, h, z, "barrier");
            }
        }
        for (int a = 45; a < 360; a += 90) {
            Decor.lanternPost(p, (int) Math.round(Math.cos(Math.toRadians(a)) * 7.5), 1,
                    (int) Math.round(Math.sin(Math.toRadians(a)) * 7.5), "spruce_fence");
        }
        p.connect();
        return p;
    }

    /** Maibaum: weiß-blau gestreift, Kranz, Spitze aus Fichtengrün. */
    private static void maypole(Plan p) {
        int height = 18;
        for (int y = 1; y <= height; y++) {
            p.set(0, y, 0, Math.floorMod(y, 4) < 2 ? "white_concrete" : "light_blue_concrete");
        }
        p.set(0, 0, 0, "stone_bricks");
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    p.set(dx, 1, dz, "stone_brick_slab[type=bottom]");
                }
            }
        }
        // Kranz
        for (int a = 0; a < 360; a += 20) {
            int x = (int) Math.round(Math.cos(Math.toRadians(a)) * 2);
            int z = (int) Math.round(Math.sin(Math.toRadians(a)) * 2);
            p.setIfEmpty(x, height - 4, z, "spruce_leaves[persistent=true]");
        }
        // Schilder-Arme mit Bannern
        for (int[] d : new int[][] {{1, 0, 4}, {-1, 0, 12}, {0, 1, 8}, {0, -1, 0}}) {
            for (int i = 1; i <= 2; i++) {
                p.set(d[0] * i, height - 7, d[1] * i, "spruce_fence");
            }
            p.banner(d[0] * 3, height - 8, d[1] * 3, Decor.bannerBlock(d[2] / 4, false, String.valueOf(d[2])),
                    Decor.bannerPatterns(d[2] / 4));
            p.set(d[0] * 3, height - 7, d[1] * 3, "spruce_fence");
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                p.set(dx, height + 1, dz, "spruce_leaves[persistent=true]");
            }
        }
        p.set(0, height + 2, 0, "spruce_leaves[persistent=true]");
    }

    /** Ring aus Beispiel-Feldern in allen Farben. */
    private static void fieldRing(Plan p) {
        FieldType[] types = FieldType.values();
        int n = 16;
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            int cx = (int) Math.round(Math.cos(a) * RING);
            int cz = (int) Math.round(Math.sin(a) * RING);
            if (cz > 8 && Math.abs(cx) < 4) {
                continue; // Weg zum Ankunftsplatz
            }
            FieldType t = types[i % types.length];
            BoardPlans.pad(p, cx, 0, cz, t, "polished_andesite");
        }
    }

    /** Biergarten: Tische mit Bänken links und rechts vom Ankunftsplatz. */
    private static void beerGarden(Plan p, Random r) {
        for (int side : new int[] {-1, 1}) {
            for (int row = 0; row < 2; row++) {
                int cx = side * (8 + row * 6);
                int cz = 15;
                for (int dz = -2; dz <= 2; dz++) {
                    p.set(cx, 1, cz + dz, "spruce_fence");
                    p.set(cx, 2, cz + dz, "spruce_slab[type=bottom]");
                    p.set(cx - 2, 1, cz + dz, "spruce_stairs[facing=east,half=bottom,shape=straight]");
                    p.set(cx + 2, 1, cz + dz, "spruce_stairs[facing=west,half=bottom,shape=straight]");
                }
                p.set(cx, 3, cz, "lantern[hanging=false]");
                if (r.nextBoolean()) {
                    p.set(cx, 3, cz + 2, "flower_pot");
                }
            }
        }
    }

    /** Siegertreppchen im Norden. */
    private static void podium(Plan p) {
        int x = PODIUM[0];
        int z = PODIUM[2];
        p.fill(x - 1, 1, z - 1, x + 1, 2, z + 1, "gold_block");
        p.fill(x - 4, 1, z - 1, x - 2, 1, z + 1, "iron_block");
        p.fill(x + 2, 1, z - 1, x + 4, 1, z + 1, "waxed_copper_block");
        for (int dx = -5; dx <= 5; dx++) {
            p.set(dx, 1, z - 2, "spruce_planks");
            p.set(dx, 2, z - 2, "spruce_planks");
            p.set(dx, 3, z - 2, "spruce_planks");
            p.set(dx, 4, z - 2, Math.floorMod(dx, 2) == 0 ? "white_wool" : "light_blue_wool");
        }
    }
}
