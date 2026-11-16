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
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Schneeballschlacht: Jeder gegen jeden auf dem verschneiten Feld. Schneebälle holt man sich an den Schneewehen
 * (anklicken), dann werfen: jeder Treffer ist ein Punkt und schubst den Getroffenen weg, der danach kurz unverwundbar
 * ist (er leuchtet). Hinter den Schneemauern kann man sich verstecken.
 */
final class SchneeballSchlacht extends MiniRun {

    static final int HALF = 12;
    /** Schneewehen (x, z): hier gibt es Nachschub. */
    static final int[][] DRIFTS = {{0, 0}, {-8, -8}, {8, 8}, {-8, 8}, {8, -8}, {0, -10}, {0, 10}, {-10, 0}, {10, 0}};
    private static final int PER_GRAB = 4;
    private static final int MAX_BALLS = 12;
    private static final int START_BALLS = 4;
    private static final int GRAB_GAP = SECOND;
    private static final int IMMUNE = 30;

    private final Map<UUID, Integer> lastGrab = new HashMap<>();
    private final Map<UUID, Integer> immune = new HashMap<>();

    SchneeballSchlacht(Setup s) {
        super(s);
    }

    static void build(Plan p) {
        Random r = new Random(52);
        for (int x = -HALF - 1; x <= HALF + 1; x++) {
            for (int z = -HALF - 1; z <= HALF + 1; z++) {
                p.set(x, -1, z, "packed_ice");
                p.set(x, 0, z, "snow_block");
                if (Math.abs(x) == HALF + 1 || Math.abs(z) == HALF + 1) {
                    p.set(x, 1, z, "spruce_fence");
                    for (int h = 2; h <= 5; h++) {
                        p.set(x, h, z, "barrier");
                    }
                } else if (r.nextInt(5) == 0) {
                    p.set(x, 1, z, "snow[layers=1]");
                }
            }
        }
        // Schneemauern (Deckung) mit Lücken
        int[][] walls = {{-5, -3, -5, 3}, {5, -3, 5, 3}, {-3, -5, 3, -5}, {-3, 5, 3, 5}, {-11, -4, -11, -1},
            {11, 1, 11, 4}, {-4, 11, -1, 11}, {1, -11, 4, -11}};
        for (int[] w : walls) {
            for (int x = w[0]; x <= w[2]; x++) {
                for (int z = w[1]; z <= w[3]; z++) {
                    if ((x + z) % 7 != 0) {
                        p.set(x, 1, z, "snow_block");
                        p.set(x, 2, z, "snow[layers=4]");
                    }
                }
            }
        }
        for (int[] d : DRIFTS) {
            p.set(d[0], 1, d[1], "snow_block");
            p.set(d[0], 2, d[1], "snow[layers=3]");
            for (int[] n : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                p.set(d[0] + n[0], 1, d[1] + n[1], "snow[layers=4]");
            }
        }
        Decor.island(p, HALF + 1, 7, r, "packed_ice", 1);
    }

    @Override
    protected GameMode gameMode() {
        return GameMode.SURVIVAL;
    }

    @Override
    protected List<Location> spawns(int n) {
        List<Location> out = new ArrayList<>();
        double r = HALF - 2.5;
        for (int i = 0; i < Math.max(1, n); i++) {
            double a = 2 * Math.PI * i / Math.max(1, n) + Math.PI / 8;
            out.add(facing(0.5 + Math.cos(a) * r, 1, 0.5 + Math.sin(a) * r, 0.5, 1, 0.5));
        }
        return out;
    }

    @Override
    public Location viewPoint() {
        return facing(0.5, 14, -HALF - 10, 0.5, 0, 0.5);
    }

    @Override
    protected void equip(Player p) {
        p.getInventory().setItem(0, balls(p, START_BALLS));
    }

    private ItemStack balls(Player p, int n) {
        return Items.named(Material.SNOWBALL, n, Texts.text(plugin.lang(p), "mini.scheibenschiessen.item"), null);
    }

    private int count(Player p) {
        int n = 0;
        for (ItemStack it : p.getInventory().getContents()) {
            if (it != null && it.getType() == Material.SNOWBALL) {
                n += it.getAmount();
            }
        }
        return n;
    }

    private boolean drift(Block b) {
        if (b == null) {
            return false;
        }
        for (int[] d : DRIFTS) {
            if (b.getX() == ox + d[0] && b.getZ() == oz + d[1] && b.getY() >= oy + 1 && b.getY() <= oy + 2) {
                return true;
            }
        }
        return false;
    }

    private void grab(Player p, Block b) {
        UUID u = p.getUniqueId();
        if (!drift(b) || !score.alive(u) || ticks - lastGrab.getOrDefault(u, -GRAB_GAP) < GRAB_GAP) {
            return;
        }
        lastGrab.put(u, ticks);
        int have = count(p);
        int add = Math.min(PER_GRAB, MAX_BALLS - have);
        if (add <= 0) {
            bar(p, "mini.schneeballschlacht.full");
            return;
        }
        p.getInventory().clear();
        p.getInventory().setItem(0, balls(p, have + add));
        world.spawnParticle(Particle.SNOWFLAKE, b.getLocation().add(0.5, 1, 0.5), 8, 0.3, 0.2, 0.3, 0.02);
        Ui.sound(p, "minecraft:block.snow.break", 1.2f);
        bar(p, "mini.schneeballschlacht.grab", Placeholder.unparsed("n", String.valueOf(have + add)));
    }

    @Override
    public void onBlockDamage(Player p, BlockDamageEvent e) {
        e.setCancelled(true);
        grab(p, e.getBlock());
    }

    @Override
    public void onInteract(Player p, PlayerInteractEvent e) {
        boolean onBlock = e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_BLOCK;
        if (onBlock && drift(e.getClickedBlock())) {
            e.setCancelled(true);
            grab(p, e.getClickedBlock());
        } else if (e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.PHYSICAL) {
            e.setCancelled(true);
        }
        // sonst: Schneeball werfen wie gewohnt
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Snowball ball) || !(ball.getShooter() instanceof Player thrower)
                || !(e.getHitEntity() instanceof Player victim) || victim.equals(thrower)) {
            return;
        }
        if (!score.alive(thrower.getUniqueId()) || !score.alive(victim.getUniqueId())) {
            return;
        }
        e.setCancelled(true);
        ball.remove();
        if (mates(thrower, victim)) {
            return; // eigene Seite: zählt nicht
        }
        if (immune.getOrDefault(victim.getUniqueId(), -1) > ticks) {
            return;
        }
        immune.put(victim.getUniqueId(), ticks + IMMUNE);
        point(thrower, 1);
        Vector push = ball.getVelocity().setY(0);
        if (push.lengthSquared() < 1e-4) {
            push = victim.getLocation().toVector().subtract(thrower.getLocation().toVector()).setY(0);
        }
        victim.setVelocity(push.normalize().multiply(0.7).setY(0.35));
        victim.setGlowing(true);
        world.spawnParticle(Particle.SNOWFLAKE, victim.getLocation().add(0, 1.2, 0), 16, 0.3, 0.4, 0.3, 0.05);
        Ui.sound(victim, "minecraft:entity.player.hurt_freeze", 1f);
        bar(victim, "mini.schneeballschlacht.hit", Placeholder.unparsed("name", thrower.getName()));
    }

    @Override
    protected void onTick() {
        for (var e : immune.entrySet()) {
            if (e.getValue() == ticks) {
                Player p = plugin.getServer().getPlayer(e.getKey());
                if (p != null) {
                    p.setGlowing(false);
                }
            }
        }
    }
}
