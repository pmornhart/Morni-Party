# Morni Party

@STAND.md

## Regeln für dieses Projekt

- Repo ist **öffentlich**: keine Geheimnisse, Tokens oder privaten Daten committen (Geheimes nur in `.env`, nie im Repo;
  auch keine Server-Ordner, Welten, Spielerdaten oder RCON-Passwörter aus Tests).
- Texte im Spiel auf Deutsch (Standard) und Englisch, freundlich, keine Fachbegriffe ohne Erklärung. Alle Texte stehen
  in `Texts.java` (MiniMessage, beide Sprachen; `TextsTest` prüft Platzhalter, Zeichen und dass jeder Schlüssel aus dem
  Quelltext einen Text hat).
- **„Verfasst mit Claude 🤖“** als letzte Zeile in Commit-Messages (durch eine Leerzeile getrennt), in PRs, Issues und
  Release-Notes; einmal als Fußzeile im README.
- Commits mit Stichpunkten `- Neu:` / `- Behoben:` / `- Verbessert:` / `- Entfernt:`.
- Keine fremden Marken, Figuren, Namen, Texte oder Grafiken (nichts von Mojang, Nintendo, Hypixel, Cytooxien oder anderen
  Servern): nur Spielmechanik als Vorbild, eigene Namen, eigene Bretter, eigene Minispiele.
- Die Netzwerk-Schnittstelle `src/api/java/de/moinmorni/api/MoinMorni.java` ist eine Kopie aus `pmornhart/moinmorni.net`
  (`plugins/moinmorni-network/src/main/java/de/moinmorni/api/MoinMorni.java`) und muss **genau gleich** bleiben. Sie wird
  nur mitübersetzt und kommt **nicht** in die Jar (die echten Klassen bringt das Netzwerk-Plugin mit).
- Morni Party muss auch ohne Netzwerk laufen (`softdepend: [MoinMorni]`; nur `net/Connected` kennt die Netzwerk-Klassen).
- Paper 26.x, Java 25. Der Code muss auch gegen Paper 26.1 übersetzen (`mvn -Ppaper-26.1 compile`, läuft in der CI).
- Spielregeln gehören in `game/Engine` (ohne Minecraft, mit Tests), Anzeige und Ablauf in `play/Game`.
- Neue Version: `<version>` in `pom.xml` erhöhen und `docs/releases/v<version>.md` anlegen; ein Push auf `main` baut das
  Release mit `Morni-Party.jar` – das Netzwerk holt es sich von dort.
- Vor jeder Veröffentlichung (Merge nach `main`) Philips Go abwarten.
- Ändern sich Festplatz, Spielbretter oder Minispiel-Plätze: `Construction.VERSION` erhöhen – dann baut der Server beim
  nächsten Start neu.
- Nach jeder sichtbaren Änderung Bilder wie im echten Minecraft (echte Client-Texturen nur lokal, nie ins Repo).
- „Alles im Panel“: jede Einstellung, die per Befehl geht, muss auch im Panel gehen. Darum hat Morni Party keine
  Einstellungs-Befehle, nur die `config.yml`; die Anbindung an den Einstellungs-Katalog gehört ins Netzwerk-Repo.
  (`/mparty` ist nur zum Verwalten und Testen.)
- Echter Test nach jeder Änderung am Spielablauf: `test/run.sh` (siehe README).
- `STAND.md` nach jedem größeren Schritt aktualisieren.
