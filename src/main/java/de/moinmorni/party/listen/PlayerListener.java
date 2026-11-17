package de.moinmorni.party.listen;

import de.moinmorni.party.Items;
import de.moinmorni.party.PartyPlugin;
import de.moinmorni.party.Texts;
import de.moinmorni.party.Ui;
import de.moinmorni.party.menu.Menu;
import de.moinmorni.party.mini.MiniRun;
import de.moinmorni.party.play.Game;
import de.moinmorni.party.world.Places;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
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
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
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

    /** Auf dem Festplatz ankommen – oder, wenn der Platz in der Partie noch frei ist, dort, wo man war. */
    @EventHandler
    public void onSpawnLocation(AsyncPlayerSpawnLocationEvent e) {
        Location back = plugin.games().returnSpot(e.getConnection().getProfile().getId());
        e.setSpawnLocation(back != null ? back : new Location(plugin.world(), Places.SPAWN_X, Places.FLOOR_Y + 1,
                Places.SPAWN_Z, Places.SPAWN_YAW, 0f));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.joinMessage(null);
        Player p = e.getPlayer();
        Optional<Game> running = plugin.games().of(p.getUniqueId());
        if (running.isPresent() && running.get().rejoin(p)) {
            plugin.stateChanged();
            return;
        }
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
            if (plugin.games().wasMissed(p.getUniqueId())) {
                p.sendMessage(Texts.msg(lang, "game.missed"));
            }
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
        plugin.games().involved(u).ifPresent(g -> g.quit(p));
        plugin.net().forget(u);
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
            if (!game.get().mini().score().has(p.getUniqueId())) {
                e.setCancelled(true); // Duell: wer nicht mitspielt, wartet auf dem Brett
                return;
            }
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

    /** Das Minispiel, in dem diese Person gerade (noch) mitspielt. */
    private Optional<MiniRun> playing(Player p) {
        return plugin.games().of(p.getUniqueId()).filter(g -> g.phase() == Game.Phase.MINI).map(Game::mini)
                .filter(m -> m != null && m.running() && m.score().alive(p.getUniqueId()));
    }

    /** Rechtsklick auf ein Wesen (Murmeltier, Schlitten …): nur das Minispiel entscheidet. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onUseEntity(PlayerInteractEntityEvent e) {
        Optional<MiniRun> m = playing(e.getPlayer());
        if (m.isEmpty()) {
            return;
        }
        if (e.getHand() == EquipmentSlot.HAND) {
            m.get().onEntityClick(e.getPlayer(), e.getRightClicked());
        }
        e.setCancelled(true);
    }

    /** Schlag auf ein Wesen oder eine Klickfläche (kommt vor jedem Schaden, auch bei Wesen ohne Leben). */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAttack(PrePlayerAttackEntityEvent e) {
        if (e.getAttacked() instanceof Player) {
            return; // Schaden an Mitspielenden regelt das Minispiel über das Schadens-Ereignis
        }
        Optional<MiniRun> m = playing(e.getPlayer());
        if (m.isPresent()) {
            e.setCancelled(true);
            m.get().onEntityClick(e.getPlayer(), e.getAttacked());
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        playing(e.getPlayer()).ifPresent(m -> m.onSneak(e.getPlayer(), e.isSneaking()));
    }

    @EventHandler
    public void onJump(PlayerJumpEvent e) {
        playing(e.getPlayer()).ifPresent(m -> m.onJump(e.getPlayer()));
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
        if (mini != null && mini.running() && mini.score().has(p.getUniqueId())) {
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
            if (g.phase() == Game.Phase.MINI && g.mini() != null && g.mini().running()
                    && g.mini().score().has(p.getUniqueId())) {
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
