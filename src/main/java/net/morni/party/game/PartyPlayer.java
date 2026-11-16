package net.morni.party.game;

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
    /** Taler und Edelweiß (in der Seilschaft mit dem Partner geteilt). */
    Purse purse = new Purse();
    /** Seilschaft: Nummer der Seilschaft (-1 = keine). */
    int team = -1;
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
    /** Für die Auszeichnungen: gelaufene Felder, rote Felder, gewonnene Duelle. */
    int walked;
    int redFields;
    int duelsWon;
    /** Hüttenaufstieg: Leben übrig (0 = raus) und in welchem Minispiel jemand rausflog. */
    int lives;
    int outRound;
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
        return purse.taler;
    }

    public int edelweiss() {
        return purse.edelweiss;
    }

    /** Seilschaft: Nummer der Seilschaft (-1 = keine). */
    public int team() {
        return team;
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

    /** Minispiel-Marathon: Platz-Punkte. */
    public int minigamePoints() {
        return minigamePoints;
    }

    /** Hüttenaufstieg: Leben übrig. */
    public int lives() {
        return lives;
    }

    /** Hüttenaufstieg/Turnier: in welcher Runde jemand rausflog (0 = noch dabei). */
    public int outRound() {
        return outRound;
    }

    /** Hüttenaufstieg: kein Leben mehr (schaut zu)? */
    public boolean out() {
        return outRound > 0;
    }

    public int minigamesWon() {
        return minigamesWon;
    }

    // ---------- Verwaltung (/mparty) und Tests ----------

    public void setPos(int pos) {
        this.pos = pos;
    }

    public void setTaler(int taler) {
        purse.taler = Math.max(0, taler);
    }

    public void setEdelweiss(int edelweiss) {
        purse.edelweiss = Math.max(0, edelweiss);
    }

    public void setLives(int lives) {
        this.lives = Math.max(0, lives);
    }

    public void fixNextRoll(int value) {
        fixed.addLast(value);
    }

    /** Taler ändern, nie unter 0; gibt die echte Änderung zurück. */
    int addTaler(int delta) {
        int before = purse.taler;
        purse.taler = Math.max(0, purse.taler + delta);
        return purse.taler - before;
    }
}
