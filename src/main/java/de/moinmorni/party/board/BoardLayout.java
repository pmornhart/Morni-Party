package de.moinmorni.party.board;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Der Weg eines Spielbretts: Felder von 0 (Start) bis zum Ziel (letztes Feld). Der Weg ist eine Spirale um die Mitte
 * – beim Berg nach oben, beim Bergwerk nach unten. Rein rechnerisch (ohne Minecraft), damit Spielregeln und Tests
 * ohne Server laufen; den Bauplan dazu macht {@code world.BoardPlans}.
 */
public final class BoardLayout {

    /** Zahl der Felder (mit Start und Ziel). */
    public static final int FIELDS = 36;
    private static final int SAMPLES = 4000;

    /**
     * Form der Spirale.
     *
     * @param outer  Abstand des Starts von der Mitte
     * @param inner  Abstand des Ziels von der Mitte
     * @param height Höhenunterschied zwischen Start und Ziel
     * @param turns  Umdrehungen der Spirale
     * @param up     true = Weg führt nach oben (Berg), false = nach unten (Bergwerk)
     * @param angle  Startwinkel (Bogenmaß)
     */
    public record Shape(double outer, double inner, double height, double turns, boolean up, double angle) {
    }

    /** Ein Punkt auf der Mittellinie des Wegs (fein abgetastet, für Bauplan und Wege zwischen den Feldern). */
    public record Sample(double x, double z, double y, double r) {
    }

    private final BoardId id;
    private final Shape shape;
    private final List<Field> fields;
    private final List<Sample> samples;

    private BoardLayout(BoardId id, Shape shape, List<Field> fields, List<Sample> samples) {
        this.id = id;
        this.shape = shape;
        this.fields = Collections.unmodifiableList(fields);
        this.samples = Collections.unmodifiableList(samples);
    }

    /** Die Felder-Folge zwischen Start und Ziel (wiederholt sich, verschoben je Brett). */
    private static final FieldType[] PATTERN = {
        FieldType.TALER, FieldType.TALER, FieldType.GLUECK, FieldType.TALER, FieldType.VERLUST,
        FieldType.GEGENSTAND, FieldType.TALER, FieldType.PECH, FieldType.TALER, FieldType.DUELL,
        FieldType.VERLUST, FieldType.LADEN,
    };

    public static BoardLayout of(BoardId id) {
        return switch (id) {
            case ALMSOMMER -> build(id, new Shape(42, 7, 30, 1.3, true, 0.25 * Math.PI), 0);
            case SCHLOSS -> build(id, new Shape(44, 9, 26, 1.2, true, 1.5 * Math.PI), 4);
            case GOLDSTOLLEN -> build(id, new Shape(42, 7, 26, 1.35, false, 0.9 * Math.PI), 8);
        };
    }

    static BoardLayout build(BoardId id, Shape s, int patternShift) {
        // Spirale fein abtasten, dann Felder in gleichen Abständen entlang des Wegs verteilen.
        List<Sample> samples = new ArrayList<>(SAMPLES + 1);
        double[] length = new double[SAMPLES + 1];
        for (int i = 0; i <= SAMPLES; i++) {
            double t = (double) i / SAMPLES;
            samples.add(sample(s, t));
            if (i > 0) {
                Sample a = samples.get(i - 1);
                Sample b = samples.get(i);
                length[i] = length[i - 1] + Math.hypot(b.x() - a.x(), b.z() - a.z());
            }
        }
        double total = length[SAMPLES];
        List<Field> fields = new ArrayList<>(FIELDS);
        int j = 0;
        for (int f = 0; f < FIELDS; f++) {
            double want = total * f / (FIELDS - 1);
            while (j < SAMPLES && length[j] < want) {
                j++;
            }
            Sample p = samples.get(j);
            FieldType type = f == 0 ? FieldType.START
                    : f == FIELDS - 1 ? FieldType.ZIEL
                    : PATTERN[(f - 1 + patternShift) % PATTERN.length];
            fields.add(new Field(f, type, (int) Math.round(p.x()), (int) Math.round(p.y()), (int) Math.round(p.z())));
        }
        return new BoardLayout(id, s, fields, samples);
    }

    private static Sample sample(Shape s, double t) {
        double r = s.outer() - (s.outer() - s.inner()) * t;
        double a = s.angle() + s.turns() * 2 * Math.PI * t;
        double y = s.up() ? s.height() * t : s.height() * (1 - t);
        return new Sample(Math.cos(a) * r, Math.sin(a) * r, y, r);
    }

    /**
     * Höhe des Geländes in diesem Abstand von der Mitte – genau so, dass der Weg darauf liegt (beim Berg fällt sie
     * nach außen ab, beim Bergwerk steigt sie nach außen an).
     */
    public double terrainHeight(double d) {
        double span = shape.outer() - shape.inner();
        double u = (shape.outer() - d) / span; // 0 außen am Start, 1 innen am Ziel
        if (d < shape.inner()) {
            u = 1;
        }
        return shape.up() ? shape.height() * u : shape.height() * (1 - u);
    }

    public BoardId id() {
        return id;
    }

    public Shape shape() {
        return shape;
    }

    public List<Field> fields() {
        return fields;
    }

    public Field field(int index) {
        return fields.get(index);
    }

    public List<Sample> samples() {
        return samples;
    }

    public int size() {
        return fields.size();
    }

    /** Index des Ziels. */
    public int goal() {
        return fields.size() - 1;
    }

    /** Felder, auf denen der Edelweiß-Stand stehen darf. */
    public List<Integer> standFields() {
        List<Integer> out = new ArrayList<>();
        for (Field f : fields) {
            if (f.type().standAllowed()) {
                out.add(f.index());
            }
        }
        return out;
    }
}
