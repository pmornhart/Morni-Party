package de.moinmorni.party.mini;

import de.moinmorni.party.Items;
import de.moinmorni.party.Texts;
import de.moinmorni.party.Ui;
import de.moinmorni.party.menu.Menu;
import de.moinmorni.party.world.Decor;
import de.moinmorni.party.world.Plan;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.InventoryHolder;

/**
 * Schäfchen zählen: Im Pferch laufen Schafe herum. Nach ein paar Sekunden sind sie weg – wie viele waren es? Antwort
 * im Menü: genau richtig 3 Punkte, um eins daneben 1 Punkt. Drei Runden.
 */
final class Schaefchen extends MiniRun {

    static final int PEN = 6;
    static final int ROUNDS = 3;
    private static final int WATCH = 6 * SECOND;
    private static final int ANSWER = 8 * SECOND;
    private static final int RESULT = 2 * SECOND;
    private static final DyeColor[] WOOL = {DyeColor.WHITE, DyeColor.WHITE, DyeColor.WHITE, DyeColor.BROWN,
        DyeColor.BLACK, DyeColor.LIGHT_GRAY};

    private enum Phase { WATCH, ANSWER, RESULT }

    private Phase phase;
    private int phaseEnd;
    private int round;
    private int count;
    private final List<Sheep> sheep = new ArrayList<>();
    private final Map<UUID, Integer> answers = new HashMap<>();
    private final Map<UUID, Menu> menus = new HashMap<>();

    Schaefchen(Setup s) {
        super(s);
    }

    static void build(Plan p) {
        Random r = new Random(5);
        for (int x = -PEN - 3; x <= PEN + 3; x++) {
            for (int z = -PEN - 6; z <= PEN + 3; z++) {
                p.set(x, -1, z, "dirt");
                p.set(x, 0, z, r.nextInt(6) == 0 ? "moss_block" : "grass_block[snowy=false]");
            }
        }
        for (int i = -PEN - 1; i <= PEN + 1; i++) {
            for (int[] c : new int[][] {{i, -PEN - 1}, {i, PEN + 1}, {-PEN - 1, i}, {PEN + 1, i}}) {
                p.set(c[0], 1, c[1], "oak_fence");
                p.set(c[0], 2, c[1], "barrier");
                p.set(c[0], 3, c[1], "barrier");
            }
        }
        // Zuschauer-Steg (Balkon) vor dem Pferch
        for (int x = -PEN - 2; x <= PEN + 2; x++) {
            for (int z = -PEN - 5; z <= -PEN - 3; z++) {
                p.set(x, 3, z, "spruce_planks");
            }
            p.set(x, 4, -PEN - 2, "spruce_fence");
            p.set(x, 4, -PEN - 6, "barrier");
            p.set(x, 5, -PEN - 6, "barrier");
            p.set(x, 5, -PEN - 2, "barrier");
        }
        for (int x : new int[] {-PEN - 2, PEN + 2}) {
            p.fill(x, 1, -PEN - 5, x, 2, -PEN - 3, "spruce_log[axis=y]");
            for (int z = -PEN - 6; z <= -PEN - 2; z++) {
                p.set(x + Integer.signum(x), 4, z, "barrier");
                p.set(x + Integer.signum(x), 5, z, "barrier");
            }
        }
        // Stall im Hintergrund
        p.fill(-3, 1, PEN + 2, 3, 3, PEN + 3, "spruce_planks");
        p.fill(-3, 4, PEN + 2, 3, 4, PEN + 3, "dark_oak_slab[type=bottom]");
        Decor.island(p, PEN + 4, 6, r, "grass_block[snowy=false]", 1);
    }

    @Override
    protected List<Location> spawns(int n) {
        List<Location> out = new ArrayList<>();
        for (int i = 0; i < Math.max(1, n); i++) {
            double x = -PEN + 0.5 + (2.0 * PEN * i / Math.max(1, n - 1));
            if (n == 1) {
                x = 0.5;
            }
            out.add(facing(x, 4, -PEN - 3.5, 0.5, 0, 0.5));
        }
        return out;
    }

