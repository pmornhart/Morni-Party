package de.moinmorni.party.game;

import de.moinmorni.party.board.BoardLayout;
import de.moinmorni.party.board.FieldType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Die Spielregeln des Brettspiels, ohne Minecraft: Würfeln, Ziehen, Felder, Edelweiß-Stand, Laden, Gegenstände,
 * Würfelduell, Taler aus den Minispielen, Runden und Sieg. Die Spielrunde im Spiel ({@code play.Game}) ruft diese
 * Schritte auf und zeigt sie an.
 */
public final class Engine {

    public static final int DIE = 6;

    /** Ein Wurf: einzelne Würfel und Summe. */
    public record Roll(int[] dice, int total) {
    }

    /** Ein Schritt auf dem Weg. */
    public record Step(int field, boolean stand, boolean wrapped, boolean goal) {
    }

    /** Was beim Draufkommen passiert. */
    public enum Kind {
        NONE, TALER, LUCK, ITEM, DUEL, SHOP, GOAL
    }

    /**
     * Ergebnis eines Felds.
     *
     * @param taler   Änderung der eigenen Taler
     * @param steps   Felder vor (positiv) oder zurück (negativ), die die Spielrunde noch ziehen muss
     * @param blocked Schild hat etwas abgehalten
     * @param others  Taler-Änderung bei den anderen (Sammeln/Spendieren)
     */
    public record Outcome(FieldType field, Kind kind, int taler, int steps, ItemType item, Luck luck, boolean blocked,
            Map<UUID, Integer> others) {
    }

    /** Würfelduell. {@code winner} null = unentschieden. */
    public record Duel(UUID challenger, UUID opponent, int challengerRoll, int opponentRoll, UUID winner, int taler,
            boolean blocked) {
    }

    /** Ergebnis eines Gegenstands. */
    public record ItemUse(ItemType item, boolean ok, UUID target, int amount, boolean blocked, String problem) {
    }

    private final BoardLayout board;
    private final Mode mode;
    private final int rounds;
    private final Rules rules;
    private final Random rng;
    private final List<PartyPlayer> players;
    private int round = 1;
    private int turn;
    private int stand;
    private boolean over;
    private int finishedCount;

    public Engine(BoardLayout board, Mode mode, int rounds, List<PartyPlayer> order, Rules rules, Random rng) {
        if (order.isEmpty()) {
            throw new IllegalArgumentException("niemand spielt");
        }
        this.board = board;
        this.mode = mode;
        this.rounds = mode == Mode.GIPFELRENNEN ? rules.raceMaxRounds() : rounds;
        this.rules = rules;
        this.rng = rng;
        this.players = new ArrayList<>(order);
        for (PartyPlayer p : players) {
            p.pos = 0;
            p.taler = rules.startTaler();
        }
        List<Integer> spots = board.standFields();
        // Der erste Stand steht nicht gleich am Anfang (nicht in den ersten 6 Feldern).
        List<Integer> far = spots.stream().filter(i -> i > DIE).toList();
        stand = (far.isEmpty() ? spots : far).get(rng.nextInt((far.isEmpty() ? spots : far).size()));
    }

    // ---------- Zustand ----------

    public BoardLayout board() {
        return board;
    }

    public Mode mode() {
        return mode;
    }

    public Rules rules() {
        return rules;
    }

    /** Rundenzahl (Gipfelrennen: Höchstzahl). */
    public int rounds() {
        return rounds;
    }

    public int round() {
        return round;
    }

    public int stand() {
        return stand;
    }

    public boolean over() {
        return over;
    }

    public List<PartyPlayer> players() {
        return List.copyOf(players);
    }

    public List<PartyPlayer> active() {
        return players.stream().filter(p -> !p.left).toList();
    }

    public Optional<PartyPlayer> player(UUID uuid) {
        return players.stream().filter(p -> p.uuid().equals(uuid)).findFirst();
    }

    /** Wer gerade dran ist. */
    public PartyPlayer current() {
        return players.get(turn);
    }

    // ---------- Würfeln und Ziehen ----------

    /** Würfeln: feste Werte (Verwaltung), Zielwürfel, Doppelwürfel oder ein normaler Würfel. */
    public Roll roll(PartyPlayer p) {
        Roll r;
        if (!p.fixed.isEmpty()) {
            int v = p.fixed.pollFirst();
            r = new Roll(new int[] {v}, v);
        } else if (p.targetDice > 0) {
            r = new Roll(new int[] {p.targetDice}, p.targetDice);
        } else if (p.doubleDice) {
            int a = die();
            int b = die();
            r = new Roll(new int[] {a, b}, a + b);
        } else {
            int a = die();
            r = new Roll(new int[] {a}, a);
        }
        p.targetDice = 0;
        p.doubleDice = false;
        return r;
    }

