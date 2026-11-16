package net.morni.party.mini;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GamsRevierTest {

    @Test
    void spawnsAreFree() {
        for (double[] s : GamsRevier.spawns()) {
            int x = (int) Math.floor(s[0] + 0.5);
            int z = (int) Math.floor(s[1] + 0.5);
            assertEquals(0, GamsRevier.height(x, z), "Fels am Start " + x + "/" + z);
            assertNull(GamsRevier.at(x, 1, z), "etwas am Start " + x + "/" + z);
            assertNull(GamsRevier.at(x, 2, z));
        }
    }

    @Test
    void everyRockCanBeClimbed() {
        int h = GamsRevier.HALF;
        int rocks = 0;
        for (int x = -h; x <= h; x++) {
            for (int z = -h; z <= h; z++) {
                int here = GamsRevier.height(x, z);
                rocks += here > 0 ? 1 : 0;
                for (int[] d : new int[][] {{1, 0}, {0, 1}}) {
                    assertTrue(Math.abs(here - GamsRevier.height(x + d[0], z + d[1])) <= 1, x + "/" + z);
                }
            }
        }
        assertTrue(rocks > 40, "genug Felsen zum Verstecken: " + rocks);
    }

    @Test
    void seatHasLadderAndPlatform() {
        assertEquals("spruce_planks", GamsRevier.at(GamsRevier.SEAT_X, GamsRevier.SEAT_Y, GamsRevier.SEAT_Z).block());
        assertTrue(GamsRevier.at(GamsRevier.SEAT_X - 2, 1, GamsRevier.SEAT_Z).block().startsWith("ladder"));
    }
}
