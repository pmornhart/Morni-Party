/**
 * Echter Test von Morni Party mit Test-Spielern (mineflayer) auf Paper 26.1.2 – gestartet von test/run.sh.
 *   node party-test.cjs <host> <port> <rcon-host:port> <rcon-passwort-datei> <server-ordner>
 *
 * Spieler: Anna (deutsch, Party-Leiterin mit Bert), Bert (englisch), Carl, Dora (deutsch). Sprache und Party kommen
 * vom Test-Netzwerk (test/fake-network), das auch Statistik und Zustand in calls.log mitschreibt. Würfel, Taler und
 * Felder legt der Test teils per Verwaltung fest (/mparty), damit jedes Feld und jeder Gegenstand einmal drankommt.
 *
 *   Partie 1: Edelweiß-Jagd, 2 Runden, Schloss Weißenstein (Minispiele Kuhglocke, Schäfchen zählen)
 *   Partie 2: Gipfelrennen, Almsommer, zu viert (Bergsprint, Bröckelboden), Dora geht mittendrin
 *   Partie 3: Edelweiß-Jagd, 8 Runden, Goldstollen – die restlichen acht Minispiele
 *   Partie 4: neue Minispiele (Paket 1): Lawinenhang, Maibaumkraxeln, Eisstockschießen, Murmeltier-Klopfen
 *   Partie 5: Paket 2 – Goldwaschen, Schuhplattler, Brückenbau, Schneeballschlacht
 *   Partie 6: Paket 3 – Schlittenrennen, Enzian-Suche, Musterweber, Föhnsturm
 *   Partie 7: Minispiel-Marathon mit drei Minispielen zu viert
 *   Partie 8: Team- und Duell-Minispiele zu viert (Seiten nach Feldfarben, Tauziehen, Gamsjagd, Gleichstand)
 *   Partie 9: Seilschaft zu viert (Party als Seilschaft, gemeinsame Taler, 2 gegen 2)
 *   Partie 10: Schnelle Partie zu dritt · Partie 11: Bergduell zu zweit
 *   Partie 12: Hüttenaufstieg (Leben) zu viert · Partie 13: Gipfel-Turnier zu viert
 *   Partie 14: Der Tatzelwurm zu zweit · Partie 15: die drei neuen Bretter zu dritt
 *
 * Verfasst mit Claude 🤖
 */
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const mineflayer = require('mineflayer');
const { rcon } = require('./rcon.cjs');

const [HOST = '127.0.0.1', PORT = '25770', RCON = '127.0.0.1:25870', PASS_FILE, SERVER] = process.argv.slice(2);
const PASS = fs.readFileSync(PASS_FILE, 'utf8').trim();
const VERSION = process.env.MC_VERSION ?? '26.1';
const PARTS = (process.env.PARTS ?? '1,2,3').split(',').map(Number);
const HUB = { x: 0.5, y: 101, z: 14.5 };

const results = [];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
function check(name, ok, detail = '') {
  results.push({ name, ok: Boolean(ok) });
  console.log(`${ok ? '✓' : '✗'} ${name}${detail ? ` – ${detail}` : ''}`);
}

/** Text aus Chat-Komponenten (String, JSON oder NBT) zusammensetzen. */
function plain(v) {
  if (v == null) return '';
  if (typeof v === 'string') {
    if (v.startsWith('{') || v.startsWith('[')) {
      try {
        return plain(JSON.parse(v));
      } catch {
        return v;
      }
    }
    return v;
  }
  if (Array.isArray(v)) return v.map(plain).join('');
  if (typeof v !== 'object') return String(v);
  if (v.type === 'compound') return plain(v.value);
  if (v.type === 'list') return plain(v.value?.value ?? []);
  if (v.type === 'string') return v.value;
  let s = '';
  if ('text' in v) s += plain(v.text);
  if ('' in v) s += plain(v['']);
  if ('translate' in v) s += plain(v.translate);
  if ('extra' in v) s += plain(v.extra);
  return s;
}

function offlineUuid(name) {
  const h = crypto.createHash('md5').update(`OfflinePlayer:${name}`, 'utf8').digest();
  h[6] = (h[6] & 0x0f) | 0x30;
  h[8] = (h[8] & 0x3f) | 0x80;
  const x = h.toString('hex');
  return `${x.slice(0, 8)}-${x.slice(8, 12)}-${x.slice(12, 16)}-${x.slice(16, 20)}-${x.slice(20)}`;
}

const cmd = (...commands) => rcon(RCON, PASS, commands);
const readFile = (file) => (fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '');
const calls = () =>
  readFile(path.join(SERVER, 'plugins/MoinMorni/calls.log'))
    .split('\n')
    .filter(Boolean)
    .map((l) => JSON.parse(l));
const statsFile = (name) => readFile(path.join(SERVER, 'plugins/MorniParty/spieler', `${offlineUuid(name)}.yml`));

async function state() {
  const [out] = await cmd('mparty zustand');
  try {
    return JSON.parse(out.replace(/§./g, ''));
  } catch {
    return { games: [] };
  }
}
async function game() {
  return (await state()).games[0] ?? null;
}

async function waitFor(cond, ms = 5000, step = 150) {
  const start = Date.now();
  while (Date.now() - start < ms) {
    if (await cond()) return true;
    await sleep(step);
  }
  return Boolean(await cond());
}

/** Plätze im Menü „Welche Spielart?“ (je Spielart eine halbe Zeile, Testwerte: Jagd 2/8, Marathon 3/6). */
const MODE = { gipfel: 0, jagd2: 5, jagd8: 6, marathon3: 9, marathon6: 10, seil3: 14, schnell: 18, duell: 23, huette: 27, turnier: 32, wurm: 36 };

const bots = {};
let lastJoin = 0;
async function join(name) {
  const wait = lastJoin + 1500 - Date.now();
  if (wait > 0) await sleep(wait);
  lastJoin = Date.now();
  const bot = mineflayer.createBot({ host: HOST, port: Number(PORT), username: name, version: VERSION, auth: 'offline' });
  bot.chats = [];
  bot.bars = [];
  bot.titles = [];
  bot.windows = [];
  bot.policy = null; // (bot, window, title) => Promise
  bot.on('messagestr', (m, position) => (position === 'game_info' ? bot.bars : bot.chats).push(m));
  bot._client.on('action_bar', (p) => bot.bars.push(plain(p.text)));
  bot._client.on('set_title_text', (p) => bot.titles.push(plain(p.text)));
  bot._client.on('set_title_subtitle', (p) => bot.titles.push(plain(p.text)));
  bot.on('kicked', (r) => {
    bot.kickReason = plain(r);
  });
  bot.on('error', (e) => console.log(`  (${name}: ${e.message})`));
  bot.on('windowOpen', (w) => {
    const title = plain(w.title);
    bot.windows.push(title);
    if (bot.policy) {
      Promise.resolve(bot.policy(bot, w, title)).catch((e) => console.log(`  (${name} Menü: ${e.message})`));
    }
  });
  await new Promise((resolve, reject) => {
    bot.once('spawn', resolve);
    bot.once('kicked', (r) => reject(new Error(`${name} gekickt: ${plain(r)}`)));
    setTimeout(() => reject(new Error(`${name}: kein Spawn`)), 20000);
  });
  await sleep(1500);
  bots[name] = bot;
  return bot;
}

const slot = (bot, i) => bot.inventory.slots[36 + i];
const near = (p, q, d) => Math.abs(p.x - q.x) < d && Math.abs(p.y - q.y) < d && Math.abs(p.z - q.z) < d;
const inHub = (bot) => near(bot.entity.position, HUB, 4) && bot.game.gameMode === 'adventure';
const sees = (bot, re) => bot.chats.some((c) => re.test(c));
const sawTitle = (bot, re) => bot.titles.some((t) => re.test(t));
const clear = (...bs) =>
  bs.forEach((b) => {
    b.chats.length = 0;
    b.bars.length = 0;
    b.titles.length = 0;
    b.windows.length = 0;
  });

async function use(bot, hotbar) {
  bot.setQuickBarSlot(hotbar);
  await sleep(250);
  bot.activateItem();
  await sleep(150);
  bot.deactivateItem();
}

async function openWithItem(bot, hotbar) {
  const opened = new Promise((resolve) => {
    bot.once('windowOpen', resolve);
    setTimeout(() => resolve(null), 4000);
  });
  await use(bot, hotbar);
  return opened;
}

/** Klick in ein Menü. Schließt der Klick das Menü, wartet mineflayer vergeblich auf eine Antwort – darum begrenzt. */
const click = async (bot, s) => {
  await Promise.race([bot.clickWindow(s, 0, 0).catch(() => {}), sleep(1500)]);
  await sleep(300);
};

function nextWindow(bot) {
  return new Promise((resolve) => {
    bot.once('windowOpen', resolve);
    setTimeout(() => resolve(null), 4000);
  });
}

/** Standard-Verhalten in Menüs der Partie: Edelweiß kaufen, im Laden das Erste kaufen und gehen, Duell gegen den Ersten. */
async function defaultPolicy(bot, w, title) {
  await sleep(400);
  if (/Edelweiß-Stand|Edelweiss stand/.test(title)) {
    await click(bot, bot.buyEdelweiss === false ? 15 : 11);
  } else if (/Krämerladen|Shop/.test(title)) {
    await click(bot, 10);
    await sleep(300);
    await click(bot, 22);
  } else if (/Gegen wen|Who do you duel/.test(title)) {
    await click(bot, 0);
  }
}

/** Wartet auf eine Phase (oder Bedingung) der Partie. */
async function phase(pred, ms = 30000) {
  let g = null;
  const ok = await waitFor(async () => {
    g = await game();
    return g && pred(g);
  }, ms, 300);
  return ok ? g : null;
}

/** Wer ist gerade dran (als Bot)? */
const currentBot = (g) => bots[g.current];
const pl = (g, name) => g.players.find((p) => p.name === name);
const fieldIndex = (g, letter, after = 0) => {
  for (let i = after + 1; i < g.fields.length - 1; i++) if (g.fields[i] === letter) return i;
  return -1;
};
/**
 * Würfelzahl für den, der dran ist (am liebsten {@code want}), mit der er nicht auf einem Duellfeld landet – ein Duell
 * bringt ein eigenes Minispiel und würde das festgelegte Minispiel der Runde verschieben.
 */
const safeDie = (g, want = 1) => {
  const p = pl(g, g.current);
  const goal = g.fields.length - 1;
  const land = (n) => {
    let pos = p.pos;
    for (let k = 0; k < n; k++) pos = pos === goal ? 1 : pos + 1;
    return pos;
  };
  for (const n of [want, 1, 2, 3, 4, 5, 6]) if (g.fields[land(n)] !== 'D') return n;
  return want;
};

/** Zug spielen: Würfel benutzen (Rechtsklick), warten bis der Zug weiter ist. */
async function playTurn(g, { roll = true } = {}) {
  const bot = currentBot(g);
  const turnOf = g.current;
  const round = g.round;
  if (roll && bot) {
    await waitFor(() => slot(bot, 0)?.name === 'white_concrete', 4000);
    await use(bot, 0);
  }
  return phase((x) => x.current !== turnOf || x.round !== round || x.phase.startsWith('mini') || x.phase === 'end', 45000);
}

// ------------------------------------------------------------------ Minispiel-Helfer

/** Kuhglocke: bei „JETZT!“ klicken; einer klickt in der ersten Runde zu früh. */
function kuhglocke(list, early) {
  let stop = false;
  const seen = new Map(list.map((b) => [b.username, 0]));
  const loop = async () => {
    let earlyDone = false;
    while (!stop) {
      for (const b of list) {
        const nows = b.titles.filter((t) => /JETZT|NOW/.test(t)).length;
        if (nows > seen.get(b.username)) {
          seen.set(b.username, nows);
          await sleep(100 + Math.random() * 300);
          b.setQuickBarSlot(0);
          b.activateItem();
          b.deactivateItem();
        }
        if (early && b === early && !earlyDone && b.titles.some((t) => /Warte/.test(t))) {
          earlyDone = true;
          b.setQuickBarSlot(0);
          b.activateItem();
          b.deactivateItem();
        }
      }
      await sleep(80);
    }
  };
  loop();
  return () => (stop = true);
}

/** Schäfchen zählen: Schafe in der Nähe zählen, im Menü die gezählte Zahl wählen. */
function schaefchen(list) {
  const counts = new Map();
  let stop = false;
  for (const b of list) {
    b.policy = async (bot, w, title) => {
      if (/Wie viele Schafe|How many sheep/.test(title)) {
        await sleep(300);
        const n = counts.get(bot.username) ?? 0;
        let best = 1;
        let diff = 99;
        for (let s = 0; s < 9; s++) {
          const it = w.slots[s];
          if (it && Math.abs(it.count - n) < diff) {
            diff = Math.abs(it.count - n);
            best = s;
          }
        }
        await click(bot, best);
      }
    };
  }
  const loop = async () => {
    while (!stop) {
      for (const b of list) {
        const sheep = Object.values(b.entities).filter((e) => e.name === 'sheep' && e.position.distanceTo(b.entity.position) < 30);
        if (sheep.length > 0) counts.set(b.username, Math.max(counts.get(b.username) ?? 0, sheep.length));
        else if (b.currentWindow == null) counts.set(b.username, 0);
      }
      await sleep(200);
    }
  };
  loop();
  return () => {
    stop = true;
    list.forEach((b) => (b.policy = defaultPolicy));
  };
}

/** Ergebniszeilen „1. Name (n Punkte) → +x Taler“ aus dem Chat. */
const resultLines = (bot) => bot.chats.filter((c) => /^\d+\. \S+ .*(Taler|coins?)/.test(c));

async function waitMini(id, ms = 60000) {
  return phase((x) => x.mini === id && x.phase === 'mini', ms);
}
async function waitMiniEnd(ms = 60000) {
  return phase((x) => x.phase !== 'mini' && x.phase !== 'mini_countdown' && x.phase !== 'mini_intro', ms);
}

// ------------------------------------------------------------------ Partie 1

