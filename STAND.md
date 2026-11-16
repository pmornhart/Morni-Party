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

## Offen

- Philip: Bilder ansehen, Spielbretter und Minispiele ausprobieren; Go für `main` (dann erstes Release v0.1.0).
- Einstellungen (Rundenzahlen, Zeiten, Regeln, Minispiel-Auswahl) im Panel des Netzwerks (Einstellungs-Katalog) –
  gehört ins Netzwerk-Repo; Lobby-Figur „Morni Party“ verbindet über `MODE_REPOS`, sobald es ein Release gibt.
- Ideen: Brettfiguren als eigene Modelle statt der Spieler selbst, Bedrock-Formulare (Floodgate) statt Truhen.