    private int die() {
        return 1 + rng.nextInt(DIE);
    }

    /**
     * Ein Feld weiter. Gipfelrennen: am Ziel ist Schluss (Sieg). Edelweiß-Jagd: über das Ziel hinaus geht es mit der
     * Seilbahn zurück auf Feld 1 (mit Gipfel-Bonus).
     */
    public Step step(PartyPlayer p) {
        if (p.pos == board.goal()) {
            if (mode == Mode.GIPFELRENNEN) {
                return new Step(p.pos, false, false, true);
            }
            p.pos = 1;
            p.addTaler(rules.summitBonus());
            return new Step(p.pos, p.pos == stand, true, false);
        }
        p.pos++;
        boolean goal = p.pos == board.goal();
        if (goal && mode == Mode.GIPFELRENNEN) {
            finish(p);
        }
        return new Step(p.pos, p.pos == stand, false, goal);
    }

    /** Ein Feld zurück (nie vor den Start). */
    public int stepBack(PartyPlayer p) {
        if (p.pos > 0) {
            p.pos--;
        }
        return p.pos;
    }

    private void finish(PartyPlayer p) {
        if (p.finished == 0) {
            p.finished = ++finishedCount;
        }
        over = true;
    }

    // ---------- Edelweiß-Stand ----------

    public boolean canBuyEdelweiss(PartyPlayer p) {
        return p.taler >= rules.edelweissPrice();
    }

    /** Ein Edelweiß kaufen; danach zieht der Stand weiter. */
    public boolean buyEdelweiss(PartyPlayer p) {
        if (!canBuyEdelweiss(p)) {
            return false;
        }
        p.taler -= rules.edelweissPrice();
        p.edelweiss++;
        moveStand();
        return true;
    }

    /** Stand an ein anderes einfaches Feld stellen (nicht dorthin, wo er war, und nicht dorthin, wo jemand steht). */
    public int moveStand() {
        List<Integer> spots = new ArrayList<>(board.standFields());
        spots.remove(Integer.valueOf(stand));
        List<Integer> free = spots.stream().filter(i -> active().stream().noneMatch(p -> p.pos == i)).toList();
        List<Integer> pick = free.isEmpty() ? spots : free;
        if (!pick.isEmpty()) {
            stand = pick.get(rng.nextInt(pick.size()));
        }
        return stand;
    }

    /** Verwaltung/Tests: Stand auf ein bestimmtes Feld. */
    public void setStand(int field) {
        if (field > 0 && field < board.goal()) {
            stand = field;
        }
    }

    // ---------- Felder ----------

    /** Wirkung des Felds, auf dem {@code p} gerade steht. */
    public Outcome land(PartyPlayer p) {
        FieldType type = board.field(p.pos).type();
        return switch (type) {
            case TALER -> new Outcome(type, Kind.TALER, p.addTaler(rules.fieldPlus()), 0, null, null, false, Map.of());
            case VERLUST -> {
                if (consumeShield(p)) {
                    yield new Outcome(type, Kind.TALER, 0, 0, null, null, true, Map.of());
                }
                yield new Outcome(type, Kind.TALER, p.addTaler(-rules.fieldMinus()), 0, null, null, false, Map.of());
            }
            case GLUECK -> luck(p, Luck.GOOD.get(rng.nextInt(Luck.GOOD.size())));
            case PECH -> luck(p, Luck.BAD.get(rng.nextInt(Luck.BAD.size())));
            case GEGENSTAND -> {
                ItemType item = randomItem();
                if (giveItem(p, item)) {
                    yield new Outcome(type, Kind.ITEM, 0, 0, item, null, false, Map.of());
                }
                // Rucksack voll: stattdessen Taler.
                yield new Outcome(type, Kind.TALER, p.addTaler(rules.fieldPlus()), 0, null, null, false, Map.of());
            }
            case DUELL -> new Outcome(type, active().size() > 1 ? Kind.DUEL : Kind.NONE, 0, 0, null, null, false,
                    Map.of());
            case LADEN -> new Outcome(type, Kind.SHOP, 0, 0, null, null, false, Map.of());
            case ZIEL -> new Outcome(type, Kind.GOAL, 0, 0, null, null, false, Map.of());
            case START -> new Outcome(type, Kind.NONE, 0, 0, null, null, false, Map.of());
        };
    }

