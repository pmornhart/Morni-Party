package de.moinmorni.party.play;

import de.moinmorni.party.Items;
import de.moinmorni.party.PartyPlugin;
import de.moinmorni.party.Texts;
import de.moinmorni.party.Ui;
import de.moinmorni.party.board.BoardId;
import de.moinmorni.party.data.PlayerStats;
import de.moinmorni.party.data.Store;
import de.moinmorni.party.game.GameKind;
import de.moinmorni.party.game.Mode;
import de.moinmorni.party.game.Votes;
import de.moinmorni.party.game.WaitQueue;
import de.moinmorni.party.menu.Menu;
import de.moinmorni.party.world.Places;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

/**
 * Der Festplatz: Warteschlange für die nächste Partie, Abstimmung über Spielbrett und Spielart, Countdown, Zuschauen
 * und Statistik. Partys warten zusammen: kommt die Party-Leitung dazu, nimmt sie ihre Leute auf dem Festplatz mit.
 */
public final class Lobby {

    private static final int[] ANNOUNCE = {60, 30, 20, 10, 5, 4, 3, 2, 1};
    private final PartyPlugin plugin;
    private final WaitQueue queue = new WaitQueue();
    private final Votes<BoardId> boardVotes = new Votes<>();
    private final Votes<GameKind> kindVotes = new Votes<>();
    private final Random rng = new Random();
    private int countdown = -1;

    public Lobby(PartyPlugin plugin) {
        this.plugin = plugin;
    }

    public Location spawn() {
        return new Location(plugin.world(), Places.SPAWN_X, Places.FLOOR_Y + 1, Places.SPAWN_Z, Places.SPAWN_YAW, 0f);
    }

    public boolean queued(UUID u) {
        return queue.contains(u);
    }

    public int waiting() {
        return queue.size();
    }

    public int countdown() {
        return countdown;
    }

    public List<GameKind> kinds() {
        List<GameKind> out = new ArrayList<>();
        out.add(new GameKind(Mode.GIPFELRENNEN, 0));
        plugin.settings().huntRounds().forEach(r -> out.add(new GameKind(Mode.EDELWEISS_JAGD, r)));
        plugin.settings().marathonRounds().forEach(r -> out.add(new GameKind(Mode.MARATHON, r)));
        return out;
    }

    /** Höchstzahl für eine Spielart (Bretter 8, Marathon bis 16). */
    public int maxFor(Mode mode) {
        return mode == Mode.MARATHON ? plugin.settings().marathonMax() : plugin.settings().maxPlayers();
    }

    /**
     * Höchstzahl für die nächste Partie, nach dem Stand der Abstimmung: nur wenn der Marathon klar vorn liegt, dürfen
     * mehr mit (bei Gleichstand entscheidet erst der Start – bis dahin gilt die kleinere Zahl).
     */
    public int maxNow() {
        Map<GameKind, Integer> count = kindVotes.count(kinds(), queue.all());
        int marathon = 0;
        int other = 0;
        for (var e : count.entrySet()) {
            if (e.getKey().mode() == Mode.MARATHON) {
                marathon = Math.max(marathon, e.getValue());
            } else {
                other = Math.max(other, e.getValue());
            }
        }
        return marathon > other ? maxFor(Mode.MARATHON) : plugin.settings().maxPlayers();
    }

    /** Name einer Spielart mit Rundenzahl (für Menü und Seitenleiste). */
    public static Component kindName(String lang, GameKind k) {
        return switch (k.mode()) {
            case GIPFELRENNEN -> Texts.text(lang, "mode.gipfelrennen");
            case EDELWEISS_JAGD -> Texts.text(lang, "mode.edelweiss-jagd.rounds", Placeholder.unparsed("n",
                    String.valueOf(k.rounds())));
            case MARATHON -> Texts.text(lang, "mode.marathon.rounds", Placeholder.unparsed("n", String.valueOf(k.rounds())));
        };
    }

    // ---------- Ankommen ----------

    /**
     * Auf den Festplatz (nach dem Betreten oder nach einer Partie). {@code join}: gleich wieder mitspielen (wenn das
     * in der config.yml so eingestellt ist) – nicht nach /verlassen.
     */
    public void enterHub(Player p, boolean join) {
        p.closeInventory();
        p.setGameMode(GameMode.ADVENTURE);
        p.getInventory().clear();
        p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
        var max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(max == null ? 20 : max.getValue());
        p.setFoodLevel(20);
        p.setGlowing(false);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.teleport(spawn());
        if (join && plugin.settings().autoJoin() && plugin.ready()) {
            join(p, true);
        }
        giveItems(p);
        refresh(p);
    }

