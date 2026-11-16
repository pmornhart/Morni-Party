package net.morni.party.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * K.-o.-Turnierbaum, ohne Minecraft. Die Teilnehmenden werden gemischt und auf die nächste Zweierpotenz aufgefüllt
 * (Freilos = {@code null}); Freilose werden so verteilt, dass niemand gegen ein Freilos und dann gleich wieder gegen
 * eins spielt (sie stehen jeweils an zweiter Stelle eines Paars). Gespielt wird Runde für Runde, Paar für Paar. Wer
 * verliert, ist raus; wer das Finale gewinnt, gewinnt das Turnier.
 */
public final class Bracket {

    /** Ein Spiel im Baum: {@code b == null} ist ein Freilos (a kommt kampflos weiter). */
    public record Match(int round, int index, UUID a, UUID b) {
        public boolean bye() {
            return a == null || b == null;
        }
    }

    private final List<List<UUID>> rounds = new ArrayList<>();
    private final List<List<UUID>> winners = new ArrayList<>();
    private int round;
    private int index;

    public Bracket(List<UUID> players, Random rng) {
        if (players.size() < 2) {
            throw new IllegalArgumentException("mindestens zwei");
        }
        List<UUID> seeded = new ArrayList<>(players);
        Collections.shuffle(seeded, rng);
        int size = 1;
        while (size < seeded.size()) {
            size *= 2;
        }
        int byes = size - seeded.size();
        List<UUID> first = new ArrayList<>();
        int k = 0;
        for (int pair = 0; pair < size / 2; pair++) {
            first.add(seeded.get(k++));
            // die ersten {@code byes} Paare bekommen ein Freilos
            first.add(pair < byes ? null : seeded.get(k++));
        }
        rounds.add(first);
        winners.add(new ArrayList<>());
        skipByes();
    }

    /** So viele Runden hat der Baum (2 Leute: 1, 3–4: 2, 5–8: 3, 9–16: 4). */
    public int roundCount() {
        int n = rounds.getFirst().size();
        int r = 0;
        while (n > 1) {
            n /= 2;
            r++;
        }
        return r;
    }

    /** Aktuelle Runde (0 = erste). */
    public int round() {
        return round;
    }

    /** Wie viele Runden noch kommen (1 = Finale, 2 = Halbfinale …), für den Namen der Runde. */
    public int left() {
        return roundCount() - round;
    }

    public boolean done() {
        return round >= roundCount();
    }

    /** Nächstes Spiel (ohne Freilose), oder null, wenn das Turnier vorbei ist. */
    public Match next() {
        if (done()) {
            return null;
        }
        List<UUID> r = rounds.get(round);
        return new Match(round, index, r.get(2 * index), r.get(2 * index + 1));
    }

    /** Ergebnis des aktuellen Spiels melden. {@code winner} muss einer der beiden sein. */
    public void report(UUID winner) {
        Match m = next();
        if (m == null || m.bye() || !(winner.equals(m.a()) || winner.equals(m.b()))) {
            throw new IllegalArgumentException("kein passendes Spiel");
        }
        resolve(winner);
        skipByes();
    }

    /** Jemand hat das Turnier verlassen: wer gegen ihn spielen müsste, kommt kampflos weiter. */
    public void forfeit(UUID gone) {
        if (done()) {
            return;
        }
        Collections.replaceAll(rounds.get(round), gone, null);
        Collections.replaceAll(winners.get(round), gone, null);
        skipByes();
    }

    /** Ein Spiel ist entschieden ({@code through} darf null sein: beide fehlen). */
    private void resolve(UUID through) {
        winners.get(round).add(through);
        index++;
        if (2 * index >= rounds.get(round).size()) {
            rounds.add(new ArrayList<>(winners.get(round)));
            winners.add(new ArrayList<>());
            round++;
            index = 0;
        }
    }

    private void skipByes() {
        while (!done()) {
            Match m = next();
            if (!m.bye()) {
                return;
            }
            resolve(m.a() != null ? m.a() : m.b());
        }
    }

    /** Sieger des Turniers (null, solange es läuft – oder wenn alle gegangen sind). */
    public UUID champion() {
        return done() ? rounds.get(round).getFirst() : null;
    }

    /** Teilnehmende einer Runde in Paaren (null = Freilos/noch offen), für die Tafel. */
    public List<List<UUID>> rounds() {
        List<List<UUID>> out = new ArrayList<>();
        for (List<UUID> r : rounds) {
            out.add(Collections.unmodifiableList(new ArrayList<>(r)));
        }
        return out;
    }

    /** Hat {@code u} das Spiel in Runde {@code r} gewonnen? */
    public boolean won(int r, UUID u) {
        return r < winners.size() && winners.get(r).contains(u);
    }
}
