package de.moinmorni.party.board;

/**
 * Arten von Feldern auf dem Spielbrett. Jede Art hat eine eigene Farbe auf dem Brett (siehe Bauplan) und eine eigene
 * Wirkung beim Draufkommen (siehe {@code game.Engine#land}).
 */
public enum FieldType {
    /** Start im Tal (bzw. am Rand des Stollens). */
    START,
    /** Blau: +Taler. */
    TALER,
    /** Rot: −Taler. */
    VERLUST,
    /** Grün: ein zufälliges gutes Ereignis. */
    GLUECK,
    /** Schwarz: ein zufälliges Pech-Ereignis. */
    PECH,
    /** Orange: Würfelduell gegen jemanden. */
    DUELL,
    /** Gelb: Krämerladen – Gegenstände kaufen. */
    LADEN,
    /** Rosa: ein Gegenstand geschenkt. */
    GEGENSTAND,
    /** Ziel: Gipfel, Turmspitze oder Goldkammer. */
    ZIEL;

    /** Schlüssel für Texte, z. B. „field.taler“. */
    public String key() {
        return "field." + name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Kann der Edelweiß-Stand hier stehen? Nur auf einfachen Feldern. */
    public boolean standAllowed() {
        return this == TALER || this == VERLUST;
    }
}
