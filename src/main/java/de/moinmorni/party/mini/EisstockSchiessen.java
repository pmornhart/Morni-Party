package de.moinmorni.party.mini;

import de.moinmorni.party.Items;
import de.moinmorni.party.Texts;
import de.moinmorni.party.Ui;
import de.moinmorni.party.world.Plan;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Eisstockschießen: Alle stehen am Abwurf und werfen gleichzeitig – drei Würfe pro Person. Ein Kraftbalken pendelt
 * hin und her; ein Klick wirft den Eisstock in Blickrichtung. Am Ende zählt, wie nah die eigenen Stöcke an der Daube
 * liegen (5, 3, 2, 1 Punkte je Ring). Stöcke stoßen sich gegenseitig weg – auch die der anderen.
 */
final class EisstockSchiessen extends MiniRun {

    static final int HALF = 5;
    static final int ROUNDS = 3;
    static final double THROW_Z = -13.5;
    static final double STAND_Z = -15.5;
    private static final int AIM = 7 * SECOND;
    private static final int PERIOD = 40;
    private static final int MAX_SLIDE = 9 * SECOND;
    private static final int PAUSE = 2 * SECOND;
    private static final int CELLS = 20;

    private enum Phase { AIM, SLIDE, PAUSE }

    private final Eisbahn bahn = new Eisbahn();
    private final Map<Eisbahn.Stone, BlockDisplay[]> looks = new IdentityHashMap<>();
    private final Map<UUID, Double> lane = new HashMap<>();
    private final Map<UUID, String> colour = new HashMap<>();
    private final Set<UUID> thrown = new HashSet<>();
    private Phase phase = Phase.PAUSE;
    private int phaseStart;
    private int round;

    EisstockSchiessen(Setup s) {
        super(s);
    }

    static void build(Plan p) {
        // Abwurf (Holzpodest) mit Geländer dahinter
        for (int x = -HALF - 1; x <= HALF + 1; x++) {
            for (int z = -18; z <= -15; z++) {
                p.set(x, 0, z, "spruce_planks");
                p.set(x, -1, z, "spruce_planks");
            }
            p.set(x, 1, -18, "spruce_fence");
            p.set(x, 2, -18, "barrier");
            p.set(x, 3, -18, "barrier");
        }
        for (int z = -18; z <= -15; z++) {
            for (int y = 1; y <= 3; y++) {
                p.set(-HALF - 2, y, z, "barrier");
                p.set(HALF + 2, y, z, "barrier");
            }
        }
        // Bahn mit Ringen um die Daube
        for (int x = -HALF; x <= HALF; x++) {
            for (int z = -14; z <= 21; z++) {
                double d = Math.hypot(x - Eisbahn.TARGET_X, z - Eisbahn.TARGET_Z);
                String ice = z == -14 ? "blue_ice"
                        : d <= Eisbahn.RINGS[0] ? "red_concrete"
                        : d <= Eisbahn.RINGS[1] ? "white_concrete"
                        : d <= Eisbahn.RINGS[2] ? "light_blue_concrete"
                        : d <= Eisbahn.RINGS[3] ? "blue_concrete" : "packed_ice";
                p.set(x, 0, z, ice);
                p.set(x, -1, z, "packed_ice");
            }
        }
        // Banden aus Holz, hinten eine Schneewehe
        for (int z = -14; z <= 22; z++) {
            for (int x : new int[] {-HALF - 1, HALF + 1}) {
                p.set(x, 0, z, "stripped_spruce_log[axis=z]");
                p.set(x, -1, z, "spruce_planks");
                p.set(x, 1, z, z % 4 == 0 ? "spruce_fence" : "spruce_slab[type=bottom]");
            }
        }
        for (int x = -HALF; x <= HALF; x++) {
            p.set(x, 0, 22, "snow_block");
            p.set(x, 1, 22, "snow[layers=4]");
            p.set(x, 0, 23, "snow_block");
            p.set(x, 1, 23, "snow_block");
            p.set(x, 2, 23, "snow[layers=3]");
        }
    }

    /** Wo die Werfer stehen (quer zur Bahn verteilt). */
    static double laneX(int i, int n) {
        if (n <= 1) {
            return 0;
        }
        double spacing = Math.min(1.6, (2.0 * HALF - 3) / (n - 1));
        return (i - (n - 1) / 2.0) * spacing;
    }

