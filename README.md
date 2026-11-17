# Morni Party

Der Brettspiel-Modus des Minecraft-Netzwerks **MoinMorni** (moinmorni.net): ein Spieleabend für **2 bis 8** –
würfeln, über das Spielbrett ziehen, Taler sammeln, am wandernden Stand **Edelweiß** kaufen, und nach jeder Runde ein
kurzes **Minispiel** für alle. Ein Paper-Plugin für Paper 26.x (Java 25). Es läuft im Netzwerk und auch ganz allein.

## So geht's

1. **Festplatz** (Wartebereich): eine schwebende Wiese mit Maibaum. Mit der **Farbe** in der Schnellleiste (oder
   `/mitspielen`) meldest du dich an, mit **Karte** und **Uhr** stimmst du über Spielbrett und Spielart ab. Ab zwei
   Leuten läuft ein Countdown; mit acht geht es schneller. Eine Party (vom Netzwerk) spielt immer zusammen: meldet sich
   die Party-Leitung an, kommen alle Mitglieder auf dem Festplatz mit.
2. **Dein Zug:** Rechtsklick auf den **Würfel**. Vorher darfst du einen Gegenstand aus dem **Rucksack** einsetzen. Die
   Figur bist du selbst – du ziehst Feld für Feld.
3. **Edelweiß-Stand:** Kommst du am Stand vorbei (oder landest darauf), kannst du ein Edelweiß für 20 Taler kaufen.
   Danach zieht der Stand an ein anderes Feld.
4. Nach jeder Runde (alle waren einmal dran) kommt ein **Minispiel** (30–60 Sekunden). Platz 1 bekommt 10 Taler,
   Platz 2 bekommt 6, dann 4, 3, 2, 1 …
5. **Ende:** Siegerehrung, Statistik, zurück auf den Festplatz.

### Spielarten

| Spielart | Wer gewinnt? |
|---|---|
| **Gipfelrennen** | Wer zuerst das Ziel erreicht. Spätestens nach 20 Runden ist Schluss – dann zählt, wer am weitesten ist (danach Edelweiß, Taler). |
| **Edelweiß-Jagd** (10, 15 oder 20 Runden) | Wer nach der letzten Runde die meisten Edelweiß hat (bei Gleichstand: mehr Taler). Wer über das Ziel hinauskommt, fährt mit der Seilbahn zurück ins Tal und bekommt 5 Taler Gipfelbonus. |
| **Minispiel-Marathon** (6, 10 oder 15 Minispiele) | Ohne Spielbrett, **2 bis 16** Leute: ein Minispiel nach dem anderen (gemischt, ohne Wiederholung). Nach jedem gibt es Platz-Punkte (10, 7, 5, 4, 3, 2, 1 …), Zwischenstand im Chat und in der Seitenleiste; am Ende gewinnt, wer die meisten hat. Minispiele, die nur 8 Plätze haben (Eisstockschießen, Brückenbau), fallen in größeren Runden aus. |
| **Seilschaft** (10 oder 15 Runden) | Edelweiß-Jagd **zu zweit**: Taler und Edelweiß gehören beiden (ein gemeinsamer Beutel), die Züge wechseln zwischen den Seilschaften, zu viert ist jedes Minispiel 2 gegen 2. Partys aus zwei Leuten sind eine Seilschaft. Kein Talerklau beim Partner, kein Duell gegen ihn; Platztausch mit dem Partner geht. Gerade Zahl, mindestens vier. |
| **Schnelle Partie** (5 Runden) | Edelweiß-Jagd für zwischendurch: 20 Start-Taler, Edelweiß für 15, Minispiele etwas kürzer. |
| **Bergduell** (8 Runden) | Genau zwei auf dem Brett, jedes Minispiel nach der Runde ist ein **Duell** (Sieger 8 Taler). |
| **Hüttenaufstieg** | Ohne Brett, **2 bis 16**: alle haben 3 ❤. Nach jedem Minispiel verliert die **untere Hälfte** ein Leben (bei zwei: der Zweite; ein geteilter Platz zählt nach dem besten Rang; alle gleich = niemand). Wer keins mehr hat, schaut zu – wer zuletzt übrig ist, erreicht die Hütte. |
| **Gipfel-Turnier** | Ohne Brett, **2 bis 16**: K.-o.-Turnier aus Duell-Minispielen (Freilose werden verteilt). Unentschieden = ein neues Duell, beim zweiten das Los. Wer wartet oder raus ist, schaut zu; der **Turnierbaum** steht als Tafel vor den Zuschauern (in der eigenen Sprache). |
| **Der Tatzelwurm** (Koop) | Ohne Brett, alle **zusammen** gegen einen Wurm mit Katzenkopf in einer Felsenklamm, in drei Abschnitten: *Aus dem Boden* (der Kopf bricht an einem von sechs Wurmlöchern heraus – draufhauen; dazwischen spuckt er Felsbrocken, ein Schatten warnt), *Die Jagd* (er schlängelt durch die Klamm, nur leuchtende Schuppen sind verwundbar, der Kopf stößt um), *Der Zorn* (Bodenwellen – drüberspringen; Kopfschlag auf einen roten Kreis, danach liegt der Kopf kurz am Boden). Wer getroffen wird, ist kurz benommen, niemand scheidet aus. Gemeinsame Wertung mit bis zu drei Sternen (Tempo und Vorsicht). |

