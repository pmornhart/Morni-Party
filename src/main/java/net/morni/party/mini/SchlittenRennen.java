package net.morni.party.mini;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.morni.party.Texts;
import net.morni.party.Ui;
import net.morni.party.world.Plan;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/**
 * Schlittenrennen: Zwei Runden im Schlitten (einem Holzboot) über die Eisbahn – auf dem Eis rutscht ein Boot schnell,
 * gelenkt wird wie immer mit den Lauftasten (Bedrock: Steuerkreuz). Sechs Tore zählen nur der Reihe nach, Abkürzen
 * bringt nichts. Wer aus dem Schlitten fällt (oder aussteigt), sitzt nach einem Augenblick in einem neuen.
 * Bewusst echte Boote: die fahren auf Java (jede Version über ViaVersion) und auf Bedrock (Geyser) gleich vertraut.
 */
final class SchlittenRennen extends MiniRun {

    static final int COLUMNS = 3;
    private static final int RESEAT = 20;
    private static final EntityType[] BOATS = {EntityType.SPRUCE_BOAT, EntityType.BIRCH_BOAT, EntityType.OAK_BOAT,
        EntityType.DARK_OAK_BOAT, EntityType.ACACIA_BOAT, EntityType.CHERRY_BOAT, EntityType.JUNGLE_BOAT,
        EntityType.MANGROVE_BOAT};

    private final Map<UUID, Rennbahn.Racer> racers = new HashMap<>();
    private final Map<UUID, Entity> sleds = new HashMap<>();
    private final Map<UUID, Integer> outSince = new HashMap<>();
    private final Map<UUID, Integer> boatType = new HashMap<>();

    SchlittenRennen(Setup s) {
        super(s);
    }

    static void build(Plan p) {
        int r = (int) Math.ceil(Rennbahn.STRAIGHT + Rennbahn.RADIUS + Rennbahn.HALF_WIDTH + 2);
        int rz = (int) Math.ceil(Rennbahn.RADIUS + Rennbahn.HALF_WIDTH + 2);
        for (int x = -r; x <= r; x++) {
            for (int z = -rz; z <= rz; z++) {
                double d = Rennbahn.offCenter(x + 0.5, z + 0.5);
                if (d <= Rennbahn.HALF_WIDTH) {
                    double s = Rennbahn.locate(x + 0.5, z + 0.5);
                    boolean straight = Math.abs(x + 0.5) < Rennbahn.STRAIGHT - 2;
                    boolean gate = false;
                    for (int g = 0; g < Rennbahn.GATES; g++) {
                        double delta = Math.abs(s - Rennbahn.gate(g));
                        gate |= Math.min(delta, Rennbahn.LENGTH - delta) < 0.6;
                    }
                    String ice = gate ? (g0(s) && Math.floorMod(z, 2) == 0 ? "ice" : "blue_ice")
                            : straight && Math.floorMod((int) s, 9) < 3 ? "blue_ice" : "packed_ice";
                    p.set(x, 0, z, ice);
                    p.set(x, -1, z, "packed_ice");
                } else if (d <= Rennbahn.HALF_WIDTH + 1.2) {
                    // Bande: ein Schneewall (Boote kommen nicht drüber), darüber unsichtbar
                    p.set(x, -1, z, "packed_ice");
                    p.set(x, 0, z, "snow_block");
                    p.set(x, 1, z, "snow_block");
                    p.set(x, 2, z, "barrier");
                    p.set(x, 3, z, "barrier");
                }
            }
        }
        // Torbögen über der Bahn (weit oben, damit nichts im Weg ist)
        for (int g = 0; g < Rennbahn.GATES; g++) {
            double[] c = Rennbahn.point(Rennbahn.gate(g));
            double a = Math.toRadians(c[2]);
            double nx = Math.cos(a);
            double nz = Math.sin(a);
            for (int side : new int[] {-1, 1}) {
                int px = (int) Math.floor(c[0] + side * nx * (Rennbahn.HALF_WIDTH + 1.6));
                int pz = (int) Math.floor(c[1] + side * nz * (Rennbahn.HALF_WIDTH + 1.6));
                for (int y = 0; y <= 6; y++) {
                    p.set(px, y, pz, "stripped_spruce_log[axis=y]");
                }
                p.set(px, 7, pz, g == 0 ? "gold_block" : "light_blue_wool");
            }
            for (double t = -Rennbahn.HALF_WIDTH - 1.6; t <= Rennbahn.HALF_WIDTH + 1.6; t += 0.5) {
                int x = (int) Math.floor(c[0] + t * nx);
                int z = (int) Math.floor(c[1] + t * nz);
                p.set(x, 7, z, g == 0 ? (Math.floorMod(x + z, 2) == 0 ? "white_wool" : "black_wool")
                        : Math.floorMod(x + z, 2) == 0 ? "white_wool" : "light_blue_wool");
            }
        }
    }

