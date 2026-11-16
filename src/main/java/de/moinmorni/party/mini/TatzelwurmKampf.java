package de.moinmorni.party.mini;

import de.moinmorni.party.Items;
import de.moinmorni.party.Texts;
import de.moinmorni.party.Ui;
import de.moinmorni.party.world.Plan;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Der Tatzelwurm (Spielart „Koop“): alle zusammen gegen einen Wurm mit Katzenkopf, in drei Abschnitten (Regeln in
 * {@link WurmRegeln}). Der Wurm besteht aus Block-Anzeigen, Treffer gehen über unsichtbare Klickflächen – Schlagen
 * reicht, auch auf Bedrock. Wer getroffen wird, ist kurz benommen; niemand scheidet aus.
 */
final class TatzelwurmKampf extends MiniRun {

    static final int HALF = 17;
    private static final int SEGMENTS = 11;
    private static final double SPACING = 1.05;
    private static final int RISE = 14;
    private static final int OUT = 60;
    private static final int ROCK_FALL = 26;
    /** Der Kopf ist groß: alle Teile × dieser Faktor. */
    private static final float HEAD_SCALE = 1.5f;

    /** Ein Teil des Kopfes: Block, Größe, Versatz (vorn = +z). */
    private record Part(Material block, float sx, float sy, float sz, float ox, float oy, float oz) {
    }

    private static final List<Part> HEAD = List.of(
            new Part(Material.ORANGE_TERRACOTTA, 1.8f, 1.4f, 1.7f, 0, 0, 0),
            new Part(Material.ORANGE_TERRACOTTA, 1.1f, 0.8f, 0.8f, 0, -0.2f, 1.1f),
            new Part(Material.PINK_TERRACOTTA, 0.5f, 0.3f, 0.2f, 0, 0.0f, 1.55f),
            new Part(Material.YELLOW_CONCRETE, 0.34f, 0.34f, 0.12f, -0.5f, 0.3f, 0.86f),
            new Part(Material.YELLOW_CONCRETE, 0.34f, 0.34f, 0.12f, 0.5f, 0.3f, 0.86f),
            new Part(Material.BLACK_CONCRETE, 0.12f, 0.26f, 0.06f, -0.5f, 0.3f, 0.93f),
            new Part(Material.BLACK_CONCRETE, 0.12f, 0.26f, 0.06f, 0.5f, 0.3f, 0.93f),
            new Part(Material.BROWN_TERRACOTTA, 0.45f, 0.6f, 0.25f, -0.55f, 0.95f, 0.1f),
            new Part(Material.BROWN_TERRACOTTA, 0.45f, 0.6f, 0.25f, 0.55f, 0.95f, 0.1f),
            new Part(Material.WHITE_CONCRETE, 0.14f, 0.22f, 0.14f, -0.3f, -0.68f, 1.4f),
            new Part(Material.WHITE_CONCRETE, 0.14f, 0.22f, 0.14f, 0.3f, -0.68f, 1.4f),
            new Part(Material.WHITE_CONCRETE, 0.9f, 0.08f, 0.08f, -0.55f, -0.25f, 1.45f),
            new Part(Material.WHITE_CONCRETE, 0.9f, 0.08f, 0.08f, 0.55f, -0.25f, 1.45f));

    private final List<BlockDisplay> head = new ArrayList<>();
    private final List<BlockDisplay> body = new ArrayList<>();
    private final List<Interaction> bodyBoxes = new ArrayList<>();
    private Interaction headBox;
    private TextDisplay label;
    private final Deque<double[]> trail = new ArrayDeque<>();
    private final Map<UUID, Integer> stunnedUntil = new HashMap<>();
    private final Map<UUID, Integer> lastHit = new HashMap<>();
    private final List<int[]> weak = new ArrayList<>();
    private final List<Rock> rocks = new ArrayList<>();
    private final int max;
    private int hp;
    private int phase = 1;
    private int phaseTick;
    private int stuns;
    private boolean won;
    // Kopf: Lage und ob man ihn treffen kann
    private double hx;
    private double hy = -4;
    private double hz;
    private float yaw;
    private boolean headOpen;
    // Abschnitt 1
    private int hole = -1;
    private int holeTick;
    // Abschnitt 2
    private double travelled;
    // Abschnitt 3
    private double wave = -1;
    private double[] slamAt;
    private int slamTick = -1;

