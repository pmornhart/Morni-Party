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