**Auszeichnungen** (Brett-Partien mit Edelweiß): vor der Siegerehrung bekommt je ein Edelweiß, wer die meisten Felder
gelaufen ist (*Wanderer*), die meisten Minispiele gewonnen hat (*Minispiel-Meister*), am öftesten auf roten Feldern
stand (*Pechvogel*, als Trost) und die meisten Duelle gewonnen hat (*Duellkönig*). In der config.yml abschaltbar
(`auszeichnungen`). Gibt es für eine Spielart noch nicht genug Leute (Seilschaft, Bergduell), wartet der Festplatz
höchstens zweimal einen Countdown länger und startet dann die beliebteste Spielart, die schon geht.

### Spielbretter (aus dem Code gebaut, eigene Entwürfe)

| Spielbrett | Was dich erwartet |
|---|---|
| **Almsommer am Morni-Kogel** | Grüner Berg mit Almhütte, Weide, Fichten und Blumen; der Weg windet sich hinauf bis zum Gipfelkreuz im Schnee. |
| **Schloss Weißenstein** | Unten der Schlossgarten mit Hecken, Rosenbögen, zwei Brunnen und Tulpenbeeten; durchs Torhaus mit Fallgitter geht es in die Burg: weiße Terrassen und Mauern mit Zinnen, Ecktürme mit blauen Spitzdächern und weiß-blauen Fahnen, oben der Bergfried mit Kegeldach. |
| **Goldstollen** | Ein offenes Bergwerk: Erzadern, Stützbalken mit Laternen, Förderturm am Rand – der Weg führt hinab zur Goldkammer. |
| **Königssee** | Stege aus Holz über einem Bergsee zwischen steilen Felswänden, Seerosen, Schilf, Bootshaus und Ruderbooten; Ziel ist die Kapelle mit roten Zwiebeltürmen auf der Insel. **Fähre** (türkis): mit dem Boot in gerader Linie übers Wasser mindestens 6 Felder voraus (Bojen zeigen die Route). |
| **Wiesn** | Festwiese mit Bierzelten (weiß-blau gestreift), Kettenkarussell, Achterbahn, Buden mit Lebkuchenherzen, Maibaum und Lichtern; in der Mitte das Riesenrad. **Riesenrad** (rosa): Platz mit jemandem tauschen. **Fahrgeschäft** (lila): Achterbahn (4 vor), Autoscooter (wer bis 3 Felder dahinter steht, gibt dir 2 Taler), Kettenkarussell (es schleudert dich 2 zurück bis 3 vor) oder Losbude (Gegenstand oder Taler). |
| **Wintersonnwend** | Verschneiter Berg mit verschneiten Fichten, Lawinenverbau, Skihütte, Iglu und gefrorenem See; oben das große Sonnwendfeuer, kleine Feuer auf den Felsen. **Eis** (Packeis): man rutscht bis ans Ende der Eisfläche weiter. **Lawine** (hellgrau, zählt als rotes Feld): du und alle bis 5 Felder vor dir rutschen 3 zurück (Schild schützt). |

