package de.moinmorni.party.mini;

import de.moinmorni.party.world.Decor;
import de.moinmorni.party.world.Plan;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

/**
 * Maibaumkraxeln: Am weiß-blauen Maibaum hängen ringsum Leitern und Ranken. Hinauf zum Kranz! Immer wieder bricht ein
 * Stück weg (vorher staubt es) und wächst nach ein paar Sekunden nach – dann seitlich ausweichen. Wer zuerst oben
 * steht, gewinnt; danach zählt, wer am höchsten kam.
 */
final class MaibaumKraxeln extends MiniRun {

    /** Höhe des Stamms: oben (y = TOP + 1) steht man am Kranz. */
    static final int TOP = 24;
    static final int PLAZA = 9;
    private static final int GAP_LENGTH = 3;
    private static final int WARN = 15;
    private static final int GONE = 50;
    private static final int LOWEST_GAP = 4;

    /** Eine Kletterspur: x, z und der Block (Leiter oder Ranke). */
    record Lane(int x, int z, String block) {
    }

    static final List<Lane> LANES = lanes();

    private static List<Lane> lanes() {
        List<Lane> out = new ArrayList<>();
        for (int i = -1; i <= 1; i++) {
            out.add(new Lane(2, i, "ladder[facing=east]"));
            out.add(new Lane(-2, i, "ladder[facing=west]"));
            out.add(new Lane(i, 2, "vine[north=true]"));
            out.add(new Lane(i, -2, "vine[south=true]"));
        }
        return List.copyOf(out);
    }

    /** Ein Stück, das gleich wegbricht oder schon weg ist. */
    private static final class Gap {
        int lane;
        int y0;
        int breakAt;
        int backAt;
        boolean gone;
    }

    private final List<Gap> gaps = new ArrayList<>();

    MaibaumKraxeln(Setup s) {
        super(s);
    }

