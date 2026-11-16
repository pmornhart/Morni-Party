package net.morni.party.mini;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.morni.party.Items;
import net.morni.party.Texts;
import net.morni.party.Ui;
import net.morni.party.world.Decor;
import net.morni.party.world.Plan;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Murmeltier-Klopfen: Aus den Löchern der Bergwiese schauen Murmeltiere heraus – schnell hin und draufhauen
 * (Schlag oder Rechtsklick)! Ein braunes gibt einen Punkt, ein goldenes drei. Manchmal kommt ein Steinbrocken hoch:
 * wer den trifft, verliert einen Punkt.
 */
final class MurmelTier extends MiniRun {

    static final int HALF = 8;
    static final int[] GRID = {-6, -2, 2, 6};
    private static final int RISE = 5;
    private static final double LOW = 0.3;
    private static final float SCALE = 1.7f;
    private static final int HIT_GAP = 4;

    enum Kind { BROWN, GOLD, STONE }

    /** Was gerade aus einem Loch schaut. */
    private static final class Pop {
        int hole;
        Kind kind;
        Entity body;
        BlockDisplay look;
        int born;
        int up;
    }

    private final List<Pop> pops = new ArrayList<>();
    private final Map<UUID, Integer> lastHit = new HashMap<>();
    private int nextPop;

    MurmelTier(Setup s) {
        super(s);
    }

