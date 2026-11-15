package de.moinmorni.party.game;

import java.util.Locale;

/** Die zwei Spielarten. */
public enum Mode {
    /** Wer zuerst am Ziel ist, gewinnt. */
    GIPFELRENNEN,
    /** Wer nach der letzten Runde die meisten Edelweiß hat, gewinnt. */
    EDELWEISS_JAGD;

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