    /** Ein Glücks- oder Pech-Ereignis anwenden (öffentlich für Tests). */
    public Outcome luck(PartyPlayer p, Luck luck) {
        FieldType type = board.field(p.pos).type();
        if (!luck.good() && consumeShield(p)) {
            return new Outcome(type, Kind.LUCK, 0, 0, null, luck, true, Map.of());
        }
        Map<UUID, Integer> others = new LinkedHashMap<>();
        return switch (luck) {
            case GESCHENK -> new Outcome(type, Kind.LUCK, p.addTaler(rules.luckTaler()), 0, null, luck, false, Map.of());
            case RUECKENWIND -> new Outcome(type, Kind.LUCK, 0, rules.luckSteps(), null, luck, false, Map.of());
            case SAMMELN -> {
                int sum = 0;
                for (PartyPlayer o : active()) {
                    if (o != p) {
                        int paid = -o.addTaler(-rules.shareTaler());
                        others.put(o.uuid(), -paid);
                        sum += paid;
                    }
                }
                p.addTaler(sum);
                yield new Outcome(type, Kind.LUCK, sum, 0, null, luck, false, others);
            }
            case FUND -> {
                ItemType item = randomItem();
                boolean got = giveItem(p, item);
                yield new Outcome(type, Kind.LUCK, got ? 0 : p.addTaler(rules.fieldPlus()), 0, got ? item : null, luck,
                        false, Map.of());
            }
            case STEINSCHLAG -> new Outcome(type, Kind.LUCK, p.addTaler(-rules.luckTaler()), 0, null, luck, false,
                    Map.of());
            case NEBEL -> new Outcome(type, Kind.LUCK, 0, -rules.luckSteps(), null, luck, false, Map.of());
            case SPENDIEREN -> {
                int sum = 0;
                for (PartyPlayer o : active()) {
                    if (o != p && p.taler >= rules.shareTaler()) {
                        p.addTaler(-rules.shareTaler());
                        o.addTaler(rules.shareTaler());
                        others.put(o.uuid(), rules.shareTaler());
                        sum += rules.shareTaler();
                    }
                }
                yield new Outcome(type, Kind.LUCK, -sum, 0, null, luck, false, others);
            }
            case VERLOREN -> {
                ItemType lost = p.items.isEmpty() ? null : p.items.remove(rng.nextInt(p.items.size()));
                yield new Outcome(type, Kind.LUCK, lost == null ? p.addTaler(-rules.fieldMinus()) : 0, 0, lost, luck,
                        false, Map.of());
            }
        };
    }

    private boolean consumeShield(PartyPlayer p) {
        if (p.shield) {
            p.shield = false;
            return true;
        }
        return false;
    }

    private ItemType randomItem() {
        ItemType[] all = ItemType.values();
        return all[rng.nextInt(all.length)];
    }

    // ---------- Würfelduell ----------

    /** Beide würfeln; wer höher würfelt, bekommt Taler vom anderen (Schild schützt den Verlierer). */
    public Duel duel(PartyPlayer challenger, PartyPlayer opponent) {
        int a = roll(challenger).total();
        int b = roll(opponent).total();
        if (a == b) {
            return new Duel(challenger.uuid(), opponent.uuid(), a, b, null, 0, false);
        }
        PartyPlayer win = a > b ? challenger : opponent;
        PartyPlayer lose = a > b ? opponent : challenger;
        if (consumeShield(lose)) {
            return new Duel(challenger.uuid(), opponent.uuid(), a, b, win.uuid(), 0, true);
        }
        int paid = -lose.addTaler(-rules.duelStake());
        win.addTaler(paid);
        return new Duel(challenger.uuid(), opponent.uuid(), a, b, win.uuid(), paid, false);
    }

    /**
     * Duell-Minispiel entschieden: der Sieger bekommt den Einsatz vom Verlierer (Schild schützt den Verlierer).
     * {@code winner} = null: unentschieden, niemand zahlt.
     */
    public Duel settleDuel(PartyPlayer challenger, PartyPlayer opponent, UUID winner) {
        if (winner == null || !(winner.equals(challenger.uuid()) || winner.equals(opponent.uuid()))) {
            return new Duel(challenger.uuid(), opponent.uuid(), 0, 0, null, 0, false);
        }
        PartyPlayer win = winner.equals(challenger.uuid()) ? challenger : opponent;
        PartyPlayer lose = win == challenger ? opponent : challenger;
        if (consumeShield(lose)) {
            return new Duel(challenger.uuid(), opponent.uuid(), 0, 0, win.uuid(), 0, true);
        }
        int paid = -lose.addTaler(-rules.duelStake());
        win.addTaler(paid);
        return new Duel(challenger.uuid(), opponent.uuid(), 0, 0, win.uuid(), paid, false);
    }

