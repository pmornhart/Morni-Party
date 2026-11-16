package net.morni.party.mini;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class EisbahnTest {

    private static final double START_Z = -13.5;
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();

    private static void slide(Eisbahn bahn) {
        for (int i = 0; i < 2000 && bahn.moving(); i++) {
            bahn.step();
        }
        assertFalse(bahn.moving(), "alle Stöcke kommen zur Ruhe");
    }

    @Test
    void aboutTwoThirdsPowerReachesTheDaube() {
        Eisbahn bahn = new Eisbahn();
        Eisbahn.Stone s = bahn.launch(a, 0, START_Z, 0, 0.67);
        slide(bahn);
        assertFalse(s.out);
        assertEquals(0, s.x, 1e-9, "geradeaus bleibt geradeaus");
        assertTrue(Eisbahn.points(s.distance()) >= 3, "landet nah an der Daube: z = " + s.z);
    }

    @Test
    void moreForceGoesFurtherAndFullForceFallsOffTheEnd() {
        Eisbahn bahn = new Eisbahn();
        Eisbahn.Stone weak = bahn.launch(a, -3, START_Z, 0, 0.2);
        Eisbahn.Stone mid = bahn.launch(a, 0, START_Z, 0, 0.5);
        Eisbahn.Stone full = bahn.launch(b, 3, START_Z, 0, 1.0);
        slide(bahn);
        assertTrue(weak.z < mid.z, weak.z + " < " + mid.z);
        assertTrue(full.out, "volle Kraft rutscht hinten hinaus");
        assertEquals(0, bahn.score().get(b));
    }

    @Test
    void sideThrowsAreLimitedAndCanFallOffTheSide() {
        assertEquals(Eisbahn.MAX_ANGLE, Eisbahn.clampAngle(Math.PI / 2), 1e-9);
        assertEquals(-Eisbahn.MAX_ANGLE, Eisbahn.clampAngle(-3), 1e-9);
        Eisbahn bahn = new Eisbahn();
        Eisbahn.Stone s = bahn.launch(a, 4, START_Z, Math.PI / 2, 0.8);
        slide(bahn);
        assertTrue(s.out, "schräg vom Rand aus fällt er seitlich hinunter");
    }

    @Test
    void aHitKnocksTheOtherStoneAway() {
        Eisbahn bahn = new Eisbahn();
        Eisbahn.Stone lying = bahn.place(b, 0, Eisbahn.TARGET_Z);
        Eisbahn.Stone shot = bahn.launch(a, 0, START_Z, 0, 0.9);
        int hits = 0;
        for (int i = 0; i < 2000 && bahn.moving(); i++) {
            hits += bahn.step();
        }
        assertEquals(1, hits, "genau ein Zusammenstoß");
        assertTrue(lying.z > Eisbahn.TARGET_Z + 2 || lying.out, "der getroffene Stock rutscht weg: " + lying.z);
        assertTrue(Math.abs(shot.z - Eisbahn.TARGET_Z) < 1.0, "der Werfer bleibt fast stehen, wo der andere lag: " + shot.z);
        assertEquals(5, bahn.score().get(a), "jetzt liegt der eigene Stock an der Daube");
    }

    @Test
    void ringsGiveFiveThreeTwoOneNothing() {
        assertEquals(5, Eisbahn.points(0.0));
        assertEquals(5, Eisbahn.points(1.0));
        assertEquals(3, Eisbahn.points(1.5));
        assertEquals(2, Eisbahn.points(2.9));
        assertEquals(1, Eisbahn.points(4.0));
        assertEquals(0, Eisbahn.points(4.1));
        Eisbahn bahn = new Eisbahn();
        bahn.place(a, 0, Eisbahn.TARGET_Z + 0.5);
        bahn.place(a, 2.5, Eisbahn.TARGET_Z);
        bahn.place(b, 0, Eisbahn.TARGET_Z - 6);
        assertEquals(7, bahn.score().get(a), "Punkte aller eigenen Stöcke zusammen");
        assertEquals(0, bahn.score().get(b));
    }
}
