package de.moinmorni.party.mini;

import de.moinmorni.party.Items;
import de.moinmorni.party.Texts;
import de.moinmorni.party.world.Plan;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;

/**
 * Scheibenschießen: Wie beim Schützenfest – vom Schützenstand aus mit Schneebällen auf die Zielscheiben werfen. Jede
 * getroffene Scheibe ist ein Punkt und springt an eine neue Stelle.
 */
final class ScheibenSchiessen extends MiniRun {

    static final int HALF = 8;
    static final int WALL_Z = 16;
    private static final int TARGETS = 4;
    private static final int REFILL = 2 * SECOND;
    private static final int BALLS = 16;
    private final List<Block> targets = new ArrayList<>();

    ScheibenSchiessen(Setup s) {
        super(s);
    }

    static void build(Plan p) {
        for (int x = -HALF - 1; x <= HALF + 1; x++) {
            for (int z = -6; z <= WALL_Z; z++) {
                p.set(x, 0, z, z <= -2 ? "spruce_planks" : "grass_block[snowy=false]");
                p.set(x, -1, z, "dirt");
            }
            // Brüstung des Schützenstands (Schneebälle fliegen darüber)
            p.set(x, 1, -2, "spruce_fence");
            p.set(x, 1, -7, "barrier");
            p.set(x, 2, -7, "barrier");
            p.set(x, 2, -2, "air");
            // Kugelfang: Wand hinter den Scheiben
            for (int y = 1; y <= 9; y++) {
                p.set(x, y, WALL_Z + 1, y % 3 == 0 ? "stripped_spruce_log[axis=x]" : "spruce_planks");
            }
        }
        for (int z = -7; z <= -2; z++) {
            for (int y = 1; y <= 3; y++) {
                p.set(-HALF - 2, y, z, "barrier");
                p.set(HALF + 2, y, z, "barrier");
            }
        }
        de.moinmorni.party.world.Decor.island(p, HALF + 2, 8, new java.util.Random(9), "dirt", 1);
        // Dach über dem Stand
        for (int x = -HALF - 1; x <= HALF + 1; x++) {
            for (int z = -7; z <= -2; z++) {
                p.set(x, 5, z, z % 2 == 0 ? "red_wool" : "white_wool");
            }
        }
        for (int x : new int[] {-HALF - 1, HALF + 1}) {
            p.fill(x, 1, -2, x, 4, -2, "spruce_log[axis=y]");
            p.fill(x, 1, -7, x, 4, -7, "spruce_log[axis=y]");
        }
    }

    @Override
    protected List<Location> spawns(int n) {
        List<Location> out = new ArrayList<>();
        for (int i = 0; i < Math.max(1, n); i++) {
            double x = -HALF + 1.5 + ((2.0 * HALF - 2) * i / Math.max(1, n - 1));
            if (n == 1) {
                x = 0.5;
            }
            out.add(facing(x, 1, -3.5, x, 3, WALL_Z));
        }
        return out;
    }

    @Override
    public Location viewPoint() {
        return facing(0, 7, -10, 0, 3, WALL_Z);
    }

    @Override
    protected void equip(Player p) {
        p.getInventory().setItem(0, Items.named(Material.SNOWBALL, BALLS,
                Texts.text(plugin.lang(p), "mini.scheibenschiessen.item"), null));
    }

    @Override
    protected void onStart() {
        for (int i = 0; i < TARGETS; i++) {
            newTarget();
        }
    }

    private void newTarget() {
        for (int tries = 0; tries < 20; tries++) {
            int x = rng.nextInt(2 * HALF - 1) - HALF + 1;
            int y = 1 + rng.nextInt(6);
            int z = 8 + rng.nextInt(WALL_Z - 8);
            Block b = block(x, y, z);
            if (b.getType() == Material.AIR) {
                b.setType(Material.TARGET, false);
                targets.add(b);
                return;
            }
        }
    }

    @Override
    protected void onTick() {
        if (ticks % REFILL == 0) {
            for (Player p : alivePlayers()) {
                var item = p.getInventory().getItem(0);
                if (item == null || item.getType() != Material.SNOWBALL || item.getAmount() < BALLS) {
                    equip(p);
                }
            }
        }
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Snowball ball) || !(ball.getShooter() instanceof Player p)) {
            return;
        }
        Block b = e.getHitBlock();
        if (b != null && targets.contains(b) && score.alive(p.getUniqueId())) {
            targets.remove(b);
            b.setType(Material.AIR, false);
            point(p, 1);
            newTarget();
        }
    }

    @Override
    protected void onFinish() {
        targets.forEach(b -> b.setType(Material.AIR, false));
        targets.clear();
    }
}
