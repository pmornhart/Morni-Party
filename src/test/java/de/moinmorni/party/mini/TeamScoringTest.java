package de.moinmorni.party.mini;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.party.game.Teams;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TeamScoringTest {

    private final UUID a1 = UUID.randomUUID();
    private final UUID a2 = UUID.randomUUID();
    private final UUID b1 = UUID.randomUUID();
    private final UUID b2 = UUID.randomUUID();
    private final UUID b3 = UUID.randomUUID();

    private Scoring score(Scoring.Kind k, UUID... all) {
        Scoring s = new Scoring(k);
        for (UUID u : all) {
            s.join(u);
        }
        return s;
    }

    @Test
    void pointsAddUpPerSide() {
        Scoring s = score(Scoring.Kind.POINTS, a1, a2, b1, b2);
        Teams.Split t = new Teams.Split(Teams.Format.TEAM, List.of(a1, a2), List.of(b1, b2));
        s.add(a1, 9);
        s.add(b1, 5);
        s.add(b2, 5);
        assertEquals(Teams.Outcome.B, s.teamOutcome(t, 4));
        s.add(a2, 1);
        assertEquals(Teams.Outcome.DRAW, s.teamOutcome(t, 4));
        s.add(a2, 1);
        assertEquals(Teams.Outcome.A, s.teamOutcome(t, 4));
    }

    @Test
    void loneSideCountsMore() {
        Scoring s = score(Scoring.Kind.POINTS, a1, b1, b2, b3);
        Teams.Split t = new Teams.Split(Teams.Format.SOLO, List.of(a1), List.of(b1, b2, b3));
        s.add(a1, 3);
        s.add(b1, 4);
        s.add(b2, 4);
        s.add(b3, 4);
        // 3 × 4 = 12 gegen 12
        assertEquals(Teams.Outcome.DRAW, s.teamOutcome(t, 4));
        assertEquals(Teams.Outcome.B, s.teamOutcome(t, 3));
        s.add(a1, 1);
        assertEquals(Teams.Outcome.A, s.teamOutcome(t, 4));
    }

    @Test
    void survivalBestPlaceWins() {
        Scoring s = score(Scoring.Kind.SURVIVAL, a1, a2, b1, b2);
        Teams.Split t = new Teams.Split(Teams.Format.TEAM, List.of(a1, a2), List.of(b1, b2));
        s.eliminate(a1, 10);
        s.eliminate(b1, 20);
        s.eliminate(b2, 30);
        // a2 übrig
        assertEquals(Teams.Outcome.A, s.teamOutcome(t, 4));
    }

    @Test
    void survivalTieGoesToNextBest() {
        Scoring s = score(Scoring.Kind.SURVIVAL, a1, a2, b1, b2);
        Teams.Split t = new Teams.Split(Teams.Format.TEAM, List.of(a1, a2), List.of(b1, b2));
        s.eliminate(a1, 10);
        s.eliminate(b1, 20);
        // a2 und b2 teilen sich Platz 1, b1 hielt länger als a1
        assertEquals(Teams.Outcome.B, s.teamOutcome(t, 4));
    }

    @Test
    void raceFirstArrivalWins() {
        Scoring s = score(Scoring.Kind.RACE, a1, b1);
        Teams.Split t = Teams.Split.duel(a1, b1);
        s.set(a1, 30);
        s.set(b1, 20);
        assertEquals(Teams.Outcome.A, s.teamOutcome(t, 4));
        s.finish(b1, 100);
        assertEquals(Teams.Outcome.B, s.teamOutcome(t, 4));
    }

    @Test
    void emptySideLoses() {
        Scoring s = score(Scoring.Kind.POINTS, a1, b1);
        Teams.Split t = Teams.Split.duel(a1, b1);
        s.add(b1, 5);
        s.drop(b1);
        assertEquals(Teams.Outcome.A, s.teamOutcome(t, 4));
        s.drop(a1);
        assertEquals(Teams.Outcome.DRAW, s.teamOutcome(t, 4));
    }

    @Test
    void ropeNeedsBothSides() {
        Seil r = new Seil(2, 2);
        UUID x = UUID.randomUUID();
        assertTrue(r.pull(x, 0, 0));
        assertFalse(r.pull(x, 0, 1), "zu schnell");
        assertTrue(r.pull(x, 0, 2));
        assertEquals(-1.0, r.pos(), 1e-9);
        assertFalse(r.pull(UUID.randomUUID(), -1, 4), "ohne Seite");
        assertEquals(-1, r.leader());
        r.pull(UUID.randomUUID(), 1, 4);
        assertEquals(0, r.leader(), "knapp an der Mitte = Gleichstand");
    }

    @Test
    void ropeIsFairForSmallerSide() {
        // Einer gegen zwei (jemand ist gegangen): gleich schnelles Klicken bleibt ausgeglichen.
        Seil r = new Seil(1, 2);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        for (int t = 0; t < 40; t += 2) {
            r.pull(a, 0, t);
            r.pull(b, 1, t);
            r.pull(c, 1, t);
        }
        assertEquals(0, r.pos(), 1e-9);
    }

    @Test
    void ropeWinsAtLimitAndStops() {
        Seil r = new Seil(1, 1);
        UUID a = UUID.randomUUID();
        int t = 0;
        while (r.won() == 0) {
            assertTrue(r.pull(a, 1, t));
            t += Seil.GAP;
        }
        assertEquals(1, r.won());
        assertEquals(Seil.LIMIT, r.pos(), 1e-9);
        assertFalse(r.pull(a, 0, t + 10), "vorbei");
        assertEquals(1, r.leader());
    }
}