Jedes Brett hat 36 Felder auf einem Spiralweg:

| Feld | Farbe | Was passiert |
|---|---|---|
| Talerfeld | hellblau | +3 Taler |
| Verlustfeld | rot | −3 Taler |
| Glücksfeld | grün | Geschenk vom Almwirt, Rückenwind (3 vor), alle geben dir etwas, Fundstück |
| Pechfeld | schwarz | Steinschlag, Nebel (3 zurück), du gibst eine Runde aus, etwas fällt aus dem Rucksack |
| Duellfeld | orange | Duell gegen jemanden deiner Wahl: ein kurzes Minispiel zu zweit – der Sieger bekommt 5 Taler |
| Krämerladen | gelb | Gegenstände kaufen |
| Fundstück | rosa | ein Gegenstand gratis |

### Gegenstände (höchstens 3 im Rucksack, einer pro Zug, vor dem Würfeln)

| Gegenstand | Preis | Wirkung |
|---|---|---|
| Doppelwürfel | 8 | zwei Würfel (2–12) |
| Zielwürfel | 6 | Augenzahl 1–6 selbst wählen |
| Platztausch | 10 | Platz mit jemandem tauschen |
| Talerklau | 8 | jemandem 7 Taler abnehmen |
| Schild | 5 | schützt einmal vor Verlust, Pech, Klau und Tausch |
| Lockruf | 12 | direkt vor den Edelweiß-Stand |

### Minispiele (eigene Entwürfe, einfache Steuerung – auch mit Bedrock)

