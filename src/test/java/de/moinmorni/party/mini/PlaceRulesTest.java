package de.moinmorni.party.mini;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Regeln der Minispiel-Plätze, die sich ohne Server prüfen lassen (begehbar, erreichbar, nichts doppelt). */
class PlaceRulesTest {

    @Test
    void avalancheSlopeIsWalkableWithoutJumping() {
        assertEquals(0, LawinenHang.eighths(LawinenHang.START_Z), "Start ist flach");
        assertEquals(1.0, LawinenHang.surface(LawinenHang.START_Z), 1e-9);
        int top = LawinenHang.eighths(LawinenHang.GOAL_Z);
        assertEquals(top, LawinenHang.eighths(LawinenHang.BACK_Z), "oben ein flaches Ziel-Plateau");
        assertTrue(top >= 64, "der Hang ist hoch genug: " + top / 8 + " Blöcke");
        for (int z = LawinenHang.START_Z - 4; z < LawinenHang.BACK_Z; z++) {
            int step = LawinenHang.eighths(z + 1) - LawinenHang.eighths(z);
            assertTrue(step >= 0 && step <= 4, "höchstens ein halber Block pro Schritt (Minecraft steigt 0,6 von selbst): z = " + z);
        }
        assertTrue(LawinenHang.SLIDE < LawinenHang.GOAL_Z - LawinenHang.START_Z);
    }

    @Test
    void maypoleLanesHangOnTheTrunkAndDoNotOverlap() {
        Set<String> seen = new HashSet<>();
        for (MaibaumKraxeln.Lane l : MaibaumKraxeln.LANES) {
            boolean side = (Math.abs(l.x()) == 2 && Math.abs(l.z()) <= 1) || (Math.abs(l.z()) == 2 && Math.abs(l.x()) <= 1);
            assertTrue(side, "Spur direkt am Stamm: " + l);
            assertTrue(seen.add(l.x() + "/" + l.z()), "doppelt: " + l);
            assertTrue(l.block().startsWith("ladder") || l.block().startsWith("vine"), l.block());
        }
        assertEquals(12, MaibaumKraxeln.LANES.size(), "drei Spuren an jeder Seite");
    }

    @Test
    void throwersStandOnThePlatformWithRoomForTheirStones() {
        for (int n = 1; n <= 8; n++) {
            for (int i = 0; i < n; i++) {
                double x = EisstockSchiessen.laneX(i, n);
                assertTrue(Math.abs(x) <= EisstockSchiessen.HALF - 1, n + "/" + i + ": " + x);
                if (i > 0) {
                    assertTrue(x - EisstockSchiessen.laneX(i - 1, n) >= 2 * Eisbahn.RADIUS, "Stöcke liegen nicht übereinander");
                }
            }
        }
    }

    @Test
    void marmotHolesAreInsideTheMeadow() {
        for (int g : MurmelTier.GRID) {
            assertTrue(Math.abs(g) < MurmelTier.HALF, String.valueOf(g));
        }
    }
}
