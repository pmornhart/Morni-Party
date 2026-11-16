package de.moinmorni.party.game;

/**
 * Wahl im Wartebereich: Spielart samt Rundenzahl (Edelweiß-Jagd: 10, 15 oder 20 Runden; Marathon: so viele
 * Minispiele).
 */
public record GameKind(Mode mode, int rounds) {

    public String id() {
        return mode.counted() ? mode.id() + "-" + rounds : mode.id();
    }

    public static GameKind parse(String s) {
        if (s == null) {
            return null;
        }
        int dash = s.lastIndexOf('-');
        if (dash > 0) {
            try {
                int r = Integer.parseInt(s.substring(dash + 1));
                Mode m = Mode.parse(s.substring(0, dash));
                if (m != null && m.counted() && r > 0) {
                    return new GameKind(m, r);
                }
            } catch (NumberFormatException e) {
                // weiter unten
            }
        }
        Mode m = Mode.parse(s);
        return m != null && !m.counted() ? new GameKind(m, 0) : null;
    }
}
