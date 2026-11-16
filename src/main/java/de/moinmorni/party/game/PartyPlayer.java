package de.moinmorni.party.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/** Stand einer Person im Brettspiel. */
public final class PartyPlayer {

    private final UUID uuid;
    private final String name;
    private final int colour;
    int pos;
    int taler;
    int edelweiss;
    final List<ItemType> items = new ArrayList<>();
    boolean shield;
    boolean doubleDice;
    int targetDice;
    boolean itemUsed;
    boolean left;
    /** Gipfelrennen: als Wievielte am Ziel (0 = noch nicht). */
    int finished;
    int minigamesWon;
    int minigamePoints;
    /** Nur für Tests/Verwaltung: feste nächste Würfe. */
    final Deque<Integer> fixed = new ArrayDeque<>();

    public PartyPlayer(UUID uuid, String name, int colour) {
        this.uuid = uuid;
        this.name = name;
        this.colour = colour;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    /** Farbe der Spielfigur (0–7). */
    public int colour() {
        return colour;
    }

    public int pos() {
        return pos;
    }

    public int taler() {
        return taler;
    }

    public int edelweiss() {
        return edelweiss;
    }

    public List<ItemType> items() {
        return List.copyOf(items);
    }

    public boolean shield() {
        return shield;
    }

    public boolean doubleDice() {
        return doubleDice;
    }

    public int targetDice() {
        return targetDice;
    }

    public boolean itemUsed() {
        return itemUsed;
    }

    public boolean left() {
        return left;
    }

    public int finished() {
        return finished;
    }

    public int minigamesWon() {
        return minigamesWon;
    }

    // ---------- Verwaltung (/mparty) und Tests ----------

    public void setPos(int pos) {
        this.pos = pos;
    }

    public void setTaler(int taler) {
        this.taler = Math.max(0, taler);
    }

    public void setEdelweiss(int edelweiss) {
        this.edelweiss = Math.max(0, edelweiss);
    }

    public void fixNextRoll(int value) {
        fixed.addLast(value);
    }

    /** Taler ändern, nie unter 0; gibt die echte Änderung zurück. */
    int addTaler(int delta) {
        int before = taler;
        taler = Math.max(0, taler + delta);
        return taler - before;
    }
}
