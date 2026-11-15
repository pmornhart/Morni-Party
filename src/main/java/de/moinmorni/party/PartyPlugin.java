package de.moinmorni.party;

import de.moinmorni.party.board.BoardId;
import de.moinmorni.party.board.BoardLayout;
import de.moinmorni.party.data.Store;
import de.moinmorni.party.listen.GuardListener;
import de.moinmorni.party.listen.PlayerListener;
import de.moinmorni.party.mini.MiniType;
import de.moinmorni.party.net.Network;
import de.moinmorni.party.play.Games;
import de.moinmorni.party.play.Lobby;
import de.moinmorni.party.play.Sidebar;
import de.moinmorni.party.world.BoardPlans;
import de.moinmorni.party.world.Construction;
import de.moinmorni.party.world.HubSigns;
import de.moinmorni.party.world.Places;
import de.moinmorni.party.world.Plan;
import de.moinmorni.party.world.VoidGenerator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Morni Party: ein Brettspiel-Abend für 2–8 – würfeln, ziehen, Taler sammeln, Edelweiß kaufen, nach jeder Runde ein
 * Minispiel. Festplatz mit Warteschlange und Abstimmung, drei Spielbretter, zwölf Minispiele. Spricht mit dem
 * MoinMorni-Netzwerk, wenn es da ist, und läuft sonst allein.
 */
public final class PartyPlugin extends JavaPlugin {

    private static final String ADMIN = "moinmorni.party.admin";
    private static final long SECOND = 20L;
    private static final long STATE_REFRESH = 20L * 60;
    private static final long LANGUAGE_CHECK = 100L;

