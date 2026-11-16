package de.moinmorni.party.mini;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WurmRegelnTest {

    @Test
    void moreLifeForMorePlayers() {
        assertEquals(30, WurmRegeln.hp(1));
        assertEquals(66, WurmRegeln.hp(4));
        assertTrue(WurmRegeln.hp(8) > WurmRegeln.hp(4));
    }

    @Test
    void threeStages() {
        int max = 60;
        assertEquals(1, WurmRegeln.phase(60, max));
        assertEquals(1, WurmRegeln.phase(41, max));
        assertEquals(2, WurmRegeln.phase(40, max));
        assertEquals(2, WurmRegeln.phase(21, max));
        assertEquals(3, WurmRegeln.phase(20, max));
        assertEquals(3, WurmRegeln.phase(0, max));
    }

    @Test
    void headCountsDouble() {
        assertEquals(2, WurmRegeln.damage(true));
        assertEquals(1, WurmRegeln.damage(false));
    }

    @Test
    void starsForSpeedAndCare() {
        int d = 4800;
        assertEquals(0, WurmRegeln.stars(false, 100, d, 0, 4));
        assertEquals(3, WurmRegeln.stars(true, 2400, d, 0, 4));
        assertEquals(2, WurmRegeln.stars(true, 2401, d, 0, 4));
        assertEquals(2, WurmRegeln.stars(true, 3840, d, 0, 4));
        assertEquals(1, WurmRegeln.stars(true, 3841, d, 0, 4));
        // zu oft benommen: höchstens zwei Sterne
        assertEquals(2, WurmRegeln.stars(true, 1000, d, 8, 4));
        assertEquals(3, WurmRegeln.stars(true, 1000, d, 7, 4));
    }

    @Test
    void holesAndPathStayInsideTheArena() {
        for (int i = 0; i < WurmRegeln.HOLES; i++) {
            double[] h = WurmRegeln.hole(i);
            assertTrue(Math.hypot(h[0], h[1]) < TatzelwurmKampf.HALF - 3);
        }
        for (double s = 0; s < 200; s += 0.5) {
            double[] p = WurmRegeln.path(s);
            double r = Math.hypot(p[0], p[1]);
            assertTrue(r > 4 && r < TatzelwurmKampf.HALF - 2, "Bahn bei " + s + ": " + r);
        }
    }
}
