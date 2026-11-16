package de.moinmorni.party.game;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Seilschaften bilden, ohne Minecraft: je zwei Leute teilen sich Taler und Edelweiß. Partys bleiben zusammen – eine
 * Party aus zwei ist eine Seilschaft, größere Partys werden der Reihe nach in Paare geteilt, alle anderen werden in der
 * Reihenfolge der Anmeldung zusammengestellt.
 */
public final class Seilschaft {

    /** So viele gehören zu einer Seilschaft. */
    public static final int SIZE = 2;

    private Seilschaft() {
    }

    /**
     * Paare aus den Wartegruppen (Partys und Einzelne, in der Reihenfolge der Anmeldung). Bleibt bei ungerader Zahl
     * einer übrig, steht er allein (die Warteschlange sorgt dafür, dass es nicht vorkommt).
     */
    public static List<List<UUID>> pair(List<List<UUID>> groups) {
        List<List<UUID>> pairs = new ArrayList<>();
        List<UUID> singles = new ArrayList<>();
        for (List<UUID> g : groups) {
            int i = 0;
            for (; i + SIZE <= g.size(); i += SIZE) {
                pairs.add(List.copyOf(g.subList(i, i + SIZE)));
            }
            singles.addAll(g.subList(i, g.size()));
        }
        for (int i = 0; i < singles.size(); i += SIZE) {
            pairs.add(List.copyOf(singles.subList(i, Math.min(singles.size(), i + SIZE))));
        }
        return pairs;
    }

    /**
     * Zugreihenfolge: abwechselnd aus jeder Seilschaft – erst alle Ersten, dann alle Zweiten (A1, B1, A2, B2). Jedes
     * Element ist eine Liste mit genau einer Person (so bleibt die Reihenfolge der Seilschaften erkennbar).
     */
    public static List<List<UUID>> order(List<List<UUID>> teams) {
        List<List<UUID>> out = new ArrayList<>();
        int longest = teams.stream().mapToInt(List::size).max().orElse(0);
        for (int k = 0; k < longest; k++) {
            for (List<UUID> t : teams) {
                if (k < t.size()) {
                    out.add(List.of(t.get(k)));
                }
            }
        }
        return out;
    }

    /** Größte gerade Zahl Leute, die höchstens {@code max} ist. */
    public static int evenMax(int max) {
        return max - Math.floorMod(max, SIZE);
    }
}
