package de.moinmorni.party.mini;

import de.moinmorni.party.Items;
import de.moinmorni.party.Texts;
import de.moinmorni.party.Ui;
import de.moinmorni.party.world.Decor;
import de.moinmorni.party.world.Plan;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDamageEvent;

/**
 * Holzhacker: Auf den Baumstümpfen liegen Holzklötze. Drei Hiebe, und der Klotz ist gespalten – wer den letzten Hieb
 * setzt, bekommt den Punkt. Danach liegt woanders ein neuer Klotz.
 */
final class HolzHacker extends MiniRun {

    static final int HALF = 10;
    static final int[][] STUMPS = {
        {-7, -7}, {-2, -8}, {3, -7}, {8, -6}, {-8, -2}, {-3, -3}, {2, -2}, {7, -1},
        {-7, 3}, {-2, 2}, {3, 3}, {8, 4}, {-6, 8}, {-1, 7}, {4, 8}, {0, -5},
    };
    private static final int LOGS = 7;
    private static final int HITS = 3;
    private static final int RESPAWN = 2 * SECOND;
    private static final int HIT_GAP = 4;
    private final Map<Block, Integer> hits = new HashMap<>();
    private final List<Integer> waiting = new ArrayList<>();
    private final Map<UUID, Integer> lastHit = new HashMap<>();

    HolzHacker(Setup s) {
        super(s);
    }

    static void build(Plan p) {
        Random r = new Random(3);
        for (int x = -HALF - 1; x <= HALF + 1; x++) {
            for (int z = -HALF - 1; z <= HALF + 1; z++) {
                p.set(x, -1, z, "dirt");
                p.set(x, 0, z, r.nextInt(5) == 0 ? "coarse_dirt" : r.nextInt(7) == 0 ? "podzol[snowy=false]"
                        : "grass_block[snowy=false]");
            }
        }
        for (int[] s : STUMPS) {
            p.set(s[0], 0, s[1], "stripped_oak_log[axis=y]");
        }
        for (int i = -HALF - 1; i <= HALF + 1; i++) {
            for (int[] c : new int[][] {{i, -HALF - 1}, {i, HALF + 1}, {-HALF - 1, i}, {HALF + 1, i}}) {
                p.set(c[0], 1, c[1], "spruce_log[axis=y]");
                p.set(c[0], 2, c[1], "barrier");
                p.set(c[0], 3, c[1], "barrier");
            }
        }
        Decor.island(p, HALF + 1, 7, r, "grass_block[snowy=false]", 1);
        for (int[] c : new int[][] {{-HALF - 4, 0}, {HALF + 4, 0}, {0, HALF + 4}, {0, -HALF - 4}}) {
            p.fill(c[0] - 2, -1, c[1] - 2, c[0] + 2, 0, c[1] + 2, "grass_block[snowy=false]");
            Decor.spruce(p, c[0], 1, c[1], 8, r);
        }
    }

    @Override
    protected GameMode gameMode() {
        // Im Überlebensmodus darf man Blöcke anschlagen (abgebaut wird nie – das verhindert der Schutz).
        return GameMode.SURVIVAL;
    }

    @Override
    protected void equip(Player p) {
        p.getInventory().setItem(0, Items.named(Material.IRON_AXE, 1, Texts.text(plugin.lang(p), "mini.holzhacker.item"),
                null));
    }

    @Override
    protected void onStart() {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < STUMPS.length; i++) {
            idx.add(i);
        }
        java.util.Collections.shuffle(idx, rng);
        for (int i = 0; i < LOGS; i++) {
            log(idx.get(i)).setType(Material.OAK_LOG, false);
        }
    }

    private Block log(int stump) {
        return block(STUMPS[stump][0], 1, STUMPS[stump][1]);
    }

    @Override
    protected void onTick() {
        if (!waiting.isEmpty() && ticks % RESPAWN == 0) {
            List<Integer> free = new ArrayList<>();
            for (int i = 0; i < STUMPS.length; i++) {
                if (log(i).getType() == Material.AIR) {
                    free.add(i);
                }
            }
            if (!free.isEmpty()) {
                log(free.get(rng.nextInt(free.size()))).setType(Material.OAK_LOG, false);
                waiting.removeFirst();
            }
        }
    }

    @Override
    public void onBlockDamage(Player p, BlockDamageEvent e) {
        e.setCancelled(true);
        Block b = e.getBlock();
        if (b.getType() != Material.OAK_LOG || b.getY() != oy + 1 || !onStump(b) || !score.alive(p.getUniqueId())) {
            return;
        }
        int last = lastHit.getOrDefault(p.getUniqueId(), -HIT_GAP);
        if (ticks - last < HIT_GAP) {
            return;
        }
        lastHit.put(p.getUniqueId(), ticks);
        int n = hits.merge(b, 1, Integer::sum);
        Ui.sound(p, "minecraft:block.wood.hit", 0.9f + n * 0.1f);
        world.spawnParticle(Particle.BLOCK, b.getLocation().add(0.5, 0.6, 0.5), 8, 0.3, 0.3, 0.3,
                b.getBlockData());
        if (n >= HITS) {
            hits.remove(b);
            b.setType(Material.AIR, false);
            Ui.sound(p, "minecraft:block.wood.break", 1f);
            point(p, 1);
            waiting.add(1);
        }
    }

    private boolean onStump(Block b) {
        for (int[] s : STUMPS) {
            if (b.getX() == ox + s[0] && b.getZ() == oz + s[1]) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onFinish() {
        for (int i = 0; i < STUMPS.length; i++) {
            log(i).setType(Material.AIR, false);
        }
    }
}
