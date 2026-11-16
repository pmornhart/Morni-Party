package net.morni.party.game;

import java.util.Locale;

/** Die Spielarten. */
public enum Mode {
    /** Wer zuerst am Ziel ist, gewinnt. */
    GIPFELRENNEN,
    /** Wer nach der letzten Runde die meisten Edelweiß hat, gewinnt. */
    EDELWEISS_JAGD,
    /** Ohne Spielbrett: nur Minispiele, Platz-Punkte nach jedem, bis 16 Leute. */
    MARATHON,
    /** Edelweiß-Jagd in Seilschaften zu zweit: Taler und Edelweiß gehören beiden, Minispiele 2 gegen 2. */
    SEILSCHAFT,
    /** Kurze Edelweiß-Jagd (5 Runden) mit mehr Start-Talern und günstigerem Edelweiß. */
    SCHNELL,
    /** Zwei gegeneinander auf dem Brett: 8 Runden, jedes Minispiel ist ein Duell. */
    BERGDUELL,
    /** Ohne Spielbrett: jeder hat drei Leben, wer im Minispiel in der unteren Hälfte landet, verliert eins. */
    HUETTE,
    /** Ohne Spielbrett: K.-o.-Turnier aus Duell-Minispielen, bis 16 Leute. */
    TURNIER,
    /** Ohne Spielbrett, alle zusammen: der Tatzelwurm in drei Abschnitten, gemeinsame Wertung. */
    TATZELWURM;

    /** Wird auf einem Spielbrett gespielt? */
    public boolean board() {
        return this != MARATHON && this != HUETTE && this != TURNIER && this != TATZELWURM;
    }

    /** Hat eine wählbare Rundenzahl (Edelweiß-Jagd, Marathon)? */
    public boolean counted() {
        return this != GIPFELRENNEN && this != HUETTE && this != TURNIER && this != TATZELWURM;
    }

    /** So viele müssen mindestens mitspielen (Seilschaft: zwei Seilschaften). */
    public int minPlayers() {
        return this == SEILSCHAFT ? 2 * Seilschaft.SIZE : 2;
    }

    /** Höchstens so viele (0 = wie in der config.yml). */
    public int fixedMax() {
        return this == BERGDUELL ? 2 : 0;
    }

    /** Geht die Spielart mit {@code n} Leuten (Seilschaft: nur gerade Zahl, Bergduell: genau zwei)? */
    public boolean fits(int n) {
        return n >= minPlayers() && (this != SEILSCHAFT || n % Seilschaft.SIZE == 0) && (fixedMax() == 0 || n <= fixedMax());
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT).replace("_", "-");
    }

    public static Mode parse(String s) {
        if (s == null) {
            return null;
        }
        String l = s.toLowerCase(Locale.ROOT).replace("_", "-").replace("ß", "ss");
        for (Mode m : values()) {
            if (m.id().equals(l) || (l.length() >= 3 && m.id().startsWith(l))) {
                return m;
            }
        }
        return null;
    }
}