    public void giveItems(Player p) {
        String lang = plugin.lang(p);
        p.getInventory().clear();
        boolean in = queue.contains(p.getUniqueId());
        p.getInventory().setItem(0, Items.action(Items.glow(Items.named(in ? Material.LIME_DYE : Material.GRAY_DYE, 1,
                Texts.text(lang, in ? "hub.leave" : "hub.join"), Texts.lines(lang, in ? "hub.leave.lore" : "hub.join.lore")),
                in), "join"));
        p.getInventory().setItem(2, Items.action(Items.named(Material.MAP, 1, Texts.text(lang, "hub.vote.board"),
                Texts.lines(lang, "hub.vote.board.lore")), "vote-board"));
        p.getInventory().setItem(3, Items.action(Items.named(Material.CLOCK, 1, Texts.text(lang, "hub.vote.mode"),
                Texts.lines(lang, "hub.vote.mode.lore")), "vote-mode"));
        p.getInventory().setItem(5, Items.action(Items.named(Material.SPYGLASS, 1, Texts.text(lang, "hub.spectate"),
                Texts.lines(lang, "hub.spectate.lore")), "spectate"));
        p.getInventory().setItem(7, Items.action(Items.named(Material.GOLD_INGOT, 1, Texts.text(lang, "hub.stats"),
                Texts.lines(lang, "hub.stats.lore")), "stats"));
        p.getInventory().setItem(8, Items.action(Items.named(Material.RED_BED, 1, Texts.text(lang, "hub.lobby"),
                Texts.lines(lang, "hub.lobby.lore")), "lobby"));
    }

    /** Knopf in der Schnellleiste. */
    public void use(Player p, String action) {
        switch (action) {
            case "join" -> {
                if (queue.contains(p.getUniqueId())) {
                    leaveQueue(p.getUniqueId(), true);
                } else {
                    join(p, true);
                }
            }
            case "vote-board" -> openBoardVote(p);
            case "vote-mode" -> openModeVote(p);
            case "spectate" -> openSpectate(p);
            case "stats" -> openStats(p);
            case "lobby" -> {
                p.sendMessage(Texts.msg(plugin.lang(p), "lobby.going"));
                plugin.net().toLobby(p, Texts.text(plugin.lang(p), "lobby.kick"));
            }
            default -> {
            }
        }
    }

    // ---------- Warteschlange ----------

    /** Mitspielen. {@code bringParty}: als Party-Leitung die eigenen Leute mitnehmen. */
    public void join(Player p, boolean bringParty) {
        UUID u = p.getUniqueId();
        String lang = plugin.lang(p);
        if (plugin.games().involved(u).isPresent()) {
            p.sendMessage(Texts.msg(lang, "queue.ingame"));
            return;
        }
        if (!plugin.ready()) {
            p.sendMessage(Texts.msg(lang, "not-ready"));
            return;
        }
        if (!queue.contains(u)) {
            queue.add(List.of(u));
            p.sendMessage(Texts.msg(lang, "queue.joined", Placeholder.unparsed("n", String.valueOf(queue.size())),
                    Placeholder.unparsed("min", String.valueOf(plugin.settings().minPlayers()))));
            Ui.sound(p, "minecraft:block.note_block.chime", 1.4f);
        }
        giveItems(p);
        refreshAll();
        plugin.net().party(u).thenAccept(party -> plugin.getServer().getScheduler().runTask(plugin,
                () -> party.ifPresent(pt -> joinParty(p, pt, bringParty))));
    }

    private void joinParty(Player p, de.moinmorni.party.net.Network.Party party, boolean bringParty) {
        UUID u = p.getUniqueId();
        if (!queue.contains(u)) {
            return;
        }
        if (party.leader().equals(u)) {
            if (!bringParty) {
                return;
            }
            for (var m : party.members()) {
                Player q = plugin.getServer().getPlayer(m.uuid());
                if (q == null || q.equals(p) || plugin.games().involved(q.getUniqueId()).isPresent()) {
                    continue;
                }
                queue.join(q.getUniqueId(), u);
                q.sendMessage(Texts.msg(plugin.lang(q), "queue.party", Placeholder.unparsed("name", p.getName())));
                giveItems(q);
            }
        } else if (queue.contains(party.leader())) {
            // Mitglied kommt nach: zur Einheit der Leitung
            queue.join(u, party.leader());
        }
        refreshAll();
    }

