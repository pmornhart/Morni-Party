package net.morni.party.mini;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Das Seil beim Tauziehen, ohne Minecraft. Jeder Zug (Klick) zieht das Seil um eine Einheit zur eigenen Seite – geteilt
 * durch die Größe der eigenen Seite, damit zwei gegen einen (wenn jemand gegangen ist) nicht doppelt so stark sind.
 * Je Person zählt höchstens ein Zug alle {@link #GAP} Ticks (schneller klicken bringt nichts). Seite a zieht nach
 * minus, Seite b nach plus. Wer das Seil bis {@link #LIMIT} herübergezogen hat, gewinnt sofort; sonst entscheidet am
 * Ende, auf welcher Seite der Knoten steht.
 */
final class Seil {

    static final double LIMIT = 50;
    static final int GAP = 2;
    /** So nah an der Mitte gilt als Gleichstand. */
    static final double DRAW = 1;

    private final int sizeA;
    private final int sizeB;
    private final Map<UUID, Integer> last = new HashMap<>();
    private double pos;

    Seil(int sizeA, int sizeB) {
        this.sizeA = Math.max(1, sizeA);
        this.sizeB = Math.max(1, sizeB);
    }

    /** Ein Zug von {@code u} auf Seite 0 (a) oder 1 (b) zum Zeitpunkt {@code tick}. true, wenn er gezählt hat. */
    boolean pull(UUID u, int side, int tick) {
        if (side < 0 || won() != 0) {
            return false;
        }
        Integer l = last.get(u);
        if (l != null && tick - l < GAP) {
            return false;
        }
        last.put(u, tick);
        pos += side == 0 ? -1.0 / sizeA : 1.0 / sizeB;
        pos = Math.max(-LIMIT, Math.min(LIMIT, pos));
        return true;
    }

    double pos() {
        return pos;
    }

    /** Anteil von -1 (ganz bei a) bis 1 (ganz bei b). */
    double share() {
        return pos / LIMIT;
    }

    /** -1: a hat es ganz herübergezogen, 1: b, 0: noch nicht. */
    int won() {
        return pos <= -LIMIT ? -1 : pos >= LIMIT ? 1 : 0;
    }

    /** Am Ende: -1 a vorn, 1 b vorn, 0 Gleichstand. */
    int leader() {
        return pos <= -DRAW ? -1 : pos >= DRAW ? 1 : 0;
    }
}
