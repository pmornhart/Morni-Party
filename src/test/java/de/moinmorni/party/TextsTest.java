package de.moinmorni.party;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.party.board.BoardId;
import de.moinmorni.party.board.FieldType;
import de.moinmorni.party.game.ItemType;
import de.moinmorni.party.game.Luck;
import de.moinmorni.party.game.Mode;
import de.moinmorni.party.mini.MiniType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class TextsTest {

    /** Formatierungen, die keine Platzhalter sind. */
    private static final Set<String> STYLES = Set.of("gray", "white", "em", "au", "bl", "ed", "red", "green",
            "dark_gray", "b", "bold", "gold", "yellow", "light_purple", "aqua", "blue");
    // Tags wie <name> oder </gray> – aber nicht \<spieler> (maskiert).
    // https://regex101.com/?flavor=java&regex=(%3F%3C!%5C%5C)%3C%2F%3F(%5Ba-z_%5D%2B)%3E&testString=%3Cgray%3EHi%20%3Cname%3E%3C%2Fgray%3E%20%5C%3Cspieler%3E
    private static final Pattern TAG = Pattern.compile("(?<!\\\\)</?([a-z_]+)>");
    /** Schlüssel in den Quelltexten: "mini.out", "queue.joined" … */
    // https://regex101.com/?flavor=java&regex=%22(%5Ba-z%5D%5Ba-z0-9-%5D*(%3F%3A%5C.%5Ba-z0-9-%5D%2B)%2B)%22&testString=Texts.msg(lang%2C%20%22queue.joined%22)
    private static final Pattern KEY = Pattern.compile("\"([a-z][a-z0-9-]*(?:\\.[a-z0-9-]+)+)\"");
    /** Einzahl/Mehrzahl: {@code <points_pl:'0#Punkte|1#Punkt|1<Punkte'/>} (Zahl dazu in {@code <points>}). */
    // https://regex101.com/?flavor=java&regex=%3C(%5Ba-z_%5D%2B)_pl%3A'(%5B%5E'%5D*)'%2F%3E&testString=(%3Cpoints%3E%20%3Cpoints_pl%3A'0%23Punkte%7C1%23Punkt%7C1%3CPunkte'%2F%3E)
    private static final Pattern CHOICE = Pattern.compile("<([a-z_]+)_pl:'([^']*)'/>");

    private static Set<String> placeholders(String text) {
        Set<String> out = new TreeSet<>();
        Matcher m = TAG.matcher(text);
        while (m.find()) {
            if (!STYLES.contains(m.group(1))) {
                out.add(m.group(1));
            }
        }
        return out;
    }

    @Test
    void everyTextExistsInBothLanguagesWithTheSamePlaceholders() {
        for (String key : Texts.keys()) {
            String de = Texts.raw("de", key);
            String en = Texts.raw("en", key);
            assertFalse(de.isBlank(), key);
            assertFalse(en.isBlank(), key);
            assertEquals(placeholders(de), placeholders(en), "Platzhalter in " + key);
        }
    }

    @Test
    void everyTextParsesStrictly() {
        MiniMessage strict = MiniMessage.builder().strict(true)
                .editTags(t -> t.resolvers(
                        TagResolver.resolver("em", Tag.styling(TextColor.color(Texts.EMERALD))),
                        TagResolver.resolver("au", Tag.styling(TextColor.color(Texts.GOLD))),
                        TagResolver.resolver("bl", Tag.styling(TextColor.color(Texts.BLUE))),
                        TagResolver.resolver("ed", Tag.styling(TextColor.color(Texts.EDELWEISS)))))
                .build();
        for (String key : Texts.keys()) {
            for (String lang : new String[] {"de", "en"}) {
                String text = Texts.raw(lang, key);
                TagResolver.Builder resolvers = TagResolver.builder();
                placeholders(text).forEach(p -> resolvers.resolver(Placeholder.unparsed(p, "x")));
                Matcher c = CHOICE.matcher(text);
                while (c.find()) {
                    resolvers.resolver(Formatter.choice(c.group(1) + "_pl", 2));
                }
                TagResolver all = resolvers.build();
                for (String line : text.split("\n")) {
                    assertDoesNotThrow(() -> strict.deserialize(line, all), lang + " " + key + ": " + line);
                }
            }
        }
    }

    /** Jedes gezählte Wort steht neben seiner Zahl und hat Formen für 0, 1 und mehr. */
    @Test
    void countedWordsHaveTheirNumber() {
        for (String key : Texts.keys()) {
            for (String lang : new String[] {"de", "en"}) {
                String text = Texts.raw(lang, key);
                Matcher c = CHOICE.matcher(text);
                while (c.find()) {
                    assertTrue(placeholders(text).contains(c.group(1)), lang + " " + key + ": <" + c.group(1) + "> fehlt");
                    assertTrue(c.group(2).matches("0#[^|]+\\|1#[^|]+\\|1<[^|]+"), lang + " " + key + ": " + c.group(2));
                }
            }
        }
    }

    @Test
    void singularAndPluralReadNaturally() {
        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
        TagResolver line = TagResolver.resolver(Placeholder.unparsed("pos", "1"), Placeholder.unparsed("name", "Anna"),
                Placeholder.unparsed("prize", "+1"), Placeholder.unparsed("edelweiss", "2"));
        assertTrue(plain.serialize(Texts.text("de", "mini.line.points", line, Texts.count("points", 1))).contains("(1 Punkt)"));
        assertTrue(plain.serialize(Texts.text("de", "mini.line.points", line, Texts.count("points", 0))).contains("(0 Punkte)"));
        assertTrue(plain.serialize(Texts.text("en", "mini.line.points", line, Texts.count("points", 7))).contains("(7 points)"));
        assertEquals("+1 coin", plain.serialize(Texts.text("en", "mini.prize.taler", Texts.count("n", 1))));
        assertEquals("+4 coins", plain.serialize(Texts.text("en", "mini.prize.taler", Texts.count("n", 4))));
        assertEquals("1 Feld weit", plain.serialize(Texts.text("de", "roll.sub", Texts.count("n", 1))));
        assertEquals("12 Felder weit", plain.serialize(Texts.text("de", "roll.sub", Texts.count("n", 12))));
        assertTrue(plain.serialize(Texts.text("de", "top.line", line, Texts.count("wins", 1))).contains("1 Sieg ·"));
        assertTrue(plain.serialize(Texts.text("de", "stats.mode", Placeholder.unparsed("mode", "Seilschaft"),
                Texts.count("games", 1), Texts.count("wins", 0))).endsWith("1 Partie · 0 Siege"));
        assertEquals("1 duel won", plain.serialize(Texts.text("en", "award.duellkoenig.value", Texts.count("n", 1))));
    }

    /** Minecraft zeigt nur Zeichen der „Basic Multilingual Plane“ – Emoji wie 🏆 wären ein leeres Kästchen. */
    @Test
    void onlyCharactersMinecraftCanShow() {
        for (String key : Texts.keys()) {
            for (String lang : new String[] {"de", "en"}) {
                String text = Texts.raw(lang, key);
                assertEquals(text.length(), text.codePointCount(0, text.length()), lang + " " + key);
            }
        }
    }

    @Test
    void englishIsReallyEnglish() {
        assertNotEquals(Texts.raw("de", "turn.you"), Texts.raw("en", "turn.you"));
        assertEquals(Texts.raw("de", "board.schloss"), Texts.raw("fr", "board.schloss"), "unbekannte Sprache → Deutsch");
        assertEquals("missing.key", Texts.raw("de", "missing.key"));
    }

    /** Jeder Schlüssel, der zusammengesetzt wird (Felder, Gegenstände, Minispiele …), hat einen Text. */
    @Test
    void composedKeysExist() {
        List<String> keys = new ArrayList<>();
        for (BoardId b : BoardId.values()) {
            keys.addAll(List.of("board." + b.id(), "board." + b.id() + ".short", "board." + b.id() + ".desc",
                    "goal." + b.id()));
        }
        for (FieldType f : FieldType.values()) {
            keys.addAll(List.of(f.key(), f.key() + ".sign"));
        }
        for (ItemType i : ItemType.values()) {
            keys.addAll(List.of(i.key(), i.key() + ".desc", "use." + i.id()));
        }
        for (Luck l : Luck.values()) {
            keys.add(l.key());
        }
        for (MiniType m : MiniType.values()) {
            keys.addAll(List.of(m.key(), m.key() + ".desc"));
        }
        for (Mode m : Mode.values()) {
            keys.addAll(List.of("mode." + m.id() + ".desc", "game.goal." + m.id()));
        }
        for (String p : List.of("used", "missing", "target", "number", "shield")) {
            keys.add("bag.problem." + p);
        }
        for (String c : de.moinmorni.party.mini.TrachtenFarbenColours.ALL) {
            keys.add("colour." + c);
        }
        for (String k : keys) {
            assertTrue(Texts.keys().contains(k), "Text fehlt: " + k);
        }
    }

    /** Jeder Schlüssel, der im Quelltext steht, hat einen Text (Tippfehler fallen sofort auf). */
    @Test
    void everyKeyInTheSourceExists() throws IOException {
        Set<String> missing = new TreeSet<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher m = KEY.matcher(Files.readString(f));
                while (m.find()) {
                    String k = m.group(1);
                    boolean code = k.startsWith("minecraft.") || k.startsWith("moinmorni.") || k.startsWith("de.")
                            || k.endsWith(".yml") || k.endsWith(".txt") || k.endsWith(".json") || k.endsWith(".log")
                            || k.endsWith(".properties") || k.endsWith(".net") || k.contains("block.")
                            || k.startsWith("entity.") || k.startsWith("ui.") || k.startsWith("item.armor")
                            || k.startsWith("regeln.") || k.startsWith("spieler.") || k.startsWith("warten.")
                            || k.startsWith("zug.") || k.startsWith("minispiele.") || k.startsWith("minispiel-marathon.") || k.startsWith("seilschaft.") || k.startsWith("schnelle-partie.") || k.startsWith("bergduell.") || k.startsWith("huettenaufstieg.") || k.startsWith("auszeichnungen.")
                            || k.endsWith(".");
                    if (!code && !Texts.keys().contains(k)) {
                        missing.add(k + " (" + f.getFileName() + ")");
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "Texte fehlen: " + missing);
    }
}
