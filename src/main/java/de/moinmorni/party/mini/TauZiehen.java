package de.moinmorni.party.mini;

import de.moinmorni.party.Items;
import de.moinmorni.party.Texts;
import de.moinmorni.party.Ui;
import de.moinmorni.party.game.Teams;
import de.moinmorni.party.world.Plan;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Tauziehen (nur mit Seiten: 2 gegen 2 oder Duell): Zwei Bretterstege über einer Schlammgrube, dazwischen ein Seil mit
 * rotem Knoten. Jeder Klick (Schlag oder Rechtsklick mit dem Seil) zieht – wer den Knoten ganz zu sich holt, gewinnt;
 * sonst am Ende, auf wessen Seite er steht. Alle stehen fest an ihrem Platz. Regeln in {@link Seil}.
 */
final class TauZiehen extends MiniRun {

    static final int HALF_PIT = 2;
    static final int END = 10;
    static final int WIDTH = 3;
    private static final double ROPE_Y = 1.35;
    /** So weit wandert der Knoten höchstens (bis über den Rand der Grube). */
    private static final double SWING = HALF_PIT + 2.5;

    private final Seil rope;
    private final List<BlockDisplay> parts = new ArrayList<>();
    private BlockDisplay knot;
    private int shownWin;

    TauZiehen(Setup s) {
        super(s);
        rope = new Seil(s.teams().a().size(), s.teams().b().size());
    }

    static void build(Plan p) {
        for (int x = -END; x <= END; x++) {
            for (int z = -WIDTH; z <= WIDTH; z++) {
                boolean pit = Math.abs(x) <= HALF_PIT;
                boolean edge = Math.abs(z) == WIDTH || Math.abs(x) == END;
                p.set(x, -2, z, "mud");
                p.set(x, -1, z, pit ? "mud" : "spruce_planks");
                p.set(x, 0, z, pit ? "air" : edge ? "stripped_spruce_log[axis=x]" : "spruce_planks");
                if (!pit && edge) {
                    p.set(x, 1, z, "spruce_fence");
                }
            }
        }
        // Pfähle am Rand der Grube
        for (int sx : new int[] {-HALF_PIT - 1, HALF_PIT + 1}) {
            for (int sz : new int[] {-WIDTH, WIDTH}) {
                p.set(sx, 1, sz, "spruce_log[axis=y]");
                p.set(sx, 2, sz, "lantern");
            }
        }
        // Mitte markieren
        p.set(0, -1, -WIDTH - 1, "red_wool");
        p.set(0, -1, WIDTH + 1, "red_wool");
    }

    @Override
    public void prepare() {
        place(plugin.arenaPlan(type));
        for (Player p : players) {
            int side = Math.max(0, split.side(p.getUniqueId()));
            List<java.util.UUID> mine = split.side(side);
            int i = Math.max(0, mine.indexOf(p.getUniqueId()));
            double x = (side == 0 ? -1 : 1) * (HALF_PIT + 3 + i * 2.2);
            reset(p);
            p.setGameMode(gameMode());
            p.teleport(facing(x + 0.5, 0, 0.5 + (i % 2 == 0 ? 0.6 : -0.6), 0.5, 1, 0.5));
            equip(p);
            teamCap(p);
        }
        spawnRope();
    }

    private void spawnRope() {
        for (int k = -END + 2; k <= END - 2; k++) {
            int x = k;
            parts.add(track(world.spawn(ropeAt(x), BlockDisplay.class, d -> {
                d.setBlock(Material.IRON_CHAIN.createBlockData("[axis=x]"));
                d.setTeleportDuration(2);
                d.setPersistent(false);
                d.setTransformation(new Transformation(new Vector3f(-0.5f, -0.5f, -0.5f), new AxisAngle4f(),
                        new Vector3f(1f, 1f, 1f), new AxisAngle4f()));
            })));
        }
        knot = track(world.spawn(ropeAt(0), BlockDisplay.class, d -> {
            d.setBlock(Material.RED_WOOL.createBlockData());
            d.setTeleportDuration(2);
            d.setPersistent(false);
            d.setTransformation(new Transformation(new Vector3f(-0.2f, -0.2f, -0.2f), new AxisAngle4f(),
                    new Vector3f(0.4f, 0.4f, 0.4f), new AxisAngle4f()));
        }));
    }

    private Location ropeAt(double x) {
        return at(x + 0.5 + rope.share() * SWING, ROPE_Y, 0.5);
    }

    @Override
    public boolean frozen() {
        return true;
    }

    @Override
    protected void equip(Player p) {
        p.getInventory().setItem(0, Items.named(Material.LEAD, 1, Texts.text(plugin.lang(p), "mini.tauziehen.item"),
                null));
    }

    @Override
    public void onInteract(Player p, PlayerInteractEvent e) {
        e.setCancelled(true);
        if (e.getAction() == Action.PHYSICAL) {
            return;
        }
        pull(p);
    }

    @Override
    public void onEntityClick(Player p, org.bukkit.entity.Entity target) {
        pull(p);
    }

    private void pull(Player p) {
        int side = split.side(p.getUniqueId());
        if (!rope.pull(p.getUniqueId(), side, ticks)) {
            return;
        }
        score.add(p.getUniqueId(), 1);
        if (score.points(p.getUniqueId()) % 4 == 0) {
            Ui.sound(p, "minecraft:entity.leash_knot.place", 0.8f + (float) rng.nextDouble() * 0.4f);
        }
    }

    @Override
    protected void onTick() {
        for (int i = 0; i < parts.size(); i++) {
            parts.get(i).teleport(ropeAt(-END + 2 + i));
        }
        knot.teleport(ropeAt(0));
        if (ticks % 4 == 0) {
            String barText = bar();
            for (Player p : alivePlayers()) {
                int side = split.side(p.getUniqueId());
                bar(p, "mini.tauziehen.bar", Placeholder.parsed("rope", barText),
                        Placeholder.component("side", Texts.text(plugin.lang(p), side == 0 ? "mini.tauziehen.left"
                                : "mini.tauziehen.right")));
            }
        }
        if (rope.won() != 0 && shownWin == 0) {
            shownWin = rope.won();
            world.spawnParticle(Particle.SPLASH, at(0.5, 0.5, 0.5), 60, 1.5, 0.3, 1.5, 0.2);
            soundAll("minecraft:entity.generic.splash", 0.8f);
        }
    }

    /** Seil als Zeichen in der Aktionsleiste: links blau/rot, Knoten ◆. */
    private String bar() {
        int cells = 21;
        int k = (int) Math.round((rope.share() + 1) / 2 * (cells - 1));
        StringBuilder b = new StringBuilder("<blue>◀</blue> <gray>");
        for (int i = 0; i < cells; i++) {
            b.append(i == k ? "</gray><red>◆</red><gray>" : i == cells / 2 ? "|" : "─");
        }
        return b.append("</gray> <red>▶</red>").toString();
    }

    @Override
    protected boolean complete() {
        return rope.won() != 0;
    }

    @Override
    public Teams.Outcome teamOutcome() {
        int w = rope.won() != 0 ? rope.won() : rope.leader();
        return w < 0 ? Teams.Outcome.A : w > 0 ? Teams.Outcome.B : Teams.Outcome.DRAW;
    }

    @Override
    public Location viewPoint() {
        return facing(0.5, 7, -12, 0.5, 0, 0.5);
    }
}
