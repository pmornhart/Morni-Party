package net.morni.party.mini;

/**
 * Regeln des Tatzelwurms, ohne Minecraft: Lebenspunkte nach Spielerzahl, drei Abschnitte, Treffer, Sterne. Alle
 * kämpfen zusammen, die Wertung ist gemeinsam.
 * <ol>
 * <li><b>Aus dem Boden</b>: der Kopf bricht an einer der Wurmlöcher heraus – draufhauen, solange er draußen ist;
 * dazwischen spuckt er Felsbrocken (Schatten am Boden warnt).</li>
 * <li><b>Die Jagd</b>: der Wurm schlängelt im Kreis um den Platz; leuchtende Schuppen am Körper sind verwundbar, der
 * Kopf stößt um.</li>
 * <li><b>Der Zorn</b>: aufgerichtet in der Mitte schickt er Bodenwellen (drüberspringen) und schlägt mit dem Kopf
 * zu (roter Kreis warnt); danach liegt der Kopf kurz am Boden – jetzt zuschlagen.</li>
 * </ol>
 */
final class WurmRegeln {

    /** Lebenspunkte: Grundwert und je Person. */
    static final int BASE_HP = 18;
    static final int HP_PER_PLAYER = 12;
    /** Ab diesem Anteil der Lebenspunkte beginnt Abschnitt 2 bzw. 3. */
    static final double PHASE_2 = 2.0 / 3;
    static final double PHASE_3 = 1.0 / 3;
    /** So lange (Ticks) ist man nach einem Treffer benommen und kann nicht zuschlagen. */
    static final int STUN = 40;
    /** Mindestabstand zwischen zwei Treffern derselben Person (Ticks) – Dauerklicken bringt nichts. */
    static final int HIT_GAP = 6;

    private WurmRegeln() {
    }

    static int hp(int players) {
        return BASE_HP + HP_PER_PLAYER * Math.max(1, players);
    }

    /** Abschnitt (1–3) bei {@code hp} von {@code max} Lebenspunkten. */
    static int phase(int hp, int max) {
        double f = (double) hp / Math.max(1, max);
        return f > PHASE_2 ? 1 : f > PHASE_3 ? 2 : 3;
    }

    /** Schaden eines Treffers: am Kopf mehr als an einer Schuppe. */
    static int damage(boolean head) {
        return head ? 2 : 1;
    }

    /**
     * Sterne der Gruppe: besiegt in höchstens der Hälfte der Zeit 3, in höchstens vier Fünfteln 2, sonst 1; nicht
     * besiegt 0. Ab zwei Benommenheiten pro Person (alle zusammengezählt) gibt es höchstens 2 Sterne – vorsichtig
     * spielen lohnt sich.
     */
    static int stars(boolean won, int ticks, int duration, int stuns, int players) {
        if (!won) {
            return 0;
        }
        int s = ticks * 2 <= duration ? 3 : ticks * 5 <= duration * 4 ? 2 : 1;
        if (stuns >= 2 * Math.max(1, players)) {
            s = Math.min(s, 2);
        }
        return s;
    }

    /** Wie viele Wurmlöcher es gibt und wo (Winkel in Grad, Abstand zur Mitte). */
    static final int HOLES = 6;
    static final double HOLE_RADIUS = 9.5;

    static double[] hole(int i) {
        double a = Math.toRadians(360.0 * i / HOLES + 15);
        return new double[] {Math.cos(a) * HOLE_RADIUS, Math.sin(a) * HOLE_RADIUS};
    }

    /** Abschnitt 2: Punkt auf der Bahn des Wurms (Kreis mit Schlangenlinie) bei Strecke {@code s} (Blöcke). */
    static double[] path(double s) {
        double r = 11 + 1.6 * Math.sin(s / 4.0);
        double a = s / 11.0;
        return new double[] {Math.cos(a) * r, Math.sin(a) * r};
    }
}
