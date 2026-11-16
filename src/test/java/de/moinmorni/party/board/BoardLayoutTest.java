package de.moinmorni.party.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BoardLayoutTest {

    @Test
    void newBoardsHaveTheirSpecialFields() {
        BoardLayout lake = BoardLayout.of(BoardId.KOENIGSSEE);
        int ferries = 0;
        for (var f : lake.fields()) {
            if (f.type() == FieldType.FAEHRE) {
                ferries++;
                int to = lake.ferryTarget(f.index());
                assertTrue(to >= f.index() + BoardLayout.FERRY && to < lake.goal(), "Fähre " + f.index() + " → " + to);
                var t = lake.field(to);
                // in gerader Linie über das Wasser: kürzer als der Weg über die Stege
                double walk = 0;
                for (int k = f.index(); k < to; k++) {
                    walk += Math.hypot(lake.field(k + 1).x() - lake.field(k).x(), lake.field(k + 1).z() - lake.field(k).z());
                }
                assertTrue(Math.hypot(t.x() - f.x(), t.z() - f.z()) < walk, "Abkürzung " + f.index());
            }
        }
        assertEquals(3, ferries);
        assertEquals(-1, lake.ferryTarget(1));
        BoardLayout wiesn = BoardLayout.of(BoardId.WIESN);
        assertEquals(2, wiesn.fields().stream().filter(f -> f.type() == FieldType.RIESENRAD).count());
        assertEquals(4, wiesn.fields().stream().filter(f -> f.type() == FieldType.FAHRGESCHAEFT).count());
        BoardLayout winter = BoardLayout.of(BoardId.WINTERSONNWEND);
        assertEquals(9, winter.iceEnd(6));
        assertEquals(9, winter.iceEnd(8));
        assertEquals(19, winter.iceEnd(17));
        assertEquals(5, winter.iceEnd(5));
        assertEquals(3, winter.fields().stream().filter(f -> f.type() == FieldType.LAWINE).count());
        for (BoardId id : List.of(BoardId.ALMSOMMER, BoardId.SCHLOSS, BoardId.GOLDSTOLLEN)) {
            assertTrue(BoardLayout.of(id).fields().stream().noneMatch(f -> f.type().ordinal() > FieldType.ZIEL.ordinal()));
        }
    }

    @Test
    void everyBoardHasStartGoalAndAllFieldTypes() {
        for (BoardId id : BoardId.values()) {
            BoardLayout b = BoardLayout.of(id);
            assertEquals(BoardLayout.FIELDS, b.size(), id.id());
            assertEquals(FieldType.START, b.field(0).type());
            assertEquals(FieldType.ZIEL, b.field(b.goal()).type());
            Map<FieldType, Integer> count = new EnumMap<>(FieldType.class);
            b.fields().forEach(f -> count.merge(f.type(), 1, Integer::sum));
            for (FieldType t : List.of(FieldType.TALER, FieldType.VERLUST, FieldType.GLUECK, FieldType.PECH,
                    FieldType.DUELL, FieldType.LADEN, FieldType.GEGENSTAND)) {
                assertTrue(count.getOrDefault(t, 0) > 0, id + " ohne " + t);
            }
            assertEquals(1, count.get(FieldType.START));
            assertEquals(1, count.get(FieldType.ZIEL));
            assertTrue(count.get(FieldType.LADEN) >= 2, id + ": zu wenig Läden");
            assertTrue(b.standFields().size() >= 10, id + ": zu wenig Plätze für den Edelweiß-Stand");
        }
    }

    @Test
    void fieldsAreEvenlySpacedAndDoNotOverlap() {
        for (BoardId id : BoardId.values()) {
            BoardLayout b = BoardLayout.of(id);
            Set<String> seen = new HashSet<>();
            for (int i = 1; i < b.size(); i++) {
                Field a = b.field(i - 1);
                Field c = b.field(i);
                double d = Math.hypot(a.x() - c.x(), a.z() - c.z());
                // Felder sind 3×3 groß: mindestens ein Block Weg dazwischen, aber nicht zu weit auseinander.
                assertTrue(d >= 4.0 && d <= 8.0, id + " Abstand " + i + ": " + d);
                assertTrue(Math.abs(a.y() - c.y()) <= 2, id + " Stufe " + i);
            }
            for (Field f : b.fields()) {
                for (Field o : b.fields()) {
                    if (o.index() != f.index()) {
                        double d = Math.hypot(f.x() - o.x(), f.z() - o.z());
                        assertTrue(d >= 4.0 || Math.abs(f.y() - o.y()) > 3, id + " Überlappung " + f + " / " + o);
                    }
                }
                assertTrue(seen.add(f.x() + "," + f.z()), id + " doppelt " + f);
                assertTrue(Math.hypot(f.x(), f.z()) <= b.shape().outer() + 1, id + " außerhalb " + f);
            }
        }
    }

    @Test
    void terrainCarriesTheTrail() {
        for (BoardId id : BoardId.values()) {
            BoardLayout b = BoardLayout.of(id);
            for (BoardLayout.Sample s : b.samples()) {
                assertEquals(s.y(), b.terrainHeight(s.r()), 0.01, id + " bei r=" + s.r());
            }
            // Berg nach oben, Bergwerk nach unten
            boolean up = b.field(b.goal()).y() > b.field(0).y();
            assertEquals(id != BoardId.GOLDSTOLLEN, up, id.id());
        }
    }

    @Test
    void boardNamesParse() {
        assertEquals(BoardId.SCHLOSS, BoardId.parse("schloss"));
        assertEquals(BoardId.GOLDSTOLLEN, BoardId.parse("gold"));
        assertEquals(null, BoardId.parse("x"));
    }
}
