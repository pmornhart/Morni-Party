package de.moinmorni.party.listen;

import de.moinmorni.party.Items;
import de.moinmorni.party.PartyPlugin;
import de.moinmorni.party.Texts;
import de.moinmorni.party.Ui;
import de.moinmorni.party.menu.Menu;
import de.moinmorni.party.mini.MiniRun;
import de.moinmorni.party.play.Game;
import de.moinmorni.party.world.Places;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Ankommen, Gehen, Knöpfe, Menüs, Bewegung (Einfrieren auf dem Brett, Sturz vom Festplatz). */
public final class PlayerListener implements Listener {

    /** Doppelklicks (Rechtsklick auf Block + Luft) nur einmal zählen. */
    private static final long CLICK_GAP_MS = 250L;
    private static final long WELCOME_DELAY_TICKS = 20L;
    private static final double HUB_FALL = 25.0;

    private final PartyPlugin plugin;
    private final Map<UUID, Long> lastClick = new HashMap<>();

    public PlayerListener(PartyPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------- Kommen und Gehen ----------

    @EventHandler
    public void onSpawnLocation(AsyncPlayerSpawnLocationEvent e) {
        e.setSpawnLocation(new Location(plugin.world(), Places.SPAWN_X, Places.FLOOR_Y + 1, Places.SPAWN_Z,
                Places.SPAWN_YAW, 0f));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.joinMessage(null);
        Player p = e.getPlayer();
        plugin.lobby().enterHub(p, true);
        plugin.stateChanged();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) {
                return;
            }
            String lang = plugin.lang(p);
            plugin.lobby().giveItems(p);
            plugin.lobby().refresh(p);
            plugin.signs().update();
            Ui.title(p, Texts.text(lang, "join.title"), Texts.text(lang, "join.subtitle"), 2500);
            p.sendMessage(Texts.msg(lang, "join.welcome"));
            if (!plugin.ready()) {
                p.sendMessage(Texts.msg(lang, "not-ready"));
            }
        }, WELCOME_DELAY_TICKS);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        e.quitMessage(null);
        Player p = e.getPlayer();
        UUID u = p.getUniqueId();
        plugin.games().involved(u).ifPresent(g -> g.leave(p, false));
        plugin.lobby().forget(u);
        plugin.sidebar().forget(u);
        lastClick.remove(u);
        plugin.stateChanged();
    }

    // ---------- Knöpfe ----------

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Optional<Game> game = plugin.games().of(p.getUniqueId());
        if (game.isPresent() && game.get().phase() == Game.Phase.MINI && game.get().mini() != null) {
            if (e.getHand() == EquipmentSlot.HAND || e.getAction() == Action.PHYSICAL) {
                game.get().mini().onInteract(p, e);
            }
            return;
        }
        if (e.getAction() == Action.PHYSICAL) {
            e.setCancelled(true);
            return;
        }
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        String action = Items.action(e.getItem());
        if (action == null) {
            if (!plugin.builder(p)) {
                e.setUseInteractedBlock(Event.Result.DENY);
            }
            return;
        }
        boolean right = e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK;
        boolean bedrockTap = e.getAction() == Action.LEFT_CLICK_AIR && Ui.bedrock(p.getUniqueId());
        e.setUseItemInHand(Event.Result.DENY);
        e.setUseInteractedBlock(Event.Result.DENY);
        if (!(right || bedrockTap) || !debounce(p)) {
            return;
        }
        if (game.isPresent()) {
            game.get().use(p, action);
        } else if (plugin.games().watching(p.getUniqueId()).isEmpty()) {
            plugin.lobby().use(p, action);
        }
    }

    private boolean debounce(Player p) {
        long now = System.currentTimeMillis();
        Long last = lastClick.get(p.getUniqueId());
        if (last != null && now - last < CLICK_GAP_MS) {
            return false;
        }
        lastClick.put(p.getUniqueId(), now);
        return true;
    }

    // ---------- Menüs und Inventar ----------

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof Menu menu) {
            menu.click(e);
            return;
        }
        if (e.getWhoClicked() instanceof Player p && !plugin.builder(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Menu menu) {
            menu.drag(e);
            return;
        }
        if (e.getWhoClicked() instanceof Player p && !plugin.builder(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof Menu menu && e.getPlayer() instanceof Player p) {
            // Erst nach dem Schließen handeln (sonst öffnet ein neues Menü im falschen Augenblick).
            plugin.getServer().getScheduler().runTask(plugin, () -> menu.closed(p));
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (!plugin.builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onPickup(PlayerAttemptPickupItemEvent e) {
        Player p = e.getPlayer();
        Optional<Game> game = plugin.games().of(p.getUniqueId());
        MiniRun mini = game.map(Game::mini).orElse(null);
        if (mini != null && mini.running()) {
            mini.onPickup(p, e.getItem(), e);
            return;
        }
        e.setCancelled(true);
    }

    // ---------- Bewegung ----------

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        Optional<Game> game = plugin.games().of(p.getUniqueId());
        if (game.isPresent()) {
            Game g = game.get();
            if (g.phase() == Game.Phase.MINI && g.mini() != null && g.mini().running()) {
                g.mini().onMove(p, e);
            }
            if (g.frozen(p) && e.hasChangedPosition()) {
                Location to = e.getFrom().clone();
                to.setYaw(e.getTo().getYaw());
                to.setPitch(e.getTo().getPitch());
                e.setTo(to);
            }
            return;
        }
        if (plugin.games().watching(p.getUniqueId()).isPresent()) {
            return;
        }
        if (e.getTo().getY() < Places.FLOOR_Y - HUB_FALL) {
            p.teleport(plugin.lobby().spawn());
            p.setFallDistance(0);
        }
    }
}
