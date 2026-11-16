package de.moinmorni.party.game;

import de.moinmorni.party.board.FieldType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Wer spielt im Minispiel mit wem? Ohne Minecraft. Nach jeder Brett-Runde zählt die Farbe des Feldes, auf dem jemand
 * zuletzt stand: blau (gute Felder) oder rot (Verlust, Pech, Duell). Nur bei genau vier Leuten wird gemischt:
 * <ul>
 * <li>alle gleich (4 : 0) – jeder gegen jeden;</li>
 * <li>2 : 2 – Blau gegen Rot;</li>
 * <li>3 : 1 – der Einzelne gegen die drei anderen (mit Vorteil im Spiel).</li>
 * </ul>
 * Ein Duell (Duellfeld) ist immer eins gegen eins.
 */
public final class Teams {

    /** Farbe eines Feldes. */
    public enum Colour {
        BLUE, RED
    }

    /** Wie gespielt wird. */
    public enum Format {
        /** Jeder gegen jeden. */
        FREE,
        /** Zwei gegen zwei. */
        TEAM,
        /** Einer gegen drei. */
        SOLO,
        /** Eins gegen eins (Duellfeld). */
        DUEL;

        public String key() {
            return "format." + name().toLowerCase(Locale.ROOT);
        }
    }

    /** Ergebnis zwischen zwei Seiten. */
    public enum Outcome {
        A, B, DRAW
    }

    /**
     * Aufteilung. Bei {@link Format#FREE} sind beide Seiten leer. Bei {@link Format#SOLO} steht der Einzelne in
     * {@code a}, bei {@link Format#TEAM} Blau in {@code a} und Rot in {@code b}.
     */
    public record Split(Format format, List<UUID> a, List<UUID> b) {

        public static final Split FREE = new Split(Format.FREE, List.of(), List.of());

        public Split {
            a = List.copyOf(a);
            b = List.copyOf(b);
        }

        public static Split duel(UUID one, UUID two) {
            return new Split(Format.DUEL, List.of(one), List.of(two));
        }

        /** Seite einer Person: 0 = a, 1 = b, -1 = keine (jeder gegen jeden). */
        public int side(UUID u) {
            return a.contains(u) ? 0 : b.contains(u) ? 1 : -1;
        }

        /** Spielen zwei Leute zusammen (gleiche Seite, nicht dieselbe Person)? */
        public boolean mates(UUID x, UUID y) {
            int s = side(x);
            return s >= 0 && !x.equals(y) && s == side(y);
        }

        public boolean sides() {
            return format != Format.FREE;
        }

        public List<UUID> side(int s) {
            return s == 0 ? a : b;
        }

        /** Wer zur siegreichen Seite gehört. */
        public List<UUID> winners(Outcome o) {
            return o == Outcome.A ? a : o == Outcome.B ? b : List.of();
        }

        public List<UUID> losers(Outcome o) {
            return o == Outcome.A ? b : o == Outcome.B ? a : List.of();
        }
    }

    private Teams() {
    }

    /** Farbe eines Feldes: Verlust, Pech und Duell sind rot, alles andere blau. */
    public static Colour colour(FieldType t) {
        return t == FieldType.VERLUST || t == FieldType.PECH || t == FieldType.DUELL ? Colour.RED : Colour.BLUE;
    }

    /**
     * Aufteilung für das Minispiel nach der Runde. {@code players}: alle Aktiven in Zugreihenfolge,
     * {@code landed}: Feld, auf dem jemand zuletzt stand (fehlt = blau).
     */
    public static Split decide(List<UUID> players, Map<UUID, FieldType> landed) {
        if (players.size() != 4) {
            return Split.FREE;
        }
        List<UUID> blue = new ArrayList<>();
        List<UUID> red = new ArrayList<>();
        for (UUID u : players) {
            FieldType f = landed.get(u);
            (f != null && colour(f) == Colour.RED ? red : blue).add(u);
        }
        if (blue.size() == 2) {
            return new Split(Format.TEAM, blue, red);
        }
        if (blue.size() == 1) {
            return new Split(Format.SOLO, blue, red);
        }
        if (red.size() == 1) {
            return new Split(Format.SOLO, red, blue);
        }
        return Split.FREE;
    }
}
