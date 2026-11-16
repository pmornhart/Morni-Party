package de.moinmorni.party.mini;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RennbahnTest {

    @Test
    void pointAndLocateFitTogetherAllAround() {
        for (double s = 0; s < Rennbahn.LENGTH; s += 1.7) {
            double[] p = Rennbahn.point(s);
            assertEquals(s, Rennbahn.locate(p[0], p[1]), 0.01, "s = " + s);
            assertEquals(0, Rennbahn.offCenter(p[0], p[1]), 1e-6);
        }
        assertEquals(0, Rennbahn.locate(0, -Rennbahn.RADIUS), 1e-9, "Ziellinie unten in der Mitte");
        assertEquals(-90, Rennbahn.point(5)[2], 1e-9, "Start fährt nach +x");
    }

    @Test
    void edgesOfTheLaneBelongToTheSameSpot() {
        double s = Rennbahn.locate(5, -Rennbahn.RADIUS);
        assertEquals(s, Rennbahn.locate(5, -Rennbahn.RADIUS - 3), 1e-9);
        assertEquals(s, Rennbahn.locate(5, -Rennbahn.RADIUS + 3), 1e-9);
        assertEquals(3, Rennbahn.offCenter(5, -Rennbahn.RADIUS + 3), 1e-9);
    }

    @Test
    void twoLapsThroughAllGatesFinish() {
        Rennbahn.Racer r = new Rennbahn.Racer();
        r.update(Rennbahn.LENGTH - 8); // Startaufstellung hinter der Linie
        r.update(1); // über die Linie: zählt noch nicht
        assertEquals(0, r.passed());
        for (double s = 1; s < 2 * Rennbahn.LENGTH + 5; s += 3) {
            r.update(s % Rennbahn.LENGTH);
        }
        assertEquals(2 * Rennbahn.GATES, r.passed());
        assertTrue(r.finished());
        assertEquals(Rennbahn.LAPS, r.lap());
    }

    @Test
    void noShortcutsNoBackwards() {
        Rennbahn.Racer r = new Rennbahn.Racer();
        r.update(Rennbahn.gate(2) + 1); // Tor 1 ausgelassen
        assertEquals(0, r.passed());
        r.update(Rennbahn.gate(1) + 2);
        assertEquals(1, r.passed());
        r.update(Rennbahn.gate(1) - 5); // zurück
        r.update(Rennbahn.gate(1) + 2); // und wieder vor: dasselbe Tor zählt nicht doppelt
        assertEquals(1, r.passed());
        // hin und her über die Ziellinie bringt keine Runde
        Rennbahn.Racer q = new Rennbahn.Racer();
        for (int i = 0; i < 10; i++) {
            q.update(Rennbahn.LENGTH - 2);
            q.update(2);
        }
        assertEquals(0, q.passed());
        assertFalse(q.finished());
    }

    @Test
    void progressRanksByGatesThenDistance() {
        Rennbahn.Racer a = new Rennbahn.Racer();
        Rennbahn.Racer b = new Rennbahn.Racer();
        a.update(Rennbahn.gate(1) + 1);
        assertTrue(a.progress(Rennbahn.gate(1) + 10) > b.progress(Rennbahn.gate(1) - 1));
        assertTrue(a.progress(Rennbahn.gate(1) + 10) > a.progress(Rennbahn.gate(1) + 2));
    }
}