    @Override
    public boolean frozen() {
        return true;
    }

    @Override
    public Location viewPoint() {
        return facing(0, 9, -PEN - 8, 0, 0, 0);
    }

    @Override
    protected void equip(Player p) {
        // zählen
    }

    @Override
    protected void onStart() {
        startRound();
    }

    private void startRound() {
        round++;
        count = 5 + rng.nextInt(12);
        answers.clear();
        for (int i = 0; i < count; i++) {
            double x = rng.nextDouble() * (2 * PEN - 1) - PEN + 1;
            double z = rng.nextDouble() * (2 * PEN - 1) - PEN + 1;
            DyeColor colour = WOOL[rng.nextInt(WOOL.length)];
            Sheep s = world.spawn(at(x, 1, z), Sheep.class, sh -> {
                sh.setColor(colour);
                sh.setInvulnerable(true);
                sh.setPersistent(false);
                sh.setRemoveWhenFarAway(false);
                sh.setSilent(rng.nextBoolean());
            });
            sheep.add(track(s));
        }
        phase = Phase.WATCH;
        phaseEnd = ticks + WATCH;
        for (Player p : alivePlayers()) {
            Ui.title(p, Texts.text(plugin.lang(p), "mini.schaefchen.count"),
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
            case WATCH -> ask();
            case ANSWER -> reveal();
            case RESULT -> {
                if (round >= ROUNDS) {
                    finish();
                } else {
                    startRound();
                }
            }
        }
    }

    private void ask() {
        sheep.forEach(this::untrack);
        sheep.clear();
        phase = Phase.ANSWER;
        phaseEnd = ticks + ANSWER;
        List<Integer> options = new ArrayList<>();
        options.add(count);
        while (options.size() < 4) {
            int o = Math.max(1, count + rng.nextInt(9) - 4);
            if (!options.contains(o)) {
                options.add(o);
            }
        }
        Collections.sort(options);
        for (Player p : alivePlayers()) {
            String lang = plugin.lang(p);
            Menu m = new Menu(1, Texts.text(lang, "mini.schaefchen.menu"));
            for (int i = 0; i < options.size(); i++) {
                int n = options.get(i);
                m.set(1 + i * 2, Items.named(Material.WHITE_WOOL, n,
                        Texts.text(lang, "mini.schaefchen.option", Placeholder.unparsed("n", String.valueOf(n))), null),
                        who -> answer(who, n));
            }
            menus.put(p.getUniqueId(), m);
            m.open(p);
            Ui.sound(p, "minecraft:entity.sheep.ambient", 1f);
        }
    }

    private void answer(Player p, int n) {
        UUID u = p.getUniqueId();
        if (phase != Phase.ANSWER || answers.containsKey(u)) {
            return;
        }
        answers.put(u, n);
        p.closeInventory();
        bar(p, "mini.schaefchen.answered", Placeholder.unparsed("n", String.valueOf(n)));
        if (alivePlayers().stream().allMatch(x -> answers.containsKey(x.getUniqueId()))) {
            reveal();
        }
    }

    private void reveal() {
        phase = Phase.RESULT;
        phaseEnd = ticks + RESULT;
        for (Player p : alivePlayers()) {
            InventoryHolder h = p.getOpenInventory().getTopInventory().getHolder();
            if (h instanceof Menu) {
                p.closeInventory();
            }
            Integer a = answers.get(p.getUniqueId());
            if (a != null && a == count) {
                point(p, 3);
            } else if (a != null && Math.abs(a - count) == 1) {
                point(p, 1);
            }
        }
        menus.clear();
        say("mini.schaefchen.result", Placeholder.unparsed("n", String.valueOf(count)));
    }

    @Override
    protected void onFinish() {
        sheep.clear();
        for (Player p : players) {
            if (p.isOnline() && p.getOpenInventory().getTopInventory().getHolder() instanceof Menu) {
                p.closeInventory();
            }
        }
    }

    /** Für Tests über die Verwaltung. */
    int count() {
        return count;
    }
}
