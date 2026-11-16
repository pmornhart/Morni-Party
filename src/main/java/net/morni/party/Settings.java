package net.morni.party;

import java.util.ArrayList;
import java.util.List;
import net.morni.party.game.Rules;
import net.morni.party.mini.MiniType;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Einstellungen aus der config.yml (mit Erklärungen in der Datei).
 *
 * @param world          eigene leere Welt
 * @param tables         so viele Partien gleichzeitig (jeder Tisch hat alle Bretter und Minispiel-Plätze)
 * @param minPlayers     ab so vielen geht es los
 * @param maxPlayers     höchstens so viele pro Partie (8)
 * @param countdown      Wartezeit, sobald genug da sind
 * @param fullCountdown  Wartezeit, wenn die Partie voll ist
 * @param autoJoin       wer ankommt, wartet gleich mit
 * @param huntRounds     Rundenzahlen der Edelweiß-Jagd zur Wahl
 * @param turnSeconds    so lange hat man zum Würfeln, dann würfelt das Spiel
 * @param menuSeconds    so lange bleiben Laden, Edelweiß-Stand und Duell-Wahl offen
 * @param stepTicks      Tempo der Figur (Ticks pro Feld)
 * @param minigames      erlaubte Minispiele (leer = alle)
 * @param fixedOrder     Minispiele der Reihe nach statt zufällig
 * @param miniFactor     Dauer der Minispiele × Faktor
 * @param rules          Zahlen der Spielregeln
 * @param marathonRounds Minispiel-Marathon: Zahl der Minispiele zur Wahl
 * @param marathonMax    Minispiel-Marathon: höchstens so viele Leute (bis 16)
 * @param seilschaftRounds Seilschaft: Rundenzahlen zur Wahl
 * @param quick          Schnelle Partie: Runden, Start-Taler, Edelweiß-Preis, Minispiel-Dauer
 * @param duelRounds     Bergduell: Runden
 * @param hutMax         Hüttenaufstieg: spätestens nach so vielen Minispielen ist Schluss
 * @param awards         Auszeichnungen am Ende einer Brett-Partie (leer = aus)
 */