    /** Warteschlange verlassen. */
    public void leaveQueue(UUID u, boolean tell) {
        boolean was = queue.remove(u);
        boardVotes.forget(u);
        kindVotes.forget(u);
        Player p = plugin.getServer().getPlayer(u);
        if (p != null && tell && was) {
            p.sendMessage(Texts.msg(plugin.lang(p), "queue.left"));
            giveItems(p);
        }
        refreshAll();
    }

    /** Jede Sekunde: Countdown und Start. */
    public void tick() {
        // Wer nicht mehr da ist, wartet nicht mehr
        for (UUID u : queue.all()) {
            if (plugin.getServer().getPlayer(u) == null) {
                queue.remove(u);
            }
        }
        int max = maxNow();
        int fitting = queue.fitting(max);
        if (fitting < plugin.settings().minPlayers()) {
            if (countdown >= 0) {
                countdown = -1;
                for (UUID u : queue.all()) {
                    Player p = plugin.getServer().getPlayer(u);
                    if (p != null) {
                        p.sendMessage(Texts.msg(plugin.lang(p), "queue.stopped"));
                    }
                }
            }
            refreshAll();
            return;
        }
        if (countdown < 0) {
            countdown = plugin.settings().countdown();
        }
        if (fitting >= max) {
            countdown = Math.min(countdown, plugin.settings().fullCountdown());
        }
        for (UUID u : queue.all()) {
            Player p = plugin.getServer().getPlayer(u);
            if (p == null) {
                continue;
            }
            p.sendActionBar(Texts.text(plugin.lang(p), "queue.bar", Placeholder.unparsed("s", String.valueOf(countdown)),
                    Placeholder.unparsed("n", String.valueOf(fitting))));
            for (int a : ANNOUNCE) {
                if (a == countdown) {
                    p.sendMessage(Texts.msg(plugin.lang(p), "queue.countdown", Placeholder.unparsed("s", String.valueOf(a))));
                    Ui.sound(p, "minecraft:block.note_block.hat", 1.5f);
                }
            }
        }
        if (countdown <= 0) {
            startGame();
        } else {
            countdown--;
        }
        refreshAll();
    }

    /** Partie starten (Countdown abgelaufen oder /mparty start). */
    public boolean startGame() {
        int table = plugin.games().freeTable();
        if (table < 0) {
            countdown = 5;
            for (UUID u : queue.all()) {
                Player p = plugin.getServer().getPlayer(u);
                if (p != null) {
                    p.sendMessage(Texts.msg(plugin.lang(p), "queue.tables"));
                }
            }
            return false;
        }
        GameKind kind = kindVotes.winner(kinds(), queue.all(), rng);
        List<UUID> ids = queue.take(maxFor(kind.mode()));
        List<Player> players = new ArrayList<>();
        for (UUID u : ids) {
            Player p = plugin.getServer().getPlayer(u);
            if (p != null) {
                players.add(p);
            }
        }
        countdown = -1;
        if (players.size() < 2) {
            players.forEach(p -> queue.add(List.of(p.getUniqueId())));
            return false;
        }
        List<BoardId> boards = List.of(BoardId.values());
        BoardId board = boardVotes.winner(boards, ids, rng);
        ids.forEach(u -> {
            boardVotes.forget(u);
            kindVotes.forget(u);
        });
        Game g = new Game(plugin, table, board, kind, players);
        plugin.games().add(g);
        g.start();
        refreshAll();
        return true;
    }

    // ---------- Abstimmung ----------

