package net.morni.party.game;

import java.util.Locale;

/** Gegenstände: im Laden kaufen, auf rosa Feldern finden, vor dem Würfeln einsetzen (einer pro Zug). */
public enum ItemType {
    /** Zwei Würfel statt einem (2–12). */
    DOPPELWUERFEL(8, false),
    /** Augenzahl 1–6 selbst wählen. */
    ZIELWUERFEL(6, false),
    /** Platz mit jemandem tauschen. */
    TAUSCH(10, true),
    /** Jemandem Taler abnehmen. */
    TALERKLAU(8, true),
    /** Schützt einmal vor Verlust, Pech, Klau und Tausch. */
    SCHILD(5, false),
    /** Bringt dich direkt vor den Edelweiß-Stand. */
    LOCKRUF(12, false);

    private final int price;
    private final boolean needsTarget;

    ItemType(int price, boolean needsTarget) {
        this.price = price;
        this.needsTarget = needsTarget;
    }

    public int price() {
        return price;
    }

    /** Braucht der Gegenstand ein Ziel (jemand anderes)? */
    public boolean needsTarget() {
        return needsTarget;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String key() {
        return "item." + id();
    }

    public static ItemType parse(String s) {
        if (s == null) {
            return null;
        }
        String l = s.toLowerCase(Locale.ROOT);
        for (ItemType t : values()) {
            if (t.id().equals(l) || (l.length() >= 3 && t.id().startsWith(l))) {
                return t;
            }
        }
        return null;
    }
}