    @Override
    protected List<Location> spawns(int n) {
        List<Location> out = new ArrayList<>();
        for (int i = 0; i < Math.max(1, n); i++) {
            double x = laneX(i, n) + 0.5;
            out.add(facing(x, 1, STAND_Z + 0.5, x, 1, Eisbahn.TARGET_Z + 0.5));
        }
        return out;
    }

    @Override
    public boolean frozen() {
        return true;
    }

    @Override
    public Location viewPoint() {
        return facing(0.5, 9, -20, 0.5, 0, 6);
    }

    @Override
    protected void equip(Player p) {
        String lang = plugin.lang(p);
        p.getInventory().setItem(0, Items.named(Material.POLISHED_ANDESITE_SLAB, 1,
                Texts.text(lang, "mini.eisstockschiessen.item"), Texts.lines(lang, "mini.eisstockschiessen.item.lore")));
    }

    @Override
    protected void onStart() {
        List<String> palette = TrachtenFarben.COLOURS;
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            lane.put(p.getUniqueId(), laneX(i, players.size()));
            String c = palette.get(i % palette.size());
            colour.put(p.getUniqueId(), c);
            String lang = plugin.lang(p);
            p.sendMessage(Texts.msg(lang, "mini.eisstockschiessen.colour",
                    Placeholder.component("colour", Texts.text(lang, "colour." + c))));
        }
        track(world.spawn(at(Eisbahn.TARGET_X + 0.5, 1, Eisbahn.TARGET_Z + 0.5), BlockDisplay.class, d -> {
            d.setBlock(Material.STRIPPED_OAK_WOOD.createBlockData());
            d.setPersistent(false);
            d.setTransformation(new Transformation(new Vector3f(-0.15f, 0f, -0.15f), new Quaternionf(),
                    new Vector3f(0.3f, 0.3f, 0.3f), new Quaternionf()));
        }));
        nextRound();
    }

    private void nextRound() {
        round++;
        thrown.clear();
        phase = Phase.AIM;
        phaseStart = ticks;
        for (Player p : alivePlayers()) {
            String lang = plugin.lang(p);
            Ui.title(p, Texts.text(lang, "mini.eisstockschiessen.round", Placeholder.unparsed("n", String.valueOf(round)),
                    Placeholder.unparsed("of", String.valueOf(ROUNDS))),
                    Texts.text(lang, "mini.eisstockschiessen.colour.short",
                            Placeholder.component("colour", Texts.text(lang, "colour." + colour.get(p.getUniqueId())))),
                    1500);
        }
    }

    /** Kraft im Augenblick: pendelt 0 → 1 → 0. */
    private double power() {
        double t = Math.floorMod(ticks - phaseStart, PERIOD) / (PERIOD / 2.0);
        return 1 - Math.abs(t - 1);
    }

    @Override
    protected void onTick() {
        switch (phase) {
            case AIM -> {
                if (ticks % 2 == 0) {
                    double pw = power();
                    for (Player p : alivePlayers()) {
                        if (!thrown.contains(p.getUniqueId())) {
                            bar(p, "mini.eisstockschiessen.power", Placeholder.parsed("bar", powerBar(pw)),
                                    Placeholder.unparsed("n", String.valueOf(Math.round(pw * 100))));
                        }
                    }
                }
                if (ticks - phaseStart >= AIM) {
                    double pw = power();
                    for (Player p : alivePlayers()) {
                        if (!thrown.contains(p.getUniqueId())) {
                            throwStone(p, pw);
                        }
                    }
                }
                if (!alivePlayers().isEmpty() && alivePlayers().stream().allMatch(p -> thrown.contains(p.getUniqueId()))) {
                    phase = Phase.SLIDE;
                    phaseStart = ticks;
                }
            }
            case SLIDE -> slide();
            case PAUSE -> {
                if (ticks - phaseStart >= PAUSE) {
                    if (round >= ROUNDS) {
                        finish();
                    } else {
                        nextRound();
                    }
                }
            }
        }
    }

    /** Kraftbalken: grün, die gute Zone (60–75 %) gold, zu viel rot. */
    static String powerBar(double power) {
        int filled = (int) Math.round(power * CELLS);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < CELLS; i++) {
            String tag = i >= filled ? "dark_gray" : i < 12 ? "green" : i < 15 ? "gold" : "red";
            b.append('<').append(tag).append('>').append(i < filled ? '■' : '□').append("</").append(tag).append('>');
        }
        return b.toString();
    }

    @Override
    public void onInteract(Player p, PlayerInteractEvent e) {
        e.setCancelled(true);
        if (e.getAction() == Action.PHYSICAL || phase != Phase.AIM || !score.alive(p.getUniqueId())
                || thrown.contains(p.getUniqueId())) {
            return;
        }
        throwStone(p, power());
    }

    private void throwStone(Player p, double power) {
        UUID u = p.getUniqueId();
        thrown.add(u);
        double yaw = Math.floorMod((long) Math.round(p.getLocation().getYaw()), 360L);
        if (yaw > 180) {
            yaw -= 360;
        }
        Eisbahn.Stone s = bahn.launch(u, lane.getOrDefault(u, 0.0), THROW_Z, -Math.toRadians(yaw), power);
        Material concrete = Material.matchMaterial(colour.getOrDefault(u, "white") + "_concrete");
        Location l = where(s);
        BlockDisplay base = track(world.spawn(l, BlockDisplay.class, d -> {
            d.setBlock((concrete == null ? Material.WHITE_CONCRETE : concrete).createBlockData());
            d.setTeleportDuration(1);
            d.setPersistent(false);
            d.setTransformation(new Transformation(new Vector3f(-0.35f, 0f, -0.35f), new Quaternionf(),
                    new Vector3f(0.7f, 0.25f, 0.7f), new Quaternionf()));
        }));
        BlockDisplay handle = track(world.spawn(l, BlockDisplay.class, d -> {
            d.setBlock(Material.DARK_OAK_PLANKS.createBlockData());
            d.setTeleportDuration(1);
            d.setPersistent(false);
            d.setTransformation(new Transformation(new Vector3f(-0.07f, 0.25f, -0.07f), new Quaternionf(),
                    new Vector3f(0.14f, 0.32f, 0.14f), new Quaternionf()));
        }));
        looks.put(s, new BlockDisplay[] {base, handle});
        Ui.sound(p, "minecraft:block.stone.place", 0.7f);
        bar(p, "mini.eisstockschiessen.thrown", Placeholder.unparsed("n", String.valueOf(Math.round(power * 100))));
    }

    private Location where(Eisbahn.Stone s) {
        return at(s.x + 0.5, 1, s.z + 0.5);
    }

    private void slide() {
        if (bahn.step() > 0) {
            world.playSound(at(0.5, 1, Eisbahn.TARGET_Z + 0.5), "minecraft:block.stone.hit", 1.2f, 0.8f);
        }
        for (var e : looks.entrySet()) {
            Eisbahn.Stone s = e.getKey();
            BlockDisplay[] d = e.getValue();
            if (d[0] == null) {
                continue;
            }
            if (s.out) {
                world.spawnParticle(Particle.SNOWFLAKE, where(s), 10, 0.3, 0.2, 0.3, 0.02);
                for (BlockDisplay x : d) {
                    untrack(x);
                }
                d[0] = null;
                continue;
            }
            Location l = where(s);
            if (d[0].getLocation().distanceSquared(l) > 1e-6) {
                for (BlockDisplay x : d) {
                    x.teleport(l);
                }
            }
        }
        if (!bahn.moving() || ticks - phaseStart >= MAX_SLIDE) {
            endRound();
        }
    }

    private void endRound() {
        phase = Phase.PAUSE;
        phaseStart = ticks;
        Map<UUID, Integer> now = bahn.score();
        UUID lead = null;
        for (Player p : players) {
            int pts = now.getOrDefault(p.getUniqueId(), 0);
            score.set(p.getUniqueId(), pts);
            if (pts > 0 && (lead == null || pts > now.getOrDefault(lead, 0))) {
                lead = p.getUniqueId();
            }
        }
        if (lead == null) {
            say("mini.eisstockschiessen.nobody", Placeholder.unparsed("n", String.valueOf(round)));
        } else {
            Player best = plugin.getServer().getPlayer(lead);
            say("mini.eisstockschiessen.lead", Placeholder.unparsed("n", String.valueOf(round)),
                    Placeholder.unparsed("name", best == null ? "?" : best.getName()),
                    Placeholder.unparsed("points", String.valueOf(now.get(lead))));
        }
        soundAll("minecraft:block.note_block.chime", 1.2f);
    }

    @Override
    protected void onFinish() {
        looks.clear();
    }
}
