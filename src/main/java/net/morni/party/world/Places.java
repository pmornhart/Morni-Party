package net.morni.party.world;

import net.morni.party.board.BoardId;
import net.morni.party.mini.MiniType;

/**
 * Wo was in der Welt steht. Mitte: der Festplatz (Wartebereich). Jeder Tisch (eine Partie gleichzeitig) hat seine
 * drei Spielbretter und alle Minispiel-Plätze (vier nebeneinander, Reihe für Reihe), weit weg vom Festplatz und
 * voneinander.
 */
public final class Places {

    public static final int FLOOR_Y = 100;
    public static final double SPAWN_X = 0.5;
    public static final double SPAWN_Z = 14.5;
    /** Blick nach Norden (zum Maibaum). */
    public static final float SPAWN_YAW = 180f;
    private static final int TABLE_SPACING = 1000;
    private static final int BOARD_SPACING = 300;
    private static final int MINI_OFFSET = 300;
    private static final int MINI_SPACING = 140;
    private static final int MINI_COLUMNS = 4;

    private Places() {
    }

    /** Mitte eines Spielbretts: {x, y, z}. */
    public static int[] board(int table, BoardId board) {
        return new int[] {TABLE_SPACING * (table + 1), FLOOR_Y, BOARD_SPACING * (board.ordinal() - 1)};
    }

    /** Mitte eines Minispiel-Platzes: {x, y, z}. */
    public static int[] mini(int table, MiniType type) {
        int i = type.ordinal();
        return new int[] {TABLE_SPACING * (table + 1) + MINI_OFFSET + (i % MINI_COLUMNS) * MINI_SPACING, FLOOR_Y,
            MINI_SPACING * (i / MINI_COLUMNS - 1)};
    }
}
