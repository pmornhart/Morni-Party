package de.moinmorni.party.mini;

import de.moinmorni.party.Items;
import de.moinmorni.party.Texts;
import de.moinmorni.party.world.Plan;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Gipfelkönig: Wer oben auf dem goldenen Gipfel steht, sammelt jede Sekunde einen Punkt – allein oben sogar zwei.
 * Mit dem Wanderstock kann man die anderen herunterschubsen.
 */
final class GipfelKoenig extends MiniRun {

    static final int RADIUS = 11;
    static final int TOP = 4;

    GipfelKoenig(Setup s) {
        super(s);
    }

    static void build(Plan p) {
        for (int x = -RADIUS - 1; x <= RADIUS + 1; x++) {
            for (int z = -RADIUS - 1; z <= RADIUS + 1; z++) {
                double d = Math.hypot(x, z);
                if (d > RADIUS + 0.5) {
                    continue;
                }
                p.set(x, -1, z, "dirt");
                p.set(x, 0, z, "grass_block[snowy=false]");
                // Stufen-Pyramide: jede Stufe zwei Blöcke breit
                int level = Math.max(0, TOP - (int) Math.floor(Math.max(Math.abs(x), Math.abs(z)) / 2.0));
                for (int y = 1; y <= level; y++) {
                    p.set(x, y, z, y == level ? (level == TOP ? "gold_block" : "grass_block[snowy=false]") : "dirt");
                }
            }
        }
        de.moinmorni.party.world.Decor.island(p, RADIUS, 9, new java.util.Random(2), "dirt", 1);
        for (int a = 0; a < 360; a += 3) {
            int x = (int) Math.round(Math.cos(Math.toRadians(a)) * (RADIUS + 1));
            int z = (int) Math.round(Math.sin(Math.toRadians(a)) * (RADIUS + 1));
            p.set(x, 0, z, "cobblestone");
            p.set(x, 1, z, "cobblestone_wall");
            for (int h = 2; h <= 6; h++) {
                p.set(x, h, z, "barrier");
            }
        }
    }

    @Override
    protected List<org.bukkit.Location> spawns(int n) {
        List<org.bukkit.Location> out = new java.util.ArrayList<>();
        double r = RADIUS - 1.5;
        for (int i = 0; i < Math.max(1, n); i++) {
            double a = 2 * Math.PI * i / Math.max(1, n);
            out.add(facing(Math.cos(a) * r, 1, Math.sin(a) * r, 0, TOP, 0));
        }
        return out;
    }

    @Override
    protected void equip(Player p) {
        ItemStack stick = Items.named(Material.STICK, 1, Texts.text(plugin.lang(p), "mini.stick"), null);
        stick.addUnsafeEnchantment(Enchantment.KNOCKBACK, 1);
        p.getInventory().setItem(0, stick);
    }

    @Override
    protected void onTick() {
        if (ticks % SECOND != 0) {
            return;
        }
        List<Player> top = alivePlayers().stream().filter(this::onTop).toList();
        for (Player p : top) {
            point(p, top.size() == 1 ? 2 : 1);
        }
    }

    private boolean onTop(Player p) {
        var l = p.getLocation();
        return l.getY() >= oy + TOP + 0.9 && Math.abs(l.getX() - ox - 0.5) <= 2.2 && Math.abs(l.getZ() - oz - 0.5) <= 2.2
                && world.getBlockAt(l.getBlockX(), (int) Math.floor(l.getY() - 0.1), l.getBlockZ()).getType()
                        == Material.GOLD_BLOCK;
    }

    @Override
    public void onDamage(Player victim, EntityDamageEvent e) {
        if (e instanceof EntityDamageByEntityEvent by && by.getDamager() instanceof Player) {
            e.setDamage(0); // nur schubsen
            return;
        }
        e.setCancelled(true);
    }
}
