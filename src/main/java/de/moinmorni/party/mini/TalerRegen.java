package de.moinmorni.party.mini;

import de.moinmorni.party.Ui;
import de.moinmorni.party.world.Decor;
import de.moinmorni.party.world.Plan;
import java.util.Random;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

/** Talerregen: Goldtaler fallen vom Himmel – wer die meisten einsammelt, gewinnt. Ab und zu ein Goldbarren (3 Punkte). */
final class TalerRegen extends MiniRun {

    static final int HALF = 10;
    private static final int GOLD_CHANCE = 10;
    private final NamespacedKey worth;

    TalerRegen(Setup s) {
        super(s);
        worth = new NamespacedKey(plugin, "worth");
    }

    static void build(Plan p) {
        Random r = new Random(11);
        p.fill(-HALF - 1, -1, -HALF - 1, HALF + 1, -1, HALF + 1, "dirt");
        for (int x = -HALF; x <= HALF; x++) {
            for (int z = -HALF; z <= HALF; z++) {
                p.set(x, 0, z, r.nextInt(9) == 0 ? "moss_block" : "grass_block[snowy=false]");
                if (r.nextInt(14) == 0) {
                    p.set(x, 1, z, Decor.pick(r, "short_grass", "dandelion", "poppy", "cornflower"));
                }
            }
        }
        for (int i = -HALF - 1; i <= HALF + 1; i++) {
            p.set(i, 0, -HALF - 1, "spruce_planks");
            p.set(i, 0, HALF + 1, "spruce_planks");
            p.set(-HALF - 1, 0, i, "spruce_planks");
            p.set(HALF + 1, 0, i, "spruce_planks");
            p.set(i, 1, -HALF - 1, "spruce_fence");
            p.set(i, 1, HALF + 1, "spruce_fence");
            p.set(-HALF - 1, 1, i, "spruce_fence");
            p.set(HALF + 1, 1, i, "spruce_fence");
            for (int h = 2; h <= 4; h++) {
                p.set(i, h, -HALF - 1, "barrier");
                p.set(i, h, HALF + 1, "barrier");
                p.set(-HALF - 1, h, i, "barrier");
                p.set(HALF + 1, h, i, "barrier");
            }
        }
        Decor.island(p, HALF + 1, 7, r, "dirt", 1);
        for (int[] c : new int[][] {{-HALF - 4, -HALF - 4}, {HALF + 4, HALF + 4}, {-HALF - 4, HALF + 4}, {HALF + 4, -HALF - 4}}) {
            p.fill(c[0] - 2, -1, c[1] - 2, c[0] + 2, 0, c[1] + 2, "grass_block[snowy=false]");
            Decor.spruce(p, c[0], 1, c[1], 7, r);
        }
    }

    @Override
    protected void equip(Player p) {
        // Hände frei zum Sammeln
    }

    @Override
    protected void onTick() {
        int every = ticks < 15 * SECOND ? 5 : 3;
        if (ticks % every != 0) {
            return;
        }
        boolean gold = rng.nextInt(100) < GOLD_CHANCE;
        ItemStack stack = new ItemStack(gold ? Material.GOLD_INGOT : Material.GOLD_NUGGET);
        stack.editMeta(m -> m.getPersistentDataContainer().set(worth, PersistentDataType.INTEGER, gold ? 3 : 1));
        double x = rng.nextDouble() * (2 * HALF - 1) - HALF + 0.5;
        double z = rng.nextDouble() * (2 * HALF - 1) - HALF + 0.5;
        Item item = world.dropItem(at(x, 9, z), stack);
        item.setVelocity(new Vector(0, -0.2, 0));
        item.setPickupDelay(0);
        item.setCanMobPickup(false);
        item.setUnlimitedLifetime(true);
        track(item);
    }

    @Override
    public void onPickup(Player p, Item item, PlayerAttemptPickupItemEvent e) {
        e.setCancelled(true);
        Integer value = item.getItemStack().getItemMeta() == null ? null
                : item.getItemStack().getItemMeta().getPersistentDataContainer().get(worth, PersistentDataType.INTEGER);
        if (value == null || !score.alive(p.getUniqueId())) {
            return;
        }
        untrack(item);
        point(p, value);
        if (value > 1) {
            Ui.sound(p, "minecraft:block.note_block.bell", 1.5f);
        }
    }
}
