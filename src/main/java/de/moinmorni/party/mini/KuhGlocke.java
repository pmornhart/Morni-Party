package de.moinmorni.party.mini;

import de.moinmorni.party.Items;
import de.moinmorni.party.Texts;
import de.moinmorni.party.Ui;
import de.moinmorni.party.world.Plan;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Kuhglocke: Alle stehen vor der großen Glocke. Sobald die Tafel grün wird und es „Jetzt!“ heißt, so schnell wie
 * möglich klicken. Der Schnellste bekommt 3 Punkte, der Zweite 2, der Dritte 1 – wer zu früh klickt, verliert einen
 * Punkt und setzt die Runde aus. Fünf Runden.
 */
final class KuhGlocke extends MiniRun {

    static final int ROUNDS = 5;
    private static final int[] REWARD = {3, 2, 1};
    private static final int WINDOW = 3 * SECOND;

    private enum Phase { WAIT, GO, PAUSE }

    private Phase phase = Phase.PAUSE;
    private int phaseEnd;
    private int round;
    private final List<UUID> clicked = new ArrayList<>();
    private final Set<UUID> early = new HashSet<>();

    KuhGlocke(Setup s) {
        super(s);
    }

    static void build(Plan p) {
        for (int x = -9; x <= 9; x++) {
            for (int z = -5; z <= 6; z++) {
                p.set(x, 0, z, z >= 4 ? "hay_block[axis=x]" : "stripped_oak_wood[axis=x]");
                p.set(x, -1, z, "oak_planks");
            }
        }
        // Tafel: 11 × 5 Wolle (rot), daneben die große Glocke
        signal(p, "red_wool");
        p.fill(-7, 1, 6, -7, 7, 6, "spruce_log[axis=y]");
        p.fill(7, 1, 6, 7, 7, 6, "spruce_log[axis=y]");
        p.fill(-7, 8, 6, 7, 8, 6, "spruce_slab[type=bottom]");
        p.set(0, 7, 4, "bell[attachment=ceiling,facing=north]");
        p.set(0, 8, 4, "spruce_planks");
        p.set(0, 8, 5, "spruce_planks");
        for (int x = -9; x <= 9; x++) {
            for (int h = 1; h <= 3; h++) {
                p.set(x, h, -6, "barrier");
            }
        }
        for (int z = -6; z <= 6; z++) {
            for (int h = 1; h <= 3; h++) {
                p.set(-10, h, z, "barrier");
                p.set(10, h, z, "barrier");
            }
        }
    }

    static void signal(Plan p, String wool) {
        p.fill(-5, 2, 7, 5, 6, 7, wool);
    }

    private void paint(Material m) {
        for (int x = -5; x <= 5; x++) {
            for (int y = 2; y <= 6; y++) {
                block(x, y, 7).setType(m, false);
            }
        }
    }

    @Override
    protected List<Location> spawns(int n) {
        List<Location> out = new ArrayList<>();
        for (int i = 0; i < Math.max(1, n); i++) {
            double x = -7 + 0.5 + (14.0 * i / Math.max(1, Math.max(n - 1, 1)));
            if (n == 1) {
                x = 0.5;
            }
            out.add(facing(x, 1, -2.5, 0.5, 4, 7));
        }
        return out;
    }

    @Override
    public boolean frozen() {
        return true;
    }

    @Override
    public Location viewPoint() {
        return facing(0, 6, -12, 0, 3, 6);
    }

    @Override
    protected void equip(Player p) {
        p.getInventory().setItem(0, Items.named(Material.LEAD, 1, Texts.text(plugin.lang(p), "mini.kuhglocke.item"),
                Texts.lines(plugin.lang(p), "mini.kuhglocke.item.lore")));
    }

    @Override
    protected void onStart() {
        startRound();
    }

    private void startRound() {
        round++;
        clicked.clear();
        early.clear();
        paint(Material.RED_WOOL);
        phase = Phase.WAIT;
        phaseEnd = ticks + 2 * SECOND + rng.nextInt(3 * SECOND);
        for (Player p : alivePlayers()) {
            Ui.title(p, Texts.text(plugin.lang(p), "mini.kuhglocke.wait"),
                    Texts.text(plugin.lang(p), "mini.kuhglocke.round", Placeholder.unparsed("n", String.valueOf(round)),
                            Placeholder.unparsed("of", String.valueOf(ROUNDS))), 1500);
        }
    }

    @Override
    protected void onTick() {
        if (ticks < phaseEnd) {
            return;
        }
        switch (phase) {
            case WAIT -> {
                paint(Material.LIME_WOOL);
                phase = Phase.GO;
                phaseEnd = ticks + WINDOW;
                for (Player p : audience()) {
                    Ui.title(p, Texts.text(plugin.lang(p), "mini.kuhglocke.now"), net.kyori.adventure.text.Component.empty(), 700);
                    Ui.sound(p, "minecraft:block.bell.use", 1.0f);
                }
            }
            case GO -> endRound();
            case PAUSE -> {
                if (round >= ROUNDS) {
                    finish();
                } else {
                    startRound();
                }
            }
        }
    }

    private void endRound() {
        phase = Phase.PAUSE;
        phaseEnd = ticks + SECOND + SECOND / 2;
        paint(Material.YELLOW_WOOL);
        if (clicked.isEmpty()) {
            say("mini.kuhglocke.nobody");
        }
    }

    @Override
    public void onInteract(Player p, PlayerInteractEvent e) {
        e.setCancelled(true);
        if (e.getAction() == Action.PHYSICAL) {
            return;
        }
        UUID u = p.getUniqueId();
        if (!score.alive(u) || clicked.contains(u) || early.contains(u)) {
            return;
        }
        if (phase == Phase.WAIT) {
            early.add(u);
            score.add(u, -1);
            Ui.sound(p, "minecraft:entity.villager.no", 1f);
            bar(p, "mini.kuhglocke.early");
            return;
        }
        if (phase != Phase.GO) {
            return;
        }
        clicked.add(u);
        int place = clicked.size() - 1;
        if (place < REWARD.length) {
            point(p, REWARD[place]);
            say("mini.kuhglocke.fast", Placeholder.unparsed("name", p.getName()),
                    Placeholder.unparsed("pos", String.valueOf(place + 1)));
        }
        long waiting = alivePlayers().stream().filter(x -> !clicked.contains(x.getUniqueId())
                && !early.contains(x.getUniqueId())).count();
        if (waiting == 0) {
            endRound();
        }
    }
}