async function part1() {
  console.log('\n— Partie 1: Edelweiß-Jagd (2 Runden) auf Schloss Weißenstein —');
  const { Anna: anna, Bert: bert, Carl: carl, Dora: dora } = bots;
  check('Ankunft auf dem Festplatz', inHub(anna), JSON.stringify(anna.entity.position));
  check('Schnellleiste: Farbe, Karte, Uhr, Fernrohr, Gold, Bett',
    slot(anna, 0)?.name === 'gray_dye' && slot(anna, 2)?.name === 'map' && slot(anna, 3)?.name === 'clock' &&
      slot(anna, 5)?.name === 'spyglass' && slot(anna, 7)?.name === 'gold_ingot' && slot(anna, 8)?.name === 'red_bed',
    [0, 2, 3, 5, 7, 8].map((i) => slot(anna, i)?.name).join(','));
  check('Deutsch: „Mitspielen“', plain(slot(anna, 0)?.customName).includes('Mitspielen'), plain(slot(anna, 0)?.customName));
  check('Englisch (Sprache vom Netzwerk): „Play“', plain(slot(bert, 0)?.customName).includes('Play'), plain(slot(bert, 0)?.customName));
  check('Begrüßung im Chat', sees(anna, /Willkommen bei Morni Party/));

  clear(anna, bert);
  await use(anna, 0);
  check('Anna spielt mit (Chat, grüne Farbe)', await waitFor(() => sees(anna, /Du spielst mit!/) && slot(anna, 0)?.name === 'lime_dye', 3000),
    anna.chats.join(' | '));
  check('Party: Bert kommt mit (englisch)', await waitFor(() => sees(bert, /Your party leader Anna brought you along/), 3000),
    bert.chats.join(' | '));
  let st = await state();
  check('Zwei warten', st.waiting === 2, JSON.stringify(st));

  let win = await openWithItem(anna, 2);
  check('Karte öffnet „Welches Spielbrett?“', win && plain(win.title).includes('Welches Spielbrett'), win && plain(win.title));
  check('Drei Bretter zur Wahl', win && [11, 13, 15].every((s) => win.slots[s]) &&
    plain(win.slots[13].customName).includes('Schloss Weißenstein'), win && plain(win.slots[13]?.customName));
  await click(anna, 13);
  check('Stimme für Schloss Weißenstein', await waitFor(() => sees(anna, /Deine Stimme: Schloss Weißenstein/), 2000));
  win = await openWithItem(bert, 2);
  check('Englisch: „Weissenstein Castle“', win && plain(win.slots[13]?.customName).includes('Weissenstein Castle'));
  await click(bert, 13);
  win = await openWithItem(anna, 3);
  check('Uhr öffnet „Welche Spielart?“ mit Gipfelrennen und Edelweiß-Jagd',
    win && plain(win.slots[MODE.gipfel]?.customName).includes('Gipfelrennen') && plain(win.slots[MODE.jagd2]?.customName).includes('2 Runden'),
    win && `${plain(win.slots[MODE.gipfel]?.customName)} / ${plain(win.slots[MODE.jagd2]?.customName)}`);
  check('Uhr: auch der Minispiel-Marathon steht zur Wahl', win && plain(win.slots[MODE.marathon3]?.customName).includes('Minispiel-Marathon'),
    win && plain(win.slots[MODE.marathon3]?.customName));
  await click(anna, MODE.jagd2);
  win = await openWithItem(bert, 3);
  await click(bert, MODE.jagd2);

  clear(carl);
  carl.chat('/mitspielen');
  check('Carl per /mitspielen', await waitFor(() => sees(carl, /Du spielst mit!/), 3000));
  check('Countdown läuft (Aktionsleiste)', await waitFor(() => carl.bars.some((b) => /Start in \d+ s/.test(b)), 4000),
    carl.bars.slice(-1)[0]);
  const g = await phase((x) => x.phase === 'turn', 30000);
  check('Partie startet: Schloss Weißenstein, Edelweiß-Jagd 2 Runden, drei Spieler',
    g && g.board === 'schloss' && g.mode === 'edelweiss-jagd-2' && g.players.length === 3, g && `${g.board} ${g.mode}`);
  check('Dora wartet nicht und bleibt auf dem Festplatz', inHub(dora));
  check('Alle am Spieltisch (weit weg vom Festplatz)', [anna, bert, carl].every((b) => Math.abs(b.entity.position.x) > 500),
    JSON.stringify(anna.entity.position));
  check('Titel mit Brett und Spielart', sawTitle(anna, /Schloss Weißenstein/) && sawTitle(bert, /Weissenstein Castle/));
  await cmd('mparty minispiel kuhglocke');
  check('Spielregel im Chat', sees(anna, /Nach 2 Runden gewinnt, wer die meisten hat/), anna.chats.find((c) => /Runden/.test(c)));
  check('Alle starten mit 10 Talern auf Feld 0', g.players.every((p) => p.taler === 10 && p.pos === 0));

  // ---- Zug 1: Edelweiß-Stand
  let cur = currentBot(g);
  const first = g.current;
  check('Wer dran ist, hat den Würfel', slot(cur, 0)?.name === 'white_concrete' && slot(cur, 1)?.name === 'bundle',
    `${first}: ${slot(cur, 0)?.name}`);
  const others = [anna, bert, carl].filter((b) => b !== cur);
  check('Die anderen haben keinen Würfel', others.every((b) => slot(b, 0)?.name !== 'white_concrete'));
  check('Seitenleiste ist da', cur.scoreboard?.sidebar != null || Object.keys(cur.scoreboards ?? {}).length > 0);
  await cmd(`mparty taler ${first} 30`, `mparty stand 4`, `mparty wuerfel ${first} 4`);
  clear(anna, bert, carl);
  await use(cur, 0);
  let after = await phase((x) => x.current !== first, 30000);
  check(`${first} würfelt die 4 (Chat)`, [anna, bert, carl].some((b) => b.chats.some((c) => new RegExp(`${first} (würfelt|rolls) 4`).test(c))));
  check('Edelweiß-Stand: Menü geht auf', cur.windows.some((t) => /Edelweiß-Stand|Edelweiss stand/.test(t)), cur.windows.join(','));
  check(`${first} kauft ein Edelweiß (20 Taler)`, after && pl(after, first).edelweiss === 1, after && JSON.stringify(pl(after, first)));
  check('Alle lesen es im Chat', sees(anna, new RegExp(`${first} kauft ein ✿ Edelweiß`)) || sees(anna, new RegExp(`${first} buys`)),
    anna.chats.filter((c) => /Edelwei/.test(c)).join(' | '));
  check('Der Stand zieht weiter', after && after.stand !== 4, after && String(after.stand));

  // ---- Zug 2: Doppelwürfel aus dem Rucksack
  const second = after.current;
  cur = currentBot(after);
  await cmd(`mparty gegenstand ${second} doppelwuerfel`);
  await sleep(300);
  win = await openWithItem(cur, 1);
  check('Rucksack öffnet sich', win && /Rucksack|backpack/.test(plain(win.title)), win && plain(win.title));
  const dSlot = win ? win.slots.findIndex((it, i) => i < 9 && it?.name === 'quartz_block') : -1;
  check('Doppelwürfel im Rucksack', dSlot >= 0, win && win.slots.slice(0, 9).map((it) => it?.name).join(','));
  clear(anna, bert, carl);
  await click(cur, dSlot);
  check('Doppelwürfel eingesetzt (Chat)', await waitFor(() => sees(anna, /(zwei Würfel|two dice)/), 2000), anna.chats.join(' | '));
  await use(cur, 0);
  after = await phase((x) => x.current !== second, 30000);
  check('Zwei Würfel (a + b = c)', [anna, bert, carl].some((b) => b.chats.some((c) => /\d \+ \d = \d+/.test(c))),
    anna.chats.filter((c) => /würfelt|rolls/.test(c)).join(' | '));

  // ---- Zug 3: Krämerladen
  const third = after.current;
  cur = currentBot(after);
  const shop = fieldIndex(after, 'L', 2);
  await cmd(`mparty feld ${third} ${shop - 2}`, `mparty wuerfel ${third} 2`, `mparty taler ${third} 20`);
  clear(anna, bert, carl);
  await use(cur, 0);
  const roundOver = await phase((x) => x.phase.startsWith('mini'), 30000);
  check('Krämerladen: Menü geht auf', cur.windows.some((t) => /Krämerladen|Shop/.test(t)), cur.windows.join(','));
  check(`${third} kauft einen Doppelwürfel (8 Taler)`, roundOver && pl(roundOver, third).items.includes('doppelwuerfel') &&
    pl(roundOver, third).taler === 12, roundOver && JSON.stringify(pl(roundOver, third)));

  // ---- Minispiel 1: Kuhglocke
  check('Nach der Runde: Minispiel Kuhglocke', roundOver && roundOver.mini === 'kuhglocke', roundOver?.mini);
  check('Titel „Minispiel!“', await waitFor(() => sawTitle(anna, /Minispiel/), 3000));
  const stopK = kuhglocke([anna, bert, carl], carl);
  await waitMini('kuhglocke', 15000);
  check('Kuhglocke: Glockenseil in der Hand', slot(anna, 0)?.name === 'lead', slot(anna, 0)?.name);
  const end1 = await waitMiniEnd(60000);
  stopK();
  check('Kuhglocke: „JETZT!“ gesehen', sawTitle(anna, /JETZT/) && sawTitle(bert, /NOW/));
  check('Kuhglocke: zu früh geklickt kostet einen Punkt', carl.bars.some((b) => /Zu früh/.test(b)), carl.bars.slice(-3).join(' | '));
  check('Kuhglocke: Schnellste im Chat', sees(anna, /war Nummer 1/), anna.chats.filter((c) => /Nummer/.test(c)).slice(0, 2).join(' | '));
  check('Ergebnis mit Punkten und Talern', resultLines(anna).length === 3, resultLines(anna).join(' | '));
  check('Englisches Ergebnis', bert.chats.some((c) => /Result: Cowbell/.test(c)) && resultLines(bert).some((l) => /coins?/.test(l)));
  check('Ein Minispiel-Sieg wird gezählt', end1 && end1.players.some((p) => p.minis === 1), end1 && JSON.stringify(end1.players.map((p) => p.minis)));
  const r2 = await phase((x) => x.phase === 'turn' && x.round === 2, 20000);
  check('Zurück aufs Brett, Runde 2', r2 && [anna, bert, carl].every((b) => Math.abs(b.entity.position.x - r2.boardAt[0]) < 60),
    r2 && JSON.stringify(anna.entity.position));
  check('Abenteuermodus auf dem Brett', anna.game.gameMode === 'adventure');

  // ---- Zuschauen
  clear(dora);
  dora.chat('/zuschauen Anna');
  check('Dora schaut zu (Zuschauermodus)', await waitFor(() => dora.game.gameMode === 'spectator', 3000), dora.game.gameMode);
  st = await state();
  check('Partie zählt einen Zuschauer', st.games[0]?.spectators === 1);

  // ---- Runde 2, Zug 1: Würfelduell
  let g2 = r2;
  const d1 = g2.current;
  const duel = fieldIndex(g2, 'D', 1);
  await cmd(`mparty feld ${d1} ${duel - 1}`, `mparty wuerfel ${d1} 1`, 'mparty minispiel tauziehen');
  clear(anna, bert, carl);
  const before2 = Object.fromEntries(g2.players.map((p) => [p.name, p.taler]));
  g2 = await playTurn(g2);
  check('Duellfeld: Menü „Wen forderst du heraus?“', currentBotByName(d1).windows.some((t) => /Wen forderst|Who do you challenge/.test(t)),
    currentBotByName(d1).windows.join(','));
  const dg = await waitMini('tauziehen', 30000);
  check('Duell-Minispiel: Tauziehen zu zweit', dg && dg.format === 'duel' && dg.miniPlayers === 2 && dg.sideA === d1, dg && `${dg.format} ${dg.miniPlayers} ${dg.sideA}`);
  const puller = currentBotByName(d1);
  let pulling = true;
  const pullJob = (async () => { while (pulling) { puller.swingArm(); await sleep(60); } })();
  await waitMiniEnd(40000);
  pulling = false;
  await pullJob;
  g2 = await phase((x) => x.phase === 'turn' && x.current !== d1, 30000);
  const opp = dg && g2 && g2.players.find((p) => p.name === dg.sideB);
  check('Duell im Chat: Sieger bekommt den Einsatz', sees(anna, new RegExp(`${d1} gewinnt das Duell und bekommt 5 Taler`)) || sees(anna, /Schild/),
    anna.chats.filter((c) => /Duell|duel|⚔/.test(c)).join(' | '));
  check('Duell: Taler wandern (Tauziehen gewonnen)', g2 && opp && pl(g2, d1).taler === before2[d1] + 5 && opp.taler === before2[opp.name] - 5,
    g2 && JSON.stringify(g2.players.map((p) => `${p.name}:${before2[p.name]}→${p.taler}`)));
  await cmd('mparty minispiel schaefchen'); // erst nach dem Duell (das hätte es sonst verbraucht)

  // ---- Runde 2, Zug 2: Platztausch
  const t1 = g2.current;
  const tb = currentBot(g2);
  await cmd(`mparty gegenstand ${t1} tausch`, `mparty feld ${t1} 1`);
  const target = g2.players.find((p) => p.name !== t1);
  await cmd(`mparty feld ${target.name} 20`);
  await sleep(300);
  await phase((x) => x.phase === 'turn' && x.current === t1, 5000);
  win = await openWithItem(tb, 1);
  if (!win) win = await openWithItem(tb, 1);
  // Der Platztausch (Enderperle) – vorher Gefundenes liegt davor im Rucksack
  const tSlot = win ? win.slots.findIndex((it, i) => i < 9 && it?.name === 'ender_pearl') : 2;
  const targetMenu = nextWindow(tb);
  await click(tb, tSlot);
  // Ziel-Menü: der Erste in der Liste
  const tw = await targetMenu;
  check('Platztausch: Menü „Wen trifft es?“', tw && /Wen trifft es|Who is it for/.test(plain(tw.title)), tw && plain(tw.title));
  const targetName = tw ? plain(tw.slots[0]?.customName) : '';
  await click(tb, 0);
  let gs = await game();
  check(`Platztausch: ${t1} steht jetzt auf dem Feld von ${targetName}`, gs && pl(gs, t1).pos === 20 && pl(gs, targetName)?.pos === 1,
    gs && JSON.stringify(gs.players.map((p) => `${p.name}:${p.pos}`)));
  await use(tb, 0);
  g2 = await phase((x) => x.current !== t1, 30000);

  // ---- Runde 2, Zug 3: nicht würfeln → das Spiel würfelt
  const lazy = currentBot(g2);
  clear(lazy);
  const g3 = await phase((x) => x.phase.startsWith('mini'), 40000);
  check('Wer nicht würfelt: das Spiel würfelt nach der Zeit', sees(lazy, /(Die Zeit ist um|Time's up)/), lazy.chats.join(' | '));

  // ---- Minispiel 2: Schäfchen zählen
  check('Minispiel Schäfchen zählen', g3 && g3.mini === 'schaefchen', g3?.mini);
  const stopS = schaefchen([anna, bert, carl]);
  check('Dora (Zuschauerin) ist mit beim Minispiel', await waitFor(() => Math.abs(dora.entity.position.x - (g3.miniAt?.[0] ?? 0)) < 60, 8000),
    JSON.stringify(dora.entity.position));
  const end2 = await waitMiniEnd(90000);
  stopS();
  check('Schäfchen: Menü „Wie viele Schafe?“ dreimal', anna.windows.filter((t) => /Wie viele Schafe/.test(t)).length === 3,
    anna.windows.join(','));
  check('Schäfchen: Auflösung im Chat', sees(anna, /Es waren \d+ Schafe/));
  check('Schäfchen: richtig gezählt gibt Punkte', resultLines(anna).some((l) => /\((3|4|5|6|7|9) Punkte\)/.test(l)), resultLines(anna).join(' | '));

  // ---- Ende
  const done = await phase((x) => x.phase === 'end', 20000);
  check('Nach Runde 2 ist Schluss', done != null);
  check('Auszeichnungen vor der Siegerehrung (de/en)', await waitFor(() => sees(anna, /Auszeichnungen – je ein Edelweiß extra/) &&
    sees(bert, /Awards – one extra edelweiss each/), 5000));
  check('Wanderer und Minispiel-Meister', await waitFor(() => sees(anna, /✿ Wanderer: .* \(\d+ Feld(er)? gelaufen\)/) &&
    sees(anna, /✿ Minispiel-Meister: .* \(\d+ Minispiele? gewonnen\)/), 12000), anna.chats.filter((c) => /✿ /.test(c)).join(' | '));
  check('Siegerehrung im Chat', await waitFor(() => sees(anna, /Die Partie ist vorbei/) && sees(bert, /The game is over/), 15000));
  const winnerLine = anna.chats.find((c) => /^1\. .*✿/.test(c));
  check('Platz 1 mit Edelweiß und Talern', winnerLine && /✿ \d+ ● \d+/.test(winnerLine), winnerLine);
  check('Titel Gewonnen!/Vorbei!', [anna, bert, carl].some((b) => sawTitle(b, /Gewonnen|You won/)) && [anna, bert, carl].some((b) => sawTitle(b, /Sieg|Winner/)));
  const log = calls().filter((c) => c.t === 'stats' && c.mode === 'party');
  check('Netzwerk-Statistik für alle drei (games, wins, edelweiss, minigames_won)',
    log.length === 3 && log.every((c) => c.add.games === 1 && 'wins' in c.add && 'edelweiss' in c.add && 'minigames_won' in c.add) &&
      log.reduce((s, c) => s + c.add.wins, 0) >= 1, JSON.stringify(log.map((c) => [c.name, c.add])));
  check('Edelweiß zählt in der Statistik', log.some((c) => c.name === first && c.add.edelweiss >= 1));
  const states = calls().filter((c) => c.t === 'state').map((c) => c.state);
  check('Zustand ans Netzwerk: running', states.includes('running'), states.join(','));
  check('Zurück auf den Festplatz', await waitFor(() => [anna, bert, carl].every(inHub), 20000), JSON.stringify(anna.entity.position));
  check('Dora (Zuschauerin) auch zurück', await waitFor(() => inHub(dora), 5000), dora.game.gameMode);
  check('Statistik-Datei: 1 Partie', /partien: 1/.test(statsFile('Anna')), statsFile('Anna'));
  clear(bert);
  bert.chat('/stats');
  check('/stats (englisch)', await waitFor(() => sees(bert, /Games: 1/), 3000), bert.chats.join(' | '));
  check('Zustand: waiting', await waitFor(() => calls().filter((c) => c.t === 'state').slice(-1)[0]?.state === 'waiting', 5000));
}

const currentBotByName = (n) => bots[n];

// ------------------------------------------------------------------ Partie 2

async function part2() {
  console.log('\n— Partie 2: Gipfelrennen auf dem Almsommer, zu viert —');
  const { Anna: anna, Bert: bert, Carl: carl, Dora: dora } = bots;
  const gamesBefore = Number((statsFile('Anna').match(/^partien: (\d+)/m) ?? [0, 0])[1]);
  clear(anna, bert, carl, dora);
  for (const b of [anna, bert, carl, dora]) {
    let w = await openWithItem(b, 2);
    if (w) await click(b, 11);
    w = await openWithItem(b, 3);
    if (w) await click(b, MODE.gipfel);
  }
  check('Abstimmen meldet auch gleich an', await waitFor(async () => (await state()).waiting === 4, 4000));
  let g = await phase((x) => x.phase === 'turn', 30000);
  check('Gipfelrennen auf dem Almsommer zu viert', g && g.board === 'almsommer' && g.mode === 'gipfelrennen' && g.players.length === 4,
    g && `${g.board} ${g.mode} ${g.players.length}`);
  await cmd('mparty minispiel bergsprint');
  // Runde 1: alle würfeln (kleine Zahlen)
  for (let i = 0; i < 4 && g && g.phase === 'turn'; i++) {
    await cmd(`mparty wuerfel ${g.current} 2`);
    g = await playTurn(g);
  }
  g = await waitMini('bergsprint', 20000);
  check('Minispiel Bergsprint', g != null);
  const [mx, my, mz] = g.miniAt;
  for (const [i, b] of [anna, bert, carl].entries()) {
    await sleep(700);
    await cmd(`execute at ${b.username} run tp ${b.username} ${mx + 0.5 - 2 + i * 2} ${my + 11} ${mz + 31.5}`);
  }
  // Dora läuft ein Stück
  await cmd(`execute at Dora run tp Dora ${mx + 0.5} ${my + 4} ${mz - 5}`);
  let end = await waitMiniEnd(60000);
  check('Bergsprint: Anna als Erste im Ziel', sees(bert, /Anna crossed the finish line – place 1/) && sees(anna, /Anna ist im Ziel – Platz 1/),
    anna.chats.filter((c) => /Ziel/.test(c)).join(' | '));
  check('Bergsprint: Reihenfolge Anna, Bert, Carl, Dora', resultLines(anna).slice(0, 4).map((l) => l.split(' ')[1]).join(',') === 'Anna,Bert,Carl,Dora',
    resultLines(anna).join(' | '));
  check('Bergsprint: Taler 10/6/4/3', /\+10 Taler/.test(resultLines(anna)[0] ?? '') && /\+3 Taler/.test(resultLines(anna)[3] ?? ''));
  await cmd('mparty minispiel broeckelboden');
  g = await phase((x) => x.phase === 'turn' && x.round === 2, 20000);
  for (let i = 0; i < 4 && g && g.phase === 'turn'; i++) {
    await cmd(`mparty wuerfel ${g.current} 1`);
    g = await playTurn(g);
  }
  clear(anna, bert, carl, dora);
  g = await waitMini('broeckelboden', 20000);
  check('Minispiel Bröckelboden', g != null);
  // Alle stehen still – der Boden bröckelt unter ihnen weg
  end = await waitMiniEnd(60000);
  check('Bröckelboden: Leute fallen hinaus („ist raus“)', anna.chats.filter((c) => /ist raus/.test(c)).length >= 2,
    anna.chats.filter((c) => /ist raus/.test(c)).join(' | '));
  check('Bröckelboden: Ergebnis', resultLines(anna).length === 4, resultLines(anna).join(' | '));
  check('Ausgeschiedene schauen zu (Zuschauermodus)', ['spectator', 'adventure'].includes(bert.game.gameMode));

  g = await phase((x) => x.phase === 'turn' && x.round === 3, 20000);
  // Dora geht mitten in der Partie
  clear(anna);
  dora.quit();
  delete bots.Dora;
  check('Dora geht: alle lesen es', await waitFor(() => sees(anna, /Dora hat die Partie verlassen/), 4000), anna.chats.join(' | '));
  g = await game();
  check('Partie läuft zu dritt weiter', g && g.players.filter((p) => !p.left).length === 3 && g.phase !== 'end');
  // Anna kurz vor den Gipfel, dann läuft die Runde bis Anna dran ist
  await cmd(`mparty feld Anna ${g.fields.length - 3}`);
  for (let i = 0; i < 6 && g && g.phase !== 'end'; i++) {
    if (g.phase === 'turn' && g.current === 'Anna') {
      await cmd('mparty wuerfel Anna 4');
      clear(anna, bert, carl);
    } else if (g.phase === 'turn') {
      await cmd(`mparty wuerfel ${g.current} 1`);
    }
    if (g.phase === 'turn') g = await playTurn(g);
    else g = await phase((x) => x.phase === 'turn' || x.phase === 'end', 60000);
  }
  check('Anna erreicht das Gipfelkreuz und gewinnt', sees(anna, /Anna hat das Gipfelkreuz erreicht/) || sees(bert, /Anna reached the summit cross/),
    anna.chats.filter((c) => /erreicht|Gipfel/.test(c)).join(' | '));
  check('Partie endet sofort', await waitFor(() => sees(anna, /Die Partie ist vorbei/), 5000));
  check('Anna: Titel „Gewonnen!“', sawTitle(anna, /Gewonnen/));
  const log = calls().filter((c) => c.t === 'stats' && c.mode === 'party' && c.add.games_gipfelrennen === 1);
  const annaStats = log.find((c) => c.name === 'Anna');
  check('Statistik: Sieg und Gipfel für Anna', annaStats && annaStats.add.wins === 1 && annaStats.add.summits === 1, JSON.stringify(annaStats));
  check('Dora (gegangen) bekommt keine Partie gezählt', !log.some((c) => c.name === 'Dora'));
  check('Zurück auf den Festplatz', await waitFor(() => [anna, bert, carl].every(inHub), 20000));
  check('Statistik-Datei: Anna eine Partie mehr, 1 Gipfel', new RegExp(`partien: ${gamesBefore + 1}\\b`).test(statsFile('Anna')) && /gipfel: 1/.test(statsFile('Anna')), statsFile('Anna'));
}

// ------------------------------------------------------------------ Partie 3

const REST = ['talerregen', 'pfeilhagel', 'heisse-brezn', 'trachtenfarben', 'gipfelkoenig', 'holzhacker', 'rempelei',
  'scheibenschiessen'];

async function playMini(id, g, list) {
  const [ox, oy, oz] = g.miniAt;
  let stop = false;
  const tp = (b, x, y, z) => cmd(`execute at ${b.username} run tp ${b.username} ${x.toFixed(2)} ${y.toFixed(2)} ${z.toFixed(2)}`);
  const loop = async () => {
    while (!stop) {
      try {
        if (id === 'talerregen') {
          for (const b of list) {
            const nug = Object.values(b.entities).filter((e) => e.name === 'item' && e.position.distanceTo(b.entity.position) < 25)
              .sort((p, q) => p.position.distanceTo(b.entity.position) - q.position.distanceTo(b.entity.position))[0];
            if (nug && nug.position.y < oy + 3) await tp(b, nug.position.x, oy + 1, nug.position.z);
          }
          await sleep(250);
        } else if (id === 'heisse-brezn') {
          for (const b of list) {
            if (b.heldItem?.name === 'bread' || slot(b, 0)?.name === 'bread') {
              const t = list.find((o) => o !== b && o.game.gameMode === 'adventure');
              if (!t) continue;
              await cmd(`execute at ${t.username} run tp ${b.username} ~1.2 ~ ~`);
              await sleep(250);
              const ent = b.players[t.username]?.entity;
              if (ent) {
                await b.lookAt(ent.position.offset(0, 1.4, 0), true);
                b.attack(ent);
              }
              await sleep(700);
            }
          }
          await sleep(200);
        } else if (id === 'trachtenfarben') {
          for (const b of list.slice(0, 2)) {
            const want = b.colour;
            if (!want) continue;
            const block = b.findBlock({ matching: (bl) => bl.name === `${want}_wool`, maxDistance: 20 });
            if (block && Math.abs(block.position.y - oy) < 1) await tp(b, block.position.x + 0.5, oy + 1, block.position.z + 0.5);
          }
          await sleep(300);
        } else if (id === 'gipfelkoenig') {
          await tp(list[0], ox + 0.5, oy + 5.1, oz + 0.5);
          await sleep(1000);
        } else if (id === 'holzhacker') {
          for (const b of list.slice(0, 2)) {
            const id = b.registry.blocksByName.oak_log.id;
            const spots = b.findBlocks({ matching: id, maxDistance: 20, count: 40 }).filter((v) => v.y === oy + 1);
            if (spots.length === 0) continue;
            const log = { position: spots[Math.floor(Math.random() * spots.length)] };
            await tp(b, log.position.x + 0.5, oy + 1, log.position.z - 0.7);
            await sleep(300);
            for (let i = 0; i < 3; i++) {
              const blk = b.blockAt(log.position);
              if (!blk || blk.name !== 'oak_log') break;
              b.dig(blk, true).catch(() => {});
              await sleep(300);
              b.stopDigging();
              await sleep(100);
            }
          }
          await sleep(200);
        } else if (id === 'rempelei') {
          const a = list[0];
          const t = list.find((o) => o !== a && o.game.gameMode === 'adventure');
          if (t) {
            const ent = a.players[t.username]?.entity;
            if (ent && ent.position.distanceTo(a.entity.position) < 3.2) {
              await a.lookAt(ent.position.offset(0, 1.4, 0), true);
              a.attack(ent);
            } else if (ent) {
              await cmd(`execute at ${t.username} run tp ${a.username} ^ ^ ^-1.5`);
            }
          }
          await sleep(500);
        } else if (id === 'scheibenschiessen') {
          for (const b of list) {
            const tgt = b.findBlock({ matching: (bl) => bl.name === 'target', maxDistance: 26 });
            if (!tgt) continue;
            const c = tgt.position.offset(0.5, 0.5, 0.5);
            const d = c.distanceTo(b.entity.position);
            await b.lookAt(c.offset(0, d * d * 0.0075, 0), true);
            b.setQuickBarSlot(0);
            b.activateItem();
            b.deactivateItem();
          }
          await sleep(400);
        } else {
          await sleep(300);
        }
      } catch (e) {
        await sleep(300);
      }
    }
  };
  // Trachtenfarben: Farbe aus dem Titel merken
  const onTitle = {};
  if (id === 'trachtenfarben') {
    for (const b of list) {
      onTitle[b.username] = (p) => {
        const t = plain(p.text);
        const map = { Rot: 'red', Gelb: 'yellow', Hellgrün: 'lime', Hellblau: 'light_blue', Blau: 'blue', Orange: 'orange', Rosa: 'pink', 'Weiß': 'white',
          Red: 'red', Yellow: 'yellow', Lime: 'lime', 'Light blue': 'light_blue', Blue: 'blue', Pink: 'pink', White: 'white' };
        if (map[t]) b.colour = map[t];
      };
      b._client.on('set_title_text', onTitle[b.username]);
    }
  }
  loop();
  return () => {
    stop = true;
    for (const b of list) if (onTitle[b.username]) b._client.removeListener('set_title_text', onTitle[b.username]);
  };
}

async function part3() {
  console.log('\n— Partie 3: Edelweiß-Jagd (8 Runden) im Goldstollen – die restlichen Minispiele —');
  const { Anna: anna, Bert: bert, Carl: carl } = bots;
  const list = [anna, bert, carl];
  clear(...list);
  for (const b of list) {
    let w = await openWithItem(b, 2);
    if (w) await click(b, 15);
    w = await openWithItem(b, 3);
    if (w) await click(b, MODE.jagd8);
  }
  let g = await phase((x) => x.phase === 'turn', 30000);
  check('Goldstollen, Edelweiß-Jagd 8 Runden', g && g.board === 'goldstollen' && g.mode === 'edelweiss-jagd-8', g && `${g.board} ${g.mode}`);
  for (let r = 0; r < REST.length; r++) {
    const id = REST[r];
    await cmd(`mparty minispiel ${id}`);
    for (let i = 0; i < 4 && g && g.phase === 'turn'; i++) {
      await cmd(`mparty wuerfel ${g.current} ${safeDie(g, 1 + (i % 3))}`);
      g = await playTurn(g);
    }
    clear(...list);
    g = await waitMini(id, 30000);
    if (!g) {
      check(`Minispiel ${id} startet`, false);
      g = await phase((x) => x.phase === 'turn' || x.phase === 'end', 60000);
      continue;
    }
    const stop = await playMini(id, g, list);
    const end = await waitMiniEnd(90000);
    stop();
    const lines = resultLines(anna);
    check(`${id}: läuft und hat ein Ergebnis`, end && lines.length === 3, lines.join(' | '));
    if (id === 'talerregen') check('Talerregen: Taler eingesammelt', lines.some((l) => /\((?!0 )\d+ Punkte?\)/.test(l)), lines.join(' | '));
    if (id === 'pfeilhagel') check('Pfeilhagel: getroffen = raus', anna.chats.some((c) => /ist raus/.test(c)) || lines.length === 3,
      anna.chats.filter((c) => /raus/.test(c)).join(' | '));
    if (id === 'heisse-brezn') {
      const holders = new Set(anna.chats.map((c) => (c.match(/» (\S+) hat die heiße Brezn/) ?? [])[1]).filter(Boolean));
      check('Heiße Brezn: wird weitergegeben', holders.size >= 2, [...holders].join(','));
      check('Heiße Brezn: wer sie zuletzt hat, ist raus', anna.chats.some((c) => /ist raus/.test(c)));
    }
    if (id === 'trachtenfarben') {
      check('Trachtenfarben: Farbe gezeigt und gesucht', sawTitle(anna, /Merk dir/) && sees(anna, /Gesucht war/));
      // Wer nicht auf der Farbe steht, fällt (Carl bleibt stehen – trifft er zufällig die Farbe, fallen die anderen)
      check('Trachtenfarben: wer nicht auf der Farbe steht, fällt', anna.chats.some((c) => /ist raus/.test(c)), anna.chats.filter((c) => /raus/.test(c)).join(' | '));
    }
    if (id === 'gipfelkoenig') check('Gipfelkönig: Anna oben sammelt Punkte', /^1\. Anna \((\d+) Punkte?\)/.test(lines[0] ?? '') && !/\(0 Punkte\)/.test(lines[0]), lines[0]);
    if (id === 'holzhacker') check('Holzhacker: Holz gespalten', lines.some((l) => /\([1-9]\d* Punkte?\)/.test(l)), lines.join(' | '));
    if (id === 'rempelei') check('Rempelei: jemand fällt hinunter', anna.chats.some((c) => /ist raus|Rand bröckelt/.test(c)), anna.chats.filter((c) => /raus|bröckelt/.test(c)).join(' | '));
    if (id === 'scheibenschiessen') check('Scheibenschießen: Treffer zählen', lines.some((l) => /\([1-9]\d* Punkte?\)/.test(l)), lines.join(' | '));
    g = await phase((x) => x.phase === 'turn' || x.phase === 'end', 30000);
  }
  const done = await phase((x) => x.phase === 'end', 60000);
  check('Nach 8 Runden ist Schluss', done != null);
  check('Zurück auf den Festplatz', await waitFor(() => list.every(inHub), 25000));
  clear(anna);
  anna.chat('/top');
  check('/top zeigt die Besten', await waitFor(() => sees(anna, /^1\. \S+ \d+ Siege?/), 3000), anna.chats.join(' | '));
}

// ------------------------------------------------------------------ Neue Minispiele

const sleepUntil = (t) => sleep(Math.max(0, t - Date.now()));
const ents = (b, name, d = 40) => Object.values(b.entities).filter((e) => e.name === name && e.position.distanceTo(b.entity.position) < d);
const tpTo = (b, x, y, z) => cmd(`execute at ${b.username} run tp ${b.username} ${x.toFixed(2)} ${y.toFixed(2)} ${z.toFixed(2)}`);
/** Lawinenhang: Oberfläche (y relativ) an einer Stelle z (relativ). */
const slope = (z) => 1 + Math.max(0, Math.min(80, 2 * (Math.floor(z) + 20))) / 8;

/** Schlittenrennen: Punkt der Mittellinie bei Strecke s (wie mini/Rennbahn.point). */
const TRACK_LENGTH = 80 + 28 * Math.PI;
function track(s) {
  const t = ((s % TRACK_LENGTH) + TRACK_LENGTH) % TRACK_LENGTH;
  const R = 14;
  const curve = Math.PI * R;
  if (t < 20) return [t, -R];
  if (t < 20 + curve) {
    const a = (t - 20) / R - Math.PI / 2;
    return [20 + Math.cos(a) * R, Math.sin(a) * R];
  }
  if (t < 60 + curve) return [20 - (t - 20 - curve), R];
  if (t < 60 + 2 * curve) {
    const a = (t - 60 - curve) / R + Math.PI / 2;
    return [-20 + Math.cos(a) * R, Math.sin(a) * R];
  }
  return [t - TRACK_LENGTH, -R];
}

/** Was die Test-Spieler im Minispiel tun; gibt eine Stopp-Funktion zurück. Alles Gesehene landet in seen. */
function playNew(id, g, list, seen) {
  const [ox, oy, oz] = g.miniAt;
  const [anna, bert, carl] = list;
  let stop = false;
  const jobs = [];
  const run = (fn) => jobs.push(fn().catch((e) => console.log(`  (${id}: ${e.message})`)));
  if (id === 'lawinenhang') {
    run(async () => {
      await sleep(3500);
      // Carl stellt sich einem Schneebrocken in den Weg
      for (let i = 0; i < 6 && !seen.hit; i++) {
        const boulder = ents(carl, 'block_display', 70).sort((p, q) => q.position.z - p.position.z)[0];
        if (boulder) {
          const z = boulder.position.z - 5 - oz;
          await tpTo(carl, boulder.position.x, oy + slope(z) + 0.1, oz + z);
          seen.hit = await waitFor(() => carl.bars.some((b) => /Erwischt/.test(b)), 2500);
        } else await sleep(800);
      }
      seen.carlZ = carl.entity.position.z - oz;
      await tpTo(anna, ox + 0.5, oy + 11.1, oz + 23.5);
      await sleep(1200);
      await tpTo(bert, ox + 1.5, oy + 11.1, oz + 23.5);
    });
  } else if (id === 'maibaumkraxeln') {
    run(async () => {
      const climbIds = [carl.registry.blocksByName.ladder.id, carl.registry.blocksByName.vine.id];
      const count = () => carl.findBlocks({ matching: climbIds, maxDistance: 40, count: 400 }).length;
      seen.before = count();
      // Carl klettert echt: an die Leiter, nach vorn drücken
      await tpTo(carl, ox + 3.3, oy + 1, oz + 0.5);
      await sleep(400);
      await carl.lookAt(carl.entity.position.offset(-3, 1.5, 0), true);
      carl.setControlState('forward', true);
      await sleep(3000);
      carl.setControlState('forward', false);
      seen.climbed = carl.entity.position.y - oy;
      for (let i = 0; i < 10; i++) {
        seen.min = Math.min(seen.min ?? 999, count());
        await sleep(300);
      }
      await tpTo(anna, ox + 1.5, oy + 25.05, oz + 0.5);
      await sleep(1200);
      await tpTo(bert, ox - 0.5, oy + 25.05, oz + 0.5);
    });
  } else if (id === 'eisstockschiessen') {
    for (const b of list) {
      run(async () => {
        let last = -1;
        while (!stop) {
          const bar = b.bars[b.bars.length - 1] ?? '';
          const m = bar.match(/(\d+) %/);
          const n = m ? Number(m[1]) : -1;
          // knapp vor zwei Dritteln klicken (der Balken steigt), dann wartet der Stock bis zur nächsten Runde
          if (/Kraft|Power/.test(bar) && n >= 55 && n <= 66 && n > last) {
            await b.lookAt(new (b.entity.position.constructor)(ox + 0.5, oy + 1, oz + 10.5), true);
            b.setQuickBarSlot(0);
            b.activateItem();
            b.deactivateItem();
            seen.throws = (seen.throws ?? 0) + 1;
            await sleep(1500);
          }
          last = n;
          await sleep(40);
        }
      });
    }
    run(async () => {
      while (!stop) {
        seen.stones = Math.max(seen.stones ?? 0, ents(anna, 'block_display', 50).length);
        await sleep(500);
      }
    });
  } else if (id === 'murmeltier') {
    for (const b of list) {
      run(async () => {
        while (!stop) {
          const wantStone = b === carl && !seen.stone;
          const t = ents(b, wantStone ? 'interaction' : 'rabbit', 30)
            .sort((p, q) => p.position.distanceTo(b.entity.position) - q.position.distanceTo(b.entity.position))[0];
          if (t) {
            if (t.position.distanceTo(b.entity.position) > 2.5) {
              await tpTo(b, t.position.x + 1.3, oy + 1, t.position.z + 0.2);
              await sleep(250);
            }
            await b.lookAt(t.position.offset(0, 0.6, 0), true);
            b.attack(t);
            if (wantStone) seen.stone = await waitFor(() => carl.bars.some((x) => /Autsch/.test(x)), 1000);
            await sleep(300);
          } else await sleep(200);
        }
      });
    }
  }
  if (id === 'goldwaschen') {
    for (const b of list) {
      run(async () => {
        const ids = ['gravel', 'sand'].map((n) => b.registry.blocksByName[n].id);
        while (!stop) {
          // Kies und Sand gibt es auch im Bachbett – nur die Haufen oben auf der Wiese zählen
          const spots = b.findBlocks({ matching: ids, maxDistance: 20, count: 2000 }).filter((v) => v.y === oy + 1);
          if (spots.length === 0) {
            await sleep(300);
            continue;
          }
          const v = spots[Math.floor(Math.random() * spots.length)];
          await tpTo(b, v.x + 0.5, oy + 1, v.z - 0.7);
          await sleep(250);
          const blk = b.blockAt(v);
          if (blk && (blk.name === 'gravel' || blk.name === 'sand')) {
            b.dig(blk, true).catch(() => {});
            await sleep(250);
            b.stopDigging();
            seen.digs = (seen.digs ?? 0) + 1;
          }
          await sleep(200);
        }
      });
    }
  } else if (id === 'schuhplattler') {
    for (const b of [anna, bert]) {
      const onTitle = (pk) => {
        const t = plain(pk.text);
        let move = /Springen|Jump/.test(t) ? 'jump' : /Ducken|Duck/.test(t) ? 'duck' : /Klatschen|Clap/.test(t) ? 'clap' : null;
        if (!move) return;
        // Bert macht absichtlich das Falsche (Ducken statt Springen …)
        if (b === bert) move = { jump: 'duck', duck: 'clap', clap: 'jump' }[move];
        seen.cues = (seen.cues ?? 0) + 1;
        setTimeout(async () => {
          if (move === 'jump') {
            b.setControlState('jump', true);
            await sleep(250);
            b.setControlState('jump', false);
          } else if (move === 'duck') {
            b.setControlState('sneak', true);
            await sleep(500);
            b.setControlState('sneak', false);
          } else {
            b.setQuickBarSlot(0);
            b.activateItem();
            b.deactivateItem();
          }
        }, 1430);
      };
      b._client.on('set_title_text', onTitle);
      jobs.push((async () => {
        while (!stop) await sleep(200);
        b._client.removeListener('set_title_text', onTitle);
      })());
    }
  } else if (id === 'brueckenbau') {
    run(async () => {
      const { Vec3 } = require('vec3');
      await sleep(500);
      const x = Math.floor(anna.entity.position.x);
      await tpTo(anna, x + 0.5, oy + 1, oz - 2.5);
      anna.setQuickBarSlot(0);
      await sleep(600);
      for (let z = -3; z < 11 && !stop; z++) {
        const ref = anna.blockAt(new Vec3(x, oy, oz + z));
        try {
          await anna.lookAt(ref.position.offset(0.5, 0.5, 1), true);
          await anna.placeBlock(ref, new Vec3(0, 0, 1));
          seen.placed = (seen.placed ?? 0) + 1;
        } catch (e) {
          seen.placeError = e.message;
        }
        await tpTo(anna, x + 0.5, oy + 1, oz + z + 1.5);
        await sleep(250);
      }
      await tpTo(anna, x + 0.5, oy + 1, oz + 12.5);
      // Bert fällt in die Schlucht und landet wieder am Start
      await tpTo(bert, bert.entity.position.x, oy + 1, oz + 5.5);
      seen.fell = await waitFor(() => bert.bars.some((t) => /fell|Hinuntergefallen/.test(t)), 6000);
    });
  } else if (id === 'schneeballschlacht') {
    run(async () => {
      await sleep(500);
      // Nachschub an der Schneewehe in der Mitte holen
      await tpTo(anna, ox + 1.5, oy + 1, oz + 0.5);
      await sleep(400);
      const drift = anna.blockAt(anna.entity.position.offset(-1, 0, 0).floored());
      if (drift) await anna.activateBlock(drift).catch(() => {});
      seen.grab = await waitFor(() => anna.bars.some((t) => /Schneebälle: \d+/.test(t)), 3000);
      // Bert steht fünf Blöcke weiter, Anna wirft
      await tpTo(bert, ox + 6.5, oy + 1, oz + 6.5);
      await tpTo(anna, ox + 6.5, oy + 1, oz + 1.5);
      await sleep(700);
      for (let i = 0; i < 8 && !stop; i++) {
        const t = anna.players.Bert?.entity;
        if (t) {
          const d = t.position.distanceTo(anna.entity.position);
          await anna.lookAt(t.position.offset(0, 1.2 + d * d * 0.005, 0), true);
          anna.setQuickBarSlot(0);
          anna.activateItem();
          anna.deactivateItem();
        }
        await sleep(900);
      }
      seen.hit = bert.bars.some((t) => /Hit by Anna/.test(t));
    });
  }
  if (id === 'schlittenrennen') {
    run(async () => {
      await sleep(1500);
      seen.seated = [anna, bert, carl].filter((x) => x.vehicle).length;
      seen.boats = ents(anna, 'spruce_boat', 60).length + ents(anna, 'birch_boat', 60).length + ents(anna, 'oak_boat', 60).length;
      // Bert fährt hin und her über die Ziellinie – das zählt nicht
      for (let i = 0; i < 3; i++) {
        let p = track(-2);
        await tpTo(bert, ox + 0.5 + p[0], oy + 1.1, oz + 0.5 + p[1]);
        await sleep(400);
        p = track(3);
        await tpTo(bert, ox + 0.5 + p[0], oy + 1.1, oz + 0.5 + p[1]);
        await sleep(400);
      }
      // Anna fährt durch alle Tore, zweimal herum
      for (let k = 1; k <= 12 && !stop; k++) {
        for (const extra of [-6, 3]) {
          const p = track((TRACK_LENGTH * k) / 6 + extra);
          await tpTo(anna, ox + 0.5 + p[0], oy + 1.1, oz + 0.5 + p[1]);
          await sleep(350);
        }
      }
      seen.reseated = await waitFor(() => anna.vehicle != null, 3000);
    });
  } else if (id === 'enzian-suche') {
    for (const b of [anna, bert]) {
      run(async () => {
        const flower = b.registry.blocksByName.cornflower.id;
        while (!stop) {
          const f = b.findBlock({ matching: flower, maxDistance: 40 });
          if (!f) {
            await sleep(300);
            continue;
          }
          await tpTo(b, f.position.x + 0.5, oy + 1, f.position.z - 0.8);
          await sleep(300);
          const blk = b.blockAt(f.position);
          if (blk?.name === 'cornflower') {
            if (b === anna) {
              b.dig(blk, true).catch(() => {});
              await sleep(150);
              b.stopDigging();
            } else {
              await b.activateBlock(blk).catch(() => {});
            }
            seen[b.username] = (seen[b.username] ?? 0) + 1;
          }
          await sleep(250);
        }
      });
    }
  } else if (id === 'musterweber') {
    for (const b of [anna, bert]) {
      run(async () => {
        const colours = ['red', 'white', 'blue', 'yellow'];
        const ids = colours.map((c) => b.registry.blocksByName[`${c}_wool`].id);
        let seenRounds = 0;
        while (!stop) {
          const shows = b.titles.filter((t) => /Merk dir das Muster|Remember the pattern/.test(t)).length;
          if (shows > seenRounds) {
            seenRounds = shows;
            await sleep(600);
            const cells = b.findBlocks({ matching: ids, maxDistance: 5.5, count: 32 }).map((v) => ({ v, name: b.blockAt(v).name }));
            seen[`${b.username}cells`] = cells.length;
            await waitFor(() => b.titles.filter((t) => /Jetzt weben|Weave now/.test(t)).length >= seenRounds, 8000, 100);
            await sleep(400);
            // Anna legt alles richtig nach, Bert nur jedes zweite Feld
            for (const [i, c] of cells.entries()) {
              if (b === bert && i % 2 === 1) continue;
              const slotIdx = b.inventory.slots.findIndex((it, si) => si >= 36 && si < 45 && it?.name === c.name);
              if (slotIdx < 0) continue;
              b.setQuickBarSlot(slotIdx - 36);
              await sleep(60);
              await b.activateBlock(b.blockAt(c.v)).catch(() => {});
              await sleep(90);
            }
          }
          await sleep(100);
        }
      });
    }
  } else if (id === 'foehnsturm') {
    run(async () => {
      anna.setControlState('sneak', true);
      await waitFor(() => anna.titles.some((t) => /Föhnböe/.test(t)), 12000);
      seen.gust = true;
      await sleep(1000);
      // Carl stellt sich an den Rand und fällt hinunter
      await tpTo(carl, ox + 12.5, oy + 1, oz + 0.5);
      seen.out = await waitFor(() => anna.chats.some((c) => /Carl ist raus/.test(c)), 6000);
      anna.setControlState('sneak', false);
    });
  }
  return async () => {
    stop = true;
    await Promise.all(jobs);
  };
}

/** Eine Partie (Edelweiß-Jagd, 8 Runden, Goldstollen) nur für diese Minispiele; danach /mparty stopp. */
async function newMinis(title, ids) {
  console.log(`\n— ${title} —`);
  const { Anna: anna, Bert: bert, Carl: carl } = bots;
  const list = [anna, bert, carl];
  clear(...list);
  for (const b of list) {
    let w = await openWithItem(b, 2);
    if (w) await click(b, 15);
    w = await openWithItem(b, 3);
    if (w) await click(b, MODE.jagd8);
  }
  let g = await phase((x) => x.phase === 'turn', 30000);
  check(`${title}: Partie startet`, g != null);
  for (const id of ids.filter((x) => !process.env.ONLY || process.env.ONLY.split(',').includes(x))) {
    await cmd(`mparty minispiel ${id}`);
    for (let i = 0; i < 4 && g && g.phase === 'turn'; i++) {
      await cmd(`mparty wuerfel ${g.current} ${safeDie(g, 1 + (i % 3))}`);
      g = await playTurn(g);
    }
    clear(...list);
    g = await waitMini(id, 30000);
    if (!g) {
      check(`Minispiel ${id} startet`, false);
      g = await phase((x) => x.phase === 'turn' || x.phase === 'end', 60000);
      continue;
    }
    const before = Object.fromEntries(g.players.map((p) => [p.name, p.taler]));
    const seen = {};
    const stop = playNew(id, g, list, seen);
    const end = await waitMiniEnd(120000);
    await stop();
    const lines = resultLines(anna);
    check(`${id}: läuft und hat ein Ergebnis`, end && lines.length === 3, lines.join(' | '));
    const after = await game();
    check(`${id}: Taler für die Plätze`, after && after.players.some((p) => p.taler >= before[p.name] + 10),
      after && JSON.stringify(after.players.map((p) => `${p.name}:${before[p.name]}→${p.taler}`)));
    if (id === 'lawinenhang') {
      check('Lawinenhang: Schneebrocken trifft und schiebt zurück', seen.hit, carl.bars.slice(-3).join(' | '));
      check('Lawinenhang: Anna zuerst oben, Bert Zweiter', /^1\. Anna \(im Ziel/.test(lines[0] ?? '') && /^2\. Bert \(im Ziel/.test(lines[1] ?? ''),
        lines.join(' | '));
      check('Lawinenhang: Carl nach Strecke gewertet', /^3\. Carl \(\d+ (Blöcke|Block) weit\)/.test(lines[2] ?? ''), lines[2]);
      check('Englisch: „finished in“', bert.chats.some((c) => /Anna \(finished in/.test(c)));
    }
    if (id === 'maibaumkraxeln') {
      check('Maibaum: Carl klettert an der Leiter hinauf', seen.climbed > 2.5, String(seen.climbed));
      check('Maibaum: Stücke brechen weg', seen.min < seen.before, `${seen.before} → ${seen.min}`);
      check('Maibaum: Anna hat den Kranz zuerst', sees(anna, /Anna hat den Kranz erreicht – Platz 1/) && /^1\. Anna \(oben nach/.test(lines[0] ?? ''),
        lines.join(' | '));
      check('Maibaum: Carl nach Höhe gewertet', /^3\. Carl \(\d+ (Blöcke|Block) hoch\)/.test(lines[2] ?? ''), lines[2]);
    }
    if (id === 'eisstockschiessen') {
      check('Eisstock: Kraftbalken in der Aktionsleiste', anna.bars.some((b) => /Kraft .*\d+ %/.test(b)) && bert.bars.some((b) => /Power/.test(b)),
        anna.bars.slice(-1)[0]);
      check('Eisstock: alle werfen dreimal (Klick)', (seen.throws ?? 0) >= 6, String(seen.throws));
      check('Eisstock: Stöcke gleiten über die Bahn', (seen.stones ?? 0) >= 4, String(seen.stones));
      check('Eisstock: Zwischenstand nach jedem Wurf', sees(anna, /Nach Wurf 3/), anna.chats.filter((c) => /Wurf/.test(c)).join(' | '));
      check('Eisstock: Punkte nach Abstand zur Daube', lines.some((l) => /\([1-9]\d* Punkte?\)/.test(l)), lines.join(' | '));
    }
    if (id === 'murmeltier') {
      check('Murmeltier: Treffer geben Punkte', lines.some((l) => /\([1-9]\d* Punkte?\)/.test(l)), lines.join(' | '));
      check('Murmeltier: Steinbrocken kostet einen Punkt', seen.stone, carl.bars.slice(-3).join(' | '));
    }
    if (id === 'goldwaschen') {
      check('Goldwaschen: Haufen auswaschen', (seen.digs ?? 0) >= 6 && [anna, bert, carl].some((x) => x.bars.some((t) => /Nur Kies|Just gravel|Punkte|points/.test(t))),
        `${seen.digs} Mal`);
      check('Goldwaschen: Gold gefunden gibt Punkte', lines.some((l) => /\([1-9]\d* (Punkte?|points?)\)/.test(l)), lines.join(' | '));
    }
    if (id === 'schuhplattler') {
      check('Schuhplattler: Ansagen im Takt', (seen.cues ?? 0) >= 4, String(seen.cues));
      check('Schuhplattler: richtig getanzt gibt Punkte', anna.bars.some((t) => /Perfekt|Gut!/.test(t)), anna.bars.filter((t) => /Perfekt|Gut|Daneben/.test(t)).slice(-4).join(' | '));
      check('Schuhplattler: falsch getanzt gibt nichts', bert.bars.some((t) => /Missed/.test(t)), bert.bars.slice(-3).join(' | '));
      check('Schuhplattler: Anna vor Bert', lines.findIndex((l) => / Anna /.test(l)) < lines.findIndex((l) => / Bert /.test(l)), lines.join(' | '));
    }
    if (id === 'brueckenbau') {
      check('Brückenbau: Anna baut echt Brett für Brett', (seen.placed ?? 0) >= 12, `${seen.placed} ${seen.placeError ?? ''}`);
      check('Brückenbau: Anna ist zuerst drüben', /^1\. Anna \(im Ziel/.test(lines[0] ?? ''), lines.join(' | '));
      check('Brückenbau: wer fällt, steht wieder am Start', seen.fell, bert.bars.slice(-2).join(' | '));
    }
    if (id === 'schneeballschlacht') {
      check('Schneeballschlacht: Schneebälle aus der Schneewehe', seen.grab, anna.bars.slice(-2).join(' | '));
      check('Schneeballschlacht: Treffer (Bert wird getroffen)', seen.hit, bert.bars.slice(-3).join(' | '));
      check('Schneeballschlacht: Anna hat Punkte', /^1\. Anna \([1-9]/.test(lines[0] ?? ''), lines.join(' | '));
    }
    if (id === 'schlittenrennen') {
      check('Schlittenrennen: alle sitzen im Schlitten', seen.seated === 3 && seen.boats >= 2, `${seen.seated} sitzen, ${seen.boats} Boote`);
      check('Schlittenrennen: Anna fährt zwei Runden durch alle Tore', /^1\. Anna \(im Ziel/.test(lines[0] ?? ''), lines.join(' | '));
      check('Schlittenrennen: hin und her über die Linie zählt nicht', lines.some((l) => /Bert \(0 Tore\)/.test(l)), lines.join(' | '));
      check('Schlittenrennen: wer herausfällt, sitzt gleich wieder', seen.reseated);
      check('Englisch: „gates“', bert.chats.some((c) => /Bert \(0 gates\)/.test(c)));
    }
    if (id === 'enzian-suche') {
      check('Enzian-Suche: Enziane gefunden und gepflückt (Schlag und Rechtsklick)', (seen.Anna ?? 0) >= 2 && (seen.Bert ?? 0) >= 2,
        `${seen.Anna}/${seen.Bert}`);
      check('Enzian-Suche: Punkte für Anna und Bert', lines.filter((l) => /(Anna|Bert) \([1-9]\d* Punkte?\)/.test(l)).length === 2, lines.join(' | '));
    }
    if (id === 'musterweber') {
      check('Musterweber: Muster gezeigt (16 Felder)', seen.Annacells === 16, String(seen.Annacells));
      check('Musterweber: Anna legt alles richtig nach', anna.bars.some((t) => /Alles richtig/.test(t)), anna.bars.filter((t) => /richtig/.test(t)).join(' | '));
      check('Musterweber: drei Runden, Anna vorn', /^1\. Anna \((6[0-9]|5\d) Punkte\)/.test(lines[0] ?? ''), lines.join(' | '));
    }
    if (id === 'foehnsturm') {
      check('Föhnsturm: Böe angekündigt (Titel und Richtung)', seen.gust && anna.titles.some((t) => /aus (Norden|Nordosten|Osten|Südosten|Süden|Südwesten|Westen|Nordwesten)/.test(t)));
      check('Föhnsturm: wer hinunterfällt, ist raus', seen.out, anna.chats.filter((c) => /raus/.test(c)).join(' | '));
      check('Föhnsturm: Carl ist Letzter', /^3\. Carl/.test(lines[2] ?? ''), lines.join(' | '));
    }
    g = await phase((x) => x.phase === 'turn' || x.phase === 'end', 30000);
  }
  await cmd('mparty stopp');
  check(`${title}: zurück auf den Festplatz`, await waitFor(() => list.every(inHub), 30000));
}

const PAKET1 = ['lawinenhang', 'maibaumkraxeln', 'eisstockschiessen', 'murmeltier'];
const PAKET2 = ['goldwaschen', 'schuhplattler', 'brueckenbau', 'schneeballschlacht'];
const PAKET3 = ['schlittenrennen', 'enzian-suche', 'musterweber', 'foehnsturm'];

// ------------------------------------------------------------------ Minispiel-Marathon

async function marathon() {
  console.log('\n— Partie 7: Minispiel-Marathon (3 Minispiele) zu viert —');
  if (!bots.Dora) bots.Dora = await join('Dora');
  bots.Dora.policy = defaultPolicy;
  const { Anna: anna, Bert: bert, Carl: carl, Dora: dora } = bots;
  const list = [anna, bert, carl, dora];
  clear(...list);
  for (const b of list) {
    const w = await openWithItem(b, 3);
    if (w) await click(b, MODE.marathon3);
  }
  let g = await phase((x) => x.phase !== 'end', 30000);
  check('Marathon startet zu viert, ohne Spielbrett', g && g.mode === 'marathon-3' && g.players.length === 4, g && `${g.mode} ${g.players.length}`);
  await cmd('mparty minispiel bergsprint');
  check('Titel „Minispiel-Marathon“ (Englisch: „Minigame Marathon“)', await waitFor(() => sawTitle(anna, /Minispiel-Marathon/) && sawTitle(bert, /Minigame Marathon/), 5000));
  check('Regeln: Platz-Punkte 10, 7, 5', sees(anna, /Platz-Punkte: 10 für Platz 1, 7 für Platz 2, 5 für Platz 3/), anna.chats.find((c) => /Platz-Punkte/.test(c)));
  // 1. Bergsprint: Reihenfolge Anna, Bert, Carl, Dora
  g = await waitMini('bergsprint', 30000);
  check('Kein Brett: gleich das erste Minispiel', g != null && sees(anna, /Minispiel 1 von 3/));
  const [mx, my, mz] = g.miniAt;
  for (const [i, b] of [anna, bert, carl].entries()) {
    await sleep(700);
    await tpTo(b, mx + 0.5 - 2 + i * 2, my + 11, mz + 31.5);
  }
  await waitMiniEnd(60000);
  check('Ergebnis mit Platz-Punkten', resultLines2(anna).some((l) => /^1\. Anna .*\+10 Punkte/.test(l)) && resultLines2(bert).some((l) => /\+7 points/.test(l)),
    resultLines2(anna).join(' | '));
  await cmd('mparty minispiel gipfelkoenig');
  check('Zwischenstand nach dem Minispiel', await waitFor(() => sees(anna, /Zwischenstand nach 1 von 3/) && sees(anna, /1\. Anna – 10 Punkte/), 12000),
    anna.chats.filter((c) => /Zwischenstand|Punkte$/.test(c)).join(' | '));
  g = await game();
  check('Punkte im Zustand: Anna 10, Bert 7, Carl 5, Dora 4', g && ['Anna:10', 'Bert:7', 'Carl:5', 'Dora:4'].every((x) => g.players.some((p) => `${p.name}:${p.points}` === x)),
    g && JSON.stringify(g.players.map((p) => `${p.name}:${p.points}`)));
  const side = Object.values(anna.scoreboards ?? {}).some((sb) => Object.values(sb.itemsMap ?? {}).some((it) => /Minispiel/.test(plain(it.displayName))));
  check('Seitenleiste zeigt den Marathon', side || anna.scoreboard?.sidebar != null);
  // 2. Gipfelkönig: Anna oben
  clear(...list);
  g = await waitMini('gipfelkoenig', 30000);
  const stop2 = await playMini('gipfelkoenig', g, list);
  await waitMiniEnd(60000);
  stop2();
  await cmd('mparty minispiel broeckelboden');
  // 3. Bröckelboden: alle stehen still
  g = await waitMini('broeckelboden', 30000);
  check('Drittes Minispiel', g != null && sees(anna, /Minispiel 3 von 3/));
  const done = await phase((x) => x.phase === 'end', 90000);
  check('Nach drei Minispielen: Siegerehrung', done != null && await waitFor(() => sees(anna, /Die Partie ist vorbei/) && sees(bert, /The game is over/), 5000));
  const line = anna.chats.find((c) => /^1\. Anna – \d+ Punkte · \d+ Minispiele? gewonnen/.test(c));
  check('Anna gewinnt mit Punkten und Minispiel-Siegen', line != null, anna.chats.filter((c) => /Punkte ·/.test(c)).join(' | '));
  check('Titel Gewonnen!', sawTitle(anna, /Gewonnen/));
  const log = calls().filter((c) => c.t === 'stats' && c.mode === 'party' && c.add.games_marathon === 1);
  const a = log.find((c) => c.name === 'Anna');
  check('Statistik ans Netzwerk: games_marathon, wins_marathon, marathon_points_best', log.length === 4 && a && a.add.wins_marathon === 1 &&
    a.best.marathon_points_best >= 20, JSON.stringify(a));
  check('Zurück auf den Festplatz', await waitFor(() => list.every(inHub), 25000));
  clear(anna, bert);
  anna.chat('/stats');
  bert.chat('/stats');
  check('/stats zeigt den Marathon (de/en)', await waitFor(() => sees(anna, /Minispiel-Marathon: 1 Partie · 1 Sieg\b/) && sees(bert, /Minigame Marathon: 1 game\b/), 4000),
    anna.chats.join(' | '));
}

/** Vier Züge spielen (Würfel fest), bis das Minispiel der Runde beginnt. */
async function playRound(g, setup) {
  for (let i = 0; i < 4 && g && g.phase === 'turn'; i++) {
    if (setup) await setup(g);
    else {
      // sicher auf ein blaues Feld (kein Duellfeld, das das festgelegte Minispiel vorher verbrauchen würde)
      const safe = fieldIndex(g, 'T', 18);
      await cmd(`mparty feld ${g.current} ${safe - 1}`, `mparty wuerfel ${g.current} 1`);
    }
    g = await playTurn(g);
  }
  return g;
}

/** Ergebnis aller Seiten (Taler vorher → nachher). */
const talerOf = (g) => Object.fromEntries(g.players.map((p) => [p.name, p.taler]));

async function teams() {
  console.log('\n— Partie 8: Team- und Duell-Minispiele zu viert —');
  if (!bots.Dora) bots.Dora = await join('Dora');
  bots.Dora.policy = defaultPolicy;
  const { Anna: anna, Bert: bert, Carl: carl, Dora: dora } = bots;
  const list = [anna, bert, carl, dora];
  clear(...list);
  for (const b of list) {
    let w = await openWithItem(b, 2);
    if (w) await click(b, 15);
    w = await openWithItem(b, 3);
    if (w) await click(b, MODE.jagd8);
  }
  let g = await phase((x) => x.phase === 'turn', 30000);
  check('Teams: Partie zu viert startet', g && g.players.length === 4, g && String(g.players.length));
  const order = g.players.map((p) => p.name);

  // ---- Runde 1: zwei landen auf blauen, zwei auf roten Feldern → 2 gegen 2 von selbst
  await cmd(`mparty stand ${fieldIndex(g, 'T', 0)}`);
  const blue1 = fieldIndex(g, 'T', 18);
  const blue2 = fieldIndex(g, 'T', blue1);
  const red1 = fieldIndex(g, 'V', 18);
  const red2 = fieldIndex(g, 'V', red1);
  const target = { [order[0]]: blue1, [order[1]]: red1, [order[2]]: blue2, [order[3]]: red2 };
  g = await playRound(g, async (x) => cmd(`mparty feld ${x.current} ${target[x.current] - 1}`, `mparty wuerfel ${x.current} 1`));
  g = await phase((x) => x.phase === 'mini', 30000);
  check('Felder entscheiden: 2 gegen 2 (blau gegen rot)', g && g.format === 'team' && g.sideA === `${order[0]} + ${order[2]}` &&
    g.sideB === `${order[1]} + ${order[3]}`, g && `${g.format} ${g.mini} ${g.sideA} | ${g.sideB}`);
  check('Ansage „Zwei gegen zwei“ (Englisch: „Two against two“)', sees(anna, /Zwei gegen zwei:/) && sees(bert, /Two against two:/));
  check('Mützen in der Farbe der Seite', list.some((b) => b.inventory.slots[5]?.name === 'leather_helmet'),
    list.map((b) => b.inventory.slots[5]?.name).join(','));
  await waitMiniEnd(90000);
  check('Team-Ergebnis im Chat', await waitFor(() => sees(anna, /gewinnt – je \d+ Taler|Gleichstand zwischen den Seiten/), 5000),
    anna.chats.filter((c) => /Taler/.test(c)).slice(-3).join(' | '));
  g = await phase((x) => x.phase === 'turn', 30000);

  // ---- Runde 2: Tauziehen 2 gegen 2, Seite a zieht, Seite b nicht
  await cmd('mparty seiten team', 'mparty minispiel tauziehen');
  g = await playRound(g);
  let before = talerOf(g);
  g = await waitMini('tauziehen', 30000);
  check('Tauziehen: 2 gegen 2', g && g.format === 'team' && g.sideA === `${order[0]} + ${order[1]}`, g && `${g.format} ${g.sideA}`);
  let pulling = true;
  const pullers = [bots[order[0]], bots[order[1]]];
  const pullJob = (async () => { while (pulling) { for (const b of pullers) b.swingArm(); await sleep(Number(process.env.PULL_MS ?? 80)); } })();
  const start = Date.now();
  await waitMiniEnd(40000);
  pulling = false;
  await pullJob;
  check('Tauziehen: Seil ganz herübergezogen vor Ablauf der Zeit', Date.now() - start < 29000, `${Date.now() - start} ms`);
  check('Tauziehen: Seil in der Aktionsleiste', bots[order[0]].bars.some((b) => /◆/.test(b)), bots[order[0]].bars.slice(-1)[0]);
  g = await phase((x) => x.phase === 'turn', 30000);
  check('Tauziehen: Sieger je 8 Taler, Verlierer nichts', g && pl(g, order[0]).taler === before[order[0]] + 8 &&
    pl(g, order[1]).taler === before[order[1]] + 8 && pl(g, order[2]).taler === before[order[2]],
    g && JSON.stringify(g.players.map((p) => `${p.name}:${before[p.name]}→${p.taler}`)));
  check('Team-Sieg im Chat', sees(anna, new RegExp(`${order[0]} \\+ ${order[1]} gewinnen – je 8 Taler`)), anna.chats.slice(-6).join(' | '));

  // ---- Runde 3: Gamsjagd 1 gegen 3, der Jäger fängt alle
  await cmd('mparty seiten einzel', 'mparty minispiel gamsjagd');
  g = await playRound(g);
  before = talerOf(g);
  clear(...list);
  g = await waitMini('gamsjagd', 30000);
  check('Gamsjagd: einer gegen drei', g && g.format === 'solo' && g.sideA === order[0], g && `${g.format} ${g.sideA}`);
  const hunter = bots[order[0]];
  check('Gamsjagd: Rollen als Titel', await waitFor(() => sawTitle(hunter, /Jäger|hunter/) && sawTitle(bots[order[1]], /Gämse|chamois/), 5000));
  await sleep(3500);
  for (const name of order.slice(1)) {
    const prey = bots[name];
    for (let k = 0; k < 6; k++) {
      await cmd(`execute at ${name} run tp ${hunter.username} ~1 ~ ~`);
      await sleep(400);
      const e = hunter.players[name]?.entity;
      if (e) hunter.attack(e);
      await sleep(300);
      if (sees(anna, new RegExp(`${name} wurde von ${hunter.username} gefangen`))) break;
    }
    check(`Gamsjagd: ${name} gefangen`, sees(anna, new RegExp(`${name} wurde von ${hunter.username} gefangen`)), prey.username);
  }
  await waitMiniEnd(30000);
  g = await phase((x) => x.phase === 'turn', 30000);
  check('Gamsjagd: „gewinnt allein gegen alle“', sees(anna, new RegExp(`${order[0]} gewinnt allein gegen alle – 18 Taler`)));
  check('Gamsjagd: der Jäger gewinnt 18 Taler', g && pl(g, order[0]).taler === before[order[0]] + 18 && pl(g, order[1]).taler === before[order[1]],
    g && JSON.stringify(g.players.map((p) => `${p.name}:${before[p.name]}→${p.taler}`)));

  // ---- Runde 4: Murmeltier 1 gegen 3, niemand trifft → Gleichstand
  await cmd('mparty seiten einzel', 'mparty minispiel murmeltier');
  g = await playRound(g);
  before = talerOf(g);
  g = await waitMini('murmeltier', 30000);
  check('Murmeltier als einer gegen drei', g && g.format === 'solo', g && g.format);
  check('Ansage „Einer gegen drei“ mit Vorteil', sees(anna, /Einer gegen drei: .* der Einzelne hat einen Vorteil/));
  await waitMiniEnd(70000);
  g = await phase((x) => x.phase === 'turn', 30000);
  check('Gleichstand: alle bekommen 3 Taler', g && g.players.every((p) => p.taler === before[p.name] + 3) &&
    sees(anna, /Gleichstand zwischen den Seiten/), g && JSON.stringify(g.players.map((p) => `${p.name}:${before[p.name]}→${p.taler}`)));
  await cmd('mparty stopp');
  check('Teams: zurück auf den Festplatz', await waitFor(() => list.every(inHub), 30000));
}

async function seilschaft() {
  console.log('\n— Partie 9: Seilschaft (3 Runden) zu viert —');
  if (!bots.Dora) bots.Dora = await join('Dora');
  bots.Dora.policy = defaultPolicy;
  const { Anna: anna, Bert: bert, Carl: carl, Dora: dora } = bots;
  const list = [anna, bert, carl, dora];
  clear(...list);
  for (const b of list) {
    let w = await openWithItem(b, 2);
    if (w) await click(b, 15);
    w = await openWithItem(b, 3);
    if (w) await click(b, MODE.seil3);
  }
  let g = await phase((x) => x.phase === 'turn', 30000);
  check('Seilschaft startet zu viert', g && g.mode === 'seilschaft-3' && g.players.length === 4, g && g.mode);
  const team = (n) => pl(g, n).team;
  check('Party Anna + Bert ist eine Seilschaft', g && team('Anna') === team('Bert') && team('Carl') === team('Dora') && team('Anna') !== team('Carl'),
    g && JSON.stringify(g.players.map((p) => `${p.name}:${p.team}`)));
  const order = g.players.map((p) => p.name);
  check('Züge wechseln zwischen den Seilschaften', g && team(order[0]) !== team(order[1]) && team(order[1]) !== team(order[2]), order.join(','));
  check('Ansage der Seilschaften (gemeinsam 20 Taler)', sees(anna, /Anna & Bert – gemeinsam 20 Taler/) && sees(bert, /Anna & Bert – together 20 coins/),
    anna.chats.filter((c) => /gemeinsam/.test(c)).join(' | '));
  await cmd('mparty taler Anna 30');
  g = await game();
  check('Gemeinsamer Geldbeutel: Bert hat auch 30', g && pl(g, 'Bert').taler === 30 && pl(g, 'Carl').taler === 20,
    g && JSON.stringify(g.players.map((p) => `${p.name}:${p.taler}`)));

  // ---- Runde 1: Duell nur gegen die andere Seilschaft, danach Tauziehen 2 gegen 2
  const first = g.current;
  const duelField = fieldIndex(g, 'D', 1);
  await cmd(`mparty feld ${first} ${duelField - 1}`, `mparty wuerfel ${first} 1`, 'mparty minispiel tauziehen');
  clear(...list);
  const fb = bots[first];
  const menuP = nextWindow(fb);
  const oldPolicy = fb.policy;
  fb.policy = async () => {};
  await use(fb, 0);
  const menu = await menuP;
  const heads = menu ? menu.slots.filter((it) => it && it.name === 'player_head').map((it) => plain(it.customName ?? it.displayName ?? '')) : [];
  const partner = g.players.find((p) => p.name !== first && p.team === pl(g, first).team).name;
  check('Duell-Menü ohne den Partner', heads.length === 2 && !heads.some((h) => h.includes(partner)), heads.join(','));
  fb.policy = oldPolicy;
  if (menu) fb.closeWindow(fb.currentWindow ?? menu);
  const dg = await waitMini('tauziehen', 30000);
  check('Duell gegen die andere Seilschaft', dg && dg.format === 'duel' && dg.sideB !== partner, dg && `${dg.sideA} gegen ${dg.sideB}`);
  await waitMiniEnd(40000);
  g = await phase((x) => x.phase === 'turn' && x.current !== first, 40000);
  await cmd('mparty minispiel tauziehen');
  for (let i = 0; i < 3 && g && g.phase === 'turn'; i++) {
    const safe = fieldIndex(g, 'T', 18);
    await cmd(`mparty feld ${g.current} ${safe - 1}`, `mparty wuerfel ${g.current} 1`);
    g = await playTurn(g);
  }
  const before = await game();
  g = await waitMini('tauziehen', 30000);
  check('Minispiel nach der Runde: Seilschaft gegen Seilschaft', g && g.format === 'team' &&
    [g.sideA, g.sideB].includes('Anna + Bert') && [g.sideA, g.sideB].includes('Carl + Dora'), g && `${g.sideA} | ${g.sideB}`);
  let pulling = true;
  const pullJob = (async () => { while (pulling) { anna.swingArm(); bert.swingArm(); await sleep(Number(process.env.PULL_MS ?? 80)); } })();
  await waitMiniEnd(40000);
  pulling = false;
  await pullJob;
  g = await phase((x) => x.phase === 'turn' || x.phase === 'end', 30000);
  check('Sieg im Tauziehen: +16 für den gemeinsamen Beutel (je 8)', g && before && pl(g, 'Anna').taler === pl(before, 'Anna').taler + 16 &&
    pl(g, 'Bert').taler === pl(g, 'Anna').taler, g && before && `${pl(before, 'Anna').taler} → ${pl(g, 'Anna').taler}`);
  const side = Object.values(anna.scoreboards ?? {}).flatMap((sb) => Object.values(sb.itemsMap ?? {}).map((it) => plain(it.displayName)));
  // mineflayer liest die Anzeigenamen der Zeilen nicht – die Zeilen selbst sieht man auf dem Bild (echter Client)
  check('Seitenleiste da', side.some((l) => /Anna & Bert|Bert & Anna/.test(l.replace('▶', ''))) || anna.scoreboard?.sidebar != null);
  const done = await phase((x) => x.phase === 'end', 400000);
  check('Seilschaft endet nach 3 Runden', done != null);
  // erst die Auszeichnungen, dann die Siegerehrung mit „Anna & Bert“
  check('Sieger als Seilschaft angesagt', await waitFor(() => sawTitle(anna, /&/) || sawTitle(carl, /&/), 25000), anna.titles.slice(-3).join(' | '));
  check('Zurück auf den Festplatz', await waitFor(() => list.every(inHub), 30000));
  const log = calls().filter((c) => c.t === 'stats' && c.add && c.add.games_seilschaft === 1);
  check('Statistik ans Netzwerk: games_seilschaft für alle vier', log.length === 4, String(log.length));
  clear(anna, bert);
  anna.chat('/stats');
  bert.chat('/stats');
  check('/stats zeigt die Seilschaft (de/en)', await waitFor(() => sees(anna, /Seilschaft: 1 Partie\b/) && sees(bert, /Rope Team: 1 game\b/), 4000),
    anna.chats.slice(-6).join(' | '));
}

/** Abstimmen und warten, bis die Partie läuft. */
async function startWith(list, slot) {
  clear(...list);
  for (const b of list) {
    let w = await openWithItem(b, 2);
    if (w) await click(b, 15);
    w = await openWithItem(b, 3);
    if (w) await click(b, slot);
  }
  return phase(() => true, 30000);
}

async function schnell() {
  console.log('\n— Partie 10: Schnelle Partie (2 Runden) zu dritt —');
  const { Anna: anna, Bert: bert, Carl: carl } = bots;
  const list = [anna, bert, carl];
  let g = await startWith(list, MODE.schnell);
  g = await phase((x) => x.phase === 'turn', 30000);
  check('Schnelle Partie startet', g && g.mode === 'schnell-2' && g.players.length === 3, g && g.mode);
  check('Mehr Start-Taler (20)', g && g.players.every((p) => p.taler === 20), g && JSON.stringify(g.players.map((p) => p.taler)));
  check('Ansage: Edelweiß nur 15 Taler (de/en)', sees(anna, /Schnelle Partie: nur 2 Runden!.*nur 15 Taler/) && sees(bert, /Quick game: only 2 rounds!.*only 15 coins/),
    anna.chats.filter((c) => /Schnelle/.test(c)).join(' | '));
  await cmd('mparty minispiel bergsprint');
  for (let i = 0; i < 3 && g && g.phase === 'turn'; i++) {
    await cmd(`mparty wuerfel ${g.current} ${safeDie(g, 1 + i)}`);
    g = await playTurn(g);
  }
  g = await waitMini('bergsprint', 30000);
  check('Minispiel kürzer (45 s × 0,4 × 0,75 = 13,5 s)', g && g.miniTicks === 270, g && String(g.miniTicks));
  const done = await phase((x) => x.phase === 'end', 300000);
  check('Nach 2 Runden vorbei', done != null);
  check('Zurück auf den Festplatz', await waitFor(() => list.every(inHub), 30000));
  const log = calls().filter((c) => c.t === 'stats' && c.add && c.add.games_schnell === 1);
  check('Statistik: games_schnell', log.length === 3, String(log.length));
}

async function bergduell() {
  console.log('\n— Partie 11: Bergduell (2 Runden), Anna gegen Bert —');
  const { Anna: anna, Bert: bert } = bots;
  const list = [anna, bert];
  let g = await startWith(list, MODE.duell);
  g = await phase((x) => x.phase === 'turn', 30000);
  check('Bergduell startet zu zweit', g && g.mode === 'bergduell-2' && g.players.length === 2, g && `${g.mode} ${g.players.length}`);
  check('Ansage Bergduell (de/en)', sees(anna, /Bergduell: ihr zwei gegeneinander/) && sees(bert, /Mountain duel: the two of you/));
  await cmd('mparty minispiel tauziehen');
  for (let i = 0; i < 2 && g && g.phase === 'turn'; i++) {
    const safe = fieldIndex(g, 'T', 18);
    await cmd(`mparty feld ${g.current} ${safe - 1}`, `mparty wuerfel ${g.current} 1`);
    g = await playTurn(g);
  }
  const before = await game();
  g = await waitMini('tauziehen', 30000);
  check('Minispiel nach der Runde ist ein Duell', g && g.format === 'duel' && g.miniPlayers === 2, g && g.format);
  let pulling = true;
  const pullJob = (async () => { while (pulling) { anna.swingArm(); await sleep(80); } })();
  await waitMiniEnd(40000);
  pulling = false;
  await pullJob;
  g = await phase((x) => x.phase === 'turn' || x.phase === 'end', 30000);
  check('Duell-Sieg: Anna +8 Taler', g && before && pl(g, 'Anna').taler === pl(before, 'Anna').taler + 8 && pl(g, 'Bert').taler === pl(before, 'Bert').taler,
    g && before && `${pl(before, 'Anna').taler}→${pl(g, 'Anna').taler} ${pl(before, 'Bert').taler}→${pl(g, 'Bert').taler}`);
  check('Ansage „gewinnt das Duell“', sees(anna, /Anna gewinnt das Duell – 8 Taler/), anna.chats.slice(-5).join(' | '));
  const done = await phase((x) => x.phase === 'end', 300000);
  check('Bergduell vorbei', done != null);
  check('Zurück auf den Festplatz', await waitFor(() => list.every(inHub), 30000));
  const log = calls().filter((c) => c.t === 'stats' && c.add && c.add.games_bergduell === 1);
  check('Statistik: games_bergduell', log.length === 2, String(log.length));
}

/** Bergsprint: diese Leute nacheinander ins Ziel bringen (die anderen bleiben unten). */
async function sprint(order) {
  const g = await waitMini('bergsprint', 40000);
  if (!g) return null;
  const [mx, my, mz] = g.miniAt;
  for (const [i, b] of order.entries()) {
    await sleep(700);
    await tpTo(b, mx + 0.5 - 2 + i * 2, my + 11, mz + 31.5);
  }
  await waitMiniEnd(60000);
  return g;
}

async function huette() {
  console.log('\n— Partie 12: Hüttenaufstieg (2 Leben) zu viert —');
  if (!bots.Dora) bots.Dora = await join('Dora');
  bots.Dora.policy = defaultPolicy;
  const { Anna: anna, Bert: bert, Carl: carl, Dora: dora } = bots;
  const list = [anna, bert, carl, dora];
  let g = await startWith(list, MODE.huette);
  await cmd('mparty minispiel bergsprint');
  g = await game();
  check('Hüttenaufstieg startet ohne Brett', g && g.mode === 'huette' && g.players.length === 4, g && g.mode);
  check('Ansage mit Leben (de/en)', await waitFor(() => sees(anna, /Jeder hat 2 ❤/) && sees(bert, /Everyone has 2 ❤/), 5000));
  const lives = async () => Object.fromEntries((await game())?.players.map((p) => [p.name, p.lives]) ?? []);
  // 1: Anna, Bert oben – Carl und Dora verlieren ein Leben
  await sprint([anna, bert]);
  await cmd('mparty minispiel holzhacker');
  let l = await lives();
  check('Untere Hälfte verliert ein Leben', l.Anna === 2 && l.Bert === 2 && l.Carl === 1 && l.Dora === 1, JSON.stringify(l));
  check('Ansage „verliert ein Leben – ❤♡“', sees(anna, /Carl verliert ein Leben – ❤♡/), anna.chats.filter((c) => /Leben/.test(c)).join(' | '));
  check('Ergebnis: −1 ❤ / sicher', resultLines3(anna).some((l) => /Carl .*−1 ❤/.test(l)) && resultLines3(anna).some((l) => /Anna .*sicher/.test(l)),
    resultLines3(anna).join(' | '));
  // 2: Holzhacker, niemand hackt → alle gleich, niemand verliert
  g = await waitMini('holzhacker', 40000);
  await cmd('mparty minispiel bergsprint');
  await waitMiniEnd(90000);
  l = await lives();
  check('Gleichstand: niemand verliert', l.Anna === 2 && l.Carl === 1 && l.Dora === 1, JSON.stringify(l));
  // 3: Anna, Carl oben – Dora fliegt raus, Bert verliert eins
  await sprint([anna, carl]);
  await cmd('mparty minispiel bergsprint');
  l = await lives();
  g = await game();
  check('Dora ist raus, Bert hat noch eins', l.Dora === 0 && l.Bert === 1 && pl(g, 'Dora').out, JSON.stringify(l));
  check('Ansage: kein Leben mehr, schaut zu', sees(anna, /Dora hat kein Leben mehr/));
  check('Dora schaut zu (Zuschauermodus)', await waitFor(() => dora.game?.gameMode === 'spectator', 5000), dora.game?.gameMode);
  // 4: nur noch drei – Anna, Bert oben, Carl raus
  g = await waitMini('bergsprint', 40000);
  check('Dora spielt nicht mehr mit', g && g.miniPlayers === 3, g && String(g.miniPlayers));
  await sprint([anna, bert]);
  await cmd('mparty minispiel bergsprint');
  l = await lives();
  check('Carl ist raus', l.Carl === 0, JSON.stringify(l));
  // 5: Anna gegen Bert – Bert raus, Anna erreicht die Hütte
  await sprint([anna]);
  const done = await phase((x) => x.phase === 'end', 30000);
  check('Hüttenaufstieg vorbei, als nur noch eine übrig ist', done != null);
  check('Siegerehrung mit Herzen', await waitFor(() => sees(anna, /^1\. Anna ❤/) && sees(anna, /^2\. Bert/), 5000),
    anna.chats.filter((c) => /^\d\. /.test(c)).join(' | '));
  check('Zurück auf den Festplatz', await waitFor(() => list.every(inHub), 30000));
  const log = calls().filter((c) => c.t === 'stats' && c.add && c.add.games_huette === 1);
  check('Statistik: games_huette für alle vier, Anna gewinnt', log.length === 4 && log.find((c) => c.name === 'Anna')?.add.wins_huette === 1,
    String(log.length));
}

/** Ergebniszeilen im Hüttenaufstieg. */
const resultLines3 = (bot) => bot.chats.filter((c) => /^\d+\. \S+ .*(❤|sicher|safe)$/.test(c));

/** Tauziehen im Turnier: wer auf Seite a steht, zieht (oder niemand). Gibt den Zustand zu Beginn zurück. */
async function tug(pull) {
  const g = await waitMini('tauziehen', 60000);
  if (!g) return null;
  const puller = bots[g.sideA];
  let on = pull;
  const job = (async () => { while (on) { puller.swingArm(); await sleep(80); } })();
  await waitMiniEnd(45000);
  on = false;
  await job;
  return g;
}

async function turnier() {
  console.log('\n— Partie 13: Gipfel-Turnier zu viert —');
  if (!bots.Dora) bots.Dora = await join('Dora');
  bots.Dora.policy = defaultPolicy;
  const { Anna: anna, Bert: bert, Carl: carl, Dora: dora } = bots;
  const list = [anna, bert, carl, dora];
  clear(...list);
  for (const b of list) {
    let w = await openWithItem(b, 2);
    if (w) await click(b, 15);
    w = await openWithItem(b, 3);
    if (w) await click(b, MODE.turnier);
  }
  let g = await phase(() => true, 30000);
  await cmd('mparty minispiel tauziehen');
  check('Gipfel-Turnier startet ohne Brett', g && g.mode === 'turnier' && g.players.length === 4, g && g.mode);
  // Halbfinale 1: erst unentschieden (niemand zieht) → neues Duell
  let m1 = await tug(false);
  check('Halbfinale: Duell zu zweit, die anderen schauen zu', m1 && m1.format === 'duel' && m1.miniPlayers === 2, m1 && m1.format);
  check('Ansage Halbfinale (de/en)', sees(anna, /Halbfinale – \S+ gegen \S+/) && sees(bert, /Semi-final – \S+ vs \S+/));
  await cmd('mparty minispiel tauziehen');
  check('Unentschieden → neues Duell', await waitFor(() => sees(anna, /es gibt ein neues Duell/), 8000));
  m1 = await tug(true);
  check('Sieger kommt weiter', await waitFor(() => sees(anna, new RegExp(`${m1?.sideA} kommt weiter, ${m1?.sideB} ist raus`)), 6000),
    anna.chats.slice(-4).join(' | '));
  const watcher = list.find((b) => b.username !== m1.sideA && b.username !== m1.sideB);
  check('Wer wartet, schaut im Zuschauermodus zu', watcher.game?.gameMode === 'spectator', watcher.game?.gameMode);
  const boards = Object.values(watcher.entities).filter((e) => e.name === 'text_display');
  check('Turnierbaum als Tafel', boards.length >= 1, String(boards.length));
  // Halbfinale 2
  await cmd('mparty minispiel tauziehen');
  const m2 = await tug(true);
  // Finale
  await cmd('mparty minispiel tauziehen');
  const fin = await tug(true);
  check('Finale zwischen den Siegern', fin && [m1.sideA, m2.sideA].includes(fin.sideA) && [m1.sideA, m2.sideA].includes(fin.sideB),
    fin && `${fin.sideA} gegen ${fin.sideB}`);
  check('Ansage Turniersieg', await waitFor(() => sees(anna, new RegExp(`★ ${fin.sideA} gewinnt das Gipfel-Turnier! ★`)), 6000));
  const done = await phase((x) => x.phase === 'end', 30000);
  check('Turnier vorbei', done != null);
  check('Siegerehrung: Sieg, Finale, Halbfinale', await waitFor(() => sees(anna, new RegExp(`^1\\. ${fin.sideA} – Turniersieg`)) &&
    sees(anna, new RegExp(`^2\\. ${fin.sideB} – raus im Finale`)) && anna.chats.filter((c) => /^3\. \S+ – raus im Halbfinale/.test(c)).length === 2, 6000),
    anna.chats.filter((c) => /^\d\. /.test(c)).slice(-4).join(' | '));
  check('Englisch: „Cup winner“', sees(bert, new RegExp(`^1\\. ${fin.sideA} – Cup winner`)));
  check('Zurück auf den Festplatz', await waitFor(() => list.every(inHub), 30000));
  const log = calls().filter((c) => c.t === 'stats' && c.add && c.add.games_turnier === 1);
  check('Statistik: games_turnier, Sieger wins_turnier', log.length === 4 && log.find((c) => c.name === fin.sideA)?.add.wins_turnier === 1,
    String(log.length));
}

/**
 * Auf den Wurm einschlagen: die Stellen, an denen man gerade treffen kann, meldet der Server im Zustand (mineflayer
 * kennt die Lage teleportierter Klickflächen nicht genau) – hin, hinschauen, auf alle Klickflächen schlagen (nur die in
 * Reichweite zählt).
 */
async function hitWorm(bot, g, until, ms) {
  const [ox, oy, oz] = g.miniAt;
  const end = Date.now() + ms;
  while (Date.now() < end && !(await until())) {
    const st = await game();
    for (const [tx, ty, tz] of st?.targets ?? []) {
      await tpTo(bot, ox + tx + 1.6, oy + 1, oz + tz);
      await sleep(200);
      await bot.lookAt(new (bot.entity.position.constructor)(ox + tx, oy + ty + 0.8, oz + tz), true);
      const inter = Object.values(bot.entities).filter((e) => e.name === 'interaction');
      if (process.env.DEBUG) console.log('wurm-ziel', tx, ty, tz, 'klickflächen', inter.length, bot.game.gameMode);
      for (const e of inter) bot.attack(e);
      await sleep(350);
    }
    await sleep(200);
  }
}

async function tatzelwurm() {
  console.log('\n— Partie 14: Der Tatzelwurm, Anna und Bert zusammen —');
  const { Anna: anna, Bert: bert } = bots;
  const list = [anna, bert];
  let g = await startWith(list, MODE.wurm);
  check('Tatzelwurm startet', g && g.mode === 'tatzelwurm' && g.players.length === 2, g && g.mode);
  g = await waitMini('tatzelwurm', 40000);
  check('Kampf läuft, Leben 18 + 2 × 12', g && g.wurmHp === 42, g && String(g.wurmHp));
  check('Abschnitt 1 angesagt (de/en)', await waitFor(() => sawTitle(anna, /Aus dem Boden/) && sawTitle(bert, /Out of the ground/), 5000));
  await hitWorm(anna, g, async () => ((await game())?.wurmHp ?? 42) <= 38, 40000);
  g = await game();
  check('Treffer am Kopf zählen doppelt', g && g.wurmHp <= 38 && g.wurmHp % 2 === 0, g && String(g.wurmHp));
  check('Felsbrocken: jemand wird benommen', await waitFor(() => sawTitle(anna, /Benommen/) || sawTitle(bert, /Stunned/), 25000));
  await cmd('mparty wurm 26');
  check('Abschnitt 2: die Jagd', await waitFor(() => sawTitle(anna, /Die Jagd/), 5000));
  await hitWorm(anna, g, async () => ((await game())?.wurmHp ?? 26) <= 23, 30000);
  g = await game();
  check('Leuchtende Schuppen treffen', g && g.wurmHp <= 23, g && String(g.wurmHp));
    await cmd('mparty wurm 4');
  check('Abschnitt 3: der Zorn', await waitFor(() => sawTitle(anna, /Der Zorn/), 5000));
  await hitWorm(anna, g, async () => (await game())?.phase !== 'mini', 60000);
  check('Tatzelwurm besiegt', await waitFor(() => sees(anna, /der Tatzelwurm ist besiegt/), 8000), anna.chats.slice(-4).join(' | '));
  check('Gemeinsames Ergebnis mit Sternen (de/en)', await waitFor(() => sees(anna, /Geschafft! Der Tatzelwurm ist besiegt – in \d+:\d\d, \d+ mal benommen: [★☆]{3}/) &&
    sees(bert, /Done! The Tatzelwurm is beaten/), 8000), anna.chats.slice(-3).join(' | '));
  const done = await phase((x) => x.phase === 'end', 20000);
  check('Siegerehrung mit Treffern', done != null && await waitFor(() => sees(anna, /^Anna – \d+ Treffer/) && sees(bert, /^Bert – \d+ hits?/), 5000),
    anna.chats.filter((c) => /Treffer/.test(c)).join(' | '));
  check('Beide gewinnen zusammen', sawTitle(anna, /Gewonnen/) && sawTitle(bert, /Won|You won/), `${anna.titles.slice(-2)} / ${bert.titles.slice(-2)}`);
  check('Zurück auf den Festplatz', await waitFor(() => list.every(inHub), 30000));
  const log = calls().filter((c) => c.t === 'stats' && c.add && c.add.games_tatzelwurm === 1);
  check('Statistik: games/wins_tatzelwurm', log.length === 2 && log.every((c) => c.add.wins_tatzelwurm === 1), String(log.length));
}

/** Eine Edelweiß-Jagd (2 Runden) zu dritt auf einem bestimmten Brett starten (Brett-Menü: Platz {@code boardSlot}). */
async function boardGame(list, boardSlot) {
  clear(...list);
  for (const b of list) {
    let w = await openWithItem(b, 2);
    if (w) await click(b, boardSlot);
    w = await openWithItem(b, 3);
    if (w) await click(b, MODE.jagd2);
  }
  return phase((x) => x.phase === 'turn', 40000);
}

/** Den aktuellen Zug so spielen, dass er genau auf Feld {@code target} landet. */
async function landOn(g, target) {
  await cmd(`mparty feld ${g.current} ${target - 1}`, `mparty wuerfel ${g.current} 1`);
  await sleep(300);
  const bot = bots[g.current];
  await waitFor(() => slot(bot, 0)?.name === 'white_concrete', 5000);
  await use(bot, 0);
}

async function newBoards() {
  console.log('\n— Partie 15: die drei neuen Bretter zu dritt —');
  const { Anna: anna, Bert: bert, Carl: carl } = bots;
  const list = [anna, bert, carl];

  // ---- Königssee: Fähre
  let g = await boardGame(list, 20);
  check('Königssee: Partie startet', g && g.board === 'koenigssee', g && g.board);
  const ferry = fieldIndex(g, 'F', 0);
  let who = g.current;
  await landOn(g, ferry);
  check('Königssee: Fähre im Chat (de/en)', await waitFor(() => sees(anna, new RegExp(`${who} nimmt die Fähre – quer über den See, \\d+ Felder voraus`)) &&
    sees(bert, /takes the ferry/), 8000), anna.chats.slice(-3).join(' | '));
  g = await phase((x) => x.current !== who || x.phase !== 'turn', 15000);
  await sleep(3500);
  g = await game();
  check('Königssee: mindestens 6 Felder voraus auf einem einfachen Feld', g && pl(g, who).pos >= ferry + 6 && 'TVGP'.includes(g.fields[pl(g, who).pos]),
    g && `${ferry} → ${pl(g, who).pos} (${g.fields[pl(g, who).pos]})`);
  await cmd('mparty stopp');
  await waitFor(() => list.every(inHub), 30000);

  // ---- Wiesn: Riesenrad und Fahrgeschäft
  g = await boardGame(list, 22);
  check('Wiesn: Partie startet', g && g.board === 'wiesn', g && g.board);
  const wheel = fieldIndex(g, 'R', 0);
  who = g.current;
  const others = g.players.filter((p) => p.name !== who).map((p) => p.name);
  await cmd(`mparty feld ${others[0]} 30`, `mparty feld ${others[1]} 30`);
  await landOn(g, wheel);
  check('Wiesn: Riesenrad tauscht Plätze (de/en)', await waitFor(() => sees(anna, new RegExp(`${who} fährt Riesenrad und tauscht oben den Platz mit`)) &&
    sees(bert, /rides the Ferris wheel/), 8000), anna.chats.slice(-3).join(' | '));
  await sleep(2500);
  g = await game();
  check('Wiesn: nach dem Tausch auf Feld 30', g && pl(g, who).pos === 30 && others.some((n) => pl(g, n).pos === wheel),
    g && JSON.stringify(g.players.map((p) => `${p.name}:${p.pos}`)));
  g = await phase((x) => x.phase === 'turn' && x.current !== who, 20000);
  who = g.current;
  const ride = fieldIndex(g, 'K', 0);
  await landOn(g, ride);
  check('Wiesn: Fahrgeschäft (Achterbahn, Autoscooter, Kettenkarussell oder Losbude)',
    await waitFor(() => sees(anna, new RegExp(`${who} (fährt Achterbahn|rempelt im Autoscooter|fliegt im Kettenkarussell|zieht an der Losbude)`)), 8000),
    anna.chats.slice(-3).join(' | '));
  await cmd('mparty stopp');
  await waitFor(() => list.every(inHub), 30000);

  // ---- Wintersonnwend: Eis und Lawine
  g = await boardGame(list, 24);
  check('Wintersonnwend: Partie startet', g && g.board === 'wintersonnwend', g && g.board);
  const ice = fieldIndex(g, 'E', 0);
  let iceEnd = ice;
  while (g.fields[iceEnd] === 'E') iceEnd++;
  who = g.current;
  await landOn(g, ice);
  check('Wintersonnwend: Eis – rutscht weiter (de/en)', await waitFor(() => sees(anna, new RegExp(`${who} rutscht übers Eis`)) &&
    sees(bert, /slides across the ice/), 8000), anna.chats.slice(-3).join(' | '));
  g = await phase((x) => x.phase === 'turn' && x.current !== who, 20000);
  const afterIce = pl(await game(), who).pos;
  check('Wintersonnwend: bis ans Ende der Eisfläche', afterIce === iceEnd, `${ice} → ${afterIce} (Ende ${iceEnd})`);
  who = g.current;
  const aval = fieldIndex(g, 'A', 2);
  const victim = g.players.find((p) => p.name !== who).name;
  await cmd(`mparty feld ${victim} ${aval + 2}`);
  await landOn(g, aval);
  check('Wintersonnwend: Lawine (de/en)', await waitFor(() => sees(anna, new RegExp(`${who} tritt eine Lawine los! Zurück um 3: .*${victim}`)) &&
    sees(bert, /sets off an avalanche/), 8000), anna.chats.slice(-3).join(' | '));
  g = await phase((x) => x.phase === 'turn' && x.current !== who, 25000);
  await sleep(1000);
  g = await game();
  check('Wintersonnwend: beide rutschen 3 zurück', g && pl(g, who).pos === aval - 3 && pl(g, victim).pos === aval - 1,
    g && JSON.stringify(g.players.map((p) => `${p.name}:${p.pos}`)));
  await cmd('mparty stopp');
  check('Neue Bretter: zurück auf den Festplatz', await waitFor(() => list.every(inHub), 30000));
}

/** Ergebniszeilen im Marathon („… → +10 Punkte“). */
const resultLines2 = (bot) => bot.chats.filter((c) => /^\d+\. \S+ .*(Punkte?|points?)$/.test(c));

async function main() {
  await join('Anna');
  await join('Bert');
  await join('Carl');
  await join('Dora');
  for (const b of Object.values(bots)) b.policy = defaultPolicy;
  if (PARTS.includes(1)) await part1();
  if (PARTS.includes(2)) await part2();
  if (PARTS.includes(3)) await part3();
  if (PARTS.includes(4)) await newMinis('Partie 4: neue Minispiele, Paket 1', PAKET1);
  if (PARTS.includes(5)) await newMinis('Partie 5: neue Minispiele, Paket 2', PAKET2);
  if (PARTS.includes(6)) await newMinis('Partie 6: neue Minispiele, Paket 3', PAKET3);
  if (PARTS.includes(7)) await marathon();
  if (PARTS.includes(8)) await teams();
  if (PARTS.includes(9)) await seilschaft();
  if (PARTS.includes(10)) await schnell();
  if (PARTS.includes(11)) await bergduell();
  if (PARTS.includes(12)) await huette();
  if (PARTS.includes(13)) await turnier();
  if (PARTS.includes(14)) await tatzelwurm();
  if (PARTS.includes(15)) await newBoards();
  for (const b of Object.values(bots)) b.quit();
  await sleep(500);
  const failed = results.filter((r) => !r.ok).length;
  console.log(`\n${results.length - failed}/${results.length} bestanden`);
  process.exit(failed ? 1 : 0);
}

setTimeout(() => {
  console.log('Zeit abgelaufen');
  console.log(`${results.filter((r) => r.ok).length}/${results.length} bis hierhin bestanden`);
  process.exit(2);
}, 3_600_000);

main().catch((e) => {
  console.error(e);
  console.log(`${results.filter((r) => r.ok).length}/${results.length} bis hierhin bestanden`);
  process.exit(1);
});
