# Stand – Morni Party

**Was es ist:** Der Brettspiel-Modus des Netzwerks morni.net (Paper-Plugin `MorniParty`, Java 25, Paper 26.x).
Entscheidungen von Philip (14.11.2026): Brettspiel für 2–8, würfeln, Taler sammeln, Edelweiß (Siegwährung) am
wandernden Stand kaufen, Ereignisfelder (Glück, Pech, Duell, Laden, Gegenstand), Gegenstände (u. a. Doppelwürfel,
Tausch, Talerklau, Schild), nach jeder Runde ein Minispiel; Spielarten Gipfelrennen und Edelweiß-Jagd (10/15/20
Runden); drei Bretter Almsommer am Morni-Kogel, Schloss Weißenstein, Goldstollen; zwölf Minispiele. Vorbild nur die
Mechanik großer Party-Spiele – eigene Namen und eigene Optik. Seit dem 16.11. (Branch `claude/mehr-minispiele`): neun
Spielarten, sechs Bretter, 26 Minispiele und der Tatzelwurm, Seiten nach Feldfarben (2 gegen 2, 1 gegen 3, Duell) und
Auszeichnungen am Ende.

## Aufbau

- `src/main/java/net/morni/party/`: `PartyPlugin` (Start, Welt, Aufgaben), `Settings` (config.yml), `Texts` (de/en),
  `Ui`, `Items`, `Commands`.
- `board/` (ohne Minecraft): `BoardId`, `FieldType`, `Field`, `BoardLayout` (Spiralweg mit 36 Feldern, Höhe des Geländes).
- `game/` (ohne Minecraft): `Engine` (alle Spielregeln), `PartyPlayer`, `Purse` (Beutel, in der Seilschaft geteilt),
  `Rules`, `Mode` (neun Spielarten), `GameKind`, `ItemType`, `Luck`, `Ride` (Fahrgeschäfte), `WaitQueue` (Partys
  bleiben zusammen), `Votes`, `Marathon`, `Teams` (Seiten nach Feldfarben), `Seilschaft`, `Huette`, `Bracket`
  (Turnierbaum), `Awards` (Auszeichnungen).
- `play/`: `Lobby` (Festplatz, Warteschlange, Abstimmung, Countdown, Zuschauen, Statistik-Menü), `Game` (Partie: Züge,
  Menüs, Minispiele, Ende), `Games`, `Sidebar`.
- `mini/`: `MiniType` (27 Plätze: 24 Minispiele für alle, Tauziehen, Gamsjagd, Tatzelwurm), `MiniRun` (Ablauf, Helfer,
  Seiten, Koop), `Scoring` (Wertung inkl. Seiten, ohne Minecraft), je Minispiel eine Klasse, reine Regeln in eigenen
  Klassen (`Seil`, `GamsRevier`, `WurmRegeln`, `Eisbahn`, `Takt`, `Rennbahn`, `EnzianWiese`, `Webmuster`, `Wind`).
- `world/`: `Plan`, `Decor`, `VoidGenerator`, `Places` (wo was steht), `HubPlan`, `BoardPlans` (+ `BoardPlans2` für
  Königssee, Wiesn, Wintersonnwend), `HubSigns`,
  `Construction` (baut nur bei neuer Bau-Version oder anderer Zahl Tische).
- `data/`: `PlayerStats`, `Store` (YAML im Hintergrund, Rangliste). `net/`: `Network`, `Connected`, `Standalone`.
- `src/api/java/net/morni/api/MorniNet.java`: Kopie der Netzwerk-Schnittstelle (nicht in der Jar).
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

- **Paket 5: Team- und Duell-Minispiele** (16.11.): Bei genau vier Leuten entscheidet die Farbe des zuletzt
  betretenen Felds (blau: Taler, Glück, Laden, Fundstück; rot: Verlust, Pech, Duell, Lawine) über die Spielweise des
  Minispiels – alle gleich: jeder gegen jeden, 2:2: zwei gegen zwei, 3:1: einer gegen drei (`game/Teams`, rein).
  Teamwertung in `Scoring.teamOutcome` (Punkte-Summe je Seite, Punkte des Einzelnen zählen vierfach; Überleben/Rennen:
  bester Platz, gleich = nächstbester), Taler in `Engine.awardTeams` (Sieger je 8, Einzelner 18, Gleichstand alle 3,
  alles in der config.yml). Das **Duellfeld** startet ein Duell-Minispiel zu zweit statt des Würfelduells
  (`Engine.settleDuel`, Schild schützt; die anderen bleiben auf dem Brett). Zwei neue Minispiele mit eigenen Plätzen:
  **Tauziehen** (2 gegen 2 und Duell, Regeln in `mini/Seil`: jeder Klick zieht, höchstens alle 2 Ticks, geteilt durch
  die Größe der Seite; Verlierer landen im Schlamm; Festwiese im Talkessel mit Tribünen, Wimpelketten,
  Schiedsrichterstuhl) und **Gamsjagd** (einer gegen drei: der Jäger ist etwas schneller und startet drei Sekunden
  später; Gamsrevier `mini/GamsRevier` mit Felsbuckeln, die man immer erklettern kann, Latschen, Zirben, Hochsitz).
  Mützen in der Farbe der Seite, Seiten-Punkte in der Aktionsleiste, Schneeballschlacht zählt die eigene Seite nicht.
  Minispiele je Spielweise: 2:2 Tauziehen, Schneeballschlacht, Talerregen, Holzhacker, Goldwaschen, Enzian-Suche; 1:3
  Gamsjagd, Murmeltier, Scheibenschießen, Goldwaschen; Duell Tauziehen, Scheibenschießen, Holzhacker, Bergsprint,
  Schneeballschlacht, Rempelei, Murmeltier. `/mparty seiten auto|frei|team|einzel`. Echt: Partie 8 20/20,
  Partie 1 mit Duell-Minispiel 94/94 (zusammen mit 8).
