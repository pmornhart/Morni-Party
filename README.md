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

### Spielbretter (aus dem Code gebaut, eigene Entwürfe)

| Spielbrett | Was dich erwartet |
|---|---|
| **Almsommer am Morni-Kogel** | Grüner Berg mit Almhütte, Weide, Fichten und Blumen; der Weg windet sich hinauf bis zum Gipfelkreuz im Schnee. |
| **Schloss Weißenstein** | Unten der Schlossgarten mit Hecken, Rosenbögen, zwei Brunnen und Tulpenbeeten; durchs Torhaus mit Fallgitter geht es in die Burg: weiße Terrassen und Mauern mit Zinnen, Ecktürme mit blauen Spitzdächern und weiß-blauen Fahnen, oben der Bergfried mit Kegeldach. |
| **Goldstollen** | Ein offenes Bergwerk: Erzadern, Stützbalken mit Laternen, Förderturm am Rand – der Weg führt hinab zur Goldkammer. |

Jedes Brett hat 36 Felder auf einem Spiralweg:

| Feld | Farbe | Was passiert |
|---|---|---|
| Talerfeld | hellblau | +3 Taler |
| Verlustfeld | rot | −3 Taler |
| Glücksfeld | grün | Geschenk vom Almwirt, Rückenwind (3 vor), alle geben dir etwas, Fundstück |
| Pechfeld | schwarz | Steinschlag, Nebel (3 zurück), du gibst eine Runde aus, etwas fällt aus dem Rucksack |
| Duellfeld | orange | Würfelduell gegen jemanden deiner Wahl – wer höher würfelt, bekommt 5 Taler |
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
| Bergsprint | Ziel | Wettlauf den Almweg hinauf über Heuballen. | Felsen, Hütte am Start, Gipfel mit Fahnen am Ziel |
| Pfeilhagel | Überleben | Pfeile prasseln herab – ausweichen. | Burghof mit Mauern, Türmen und Bannern |
| Heiße Brezn | Überleben | Mit einem Schlag weitergeben; wer sie hält, wenn die Zeit abläuft, ist raus. | Bäckerei mit rauchendem Ofen, Brezn-Theke, Biertische |
| Trachtenfarben | Überleben | Farbe merken – danach verschwinden alle anderen. | Tanzplatz mit Maibaum, Wimpeln und Musikbühne |
| Gipfelkönig | Punkte | Oben auf dem goldenen Gipfel stehen, mit dem Wanderstock schubsen. | Schneegipfel über den Wolken mit Gipfelkreuz |
| Kuhglocke | Punkte | Warten, bis die Tafel grün wird, dann schnell klicken – zu früh kostet. | Almweide mit Zaun, Kühen und Stall |
| Schäfchen zählen | Punkte | Schafe im Pferch zählen, im Menü die Zahl wählen. | Bauernhof mit Steinmauer, Heuschober, Obstbäumen |
| Holzhacker | Punkte | Drei Hiebe spalten einen Holzklotz. | Sägewerk mit Wasserrad, Holzstapel, Fichtenwald |
| Rempelei | Überleben | Die anderen von der Plattform schubsen; der Rand bröckelt. | Felsnadel über einer Schlucht |
| Scheibenschießen | Punkte | Mit Schneebällen auf Zielscheiben werfen. | Schützenfest mit Bierzelt, Riesenrad und Preisregal |

Alle Menüs sind Truhen und gehen auch mit Bedrock (Geyser); auf Bedrock reicht ein Antippen statt Rechtsklick.

## Befehle

| Befehl | Was er tut |
|---|---|
| `/mitspielen` | für die nächste Partie anmelden (auch `/play`) |
| `/verlassen` | Warteschlange, Partie oder Zuschauen verlassen (auch `/leave`) |
| `/zuschauen [spieler]` | einer Partie zuschauen (ohne Namen: Menü) |
| `/stats [spieler]` · `/top` | Statistik und Bestenliste |
| `/mparty …` | Verwaltung (`moinmorni.party.admin`): `info`, `zustand` (JSON), `start`, `stopp`, `wuerfel/taler/edelweiss/feld <spieler> <zahl>`, `gegenstand <spieler> <gegenstand>`, `stand <feld>`, `minispiel <name>`, `neubauen`, `neuladen` |

## Im Netzwerk

Das Netzwerk-Plugin („MoinMorni“) stellt die Schnittstelle `de.moinmorni.api.MoinMorni` bereit; Morni Party nutzt
sie, wenn sie da ist (`softdepend`): Sprache, Party, Statistik, Zustand für Lobby-Figur und Webseite, zurück zur Lobby.
Ohne Netzwerk: deutsche Texte, Statistik nur lokal, „zur Lobby“ trennt die Verbindung mit einem Hinweis.

Statistik nach jeder Partie (`stats("party", …)`, nur wer bis zum Schluss dabei war):

- hochgezählt: `games`, `wins`, `edelweiss`, `minigames_won`, je Spielart `games_gipfelrennen` /
  `games_edelweiss_jagd`, im Gipfelrennen `summits` (Ziel erreicht)
- Bestwert: `edelweiss_best`

Zustand: `waiting` (keine Partie) oder `running`, dazu die Spielerzahl.

Bei jedem Push auf `main` baut GitHub ein Release mit `Morni-Party.jar` (Version aus `pom.xml`); das Netzwerk holt sich
das neueste.

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
Edelweiß-Jagd im Goldstollen – dabei kommt jedes der zwölf Minispiele einmal dran. `PARTS=1,2` spielt nur einen Teil.

## Einstellungen

`plugins/MorniParty/config.yml`: Welt, Zahl der Spieltische, Spielerzahl, Countdown, automatisch mitspielen,
Rundenzahlen der Edelweiß-Jagd, Zug- und Menü-Zeiten, Tempo der Figur, welche Minispiele (und ob zufällig), Dauer der
Minispiele und alle Zahlen der Spielregeln – mit Erklärungen in der Datei.

---

Verfasst mit Claude 🤖
