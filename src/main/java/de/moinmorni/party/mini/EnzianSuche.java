package de.moinmorni.party.mini;

import de.moinmorni.party.Items;
import de.moinmorni.party.Texts;
import de.moinmorni.party.world.Plan;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Enzian-Suche: Auf der bunten Almwiese wachsen blaue Enziane – versteckt im hohen Gras, hinter Felsen und Büschen.
 * Finden und pflücken (Schlag oder Rechtsklick), jeder ist ein Punkt. Gepflückte wachsen woanders nach.
 */
final class EnzianSuche extends MiniRun {

    static final int HALF = EnzianWiese.HALF;
    private static final int REGROW = SECOND;
    private static final int PICK_GAP = 4;

    private final Set<Integer> growing = new HashSet<>();
    private final List<Integer> waiting = new ArrayList<>();
    private final Map<UUID, Integer> lastPick = new HashMap<>();
    private final List<int[]> spots = EnzianWiese.spots();

    EnzianSuche(Setup s) {
        super(s);
    }

    static void build(Plan p) {
        for (int x = -HALF - 1; x <= HALF + 1; x++) {
            for (int z = -HALF - 1; z <= HALF + 1; z++) {
                boolean edge = Math.abs(x) == HALF + 1 || Math.abs(z) == HALF + 1;
                p.set(x, -1, z, "dirt");
                p.set(x, 0, z, edge ? "mossy_cobblestone" : Math.floorMod(x * 7 + z * 13, 11) == 0 ? "moss_block"
                        : "grass_block[snowy=false]");
                if (edge) {
                    p.set(x, 1, z, "oak_fence");
                    for (int h = 2; h <= 4; h++) {
                        p.set(x, h, z, "barrier");
                    }
                }
            }
        }
        for (EnzianWiese.Cell c : EnzianWiese.cells()) {
            p.set(c.x(), c.y(), c.z(), c.block());
        }
    }

    @Override
    public void prepare() {
        // Enziane vom letzten Mal weg (auch nach einem Absturz des Servers)
        for (int i = 0; i < spots.size(); i++) {
            if (spot(i).getType() == Material.CORNFLOWER) {
                spot(i).setType(Material.AIR, false);
            }
        }
        super.prepare();
    }

    @Override
    protected GameMode gameMode() {
        return GameMode.SURVIVAL;
    }

    @Override
    protected List<Location> spawns(int n) {
        List<Location> out = new ArrayList<>();
        double r = HALF - 1.5;
        for (int i = 0; i < Math.max(1, n); i++) {
            double a = 2 * Math.PI * i / Math.max(1, n) + Math.PI / 6;
            out.add(facing(0.5 + Math.cos(a) * r, 1, 0.5 + Math.sin(a) * r, 0.5, 1, 0.5));
        }
        return out;
    }

    @Override
    public Location viewPoint() {
        return facing(0.5, 14, -HALF - 9, 0.5, 0, 0.5);
    }

    @Override
    protected void equip(Player p) {
        p.getInventory().setItem(0, Items.named(Material.SHEARS, 1, Texts.text(plugin.lang(p), "mini.enziansuche.item"),
                null));
    }

    private int active() {
        return Math.min(spots.size() / 3, 6 + players.size() / 2);
    }

    @Override
    protected void onStart() {
        for (int i = 0; i < active(); i++) {
            grow();
        }
    }

    private Block spot(int i) {
        return block(spots.get(i)[0], 1, spots.get(i)[1]);
    }

    private void grow() {
        for (int tries = 0; tries < 30; tries++) {
            int i = rng.nextInt(spots.size());
            if (growing.contains(i) || spot(i).getType() != Material.AIR) {
                continue;
            }
            // nicht direkt neben jemandem
            Location c = spot(i).getLocation().add(0.5, 0, 0.5);
            boolean near = alivePlayers().stream().anyMatch(p -> p.getLocation().distanceSquared(c) < 9);
            if (near) {
                continue;
            }
            spot(i).setType(Material.CORNFLOWER, false);
            growing.add(i);
            return;
        }
    }

    @Override
    protected void onTick() {
        while (!waiting.isEmpty() && ticks >= waiting.getFirst()) {
            waiting.removeFirst();
            grow();
        }
    }

    @Override
    public void onBlockDamage(Player p, BlockDamageEvent e) {
        e.setCancelled(true);
        pick(p, e.getBlock());
    }

    @Override
    public void onInteract(Player p, PlayerInteractEvent e) {
        e.setCancelled(true);
        if ((e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_BLOCK)
                && e.getClickedBlock() != null) {
            pick(p, e.getClickedBlock());
        }
    }

    private void pick(Player p, Block b) {
        UUID u = p.getUniqueId();
        if (!score.alive(u) || b.getType() != Material.CORNFLOWER || ticks - lastPick.getOrDefault(u, -PICK_GAP) < PICK_GAP) {
            return;
        }
        int found = -1;
        for (int i : growing) {
            if (spot(i).equals(b)) {
                found = i;
            }
        }
        if (found < 0) {
            return;
        }
        lastPick.put(u, ticks);
        growing.remove(found);
        b.setType(Material.AIR, false);
        world.spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 0.5, 0.5), 8, 0.3, 0.3, 0.3, 0);
        point(p, 1);
        waiting.add(ticks + REGROW);
    }

    @Override
    protected void onFinish() {
        for (int i : growing) {
            spot(i).setType(Material.AIR, false);
        }
        growing.clear();
    }
}
