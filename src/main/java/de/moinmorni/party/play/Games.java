package de.moinmorni.party.play;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;

/** Alle laufenden Partien (eine pro Tisch). */
public final class Games {

    /** So lange erfährt jemand beim nächsten Betreten noch, dass die Partie ohne ihn weiterging. */
    private static final long MISSED_MS = 15 * 60_000L;

    private final List<Game> games = new ArrayList<>();
    private final int tables;
    /** Wo jemand stand, dessen Platz frei ist – Ort beim Betreten (wird außerhalb des Server-Takts gelesen). */
    private final Map<UUID, Location> spots = new ConcurrentHashMap<>();
    /** Wer zu spät kam (Zeitpunkt): Hinweis beim nächsten Betreten. */
    private final Map<UUID, Long> missed = new HashMap<>();

    public Games(int tables) {
        this.tables = tables;
    }

    public void add(Game g) {
        games.add(g);
    }

    public void remove(Game g) {
        games.remove(g);
    }

    public List<Game> all() {
        return List.copyOf(games);
    }

    public int running() {
        return games.size();
    }

    /** Freier Tisch oder -1. */
    public int freeTable() {
        for (int t = 0; t < tables; t++) {
            int table = t;
            if (games.stream().noneMatch(g -> g.table() == table)) {
                return t;
            }
        }
        return -1;
    }

    /** Partie, in der {@code u} mitspielt. */
    public Optional<Game> of(UUID u) {
        return games.stream().filter(g -> g.plays(u)).findFirst();
    }

    /** Partie, der {@code u} zuschaut. */
    public Optional<Game> watching(UUID u) {
        return games.stream().filter(g -> g.watches(u)).findFirst();
    }

    /** Spielt mit oder schaut zu. */
    public Optional<Game> involved(UUID u) {
        return games.stream().filter(g -> g.plays(u) || g.watches(u)).findFirst();
    }

    /** Platz frei: hier stand die Person, als die Verbindung abriss. */
    void holdSpot(UUID u, Location at) {
        spots.put(u, at.clone());
    }

    void releaseSpot(UUID u) {
        spots.remove(u);
    }

    /** Ort beim Betreten für jemanden, dessen Platz frei ist (sonst null). Darf von überall gerufen werden. */
    public Location returnSpot(UUID u) {
        Location l = spots.get(u);
        return l == null ? null : l.clone();
    }

    /** Die Partie ging ohne {@code u} weiter (Frist um oder vorbei). */
    void missed(UUID u) {
        long now = System.currentTimeMillis();
        missed.values().removeIf(at -> now - at > MISSED_MS);
        missed.put(u, now);
    }

    /** Kam {@code u} zu spät zurück? Gibt es nur einmal. */
    public boolean wasMissed(UUID u) {
        Long at = missed.remove(u);
        return at != null && System.currentTimeMillis() - at <= MISSED_MS;
    }

    public void tick() {
        for (Game g : List.copyOf(games)) {
            g.tick();
        }
    }

    public void closeAll() {
        for (Game g : List.copyOf(games)) {
            g.close();
        }
    }
}