    static void build(Plan p) {
        Random r = new Random(17);
        for (int x = -HALF - 1; x <= HALF + 1; x++) {
            for (int z = -HALF - 1; z <= HALF + 1; z++) {
                p.set(x, -1, z, "dirt");
                p.set(x, 0, z, r.nextInt(7) == 0 ? "moss_block" : "grass_block[snowy=false]");
            }
        }
        for (int hx : GRID) {
            for (int hz : GRID) {
                p.set(hx, 0, hz, "mud");
                for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    p.set(hx + d[0], 0, hz + d[1], "coarse_dirt");
                }
            }
        }
        for (int i = -HALF - 1; i <= HALF + 1; i++) {
            for (int[] c : new int[][] {{i, -HALF - 1}, {i, HALF + 1}, {-HALF - 1, i}, {HALF + 1, i}}) {
                p.set(c[0], 0, c[1], "cobblestone");
                p.set(c[0], 1, c[1], "mossy_cobblestone_wall");
                for (int h = 2; h <= 4; h++) {
                    p.set(c[0], h, c[1], "barrier");
                }
            }
        }
        Decor.island(p, HALF + 1, 7, r, "grass_block[snowy=false]", 1);
    }

    @Override
    protected List<Location> spawns(int n) {
        List<Location> out = new ArrayList<>();
        double r = 4;
        for (int i = 0; i < Math.max(1, n); i++) {
            double a = 2 * Math.PI * i / Math.max(1, n) + Math.PI / 4;
            out.add(facing(0.5 + Math.cos(a) * r, 1, 0.5 + Math.sin(a) * r, 0.5, 1, 0.5));
        }
        return out;
    }

    @Override
    public Location viewPoint() {
        return facing(0.5, 10, -15, 0.5, 0, 0.5);
    }

    @Override
    protected void equip(Player p) {
        p.getInventory().setItem(0, Items.named(Material.MACE, 1, Texts.text(plugin.lang(p), "mini.murmeltier.item"),
                null));
    }

    @Override
    protected void onStart() {
        nextPop = ticks + SECOND;
    }

    private Location holeAt(int hole, double y) {
        return at(GRID[hole / GRID.length] + 0.5, y, GRID[hole % GRID.length] + 0.5);
    }

    @Override
    protected void onTick() {
        int most = Math.min(GRID.length * GRID.length / 2, 2 + alivePlayers().size());
        if (ticks >= nextPop && pops.size() < most) {
            pop();
            nextPop = ticks + Math.max(4, 10 - ticks / (4 * SECOND));
        }
        Iterator<Pop> it = pops.iterator();
        while (it.hasNext()) {
            Pop pop = it.next();
            int age = ticks - pop.born;
            double y;
            if (age < RISE) {
                y = LOW + (1 - LOW) * age / RISE;
            } else if (age < RISE + pop.up) {
                y = 1;
            } else if (age < 2 * RISE + pop.up) {
                y = 1 - (1 - LOW) * (age - RISE - pop.up) / RISE;
            } else {
                gone(pop);
                it.remove();
                continue;
            }
            Location l = holeAt(pop.hole, y);
            l.setYaw(pop.body.getLocation().getYaw());
            pop.body.teleport(l);
            if (pop.look != null) {
                pop.look.teleport(l);
            }
            if (pop.kind == Kind.GOLD && ticks % 4 == 0) {
                world.spawnParticle(Particle.WAX_ON, l.clone().add(0, 0.9, 0), 3, 0.3, 0.3, 0.3, 0);
            }
        }
    }

    private void pop() {
        List<Integer> free = new ArrayList<>();
        for (int i = 0; i < GRID.length * GRID.length; i++) {
            int hole = i;
            if (pops.stream().noneMatch(p -> p.hole == hole)) {
                free.add(i);
            }
        }
        if (free.isEmpty()) {
            return;
        }
        Pop pop = new Pop();
        pop.hole = free.get(rng.nextInt(free.size()));
        int roll = rng.nextInt(100);
        pop.kind = roll < 12 ? Kind.GOLD : roll < 32 ? Kind.STONE : Kind.BROWN;
        pop.born = ticks;
        double late = Math.min(1.0, ticks / (double) Math.max(1, durationTicks()));
        pop.up = (int) Math.round(26 - 12 * late) + rng.nextInt(6);
        Location l = holeAt(pop.hole, LOW);
        l.setYaw(rng.nextInt(360));
        if (pop.kind == Kind.STONE) {
            pop.body = track(world.spawn(l, Interaction.class, i -> {
                i.setInteractionWidth(0.9f);
                i.setInteractionHeight(0.9f);
                i.setResponsive(true);
                i.setPersistent(false);
            }));
            pop.look = track(world.spawn(l, BlockDisplay.class, d -> {
                d.setBlock(Material.COBBLESTONE.createBlockData());
                d.setTeleportDuration(1);
                d.setPersistent(false);
                d.setTransformation(new Transformation(new Vector3f(-0.42f, 0f, -0.42f),
                        new Quaternionf().rotateY(0.4f), new Vector3f(0.84f, 0.8f, 0.84f), new Quaternionf()));
            }));
        } else {
            pop.body = track(world.spawn(l, Rabbit.class, rb -> {
                rb.setRabbitType(pop.kind == Kind.GOLD ? Rabbit.Type.GOLD : Rabbit.Type.BROWN);
                rb.setAI(false);
                rb.setGravity(false);
                rb.setCollidable(false);
                rb.setSilent(true);
                rb.setPersistent(false);
                rb.setRemoveWhenFarAway(false);
                rb.setGlowing(pop.kind == Kind.GOLD);
                var scale = rb.getAttribute(Attribute.SCALE);
                if (scale != null) {
                    scale.setBaseValue(SCALE);
                }
            }));
        }
        pops.add(pop);
        world.playSound(l, "minecraft:block.rooted_dirt.break", 0.8f, 1.2f);
    }

    private void gone(Pop pop) {
        untrack(pop.body);
        if (pop.look != null) {
            untrack(pop.look);
        }
    }

    @Override
    public void onEntityClick(Player p, Entity target) {
        UUID u = p.getUniqueId();
        int last = lastHit.getOrDefault(u, -HIT_GAP);
        if (ticks - last < HIT_GAP) {
            return;
        }
        Iterator<Pop> it = pops.iterator();
        while (it.hasNext()) {
            Pop pop = it.next();
            if (pop.body != target) {
                continue;
            }
            lastHit.put(u, ticks);
            it.remove();
            Location l = pop.body.getLocation().add(0, 0.5, 0);
            gone(pop);
            switch (pop.kind) {
                case BROWN -> {
                    world.spawnParticle(Particle.POOF, l, 6, 0.2, 0.2, 0.2, 0.02);
                    point(p, 1);
                }
                case GOLD -> {
                    world.spawnParticle(Particle.WAX_ON, l, 16, 0.4, 0.4, 0.4, 0);
                    Ui.sound(p, "minecraft:block.note_block.bell", 1.6f);
                    point(p, 3);
                }
                case STONE -> {
                    world.spawnParticle(Particle.BLOCK, l, 12, 0.3, 0.3, 0.3, Material.COBBLESTONE.createBlockData());
                    score.add(u, -1);
                    Ui.sound(p, "minecraft:block.stone.break", 0.7f);
                    bar(p, "mini.murmeltier.stone");
                }
            }
            return;
        }
    }

    @Override
    protected void onFinish() {
        pops.clear();
    }
}