    // ---------- Laden und Gegenstände ----------

    public boolean canBuyItem(PartyPlayer p, ItemType item) {
        return p.taler >= item.price() && p.items.size() < rules.maxItems();
    }

    public boolean buyItem(PartyPlayer p, ItemType item) {
        if (!canBuyItem(p, item)) {
            return false;
        }
        p.taler -= item.price();
        p.items.add(item);
        return true;
    }

    /** Gegenstand in den Rucksack (false, wenn er voll ist). */
    public boolean giveItem(PartyPlayer p, ItemType item) {
        if (p.items.size() >= rules.maxItems()) {
            return false;
        }
        p.items.add(item);
        return true;
    }

    /**
     * Gegenstand einsetzen (vor dem Würfeln, einer pro Zug). {@code target} für Tausch und Talerklau, {@code number}
     * für den Zielwürfel.
     */
    public ItemUse useItem(PartyPlayer p, ItemType item, UUID target, int number) {
        if (p.itemUsed) {
            return new ItemUse(item, false, null, 0, false, "used");
        }
        if (!p.items.contains(item)) {
            return new ItemUse(item, false, null, 0, false, "missing");
        }
        PartyPlayer other = null;
        if (item.needsTarget()) {
            other = target == null ? null : player(target).filter(o -> !o.left && o != p).orElse(null);
            if (other == null) {
                return new ItemUse(item, false, null, 0, false, "target");
            }
        }
        if (item == ItemType.ZIELWUERFEL && (number < 1 || number > DIE)) {
            return new ItemUse(item, false, null, 0, false, "number");
        }
        if (item == ItemType.SCHILD && p.shield) {
            return new ItemUse(item, false, null, 0, false, "shield");
        }
        p.items.remove(item);
        p.itemUsed = true;
        return switch (item) {
            case DOPPELWUERFEL -> {
                p.doubleDice = true;
                yield new ItemUse(item, true, null, 0, false, null);
            }
            case ZIELWUERFEL -> {
                p.targetDice = number;
                yield new ItemUse(item, true, null, number, false, null);
            }
            case SCHILD -> {
                p.shield = true;
                yield new ItemUse(item, true, null, 0, false, null);
            }
            case LOCKRUF -> {
                p.pos = Math.max(0, stand - 1);
                yield new ItemUse(item, true, null, p.pos, false, null);
            }
            case TAUSCH -> {
                if (consumeShield(other)) {
                    yield new ItemUse(item, true, other.uuid(), 0, true, null);
                }
                int mine = p.pos;
                p.pos = other.pos;
                other.pos = mine;
                yield new ItemUse(item, true, other.uuid(), 0, false, null);
            }
            case TALERKLAU -> {
                if (consumeShield(other)) {
                    yield new ItemUse(item, true, other.uuid(), 0, true, null);
                }
                int taken = -other.addTaler(-rules.steal());
                p.addTaler(taken);
                yield new ItemUse(item, true, other.uuid(), taken, false, null);
            }
        };
    }

    // ---------- Züge und Runden ----------

    /**
     * Zug beenden: weiter zum Nächsten. {@code true}, wenn damit alle einmal dran waren (Runde fertig → Minispiel).
     */
    public boolean endTurn() {
        current().itemUsed = false;
        if (active().isEmpty()) {
            return true;
        }
        int n = players.size();
        for (int i = 1; i <= n; i++) {
            int next = turn + i;
            if (next >= n) {
                // Runde vorbei: beim ersten Aktiven weiter
                turn = firstActive();
                return true;
            }
            if (!players.get(next).left) {
                turn = next;
                return false;
            }
        }
        return true;
    }

