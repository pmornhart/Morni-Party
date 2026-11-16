package net.morni.party.game;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Hüttenaufstieg, ohne Minecraft: alle starten mit gleich vielen Leben und spielen ein Minispiel nach dem anderen. Wer
 * in der unteren Hälfte landet (bei zwei: der Zweite, bei drei: der Dritte, bei acht: Platz 5–8), verliert ein Leben –
 * ein geteilter Platz zählt nach seinem besten Rang. Wer kein Leben mehr hat, schaut zu. Wer zuletzt übrig ist,
 * erreicht die Hütte und gewinnt.
 */
public final class Huette {

    /** Leben zu Beginn (Vorgabe). */
    public static final int DEFAULT_LIVES = 3;
    /** Spätestens nach so vielen Minispielen ist Schluss (dann zählen die Leben). */
    public static final int DEFAULT_MAX = 25;

    private Huette() {
    }

    /**
     * Wer verliert ein Leben? {@code places}: Plätze des Minispiels (Gruppen teilen sich einen Platz). Sicher ist die
     * obere Hälfte (aufgerundet); eine Gruppe, die dort beginnt, ist ganz sicher. Stehen alle auf demselben Platz,
     * verliert niemand.
     */
    public static List<UUID> losers(List<List<UUID>> places) {
        int n = places.stream().mapToInt(List::size).sum();
        if (places.size() <= 1) {
            return List.of();
        }
        int safe = (n + 1) / 2;
        List<UUID> out = new ArrayList<>();
        int place = 0;
        for (List<UUID> g : places) {
            if (place >= safe) {
                out.addAll(g);
            }
            place += g.size();
        }
        return out;
    }

    /** Herzen als Text: volle ❤ und leere ♡ (z. B. „❤❤♡“). */
    public static String hearts(int lives, int max) {
        return "❤".repeat(Math.max(0, lives)) + "♡".repeat(Math.max(0, max - lives));
    }
}
