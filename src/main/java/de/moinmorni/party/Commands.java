package de.moinmorni.party;

import de.moinmorni.party.board.FieldType;
import de.moinmorni.party.data.PlayerStats;
import de.moinmorni.party.data.Store;
import de.moinmorni.party.game.ItemType;
import de.moinmorni.party.game.PartyPlayer;
import de.moinmorni.party.mini.MiniType;
import de.moinmorni.party.play.Game;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** Befehle: /mitspielen, /verlassen, /zuschauen, /stats, /top und die Verwaltung /mparty. */
final class Commands implements TabExecutor {

    private static final String ADMIN = "moinmorni.party.admin";
    private static final List<String> ADMIN_SUB = List.of("info", "zustand", "start", "stopp", "wuerfel", "taler",
            "edelweiss", "feld", "stand", "gegenstand", "minispiel", "seiten", "neubauen", "neuladen");
    private final PartyPlugin plugin;

    Commands(PartyPlugin plugin) {
        this.plugin = plugin;
    }

    private String lang(CommandSender s) {
        return s instanceof Player p ? plugin.lang(p) : "de";
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        String lang = lang(s);
        switch (cmd.getName()) {
            case "mitspielen" -> {
                if (s instanceof Player p) {
                    plugin.lobby().join(p, true);
                } else {
                    s.sendMessage(Texts.msg(lang, "only-players"));
                }
            }
            case "verlassen" -> {
                if (!(s instanceof Player p)) {
                    s.sendMessage(Texts.msg(lang, "only-players"));
                    return true;
                }
                Optional<Game> g = plugin.games().involved(p.getUniqueId());
                if (g.isPresent()) {
                    g.get().leave(p, true);
                    plugin.lobby().enterHub(p, false);
                    p.sendMessage(Texts.msg(lang, "leave.game"));
                } else if (plugin.lobby().queued(p.getUniqueId())) {
                    plugin.lobby().leaveQueue(p.getUniqueId(), true);
                } else {
                    p.sendMessage(Texts.msg(lang, "leave.nothing"));
                }
            }
            case "zuschauen" -> spectate(s, args);
            case "stats" -> stats(s, args);
            case "top" -> top(s);
            case "mparty" -> admin(s, args);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void spectate(CommandSender s, String[] args) {
        String lang = lang(s);
        if (!(s instanceof Player p)) {
            s.sendMessage(Texts.msg(lang, "only-players"));
            return;
        }
        if (plugin.games().involved(p.getUniqueId()).isPresent()) {
            p.sendMessage(Texts.msg(lang, "queue.ingame"));
            return;
        }
        if (args.length == 0) {
            plugin.lobby().use(p, "spectate");
            return;
        }
        Player t = plugin.getServer().getPlayerExact(args[0]);
        Optional<Game> g = t == null ? Optional.empty() : plugin.games().of(t.getUniqueId());
        if (g.isEmpty()) {
            p.sendMessage(Texts.msg(lang, "spectate.notplaying", Placeholder.unparsed("name", args[0])));
            return;
        }
        plugin.lobby().watch(p, g.get());
    }

    private void stats(CommandSender s, String[] args) {
        String lang = lang(s);
        String name = args.length > 0 ? args[0] : s.getName();
        UUID u = null;
        Player online = plugin.getServer().getPlayerExact(name);
        if (online != null) {
            u = online.getUniqueId();
        } else {
            u = plugin.store().find(name).orElse(null);
        }
        Optional<PlayerStats> st = u == null ? Optional.empty() : plugin.store().read(u);
        if (st.isEmpty()) {
            s.sendMessage(Texts.msg(lang, "stats.none", Placeholder.unparsed("name", name)));
            return;
        }
        s.sendMessage(Texts.msg(lang, "stats.header", Placeholder.unparsed("name", st.get().name())));
        plugin.lobby().statLines(lang, st.get()).forEach(s::sendMessage);
    }

    private void top(CommandSender s) {
        String lang = lang(s);
        s.sendMessage(Texts.msg(lang, "top.title"));
        List<Store.Entry> top = plugin.store().top(10);
        if (top.isEmpty()) {
            s.sendMessage(Texts.text(lang, "top.empty"));
        }
        int i = 1;
        for (Store.Entry e : top) {
            s.sendMessage(Texts.text(lang, "top.line", Placeholder.unparsed("pos", String.valueOf(i++)),
                    Placeholder.unparsed("name", e.name()), Placeholder.unparsed("wins", String.valueOf(e.wins())),
                    Placeholder.unparsed("edelweiss", String.valueOf(e.edelweiss()))));
        }
    }

    // ---------- Verwaltung ----------

    private void admin(CommandSender s, String[] args) {
        String lang = lang(s);
        if (!s.hasPermission(ADMIN)) {
            s.sendMessage(Texts.msg(lang, "no-permission"));
            return;
        }
        if (args.length == 0) {
            s.sendMessage(Texts.msg(lang, "admin.usage"));
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "info" -> s.sendMessage(Texts.msg(lang, "admin.info",
                    Placeholder.unparsed("games", String.valueOf(plugin.games().running())),
                    Placeholder.unparsed("waiting", String.valueOf(plugin.lobby().waiting())),
                    Placeholder.unparsed("ready", String.valueOf(plugin.ready())),
                    Placeholder.unparsed("net", plugin.net().connected() ? "ja/yes" : "nein/no")));
            case "zustand" -> {
                StringBuilder b = new StringBuilder("{\"ready\":").append(plugin.ready()).append(",\"waiting\":")
                        .append(plugin.lobby().waiting()).append(",\"countdown\":").append(plugin.lobby().countdown())
                        .append(",\"games\":[");
                List<Game> all = plugin.games().all();
                for (int i = 0; i < all.size(); i++) {
                    b.append(i > 0 ? "," : "").append(all.get(i).json());
                }
                s.sendMessage(Component.text(b.append("]}").toString()));
            }
            case "start" -> s.sendMessage(Texts.msg(lang, plugin.lobby().startGame() ? "admin.started" : "admin.notstarted"));
            case "stopp" -> {
                plugin.games().all().forEach(g -> {
                    g.engine().stop();
                    g.end();
                });
                s.sendMessage(Texts.msg(lang, "admin.stopped"));
            }
            case "wuerfel", "taler", "edelweiss", "feld", "gegenstand" -> playerValue(s, sub, args);
            case "stand" -> {
                Integer n = args.length > 1 ? number(args[1]) : null;
                Game g = plugin.games().all().stream().findFirst().orElse(null);
                if (g == null || n == null) {
                    s.sendMessage(Texts.msg(lang, "admin.nogame"));
                    return;
                }
                g.engine().setStand(n);
                g.respot();
                s.sendMessage(Texts.msg(lang, "admin.ok"));
            }
            case "minispiel" -> {
                MiniType t = args.length > 1 ? MiniType.parse(args[1]) : null;
                if (t == null) {
                    s.sendMessage(Texts.msg(lang, "admin.minis", Placeholder.unparsed("list",
                            String.join(", ", Stream.of(MiniType.values()).map(MiniType::id).toList()))));
                    return;
                }
                plugin.games().all().forEach(g -> g.forceMini(t));
                s.sendMessage(Texts.msg(lang, "admin.ok"));
            }
            case "seiten" -> {
                String w = args.length > 1 ? args[1].toLowerCase(java.util.Locale.ROOT) : "";
                de.moinmorni.party.game.Teams.Format f = switch (w) {
                    case "frei", "free" -> de.moinmorni.party.game.Teams.Format.FREE;
                    case "team" -> de.moinmorni.party.game.Teams.Format.TEAM;
                    case "einzel", "solo" -> de.moinmorni.party.game.Teams.Format.SOLO;
                    default -> null;
                };
                if (f == null && !w.equals("auto")) {
                    s.sendMessage(Texts.msg(lang, "admin.sides"));
                    return;
                }
                plugin.games().all().forEach(g -> g.forceFormat(f));
                s.sendMessage(Texts.msg(lang, "admin.ok"));
            }
            case "neubauen" -> {
                s.sendMessage(Texts.msg(lang, "admin.rebuild"));
                plugin.rebuild(() -> s.sendMessage(Texts.msg(lang, "admin.rebuilt")));
            }
            case "neuladen" -> {
                plugin.reloadSettings();
                s.sendMessage(Texts.msg(lang, "admin.reloaded"));
            }
            default -> s.sendMessage(Texts.msg(lang, "admin.usage"));
        }
    }

    /** /mparty wuerfel|taler|edelweiss|feld <spieler> <zahl> · gegenstand <spieler> <gegenstand>. */
    private void playerValue(CommandSender s, String sub, String[] args) {
        String lang = lang(s);
        if (args.length < 3) {
            s.sendMessage(Texts.msg(lang, "admin.usage"));
            return;
        }
        Optional<PartyPlayer> found = Optional.empty();
        Game game = null;
        for (Game g : plugin.games().all()) {
            found = g.find(args[1]);
            if (found.isPresent()) {
                game = g;
                break;
            }
        }
        if (found.isEmpty()) {
            s.sendMessage(Texts.msg(lang, "admin.noplayer", Placeholder.unparsed("name", args[1])));
            return;
        }
        PartyPlayer p = found.get();
        if (sub.equals("gegenstand")) {
            ItemType t = ItemType.parse(args[2]);
            if (t == null || !game.engine().giveItem(p, t)) {
                s.sendMessage(Texts.msg(lang, "admin.noitem"));
                return;
            }
        } else {
            Integer n = number(args[2]);
            if (n == null) {
                s.sendMessage(Texts.msg(lang, "admin.usage"));
                return;
            }
            switch (sub) {
                case "wuerfel" -> p.fixNextRoll(Math.max(1, Math.min(12, n)));
                case "taler" -> p.setTaler(n);
                case "edelweiss" -> p.setEdelweiss(n);
                case "feld" -> p.setPos(Math.max(0, Math.min(game.engine().board().goal(), n)));
                default -> {
                }
            }
        }
        game.respot();
        s.sendMessage(Texts.msg(lang, "admin.ok"));
    }

    private static Integer number(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] args) {
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        switch (cmd.getName()) {
            case "zuschauen", "stats" -> {
                if (args.length == 1) {
                    plugin.getServer().getOnlinePlayers().forEach(p -> options.add(p.getName()));
                }
            }
            case "mparty" -> {
                if (!s.hasPermission(ADMIN)) {
                    return List.of();
                }
                if (args.length == 1) {
                    options.addAll(ADMIN_SUB);
                } else if (args.length == 2 && args[0].equalsIgnoreCase("minispiel")) {
                    Stream.of(MiniType.values()).map(MiniType::id).forEach(options::add);
                } else if (args.length == 2) {
                    plugin.getServer().getOnlinePlayers().forEach(p -> options.add(p.getName()));
                } else if (args.length == 3 && args[0].equalsIgnoreCase("gegenstand")) {
                    Stream.of(ItemType.values()).map(ItemType::id).forEach(options::add);
                }
            }
            default -> {
            }
        }
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(last)).toList();
    }

}