| Minispiel | Wertung | Worum es geht | Kulisse |
|---|---|---|---|
| Bröckelboden | Überleben | Drei Böden übereinander, wo du stehst, bröckelt es weg. | Gletscher mit Eiszacken und verschneiten Gipfeln |
| Talerregen | Punkte | Goldtaler fallen vom Himmel – einsammeln (Goldbarren = 3). | Marktplatz mit Ständen und Schatzhaus |
| Bergsprint | Rennen | Wettlauf den Almweg hinauf über Heuballen. | Felsen, Hütte am Start, Gipfel mit Fahnen am Ziel |
| Pfeilhagel | Überleben | Pfeile prasseln herab – ausweichen. | Burghof mit Mauern, Türmen und Bannern |
| Heiße Brezn | Überleben | Mit einem Schlag weitergeben; wer sie hält, wenn die Zeit abläuft, ist raus. | Bäckerei mit rauchendem Ofen, Brezn-Theke, Biertische |
| Trachtenfarben | Überleben | Farbe merken – danach verschwinden alle anderen. | Tanzplatz mit Maibaum, Wimpeln und Musikbühne |
| Gipfelkönig | Punkte | Oben auf dem goldenen Gipfel stehen, mit dem Wanderstock schubsen. | Schneegipfel über den Wolken mit Gipfelkreuz |
| Kuhglocke | Punkte | Warten, bis die Tafel grün wird, dann schnell klicken – zu früh kostet. | Almweide mit Zaun, Kühen und Stall |
| Schäfchen zählen | Punkte | Schafe im Pferch zählen, im Menü die Zahl wählen. | Bauernhof mit Steinmauer, Heuschober, Obstbäumen |
| Holzhacker | Punkte | Drei Hiebe spalten einen Holzklotz. | Sägewerk mit Wasserrad, Holzstapel, Fichtenwald |
| Rempelei | Überleben | Die anderen von der Plattform schubsen; der Rand bröckelt. | Felsnadel über einer Schlucht |
| Scheibenschießen | Punkte | Mit Schneebällen auf Zielscheiben werfen. | Schützenfest mit Bierzelt, Riesenrad und Preisregal |
| Lawinenhang | Rennen | Den Schneehang hinauf; rollende Schneebrocken schieben dich ein Stück zurück. | Bergflanke mit Lawinenzäunen, Starthütte, Bergwacht-Hütte oben |
| Maibaumkraxeln | Rennen | An Leitern und Ranken den Maibaum hinauf zum Kranz; Stücke brechen weg und wachsen nach. | Dorfplatz mit Häusern, Zwiebelturm, Wirtsgarten und Wimpeln |
| Eisstockschießen | Punkte | Drei Würfe für alle gleichzeitig: Kraftbalken, Punkte nach Abstand zur Daube (5/3/2/1); Stöcke stoßen sich weg. | Zugefrorener Bergsee mit Stockschützenhütte und Glühweinstand |
| Murmeltier-Klopfen | Punkte | Auf herausschauende Murmeltiere hauen (golden = 3); Steinbrocken kosten einen Punkt. | Bergwiese mit Felsbrocken, Almrosen und Murmeltierbauten |
| Goldwaschen | Punkte | Kies- und Sandhaufen am Bach auswaschen (Schlag/Rechtsklick); Goldkorn 1, Goldklumpen 3, wo es glitzert, lohnt es sich. | Goldsucher-Lager am Bergbach mit Wasserfall, Waschrinne, Zelten und Lore |
| Schuhplattler | Punkte | Im Dreivierteltakt: Springen, Ducken, Klatschen (Klick) oder Stillstehen auf den nächsten Stampfer – genau = 2, knapp = 1. | Festzelt mit weiß-blauem Dach, Musikbühne und Bänken |
| Brückenbau | Rennen | Mit Brettern eine eigene Brücke über die Schlucht bauen; wer fällt, fängt am Start wieder an (die Brücke bleibt). | Tiefe Schlucht mit Felswänden, Fluss und Wasserfall |
| Schneeballschlacht | Punkte | Jeder gegen jeden, Schneebälle aus den Schneewehen holen; Treffer = Punkt, Getroffene leuchten kurz und sind so lange sicher. | Winterwiese mit Schneemännern, Iglu und verschneiten Fichten |
| Schlittenrennen | Rennen | Zwei Runden im Schlitten (Holzboot) über die Eisbahn; sechs Tore zählen nur der Reihe nach; wer herausfällt, sitzt gleich wieder. | Verschneiter Wald, Tribüne und Hütte im Innenfeld, Torbögen |
| Enzian-Suche | Punkte | Blaue Enziane auf der Almwiese finden und pflücken – im hohen Gras, hinter Felsen und Büschen versteckt. | Weite Alm mit Kühen, Holzzaun und Almhütte |
| Musterweber | Punkte | Ein 4×4-Muster kurz anschauen, dann am eigenen Webrahmen aus dem Gedächtnis nachlegen; drei Runden mit 2, 3, 4 Farben. | Weberei mit Fachwerk, Wollballen und Garten |
| Föhnsturm | Überleben | Windböen (vorher Wolken, Ton und Richtung im Titel) schieben vom Gipfel; Schutz hinter Felsen oder geduckt. | Gipfel über dem Wolkenmeer mit Föhnmauer und Linsenwolken |
| Tauziehen | Seiten | Nur 2 gegen 2 und im Duell: so schnell wie möglich klicken, wer den roten Knoten ganz zu sich zieht, gewinnt. | Festwiese mit Schlammgrube, Bretterstegen, Zuschauerbänken und blau-roten Fahnen |
| Gamsjagd | Seiten | Nur einer gegen drei: der Einzelne ist der Jäger (schneller, startet drei Sekunden später), ein Schlag fängt eine Gämse; bleibt eine übrig, gewinnen die Gämsen. | Weite Alm mit Felsen, Büschen und hohem Gras zum Ausweichen |

#### Jeder gegen jeden, 2 gegen 2, 1 gegen 3, Duell

Nach jeder Brett-Runde zählt bei **genau vier Leuten** die Farbe des Feldes, auf dem jeder zuletzt stand: **rot**
(Verlust, Pech, Duell, Lawine) oder **blau** (alle anderen: Taler, Glück, Laden, Fundstück, Fähre, Riesenrad,
Fahrgeschäft, Eis).

