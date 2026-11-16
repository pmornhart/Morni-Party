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
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Alle Texte für Spieler, auf Deutsch und Englisch, als MiniMessage. Eigene Farben: {@code <em>} Smaragd,
 * {@code <au>} Gold, {@code <bl>} Weiß-Blau, {@code <ed>} Edelweiß-Weiß. Namen von Spielern kommen immer als {@code Placeholder.unparsed} hinein.
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
        t("goal.almsommer", "das Gipfelkreuz",
                "the summit cross");
        t("goal.schloss", "die Turmplattform",
                "the tower platform");
        t("goal.goldstollen", "die Goldkammer",
                "the gold chamber");

        // ---------- Spielarten ----------
        t("mode.gipfelrennen", "<au>Gipfelrennen</au>",
                "<au>Summit Race</au>");
        t("mode.gipfelrennen.line", "<au>Gipfelrennen</au> <gray>– wer zuerst oben ist, gewinnt</gray>",
                "<au>Summit Race</au> <gray>– first one at the top wins</gray>");
        t("mode.gipfelrennen.desc", "<gray>Wer zuerst das Ziel erreicht, gewinnt.</gray>\n<gray>Edelweiß zählt nur bei Gleichstand.</gray>",
                "<gray>The first to reach the goal wins.</gray>\n<gray>Edelweiss only counts on a tie.</gray>");
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
        t("game.goal.edelweiss-jagd", "<gray>Sammle Taler und kauf am wandernden Stand <ed>Edelweiß</ed> für <au><price> Taler</au>. Nach <white><n></white> Runden gewinnt, wer die meisten hat.</gray>",
                "<gray>Collect coins and buy <ed>Edelweiss</ed> at the moving stand for <au><price> coins</au>. After <white><n></white> rounds, whoever has the most wins.</gray>");
        t("game.order", "<gray>Reihenfolge: <white><names></white></gray>",
                "<gray>Turn order: <white><names></white></gray>");
        t("game.left", "<gray><white><name></white> hat die Partie verlassen.</gray>",
                "<gray><white><name></white> left the game.</gray>");
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
        t("roll.sub", "<gray><n> Felder weit</gray>",
                "<gray><n> fields</gray>");
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
        t("field.duell.sign", "<gold><b>Duellfeld</b></gold>\n<gray>Würfelduell um Taler</gray>",
                "<gold><b>Duel field</b></gold>\n<gray>Dice duel for coins</gray>");
        t("field.laden", "<yellow>Krämerladen</yellow>",
                "<yellow>Shop</yellow>");
        t("field.laden.sign", "<yellow><b>Krämerladen</b></yellow>\n<gray>Gegenstände kaufen</gray>",
                "<yellow><b>Shop</b></yellow>\n<gray>Buy items</gray>");
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
        t("luck.rueckenwind", "<em>Glück!</em> <gray>Rückenwind – <white><name></white> zieht <white><steps></white> Felder weiter.</gray>",
                "<em>Lucky!</em> <gray>Tailwind – <white><name></white> moves <white><steps></white> fields ahead.</gray>");
        t("luck.sammeln", "<em>Glück!</em> <gray>Alle anderen geben <white><name></white> etwas ab – zusammen</gray> <au><n> Taler</au><gray>.</gray>",
                "<em>Lucky!</em> <gray>Everyone else chips in for <white><name></white> –</gray> <au><n> coins</au> <gray>in total.</gray>");
        t("luck.fund", "<em>Glück!</em> <gray><white><name></white> findet etwas:</gray> <item>",
                "<em>Lucky!</em> <gray><white><name></white> finds something:</gray> <item>");
        t("luck.steinschlag", "<red>Pech!</red> <gray>Steinschlag – <white><name></white> verliert</gray> <red><n> Taler</red><gray>.</gray>",
                "<red>Bad luck!</red> <gray>Falling rocks – <white><name></white> loses</gray> <red><n> coins</red><gray>.</gray>");
        t("luck.nebel", "<red>Pech!</red> <gray>Dichter Nebel – <white><name></white> muss <white><steps></white> Felder zurück.</gray>",
                "<red>Bad luck!</red> <gray>Thick fog – <white><name></white> has to go back <white><steps></white> fields.</gray>");
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
        t("duel.title", "Gegen wen würfelst du?",
                "Who do you duel?");
        t("duel.pick", "<gold>⚔ <name></gold>", "<gold>⚔ <name></gold>");
        t("duel.pick.lore", "<gray>Hat <au><taler> Taler</au>.</gray>",
                "<gray>Has <au><taler> coins</au>.</gray>");
        t("duel.field", "<white><name></white> <gray>steht auf einem <gold>Duellfeld</gold> – Würfelduell um <au><n> Taler</au>!</gray>",
                "<white><name></white> <gray>is on a <gold>duel field</gold> – dice duel for <au><n> coins</au>!</gray>");
        t("duel.vs", "<gold>⚔</gold> <white><one></white> <gray>gegen</gray> <white><two></white> <gray>– beide würfeln …</gray>",
                "<gold>⚔</gold> <white><one></white> <gray>vs</gray> <white><two></white> <gray>– both roll …</gray>");
        t("duel.won", "<white><winner></white> <gray>gewinnt das Duell und bekommt</gray> <au><n> Taler</au> <gray>von <white><loser></white>.</gray>",
                "<white><winner></white> <gray>wins the duel and gets</gray> <au><n> coins</au> <gray>from <white><loser></white>.</gray>");
        t("duel.blocked", "<white><winner></white> <gray>gewinnt das Duell – aber der <bl>Schild</bl> von <white><loser></white> schützt die Taler (<n>).</gray>",
                "<white><winner></white> <gray>wins the duel – but <white><loser></white>'s <bl>shield</bl> protects the coins (<n>).</gray>");
        t("duel.draw", "<gray>Gleich hoch – unentschieden, niemand zahlt.</gray>",
                "<gray>Same number – a draw, nobody pays.</gray>");
        t("duel.alone", "<gray>Niemand da zum Duellieren.</gray>",
                "<gray>Nobody left to duel.</gray>");

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
        t("mini.points", "<au>Punkte: <n></au>",
                "<au>Points: <n></au>");
        t("mini.results", "<au>━━ Ergebnis: <name> ━━</au>",
                "<au>━━ Result: <name> ━━</au>");
        t("mini.line", "<au><pos>.</au> <white><name></white> <gray>→</gray> <au>+<taler> Taler</au>",
                "<au><pos>.</au> <white><name></white> <gray>→</gray> <au>+<taler> coins</au>");
        t("mini.line.points", "<au><pos>.</au> <white><name></white> <gray>(<points> Punkte) →</gray> <au>+<taler> Taler</au>",
                "<au><pos>.</au> <white><name></white> <gray>(<points> points) →</gray> <au>+<taler> coins</au>");
        t("mini.line.finish", "<au><pos>.</au> <white><name></white> <gray>(im Ziel nach <time> s) →</gray> <au>+<taler> Taler</au>",
                "<au><pos>.</au> <white><name></white> <gray>(finished in <time> s) →</gray> <au>+<taler> coins</au>");
        t("mini.line.way", "<au><pos>.</au> <white><name></white> <gray>(<points> Blöcke weit) →</gray> <au>+<taler> Taler</au>",
                "<au><pos>.</au> <white><name></white> <gray>(<points> blocks far) →</gray> <au>+<taler> coins</au>");
        t("mini.line.top", "<au><pos>.</au> <white><name></white> <gray>(oben nach <time> s) →</gray> <au>+<taler> Taler</au>",
                "<au><pos>.</au> <white><name></white> <gray>(at the top in <time> s) →</gray> <au>+<taler> coins</au>");
        t("mini.line.height", "<au><pos>.</au> <white><name></white> <gray>(<points> Blöcke hoch) →</gray> <au>+<taler> Taler</au>",
                "<au><pos>.</au> <white><name></white> <gray>(<points> blocks high) →</gray> <au>+<taler> coins</au>");
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
        t("mini.eisstockschiessen.lead", "<gray>Nach Wurf <n> liegt <white><name></white> vorn: <au><points> Punkte</au>.</gray>",
                "<gray>After throw <n>, <white><name></white> leads: <au><points> points</au>.</gray>");
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
        t("stats.none", "<gray><white><name></white> hat noch keine Partie gespielt.</gray>",
                "<gray><white><name></white> hasn't played a game yet.</gray>");
        t("top.title", "<au>★ Bestenliste (Siege)</au>",
                "<au>★ Leaderboard (wins)</au>");
        t("top.line", "<au><pos>.</au> <white><name></white> <gray><wins> Siege · </gray><ed>✿<edelweiss></ed>",
                "<au><pos>.</au> <white><name></white> <gray><wins> wins · </gray><ed>✿<edelweiss></ed>");
        t("top.empty", "<gray>Noch frei – sei die oder der Erste!</gray>",
                "<gray>Still empty – be the first!</gray>");
        t("sign.welcome", "<ed><b>✿</b></ed> <au><b>Morni Party</b></au> <ed><b>✿</b></ed>\n<gray>Würfeln, Taler sammeln, <ed>Edelweiß</ed> kaufen</gray>\n<gray>und nach jeder Runde ein Minispiel!</gray>\n<gray><white>Farbe</white>: mitspielen · <white>Karte/Uhr</white>: abstimmen</gray>",
                "<ed><b>✿</b></ed> <au><b>Morni Party</b></au> <ed><b>✿</b></ed>\n<gray>Roll the dice, collect coins, buy <ed>Edelweiss</ed></gray>\n<gray>and play a minigame after every round!</gray>\n<gray><white>Dye</white>: play · <white>map/clock</white>: vote</gray>");
        t("sign.top", "<au><b>★ Die Besten ★</b></au>",
                "<au><b>★ The best ★</b></au>");

        // ---------- Verwaltung ----------
        t("admin.usage", "<gray>/mparty info · zustand · start · stopp · wuerfel|taler|edelweiss|feld \\<spieler> \\<zahl> · gegenstand \\<spieler> \\<gegenstand> · stand \\<feld> · minispiel \\<name> · neubauen · neuladen</gray>",
                "<gray>/mparty info · zustand (state) · start · stopp (stop) · wuerfel|taler|edelweiss|feld \\<player> \\<number> · gegenstand \\<player> \\<item> · stand \\<field> · minispiel \\<name> · neubauen (rebuild) · neuladen (reload)</gray>");
        t("admin.info", "<gray>Partien: <white><games></white> · Wartend: <white><waiting></white> · Bereit: <white><ready></white> · Netzwerk: <white><net></white></gray>",
                "<gray>Games: <white><games></white> · Waiting: <white><waiting></white> · Ready: <white><ready></white> · Network: <white><net></white></gray>");
        t("admin.ok", "<em>Erledigt.</em>",
                "<em>Done.</em>");
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
