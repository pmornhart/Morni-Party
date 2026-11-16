package de.moinmorni.party;

import de.moinmorni.party.game.Rules;
import de.moinmorni.party.mini.MiniType;
import java.util.ArrayList;
import java.util.List;
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
 */
public record Settings(String world, int tables, int minPlayers, int maxPlayers, int countdown, int fullCountdown,
        boolean autoJoin, List<Integer> huntRounds, int turnSeconds, int menuSeconds, int stepTicks,
        List<MiniType> minigames, boolean fixedOrder, double miniFactor, Rules rules, List<Integer> marathonRounds,
        int marathonMax) {

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
                Math.max(1, Math.min(5, c.getInt("regeln.einzel-faktor", d.soloFactor()))));
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
                Math.max(2, Math.min(de.moinmorni.party.game.Marathon.MAX_PLAYERS,
                        c.getInt("minispiel-marathon.hoechstens", de.moinmorni.party.game.Marathon.MAX_PLAYERS))));
    }
}
