package net.morni.party.mini;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.morni.party.Items;
import net.morni.party.Texts;
import net.morni.party.Ui;
import net.morni.party.world.Decor;
import net.morni.party.world.Plan;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Goldwaschen: Am Bach liegen Kies- und Sandhaufen. Ein Schlag (oder Rechtsklick) wäscht einen Haufen aus – in manchen
 * steckt ein Goldkorn (1 Punkt), selten ein Goldklumpen (3 Punkte). Ab und zu glitzert es kurz, wo Gold liegt.
 * Ausgewaschene Haufen kommen woanders wieder.
 */
final class GoldWaschen extends MiniRun {

    static final int HALF = 10;
    /** Bach entlang x, so breit (z von −STREAM bis +STREAM). */
    static final int STREAM = 1;
    /** Mögliche Stellen für Haufen (x, z) – auf beiden Ufern. */
    static final List<int[]> SPOTS = spots();
    private static final int PILES = 16;
    private static final int RESPAWN = 2 * SECOND;
    private static final int HIT_GAP = 6;
    private static final int GLITTER = 30;

    /** Ein Wurf aus 100: was steckt im Haufen? 3 = Goldklumpen, 1 = Goldkorn, 0 = nur Kies. */
    static int worth(int roll) {
        return roll < 6 ? 3 : roll < 34 ? 1 : 0;
    }

    private static List<int[]> spots() {
        List<int[]> out = new ArrayList<>();
        for (int x = -HALF + 1; x <= HALF - 1; x += 2) {
            for (int z : new int[] {-STREAM - 2, -STREAM - 5, -STREAM - 8, STREAM + 2, STREAM + 5, STREAM + 8}) {
                out.add(new int[] {x + Math.floorMod(z, 2), z});
            }
        }
        return List.copyOf(out);
    }

    private final Map<Integer, Integer> piles = new HashMap<>();
    private final List<Integer> waiting = new ArrayList<>();
    private final Map<UUID, Integer> lastHit = new HashMap<>();

    GoldWaschen(Setup s) {
        super(s);
    }

    static void build(Plan p) {
        Random r = new Random(31);
        for (int x = -HALF - 1; x <= HALF + 1; x++) {
            for (int z = -HALF - 1; z <= HALF + 1; z++) {
                p.set(x, -2, z, "stone");
                if (Math.abs(z) <= STREAM) {
                    p.set(x, -1, z, r.nextInt(3) == 0 ? "sand" : "gravel");
                    p.set(x, 0, z, "water");
                } else {
                    p.set(x, -1, z, "dirt");
                    p.set(x, 0, z, Math.abs(z) == STREAM + 1 ? (r.nextBoolean() ? "gravel" : "coarse_dirt")
                            : r.nextInt(6) == 0 ? "coarse_dirt" : "grass_block[snowy=false]");
                }
            }
        }
        // Stege über den Bach
        for (int x : new int[] {-6, 0, 6}) {
            for (int z = -STREAM; z <= STREAM; z++) {
                p.set(x, 0, z, "spruce_planks");
            }
        }
        for (int i = -HALF - 1; i <= HALF + 1; i++) {
            for (int[] c : new int[][] {{i, -HALF - 1}, {i, HALF + 1}}) {
                p.set(c[0], 0, c[1], "mossy_cobblestone");
                p.set(c[0], 1, c[1], "mossy_cobblestone_wall");
                for (int h = 2; h <= 4; h++) {
                    p.set(c[0], h, c[1], "barrier");
                }
            }
            for (int[] c : new int[][] {{-HALF - 1, i}, {HALF + 1, i}}) {
                boolean water = Math.abs(c[1]) <= STREAM;
                p.set(c[0], 0, c[1], water ? "mossy_cobblestone" : "mossy_cobblestone");
                p.set(c[0], 1, c[1], water ? "barrier" : "mossy_cobblestone_wall");
                for (int h = 2; h <= 4; h++) {
                    p.set(c[0], h, c[1], "barrier");
                }
            }
        }
        Decor.island(p, HALF + 1, 7, r, "dirt", 1);
    }

    @Override
    protected GameMode gameMode() {
        // Schlagen auf Blöcke geht nur im Überlebensmodus (abgebaut wird nie – das verhindert der Schutz).
        return GameMode.SURVIVAL;
    }

    @Override
    protected List<Location> spawns(int n) {
        List<Location> out = new ArrayList<>();
        for (int i = 0; i < Math.max(1, n); i++) {
            int side = i % 2 == 0 ? -1 : 1;
            double x = -HALF + 1.5 + ((i / 2) * 2.5) % (2 * HALF - 2);
            out.add(facing(x, 1, 0.5 + side * (STREAM + 3.5), x, 1, 0.5));
        }
        return out;
    }

