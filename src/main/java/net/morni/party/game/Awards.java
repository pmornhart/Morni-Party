package net.morni.party.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;

/**
 * Auszeichnungen am Ende einer Brett-Partie, ohne Minecraft: wer in einer Sache vorn liegt, bekommt ein Edelweiß dazu
 * – auch der Pechvogel als Trost. Gleichstand: alle Besten bekommen es (nur wenn der Wert größer als 0 ist).
 */
public final class Awards {

    /** Die Auszeichnungen. */
    public enum Type {
        /** Die meisten Felder gelaufen. */
        WANDERER(p -> p.walked),
        /** Die meisten Minispiele gewonnen. */
        MINISPIEL_MEISTER(p -> p.minigamesWon),
        /** Am öftesten auf roten Feldern (Verlust, Pech) gelandet. */
        PECHVOGEL(p -> p.redFields),
        /** Die meisten Duelle gewonnen. */
        DUELLKOENIG(p -> p.duelsWon);

        private final ToIntFunction<PartyPlayer> value;

        Type(ToIntFunction<PartyPlayer> value) {
            this.value = value;
        }

        public int of(PartyPlayer p) {
            return value.applyAsInt(p);
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT).replace("_", "-");
        }

        public static Type parse(String s) {
            for (Type t : values()) {
                if (t.id().equalsIgnoreCase(s == null ? "" : s.trim())) {
                    return t;
                }
            }
            return null;
        }
    }

    /** Eine vergebene Auszeichnung: wer, mit welchem Wert. */
    public record Award(Type type, List<PartyPlayer> winners, int value) {
    }

    private Awards() {
    }

    /** Wer bekommt was? Nur aktive Leute zählen, Werte 0 bekommen nichts. */
    public static List<Award> pick(List<PartyPlayer> players, List<Type> enabled) {
        List<Award> out = new ArrayList<>();
        for (Type t : enabled) {
            int best = players.stream().mapToInt(t::of).max().orElse(0);
            if (best <= 0) {
                continue;
            }
            List<PartyPlayer> top = players.stream().filter(p -> t.of(p) == best).toList();
            out.add(new Award(t, top, best));
        }
        return out;
    }
}
