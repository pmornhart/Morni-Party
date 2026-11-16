package net.morni.party.mini;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TaktTest {

    private static Takt.Done d(Takt.Move m, int offset) {
        return new Takt.Done(m, offset);
    }

    @Test
    void rightMoveOnTheBeatGivesTwoNearlyOne() {
        assertEquals(2, Takt.judge(Takt.Move.JUMP, List.of(d(Takt.Move.JUMP, 1)), 1.0));
        assertEquals(2, Takt.judge(Takt.Move.DUCK, List.of(d(Takt.Move.DUCK, -2)), 0));
        assertEquals(1, Takt.judge(Takt.Move.CLAP, List.of(d(Takt.Move.CLAP, 5)), 0));
        assertEquals(1, Takt.judge(Takt.Move.CLAP, List.of(d(Takt.Move.CLAP, -6)), 0));
        assertEquals(0, Takt.judge(Takt.Move.CLAP, List.of(d(Takt.Move.CLAP, -8)), 0), "zu früh");
        assertEquals(0, Takt.judge(Takt.Move.JUMP, List.of(), 0), "nichts getan");
    }

    @Test
    void wrongMoveSpoilsIt() {
        assertEquals(0, Takt.judge(Takt.Move.JUMP, List.of(d(Takt.Move.JUMP, 0), d(Takt.Move.DUCK, 3)), 0));
        assertEquals(2, Takt.judge(Takt.Move.JUMP, List.of(d(Takt.Move.DUCK, -9), d(Takt.Move.JUMP, 0)), 0),
                "eine falsche Bewegung weit vor dem Fenster zählt nicht");
        assertEquals(2, Takt.judge(Takt.Move.JUMP, List.of(d(Takt.Move.JUMP, -5), d(Takt.Move.JUMP, 1)), 0),
                "mehrmals springen: der beste Zeitpunkt zählt");
    }

    @Test
    void standingStillMeansDoingNothing() {
        assertEquals(1, Takt.judge(Takt.Move.STILL, List.of(), 0.1));
        assertEquals(0, Takt.judge(Takt.Move.STILL, List.of(), 1.5), "gelaufen");
        assertEquals(0, Takt.judge(Takt.Move.STILL, List.of(d(Takt.Move.CLAP, 0)), 0), "geklatscht");
    }

    @Test
    void movesAreMixedButNeverStillTwice() {
        assertEquals(Takt.Move.JUMP, Takt.pick(0, null));
        assertEquals(Takt.Move.DUCK, Takt.pick(45, null));
        assertEquals(Takt.Move.CLAP, Takt.pick(70, null));
        assertEquals(Takt.Move.STILL, Takt.pick(95, Takt.Move.JUMP));
        assertNotEquals(Takt.Move.STILL, Takt.pick(95, Takt.Move.STILL));
        assertTrue(Takt.LEAD >= Takt.BAR, "Ansage mindestens einen Takt vorher");
        assertTrue(Takt.CYCLE > Takt.LEAD + Takt.WINDOW, "Fenster vorbei, bevor die nächste Ansage kommt");
    }

    @Test
    void goldPilesHoldMostlyGravelSomeGoldRarelyANugget() {
        int nothing = 0;
        int grain = 0;
        int nugget = 0;
        for (int roll = 0; roll < 100; roll++) {
            switch (GoldWaschen.worth(roll)) {
                case 0 -> nothing++;
                case 1 -> grain++;
                case 3 -> nugget++;
                default -> throw new AssertionError();
            }
        }
        assertTrue(nothing > grain && grain > nugget && nugget > 0, nothing + "/" + grain + "/" + nugget);
        assertTrue(GoldWaschen.SPOTS.size() > 2 * 16, "genug freie Stellen für neue Haufen");
    }

    @Test
    void bridgeLanesDoNotTouch() {
        for (int i = 0; i + 1 < BrueckenBau.LANES; i++) {
            assertEquals(BrueckenBau.LANE_WIDTH, BrueckenBau.laneLeft(i + 1) - BrueckenBau.laneLeft(i));
        }
        assertTrue(BrueckenBau.laneLeft(BrueckenBau.LANES - 1) + BrueckenBau.LANE_WIDTH - 1 <= BrueckenBau.SIDE);
        assertEquals(3, BrueckenBau.laneOf(0, 2), "zwei Leute: die mittleren Spuren");
        assertEquals(0, BrueckenBau.laneOf(0, 8));
    }
}
