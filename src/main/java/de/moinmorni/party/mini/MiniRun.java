package de.moinmorni.party.mini;

import de.moinmorni.party.PartyPlugin;
import de.moinmorni.party.Texts;
import de.moinmorni.party.Ui;
import de.moinmorni.party.world.Plan;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;

/**
 * Ein laufendes Minispiel. Ablauf: {@link #prepare()} (Platz frisch legen, alle hinstellen), Countdown (die Spielrunde
 * friert alle ein), {@link #start()}, jeden Tick {@link #tick()}, bis die Zeit um ist oder das Spiel selbst fertig
 * ist ({@link #done()}). Die Wertung steckt in {@link #score()}.
 */
public abstract class MiniRun {

    /** Alles, was ein Minispiel zum Laufen braucht. */
    public record Setup(PartyPlugin plugin, MiniType type, World world, int ox, int oy, int oz, List<Player> players,
            Supplier<List<Player>> audience, Random rng, double factor) {
    }

    protected static final int SECOND = 20;
    private static final int FALL_LIMIT = 8;

    protected final PartyPlugin plugin;
    protected final MiniType type;
    protected final World world;
    protected final int ox;
    protected final int oy;
    protected final int oz;
    protected final List<Player> players;
    protected final Random rng;
    protected final Scoring score;
    private final Supplier<List<Player>> audience;
    private final int duration;
    private final List<Entity> spawned = new ArrayList<>();
    protected int ticks;
    private boolean running;
    private boolean done;

    protected MiniRun(Setup s) {
        this.plugin = s.plugin();
        this.type = s.type();
        this.world = s.world();
        this.ox = s.ox();
        this.oy = s.oy();
        this.oz = s.oz();
        this.players = new ArrayList<>(s.players());
        this.rng = s.rng();
        this.audience = s.audience();
        this.score = new Scoring(s.type().kind());
        this.duration = (int) Math.round(s.type().seconds() * (s.type().scaled() ? s.factor() : 1.0) * SECOND);
        players.forEach(p -> score.join(p.getUniqueId()));
    }

    public MiniType type() {
        return type;
    }

    public Scoring score() {
        return score;
    }

    public boolean running() {
        return running;
    }

    public boolean done() {
        return done;
    }

    /** Verbleibende Sekunden. */
    public int secondsLeft() {
        return Math.max(0, (duration - ticks + SECOND - 1) / SECOND);
    }

    /** Ticks seit dem Start. */
    public int elapsed() {
        return ticks;
    }

    public int durationTicks() {
        return duration;
    }

    // ---------- Ablauf ----------

