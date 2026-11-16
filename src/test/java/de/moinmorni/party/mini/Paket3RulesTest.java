package de.moinmorni.party.mini;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Regeln von Musterweber, Föhnsturm und Enzian-Suche. */
class Paket3RulesTest {

    @Test
    void patternsGetHarderAndUseEveryColourTwice() {
        assertEquals(2, Webmuster.colours(1));
        assertEquals(3, Webmuster.colours(2));
        assertEquals(4, Webmuster.colours(3));
        assertTrue(Webmuster.showTicks(3) < Webmuster.showTicks(1));
        Random rng = new Random(3);
        for (int round = 1; round <= Webmuster.ROUNDS; round++) {
            int[] m = Webmuster.make(rng, Webmuster.colours(round));
            assertEquals(Webmuster.CELLS, m.length);
            for (int c = 0; c < Webmuster.colours(round); c++) {
                int colour = c;
                assertTrue(Arrays.stream(m).filter(x -> x == colour).count() >= 2, "Farbe " + c + " in Runde " + round);
            }
        }
    }

    @Test
    void everyRightCellIsAPointAllRightGivesABonus() {
        int[] target = new int[Webmuster.CELLS];
        int[] attempt = new int[Webmuster.CELLS];
        Arrays.fill(attempt, -1);
        assertEquals(0, Webmuster.points(target, attempt), "leer zählt nicht");
        attempt[0] = 0;
        attempt[1] = 1;
        assertEquals(1, Webmuster.points(target, attempt));
        Arrays.fill(attempt, 0);
        assertEquals(Webmuster.CELLS + Webmuster.PERFECT_BONUS, Webmuster.points(target, attempt));
    }

    @Test
    void rocksShelterFromTheWindOnlyOnTheWindySide() {
        Set<String> rocks = new HashSet<>(Set.of("0/0"));
        // Wind aus Westen bläst nach +x: wer östlich vom Fels steht, ist geschützt
        assertTrue(Wind.sheltered(2, 0, Wind.From.W, (x, z) -> rocks.contains(x + "/" + z)));
        assertFalse(Wind.sheltered(-2, 0, Wind.From.W, (x, z) -> rocks.contains(x + "/" + z)), "auf der falschen Seite");
        assertFalse(Wind.sheltered(5, 0, Wind.From.W, (x, z) -> rocks.contains(x + "/" + z)), "zu weit weg");
        assertTrue(Wind.sheltered(2, -2, Wind.From.SW, (x, z) -> rocks.contains(x + "/" + z)), "schräg");
        assertEquals(0, Wind.factor(true, false));
        assertEquals(Wind.DUCKED, Wind.factor(false, true));
        assertEquals(1, Wind.factor(false, false));
        assertTrue(Wind.strength(1) > Wind.strength(0), "der Föhn wird stärker");
        assertArrayEquals(new double[] {1, 0}, Wind.From.W.push(), 1e-9);
    }

    @Test
    void meadowHidesEnoughGentiansAndNothingBlueGrowsThere() {
        assertTrue(EnzianWiese.spots().size() >= 40, "Verstecke: " + EnzianWiese.spots().size());
        for (int[] s : EnzianWiese.spots()) {
            assertNull(EnzianWiese.at(s[0], 1, s[1]), "Versteck ist frei");
            assertNull(EnzianWiese.at(s[0], 2, s[1]));
            assertTrue(Math.abs(s[0]) < EnzianWiese.HALF && Math.abs(s[1]) < EnzianWiese.HALF);
            boolean next = false;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    next |= EnzianWiese.hides(EnzianWiese.at(s[0] + dx, 1, s[1] + dz));
                }
            }
            assertTrue(next, "gleich neben etwas Verdeckendem");
        }
        for (EnzianWiese.Cell c : EnzianWiese.cells()) {
            assertFalse(c.block().contains("cornflower") || c.block().contains("blue_orchid"), "nichts Blaues: " + c);
        }
        assertNotNull(EnzianWiese.cells().stream().filter(c -> c.block().startsWith("tall_grass")).findAny().orElse(null));
    }
}
