package de.moinmorni.party.mini;

import de.moinmorni.party.Items;
import de.moinmorni.party.Texts;
import de.moinmorni.party.Ui;
import de.moinmorni.party.game.Teams;
import de.moinmorni.party.world.Plan;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Gamsjagd (nur einer gegen drei): Auf der Almwiese ist der Einzelne der Jäger, die drei anderen sind Gämsen. Der
 * Jäger ist schneller (sein Vorteil) und darf erst nach drei Sekunden los; ein Schlag fängt eine Gämse. Sind alle
 * gefangen, gewinnt der Jäger – bleibt bis zum Ende eine übrig, gewinnen die Gämsen. Felsen, Büsche und hohes Gras
 * helfen beim Ausweichen.
 */
final class GamsJagd extends MiniRun {

    private static final int HEAD_START = 3 * SECOND;

    private final UUID hunter;

    GamsJagd(Setup s) {
        super(s);
        hunter = s.teams().a().isEmpty() ? null : s.teams().a().getFirst();
    }

    static void build(Plan p) {
        EnzianSuche.build(p);
    }

    @Override
    public void prepare() {
        place(plugin.arenaPlan(type));
        List<Location> gams = new ArrayList<>(super.spawns(3));
        int k = 0;
        for (Player p : players) {
            reset(p);
            p.setGameMode(gameMode());
            boolean isHunter = p.getUniqueId().equals(hunter);
            p.teleport(isHunter ? facing(0.5, 1, 0.5, 0.5, 1, 6) : gamsSpot(k++));
            equip(p);
            teamCap(p);
        }
    }

    private Location gamsSpot(int k) {
        double r = EnzianSuche.HALF - 2;
        double a = 2 * Math.PI * k / 3 + Math.PI / 6;
        return facing(0.5 + Math.cos(a) * r, 1, 0.5 + Math.sin(a) * r, 0.5, 1, 0.5);
    }

    @Override
    protected void equip(Player p) {
        String lang = plugin.lang(p);
        if (p.getUniqueId().equals(hunter)) {
            p.getInventory().setItem(0, Items.named(Material.STICK, 1, Texts.text(lang, "mini.gamsjagd.item"), null));
        }
    }

    @Override
    protected void onStart() {
        Player h = hunterPlayer();
        if (h != null) {
            effect(h, new PotionEffect(PotionEffectType.SLOWNESS, HEAD_START, 9, false, false));
            h.setGlowing(true);
        }
        for (Player p : alivePlayers()) {
            String lang = plugin.lang(p);
            Ui.title(p, Texts.text(lang, p.getUniqueId().equals(hunter) ? "mini.gamsjagd.you.hunter"
                    : "mini.gamsjagd.you.gams"), net.kyori.adventure.text.Component.empty(), 1800);
        }
    }

    private Player hunterPlayer() {
        return hunter == null ? null : players.stream().filter(p -> p.getUniqueId().equals(hunter)).findFirst()
                .orElse(null);
    }

    @Override
    protected void onTick() {
        Player h = hunterPlayer();
        if (h == null) {
            return;
        }
        if (ticks == HEAD_START) {
            effect(h, new PotionEffect(PotionEffectType.SPEED, durationTicks(), 0, false, false));
            soundAll("minecraft:entity.goat.screaming.ambient", 1f);
            say("mini.gamsjagd.go", Placeholder.unparsed("name", h.getName()));
        }
        if (ticks % 10 == 0) {
            int left = gamsLeft();
            for (Player p : alivePlayers()) {
                bar(p, "mini.gamsjagd.bar", Placeholder.unparsed("n", String.valueOf(left)));
            }
        }
    }

    private int gamsLeft() {
        return (int) score.alive().stream().filter(u -> !u.equals(hunter)).count();
    }

    @Override
    public void onDamage(Player victim, EntityDamageEvent e) {
        e.setCancelled(true);
        if (!(e instanceof EntityDamageByEntityEvent by) || !(by.getDamager() instanceof Player attacker)
                || !attacker.getUniqueId().equals(hunter) || victim.getUniqueId().equals(hunter)
                || ticks < HEAD_START) {
            return;
        }
        score.add(hunter, 1);
        world.spawnParticle(Particle.CLOUD, victim.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
        Ui.sound(attacker, "minecraft:entity.player.levelup", 1.6f);
        say("mini.gamsjagd.caught", Placeholder.unparsed("name", victim.getName()),
                Placeholder.unparsed("hunter", attacker.getName()));
        out(victim);
    }

    @Override
    protected boolean complete() {
        return hunter == null || !score.has(hunter) || gamsLeft() == 0;
    }

    @Override
    protected void onLeave(Player p) {
        // Geht der Jäger, ist die Jagd vorbei – die Gämsen gewinnen.
    }

    @Override
    public Teams.Outcome teamOutcome() {
        if (hunter == null || !score.has(hunter)) {
            return Teams.Outcome.B;
        }
        return gamsLeft() == 0 ? Teams.Outcome.A : Teams.Outcome.B;
    }

    @Override
    public Location viewPoint() {
        return facing(0.5, 14, -EnzianSuche.HALF - 9, 0.5, 0, 0.5);
    }
}
