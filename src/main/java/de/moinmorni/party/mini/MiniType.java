package de.moinmorni.party.mini;

import de.moinmorni.party.world.Plan;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import org.bukkit.Material;

/**
 * Die Minispiele (eigene Entwürfe, kurz und fair, ohne schwierige Steuerung – auch mit Bedrock gut spielbar). Jedes
 * hat einen eigenen Platz, der beim Bau einmal entsteht und vor jeder Runde frisch gelegt wird. Neue Minispiele immer
 * hinten anhängen – die Reihenfolge bestimmt den Platz in der Welt ({@link de.moinmorni.party.world.Places}).
 */
public enum MiniType {
    BROECKELBODEN(Material.SNOW_BLOCK, 60, Scoring.Kind.SURVIVAL, BroeckelBoden::new, BroeckelBoden::build),
    TALERREGEN(Material.GOLD_NUGGET, 30, Scoring.Kind.POINTS, TalerRegen::new, TalerRegen::build),
    BERGSPRINT(Material.LEATHER_BOOTS, 45, Scoring.Kind.RACE, BergSprint::new, BergSprint::build),
    PFEILHAGEL(Material.ARROW, 40, Scoring.Kind.SURVIVAL, PfeilHagel::new, PfeilHagel::build),
    HEISSE_BREZN(Material.BREAD, 75, Scoring.Kind.SURVIVAL, HeisseBrezn::new, HeisseBrezn::build),
    TRACHTENFARBEN(Material.LIME_WOOL, 60, Scoring.Kind.SURVIVAL, TrachtenFarben::new, TrachtenFarben::build),
    GIPFELKOENIG(Material.GOLD_BLOCK, 40, Scoring.Kind.POINTS, GipfelKoenig::new, GipfelKoenig::build),
    KUHGLOCKE(Material.BELL, 60, Scoring.Kind.POINTS, false, KuhGlocke::new, KuhGlocke::build),
    SCHAEFCHEN(Material.WHITE_WOOL, 60, Scoring.Kind.POINTS, false, Schaefchen::new, Schaefchen::build),
    HOLZHACKER(Material.IRON_AXE, 35, Scoring.Kind.POINTS, HolzHacker::new, HolzHacker::build),
    REMPELEI(Material.STICK, 60, Scoring.Kind.SURVIVAL, Rempelei::new, Rempelei::build),
    SCHEIBENSCHIESSEN(Material.TARGET, 35, Scoring.Kind.POINTS, ScheibenSchiessen::new, ScheibenSchiessen::build),
    LAWINENHANG(Material.SNOWBALL, 50, Scoring.Kind.RACE, LawinenHang::new, LawinenHang::build),
    MAIBAUMKRAXELN(Material.LADDER, 45, Scoring.Kind.RACE, MaibaumKraxeln::new, MaibaumKraxeln::build),
    EISSTOCKSCHIESSEN(Material.POLISHED_ANDESITE_SLAB, 75, Scoring.Kind.POINTS, false, EisstockSchiessen::new,
            EisstockSchiessen::build),
    MURMELTIER(Material.RABBIT_HIDE, 40, Scoring.Kind.POINTS, MurmelTier::new, MurmelTier::build),
    GOLDWASCHEN(Material.GOLD_NUGGET, 40, Scoring.Kind.POINTS, GoldWaschen::new, GoldWaschen::build),
    SCHUHPLATTLER(Material.NOTE_BLOCK, 50, Scoring.Kind.POINTS, SchuhPlattler::new, SchuhPlattler::build),
    BRUECKENBAU(Material.OAK_PLANKS, 60, Scoring.Kind.RACE, BrueckenBau::new, BrueckenBau::build),
    SCHNEEBALLSCHLACHT(Material.SNOW_BLOCK, 45, Scoring.Kind.POINTS, SchneeballSchlacht::new, SchneeballSchlacht::build);

    private final Material icon;
    private final int seconds;
    private final Scoring.Kind kind;
    private final boolean scaled;
    private final Function<MiniRun.Setup, MiniRun> factory;
    private final Consumer<Plan> builder;

    MiniType(Material icon, int seconds, Scoring.Kind kind, Function<MiniRun.Setup, MiniRun> factory,
            Consumer<Plan> builder) {
        this(icon, seconds, kind, true, factory, builder);
    }

    MiniType(Material icon, int seconds, Scoring.Kind kind, boolean scaled, Function<MiniRun.Setup, MiniRun> factory,
            Consumer<Plan> builder) {
        this.icon = icon;
        this.seconds = seconds;
        this.kind = kind;
        this.scaled = scaled;
        this.factory = factory;
        this.builder = builder;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT).replace("_", "-");
    }

    public Material icon() {
        return icon;
    }

    /** Längste Dauer in Sekunden (ohne Faktor). */
    public int seconds() {
        return seconds;
    }

    /**
     * Wird die Dauer mit dem Faktor aus der config.yml verkürzt? Spiele mit festen Runden (Kuhglocke, Schäfchen
     * zählen, Eisstockschießen …) enden von selbst – ihre Dauer ist nur die Obergrenze.
     */
    public boolean scaled() {
        return scaled;
    }

    public Scoring.Kind kind() {
        return kind;
    }

    /** Text-Schlüssel des Namens („mini.broeckelboden“); Beschreibung: + „.desc“. */
    public String key() {
        return "mini." + name().toLowerCase(Locale.ROOT).replace("_", "");
    }

    public MiniRun create(MiniRun.Setup setup) {
        return factory.apply(setup);
    }

    /** Bauplan des Platzes (Mitte 0/0, y = 0 ist der Boden, auf dem man steht). */
    public Plan plan() {
        Plan p = new Plan();
        builder.accept(p);
        p.connect();
        return p;
    }

    /** Bauplan mit Kulisse rundherum (nur beim Bau; vor jeder Runde wird nur {@link #plan()} neu gelegt). */
    public Plan fullPlan() {
        Plan p = new Plan();
        MiniDecor.build(this, p);
        builder.accept(p);
        p.connect();
        return p;
    }

    public static MiniType parse(String s) {
        if (s == null) {
            return null;
        }
        String l = s.toLowerCase(Locale.ROOT).replace("ä", "ae").replace("ö", "oe").replace("ü", "ue")
                .replace("ß", "ss").replace("_", "-");
        for (MiniType t : values()) {
            if (t.id().equals(l) || t.id().replace("-", "").equals(l.replace("-", ""))) {
                return t;
            }
        }
        return null;
    }
}
