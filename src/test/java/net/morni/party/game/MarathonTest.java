package net.morni.party.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.morni.party.board.BoardId;
import net.morni.party.board.BoardLayout;
import org.junit.jupiter.api.Test;

class MarathonTest {

    @Test
    void placePointsFallAndEveryoneGetsAtLeastTheLast() {
        int[] t = Marathon.DEFAULT_POINTS;
        assertEquals(10, Marathon.points(t, 0));
        assertEquals(7, Marathon.points(t, 1));
        assertEquals(5, Marathon.points(t, 2));
        assertEquals(1, Marathon.points(t, 6));
        assertEquals(1, Marathon.points(t, 15), "Platz 16 bekommt auch noch einen Punkt");
        assertEquals(0, Marathon.points(new int[0], 0));
    }

    @Test
    void everyMinigameOnceBeforeAnyRepeats() {
        List<String> all = List.of("a", "b", "c", "d", "e");
        List<String> bag = new ArrayList<>();
        Random rng = new Random(1);
        Set<String> first = new HashSet<>();
        String last = null;
        for (int i = 0; i < all.size(); i++) {
            last = Marathon.draw(bag, all, last, x -> true, rng);
            first.add(last);
        }
        assertEquals(Set.copyOf(all), first, "erst alle einmal");
        String next = Marathon.draw(bag, all, last, x -> true, rng);
        assertNotEquals(last, next, "das zuletzt gespielte kommt nicht gleich wieder");
    }

    @Test
    void minigamesThatDoNotFitAreSkipped() {
        List<String> all = List.of("klein", "gross1", "gross2");
        List<String> bag = new ArrayList<>();
        Random rng = new Random(2);
        for (int i = 0; i < 10; i++) {
            assertTrue(Marathon.draw(bag, all, null, x -> x.startsWith("gross"), rng).startsWith("gross"));
        }
        assertNull(Marathon.draw(new ArrayList<>(), all, null, x -> false, rng), "keins passt");
    }

    private static List<PartyPlayer> players(int n) {
        List<PartyPlayer> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(new PartyPlayer(UUID.randomUUID(), "P" + i, i));
        }
        return out;
    }

    @Test
    void marathonGivesPlacePointsNotTalerAndRanksByPoints() {
        List<PartyPlayer> ps = players(4);
        Engine e = new Engine(BoardLayout.of(BoardId.ALMSOMMER), Mode.MARATHON, 3, ps, Rules.defaults(), new Random(1));
        int startTaler = ps.getFirst().taler();
        UUID a = ps.get(0).uuid();
        UUID b = ps.get(1).uuid();
        UUID c = ps.get(2).uuid();
        UUID d = ps.get(3).uuid();
        Map<UUID, Integer> got = e.awardMinigame(List.of(List.of(b), List.of(a, c), List.of(d)));
        assertEquals(10, got.get(b));
        assertEquals(7, got.get(a), "Gleichstand: beide bekommen die Punkte des besseren Platzes");
        assertEquals(7, got.get(c));
        assertEquals(4, got.get(d), "Platz 4 (zwei teilen sich Platz 2)");
        assertEquals(startTaler, ps.getFirst().taler(), "im Marathon gibt es keine Taler");
        e.awardMinigame(List.of(List.of(a), List.of(b), List.of(c), List.of(d)));
        // a: 7 + 10 = 17, b: 10 + 7 = 17, c: 7 + 5 = 12, d: 4 + 4 = 8 → a und b gleich, beide je ein Minispiel gewonnen
        assertEquals(17, ps.get(0).minigamePoints());
        assertEquals(17, ps.get(1).minigamePoints());
        assertEquals(List.of(List.of(ps.get(0), ps.get(1)), List.of(ps.get(2)), List.of(ps.get(3))), e.places(),
                "gleich viele Punkte und Siege → gleicher Platz");
        e.nextRound();
        e.nextRound();
        assertFalse(e.over());
        e.nextRound();
        assertTrue(e.over(), "nach drei Minispielen ist Schluss");
    }

    @Test
    void sixteenPlayersAllGetPoints() {
        List<PartyPlayer> ps = players(16);
        Engine e = new Engine(BoardLayout.of(BoardId.ALMSOMMER), Mode.MARATHON, 6, ps, Rules.defaults(), new Random(1));
        List<List<UUID>> places = new ArrayList<>();
        ps.forEach(p -> places.add(List.of(p.uuid())));
        Map<UUID, Integer> got = e.awardMinigame(places);
        assertEquals(16, got.size());
        assertTrue(got.values().stream().allMatch(v -> v >= 1));
        assertEquals(1, ps.get(0).minigamesWon());
    }

    @Test
    void marathonKindsParse() {
        assertEquals(new GameKind(Mode.MARATHON, 10), GameKind.parse("marathon-10"));
        assertEquals("marathon-6", new GameKind(Mode.MARATHON, 6).id());
        assertEquals(new GameKind(Mode.GIPFELRENNEN, 0), GameKind.parse("gipfelrennen"));
        assertNull(GameKind.parse("marathon"), "Marathon braucht eine Zahl");
        assertFalse(Mode.MARATHON.board());
    }

    @Test
    void statsCountGamesAndWinsPerMode() {
        var s = new net.morni.party.data.PlayerStats(UUID.randomUUID(), "Anna");
        s.record("marathon", true, 0, 3, false);
        s.record("marathon", false, 0, 1, false);
        s.record("gipfelrennen", true, 2, 0, true);
        var back = net.morni.party.data.PlayerStats.fromYaml(s.uuid(), s.toYaml());
        assertEquals(2, back.modeGames("marathon"));
        assertEquals(1, back.modeWins("marathon"));
        assertEquals(1, back.modeGames("gipfelrennen"));
        assertEquals(0, back.modeGames("edelweiss-jagd"));
        assertEquals(3, back.games(), "alle Partien zusammen");
        assertEquals(2, back.wins());
    }
}
