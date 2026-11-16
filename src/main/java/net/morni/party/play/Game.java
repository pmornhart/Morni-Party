package net.morni.party.play;

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
import net.morni.party.Items;
import net.morni.party.PartyPlugin;
import net.morni.party.Texts;
import net.morni.party.Ui;
import net.morni.party.board.BoardId;
import net.morni.party.board.BoardLayout;
import net.morni.party.board.Field;
import net.morni.party.board.FieldType;
import net.morni.party.game.Engine;
import net.morni.party.game.GameKind;
import net.morni.party.game.Huette;
import net.morni.party.game.ItemType;
import net.morni.party.game.Mode;
import net.morni.party.game.PartyPlayer;
import net.morni.party.game.Teams;
import net.morni.party.menu.Menu;
import net.morni.party.mini.MiniRun;
import net.morni.party.mini.MiniType;
import net.morni.party.world.Places;
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
    private static final int AWARD_TICKS = 5 * SECOND / 2;

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
    /** Vorrat je Spielweise (2 gegen 2, 1 gegen 3, Duell): gemischt, ohne Wiederholung. */
    private final Map<Teams.Format, List<MiniType>> formatBags = new HashMap<>();
    /** Feld, auf dem jemand in dieser Runde zuletzt stand (Farbe entscheidet über die Seiten im Minispiel). */
    private final Map<UUID, FieldType> lastField = new HashMap<>();
    /** Läuft gerade ein Duell-Minispiel (Duellfeld)? Dann: wer gegen wen. */
    private PartyPlayer[] duelPair;
    private Teams.Format forcedFormat;
    /** Gipfel-Turnier: Turnierbaum, laufendes Spiel, Unentschieden im laufenden Spiel, Tafel. */
    private net.morni.party.game.Bracket bracket;
    private net.morni.party.game.Bracket.Match match;
    private int draws;
    /** Tatzelwurm: Ergebnis des Kampfs (null, solange er läuft). */
    private MiniRun.Coop wurmResult;
    private boolean awardsShown;
    private final Map<String, TextDisplay> tafeln = new HashMap<>();
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
    /** Zuletzt gespielt und dessen Blickpunkt (Marathon: Zuschauer zwischen zwei Minispielen). */
    private MiniType lastMini;
    private Location lastView;
    private final Map<String, BossBar> miniBars = new HashMap<>();
    private boolean closed;
    private boolean ended;

    public Game(PartyPlugin plugin, int table, BoardId boardId, GameKind kind, List<Player> players) {
        this(plugin, table, boardId, kind, players, players.stream().map(p -> List.of(p.getUniqueId())).toList());
    }

    /** {@code groups}: wie sie gewartet haben (Partys zusammen) – für die Seilschaften. */
    public Game(PartyPlugin plugin, int table, BoardId boardId, GameKind kind, List<Player> players,
            List<List<UUID>> groups) {
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
        var quick = plugin.settings().quick();
        var rules = kind.mode() == Mode.SCHNELL ? plugin.settings().rules().quick(quick.startTaler(), quick.edelweissPrice())
                : plugin.settings().rules();
        int rounds = kind.mode() == Mode.HUETTE ? plugin.settings().hutMax() : kind.rounds();
        this.engine = new Engine(layout, kind.mode(), rounds, pps, rules, rng);
        if (kind.mode() == Mode.SEILSCHAFT) {
            List<UUID> here = order.stream().map(Player::getUniqueId).toList();
            List<List<UUID>> known = groups.stream().map(g -> g.stream().filter(here::contains).toList())
                    .filter(g -> !g.isEmpty()).toList();
            engine.team(net.morni.party.game.Seilschaft.pair(known));
        }
    }

    /** Seilschaft? */
    public boolean roped() {
        return kind.mode() == Mode.SEILSCHAFT;
    }

    /** Seilschaft als Text: „Anna & Bert“. */
    private String ropeNames(List<PartyPlayer> team) {
        return String.join(" & ", team.stream().map(PartyPlayer::name).toList());
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
        for (PartyPlayer pp : huette() ? engine.climbing() : engine.active()) {
            Player p = plugin.getServer().getPlayer(pp.uuid());
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    /** Alle, die etwas sehen sollen (Mitspielende und Zuschauer). */
    public List<Player> audience() {
        List<Player> out = new ArrayList<>();
        for (PartyPlayer pp : engine.active()) {
            Player p = plugin.getServer().getPlayer(pp.uuid());
            if (p != null) {
                out.add(p);
            }
        }
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
        if (huette() && engine.player(p.getUniqueId()).map(PartyPlayer::out).orElse(false)) {
            return false; // raus: schaut frei zu
        }
        if (turnier() && (mini == null || !mini.score().has(p.getUniqueId()))) {
            return false; // wartet auf das eigene Spiel oder ist raus: schaut frei zu
        }
        if (phase == Phase.MINI) {
            // Beim Duell spielen nur zwei – alle anderen bleiben auf ihrem Feld.
            return mini == null || !mini.score().has(p.getUniqueId()) || mini.frozen();
        }
        return true;
    }

    /**
     * Verwaltung/Tests: Seiten fürs nächste Minispiel festlegen (null = nach den Feldern). Team: die ersten zwei in
     * Zugreihenfolge gegen die anderen zwei; Einer gegen drei: der Erste gegen den Rest.
     */
    public void forceFormat(Teams.Format f) {
        forcedFormat = f;
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
                if (duelPair != null) {
                    duelResults();
                } else if (turnier()) {
                    matchResults();
                } else if (mini.coop() != null) {
                    coopResults();
                } else {
                    miniResults();
                }
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

    /** Minispiel-Marathon (ohne Spielbrett)? */
    public boolean marathon() {
        return kind.mode() == Mode.MARATHON;
    }

    /** Hüttenaufstieg (ohne Spielbrett, mit Leben)? */
    public boolean huette() {
        return kind.mode() == Mode.HUETTE;
    }

    /** Der Tatzelwurm (alle zusammen)? */
    public boolean wurm() {
        return kind.mode() == Mode.TATZELWURM;
    }

    /** Gipfel-Turnier? */
    public boolean turnier() {
        return kind.mode() == Mode.TURNIER;
    }

    /** Ohne Spielbrett: nur Minispiele (Marathon, Hüttenaufstieg, Turnier)? */
    public boolean boardless() {
        return !kind.mode().board();
    }

    public void start() {
        if (boardless()) {
            startMarathon();
            return;
        }
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
            if (roped()) {
                for (List<PartyPlayer> t : engine.teams()) {
                    p.sendMessage(Texts.msg(lang, "rope.team", Placeholder.unparsed("names", ropeNames(t)),
                            Placeholder.unparsed("taler", String.valueOf(t.getFirst().taler()))));
                }
            }
            Ui.sound(p, "minecraft:ui.toast.challenge_complete", 1.2f);
        }
        refreshSidebar();
        plugin.stateChanged();
        later(5 * SECOND, this::beginTurn);
    }

    private Component modeLine(String lang) {
        return switch (kind.mode()) {
            case GIPFELRENNEN -> Texts.text(lang, "mode.gipfelrennen.line");
            case EDELWEISS_JAGD -> Texts.text(lang, "mode.edelweiss-jagd.line", Placeholder.unparsed("n",
                    String.valueOf(kind.rounds())));
            case MARATHON -> Texts.text(lang, "mode.marathon.line", Placeholder.unparsed("n", String.valueOf(kind.rounds())));
            case SEILSCHAFT -> Texts.text(lang, "mode.seilschaft.line", Placeholder.unparsed("n",
                    String.valueOf(kind.rounds())));
            case SCHNELL -> Texts.text(lang, "mode.schnell.line", Placeholder.unparsed("n", String.valueOf(kind.rounds())));
            case BERGDUELL -> Texts.text(lang, "mode.bergduell.line", Placeholder.unparsed("n", String.valueOf(kind.rounds())));
            case HUETTE -> Texts.text(lang, "mode.huette.line", Placeholder.unparsed("n", String.valueOf(engine.rules().lives())));
            case TURNIER -> Texts.text(lang, "mode.turnier.line", Placeholder.unparsed("n", String.valueOf(engine.players().size())));
            case TATZELWURM -> Texts.text(lang, "mode.tatzelwurm.line");
        };
    }

    /** Marathon: kein Brett – nach einer kurzen Ansage geht es gleich zum ersten Minispiel. */
    private void startMarathon() {
        for (PartyPlayer pp : engine.active()) {
            Player p = online(pp);
            if (p != null) {
                plugin.lobby().leaveQueue(p.getUniqueId(), false);
                p.closeInventory();
                p.getInventory().clear();
            }
        }
        String names = String.join(", ", engine.active().stream().map(PartyPlayer::name).toList());
        int[] pts = engine.rules().marathonPoints();
        for (Player p : audience()) {
            String lang = plugin.lang(p);
            Ui.title(p, Texts.text(lang, "mode." + kind.mode().id()), modeLine(lang), 3500);
            p.sendMessage(Texts.msg(lang, "game.goal." + kind.mode().id(), Placeholder.unparsed("n", String.valueOf(engine.rounds())),
                    Placeholder.unparsed("lives", String.valueOf(engine.rules().lives())),
                    Placeholder.unparsed("first", String.valueOf(net.morni.party.game.Marathon.points(pts, 0))),
                    Placeholder.unparsed("second", String.valueOf(net.morni.party.game.Marathon.points(pts, 1))),
                    Placeholder.unparsed("third", String.valueOf(net.morni.party.game.Marathon.points(pts, 2)))));
            p.sendMessage(Texts.msg(lang, "game.players", Placeholder.unparsed("names", names)));
            Ui.sound(p, "minecraft:ui.toast.challenge_complete", 1.2f);
        }
        refreshSidebar();
        plugin.stateChanged();
        later(4 * SECOND, this::startMinigame);
    }

    // ---------- Auszeichnungen ----------

    /**
     * Vor der Siegerehrung einer Edelweiß-Partie auf dem Brett: Auszeichnungen nacheinander zeigen (je ein Edelweiß
     * dazu), danach die Siegerehrung. false, wenn es nichts zu zeigen gibt.
     */
    private boolean showAwards() {
        awardsShown = true;
        if (!kind.mode().board() || kind.mode() == Mode.GIPFELRENNEN || engine.active().size() < 2) {
            return false;
        }
        List<net.morni.party.game.Awards.Award> list = engine.awards(plugin.settings().awards());
        if (list.isEmpty()) {
            return false;
        }
        token++;
        phase = Phase.END;
        clearTimeout();
        closeMenus();
        if (mini != null && !mini.done()) {
            mini.finish();
        }
        hideBars();
        say("award.header");
        for (int i = 0; i < list.size(); i++) {
            var a = list.get(i);
            later(SECOND + i * AWARD_TICKS, () -> {
                String names = String.join(roped() ? " & " : ", ", a.winners().stream().map(PartyPlayer::name).toList());
                for (Player x : audience()) {
                    String lang = plugin.lang(x);
                    Component title = Texts.text(lang, "award." + a.type().id());
                    Component what = Texts.text(lang, "award." + a.type().id() + ".value", Texts.count("n", a.value()));
                    x.sendMessage(Texts.msg(lang, "award.line", Placeholder.component("award", title),
                            Placeholder.unparsed("names", names), Placeholder.component("what", what)));
                    Ui.title(x, title, Texts.text(lang, "award.sub", Placeholder.unparsed("names", names),
                            Placeholder.component("what", what)), 2600);
                    Ui.sound(x, "minecraft:block.note_block.chime", 1.2f);
                }
                refreshSidebar();
            });
        }
        later(SECOND + list.size() * AWARD_TICKS + SECOND, this::end);
        return true;
    }

    // ---------- Tatzelwurm ----------

    /** Kampf vorbei: gemeinsames Ergebnis mit Sternen, Treffer je Person, dann Siegerehrung. */
    private void coopResults() {
        phase = Phase.MINI_RESULT;
        MiniRun run = mini;
        hideBars();
        wurmResult = run.coop();
        for (UUID u : run.score().all()) {
            engine.addPoints(u, run.score().points(u));
        }
        String stars = "★".repeat(wurmResult.stars()) + "☆".repeat(3 - wurmResult.stars());
        for (Player a : audience()) {
            String lang = plugin.lang(a);
            a.sendMessage(Texts.msg(lang, wurmResult.won() ? "wurm.won" : "wurm.lost",
                    Placeholder.unparsed("time", Ui.clock(wurmResult.seconds())), Placeholder.unparsed("stars", stars),
                    Placeholder.unparsed("hp", String.valueOf(wurmResult.hp())), Placeholder.unparsed("max", String.valueOf(wurmResult.max())),
                    Placeholder.unparsed("stuns", String.valueOf(wurmResult.stuns()))));
            Ui.title(a, Texts.text(lang, wurmResult.won() ? "wurm.title.won" : "wurm.title.lost"),
                    Texts.MM.deserialize("<au>" + stars + "</au>"), 3500);
            Ui.sound(a, wurmResult.won() ? "minecraft:ui.toast.challenge_complete" : "minecraft:entity.wither.ambient", 1f);
        }
        later(RESULT_TICKS, () -> {
            mini = null;
            engine.stop();
            end();
        });
    }

    // ---------- Gipfel-Turnier ----------

    private String nameOf(UUID u) {
        return u == null ? "?" : engine.player(u).map(PartyPlayer::name).orElse("?");
    }

    /** Name einer Runde, vom Ende her: Finale, Halbfinale, Viertelfinale, Achtelfinale. */
    private Component roundName(String lang, int left) {
        return left >= 1 && left <= 4 ? Texts.text(lang, "turnier.round." + left)
                : Texts.text(lang, "turnier.round", Placeholder.unparsed("n", String.valueOf(left)));
    }

    private Component roundName(String lang) {
        return bracket == null || bracket.done() ? Texts.text(lang, "turnier.round.done") : roundName(lang, bracket.left());
    }

    /** Wie weit jemand kam (für die Siegerehrung). */
    private Component reached(String lang, PartyPlayer pp) {
        if (bracket == null) {
            return Component.empty();
        }
        if (!pp.out()) {
            return Texts.text(lang, "turnier.reached.won");
        }
        return Texts.text(lang, "turnier.reached", Placeholder.component("round",
                roundName(lang, bracket.roundCount() - pp.outRound() + 1)));
    }

    /** Nächstes Spiel des Turniers (der Baum überspringt Freilose); ist es vorbei: Siegerehrung. */
    private void nextMatch() {
        if (bracket == null) {
            bracket = new net.morni.party.game.Bracket(engine.active().stream().map(PartyPlayer::uuid).toList(), rng);
        }
        match = bracket.next();
        if (match == null) {
            engine.stop();
            end();
            return;
        }
        Player pa = plugin.getServer().getPlayer(match.a());
        Player pb = plugin.getServer().getPlayer(match.b());
        if (pa == null || pb == null) {
            bracket.forfeit(pa == null ? match.a() : match.b());
            later(SECOND, this::nextMatch);
            return;
        }
        MiniType type = nextMini(Teams.Format.DUEL);
        for (Player a : audience()) {
            String lang = plugin.lang(a);
            a.sendMessage(Texts.msg(lang, "turnier.match", Placeholder.component("round", roundName(lang)),
                    Placeholder.unparsed("one", pa.getName()), Placeholder.unparsed("two", pb.getName())));
        }
        runMini(type == null ? MiniType.TAUZIEHEN : type, List.of(pa, pb), Teams.Split.duel(match.a(), match.b()));
    }

    /** Spiel vorbei: Sieger weiter, Verlierer raus; bei Gleichstand ein neues Duell, beim zweiten das Los. */
    private void matchResults() {
        phase = Phase.MINI_RESULT;
        MiniRun run = mini;
        hideBars();
        Teams.Outcome o = run.teamOutcome();
        UUID a = match.a();
        UUID b = match.b();
        if (o == Teams.Outcome.DRAW && draws < 1) {
            draws++;
            say("turnier.draw", Placeholder.unparsed("one", nameOf(a)), Placeholder.unparsed("two", nameOf(b)));
            later(RESULT_TICKS, () -> {
                mini = null;
                Player pa = plugin.getServer().getPlayer(a);
                Player pb = plugin.getServer().getPlayer(b);
                if (pa != null && pb != null) {
                    runMini(nextMini(Teams.Format.DUEL), List.of(pa, pb), Teams.Split.duel(a, b));
                } else {
                    bracket.forfeit(pa == null ? a : b);
                    nextMatch();
                }
            });
            return;
        }
        boolean lot = o == Teams.Outcome.DRAW;
        UUID winner = o == Teams.Outcome.A ? a : o == Teams.Outcome.B ? b : rng.nextBoolean() ? a : b;
        UUID loser = winner.equals(a) ? b : a;
        draws = 0;
        int roundNo = bracket.round() + 1;
        boolean finale = bracket.left() == 1;
        bracket.report(winner);
        engine.knockOut(winner, loser, roundNo);
        for (Player x : audience()) {
            String lang = plugin.lang(x);
            x.sendMessage(Texts.msg(lang, "duel.result", Placeholder.component("name", Texts.text(lang, run.type().key())),
                    Placeholder.unparsed("one", nameOf(a)), Placeholder.unparsed("two", nameOf(b)),
                    Placeholder.unparsed("p1", String.valueOf(run.shownPoints(a))),
                    Placeholder.unparsed("p2", String.valueOf(run.shownPoints(b)))));
            x.sendMessage(Texts.msg(lang, lot ? "turnier.lot" : finale ? "turnier.champion" : "turnier.through",
                    Placeholder.unparsed("winner", nameOf(winner)), Placeholder.unparsed("loser", nameOf(loser))));
            if (x.getUniqueId().equals(winner) || x.getUniqueId().equals(loser)) {
                boolean won = x.getUniqueId().equals(winner);
                Ui.title(x, Texts.text(lang, won ? "mini.won" : "mini.over"),
                        Texts.text(lang, won ? (finale ? "turnier.sub.champion" : "turnier.sub.through") : "turnier.sub.out"), 2500);
                Ui.sound(x, won ? "minecraft:ui.toast.challenge_complete" : "minecraft:entity.villager.no", 1f);
            }
        }
        showTafel();
        refreshSidebar();
        later(RESULT_TICKS, () -> {
            mini = null;
            if (bracket.done()) {
                engine.stop();
                end();
                return;
            }
            nextMatch();
        });
    }

    /**
     * Turnierbaum als Tafel vor den Zuschauern (am Platz des laufenden Spiels) – eine Tafel je Sprache, jeder sieht nur
     * die in seiner Sprache.
     */
    private void showTafel() {
        if (bracket == null || mini == null) {
            return;
        }
        Location view = mini.viewPoint();
        Location at = view.clone().add(view.getDirection().setY(0).normalize().multiply(7)).add(0, 1.5, 0);
        for (String lang : List.of("de", "en")) {
            TextDisplay d = tafeln.get(lang);
            if (d == null || d.isDead() || !d.getWorld().equals(at.getWorld()) || d.getLocation().distanceSquared(at) > 1) {
                if (d != null) {
                    d.remove();
                }
                d = at.getWorld().spawn(at, TextDisplay.class, t -> {
                    t.setBillboard(Display.Billboard.CENTER);
                    t.setPersistent(false);
                    t.setLineWidth(400);
                    t.setBackgroundColor(Color.fromARGB(170, 20, 30, 25));
                    t.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(0.6f, 0.6f, 0.6f),
                            new AxisAngle4f()));
                });
                tafeln.put(lang, d);
            }
            d.text(tafelText(lang));
        }
        for (Player a : audience()) {
            String mine = "en".equals(plugin.lang(a)) ? "en" : "de";
            tafeln.forEach((lang, d) -> {
                if (lang.equals(mine)) {
                    a.showEntity(plugin, d);
                } else {
                    a.hideEntity(plugin, d);
                }
            });
        }
    }

    private Component tafelText(String lang) {
        Component c = Texts.text(lang, "turnier.tafel.title");
        List<List<UUID>> rounds = bracket.rounds();
        for (int r = 0; r < rounds.size() && r < bracket.roundCount(); r++) {
            List<UUID> slots = rounds.get(r);
            c = c.append(Component.newline()).append(Component.newline()).append(roundName(lang, bracket.roundCount() - r));
            for (int i = 0; i + 1 < slots.size(); i += 2) {
                c = c.append(Component.newline()).append(slot(lang, r, slots.get(i))).append(Texts.text(lang, "turnier.tafel.vs"))
                        .append(slot(lang, r, slots.get(i + 1)));
            }
        }
        UUID champ = bracket.champion();
        if (champ != null) {
            c = c.append(Component.newline()).append(Component.newline())
                    .append(Texts.text(lang, "turnier.tafel.champion", Placeholder.unparsed("name", nameOf(champ))));
        }
        return c;
    }

    private Component slot(String lang, int round, UUID u) {
        if (u == null) {
            return Texts.text(lang, "turnier.tafel.bye");
        }
        if (bracket.won(round, u)) {
            return Texts.text(lang, "turnier.tafel.won", Placeholder.unparsed("name", nameOf(u)));
        }
        int pair = bracket.rounds().get(round).indexOf(u) / 2;
        boolean played = bracket.round() > round || (!bracket.done() && bracket.round() == round
                && pair < bracket.next().index());
        return Texts.text(lang, played ? "turnier.tafel.lost" : "turnier.tafel.open", Placeholder.unparsed("name", nameOf(u)));
    }

    private String hearts(UUID u) {
        return engine.player(u).map(p -> Huette.hearts(p.lives(), engine.rules().lives())).orElse("");
    }

    /** Hüttenaufstieg: wer ein Leben verlor, wer raus ist (die schauen ab jetzt zu). */
    private void announceClimb(Engine.Climb climb) {
        for (UUID u : climb.lost()) {
            String name = engine.player(u).map(PartyPlayer::name).orElse("?");
            if (climb.out().contains(u)) {
                say("huette.out", Placeholder.unparsed("name", name));
            } else {
                say("huette.lost", Placeholder.unparsed("name", name), Placeholder.unparsed("hearts", hearts(u)));
            }
        }
        for (UUID u : climb.out()) {
            Player p = plugin.getServer().getPlayer(u);
            if (p != null) {
                p.getInventory().clear();
                p.setGameMode(GameMode.SPECTATOR);
                Ui.title(p, Texts.text(plugin.lang(p), "huette.out.title"), Texts.text(plugin.lang(p), "huette.out.sub"), 3000);
            }
        }
    }

    /** Hüttenaufstieg: Zwischenstand mit Herzen. */
    private void hutStanding() {
        for (Player a : audience()) {
            String lang = plugin.lang(a);
            a.sendMessage(Texts.msg(lang, "huette.standing", Placeholder.unparsed("left", String.valueOf(engine.climbing().size()))));
            for (PartyPlayer pp : engine.climbing()) {
                a.sendMessage(Texts.text(lang, "huette.standing.line", Placeholder.unparsed("name", pp.name()),
                        Placeholder.unparsed("hearts", Huette.hearts(pp.lives(), engine.rules().lives()))));
            }
        }
    }

    /** Marathon: Zwischenstand als kurze Tafel im Chat (die ersten fünf und die eigene Zeile). */
    private void marathonStanding() {
        List<List<PartyPlayer>> places = engine.places();
        for (Player a : audience()) {
            String lang = plugin.lang(a);
            a.sendMessage(Texts.msg(lang, "marathon.standing", Placeholder.unparsed("n", String.valueOf(engine.round() - 1)),
                    Placeholder.unparsed("of", String.valueOf(engine.rounds()))));
            int place = 1;
            int shown = 0;
            for (List<PartyPlayer> g : places) {
                for (PartyPlayer pp : g) {
                    if (shown < 5 || pp.uuid().equals(a.getUniqueId())) {
                        a.sendMessage(Texts.text(lang, "marathon.standing.line", Placeholder.unparsed("pos", String.valueOf(place)),
                                Placeholder.unparsed("name", pp.name()),
                                Texts.count("points", pp.minigamePoints())));
                    }
                    shown++;
                }
                place += g.size();
            }
        }
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
                case FAEHRE -> "<aqua><b>⚓</b></aqua>";
                case RIESENRAD -> "<light_purple><b>✪</b></light_purple>";
                case FAHRGESCHAEFT -> "<dark_purple><b>♫</b></dark_purple>";
                case LAWINE -> "<white><b>❄!</b></white>";
                case EIS -> "<aqua><b>❄</b></aqua>";
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
                            Texts.text(lang, "roll.sub", Texts.count("n", roll.total())), 1200);
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
        lastField.put(pp.uuid(), o.field());
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
            case FERRY -> ferry(pp, o.steps());
            case SWAP -> {
                UUID other = o.others().keySet().iterator().next();
                String oname = engine.player(other).map(PartyPlayer::name).orElse("?");
                say(o.blocked() ? "land.wheel.blocked" : "land.wheel", Placeholder.unparsed("name", pp.name()),
                        Placeholder.unparsed("other", oname));
                if (!o.blocked()) {
                    later(SECOND, () -> {
                        engine.player(other).ifPresent(op -> {
                            Player x = online(op);
                            if (x != null) {
                                x.teleport(spot(op));
                            }
                        });
                        if (p != null) {
                            p.teleport(spot(pp));
                        }
                        audience().forEach(a -> Ui.sound(a, "minecraft:entity.player.levelup", 1.6f));
                        refreshSidebar();
                    });
                }
                later(2 * SECOND + SECOND / 2, this::finishTurn);
            }
            case RIDE -> ride(pp, o);
            case AVALANCHE -> avalanche(pp, o);
            case SLIDE -> {
                say("land.ice", Placeholder.unparsed("name", pp.name()), Placeholder.unparsed("n", String.valueOf(o.steps())));
                if (p != null) {
                    Ui.sound(p, "minecraft:block.glass.step", 1.6f);
                }
                later(SECOND / 2, () -> move(pp, o.steps(), false, this::finishTurn));
            }
        }
        if (p != null && o.kind() != Engine.Kind.SHOP && o.kind() != Engine.Kind.DUEL && o.kind() != Engine.Kind.RIDE) {
            p.sendActionBar(Texts.text(plugin.lang(p), field));
        }
        refreshSidebar();
    }

    /** Königssee: mit der Fähre quer über den See – die Figur gleitet in gerader Linie zum Ziel-Steg. */
    private void ferry(PartyPlayer pp, int steps) {
        say("land.ferry", Placeholder.unparsed("name", pp.name()), Placeholder.unparsed("n", String.valueOf(steps)));
        Player p = online(pp);
        if (p == null) {
            laterFinish();
            return;
        }
        Location from = p.getLocation();
        Location to = spot(pp);
        int frames = 40;
        Ui.sound(p, "minecraft:entity.boat.paddle_water", 1f);
        for (int i = 1; i <= frames; i++) {
            int k = i;
            later(SECOND + k, () -> {
                double a = (double) k / frames;
                Location l = from.clone().add(to.clone().subtract(from).toVector().multiply(a));
                l.setY(Math.max(from.getY(), to.getY()) + Math.sin(a * Math.PI) * 1.2);
                l.setYaw(to.getYaw());
                p.teleport(l);
                if (k % 4 == 0) {
                    plugin.world().spawnParticle(org.bukkit.Particle.SPLASH, l, 6, 0.4, 0.1, 0.4, 0.05);
                }
            });
        }
        later(SECOND + frames + 2, () -> {
            p.teleport(spot(pp));
            refreshSidebar();
            laterFinish();
        });
    }

    /** Wiesn: ein Fahrgeschäft. */
    private void ride(PartyPlayer pp, Engine.Outcome o) {
        Player p = online(pp);
        for (Player a : audience()) {
            String lang = plugin.lang(a);
            Component item = o.item() == null ? Component.empty() : Texts.text(lang, o.item().key());
            a.sendMessage(Texts.msg(lang, o.ride().key(), Placeholder.unparsed("name", pp.name()),
                    Placeholder.unparsed("n", String.valueOf(Math.abs(o.steps()))),
                    Placeholder.unparsed("taler", String.valueOf(o.taler())),
                    Placeholder.unparsed("count", String.valueOf(o.others().size())),
                    Placeholder.component("item", item)));
            Ui.sound(a, "minecraft:block.note_block.bell", 1.4f);
        }
        if (p != null) {
            Ui.title(p, Texts.text(plugin.lang(p), o.ride().key() + ".title"), Component.empty(), 1800);
        }
        if (o.steps() > 0) {
            later(SECOND + SECOND / 2, () -> move(pp, o.steps(), false, this::finishTurn));
        } else if (o.steps() < 0) {
            later(SECOND + SECOND / 2, () -> moveBack(pp, -o.steps(), this::finishTurn));
        } else {
            laterFinish();
        }
        refreshSidebar();
    }

    /** Wintersonnwend: Lawine – alle Getroffenen rutschen nacheinander zurück. */
    private void avalanche(PartyPlayer pp, Engine.Outcome o) {
        List<String> names = o.others().keySet().stream().map(u -> engine.player(u).map(PartyPlayer::name).orElse("?")).toList();
        say(o.others().isEmpty() ? "land.avalanche.blocked" : "land.avalanche", Placeholder.unparsed("name", pp.name()),
                Placeholder.unparsed("names", String.join(", ", names)),
                Placeholder.unparsed("n", String.valueOf(Engine.AVALANCHE_BACK)));
        audience().forEach(a -> Ui.sound(a, "minecraft:block.snow.break", 0.6f));
        Player p = online(pp);
        if (p != null) {
            plugin.world().spawnParticle(org.bukkit.Particle.SNOWFLAKE, p.getLocation().add(0, 1, 0), 120, 3, 2, 3, 0.1);
        }
        List<Map.Entry<UUID, Integer>> list = new ArrayList<>(o.others().entrySet());
        Runnable[] next = new Runnable[1];
        int[] i = {0};
        next[0] = () -> {
            if (i[0] >= list.size()) {
                refreshSidebar();
                laterFinish();
                return;
            }
            var e = list.get(i[0]++);
            PartyPlayer who = engine.player(e.getKey()).orElse(null);
            if (who == null || e.getValue() <= 0) {
                next[0].run();
                return;
            }
            moveBack(who, e.getValue(), next[0]);
        };
        later(SECOND + SECOND / 2, next[0]);
    }

    private void luckMessage(PartyPlayer pp, Engine.Outcome o) {
        for (Player a : audience()) {
            String lang = plugin.lang(a);
            Component item = o.item() == null ? Component.empty() : Texts.text(lang, o.item().key());
            String key = o.blocked() ? "luck.blocked" : o.luck().key();
            a.sendMessage(Texts.msg(lang, key, Placeholder.unparsed("name", pp.name()),
                    Placeholder.unparsed("n", String.valueOf(Math.abs(o.taler()))),
                    Texts.count("steps", Math.abs(o.steps())),
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
        List<PartyPlayer> others = engine.active().stream().filter(o -> o != pp && !engine.mates(o, pp)).toList();
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
        Player pa = online(a);
        Player pb = online(b);
        MiniType type = pa == null || pb == null ? null : nextMini(Teams.Format.DUEL);
        if (type == null) {
            diceDuel(a, b);
            return;
        }
        duelPair = new PartyPlayer[] {a, b};
        say("duel.vs", Placeholder.unparsed("one", a.name()), Placeholder.unparsed("two", b.name()));
        later(SECOND, () -> runMini(type, List.of(pa, pb), Teams.Split.duel(a.uuid(), b.uuid())));
    }

    /** Ohne passendes Duell-Minispiel (alle abgeschaltet oder jemand nicht da): wie früher würfeln. */
    private void diceDuel(PartyPlayer a, PartyPlayer b) {
        Engine.Duel d = engine.duel(a, b);
        say("duel.dice", Placeholder.unparsed("one", a.name()), Placeholder.unparsed("two", b.name()));
        later(SECOND, () -> {
            for (Player x : audience()) {
                Ui.title(x, Texts.MM.deserialize("<au>" + d.challengerRoll() + "</au> <gray>:</gray> <au>"
                        + d.opponentRoll() + "</au>"), Texts.MM.deserialize("<white>" + a.name() + "</white> <gray>⚔</gray> <white>"
                        + b.name() + "</white>"), 1800);
                Ui.sound(x, "minecraft:entity.player.attack.strong", 1f);
            }
            duelMessage(a, b, d);
            later(2 * SECOND, this::finishTurn);
        });
    }

    private void duelMessage(PartyPlayer a, PartyPlayer b, Engine.Duel d) {
        if (d.winner() == null) {
            say("duel.draw");
        } else {
            String winner = d.winner().equals(a.uuid()) ? a.name() : b.name();
            String loser = d.winner().equals(a.uuid()) ? b.name() : a.name();
            say(d.blocked() ? "duel.blocked" : "duel.won", Placeholder.unparsed("winner", winner),
                    Placeholder.unparsed("loser", loser), Placeholder.unparsed("n", String.valueOf(d.taler())));
        }
        refreshSidebar();
    }

    /** Duell-Minispiel vorbei: Sieger bekommt den Einsatz, dann zurück aufs Brett und der Zug geht weiter. */
    private void duelResults() {
        phase = Phase.MINI_RESULT;
        MiniRun run = mini;
        PartyPlayer a = duelPair[0];
        PartyPlayer b = duelPair[1];
        duelPair = null;
        hideBars();
        Teams.Outcome o = run.teamOutcome();
        UUID winner = o == Teams.Outcome.A ? a.uuid() : o == Teams.Outcome.B ? b.uuid() : null;
        Engine.Duel d = engine.settleDuel(a, b, winner);
        for (Player x : audience()) {
            String lang = plugin.lang(x);
            x.sendMessage(Texts.msg(lang, "duel.result", Placeholder.component("name", Texts.text(lang, run.type().key())),
                    Placeholder.unparsed("one", a.name()), Placeholder.unparsed("two", b.name()),
                    Placeholder.unparsed("p1", String.valueOf(run.shownPoints(a.uuid()))),
                    Placeholder.unparsed("p2", String.valueOf(run.shownPoints(b.uuid())))));
            if (x.getUniqueId().equals(a.uuid()) || x.getUniqueId().equals(b.uuid())) {
                boolean won = x.getUniqueId().equals(winner);
                Ui.title(x, Texts.text(lang, winner == null ? "duel.title.draw" : won ? "mini.won" : "mini.over"),
                        Component.empty(), 2000);
                Ui.sound(x, won ? "minecraft:ui.toast.challenge_complete" : "minecraft:entity.experience_orb.pickup", 1f);
            }
        }
        duelMessage(a, b, d);
        later(RESULT_TICKS - 2 * SECOND, () -> {
            mini = null;
            backToBoard();
            later(SECOND, this::finishTurn);
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
            // Talerklau nie beim Partner (Platztausch mit dem Partner geht – das kann helfen)
            List<PartyPlayer> others = engine.active().stream()
                    .filter(o -> o != pp && (t != ItemType.TALERKLAU || !engine.mates(o, pp))).toList();
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
        // Nur das Profil der Person, die gerade da ist (mit Skin) – keine Abfrage bei Mojang.
        Player online = plugin.getServer().getPlayer(o.uuid());
        if (online != null) {
            head.editMeta(org.bukkit.inventory.meta.SkullMeta.class, meta -> meta.setPlayerProfile(online.getPlayerProfile()));
        }
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
        if (boardless()) {
            // gemischt, ohne Wiederholung, nur Plätze, die so viele Leute aushalten
            int n = players().size();
            List<MiniType> all = plugin.settings().minigames();
            MiniType t = net.morni.party.game.Marathon.draw(miniOrder, all, lastMini,
                    x -> x.maxPlayers() >= n && x.fits(Teams.Format.FREE), rng);
            return t != null ? t : MiniType.BROECKELBODEN;
        }
        for (int tries = 0; tries < 2; tries++) {
            if (miniOrder.isEmpty()) {
                plugin.settings().minigames().stream().filter(x -> x.fits(Teams.Format.FREE)).forEach(miniOrder::add);
                if (!plugin.settings().fixedOrder()) {
                    Collections.shuffle(miniOrder, rng);
                }
            }
            if (!miniOrder.isEmpty()) {
                return miniOrder.removeFirst();
            }
        }
        return MiniType.BROECKELBODEN;
    }

    /**
     * Nächstes Minispiel für eine Spielweise mit Seiten (2 gegen 2, 1 gegen 3, Duell) – gemischt, ohne Wiederholung, nur
     * aus den erlaubten. Ein festgelegtes (Verwaltung) gilt, wenn es passt. null: keines passt.
     */
    private MiniType nextMini(Teams.Format f) {
        if (forcedMini != null && forcedMini.fits(f)) {
            MiniType t = forcedMini;
            forcedMini = null;
            return t;
        }
        List<MiniType> all = plugin.settings().minigames().stream().filter(x -> x.fits(f)).toList();
        if (all.isEmpty()) {
            return null;
        }
        return net.morni.party.game.Marathon.draw(formatBags.computeIfAbsent(f, k -> new ArrayList<>()), all, null,
                x -> true, rng);
    }

    private void startMinigame() {
        if (turnier()) {
            nextMatch();
            return;
        }
        if (wurm()) {
            runMini(MiniType.TATZELWURM, players(), Teams.Split.FREE);
            return;
        }
        Teams.Split split = Teams.Split.FREE;
        MiniType type = null;
        if (!boardless()) {
            List<UUID> order = engine.active().stream().map(PartyPlayer::uuid).toList();
            split = roped() ? ropeSplit() : kind.mode() == Mode.BERGDUELL && order.size() == 2
                    ? Teams.Split.duel(order.get(0), order.get(1)) : Teams.decide(order, lastField);
            if (forcedFormat != null) {
                split = roped() && forcedFormat == Teams.Format.TEAM ? ropeSplit() : forcedSplit(forcedFormat, order);
                forcedFormat = null;
            } else if (forcedMini != null && !((roped() || kind.mode() == Mode.BERGDUELL) && forcedMini.fits(split.format()))) {
                split = Teams.Split.FREE; // Verwaltung/Tests: ein festgelegtes Spiel ist jeder gegen jeden
            }
            if (split.sides()) {
                type = nextMini(split.format());
                if (type == null) {
                    split = Teams.Split.FREE;
                }
            }
        }
        lastField.clear();
        runMini(type != null ? type : nextMini(), players(), split);
    }

    /** Seilschaft: genau zwei Seilschaften zu zweit → 2 gegen 2, sonst jeder gegen jeden. */
    private Teams.Split ropeSplit() {
        List<List<PartyPlayer>> t = engine.teams();
        if (t.size() != 2 || t.get(0).size() != 2 || t.get(1).size() != 2) {
            return Teams.Split.FREE;
        }
        return new Teams.Split(Teams.Format.TEAM, t.get(0).stream().map(PartyPlayer::uuid).toList(),
                t.get(1).stream().map(PartyPlayer::uuid).toList());
    }

    private static Teams.Split forcedSplit(Teams.Format f, List<UUID> order) {
        if (order.size() != 4 || f == Teams.Format.FREE || f == Teams.Format.DUEL) {
            return Teams.Split.FREE;
        }
        return f == Teams.Format.TEAM ? new Teams.Split(f, order.subList(0, 2), order.subList(2, 4))
                : new Teams.Split(f, order.subList(0, 1), order.subList(1, 4));
    }

    /** Minispiel ansagen, Platz legen, Countdown, los – für die Runde und fürs Duell. */
    private void runMini(MiniType type, List<Player> ps, Teams.Split split) {
        phase = Phase.MINI_INTRO;
        if (duelPair == null) {
            lastMini = type;
        }
        int[] o = Places.mini(table, type);
        double factor = plugin.settings().miniFactor() * (kind.mode() == Mode.SCHNELL ? plugin.settings().quick().miniFactor() : 1);
        mini = type.create(new MiniRun.Setup(plugin, type, plugin.world(), o[0], o[1], o[2], ps, this::audience, rng,
                factor, split));
        lastView = mini.viewPoint();
        for (Player a : audience()) {
            String lang = plugin.lang(a);
            Ui.title(a, Texts.text(lang, split.format() == Teams.Format.DUEL ? "duel.mini" : "mini.title"),
                    Texts.text(lang, type.key()), 2500);
            a.sendMessage(Texts.msg(lang, "mini.announce", Placeholder.component("name", Texts.text(lang, type.key()))));
            if (split.sides()) {
                a.sendMessage(Texts.msg(lang, split.format().key(), Placeholder.unparsed("sidea", names(split.a())),
                        Placeholder.unparsed("sideb", names(split.b()))));
            }
            if (marathon()) {
                a.sendMessage(Texts.msg(lang, "marathon.next", Placeholder.unparsed("n", String.valueOf(engine.round())),
                        Placeholder.unparsed("of", String.valueOf(engine.rounds()))));
            } else if (huette() && duelPair == null) {
                a.sendMessage(Texts.msg(lang, "huette.next", Placeholder.unparsed("n", String.valueOf(engine.round())),
                        Placeholder.unparsed("left", String.valueOf(engine.climbing().size()))));
            }
            a.sendMessage(Texts.msg(lang, type.key() + ".desc"));
            Ui.sound(a, "minecraft:block.note_block.flute", 1.2f);
            if (ps.contains(a) || !plays(a.getUniqueId())) {
                a.getInventory().clear();
                a.closeInventory();
            }
        }
        later(3 * SECOND, () -> {
            mini.prepare();
            List<UUID> watching = new ArrayList<>(spectators);
            if (huette()) {
                engine.active().stream().filter(PartyPlayer::out).forEach(pp -> watching.add(pp.uuid()));
            }
            if (turnier()) {
                for (PartyPlayer pp : engine.active()) {
                    Player w = online(pp);
                    if (w != null && !ps.contains(w)) {
                        w.getInventory().clear();
                        w.setGameMode(GameMode.SPECTATOR);
                        watching.add(pp.uuid());
                    }
                }
                showTafel();
            }
            for (UUID u : watching) {
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

    private String names(List<UUID> list) {
        return String.join(" + ", list.stream().map(u -> engine.player(u).map(PartyPlayer::name).orElse("?")).toList());
    }

    private void miniResults() {
        phase = Phase.MINI_RESULT;
        MiniRun run = mini;
        hideBars();
        List<List<UUID>> places = run.score().places();
        Teams.Split split = run.split();
        Teams.Outcome outcome = split.sides() ? run.teamOutcome() : null;
        Engine.Climb climb = huette() ? engine.climb(places) : null;
        Map<UUID, Integer> got = climb != null ? Map.of() : split.sides() ? engine.awardTeams(split, outcome)
                : engine.awardMinigame(places);
        List<UUID> winners = split.sides() ? split.winners(outcome) : places.isEmpty() ? List.of() : places.getFirst();
        for (Player a : audience()) {
            String lang = plugin.lang(a);
            a.sendMessage(Texts.msg(lang, "mini.results", Placeholder.component("name", Texts.text(lang, run.type().key()))));
            int place = 1;
            for (List<UUID> g : places) {
                for (UUID u : g) {
                    String name = engine.player(u).map(PartyPlayer::name).orElse("?");
                    long at = run.score().finishTick(u);
                    Component prize = climb != null
                            ? Texts.text(lang, climb.lost().contains(u) ? "mini.prize.life" : "mini.prize.safe")
                            : Texts.text(lang, marathon() ? "mini.prize.points" : "mini.prize.taler",
                                    Texts.count("n", got.getOrDefault(u, 0)));
                    a.sendMessage(Texts.text(lang, run.resultKey(u), Placeholder.unparsed("pos", String.valueOf(place)),
                            Placeholder.unparsed("name", name), Texts.count("points", run.shownPoints(u)),
                            Placeholder.unparsed("time", Ui.seconds(Math.max(0, at) * 50, lang)),
                            Placeholder.component("prize", prize)));
                }
                place += g.size();
            }
            if (outcome != null) {
                a.sendMessage(Texts.msg(lang, outcome == Teams.Outcome.DRAW ? "team.draw"
                        : split.format() == Teams.Format.DUEL ? "team.won.duel" : split.winners(outcome).size() == 1 ? "team.won.one"
                        : "team.won", Placeholder.unparsed("names", names(split.winners(outcome))),
                        Placeholder.unparsed("n", String.valueOf(outcome == Teams.Outcome.DRAW
                                ? engine.rules().teamDraw() : got.getOrDefault(split.winners(outcome).getFirst(), 0)))));
            }
            boolean won = winners.contains(a.getUniqueId());
            if (plays(a.getUniqueId())) {
                Component sub = climb != null ? Texts.text(lang, climb.lost().contains(a.getUniqueId()) ? "huette.sub.lost"
                        : "huette.sub.safe", Placeholder.unparsed("hearts", hearts(a.getUniqueId())))
                        : Texts.text(lang, marathon() ? "mini.got.points" : "mini.got",
                                Placeholder.unparsed("n", String.valueOf(got.getOrDefault(a.getUniqueId(), 0))));
                Ui.title(a, Texts.text(lang, won ? "mini.won" : "mini.over"), sub, 2500);
                Ui.sound(a, won ? "minecraft:ui.toast.challenge_complete" : "minecraft:entity.experience_orb.pickup", 1f);
            }
        }
        if (climb != null) {
            announceClimb(climb);
        }
        later(RESULT_TICKS, () -> {
            mini = null;
            if (boardless()) {
                engine.nextRound();
                if (engine.over()) {
                    end();
                    return;
                }
                if (huette()) {
                    hutStanding();
                } else {
                    marathonStanding();
                }
                refreshSidebar();
                later(3 * SECOND, this::startMinigame);
                return;
            }
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

    /** Blick aufs ganze Brett (für Zuschauer); im Marathon der Platz des letzten Minispiels. */
    public Location viewPoint() {
        if (boardless()) {
            return lastView == null ? plugin.lobby().spawn() : lastView.clone();
        }
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
        if (!awardsShown && showAwards()) {
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
        List<PartyPlayer> winners = places.isEmpty() || (wurm() && (wurmResult == null || !wurmResult.won())) ? List.of()
                : places.getFirst();
        String names = String.join(roped() ? " & " : ", ", winners.stream().map(PartyPlayer::name).toList());
        for (Player a : audience()) {
            String lang = plugin.lang(a);
            a.sendMessage(Texts.msg(lang, "end.header"));
            int place = 1;
            for (List<PartyPlayer> g : places) {
                for (PartyPlayer pp : g) {
                    a.sendMessage(Texts.text(lang, marathon() ? "end.line.marathon" : huette() ? "end.line.huette"
                            : turnier() ? "end.line.turnier" : wurm() ? "end.line.wurm" : "end.line",
                            Placeholder.component("reached", turnier() ? reached(lang, pp) : Component.empty()),
                            Placeholder.unparsed("hearts", Huette.hearts(pp.lives(), engine.rules().lives())),
                            Placeholder.unparsed("pos", String.valueOf(place)),
                            Placeholder.unparsed("name", pp.name()), Placeholder.unparsed("edelweiss", String.valueOf(pp.edelweiss())),
                            Placeholder.unparsed("taler", String.valueOf(pp.taler())),
                            Placeholder.unparsed("field", String.valueOf(pp.pos())),
                            Texts.count("points", pp.minigamePoints()),
                            Texts.count("minis", pp.minigamesWon())));
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
                plugin.store().record(pp.uuid(), pp.name(), kind.mode().id(), won, pp.edelweiss(), pp.minigamesWon(), summit);
                Map<String, Long> add = new HashMap<>();
                add.put("games", 1L);
                add.put("wins", won ? 1L : 0L);
                add.put("edelweiss", (long) pp.edelweiss());
                add.put("minigames_won", (long) pp.minigamesWon());
                add.put("games_" + kind.mode().id().replace("-", "_"), 1L);
                add.put("wins_" + kind.mode().id().replace("-", "_"), won ? 1L : 0L);
                if (summit) {
                    add.put("summits", 1L);
                }
                plugin.net().stats(pp.uuid(), add, marathon() ? Map.of("marathon_points_best", (long) pp.minigamePoints())
                        : huette() ? Map.of("huette_lives_best", (long) pp.lives())
                        : turnier() ? Map.of("turnier_matches_best", (long) pp.minigamesWon())
                        : wurm() ? Map.of("tatzelwurm_stars_best", (long) (wurmResult == null ? 0 : wurmResult.stars()))
                        : Map.of("edelweiss_best", (long) pp.edelweiss()));
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
        tafeln.values().forEach(Entity::remove);
        tafeln.clear();
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
        boolean inMatch = mini != null && !mini.done() && mini.score().has(u);
        if (mini != null && !mini.done()) {
            mini.leave(p);
        }
        if (turnier() && bracket != null && !inMatch) {
            bracket.forfeit(u); // wartete noch: der nächste Gegner kommt kampflos weiter
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
        if (!boardless() && current && phase != Phase.MINI_INTRO && phase != Phase.MINI_COUNTDOWN && phase != Phase.MINI
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
        if (boardless()) {
            marathonSidebar(sorted, places);
            return;
        }
        for (Player a : audience()) {
            String lang = plugin.lang(a);
            List<Component> lines = new ArrayList<>();
            lines.add(Texts.text(lang, "board." + boardId.id() + ".short"));
            lines.add(kind.mode() == Mode.GIPFELRENNEN
                    ? Texts.text(lang, "side.race", Placeholder.unparsed("round", String.valueOf(engine.round())))
                    : Texts.text(lang, "side.round", Placeholder.unparsed("round", String.valueOf(Math.min(engine.round(),
                            engine.rounds()))), Placeholder.unparsed("rounds", String.valueOf(engine.rounds()))));
            lines.add(Component.empty());
            if (roped()) {
                List<List<PartyPlayer>> teams = new ArrayList<>(engine.teams());
                teams.sort((x, y) -> Integer.compare(places.getOrDefault(x.getFirst().uuid(), 99),
                        places.getOrDefault(y.getFirst().uuid(), 99)));
                for (List<PartyPlayer> t : teams) {
                    Component names = Component.empty();
                    for (int k = 0; k < t.size(); k++) {
                        PartyPlayer pp = t.get(k);
                        boolean cur = !ended && engine.current() == pp && phase.ordinal() <= Phase.LANDING.ordinal();
                        if (k > 0) {
                            names = names.append(Component.text(" & ", TextColor.color(0xAAAAAA)));
                        }
                        names = names.append(Component.text((cur ? "▶" : "") + pp.name(),
                                TextColor.color(COLOURS[pp.colour() % COLOURS.length])));
                    }
                    lines.add(Texts.text(lang, "side.rope", Placeholder.unparsed("pos",
                            String.valueOf(places.getOrDefault(t.getFirst().uuid(), 0))), Placeholder.component("names", names),
                            Placeholder.unparsed("edelweiss", String.valueOf(t.getFirst().edelweiss())),
                            Placeholder.unparsed("taler", String.valueOf(t.getFirst().taler()))));
                }
            }
            for (PartyPlayer pp : roped() ? List.<PartyPlayer>of() : sorted) {
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

    /** Marathon: Minispiel r von n und die Punkte (höchstens zwölf Zeilen mit Namen). */
    private void marathonSidebar(List<PartyPlayer> sorted, Map<UUID, Integer> places) {
        for (Player a : audience()) {
            String lang = plugin.lang(a);
            List<Component> lines = new ArrayList<>();
            lines.add(Texts.text(lang, "mode." + kind.mode().id()));
            lines.add(turnier() ? Texts.text(lang, "side.turnier", Placeholder.component("round", roundName(lang)))
                    : huette() ? Texts.text(lang, "side.huette", Placeholder.unparsed("round", String.valueOf(engine.round())),
                    Placeholder.unparsed("left", String.valueOf(engine.climbing().size())))
                    : Texts.text(lang, "side.marathon", Placeholder.unparsed("round", String.valueOf(Math.min(engine.round(),
                    engine.rounds()))), Placeholder.unparsed("rounds", String.valueOf(engine.rounds()))));
            lines.add(Component.empty());
            int shown = 0;
            for (PartyPlayer pp : sorted) {
                boolean me = pp.uuid().equals(a.getUniqueId());
                if (shown >= 11 && !me) {
                    continue;
                }
                Component name = Component.text(pp.name(), TextColor.color(COLOURS[pp.colour() % COLOURS.length]));
                lines.add(turnier() ? Texts.text(lang, pp.out() ? "side.player.huette.out" : "side.player.turnier",
                        Placeholder.component("name", name), Placeholder.unparsed("wins", String.valueOf(pp.minigamesWon())))
                        : huette() ? Texts.text(lang, pp.out() ? "side.player.huette.out" : "side.player.huette",
                        Placeholder.component("name", name),
                        Placeholder.unparsed("hearts", Huette.hearts(pp.lives(), engine.rules().lives())))
                        : Texts.text(lang, "side.player.marathon", Placeholder.unparsed("pos",
                        String.valueOf(places.getOrDefault(pp.uuid(), 0))), Placeholder.component("name", name),
                        Placeholder.unparsed("points", String.valueOf(pp.minigamePoints()))));
                shown++;
            }
            lines.add(Component.text(" "));
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
                .append("\",\"mini\":\"").append(mini == null ? "" : mini.type().id())
                .append("\",\"format\":\"").append(mini == null ? "" : mini.split().format().name().toLowerCase(java.util.Locale.ROOT))
                .append("\",\"sideA\":\"").append(mini == null ? "" : names(mini.split().a()))
                .append("\",\"sideB\":\"").append(mini == null ? "" : names(mini.split().b()))
                .append("\",\"miniPlayers\":").append(mini == null ? 0 : mini.score().all().size())
                .append(",\"miniTicks\":").append(mini == null ? 0 : mini.durationTicks())
                .append(",\"wurmHp\":").append(mini == null || mini.coop() == null ? -1 : mini.coop().hp())
                .append(",\"targets\":[").append(mini == null ? "" : String.join(",", mini.targets().stream()
                        .map(t -> String.format(java.util.Locale.ROOT, "[%.2f,%.2f,%.2f]", t[0], t[1], t[2])).toList())).append("]")
                .append(",\"miniAt\":");
        if (mini == null) {
            b.append("null");
        } else {
            int[] o = Places.mini(table, mini.type());
            b.append('[').append(o[0]).append(',').append(o[1]).append(',').append(o[2]).append(']');
        }
        int[] bo = origin;
        b.append(",\"boardAt\":[").append(bo[0]).append(',').append(bo[1]).append(',').append(bo[2]).append("],\"fields\":\"");
        for (Field f : layout.fields()) {
            b.append(switch (f.type()) {
                case GEGENSTAND -> 'I';
                case FAHRGESCHAEFT -> 'K';
                case LAWINE -> 'A';
                default -> f.type().name().charAt(0);
            });
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
                    .append(p.left()).append(",\"minis\":").append(p.minigamesWon()).append(",\"points\":")
                    .append(p.minigamePoints()).append(",\"shield\":")
                    .append(p.shield()).append(",\"team\":").append(p.team()).append(",\"lives\":").append(p.lives())
                    .append(",\"out\":").append(p.out()).append('}');
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