| Farben | Spielweise | Minispiele | Taler |
|---|---|---|---|
| alle gleich | jeder gegen jeden | alle außer Tauziehen und Gamsjagd | 10, 6, 4, … nach Platz |
| 2 blau, 2 rot | **2 gegen 2** (Blau gegen Rot, farbige Mützen) | Tauziehen, Schneeballschlacht (eigene Seite zählt nicht), Talerregen, Holzhacker, Goldwaschen, Enzian-Suche | Sieger je 8, Gleichstand alle 3 |
| 3 : 1 | **1 gegen 3** – der Einzelne hat einen Vorteil | Gamsjagd (schneller), Murmeltier-Klopfen, Scheibenschießen, Goldwaschen (seine Punkte zählen vierfach) | Einzelner als Sieger 18, sonst die drei je 8 |
| Duellfeld | **Duell 1 gegen 1** | Tauziehen, Scheibenschießen, Holzhacker, Bergsprint, Schneeballschlacht, Rempelei, Murmeltier-Klopfen | Sieger bekommt 5 Taler vom Verlierer (Schild schützt) |

Bei den Seiten zählt bei Punkte-Spielen die Summe der Seite, bei Überleben und Rennen der beste Platz der Seite.
Die Aktionsleiste zeigt die eigenen Punkte und die der Seite.

Wertung: **Punkte** – wer die meisten hat; **Überleben** – wer am längsten dabei bleibt; **Rennen** – wer zuerst
ankommt, danach alle anderen nach der geschafften Strecke.

Alle Menüs sind Truhen und gehen auch mit Bedrock (Geyser); auf Bedrock reicht ein Antippen statt Rechtsklick.

## Befehle

| Befehl | Was er tut |
|---|---|
| `/mitspielen` | für die nächste Partie anmelden (auch `/play`) |
| `/verlassen` | Warteschlange, Partie oder Zuschauen verlassen (auch `/leave`) |
| `/zuschauen [spieler]` | einer Partie zuschauen (ohne Namen: Menü) |
| `/stats [spieler]` · `/top` | Statistik und Bestenliste |
| `/mparty …` | Verwaltung (`moinmorni.party.admin` – im Netzwerk Owner und Admin über „Spielmodi verwalten“): `info`, `zustand` (JSON), `start`, `stopp`, `geld <spieler> <betrag>` (Taler in der laufenden Partie dazugeben, zum Testen), `wuerfel/taler/edelweiss/feld <spieler> <zahl>`, `gegenstand <spieler> <gegenstand>`, `stand <feld>`, `minispiel <name>`, `seiten auto/frei/team/einzel`, `wurm <leben>`, `neubauen`, `neuladen` |

## Im Netzwerk

Das Netzwerk-Plugin („MoinMorni“) stellt die Schnittstelle `de.moinmorni.api.MoinMorni` bereit; Morni Party nutzt
sie, wenn sie da ist (`softdepend`): Sprache, Party, Statistik, Zustand für Lobby-Figur und Webseite, zurück zur Lobby.
Ohne Netzwerk: deutsche Texte, Statistik nur lokal, „zur Lobby“ trennt die Verbindung mit einem Hinweis.

Ab Schnittstelle 2 meldet Morni Party außerdem, wer gerade spielt (`playing`: vom Start der Partie bis zum Ausscheiden
oder Ende), hält nach einem Verbindungsabbruch den Platz frei (`holdSlot`: die Lobby bietet „Zurück ins Spiel“ an, `0`
nimmt das Angebot zurück) und schickt mit dem Bett auf dem Festplatz über `askToLobby`. Ein älteres Netzwerk-Plugin
kennt das nicht; dann läuft alles wie bisher.

### Zurück ins Spiel

Verbindung mitten in der Partie weg (Spiel abgestürzt, Internet weg)? Der Platz bleibt 120 Sekunden frei
(`rueckkehr-sekunden`): Figur, Taler, Edelweiß und Gegenstände bleiben. Ist die Person am Zug, würfelt das Spiel nach
der Würfel-Zeit für sie; Minispiele laufen ohne sie (nicht angetreten, im Hüttenaufstieg letzter Platz). Wer
rechtzeitig zurückkommt, steht wieder auf seinem Feld (oder schaut dem laufenden Minispiel zu und ist danach wieder
dabei); wer nicht kommt, ist raus wie bisher. Sind alle weg, hält die Partie an.

Statistik nach jeder Partie (`stats("party", …)`, nur wer bis zum Schluss dabei war):

- hochgezählt: `games`, `wins`, `edelweiss`, `minigames_won`, je Spielart `games_<spielart>` und `wins_<spielart>`
  (`gipfelrennen`, `edelweiss_jagd`, `marathon`), im Gipfelrennen `summits` (Ziel erreicht)