- **Paket 6: Seilschaft** (16.11.): Edelweiß-Jagd zu zweit (10/15 Runden, gerade Zahl, mindestens vier): ein
  gemeinsamer Beutel (`game/Purse`), Partys aus zwei sind eine Seilschaft, größere Partys werden geteilt
  (`game/Seilschaft`), Züge abwechselnd A1, B1, A2, B2, zu viert jedes Minispiel 2 gegen 2, kein Talerklau und kein Duell
  gegen den Partner, Seitenleiste mit einer Zeile je Seilschaft, Sieger „Anna & Bert“. Festplatz: fehlen für die
  gewünschte Spielart noch Leute, wartet er höchstens zweimal einen Countdown länger. Echt: Partie 9 alle grün.
- **Paket 7: Schnelle Partie und Bergduell** (16.11.): Schnelle Partie (5 Runden, 20 Start-Taler, Edelweiß 15,
  Minispiele × 0,75) und Bergduell (genau zwei, 8 Runden, jedes Minispiel ein Duell, Sieger 8 Taler). Echt: Partien 10
  und 11 alle grün.
- **Paket 8: Hüttenaufstieg** (16.11.): ohne Brett, 3 ❤ für alle; nach jedem Minispiel verliert die untere Hälfte
  ein Leben (`game/Huette.losers`, geteilter Platz zählt nach dem besten Rang, alle gleich = niemand), wer keins mehr
  hat, schaut im Zuschauermodus zu, wer zuletzt übrig ist, gewinnt; Herzen in Ergebnis, Seitenleiste und
  Siegerehrung. Echt: Partie 12 alle grün.
- **Paket 9: Gipfel-Turnier** (16.11.): K.-o.-Turnier aus Duell-Minispielen bis 16 (`game/Bracket`: Freilose so
  verteilt, dass niemand zweimal eins bekommt, Aufgeben = kampflos weiter), Unentschieden = neues Duell, beim zweiten
  das Los; Turnierbaum als Tafel vor den Zuschauern, eine je Sprache (nur die eigene ist sichtbar). Echt: Partie 13
  alle grün (Unentschieden, neues Duell, Halbfinale, Finale, Siegerehrung „raus im Halbfinale“).
- **Paket 10: Der Tatzelwurm** (16.11.): Koop ohne Brett, alle gegen einen Wurm mit Katzenkopf aus Block-Anzeigen in
  einer Felsenklamm (eigener Platz), drei Abschnitte (Regeln in `mini/WurmRegeln`): aus den sechs Wurmlöchern brechen
  (Kopf treffen, Felsbrocken mit Schatten-Warnung), Jagd im Kreis (nur leuchtende Schuppen verwundbar, Kopf stößt um),
  Zorn in der Mitte (Bodenwellen überspringen, Kopfschlag auf roten Kreis, danach liegt der Kopf harmlos am Boden).
  Benommen statt ausscheiden, gemeinsame Wertung mit bis zu drei Sternen, `/mparty wurm <leben>` zum Testen.
  Echt: Partie 14 14/14 (Kopf- und Schuppentreffer, Felsbrocken machen benommen, alle drei Abschnitte, Sieg, Sterne,
  Statistik). Gefunden: mineflayer kennt die Lage teleportierter Interaktions-Wesen nicht – der Test zielt über die
  Trefferflächen aus `/mparty zustand` (`targets`); `Interaction#setResponsive(true)`, Kopf größer.
