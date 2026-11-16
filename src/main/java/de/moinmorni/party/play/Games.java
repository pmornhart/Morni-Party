package de.moinmorni.party.play;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Alle laufenden Partien (eine pro Tisch). */
public final class Games {

    private final List<Game> games = new ArrayList<>();
    private final int tables;

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
