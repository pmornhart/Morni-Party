package net.morni.party.mini;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import net.morni.party.world.Plan;
import org.bukkit.Material;

/**
 * Die Minispiele (eigene Entwürfe, kurz und fair, ohne schwierige Steuerung – auch mit Bedrock gut spielbar). Jedes
 * hat einen eigenen Platz, der beim Bau einmal entsteht und vor jeder Runde frisch gelegt wird. Neue Minispiele immer
 * hinten anhängen – die Reihenfolge bestimmt den Platz in der Welt ({@link net.morni.party.world.Places}).
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
    SCHNEEBALLSCHLACHT(Material.SNOW_BLOCK, 45, Scoring.Kind.POINTS, SchneeballSchlacht::new, SchneeballSchlacht::build),
    SCHLITTENRENNEN(Material.SPRUCE_BOAT, 60, Scoring.Kind.RACE, SchlittenRennen::new, SchlittenRennen::build),
    ENZIAN_SUCHE(Material.CORNFLOWER, 40, Scoring.Kind.POINTS, EnzianSuche::new, EnzianSuche::build),
    MUSTERWEBER(Material.LOOM, 60, Scoring.Kind.POINTS, false, MusterWeber::new, MusterWeber::build),
    FOEHNSTURM(Material.FEATHER, 60, Scoring.Kind.SURVIVAL, FoehnSturm::new, FoehnSturm::build),
    /** Nur mit Seiten: 2 gegen 2 oder Duell. */
    TAUZIEHEN(Material.LEAD, 30, Scoring.Kind.POINTS, TauZiehen::new, TauZiehen::build),
    /** Nur einer gegen drei: der Einzelne jagt. */
    GAMSJAGD(Material.GOAT_HORN, 45, Scoring.Kind.SURVIVAL, GamsJagd::new, GamsJagd::build),
    /** Nur in der Spielart „Der Tatzelwurm“: alle zusammen gegen den Wurm. */
    TATZELWURM(Material.DRAGON_HEAD, 240, Scoring.Kind.POINTS, false, TatzelwurmKampf::new, TatzelwurmKampf::build);

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

    /**
     * So viele Leute hält der Platz aus (Startplätze, Spuren). Die meisten gehen bis 16 (Minispiel-Marathon);
     * Eisstockschießen und Brückenbau haben nur acht Plätze und fallen in größeren Runden aus.
     */
    public int maxPlayers() {
        return this == EISSTOCKSCHIESSEN || this == BRUECKENBAU ? 8 : net.morni.party.game.Marathon.MAX_PLAYERS;
    }

    /**
     * Passt das Minispiel zu dieser Spielweise? Jeder gegen jeden: alle außer Tauziehen und Gamsjagd. 2 gegen 2, 1 gegen
     * 3 und Duell: nur Spiele, in denen Seiten Sinn ergeben (beim Einzelnen zählen die Punkte mehrfach – sein Vorteil).
     */
    public boolean fits(net.morni.party.game.Teams.Format f) {
        return switch (f) {
            case FREE -> this != TAUZIEHEN && this != GAMSJAGD && this != TATZELWURM;
            case TEAM -> java.util.List.of(TAUZIEHEN, SCHNEEBALLSCHLACHT, TALERREGEN, HOLZHACKER, GOLDWASCHEN, ENZIAN_SUCHE)
                    .contains(this);
            case SOLO -> java.util.List.of(GAMSJAGD, MURMELTIER, SCHEIBENSCHIESSEN, GOLDWASCHEN).contains(this);
            case DUEL -> java.util.List.of(TAUZIEHEN, SCHEIBENSCHIESSEN, HOLZHACKER, BERGSPRINT, SCHNEEBALLSCHLACHT,
                    REMPELEI, MURMELTIER).contains(this);
        };
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
