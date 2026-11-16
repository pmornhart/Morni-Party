package de.moinmorni.party.game;

import java.util.Locale;

/** Fahrgeschäfte der Wiesn (Ereignis auf dem Fahrgeschäft-Feld). */
public enum Ride {
    /** Achterbahn: vier Felder vor. */
    ACHTERBAHN,
    /** Autoscooter: wer bis zu drei Felder hinter dir steht, wird angerempelt und gibt dir Taler. */
    AUTOSCOOTER,
    /** Kettenkarussell: es schleudert dich zufällig vor oder zurück. */
    KETTENKARUSSELL,
    /** Losbude: ein Gegenstand oder Taler. */
    LOSBUDE;

    public String key() {
        return "ride." + name().toLowerCase(Locale.ROOT);
    }
}