- **Paket 11: drei neue Bretter** (16.11.): Königssee (Holzstege auf Pfählen über dem Bergsee, Seerosen, Schilf,
  Bootshaus, Ruderboote, Felswände, Kapelle mit roten Zwiebeltürmen auf der Insel; **Fähre** in gerader Linie
  mindestens 6 Felder voraus, Bojen zeigen die Route), Wiesn (Bierzelte, Riesenrad in der Mitte, Kettenkarussell,
  Achterbahn, Buden, Maibaum, Lichter; **Riesenrad** tauscht den Platz, **Fahrgeschäft**: Achterbahn, Autoscooter,
  Kettenkarussell, Losbude – `game/Ride`), Wintersonnwend (verschneiter Berg, Lawinenverbau, Skihütte, Iglu,
  gefrorener See, großes Sonnwendfeuer als Kegel-Holzstoß; **Eis** rutscht bis ans Ende, **Lawine** schiebt dich und
  alle bis 5 Felder vor dir 3 zurück). Regeln in `board/BoardLayout` (Sonderfelder, Fähre, Eis) und `game/Engine`.
  Bau-Version 8. Echt: Partie 15 12/12 (Fähre 5 → 11, Riesenrad-Tausch, Achterbahn, Eis bis ans Ende, Lawine für
  zwei).
- **Paket 12: Auszeichnungen** (16.11.): vor der Siegerehrung einer Brett-Partie mit Edelweiß je ein Edelweiß für
  Wanderer, Minispiel-Meister, Pechvogel und Duellkönig (`game/Awards`, Gleichstand: alle Besten), in der config.yml
  abschaltbar (`auszeichnungen`). Echt: Partie 1.
- **Feinschliff** (16.11.): Einzahl und Mehrzahl richtig – „1 Punkt“, „1 Feld weit“, „1 Sieg“, „1 Partie“, englisch
  „+1 coin“ – in Ergebniszeilen, Zwischenstand, Siegerehrung, Bestenliste, `/stats`, Auszeichnungen, Würfel-Titel und
  Glück/Pech (`Texts.count` + MiniMessage-Auswahl `<points_pl:'0#Punkte|1#Punkt|1<Punkte'/>`, geprüft in `TextsTest`).
  config.yml mit Abschnitt `huettenaufstieg` und Erklärungen; Duellfelder im echten Test umgangen, wo ein
  festgelegtes Minispiel kommen soll (`safeDie`).
- Gesamttest (16.11. nachmittags, Paper 26.1.2, vier Test-Spieler): Partien 2–11, 13 und 15 **203/204**, Server-Log
  ohne Fehler; der eine Fehlschlag war eine zu früh geprüfte Siegerehrung der Seilschaft (erst kommen die
  Auszeichnungen – der Test wartet jetzt). Partien 1, 12 und 14 davor einzeln grün. Gefunden: seit das Duellfeld ein
  Minispiel startet, verschob ein Duell im Test das festgelegte Minispiel der Runde – die Würfel im Test meiden
  Duellfelder jetzt (`safeDie`); die Statistik-Datei wird relativ zum Stand vor Partie 2 geprüft.
- Unit-Tests: 129 (u. a. Teams, Seil, Gamsrevier, Seilschaft, Modi, Hüttenaufstieg, Turnierbaum, Tatzelwurm-Regeln,
  neue Bretter, Auszeichnungen, Einzahl/Mehrzahl).
- **Umbenannt in morni.net (16.11.2026)** (Philip): Paket `net.morni.party` (vorher unter `de.`), Netzwerk-Schnittstelle
  `net.morni.api.MorniNet` als byte-gleiche Kopie, `softdepend: [MorniNet]`, Plugin sucht das Netzwerk-Plugin
  „MorniNet“, Verwaltungsrecht `morninet.party.admin`, Texte „morni.net“ (Seitenleiste, Zurück in die Lobby),
  `groupId` `net.morni`, CI prüft `net/morni/api/`. Test-Netzwerk heißt „MorniNet“ (`plugins/MorniNet/`, Jar
  `MorniNet-FakeNetwork.jar`; `run.sh` entfernt alte Test-Jars, damit nie zwei Netzwerk-Plugins laden). Spielerdaten
  und Einstellungen liegen weiter in `plugins/MorniParty/` – nichts muss umziehen. Version bleibt 0.1.0 (noch nicht
  veröffentlicht). Bilder der neuen Plätze und Bretter mit echtem Client 26.3 nur lokal unter
  `~/mm-cache/bilder-party-mehr/`.

## Offen

- Philip: Bilder ansehen, Spielbretter und Minispiele ausprobieren; Go für `main` (dann erstes Release v0.1.0).
- Einstellungen (Rundenzahlen, Zeiten, Regeln, Minispiel-Auswahl) im Panel des Netzwerks (Einstellungs-Katalog) –
  gehört ins Netzwerk-Repo; Lobby-Figur „Morni Party“ verbindet über `MODE_REPOS`, sobald es ein Release gibt.
- Ideen: Brettfiguren als eigene Modelle statt der Spieler selbst, Bedrock-Formulare (Floodgate) statt Truhen.
