package net.morni.party.world;

import java.util.List;
import java.util.Random;
import org.bukkit.DyeColor;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;

/** Bausteine für Festplatz, Spielbretter und Minispiele: schwebende Inseln, Bäume, Laternen, Banner in unseren Farben. */
public final class Decor {

    private Decor() {
    }

    public static String pick(Random r, String... options) {
        return options[r.nextInt(options.length)];
    }

    /** Fels mit etwas Abwechslung; tiefer unten mehr Tiefenschiefer. */
    public static String rock(Random r, int depth) {
        int roll = r.nextInt(100);
        if (depth > 8) {
            return roll < 45 ? "deepslate" : roll < 70 ? "tuff" : roll < 85 ? "stone" : "cobbled_deepslate";
        }
        return roll < 60 ? "stone" : roll < 75 ? "andesite" : roll < 88 ? "tuff" : roll < 96 ? "cobblestone" : "gravel";
    }

    /**
     * Schwebende Insel unter einer runden Ecke-Fläche (alles schwebt im leeren Himmel): oben {@code soilDepth}
     * Blöcke Erde, darunter Fels, nach unten spitz zulaufend.
     */
    public static void island(Plan p, int radius, int depth, Random r, String soil, int soilDepth) {
        int edge = radius + 2;
        for (int x = -edge; x <= edge; x++) {
            for (int z = -edge; z <= edge; z++) {
                double rr = Math.pow(Math.pow(Math.abs(x), 4) + Math.pow(Math.abs(z), 4), 0.25);
                if (rr > edge + r.nextDouble() * 1.5 - 0.5) {
                    continue;
                }
                double f = Math.max(0, 1 - (rr / (edge + 1)) * (rr / (edge + 1)));
                int col = 2 + (int) Math.round(depth * Math.sqrt(f)) + r.nextInt(2);
                for (int d = 1; d <= col; d++) {
                    p.setIfEmpty(x, -d, z, d <= soilDepth && soil != null ? soil : rock(r, d));
                }
            }
        }
    }

    /** Unsichtbare Wand (und Decke) um die Spielfläche: niemand fällt hinaus, keine Perle fliegt darüber. */
    public static void barrierBox(Plan p, int half, int top, boolean ceiling) {
        int w = half + 1;
        for (int y = 1; y <= top; y++) {
            for (int i = -w; i <= w; i++) {
                p.setIfEmpty(i, y, -w, "barrier");
                p.setIfEmpty(i, y, w, "barrier");
                p.setIfEmpty(-w, y, i, "barrier");
                p.setIfEmpty(w, y, i, "barrier");
            }
        }
        if (ceiling) {
            for (int x = -w; x <= w; x++) {
                for (int z = -w; z <= w; z++) {
                    p.setIfEmpty(x, top + 1, z, "barrier");
                }
            }
        }
    }

