package net.morni.party.mini;

import java.util.List;
import java.util.Random;

/**
 * Regeln des Musterwebers, ohne Minecraft: ein Muster aus 4 × 4 Feldern in wenigen Farben, das sich jemand merken und
 * nachlegen muss. Mit jeder Runde mehr Farben und weniger Zeit zum Anschauen.
 */
final class Webmuster {

    static final int SIZE = 4;
    static final int CELLS = SIZE * SIZE;
    static final int ROUNDS = 3;
    /** Farben (Wolle) in der Reihenfolge, in der sie dazukommen; „leer“ ist hellgrau. */
    static final List<String> PALETTE = List.of("red", "white", "blue", "yellow");
    static final String BLANK = "light_gray";
    /** Bonus, wenn alles stimmt. */
    static final int PERFECT_BONUS = 4;

    private Webmuster() {
    }

    /** So viele Farben in Runde r (1, 2, 3 …): 2, 3, 4. */
    static int colours(int round) {
        return Math.min(PALETTE.size(), round + 1);
    }

    /** So lange (Ticks) ist das Muster in Runde r zu sehen. */
    static int showTicks(int round) {
        return Math.max(40, 90 - 15 * (round - 1));
    }

    /** Neues Muster (Farbnummern), jede der Farben kommt mindestens zweimal vor. */
    static int[] make(Random rng, int colours) {
        int[] cells = new int[CELLS];
        for (int tries = 0; tries < 100; tries++) {
            int[] count = new int[colours];
            for (int i = 0; i < CELLS; i++) {
                cells[i] = rng.nextInt(colours);
                count[cells[i]]++;
            }
            boolean ok = true;
            for (int c : count) {
                ok &= c >= 2;
            }
            if (ok) {
                return cells;
            }
        }
        return cells;
    }

    /** Wie viele Felder stimmen (−1 = leer zählt nie). */
    static int correct(int[] target, int[] attempt) {
        int n = 0;
        for (int i = 0; i < CELLS; i++) {
            if (attempt[i] >= 0 && attempt[i] == target[i]) {
                n++;
            }
        }
        return n;
    }

    /** Punkte einer Runde: je richtiges Feld einer, alles richtig gibt den Bonus dazu. */
    static int points(int[] target, int[] attempt) {
        int c = correct(target, attempt);
        return c == CELLS ? c + PERFECT_BONUS : c;
    }
}
