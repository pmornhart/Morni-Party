package de.moinmorni.party.mini;

import java.util.function.BiPredicate;

/**
 * Der Föhnsturm, ohne Minecraft: aus welcher Richtung die Böe kommt, wer dahinter Schutz hat (ein Fels zwischen ihm
 * und dem Wind) und wie stark sie schiebt (Ducken hilft).
 */
final class Wind {

    /** Acht Richtungen, aus denen der Wind kommt; dx/dz zeigen, wohin er bläst. */
    enum From {
        N(0, 1), NE(-1, 1), E(-1, 0), SE(-1, -1), S(0, -1), SW(1, -1), W(1, 0), NW(1, 1);

        final int dx;
        final int dz;

        From(int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
        }

        /** Richtung als Einheitsvektor (wohin er bläst). */
        double[] push() {
            double l = Math.hypot(dx, dz);
            return new double[] {dx / l, dz / l};
        }
    }

    /** So weit hinter einem Fels ist man noch geschützt. */
    static final int SHELTER = 3;
    /** Ducken: nur noch so viel der Böe kommt an. */
    static final double DUCKED = 0.3;

    private Wind() {
    }

    /**
     * Geschützt, wenn zwischen der Stelle (Block x, z) und dem Wind in bis zu {@link #SHELTER} Blöcken ein fester Block
     * steht ({@code solid(x, z)} fragt die Höhe der Füße und des Kopfes ab).
     */
    static boolean sheltered(int x, int z, From from, BiPredicate<Integer, Integer> solid) {
        for (int i = 1; i <= SHELTER; i++) {
            if (solid.test(x - from.dx * i, z - from.dz * i)) {
                return true;
            }
        }
        return false;
    }

    /** Stärke einer Böe zur Zeit t (0 = Anfang, 1 = Ende des Spiels): wird stärker. */
    static double strength(double t) {
        return 0.22 + 0.2 * Math.max(0, Math.min(1, t));
    }

    /** Wie stark es jemanden trifft: geschützt gar nicht, geduckt ein Drittel. */
    static double factor(boolean sheltered, boolean ducking) {
        return sheltered ? 0 : ducking ? DUCKED : 1;
    }
}
