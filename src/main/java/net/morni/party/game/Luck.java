package net.morni.party.game;

import java.util.List;
import java.util.Locale;

/** Ereignisse auf Glücks- (grün) und Pechfeldern (schwarz). Ein Schild hält ein Pech-Ereignis ab. */
public enum Luck {
    /** Der Almwirt spendiert Taler. */
    GESCHENK(true),
    /** Rückenwind: ein paar Felder vor. */
    RUECKENWIND(true),
    /** Alle anderen geben dir Taler. */
    SAMMELN(true),
    /** Ein Gegenstand liegt am Weg. */
    FUND(true),
    /** Steinschlag: Taler weg. */
    STEINSCHLAG(false),
    /** Nebel: ein paar Felder zurück. */
    NEBEL(false),
    /** Du gibst allen anderen eine Runde aus. */
    SPENDIEREN(false),
    /** Ein Gegenstand fällt aus dem Rucksack. */
    VERLOREN(false);

    public static final List<Luck> GOOD = List.of(GESCHENK, RUECKENWIND, SAMMELN, FUND);
    public static final List<Luck> BAD = List.of(STEINSCHLAG, NEBEL, SPENDIEREN, VERLOREN);

    private final boolean good;

    Luck(boolean good) {
        this.good = good;
    }

    public boolean good() {
        return good;
    }

    public String key() {
        return "luck." + name().toLowerCase(Locale.ROOT);
    }
}