    @Override
    public Location viewPoint() {
        return facing(0.5, 10, -HALF - 8, 0.5, 0, 0.5);
    }

    @Override
    protected void equip(Player p) {
        ItemStack pan = Items.named(Material.BOWL, 1, Texts.text(plugin.lang(p), "mini.goldwaschen.item"),
                Texts.lines(plugin.lang(p), "mini.goldwaschen.item.lore"));
        p.getInventory().setItem(0, pan);
    }

    private Block pileBlock(int spot) {
        return block(SPOTS.get(spot)[0], 1, SPOTS.get(spot)[1]);
    }

    @Override
    protected void onStart() {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < SPOTS.size(); i++) {
            idx.add(i);
        }
        java.util.Collections.shuffle(idx, rng);
        for (int i = 0; i < PILES; i++) {
            newPile(idx.get(i));
        }
    }

    private void newPile(int spot) {
        piles.put(spot, worth(rng.nextInt(100)));
        pileBlock(spot).setType(rng.nextInt(3) == 0 ? Material.SAND : Material.GRAVEL, false);
    }

    @Override
    protected void onTick() {
        if (!waiting.isEmpty() && ticks >= waiting.getFirst()) {
            waiting.removeFirst();
            List<Integer> free = new ArrayList<>();
            for (int i = 0; i < SPOTS.size(); i++) {
                if (!piles.containsKey(i)) {
                    free.add(i);
                }
            }
            if (!free.isEmpty()) {
                newPile(free.get(rng.nextInt(free.size())));
            }
        }
        if (ticks % GLITTER == 0) {
            List<Integer> gold = piles.entrySet().stream().filter(e -> e.getValue() > 0).map(Map.Entry::getKey).toList();
            if (!gold.isEmpty()) {
                Block b = pileBlock(gold.get(rng.nextInt(gold.size())));
                world.spawnParticle(Particle.WAX_ON, b.getLocation().add(0.5, 1.0, 0.5), 8, 0.3, 0.2, 0.3, 0);
            }
        }
    }

    @Override
    public void onBlockDamage(Player p, BlockDamageEvent e) {
        e.setCancelled(true);
        wash(p, e.getBlock());
    }

    /** Schlag oder Rechtsklick auf einen Haufen (der Schlag kommt hier an, bevor das Abbauen beginnt). */
    @Override
    public void onInteract(Player p, PlayerInteractEvent e) {
        e.setCancelled(true);
        if ((e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_BLOCK)
                && e.getClickedBlock() != null) {
            wash(p, e.getClickedBlock());
        }
    }

    private void wash(Player p, Block b) {
        UUID u = p.getUniqueId();
        if (!score.alive(u) || b.getY() != oy + 1) {
            return;
        }
        int spot = -1;
        for (int i = 0; i < SPOTS.size(); i++) {
            if (b.getX() == ox + SPOTS.get(i)[0] && b.getZ() == oz + SPOTS.get(i)[1]) {
                spot = i;
            }
        }
        Integer worth = spot < 0 ? null : piles.get(spot);
        if (worth == null || ticks - lastHit.getOrDefault(u, -HIT_GAP) < HIT_GAP) {
            return;
        }
        lastHit.put(u, ticks);
        piles.remove(spot);
        waiting.add(ticks + RESPAWN);
        Location c = b.getLocation().add(0.5, 0.5, 0.5);
        world.spawnParticle(Particle.BLOCK, c, 14, 0.3, 0.3, 0.3, b.getBlockData());
        world.spawnParticle(Particle.SPLASH, c, 10, 0.3, 0.2, 0.3, 0);
        b.setType(Material.AIR, false);
        if (worth > 0) {
            world.spawnParticle(Particle.ITEM, c, 8, 0.2, 0.3, 0.2, 0.05,
                    new ItemStack(worth > 1 ? Material.RAW_GOLD : Material.GOLD_NUGGET));
            Ui.sound(p, "minecraft:block.note_block.bell", worth > 1 ? 1.4f : 1.8f);
            point(p, worth);
            if (worth > 1) {
                bar(p, "mini.goldwaschen.big");
            }
        } else {
            Ui.sound(p, "minecraft:block.gravel.break", 1f);
            bar(p, "mini.goldwaschen.empty");
        }
    }

    @Override
    protected void onFinish() {
        for (int i = 0; i < SPOTS.size(); i++) {
            pileBlock(i).setType(Material.AIR, false);
        }
        piles.clear();
    }
}
