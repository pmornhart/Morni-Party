package de.moinmorni.party.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Abstimmung im Wartebereich: eine Stimme pro Person, Gleichstand entscheidet der Zufall. */
public final class Votes<T> {

    private final Map<UUID, T> votes = new HashMap<>();

    public void vote(UUID who, T option) {
        votes.put(who, option);
    }

    public void forget(UUID who) {
        votes.remove(who);
    }

    public T of(UUID who) {
        return votes.get(who);
    }

    /** Stimmen je Möglichkeit (nur von {@code voters}, z. B. wer noch wartet). */
    public Map<T, Integer> count(List<T> options, java.util.Collection<UUID> voters) {
        Map<T, Integer> out = new LinkedHashMap<>();
        options.forEach(o -> out.put(o, 0));
        votes.forEach((u, o) -> {
            if (voters.contains(u) && out.containsKey(o)) {
                out.merge(o, 1, Integer::sum);
            }
        });
        return out;
    }

    /** Gewinner: meiste Stimmen; Gleichstand (auch: keiner hat abgestimmt) → Zufall unter den Besten. */
    public T winner(List<T> options, java.util.Collection<UUID> voters, Random rng) {
        Map<T, Integer> c = count(options, voters);
        int best = c.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<T> top = new ArrayList<>();
        c.forEach((o, n) -> {
            if (n == best) {
                top.add(o);
            }
        });
        return top.get(rng.nextInt(top.size()));
    }

    public void clear() {
        votes.clear();
    }
}
