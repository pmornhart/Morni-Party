# Stand – Morni Party

**Was es ist:** Der Brettspiel-Modus des MoinMorni-Netzwerks (Paper-Plugin `MorniParty`, Java 25, Paper 26.x).
Entscheidungen von Philip (14.11.2026): Brettspiel für 2–8, würfeln, Taler sammeln, Edelweiß (Siegwährung) am
wandernden Stand kaufen, Ereignisfelder (Glück, Pech, Duell, Laden, Gegenstand), Gegenstände (u. a. Doppelwürfel,
Tausch, Talerklau, Schild), nach jeder Runde ein Minispiel; Spielarten Gipfelrennen und Edelweiß-Jagd (10/15/20
Runden); drei Bretter Almsommer am Morni-Kogel, Schloss Weißenstein, Goldstollen; zwölf Minispiele. Vorbild nur die
Mechanik großer Party-Spiele – eigene Namen und eigene Optik.

## Aufbau

- `src/main/java/de/moinmorni/party/`: `PartyPlugin` (Start, Welt, Aufgaben), `Settings` (config.yml), `Texts` (de/en),
  `Ui`, `Items`, `Commands`.
- `board/` (ohne Minecraft): `BoardId`, `FieldType`, `Field`, `BoardLayout` (Spiralweg mit 36 Feldern, Höhe des Geländes).
- `game/` (ohne Minecraft): `Engine` (alle Spielregeln), `PartyPlayer`, `Rules`, `Mode`, `GameKind`, `ItemType`, `Luck`,
  `WaitQueue` (Partys bleiben zusammen), `Votes`.
- `play/`: `Lobby` (Festplatz, Warteschlange, Abstimmung, Countdown, Zuschauen, Statistik-Menü), `Game` (Partie: Züge,
  Menüs, Minispiele, Ende), `Games`, `Sidebar`.
- `mini/`: `MiniType` (die zwölf), `MiniRun` (Ablauf, Helfer), `Scoring` (Wertung, ohne Minecraft), je Minispiel eine Klasse.
- `world/`: `Plan`, `Decor`, `VoidGenerator`, `Places` (wo was steht), `HubPlan`, `BoardPlans`, `HubSigns`,
  `Construction` (baut nur bei neuer Bau-Version oder anderer Zahl Tische).
- `data/`: `PlayerStats`, `Store` (YAML im Hintergrund, Rangliste). `net/`: `Network`, `Connected`, `Standalone`.
- `src/api/java/de/moinmorni/api/MoinMorni.java`: Kopie der Netzwerk-Schnittstelle (nicht in der Jar).
- `test/`: `run.sh` (echter Server), `party-test.cjs` (mineflayer), `rcon.cjs`, `fake-network/` (Test-Netzwerk).

## Stand (15.11.2026)

- Version 0.1.0 (erster Stand, Branch `claude/erster-stand`, noch nicht veröffentlicht): alles oben Genannte.
- Bau-Version 2 (15.11. abends): Schloss Weißenstein als echte Burg – Terrassen mit weißen Mauern und Zinnen,
  Brustwehr außen am Weg, Ecktürme mit blauen Spitzdächern und weiß-blauen Fahnen, Bergfried (Zinnenkranz, Kegeldach,
  Banner), Torhaus mit Fallgitter, Schlossgarten mit Hecken, Rosenbögen, zwei Brunnen, Tulpenbeeten, Formschnitt.
  Weg, Felder und Standplätze unverändert. Kulissen um alle zwölf Minispiel-Plätze (`mini/MiniDecor`, nur beim Bau;
  vor jeder Runde wird nur die Spielfläche neu gelegt), Deko-Tiere im Bauplan (Kühe auf der Almweide, ohne KI).
  Neubau eines Tischs: 640 859 Blöcke. Echter Test danach wieder 111/111, Bilder mit Client 26.3 (nur lokal).
