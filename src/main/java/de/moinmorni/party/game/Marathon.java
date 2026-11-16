package de.moinmorni.party.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Regeln des Minispiel-Marathons, ohne Minecraft: Platz-Punkte nach jedem Minispiel und die Reihenfolge der
 * Minispiele (gemischt, keine Wiederholung, bis alle dran waren; zu kleine Plätze für viele Leute fallen aus).
 */
public final class Marathon {

    /** Punkte für Platz 1, 2, 3 … (alle weiteren bekommen den letzten Wert – Mitmachen zählt). */
    public static final int[] DEFAULT_POINTS = {10, 7, 5, 4, 3, 2, 1};
    /** So viele Leute höchstens in einem Marathon. */
    public static final int MAX_PLAYERS = 16;

    private Marathon() {
    }

    /** Punkte für einen Platz (0 = erster). */
    public static int points(int[] table, int place) {
        if (table.length == 0) {
            return 0;
        }
        return table[Math.max(0, Math.min(place, table.length - 1))];
    }

    /**
     * Die nächsten Minispiele aus einem Vorrat ziehen: gemischt, ohne Wiederholung, bis alle dran waren; dann wird neu
     * gemischt (das zuletzt gespielte kommt nicht gleich wieder). {@code fits} sagt, ob ein Minispiel für die
     * Spielerzahl geht – die anderen werden übersprungen.
     *
     * @param <T>  Minispiel
     * @param bag  Vorrat (wird verändert)
     * @param all  alle erlaubten Minispiele
     * @param last zuletzt gespielt (oder null)
     * @return das nächste, oder null, wenn keines passt
     */
    public static <T> T draw(List<T> bag, List<T> all, T last, Predicate<T> fits, Random rng) {
        for (int pass = 0; pass < 2; pass++) {
            while (!bag.isEmpty()) {
                T t = bag.removeFirst();
                if (fits.test(t)) {
                    return t;
                }
            }
            List<T> fresh = new ArrayList<>(all);
            Collections.shuffle(fresh, rng);
            if (fresh.size() > 1 && last != null && fresh.getFirst().equals(last)) {
                fresh.add(fresh.removeFirst());
            }
            bag.addAll(fresh);
        }
        return null;
    }
}
