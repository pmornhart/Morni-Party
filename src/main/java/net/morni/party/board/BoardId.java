package net.morni.party.board;

import java.util.Locale;

/** Die drei Spielbretter – alle eigene Entwürfe im Alpen-Stil. */
public enum BoardId {
    /** Almsommer am Morni-Kogel: ein grüner Berg, der Weg windet sich hinauf zum Gipfelkreuz. */
    ALMSOMMER,
    /** Schloss Weißenstein: Schlossgarten, Terrassen und Mauern hinauf zur Turmplattform. */
    SCHLOSS,
    /** Goldstollen: ein offenes Bergwerk, der Weg führt hinab zur Goldkammer. */
    GOLDSTOLLEN,
    /** Königssee: Stege über einem Bergsee, Fähren als Abkürzung, Kapelle auf der Insel als Ziel. */
    KOENIGSSEE,
    /** Wiesn: Festwiese mit Zelten und Fahrgeschäften, das Riesenrad tauscht Plätze. */
    WIESN,
    /** Wintersonnwend: verschneiter Berg mit Eisflächen und Lawinen, oben das Sonnwendfeuer. */
    WINTERSONNWEND;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Aus Text (id oder Anfang), sonst null. */
    public static BoardId parse(String s) {
        if (s == null) {
            return null;
        }
        String l = s.toLowerCase(Locale.ROOT);
        for (BoardId b : values()) {
            if (b.id().equals(l) || (l.length() >= 3 && b.id().startsWith(l))) {
                return b;
            }
        }
        return null;
    }
}