- Geprüft: 37 Unit-Tests (Brett, Spielregeln beider Spielarten, Felder, Stand, Laden, Gegenstände, Duell, Runden,
  Gehen, Minispiel-Taler, Warteschlange mit Partys, Abstimmung, Wertung der Minispiele, Texte samt Prüfung, dass jeder
  Schlüssel im Quelltext einen Text hat). Gegen Paper-API 26.3 gebaut, übersetzt auch gegen 26.1.
- Echter Test (`test/run.sh`, Paper 26.1.2 + ViaVersion/ViaBackwards 5.12.2, offline, Port 25770, vier Test-Spieler
  mit mineflayer): **111/111** in drei Partien – Festplatz (Knöpfe, Sprache, Party bringt Bert mit, Abstimmung,
  Countdown), Edelweiß-Jagd auf Schloss Weißenstein (Edelweiß-Stand, Doppelwürfel, Krämerladen, Würfelduell,
  Platztausch, Würfeln nach Zeitablauf, Zuschauen, Kuhglocke, Schäfchen zählen, Siegerehrung, Netzwerk-Statistik,
  /stats), Gipfelrennen auf dem Almsommer zu viert (Bergsprint, Bröckelboden, Dora geht mittendrin, Anna erreicht das
  Gipfelkreuz), Edelweiß-Jagd mit 8 Runden im Goldstollen (Talerregen, Pfeilhagel, Heiße Brezn, Trachtenfarben,
  Gipfelkönig, Holzhacker, Rempelei, Scheibenschießen, /top). Kein Fehler im Server-Log außer Mojang-429 für
  Spielerköpfe im Offline-Test.
- Bilder mit echtem Client 26.3 (über ViaVersion, nur lokal): Festplatz, alle drei Bretter, mehrere Minispiel-Plätze,
  Partie mit Seitenleiste als Zuschauer.
- Nicht echt geprüft: Bedrock (Geyser) – Menüs und Antippen sind dafür gebaut; echte Spieler (Steuerung,
  Gleichgewicht der Minispiele, Tempo); Netzwerk mit echtem Hub; Paper 26.3 als Server (nur gebaut); mehr als ein
  Tisch gleichzeitig.

- **Mehr Minispiele, Paket 1** (16.11., Branch `claude/mehr-minispiele`): Lawinenhang (Rennen den Schneehang hinauf,
  Schneeschichten = ohne Springen begehbar, rollende Schneebrocken als Block-Anzeigen schieben 6 Blöcke zurück),
  Maibaumkraxeln (12 Kletterspuren aus Leitern und Ranken, Stücke brechen nach Staub-Warnung weg und wachsen nach),
  Eisstockschießen (alle werfen gleichzeitig, Kraftbalken in der Aktionsleiste, eigene Physik `mini/Eisbahn` mit
  Zusammenstößen, Ringe 5/3/2/1), Murmeltier-Klopfen (Kaninchen als Murmeltiere, golden = 3, Steinbrocken = −1 über
  Klickflächen). Neue Wertungsart **Rennen** in `Scoring` (Ankunft, sonst Strecke; Bergsprint nutzt sie jetzt,
  Ergebniszeile „im Ziel nach 4,5 s“ / „46 Blöcke weit“). Bau-Version 3. 48 Unit-Tests, echt 135/136 (der eine Fehlschlag:
  Trachtenfarben-Prüfung war zu streng – Carl stand zufällig richtig; Prüfung angepasst).
- **Paket 2** (16.11.): Goldwaschen (Kies-/Sandhaufen auswaschen, Goldkorn 1 / Goldklumpen 3, Glitzern als Hinweis),
  Schuhplattler (Rhythmus im Dreivierteltakt mit eigener Melodie aus Notenblock-Klängen, Ansage einen Takt vorher,
  Bewertung in `mini/Takt`: genau ±2 Ticks = 2, ±6 = 1, falsche Bewegung = 0), Brückenbau (eigene Spur je Person,
  echtes Bretter-Setzen nur auf Brückenhöhe, Fallen = zurück an den Start), Schneeballschlacht (Schneewehen anklicken,
  Treffer mit Rückstoß, danach 1,5 s geschützt). Gefunden beim echten Test: ein abgefangener Linksklick verhindert im
  Überlebensmodus das Abbau-Ereignis – Klick-Minispiele werten den Linksklick jetzt selbst aus. Bau-Version 4.
  54 Unit-Tests; echt Partie 5 alle vier Minispiele (Goldwaschen nach der Korrektur 6/6).
