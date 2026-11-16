package de.moinmorni.party.mini;

import de.moinmorni.party.Items;
import de.moinmorni.party.Texts;
import de.moinmorni.party.Ui;
import de.moinmorni.party.world.Plan;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Brückenbau: Jede und jeder hat eine eigene Spur über die Schlucht und einen Stapel Bretter. Bretter an die Kante
 * setzen, draufsteigen, weiterbauen – wer zuerst drüben ist, gewinnt. Wer hinunterfällt, fängt am Start wieder an
 * (die eigene Brücke bleibt stehen).
 */
final class BrueckenBau extends MiniRun {

    /** Höchstens so viele Spuren (mehr Leute: Minispiel fällt im Marathon aus). */
    static final int LANES = 8;
    static final int LANE_WIDTH = 4;
    /** Startkante (Boden bis hier), Schlucht dazwischen, Zielufer ab hier. */
    static final int EDGE_Z = -3;
    static final int FAR_Z = 12;
    static final int START_Z = -8;
    static final int END_Z = 17;
    static final int SIDE = LANES * LANE_WIDTH / 2;
    private static final int PLANKS = 64;
    private static final int FALL = 4;

    private final Map<UUID, Integer> lane = new HashMap<>();
    private final List<Block> placed = new ArrayList<>();

    BrueckenBau(Setup s) {
        super(s);
    }

    /** Linke Kante (x) der Spur i; die Spur ist x .. x + 2, daneben eine unsichtbare Wand. */
    static int laneLeft(int i) {
        return -SIDE + 1 + i * LANE_WIDTH;
    }

    /** Spuren für n Leute, mittig verteilt. */
    static int laneOf(int i, int n) {
        return (LANES - Math.min(n, LANES)) / 2 + i;
    }

    static void build(Plan p) {
        for (int x = -SIDE; x <= SIDE; x++) {
            boolean wall = Math.floorMod(x + SIDE, LANE_WIDTH) == 0;
            for (int z = START_Z - 1; z <= END_Z + 1; z++) {
                boolean ground = z <= EDGE_Z || z >= FAR_Z;
                if (ground) {
                    p.set(x, -1, z, "stone");
                    p.set(x, 0, z, z == EDGE_Z || z == FAR_Z ? "stripped_spruce_log[axis=x]"
                            : z > FAR_Z ? "grass_block[snowy=false]" : "coarse_dirt");
                }
                if (wall || z == START_Z - 1 || z == END_Z + 1) {
                    for (int y = ground ? 1 : -6; y <= 4; y++) {
                        p.set(x, y, z, "barrier");
                    }
                }
            }
        }
        // Farbige Startfelder je Spur, Fähnchen am Ziel
        String[] colours = {"red", "orange", "yellow", "lime", "light_blue", "blue", "magenta", "pink"};
        for (int i = 0; i < LANES; i++) {
            for (int x = laneLeft(i); x < laneLeft(i) + LANE_WIDTH - 1; x++) {
                p.set(x, 0, START_Z + 1, colours[i] + "_wool");
                p.set(x, 0, END_Z - 1, colours[i] + "_wool");
            }
        }
    }

    @Override
    protected GameMode gameMode() {
        return GameMode.SURVIVAL;
    }

    @Override
    public void prepare() {
        // Bretter vom letzten Mal weg (auch nach einem Absturz des Servers)
        for (int x = -SIDE; x <= SIDE; x++) {
            for (int z = EDGE_Z + 1; z < FAR_Z; z++) {
                for (int y = -1; y <= 2; y++) {
                    Block b = block(x, y, z);
                    if (b.getType() != Material.AIR && b.getType() != Material.BARRIER) {
                        b.setType(Material.AIR, false);
                    }
                }
            }
        }
        super.prepare();
    }

    @Override
    protected List<Location> spawns(int n) {
        List<Location> out = new ArrayList<>();
        for (int i = 0; i < Math.max(1, n); i++) {
            out.add(start(laneOf(i, n)));
        }
        return out;
    }

    private Location start(int laneIdx) {
        double x = laneLeft(laneIdx) + 1.5;
        return facing(x, 1, START_Z + 3.5, x, 0, FAR_Z);
    }

    @Override
    public Location viewPoint() {
        return facing(0.5, 14, START_Z - 10, 0.5, 0, (EDGE_Z + FAR_Z) / 2.0);
    }

    @Override
    protected int fallLimit() {
        return FALL;
    }

    @Override
    protected void equip(Player p) {
        p.getInventory().setItem(0, Items.named(Material.OAK_PLANKS, PLANKS, Texts.text(plugin.lang(p),
                "mini.brueckenbau.item"), null));
    }

    @Override
    protected void onStart() {
        for (int i = 0; i < players.size(); i++) {
            lane.put(players.get(i).getUniqueId(), laneOf(i, players.size()));
        }
    }

    @Override
    protected void fell(Player p) {
        p.teleport(start(lane.getOrDefault(p.getUniqueId(), 0)));
        p.setFallDistance(0);
        Ui.sound(p, "minecraft:entity.player.splash", 0.8f);
        bar(p, "mini.brueckenbau.fell");
    }

    @Override
    protected void onTick() {
        if (ticks % SECOND == 0) {
            for (Player p : alivePlayers()) {
                var item = p.getInventory().getItem(0);
                if (item == null || item.getType() != Material.OAK_PLANKS || item.getAmount() < PLANKS / 2) {
                    equip(p);
                }
            }
        }
        if (ticks % 2 == 0) {
            for (Player p : alivePlayers()) {
                Location l = p.getLocation();
                double z = l.getZ() - oz;
                if (z >= FAR_Z && l.getY() >= oy + 0.9) {
                    arrive(p, "mini.race.finish");
                } else {
                    progress(p, (int) Math.floor(Math.max(0, z - EDGE_Z)));
                }
            }
        }
    }

    @Override
    public void onPlace(Player p, BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        int x = b.getX() - ox;
        int z = b.getZ() - oz;
        int myLane = lane.getOrDefault(p.getUniqueId(), -1);
        boolean inLane = myLane >= 0 && x >= laneLeft(myLane) && x < laneLeft(myLane) + LANE_WIDTH - 1;
        boolean ok = b.getType() == Material.OAK_PLANKS && inLane && b.getY() == oy && z > EDGE_Z && z < FAR_Z;
        if (!ok) {
            e.setCancelled(true);
            return;
        }
        placed.add(b);
        Ui.sound(p, "minecraft:block.wood.place", 1.1f);
    }

    @Override
    protected void onFinish() {
        placed.forEach(b -> b.setType(Material.AIR, false));
        placed.clear();
    }

    /** Für die Wertung „… Blöcke weit“ zählt die Strecke über die Schlucht. */
    @Override
    protected String wayKey() {
        return "mini.line.way";
    }
}
