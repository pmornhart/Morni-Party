package net.morni.party.mini;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.morni.party.Items;
import net.morni.party.Texts;
import net.morni.party.Ui;
import net.morni.party.world.Plan;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Schuhplattler: Zur Musik im Dreivierteltakt wird eine Bewegung angesagt – Springen, Ducken, Klatschen (klicken)
 * oder Stillstehen. Auf die Eins des nächsten Takts (der Stampfer) muss sie sitzen: genau = 2 Punkte, knapp = 1.
 */
final class SchuhPlattler extends MiniRun {

    static final int HALF = 8;
    private static final int FIRST = SECOND;
    /** Eigene Melodie (Halbtöne über Fis, -1 = Pause), ein Ton pro Schlag, acht Takte. */
    private static final int[] MELODY = {8, 13, 17, 15, 13, 12, 10, 13, 17, 15, -1, -1, 8, 12, 15, 13, 12, 10, 8, 10, 12,
        13, -1, -1};
    /** Grundton je Takt (G, D, G, D, G, C, D, G) und der Begleitakkord dazu. */
    private static final int[] BASS = {1, 8, 1, 8, 1, 6, 8, 1};
    private static final int[][] CHORD = {{5, 8}, {3, 12}, {5, 8}, {3, 12}, {5, 8}, {6, 10}, {3, 12}, {5, 8}};

    private final Map<UUID, List<int[]>> moves = new HashMap<>();
    private final Map<UUID, Location> windowStart = new HashMap<>();
    private Takt.Move cue;
    private Takt.Move last;
    private int due = -1;

    SchuhPlattler(Setup s) {
        super(s);
    }

    static void build(Plan p) {
        for (int x = -HALF - 1; x <= HALF + 1; x++) {
            for (int z = -HALF - 1; z <= HALF + 1; z++) {
                boolean edge = Math.abs(x) == HALF + 1 || Math.abs(z) == HALF + 1;
                p.set(x, -1, z, "spruce_planks");
                p.set(x, 0, z, edge ? "dark_oak_planks"
                        : Math.floorMod(x, 3) == 0 && Math.floorMod(z, 3) == 0 ? "stripped_birch_wood[axis=y]"
                        : Math.floorMod(x + z, 2) == 0 ? "spruce_planks" : "stripped_spruce_wood[axis=y]");
                if (edge) {
                    p.set(x, 1, z, "dark_oak_fence");
                    for (int h = 2; h <= 4; h++) {
                        p.set(x, h, z, "barrier");
                    }
                }
            }
        }
    }

    /** Musiknoten: Halbtöne über Fis → Tonhöhe des Notenblocks. */
    static float pitch(int semitones) {
        return (float) Math.pow(2, (semitones - 12) / 12.0);
    }

    @Override
    protected List<Location> spawns(int n) {
        List<Location> out = new ArrayList<>();
        int cols = n <= 4 ? n : n <= 9 ? 3 : 4;
        cols = Math.max(1, cols);
        int rows = (n + cols - 1) / cols;
        for (int i = 0; i < Math.max(1, n); i++) {
            int c = i % cols;
            int r = i / cols;
            double x = (c - (cols - 1) / 2.0) * 3.5 + 0.5;
            double z = (r - (rows - 1) / 2.0) * 3.5 + 0.5;
            out.add(facing(x, 1, z, x, 1, HALF + 6));
        }
        return out;
    }

    @Override
    public Location viewPoint() {
        return facing(0.5, 9, -HALF - 9, 0.5, 1, 0.5);
    }

    @Override
    protected void equip(Player p) {
        String lang = plugin.lang(p);
        p.getInventory().setItem(0, Items.named(Material.FEATHER, 1, Texts.text(lang, "mini.schuhplattler.item"),
                Texts.lines(lang, "mini.schuhplattler.item.lore")));
    }

