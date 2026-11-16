package de.moinmorni.party.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.party.board.BoardId;
import de.moinmorni.party.board.BoardLayout;
import de.moinmorni.party.board.FieldType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EngineTest {

    private static final BoardLayout BOARD = BoardLayout.of(BoardId.ALMSOMMER);

    private static List<PartyPlayer> players(int n) {
        List<PartyPlayer> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(new PartyPlayer(UUID.randomUUID(), "P" + i, i));
        }
        return out;
    }

    private static Engine engine(Mode mode, int rounds, List<PartyPlayer> ps) {
        return new Engine(BOARD, mode, rounds, ps, Rules.defaults(), new Random(42));
    }

    private static int fieldOf(FieldType type) {
        return BOARD.fields().stream().filter(f -> f.type() == type).findFirst().orElseThrow().index();
    }

    private static void moveTo(Engine e, PartyPlayer p, int field) {
        p.setPos(field);
    }

    @Test
    void startState() {
        List<PartyPlayer> ps = players(3);
        Engine e = engine(Mode.EDELWEISS_JAGD, 10, ps);
        for (PartyPlayer p : ps) {
            assertEquals(0, p.pos());
            assertEquals(10, p.taler());
        }
        assertEquals(1, e.round());
        assertTrue(e.stand() > Engine.DIE, "Stand nicht gleich am Anfang");
        assertTrue(BOARD.field(e.stand()).type().standAllowed());
        assertEquals(ps.get(0), e.current());
    }

    @Test
    void diceRanges() {
        List<PartyPlayer> ps = players(2);
        Engine e = engine(Mode.EDELWEISS_JAGD, 10, ps);
        PartyPlayer p = ps.get(0);
        for (int i = 0; i < 200; i++) {
            int r = e.roll(p).total();
            assertTrue(r >= 1 && r <= 6);
        }
        boolean big = false;
        for (int i = 0; i < 200; i++) {
            p.doubleDice = true;
            Engine.Roll r = e.roll(p);
            assertEquals(2, r.dice().length);
            assertTrue(r.total() >= 2 && r.total() <= 12);
            big |= r.total() > 6;
        }
        assertTrue(big);
        p.fixNextRoll(4);
        assertEquals(4, e.roll(p).total());
    }

    @Test
    void stepsAndWrapInEdelweissHunt() {
        List<PartyPlayer> ps = players(2);
        Engine e = engine(Mode.EDELWEISS_JAGD, 10, ps);
        PartyPlayer p = ps.get(0);
        moveTo(e, p, BOARD.goal() - 1);
        Engine.Step s = e.step(p);
        assertTrue(s.goal());
        assertFalse(e.over(), "Edelweiß-Jagd endet nicht am Ziel");
        int before = p.taler();
        s = e.step(p);
        assertTrue(s.wrapped());
        assertEquals(1, p.pos(), "Mit der Seilbahn zurück auf Feld 1");
        assertEquals(before + Rules.defaults().summitBonus(), p.taler());
    }

    @Test
    void raceEndsAtTheGoal() {
        List<PartyPlayer> ps = players(3);
        Engine e = engine(Mode.GIPFELRENNEN, 0, ps);
        assertEquals(Rules.defaults().raceMaxRounds(), e.rounds());
        PartyPlayer p = ps.get(1);
        moveTo(e, p, BOARD.goal() - 2);
        e.step(p);
        assertFalse(e.over());
        Engine.Step s = e.step(p);
        assertTrue(s.goal());
        assertTrue(e.over());
        assertEquals(1, p.finished());
        assertEquals(List.of(p), e.places().getFirst());
        // Am Ziel geht es nicht weiter
        assertEquals(BOARD.goal(), e.step(p).field());
    }

    @Test
    void raceRankingWithoutFinisherUsesDistanceThenEdelweiss() {
        List<PartyPlayer> ps = players(3);
        Engine e = engine(Mode.GIPFELRENNEN, 0, ps);
        ps.get(0).setPos(10);
        ps.get(1).setPos(20);
        ps.get(2).setPos(20);
        ps.get(2).setEdelweiss(1);
        List<List<PartyPlayer>> places = e.places();
        assertEquals(ps.get(2), places.get(0).getFirst());
        assertEquals(ps.get(1), places.get(1).getFirst());
        assertEquals(ps.get(0), places.get(2).getFirst());
    }

    @Test
    void raceStopsAfterMaxRounds() {
        Engine e = engine(Mode.GIPFELRENNEN, 0, players(2));
        for (int r = 0; r < Rules.defaults().raceMaxRounds(); r++) {
            assertFalse(e.over());
            e.nextRound();
        }
        assertTrue(e.over());
    }

    @Test
    void edelweissStandBuyMovesTheStand() {
        List<PartyPlayer> ps = players(2);
        Engine e = engine(Mode.EDELWEISS_JAGD, 10, ps);
        PartyPlayer p = ps.get(0);
        int stand = e.stand();
        assertFalse(e.canBuyEdelweiss(p), "10 Taler reichen nicht");
        assertFalse(e.buyEdelweiss(p));
        p.setTaler(45);
        assertTrue(e.buyEdelweiss(p));
        assertEquals(1, p.edelweiss());
        assertEquals(25, p.taler());
        assertNotEquals(stand, e.stand());
        assertTrue(BOARD.field(e.stand()).type().standAllowed());
        // Beim Vorbeiziehen meldet der Schritt den Stand
        p.setPos(e.stand() - 1);
        assertTrue(e.step(p).stand());
    }

    @Test
    void basicFields() {
        List<PartyPlayer> ps = players(2);
        Engine e = engine(Mode.EDELWEISS_JAGD, 10, ps);
        PartyPlayer p = ps.get(0);
        p.setPos(fieldOf(FieldType.TALER));
        assertEquals(3, e.land(p).taler());
        assertEquals(13, p.taler());
        p.setPos(fieldOf(FieldType.VERLUST));
        assertEquals(-3, e.land(p).taler());
        assertEquals(10, p.taler());
        p.setTaler(1);
        assertEquals(-1, e.land(p).taler(), "nie unter 0");
        assertEquals(0, p.taler());
        p.setPos(fieldOf(FieldType.LADEN));
        assertEquals(Engine.Kind.SHOP, e.land(p).kind());
        p.setPos(fieldOf(FieldType.DUELL));
        assertEquals(Engine.Kind.DUEL, e.land(p).kind());
        p.setPos(fieldOf(FieldType.GEGENSTAND));
        Engine.Outcome o = e.land(p);
        assertEquals(Engine.Kind.ITEM, o.kind());
        assertEquals(List.of(o.item()), p.items());
    }

    @Test
    void itemFieldWithFullBackpackGivesTaler() {
        List<PartyPlayer> ps = players(2);
        Engine e = engine(Mode.EDELWEISS_JAGD, 10, ps);
        PartyPlayer p = ps.get(0);
        for (int i = 0; i < 3; i++) {
            assertTrue(e.giveItem(p, ItemType.SCHILD));
        }
        assertFalse(e.giveItem(p, ItemType.SCHILD));
        p.setPos(fieldOf(FieldType.GEGENSTAND));
        assertEquals(Engine.Kind.TALER, e.land(p).kind());
        assertEquals(13, p.taler());
    }

    @Test
    void shieldBlocksLossAndBadLuckOnce() {
        List<PartyPlayer> ps = players(2);
        Engine e = engine(Mode.EDELWEISS_JAGD, 10, ps);
        PartyPlayer p = ps.get(0);
        e.giveItem(p, ItemType.SCHILD);
        assertTrue(e.useItem(p, ItemType.SCHILD, null, 0).ok());
        assertTrue(p.shield());
        p.setPos(fieldOf(FieldType.VERLUST));
        assertTrue(e.land(p).blocked());
        assertEquals(10, p.taler());
        assertFalse(p.shield());
        assertEquals(-5, e.luck(p, Luck.STEINSCHLAG).taler());
        p.shield = true;
        assertTrue(e.luck(p, Luck.NEBEL).blocked());
    }

    @Test
    void luckEvents() {
        List<PartyPlayer> ps = players(3);
        Engine e = engine(Mode.EDELWEISS_JAGD, 10, ps);
        PartyPlayer p = ps.get(0);
        p.setPos(fieldOf(FieldType.GLUECK));
        assertEquals(5, e.luck(p, Luck.GESCHENK).taler());
        assertEquals(3, e.luck(p, Luck.RUECKENWIND).steps());
        assertEquals(-3, e.luck(p, Luck.NEBEL).steps());
        Engine.Outcome s = e.luck(p, Luck.SAMMELN);
        assertEquals(4, s.taler());
        assertEquals(Map.of(ps.get(1).uuid(), -2, ps.get(2).uuid(), -2), s.others());
        assertEquals(8, ps.get(1).taler());
        Engine.Outcome sp = e.luck(p, Luck.SPENDIEREN);
        assertEquals(-4, sp.taler());
        assertEquals(10, ps.get(1).taler());
        e.giveItem(p, ItemType.TAUSCH);
        Engine.Outcome lost = e.luck(p, Luck.VERLOREN);
        assertEquals(ItemType.TAUSCH, lost.item());
        assertTrue(p.items().isEmpty());
    }

    @Test
    void stepBackNeverBeforeStart() {
        List<PartyPlayer> ps = players(2);
        Engine e = engine(Mode.EDELWEISS_JAGD, 10, ps);
        PartyPlayer p = ps.get(0);
        p.setPos(1);
        assertEquals(0, e.stepBack(p));
        assertEquals(0, e.stepBack(p));
    }

    @Test
    void duelMovesTaler() {
        List<PartyPlayer> ps = players(2);
        Engine e = engine(Mode.EDELWEISS_JAGD, 10, ps);
        PartyPlayer a = ps.get(0);
        PartyPlayer b = ps.get(1);
        a.fixNextRoll(6);
        b.fixNextRoll(2);
        Engine.Duel d = e.duel(a, b);
        assertEquals(a.uuid(), d.winner());
        assertEquals(5, d.taler());
        assertEquals(15, a.taler());
        assertEquals(5, b.taler());
        a.fixNextRoll(3);
        b.fixNextRoll(3);
        assertNull(e.duel(a, b).winner());
        // Schild schützt den Verlierer
        b.shield = true;
        a.fixNextRoll(5);
        b.fixNextRoll(1);
        Engine.Duel blocked = e.duel(a, b);
        assertTrue(blocked.blocked());
        assertEquals(5, b.taler());
    }

    @Test
    void shopRespectsMoneyAndBackpack() {
        List<PartyPlayer> ps = players(2);
        Engine e = engine(Mode.EDELWEISS_JAGD, 10, ps);
        PartyPlayer p = ps.get(0);
        assertFalse(e.buyItem(p, ItemType.LOCKRUF), "12 Taler, nur 10 da");
        assertTrue(e.buyItem(p, ItemType.DOPPELWUERFEL));
        assertEquals(2, p.taler());
        p.setTaler(100);
        assertTrue(e.buyItem(p, ItemType.SCHILD));
        assertTrue(e.buyItem(p, ItemType.SCHILD));
        assertFalse(e.buyItem(p, ItemType.SCHILD), "Rucksack voll");
    }

    @Test
    void itemsWork() {
        List<PartyPlayer> ps = players(3);
        Engine e = engine(Mode.EDELWEISS_JAGD, 10, ps);
        PartyPlayer p = ps.get(0);
        PartyPlayer o = ps.get(1);
        // Doppelwürfel
        e.giveItem(p, ItemType.DOPPELWUERFEL);
        assertTrue(e.useItem(p, ItemType.DOPPELWUERFEL, null, 0).ok());
        assertEquals(2, e.roll(p).dice().length);
        assertEquals("used", e.useItem(p, ItemType.SCHILD, null, 0).problem(), "nur einer pro Zug");
        e.endTurn();
        e.endTurn();
        e.endTurn();
        assertEquals(p, e.current());
        // Zielwürfel
        e.giveItem(p, ItemType.ZIELWUERFEL);
        assertEquals("number", e.useItem(p, ItemType.ZIELWUERFEL, null, 9).problem());
        assertTrue(e.useItem(p, ItemType.ZIELWUERFEL, null, 5).ok());
        assertEquals(5, e.roll(p).total());
        assertEquals(1, e.roll(p).dice().length, "danach wieder normal");
        p.itemUsed = false;
        // Tausch
        p.setPos(3);
        o.setPos(17);
        e.giveItem(p, ItemType.TAUSCH);
        assertEquals("target", e.useItem(p, ItemType.TAUSCH, null, 0).problem());
        assertEquals("target", e.useItem(p, ItemType.TAUSCH, p.uuid(), 0).problem());
        assertTrue(e.useItem(p, ItemType.TAUSCH, o.uuid(), 0).ok());
        assertEquals(17, p.pos());
        assertEquals(3, o.pos());
        p.itemUsed = false;
        // Talerklau
        e.giveItem(p, ItemType.TALERKLAU);
        o.setTaler(4);
        Engine.ItemUse steal = e.useItem(p, ItemType.TALERKLAU, o.uuid(), 0);
        assertEquals(4, steal.amount(), "nur was da ist");
        assertEquals(0, o.taler());
        assertEquals(14, p.taler());
        p.itemUsed = false;
        // Lockruf
        e.giveItem(p, ItemType.LOCKRUF);
        assertTrue(e.useItem(p, ItemType.LOCKRUF, null, 0).ok());
        assertEquals(e.stand() - 1, p.pos());
        assertTrue(e.step(p).stand());
        p.itemUsed = false;
        // Schild beim Ziel hält Tausch ab
        e.giveItem(p, ItemType.TAUSCH);
        o.shield = true;
        int mine = p.pos();
        assertTrue(e.useItem(p, ItemType.TAUSCH, o.uuid(), 0).blocked());
        assertEquals(mine, p.pos());
        p.itemUsed = false;
        assertEquals("missing", e.useItem(p, ItemType.TAUSCH, o.uuid(), 0).problem());
    }

    @Test
    void roundFlowAndEdelweissHuntEnd() {
        List<PartyPlayer> ps = players(3);
        Engine e = engine(Mode.EDELWEISS_JAGD, 2, ps);
        assertFalse(e.endTurn());
        assertEquals(ps.get(1), e.current());
        assertFalse(e.endTurn());
        assertTrue(e.endTurn(), "nach dem Dritten ist die Runde vorbei");
        assertEquals(ps.get(0), e.current());
        e.nextRound();
        assertEquals(2, e.round());
        assertFalse(e.over());
        e.endTurn();
        e.endTurn();
        e.endTurn();
        e.nextRound();
        assertTrue(e.over());
        ps.get(1).setEdelweiss(2);
        ps.get(2).setEdelweiss(2);
        ps.get(2).setTaler(30);
        assertEquals(ps.get(2), e.places().getFirst().getFirst());
        assertEquals(1, e.places().getFirst().size());
        assertEquals(Map.of(ps.get(2).uuid(), 1, ps.get(1).uuid(), 2, ps.get(0).uuid(), 3), e.placeOf());
    }

    @Test
    void leavingPlayersAreSkippedAndEndTheGameBelowTwo() {
        List<PartyPlayer> ps = players(3);
        Engine e = engine(Mode.EDELWEISS_JAGD, 10, ps);
        e.remove(ps.get(1).uuid());
        assertFalse(e.over());
        assertFalse(e.endTurn());
        assertEquals(ps.get(2), e.current(), "der Gegangene wird übersprungen");
        assertTrue(e.endTurn());
        assertEquals(ps.get(0), e.current());
        e.remove(ps.get(2).uuid());
        assertTrue(e.over());
        assertEquals(List.of(List.of(ps.get(0))), e.places());
    }

    @Test
    void minigameAwardsWithTies() {
        List<PartyPlayer> ps = players(4);
        Engine e = engine(Mode.EDELWEISS_JAGD, 10, ps);
        Map<UUID, Integer> got = e.awardMinigame(List.of(
                List.of(ps.get(2).uuid()),
                List.of(ps.get(0).uuid(), ps.get(3).uuid()),
                List.of(ps.get(1).uuid())));
        assertEquals(10, got.get(ps.get(2).uuid()));
        assertEquals(6, got.get(ps.get(0).uuid()));
        assertEquals(6, got.get(ps.get(3).uuid()));
        assertEquals(3, got.get(ps.get(1).uuid()), "Platz 4 (zwei teilen sich Platz 2)");
        assertEquals(1, ps.get(2).minigamesWon());
        assertEquals(0, ps.get(0).minigamesWon());
    }
}
