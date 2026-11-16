package net.morni.party.mini;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.morni.party.Items;
import net.morni.party.Texts;
import net.morni.party.Ui;
import net.morni.party.world.Plan;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Musterweber: Jede und jeder steht vor einem eigenen Webrahmen. Darauf erscheint kurz ein Muster aus 4 × 4 Feldern –
 * merken! Dann ist der Rahmen leer: Farbe in der Schnellleiste wählen und die Felder anklicken. Jedes richtige Feld
 * ist ein Punkt, ganz richtig gibt 4 dazu. Drei Runden mit immer mehr Farben.
 */
final class MusterWeber extends MiniRun {

    static final int W = 13;
    static final int[] SLOT_CENTER = {-9, -3, 3, 9};
    /** Reihenfolge der Rahmen (Seite, Platz): zuerst die mittleren, dann außen – bis 16 Leute. */
    static final int[][] ORDER = {{0, 1}, {1, 2}, {2, 1}, {3, 2}, {0, 2}, {1, 1}, {2, 2}, {3, 1}, {0, 0}, {1, 3},
        {2, 0}, {3, 3}, {0, 3}, {1, 0}, {2, 3}, {3, 0}};
    private static final int BUILD = 10 * SECOND;
    private static final int PAUSE = 2 * SECOND;

    private enum Phase { SHOW, BUILD, PAUSE }

    private final Map<UUID, Integer> loom = new HashMap<>();
    private Phase phase = Phase.PAUSE;
    private int phaseEnd;
    private int round;
    private int[] target = new int[Webmuster.CELLS];

    MusterWeber(Setup s) {
        super(s);
    }

    /** Feld (Spalte, Reihe) des Rahmens k als {x, y, z}. */
    static int[] cell(int k, int col, int row) {
        int side = ORDER[k][0];
        int c = SLOT_CENTER[ORDER[k][1]];
        int y = 1 + row;
        return switch (side) {
            case 0 -> new int[] {c - 2 + col, y, -W};
            case 1 -> new int[] {c + 1 - col, y, W};
            case 2 -> new int[] {-W, y, c + 1 - col};
            default -> new int[] {W, y, c - 2 + col};
        };
    }

    /** Standplatz vor dem Rahmen k: {x, z, Blickziel x, Blickziel z}. */
    static double[] stand(int k) {
        int side = ORDER[k][0];
        double c = SLOT_CENTER[ORDER[k][1]];
        return switch (side) {
            case 0 -> new double[] {c, -W + 3.5, c, -W};
            case 1 -> new double[] {c, W - 2.5, c, W + 1};
            case 2 -> new double[] {-W + 3.5, c, -W, c};
            default -> new double[] {W - 2.5, c, W + 1, c};
        };
    }

    static void build(Plan p) {
        for (int x = -W; x <= W; x++) {
            for (int z = -W; z <= W; z++) {
                p.set(x, -1, z, "stone");
                p.set(x, 0, z, Math.abs(x) == W || Math.abs(z) == W ? "dark_oak_planks"
                        : Math.floorMod(x + z, 2) == 0 ? "spruce_planks" : "birch_planks");
            }
        }
        for (int i = -W - 1; i <= W + 1; i++) {
            for (int[] c : new int[][] {{i, -W - 1}, {i, W + 1}, {-W - 1, i}, {W + 1, i}}) {
                for (int y = 0; y <= 6; y++) {
                    p.set(c[0], y, c[1], y == 6 ? "dark_oak_slab[type=bottom]" : "spruce_planks");
                }
            }
        }
        for (int k = 0; k < ORDER.length; k++) {
            // Rahmen: Pfosten links und rechts, Balken oben und unten
            int[] a = cell(k, 0, 0);
            int[] b = cell(k, 3, 0);
            int dx = Integer.signum(b[0] - a[0]);
            int dz = Integer.signum(b[2] - a[2]);
            for (int y = 0; y <= 5; y++) {
                p.set(a[0] - dx, y, a[2] - dz, "dark_oak_log[axis=y]");
                p.set(b[0] + dx, y, b[2] + dz, "dark_oak_log[axis=y]");
            }
            for (int col = 0; col < Webmuster.SIZE; col++) {
                int[] bottom = cell(k, col, 0);
                p.set(bottom[0], 0, bottom[2], "dark_oak_planks");
                p.set(bottom[0], 5, bottom[2], "stripped_dark_oak_log[axis=" + (dx != 0 ? "x" : "z") + "]");
                for (int row = 0; row < Webmuster.SIZE; row++) {
                    int[] c = cell(k, col, row);
                    p.set(c[0], c[1], c[2], Webmuster.BLANK + "_wool");
                }
            }
        }
    }

    @Override
    protected GameMode gameMode() {
        return GameMode.SURVIVAL;
    }

    @Override
    public boolean frozen() {
        return true;
    }

    @Override
    protected List<Location> spawns(int n) {
        List<Location> out = new ArrayList<>();
        for (int i = 0; i < Math.max(1, n); i++) {
            double[] s = stand(i % ORDER.length);
            out.add(facing(s[0], 1, s[1], s[2], 2.6, s[3]));
        }
        return out;
    }

    @Override
    public Location viewPoint() {
        return facing(0.5, 16, 0.5, 0.5, 0, -W);
    }

    @Override
    protected void equip(Player p) {
        // Farben kommen mit jeder Runde.
    }

    @Override
    protected void onStart() {
        for (int i = 0; i < players.size(); i++) {
            loom.put(players.get(i).getUniqueId(), i % ORDER.length);
        }
        nextRound();
    }

