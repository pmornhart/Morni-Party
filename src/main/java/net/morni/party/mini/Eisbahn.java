package net.morni.party.mini;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Die Eisbahn des Eisstockschießens, ohne Minecraft: Eisstöcke gleiten, werden langsamer, stoßen sich gegenseitig weg
 * (gleich schwer, wie Billardkugeln) und fallen über den Rand. Punkte nach dem Abstand zur Daube.
 * Koordinaten: x quer zur Bahn (0 = Mitte), z entlang der Bahn; eine Einheit = ein Block, Tempo in Blöcken pro Tick.
 */
final class Eisbahn {

    static final double RADIUS = 0.35;
    static final double FRICTION = 0.012;
    static final double MIN_SPEED = 0.32;
    static final double MAX_SPEED = 0.95;
    static final double MAX_ANGLE = Math.toRadians(20);
    static final double LEFT = -5.3;
    static final double RIGHT = 5.3;
    static final double FRONT = -14.5;
    static final double BACK = 21.3;
    static final double TARGET_X = 0;
    static final double TARGET_Z = 10;
    /** Ringe um die Daube: bis zu diesem Abstand gibt es die Punkte daneben. */
    static final double[] RINGS = {1.0, 2.0, 3.0, 4.0};
    static final int[] RING_POINTS = {5, 3, 2, 1};

    /** Ein Eisstock. */
    static final class Stone {
        final UUID owner;
        double x;
        double z;
        double vx;
        double vz;
        boolean out;

        Stone(UUID owner, double x, double z) {
            this.owner = owner;
            this.x = x;
            this.z = z;
        }

        boolean moving() {
            return !out && (vx != 0 || vz != 0);
        }

        double distance() {
            return Math.hypot(x - TARGET_X, z - TARGET_Z);
        }
    }

    private final List<Stone> stones = new ArrayList<>();

    /** Wurfkraft 0–1 → Anfangstempo. */
    static double speed(double power) {
        return MIN_SPEED + (MAX_SPEED - MIN_SPEED) * Math.max(0, Math.min(1, power));
    }

    /** Richtung (0 = geradeaus die Bahn hinunter, positiv = nach +x), höchstens ±20°. */
    static double clampAngle(double angle) {
        return Math.max(-MAX_ANGLE, Math.min(MAX_ANGLE, angle));
    }

    /** Punkte für einen Abstand zur Daube. */
    static int points(double distance) {
        for (int i = 0; i < RINGS.length; i++) {
            if (distance <= RINGS[i]) {
                return RING_POINTS[i];
            }
        }
        return 0;
    }

    /** Eisstock abwerfen. */
    Stone launch(UUID owner, double x, double z, double angle, double power) {
        Stone s = new Stone(owner, x, z);
        double a = clampAngle(angle);
        double v = speed(power);
        s.vx = Math.sin(a) * v;
        s.vz = Math.cos(a) * v;
        stones.add(s);
        return s;
    }

    /** Ein Stock, der (z. B. von früher) still liegt. */
    Stone place(UUID owner, double x, double z) {
        Stone s = new Stone(owner, x, z);
        stones.add(s);
        return s;
    }

    List<Stone> stones() {
        return Collections.unmodifiableList(stones);
    }

    boolean moving() {
        return stones.stream().anyMatch(Stone::moving);
    }

    /** Ein Tick: gleiten, zusammenstoßen, bremsen, hinausfallen. Gibt die Zahl der Zusammenstöße zurück. */
    int step() {
        for (Stone s : stones) {
            if (s.moving()) {
                s.x += s.vx;
                s.z += s.vz;
            }
        }
        int hits = 0;
        for (int i = 0; i < stones.size(); i++) {
            for (int j = i + 1; j < stones.size(); j++) {
                if (collide(stones.get(i), stones.get(j))) {
                    hits++;
                }
            }
        }
        for (Stone s : stones) {
            if (!s.moving()) {
                continue;
            }
            double v = Math.hypot(s.vx, s.vz);
            if (v <= FRICTION) {
                s.vx = 0;
                s.vz = 0;
            } else {
                s.vx *= (v - FRICTION) / v;
                s.vz *= (v - FRICTION) / v;
            }
            if (s.x < LEFT || s.x > RIGHT || s.z < FRONT || s.z > BACK) {
                s.out = true;
                s.vx = 0;
                s.vz = 0;
            }
        }
        return hits;
    }

    /** Gleich schwere Scheiben: Tempo entlang der Verbindungslinie wird getauscht. */
    private static boolean collide(Stone a, Stone b) {
        if (a.out || b.out) {
            return false;
        }
        double dx = b.x - a.x;
        double dz = b.z - a.z;
        double d = Math.hypot(dx, dz);
        if (d >= 2 * RADIUS || d == 0) {
            return false;
        }
        double nx = dx / d;
        double nz = dz / d;
        double approach = (a.vx - b.vx) * nx + (a.vz - b.vz) * nz;
        // auseinanderschieben, damit sie nicht ineinander hängen bleiben
        double push = (2 * RADIUS - d) / 2;
        a.x -= nx * push;
        a.z -= nz * push;
        b.x += nx * push;
        b.z += nz * push;
        if (approach <= 0) {
            return false;
        }
        a.vx -= approach * nx;
        a.vz -= approach * nz;
        b.vx += approach * nx;
        b.vz += approach * nz;
        return true;
    }

    /** Punkte je Werfer (alle Stöcke auf der Bahn zusammen). */
    Map<UUID, Integer> score() {
        Map<UUID, Integer> out = new LinkedHashMap<>();
        for (Stone s : stones) {
            out.merge(s.owner, s.out ? 0 : points(s.distance()), Integer::sum);
        }
        return out;
    }
}
