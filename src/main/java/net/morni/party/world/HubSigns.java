package net.morni.party.world;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.morni.party.PartyPlugin;
import net.morni.party.Texts;
import net.morni.party.board.FieldType;
import net.morni.party.data.Store;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Schwebende Schrift auf dem Festplatz – jede Tafel gibt es auf Deutsch und Englisch, jeder sieht nur die in seiner
 * Sprache: Begrüßung am Maibaum, Namen der Beispiel-Felder, die Besten am Siegertreppchen.
 */
public final class HubSigns {

    private record Sign(TextDisplay de, TextDisplay en) {
    }

    private final PartyPlugin plugin;
    private final List<Sign> signs = new ArrayList<>();
    private Sign top;

    public HubSigns(PartyPlugin plugin) {
        this.plugin = plugin;
    }

    public void spawn() {
        remove();
        signs.add(sign(at(0.5, 6.2, 3.2), "sign.welcome", 1.0f));
        FieldType[] types = FieldType.values();
        int n = 16;
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            int cx = (int) Math.round(Math.cos(a) * HubPlan.RING);
            int cz = (int) Math.round(Math.sin(a) * HubPlan.RING);
            if (cz > 8 && Math.abs(cx) < 4) {
                continue;
            }
            FieldType t = types[i % types.length];
            signs.add(sign(at(cx + 0.5, 1.6, cz + 0.5), t.key() + ".sign", 0.8f));
        }
        top = sign(at(HubPlan.PODIUM[0] + 0.5, 5.5, HubPlan.PODIUM[2] - 1.2), "sign.top", 1.0f);
        signs.add(top);
        update();
    }

    private Location at(double x, double y, double z) {
        return new Location(plugin.world(), x, Places.FLOOR_Y + y, z);
    }

    private Sign sign(Location l, String key, float scale) {
        return new Sign(display(l, Texts.text("de", key), scale), display(l, Texts.text("en", key), scale));
    }

    private TextDisplay display(Location l, Component text, float scale) {
        return plugin.world().spawn(l, TextDisplay.class, d -> {
            d.text(text);
            d.setBillboard(Display.Billboard.CENTER);
            d.setPersistent(false);
            d.setVisibleByDefault(false);
            d.setBackgroundColor(Color.fromARGB(0x60000000));
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(scale, scale, scale),
                    new AxisAngle4f()));
        });
    }

    /** Sprache der Leute nachziehen, Bestenliste neu. */
    public void update() {
        if (top != null) {
            top.de().text(topText("de"));
            top.en().text(topText("en"));
        }
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            boolean en = "en".equals(plugin.lang(p));
            for (Sign s : signs) {
                if (en) {
                    p.showEntity(plugin, s.en());
                    p.hideEntity(plugin, s.de());
                } else {
                    p.showEntity(plugin, s.de());
                    p.hideEntity(plugin, s.en());
                }
            }
        }
    }

    private Component topText(String lang) {
        Component c = Texts.text(lang, "sign.top");
        int i = 1;
        List<Store.Entry> best = plugin.store().top(5);
        for (Store.Entry e : best) {
            c = c.append(Component.newline()).append(Texts.text(lang, "top.line", Placeholder.unparsed("pos",
                    String.valueOf(i++)), Placeholder.unparsed("name", e.name()),
                    Texts.count("wins", e.wins()),
                    Placeholder.unparsed("edelweiss", String.valueOf(e.edelweiss()))));
        }
        if (best.isEmpty()) {
            c = c.append(Component.newline()).append(Texts.text(lang, "top.empty"));
        }
        return c;
    }

    public void remove() {
        for (Sign s : signs) {
            s.de().remove();
            s.en().remove();
        }
        signs.clear();
        top = null;
    }
}
