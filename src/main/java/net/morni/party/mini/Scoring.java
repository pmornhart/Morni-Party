package net.morni.party.mini;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Wertung eines Minispiels, ohne Minecraft. Drei Arten:
 * <ul>
 * <li>{@link Kind#POINTS}: wer die meisten Punkte hat, ist vorn (gleich viele = gleicher Platz);</li>
 * <li>{@link Kind#SURVIVAL}: wer übrig bleibt, ist vorn (alle Übrigen teilen sich Platz 1); wer früher ausscheidet,
 * ist weiter hinten – wer im selben Augenblick ausscheidet, teilt sich den Platz;</li>
 * <li>{@link Kind#RACE}: wer zuerst im Ziel ist, ist vorn (im selben Augenblick = gleicher Platz); wer nicht ankommt,
 * steht dahinter – geordnet nach der geschafften Strecke (die Punkte).</li>
 * </ul>
 */
public final class Scoring {

    public enum Kind {
        POINTS, SURVIVAL, RACE
    }

    private final Kind kind;
    private final Map<UUID, Integer> points = new LinkedHashMap<>();
    private final Map<UUID, Long> out = new LinkedHashMap<>();
    private final Map<UUID, Long> finished = new LinkedHashMap<>();

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
        finished.remove(u);
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

    /** Rennen: im Ziel zum Zeitpunkt {@code tick}. false, wenn schon im Ziel (oder nicht dabei). */
    public boolean finish(UUID u, long tick) {
        if (!points.containsKey(u) || finished.containsKey(u)) {
            return false;
        }
        finished.put(u, tick);
        return true;
    }

    public boolean finished(UUID u) {
        return finished.containsKey(u);
    }

    /** Zeitpunkt der Ankunft, -1 = (noch) nicht im Ziel. */
    public long finishTick(UUID u) {
        return finished.getOrDefault(u, -1L);
    }

    /** So viele sind schon im Ziel. */
    public int finishers() {
        return finished.size();
    }

    /** Alle (noch Dabeiseienden) im Ziel? */
    public boolean allFinished() {
        return !points.isEmpty() && finished.keySet().containsAll(points.keySet());
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
        } else if (kind == Kind.RACE) {
            // Wer ankam, zuerst (früher = besser); dann alle anderen nach Strecke.
            c = Comparator.comparingInt((UUID u) -> finished.containsKey(u) ? 0 : 1)
                    .thenComparingLong(u -> finished.containsKey(u) ? finished.get(u) : -points.get(u));
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

    /**
     * Ergebnis zwischen zwei Seiten (Team, Einer gegen drei, Duell). Punkte: Summe je Seite, die Punkte des Einzelnen
     * zählen {@code soloFactor}-fach. Überleben und Rennen: die Seite mit dem besten Platz gewinnt (gleich = der
     * nächstbeste entscheidet). Wer gegangen ist, zählt nicht; ist eine Seite leer, gewinnt die andere.
     */
    public net.morni.party.game.Teams.Outcome teamOutcome(net.morni.party.game.Teams.Split split,
            int soloFactor) {
        var a = split.a().stream().filter(points::containsKey).toList();
        var b = split.b().stream().filter(points::containsKey).toList();
        if (a.isEmpty() || b.isEmpty()) {
            return a.isEmpty() == b.isEmpty() ? net.morni.party.game.Teams.Outcome.DRAW
                    : a.isEmpty() ? net.morni.party.game.Teams.Outcome.B : net.morni.party.game.Teams.Outcome.A;
        }
        int c;
        if (kind == Kind.POINTS) {
            int factor = split.format() == net.morni.party.game.Teams.Format.SOLO ? Math.max(1, soloFactor) : 1;
            int pa = a.stream().mapToInt(this::points).sum() * factor;
            int pb = b.stream().mapToInt(this::points).sum();
            c = Integer.compare(pb, pa);
        } else {
            Map<UUID, Integer> rank = new LinkedHashMap<>();
            int place = 0;
            for (List<UUID> g : places()) {
                for (UUID u : g) {
                    rank.put(u, place);
                }
                place += g.size();
            }
            List<Integer> ra = a.stream().map(rank::get).sorted().toList();
            List<Integer> rb = b.stream().map(rank::get).sorted().toList();
            c = 0;
            for (int i = 0; i < Math.min(ra.size(), rb.size()) && c == 0; i++) {
                c = Integer.compare(ra.get(i), rb.get(i));
            }
        }
        return c < 0 ? net.morni.party.game.Teams.Outcome.A : c > 0 ? net.morni.party.game.Teams.Outcome.B
                : net.morni.party.game.Teams.Outcome.DRAW;
    }

    /** Die auf Platz 1. */
    public List<UUID> winners() {
        List<List<UUID>> p = places();
        return p.isEmpty() ? List.of() : p.getFirst();
    }
}
