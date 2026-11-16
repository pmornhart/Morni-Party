package net.morni.party.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.morni.party.board.BoardId;
import net.morni.party.board.BoardLayout;
import org.junit.jupiter.api.Test;

class HuetteTest {

    private static List<UUID> ids(int n) {
        List<UUID> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(UUID.randomUUID());
        }
        return out;
    }

    private static List<List<UUID>> single(List<UUID> order) {
        return order.stream().map(List::of).toList();
    }

    @Test
    void bottomHalfLoses() {
        List<UUID> p = ids(8);
        assertEquals(p.subList(4, 8), Huette.losers(single(p)));
        List<UUID> two = ids(2);
        assertEquals(List.of(two.get(1)), Huette.losers(single(two)));
        List<UUID> three = ids(3);
        assertEquals(List.of(three.get(2)), Huette.losers(single(three)));
    }

    @Test
    void sharedPlaceCountsByBestRank() {
        List<UUID> p = ids(4);
        // Platz 1, dann drei gleich auf Platz 2 → die Gruppe beginnt in der oberen Hälfte: niemand verliert
        assertTrue(Huette.losers(List.of(List.of(p.get(0)), List.of(p.get(1), p.get(2), p.get(3)))).isEmpty());
        // alle gleich: niemand verliert
        assertTrue(Huette.losers(List.of(p)).isEmpty());
        // zwei vorn, zwei gleich hinten: die hinteren verlieren
        assertEquals(List.of(p.get(2), p.get(3)), Huette.losers(List.of(List.of(p.get(0)), List.of(p.get(1)),
                List.of(p.get(2), p.get(3)))));
    }

    @Test
    void hearts() {
        assertEquals("❤❤♡", Huette.hearts(2, 3));
        assertEquals("♡♡♡", Huette.hearts(0, 3));
    }

    @Test
    void lastOneStandingWins() {
        List<PartyPlayer> ps = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ps.add(new PartyPlayer(UUID.randomUUID(), "P" + i, i));
        }
        Engine e = new Engine(BoardLayout.of(BoardId.ALMSOMMER), Mode.HUETTE, 25, ps, Rules.defaults(), new Random(1));
        assertEquals(3, ps.get(0).lives());
        UUID a = ps.get(0).uuid();
        UUID b = ps.get(1).uuid();
        UUID c = ps.get(2).uuid();
        // C ist dreimal Letzter → raus
        for (int i = 0; i < 3; i++) {
            Engine.Climb climb = e.climb(List.of(List.of(a), List.of(b), List.of(c)));
            assertEquals(List.of(c), climb.lost());
            e.nextRound();
        }
        assertTrue(ps.get(2).out());
        assertEquals(List.of(ps.get(0), ps.get(1)), e.climbing());
        assertFalse(e.over());
        // jetzt nur noch A und B: B verliert dreimal
        for (int i = 0; i < 3; i++) {
            e.climb(List.of(List.of(a), List.of(b)));
            e.nextRound();
        }
        assertTrue(e.over());
        List<List<PartyPlayer>> places = e.places();
        assertEquals(List.of(ps.get(0)), places.get(0));
        assertEquals(List.of(ps.get(1)), places.get(1));
        assertEquals(List.of(ps.get(2)), places.get(2));
        assertEquals(6, ps.get(0).minigamesWon());
    }

    @Test
    void hutModeIsBoardless() {
        assertFalse(Mode.HUETTE.board());
        assertFalse(Mode.HUETTE.counted());
        assertEquals("huette", new GameKind(Mode.HUETTE, 0).id());
        assertEquals(new GameKind(Mode.HUETTE, 0), GameKind.parse("huette"));
    }
}
