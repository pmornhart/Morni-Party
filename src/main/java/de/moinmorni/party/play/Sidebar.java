package de.moinmorni.party.play;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Seitenleiste rechts – je Person eine eigene (jeder liest sie in seiner Sprache). Zeilen ohne rote Zahlen
 * (Paper: leeres Zahlenformat).
 */
public final class Sidebar {

    private static final int MAX_LINES = 15;
    private static final String OBJECTIVE = "mm_party";
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private final Map<UUID, Integer> lineCount = new HashMap<>();

    /** Zeilen für {@code p} setzen (oben nach unten). */
    public void show(Player p, Component title, List<Component> lines) {
        Scoreboard sb = boards.computeIfAbsent(p.getUniqueId(),
                u -> Bukkit.getScoreboardManager().getNewScoreboard());
        Objective o = sb.getObjective(OBJECTIVE);
        if (o == null) {
            o = sb.registerNewObjective(OBJECTIVE, Criteria.DUMMY, title);
            o.setDisplaySlot(DisplaySlot.SIDEBAR);
            o.numberFormat(NumberFormat.blank());
        }
        o.displayName(title);
        int n = Math.min(MAX_LINES, lines.size());
        int before = lineCount.getOrDefault(p.getUniqueId(), 0);
        for (int i = 0; i < n; i++) {
            Score s = o.getScore(entry(i));
            s.setScore(MAX_LINES - i);
            s.customName(lines.get(i));
        }
        for (int i = n; i < before; i++) {
            sb.resetScores(entry(i));
        }
        lineCount.put(p.getUniqueId(), n);
        if (p.getScoreboard() != sb) {
            p.setScoreboard(sb);
        }
    }

    private static String entry(int i) {
        return "§" + Integer.toHexString(i) + "§r";
    }

    /** Leiste weg (zurück zur Haupt-Anzeige). */
    public void hide(Player p) {
        Scoreboard sb = boards.remove(p.getUniqueId());
        lineCount.remove(p.getUniqueId());
        if (sb != null && p.getScoreboard() == sb) {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public void forget(UUID u) {
        boards.remove(u);
        lineCount.remove(u);
    }
}