    @Override
    protected void onTick() {
        if (ticks % Takt.BEAT == 0) {
            music(ticks / Takt.BEAT);
        }
        int lastCue = durationTicks() - Takt.LEAD - Takt.WINDOW - 2;
        int t = ticks - FIRST;
        if (t >= 0 && t % Takt.CYCLE == 0 && ticks <= lastCue) {
            announce();
        }
        if (due >= 0) {
            if (ticks == due - Takt.WINDOW) {
                alivePlayers().forEach(p -> windowStart.put(p.getUniqueId(), p.getLocation()));
            }
            if (ticks == due) {
                soundAll("minecraft:block.note_block.basedrum", 1f);
                world.spawnParticle(Particle.NOTE, at(0.5, 3, 0.5), 6, HALF / 2.0, 1, HALF / 2.0, 1);
                alivePlayers().forEach(p -> bar(p, "mini.schuhplattler.now"));
            }
            if (ticks == due + Takt.WINDOW + 1) {
                judgeAll();
                due = -1;
            }
        }
    }

    private void music(int beat) {
        int barNo = (beat / 3) % BASS.length;
        int inBar = beat % 3;
        if (inBar == 0) {
            soundAll("minecraft:block.note_block.bass", pitch(BASS[barNo] + 12));
        } else {
            for (int n : CHORD[barNo]) {
                soundAll("minecraft:block.note_block.guitar", pitch(n));
            }
        }
        int m = MELODY[beat % MELODY.length];
        if (m >= 0) {
            soundAll("minecraft:block.note_block.flute", pitch(m));
        }
    }

    private void announce() {
        cue = Takt.pick(rng.nextInt(100), last);
        last = cue;
        due = ticks + Takt.LEAD;
        moves.clear();
        windowStart.clear();
        String key = "mini.schuhplattler.move." + cue.name().toLowerCase(java.util.Locale.ROOT);
        for (Player p : audience()) {
            String lang = plugin.lang(p);
            Ui.title(p, Texts.text(lang, key), Texts.text(lang, "mini.schuhplattler.onone"), 1300);
        }
    }

    private void record(Player p, Takt.Move m) {
        if (due < 0 || !score.alive(p.getUniqueId())) {
            return;
        }
        int offset = ticks - due;
        if (offset >= -Takt.WINDOW - 4 && offset <= Takt.WINDOW) {
            moves.computeIfAbsent(p.getUniqueId(), u -> new ArrayList<>()).add(new int[] {m.ordinal(), offset});
        }
    }

    @Override
    public void onJump(Player p) {
        record(p, Takt.Move.JUMP);
    }

    @Override
    public void onSneak(Player p, boolean sneaking) {
        if (sneaking) {
            record(p, Takt.Move.DUCK);
        }
    }

    @Override
    public void onInteract(Player p, PlayerInteractEvent e) {
        e.setCancelled(true);
        if (e.getAction() != Action.PHYSICAL) {
            record(p, Takt.Move.CLAP);
        }
    }

    private void judgeAll() {
        for (Player p : alivePlayers()) {
            UUID u = p.getUniqueId();
            List<Takt.Done> done = new ArrayList<>();
            for (int[] m : moves.getOrDefault(u, List.of())) {
                done.add(new Takt.Done(Takt.Move.values()[m[0]], m[1]));
            }
            Location from = windowStart.get(u);
            double moved = from == null || !from.getWorld().equals(p.getWorld()) ? 0
                    : Math.hypot(from.getX() - p.getLocation().getX(), from.getZ() - p.getLocation().getZ());
            int pts = Takt.judge(cue, done, moved);
            if (pts > 0) {
                score.add(u, pts);
                Ui.sound(p, "minecraft:entity.experience_orb.pickup", pts > 1 ? 1.5f : 1.1f);
                bar(p, pts > 1 ? "mini.schuhplattler.perfect" : "mini.schuhplattler.good",
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("n",
                                String.valueOf(score.points(u))));
            } else {
                bar(p, "mini.schuhplattler.miss");
            }
        }
    }
}