    private Settings settings;
    private Network net;
    private Store store;
    private World world;
    private Lobby lobby;
    private Games games;
    private Sidebar sidebar;
    private HubSigns signs;
    private Construction construction;
    private final Map<BoardId, BoardLayout> layouts = new EnumMap<>(BoardId.class);
    private final Map<MiniType, Plan> arenaPlans = new EnumMap<>(MiniType.class);
    private volatile boolean ready;
    private boolean stateQueued;
    private String lastState = "";
    private int lastPlayers = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = Settings.load(getConfig());
        Items.init(this);
        net = Network.connect(this);
        getLogger().info(net.connected()
                ? "Mit dem MoinMorni-Netzwerk verbunden (Server „" + net.serverName() + "“)."
                : "Kein Netzwerk-Plugin: Morni Party läuft allein (deutsche Texte, Statistik nur lokal).");
        store = new Store(getLogger(), getDataFolder().toPath());
        world = createWorld(settings.world());
        sidebar = new Sidebar();
        games = new Games(settings.tables());
        lobby = new Lobby(this);
        signs = new HubSigns(this);
        Map<BoardId, Plan> boardPlans = new EnumMap<>(BoardId.class);
        for (BoardId b : BoardId.values()) {
            BoardLayout l = BoardLayout.of(b);
            layouts.put(b, l);
            boardPlans.put(b, BoardPlans.build(l));
        }
        for (MiniType t : MiniType.values()) {
            arenaPlans.put(t, t.plan());
        }

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new GuardListener(this), this);
        Commands commands = new Commands(this);
        for (String name : List.of("mitspielen", "verlassen", "zuschauen", "stats", "top", "mparty")) {
            PluginCommand c = getCommand(name);
            if (c != null) {
                c.setExecutor(commands);
                c.setTabCompleter(commands);
            }
        }

        construction = new Construction(this, world);
        construction.ensure(settings.tables(), boardPlans, arenaPlans, false, () -> {
            ready = true;
            getLogger().info("Morni Party ist bereit: " + settings.tables() + " Tische, " + BoardId.values().length
                    + " Spielbretter, " + MiniType.values().length + " Minispiele.");
            for (Player p : getServer().getOnlinePlayers()) {
                if (games.involved(p.getUniqueId()).isEmpty()) {
                    lobby.enterHub(p, true);
                }
            }
        });
        signs.spawn();

        var scheduler = getServer().getScheduler();
        scheduler.runTaskTimer(this, games::tick, 1L, 1L);
        scheduler.runTaskTimer(this, lobby::tick, SECOND, SECOND);
        scheduler.runTaskTimer(this, () -> reportState(true), STATE_REFRESH, STATE_REFRESH);
        scheduler.runTaskTimer(this, signs::update, LANGUAGE_CHECK, LANGUAGE_CHECK);

        for (Player p : getServer().getOnlinePlayers()) {
            lobby.enterHub(p, false);
        }
        stateChanged();
    }

    @Override
    public void onDisable() {
        if (games != null) {
            games.closeAll();
        }
        if (signs != null) {
            signs.remove();
        }
        if (store != null) {
            store.close();
        }
    }

    private World createWorld(String name) {
        World w = getServer().getWorld(name);
        if (w == null) {
            w = new WorldCreator(name).generator(new VoidGenerator()).environment(World.Environment.NORMAL)
                    .generateStructures(false).createWorld();
        }
        if (w == null) {
            throw new IllegalStateException("Welt " + name + " ließ sich nicht anlegen");
        }
        w.setGameRule(GameRules.ADVANCE_TIME, false);
        w.setGameRule(GameRules.ADVANCE_WEATHER, false);
        w.setGameRule(GameRules.SPAWN_MOBS, false);
        w.setGameRule(GameRules.SPAWN_MONSTERS, false);
        w.setGameRule(GameRules.SPAWN_PHANTOMS, false);
        w.setGameRule(GameRules.SPAWN_PATROLS, false);
        w.setGameRule(GameRules.SPAWN_WANDERING_TRADERS, false);
        w.setGameRule(GameRules.SHOW_DEATH_MESSAGES, false);
        w.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
        w.setGameRule(GameRules.IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);
        w.setGameRule(GameRules.MOB_GRIEFING, false);
        w.setGameRule(GameRules.BLOCK_DROPS, false);
        w.setGameRule(GameRules.ENTITY_DROPS, false);
        w.setGameRule(GameRules.FALL_DAMAGE, false);
        w.setGameRule(GameRules.KEEP_INVENTORY, true);
        w.setGameRule(GameRules.LOCATOR_BAR, false);
        w.setTime(6000L);
        w.setStorm(false);
        w.setThundering(false);
        w.setDifficulty(Difficulty.PEACEFUL);
        w.setSpawnLocation(new org.bukkit.Location(w, Places.SPAWN_X, Places.FLOOR_Y + 1, Places.SPAWN_Z));
        return w;
    }

    // ---------- Zugriff ----------

    public Settings settings() {
        return settings;
    }

    public Network net() {
        return net;
    }

    public Store store() {
        return store;
    }

    public World world() {
        return world;
    }

    public Lobby lobby() {
        return lobby;
    }

    public Games games() {
        return games;
    }

    public Sidebar sidebar() {
        return sidebar;
    }

    public HubSigns signs() {
        return signs;
    }

    public BoardLayout layout(BoardId id) {
        return layouts.get(id);
    }

    public Plan arenaPlan(MiniType t) {
        return arenaPlans.get(t);
    }

    /** Alles gebaut? Vorher gibt es keine Partien. */
    public boolean ready() {
        return ready;
    }

    public String lang(Player p) {
        return net.language(p.getUniqueId());
    }

    /** Darf hier bauen (Verwaltung im Kreativmodus)? */
    public boolean builder(Player p) {
        return p.getGameMode() == GameMode.CREATIVE && p.hasPermission(ADMIN);
    }

    /** Zustand fürs Netzwerk (Lobby-Figur, Webseite) – gesammelt, höchstens einmal pro Tick. */
    public void stateChanged() {
        if (stateQueued || !isEnabled()) {
            return;
        }
        stateQueued = true;
        getServer().getScheduler().runTask(this, () -> {
            stateQueued = false;
            reportState(false);
        });
    }

    private void reportState(boolean force) {
        String state = games.running() > 0 ? "running" : "waiting";
        int players = getServer().getOnlinePlayers().size();
        if (force || !state.equals(lastState) || players != lastPlayers) {
            lastState = state;
            lastPlayers = players;
            net.state(state, players);
        }
    }

    /** /mparty neuladen: Zeiten und Regeln aus der config.yml (gilt für neue Partien). */
    void reloadSettings() {
        reloadConfig();
        Settings fresh = Settings.load(getConfig());
        settings = new Settings(settings.world(), settings.tables(), fresh.minPlayers(), fresh.maxPlayers(),
                fresh.countdown(), fresh.fullCountdown(), fresh.autoJoin(), fresh.huntRounds(), fresh.turnSeconds(),
                fresh.menuSeconds(), fresh.stepTicks(), fresh.minigames(), fresh.fixedOrder(), fresh.miniFactor(),
                fresh.rules());
    }

    /** /mparty neubauen: alles abbrechen und frisch bauen. */
    void rebuild(Runnable done) {
        if (construction.running()) {
            return;
        }
        ready = false;
        games.closeAll();
        Map<BoardId, Plan> boardPlans = new EnumMap<>(BoardId.class);
        layouts.forEach((b, l) -> boardPlans.put(b, BoardPlans.build(l)));
        construction.ensure(settings.tables(), boardPlans, arenaPlans, true, () -> {
            ready = true;
            done.run();
        });
    }
}
