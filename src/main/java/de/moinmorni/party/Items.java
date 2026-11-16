package de.moinmorni.party;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/** Gegenstände bauen und markieren (Markierung im Gegenstand, nicht am Namen – Namen hängen an der Sprache). */
public final class Items {

    /** Knopf in der Schnellleiste: Aktion wie „roll“, „bag“, „join“, „lobby“. */
    private static NamespacedKey actionKey;

    private Items() {
    }

    static void init(Plugin plugin) {
        actionKey = new NamespacedKey(plugin, "action");
    }

    public static ItemStack named(Material material, int amount, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material, Math.max(1, Math.min(99, amount)));
        item.editMeta(meta -> {
            if (name != null) {
                meta.displayName(name);
            }
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        });
        return item;
    }

    public static ItemStack glow(ItemStack item, boolean glow) {
        item.editMeta(meta -> meta.setEnchantmentGlintOverride(glow ? Boolean.TRUE : null));
        return item;
    }

    /** Knopf mit Aktion. */
    public static ItemStack action(ItemStack item, String action) {
        item.editMeta(meta -> meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action));
        return item;
    }

    public static String action(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }

    /** Leere Glasscheibe als Rahmen in Menüs. */
    public static ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> meta.setHideTooltip(true));
        return item;
    }
}
