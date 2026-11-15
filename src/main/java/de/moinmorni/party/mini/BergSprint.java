package de.moinmorni.party.mini;

import de.moinmorni.party.Texts;
import de.moinmorni.party.Ui;
import de.moinmorni.party.world.Decor;
import de.moinmorni.party.world.Plan;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Bergsprint: ein Wettlauf den Almweg hinauf, über Heuballen und kleine Stufen. Wer zuerst durchs Ziel läuft, gewinnt;
 * wer es nicht schafft, wird nach der Strecke gewertet, die er geschafft hat.
 */
final class BergSprint extends MiniRun {

    static final int START_Z = -30;
    static final int GOAL_Z = 30;
    static final int HALF_WIDTH = 4;
    private static final int FINISH_POINTS = 1000;
    private final Set<UUID> finished = new HashSet<>();
    private int order;

    BergSprint(Setup s) {
        super(s);
    }

    /** Höhe des Wegs an dieser Stelle (alle 6 Blöcke eine Stufe). */
    static int height(int z) {
        return Math.max(0, Math.min(10, (z - START_Z) / 6));
    }

    static void build(Plan p) {
        Random r = new Random(7);
        for (int z = START_Z - 3; z <= GOAL_Z + 4; z++) {
            int h = height(Math.max(START_Z, Math.min(GOAL_Z, z)));
            for (int x = -HALF_WIDTH; x <= HALF_WIDTH; x++) {
                for (int y = h - 3; y < h; y++) {
                    p.set(x, y, z, "dirt");
                }
                p.set(x, h, z, Math.abs(x) <= 1 ? "dirt_path" : "grass_block[snowy=false]");
            }
            for (int side : new int[] {-HALF_WIDTH - 1, HALF_WIDTH + 1}) {
                for (int y = h - 3; y <= h; y++) {
                    p.set(side, y, z, Decor.rock(r, 0));
                }
                p.set(side, h + 1, z, "spruce_fence");
                for (int y = h + 2; y <= h + 4; y++) {
                    p.set(side, y, z, "barrier");
                }
            }
        }
        // Rückwand am Start und Ziel-Ende
        for (int x = -HALF_WIDTH; x <= HALF_WIDTH; x++) {
            for (int y = 1; y <= 4; y++) {
                p.set(x, y, START_Z - 4, "barrier");
                p.set(x, height(GOAL_Z) + y, GOAL_Z + 5, "barrier");
            }
        }
        // Hindernisse: Heuballen (ein Block hoch) mit Lücke, damit niemand festhängt
        for (int z = START_Z + 8; z < GOAL_Z - 4; z += 9) {
            int gap = Math.floorMod(z * 7, 2 * HALF_WIDTH - 1) - HALF_WIDTH + 1;
            for (int x = -HALF_WIDTH; x <= HALF_WIDTH; x++) {
                if (Math.abs(x - gap) > 1) {
                    p.set(x, height(z) + 1, z, "hay_block[axis=x]");
                }
            }
        }
        // Start und Ziel
        for (int x = -HALF_WIDTH; x <= HALF_WIDTH; x++) {
            p.set(x, height(START_Z), START_Z, "white_concrete");
            p.set(x, height(GOAL_Z), GOAL_Z, (x + HALF_WIDTH) % 2 == 0 ? "gold_block" : "white_concrete");
        }
        int gh = height(GOAL_Z);
        for (int side : new int[] {-HALF_WIDTH - 1, HALF_WIDTH + 1}) {
            for (int y = gh + 1; y <= gh + 5; y++) {
                p.set(side, y, GOAL_Z, "stripped_spruce_log[axis=y]");
            }
        }
        for (int x = -HALF_WIDTH - 1; x <= HALF_WIDTH + 1; x++) {
            p.set(x, gh + 6, GOAL_Z, Math.floorMod(x, 2) == 0 ? "white_wool" : "light_blue_wool");
        }
        // Bäume am Rand
        for (int z = START_Z; z <= GOAL_Z; z += 7) {
            int h = height(z);
            for (int side : new int[] {-HALF_WIDTH - 4, HALF_WIDTH + 4}) {
                p.fill(side - 1, h - 3, z - 1, side + 1, h, z + 1, "grass_block[snowy=false]");
                Decor.spruce(p, side, h + 1, z, 5 + r.nextInt(3), r);
            }
        }
    }

    @Override
    protected List<Location> spawns(int n) {
        List<Location> out = new ArrayList<>();
        for (int i = 0; i < Math.max(1, n); i++) {
            double x = -HALF_WIDTH + 0.5 + (i * 2.0) % (2 * HALF_WIDTH);
            out.add(facing(x, height(START_Z) + 1, START_Z - 2 + 0.5 - (i / HALF_WIDTH), x, 1, GOAL_Z));
        }
        return out;
    }

    @Override
    public Location viewPoint() {
        return facing(0, 16, 0, 0, 4, 10);
    }

    @Override
    protected void equip(Player p) {
        // Laufen genügt
    }

    /** Jeden zweiten Tick: wer ist wie weit? (Auch nach einem Sprung oder Teleport, nicht nur beim Laufen.) */
    @Override
    protected void onTick() {
        if (ticks % 2 != 0) {
            return;
        }
        for (Player p : alivePlayers()) {
            progress(p);
        }
    }

    private void progress(Player p) {
        UUID u = p.getUniqueId();
        if (finished.contains(u) || !score.has(u)) {
            return;
        }
        double z = p.getLocation().getZ() - oz;
        if (z >= GOAL_Z) {
            finished.add(u);
            order++;
            score.set(u, FINISH_POINTS - order);
            Ui.sound(p, "minecraft:entity.player.levelup", 1f);
            say("mini.bergsprint.finish", Placeholder.unparsed("name", p.getName()),
                    Placeholder.unparsed("pos", String.valueOf(order)));
            Ui.title(p, Texts.text(plugin.lang(p), "mini.bergsprint.done"), net.kyori.adventure.text.Component.empty(), 1200);
        } else {
            score.set(u, Math.max(score.points(u), (int) Math.floor(z - START_Z)));
        }
    }

    @Override
    protected boolean complete() {
        return !players.isEmpty() && players.stream().allMatch(p -> finished.contains(p.getUniqueId()));
    }
}
