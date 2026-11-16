package net.morni.party.mini;

import net.morni.party.world.Plan;
import org.bukkit.Particle;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

/** Pfeilhagel: Pfeile prasseln vom Himmel – wer getroffen wird, ist raus. Mit der Zeit wird es mehr. */
final class PfeilHagel extends MiniRun {

    static final int HALF = 7;
    private static final int HEIGHT = 16;

    PfeilHagel(Setup s) {
        super(s);
    }

    static void build(Plan p) {
        for (int x = -HALF; x <= HALF; x++) {
            for (int z = -HALF; z <= HALF; z++) {
                p.set(x, 0, z, (Math.floorMod(x + z, 2) == 0) ? "stone_bricks" : "polished_andesite");
                p.set(x, -1, z, "stone");
                p.set(x, -2, z, "cobblestone");
            }
        }
        for (int i = -HALF - 1; i <= HALF + 1; i++) {
            for (int[] c : new int[][] {{i, -HALF - 1}, {i, HALF + 1}, {-HALF - 1, i}, {HALF + 1, i}}) {
                p.set(c[0], 0, c[1], "mossy_stone_bricks");
                p.set(c[0], 1, c[1], "stone_brick_wall");
                for (int h = 2; h <= 4; h++) {
                    p.set(c[0], h, c[1], "barrier");
                }
            }
        }
        net.morni.party.world.Decor.island(p, HALF + 1, 8, new java.util.Random(4), null, 0);
        // Zielscheiben-Türme an den Ecken
        for (int sx : new int[] {-HALF - 1, HALF + 1}) {
            for (int sz : new int[] {-HALF - 1, HALF + 1}) {
                p.fill(sx, 1, sz, sx, 5, sz, "stone_bricks");
                p.set(sx, 6, sz, "target");
            }
        }
    }

    @Override
    protected void equip(Player p) {
        // ausweichen
    }

    @Override
    protected void onTick() {
        int every = ticks < 10 * SECOND ? 8 : ticks < 25 * SECOND ? 5 : 3;
        if (ticks % every != 0) {
            return;
        }
        int count = 1 + (ticks > 20 * SECOND ? 1 : 0) + (rng.nextInt(3) == 0 ? 1 : 0);
        for (int i = 0; i < count; i++) {
            double x;
            double z;
            // Jeder dritte Pfeil zielt grob auf jemanden – sonst könnte man in einer Ecke stehen bleiben.
            var alive = alivePlayers();
            if (!alive.isEmpty() && rng.nextInt(3) == 0) {
                var t = alive.get(rng.nextInt(alive.size())).getLocation();
                x = t.getX() - ox + rng.nextGaussian() * 1.2;
                z = t.getZ() - oz + rng.nextGaussian() * 1.2;
            } else {
                x = rng.nextDouble() * (2 * HALF + 1) - HALF;
                z = rng.nextDouble() * (2 * HALF + 1) - HALF;
            }
            x = Math.max(-HALF, Math.min(HALF + 0.99, x));
            z = Math.max(-HALF, Math.min(HALF + 0.99, z));
            Arrow a = world.spawnArrow(at(x, HEIGHT, z), new Vector(0, -1, 0), 1.1f, 0f);
            a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            a.setDamage(0.5);
            world.spawnParticle(Particle.CRIT, at(x, 1.2, z), 4, 0.2, 0.05, 0.2, 0);
            track(a);
        }
        // Gelandete Pfeile verschwinden
        if (ticks % SECOND == 0) {
            world.getNearbyEntitiesByType(Arrow.class, at(0, 2, 0), HALF + 3, 6, HALF + 3).stream()
                    .filter(AbstractArrow::isInBlock).forEach(Entity::remove);
        }
    }

    @Override
    public void onDamage(Player victim, EntityDamageEvent e) {
        e.setCancelled(true);
        if (e instanceof EntityDamageByEntityEvent by && by.getDamager() instanceof Arrow arrow) {
            arrow.remove();
            out(victim);
        }
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent e) {
        if (e.getHitEntity() instanceof Player p && score.alive(p.getUniqueId())) {
            e.getEntity().remove();
            out(p);
        }
    }
}
