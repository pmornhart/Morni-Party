package net.morni.party.mini;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.morni.party.Ui;
import net.morni.party.world.Decor;
import net.morni.party.world.Plan;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Lawinenhang: ein Wettlauf den verschneiten Hang hinauf. Von oben rollen Schneebrocken herab – wer getroffen wird,
 * rutscht ein Stück zurück. Wer zuerst oben durchs Ziel läuft, gewinnt; danach zählt, wer am weitesten kam.
 * Der Hang besteht aus Schneeschichten (jeder Block ein Achtel höher), man läuft ohne zu springen hinauf.
 */
final class LawinenHang extends MiniRun {

    static final int HALF = 4;
    static final int START_Z = -26;
    static final int SLOPE_FROM = -20;
    static final int SLOPE_TO = 20;
    static final int GOAL_Z = 22;
    static final int BACK_Z = 26;
    /** So weit rutscht man nach einem Treffer zurück. */
    static final int SLIDE = 6;
    private static final float SIZE = 1.2f;
    private static final double HIT = 0.95;
    private static final int IMMUNE = 20;
    private static final int FIRST = 2 * SECOND;

    /** Ein rollender Schneebrocken. */
    private static final class Boulder {
        BlockDisplay display;
        double x;
        double z;
        double speed;
        float angle;
    }

    private final List<Boulder> boulders = new ArrayList<>();
    private final Map<UUID, Integer> immune = new HashMap<>();
    private int nextSpawn = FIRST;

    LawinenHang(Setup s) {
        super(s);
    }

    /** Höhe des Schnees in Achteln (0 unten am Start, 80 oben). */
    static int eighths(int z) {
        return Math.max(0, Math.min(2 * (SLOPE_TO - SLOPE_FROM), 2 * (z - SLOPE_FROM)));
    }

    /** Oberfläche (y, relativ) an dieser Stelle – darauf steht man. */
    static double surface(double z) {
        return 1 + eighths((int) Math.floor(z)) / 8.0;
    }

    static void build(Plan p) {
        Random r = new Random(41);
        for (int z = START_Z - 4; z <= BACK_Z; z++) {
            int e = eighths(z);
            int full = e / 8;
            int rest = e % 8;
            for (int x = -HALF - 1; x <= HALF + 1; x++) {
                boolean wall = Math.abs(x) == HALF + 1;
                p.set(x, -1, z, "stone");
                for (int y = 0; y <= full; y++) {
                    p.set(x, y, z, wall ? Decor.rock(r, 2) : "snow_block");
                }
                if (wall) {
                    // Lawinenzaun am Rand, darüber eine unsichtbare Wand
                    p.set(x, full + 1, z, "spruce_fence");
                    for (int y = full + 2; y <= full + 4; y++) {
                        p.set(x, y, z, "barrier");
                    }
                } else {
                    // Schneeschicht: (Schichten − 1) Achtel fest, eine Schicht ist nur Puder obendrauf
                    p.set(x, full + 1, z, "snow[layers=" + (rest + 1) + "]");
                }
            }
        }
        // Startlinie und Ziellinie (ohne Puder darüber, damit man sie sieht)
        for (int x = -HALF; x <= HALF; x++) {
            p.set(x, 0, START_Z + 2, "red_concrete");
            p.clear(x, 1, START_Z + 2);
            int top = eighths(GOAL_Z) / 8;
            p.set(x, top, GOAL_Z, Math.floorMod(x, 2) == 0 ? "red_concrete" : "white_concrete");
            p.clear(x, top + 1, GOAL_Z);
        }
        // Zielbogen
        int gt = eighths(GOAL_Z) / 8;
        for (int side : new int[] {-HALF - 1, HALF + 1}) {
            for (int y = gt + 1; y <= gt + 5; y++) {
                p.set(side, y, GOAL_Z, "stripped_spruce_log[axis=y]");
            }
        }
        for (int x = -HALF - 1; x <= HALF + 1; x++) {
            p.set(x, gt + 6, GOAL_Z, Math.floorMod(x, 2) == 0 ? "red_wool" : "white_wool");
        }
        // Rückwand oben und hinter dem Start
        for (int x = -HALF; x <= HALF; x++) {
            for (int y = 1; y <= 4; y++) {
                p.set(x, gt + y, BACK_Z + 1, "barrier");
                p.set(x, y, START_Z - 5, "barrier");
            }
        }
    }

    @Override
    protected List<Location> spawns(int n) {
        List<Location> out = new ArrayList<>();
        // acht nebeneinander (mit Lücken, solange es wenige sind), bei mehr Leuten eine zweite Reihe
        for (int i = 0; i < Math.max(1, n); i++) {
            double x = -HALF + 0.5 + (n <= HALF ? i * 2.0 : i % (2 * HALF));
            out.add(facing(x, 1, START_Z - 1.5 - (n <= HALF ? 0 : i / (2 * HALF)), x, 6, GOAL_Z));
        }
        return out;
    }

