package net.morni.party.mini;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScoringTest {

    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private final UUID c = UUID.randomUUID();
    private final UUID d = UUID.randomUUID();

    private Scoring with(Scoring.Kind kind) {
        Scoring s = new Scoring(kind);
        for (UUID u : List.of(a, b, c, d)) {
            s.join(u);
        }
        return s;
    }

    @Test
    void pointsMostWinsTiesShareAPlace() {
        Scoring s = with(Scoring.Kind.POINTS);
        s.add(a, 3);
        s.add(b, 5);
        s.add(c, 3);
        assertEquals(List.of(List.of(b), List.of(a, c), List.of(d)), s.places());
        assertEquals(List.of(b), s.winners());
    }

    @Test
    void survivalLastStandingWinsSimultaneousOutShares() {
        Scoring s = with(Scoring.Kind.SURVIVAL);
        assertTrue(s.eliminate(d, 10));
        assertFalse(s.eliminate(d, 12), "zweimal raus geht nicht");
        s.eliminate(b, 30);
        s.eliminate(c, 30);
        assertEquals(List.of(a), s.alive());
        assertEquals(List.of(List.of(a), List.of(b, c), List.of(d)), s.places());
    }

    @Test
    void allSurvivorsShareFirstPlace() {
        Scoring s = with(Scoring.Kind.SURVIVAL);
        s.eliminate(c, 5);
        assertEquals(List.of(a, b, d), s.winners());
    }

    @Test
    void leaversDoNotCount() {
        Scoring s = with(Scoring.Kind.POINTS);
        s.add(a, 9);
        s.drop(a);
        s.add(a, 1);
        assertFalse(s.has(a));
        assertEquals(3, s.places().getFirst().size(), "b, c, d gleichauf");
    }

    @Test
    void raceFirstArrivalWinsOthersByDistance() {
        Scoring s = with(Scoring.Kind.RACE);
        s.set(a, 40);
        s.set(b, 12);
        s.set(c, 30);
        s.set(d, 30);
        assertTrue(s.finish(c, 200));
        assertFalse(s.finish(c, 210), "zweimal ankommen geht nicht");
        assertTrue(s.finish(a, 260));
        assertEquals(2, s.finishers());
        assertEquals(200, s.finishTick(c));
        assertEquals(-1, s.finishTick(d));
        // c vor a (früher da), obwohl a mehr Strecke hat; dann d (30) vor b (12)
        assertEquals(List.of(List.of(c), List.of(a), List.of(d), List.of(b)), s.places());
        assertFalse(s.allFinished());
        s.finish(b, 300);
        s.finish(d, 300);
        assertTrue(s.allFinished());
        assertEquals(List.of(List.of(c), List.of(a), List.of(b, d)), s.places(), "gleichzeitig = gleicher Platz");
    }

    @Test
    void raceWithoutFinishersSharesEqualDistance() {
        Scoring s = with(Scoring.Kind.RACE);
        s.set(a, 7);
        s.set(b, 7);
        s.set(c, 3);
        assertEquals(List.of(List.of(a, b), List.of(c), List.of(d)), s.places());
        s.drop(a);
        assertFalse(s.finish(a, 5), "wer gegangen ist, kommt nicht mehr an");
    }
}
