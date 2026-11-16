package net.morni.party.mini;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.morni.party.Texts;
import net.morni.party.Ui;
import net.morni.party.world.Decor;
import net.morni.party.world.Plan;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Föhnsturm: Oben auf dem Gipfel kommen Windböen – vorher sieht man Wolkenfetzen und hört es pfeifen, die Richtung
 * steht im Titel. Die Böe schiebt alle weg: hinter einem Felsen ist man sicher, geduckt kommt nur ein Drittel an,
 * gegen den Wind laufen hilft auch. Wer hinunterfällt, ist raus.
 */
final class FoehnSturm extends MiniRun {

    static final int RADIUS = 10;
    /** Felsen auf dem Gipfel (x, z, Höhe) – Schutz vor dem Wind. */
    static final int[][] ROCKS = {{4, 2, 2}, {-5, 3, 2}, {1, -6, 2}, {-3, -3, 1}, {6, -4, 2}, {-1, 6, 2}, {-7, -2, 1}};
    private static final int WARN = 30;
    private static final int GUST = 30;
    private static final int EVERY = 3;

    private Wind.From from;
    private int gustAt = 3 * SECOND;
    private int gustEnd = -1;

    FoehnSturm(Setup s) {
        super(s);
    }

    static void build(Plan p) {
        Random r = new Random(64);
        for (int x = -RADIUS - 1; x <= RADIUS + 1; x++) {
            for (int z = -RADIUS - 1; z <= RADIUS + 1; z++) {
                double d = Math.hypot(x, z);
                if (d > RADIUS + 0.5) {
                    continue;
                }
                p.set(x, 0, z, d > RADIUS - 2 ? (r.nextBoolean() ? "stone" : "andesite")
                        : r.nextInt(4) == 0 ? "snow_block" : r.nextInt(3) == 0 ? "gravel" : "stone");
                for (int k = 1; k <= 2 + (int) ((RADIUS - d) * 0.8); k++) {
                    p.set(x, -k, z, Decor.rock(r, k + 2));
                }
            }
        }
        for (int[] rock : ROCKS) {
            for (int y = 1; y <= rock[2]; y++) {
                p.set(rock[0], y, rock[1], y == rock[2] && r.nextBoolean() ? "mossy_cobblestone" : "cobblestone");
                p.set(rock[0] + 1, y, rock[1], y == 1 ? "andesite" : "cobblestone");
            }
        }
        // Windsack in der Mitte
        p.set(0, 1, 0, "spruce_fence");
        p.set(0, 2, 0, "spruce_fence");
        p.set(0, 3, 0, "spruce_fence");
        p.set(0, 4, 0, "orange_wool");
    }

    @Override
    protected int fallLimit() {
        return 6;
    }

    @Override
    protected List<Location> spawns(int n) {
        List<Location> out = new ArrayList<>();
        double r = 3.5;
        for (int i = 0; i < Math.max(1, n); i++) {
            double a = 2 * Math.PI * i / Math.max(1, n);
            double rr = n > 8 && i % 2 == 1 ? r + 2.5 : r;
            out.add(facing(0.5 + Math.cos(a) * rr, 1, 0.5 + Math.sin(a) * rr, 0.5, 1, 0.5));
        }
        return out;
    }

    @Override
    public Location viewPoint() {
        return facing(0.5, 12, -RADIUS - 12, 0.5, 0, 0.5);
    }

    @Override
    protected void equip(Player p) {
        // festhalten und ducken
    }

    private double progress() {
        return ticks / (double) Math.max(1, durationTicks());
    }

    @Override
    protected void onTick() {
        if (from == null && ticks >= gustAt - WARN) {
            from = Wind.From.values()[rng.nextInt(Wind.From.values().length)];
            String key = "mini.foehnsturm.from." + from.name().toLowerCase(Locale.ROOT);
            for (Player p : audience()) {
                String lang = plugin.lang(p);
                Ui.title(p, Texts.text(lang, "mini.foehnsturm.gust"), Texts.text(lang, key), 1200);
            }
            soundAll("minecraft:item.elytra.flying", 1.4f);
        }
        if (from != null && ticks % 2 == 0) {
            clouds(ticks >= gustAt ? 14 : 5);
        }
        if (from != null && ticks >= gustAt && gustEnd < 0) {
            gustEnd = ticks + GUST;
            soundAll("minecraft:entity.breeze.wind_burst", 0.8f);
        }
        if (gustEnd >= 0 && ticks <= gustEnd && (ticks - gustAt) % EVERY == 0) {
            blow();
        }
        if (gustEnd >= 0 && ticks > gustEnd) {
            from = null;
            gustEnd = -1;
            gustAt = ticks + Math.max(50, 110 - (int) (60 * progress())) + rng.nextInt(20);
        }
    }

    private void clouds(int n) {
        double[] d = from.push();
        for (int i = 0; i < n; i++) {
            double x = rng.nextDouble() * 2 * RADIUS - RADIUS - d[0] * 4;
            double z = rng.nextDouble() * 2 * RADIUS - RADIUS - d[1] * 4;
            world.spawnParticle(Particle.CLOUD, at(x + 0.5, 1 + rng.nextDouble() * 2, z + 0.5), 0, d[0], 0, d[1], 0.6);
        }
    }

    private void blow() {
        double[] d = from.push();
        double strength = Wind.strength(progress());
        for (Player p : alivePlayers()) {
            Location l = p.getLocation();
            int bx = l.getBlockX() - ox;
            int bz = l.getBlockZ() - oz;
            int feet = l.getBlockY();
            boolean sheltered = Wind.sheltered(bx, bz, from, (x, z) -> world.getBlockAt(ox + x, feet, oz + z).isSolid()
                    || world.getBlockAt(ox + x, feet + 1, oz + z).isSolid());
            double f = Wind.factor(sheltered, p.isSneaking()) * strength;
            if (f <= 0) {
                continue;
            }
            Vector v = new Vector(d[0] * f, 0.05, d[1] * f);
            p.setVelocity(v);
        }
    }
}