public record Settings(String world, int tables, int minPlayers, int maxPlayers, int countdown, int fullCountdown,
        boolean autoJoin, List<Integer> huntRounds, int turnSeconds, int menuSeconds, int stepTicks,
        List<MiniType> minigames, boolean fixedOrder, double miniFactor, Rules rules, List<Integer> marathonRounds,
        int marathonMax, List<Integer> seilschaftRounds, Quick quick, int duelRounds, int hutMax, List<net.morni.party.game.Awards.Type> awards) {

    /** Schnelle Partie: kurz und mit mehr Geld. */
    public record Quick(int rounds, int startTaler, int edelweissPrice, double miniFactor) {
    }

    /** Dieselben Einstellungen für eine andere Welt/Tischzahl (Neuladen ändert Welt und Tische nicht). */
    public Settings withPlace(String world, int tables) {
        return new Settings(world, tables, minPlayers, maxPlayers, countdown, fullCountdown, autoJoin, huntRounds,
                turnSeconds, menuSeconds, stepTicks, minigames, fixedOrder, miniFactor, rules, marathonRounds, marathonMax,
                seilschaftRounds, quick, duelRounds, hutMax, awards);
    }

    /** Liste positiver Zahlen aus der config.yml (leer oder kaputt = Vorgabe). */
    static List<Integer> numbers(FileConfiguration c, String path, List<Integer> fallback) {
        List<Integer> out = new ArrayList<>();
        for (Object o : c.getList(path, fallback)) {
            if (o instanceof Number n && n.intValue() > 0 && !out.contains(n.intValue())) {
                out.add(n.intValue());
            }
        }
        return out.isEmpty() ? List.copyOf(fallback) : List.copyOf(out);
    }

    public static Settings load(FileConfiguration c) {
        List<Integer> rounds = new ArrayList<>();
        for (Object o : c.getList("edelweiss-jagd-runden", List.of(10, 15, 20))) {
            if (o instanceof Number n && n.intValue() > 0) {
                rounds.add(n.intValue());
            }
        }
        if (rounds.isEmpty()) {
            rounds.addAll(List.of(10, 15, 20));
        }
        List<Integer> marathon = new ArrayList<>();
        for (Object o : c.getList("minispiel-marathon.runden", List.of(6, 10, 15))) {
            if (o instanceof Number n && n.intValue() > 0) {
                marathon.add(n.intValue());
            }
        }
        if (marathon.isEmpty()) {
            marathon.addAll(List.of(6, 10, 15));
        }
        List<MiniType> minis = new ArrayList<>();
        for (String s : c.getStringList("minispiele.auswahl")) {
            MiniType t = MiniType.parse(s);
            if (t != null && !minis.contains(t)) {
                minis.add(t);
            }
        }
        if (minis.isEmpty()) {
            minis.addAll(List.of(MiniType.values()));
        }
        Rules d = Rules.defaults();
        List<Integer> awards = c.getIntegerList("regeln.minispiel-taler");
        int[] aw = awards.isEmpty() ? d.awards() : awards.stream().mapToInt(Integer::intValue).toArray();
        List<Integer> points = c.getIntegerList("minispiel-marathon.punkte");
        int[] mp = points.isEmpty() ? d.marathonPoints() : points.stream().mapToInt(Integer::intValue).toArray();
        Rules rules = new Rules(
                Math.max(0, c.getInt("regeln.start-taler", d.startTaler())),
                Math.max(0, c.getInt("regeln.blaues-feld", d.fieldPlus())),
                Math.max(0, c.getInt("regeln.rotes-feld", d.fieldMinus())),
                Math.max(1, c.getInt("regeln.edelweiss-preis", d.edelweissPrice())),
                Math.max(0, c.getInt("regeln.duell-taler", d.duelStake())),
                Math.max(0, c.getInt("regeln.klau-taler", d.steal())),
                Math.max(0, c.getInt("regeln.glueck-taler", d.luckTaler())),
                Math.max(1, c.getInt("regeln.glueck-felder", d.luckSteps())),
                Math.max(0, c.getInt("regeln.teilen-taler", d.shareTaler())),
                Math.max(0, c.getInt("regeln.gipfel-bonus", d.summitBonus())),
                Math.max(1, c.getInt("regeln.rucksack", d.maxItems())),
                Math.max(1, c.getInt("regeln.gipfelrennen-max-runden", d.raceMaxRounds())),
                aw, mp,
                Math.max(0, c.getInt("regeln.team-taler", d.teamWin())),
                Math.max(0, c.getInt("regeln.team-unentschieden-taler", d.teamDraw())),
                Math.max(0, c.getInt("regeln.einzel-taler", d.soloWin())),
                Math.max(1, Math.min(5, c.getInt("regeln.einzel-faktor", d.soloFactor()))),
                Math.max(1, Math.min(9, c.getInt("huettenaufstieg.leben", d.lives()))));
        int min = Math.max(2, c.getInt("spieler.mindestens", 2));
        return new Settings(
                c.getString("welt", "party"),
                Math.max(1, Math.min(8, c.getInt("tische", 2))),
                min,
                Math.max(min, Math.min(8, c.getInt("spieler.hoechstens", 8))),
                Math.max(3, c.getInt("warten.countdown-sekunden", 45)),
                Math.max(3, c.getInt("warten.voll-sekunden", 10)),
                c.getBoolean("warten.automatisch-mitspielen", true),
                List.copyOf(rounds),
                Math.max(3, c.getInt("zug.wuerfel-sekunden", 20)),
                Math.max(3, c.getInt("zug.menue-sekunden", 15)),
                Math.max(2, c.getInt("zug.schritt-ticks", 6)),
                List.copyOf(minis),
                "fest".equalsIgnoreCase(c.getString("minispiele.reihenfolge", "zufall")),
                Math.max(0.2, Math.min(3.0, c.getDouble("minispiele.dauer-faktor", 1.0))),
                rules,
                List.copyOf(marathon),
                Math.max(2, Math.min(net.morni.party.game.Marathon.MAX_PLAYERS,
                        c.getInt("minispiel-marathon.hoechstens", net.morni.party.game.Marathon.MAX_PLAYERS))),
                numbers(c, "seilschaft.runden", List.of(10, 15)),
                new Quick(Math.max(1, c.getInt("schnelle-partie.runden", 5)),
                        Math.max(0, c.getInt("schnelle-partie.start-taler", 20)),
                        Math.max(1, c.getInt("schnelle-partie.edelweiss-preis", 15)),
                        Math.max(0.2, Math.min(3.0, c.getDouble("schnelle-partie.dauer-faktor", 0.75)))),
                Math.max(1, c.getInt("bergduell.runden", 8)),
                Math.max(2, c.getInt("huettenaufstieg.hoechstens-minispiele", net.morni.party.game.Huette.DEFAULT_MAX)),
                awards(c));
    }

    private static List<net.morni.party.game.Awards.Type> awards(FileConfiguration c) {
        if (!c.getBoolean("auszeichnungen.an", true)) {
            return List.of();
        }
        List<net.morni.party.game.Awards.Type> out = new ArrayList<>();
        List<String> wanted = c.isList("auszeichnungen.welche") ? c.getStringList("auszeichnungen.welche") : List.of();
        if (wanted.isEmpty()) {
            return List.of(net.morni.party.game.Awards.Type.values());
        }
        for (String w : wanted) {
            var t = net.morni.party.game.Awards.Type.parse(w);
            if (t != null && !out.contains(t)) {
                out.add(t);
            }
        }
        return List.copyOf(out);
    }
}
