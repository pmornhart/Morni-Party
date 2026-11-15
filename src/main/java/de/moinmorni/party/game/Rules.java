package de.moinmorni.party.game;

/**
 * Zahlen der Spielregeln (aus der config.yml, mit Vorgaben).
 *
 * @param startTaler      Taler zu Beginn
 * @param fieldPlus       blaues Feld
 * @param fieldMinus      rotes Feld
 * @param edelweissPrice  Preis eines Edelweiß am Stand
 * @param duelStake       so viele Taler gehen im Würfelduell an den Sieger
 * @param steal           so viele Taler nimmt der Talerklau
 * @param luckTaler       Taler bei Geschenk und Steinschlag
 * @param luckSteps       Felder bei Rückenwind und Nebel
 * @param shareTaler      Taler je Person bei Sammeln und Spendieren
 * @param summitBonus     Edelweiß-Jagd: Taler, wenn man über das Ziel hinauskommt (dann geht's mit der Seilbahn zurück)
 * @param maxItems        so viele Gegenstände passen in den Rucksack
 * @param raceMaxRounds   Gipfelrennen: spätestens nach so vielen Runden ist Schluss
 * @param awards          Taler für Platz 1, 2, 3 … in den Minispielen
 */
public record Rules(int startTaler, int fieldPlus, int fieldMinus, int edelweissPrice, int duelStake, int steal,
        int luckTaler, int luckSteps, int shareTaler, int summitBonus, int maxItems, int raceMaxRounds, int[] awards) {

    public static Rules defaults() {
        return new Rules(10, 3, 3, 20, 5, 7, 5, 3, 2, 5, 3, 20, new int[] {10, 6, 4, 3, 2, 1, 1, 1});
    }

    /** Taler für einen Platz (0 = erster). */
    public int award(int place) {
        if (awards.length == 0) {
            return 0;
        }
        return awards[Math.min(place, awards.length - 1)];
    }
}