    /** Ein spuckender Felsbrocken: Ziel am Boden, Anzeige, Ticks bis zum Aufschlag. */
    private static final class Rock {
        double x;
        double z;
        int left;
        BlockDisplay display;
    }

    TatzelwurmKampf(Setup s) {
        super(s);
        max = WurmRegeln.hp(s.players().size());
        hp = max;
    }

    static void build(Plan p) {
        for (int x = -HALF - 2; x <= HALF + 2; x++) {
            for (int z = -HALF - 2; z <= HALF + 2; z++) {
                double d = Math.hypot(x, z);
                if (d > HALF + 2.2) {
                    continue;
                }
                p.set(x, -2, z, "stone");
                p.set(x, -1, z, "stone");
                if (d > HALF + 0.5) {
                    // Felsrand und unsichtbare Wand
                    p.set(x, 0, z, "mossy_cobblestone");
                    for (int y = 1; y <= 9; y++) {
                        p.set(x, y, z, y <= 2 + Math.floorMod(x * 3 + z * 7, 3) ? "cobblestone" : "barrier");
                    }
                    continue;
                }
                // ruhige Flecken statt Schachbrett: zwei Wellen über der Fläche
                double n = Math.sin(x * 0.35 + z * 0.12) + Math.cos(z * 0.31 - x * 0.17) + 0.5 * Math.sin((x + z) * 0.7);
                int m = Math.floorMod(x * 7 + z * 13, 5);
                String floor = d < 3.2 ? (m == 0 ? "cracked_stone_bricks" : "mossy_stone_bricks")
                        : n > 1.4 ? (m < 3 ? "moss_block" : "mossy_cobblestone") : n > 0.6 ? "andesite"
                        : n < -1.4 ? (m < 2 ? "coarse_dirt" : "gravel") : n < -0.7 ? "tuff" : m == 0 ? "cobblestone" : "stone";
                p.set(x, 0, z, floor);
                for (int y = 1; y <= 9; y++) {
                    p.set(x, y, z, "air");
                }
            }
        }
        // Wurmlöcher: aufgewühlte Erde im Kreis
        for (int i = 0; i < WurmRegeln.HOLES; i++) {
            double[] h = WurmRegeln.hole(i);
            int cx = (int) Math.round(h[0]);
            int cz = (int) Math.round(h[1]);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    p.set(cx + dx, 0, cz + dz, dx == 0 && dz == 0 ? "rooted_dirt" : Math.abs(dx) + Math.abs(dz) == 2
                            ? "coarse_dirt" : "podzol");
                }
            }
        }
    }

    @Override
    public void prepare() {
        place(plugin.arenaPlan(type));
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            double a = 2 * Math.PI * i / Math.max(1, players.size());
            reset(p);
            p.setGameMode(gameMode());
            p.teleport(facing(0.5 + Math.cos(a) * 5, 1, 0.5 + Math.sin(a) * 5, 0.5, 1, 0.5));
            equip(p);
        }
        for (Part part : HEAD) {
            head.add(track(world.spawn(at(0.5, -6, 0.5), BlockDisplay.class, d -> {
                d.setBlock(part.block().createBlockData());
                d.setTeleportDuration(2);
                d.setPersistent(false);
                float k = HEAD_SCALE;
                d.setTransformation(new Transformation(new Vector3f(k * (part.ox() - part.sx() / 2), k * (part.oy() - part.sy() / 2),
                        k * (part.oz() - part.sz() / 2)), new AxisAngle4f(), new Vector3f(k * part.sx(), k * part.sy(), k * part.sz()),
                        new AxisAngle4f()));
            })));
        }
        for (int i = 0; i < SEGMENTS; i++) {
            float sz = (float) (1.6 - 0.08 * i);
            Material m = i % 2 == 0 ? Material.GREEN_TERRACOTTA : Material.MOSS_BLOCK;
            body.add(track(world.spawn(at(0.5, -6, 0.5), BlockDisplay.class, d -> {
                d.setBlock(m.createBlockData());
                d.setTeleportDuration(2);
                d.setPersistent(false);
                d.setGlowColorOverride(Color.fromRGB(0xFFD54F));
                d.setTransformation(new Transformation(new Vector3f(-sz / 2, -sz / 2, -sz / 2), new AxisAngle4f(),
                        new Vector3f(sz, sz, sz), new AxisAngle4f()));
            })));
            bodyBoxes.add(track(world.spawn(at(0.5, -6, 0.5), Interaction.class, b -> {
                b.setInteractionWidth(sz + 0.3f);
                b.setInteractionHeight(sz + 0.3f);
                b.setPersistent(false);
                b.setResponsive(true);
            })));
        }
        headBox = track(world.spawn(at(0.5, -6, 0.5), Interaction.class, b -> {
            b.setInteractionWidth(3.0f);
            b.setInteractionHeight(2.6f);
            b.setPersistent(false);
                b.setResponsive(true);
        }));
        label = track(world.spawn(at(0.5, -6, 0.5), TextDisplay.class, t -> {
            t.setBillboard(Display.Billboard.CENTER);
            t.setPersistent(false);
            t.setTeleportDuration(2);
            t.setBackgroundColor(Color.fromARGB(120, 0, 0, 0));
        }));
        for (int i = 0; i < 40; i++) {
            trail.addFirst(new double[] {0.5, -6, 0.5});
        }
    }

    @Override
    protected void equip(Player p) {
        p.getInventory().setItem(0, Items.named(Material.STICK, 1, Texts.text(plugin.lang(p), "mini.tatzelwurm.item"), null));
    }

    @Override
    public Location viewPoint() {
        return facing(0.5, 14, -HALF + 4, 0.5, 0, 2);
    }

    @Override
    protected boolean complete() {
        return hp <= 0;
    }

    @Override
    public List<double[]> targets() {
        List<double[]> out = new ArrayList<>();
        if (headOpen) {
            out.add(new double[] {hx, hy - 1.2, hz});
        }
        for (int i = 0; i < SEGMENTS; i++) {
            if (isWeak(i)) {
                Location l = bodyBoxes.get(i).getLocation();
                out.add(new double[] {l.getX() - ox, l.getY() - oy, l.getZ() - oz});
            }
        }
        return out;
    }

    @Override
    public boolean adminSet(String key, int value) {
        if (!"hp".equals(key)) {
            return false;
        }
        hp = Math.max(1, Math.min(max, value));
        return true;
    }

    @Override
    public Coop coop() {
        return new Coop(won, WurmRegeln.stars(won, ticks, durationTicks(), stuns, players.size()), stuns,
                (ticks + SECOND - 1) / SECOND, Math.max(0, hp), max);
    }

    @Override
    protected void onStart() {
        titleAll("mini.tatzelwurm.phase.1", "mini.tatzelwurm.phase.1.sub", 2500);
        soundAll("minecraft:entity.ravager.roar", 0.6f);
    }

    // ---------- Ablauf ----------

    @Override
    protected void onTick() {
        int now = WurmRegeln.phase(hp, max);
        if (now != phase && hp > 0) {
            phase = now;
            phaseTick = 0;
            headOpen = false;
            hole = -1;
            slamTick = -1;
            titleAll("mini.tatzelwurm.phase." + phase, "mini.tatzelwurm.phase." + phase + ".sub", 2500);
            soundAll("minecraft:entity.ravager.roar", phase == 3 ? 0.5f : 0.7f);
        }
        phaseTick++;
        switch (phase) {
            case 1 -> burrow();
            case 2 -> chase();
            default -> rage();
        }
        if (phase < 3 && phaseTick % (phase == 1 ? 70 : 110) == 35) {
            spit();
        }
        rocks();
        show();
        touch();
        if (ticks % 10 == 0) {
            for (Player p : alivePlayers()) {
                bar(p, "mini.tatzelwurm.bar", Placeholder.unparsed("hp", String.valueOf(Math.max(0, hp))),
                        Placeholder.unparsed("max", String.valueOf(max)), Placeholder.unparsed("hits",
                                String.valueOf(score.points(p.getUniqueId()))));
            }
        }
    }

    /** Abschnitt 1: der Kopf bricht an einem Loch heraus, bleibt kurz und taucht wieder ab. */
    private void burrow() {
        if (hole < 0) {
            int next;
            do {
                next = rng.nextInt(WurmRegeln.HOLES);
            } while (next == lastHole && WurmRegeln.HOLES > 1);
            hole = next;
            lastHole = next;
            holeTick = 0;
        }
        holeTick++;
        double[] h = WurmRegeln.hole(hole);
        hx = h[0] + 0.5;
        hz = h[1] + 0.5;
        // vorher rumpelt es am Loch
        if (holeTick < 25) {
            hy = -4;
            headOpen = false;
            if (holeTick % 4 == 0) {
                world.spawnParticle(Particle.BLOCK, at(hx, 1.1, hz), 14, 0.6, 0.1, 0.6, 0,
                        Material.ROOTED_DIRT.createBlockData());
                world.playSound(at(hx, 1, hz), "minecraft:block.rooted_dirt.break", 0.8f, 0.6f);
            }
        } else if (holeTick < 25 + RISE) {
            hy = -2 + 3.6 * (holeTick - 25) / RISE;
            headOpen = hy > 0.4;
        } else if (holeTick < 25 + RISE + OUT) {
            hy = 1.6 + 0.15 * Math.sin(holeTick * 0.3);
            headOpen = true;
            // schaut zum nächsten Spieler
            Player near = nearest(hx, hz);
            if (near != null) {
                yaw = yawTo(hx, hz, near.getLocation().getX() - ox, near.getLocation().getZ() - oz);
            }
        } else if (holeTick < 25 + 2 * RISE + OUT) {
            hy = 1.6 - 3.6 * (holeTick - 25 - RISE - OUT) / RISE;
            headOpen = hy > 0.4;
        } else {
            hole = -1;
            hy = -4;
            headOpen = false;
        }
        trail.addFirst(new double[] {hx, Math.min(hy - 1.2, -0.8), hz});
        trimTrail();
    }

    private int lastHole = -1;

    /** Abschnitt 2: Jagd im Kreis, leuchtende Schuppen sind verwundbar. */
    private void chase() {
        double speed = 0.17 + 0.05 * (1 - (double) hp / max);
        travelled += speed;
        double[] pt = WurmRegeln.path(travelled);
        double[] ahead = WurmRegeln.path(travelled + 0.5);
        double nx = pt[0] + 0.5;
        double nz = pt[1] + 0.5;
        yaw = yawTo(nx, nz, ahead[0] + 0.5, ahead[1] + 0.5);
        hx = nx;
        hz = nz;
        hy = 1.2 + 0.25 * Math.sin(travelled * 1.3);
        headOpen = false;
        trail.addFirst(new double[] {hx, 0.9, hz});
        trimTrail();
        if (weak.isEmpty() || phaseTick % 200 == 0) {
            pickWeak();
        }
    }

    /** Abschnitt 3: zusammengerollt in der Mitte, Bodenwellen und Kopfschläge. */
    private void rage() {
        // Körper als Ring um die Mitte
        trail.clear();
        for (int i = 0; i < 40; i++) {
            double a = i * 0.32 + phaseTick * 0.02;
            trail.addLast(new double[] {0.5 + Math.cos(a) * 2.4, 0.7 + i * 0.03, 0.5 + Math.sin(a) * 2.4});
        }
        int cycle = phaseTick % 180;
        if (cycle == 10) {
            wave = 3;
            soundAll("minecraft:entity.warden.sonic_charge", 1.4f);
        }
        if (wave >= 0) {
            wave += 0.32;
            for (int k = 0; k < 48; k++) {
                double a = 2 * Math.PI * k / 48;
                world.spawnParticle(Particle.BLOCK, at(0.5 + Math.cos(a) * wave, 1.05, 0.5 + Math.sin(a) * wave), 1, 0, 0, 0, 0,
                        Material.STONE.createBlockData());
            }
            for (Player p : alivePlayers()) {
                Location l = p.getLocation();
                double d = Math.hypot(l.getX() - ox - 0.5, l.getZ() - oz - 0.5);
                if (Math.abs(d - wave) < 0.55 && l.getY() - oy < 1.2 && p.isOnGround()) {
                    stun(p, l.toVector().subtract(at(0.5, 1, 0.5).toVector()));
                }
            }
            if (wave > HALF) {
                wave = -1;
            }
        }
        if (cycle == 80) {
            Player target = randomPlayer();
            if (target != null) {
                slamAt = new double[] {target.getLocation().getX() - ox, target.getLocation().getZ() - oz};
                slamTick = 0;
            }
        }
        if (slamTick >= 0) {
            slamTick++;
            if (slamTick < 30) {
                // Warnung: roter Kreis
                for (int k = 0; k < 18; k++) {
                    double a = 2 * Math.PI * k / 18;
                    world.spawnParticle(Particle.DUST, at(slamAt[0] + Math.cos(a) * 1.6, 1.1, slamAt[1] + Math.sin(a) * 1.6), 1,
                            new Particle.DustOptions(Color.RED, 1.4f));
                }
                hx = 0.5;
                hz = 0.5;
                hy = 4.5;
                yaw = yawTo(hx, hz, slamAt[0], slamAt[1]);
                headOpen = false;
            } else if (slamTick == 30) {
                hx = slamAt[0];
                hz = slamAt[1];
                hy = 1.0;
                headOpen = true;
                world.spawnParticle(Particle.EXPLOSION, at(hx, 1, hz), 2);
                soundAll("minecraft:entity.generic.explode", 0.7f);
                for (Player p : alivePlayers()) {
                    Location l = p.getLocation();
                    if (Math.hypot(l.getX() - ox - hx, l.getZ() - oz - hz) < 1.7) {
                        stun(p, l.toVector().subtract(at(hx, 1, hz).toVector()));
                    }
                }
            } else if (slamTick < 30 + 55) {
                hy = 1.0;
                headOpen = true;
            } else {
                slamTick = -1;
                hx = 0.5;
                hz = 0.5;
                hy = 4.5;
                headOpen = false;
            }
        } else {
            hx = 0.5;
            hz = 0.5;
            hy = 4.5 + 0.3 * Math.sin(phaseTick * 0.15);
            Player near = nearest(hx, hz);
            if (near != null) {
                yaw = yawTo(hx, hz, near.getLocation().getX() - ox, near.getLocation().getZ() - oz);
            }
            headOpen = false;
        }
    }

    private void trimTrail() {
        while (trail.size() > 100) {
            trail.removeLast();
        }
    }

    private void pickWeak() {
        weak.clear();
        List<Integer> idx = new ArrayList<>();
        for (int i = 2; i < SEGMENTS; i++) {
            idx.add(i);
        }
        java.util.Collections.shuffle(idx, rng);
        for (int k = 0; k < 3 && k < idx.size(); k++) {
            weak.add(new int[] {idx.get(k), 3});
        }
        for (int i = 0; i < SEGMENTS; i++) {
            body.get(i).setGlowing(isWeak(i));
        }
    }

    private boolean isWeak(int seg) {
        return phase == 2 && weak.stream().anyMatch(w -> w[0] == seg && w[1] > 0);
    }

    // ---------- Felsbrocken ----------

    private void spit() {
        Player target = randomPlayer();
        if (target == null) {
            return;
        }
        Rock r = new Rock();
        r.x = target.getLocation().getX() - ox;
        r.z = target.getLocation().getZ() - oz;
        r.left = ROCK_FALL;
        r.display = track(world.spawn(at(r.x, 12, r.z), BlockDisplay.class, d -> {
            d.setBlock(Material.COBBLESTONE.createBlockData());
            d.setTeleportDuration(1);
            d.setPersistent(false);
            d.setTransformation(new Transformation(new Vector3f(-0.6f, -0.6f, -0.6f), new AxisAngle4f(),
                    new Vector3f(1.2f, 1.2f, 1.2f), new AxisAngle4f()));
        }));
        rocks.add(r);
        world.playSound(at(hx, hy, hz), "minecraft:entity.llama.spit", 1f, 0.5f);
    }

    private void rocks() {
        Iterator<Rock> it = rocks.iterator();
        while (it.hasNext()) {
            Rock r = it.next();
            r.left--;
            double y = 1 + 11.0 * r.left / ROCK_FALL;
            r.display.teleport(at(r.x, y, r.z));
            if (r.left % 3 == 0) {
                for (int k = 0; k < 10; k++) {
                    double a = 2 * Math.PI * k / 10;
                    world.spawnParticle(Particle.SMOKE, at(r.x + Math.cos(a) * 1.3, 1.05, r.z + Math.sin(a) * 1.3), 1, 0, 0, 0, 0);
                }
            }
            if (r.left <= 0) {
                world.spawnParticle(Particle.BLOCK, at(r.x, 1.1, r.z), 30, 0.6, 0.3, 0.6, 0, Material.COBBLESTONE.createBlockData());
                world.playSound(at(r.x, 1, r.z), "minecraft:block.stone.break", 1f, 0.6f);
                for (Player p : alivePlayers()) {
                    Location l = p.getLocation();
                    if (Math.hypot(l.getX() - ox - r.x, l.getZ() - oz - r.z) < 1.5 && l.getY() - oy < 2.5) {
                        stun(p, l.toVector().subtract(at(r.x, 1, r.z).toVector()));
                    }
                }
                untrack(r.display);
                it.remove();
            }
        }
    }

    // ---------- Anzeige ----------

    private void show() {
        Location h = at(hx, hy, hz);
        h.setYaw(yaw);
        for (BlockDisplay d : head) {
            d.teleport(h);
        }
        headBox.teleport(at(hx, hy - 1.2, hz));
        label.teleport(at(hx, hy + 2.2, hz));
        if (ticks % 5 == 0) {
            label.text(labelText());
        }
        // Körper folgt der Spur des Kopfes
        List<double[]> path = new ArrayList<>(trail);
        double need = SPACING;
        int seg = 0;
        double[] prev = path.isEmpty() ? new double[] {hx, -6, hz} : path.getFirst();
        double walked = 0;
        for (int i = 1; i < path.size() && seg < SEGMENTS; i++) {
            double[] cur = path.get(i);
            walked += Math.sqrt(Math.pow(cur[0] - prev[0], 2) + Math.pow(cur[1] - prev[1], 2) + Math.pow(cur[2] - prev[2], 2));
            prev = cur;
            if (walked >= need * (seg + 1) || phase != 2) {
                if (phase != 2 && i % 3 != 0) {
                    continue;
                }
                Location l = at(cur[0], cur[1], cur[2]);
                body.get(seg).teleport(l);
                bodyBoxes.get(seg).teleport(l.clone().subtract(0, 0.65, 0));
                seg++;
            }
        }
        for (; seg < SEGMENTS; seg++) {
            body.get(seg).teleport(at(hx, -6, hz));
            bodyBoxes.get(seg).teleport(at(hx, -8, hz));
        }
    }

    private Component labelText() {
        int cells = 20;
        int full = (int) Math.round((double) Math.max(0, hp) / max * cells);
        String bar = "<red>" + "|".repeat(full) + "</red><dark_gray>" + "|".repeat(cells - full) + "</dark_gray>";
        return Texts.MM.deserialize("<gold><b>Tatzelwurm</b></gold>\n" + bar);
    }

    // ---------- Treffer ----------

    @Override
    public void onEntityClick(Player p, Entity target) {
        UUID u = p.getUniqueId();
        if (stunnedUntil.getOrDefault(u, -1) > ticks || ticks - lastHit.getOrDefault(u, -100) < WurmRegeln.HIT_GAP) {
            return;
        }
        boolean isHead = target.equals(headBox) && headOpen;
        int seg = bodyBoxes.indexOf(target);
        boolean isWeak = seg >= 0 && isWeak(seg);
        if (!isHead && !isWeak) {
            if (target.equals(headBox) || seg >= 0) {
                Ui.sound(p, "minecraft:block.stone.hit", 0.8f);
                bar(p, target.equals(headBox) ? "mini.tatzelwurm.closed" : "mini.tatzelwurm.scales");
            }
            return;
        }
        lastHit.put(u, ticks);
        int dmg = WurmRegeln.damage(isHead);
        hp -= dmg;
        score.add(u, dmg);
        if (isWeak) {
            for (int[] w : weak) {
                if (w[0] == seg) {
                    w[1]--;
                }
            }
            body.get(seg).setGlowing(isWeak(seg));
            if (weak.stream().allMatch(w -> w[1] <= 0)) {
                pickWeak();
            }
        }
        Location fx = isHead ? at(hx, hy, hz) : target.getLocation().add(0, 0.6, 0);
        world.spawnParticle(Particle.CRIT, fx, 12, 0.4, 0.4, 0.4, 0.2);
        world.playSound(fx, "minecraft:entity.player.attack.strong", 1f, isHead ? 0.8f : 1.2f);
        if (hp <= 0 && !won) {
            won = true;
            headOpen = false;
            world.spawnParticle(Particle.EXPLOSION_EMITTER, at(hx, hy, hz), 1);
            soundAll("minecraft:entity.ender_dragon.death", 1.4f);
            say("mini.tatzelwurm.won", Placeholder.unparsed("name", p.getName()));
        }
    }

    /** Kopf-Berührung: wer dem Kopf zu nah kommt, wird umgestoßen (nicht im Loch-Abschnitt). */
    private void touch() {
        if (phase == 1 || hy > 2.5 || (phase == 3 && headOpen)) {
            return; // liegt der Kopf nach dem Schlag am Boden, ist er harmlos – jetzt zuschlagen
        }
        for (Player p : alivePlayers()) {
            Location l = p.getLocation();
            double d = Math.hypot(l.getX() - ox - hx, l.getZ() - oz - hz);
            if (d < 1.7 && Math.abs(l.getY() - oy - hy) < 2.2) {
                stun(p, l.toVector().subtract(at(hx, l.getY() - oy, hz).toVector()));
            }
        }
    }

    private void stun(Player p, Vector away) {
        UUID u = p.getUniqueId();
        if (stunnedUntil.getOrDefault(u, -1) > ticks) {
            return;
        }
        stunnedUntil.put(u, ticks + WurmRegeln.STUN);
        stuns++;
        Vector push = away.setY(0);
        if (push.lengthSquared() < 1e-4) {
            push = new Vector(rng.nextDouble() - 0.5, 0, rng.nextDouble() - 0.5);
        }
        p.setVelocity(push.normalize().multiply(0.9).setY(0.45));
        effect(p, new PotionEffect(PotionEffectType.SLOWNESS, WurmRegeln.STUN, 2, false, false));
        Ui.title(p, Texts.text(plugin.lang(p), "mini.tatzelwurm.stunned"), Component.empty(), 900);
        Ui.sound(p, "minecraft:entity.player.hurt", 0.8f);
    }

    // ---------- Helfer ----------

    private Player randomPlayer() {
        List<Player> list = alivePlayers().stream().filter(p -> stunnedUntil.getOrDefault(p.getUniqueId(), -1) <= ticks).toList();
        return list.isEmpty() ? null : list.get(rng.nextInt(list.size()));
    }

    private Player nearest(double x, double z) {
        Player best = null;
        double bd = Double.MAX_VALUE;
        for (Player p : alivePlayers()) {
            double d = Math.hypot(p.getLocation().getX() - ox - x, p.getLocation().getZ() - oz - z);
            if (d < bd) {
                bd = d;
                best = p;
            }
        }
        return best;
    }

    private static float yawTo(double x, double z, double tx, double tz) {
        return (float) Math.toDegrees(Math.atan2(-(tx - x), tz - z));
    }
}
