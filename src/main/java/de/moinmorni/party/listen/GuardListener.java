package de.moinmorni.party.listen;

import de.moinmorni.party.PartyPlugin;
import de.moinmorni.party.mini.MiniRun;
import de.moinmorni.party.play.Game;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

/** Schutz: nichts abbauen, kein Hunger, kein Schaden – außer, ein Minispiel will es so. */
public final class GuardListener implements Listener {

    private final PartyPlugin plugin;

    public GuardListener(PartyPlugin plugin) {
        this.plugin = plugin;
    }

    private Optional<MiniRun> mini(Player p) {
        return plugin.games().of(p.getUniqueId()).filter(g -> g.phase() == Game.Phase.MINI).map(Game::mini)
                .filter(MiniRun::running);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent e) {
        if (!plugin.builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent e) {
        if (!plugin.builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockDamage(BlockDamageEvent e) {
        Optional<MiniRun> m = mini(e.getPlayer());
        if (m.isPresent()) {
            m.get().onBlockDamage(e.getPlayer(), e);
        } else if (!plugin.builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) {
            e.setCancelled(true);
            return;
        }
        Optional<MiniRun> m = mini(p);
        if (m.isPresent() && m.get().score().alive(p.getUniqueId())) {
            m.get().onDamage(p, e);
            if (!e.isCancelled() && p.getHealth() - e.getFinalDamage() <= 0) {
                e.setDamage(0);
            }
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        for (Game g : plugin.games().all()) {
            if (g.phase() == Game.Phase.MINI && g.mini() != null && g.mini().running()) {
                g.mini().onProjectileHit(e);
            }
        }
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent e) {
        e.setCancelled(true);
        e.getEntity().setFoodLevel(20);
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent e) {
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM
                && e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.COMMAND
                && e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onChangeBlock(EntityChangeBlockEvent e) {
        if (!(e.getEntity() instanceof Player)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onFade(BlockFadeEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onForm(BlockFormEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onSpread(BlockSpreadEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onDecay(LeavesDecayEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onPhysics(BlockPhysicsEvent e) {
        // Betonpulver und Sand sollen liegen bleiben, Wasser im Brunnen nicht auslaufen.
        if (e.getBlock().getType().hasGravity()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onWeather(WeatherChangeEvent e) {
        if (e.toWeatherState()) {
            e.setCancelled(true);
        }
    }
}
