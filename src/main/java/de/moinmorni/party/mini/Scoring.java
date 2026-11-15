package de.moinmorni.party.mini;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Wertung eines Minispiels, ohne Minecraft. Zwei Arten:
 * <ul>
 * <li>{@link Kind#POINTS}: wer die meisten Punkte hat, ist vorn (gleich viele = gleicher Platz);</li>
 * <li>{@link Kind#SURVIVAL}: wer übrig bleibt, ist vorn (alle Übrigen teilen sich Platz 1); wer früher ausscheidet,
 * ist weiter hinten – wer im selben Augenblick ausscheidet, teilt sich den Platz.</li>
 * </ul>
 */
public final class Scoring {

    public enum Kind {
        POINTS, SURVIVAL
    }

    private final Kind kind;
    private final Map<UUID, Integer> points = new LinkedHashMap<>();
    private final Map<UUID, Long> out = new LinkedHashMap<>();

    public Scoring(Kind kind) {
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public void join(UUID u) {
        points.putIfAbsent(u, 0);
    }

    /** Ganz raus (hat das Spiel verlassen): zählt gar nicht mehr. */
    public void drop(UUID u) {
        points.remove(u);
        out.remove(u);
    }

    public boolean has(UUID u) {
        return points.containsKey(u);
    }

    public int points(UUID u) {
        return points.getOrDefault(u, 0);
    }

    public void add(UUID u, int n) {
        if (points.containsKey(u)) {
            points.merge(u, n, Integer::sum);
        }
    }

    public void set(UUID u, int n) {
        if (points.containsKey(u)) {
            points.put(u, n);
        }
    }

    /** Ausgeschieden zum Zeitpunkt {@code tick}. false, wenn schon draußen. */
    public boolean eliminate(UUID u, long tick) {
        if (!points.containsKey(u) || out.containsKey(u)) {
            return false;
        }
        out.put(u, tick);
        return true;
    }

    public boolean alive(UUID u) {
        return points.containsKey(u) && !out.containsKey(u);
    }

    public List<UUID> alive() {
        return points.keySet().stream().filter(u -> !out.containsKey(u)).toList();
    }

    public List<UUID> all() {
        return List.copyOf(points.keySet());
    }

    /** Plätze von vorn; jede Gruppe teilt sich einen Platz. */
    public List<List<UUID>> places() {
        List<UUID> list = new ArrayList<>(points.keySet());
        Comparator<UUID> c;
        if (kind == Kind.POINTS) {
            c = Comparator.comparingInt((UUID u) -> -points.get(u));
        } else {
            // Übrige zuerst (Long.MAX), dann wer später ausgeschieden ist; Punkte entscheiden bei gleicher Zeit nicht.
            c = Comparator.comparingLong((UUID u) -> -out.getOrDefault(u, Long.MAX_VALUE));
        }
        list.sort(c);
        List<List<UUID>> groups = new ArrayList<>();
        for (UUID u : list) {
            if (!groups.isEmpty() && c.compare(groups.getLast().getFirst(), u) == 0) {
                groups.getLast().add(u);
            } else {
                List<UUID> g = new ArrayList<>();
                g.add(u);
                groups.add(g);
            }
        }
        return groups;
    }

    /** Die auf Platz 1. */
    public List<UUID> winners() {
        List<List<UUID>> p = places();
        return p.isEmpty() ? List.of() : p.getFirst();
    }
}
