package net.morni.party.mini;

/**
 * Die Eisbahn des Schlittenrennens, ohne Minecraft: ein Oval (zwei Geraden, zwei Halbkreise). Jede Stelle bekommt
 * ihre Strecke ab der Ziellinie; Tore (Kontrollpunkte) zählen nur der Reihe nach – Abkürzen, Rückwärtsfahren oder
 * hin und her über die Ziellinie bringt nichts.
 */
final class Rennbahn {

    /** Halbe Länge der Geraden, Radius der Kurven (Mittellinie), halbe Breite der Fahrbahn. */
    static final double STRAIGHT = 20;
    static final double RADIUS = 14;
    static final double HALF_WIDTH = 3.5;
    static final int GATES = 6;
    static final int LAPS = 2;
    static final double LENGTH = 4 * STRAIGHT + 2 * Math.PI * RADIUS;

    private Rennbahn() {
    }

    /**
     * Strecke (0 bis LENGTH) des nächsten Punkts der Mittellinie. Die Ziellinie liegt in der Mitte der unteren Geraden
     * (x = 0, z = −RADIUS), gefahren wird zuerst nach +x, dann gegen den Uhrzeigersinn (von oben gesehen mit z nach
     * unten) über die rechte Kurve zurück.
     */
    static double locate(double x, double z) {
        double half = STRAIGHT;
        double s;
        if (x >= half) {
            // rechte Kurve um (half, 0): von unten (−RADIUS) nach oben (+RADIUS)
            double a = Math.atan2(z, x - half); // −π/2 unten … +π/2 oben
            s = half + RADIUS * (a + Math.PI / 2);
        } else if (x <= -half) {
            // linke Kurve um (−half, 0): von oben nach unten
            double a = Math.atan2(z, x + half); // +π/2 oben … ±π links … −π/2 unten
            double turned = a >= Math.PI / 2 ? a - Math.PI / 2 : a + 3 * Math.PI / 2;
            s = half + Math.PI * RADIUS + 2 * half + RADIUS * turned;
        } else if (z < 0) {
            s = x; // untere Gerade, Richtung +x
        } else {
            s = half + Math.PI * RADIUS + (half - x); // obere Gerade, Richtung −x
        }
        return Math.floorMod(Math.round(s * 1000), Math.round(LENGTH * 1000)) / 1000.0;
    }

    /** Punkt der Mittellinie bei Strecke s: {x, z, Fahrtrichtung in Grad (Minecraft-Blickwinkel)}. */
    static double[] point(double s) {
        double t = Math.floorMod(Math.round(s * 1000), Math.round(LENGTH * 1000)) / 1000.0;
        double half = STRAIGHT;
        double curve = Math.PI * RADIUS;
        if (t < half) {
            return new double[] {t, -RADIUS, -90};
        }
        if (t < half + curve) {
            double a = (t - half) / RADIUS - Math.PI / 2;
            return new double[] {half + Math.cos(a) * RADIUS, Math.sin(a) * RADIUS, yaw(-Math.sin(a), Math.cos(a))};
        }
        if (t < 3 * half + curve) {
            return new double[] {half - (t - half - curve), RADIUS, 90};
        }
        if (t < 3 * half + 2 * curve) {
            double a = (t - 3 * half - curve) / RADIUS + Math.PI / 2;
            return new double[] {-half + Math.cos(a) * RADIUS, Math.sin(a) * RADIUS, yaw(-Math.sin(a), Math.cos(a))};
        }
        return new double[] {t - LENGTH, -RADIUS, -90};
    }

    /** Blickwinkel (Minecraft: 0 = +z, 90 = −x) für eine Richtung. */
    private static double yaw(double dx, double dz) {
        return Math.toDegrees(Math.atan2(-dx, dz));
    }

    /** Abstand zur Mittellinie (für den Bau: Fahrbahn, Bande, außen). */
    static double offCenter(double x, double z) {
        if (x >= STRAIGHT) {
            return Math.abs(Math.hypot(x - STRAIGHT, z) - RADIUS);
        }
        if (x <= -STRAIGHT) {
            return Math.abs(Math.hypot(x + STRAIGHT, z) - RADIUS);
        }
        return Math.abs(Math.abs(z) - RADIUS);
    }

    /** Wo ein Tor liegt (Strecke ab der Ziellinie). Tor 0 ist die Ziellinie selbst. */
    static double gate(int k) {
        return LENGTH * Math.floorMod(k, GATES) / GATES;
    }

    /** Ein Schlitten im Rennen. */
    static final class Racer {
        private int passed;

        /** Durchfahrene Tore insgesamt (die Ziellinie am Start zählt nicht). */
        int passed() {
            return passed;
        }

        int lap() {
            return Math.min(LAPS, passed / GATES + 1);
        }

        boolean finished() {
            return passed >= LAPS * GATES;
        }

        /**
         * Neue Stelle. Ein Tor zählt, wenn man in der ersten Hälfte des Abschnitts dahinter ist und es das nächste
         * ist. Gibt true zurück, wenn gerade ein Tor dazukam.
         */
        boolean update(double s) {
            if (finished()) {
                return false;
            }
            double next = gate(passed + 1);
            double ahead = Math.floorMod(Math.round((s - next) * 1000), Math.round(LENGTH * 1000)) / 1000.0;
            if (ahead < LENGTH / GATES / 2) {
                passed++;
                return true;
            }
            return false;
        }

        /** Für die Wertung ohne Ankunft: Tore × 1000 + Blöcke seit dem letzten Tor. */
        int progress(double s) {
            double since = Math.floorMod(Math.round((s - gate(passed)) * 1000), Math.round(LENGTH * 1000)) / 1000.0;
            return passed * 1000 + (int) Math.min(LENGTH / GATES, since);
        }
    }
}
