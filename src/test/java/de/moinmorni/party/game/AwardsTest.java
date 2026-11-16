package de.moinmorni.party.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.party.board.BoardId;
import de.moinmorni.party.board.BoardLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AwardsTest {

    private static List<PartyPlayer> players(int n) {
        List<PartyPlayer> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(new PartyPlayer(UUID.randomUUID(), "P" + i, i));
        }
        return out;
    }

    @Test
    void bestGetsOneEdelweissTiesShare() {
        List<PartyPlayer> ps = players(3);
        Engine e = new Engine(BoardLayout.of(BoardId.ALMSOMMER), Mode.EDELWEISS_JAGD, 10, ps, Rules.defaults(), new Random(1));
        ps.get(0).walked = 30;
        ps.get(1).walked = 22;
        ps.get(1).minigamesWon = 3;
        ps.get(2).minigamesWon = 3;
        ps.get(2).redFields = 5;
        List<Awards.Award> got = e.awards(List.of(Awards.Type.values()));
        assertEquals(3, got.size(), "Duellkönig fällt aus (niemand hat ein Duell gewonnen)");
        assertEquals(List.of(ps.get(0)), got.get(0).winners());
        assertEquals(30, got.get(0).value());
        assertEquals(List.of(ps.get(1), ps.get(2)), got.get(1).winners());
        assertEquals(1, ps.get(0).edelweiss());
        assertEquals(1, ps.get(1).edelweiss());
        assertEquals(2, ps.get(2).edelweiss());
    }

    @Test
    void walkingAndRedFieldsAndDuelsAreCounted() {
        List<PartyPlayer> ps = players(2);
        Engine e = new Engine(BoardLayout.of(BoardId.ALMSOMMER), Mode.EDELWEISS_JAGD, 10, ps, Rules.defaults(), new Random(1));
        e.step(ps.get(0));
        e.step(ps.get(0));
        assertEquals(2, ps.get(0).walked);
        int red = BoardLayout.of(BoardId.ALMSOMMER).fields().stream()
                .filter(f -> f.type() == de.moinmorni.party.board.FieldType.VERLUST).findFirst().orElseThrow().index();
        ps.get(1).setPos(red);
        e.land(ps.get(1));
        assertEquals(1, ps.get(1).redFields);
        e.settleDuel(ps.get(0), ps.get(1), ps.get(1).uuid());
        assertEquals(1, ps.get(1).duelsWon);
    }

    @Test
    void notInTheRaceOrWithoutABoard() {
        List<PartyPlayer> ps = players(2);
        ps.get(0).walked = 5;
        Engine race = new Engine(BoardLayout.of(BoardId.ALMSOMMER), Mode.GIPFELRENNEN, 0, ps, Rules.defaults(), new Random(1));
        assertTrue(race.awards(List.of(Awards.Type.values())).isEmpty());
        Engine mara = new Engine(BoardLayout.of(BoardId.ALMSOMMER), Mode.MARATHON, 3, ps, Rules.defaults(), new Random(1));
        assertTrue(mara.awards(List.of(Awards.Type.values())).isEmpty());
        assertEquals(Awards.Type.PECHVOGEL, Awards.Type.parse("pechvogel"));
    }
}
