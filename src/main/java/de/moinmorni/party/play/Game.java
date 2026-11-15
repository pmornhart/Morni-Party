package de.moinmorni.party.play;

import de.moinmorni.party.Items;
import de.moinmorni.party.PartyPlugin;
import de.moinmorni.party.Texts;
import de.moinmorni.party.Ui;
import de.moinmorni.party.board.BoardId;
import de.moinmorni.party.board.BoardLayout;
import de.moinmorni.party.board.Field;
import de.moinmorni.party.board.FieldType;
import de.moinmorni.party.game.Engine;
import de.moinmorni.party.game.GameKind;
import de.moinmorni.party.game.ItemType;
import de.moinmorni.party.game.Mode;
import de.moinmorni.party.game.PartyPlayer;
import de.moinmorni.party.menu.Menu;
import de.moinmorni.party.mini.MiniRun;
import de.moinmorni.party.mini.MiniType;
import de.moinmorni.party.world.Places;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Eine laufende Partie an einem Tisch: Spielbrett, Züge (Würfeln, Ziehen, Felder, Edelweiß-Stand, Laden,
 * Gegenstände, Duell), nach jeder Runde ein Minispiel, am Ende Siegerehrung und Statistik. Die Regeln selbst stecken
 * in {@link Engine}; hier geht es um Anzeige, Menüs, Zeit und Bewegung.
 */
public final class Game {

    public enum Phase {
        INTRO, TURN, ROLLING, MOVING, MENU, LANDING, MINI_INTRO, MINI_COUNTDOWN, MINI, MINI_RESULT, END
    }

    /** Farben der Spielfiguren (Namen in der Seitenleiste). */
    public static final int[] COLOURS = {0xE74C3C, 0x3498DB, 0x2ECC71, 0xF1C40F, 0x9B59B6, 0xE67E22, 0x1ABC9C,
        0xEC87C0};
    private static final double[][] OFFSETS = {{0, 0}, {0.8, 0}, {-0.8, 0}, {0, 0.8}, {0, -0.8}, {0.8, 0.8},
        {-0.8, -0.8}, {0.8, -0.8}};
    private static final int SECOND = 20;
    private static final int ROLL_FRAMES = 8;
    private static final int RESULT_TICKS = 5 * SECOND;
    private static final int END_TICKS = 12 * SECOND;

    private final PartyPlugin plugin;
    private final int table;
    private final BoardId boardId;
    private final BoardLayout layout;
    private final GameKind kind;
    private final Engine engine;
    private final int[] origin;
    private final Random rng = new Random();
    private final Set<UUID> spectators = new LinkedHashSet<>();
    private final List<BukkitTask> tasks = new ArrayList<>();
    private final List<Entity> markers = new ArrayList<>();
    private final List<MiniType> miniOrder = new ArrayList<>();
    private final Map<UUID, Menu> menus = new HashMap<>();
    private Entity standFlower;
    private Entity standLabel;
    private Phase phase = Phase.INTRO;
    private int token;
    private int now;
    private int deadline = -1;
    private Runnable onDeadline;
    private MiniRun mini;
    private MiniType forcedMini;
    private final Map<String, BossBar> miniBars = new HashMap<>();
    private boolean closed;
    private boolean ended;

    public Game(PartyPlugin plugin, int table, BoardId boardId, GameKind kind, List<Player> players) {
        this.plugin = plugin;
        this.table = table;
        this.boardId = boardId;
        this.layout = plugin.layout(boardId);
        this.kind = kind;
        this.origin = Places.board(table, boardId);
        List<Player> order = new ArrayList<>(players);
        Collections.shuffle(order, rng);
        List<PartyPlayer> pps = new ArrayList<>();
        for (int i = 0; i < order.size(); i++) {
            pps.add(new PartyPlayer(order.get(i).getUniqueId(), order.get(i).getName(), i));
        }
        this.engine = new Engine(layout, kind.mode(), kind.rounds(), pps, plugin.settings().rules(), rng);
    }

    // ---------- Zugriff ----------

    public int table() {
        return table;
    }

    public Engine engine() {
        return engine;
    }

    public BoardId board() {
        return boardId;
    }

    public GameKind kind() {
        return kind;
    }

    public Phase phase() {
        return phase;
    }

    public MiniRun mini() {
        return mini;
    }

    public boolean closed() {
        return closed;
    }

    /** Spielt {@code u} hier mit (und ist nicht gegangen)? */
    public boolean plays(UUID u) {
        return engine.player(u).filter(p -> !p.left()).isPresent();
    }

    public boolean watches(UUID u) {
        return spectators.contains(u);
    }

