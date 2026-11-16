package de.moinmorni.party.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.party.board.BoardId;
import de.moinmorni.party.board.BoardLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SeilschaftTest {

    private static List<UUID> ids(int n) {
        List<UUID> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(UUID.randomUUID());
        }
        return out;
    }

    @Test
    void partiesOfTwoStayTogether() {
        List<UUID> p = ids(6);
        List<List<UUID>> pairs = Seilschaft.pair(List.of(List.of(p.get(0)), List.of(p.get(1), p.get(2)), List.of(p.get(3)),
                List.of(p.get(4), p.get(5))));
        assertEquals(List.of(List.of(p.get(1), p.get(2)), List.of(p.get(4), p.get(5)), List.of(p.get(0), p.get(3))), pairs);
    }

    @Test
    void biggerPartiesAreSplitInPairs() {
        List<UUID> p = ids(4);
        List<List<UUID>> pairs = Seilschaft.pair(List.of(List.of(p.get(0), p.get(1), p.get(2)), List.of(p.get(3))));
        assertEquals(List.of(List.of(p.get(0), p.get(1)), List.of(p.get(2), p.get(3))), pairs);
    }

    @Test
    void turnsAlternateBetweenTeams() {
        List<UUID> p = ids(4);
        List<List<UUID>> order = Seilschaft.order(List.of(List.of(p.get(0), p.get(1)), List.of(p.get(2), p.get(3))));
        assertEquals(List.of(List.of(p.get(0)), List.of(p.get(2)), List.of(p.get(1)), List.of(p.get(3))), order);
        assertEquals(6, Seilschaft.evenMax(7));
        assertEquals(8, Seilschaft.evenMax(8));
    }

    @Test
    void modeNeedsAnEvenNumber() {
        assertFalse(Mode.SEILSCHAFT.fits(3));
        assertFalse(Mode.SEILSCHAFT.fits(5));
        assertTrue(Mode.SEILSCHAFT.fits(4));
        assertTrue(Mode.SEILSCHAFT.fits(8));
        assertTrue(Mode.EDELWEISS_JAGD.fits(3));
        assertEquals("seilschaft-10", new GameKind(Mode.SEILSCHAFT, 10).id());
        assertEquals(new GameKind(Mode.SEILSCHAFT, 15), GameKind.parse("seilschaft-15"));
    }

    private static Engine roped(List<PartyPlayer> ps) {
        Engine e = new Engine(BoardLayout.of(BoardId.ALMSOMMER), Mode.SEILSCHAFT, 10, ps, Rules.defaults(), new Random(3));
        e.team(List.of(List.of(ps.get(0).uuid(), ps.get(1).uuid()), List.of(ps.get(2).uuid(), ps.get(3).uuid())));
        return e;
    }

    private static List<PartyPlayer> players() {
        List<PartyPlayer> out = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            out.add(new PartyPlayer(UUID.randomUUID(), "P" + i, i));
        }
        return out;
    }

    @Test
    void purseIsShared() {
        List<PartyPlayer> ps = players();
        Engine e = roped(ps);
        assertEquals(20, ps.get(0).taler());
        assertEquals(20, ps.get(1).taler());
        ps.get(0).addTaler(5);
        assertEquals(25, ps.get(1).taler());
        assertEquals(20, ps.get(2).taler());
        assertTrue(e.mates(ps.get(0), ps.get(1)));
        assertFalse(e.mates(ps.get(0), ps.get(2)));
        assertFalse(e.mates(ps.get(0), ps.get(0)));
        // Reihenfolge: A1, B1, A2, B2
        assertEquals(List.of("P0", "P2", "P1", "P3"), e.players().stream().map(PartyPlayer::name).toList());
        assertEquals(ps.get(0), e.current());
        assertEquals(2, e.teams().size());
    }

    @Test
    void edelweissBelongsToBothAndTeamsSharePlaces() {
        List<PartyPlayer> ps = players();
        Engine e = roped(ps);
        ps.get(3).setPos(e.stand() - 1);
        ps.get(3).setTaler(40);
        assertTrue(e.buyEdelweiss(ps.get(3)));
        assertEquals(1, ps.get(2).edelweiss());
        List<List<PartyPlayer>> places = e.places();
        assertEquals(2, places.getFirst().size());
        assertTrue(places.getFirst().containsAll(List.of(ps.get(2), ps.get(3))));
    }

    @Test
    void noStealingFromThePartner() {
        List<PartyPlayer> ps = players();
        Engine e = roped(ps);
        assertTrue(e.giveItem(ps.get(0), ItemType.TALERKLAU));
        Engine.ItemUse u = e.useItem(ps.get(0), ItemType.TALERKLAU, ps.get(1).uuid(), 0);
        assertFalse(u.ok());
        assertEquals("partner", u.problem());
        Engine.ItemUse ok = e.useItem(ps.get(0), ItemType.TALERKLAU, ps.get(2).uuid(), 0);
        assertTrue(ok.ok());
        assertEquals(27, ps.get(1).taler());
        assertEquals(13, ps.get(3).taler());
    }

    @Test
    void luckSkipsThePartner() {
        List<PartyPlayer> ps = players();
        Engine e = roped(ps);
        Engine.Outcome o = e.luck(ps.get(0), Luck.SAMMELN);
        // nur die andere Seilschaft zahlt (je 2 Taler, beide aus demselben Beutel)
        assertEquals(4, o.taler());
        assertEquals(24, ps.get(1).taler());
        assertEquals(16, ps.get(2).taler());
    }
}
