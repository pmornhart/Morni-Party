package net.morni.party.mini;

import net.morni.party.Items;
import net.morni.party.Texts;
import net.morni.party.world.Plan;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Rempelei: Eine runde Plattform hoch über dem Tal, alle mit einem Wanderstock. Wer hinunterfällt, ist raus. Nach
 * einer Weile bröckelt der Rand ab, dann noch einmal.
 */
final class Rempelei extends MiniRun {

    static final int RADIUS = 9;
    private static final int[] SHRINK = {6, 4};
    private int stage;

    Rempelei(Setup s) {
        super(s);
    }

    static void build(Plan p) {
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                double d = Math.hypot(x, z);
                if (d > RADIUS + 0.4) {
                    continue;
                }
                String b = d > SHRINK[0] + 0.4 ? "mossy_stone_bricks" : d > SHRINK[1] + 0.4 ? "stone_bricks"
                        : "polished_andesite";
                p.set(x, 0, z, b);
                p.set(x, -1, z, "stone");
            }
        }
        p.set(0, 0, 0, "gold_block");
    }

    @Override
    protected void equip(Player p) {
        ItemStack stick = Items.named(Material.STICK, 1, Texts.text(plugin.lang(p), "mini.stick"), null);
        stick.addUnsafeEnchantment(Enchantment.KNOCKBACK, 2);
        p.getInventory().setItem(0, stick);
    }

    @Override
    protected int fallLimit() {
        return 6;
    }

    @Override
    protected void onTick() {
        int whole = durationTicks();
        if (stage < SHRINK.length && ticks >= whole * (stage + 2) / 5) {
            int r = SHRINK[stage];
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    if (Math.hypot(x, z) > r + 0.4) {
                        block(x, 0, z).setType(Material.AIR, false);
                        block(x, -1, z).setType(Material.AIR, false);
                    }
                }
            }
            stage++;
            soundAll("minecraft:block.stone.break", 0.6f);
            say("mini.rempelei.shrink");
        }
    }

    @Override
    public void onDamage(Player victim, EntityDamageEvent e) {
        if (e instanceof EntityDamageByEntityEvent by && by.getDamager() instanceof Player) {
            e.setDamage(0);
            return;
        }
        e.setCancelled(true);
    }
}