    /** Platz frisch legen und alle aufstellen. */
    public void prepare() {
        place(plugin.arenaPlan(type));
        List<Location> spawns = spawns(players.size());
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            reset(p);
            p.setGameMode(gameMode());
            p.teleport(spawns.get(i % spawns.size()));
            equip(p);
        }
    }

    public void start() {
        running = true;
        onStart();
    }

    public void tick() {
        if (!running || done) {
            return;
        }
        ticks++;
        for (Player p : List.copyOf(players)) {
            if (p.isOnline() && score.alive(p.getUniqueId()) && p.getLocation().getY() < oy - fallLimit()) {
                fell(p);
            }
        }
        onTick();
        boolean lastStanding = type.kind() == Scoring.Kind.SURVIVAL && score.all().size() >= 2
                && score.alive().size() <= 1;
        boolean allHome = type.kind() == Scoring.Kind.RACE && score.allFinished();
        if (ticks >= duration || lastStanding || allHome || complete()) {
            finish();
        }
    }

    /** Ende: aufräumen. Danach liest die Spielrunde die Wertung. */
    public void finish() {
        if (done) {
            return;
        }
        done = true;
        running = false;
        onFinish();
        spawned.forEach(Entity::remove);
        spawned.clear();
        for (Player p : players) {
            if (p.isOnline()) {
                p.getInventory().clear();
                p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
                p.setGlowing(false);
            }
        }
    }

    /** Jemand hat die Partie verlassen. */
    public void leave(Player p) {
        players.remove(p);
        score.drop(p.getUniqueId());
        onLeave(p);
    }

    // ---------- Für die einzelnen Spiele ----------

    protected abstract void equip(Player p);

    protected void onStart() {
    }

    protected void onTick() {
    }

    protected void onFinish() {
    }

    protected void onLeave(Player p) {
    }

    /** Selbst fertig (z. B. alle im Ziel)? */
    protected boolean complete() {
        return false;
    }

    /** Spielmodus während des Spiels. */
    protected GameMode gameMode() {
        return GameMode.ADVENTURE;
    }

    /** Bleiben alle die ganze Zeit an ihrem Platz (z. B. Reaktionsspiel)? */
    public boolean frozen() {
        return false;
    }

    protected int fallLimit() {
        return FALL_LIMIT;
    }

    /** Heruntergefallen: im Überlebensspiel raus, sonst zurück an den Start. */
    protected void fell(Player p) {
        if (type.kind() == Scoring.Kind.SURVIVAL) {
            out(p);
        } else {
            List<Location> s = spawns(players.size());
            p.teleport(s.get(rng.nextInt(s.size())));
            p.setFallDistance(0);
        }
    }

    /** Startplätze: Kreis um die Mitte, Blick zur Mitte. */
    protected List<Location> spawns(int n) {
        List<Location> out = new ArrayList<>();
        double r = 4.5;
        for (int i = 0; i < Math.max(1, n); i++) {
            double a = 2 * Math.PI * i / Math.max(1, n);
            out.add(facing(Math.cos(a) * r, 1, Math.sin(a) * r, 0, 1, 0));
        }
        return out;
    }

    /** Wo Ausgeschiedene und Zuschauer stehen. */
    public Location viewPoint() {
        return facing(0, 12, -20, 0, 0, 0);
    }

    // ---------- Ereignisse (Vorgabe: nichts) ----------

    public void onMove(Player p, PlayerMoveEvent e) {
    }

    /** Schaden an Mitspielern: Vorgabe – keiner. */
    public void onDamage(Player victim, EntityDamageEvent e) {
        e.setCancelled(true);
    }

    public void onInteract(Player p, PlayerInteractEvent e) {
    }

    public void onBlockDamage(Player p, BlockDamageEvent e) {
        e.setCancelled(true);
    }

    public void onProjectileHit(ProjectileHitEvent e) {
    }

    public void onPickup(Player p, Item item, PlayerAttemptPickupItemEvent e) {
        e.setCancelled(true);
    }

    /** Block setzen: Vorgabe – verboten. */
    public void onPlace(Player p, BlockPlaceEvent e) {
        e.setCancelled(true);
    }

    /** Klick (Schlag oder Rechtsklick) auf ein Wesen oder Anzeige-Objekt; der Klick selbst ist schon abgefangen. */
    public void onEntityClick(Player p, Entity target) {
    }

    /** Ducken an/aus. */
    public void onSneak(Player p, boolean sneaking) {
    }

    /** Gesprungen (der Server erkennt den Sprung an der Bewegung). */
    public void onJump(Player p) {
    }

    // ---------- Ergebnis ----------

    /** Text-Schlüssel der Ergebniszeile für diese Person. */
    public String resultKey(java.util.UUID u) {
        return switch (type.kind()) {
            case POINTS -> "mini.line.points";
            case SURVIVAL -> "mini.line";
            case RACE -> score.finished(u) ? finishKey() : wayKey();
        };
    }

    /** Zahl in der Ergebniszeile (Punkte, Strecke …). */
    public int shownPoints(java.util.UUID u) {
        return score.points(u);
    }

    /** Rennen: Ergebniszeile für alle, die angekommen sind. */
    protected String finishKey() {
        return "mini.line.finish";
    }

    /** Rennen: Ergebniszeile für alle, die nicht angekommen sind (Punkte = geschaffte Strecke). */
    protected String wayKey() {
        return "mini.line.way";
    }

    // ---------- Helfer ----------

    /** Ausgeschieden: zuschauen. */
    protected void out(Player p) {
        if (!score.eliminate(p.getUniqueId(), ticks)) {
            return;
        }
        p.getInventory().clear();
        p.setGlowing(false);
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(viewPoint());
        Ui.sound(p, "minecraft:entity.villager.no", 1f);
        Ui.title(p, Texts.text(plugin.lang(p), "mini.out.title"), Texts.text(plugin.lang(p), "mini.out.sub"), 1500);
        int left = score.alive().size();
        say("mini.out", Placeholder.unparsed("name", p.getName()), Placeholder.unparsed("n", String.valueOf(left)));
    }

    /** Rennen: angekommen (einmal). {@code key} ist die Meldung an alle („… ist im Ziel – Platz 2!“). */
    protected boolean arrive(Player p, String key) {
        if (!score.finish(p.getUniqueId(), ticks)) {
            return false;
        }
        int pos = score.finishers();
        Ui.sound(p, "minecraft:entity.player.levelup", 1f);
        say(key, Placeholder.unparsed("name", p.getName()), Placeholder.unparsed("pos", String.valueOf(pos)));
        Ui.title(p, Texts.text(plugin.lang(p), "mini.race.done"), net.kyori.adventure.text.Component.empty(), 1200);
        return true;
    }

    /** Rennen: geschaffte Strecke (zählt nur, wenn sie weiter ist als bisher und noch niemand angekommen ist). */
    protected void progress(Player p, int way) {
        java.util.UUID u = p.getUniqueId();
        if (score.has(u) && !score.finished(u) && way > score.points(u)) {
            score.set(u, way);
        }
    }

    protected void point(Player p, int n) {
        score.add(p.getUniqueId(), n);
        Ui.sound(p, "minecraft:entity.experience_orb.pickup", 1.2f);
        bar(p, "mini.points", Placeholder.unparsed("n", String.valueOf(score.points(p.getUniqueId()))));
    }

    /** Nachricht an alle, die zusehen oder mitspielen. */
    protected void say(String key, TagResolver... r) {
        for (Player p : audience.get()) {
            p.sendMessage(Texts.msg(plugin.lang(p), key, r));
        }
    }

    /** Alle, die zusehen oder mitspielen. */
    protected List<Player> audience() {
        return audience.get();
    }

    protected void titleAll(String key, String subKey, long ms, TagResolver... r) {
        for (Player p : audience.get()) {
            String lang = plugin.lang(p);
            Ui.title(p, Texts.text(lang, key, r), subKey == null ? net.kyori.adventure.text.Component.empty()
                    : Texts.text(lang, subKey, r), ms);
        }
    }

    protected void soundAll(String key, float pitch) {
        audience.get().forEach(p -> Ui.sound(p, key, pitch));
    }

    protected void bar(Player p, String key, TagResolver... r) {
        p.sendActionBar(Texts.text(plugin.lang(p), key, r));
    }

    protected List<Player> alivePlayers() {
        return players.stream().filter(p -> p.isOnline() && score.alive(p.getUniqueId())).toList();
    }

    protected Location at(double x, double y, double z) {
        return new Location(world, ox + x, oy + y, oz + z);
    }

    /** Ort mit Blick auf einen Punkt. */
    protected Location facing(double x, double y, double z, double tx, double ty, double tz) {
        Location l = at(x, y, z);
        double dx = tx - x;
        double dy = ty - y;
        double dz = tz - z;
        l.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
        l.setPitch((float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))));
        return l;
    }

    protected Block block(int x, int y, int z) {
        return world.getBlockAt(ox + x, oy + y, oz + z);
    }

    protected <T extends Entity> T track(T e) {
        spawned.add(e);
        return e;
    }

    protected void untrack(Entity e) {
        spawned.remove(e);
        e.remove();
    }

    /** Bauplan an diese Stelle legen (ohne Physik). */
    protected void place(Plan plan) {
        plan.forEach((k, d) -> world.getBlockAt(ox + Plan.keyX(k), oy + Plan.keyY(k), oz + Plan.keyZ(k))
                .setBlockData(d, false));
    }

    protected static void reset(Player p) {
        p.getInventory().clear();
        p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
        var max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(max == null ? 20 : max.getValue());
        p.setFoodLevel(20);
        p.setSaturation(20);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setGlowing(false);
        p.setVelocity(new org.bukkit.util.Vector());
    }

    protected static void effect(Player p, PotionEffect e) {
        p.addPotionEffect(e);
    }
}