    @Override
    public Location viewPoint() {
        return facing(0.5, 18, START_Z - 12, 0.5, 6, 4);
    }

    @Override
    protected void equip(Player p) {
        // Laufen und ausweichen
    }

    @Override
    protected void onTick() {
        if (ticks >= nextSpawn) {
            spawnBoulder();
            int gap = Math.max(7, 16 - ticks / (3 * SECOND));
            nextSpawn = ticks + gap + rng.nextInt(5);
        }
        // erst ankommen lassen, dann rollen – wer oben ist, kann nicht mehr getroffen werden
        for (Player p : alivePlayers()) {
            double z = p.getLocation().getZ() - oz;
            if (z >= GOAL_Z) {
                arrive(p, "mini.race.finish");
            } else if (ticks % 2 == 0) {
                progress(p, (int) Math.floor(z - START_Z));
            }
        }
        roll();
    }

    private void spawnBoulder() {
        double x;
        List<Player> climbing = alivePlayers().stream().filter(p -> !score.finished(p.getUniqueId())).toList();
        if (!climbing.isEmpty() && rng.nextInt(3) == 0) {
            // Jeder dritte Brocken rollt auf jemanden zu – Ecken sind nicht sicher.
            x = climbing.get(rng.nextInt(climbing.size())).getLocation().getX() - ox - 0.5;
        } else {
            x = rng.nextDouble() * (2 * HALF - 0.6) - HALF + 0.3;
        }
        Boulder b = new Boulder();
        b.x = Math.max(-HALF + 0.1, Math.min(HALF - 0.1, x));
        b.z = BACK_Z - 0.5;
        b.speed = 0.28 + 0.14 * Math.min(1.0, ticks / (double) Math.max(1, durationTicks()));
        b.display = track(world.spawn(where(b), BlockDisplay.class, d -> {
            d.setBlock(Material.SNOW_BLOCK.createBlockData());
            d.setTeleportDuration(1);
            d.setInterpolationDuration(2);
            d.setPersistent(false);
            d.setTransformation(shape(0f));
        }));
        boulders.add(b);
        world.playSound(where(b), "minecraft:block.snow.break", 1f, 0.6f);
    }

    private Location where(Boulder b) {
        return at(b.x + 0.5, surface(b.z) + SIZE / 2.0, b.z + 0.5);
    }

    /** Würfel um seine Mitte gedreht (rollt um die x-Achse). */
    private static Transformation shape(float angle) {
        Quaternionf q = new Quaternionf().rotateX(angle);
        Vector3f half = new Vector3f(SIZE / 2f, SIZE / 2f, SIZE / 2f).rotate(q);
        return new Transformation(half.negate(), q, new Vector3f(SIZE, SIZE, SIZE), new Quaternionf());
    }

    private void roll() {
        Iterator<Boulder> it = boulders.iterator();
        while (it.hasNext()) {
            Boulder b = it.next();
            b.z -= b.speed;
            if (b.z < START_Z - 4) {
                world.spawnParticle(Particle.SNOWFLAKE, where(b), 12, 0.5, 0.4, 0.5, 0.02);
                untrack(b.display);
                it.remove();
                continue;
            }
            b.display.teleport(where(b));
            if (ticks % 2 == 0) {
                b.angle -= (float) (b.speed * 2 / SIZE);
                b.display.setInterpolationDelay(0);
                b.display.setTransformation(shape(b.angle));
            }
            for (Player p : alivePlayers()) {
                hit(b, p);
            }
        }
    }

    private void hit(Boulder b, Player p) {
        UUID u = p.getUniqueId();
        if (score.finished(u) || immune.getOrDefault(u, -1) > ticks || p.getLocation().getZ() - oz >= GOAL_Z) {
            return;
        }
        Location l = p.getLocation();
        double dx = l.getX() - ox - 0.5 - b.x;
        double dz = l.getZ() - oz - 0.5 - b.z;
        double feet = l.getY() - oy;
        if (Math.abs(dx) > HIT || Math.abs(dz) > HIT || feet > surface(b.z) + SIZE - 0.2) {
            return;
        }
        immune.put(u, ticks + IMMUNE);
        double back = Math.max(START_Z, l.getZ() - oz - SLIDE);
        Location to = l.clone();
        to.setZ(oz + back);
        to.setY(oy + surface(back) + 0.05);
        world.spawnParticle(Particle.SNOWFLAKE, l.clone().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.05);
        p.teleport(to);
        p.setFallDistance(0);
        Ui.sound(p, "minecraft:entity.player.hurt_freeze", 1f);
        bar(p, "mini.lawinenhang.hit");
    }

    @Override
    protected void onFinish() {
        boulders.clear();
    }
}