    private void openBoardVote(Player p) {
        String lang = plugin.lang(p);
        Menu m = new Menu(3, Texts.text(lang, "vote.board.title"));
        Map<BoardId, Integer> count = boardVotes.count(List.of(BoardId.values()), queue.all());
        Material[] icons = {Material.GRASS_BLOCK, Material.QUARTZ_BRICKS, Material.GOLD_ORE};
        BoardId[] all = BoardId.values();
        for (int i = 0; i < all.length; i++) {
            BoardId b = all[i];
            boolean mine = b == boardVotes.of(p.getUniqueId());
            List<Component> lore = new ArrayList<>(Texts.lines(lang, "board." + b.id() + ".desc"));
            lore.add(Texts.text(lang, "vote.count", Placeholder.unparsed("n", String.valueOf(count.get(b)))));
            lore.add(Texts.text(lang, mine ? "vote.mine" : "vote.click"));
            m.set(11 + i * 2, Items.glow(Items.named(icons[i], Math.max(1, count.get(b)), Texts.text(lang, "board." + b.id()),
                    lore), mine), who -> vote(who, () -> boardVotes.vote(who.getUniqueId(), b), Texts.text(plugin.lang(who),
                            "board." + b.id())));
        }
        m.open(p);
    }

    /**
     * Abstimmung über die Spielart: jede Spielart hat eine halbe Zeile (links oder rechts), daneben ihre Rundenzahlen –
     * so passen auch viele Spielarten in ein Menü.
     */
    private void openModeVote(Player p) {
        String lang = plugin.lang(p);
        List<GameKind> kinds = kinds();
        List<Mode> modes = kinds.stream().map(GameKind::mode).distinct().toList();
        int rows = Math.max(1, Math.min(6, (modes.size() + 1) / 2));
        Menu m = new Menu(rows, Texts.text(lang, "vote.mode.title"));
        Map<GameKind, Integer> count = kindVotes.count(kinds, queue.all());
        for (int mi = 0; mi < modes.size() && mi < 2 * rows; mi++) {
            Mode mode = modes.get(mi);
            int base = 9 * (mi / 2) + (mi % 2 == 0 ? 0 : 5);
            List<GameKind> options = kinds.stream().filter(k -> k.mode() == mode).limit(4).toList();
            for (int j = 0; j < options.size(); j++) {
                GameKind k = options.get(j);
                boolean mine = k.equals(kindVotes.of(p.getUniqueId()));
                Component name = kindName(lang, k);
                List<Component> lore = new ArrayList<>(Texts.lines(lang, "mode." + k.mode().id() + ".desc"));
                lore.add(Texts.text(lang, "vote.count", Placeholder.unparsed("n", String.valueOf(count.get(k)))));
                lore.add(Texts.text(lang, mine ? "vote.mine" : "vote.click"));
                m.set(base + j, Items.glow(Items.named(icon(mode), Math.max(1, count.get(k)), name, lore), mine),
                        who -> vote(who, () -> kindVotes.vote(who.getUniqueId(), k), name));
            }
        }
        m.open(p);
    }

    private static Material icon(Mode mode) {
        return switch (mode) {
            case GIPFELRENNEN -> Material.LEATHER_BOOTS;
            case EDELWEISS_JAGD -> Material.LILY_OF_THE_VALLEY;
            case MARATHON -> Material.GOLDEN_BOOTS;
        };
    }

    private void vote(Player p, Runnable doVote, Component what) {
        doVote.run();
        p.closeInventory();
        p.sendMessage(Texts.msg(plugin.lang(p), "vote.done", Placeholder.component("what", what)));
        Ui.sound(p, "minecraft:entity.villager.yes", 1.2f);
        if (!queue.contains(p.getUniqueId())) {
            join(p, true);
        }
        refreshAll();
    }

    // ---------- Zuschauen und Statistik ----------

    private void openSpectate(Player p) {
        String lang = plugin.lang(p);
        List<Game> games = plugin.games().all();
        Menu m = new Menu(1, Texts.text(lang, "spectate.title"));
        if (games.isEmpty()) {
            m.set(4, Items.named(Material.BARRIER, 1, Texts.text(lang, "spectate.none"), null), null);
        }
        for (int i = 0; i < games.size() && i < 9; i++) {
            Game g = games.get(i);
            List<Component> lore = new ArrayList<>();
            lore.add(Texts.text(lang, "spectate.info", Placeholder.unparsed("names",
                    String.join(", ", g.players().stream().map(Player::getName).toList())),
                    Placeholder.unparsed("round", String.valueOf(g.engine().round()))));
            m.set(i, Items.named(Material.SPYGLASS, 1, Texts.text(lang, "board." + g.board().id()), lore), who -> {
                who.closeInventory();
                watch(who, g);
            });
        }
        m.open(p);
    }

