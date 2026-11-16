package de.moinmorni.party.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.party.board.BoardId;
import de.moinmorni.party.board.BoardLayout;
import de.moinmorni.party.board.FieldType;
import de.moinmorni.party.mini.MiniType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TeamsTest {

    private static List<UUID> ids(int n) {
        List<UUID> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(UUID.randomUUID());
        }
        return out;
    }

    private static Map<UUID, FieldType> landed(List<UUID> ids, FieldType... f) {
        Map<UUID, FieldType> m = new HashMap<>();
        for (int i = 0; i < f.length; i++) {
            m.put(ids.get(i), f[i]);
        }
        return m;
    }

    @Test
    void colours() {
        assertEquals(Teams.Colour.RED, Teams.colour(FieldType.VERLUST));
        assertEquals(Teams.Colour.RED, Teams.colour(FieldType.PECH));
        assertEquals(Teams.Colour.RED, Teams.colour(FieldType.DUELL));
        for (FieldType t : List.of(FieldType.TALER, FieldType.GLUECK, FieldType.LADEN, FieldType.GEGENSTAND,
                FieldType.START, FieldType.ZIEL)) {
            assertEquals(Teams.Colour.BLUE, Teams.colour(t));
        }
    }

    @Test
    void onlyFourPlayersMix() {
        List<UUID> three = ids(3);
        assertEquals(Teams.Format.FREE,
                Teams.decide(three, landed(three, FieldType.TALER, FieldType.PECH, FieldType.PECH)).format());
        List<UUID> five = ids(5);
        assertEquals(Teams.Format.FREE, Teams.decide(five, Map.of()).format());
    }

    @Test
    void allSameIsFree() {
        List<UUID> p = ids(4);
        assertEquals(Teams.Format.FREE, Teams.decide(p, landed(p, FieldType.TALER, FieldType.GLUECK, FieldType.LADEN,
                FieldType.GEGENSTAND)).format());
        assertEquals(Teams.Format.FREE, Teams.decide(p, landed(p, FieldType.PECH, FieldType.VERLUST, FieldType.DUELL,
                FieldType.PECH)).format());
        // ohne Feld (z. B. nicht gezogen) zählt blau
        assertEquals(Teams.Format.FREE, Teams.decide(p, Map.of()).format());
    }

    @Test
    void twoAgainstTwo() {
        List<UUID> p = ids(4);
        Teams.Split s = Teams.decide(p, landed(p, FieldType.TALER, FieldType.PECH, FieldType.LADEN, FieldType.VERLUST));
        assertEquals(Teams.Format.TEAM, s.format());
        assertEquals(List.of(p.get(0), p.get(2)), s.a());
        assertEquals(List.of(p.get(1), p.get(3)), s.b());
        assertTrue(s.mates(p.get(0), p.get(2)));
        assertFalse(s.mates(p.get(0), p.get(1)));
        assertFalse(s.mates(p.get(0), p.get(0)));
    }

    @Test
    void oneAgainstThree() {
        List<UUID> p = ids(4);
        Teams.Split red = Teams.decide(p, landed(p, FieldType.TALER, FieldType.TALER, FieldType.DUELL, FieldType.TALER));
        assertEquals(Teams.Format.SOLO, red.format());
        assertEquals(List.of(p.get(2)), red.a());
        assertEquals(3, red.b().size());
        Teams.Split blue = Teams.decide(p, landed(p, FieldType.PECH, FieldType.GLUECK, FieldType.PECH, FieldType.PECH));
        assertEquals(List.of(p.get(1)), blue.a());
    }

    @Test
    void everyFormatHasFourMinigames() {
        for (Teams.Format f : List.of(Teams.Format.TEAM, Teams.Format.SOLO, Teams.Format.DUEL)) {
            long n = java.util.Arrays.stream(MiniType.values()).filter(t -> t.fits(f)).count();
            assertTrue(n >= 4, f + ": " + n);
        }
        assertFalse(MiniType.TAUZIEHEN.fits(Teams.Format.FREE));
        assertFalse(MiniType.GAMSJAGD.fits(Teams.Format.FREE));
        assertTrue(MiniType.GAMSJAGD.fits(Teams.Format.SOLO));
    }

    private static List<PartyPlayer> players(int n) {
        List<PartyPlayer> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(new PartyPlayer(UUID.randomUUID(), "P" + i, i));
        }
        return out;
    }

    private static Engine engine(List<PartyPlayer> ps) {
        return new Engine(BoardLayout.of(BoardId.ALMSOMMER), Mode.EDELWEISS_JAGD, 10, ps, Rules.defaults(),
                new Random(1));
    }

    @Test
    void teamTaler() {
        List<PartyPlayer> ps = players(4);
        Engine e = engine(ps);
        Teams.Split s = new Teams.Split(Teams.Format.TEAM, List.of(ps.get(0).uuid(), ps.get(1).uuid()),
                List.of(ps.get(2).uuid(), ps.get(3).uuid()));
        Map<UUID, Integer> got = e.awardTeams(s, Teams.Outcome.B);
        assertEquals(0, got.get(ps.get(0).uuid()));
        assertEquals(8, got.get(ps.get(2).uuid()));
        assertEquals(18, ps.get(3).taler());
        assertEquals(1, ps.get(3).minigamesWon());
        assertEquals(0, ps.get(0).minigamesWon());
        e.awardTeams(s, Teams.Outcome.DRAW);
        assertEquals(13, ps.get(0).taler());
        assertEquals(21, ps.get(2).taler());
    }

    @Test
    void soloTaler() {
        List<PartyPlayer> ps = players(4);
        Engine e = engine(ps);
        Teams.Split s = new Teams.Split(Teams.Format.SOLO, List.of(ps.get(0).uuid()),
                List.of(ps.get(1).uuid(), ps.get(2).uuid(), ps.get(3).uuid()));
        e.awardTeams(s, Teams.Outcome.A);
        assertEquals(28, ps.get(0).taler());
        assertEquals(10, ps.get(1).taler());
        e.awardTeams(s, Teams.Outcome.B);
        assertEquals(28, ps.get(0).taler());
        assertEquals(18, ps.get(1).taler());
    }

    @Test
    void duelMinigameMovesStake() {
        List<PartyPlayer> ps = players(2);
        Engine e = engine(ps);
        Engine.Duel d = e.settleDuel(ps.get(0), ps.get(1), ps.get(1).uuid());
        assertEquals(ps.get(1).uuid(), d.winner());
        assertEquals(5, d.taler());
        assertEquals(5, ps.get(0).taler());
        assertEquals(15, ps.get(1).taler());
        Engine.Duel draw = e.settleDuel(ps.get(0), ps.get(1), null);
        assertNull(draw.winner());
        assertEquals(5, ps.get(0).taler());
        // Schild schützt
        ps.get(1).shield = true;
        Engine.Duel blocked = e.settleDuel(ps.get(0), ps.get(1), ps.get(0).uuid());
        assertTrue(blocked.blocked());
        assertEquals(15, ps.get(1).taler());
        // fremde UUID zählt nicht
        assertNull(e.settleDuel(ps.get(0), ps.get(1), UUID.randomUUID()).winner());
    }
}
