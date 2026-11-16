package net.morni.party.mini;

import java.util.List;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.morni.party.Items;
import net.morni.party.Texts;
import net.morni.party.Ui;
import net.morni.party.world.Plan;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Trachtenfarben: Der Boden ist ein buntes Muster. Eine Farbe wird kurz gezeigt – merk sie dir! Danach verschwindet
 * jede andere Farbe; wer nicht auf der richtigen steht, fällt. Mit jeder Runde geht es schneller.
 */
final class TrachtenFarben extends MiniRun {

    static final int TILE = 4;
    static final int TILES = 5;
    static final int HALF = TILE * TILES / 2;
    static final List<String> COLOURS = List.of("red", "yellow", "lime", "light_blue", "blue", "orange", "pink",
            "white");

    private enum Phase { SHOW, HIDE, DROP, BACK }

    private final String[][] grid = new String[TILES][TILES];
    private Phase phase = Phase.BACK;
    private int phaseEnd;
    private int round;
    private String colour;

    TrachtenFarben(Setup s) {
        super(s);
    }

    static void build(Plan p) {
        for (int tx = 0; tx < TILES; tx++) {
            for (int tz = 0; tz < TILES; tz++) {
                String c = COLOURS.get(Math.floorMod(tx * 3 + tz * 5, COLOURS.size()));
                p.fill(-HALF + tx * TILE, 0, -HALF + tz * TILE, -HALF + tx * TILE + TILE - 1, 0,
                        -HALF + tz * TILE + TILE - 1, c + "_wool");
            }
        }
        for (int i = -HALF - 1; i <= HALF; i++) {
            for (int[] c : new int[][] {{i, -HALF - 1}, {i, HALF}, {-HALF - 1, i}, {HALF, i}}) {
                p.set(c[0], 0, c[1], "white_concrete");
                for (int h = 1; h <= 3; h++) {
                    p.set(c[0], h, c[1], "barrier");
                }
            }
        }
        // Pfosten mit Wimpeln
        for (int sx : new int[] {-HALF - 1, HALF}) {
            for (int sz : new int[] {-HALF - 1, HALF}) {
                p.fill(sx, -6, sz, sx, 4, sz, "stripped_birch_log[axis=y]");
                p.set(sx, 5, sz, "light_blue_wool");
            }
        }
    }

    @Override
    protected void equip(Player p) {
        // Farbe kommt in die Hand, sobald sie gezeigt wird.
    }

    @Override
    protected int fallLimit() {
        return 5;
    }

    @Override
    protected void onStart() {
        shuffle();
        nextRound();
    }

    private void shuffle() {
        for (int tx = 0; tx < TILES; tx++) {
            for (int tz = 0; tz < TILES; tz++) {
                grid[tx][tz] = COLOURS.get(rng.nextInt(COLOURS.size()));
                paint(tx, tz, Material.matchMaterial(grid[tx][tz] + "_wool"));
            }
        }
    }

    private void paint(int tx, int tz, Material m) {
        for (int x = 0; x < TILE; x++) {
            for (int z = 0; z < TILE; z++) {
                block(-HALF + tx * TILE + x, 0, -HALF + tz * TILE + z).setType(m, false);
            }
        }
    }

    private void nextRound() {
        round++;
        // Eine Farbe, die es auf dem Boden gibt
        colour = grid[rng.nextInt(TILES)][rng.nextInt(TILES)];
        phase = Phase.SHOW;
        phaseEnd = ticks + Math.max(SECOND, 2 * SECOND - round * 2);
        Material wool = Material.matchMaterial(colour + "_wool");
        for (Player p : alivePlayers()) {
            String lang = plugin.lang(p);
            var name = Texts.text(lang, "colour." + colour);
            for (int i = 0; i < 9; i++) {
                p.getInventory().setItem(i, Items.named(wool, 1, name, null));
            }
            Ui.title(p, name, Texts.text(lang, "mini.trachtenfarben.remember"), 900);
            Ui.sound(p, "minecraft:block.note_block.pling", 1.0f + round * 0.05f);
        }
    }

    @Override
    protected void onTick() {
        if (ticks < phaseEnd) {
            return;
        }
        switch (phase) {
            case SHOW -> {
                // Farbe weg aus der Hand – jetzt zählt das Gedächtnis
                alivePlayers().forEach(p -> {
                    p.getInventory().clear();
                    bar(p, "mini.trachtenfarben.hidden");
                });
                phase = Phase.HIDE;
                phaseEnd = ticks + Math.max(SECOND, 3 * SECOND - round * 3);
            }
            case HIDE -> {
                for (int tx = 0; tx < TILES; tx++) {
                    for (int tz = 0; tz < TILES; tz++) {
                        if (!grid[tx][tz].equals(colour)) {
                            paint(tx, tz, Material.AIR);
                        }
                    }
                }
                soundAll("minecraft:block.wool.break", 0.8f);
                for (Player p : audience()) {
                    String lang = plugin.lang(p);
                    p.sendMessage(Texts.msg(lang, "mini.trachtenfarben.was",
                            Placeholder.component("colour", Texts.text(lang, "colour." + colour))));
                }
                phase = Phase.DROP;
                phaseEnd = ticks + 2 * SECOND;
            }
            case DROP -> {
                shuffle();
                phase = Phase.BACK;
                phaseEnd = ticks + SECOND;
            }
            case BACK -> nextRound();
        }
    }

    /** Für Tests über die Verwaltung: die aktuelle Farbe. */
    String colour() {
        return colour;
    }
}