- **Paket 3** (16.11.): Schlittenrennen (echte Holzboote auf Packeis/Blaueis – gehen auf Java jeder Version und
  Bedrock gleich; Oval mit sechs Toren, Wertung und Abkürzungsschutz in `mini/Rennbahn`; wer herausfällt, sitzt nach
  einer Sekunde in einem neuen Boot), Enzian-Suche (feste Almwiese `mini/EnzianWiese` mit Verstecken neben hohem Gras,
  Felsen, Büschen; Kornblume als blauer Enzian, sonst nichts Blaues), Musterweber (16 Webrahmen an vier Wänden, Muster
  4×4 in 2/3/4 Farben, Regeln in `mini/Webmuster`), Föhnsturm (acht Windrichtungen, Schutz hinter Felsen und Ducken in
  `mini/Wind`). Bau-Version 5. 63 Unit-Tests, echt Partie 6: 23/23.
- **Paket 4: Minispiel-Marathon** (16.11.): neue Spielart `Mode.MARATHON` (6/10/15 Minispiele, `minispiel-marathon`
  in der config.yml), ohne Brett, 2–16 Leute; Platz-Punkte in `game/Marathon` (10, 7, 5, 4, 3, 2, 1, alle weiteren 1),
  Reihenfolge gemischt ohne Wiederholung, Minispiele mit 8 Plätzen (`MiniType.maxPlayers`: Eisstockschießen,
  Brückenbau) fallen bei mehr Leuten aus; Bergsprint und Lawinenhang stellen bis 16 in zwei Reihen auf. Abstimmung:
  Menü „Welche Spielart?“ jetzt je Spielart eine halbe Zeile (Platz für alle kommenden Spielarten); Festplatz nimmt 16
  mit, wenn der Marathon in der Abstimmung vorn liegt. Zwischenstand im Chat und in der Seitenleiste, Siegerehrung,
  Statistik je Spielart (`spielarten.<id>` in der Spielerdatei, `/stats`, Netzwerk `games_marathon`, `wins_marathon`,
  `marathon_points_best`; `games_`/`wins_` jetzt für alle Spielarten). 70 Unit-Tests, echt Partie 7: 15/15.
  Gesamttest Partien 1–7: 194/197 – Lawinenhang: Bert wurde oben noch von einem Brocken getroffen, bevor er als
  angekommen zählte (behoben: erst Ankunft, dann Brocken; oben auf dem Plateau keine Treffer mehr, danach 8/8);
  Trachtenfarben (2×): der Server hing 10 s (Watchdog, Sandbox überlastet durch andere Server), kein Fehler im Spiel.
  Nicht echt geprüft: Runden mit mehr als vier Leuten (Speicher der Sandbox).
- Bilder der zwölf neuen Plätze mit echtem Client 26.3, je „spiel“ (laufendes Minispiel) und „weit“ (Kulisse), nur lokal
  unter `~/mm-cache/bilder-party-mehr/` (24 Bilder). Der Testserver wurde zweimal vom Speicherwächter der Sandbox
  beendet (andere Server liefen daneben) – das Schuhplattler-Bild „spiel“ steht hinter einem Zeltpfosten.

## Offen

- Philip: Bilder ansehen, Spielbretter und Minispiele ausprobieren; Go für `main` (dann erstes Release v0.1.0).
- Einstellungen (Rundenzahlen, Zeiten, Regeln, Minispiel-Auswahl) im Panel des Netzwerks (Einstellungs-Katalog) –
  gehört ins Netzwerk-Repo; Lobby-Figur „Morni Party“ verbindet über `MODE_REPOS`, sobald es ein Release gibt.
- Ideen: Brettfiguren als eigene Modelle statt der Spieler selbst, Bedrock-Formulare (Floodgate) statt Truhen.