    /** Mitspielende, die gerade da sind. */
    public List<Player> players() {
        List<Player> out = new ArrayList<>();
        for (PartyPlayer pp : engine.active()) {
            Player p = plugin.getServer().getPlayer(pp.uuid());
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    /** Alle, die etwas sehen sollen (Mitspielende und Zuschauer). */
    public List<Player> audience() {
        List<Player> out = players();
        for (UUID u : spectators) {
            Player p = plugin.getServer().getPlayer(u);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    public boolean frozen(Player p) {
        if (!plays(p.getUniqueId())) {
            return false;
        }
        if (phase == Phase.MINI) {
            return mini != null && mini.frozen();
        }
        return true;
    }

    /** Verwaltung/Tests: nächstes Minispiel festlegen. */
    public void forceMini(MiniType t) {
        forcedMini = t;
    }

    // ---------- Zeitplan ----------

    private void later(int ticks, Runnable r) {
        int my = token;
        tasks.add(plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!closed && my == token) {
                r.run();
            }
        }, Math.max(1, ticks)));
    }

    /** Jeden Tick (vom Plugin). */
    public void tick() {
        if (closed) {
            return;
        }
        now++;
        if (now % SECOND == 0) {
            tasks.removeIf(t -> t.isCancelled() || !plugin.getServer().getScheduler().isQueued(t.getTaskId()));
        }
        if (deadline >= 0 && now >= deadline && onDeadline != null) {
            Runnable r = onDeadline;
            onDeadline = null;
            deadline = -1;
            r.run();
        }
        if (phase == Phase.MINI && mini != null) {
            mini.tick();
            if (now % 5 == 0) {
                updateBars();
            }
            if (mini.done()) {
                miniResults();
            }
        }
        if (phase == Phase.TURN && deadline >= 0 && (deadline - now) % SECOND == 0) {
            Player p = online(engine.current());
            int left = (deadline - now) / SECOND;
            if (p != null && left <= 5 && left > 0) {
                p.sendActionBar(Texts.text(plugin.lang(p), "turn.hurry", Placeholder.unparsed("s", String.valueOf(left))));
            }
        }
        if (now % SECOND == 0) {
            refreshSidebar();
        }
    }

    private void timeout(int ticks, Runnable r) {
        deadline = now + ticks;
        onDeadline = r;
    }

    private void clearTimeout() {
        deadline = -1;
        onDeadline = null;
    }

    // ---------- Start ----------

    public void start() {
        spawnMarkers();
        for (PartyPlayer pp : engine.active()) {
            Player p = online(pp);
            if (p == null) {
                continue;
            }
            plugin.lobby().leaveQueue(p.getUniqueId(), false);
            prepareBoardPlayer(p);
            p.teleport(spot(pp));
        }
        String names = String.join(", ", engine.active().stream().map(PartyPlayer::name).toList());
        for (Player p : audience()) {
            String lang = plugin.lang(p);
            Ui.title(p, Texts.text(lang, "board." + boardId.id()), modeLine(lang), 3500);
            p.sendMessage(Texts.msg(lang, "game.start", Placeholder.component("board", Texts.text(lang, "board." + boardId.id())),
                    Placeholder.component("mode", modeLine(lang))));
            p.sendMessage(Texts.msg(lang, "game.goal." + kind.mode().id(),
                    Placeholder.unparsed("n", String.valueOf(engine.rounds())),
                    Placeholder.unparsed("price", String.valueOf(engine.rules().edelweissPrice()))));
            p.sendMessage(Texts.msg(lang, "game.order", Placeholder.unparsed("names", names)));
            Ui.sound(p, "minecraft:ui.toast.challenge_complete", 1.2f);
        }
        refreshSidebar();
        plugin.stateChanged();
        later(5 * SECOND, this::beginTurn);
    }

    private Component modeLine(String lang) {
        return kind.mode() == Mode.GIPFELRENNEN ? Texts.text(lang, "mode.gipfelrennen.line")
                : Texts.text(lang, "mode.edelweiss-jagd.line", Placeholder.unparsed("n", String.valueOf(kind.rounds())));
    }

    private void prepareBoardPlayer(Player p) {
        p.closeInventory();
        p.getInventory().clear();
        p.setGameMode(GameMode.ADVENTURE);
        p.setGlowing(false);
        p.setFallDistance(0);
        p.setFoodLevel(20);
        p.setFlying(false);
        p.setAllowFlight(false);
        giveBoardItems(p);
    }

    // ---------- Markierungen ----------

    private Location fieldTop(Field f) {
        return new Location(plugin.world(), origin[0] + f.x() + 0.5, origin[1] + f.y() + 1, origin[2] + f.z() + 0.5);
    }

    private void spawnMarkers() {
        for (Field f : layout.fields()) {
            String label = switch (f.type()) {
                case GLUECK -> "<green><b>?</b></green>";
                case PECH -> "<dark_gray><b>!</b></dark_gray>";
                case DUELL -> "<gold><b>⚔</b></gold>";
                case LADEN -> "<yellow><b>⌂</b></yellow>";
                case GEGENSTAND -> "<light_purple><b>✦</b></light_purple>";
                case START -> "<white><b>⚑</b></white>";
                case ZIEL -> "<au><b>★</b></au>";
                default -> null;
            };
            if (label == null) {
                continue;
            }
            Location l = fieldTop(f).add(0, 1.9, 0);
            markers.add(plugin.world().spawn(l, TextDisplay.class, d -> {
                d.text(Texts.MM.deserialize(label));
                d.setBillboard(Display.Billboard.CENTER);
                d.setPersistent(false);
                d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(2f, 2f, 2f),
                        new AxisAngle4f()));
                d.setBackgroundColor(Color.fromARGB(0x40000000));
            }));
        }
        placeStand();
    }

    private void placeStand() {
        Location l = fieldTop(layout.field(engine.stand()));
        if (standFlower != null) {
            standFlower.remove();
            standLabel.remove();
        }
        standFlower = plugin.world().spawn(l.clone().add(-0.6, 0, -0.6), BlockDisplay.class, d -> {
            d.setBlock(Material.LILY_OF_THE_VALLEY.createBlockData());
            d.setPersistent(false);
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1.2f, 1.6f, 1.2f),
                    new AxisAngle4f()));
        });
        standLabel = plugin.world().spawn(l.clone().add(0, 2.4, 0), TextDisplay.class, d -> {
            d.text(Texts.MM.deserialize("<white><b>✿</b></white> <au>" + engine.rules().edelweissPrice() + " ●</au>"));
            d.setBillboard(Display.Billboard.CENTER);
            d.setPersistent(false);
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1.8f, 1.8f, 1.8f),
                    new AxisAngle4f()));
        });
    }

    /** Standplatz einer Figur auf ihrem Feld (jede Farbe hat ihren eigenen Fleck), Blick zum nächsten Feld. */
    private Location spot(PartyPlayer pp) {
        Field f = layout.field(pp.pos());
        Field next = layout.field(Math.min(layout.goal(), pp.pos() + 1));
        if (next.index() == f.index() && f.index() > 0) {
            next = layout.field(f.index() - 1);
        }
        double[] off = OFFSETS[pp.colour() % OFFSETS.length];
        Location l = fieldTop(f).add(off[0], 0, off[1]);
        double dx = next.x() - f.x();
        double dz = next.z() - f.z();
        if (next.index() < f.index()) {
            dx = -dx;
            dz = -dz;
        }
        l.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
        l.setPitch(15f);
        return l;
    }

    private Player online(PartyPlayer pp) {
        return pp == null ? null : plugin.getServer().getPlayer(pp.uuid());
    }

    // ---------- Gegenstände in der Schnellleiste ----------

    private void giveBoardItems(Player p) {
        String lang = plugin.lang(p);
        p.getInventory().clear();
        boolean mine = phase == Phase.TURN && engine.current().uuid().equals(p.getUniqueId());
        if (mine) {
            p.getInventory().setItem(0, Items.action(Items.glow(Items.named(Material.WHITE_CONCRETE, 1,
                    Texts.text(lang, "item.dice"), Texts.lines(lang, "item.dice.lore")), true), "roll"));
            PartyPlayer pp = engine.player(p.getUniqueId()).orElseThrow();
            p.getInventory().setItem(1, Items.action(Items.named(Material.BUNDLE, Math.max(1, pp.items().size()),
                    Texts.text(lang, "item.bag", Placeholder.unparsed("n", String.valueOf(pp.items().size()))),
                    Texts.lines(lang, "item.bag.lore")), "bag"));
            p.getInventory().setHeldItemSlot(0);
        }
        p.getInventory().setItem(8, Items.action(Items.named(Material.FILLED_MAP, 1, Texts.text(lang, "item.overview"),
                Texts.lines(lang, "item.overview.lore")), "overview"));
    }

    private void refreshItems() {
        players().forEach(this::giveBoardItems);
    }

    /** Knopf in der Schnellleiste benutzt. */
    public void use(Player p, String action) {
        switch (action) {
            case "roll" -> {
                if (phase == Phase.TURN && engine.current().uuid().equals(p.getUniqueId())) {
                    roll();
                }
            }
            case "bag" -> openBag(p);
            case "overview" -> openOverview(p);
            default -> {
            }
        }
    }

    // ---------- Zug ----------

    private void beginTurn() {
        if (engine.over()) {
            end();
            return;
        }
        PartyPlayer cur = engine.current();
        Player p = online(cur);
        if (cur.left() || p == null) {
            finishTurn();
            return;
        }
        phase = Phase.TURN;
        refreshItems();
        for (Player a : audience()) {
            String lang = plugin.lang(a);
            if (a.equals(p)) {
                Ui.title(a, Texts.text(lang, "turn.you"), Texts.text(lang, "turn.you.sub"), 1800);
                Ui.sound(a, "minecraft:block.note_block.chime", 1.2f);
            } else {
                a.sendActionBar(Texts.text(lang, "turn.other", Placeholder.unparsed("name", cur.name())));
            }
            a.sendMessage(Texts.msg(lang, "turn.start", Placeholder.unparsed("name", cur.name()),
                    Placeholder.unparsed("round", String.valueOf(engine.round())),
                    Placeholder.unparsed("rounds", String.valueOf(engine.rounds()))));
        }
        refreshSidebar();
        timeout(plugin.settings().turnSeconds() * SECOND, () -> {
            Player q = online(engine.current());
            if (q != null) {
                q.sendMessage(Texts.msg(plugin.lang(q), "turn.auto"));
            }
            roll();
        });
    }

    private void roll() {
        if (phase != Phase.TURN) {
            return;
        }
        clearTimeout();
        closeMenus();
        PartyPlayer cur = engine.current();
        Player p = online(cur);
        phase = Phase.ROLLING;
        refreshItems();
        Engine.Roll roll = engine.roll(cur);
        for (int i = 0; i < ROLL_FRAMES; i++) {
            int frame = i;
            later(1 + i * 2, () -> {
                int fake = 1 + rng.nextInt(roll.dice().length * Engine.DIE);
                for (Player a : audience()) {
                    if (a.equals(p)) {
                        Ui.title(a, Texts.MM.deserialize("<gray>" + fake + "</gray>"), Component.empty(), 300);
                    }
                    if (frame % 2 == 0) {
                        Ui.sound(a, "minecraft:block.wooden_button.click_on", 1.4f + frame * 0.05f);
                    }
                }
            });
        }
        later(2 + ROLL_FRAMES * 2, () -> {
            String shown = roll.dice().length == 1 ? String.valueOf(roll.total())
                    : roll.dice()[0] + " + " + roll.dice()[1] + " = " + roll.total();
            for (Player a : audience()) {
                String lang = plugin.lang(a);
                if (a.equals(p)) {
                    Ui.title(a, Texts.MM.deserialize("<au><b>" + shown + "</b></au>"),
                            Texts.text(lang, "roll.sub", Placeholder.unparsed("n", String.valueOf(roll.total()))), 1200);
                }
                Ui.sound(a, "minecraft:entity.experience_orb.pickup", 0.8f);
                a.sendMessage(Texts.msg(lang, "roll.done", Placeholder.unparsed("name", cur.name()),
                        Placeholder.unparsed("dice", shown)));
            }
            later(SECOND, () -> move(cur, roll.total(), true, () -> land(cur)));
        });
    }

    /** Figur Feld für Feld weiterziehen. Am Edelweiß-Stand wird angehalten (wenn {@code stand}). */
    private void move(PartyPlayer pp, int steps, boolean stand, Runnable after) {
        phase = Phase.MOVING;
        if (steps <= 0) {
            after.run();
            return;
        }
        Engine.Step s = engine.step(pp);
        Player p = online(pp);
        if (p != null) {
            p.teleport(spot(pp));
            Ui.sound(p, "minecraft:block.note_block.hat", 1.0f + (steps % 6) * 0.1f);
        }
        if (s.wrapped()) {
            say("move.cablecar", Placeholder.unparsed("name", pp.name()),
                    Placeholder.unparsed("n", String.valueOf(engine.rules().summitBonus())));
        }
        if (s.goal() && kind.mode() == Mode.GIPFELRENNEN) {
            reachedGoal(pp);
            return;
        }
        Runnable next = () -> later(plugin.settings().stepTicks(), () -> move(pp, steps - 1, stand, after));
        if (s.stand() && stand) {
            later(plugin.settings().stepTicks(), () -> offerStand(pp, next));
        } else {
            next.run();
        }
    }

    private void moveBack(PartyPlayer pp, int steps, Runnable after) {
        phase = Phase.MOVING;
        if (steps <= 0) {
            after.run();
            return;
        }
        engine.stepBack(pp);
        Player p = online(pp);
        if (p != null) {
            p.teleport(spot(pp));
            Ui.sound(p, "minecraft:block.note_block.bass", 1.0f);
        }
        later(plugin.settings().stepTicks(), () -> moveBack(pp, steps - 1, after));
    }

    // ---------- Edelweiß-Stand ----------

    private void offerStand(PartyPlayer pp, Runnable cont) {
        Player p = online(pp);
        int price = engine.rules().edelweissPrice();
        if (p == null) {
            cont.run();
            return;
        }
        String lang = plugin.lang(p);
        if (!engine.canBuyEdelweiss(pp)) {
            p.sendMessage(Texts.msg(lang, "stand.poor", Placeholder.unparsed("price", String.valueOf(price)),
                    Placeholder.unparsed("taler", String.valueOf(pp.taler()))));
            later(SECOND, cont);
            return;
        }
        phase = Phase.MENU;
        Menu m = new Menu(3, Texts.text(lang, "stand.title"));
        fill(m, 27);
        m.set(11, Items.glow(Items.named(Material.LILY_OF_THE_VALLEY, 1, Texts.text(lang, "stand.buy",
                Placeholder.unparsed("price", String.valueOf(price))), Texts.lines(lang, "stand.buy.lore",
                        Placeholder.unparsed("taler", String.valueOf(pp.taler())))), true),
                who -> standChoice(pp, true, cont));
        m.set(15, Items.named(Material.BARRIER, 1, Texts.text(lang, "stand.skip"), null),
                who -> standChoice(pp, false, cont));
        m.onClose(who -> standChoice(pp, false, cont));
        openTimed(p, m, () -> standChoice(pp, false, cont));
        Ui.sound(p, "minecraft:block.note_block.bell", 1.6f);
    }

    private void standChoice(PartyPlayer pp, boolean buy, Runnable cont) {
        if (phase != Phase.MENU) {
            return;
        }
        phase = Phase.MOVING;
        clearTimeout();
        closeMenus();
        if (buy && engine.buyEdelweiss(pp)) {
            Player p = online(pp);
            for (Player a : audience()) {
                String lang = plugin.lang(a);
                a.sendMessage(Texts.msg(lang, "stand.bought", Placeholder.unparsed("name", pp.name()),
                        Placeholder.unparsed("n", String.valueOf(pp.edelweiss()))));
                Ui.sound(a, "minecraft:ui.toast.challenge_complete", 1.5f);
            }
            if (p != null) {
                Ui.title(p, Texts.text(plugin.lang(p), "stand.bought.title"), Texts.text(plugin.lang(p), "stand.moved"),
                        1800);
                firework(p.getLocation(), Color.WHITE);
            }
            placeStand();
            say("stand.moves", Placeholder.unparsed("field", String.valueOf(engine.stand())));
            refreshSidebar();
            later(SECOND + SECOND / 2, cont);
        } else {
            later(5, cont);
        }
    }

    // ---------- Felder ----------

    private void land(PartyPlayer pp) {
        phase = Phase.LANDING;
        Engine.Outcome o = engine.land(pp);
        Player p = online(pp);
        String field = o.field().key();
        switch (o.kind()) {
            case TALER -> {
                String key = o.blocked() ? "land.blocked" : o.taler() >= 0 ? "land.plus" : "land.minus";
                say(key, Placeholder.unparsed("name", pp.name()), Placeholder.unparsed("n", String.valueOf(Math.abs(o.taler()))));
                if (p != null) {
                    Ui.sound(p, o.taler() >= 0 ? "minecraft:entity.experience_orb.pickup" : "minecraft:entity.item.break",
                            1f);
                }
                laterFinish();
            }
            case ITEM -> {
                sayItem("land.item", pp, o.item());
                laterFinish();
            }
            case LUCK -> {
                luckMessage(pp, o);
                if (o.steps() > 0 && !o.blocked()) {
                    later(SECOND + SECOND / 2, () -> move(pp, o.steps(), false, this::finishTurn));
                } else if (o.steps() < 0 && !o.blocked()) {
                    later(SECOND + SECOND / 2, () -> moveBack(pp, -o.steps(), this::finishTurn));
                } else {
                    laterFinish();
                }
            }
            case DUEL -> chooseDuel(pp);
            case SHOP -> openShop(pp);
            case GOAL -> {
                say("land.summit", Placeholder.unparsed("name", pp.name()));
                laterFinish();
            }
            case NONE -> {
                if (o.field() == FieldType.DUELL) {
                    say("duel.alone");
                }
                laterFinish();
            }
        }
        if (p != null && o.kind() != Engine.Kind.SHOP && o.kind() != Engine.Kind.DUEL) {
            p.sendActionBar(Texts.text(plugin.lang(p), field));
        }
        refreshSidebar();
    }

    private void luckMessage(PartyPlayer pp, Engine.Outcome o) {
        for (Player a : audience()) {
            String lang = plugin.lang(a);
            Component item = o.item() == null ? Component.empty() : Texts.text(lang, o.item().key());
            String key = o.blocked() ? "luck.blocked" : o.luck().key();
            a.sendMessage(Texts.msg(lang, key, Placeholder.unparsed("name", pp.name()),
                    Placeholder.unparsed("n", String.valueOf(Math.abs(o.taler()))),
                    Placeholder.unparsed("steps", String.valueOf(Math.abs(o.steps()))),
                    Placeholder.component("item", item)));
            Ui.sound(a, o.luck().good() ? "minecraft:entity.player.levelup" : "minecraft:entity.wither.ambient",
                    o.luck().good() ? 1.4f : 1.8f);
        }
    }

    private void laterFinish() {
        later(SECOND + SECOND / 2, this::finishTurn);
    }

    private void finishTurn() {
        clearTimeout();
        closeMenus();
        if (ended) {
            return;
        }
        if (engine.over()) {
            end();
            return;
        }
        boolean roundOver = engine.endTurn();
        refreshSidebar();
        if (roundOver) {
            startMinigame();
        } else {
            beginTurn();
        }
    }

    // ---------- Würfelduell ----------

    private void chooseDuel(PartyPlayer pp) {
        Player p = online(pp);
        List<PartyPlayer> others = engine.active().stream().filter(o -> o != pp).toList();
        if (p == null || others.isEmpty()) {
            laterFinish();
            return;
        }
        phase = Phase.MENU;
        String lang = plugin.lang(p);
        Menu m = new Menu(1, Texts.text(lang, "duel.title"));
        for (int i = 0; i < others.size() && i < 9; i++) {
            PartyPlayer o = others.get(i);
            m.set(i, head(o, Texts.text(lang, "duel.pick", Placeholder.unparsed("name", o.name())),
                    Texts.lines(lang, "duel.pick.lore", Placeholder.unparsed("taler", String.valueOf(o.taler())))),
                    who -> duel(pp, o));
        }
        Runnable random = () -> duel(pp, others.get(rng.nextInt(others.size())));
        m.onClose(who -> random.run());
        openTimed(p, m, random);
        say("duel.field", Placeholder.unparsed("name", pp.name()),
                Placeholder.unparsed("n", String.valueOf(engine.rules().duelStake())));
    }

    private void duel(PartyPlayer a, PartyPlayer b) {
        if (phase != Phase.MENU) {
            return;
        }
        phase = Phase.LANDING;
        clearTimeout();
        closeMenus();
        Engine.Duel d = engine.duel(a, b);
        say("duel.vs", Placeholder.unparsed("one", a.name()), Placeholder.unparsed("two", b.name()));
        later(SECOND, () -> {
            for (Player x : audience()) {
                Ui.title(x, Texts.MM.deserialize("<au>" + d.challengerRoll() + "</au> <gray>:</gray> <au>"
                        + d.opponentRoll() + "</au>"), Texts.MM.deserialize("<white>" + a.name() + "</white> <gray>⚔</gray> <white>"
                        + b.name() + "</white>"), 1800);
                Ui.sound(x, "minecraft:entity.player.attack.strong", 1f);
            }
            if (d.winner() == null) {
                say("duel.draw");
            } else {
                String winner = d.winner().equals(a.uuid()) ? a.name() : b.name();
                String loser = d.winner().equals(a.uuid()) ? b.name() : a.name();
                say(d.blocked() ? "duel.blocked" : "duel.won", Placeholder.unparsed("winner", winner),
                        Placeholder.unparsed("loser", loser), Placeholder.unparsed("n", String.valueOf(d.taler())));
            }
            refreshSidebar();
            later(2 * SECOND, this::finishTurn);
        });
    }

    // ---------- Laden ----------

    private void openShop(PartyPlayer pp) {
        Player p = online(pp);
        if (p == null) {
            laterFinish();
            return;
        }
        phase = Phase.MENU;
        say("shop.enter", Placeholder.unparsed("name", pp.name()));
        Menu m = new Menu(3, Texts.text(plugin.lang(p), "shop.title"));
        drawShop(p, pp, m);
        Runnable done = () -> {
            if (phase == Phase.MENU) {
                phase = Phase.LANDING;
                clearTimeout();
                closeMenus();
                laterFinish();
            }
        };
        m.onClose(who -> done.run());
        openTimed(p, m, done);
        menus.put(p.getUniqueId(), m);
    }

    private void drawShop(Player p, PartyPlayer pp, Menu m) {
        String lang = plugin.lang(p);
        m.clear();
        fill(m, 27);
        ItemType[] all = ItemType.values();
        for (int i = 0; i < all.length; i++) {
            ItemType t = all[i];
            boolean can = engine.canBuyItem(pp, t);
            List<Component> lore = new ArrayList<>(Texts.lines(lang, t.key() + ".desc"));
            lore.add(Texts.text(lang, can ? "shop.price" : "shop.cannot", Placeholder.unparsed("price", String.valueOf(t.price()))));
            m.set(10 + i, Items.named(icon(t), 1, Texts.text(lang, t.key()), lore), who -> {
                if (engine.buyItem(pp, t)) {
                    Ui.sound(who, "minecraft:entity.villager.yes", 1.2f);
                    who.sendMessage(Texts.msg(lang, "shop.bought", Placeholder.component("item", Texts.text(lang, t.key())),
                            Placeholder.unparsed("taler", String.valueOf(pp.taler()))));
                    drawShop(who, pp, m);
                    refreshSidebar();
                } else {
                    Ui.sound(who, "minecraft:entity.villager.no", 1f);
                }
            });
        }
        m.set(22, Items.named(Material.OAK_DOOR, 1, Texts.text(lang, "shop.leave"),
                Texts.lines(lang, "shop.wallet", Placeholder.unparsed("taler", String.valueOf(pp.taler())),
                        Placeholder.unparsed("n", String.valueOf(pp.items().size())),
                        Placeholder.unparsed("max", String.valueOf(engine.rules().maxItems())))), who -> who.closeInventory());
    }

    public static Material icon(ItemType t) {
        return switch (t) {
            case DOPPELWUERFEL -> Material.QUARTZ_BLOCK;
            case ZIELWUERFEL -> Material.TARGET;
            case TAUSCH -> Material.ENDER_PEARL;
            case TALERKLAU -> Material.GOLD_NUGGET;
            case SCHILD -> Material.SHIELD;
            case LOCKRUF -> Material.GOAT_HORN;
        };
    }

    // ---------- Rucksack (Gegenstände einsetzen) ----------

    private void openBag(Player p) {
        PartyPlayer pp = engine.player(p.getUniqueId()).orElse(null);
        if (pp == null) {
            return;
        }
        String lang = plugin.lang(p);
        boolean mine = phase == Phase.TURN && engine.current() == pp;
        Menu m = new Menu(1, Texts.text(lang, "bag.title"));
        List<ItemType> items = pp.items();
        if (items.isEmpty()) {
            m.set(4, Items.named(Material.BUNDLE, 1, Texts.text(lang, "bag.empty"), Texts.lines(lang, "bag.empty.lore")), null);
        }
        for (int i = 0; i < items.size(); i++) {
            ItemType t = items.get(i);
            List<Component> lore = new ArrayList<>(Texts.lines(lang, t.key() + ".desc"));
            lore.add(Texts.text(lang, !mine ? "bag.notnow" : pp.itemUsed() ? "bag.used" : "bag.click"));
            m.set(2 + i * 2, Items.named(icon(t), 1, Texts.text(lang, t.key()), lore), who -> {
                if (phase != Phase.TURN || engine.current() != pp || pp.itemUsed()) {
                    return;
                }
                m.closeQuietly(who);
                useItem(who, pp, t);
            });
        }
        m.open(p);
    }

    private void useItem(Player p, PartyPlayer pp, ItemType t) {
        String lang = plugin.lang(p);
        if (t == ItemType.ZIELWUERFEL) {
            Menu m = new Menu(1, Texts.text(lang, "bag.number"));
            for (int n = 1; n <= Engine.DIE; n++) {
                int v = n;
                m.set(1 + n, Items.named(Material.WHITE_CONCRETE, n, Texts.MM.deserialize("<au><b>" + n + "</b></au>"), null),
                        who -> {
                            m.closeQuietly(who);
                            applyItem(who, pp, t, null, v);
                        });
            }
            m.open(p);
            return;
        }
        if (t.needsTarget()) {
            List<PartyPlayer> others = engine.active().stream().filter(o -> o != pp).toList();
            Menu m = new Menu(1, Texts.text(lang, "bag.target"));
            for (int i = 0; i < others.size() && i < 9; i++) {
                PartyPlayer o = others.get(i);
                m.set(i, head(o, Texts.MM.deserialize("<white>" + o.name() + "</white>"), Texts.lines(lang, "bag.target.lore",
                        Placeholder.unparsed("field", String.valueOf(o.pos())),
                        Placeholder.unparsed("taler", String.valueOf(o.taler())))), who -> {
                            m.closeQuietly(who);
                            applyItem(who, pp, t, o.uuid(), 0);
                        });
            }
            m.open(p);
            return;
        }
        applyItem(p, pp, t, null, 0);
    }

    private void applyItem(Player p, PartyPlayer pp, ItemType t, UUID target, int number) {
        if (phase != Phase.TURN || engine.current() != pp) {
            return;
        }
        Engine.ItemUse u = engine.useItem(pp, t, target, number);
        String lang = plugin.lang(p);
        if (!u.ok()) {
            p.sendMessage(Texts.msg(lang, "bag.problem." + u.problem()));
            return;
        }
        String other = u.target() == null ? "" : engine.player(u.target()).map(PartyPlayer::name).orElse("?");
        for (Player a : audience()) {
            String l = plugin.lang(a);
            a.sendMessage(Texts.msg(l, u.blocked() ? "use.blocked" : "use." + t.id(),
                    Placeholder.unparsed("name", pp.name()), Placeholder.unparsed("other", other),
                    Placeholder.unparsed("n", String.valueOf(u.amount())),
                    Placeholder.component("item", Texts.text(l, t.key()))));
        }
        Ui.sound(p, "minecraft:item.armor.equip_gold", 1.2f);
        if (t == ItemType.TAUSCH || t == ItemType.LOCKRUF) {
            for (PartyPlayer x : engine.active()) {
                Player q = online(x);
                if (q != null) {
                    q.teleport(spot(x));
                }
            }
        }
        refreshItems();
        refreshSidebar();
    }

    // ---------- Übersicht ----------

    private void openOverview(Player p) {
        String lang = plugin.lang(p);
        Menu m = new Menu(2, Texts.text(lang, "overview.title"));
        Map<UUID, Integer> places = engine.placeOf();
        List<PartyPlayer> all = engine.active();
        for (int i = 0; i < all.size() && i < 9; i++) {
            PartyPlayer o = all.get(i);
            List<Component> lore = new ArrayList<>(Texts.lines(lang, "overview.player",
                    Placeholder.unparsed("place", String.valueOf(places.getOrDefault(o.uuid(), 0))),
                    Placeholder.unparsed("field", String.valueOf(o.pos())),
                    Placeholder.unparsed("goal", String.valueOf(layout.goal())),
                    Placeholder.unparsed("taler", String.valueOf(o.taler())),
                    Placeholder.unparsed("edelweiss", String.valueOf(o.edelweiss())),
                    Placeholder.unparsed("minis", String.valueOf(o.minigamesWon()))));
            for (ItemType t : o.items()) {
                lore.add(Texts.MM.deserialize("<gray>• </gray>").append(Texts.text(lang, t.key())));
            }
            if (o.shield()) {
                lore.add(Texts.text(lang, "overview.shield"));
            }
            m.set(i, head(o, Texts.MM.deserialize("<white>" + o.name() + "</white>"), lore), null);
        }
        m.set(13, Items.named(Material.LILY_OF_THE_VALLEY, 1, Texts.text(lang, "overview.stand",
                Placeholder.unparsed("field", String.valueOf(engine.stand())),
                Placeholder.unparsed("price", String.valueOf(engine.rules().edelweissPrice()))), null), null);
        m.open(p);
    }

    private ItemStack head(PartyPlayer o, Component name, List<Component> lore) {
        ItemStack head = Items.named(Material.PLAYER_HEAD, 1, name, lore);
        head.editMeta(org.bukkit.inventory.meta.SkullMeta.class,
                meta -> meta.setOwningPlayer(plugin.getServer().getOfflinePlayer(o.uuid())));
        return head;
    }

    private static void fill(Menu m, int size) {
        for (int i = 0; i < size; i++) {
            m.set(i, Items.pane(Material.GREEN_STAINED_GLASS_PANE), null);
        }
    }

    private void openTimed(Player p, Menu m, Runnable onTimeout) {
        menus.put(p.getUniqueId(), m);
        m.open(p);
        timeout(plugin.settings().menuSeconds() * SECOND, onTimeout);
    }

    private void closeMenus() {
        for (var e : menus.entrySet()) {
            Player p = plugin.getServer().getPlayer(e.getKey());
            if (p != null) {
                e.getValue().closeQuietly(p);
            }
        }
        menus.clear();
    }

    // ---------- Minispiel ----------

    private MiniType nextMini() {
        if (forcedMini != null) {
            MiniType t = forcedMini;
            forcedMini = null;
            return t;
        }
        if (miniOrder.isEmpty()) {
            miniOrder.addAll(plugin.settings().minigames());
            if (!plugin.settings().fixedOrder()) {
                Collections.shuffle(miniOrder, rng);
            }
        }
        return miniOrder.removeFirst();
    }

    private void startMinigame() {
        phase = Phase.MINI_INTRO;
        MiniType type = nextMini();
        int[] o = Places.mini(table, type);
        List<Player> ps = players();
        mini = type.create(new MiniRun.Setup(plugin, type, plugin.world(), o[0], o[1], o[2], ps, this::audience, rng,
                plugin.settings().miniFactor()));
        for (Player a : audience()) {
            String lang = plugin.lang(a);
            Ui.title(a, Texts.text(lang, "mini.title"), Texts.text(lang, type.key()), 2500);
            a.sendMessage(Texts.msg(lang, "mini.announce", Placeholder.component("name", Texts.text(lang, type.key()))));
            a.sendMessage(Texts.msg(lang, type.key() + ".desc"));
            Ui.sound(a, "minecraft:block.note_block.flute", 1.2f);
            a.getInventory().clear();
            a.closeInventory();
        }
        later(3 * SECOND, () -> {
            mini.prepare();
            for (UUID u : spectators) {
                Player s = plugin.getServer().getPlayer(u);
                if (s != null) {
                    s.teleport(mini.viewPoint());
                }
            }
            phase = Phase.MINI_COUNTDOWN;
            for (int i = 3; i >= 1; i--) {
                int n = i;
                later((3 - i) * SECOND, () -> audience().forEach(a -> {
                    Ui.title(a, Texts.MM.deserialize("<au><b>" + n + "</b></au>"), Component.empty(), 700);
                    Ui.sound(a, "minecraft:block.note_block.pling", 1f);
                }));
            }
            later(3 * SECOND, () -> {
                audience().forEach(a -> {
                    Ui.title(a, Texts.text(plugin.lang(a), "mini.go"), Component.empty(), 700);
                    Ui.sound(a, "minecraft:block.note_block.pling", 2f);
                });
                phase = Phase.MINI;
                mini.start();
                showBars();
            });
        });
    }

    private BossBar bar(String lang) {
        return miniBars.computeIfAbsent(lang, l -> BossBar.bossBar(Component.empty(), 1f, BossBar.Color.GREEN,
                BossBar.Overlay.PROGRESS));
    }

    private void showBars() {
        updateBars();
        audience().forEach(a -> a.showBossBar(bar(plugin.lang(a))));
    }

    private void updateBars() {
        if (mini == null) {
            return;
        }
        float progress = Math.max(0f, Math.min(1f, (float) (mini.durationTicks() - mini.elapsed())
                / Math.max(1, mini.durationTicks())));
        for (String lang : List.of("de", "en")) {
            BossBar b = bar(lang);
            b.progress(progress);
            b.name(Texts.text(lang, "mini.bar", Placeholder.component("name", Texts.text(lang, mini.type().key())),
                    Placeholder.unparsed("s", Ui.clock(mini.secondsLeft()))));
        }
    }

    private void hideBars() {
        for (BossBar b : miniBars.values()) {
            plugin.getServer().getOnlinePlayers().forEach(p -> p.hideBossBar(b));
        }
        miniBars.clear();
    }

    private void miniResults() {
        phase = Phase.MINI_RESULT;
        MiniRun run = mini;
        hideBars();
        List<List<UUID>> places = run.score().places();
        Map<UUID, Integer> got = engine.awardMinigame(places);
        List<UUID> winners = places.isEmpty() ? List.of() : places.getFirst();
        for (Player a : audience()) {
            String lang = plugin.lang(a);
            a.sendMessage(Texts.msg(lang, "mini.results", Placeholder.component("name", Texts.text(lang, run.type().key()))));
            int place = 1;
            for (List<UUID> g : places) {
                for (UUID u : g) {
                    String name = engine.player(u).map(PartyPlayer::name).orElse("?");
                    String key = run.type().kind() == de.moinmorni.party.mini.Scoring.Kind.POINTS ? "mini.line.points"
                            : "mini.line";
                    a.sendMessage(Texts.text(lang, key, Placeholder.unparsed("pos", String.valueOf(place)),
                            Placeholder.unparsed("name", name), Placeholder.unparsed("points", String.valueOf(run.score().points(u))),
                            Placeholder.unparsed("taler", String.valueOf(got.getOrDefault(u, 0)))));
                }
                place += g.size();
            }
            boolean won = winners.contains(a.getUniqueId());
            if (plays(a.getUniqueId())) {
                Ui.title(a, Texts.text(lang, won ? "mini.won" : "mini.over"), Texts.text(lang, "mini.got",
                        Placeholder.unparsed("n", String.valueOf(got.getOrDefault(a.getUniqueId(), 0)))), 2500);
                Ui.sound(a, won ? "minecraft:ui.toast.challenge_complete" : "minecraft:entity.experience_orb.pickup", 1f);
            }
        }
        later(RESULT_TICKS, () -> {
            mini = null;
            backToBoard();
            engine.nextRound();
            if (engine.over()) {
                end();
                return;
            }
            say("round.next", Placeholder.unparsed("round", String.valueOf(engine.round())),
                    Placeholder.unparsed("rounds", String.valueOf(engine.rounds())));
            later(2 * SECOND, this::beginTurn);
        });
    }

    private void backToBoard() {
        for (PartyPlayer pp : engine.active()) {
            Player p = online(pp);
            if (p != null) {
                prepareBoardPlayer(p);
                p.teleport(spot(pp));
            }
        }
        Location view = viewPoint();
        for (UUID u : spectators) {
            Player s = plugin.getServer().getPlayer(u);
            if (s != null) {
                s.teleport(view);
            }
        }
        refreshSidebar();
    }

    /** Blick aufs ganze Brett (für Zuschauer). */
    public Location viewPoint() {
        double r = layout.shape().outer() + 12;
        double a = layout.shape().angle() - 0.4;
        Location l = new Location(plugin.world(), origin[0] + Math.cos(a) * r, origin[1] + layout.shape().height() + 14,
                origin[2] + Math.sin(a) * r);
        l.setYaw((float) Math.toDegrees(Math.atan2(Math.cos(a) * r, -Math.sin(a) * r)));
        l.setPitch(30f);
        return l;
    }

    // ---------- Ende ----------

    private void reachedGoal(PartyPlayer pp) {
        phase = Phase.END;
        Player p = online(pp);
        if (p != null) {
            for (int i = 0; i < 3; i++) {
                later(i * 10, () -> firework(p.getLocation(), Color.YELLOW));
            }
        }
        for (Player a : audience()) {
            String lang = plugin.lang(a);
            a.sendMessage(Texts.msg(lang, "goal.reached", Placeholder.unparsed("name", pp.name()),
                    Placeholder.component("goal", Texts.text(lang, "goal." + boardId.id()))));
        }
        later(2 * SECOND, this::end);
    }

    /** Spiel vorbei: Siegerehrung, Statistik, nach ein paar Sekunden zurück auf den Festplatz. */
    public void end() {
        if (ended || closed) {
            return;
        }
        ended = true;
        token++;
        phase = Phase.END;
        clearTimeout();
        closeMenus();
        if (mini != null && !mini.done()) {
            mini.finish();
        }
        hideBars();
        List<List<PartyPlayer>> places = engine.places();
        List<PartyPlayer> winners = places.isEmpty() ? List.of() : places.getFirst();
        String names = String.join(", ", winners.stream().map(PartyPlayer::name).toList());
        for (Player a : audience()) {
            String lang = plugin.lang(a);
            a.sendMessage(Texts.msg(lang, "end.header"));
            int place = 1;
            for (List<PartyPlayer> g : places) {
                for (PartyPlayer pp : g) {
                    a.sendMessage(Texts.text(lang, "end.line", Placeholder.unparsed("pos", String.valueOf(place)),
                            Placeholder.unparsed("name", pp.name()), Placeholder.unparsed("edelweiss", String.valueOf(pp.edelweiss())),
                            Placeholder.unparsed("taler", String.valueOf(pp.taler())),
                            Placeholder.unparsed("field", String.valueOf(pp.pos()))));
                }
                place += g.size();
            }
            boolean won = winners.stream().anyMatch(w -> w.uuid().equals(a.getUniqueId()));
            Ui.title(a, Texts.text(lang, won ? "end.won" : "end.title"),
                    Texts.text(lang, "end.winner", Placeholder.unparsed("names", names)), 4000);
            Ui.sound(a, won ? "minecraft:ui.toast.challenge_complete" : "minecraft:entity.player.levelup", 1f);
        }
        // Statistik: nur wer bis zum Schluss dabei war (und mindestens zu zweit gespielt hat)
        boolean counts = engine.players().size() >= 2;
        for (PartyPlayer pp : engine.active()) {
            boolean won = winners.contains(pp);
            boolean summit = kind.mode() == Mode.GIPFELRENNEN && pp.finished() > 0;
            if (counts) {
                plugin.store().record(pp.uuid(), pp.name(), won, pp.edelweiss(), pp.minigamesWon(), summit);
                Map<String, Long> add = new HashMap<>();
                add.put("games", 1L);
                add.put("wins", won ? 1L : 0L);
                add.put("edelweiss", (long) pp.edelweiss());
                add.put("minigames_won", (long) pp.minigamesWon());
                add.put("games_" + kind.mode().id().replace("-", "_"), 1L);
                if (summit) {
                    add.put("summits", 1L);
                }
                plugin.net().stats(pp.uuid(), add, Map.of("edelweiss_best", (long) pp.edelweiss()));
            }
            Player p = online(pp);
            if (won && p != null) {
                for (int i = 0; i < 4; i++) {
                    int n = i;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (p.isOnline()) {
                            firework(p.getLocation(), n % 2 == 0 ? Color.YELLOW : Color.WHITE);
                        }
                    }, 1L + i * 12L);
                }
            }
        }
        refreshSidebar();
        plugin.getServer().getScheduler().runTaskLater(plugin, this::close, END_TICKS);
    }

    /** Aufräumen und alle zurück auf den Festplatz. */
    public void close() {
        if (closed) {
            return;
        }
        if (!ended) {
            ended = true;
            if (mini != null && !mini.done()) {
                mini.finish();
            }
        }
        closed = true;
        token++;
        closeMenus();
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
        markers.forEach(Entity::remove);
        markers.clear();
        if (standFlower != null) {
            standFlower.remove();
            standLabel.remove();
        }
        hideBars();
        List<Player> back = audience();
        spectators.clear();
        plugin.games().remove(this);
        for (Player p : back) {
            plugin.lobby().enterHub(p, true);
        }
        plugin.stateChanged();
    }

    // ---------- Gehen und Zuschauen ----------

    /** Jemand verlässt die Partie (Verbindung weg oder /verlassen). */
    public void leave(Player p, boolean online) {
        UUID u = p.getUniqueId();
        if (spectators.remove(u)) {
            plugin.sidebar().hide(p);
            miniBars.values().forEach(p::hideBossBar);
            return;
        }
        if (!plays(u)) {
            return;
        }
        boolean current = engine.current().uuid().equals(u);
        engine.remove(u);
        if (menus.containsKey(u)) {
            menus.remove(u).closeQuietly(p);
        }
        if (mini != null && !mini.done()) {
            mini.leave(p);
        }
        miniBars.values().forEach(p::hideBossBar);
        plugin.sidebar().hide(p);
        say("game.left", Placeholder.unparsed("name", p.getName()));
        plugin.stateChanged();
        if (ended) {
            return;
        }
        if (engine.over()) {
            later(SECOND, this::end);
            return;
        }
        if (current && phase != Phase.MINI_INTRO && phase != Phase.MINI_COUNTDOWN && phase != Phase.MINI
                && phase != Phase.MINI_RESULT) {
            // Der Zug der gegangenen Person endet sofort.
            token++;
            clearTimeout();
            closeMenus();
            later(SECOND, this::finishTurn);
        }
    }

    public void watch(Player p) {
        spectators.add(p.getUniqueId());
        p.getInventory().clear();
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(mini != null && phase != Phase.MINI_RESULT ? mini.viewPoint() : viewPoint());
        if (!miniBars.isEmpty()) {
            showBars();
        }
        p.sendMessage(Texts.msg(plugin.lang(p), "spectate.start"));
        refreshSidebar();
    }

    // ---------- Seitenleiste ----------

    public void refreshSidebar() {
        if (closed) {
            return;
        }
        Map<UUID, Integer> places = engine.placeOf();
        List<PartyPlayer> sorted = new ArrayList<>(engine.active());
        sorted.sort((x, y) -> Integer.compare(places.getOrDefault(x.uuid(), 99), places.getOrDefault(y.uuid(), 99)));
        for (Player a : audience()) {
            String lang = plugin.lang(a);
            List<Component> lines = new ArrayList<>();
            lines.add(Texts.text(lang, "board." + boardId.id() + ".short"));
            lines.add(kind.mode() == Mode.GIPFELRENNEN
                    ? Texts.text(lang, "side.race", Placeholder.unparsed("round", String.valueOf(engine.round())))
                    : Texts.text(lang, "side.round", Placeholder.unparsed("round", String.valueOf(Math.min(engine.round(),
                            engine.rounds()))), Placeholder.unparsed("rounds", String.valueOf(engine.rounds()))));
            lines.add(Component.empty());
            for (PartyPlayer pp : sorted) {
                boolean cur = !ended && engine.current() == pp && phase.ordinal() <= Phase.LANDING.ordinal();
                Component name = Component.text(pp.name(), TextColor.color(COLOURS[pp.colour() % COLOURS.length]));
                lines.add(Texts.text(lang, kind.mode() == Mode.GIPFELRENNEN ? "side.player.race" : "side.player",
                        Placeholder.unparsed("pos", String.valueOf(places.getOrDefault(pp.uuid(), 0))),
                        Placeholder.unparsed("turn", cur ? "▶" : " "),
                        Placeholder.component("name", name),
                        Placeholder.unparsed("edelweiss", String.valueOf(pp.edelweiss())),
                        Placeholder.unparsed("taler", String.valueOf(pp.taler())),
                        Placeholder.unparsed("field", String.valueOf(pp.pos()))));
            }
            lines.add(Component.text(" "));
            lines.add(Texts.text(lang, "side.stand", Placeholder.unparsed("field", String.valueOf(engine.stand()))));
            lines.add(Texts.text(lang, "side.address"));
            plugin.sidebar().show(a, Texts.text(lang, "side.title"), lines);
        }
    }

    // ---------- Kleinkram ----------

    private void say(String key, TagResolver... r) {
        for (Player a : audience()) {
            a.sendMessage(Texts.msg(plugin.lang(a), key, r));
        }
    }

    private void sayItem(String key, PartyPlayer pp, ItemType item) {
        for (Player a : audience()) {
            String lang = plugin.lang(a);
            a.sendMessage(Texts.msg(lang, key, Placeholder.unparsed("name", pp.name()),
                    Placeholder.component("item", Texts.text(lang, item.key()))));
        }
    }

    private void firework(Location l, Color c) {
        plugin.world().spawn(l.clone().add(0, 1, 0), Firework.class, f -> {
            var meta = f.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).withColor(c, Color.LIME)
                    .withFade(Color.WHITE).build());
            meta.setPower(0);
            f.setFireworkMeta(meta);
            f.setPersistent(false);
            plugin.getServer().getScheduler().runTaskLater(plugin, f::detonate, 2L);
        });
    }

    /** Für /mparty zustand (Tests): Zustand als JSON. */
    public String json() {
        StringBuilder b = new StringBuilder();
        b.append("{\"table\":").append(table).append(",\"board\":\"").append(boardId.id()).append("\",\"mode\":\"")
                .append(kind.id()).append("\",\"phase\":\"").append(phase.name().toLowerCase(java.util.Locale.ROOT))
                .append("\",\"round\":").append(engine.round()).append(",\"rounds\":").append(engine.rounds())
                .append(",\"stand\":").append(engine.stand()).append(",\"current\":\"").append(engine.current().name())
                .append("\",\"mini\":\"").append(mini == null ? "" : mini.type().id()).append("\",\"miniAt\":");
        if (mini == null) {
            b.append("null");
        } else {
            int[] o = Places.mini(table, mini.type());
            b.append('[').append(o[0]).append(',').append(o[1]).append(',').append(o[2]).append(']');
        }
        int[] bo = origin;
        b.append(",\"boardAt\":[").append(bo[0]).append(',').append(bo[1]).append(',').append(bo[2]).append("],\"fields\":\"");
        for (Field f : layout.fields()) {
            b.append(f.type() == FieldType.GEGENSTAND ? 'I' : f.type().name().charAt(0));
        }
        b.append("\",\"players\":[");
        List<PartyPlayer> all = engine.players();
        for (int i = 0; i < all.size(); i++) {
            PartyPlayer p = all.get(i);
            if (i > 0) {
                b.append(',');
            }
            b.append("{\"name\":\"").append(p.name()).append("\",\"pos\":").append(p.pos()).append(",\"taler\":")
                    .append(p.taler()).append(",\"edelweiss\":").append(p.edelweiss()).append(",\"items\":\"")
                    .append(String.join(" ", p.items().stream().map(ItemType::id).toList())).append("\",\"left\":")
                    .append(p.left()).append(",\"minis\":").append(p.minigamesWon()).append(",\"shield\":")
                    .append(p.shield()).append('}');
        }
        b.append("],\"spectators\":").append(spectators.size()).append('}');
        return b.toString();
    }

    /** Für Verwaltung: Spielfiguren neu aufstellen (nach /mparty feld). */
    public void respot() {
        for (PartyPlayer pp : engine.active()) {
            Player p = online(pp);
            if (p != null && phase.ordinal() <= Phase.LANDING.ordinal()) {
                p.teleport(spot(pp));
            }
        }
        placeStand();
        refreshSidebar();
    }

    public Optional<PartyPlayer> find(String name) {
        return engine.players().stream().filter(p -> p.name().equalsIgnoreCase(name)).findFirst();
    }

}
