package de.moinmorni.party;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Alle Texte für Spieler, auf Deutsch und Englisch, als MiniMessage. Eigene Farben: {@code <em>} Smaragd,
 * {@code <au>} Gold, {@code <bl>} Weiß-Blau, {@code <ed>} Edelweiß-Weiß. Namen von Spielern kommen immer als {@code Placeholder.unparsed} hinein.
 * Gezählte Wörter stehen mit Einzahl und Mehrzahl im Text: {@code <points> <points_pl:'0#Punkte|1#Punkt|1<Punkte'/>}
 * (Zahl und Wort kommen zusammen über {@link #count}).
 */
public final class Texts {

    public static final int EMERALD = 0x2BB673;
    public static final int GOLD = 0xF2B632;
    public static final int BLUE = 0x5DA9E9;
    public static final int EDELWEISS = 0xF4F1E6;

    /** MiniMessage mit unseren Farben. */
    public static final MiniMessage MM = MiniMessage.builder()
            .editTags(t -> t.resolvers(
                    TagResolver.resolver("em", Tag.styling(TextColor.color(EMERALD))),
                    TagResolver.resolver("au", Tag.styling(TextColor.color(GOLD))),
                    TagResolver.resolver("bl", Tag.styling(TextColor.color(BLUE))),
                    TagResolver.resolver("ed", Tag.styling(TextColor.color(EDELWEISS)))))
            .build();

    private static final Map<String, String[]> TEXTS = new LinkedHashMap<>();

    private Texts() {
    }

    private static void t(String key, String de, String en) {
        if (TEXTS.put(key, new String[] {de, en}) != null) {
            throw new IllegalStateException("Text doppelt: " + key);
        }
    }

    static {
        t("prefix", "<ed>✿</ed> <au>Party</au> <dark_gray>»</dark_gray> ", "<ed>✿</ed> <au>Party</au> <dark_gray>»</dark_gray> ");

        // ---------- Allgemein ----------
        t("only-players", "Das geht nur im Spiel.",
                "Only possible in-game.");
        t("no-permission", "<red>Das darf nur das Team.</red>",
                "<red>Only the team can do that.</red>");
        t("not-ready", "<gray>Die Spielbretter werden gerade aufgebaut – gleich geht's los!</gray>",
                "<gray>The game boards are being set up – just a moment!</gray>");
        t("join.title", "<ed>✿</ed> <au>Morni Party</au> <ed>✿</ed>", "<ed>✿</ed> <au>Morni Party</au> <ed>✿</ed>");
        t("join.subtitle", "<gray>Willkommen auf dem <em>Festplatz</em>!</gray>",
                "<gray>Welcome to the <em>fairground</em>!</gray>");
        t("join.welcome", "<gray>Willkommen bei <au>Morni Party</au>! Würfeln, Taler sammeln, <ed>Edelweiß</ed> kaufen – und nach jeder Runde ein Minispiel. <white>Graue Farbe</white> in der Hand: mitspielen. <white>Karte</white> und <white>Uhr</white>: abstimmen.</gray>",
                "<gray>Welcome to <au>Morni Party</au>! Roll the dice, collect coins (Taler), buy <ed>Edelweiss</ed> – and play a minigame after every round. <white>Dye</white> in your hand: join. <white>Map</white> and <white>clock</white>: vote.</gray>");
        t("lobby.kick", "<au>MoinMorni</au>\n<gray>Zurück in die Lobby: Bitte verbinde dich neu mit dem Netzwerk.</gray>",
                "<au>MoinMorni</au>\n<gray>Back to the lobby: please reconnect to the network.</gray>");
        t("lobby.going", "<gray>Ab in die Lobby – bis bald!</gray>",
                "<gray>Off to the lobby – see you soon!</gray>");
        t("leave.game", "<gray>Du hast die Partie verlassen. Zurück auf dem Festplatz!</gray>",
                "<gray>You left the game. Back on the fairground!</gray>");
        t("leave.nothing", "<gray>Du wartest gerade auf nichts und spielst nicht mit.</gray>",
                "<gray>You are not waiting or playing right now.</gray>");

        // ---------- Festplatz ----------
        t("hub.join", "<em>▶ Mitspielen</em>",
                "<em>▶ Play</em>");
        t("hub.join.lore", "<gray>Rechtsklick: für die nächste Partie anmelden.</gray>",
                "<gray>Right-click: join the next game.</gray>");
        t("hub.leave", "<au>✔ Du wartest</au> <gray>(abmelden)</gray>",
                "<au>✔ You are waiting</au> <gray>(leave)</gray>");
        t("hub.leave.lore", "<gray>Rechtsklick: doch nicht mitspielen.</gray>",
                "<gray>Right-click: don't play after all.</gray>");
        t("hub.vote.board", "<em>⛰ Spielbrett wählen</em>",
                "<em>⛰ Vote for a board</em>");
        t("hub.vote.board.lore", "<gray>Rechtsklick: Almsommer, Schloss oder Goldstollen?</gray>",
                "<gray>Right-click: Alpine Summer, Castle or Gold Mine?</gray>");
        t("hub.vote.mode", "<em>⌚ Spielart wählen</em>",
                "<em>⌚ Vote for a game type</em>");
        t("hub.vote.mode.lore", "<gray>Rechtsklick: Gipfelrennen oder Edelweiß-Jagd?</gray>",
                "<gray>Right-click: Summit Race or Edelweiss Hunt?</gray>");
        t("hub.spectate", "<bl>◎ Zuschauen</bl>",
                "<bl>◎ Spectate</bl>");
        t("hub.spectate.lore", "<gray>Rechtsklick: einer laufenden Partie zuschauen.</gray>",
                "<gray>Right-click: watch a running game.</gray>");
        t("hub.stats", "<au>★ Statistik & Bestenliste</au>",
                "<au>★ Stats & leaderboard</au>");
        t("hub.stats.lore", "<gray>Rechtsklick: deine Werte und die Besten.</gray>",
                "<gray>Right-click: your stats and the best players.</gray>");
        t("hub.lobby", "<bl>⌂ Zurück zur Lobby</bl>",
                "<bl>⌂ Back to the lobby</bl>");
        t("hub.lobby.lore", "<gray>Rechtsklick: zurück ins Dorf.</gray>",
                "<gray>Right-click: back to the village.</gray>");
        t("hub.side.place", "<em>Festplatz</em>",
                "<em>Fairground</em>");
        t("hub.side.waiting", "<gray>Wartend:</gray> <white><n></white><gray>/<max></gray>",
                "<gray>Waiting:</gray> <white><n></white><gray>/<max></gray>");
        t("hub.side.countdown", "<gray>Start in</gray> <au><s> s</au>", "<gray>Start in</gray> <au><s> s</au>");
        t("hub.side.need", "<gray>Ab <white><min></white> geht's los</gray>",
                "<gray>Starts with <white><min></white> players</gray>");
        t("hub.side.in", "<em>✔ Du spielst mit</em>",
                "<em>✔ You are playing</em>");
        t("hub.side.out", "<gray>✖ Du spielst nicht mit</gray>",
                "<gray>✖ You are not playing</gray>");
        t("hub.side.vote", "<gray>Deine Wahl:</gray>",
                "<gray>Your vote:</gray>");
        t("hub.side.novote", "<dark_gray>– noch offen –</dark_gray>",
                "<dark_gray>– not yet –</dark_gray>");
        t("hub.side.games", "<gray>Partien:</gray> <white><n></white>",
                "<gray>Games:</gray> <white><n></white>");
        t("side.title", "<ed><b>✿</b></ed> <au><b>Morni Party</b></au> <ed><b>✿</b></ed>", "<ed><b>✿</b></ed> <au><b>Morni Party</b></au> <ed><b>✿</b></ed>");
        t("side.address", "<dark_gray>moinmorni.net</dark_gray>", "<dark_gray>moinmorni.net</dark_gray>");

        // ---------- Warteschlange ----------
        t("queue.joined", "<gray>Du spielst mit! <white><n></white> warten, ab <white><min></white> geht es los.</gray>",
                "<gray>You're in! <white><n></white> waiting, the game starts with <white><min></white>.</gray>");
        t("queue.left", "<gray>Du wartest nicht mehr.</gray>",
                "<gray>You left the waiting list.</gray>");
        t("queue.party", "<gray>Deine Party-Leitung <white><name></white> hat dich mitgenommen – ihr spielt zusammen.</gray>",
                "<gray>Your party leader <white><name></white> brought you along – you play together.</gray>");
        t("queue.ingame", "<gray>Du bist schon in einer Partie. Mit <white>/verlassen</white> kommst du heraus.</gray>",
                "<gray>You are already in a game. Use <white>/verlassen</white> (leave) to get out.</gray>");
        t("queue.bar", "<au>Start in <s> s</au> <dark_gray>·</dark_gray> <gray><n> spielen mit</gray>",
                "<au>Start in <s> s</au> <dark_gray>·</dark_gray> <gray><n> players</gray>");
        t("queue.countdown", "<gray>Die Partie beginnt in <au><s> Sekunden</au>!</gray>",
                "<gray>The game starts in <au><s> seconds</au>!</gray>");
        t("queue.stopped", "<gray>Zu wenige – der Start wartet noch.</gray>",
                "<gray>Not enough players – the start is on hold.</gray>");
        t("queue.tables", "<gray>Alle Spieltische sind besetzt – gleich wird einer frei.</gray>",
                "<gray>All tables are taken – one will be free soon.</gray>");

        // ---------- Abstimmung ----------
        t("vote.board.title", "Welches Spielbrett?",
                "Which board?");
        t("vote.mode.title", "Welche Spielart?",
                "Which game type?");
        t("vote.count", "<gray>Stimmen: <white><n></white></gray>",
                "<gray>Votes: <white><n></white></gray>");
        t("vote.mine", "<em>✔ Deine Wahl</em>",
                "<em>✔ Your vote</em>");
        t("vote.click", "<em>▶ Klicken zum Wählen</em>",
                "<em>▶ Click to vote</em>");
        t("vote.done", "<gray>Deine Stimme: <white><what></white>.</gray>",
                "<gray>Your vote: <white><what></white>.</gray>");

        // ---------- Spielbretter ----------
        t("board.almsommer", "<em>Almsommer am Morni-Kogel</em>",
                "<em>Alpine Summer on the Morni-Kogel</em>");
        t("board.almsommer.short", "<em>Almsommer</em>",
                "<em>Alpine Summer</em>");
        t("board.almsommer.desc", "<gray>Ein grüner Berg mit Almhütte – der Weg windet</gray>\n<gray>sich hinauf bis zum Gipfelkreuz.</gray>",
                "<gray>A green mountain with an alpine hut – the path winds</gray>\n<gray>up to the summit cross.</gray>");
        t("board.schloss", "<em>Schloss Weißenstein</em>",
                "<em>Weissenstein Castle</em>");
        t("board.schloss.short", "<em>Schloss Weißenstein</em>",
                "<em>Weissenstein Castle</em>");
        t("board.schloss.desc", "<gray>Durch den Schlossgarten und über die Mauern</gray>\n<gray>hinauf zur Plattform am Bergfried.</gray>",
                "<gray>Through the castle garden and along the walls</gray>\n<gray>up to the platform by the keep.</gray>");
        t("board.goldstollen", "<em>Goldstollen</em>",
                "<em>Gold Mine</em>");
        t("board.goldstollen.short", "<em>Goldstollen</em>",
                "<em>Gold Mine</em>");
        t("board.goldstollen.desc", "<gray>Ein offenes Bergwerk: der Weg führt vorbei an</gray>\n<gray>Erzadern hinab zur Goldkammer.</gray>",
                "<gray>An open mine: the path leads past ore veins</gray>\n<gray>down to the gold chamber.</gray>");
        t("board.koenigssee", "<em>Königssee</em>", "<em>Königssee</em>");
        t("board.koenigssee.short", "<em>Königssee</em>", "<em>Königssee</em>");
        t("board.koenigssee.desc", "<gray>Auf Stegen über den Bergsee bis zur Kapelle</gray>\n<gray>auf der Insel – die Fähre kürzt ab.</gray>",
                "<gray>Along jetties across the mountain lake to the</gray>\n<gray>chapel on the island – the ferry takes a shortcut.</gray>");
        t("board.wiesn", "<em>Wiesn</em>", "<em>Wiesn</em>");
        t("board.wiesn.short", "<em>Wiesn</em>", "<em>Wiesn</em>");
        t("board.wiesn.desc", "<gray>Festwiese mit Bierzelten und Fahrgeschäften –</gray>\n<gray>das Riesenrad tauscht Plätze.</gray>",
                "<gray>A fairground with beer tents and rides –</gray>\n<gray>the Ferris wheel swaps places.</gray>");
        t("board.wintersonnwend", "<em>Wintersonnwend</em>", "<em>Winter Solstice</em>");
        t("board.wintersonnwend.short", "<em>Wintersonnwend</em>", "<em>Winter Solstice</em>");
        t("board.wintersonnwend.desc", "<gray>Verschneiter Berg mit Eis und Lawinen hinauf</gray>\n<gray>zum großen Sonnwendfeuer.</gray>",
                "<gray>A snowy mountain with ice and avalanches up</gray>\n<gray>to the great solstice bonfire.</gray>");
        t("goal.almsommer", "das Gipfelkreuz",
                "the summit cross");
        t("goal.schloss", "die Turmplattform",
                "the tower platform");
        t("goal.goldstollen", "die Goldkammer",
                "the gold chamber");
        t("goal.koenigssee", "die Inselkapelle", "the island chapel");
        t("goal.wiesn", "das Riesenrad", "the Ferris wheel");
        t("goal.wintersonnwend", "das Sonnwendfeuer", "the solstice bonfire");

        // ---------- Spielarten ----------
        t("mode.gipfelrennen", "<au>Gipfelrennen</au>",
                "<au>Summit Race</au>");
        t("mode.gipfelrennen.line", "<au>Gipfelrennen</au> <gray>– wer zuerst oben ist, gewinnt</gray>",
                "<au>Summit Race</au> <gray>– first one at the top wins</gray>");
        t("mode.gipfelrennen.desc", "<gray>Wer zuerst das Ziel erreicht, gewinnt.</gray>\n<gray>Edelweiß zählt nur bei Gleichstand.</gray>",
                "<gray>The first to reach the goal wins.</gray>\n<gray>Edelweiss only counts on a tie.</gray>");
        t("mode.edelweiss-jagd", "<ed>Edelweiß-Jagd</ed>",
                "<ed>Edelweiss Hunt</ed>");
        t("mode.marathon", "<au>Minispiel-Marathon</au>",
                "<au>Minigame Marathon</au>");
        t("mode.marathon.rounds", "<au>Minispiel-Marathon</au> <gray>· <white><n></white> Minispiele</gray>",
                "<au>Minigame Marathon</au> <gray>· <white><n></white> minigames</gray>");
        t("mode.marathon.line", "<au>Minispiel-Marathon</au> <gray>– <n> Minispiele, die meisten Punkte gewinnen</gray>",
                "<au>Minigame Marathon</au> <gray>– <n> minigames, most points wins</gray>");
        t("mode.marathon.desc", "<gray>Ohne Spielbrett: ein Minispiel nach dem anderen,</gray>\n<gray>Platz-Punkte nach jedem – für 2 bis 16 Leute.</gray>",
                "<gray>No game board: one minigame after another,</gray>\n<gray>place points after each – for 2 to 16 players.</gray>");
        t("mode.seilschaft", "<green>Seilschaft</green>",
                "<green>Rope Team</green>");
        t("mode.seilschaft.rounds", "<green>Seilschaft</green> <gray>· <white><n></white> Runden zu zweit</gray>",
                "<green>Rope Team</green> <gray>· <white><n></white> rounds in pairs</gray>");
        t("mode.seilschaft.line", "<green>Seilschaft</green> <gray>– <n> Runden zu zweit, gemeinsame Taler und Edelweiß</gray>",
                "<green>Rope Team</green> <gray>– <n> rounds in pairs, shared coins and edelweiss</gray>");
        t("mode.seilschaft.desc", "<gray>Edelweiß-Jagd zu zweit: Taler und Edelweiß</gray>\n<gray>gehören beiden, Minispiele zwei gegen zwei.</gray>\n<gray>Partys aus zwei sind eine Seilschaft.</gray>",
                "<gray>Edelweiss Hunt in pairs: coins and edelweiss</gray>\n<gray>belong to both, minigames two against two.</gray>\n<gray>Parties of two are one rope team.</gray>");
        t("game.goal.seilschaft", "<gray>Ihr spielt in Seilschaften: Taler und Edelweiß gehören euch beiden. Nach <white><n></white> Runden gewinnt die Seilschaft mit den meisten Edelweiß (Stand: <au><price> Taler</au>). Minispiele sind zwei gegen zwei.</gray>",
                "<gray>You play in rope teams: coins and edelweiss belong to both of you. After <white><n></white> rounds the team with the most edelweiss wins (stand: <au><price> coins</au>). Minigames are two against two.</gray>");
        t("mode.schnell", "<yellow>Schnelle Partie</yellow>",
                "<yellow>Quick Game</yellow>");
        t("mode.schnell.rounds", "<yellow>Schnelle Partie</yellow> <gray>· <white><n></white> Runden</gray>",
                "<yellow>Quick Game</yellow> <gray>· <white><n></white> rounds</gray>");
        t("mode.schnell.line", "<yellow>Schnelle Partie</yellow> <gray>– <n> Runden, mehr Taler, günstigeres Edelweiß</gray>",
                "<yellow>Quick Game</yellow> <gray>– <n> rounds, more coins, cheaper edelweiss</gray>");
        t("mode.schnell.desc", "<gray>Edelweiß-Jagd für zwischendurch: wenige Runden,</gray>\n<gray>mehr Start-Taler, günstigeres Edelweiß</gray>\n<gray>und kürzere Minispiele.</gray>",
                "<gray>Edelweiss Hunt for a quick break: few rounds,</gray>\n<gray>more starting coins, cheaper edelweiss</gray>\n<gray>and shorter minigames.</gray>");
        t("game.goal.schnell", "<gray>Schnelle Partie: nur <white><n></white> Runden! Ihr startet mit mehr Talern, <ed>Edelweiß</ed> kostet nur <au><price> Taler</au>. Wer am Ende die meisten hat, gewinnt.</gray>",
                "<gray>Quick game: only <white><n></white> rounds! You start with more coins and <ed>edelweiss</ed> costs only <au><price> coins</au>. Whoever has the most at the end wins.</gray>");
        t("mode.bergduell", "<red>Bergduell</red>",
                "<red>Mountain Duel</red>");
        t("mode.bergduell.rounds", "<red>Bergduell</red> <gray>· 1 gegen 1, <white><n></white> Runden</gray>",
                "<red>Mountain Duel</red> <gray>· 1 vs 1, <white><n></white> rounds</gray>");
        t("mode.bergduell.line", "<red>Bergduell</red> <gray>– 1 gegen 1, <n> Runden, jedes Minispiel ein Duell</gray>",
                "<red>Mountain Duel</red> <gray>– 1 vs 1, <n> rounds, every minigame a duel</gray>");
        t("mode.bergduell.desc", "<gray>Nur ihr zwei auf dem Brett: Edelweiß-Jagd</gray>\n<gray>eins gegen eins, nach jeder Runde ein Duell-</gray>\n<gray>Minispiel (Tauziehen, Scheibenschießen …).</gray>",
                "<gray>Just the two of you on the board: Edelweiss</gray>\n<gray>Hunt one against one, a duel minigame after</gray>\n<gray>every round (tug of war, target throwing …).</gray>");
        t("game.goal.bergduell", "<gray>Bergduell: ihr zwei gegeneinander, <white><n></white> Runden. Nach jeder Runde ein Duell-Minispiel – der Sieger bekommt Taler. <ed>Edelweiß</ed> am Stand für <au><price> Taler</au>; wer mehr hat, gewinnt.</gray>",
                "<gray>Mountain duel: the two of you against each other, <white><n></white> rounds. A duel minigame after every round – the winner gets coins. <ed>Edelweiss</ed> at the stand for <au><price> coins</au>; whoever has more wins.</gray>");
        t("queue.waitmode", "<gray>Gewünscht ist</gray> <mode><gray> – dafür fehlen noch Leute (mindestens <n>). Wir warten noch etwas …</gray>",
                "<gray>The vote is for</gray> <mode><gray> – there aren't enough players yet (at least <n>). Waiting a little longer …</gray>");
        t("vote.need.two", "<red>Für genau zwei Leute (weitere warten auf die nächste Partie).</red>",
                "<red>For exactly two players (others wait for the next game).</red>");
        t("team.won.duel", "<au>★</au> <white><names></white> <gray>gewinnt das Duell –</gray> <au><n> Taler</au><gray>.</gray>",
                "<au>★</au> <white><names></white> <gray>wins the duel –</gray> <au><n> coins</au><gray>.</gray>");
        t("mode.huette", "<gold>Hüttenaufstieg</gold>",
                "<gold>Hut Climb</gold>");
        t("mode.huette.line", "<gold>Hüttenaufstieg</gold> <gray>– <n> Leben, die untere Hälfte verliert eins, wer zuletzt übrig ist, gewinnt</gray>",
                "<gold>Hut Climb</gold> <gray>– <n> lives, the bottom half loses one, the last one standing wins</gray>");
        t("mode.huette.desc", "<gray>Ohne Spielbrett: jeder hat Leben. Wer im</gray>\n<gray>Minispiel in der unteren Hälfte landet, verliert</gray>\n<gray>eins. Wer zuletzt übrig ist, erreicht die Hütte.</gray>",
                "<gray>No board: everyone has lives. Whoever ends</gray>\n<gray>up in the bottom half of a minigame loses one.</gray>\n<gray>The last one standing reaches the hut.</gray>");
        t("game.goal.huette", "<gray>Auf zur Hütte! Jeder hat <red><lives> ❤</red>. Nach jedem Minispiel verliert die untere Hälfte ein Leben – wer keins mehr hat, schaut zu. Wer zuletzt übrig ist, gewinnt.</gray>",
                "<gray>Up to the hut! Everyone has <red><lives> ❤</red>. After every minigame the bottom half loses a life – whoever has none left watches. The last one standing wins.</gray>");
        t("huette.next", "<gray>Etappe</gray> <white><n></white> <gray>·</gray> <white><left></white> <gray>noch dabei</gray>",
                "<gray>Stage</gray> <white><n></white> <gray>·</gray> <white><left></white> <gray>still climbing</gray>");
        t("huette.lost", "<white><name></white> <gray>verliert ein Leben –</gray> <red><hearts></red>",
                "<white><name></white> <gray>loses a life –</gray> <red><hearts></red>");
        t("huette.out", "<white><name></white> <gray>hat kein Leben mehr und schaut ab jetzt zu.</gray>",
                "<white><name></white> <gray>has no lives left and watches from now on.</gray>");
        t("huette.out.title", "<red>Keine Leben mehr</red>",
                "<red>No lives left</red>");
        t("huette.out.sub", "<gray>Du schaust jetzt zu – wer erreicht die Hütte?</gray>",
                "<gray>You're watching now – who reaches the hut?</gray>");
        t("huette.sub.lost", "<gray>Ein Leben weniger:</gray> <red><hearts></red>",
                "<gray>One life less:</gray> <red><hearts></red>");
        t("huette.sub.safe", "<gray>Sicher –</gray> <red><hearts></red>",
                "<gray>Safe –</gray> <red><hearts></red>");
        t("huette.standing", "<gold>━━ Noch <left> unterwegs zur Hütte ━━</gold>",
                "<gold>━━ <left> still on the way to the hut ━━</gold>");
        t("huette.standing.line", "<white><name></white> <red><hearts></red>",
                "<white><name></white> <red><hearts></red>");
        t("mini.prize.life", "<red>−1 ❤</red>", "<red>−1 ❤</red>");
        t("mini.prize.safe", "<green>sicher</green>", "<green>safe</green>");
        t("side.huette", "<gray>Etappe</gray> <white><round></white> <gray>· <left> dabei</gray>",
                "<gray>Stage</gray> <white><round></white> <gray>· <left> left</gray>");
        t("side.player.huette", "<name> <red><hearts></red>", "<name> <red><hearts></red>");
        t("side.player.huette.out", "<dark_gray><st><name></st></dark_gray>", "<dark_gray><st><name></st></dark_gray>");
        t("end.line.huette", "<au><pos>.</au> <white><name></white> <red><hearts></red> <gray>· <minis> <minis_pl:'0#Minispiele|1#Minispiel|1<Minispiele'/> gewonnen</gray>",
                "<au><pos>.</au> <white><name></white> <red><hearts></red> <gray>· <minis> <minis_pl:'0#minigames|1#minigame|1<minigames'/> won</gray>");
        t("mode.turnier", "<au>Gipfel-Turnier</au>",
                "<au>Summit Cup</au>");
        t("mode.turnier.line", "<au>Gipfel-Turnier</au> <gray>– <n> Leute im K.-o.-System, jedes Spiel ein Duell</gray>",
                "<au>Summit Cup</au> <gray>– <n> players, knock-out, every match a duel</gray>");
        t("mode.turnier.desc", "<gray>Ohne Spielbrett: K.-o.-Turnier aus</gray>\n<gray>Duell-Minispielen für 2 bis 16 Leute.</gray>\n<gray>Wer verliert, schaut zu – bis zum Finale.</gray>",
                "<gray>No board: a knock-out cup of duel</gray>\n<gray>minigames for 2 to 16 players. Lose and</gray>\n<gray>you watch – all the way to the final.</gray>");
        t("game.goal.turnier", "<gray>Gipfel-Turnier! Jedes Spiel ist ein Duell-Minispiel eins gegen eins – wer gewinnt, kommt weiter, wer verliert, schaut zu. Den Turnierbaum seht ihr auf der Tafel.</gray>",
                "<gray>Summit Cup! Every match is a one-on-one duel minigame – win and you go through, lose and you watch. The bracket is on the board.</gray>");
        t("turnier.round.1", "<au>Finale</au>", "<au>Final</au>");
        t("turnier.round.2", "<gold>Halbfinale</gold>", "<gold>Semi-final</gold>");
        t("turnier.round.3", "<yellow>Viertelfinale</yellow>", "<yellow>Quarter-final</yellow>");
        t("turnier.round.4", "<yellow>Achtelfinale</yellow>", "<yellow>Round of 16</yellow>");
        t("turnier.round", "<yellow>Runde <n></yellow>", "<yellow>Round <n></yellow>");
        t("turnier.round.done", "<au>Turnier vorbei</au>", "<au>Cup over</au>");
        t("turnier.match", "<round> <gray>–</gray> <white><one></white> <gray>gegen</gray> <white><two></white>",
                "<round> <gray>–</gray> <white><one></white> <gray>vs</gray> <white><two></white>");
        t("turnier.draw", "<gray>Unentschieden zwischen</gray> <white><one></white> <gray>und</gray> <white><two></white> <gray>– es gibt ein neues Duell!</gray>",
                "<gray>A draw between</gray> <white><one></white> <gray>and</gray> <white><two></white> <gray>– there's a new duel!</gray>");
        t("turnier.lot", "<gray>Wieder unentschieden – das Los schickt</gray> <white><winner></white> <gray>weiter.</gray>",
                "<gray>A draw again – the lot sends</gray> <white><winner></white> <gray>through.</gray>");
        t("turnier.through", "<white><winner></white> <gray>kommt weiter,</gray> <white><loser></white> <gray>ist raus.</gray>",
                "<white><winner></white> <gray>goes through,</gray> <white><loser></white> <gray>is out.</gray>");
        t("turnier.champion", "<au>★ <winner> gewinnt das Gipfel-Turnier! ★</au>",
                "<au>★ <winner> wins the Summit Cup! ★</au>");
        t("turnier.sub.through", "<green>Eine Runde weiter</green>", "<green>Through to the next round</green>");
        t("turnier.sub.champion", "<au>Turniersieg!</au>", "<au>Cup winner!</au>");
        t("turnier.sub.out", "<gray>Du schaust jetzt zu</gray>", "<gray>You're watching now</gray>");
        t("turnier.reached.won", "<au>Turniersieg</au>", "<au>Cup winner</au>");
        t("turnier.reached", "<gray>raus im</gray> <round>", "<gray>out in the</gray> <round>");
        t("turnier.tafel.title", "<au><b>⛰ Gipfel-Turnier ⛰</b></au>", "<au><b>⛰ Summit Cup ⛰</b></au>");
        t("turnier.tafel.vs", " <dark_gray>–</dark_gray> ", " <dark_gray>–</dark_gray> ");
        t("turnier.tafel.won", "<green><b><name> ✔</b></green>", "<green><b><name> ✔</b></green>");
        t("turnier.tafel.lost", "<gray><st><name></st></gray>", "<gray><st><name></st></gray>");
        t("turnier.tafel.open", "<white><name></white>", "<white><name></white>");
        t("turnier.tafel.bye", "<dark_gray>Freilos</dark_gray>", "<dark_gray>bye</dark_gray>");
        t("turnier.tafel.champion", "<au>★ <name> ★</au>", "<au>★ <name> ★</au>");
        t("side.turnier", "<round>", "<round>");
        t("side.player.turnier", "<name> <green><wins>✔</green>", "<name> <green><wins>✔</green>");
        t("end.line.turnier", "<au><pos>.</au> <white><name></white> <gray>–</gray> <reached>",
                "<au><pos>.</au> <white><name></white> <gray>–</gray> <reached>");
        t("mode.tatzelwurm", "<dark_green>Der Tatzelwurm</dark_green>",
                "<dark_green>The Tatzelwurm</dark_green>");
        t("mode.tatzelwurm.line", "<dark_green>Der Tatzelwurm</dark_green> <gray>– alle zusammen gegen den Wurm in der Klamm</gray>",
                "<dark_green>The Tatzelwurm</dark_green> <gray>– everyone together against the worm in the gorge</gray>");
        t("mode.tatzelwurm.desc", "<gray>Gemeinsam gegen den Tatzelwurm:</gray>\n<gray>drei Abschnitte, eine gemeinsame Wertung</gray>\n<gray>mit bis zu drei Sternen.</gray>",
                "<gray>Together against the Tatzelwurm:</gray>\n<gray>three stages, one shared result</gray>\n<gray>with up to three stars.</gray>");
        t("game.goal.tatzelwurm", "<gray>In der Klamm haust der Tatzelwurm! Haut ihn alle zusammen in drei Abschnitten, bevor die Zeit abläuft – je schneller und vorsichtiger, desto mehr Sterne.</gray>",
                "<gray>The Tatzelwurm lives in the gorge! Beat it together in three stages before time runs out – the faster and more careful, the more stars.</gray>");
        t("mini.tatzelwurm", "<dark_green>Der Tatzelwurm</dark_green>",
                "<dark_green>The Tatzelwurm</dark_green>");
        t("mini.tatzelwurm.desc", "<gray>Haut auf den Kopf, wenn er aus dem Boden bricht, auf die leuchtenden Schuppen, wenn er durch die Klamm jagt, und auf den Kopf, wenn er am Boden liegt. Schatten am Boden: ein Felsbrocken kommt! Bodenwellen: drüberspringen.</gray>",
                "<gray>Hit the head when it bursts out of the ground, the glowing scales when it races through the gorge, and the head when it lies on the ground. Shadow on the ground: a boulder is coming! Shock waves: jump over them.</gray>");
        t("mini.tatzelwurm.item", "<au>Wanderstock</au> <gray>– zuschlagen</gray>",
                "<au>Walking stick</au> <gray>– hit</gray>");
        t("mini.tatzelwurm.bar", "<dark_green>Tatzelwurm</dark_green> <red><hp></red><gray>/<max></gray> <gray>· deine Treffer:</gray> <au><hits></au>",
                "<dark_green>Tatzelwurm</dark_green> <red><hp></red><gray>/<max></gray> <gray>· your hits:</gray> <au><hits></au>");
        t("mini.tatzelwurm.closed", "<gray>Jetzt nicht – warte, bis der Kopf unten ist!</gray>",
                "<gray>Not now – wait until the head is down!</gray>");
        t("mini.tatzelwurm.scales", "<gray>Harte Schuppen – nur die leuchtenden sind verwundbar.</gray>",
                "<gray>Hard scales – only the glowing ones can be hurt.</gray>");
        t("mini.tatzelwurm.stunned", "<red>Benommen!</red>", "<red>Stunned!</red>");
        t("mini.tatzelwurm.won", "<au>★</au> <white><name></white> <gray>landet den letzten Treffer – der Tatzelwurm ist besiegt!</gray>",
                "<au>★</au> <white><name></white> <gray>lands the final hit – the Tatzelwurm is beaten!</gray>");
        t("mini.tatzelwurm.phase.1", "<dark_green>Aus dem Boden!</dark_green>", "<dark_green>Out of the ground!</dark_green>");
        t("mini.tatzelwurm.phase.1.sub", "<gray>Haut auf den Kopf, wenn er herausbricht</gray>",
                "<gray>Hit the head when it bursts out</gray>");
        t("mini.tatzelwurm.phase.2", "<gold>Die Jagd!</gold>", "<gold>The chase!</gold>");
        t("mini.tatzelwurm.phase.2.sub", "<gray>Trefft die leuchtenden Schuppen – Vorsicht vor dem Kopf</gray>",
                "<gray>Hit the glowing scales – watch out for the head</gray>");
        t("mini.tatzelwurm.phase.3", "<red>Der Zorn!</red>", "<red>The rage!</red>");
        t("mini.tatzelwurm.phase.3.sub", "<gray>Springt über die Wellen, haut auf den Kopf, wenn er am Boden liegt</gray>",
                "<gray>Jump the waves, hit the head when it lies on the ground</gray>");
        t("wurm.won", "<au>Geschafft!</au> <gray>Der Tatzelwurm ist besiegt – in</gray> <white><time></white><gray>,</gray> <white><stuns></white> <gray>mal benommen:</gray> <au><stars></au>",
                "<au>Done!</au> <gray>The Tatzelwurm is beaten – in</gray> <white><time></white><gray>, stunned</gray> <white><stuns></white> <gray>times:</gray> <au><stars></au>");
        t("wurm.lost", "<gray>Die Zeit ist um – der Tatzelwurm hat noch</gray> <red><hp>/<max></red><gray>. Beim nächsten Mal!</gray>",
                "<gray>Time's up – the Tatzelwurm still has</gray> <red><hp>/<max></red><gray>. Next time!</gray>");
        t("wurm.title.won", "<au>Tatzelwurm besiegt!</au>", "<au>Tatzelwurm beaten!</au>");
        t("wurm.title.lost", "<red>Entkommen …</red>", "<red>It got away …</red>");
        t("end.line.wurm", "<white><name></white> <gray>–</gray> <au><points> Treffer</au>",
                "<white><name></white> <gray>–</gray> <au><points> <points_pl:'0#hits|1#hit|1<hits'/></au>");
        t("award.header", "<au>━━ Auszeichnungen – je ein Edelweiß extra! ━━</au>",
                "<au>━━ Awards – one extra edelweiss each! ━━</au>");
        t("award.line", "<ed>✿</ed> <award><gray>:</gray> <white><names></white> <gray>(<what>)</gray>",
                "<ed>✿</ed> <award><gray>:</gray> <white><names></white> <gray>(<what>)</gray>");
        t("award.sub", "<white><names></white> <gray>– <what> –</gray> <ed>+1 ✿</ed>",
                "<white><names></white> <gray>– <what> –</gray> <ed>+1 ✿</ed>");
        t("award.wanderer", "<green>Wanderer</green>", "<green>Hiker</green>");
        t("award.wanderer.value", "<n> <n_pl:'0#Felder|1#Feld|1<Felder'/> gelaufen", "<n> <n_pl:'0#fields|1#field|1<fields'/> walked");
        t("award.minispiel-meister", "<au>Minispiel-Meister</au>", "<au>Minigame Master</au>");
        t("award.minispiel-meister.value", "<n> <n_pl:'0#Minispiele|1#Minispiel|1<Minispiele'/> gewonnen",
                "<n> <n_pl:'0#minigames|1#minigame|1<minigames'/> won");
        t("award.pechvogel", "<gray>Pechvogel</gray>", "<gray>Unlucky Bird</gray>");
        t("award.pechvogel.value", "<n>× auf roten Feldern", "<n>× on red fields");
        t("award.duellkoenig", "<gold>Duellkönig</gold>", "<gold>Duel King</gold>");
        t("award.duellkoenig.value", "<n> <n_pl:'0#Duelle|1#Duell|1<Duelle'/> gewonnen", "<n> <n_pl:'0#duels|1#duel|1<duels'/> won");
        t("land.ferry", "<white><name></white> <gray>nimmt die Fähre – quer über den See,</gray> <white><n></white> <gray>Felder voraus!</gray>",
                "<white><name></white> <gray>takes the ferry – across the lake,</gray> <white><n></white> <gray>fields ahead!</gray>");
        t("land.wheel", "<white><name></white> <gray>fährt Riesenrad und tauscht oben den Platz mit</gray> <white><other></white><gray>!</gray>",
                "<white><name></white> <gray>rides the Ferris wheel and swaps places with</gray> <white><other></white> <gray>at the top!</gray>");
        t("land.wheel.blocked", "<white><name></white> <gray>will mit</gray> <white><other></white> <gray>tauschen – aber der</gray> <bl>Schild</bl> <gray>hält.</gray>",
                "<white><name></white> <gray>wants to swap with</gray> <white><other></white> <gray>– but the</gray> <bl>shield</bl> <gray>holds.</gray>");
        t("land.ice", "<white><name></white> <gray>rutscht übers Eis –</gray> <white><n></white> <gray>Felder weiter!</gray>",
                "<white><name></white> <gray>slides across the ice –</gray> <white><n></white> <gray>fields on!</gray>");
        t("land.avalanche", "<white><name></white> <gray>tritt eine Lawine los! Zurück um</gray> <white><n></white><gray>:</gray> <white><names></white>",
                "<white><name></white> <gray>sets off an avalanche! Back by</gray> <white><n></white><gray>:</gray> <white><names></white>");
        t("land.avalanche.blocked", "<white><name></white> <gray>tritt eine Lawine los – aber der</gray> <bl>Schild</bl> <gray>hält sie auf.</gray>",
                "<white><name></white> <gray>sets off an avalanche – but the</gray> <bl>shield</bl> <gray>stops it.</gray>");
        t("ride.achterbahn", "<white><name></white> <gray>fährt Achterbahn –</gray> <white><n></white> <gray>Felder vor!</gray>",
                "<white><name></white> <gray>rides the roller coaster –</gray> <white><n></white> <gray>fields ahead!</gray>");
        t("ride.achterbahn.title", "<light_purple>Achterbahn!</light_purple>", "<light_purple>Roller coaster!</light_purple>");
        t("ride.autoscooter", "<white><name></white> <gray>rempelt im Autoscooter</gray> <white><count></white> <gray>an und kassiert</gray> <au><taler> Taler</au><gray>.</gray>",
                "<white><name></white> <gray>bumps</gray> <white><count></white> <gray>in the bumper cars and collects</gray> <au><taler> coins</au><gray>.</gray>");
        t("ride.autoscooter.title", "<light_purple>Autoscooter!</light_purple>", "<light_purple>Bumper cars!</light_purple>");
        t("ride.kettenkarussell", "<white><name></white> <gray>fliegt im Kettenkarussell davon –</gray> <white><n></white> <gray>Felder weit!</gray>",
                "<white><name></white> <gray>flies off the chain carousel –</gray> <white><n></white> <gray>fields away!</gray>");
        t("ride.kettenkarussell.title", "<light_purple>Kettenkarussell!</light_purple>", "<light_purple>Chain carousel!</light_purple>");
        t("ride.losbude", "<white><name></white> <gray>zieht an der Losbude:</gray> <item><au><taler></au>",
                "<white><name></white> <gray>draws a ticket at the lottery stall:</gray> <item><au><taler></au>");
        t("ride.losbude.title", "<light_purple>Losbude!</light_purple>", "<light_purple>Lottery stall!</light_purple>");
        t("rope.team", "<green>⛓</green> <white><names></white> <gray>– gemeinsam</gray> <au><taler> Taler</au>",
                "<green>⛓</green> <white><names></white> <gray>– together</gray> <au><taler> coins</au>");
        t("side.rope", "<gray><pos>.</gray> <names> <ed>✿<edelweiss></ed> <au>●<taler></au>",
                "<gray><pos>.</gray> <names> <ed>✿<edelweiss></ed> <au>●<taler></au>");
        t("vote.need", "<red>Braucht mindestens <n> Leute.</red>",
                "<red>Needs at least <n> players.</red>");
        t("vote.need.pairs", "<red>Braucht mindestens <n> Leute in gerader Zahl.</red>",
                "<red>Needs at least <n> players, an even number.</red>");
        t("bag.problem.partner", "<gray>Deinem Partner nimmst du nichts weg.</gray>",
                "<gray>You don't steal from your partner.</gray>");
        t("mode.edelweiss-jagd.rounds", "<ed>Edelweiß-Jagd</ed> <gray>· <white><n></white> Runden</gray>",
                "<ed>Edelweiss Hunt</ed> <gray>· <white><n></white> rounds</gray>");
        t("mode.edelweiss-jagd.line", "<ed>Edelweiß-Jagd</ed> <gray>– <n> Runden, die meisten Edelweiß gewinnen</gray>",
                "<ed>Edelweiss Hunt</ed> <gray>– <n> rounds, most Edelweiss wins</gray>");
        t("mode.edelweiss-jagd.desc", "<gray>Nach der letzten Runde gewinnt, wer die meisten</gray>\n<gray>Edelweiß hat (bei Gleichstand: mehr Taler).</gray>",
                "<gray>After the last round, whoever has the most</gray>\n<gray>Edelweiss wins (tie: more coins).</gray>");

        // ---------- Partie ----------
        t("game.start", "<gray>Los geht's auf <white><board></white>: <mode></gray>",
                "<gray>Let's go on <white><board></white>: <mode></gray>");
        t("game.goal.gipfelrennen", "<gray>Würfle dich nach oben! Unterwegs: Taler sammeln, Gegenstände kaufen, <ed>Edelweiß</ed> am Stand für <au><price> Taler</au>. Spätestens nach <white><n></white> Runden ist Schluss.</gray>",
                "<gray>Roll your way to the top! On the way: collect coins, buy items, <ed>Edelweiss</ed> at the stand for <au><price> coins</au>. The game ends after <white><n></white> rounds at the latest.</gray>");
        t("game.goal.marathon", "<gray><white><n></white> Minispiele nacheinander, ohne Spielbrett. Nach jedem gibt es Platz-Punkte: <au><first></au> für Platz 1, <au><second></au> für Platz 2, <au><third></au> für Platz 3 … Am Ende gewinnt, wer die meisten hat.</gray>",
                "<gray><white><n></white> minigames in a row, no game board. After each one you get place points: <au><first></au> for 1st, <au><second></au> for 2nd, <au><third></au> for 3rd … Most points at the end wins.</gray>");
        t("game.players", "<gray>Dabei: <white><names></white></gray>",
                "<gray>Playing: <white><names></white></gray>");
        t("marathon.next", "<gray>Minispiel <white><n></white> von <white><of></white></gray>",
                "<gray>Minigame <white><n></white> of <white><of></white></gray>");
        t("marathon.standing", "<au>━━ Zwischenstand nach <n> von <of> Minispielen ━━</au>",
                "<au>━━ Standings after <n> of <of> minigames ━━</au>");
        t("marathon.standing.line", "<au><pos>.</au> <white><name></white> <gray>–</gray> <au><points> <points_pl:'0#Punkte|1#Punkt|1<Punkte'/></au>",
                "<au><pos>.</au> <white><name></white> <gray>–</gray> <au><points> <points_pl:'0#points|1#point|1<points'/></au>");
        t("game.goal.edelweiss-jagd", "<gray>Sammle Taler und kauf am wandernden Stand <ed>Edelweiß</ed> für <au><price> Taler</au>. Nach <white><n></white> Runden gewinnt, wer die meisten hat.</gray>",
                "<gray>Collect coins and buy <ed>Edelweiss</ed> at the moving stand for <au><price> coins</au>. After <white><n></white> rounds, whoever has the most wins.</gray>");
        t("game.order", "<gray>Reihenfolge: <white><names></white></gray>",
                "<gray>Turn order: <white><names></white></gray>");
        t("game.left", "<gray><white><name></white> hat die Partie verlassen.</gray>",
                "<gray><white><name></white> left the game.</gray>");
        t("game.away", "<gray><white><name></white> hat die Verbindung verloren – Figur, Taler und Edelweiß bleiben, der Platz ist <white><n></white> Sekunden frei.</gray>",
                "<gray><white><name></white> lost the connection – their piece, coins and edelweiss stay, the place is kept for <white><n></white> seconds.</gray>");
        t("game.away.gone", "<gray><white><name></white> ist nicht rechtzeitig zurückgekommen und ist raus.</gray>",
                "<gray><white><name></white> didn't make it back in time and is out.</gray>");
        t("game.returned", "<gray><white><name></white> ist wieder da.</gray>",
                "<gray><white><name></white> is back.</gray>");
        t("game.back.title", "<em>Willkommen zurück!</em>",
                "<em>Welcome back!</em>");
        t("game.back.sub", "<gray>Deine Partie geht weiter.</gray>",
                "<gray>Your game goes on.</gray>");
        t("game.back", "<gray>Willkommen zurück! Du stehst wieder auf deinem Feld – Taler, Edelweiß und Gegenstände sind alle noch da.</gray>",
                "<gray>Welcome back! You're on your field again – coins, edelweiss and items are all still there.</gray>");
        t("game.back.mini", "<gray>Willkommen zurück! Das Minispiel läuft schon – schau zu, danach bist du wieder dabei.</gray>",
                "<gray>Welcome back! The minigame is already running – watch it, you're back in afterwards.</gray>");
        t("game.back.out", "<gray>Willkommen zurück! Während du weg warst, bist du ausgeschieden – du schaust jetzt zu.</gray>",
                "<gray>Welcome back! You were knocked out while you were away – you're watching now.</gray>");
        t("game.missed", "<gray>Willkommen zurück! Deine Partie ging ohne dich weiter.</gray>",
                "<gray>Welcome back! Your game went on without you.</gray>");
        t("turn.start", "<gray>Runde <white><round></white>/<rounds>: <white><name></white> ist dran.</gray>",
                "<gray>Round <white><round></white>/<rounds>: it's <white><name></white>'s turn.</gray>");
        t("turn.you", "<au>Du bist dran!</au>",
                "<au>Your turn!</au>");
        t("turn.you.sub", "<gray>Rechtsklick auf den <white>Würfel</white></gray>",
                "<gray>Right-click the <white>dice</white></gray>");
        t("turn.other", "<gray><white><name></white> ist dran …</gray>",
                "<gray>It's <white><name></white>'s turn …</gray>");
        t("turn.hurry", "<au>Noch <s> s – sonst würfelt das Spiel für dich</au>",
                "<au><s> s left – then the game rolls for you</au>");
        t("turn.auto", "<gray>Die Zeit ist um – das Spiel hat für dich gewürfelt.</gray>",
                "<gray>Time's up – the game rolled for you.</gray>");
        t("roll.sub", "<gray><n> <n_pl:'0#Felder|1#Feld|1<Felder'/> weit</gray>",
                "<gray><n> <n_pl:'0#fields|1#field|1<fields'/></gray>");
        t("roll.done", "<white><name></white> <gray>würfelt</gray> <au><dice></au><gray>.</gray>",
                "<white><name></white> <gray>rolls</gray> <au><dice></au><gray>.</gray>");
        t("move.cablecar", "<white><name></white> <gray>ist oben angekommen und fährt mit der Seilbahn zurück ins Tal – <au>+<n> Taler</au> Gipfelbonus!</gray>",
                "<white><name></white> <gray>made it to the top and takes the cable car back down – <au>+<n> coins</au> summit bonus!</gray>");
        t("round.next", "<gray>Zurück aufs Brett – Runde <white><round></white> von <white><rounds></white>.</gray>",
                "<gray>Back to the board – round <white><round></white> of <white><rounds></white>.</gray>");
        t("item.dice", "<au>⚄ Würfeln</au>",
                "<au>⚄ Roll the dice</au>");
        t("item.dice.lore", "<gray>Rechtsklick: würfeln und losziehen.</gray>\n<gray>Vorher einen Gegenstand einsetzen? Rucksack!</gray>",
                "<gray>Right-click: roll and move.</gray>\n<gray>Use an item first? Backpack!</gray>");
        t("item.bag", "<em>✦ Rucksack</em> <gray>(<n>)</gray>",
                "<em>✦ Backpack</em> <gray>(<n>)</gray>");
        t("item.bag.lore", "<gray>Rechtsklick: Gegenstand einsetzen (vor dem Würfeln).</gray>",
                "<gray>Right-click: use an item (before rolling).</gray>");
        t("item.overview", "<bl>☰ Übersicht</bl>",
                "<bl>☰ Overview</bl>");
        t("item.overview.lore", "<gray>Rechtsklick: wer wo steht, wer was hat.</gray>",
                "<gray>Right-click: who is where, who has what.</gray>");

        // ---------- Seitenleiste ----------
        t("side.round", "<gray>Runde</gray> <white><round></white><gray>/<rounds></gray>",
                "<gray>Round</gray> <white><round></white><gray>/<rounds></gray>");
        t("side.marathon", "<gray>Minispiel</gray> <white><round></white><gray>/<rounds></gray>",
                "<gray>Minigame</gray> <white><round></white><gray>/<rounds></gray>");
        t("side.player.marathon", "<gray><pos>.</gray> <name> <au><points></au>", "<gray><pos>.</gray> <name> <au><points></au>");
        t("side.race", "<au>Gipfelrennen</au> <gray>· Runde <round></gray>",
                "<au>Summit Race</au> <gray>· round <round></gray>");
        t("side.player", "<au><turn></au><gray><pos>.</gray> <name> <ed>✿<edelweiss></ed> <au>●<taler></au>", "<au><turn></au><gray><pos>.</gray> <name> <ed>✿<edelweiss></ed> <au>●<taler></au>");
        t("side.player.race", "<au><turn></au><gray><pos>.</gray> <name> <white>⚑<field></white> <au>●<taler></au>", "<au><turn></au><gray><pos>.</gray> <name> <white>⚑<field></white> <au>●<taler></au>");
        t("side.stand", "<ed>✿</ed> <gray>Stand auf Feld</gray> <white><field></white>",
                "<ed>✿</ed> <gray>Stand on field</gray> <white><field></white>");

        // ---------- Felder ----------
        t("field.start", "<white>Start</white>", "<white>Start</white>");
        t("field.start.sign", "<white><b>Start</b></white>\n<gray>Hier geht es los.</gray>",
                "<white><b>Start</b></white>\n<gray>This is where it begins.</gray>");
        t("field.taler", "<bl>Talerfeld</bl>",
                "<bl>Coin field</bl>");
        t("field.taler.sign", "<bl><b>Talerfeld</b></bl>\n<gray>+Taler</gray>",
                "<bl><b>Coin field</b></bl>\n<gray>+coins</gray>");
        t("field.verlust", "<red>Verlustfeld</red>",
                "<red>Loss field</red>");
        t("field.verlust.sign", "<red><b>Verlustfeld</b></red>\n<gray>−Taler</gray>",
                "<red><b>Loss field</b></red>\n<gray>−coins</gray>");
        t("field.glueck", "<green>Glücksfeld</green>",
                "<green>Lucky field</green>");
        t("field.glueck.sign", "<green><b>Glücksfeld</b></green>\n<gray>Etwas Gutes passiert</gray>",
                "<green><b>Lucky field</b></green>\n<gray>Something good happens</gray>");
        t("field.pech", "<dark_gray>Pechfeld</dark_gray>",
                "<dark_gray>Unlucky field</dark_gray>");
        t("field.pech.sign", "<dark_gray><b>Pechfeld</b></dark_gray>\n<gray>Etwas Dummes passiert</gray>",
                "<dark_gray><b>Unlucky field</b></dark_gray>\n<gray>Something bad happens</gray>");
        t("field.duell", "<gold>Duellfeld</gold>",
                "<gold>Duel field</gold>");
        t("field.duell.sign", "<gold><b>Duellfeld</b></gold>\n<gray>Duell um Taler</gray>",
                "<gold><b>Duel field</b></gold>\n<gray>Duel for coins</gray>");
        t("field.laden", "<yellow>Krämerladen</yellow>",
                "<yellow>Shop</yellow>");
        t("field.laden.sign", "<yellow><b>Krämerladen</b></yellow>\n<gray>Gegenstände kaufen</gray>",
                "<yellow><b>Shop</b></yellow>\n<gray>Buy items</gray>");
        t("field.faehre", "<aqua>Fähre</aqua>", "<aqua>Ferry</aqua>");
        t("field.faehre.sign", "<aqua><b>Fähre</b></aqua>\n<gray>Quer über den See voraus</gray>",
                "<aqua><b>Ferry</b></aqua>\n<gray>Across the lake, ahead</gray>");
        t("field.riesenrad", "<light_purple>Riesenrad</light_purple>", "<light_purple>Ferris wheel</light_purple>");
        t("field.riesenrad.sign", "<light_purple><b>Riesenrad</b></light_purple>\n<gray>Platz mit jemandem tauschen</gray>",
                "<light_purple><b>Ferris wheel</b></light_purple>\n<gray>Swap places with someone</gray>");
        t("field.fahrgeschaeft", "<dark_purple>Fahrgeschäft</dark_purple>", "<dark_purple>Ride</dark_purple>");
        t("field.fahrgeschaeft.sign", "<dark_purple><b>Fahrgeschäft</b></dark_purple>\n<gray>Achterbahn, Autoscooter …</gray>",
                "<dark_purple><b>Ride</b></dark_purple>\n<gray>Roller coaster, bumper cars …</gray>");
        t("field.lawine", "<white>Lawine</white>", "<white>Avalanche</white>");
        t("field.lawine.sign", "<white><b>Lawine</b></white>\n<gray>Alle in der Nähe rutschen zurück</gray>",
                "<white><b>Avalanche</b></white>\n<gray>Everyone nearby slides back</gray>");
        t("field.eis", "<aqua>Eis</aqua>", "<aqua>Ice</aqua>");
        t("field.eis.sign", "<aqua><b>Eis</b></aqua>\n<gray>Rutsch weiter bis ans Ende</gray>",
                "<aqua><b>Ice</b></aqua>\n<gray>Slide on to the end</gray>");
        t("field.gegenstand", "<light_purple>Fundstück</light_purple>",
                "<light_purple>Lucky find</light_purple>");
        t("field.gegenstand.sign", "<light_purple><b>Fundstück</b></light_purple>\n<gray>Ein Gegenstand gratis</gray>",
                "<light_purple><b>Lucky find</b></light_purple>\n<gray>A free item</gray>");
        t("field.ziel", "<au>Ziel</au>",
                "<au>Goal</au>");
        t("field.ziel.sign", "<au><b>Ziel</b></au>\n<gray>Gipfel / Turm / Goldkammer</gray>",
                "<au><b>Goal</b></au>\n<gray>Summit / tower / gold chamber</gray>");
        t("land.plus", "<white><name></white> <gray>landet auf einem Talerfeld:</gray> <au>+<n> Taler</au>",
                "<white><name></white> <gray>lands on a coin field:</gray> <au>+<n> coins</au>");
        t("land.minus", "<white><name></white> <gray>landet auf einem Verlustfeld:</gray> <red>−<n> Taler</red>",
                "<white><name></white> <gray>lands on a loss field:</gray> <red>−<n> coins</red>");
        t("land.blocked", "<white><name></white> <gray>landet auf einem Verlustfeld – aber der <bl>Schild</bl> hält!</gray>",
                "<white><name></white> <gray>lands on a loss field – but the <bl>shield</bl> holds!</gray>");
        t("land.item", "<white><name></white> <gray>findet am Wegrand:</gray> <item>",
                "<white><name></white> <gray>finds by the path:</gray> <item>");
        t("land.summit", "<white><name></white> <gray>macht oben Brotzeit – schöne Aussicht!</gray>",
                "<white><name></white> <gray>has a snack at the top – what a view!</gray>");
        t("goal.reached", "<au><b><name></b></au> <gray>hat <white><goal></white> erreicht und gewinnt das Gipfelrennen!</gray>",
                "<au><b><name></b></au> <gray>reached <white><goal></white> and wins the Summit Race!</gray>");

        // ---------- Glück und Pech ----------
        t("luck.geschenk", "<em>Glück!</em> <gray>Der Almwirt spendiert <white><name></white></gray> <au><n> Taler</au><gray>.</gray>",
                "<em>Lucky!</em> <gray>The innkeeper gives <white><name></white></gray> <au><n> coins</au><gray>.</gray>");
        t("luck.rueckenwind", "<em>Glück!</em> <gray>Rückenwind – <white><name></white> zieht <white><steps></white> <steps_pl:'0#Felder|1#Feld|1<Felder'/> weiter.</gray>",
                "<em>Lucky!</em> <gray>Tailwind – <white><name></white> moves <white><steps></white> <steps_pl:'0#fields|1#field|1<fields'/> ahead.</gray>");
        t("luck.sammeln", "<em>Glück!</em> <gray>Alle anderen geben <white><name></white> etwas ab – zusammen</gray> <au><n> Taler</au><gray>.</gray>",
                "<em>Lucky!</em> <gray>Everyone else chips in for <white><name></white> –</gray> <au><n> coins</au> <gray>in total.</gray>");
        t("luck.fund", "<em>Glück!</em> <gray><white><name></white> findet etwas:</gray> <item>",
                "<em>Lucky!</em> <gray><white><name></white> finds something:</gray> <item>");
        t("luck.steinschlag", "<red>Pech!</red> <gray>Steinschlag – <white><name></white> verliert</gray> <red><n> Taler</red><gray>.</gray>",
                "<red>Bad luck!</red> <gray>Falling rocks – <white><name></white> loses</gray> <red><n> coins</red><gray>.</gray>");
        t("luck.nebel", "<red>Pech!</red> <gray>Dichter Nebel – <white><name></white> muss <white><steps></white> <steps_pl:'0#Felder|1#Feld|1<Felder'/> zurück.</gray>",
                "<red>Bad luck!</red> <gray>Thick fog – <white><name></white> has to go back <white><steps></white> <steps_pl:'0#fields|1#field|1<fields'/>.</gray>");
        t("luck.spendieren", "<red>Pech!</red> <gray><white><name></white> gibt eine Runde aus –</gray> <red><n> Taler</red> <gray>für die anderen.</gray>",
                "<red>Bad luck!</red> <gray><white><name></white> buys a round –</gray> <red><n> coins</red> <gray>for the others.</gray>");
        t("luck.verloren", "<red>Pech!</red> <gray>Aus dem Rucksack von <white><name></white> fällt etwas heraus: <item> (oder ein paar Taler).</gray>",
                "<red>Bad luck!</red> <gray>Something falls out of <white><name></white>'s backpack: <item> (or a few coins).</gray>");
        t("luck.blocked", "<red>Pech!</red> <gray>… aber der <bl>Schild</bl> von <white><name></white> hält alles ab.</gray>",
                "<red>Bad luck!</red> <gray>… but <white><name></white>'s <bl>shield</bl> blocks it all.</gray>");

        // ---------- Edelweiß-Stand ----------
        t("stand.title", "Edelweiß-Stand",
                "Edelweiss stand");
        t("stand.buy", "<ed>✿ Edelweiß kaufen</ed> <gray>– <au><price> Taler</au></gray>",
                "<ed>✿ Buy Edelweiss</ed> <gray>– <au><price> coins</au></gray>");
        t("stand.buy.lore", "<gray>Du hast <au><taler> Taler</au>.</gray>\n<gray>Danach zieht der Stand weiter.</gray>",
                "<gray>You have <au><taler> coins</au>.</gray>\n<gray>Afterwards, the stand moves on.</gray>");
        t("stand.skip", "<gray>Nein danke, weiter</gray>",
                "<gray>No thanks, move on</gray>");
        t("stand.poor", "<gray>Am <ed>Edelweiß-Stand</ed> kostet eins <au><price> Taler</au> – du hast nur <au><taler></au>.</gray>",
                "<gray>At the <ed>Edelweiss stand</ed> one costs <au><price> coins</au> – you only have <au><taler></au>.</gray>");
        t("stand.bought", "<white><name></white> <gray>kauft ein</gray> <ed>✿ Edelweiß</ed><gray>! Jetzt: <white><n></white>.</gray>",
                "<white><name></white> <gray>buys an</gray> <ed>✿ Edelweiss</ed><gray>! Now: <white><n></white>.</gray>");
        t("stand.bought.title", "<ed>✿ Edelweiß!</ed>",
                "<ed>✿ Edelweiss!</ed>");
        t("stand.moved", "<gray>Der Stand zieht weiter …</gray>",
                "<gray>The stand moves on …</gray>");
        t("stand.moves", "<gray>Der <ed>Edelweiß-Stand</ed> steht jetzt auf Feld <white><field></white>.</gray>",
                "<gray>The <ed>Edelweiss stand</ed> is now on field <white><field></white>.</gray>");

        // ---------- Duell ----------
        t("duel.title", "Wen forderst du heraus?",
                "Who do you challenge?");
        t("duel.pick", "<gold>⚔ <name></gold>", "<gold>⚔ <name></gold>");
        t("duel.pick.lore", "<gray>Hat <au><taler> Taler</au>.</gray>",
                "<gray>Has <au><taler> coins</au>.</gray>");
        t("duel.field", "<white><name></white> <gray>steht auf einem <gold>Duellfeld</gold> – Duell um <au><n> Taler</au>!</gray>",
                "<white><name></white> <gray>is on a <gold>duel field</gold> – duel for <au><n> coins</au>!</gray>");
        t("duel.vs", "<gold>⚔</gold> <white><one></white> <gray>gegen</gray> <white><two></white> <gray>– ab zum Duell-Minispiel!</gray>",
                "<gold>⚔</gold> <white><one></white> <gray>vs</gray> <white><two></white> <gray>– off to a duel minigame!</gray>");
        t("duel.dice", "<gold>⚔</gold> <white><one></white> <gray>gegen</gray> <white><two></white> <gray>– beide würfeln …</gray>",
                "<gold>⚔</gold> <white><one></white> <gray>vs</gray> <white><two></white> <gray>– both roll …</gray>");
        t("duel.mini", "<gold>⚔ Duell! ⚔</gold>",
                "<gold>⚔ Duel! ⚔</gold>");
        t("duel.result", "<gold>⚔</gold> <name> <gray>–</gray> <white><one></white> <au><p1></au> <gray>:</gray> <au><p2></au> <white><two></white>",
                "<gold>⚔</gold> <name> <gray>–</gray> <white><one></white> <au><p1></au> <gray>:</gray> <au><p2></au> <white><two></white>");
        t("duel.title.draw", "<gray>Unentschieden</gray>",
                "<gray>Draw</gray>");
        t("duel.won", "<white><winner></white> <gray>gewinnt das Duell und bekommt</gray> <au><n> Taler</au> <gray>von <white><loser></white>.</gray>",
                "<white><winner></white> <gray>wins the duel and gets</gray> <au><n> coins</au> <gray>from <white><loser></white>.</gray>");
        t("duel.blocked", "<white><winner></white> <gray>gewinnt das Duell – aber der <bl>Schild</bl> von <white><loser></white> schützt die Taler (<n>).</gray>",
                "<white><winner></white> <gray>wins the duel – but <white><loser></white>'s <bl>shield</bl> protects the coins (<n>).</gray>");
        t("duel.draw", "<gray>Unentschieden – niemand zahlt.</gray>",
                "<gray>A draw – nobody pays.</gray>");
        t("duel.alone", "<gray>Niemand da zum Duellieren.</gray>",
                "<gray>Nobody left to duel.</gray>");

        // ---------- Seiten im Minispiel ----------
        t("format.team", "<gray>Zwei gegen zwei:</gray> <blue><sidea></blue> <gray>gegen</gray> <red><sideb></red> <gray>(je zwei auf blauen und roten Feldern)</gray>",
                "<gray>Two against two:</gray> <blue><sidea></blue> <gray>vs</gray> <red><sideb></red> <gray>(two each on blue and red fields)</gray>");
        t("format.solo", "<gray>Einer gegen drei:</gray> <au><sidea></au> <gray>gegen</gray> <blue><sideb></blue> <gray>– der Einzelne hat einen Vorteil.</gray>",
                "<gray>One against three:</gray> <au><sidea></au> <gray>vs</gray> <blue><sideb></blue> <gray>– the lone player has an advantage.</gray>");
        t("format.duel", "<gray>Duell:</gray> <blue><sidea></blue> <gray>gegen</gray> <red><sideb></red>",
                "<gray>Duel:</gray> <blue><sidea></blue> <gray>vs</gray> <red><sideb></red>");
        t("team.won", "<au>★</au> <white><names></white> <gray>gewinnen – je</gray> <au><n> Taler</au><gray>.</gray>",
                "<au>★</au> <white><names></white> <gray>win – </gray><au><n> coins</au> <gray>each.</gray>");
        t("team.won.one", "<au>★</au> <white><names></white> <gray>gewinnt allein gegen alle –</gray> <au><n> Taler</au><gray>!</gray>",
                "<au>★</au> <white><names></white> <gray>wins alone against everyone –</gray> <au><n> coins</au><gray>!</gray>");
        t("team.draw", "<gray>Gleichstand zwischen den Seiten – alle bekommen</gray> <au><n> Taler</au><gray>.</gray>",
                "<gray>The sides are level – everyone gets</gray> <au><n> coins</au><gray>.</gray>");

        // ---------- Laden und Gegenstände ----------
        t("shop.title", "Krämerladen",
                "Shop");
        t("shop.enter", "<white><name></white> <gray>schaut im <yellow>Krämerladen</yellow> vorbei.</gray>",
                "<white><name></white> <gray>drops by the <yellow>shop</yellow>.</gray>");
        t("shop.price", "<au>Preis: <price> Taler</au> <gray>– klicken zum Kaufen</gray>",
                "<au>Price: <price> coins</au> <gray>– click to buy</gray>");
        t("shop.cannot", "<red>Preis: <price> Taler</red> <gray>– zu teuer oder Rucksack voll</gray>",
                "<red>Price: <price> coins</red> <gray>– too expensive or backpack full</gray>");
        t("shop.bought", "<gray>Gekauft: <item>. Du hast noch <au><taler> Taler</au>.</gray>",
                "<gray>Bought: <item>. You have <au><taler> coins</au> left.</gray>");
        t("shop.leave", "<em>✔ Fertig</em>",
                "<em>✔ Done</em>");
        t("shop.wallet", "<gray>Geldbeutel: <au><taler> Taler</au></gray>\n<gray>Rucksack: <white><n></white>/<max></gray>",
                "<gray>Purse: <au><taler> coins</au></gray>\n<gray>Backpack: <white><n></white>/<max></gray>");
        t("item.doppelwuerfel", "<em>Doppelwürfel</em>",
                "<em>Double dice</em>");
        t("item.doppelwuerfel.desc", "<gray>Diesmal würfelst du mit zwei Würfeln (2–12).</gray>",
                "<gray>This time you roll two dice (2–12).</gray>");
        t("item.zielwuerfel", "<em>Zielwürfel</em>",
                "<em>Pick-a-number dice</em>");
        t("item.zielwuerfel.desc", "<gray>Du suchst dir die Augenzahl selbst aus (1–6).</gray>",
                "<gray>You pick the number yourself (1–6).</gray>");
        t("item.tausch", "<em>Platztausch</em>",
                "<em>Swap</em>");
        t("item.tausch.desc", "<gray>Tausche deinen Platz mit jemandem.</gray>",
                "<gray>Swap places with someone.</gray>");
        t("item.talerklau", "<em>Talerklau</em>",
                "<em>Coin thief</em>");
        t("item.talerklau.desc", "<gray>Nimm jemandem ein paar Taler ab.</gray>",
                "<gray>Take a few coins from someone.</gray>");
        t("item.schild", "<em>Schild</em>",
                "<em>Shield</em>");
        t("item.schild.desc", "<gray>Schützt dich einmal vor Verlust, Pech, Klau und Tausch.</gray>",
                "<gray>Protects you once from losses, bad luck, theft and swaps.</gray>");
        t("item.lockruf", "<em>Lockruf</em>",
                "<em>Edelweiss call</em>");
        t("item.lockruf.desc", "<gray>Bringt dich direkt vor den Edelweiß-Stand.</gray>",
                "<gray>Takes you right in front of the Edelweiss stand.</gray>");
        t("bag.title", "Dein Rucksack",
                "Your backpack");
        t("bag.empty", "<gray>Leer</gray>",
                "<gray>Empty</gray>");
        t("bag.empty.lore", "<gray>Gegenstände gibt es im Krämerladen</gray>\n<gray>und auf rosa Feldern.</gray>",
                "<gray>Get items in the shop</gray>\n<gray>and on pink fields.</gray>");
        t("bag.click", "<em>▶ Klicken zum Einsetzen</em>",
                "<em>▶ Click to use</em>");
        t("bag.notnow", "<gray>Einsetzen geht, wenn du dran bist.</gray>",
                "<gray>You can use it on your turn.</gray>");
        t("bag.used", "<gray>Diesen Zug hast du schon etwas eingesetzt.</gray>",
                "<gray>You already used an item this turn.</gray>");
        t("bag.number", "Welche Augenzahl?",
                "Which number?");
        t("bag.target", "Wen trifft es?",
                "Who is it for?");
        t("bag.target.lore", "<gray>Feld <white><field></white> · <au><taler> Taler</au></gray>",
                "<gray>Field <white><field></white> · <au><taler> coins</au></gray>");
        t("bag.problem.used", "<gray>Nur ein Gegenstand pro Zug.</gray>",
                "<gray>Only one item per turn.</gray>");
        t("bag.problem.missing", "<gray>Den hast du nicht (mehr).</gray>",
                "<gray>You don't have that (anymore).</gray>");
        t("bag.problem.target", "<gray>Da ist niemand zum Auswählen.</gray>",
                "<gray>There is nobody to pick.</gray>");
        t("bag.problem.number", "<gray>Bitte eine Zahl von 1 bis 6.</gray>",
                "<gray>Please pick a number from 1 to 6.</gray>");
        t("bag.problem.shield", "<gray>Du hast schon einen Schild.</gray>",
                "<gray>You already have a shield.</gray>");
        t("use.doppelwuerfel", "<white><name></white> <gray>setzt den</gray> <item> <gray>ein – zwei Würfel!</gray>",
                "<white><name></white> <gray>uses the</gray> <item> <gray>– two dice!</gray>");
        t("use.zielwuerfel", "<white><name></white> <gray>setzt den</gray> <item> <gray>ein und wählt die <au><n></au>.</gray>",
                "<white><name></white> <gray>uses the</gray> <item> <gray>and picks <au><n></au>.</gray>");
        t("use.tausch", "<white><name></white> <gray>tauscht den Platz mit <white><other></white>!</gray>",
                "<white><name></white> <gray>swaps places with <white><other></white>!</gray>");
        t("use.talerklau", "<white><name></white> <gray>nimmt <white><other></white></gray> <au><n> Taler</au> <gray>ab!</gray>",
                "<white><name></white> <gray>takes</gray> <au><n> coins</au> <gray>from <white><other></white>!</gray>");
        t("use.schild", "<white><name></white> <gray>hält jetzt einen <bl>Schild</bl> bereit.</gray>",
                "<white><name></white> <gray>now has a <bl>shield</bl> ready.</gray>");
        t("use.lockruf", "<white><name></white> <gray>folgt dem</gray> <item> <gray>– direkt vor den Edelweiß-Stand!</gray>",
                "<white><name></white> <gray>follows the</gray> <item> <gray>– right in front of the Edelweiss stand!</gray>");
        t("use.blocked", "<white><name></white> <gray>versucht es mit</gray> <item> <gray>– aber der <bl>Schild</bl> von <white><other></white> hält!</gray>",
                "<white><name></white> <gray>tries</gray> <item> <gray>– but <white><other></white>'s <bl>shield</bl> holds!</gray>");

        // ---------- Übersicht ----------
        t("overview.title", "Übersicht",
                "Overview");
        t("overview.player", "<gray>Platz <white><place></white> · Feld <white><field></white>/<goal></gray>\n<ed>✿ <edelweiss> Edelweiß</ed> <au>● <taler> Taler</au>\n<gray>Minispiele gewonnen: <white><minis></white></gray>",
                "<gray>Place <white><place></white> · field <white><field></white>/<goal></gray>\n<ed>✿ <edelweiss> Edelweiss</ed> <au>● <taler> coins</au>\n<gray>Minigames won: <white><minis></white></gray>");
        t("overview.shield", "<bl>◆ Schild bereit</bl>",
                "<bl>◆ Shield ready</bl>");
        t("overview.stand", "<ed>✿ Edelweiß-Stand</ed> <gray>auf Feld <white><field></white> · <au><price> Taler</au></gray>",
                "<ed>✿ Edelweiss stand</ed> <gray>on field <white><field></white> · <au><price> coins</au></gray>");

        // ---------- Minispiele ----------
        t("mini.title", "<au>✦ Minispiel! ✦</au>",
                "<au>✦ Minigame! ✦</au>");
        t("mini.announce", "<au>✦ Minispiel:</au> <white><name></white>",
                "<au>✦ Minigame:</au> <white><name></white>");
        t("mini.go", "<em><b>Los!</b></em>",
                "<em><b>Go!</b></em>");
        t("mini.bar", "<au><name></au> <gray>· noch</gray> <white><s></white>",
                "<au><name></au> <gray>·</gray> <white><s></white> <gray>left</gray>");
        t("mini.out.title", "<red>Raus!</red>",
                "<red>Out!</red>");
        t("mini.out.sub", "<gray>Schau den anderen zu</gray>",
                "<gray>Watch the others</gray>");
        t("mini.out", "<white><name></white> <gray>ist raus – noch <white><n></white> übrig.</gray>",
                "<white><name></white> <gray>is out – <white><n></white> left.</gray>");
        t("mini.points.team", "<au>Punkte: <n></au> <gray>· deine Seite:</gray> <au><team></au>",
                "<au>Points: <n></au> <gray>· your side:</gray> <au><team></au>");
        t("mini.points", "<au>Punkte: <n></au>",
                "<au>Points: <n></au>");
        t("mini.results", "<au>━━ Ergebnis: <name> ━━</au>",
                "<au>━━ Result: <name> ━━</au>");
        t("mini.line", "<au><pos>.</au> <white><name></white> <gray>→</gray> <au><prize></au>",
                "<au><pos>.</au> <white><name></white> <gray>→</gray> <au><prize></au>");
        t("mini.line.points", "<au><pos>.</au> <white><name></white> <gray>(<points> <points_pl:'0#Punkte|1#Punkt|1<Punkte'/>) →</gray> <au><prize></au>",
                "<au><pos>.</au> <white><name></white> <gray>(<points> <points_pl:'0#points|1#point|1<points'/>) →</gray> <au><prize></au>");
        t("mini.line.finish", "<au><pos>.</au> <white><name></white> <gray>(im Ziel nach <time> s) →</gray> <au><prize></au>",
                "<au><pos>.</au> <white><name></white> <gray>(finished in <time> s) →</gray> <au><prize></au>");
        t("mini.line.way", "<au><pos>.</au> <white><name></white> <gray>(<points> <points_pl:'0#Blöcke|1#Block|1<Blöcke'/> weit) →</gray> <au><prize></au>",
                "<au><pos>.</au> <white><name></white> <gray>(<points> <points_pl:'0#blocks|1#block|1<blocks'/> far) →</gray> <au><prize></au>");
        t("mini.line.top", "<au><pos>.</au> <white><name></white> <gray>(oben nach <time> s) →</gray> <au><prize></au>",
                "<au><pos>.</au> <white><name></white> <gray>(at the top in <time> s) →</gray> <au><prize></au>");
        t("mini.line.height", "<au><pos>.</au> <white><name></white> <gray>(<points> <points_pl:'0#Blöcke|1#Block|1<Blöcke'/> hoch) →</gray> <au><prize></au>",
                "<au><pos>.</au> <white><name></white> <gray>(<points> <points_pl:'0#blocks|1#block|1<blocks'/> high) →</gray> <au><prize></au>");
        t("mini.prize.taler", "+<n> Taler",
                "+<n> <n_pl:'0#coins|1#coin|1<coins'/>");
        t("mini.prize.points", "+<n> <n_pl:'0#Punkte|1#Punkt|1<Punkte'/>",
                "+<n> <n_pl:'0#points|1#point|1<points'/>");
        t("mini.got.points", "<au>+<n> Punkte</au>",
                "<au>+<n> points</au>");
        t("mini.won", "<au>Gewonnen!</au>",
                "<au>You won!</au>");
        t("mini.over", "<gray>Vorbei!</gray>",
                "<gray>Over!</gray>");
        t("mini.got", "<au>+<n> Taler</au>",
                "<au>+<n> coins</au>");
        t("mini.stick", "<em>Wanderstock</em>",
                "<em>Walking stick</em>");
        t("mini.broeckelboden", "<au>Bröckelboden</au>",
                "<au>Crumbling Floor</au>");
        t("mini.broeckelboden.desc", "<gray>Wo du stehst, bröckelt der Boden weg. Bleib in Bewegung – wer unten hinausfällt, ist raus!</gray>",
                "<gray>Wherever you stand, the floor crumbles away. Keep moving – whoever falls out the bottom is out!</gray>");
        t("mini.talerregen", "<au>Talerregen</au>",
                "<au>Coin Shower</au>");
        t("mini.talerregen.desc", "<gray>Goldtaler fallen vom Himmel – sammle so viele wie möglich! Goldbarren zählen dreifach.</gray>",
                "<gray>Gold coins rain from the sky – collect as many as you can! Gold bars count triple.</gray>");
        t("mini.bergsprint", "<au>Bergsprint</au>",
                "<au>Hill Sprint</au>");
        t("mini.bergsprint.desc", "<gray>Ein Wettlauf den Almweg hinauf. Wer zuerst durchs Ziel läuft, gewinnt!</gray>",
                "<gray>A race up the alpine path. First across the finish line wins!</gray>");
        t("mini.pfeilhagel", "<au>Pfeilhagel</au>",
                "<au>Arrow Storm</au>");
        t("mini.pfeilhagel.desc", "<gray>Pfeile prasseln vom Himmel. Wer getroffen wird, ist raus – weich aus!</gray>",
                "<gray>Arrows rain down from the sky. Whoever gets hit is out – dodge!</gray>");
        t("mini.heissebrezn", "<au>Heiße Brezn</au>",
                "<au>Hot Pretzel</au>");
        t("mini.heissebrezn.desc", "<gray>Wer die heiße Brezn hat, gibt sie mit einem Schlag weiter. Wer sie hält, wenn die Zeit abläuft, ist raus!</gray>",
                "<gray>Whoever has the hot pretzel passes it on with a hit. Whoever holds it when time runs out is out!</gray>");
        t("mini.trachtenfarben", "<au>Trachtenfarben</au>",
                "<au>Colour Memory</au>");
        t("mini.trachtenfarben.desc", "<gray>Merk dir die gezeigte Farbe! Danach verschwindet jede andere – stell dich rechtzeitig auf die richtige.</gray>",
                "<gray>Remember the colour you are shown! Then every other colour disappears – stand on the right one in time.</gray>");
        t("mini.gipfelkoenig", "<au>Gipfelkönig</au>",
                "<au>King of the Hill</au>");
        t("mini.gipfelkoenig.desc", "<gray>Steh oben auf dem goldenen Gipfel und sammle Punkte. Mit dem Wanderstock schubst du die anderen herunter.</gray>",
                "<gray>Stand on the golden peak to collect points. Use your walking stick to push the others off.</gray>");
        t("mini.kuhglocke", "<au>Kuhglocke</au>",
                "<au>Cowbell</au>");
        t("mini.kuhglocke.desc", "<gray>Warte, bis die Tafel grün wird – dann so schnell wie möglich klicken! Zu früh kostet einen Punkt.</gray>",
                "<gray>Wait until the board turns green – then click as fast as you can! Too early costs a point.</gray>");
        t("mini.schaefchen", "<au>Schäfchen zählen</au>",
                "<au>Counting Sheep</au>");
        t("mini.schaefchen.desc", "<gray>Zähl die Schafe im Pferch! Danach im Menü die richtige Zahl wählen. Drei Runden.</gray>",
                "<gray>Count the sheep in the pen! Then pick the right number in the menu. Three rounds.</gray>");
        t("mini.holzhacker", "<au>Holzhacker</au>",
                "<au>Wood Chopper</au>");
        t("mini.holzhacker.desc", "<gray>Schlag auf die Holzklötze – drei Hiebe, dann ist er gespalten. Wer den letzten Hieb setzt, bekommt den Punkt.</gray>",
                "<gray>Hit the logs – three blows and it's split. Whoever lands the last blow gets the point.</gray>");
        t("mini.rempelei", "<au>Rempelei</au>",
                "<au>Shove Off</au>");
        t("mini.rempelei.desc", "<gray>Schubs die anderen mit dem Wanderstock von der Plattform. Der Rand bröckelt mit der Zeit!</gray>",
                "<gray>Push the others off the platform with your walking stick. The edge crumbles over time!</gray>");
        t("mini.scheibenschiessen", "<au>Scheibenschießen</au>",
                "<au>Target Practice</au>");
        t("mini.scheibenschiessen.desc", "<gray>Wirf Schneebälle auf die Zielscheiben. Jeder Treffer ist ein Punkt!</gray>",
                "<gray>Throw snowballs at the targets. Every hit is a point!</gray>");
        t("mini.race.finish", "<white><name></white> <gray>ist im Ziel – Platz <au><pos></au>!</gray>",
                "<white><name></white> <gray>crossed the finish line – place <au><pos></au>!</gray>");
        t("mini.race.done", "<em>Im Ziel!</em>",
                "<em>Finished!</em>");
        t("mini.heissebrezn.item", "<gold>Heiße Brezn</gold>",
                "<gold>Hot pretzel</gold>");
        t("mini.heissebrezn.got", "<gold>Du hast die Brezn!</gold>",
                "<gold>You've got the pretzel!</gold>");
        t("mini.heissebrezn.got.sub", "<gray>Schnell weitergeben – schlag jemanden!</gray>",
                "<gray>Pass it on quickly – hit someone!</gray>");
        t("mini.heissebrezn.has", "<white><name></white> <gray>hat die heiße Brezn!</gray>",
                "<white><name></white> <gray>has the hot pretzel!</gray>");
        t("mini.heissebrezn.bar", "<gold>Heiße Brezn! Noch <s> s</gold>",
                "<gold>Hot pretzel! <s> s left</gold>");
        t("mini.trachtenfarben.remember", "<gray>Merk dir diese Farbe!</gray>",
                "<gray>Remember this colour!</gray>");
        t("mini.trachtenfarben.hidden", "<gray>Welche Farbe war es? Schnell hin!</gray>",
                "<gray>Which colour was it? Hurry!</gray>");
        t("mini.trachtenfarben.was", "<gray>Gesucht war: <colour></gray>",
                "<gray>The colour was: <colour></gray>");
        t("mini.kuhglocke.item", "<em>Glockenseil</em>",
                "<em>Bell rope</em>");
        t("mini.kuhglocke.item.lore", "<gray>Klicken, sobald die Tafel grün ist!</gray>",
                "<gray>Click as soon as the board is green!</gray>");
        t("mini.kuhglocke.wait", "<red>Warte …</red>",
                "<red>Wait …</red>");
        t("mini.kuhglocke.round", "<gray>Runde <n> von <of></gray>",
                "<gray>Round <n> of <of></gray>");
        t("mini.kuhglocke.now", "<green><b>JETZT!</b></green>",
                "<green><b>NOW!</b></green>");
        t("mini.kuhglocke.early", "<red>Zu früh! −1 Punkt</red>",
                "<red>Too early! −1 point</red>");
        t("mini.kuhglocke.fast", "<white><name></white> <gray>war Nummer <au><pos></au>!</gray>",
                "<white><name></white> <gray>was number <au><pos></au>!</gray>");
        t("mini.kuhglocke.nobody", "<gray>Niemand hat geklickt …</gray>",
                "<gray>Nobody clicked …</gray>");
        t("mini.schaefchen.count", "<ed>Zähl die Schafe!</ed>",
                "<ed>Count the sheep!</ed>");
        t("mini.schaefchen.menu", "Wie viele Schafe waren es?",
                "How many sheep were there?");
        t("mini.schaefchen.option", "<au><n> Schafe</au>",
                "<au><n> sheep</au>");
        t("mini.schaefchen.answered", "<gray>Deine Antwort: <white><n></white></gray>",
                "<gray>Your answer: <white><n></white></gray>");
        t("mini.schaefchen.result", "<gray>Es waren <au><n> Schafe</au>!</gray>",
                "<gray>There were <au><n> sheep</au>!</gray>");
        t("mini.holzhacker.item", "<em>Spaltaxt</em>",
                "<em>Splitting axe</em>");
        t("mini.rempelei.shrink", "<red>Der Rand bröckelt!</red>",
                "<red>The edge is crumbling!</red>");
        t("mini.scheibenschiessen.item", "<em>Schneebälle</em>",
                "<em>Snowballs</em>");

        // ---------- Neue Minispiele (Paket 1) ----------
        t("mini.lawinenhang", "<au>Lawinenhang</au>",
                "<au>Avalanche Slope</au>");
        t("mini.lawinenhang.desc", "<gray>Lauf den Schneehang hinauf! Von oben rollen Schneebrocken – wer getroffen wird, rutscht ein Stück zurück. Ausweichen oder drüberspringen!</gray>",
                "<gray>Run up the snowy slope! Snowballs roll down from the top – if one hits you, you slide back a bit. Dodge or jump over them!</gray>");
        t("mini.lawinenhang.hit", "<aqua>Erwischt! Ein Stück zurück …</aqua>",
                "<aqua>Hit! Sliding back …</aqua>");
        t("mini.maibaumkraxeln", "<au>Maibaumkraxeln</au>",
                "<au>Maypole Climb</au>");
        t("mini.maibaumkraxeln.desc", "<gray>Kletter an Leitern und Ranken den Maibaum hinauf bis zum Kranz! Wo es staubt, bricht gleich ein Stück weg – dann zur Seite ausweichen.</gray>",
                "<gray>Climb the maypole on ladders and vines up to the wreath! Where it starts to crumble, a piece breaks away – step aside in time.</gray>");
        t("mini.maibaumkraxeln.finish", "<white><name></white> <gray>hat den Kranz erreicht – Platz <au><pos></au>!</gray>",
                "<white><name></white> <gray>reached the wreath – place <au><pos></au>!</gray>");
        t("mini.eisstockschiessen", "<au>Eisstockschießen</au>",
                "<au>Ice Stock Sport</au>");
        t("mini.eisstockschiessen.desc", "<gray>Drei Würfe für alle gleichzeitig: schau zur Daube (dem kleinen Holzklotz) und klick, wenn der Kraftbalken passt – etwa zwei Drittel. Je näher dein Stock liegt, desto mehr Punkte. Stöcke der anderen kannst du wegschießen!</gray>",
                "<gray>Three throws, everyone at once: look at the target block (the little wooden block) and click when the power bar is right – about two thirds. The closer your stone stops, the more points. You can knock the others' stones away!</gray>");
        t("mini.eisstockschiessen.power", "<gray>Kraft</gray> <bar> <white><n> %</white>",
                "<gray>Power</gray> <bar> <white><n> %</white>");
        t("mini.eisstockschiessen.thrown", "<gray>Geworfen mit <white><n> %</white> Kraft!</gray>",
                "<gray>Thrown with <white><n> %</white> power!</gray>");
        t("mini.eisstockschiessen.colour", "<gray>Dein Eisstock hat die Farbe <colour><gray>.</gray></gray>",
                "<gray>Your ice stock is <colour><gray>.</gray></gray>");
        t("mini.eisstockschiessen.colour.short", "<gray>Dein Stock: <colour></gray>",
                "<gray>Your stock: <colour></gray>");
        t("mini.eisstockschiessen.round", "<au>Wurf <n> von <of></au>",
                "<au>Throw <n> of <of></au>");
        t("mini.eisstockschiessen.lead", "<gray>Nach Wurf <n> liegt <white><name></white> vorn: <au><points> <points_pl:'0#Punkte|1#Punkt|1<Punkte'/></au>.</gray>",
                "<gray>After throw <n>, <white><name></white> leads: <au><points> <points_pl:'0#points|1#point|1<points'/></au>.</gray>");
        t("mini.eisstockschiessen.nobody", "<gray>Nach Wurf <n> liegt noch kein Stock in den Ringen.</gray>",
                "<gray>After throw <n>, no stone is inside the rings yet.</gray>");
        t("mini.eisstockschiessen.item", "<em>Eisstock</em>",
                "<em>Ice stock</em>");
        t("mini.eisstockschiessen.item.lore", "<gray>Klicken, wenn der Kraftbalken passt!</gray>",
                "<gray>Click when the power bar is right!</gray>");
        t("mini.murmeltier", "<au>Murmeltier-Klopfen</au>",
                "<au>Marmot Bonk</au>");
        t("mini.murmeltier.desc", "<gray>Murmeltiere schauen aus den Löchern – schnell hin und draufhauen! Braun gibt einen Punkt, golden drei. Vorsicht vor Steinbrocken: die kosten einen Punkt.</gray>",
                "<gray>Marmots peek out of their holes – run over and whack them! Brown ones give a point, golden ones three. Watch out for rocks: they cost a point.</gray>");
        t("mini.murmeltier.item", "<em>Klopfhammer</em>",
                "<em>Whacking hammer</em>");
        t("mini.murmeltier.stone", "<red>Autsch, ein Stein! −1 Punkt</red>",
                "<red>Ouch, a rock! −1 point</red>");
        // ---------- Neue Minispiele (Paket 2) ----------
        t("mini.goldwaschen", "<au>Goldwaschen</au>",
                "<au>Gold Panning</au>");
        t("mini.goldwaschen.desc", "<gray>Am Bach liegen Kies- und Sandhaufen. Schlag drauf (oder Rechtsklick) und wasch sie aus – in manchen steckt Gold! Wo es kurz glitzert, lohnt es sich.</gray>",
                "<gray>Gravel and sand piles lie along the stream. Hit one (or right-click) to pan it – some hide gold! Where it sparkles for a moment, it's worth a look.</gray>");
        t("mini.goldwaschen.item", "<au>Waschpfanne</au>",
                "<au>Gold pan</au>");
        t("mini.goldwaschen.item.lore", "<gray>Auf einen Kies- oder Sandhaufen schlagen</gray>",
                "<gray>Hit a gravel or sand pile</gray>");
        t("mini.goldwaschen.big", "<au><b>Ein Goldklumpen! +3</b></au>",
                "<au><b>A gold nugget! +3</b></au>");
        t("mini.goldwaschen.empty", "<gray>Nur Kies …</gray>",
                "<gray>Just gravel …</gray>");
        t("mini.schuhplattler", "<au>Schuhplattler</au>",
                "<au>Schuhplattler (Slap Dance)</au>");
        t("mini.schuhplattler.desc", "<gray>Tanz im Takt! Angesagt wird Springen, Ducken, Klatschen (klicken) oder Stillstehen – mach es genau auf den nächsten Stampfer. Ganz genau gibt 2 Punkte.</gray>",
                "<gray>Dance to the beat! The call is jump, duck, clap (click) or stand still – do it right on the next stomp. Spot on gives 2 points.</gray>");
        t("mini.schuhplattler.item", "<em>Hutfeder</em>",
                "<em>Hat feather</em>");
        t("mini.schuhplattler.item.lore", "<gray>Klicken = Klatschen</gray>",
                "<gray>Click = clap</gray>");
        t("mini.schuhplattler.move.jump", "<au><b>↑ Springen!</b></au>",
                "<au><b>↑ Jump!</b></au>");
        t("mini.schuhplattler.move.duck", "<aqua><b>↓ Ducken!</b></aqua>",
                "<aqua><b>↓ Duck!</b></aqua>");
        t("mini.schuhplattler.move.clap", "<em><b>✦ Klatschen!</b></em>",
                "<em><b>✦ Clap!</b></em>");
        t("mini.schuhplattler.move.still", "<white><b>■ Stillstehen!</b></white>",
                "<white><b>■ Stand still!</b></white>");
        t("mini.schuhplattler.onone", "<gray>auf den nächsten Stampfer</gray>",
                "<gray>on the next stomp</gray>");
        t("mini.schuhplattler.now", "<au><b>♪ Jetzt! ♪</b></au>",
                "<au><b>♪ Now! ♪</b></au>");
        t("mini.schuhplattler.perfect", "<au><b>Perfekt! +2</b></au> <gray>(Punkte: <n>)</gray>",
                "<au><b>Perfect! +2</b></au> <gray>(points: <n>)</gray>");
        t("mini.schuhplattler.good", "<em>Gut! +1</em> <gray>(Punkte: <n>)</gray>",
                "<em>Good! +1</em> <gray>(points: <n>)</gray>");
        t("mini.schuhplattler.miss", "<gray>Daneben …</gray>",
                "<gray>Missed …</gray>");
        t("mini.brueckenbau", "<au>Brückenbau</au>",
                "<au>Bridge Builders</au>");
        t("mini.brueckenbau.desc", "<gray>Bau dir mit den Brettern eine Brücke über die Schlucht – an die Kante setzen, draufsteigen, weiter! Wer zuerst drüben ist, gewinnt. Runterfallen heißt: zurück an den Start.</gray>",
                "<gray>Build a bridge across the gorge with your planks – place them at the edge, step on, keep going! First across wins. Fall down and you're back at the start.</gray>");
        t("mini.brueckenbau.item", "<em>Bretter</em>",
                "<em>Planks</em>");
        t("mini.brueckenbau.fell", "<aqua>Hinuntergefallen – zurück an den Start. Deine Brücke steht noch!</aqua>",
                "<aqua>You fell – back to the start. Your bridge is still there!</aqua>");
        t("mini.schneeballschlacht", "<au>Schneeballschlacht</au>",
                "<au>Snowball Fight</au>");
        t("mini.schneeballschlacht.desc", "<gray>Jeder gegen jeden! Hol dir Schneebälle an den Schneewehen (anklicken) und triff die anderen – jeder Treffer ist ein Punkt. Wer getroffen wurde, leuchtet kurz und ist so lange sicher.</gray>",
                "<gray>Everyone against everyone! Grab snowballs from the snowdrifts (click them) and hit the others – every hit is a point. Whoever got hit glows briefly and is safe for that moment.</gray>");
        t("mini.schneeballschlacht.grab", "<aqua>Schneebälle: <n></aqua>",
                "<aqua>Snowballs: <n></aqua>");
        t("mini.schneeballschlacht.full", "<gray>Deine Taschen sind voll.</gray>",
                "<gray>Your pockets are full.</gray>");
        t("mini.schneeballschlacht.hit", "<aqua>Getroffen von <white><name></white>!</aqua>",
                "<aqua>Hit by <white><name></white>!</aqua>");
        // ---------- Neue Minispiele (Paket 3) ----------
        t("mini.schlittenrennen", "<au>Schlittenrennen</au>",
                "<au>Sled Race</au>");
        t("mini.schlittenrennen.desc", "<gray>Zwei Runden im Schlitten über die Eisbahn! Lenken mit den Lauftasten. Die Tore zählen nur der Reihe nach – Abkürzen bringt nichts. Wer herausfällt, sitzt gleich wieder in einem neuen Schlitten.</gray>",
                "<gray>Two laps in a sled on the ice track! Steer with your movement keys. The gates only count in order – shortcuts don't help. If you fall out, you're back in a new sled right away.</gray>");
        t("mini.schlittenrennen.go", "<em><b>Ab die Post!</b></em>",
                "<em><b>Off you go!</b></em>");
        t("mini.schlittenrennen.lap", "<gray>Runde <n> von <of></gray>",
                "<gray>Lap <n> of <of></gray>");
        t("mini.schlittenrennen.last", "<au><b>Letzte Runde!</b></au>",
                "<au><b>Final lap!</b></au>");
        t("mini.schlittenrennen.lap.bar", "<aqua>Runde <white><n></white>/<of> · Tor <white><gate></white>/<gates></aqua>",
                "<aqua>Lap <white><n></white>/<of> · gate <white><gate></white>/<gates></aqua>");
        t("mini.line.gates", "<au><pos>.</au> <white><name></white> <gray>(<points> <points_pl:'0#Tore|1#Tor|1<Tore'/>) →</gray> <au><prize></au>",
                "<au><pos>.</au> <white><name></white> <gray>(<points> <points_pl:'0#gates|1#gate|1<gates'/>) →</gray> <au><prize></au>");
        t("mini.enziansuche", "<au>Enzian-Suche</au>",
                "<au>Gentian Hunt</au>");
        t("mini.enziansuche.desc", "<gray>Auf der bunten Almwiese wachsen blaue Enziane – versteckt im hohen Gras, hinter Felsen und Büschen. Finden und pflücken (Schlag oder Rechtsklick)! Jeder ist ein Punkt.</gray>",
                "<gray>Blue gentians grow on the colourful alpine meadow – hidden in tall grass, behind rocks and bushes. Find them and pick them (hit or right-click)! Each one is a point.</gray>");
        t("mini.enziansuche.item", "<em>Gartenschere</em>",
                "<em>Garden shears</em>");
        t("mini.musterweber", "<au>Musterweber</au>",
                "<au>Pattern Weaver</au>");
        t("mini.musterweber.desc", "<gray>Auf deinem Webrahmen erscheint kurz ein Muster – merk es dir! Dann ist er leer: Farbe in der Schnellleiste wählen und die Felder anklicken. Jedes richtige Feld ist ein Punkt, ganz richtig gibt 4 dazu.</gray>",
                "<gray>A pattern appears on your loom for a moment – remember it! Then it's blank: pick a colour in your hotbar and click the squares. Every right square is a point, all right gives 4 extra.</gray>");
        t("mini.musterweber.remember", "<au><b>Merk dir das Muster!</b></au>",
                "<au><b>Remember the pattern!</b></au>");
        t("mini.musterweber.round", "<gray>Runde <n> von <of></gray>",
                "<gray>Round <n> of <of></gray>");
        t("mini.musterweber.weave", "<em><b>Jetzt weben!</b></em>",
                "<em><b>Weave now!</b></em>");
        t("mini.musterweber.weave.sub", "<gray>Farbe wählen, Felder anklicken</gray>",
                "<gray>Pick a colour, click the squares</gray>");
        t("mini.musterweber.left", "<gray>Noch <white><s> s</white></gray>",
                "<gray><white><s> s</white> left</gray>");
        t("mini.musterweber.result", "<gray><white><n></white> von <of> richtig · Punkte: <au><points></au></gray>",
                "<gray><white><n></white> of <of> right · points: <au><points></au></gray>");
        t("mini.musterweber.perfect", "<au><b>Alles richtig! +4</b></au> <gray>(<n> von <of>) · Punkte: <points></gray>",
                "<au><b>All right! +4</b></au> <gray>(<n> of <of>) · points: <points></gray>");
        t("mini.foehnsturm", "<au>Föhnsturm</au>",
                "<au>Foehn Storm</au>");
        t("mini.foehnsturm.desc", "<gray>Oben am Gipfel bläst der Föhn! Vor jeder Böe ziehen Wolken, der Titel sagt, woher sie kommt. Stell dich hinter einen Felsen oder duck dich – wer hinunterfällt, ist raus.</gray>",
                "<gray>The foehn wind is blowing on the summit! Clouds come before every gust, and the title tells you where it comes from. Hide behind a rock or duck – whoever falls off is out.</gray>");
        t("mini.foehnsturm.gust", "<aqua><b>Föhnböe!</b></aqua>",
                "<aqua><b>Foehn gust!</b></aqua>");
        t("mini.foehnsturm.from.n", "<gray>aus Norden ↓</gray>", "<gray>from the north ↓</gray>");
        t("mini.foehnsturm.from.ne", "<gray>aus Nordosten ↙</gray>", "<gray>from the north-east ↙</gray>");
        t("mini.foehnsturm.from.e", "<gray>aus Osten ←</gray>", "<gray>from the east ←</gray>");
        t("mini.foehnsturm.from.se", "<gray>aus Südosten ↖</gray>", "<gray>from the south-east ↖</gray>");
        t("mini.foehnsturm.from.s", "<gray>aus Süden ↑</gray>", "<gray>from the south ↑</gray>");
        t("mini.foehnsturm.from.sw", "<gray>aus Südwesten ↗</gray>", "<gray>from the south-west ↗</gray>");
        t("mini.foehnsturm.from.w", "<gray>aus Westen →</gray>", "<gray>from the west →</gray>");
        t("mini.foehnsturm.from.nw", "<gray>aus Nordwesten ↘</gray>", "<gray>from the north-west ↘</gray>");
        t("mini.tauziehen", "<au>Tauziehen</au>",
                "<au>Tug of War</au>");
        t("mini.tauziehen.desc", "<gray>Über der Schlammgrube hängt ein Seil. Klick so schnell du kannst (Schlag oder Rechtsklick mit dem Seil) – wer den roten Knoten ganz herüberzieht, gewinnt; sonst zählt am Ende, auf welcher Seite er hängt.</gray>",
                "<gray>A rope hangs over the mud pit. Click as fast as you can (hit or right-click with the rope) – whoever pulls the red knot all the way over wins; otherwise the side the knot is on at the end wins.</gray>");
        t("mini.tauziehen.item", "<au>Seil</au> <gray>– klicken zum Ziehen</gray>",
                "<au>Rope</au> <gray>– click to pull</gray>");
        t("mini.tauziehen.bar", "<rope>  <gray>Du ziehst nach</gray> <side>",
                "<rope>  <gray>You pull to the</gray> <side>");
        t("mini.tauziehen.left", "<blue>links</blue>", "<blue>left</blue>");
        t("mini.tauziehen.right", "<red>rechts</red>", "<red>right</red>");
        t("mini.gamsjagd", "<au>Gamsjagd</au>",
                "<au>Chamois Chase</au>");
        t("mini.gamsjagd.desc", "<gray>Einer ist der Jäger, die anderen sind Gämsen. Der Jäger ist schneller, darf aber erst nach drei Sekunden los – ein Schlag fängt eine Gämse. Bleibt bis zum Ende eine übrig, gewinnen die Gämsen.</gray>",
                "<gray>One player is the hunter, the others are chamois. The hunter is faster but may only start after three seconds – one hit catches a chamois. If one is left at the end, the chamois win.</gray>");
        t("mini.gamsjagd.item", "<au>Jägerstock</au>",
                "<au>Hunter's stick</au>");
        t("mini.gamsjagd.you.hunter", "<au>Du bist der Jäger!</au>",
                "<au>You are the hunter!</au>");
        t("mini.gamsjagd.you.gams", "<green>Du bist eine Gämse – lauf!</green>",
                "<green>You are a chamois – run!</green>");
        t("mini.gamsjagd.go", "<au><name></au> <gray>läuft los!</gray>",
                "<au><name></au> <gray>is off!</gray>");
        t("mini.gamsjagd.caught", "<white><name></white> <gray>wurde von</gray> <au><hunter></au> <gray>gefangen.</gray>",
                "<white><name></white> <gray>was caught by</gray> <au><hunter></au><gray>.</gray>");
        t("mini.gamsjagd.bar", "<gray>Gämsen übrig:</gray> <green><n></green>",
                "<gray>Chamois left:</gray> <green><n></green>");
        t("colour.red", "<red><b>Rot</b></red>",
                "<red><b>Red</b></red>");
        t("colour.yellow", "<yellow><b>Gelb</b></yellow>",
                "<yellow><b>Yellow</b></yellow>");
        t("colour.lime", "<green><b>Hellgrün</b></green>",
                "<green><b>Lime</b></green>");
        t("colour.light_blue", "<aqua><b>Hellblau</b></aqua>",
                "<aqua><b>Light blue</b></aqua>");
        t("colour.blue", "<blue><b>Blau</b></blue>",
                "<blue><b>Blue</b></blue>");
        t("colour.orange", "<gold><b>Orange</b></gold>", "<gold><b>Orange</b></gold>");
        t("colour.pink", "<light_purple><b>Rosa</b></light_purple>",
                "<light_purple><b>Pink</b></light_purple>");
        t("colour.white", "<white><b>Weiß</b></white>",
                "<white><b>White</b></white>");

        // ---------- Ende ----------
        t("end.header", "<au>━━ Die Partie ist vorbei! ━━</au>",
                "<au>━━ The game is over! ━━</au>");
        t("end.line", "<au><pos>.</au> <white><name></white> <gray>–</gray> <ed>✿ <edelweiss></ed> <au>● <taler></au> <gray>· Feld <field></gray>",
                "<au><pos>.</au> <white><name></white> <gray>–</gray> <ed>✿ <edelweiss></ed> <au>● <taler></au> <gray>· field <field></gray>");
        t("end.line.marathon", "<au><pos>.</au> <white><name></white> <gray>–</gray> <au><points> <points_pl:'0#Punkte|1#Punkt|1<Punkte'/></au> <gray>· <minis> <minis_pl:'0#Minispiele|1#Minispiel|1<Minispiele'/> gewonnen</gray>",
                "<au><pos>.</au> <white><name></white> <gray>–</gray> <au><points> <points_pl:'0#points|1#point|1<points'/></au> <gray>· <minis> <minis_pl:'0#minigames|1#minigame|1<minigames'/> won</gray>");
        t("end.won", "<au><b>Gewonnen!</b></au>",
                "<au><b>You won!</b></au>");
        t("end.title", "<gray>Vorbei!</gray>",
                "<gray>Game over!</gray>");
        t("end.winner", "<gray>Sieg: <white><names></white></gray>",
                "<gray>Winner: <white><names></white></gray>");

        // ---------- Zuschauen ----------
        t("spectate.title", "Laufende Partien",
                "Running games");
        t("spectate.none", "<gray>Gerade läuft keine Partie.</gray>",
                "<gray>No game is running right now.</gray>");
        t("spectate.info", "<gray><names> · Runde <round></gray>",
                "<gray><names> · round <round></gray>");
        t("spectate.start", "<gray>Du schaust zu. Mit <white>/verlassen</white> geht's zurück auf den Festplatz.</gray>",
                "<gray>You are watching. Use <white>/verlassen</white> (leave) to go back to the fairground.</gray>");
        t("spectate.notplaying", "<gray><white><name></white> spielt gerade nicht.</gray>",
                "<gray><white><name></white> is not playing right now.</gray>");

        // ---------- Statistik ----------
        t("stats.title", "Statistik",
                "Stats");
        t("stats.mine", "<au>Deine Werte</au>",
                "<au>Your stats</au>");
        t("stats.header", "<au>━━ Statistik von <white><name></white> ━━</au>",
                "<au>━━ Stats of <white><name></white> ━━</au>");
        t("stats.lines", "<gray>Partien: <white><games></white> · Siege: <white><wins></white></gray>\n<gray>Edelweiß gesamt: <ed><edelweiss></ed> · Bestwert: <ed><best></ed></gray>\n<gray>Minispiele gewonnen: <white><minis></white> · Gipfel: <white><summits></white></gray>",
                "<gray>Games: <white><games></white> · Wins: <white><wins></white></gray>\n<gray>Edelweiss total: <ed><edelweiss></ed> · Best: <ed><best></ed></gray>\n<gray>Minigames won: <white><minis></white> · Summits: <white><summits></white></gray>");
        t("stats.mode", "<gray><mode>: <white><games></white> <games_pl:'0#Partien|1#Partie|1<Partien'/> · <white><wins></white> <wins_pl:'0#Siege|1#Sieg|1<Siege'/></gray>",
                "<gray><mode>: <white><games></white> <games_pl:'0#games|1#game|1<games'/> · <white><wins></white> <wins_pl:'0#wins|1#win|1<wins'/></gray>");
        t("stats.none", "<gray><white><name></white> hat noch keine Partie gespielt.</gray>",
                "<gray><white><name></white> hasn't played a game yet.</gray>");
        t("top.title", "<au>★ Bestenliste (Siege)</au>",
                "<au>★ Leaderboard (wins)</au>");
        t("top.line", "<au><pos>.</au> <white><name></white> <gray><wins> <wins_pl:'0#Siege|1#Sieg|1<Siege'/> · </gray><ed>✿<edelweiss></ed>",
                "<au><pos>.</au> <white><name></white> <gray><wins> <wins_pl:'0#wins|1#win|1<wins'/> · </gray><ed>✿<edelweiss></ed>");
        t("top.empty", "<gray>Noch frei – sei die oder der Erste!</gray>",
                "<gray>Still empty – be the first!</gray>");
        t("sign.welcome", "<ed><b>✿</b></ed> <au><b>Morni Party</b></au> <ed><b>✿</b></ed>\n<gray>Würfeln, Taler sammeln, <ed>Edelweiß</ed> kaufen</gray>\n<gray>und nach jeder Runde ein Minispiel!</gray>\n<gray><white>Farbe</white>: mitspielen · <white>Karte/Uhr</white>: abstimmen</gray>",
                "<ed><b>✿</b></ed> <au><b>Morni Party</b></au> <ed><b>✿</b></ed>\n<gray>Roll the dice, collect coins, buy <ed>Edelweiss</ed></gray>\n<gray>and play a minigame after every round!</gray>\n<gray><white>Dye</white>: play · <white>map/clock</white>: vote</gray>");
        t("sign.top", "<au><b>★ Die Besten ★</b></au>",
                "<au><b>★ The best ★</b></au>");

        // ---------- Verwaltung ----------
        t("admin.usage", "<gray>/mparty info · zustand · start · stopp · geld \\<spieler> \\<betrag> · wuerfel|taler|edelweiss|feld \\<spieler> \\<zahl> · gegenstand \\<spieler> \\<gegenstand> · stand \\<feld> · minispiel \\<name> · neubauen · neuladen</gray>",
                "<gray>/mparty info · zustand (state) · start · stopp (stop) · geld (money) \\<player> \\<amount> · wuerfel|taler|edelweiss|feld \\<player> \\<number> · gegenstand \\<player> \\<item> · stand \\<field> · minispiel \\<name> · neubauen (rebuild) · neuladen (reload)</gray>");
        t("admin.info", "<gray>Partien: <white><games></white> · Wartend: <white><waiting></white> · Bereit: <white><ready></white> · Netzwerk: <white><net></white></gray>",
                "<gray>Games: <white><games></white> · Waiting: <white><waiting></white> · Ready: <white><ready></white> · Network: <white><net></white></gray>");
        t("admin.sides", "<gray>/mparty seiten auto|frei|team|einzel – Seiten fürs nächste Minispiel (nur zu viert).</gray>",
                "<gray>/mparty seiten auto|frei|team|einzel – sides for the next minigame (four players only).</gray>");
        t("admin.ok", "<em>Erledigt.</em>",
                "<em>Done.</em>");
        t("admin.money", "<em>✔</em> <white><name></white> <gray>bekommt</gray> <au><n> Taler</au><gray> – jetzt</gray> <au><total></au><gray>.</gray>",
                "<em>✔</em> <white><name></white> <gray>gets</gray> <au><n> <n_pl:'0#coins|1#coin|1<coins'/></au><gray> – now</gray> <au><total></au><gray>.</gray>");
        t("admin.money.usage", "<gray>So geht's: <white>/mparty geld \\<spieler> \\<betrag></white> – gibt Taler in der laufenden Partie (zum Testen).</gray>",
                "<gray>Usage: <white>/mparty geld \\<player> \\<amount></white> – gives coins in the running game (for testing).</gray>");
        t("money.got", "<gray>Vom Team zum Testen:</gray> <au>+<n> Taler</au><gray>.</gray>",
                "<gray>From the team for testing:</gray> <au>+<n> <n_pl:'0#coins|1#coin|1<coins'/></au><gray>.</gray>");
        t("admin.started", "<em>Partie gestartet.</em>",
                "<em>Game started.</em>");
        t("admin.notstarted", "<gray>Keine Partie gestartet (zu wenige oder kein Tisch frei).</gray>",
                "<gray>No game started (too few players or no free table).</gray>");
        t("admin.stopped", "<gray>Alle Partien beendet.</gray>",
                "<gray>All games ended.</gray>");
        t("admin.nogame", "<gray>Keine laufende Partie oder falsche Zahl.</gray>",
                "<gray>No running game or wrong number.</gray>");
        t("admin.noplayer", "<gray><white><name></white> spielt in keiner Partie.</gray>",
                "<gray><white><name></white> is not in a game.</gray>");
        t("admin.noitem", "<gray>Unbekannter Gegenstand oder Rucksack voll.</gray>",
                "<gray>Unknown item or backpack full.</gray>");
        t("admin.minis", "<gray>Minispiele: <white><list></white></gray>",
                "<gray>Minigames: <white><list></white></gray>");
        t("admin.rebuild", "<gray>Alles wird neu gebaut …</gray>",
                "<gray>Rebuilding everything …</gray>");
        t("admin.rebuilt", "<em>Fertig gebaut.</em>",
                "<em>Rebuilt.</em>");
        t("admin.reloaded", "<gray>Einstellungen neu geladen (gilt für neue Partien).</gray>",
                "<gray>Settings reloaded (for new games).</gray>");

    }

    /**
     * Eine Zahl als Platzhalter {@code <key>} und dazu {@code <key_pl:'…'/>}, das im Text das passende Wort wählt
     * (Java-{@code ChoiceFormat}: {@code 0#Punkte|1#Punkt|1<Punkte}) – „1 Punkt“, „2 Punkte“, in jeder Sprache richtig.
     */
    public static TagResolver count(String key, long n) {
        return TagResolver.resolver(Placeholder.unparsed(key, String.valueOf(n)), Formatter.choice(key + "_pl", n));
    }

    /** Alle Schlüssel (für Tests). */
    public static Set<String> keys() {
        return Collections.unmodifiableSet(TEXTS.keySet());
    }

    /** Roher MiniMessage-Text. Unbekannte Sprache → Deutsch. */
    public static String raw(String lang, String key) {
        String[] t = TEXTS.get(key);
        if (t == null) {
            return key;
        }
        return "en".equals(lang) ? t[1] : t[0];
    }

    /** Text ohne Vorsilbe; für Gegenstände ohne kursiv. */
    public static Component text(String lang, String key, TagResolver... resolvers) {
        return MM.deserialize(raw(lang, key), resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /** Nachricht für den Chat, mit „✿ Party »“ davor. */
    public static Component msg(String lang, String key, TagResolver... resolvers) {
        return MM.deserialize(raw(lang, "prefix")).append(MM.deserialize(raw(lang, key), resolvers));
    }

    /** Mehrzeiliger Text als einzelne Zeilen (für Beschreibungen von Gegenständen). */
    public static java.util.List<Component> lines(String lang, String key, TagResolver... resolvers) {
        java.util.List<Component> out = new java.util.ArrayList<>();
        for (String line : raw(lang, key).split("\n")) {
            out.add(MM.deserialize(line, resolvers).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
        return out;
    }
}