    private int firstActive() {
        for (int i = 0; i < players.size(); i++) {
            if (!players.get(i).left) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Taler aus dem Minispiel verteilen (im Marathon: Platz-Punkte). {@code places}: Plätze von vorn, jede Gruppe teilt
     * sich einen Platz (Gleichstand). Gibt die Taler bzw. Punkte je Person zurück.
     */
    public Map<UUID, Integer> awardMinigame(List<List<UUID>> places) {
        Map<UUID, Integer> got = new LinkedHashMap<>();
        int place = 0;
        for (List<UUID> group : places) {
            int amount = mode == Mode.MARATHON ? Marathon.points(rules.marathonPoints(), place) : rules.award(place);
            for (UUID u : group) {
                Optional<PartyPlayer> p = player(u).filter(x -> !x.left);
                if (p.isPresent()) {
                    if (mode == Mode.MARATHON) {
                        p.get().minigamePoints += amount;
                        got.put(u, amount);
                    } else {
                        got.put(u, p.get().addTaler(amount));
                    }
                    if (place == 0) {
                        p.get().minigamesWon++;
                    }
                }
            }
            place += group.size();
        }
        return got;
    }

    /**
     * Taler aus einem Team-Minispiel (2 gegen 2 oder 1 gegen 3): die siegreiche Seite bekommt je {@code teamWin},
     * der Einzelne als Sieger {@code soloWin}; bei Gleichstand alle {@code teamDraw}. Sieger zählen als
     * Minispiel-Sieg. Gibt die Taler je Person zurück (auch 0 für die Verlierer).
     */
    public Map<UUID, Integer> awardTeams(Teams.Split split, Teams.Outcome outcome) {
        Map<UUID, Integer> got = new LinkedHashMap<>();
        List<UUID> all = new ArrayList<>(split.a());
        all.addAll(split.b());
        List<UUID> winners = split.winners(outcome);
        for (UUID u : all) {
            Optional<PartyPlayer> p = player(u).filter(x -> !x.left);
            if (p.isEmpty()) {
                continue;
            }
            int amount;
            if (outcome == Teams.Outcome.DRAW) {
                amount = rules.teamDraw();
            } else if (!winners.contains(u)) {
                amount = 0;
            } else {
                amount = split.format() == Teams.Format.SOLO && split.a().contains(u) ? rules.soloWin() : rules.teamWin();
                p.get().minigamesWon++;
            }
            got.put(u, p.get().addTaler(amount));
        }
        return got;
    }

    /** Nächste Runde (nach dem Minispiel). Danach prüfen, ob das Spiel vorbei ist. */
    public void nextRound() {
        round++;
        if (round > rounds) {
            over = true;
        }
    }

    /** Jemand ist gegangen. Ist danach weniger als zwei übrig, ist das Spiel vorbei. */
    public void remove(UUID uuid) {
        player(uuid).ifPresent(p -> p.left = true);
        if (active().size() < 2) {
            over = true;
        }
    }

    /** Spiel sofort beenden (Verwaltung). */
    public void stop() {
        over = true;
    }

    // ---------- Wertung ----------

    private Comparator<PartyPlayer> order() {
        Comparator<PartyPlayer> byEdelweiss = Comparator.comparingInt((PartyPlayer p) -> -p.edelweiss);
        Comparator<PartyPlayer> byTaler = Comparator.comparingInt((PartyPlayer p) -> -p.taler);
        if (mode == Mode.GIPFELRENNEN) {
            return Comparator.comparingInt((PartyPlayer p) -> p.finished == 0 ? Integer.MAX_VALUE : p.finished)
                    .thenComparingInt(p -> -p.pos).thenComparing(byEdelweiss).thenComparing(byTaler);
        }
        if (mode == Mode.MARATHON) {
            return Comparator.comparingInt((PartyPlayer p) -> -p.minigamePoints).thenComparingInt(p -> -p.minigamesWon);
        }
        return byEdelweiss.thenComparing(byTaler);
    }

    /** Wer gewonnen hat und wie die Plätze sind; Gleichstand teilt sich einen Platz. Gegangene zählen nicht. */
    public List<List<PartyPlayer>> places() {
        List<PartyPlayer> sorted = new ArrayList<>(active());
        Comparator<PartyPlayer> c = order();
        sorted.sort(c);
        List<List<PartyPlayer>> out = new ArrayList<>();
        for (PartyPlayer p : sorted) {
            if (!out.isEmpty() && c.compare(out.getLast().getFirst(), p) == 0) {
                out.getLast().add(p);
            } else {
                List<PartyPlayer> g = new ArrayList<>();
                g.add(p);
                out.add(g);
            }
        }
        return out;
    }

    /** Platz je Person (1 = vorn), für die Seitenleiste. */
    public Map<UUID, Integer> placeOf() {
        Map<UUID, Integer> out = new HashMap<>();
        int place = 1;
        for (List<PartyPlayer> g : places()) {
            for (PartyPlayer p : g) {
                out.put(p.uuid(), place);
            }
            place += g.size();
        }
        return out;
    }
}