    /** Fichte: Stamm und kegelförmige Krone. */
    public static void spruce(Plan p, int x, int y, int z, int height, Random r) {
        for (int i = 0; i < height; i++) {
            p.set(x, y + i, z, "spruce_log[axis=y]");
        }
        int top = y + height;
        p.set(x, top, z, "spruce_leaves[persistent=true]");
        p.set(x, top + 1, z, "spruce_leaves[persistent=true]");
        for (int layer = 0; layer < height - 1; layer++) {
            int ly = top - layer;
            int rad = 1 + layer / 2 - (layer % 2 == 0 ? 0 : 1);
            rad = Math.max(1, Math.min(3, rad));
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    if ((dx != 0 || dz != 0) && Math.abs(dx) + Math.abs(dz) <= rad + 1 && r.nextInt(10) > 0) {
                        p.setIfEmpty(x + dx, ly, z + dz, "spruce_leaves[persistent=true]");
                    }
                }
            }
        }
    }

    /** Laubbaum mit runder Krone (Kastanie, Linde …). */
    public static void broadTree(Plan p, int x, int y, int z, String log, String leaves, int trunk, int radius, Random r) {
        for (int i = 0; i < trunk; i++) {
            p.set(x, y + i, z, log + "[axis=y]");
        }
        int cy = y + trunk + radius / 2;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius + 1; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double d = Math.sqrt(dx * dx + dy * dy * 1.6 + dz * dz);
                    if (d <= radius + 0.3 - r.nextDouble() * 0.6) {
                        p.setIfEmpty(x + dx, cy + dy, z + dz, leaves + "[persistent=true]");
                    }
                }
            }
        }
        for (int i = trunk; i < trunk + radius / 2 + 1; i++) {
            p.set(x, y + i, z, log + "[axis=y]");
        }
    }

    /** Laternenpfahl: drei Zaunpfosten, oben eine Laterne. */
    public static void lanternPost(Plan p, int x, int y, int z, String fence) {
        for (int i = 0; i < 3; i++) {
            p.set(x, y + i, z, fence);
        }
        p.set(x, y + 3, z, "lantern[hanging=false]");
    }

    /** Hängende Laterne an einer Kette. */
    public static void hangingLantern(Plan p, int x, int y, int z, int chain) {
        for (int i = 0; i < chain; i++) {
            p.set(x, y - i, z, "iron_chain[axis=y]");
        }
        p.set(x, y - chain, z, "lantern[hanging=true]");
    }

    /** Kulissen-Berg mit Schneekappe (schwebt in der Ferne). */
    public static void mountain(Plan p, int cx, int cz, int y0, int radius, int height, Random r) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) / radius;
                if (d > 1) {
                    continue;
                }
                double ridge = 0.12 * Math.sin(dx * 0.7) * Math.cos(dz * 0.5);
                int top = (int) Math.round(height * Math.pow(1 - d, 1.25) + height * ridge * (1 - d));
                if (top < 1) {
                    continue;
                }
                int crust = 5 + r.nextInt(3);
                for (int y = Math.max(-6, top - crust); y <= top; y++) {
                    String block;
                    if (y >= top - 1 && top > height * 0.62) {
                        block = r.nextInt(6) == 0 ? "powder_snow" : "snow_block";
                    } else if (y >= top - 1 && top < height * 0.25) {
                        block = r.nextInt(3) == 0 ? "moss_block" : "grass_block[snowy=false]";
                    } else {
                        block = rock(r, top - y > 3 ? 10 : 0);
                    }
                    if (block.equals("powder_snow")) {
                        block = "snow_block";
                    }
                    p.setIfEmpty(cx + dx, y0 + y, cz + dz, block);
                }
            }
        }
    }

    /** Rautenmuster wie die weiß-blaue Fahne: schräg gestelltes Schachbrett. */
    public static boolean lozenge(int x, int z, int size) {
        return Math.floorMod(Math.floorDiv(x + z, size) + Math.floorDiv(x - z, size), 2) == 0;
    }

    /** Pyramidendach aus Treppen (für Türme), Spitze mit Fahnenmast und Banner. */
    public static void towerRoof(Plan p, int cx, int y, int cz, int half, String stairs, String planks, int design) {
        for (int layer = 0; layer <= half; layer++) {
            int h = half - layer;
            int ly = y + layer;
            for (int dx = -h; dx <= h; dx++) {
                for (int dz = -h; dz <= h; dz++) {
                    boolean edge = Math.abs(dx) == h || Math.abs(dz) == h;
                    if (!edge || h == 0) {
                        p.set(cx + dx, ly, cz + dz, planks);
                        continue;
                    }
                    String facing;
                    if (Math.abs(dx) == h && Math.abs(dz) == h) {
                        p.set(cx + dx, ly, cz + dz, planks);
                        continue;
                    } else if (dz == -h) {
                        facing = "south";
                    } else if (dz == h) {
                        facing = "north";
                    } else if (dx == -h) {
                        facing = "east";
                    } else {
                        facing = "west";
                    }
                    p.set(cx + dx, ly, cz + dz, stairs + "[facing=" + facing + ",half=bottom,shape=straight]");
                }
            }
        }
        int top = y + half + 1;
        p.set(cx, top, cz, "spruce_fence");
        p.set(cx, top + 1, cz, "spruce_fence");
        p.banner(cx, top + 2, cz, bannerBlock(design, false, "0"), bannerPatterns(design));
    }

    // ---------- Banner in unseren Farben (eigene Muster) ----------

    /** Smaragd: grün mit hellgrüner Raute und goldenem Rand. */
    public static List<Pattern> emerald() {
        return List.of(new Pattern(DyeColor.LIME, PatternType.RHOMBUS),
                new Pattern(DyeColor.GREEN, PatternType.CIRCLE),
                new Pattern(DyeColor.YELLOW, PatternType.BORDER));
    }

    /** Gold: gelb mit orangem Verlauf und Zackenrand. */
    public static List<Pattern> gold() {
        return List.of(new Pattern(DyeColor.ORANGE, PatternType.GRADIENT_UP),
                new Pattern(DyeColor.YELLOW, PatternType.STRIPE_MIDDLE),
                new Pattern(DyeColor.ORANGE, PatternType.CURLY_BORDER));
    }

    /** Weiß-blaue Rauten. */
    public static List<Pattern> lozenges() {
        return List.of(new Pattern(DyeColor.LIGHT_BLUE, PatternType.RHOMBUS),
                new Pattern(DyeColor.LIGHT_BLUE, PatternType.TRIANGLES_TOP),
                new Pattern(DyeColor.LIGHT_BLUE, PatternType.TRIANGLES_BOTTOM));
    }

    /** Grundfarbe des Banners zum Muster (Blockname). */
    public static String bannerBlock(int design, boolean wall, String facingOrRotation) {
        String colour = switch (Math.floorMod(design, 3)) {
            case 0 -> "green";
            case 1 -> "yellow";
            default -> "white";
        };
        return wall ? colour + "_wall_banner[facing=" + facingOrRotation + "]"
                : colour + "_banner[rotation=" + facingOrRotation + "]";
    }

    public static List<Pattern> bannerPatterns(int design) {
        return switch (Math.floorMod(design, 3)) {
            case 0 -> emerald();
            case 1 -> gold();
            default -> lozenges();
        };
    }
}
