package net.morni.party.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.morni.party.board.BoardId;
import net.morni.party.board.BoardLayout;
import org.junit.jupiter.api.Test;

class ModesTest {

    @Test
    void mountainDuelIsForTwo() {
        assertTrue(Mode.BERGDUELL.fits(2));
        assertFalse(Mode.BERGDUELL.fits(3));
        assertFalse(Mode.BERGDUELL.fits(1));
        assertEquals(2, Mode.BERGDUELL.fixedMax());
        assertTrue(Mode.BERGDUELL.board());
        assertEquals("bergduell-8", new GameKind(Mode.BERGDUELL, 8).id());
        assertEquals(new GameKind(Mode.BERGDUELL, 8), GameKind.parse("bergduell-8"));
    }

    @Test
    void quickGameHasMoreMoneyAndCheaperEdelweiss() {
        Rules q = Rules.defaults().quick(20, 15);
        assertEquals(20, q.startTaler());
        assertEquals(15, q.edelweissPrice());
        assertEquals(Rules.defaults().duelStake(), q.duelStake());
        assertEquals("schnell-5", new GameKind(Mode.SCHNELL, 5).id());
        PartyPlayer a = new PartyPlayer(UUID.randomUUID(), "A", 0);
        PartyPlayer b = new PartyPlayer(UUID.randomUUID(), "B", 1);
        Engine e = new Engine(BoardLayout.of(BoardId.ALMSOMMER), Mode.SCHNELL, 5, List.of(a, b), q, new Random(1));
        assertEquals(20, a.taler());
        assertEquals(5, e.rounds());
        a.setPos(e.stand() - 1);
        assertTrue(e.canBuyEdelweiss(a));
        assertTrue(e.buyEdelweiss(a));
        assertEquals(5, a.taler());
    }

    @Test
    void everyModeHasAnIdThatParsesBack() {
        for (Mode m : Mode.values()) {
            assertEquals(m, Mode.parse(m.id()));
        }
    }
}
