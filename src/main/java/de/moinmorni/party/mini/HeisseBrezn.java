package de.moinmorni.party.mini;

import de.moinmorni.party.Items;
import de.moinmorni.party.Texts;
import de.moinmorni.party.Ui;
import de.moinmorni.party.world.Plan;
import java.util.List;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Heiße Brezn: Eine Brezn geht reihum – wer sie hat, gibt sie mit einem Schlag weiter. Wer sie hält, wenn die Zeit
 * abläuft, ist raus. Wer übrig bleibt, gewinnt.
 */
final class HeisseBrezn extends MiniRun {

    static final int HALF = 8;
    private static final int MIN_FUSE = 6 * SECOND;
    private static final int MAX_FUSE = 10 * SECOND;
    private static final int PASS_COOLDOWN = 10;
    private Player holder;
    private int fuseEnd;
    private int lastPass;

    HeisseBrezn(Setup s) {
        super(s);
    }

    static void build(Plan p) {
        for (int x = -HALF; x <= HALF; x++) {
            for (int z = -HALF; z <= HALF; z++) {
                p.set(x, 0, z, Math.floorMod(x, 4) == 0 ? "stripped_spruce_wood[axis=z]" : "spruce_planks");
                p.set(x, -1, z, "spruce_planks");
            }
        }
        for (int i = -HALF - 1; i <= HALF + 1; i++) {
            for (int[] c : new int[][] {{i, -HALF - 1}, {i, HALF + 1}, {-HALF - 1, i}, {HALF + 1, i}}) {
                p.set(c[0], 0, c[1], "dark_oak_planks");
                p.set(c[0], 1, c[1], "dark_oak_fence");
                p.set(c[0], 2, c[1], "barrier");
                p.set(c[0], 3, c[1], "barrier");
            }
        }
        de.moinmorni.party.world.Decor.island(p, HALF + 1, 7, new java.util.Random(6), "grass_block[snowy=false]", 1);
        // Wirtshaus-Ecken: Fässer und Laternen
        for (int sx : new int[] {-HALF, HALF}) {
            for (int sz : new int[] {-HALF, HALF}) {
                p.set(sx, 1, sz, "barrel[facing=up]");
                p.set(sx, 2, sz, "lantern[hanging=false]");
            }
        }
    }

    @Override
    protected void equip(Player p) {
        // Die Brezn bekommt nur, wer sie hat.
    }

    @Override
    protected void onStart() {
        newHolder();
    }

    private void newHolder() {
        List<Player> alive = alivePlayers();
        if (alive.size() < 2) {
            holder = null;
            return;
        }
        give(alive.get(rng.nextInt(alive.size())));
        fuseEnd = ticks + MIN_FUSE + rng.nextInt(MAX_FUSE - MIN_FUSE);
    }

    private void give(Player p) {
        if (holder != null && holder.isOnline()) {
            holder.getInventory().clear();
            holder.setGlowing(false);
        }
        holder = p;
        lastPass = ticks;
        ItemStack brezn = Items.named(Material.BREAD, 1, Texts.text(plugin.lang(p), "mini.heissebrezn.item"), null);
        for (int i = 0; i < 9; i++) {
            p.getInventory().setItem(i, brezn.clone());
        }
        p.setGlowing(true);
        Ui.sound(p, "minecraft:block.fire.ambient", 1.2f);
        Ui.title(p, Texts.text(plugin.lang(p), "mini.heissebrezn.got"), Texts.text(plugin.lang(p), "mini.heissebrezn.got.sub"), 1000);
        say("mini.heissebrezn.has", Placeholder.unparsed("name", p.getName()));
    }

    @Override
    protected void onTick() {
        if (holder == null) {
            newHolder();
            return;
        }
        if (!holder.isOnline() || !score.alive(holder.getUniqueId())) {
            newHolder();
            return;
        }
        int left = fuseEnd - ticks;
        if (ticks % 5 == 0) {
            holder.getWorld().spawnParticle(Particle.SMOKE, holder.getLocation().add(0, 2.1, 0), 3, 0.1, 0.1, 0.1, 0.01);
            bar(holder, "mini.heissebrezn.bar", Placeholder.unparsed("s", String.valueOf((left + SECOND - 1) / SECOND)));
        }
        if (left <= 0) {
            Player burnt = holder;
            holder = null;
            burnt.getWorld().spawnParticle(Particle.EXPLOSION, burnt.getLocation().add(0, 1, 0), 1);
            soundAll("minecraft:entity.generic.explode", 1.3f);
            out(burnt);
            newHolder();
        }
    }

    @Override
    public void onDamage(Player victim, EntityDamageEvent e) {
        if (e instanceof EntityDamageByEntityEvent by && by.getDamager() instanceof Player attacker
                && attacker.equals(holder) && score.alive(victim.getUniqueId()) && ticks - lastPass >= PASS_COOLDOWN) {
            e.setDamage(0);
            give(victim);
            return;
        }
        if (e instanceof EntityDamageByEntityEvent by && by.getDamager() instanceof Player) {
            e.setDamage(0); // schubsen geht, wehtun nicht
            return;
        }
        e.setCancelled(true);
    }

    @Override
    protected void onLeave(Player p) {
        if (p.equals(holder)) {
            holder = null;
        }
    }

    /** Für die Spielrunde (Verwaltung/Tests): wer hält gerade die Brezn? */
    Player holder() {
        return holder;
    }
}
