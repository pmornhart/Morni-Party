package net.morni.party.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QueueAndVotesTest {

    private static List<UUID> ids(int n) {
        List<UUID> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(UUID.randomUUID());
        }
        return out;
    }

    @Test
    void partiesStayTogether() {
        WaitQueue q = new WaitQueue();
        List<UUID> p = ids(9);
        q.add(List.of(p.get(0)));
        q.add(List.of(p.get(1), p.get(2), p.get(3), p.get(4))); // Party zu viert
        q.add(List.of(p.get(5), p.get(6), p.get(7)));
        q.add(List.of(p.get(8)));
        assertEquals(9, q.size());
        assertEquals(8, q.fitting(8));
        List<UUID> game = q.take(6);
        // 1 + 4 = 5, die Dreier-Party passt nicht mehr, der Einzelne danach schon
        assertEquals(List.of(p.get(0), p.get(1), p.get(2), p.get(3), p.get(4), p.get(8)), game);
        assertEquals(3, q.size());
    }

    @Test
    void joiningMovesYouIntoTheUnit() {
        WaitQueue q = new WaitQueue();
        List<UUID> p = ids(3);
        q.add(List.of(p.get(0)));
        q.add(List.of(p.get(1)));
        assertTrue(q.join(p.get(1), p.get(0)));
        assertEquals(List.of(p.get(0), p.get(1)), q.take(2));
        q.add(List.of(p.get(2)));
        assertTrue(q.remove(p.get(2)));
        assertEquals(0, q.size());
    }

    @Test
    void tooBigPartyIsSplitOnlyIfAlone() {
        WaitQueue q = new WaitQueue();
        List<UUID> p = ids(10);
        q.add(p);
        assertEquals(8, q.take(8).size());
        assertEquals(2, q.size());
    }

    @Test
    void votesMajorityAndRandomTie() {
        Votes<String> v = new Votes<>();
        List<UUID> p = ids(3);
        v.vote(p.get(0), "schloss");
        v.vote(p.get(1), "schloss");
        v.vote(p.get(2), "gold");
        List<String> options = List.of("alm", "schloss", "gold");
        assertEquals("schloss", v.winner(options, p, new Random(1)));
        // Wer nicht mehr wartet, zählt nicht
        assertEquals("gold", v.winner(options, List.of(p.get(2)), new Random(1)));
        // Keiner stimmt ab → irgendeine Möglichkeit
        assertTrue(options.contains(v.winner(options, List.of(), new Random(5))));
    }

    @Test
    void gameKindsParse() {
        assertEquals(new GameKind(Mode.EDELWEISS_JAGD, 15), GameKind.parse("edelweiss-jagd-15"));
        assertEquals(new GameKind(Mode.GIPFELRENNEN, 0), GameKind.parse("gipfelrennen"));
        assertEquals("edelweiss-jagd-10", new GameKind(Mode.EDELWEISS_JAGD, 10).id());
        assertNull(GameKind.parse("quatsch"));
        assertEquals(Mode.EDELWEISS_JAGD, Mode.parse("edelweiß-jagd"));
    }
}
