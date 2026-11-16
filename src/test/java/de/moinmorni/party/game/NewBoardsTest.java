package de.moinmorni.party.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.party.board.BoardId;
import de.moinmorni.party.board.BoardLayout;
import de.moinmorni.party.board.FieldType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NewBoardsTest {

    private static List<PartyPlayer> players(int n) {
        List<PartyPlayer> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(new PartyPlayer(UUID.randomUUID(), "P" + i, i));
        }
        return out;
    }

    private static Engine engine(BoardId id, List<PartyPlayer> ps) {
        return new Engine(BoardLayout.of(id), Mode.EDELWEISS_JAGD, 10, ps, Rules.defaults(), new Random(4));
    }

    private static int first(BoardId id, FieldType t) {
        return BoardLayout.of(id).fields().stream().filter(f -> f.type() == t).findFirst().orElseThrow().index();
    }

    @Test
    void ferryTakesAShortcut() {
        List<PartyPlayer> ps = players(2);
        Engine e = engine(BoardId.KOENIGSSEE, ps);
        int f = first(BoardId.KOENIGSSEE, FieldType.FAEHRE);
        ps.get(0).setPos(f);
        Engine.Outcome o = e.land(ps.get(0));
        assertEquals(Engine.Kind.FERRY, o.kind());
        assertEquals(BoardLayout.of(BoardId.KOENIGSSEE).ferryTarget(f), ps.get(0).pos());
        assertTrue(o.steps() >= BoardLayout.FERRY);
    }

    @Test
    void ferrisWheelSwapsUnlessShielded() {
        List<PartyPlayer> ps = players(2);
        Engine e = engine(BoardId.WIESN, ps);
        int w = first(BoardId.WIESN, FieldType.RIESENRAD);
        ps.get(0).setPos(w);
        ps.get(1).setPos(30);
        Engine.Outcome o = e.land(ps.get(0));
        assertEquals(Engine.Kind.SWAP, o.kind());
        assertEquals(30, ps.get(0).pos());
        assertEquals(w, ps.get(1).pos());
        ps.get(0).setPos(w);
        ps.get(1).setPos(30);
        ps.get(1).shield = true;
        Engine.Outcome b = e.land(ps.get(0));
        assertTrue(b.blocked());
        assertEquals(w, ps.get(0).pos());
    }

    @Test
    void rides() {
        List<PartyPlayer> ps = players(3);
        Engine e = engine(BoardId.WIESN, ps);
        ps.get(0).setPos(10);
        assertEquals(Engine.COASTER, e.ride(ps.get(0), Ride.ACHTERBAHN).steps());
        // Autoscooter: wer bis zu 3 Felder dahinter steht, zahlt 2
        ps.get(1).setPos(8);
        ps.get(2).setPos(5);
        Engine.Outcome bump = e.ride(ps.get(0), Ride.AUTOSCOOTER);
        assertEquals(2, bump.taler());
        assertEquals(12, ps.get(0).taler());
        assertEquals(8, ps.get(1).taler());
        assertEquals(10, ps.get(2).taler());
        int k = e.ride(ps.get(0), Ride.KETTENKARUSSELL).steps();
        assertTrue(k != 0 && k >= -2 && k <= 3);
        Engine.Outcome lot = e.ride(ps.get(0), Ride.LOSBUDE);
        assertTrue(lot.item() != null || lot.taler() > 0);
    }

    @Test
    void avalancheHitsYouAndThoseJustAhead() {
        List<PartyPlayer> ps = players(4);
        Engine e = engine(BoardId.WINTERSONNWEND, ps);
        int l = first(BoardId.WINTERSONNWEND, FieldType.LAWINE);
        ps.get(0).setPos(l);
        ps.get(1).setPos(l + 2);
        ps.get(2).setPos(l + Engine.AVALANCHE_REACH + 1);
        ps.get(3).setPos(l - 1);
        Engine.Outcome o = e.land(ps.get(0));
        assertEquals(Engine.Kind.AVALANCHE, o.kind());
        assertEquals(List.of(ps.get(0).uuid(), ps.get(1).uuid()), List.copyOf(o.others().keySet()));
        assertEquals(-Engine.AVALANCHE_BACK, o.steps());
        ps.get(1).shield = true;
        Engine.Outcome b = e.land(ps.get(0));
        assertFalse(b.others().containsKey(ps.get(1).uuid()));
        assertEquals(1, ps.get(0).redFields > 0 ? 1 : 0);
    }

    @Test
    void iceSlidesToTheEnd() {
        List<PartyPlayer> ps = players(2);
        Engine e = engine(BoardId.WINTERSONNWEND, ps);
        ps.get(0).setPos(6);
        Engine.Outcome o = e.land(ps.get(0));
        assertEquals(Engine.Kind.SLIDE, o.kind());
        assertEquals(3, o.steps());
        assertEquals(Teams.Colour.RED, Teams.colour(FieldType.LAWINE));
        assertEquals(Teams.Colour.BLUE, Teams.colour(FieldType.EIS));
    }
}
