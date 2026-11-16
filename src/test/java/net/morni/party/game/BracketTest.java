package net.morni.party.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BracketTest {

    private static List<UUID> ids(int n) {
        List<UUID> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(UUID.randomUUID());
        }
        return out;
    }

    /** Spielt das Turnier durch (immer gewinnt a) und zählt die Spiele. */
    private static int playOut(Bracket b, Set<UUID> seen) {
        int matches = 0;
        while (!b.done()) {
            Bracket.Match m = b.next();
            assertFalse(m.bye(), "Freilose werden übersprungen");
            assertTrue(seen.add(m.b()), "wer verliert, spielt nicht wieder");
            b.report(m.a());
            matches++;
        }
        return matches;
    }

    @Test
    void everyoneButTheChampionLosesExactlyOnce() {
        for (int n = 2; n <= 16; n++) {
            List<UUID> p = ids(n);
            Bracket b = new Bracket(p, new Random(n));
            Set<UUID> losers = new HashSet<>();
            int matches = playOut(b, losers);
            assertEquals(n - 1, matches, "n = " + n);
            assertNotNull(b.champion());
            assertFalse(losers.contains(b.champion()));
            assertEquals(n - 1, losers.size());
        }
    }

    @Test
    void roundNamesFromTheEnd() {
        Bracket b = new Bracket(ids(8), new Random(1));
        assertEquals(3, b.roundCount());
        assertEquals(3, b.left());
        for (int i = 0; i < 4; i++) {
            b.report(b.next().a());
        }
        assertEquals(2, b.left(), "Halbfinale");
        b.report(b.next().a());
        b.report(b.next().a());
        assertEquals(1, b.left(), "Finale");
        assertNull(b.champion());
        b.report(b.next().b());
        assertTrue(b.done());
        assertNotNull(b.champion());
    }

    @Test
    void byesNeverMeetTwice() {
        // 5 Leute: drei Freilose, die drei mit Freilos spielen danach nicht gegen ein weiteres Freilos
        Bracket b = new Bracket(ids(5), new Random(7));
        assertEquals(3, b.roundCount());
        Bracket.Match first = b.next();
        assertFalse(first.bye());
        b.report(first.a());
        // jetzt Runde 2 – vier echte Teilnehmende, keine Freilose
        assertEquals(1, b.round());
        assertFalse(b.next().bye());
    }

    @Test
    void wrongWinnerIsRefused() {
        Bracket b = new Bracket(ids(4), new Random(2));
        assertThrows(IllegalArgumentException.class, () -> b.report(UUID.randomUUID()));
    }

    @Test
    void leavingGivesAWalkover() {
        List<UUID> p = ids(4);
        Bracket b = new Bracket(p, new Random(3));
        Bracket.Match m = b.next();
        b.forfeit(m.b());
        // das Spiel mit dem Gegangenen ist erledigt, a ist weiter
        Bracket.Match after = b.next();
        assertTrue(after.round() > 0 || after.index() > 0);
        while (!b.done()) {
            b.report(b.next().a());
        }
        assertNotNull(b.champion());
        assertTrue(b.champion() != m.b());
    }

    @Test
    void needsTwo() {
        assertThrows(IllegalArgumentException.class, () -> new Bracket(ids(1), new Random()));
    }
}