    private Block cellBlock(int k, int i) {
        int[] c = cell(k, i % Webmuster.SIZE, i / Webmuster.SIZE);
        return block(c[0], c[1], c[2]);
    }

    private static Material wool(String colour) {
        Material m = Material.matchMaterial(colour + "_wool");
        return m == null ? Material.WHITE_WOOL : m;
    }

    private void paintAll(int[] cells) {
        for (int k : loom.values()) {
            for (int i = 0; i < Webmuster.CELLS; i++) {
                cellBlock(k, i).setType(cells == null ? wool(Webmuster.BLANK) : wool(Webmuster.PALETTE.get(cells[i])), false);
            }
        }
    }

    private void nextRound() {
        round++;
        target = Webmuster.make(rng, Webmuster.colours(round));
        paintAll(target);
        phase = Phase.SHOW;
        phaseEnd = ticks + Webmuster.showTicks(round);
        for (Player p : alivePlayers()) {
            p.getInventory().clear();
            String lang = plugin.lang(p);
            Ui.title(p, Texts.text(lang, "mini.musterweber.remember"), Texts.text(lang, "mini.musterweber.round",
                    Placeholder.unparsed("n", String.valueOf(round)), Placeholder.unparsed("of",
                            String.valueOf(Webmuster.ROUNDS))), 1500);
            Ui.sound(p, "minecraft:block.note_block.chime", 1f);
        }
    }

    @Override
    protected void onTick() {
        if (ticks < phaseEnd) {
            if (phase == Phase.BUILD && (phaseEnd - ticks) % SECOND == 0) {
                int left = (phaseEnd - ticks) / SECOND;
                alivePlayers().forEach(p -> bar(p, "mini.musterweber.left", Placeholder.unparsed("s", String.valueOf(left))));
            }
            return;
        }
        switch (phase) {
            case SHOW -> {
                paintAll(null);
                phase = Phase.BUILD;
                phaseEnd = ticks + BUILD;
                for (Player p : alivePlayers()) {
                    String lang = plugin.lang(p);
                    for (int c = 0; c < Webmuster.colours(round); c++) {
                        String colour = Webmuster.PALETTE.get(c);
                        ItemStack it = Items.named(wool(colour), 1, Texts.text(lang, "colour." + colour), null);
                        p.getInventory().setItem(c, it);
                    }
                    p.getInventory().setHeldItemSlot(0);
                    Ui.title(p, Texts.text(lang, "mini.musterweber.weave"), Texts.text(lang, "mini.musterweber.weave.sub"), 1500);
                }
            }
            case BUILD -> judge();
            case PAUSE -> {
                if (round >= Webmuster.ROUNDS) {
                    finish();
                } else {
                    nextRound();
                }
            }
        }
    }

    private void judge() {
        phase = Phase.PAUSE;
        phaseEnd = ticks + PAUSE;
        for (Player p : alivePlayers()) {
            Integer k = loom.get(p.getUniqueId());
            if (k == null) {
                continue;
            }
            int[] attempt = new int[Webmuster.CELLS];
            for (int i = 0; i < Webmuster.CELLS; i++) {
                attempt[i] = Webmuster.PALETTE.indexOf(colourOf(cellBlock(k, i).getType()));
            }
            int right = Webmuster.correct(target, attempt);
            int pts = Webmuster.points(target, attempt);
            score.add(p.getUniqueId(), pts);
            p.getInventory().clear();
            Ui.sound(p, right == Webmuster.CELLS ? "minecraft:ui.toast.challenge_complete" : "minecraft:entity.experience_orb.pickup", 1f);
            bar(p, right == Webmuster.CELLS ? "mini.musterweber.perfect" : "mini.musterweber.result",
                    Placeholder.unparsed("n", String.valueOf(right)), Placeholder.unparsed("of", String.valueOf(Webmuster.CELLS)),
                    Placeholder.unparsed("points", String.valueOf(score.points(p.getUniqueId()))));
        }
    }

    private static String colourOf(Material m) {
        String n = m.name().toLowerCase(java.util.Locale.ROOT);
        return n.endsWith("_wool") ? n.substring(0, n.length() - "_wool".length()) : "";
    }

    @Override
    public void onBlockDamage(Player p, BlockDamageEvent e) {
        e.setCancelled(true);
        paint(p, e.getBlock());
    }

    @Override
    public void onInteract(Player p, PlayerInteractEvent e) {
        e.setCancelled(true);
        if ((e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_BLOCK)
                && e.getClickedBlock() != null) {
            paint(p, e.getClickedBlock());
        }
    }

    private void paint(Player p, Block b) {
        Integer k = loom.get(p.getUniqueId());
        if (phase != Phase.BUILD || k == null || !score.alive(p.getUniqueId())) {
            return;
        }
        ItemStack held = p.getInventory().getItemInMainHand();
        String colour = colourOf(held.getType());
        int idx = Webmuster.PALETTE.indexOf(colour);
        if (idx < 0 || idx >= Webmuster.colours(round)) {
            return;
        }
        for (int i = 0; i < Webmuster.CELLS; i++) {
            if (cellBlock(k, i).equals(b)) {
                if (b.getType() != held.getType()) {
                    b.setType(held.getType(), false);
                    Ui.sound(p, "minecraft:block.wool.place", 1.1f);
                }
                return;
            }
        }
    }

    @Override
    protected void onFinish() {
        paintAll(null);
    }
}