- Bestwert: `edelweiss_best`, im Marathon `marathon_points_best`

Zustand: `waiting` (keine Partie) oder `running`, dazu die Spielerzahl.

Bei jedem Push auf `main` baut GitHub ein Release mit `Morni-Party.jar` (Version aus `pom.xml`); das Netzwerk holt sich
das neueste. Aktuell ist **0.2.2** (Zurück ins Spiel; mit 0.2.0 kamen neue Spielarten, Bretter und Minispiele) – was
sich je Version geändert hat, steht in `docs/releases/`.

## Bauen und testen

```bash
mvn package                      # Tests + target/Morni-Party.jar (Java 25)
mvn -Ppaper-26.1 compile         # passt der Code auch zu Paper 26.1?
test/run.sh                      # echter Test: Paper-Server + Test-Spieler (mineflayer)
```

`test/run.sh` baut das Plugin und ein Test-Netzwerk (`test/fake-network`: tut so, als wäre es das Netzwerk-Plugin –
Bert spricht Englisch, Anna und Bert sind eine Party, alle Meldungen landen in `calls.log`), richtet einen
Paper-26.1.2-Server ein (offline, Port 25770, RCON 25870) und schickt vier Test-Spieler durch drei Partien
(`test/party-test.cjs`): Edelweiß-Jagd auf Schloss Weißenstein, Gipfelrennen auf dem Almsommer und eine lange
Edelweiß-Jagd im Goldstollen – dabei kommt jedes der zwölf ersten Minispiele einmal dran. Partie 4 spielt die neuen
Minispiele (Paket 1: Lawinenhang, Maibaumkraxeln, Eisstockschießen, Murmeltier-Klopfen), Partie 5 Paket 2
(Goldwaschen, Schuhplattler, Brückenbau, Schneeballschlacht), Partie 6 Paket 3 (Schlittenrennen, Enzian-Suche,
Musterweber, Föhnsturm). `PARTS=1,2` spielt nur einen Teil,
`ONLY=lawinenhang,murmeltier` nur diese Minispiele aus den Partien 4 bis 6. Partie 7 ist ein Minispiel-Marathon
zu viert (drei Minispiele, Zwischenstand, Siegerehrung, Statistik, Englisch). Partie 8 prüft die Seiten zu viert:
2 gegen 2 von selbst nach den Feldfarben, Tauziehen, Gamsjagd (einer gegen drei) und einen Gleichstand. Partie 9 ist
eine Seilschaft zu viert (die Party Anna + Bert als Seilschaft, gemeinsame Taler, 2 gegen 2), Partie 10 eine Schnelle
Partie, Partie 11 ein Bergduell, Partie 12 ein Hüttenaufstieg (Leben, Ausscheiden, Gleichstand), Partie 13 ein
Gipfel-Turnier zu viert (Turnierbaum, Halbfinale, Finale), Partie 14 der Tatzelwurm zu zweit (drei Abschnitte,
Treffer, Sterne) und Partie 15 die drei neuen Bretter (Fähre am Königssee, Riesenrad und Fahrgeschäft auf der Wiesn,
Lawine und Eis zur Wintersonnwende), Partie 16 „Zurück ins Spiel“ (Verbindung weg und rechtzeitig zurück, zu lange
weg, Bett auf dem Festplatz, älteres Netzwerk-Plugin). Ohne `PARTS` laufen alle 16 Partien (gut eine Stunde).

## Einstellungen

`plugins/MorniParty/config.yml`: Rückkehr nach Verbindungsabbruch, Welt, Zahl der Spieltische, Spielerzahl, Countdown, automatisch mitspielen,
Rundenzahlen der Edelweiß-Jagd, Zug- und Menü-Zeiten, Tempo der Figur, welche Minispiele (und ob zufällig), Dauer der
Minispiele und alle Zahlen der Spielregeln, Team-/Einzel-Taler, Seilschaft, Schnelle Partie, Bergduell, Hüttenaufstieg
(Leben, höchstens Minispiele) und Auszeichnungen – mit Erklärungen in der Datei.

---

Verfasst mit Claude 🤖