    private static boolean g0(double s) {
        return Math.min(s, Rennbahn.LENGTH - s) < 0.6;
    }

    @Override
    protected List<Location> spawns(int n) {
        List<Location> out = new ArrayList<>();
        for (int i = 0; i < Math.max(1, n); i++) {
            int row = i / COLUMNS;
            int col = i % COLUMNS;
            double x = -3 - row * 3;
            double z = -Rennbahn.RADIUS + (col - 1) * 2.2;
            Location l = at(x + 0.5, 1, z + 0.5);
            l.setYaw(-90);
            out.add(l);
        }
        return out;
    }

    @Override
    public Location viewPoint() {
        return facing(0.5, 22, -Rennbahn.RADIUS - 22, 0.5, 0, 0);
    }

    @Override
    protected void equip(Player p) {
        // Der Schlitten kommt beim Start.
    }

    @Override
    protected void onStart() {
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            racers.put(p.getUniqueId(), new Rennbahn.Racer());
            boatType.put(p.getUniqueId(), i % BOATS.length);
            seat(p);
            String lang = plugin.lang(p);
            Ui.title(p, Texts.text(lang, "mini.schlittenrennen.go"), Texts.text(lang, "mini.schlittenrennen.lap",
                    Placeholder.unparsed("n", "1"), Placeholder.unparsed("of", String.valueOf(Rennbahn.LAPS))), 1200);
        }
    }

    /** Neuer Schlitten an der Stelle, an der jemand gerade steht. */
    private void seat(Player p) {
        Entity old = sleds.remove(p.getUniqueId());
        if (old != null) {
            untrack(old);
        }
        Location l = p.getLocation();
        double s = Rennbahn.locate(l.getX() - ox - 0.5, l.getZ() - oz - 0.5);
        l.setYaw((float) Rennbahn.point(s)[2]);
        l.setPitch(0);
        if (Rennbahn.offCenter(l.getX() - ox - 0.5, l.getZ() - oz - 0.5) > Rennbahn.HALF_WIDTH - 0.6) {
            double[] c = Rennbahn.point(s);
            l.setX(ox + 0.5 + c[0]);
            l.setZ(oz + 0.5 + c[1]);
        }
        l.setY(oy + 1.05);
        Entity boat = track(world.spawnEntity(l, BOATS[boatType.getOrDefault(p.getUniqueId(), 0)]));
        boat.setPersistent(false);
        if (boat instanceof Boat b) {
            b.setInvulnerable(true);
        }
        boat.addPassenger(p);
        sleds.put(p.getUniqueId(), boat);
        outSince.remove(p.getUniqueId());
    }

    @Override
    protected void onTick() {
        for (Player p : alivePlayers()) {
            UUID u = p.getUniqueId();
            Location l = p.getLocation();
            double x = l.getX() - ox - 0.5;
            double z = l.getZ() - oz - 0.5;
            Rennbahn.Racer r = racers.get(u);
            if (r == null) {
                continue;
            }
            if (ticks % 2 == 0 && !r.finished()) {
                double s = Rennbahn.locate(x, z);
                int lapBefore = r.lap();
                if (r.update(s)) {
                    Ui.sound(p, "minecraft:block.note_block.chime", 1.2f + 0.1f * (r.passed() % Rennbahn.GATES));
                    if (r.finished()) {
                        arrive(p, "mini.race.finish");
                    } else if (r.lap() > lapBefore) {
                        Ui.title(p, Texts.text(plugin.lang(p), "mini.schlittenrennen.last"), net.kyori.adventure.text.Component.empty(), 1200);
                    }
                }
                progress(p, r.progress(s));
                if (!r.finished()) {
                    bar(p, "mini.schlittenrennen.lap.bar", Placeholder.unparsed("n", String.valueOf(r.lap())),
                            Placeholder.unparsed("of", String.valueOf(Rennbahn.LAPS)),
                            Placeholder.unparsed("gate", String.valueOf(r.passed() % Rennbahn.GATES)),
                            Placeholder.unparsed("gates", String.valueOf(Rennbahn.GATES)));
                }
            }
            // Aus dem Schlitten gefallen oder ausgestiegen: gleich wieder hinein
            if (!score.finished(u) && p.getVehicle() == null) {
                int since = outSince.computeIfAbsent(u, k -> ticks);
                if (ticks - since >= RESEAT && l.getY() > oy - 2) {
                    seat(p);
                }
            } else {
                outSince.remove(u);
            }
        }
    }

    @Override
    protected String wayKey() {
        return "mini.line.gates";
    }

    /** Ergebniszeile: Tore statt Punkte. */
    @Override
    public int shownPoints(UUID u) {
        Rennbahn.Racer r = racers.get(u);
        return r == null ? 0 : r.passed();
    }

    @Override
    protected void onFinish() {
        sleds.clear();
    }
}
