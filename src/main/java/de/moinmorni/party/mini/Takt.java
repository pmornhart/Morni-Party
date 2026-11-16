package de.moinmorni.party.mini;

import java.util.List;

/**
 * Regeln des Schuhplattlers, ohne Minecraft: Ansagen im Takt und die Bewertung, ob jemand die richtige Bewegung zur
 * rechten Zeit gemacht hat.
 */
final class Takt {

    /** Was angesagt wird. */
    enum Move { JUMP, DUCK, CLAP, STILL }

    /** Ticks pro Schlag (120 Schläge pro Minute) und Schläge pro Takt (Dreivierteltakt wie beim Ländler). */
    static final int BEAT = 10;
    static final int BAR = 3 * BEAT;
    /** Ansage einen Takt vorher, die Bewegung auf die Eins des nächsten Takts; danach ein Takt Pause. */
    static final int LEAD = BAR;
    static final int CYCLE = 2 * BAR;
    /** So weit daneben zählt noch („gut“), so nah ist „perfekt“. */
    static final int WINDOW = 6;
    static final int PERFECT = 2;
    /** Nicht stillgestanden, wenn man sich weiter als das bewegt hat. */
    static final double STILL_DISTANCE = 0.4;

    private Takt() {
    }

    /** Welche Bewegung zur n-ten Ansage gehört (Würfel 0–99); nie zweimal „still“ hintereinander. */
    static Move pick(int roll, Move before) {
        Move m = roll < 30 ? Move.JUMP : roll < 60 ? Move.DUCK : roll < 85 ? Move.CLAP : Move.STILL;
        return m == Move.STILL && before == Move.STILL ? Move.CLAP : m;
    }

    /** Was jemand im Fenster getan hat: Bewegung und Abstand zur Eins in Ticks. */
    record Done(Move move, int offset) {
    }

    /**
     * Punkte für eine Ansage: richtige Bewegung im Fenster = 1, ganz genau = 2; „still“ gibt 1, wenn man nichts getan
     * und sich kaum bewegt hat. Eine falsche Bewegung im Fenster macht alles zunichte.
     */
    static int judge(Move wanted, List<Done> done, double moved) {
        boolean wrong = done.stream().anyMatch(d -> d.move() != wanted && Math.abs(d.offset()) <= WINDOW);
        if (wrong) {
            return 0;
        }
        if (wanted == Move.STILL) {
            return done.isEmpty() && moved < STILL_DISTANCE ? 1 : 0;
        }
        int best = done.stream().filter(d -> d.move() == wanted).mapToInt(d -> Math.abs(d.offset())).min()
                .orElse(Integer.MAX_VALUE);
        return best <= PERFECT ? 2 : best <= WINDOW ? 1 : 0;
    }
}