    static void build(Plan p) {
        Random r = new Random(23);
        // Dorfplatz
        for (int x = -PLAZA - 1; x <= PLAZA + 1; x++) {
            for (int z = -PLAZA - 1; z <= PLAZA + 1; z++) {
                double d = Math.hypot(x, z);
                if (d > PLAZA + 1.4) {
                    continue;
                }
                p.set(x, -1, z, "dirt");
                p.set(x, 0, z, d > PLAZA + 0.4 ? "stone_bricks"
                        : d < 3.6 ? "polished_andesite" : r.nextInt(5) == 0 ? "mossy_cobblestone" : "cobblestone");
                if (d > PLAZA + 0.4) {
                    p.set(x, 1, z, "spruce_fence");
                    for (int h = 2; h <= 4; h++) {
                        p.set(x, h, z, "barrier");
                    }
                }
            }
        }
        // Stamm 3 × 3, weiß-blau gewunden
        for (int y = 1; y <= TOP; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) {
                        p.set(x, y, z, "stripped_birch_log[axis=y]");
                        continue;
                    }
                    int k = Math.floorMod((int) Math.round(Math.atan2(z, x) / (Math.PI / 4)), 8);
                    p.set(x, y, z, Math.floorMod(y + k, 6) < 3 ? "light_blue_concrete" : "white_concrete");
                }
            }
        }
        for (Lane l : LANES) {
            for (int y = 1; y <= TOP; y++) {
                p.set(l.x(), y, l.z(), l.block());
            }
        }
        // Spitze mit Kranz, Bändern und Fahne
        for (int y = TOP + 1; y <= TOP + 6; y++) {
            p.set(0, y, 0, "stripped_birch_log[axis=y]");
        }
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                double d = Math.hypot(x, z);
                if (d >= 1.5 && d <= 2.6) {
                    p.set(x, TOP + 5, z, (x + z) % 3 == 0 ? "flowering_azalea_leaves[persistent=true]"
                            : "azalea_leaves[persistent=true]");
                }
            }
        }
        for (int[] c : new int[][] {{2, 2}, {-2, 2}, {2, -2}, {-2, -2}}) {
            p.set(c[0], TOP + 4, c[1], "white_wool");
            p.set(c[0], TOP + 3, c[1], "light_blue_wool");
        }
        p.banner(0, TOP + 7, 0, Decor.bannerBlock(2, false, "0"), Decor.bannerPatterns(2));
    }

    @Override
    protected List<Location> spawns(int n) {
        List<Location> out = new ArrayList<>();
        double r = 5;
        for (int i = 0; i < Math.max(1, n); i++) {
            double a = 2 * Math.PI * i / Math.max(1, n) + Math.PI / 8;
            out.add(facing(0.5 + Math.cos(a) * r, 1, 0.5 + Math.sin(a) * r, 0.5, 6, 0.5));
        }
        return out;
    }

    @Override
    public Location viewPoint() {
        return facing(0.5, 16, -17, 0.5, 13, 0.5);
    }

    @Override
    protected void equip(Player p) {
        // Klettern genügt
    }

    @Override
    protected void onTick() {
        if (ticks % 2 == 0) {
            for (Player p : alivePlayers()) {
                Location l = p.getLocation();
                double y = l.getY() - oy;
                boolean onTop = y >= TOP + 0.9 && Math.abs(l.getX() - ox - 0.5) <= 1.7
                        && Math.abs(l.getZ() - oz - 0.5) <= 1.7;
                if (onTop) {
                    arrive(p, "mini.maibaumkraxeln.finish");
                } else {
                    progress(p, (int) Math.floor(y - 1));
                }
            }
        }
        int most = Math.min(6, 2 + ticks / (8 * SECOND));
        if (ticks % 12 == 0 && gaps.size() < most) {
            newGap();
        }
        Iterator<Gap> it = gaps.iterator();
        while (it.hasNext()) {
            Gap g = it.next();
            Lane l = LANES.get(g.lane);
            if (!g.gone && ticks < g.breakAt && ticks % 3 == 0) {
                for (int i = 0; i < GAP_LENGTH; i++) {
                    Block b = block(l.x(), g.y0 + i, l.z());
                    world.spawnParticle(Particle.BLOCK, b.getLocation().add(0.5, 0.5, 0.5), 3, 0.2, 0.3, 0.2,
                            b.getBlockData());
                }
            } else if (!g.gone && ticks >= g.breakAt) {
                g.gone = true;
                for (int i = 0; i < GAP_LENGTH; i++) {
                    block(l.x(), g.y0 + i, l.z()).setType(Material.AIR, false);
                }
                world.playSound(at(l.x() + 0.5, g.y0 + 1, l.z() + 0.5), "minecraft:block.wood.break", 1f, 0.8f);
            } else if (g.gone && ticks >= g.backAt) {
                restore(g);
                it.remove();
            }
        }
    }

    private void newGap() {
        for (int tries = 0; tries < 10; tries++) {
            int lane = rng.nextInt(LANES.size());
            int y0 = LOWEST_GAP + rng.nextInt(TOP - LOWEST_GAP - GAP_LENGTH);
            boolean taken = gaps.stream().anyMatch(g -> g.lane == lane && Math.abs(g.y0 - y0) < GAP_LENGTH + 2);
            if (!taken) {
                Gap g = new Gap();
                g.lane = lane;
                g.y0 = y0;
                g.breakAt = ticks + WARN;
                g.backAt = ticks + WARN + GONE;
                gaps.add(g);
                return;
            }
        }
    }

    private void restore(Gap g) {
        Lane l = LANES.get(g.lane);
        BlockData d = de.moinmorni.party.world.Plan.data(l.block());
        for (int i = 0; i < GAP_LENGTH; i++) {
            block(l.x(), g.y0 + i, l.z()).setBlockData(d, false);
        }
    }

    @Override
    protected void onFinish() {
        gaps.stream().filter(g -> g.gone).forEach(this::restore);
        gaps.clear();
    }

    @Override
    protected String wayKey() {
        return "mini.line.height";
    }

    @Override
    protected String finishKey() {
        return "mini.line.top";
    }
}
