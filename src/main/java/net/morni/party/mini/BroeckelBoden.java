package net.morni.party.mini;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.morni.party.world.Plan;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Bröckelboden: drei Böden übereinander; wo man steht, bröckelt der Boden nach einem Augenblick weg. Wer unten
 * hinausfällt, ist raus – wer am längsten oben bleibt, gewinnt.
 */
final class BroeckelBoden extends MiniRun {

    static final int HALF = 9;
    static final int[] LAYERS = {0, -8, -16};
    private static final String[] FLOORS = {"snow_block", "packed_ice", "blue_ice"};
    private static final int CRUMBLE_TICKS = 9;
    private static final double FOOT = 0.3;

    /** Block → Tick, an dem er verschwindet. */
    private final Map<Block, Integer> crumbling = new HashMap<>();

    BroeckelBoden(Setup s) {
        super(s);
    }

    static void build(Plan p) {
        for (int l = 0; l < LAYERS.length; l++) {
            int y = LAYERS[l];
            p.fill(-HALF, y, -HALF, HALF, y, HALF, FLOORS[l]);
            for (int i = -HALF - 1; i <= HALF + 1; i++) {
                for (int h = 1; h <= 3; h++) {
                    p.set(i, y + h, -HALF - 1, "barrier");
                    p.set(i, y + h, HALF + 1, "barrier");
                    p.set(-HALF - 1, y + h, i, "barrier");
                    p.set(HALF + 1, y + h, i, "barrier");
                }
            }
        }
        // Eiszapfen-Säulen an den Ecken als Rahmen
        for (int sx : new int[] {-HALF - 2, HALF + 2}) {
            for (int sz : new int[] {-HALF - 2, HALF + 2}) {
                p.fill(sx, -18, sz, sx, 3, sz, "packed_ice");
                p.set(sx, 4, sz, "sea_lantern");
            }
        }
    }

    @Override
    protected void equip(Player p) {
        // nichts in der Hand – nur laufen
    }

    @Override
    protected int fallLimit() {
        return 24;
    }

    @Override
    public org.bukkit.Location viewPoint() {
        return facing(0, 10, -HALF - 10, 0, -6, 0);
    }

    @Override
    protected void onTick() {
        for (Player p : alivePlayers()) {
            double x = p.getLocation().getX();
            double z = p.getLocation().getZ();
            int y = (int) Math.floor(p.getLocation().getY() - 0.05);
            for (double dx : new double[] {-FOOT, FOOT}) {
                for (double dz : new double[] {-FOOT, FOOT}) {
                    Block b = world.getBlockAt((int) Math.floor(x + dx), y, (int) Math.floor(z + dz));
                    if (isFloor(b.getType()) && !crumbling.containsKey(b)) {
                        crumbling.put(b, ticks + CRUMBLE_TICKS);
                        b.setType(crack(b.getType()), false);
                    }
                }
            }
        }
        Iterator<Map.Entry<Block, Integer>> it = crumbling.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Block, Integer> e = it.next();
            if (ticks >= e.getValue()) {
                Block b = e.getKey();
                world.spawnParticle(Particle.SNOWFLAKE, b.getLocation().add(0.5, 0.8, 0.5), 6, 0.3, 0.2, 0.3, 0.01);
                b.setType(Material.AIR, false);
                it.remove();
            }
        }
    }

    private static boolean isFloor(Material m) {
        return List.of(Material.SNOW_BLOCK, Material.PACKED_ICE, Material.BLUE_ICE).contains(m);
    }

    /** Angeknackst: gleiche Farbe, aber heller/rissig. */
    private static Material crack(Material m) {
        return switch (m) {
            case SNOW_BLOCK -> Material.WHITE_CONCRETE_POWDER;
            case PACKED_ICE -> Material.LIGHT_BLUE_CONCRETE_POWDER;
            default -> Material.CYAN_CONCRETE_POWDER;
        };
    }

    @Override
    protected void onFinish() {
        crumbling.clear();
    }
}