    /** Zuschauen bei einer Partie. */
    public void watch(Player p, Game g) {
        leaveQueue(p.getUniqueId(), false);
        g.watch(p);
    }

    private void openStats(Player p) {
        String lang = plugin.lang(p);
        Menu m = new Menu(3, Texts.text(lang, "stats.title"));
        Optional<PlayerStats> s = plugin.store().read(p.getUniqueId());
        PlayerStats st = s.orElseGet(() -> new PlayerStats(p.getUniqueId(), p.getName()));
        m.set(11, Items.named(Material.PLAYER_HEAD, 1, Texts.text(lang, "stats.mine"), statLines(lang, st)), null);
        List<Component> top = new ArrayList<>();
        int i = 1;
        for (Store.Entry e : plugin.store().top(10)) {
            top.add(Texts.text(lang, "top.line", Placeholder.unparsed("pos", String.valueOf(i++)),
                    Placeholder.unparsed("name", e.name()), Placeholder.unparsed("wins", String.valueOf(e.wins())),
                    Placeholder.unparsed("edelweiss", String.valueOf(e.edelweiss()))));
        }
        if (top.isEmpty()) {
            top.add(Texts.text(lang, "top.empty"));
        }
        m.set(15, Items.named(Material.GOLD_BLOCK, 1, Texts.text(lang, "top.title"), top), null);
        m.open(p);
    }

    public List<Component> statLines(String lang, PlayerStats s) {
        List<Component> out = new ArrayList<>(Texts.lines(lang, "stats.lines", Placeholder.unparsed("games", String.valueOf(s.games())),
                Placeholder.unparsed("wins", String.valueOf(s.wins())),
                Placeholder.unparsed("edelweiss", String.valueOf(s.edelweiss())),
                Placeholder.unparsed("best", String.valueOf(s.bestEdelweiss())),
                Placeholder.unparsed("minis", String.valueOf(s.minigamesWon())),
                Placeholder.unparsed("summits", String.valueOf(s.summits()))));
        for (Mode mode : Mode.values()) {
            if (s.modeGames(mode.id()) > 0) {
                out.add(Texts.text(lang, "stats.mode", Placeholder.component("mode", Texts.text(lang, "mode." + mode.id())),
                        Placeholder.unparsed("games", String.valueOf(s.modeGames(mode.id()))),
                        Placeholder.unparsed("wins", String.valueOf(s.modeWins(mode.id())))));
            }
        }
        return out;
    }

    // ---------- Seitenleiste ----------

    public void refreshAll() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (plugin.games().involved(p.getUniqueId()).isEmpty()) {
                refresh(p);
            }
        }
    }

    public void refresh(Player p) {
        String lang = plugin.lang(p);
        List<Component> lines = new ArrayList<>();
        lines.add(Texts.text(lang, "hub.side.place"));
        lines.add(Component.empty());
        lines.add(Texts.text(lang, "hub.side.waiting", Placeholder.unparsed("n", String.valueOf(queue.size())),
                Placeholder.unparsed("max", String.valueOf(maxNow()))));
        lines.add(countdown >= 0 ? Texts.text(lang, "hub.side.countdown", Placeholder.unparsed("s", String.valueOf(countdown)))
                : Texts.text(lang, "hub.side.need", Placeholder.unparsed("min", String.valueOf(plugin.settings().minPlayers()))));
        lines.add(Texts.text(lang, queue.contains(p.getUniqueId()) ? "hub.side.in" : "hub.side.out"));
        lines.add(Component.empty());
        BoardId b = boardVotes.of(p.getUniqueId());
        GameKind k = kindVotes.of(p.getUniqueId());
        lines.add(Texts.text(lang, "hub.side.vote"));
        lines.add(b == null ? Texts.text(lang, "hub.side.novote") : Texts.text(lang, "board." + b.id() + ".short"));
        lines.add(k == null ? Texts.text(lang, "hub.side.novote") : kindName(lang, k));
        lines.add(Component.empty());
        lines.add(Texts.text(lang, "hub.side.games", Placeholder.unparsed("n", String.valueOf(plugin.games().running()))));
        lines.add(Texts.text(lang, "side.address"));
        plugin.sidebar().show(p, Texts.text(lang, "side.title"), lines);
    }

    public void forget(UUID u) {
        leaveQueue(u, false);
    }
}
