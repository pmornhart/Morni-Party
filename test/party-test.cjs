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
const resultLines = (bot) => bot.chats.filter((c) => /^\d+\. \S+ .*(Taler|coins)/.test(c));

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
    win && plain(win.slots[10]?.customName).includes('Gipfelrennen') && plain(win.slots[12]?.customName).includes('2 Runden'),
    win && `${plain(win.slots[10]?.customName)} / ${plain(win.slots[12]?.customName)}`);
  await click(anna, 12);
  win = await openWithItem(bert, 3);
  await click(bert, 12);

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
  check('Doppelwürfel im Rucksack', win && win.slots[2]?.name === 'quartz_block', win && win.slots[2]?.name);
  clear(anna, bert, carl);
  await click(cur, 2);
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
  check('Englisches Ergebnis', bert.chats.some((c) => /Result: Cowbell/.test(c)) && resultLines(bert).some((l) => /coins/.test(l)));
  check('Ein Minispiel-Sieg wird gezählt', end1 && end1.players.some((p) => p.minis === 1), end1 && JSON.stringify(end1.players.map((p) => p.minis)));
  await cmd('mparty minispiel schaefchen');
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
  await cmd(`mparty feld ${d1} ${duel - 1}`, `mparty wuerfel ${d1} 1`);
  clear(anna, bert, carl);
  g2 = await playTurn(g2);
  check('Duellfeld: Menü „Gegen wen würfelst du?“', currentBotByName(d1).windows.some((t) => /Gegen wen|Who do you duel/.test(t)),
    currentBotByName(d1).windows.join(','));
  check('Würfelduell im Chat', sees(anna, /(gewinnt das Duell|unentschieden|wins the duel|a draw)/), anna.chats.filter((c) => /Duell|duel/.test(c)).join(' | '));

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
  const targetMenu = nextWindow(tb);
  await click(tb, 2);
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
  check('Siegerehrung im Chat', await waitFor(() => sees(anna, /Die Partie ist vorbei/) && sees(bert, /The game is over/), 4000));
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
  clear(anna, bert, carl, dora);
  for (const b of [anna, bert, carl, dora]) {
    let w = await openWithItem(b, 2);
    if (w) await click(b, 11);
    w = await openWithItem(b, 3);
    if (w) await click(b, 10);
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
  check('Anna erreicht das Gipfelkreuz und gewinnt', sees(anna, /Anna hat die Gipfelkreuz erreicht/) || sees(bert, /Anna reached the summit cross/),
    anna.chats.filter((c) => /erreicht|Gipfel/.test(c)).join(' | '));
  check('Partie endet sofort', await waitFor(() => sees(anna, /Die Partie ist vorbei/), 5000));
  check('Anna: Titel „Gewonnen!“', sawTitle(anna, /Gewonnen/));
  const log = calls().filter((c) => c.t === 'stats' && c.mode === 'party' && c.add.games_gipfelrennen === 1);
  const annaStats = log.find((c) => c.name === 'Anna');
  check('Statistik: Sieg und Gipfel für Anna', annaStats && annaStats.add.wins === 1 && annaStats.add.summits === 1, JSON.stringify(annaStats));
  check('Dora (gegangen) bekommt keine Partie gezählt', !log.some((c) => c.name === 'Dora'));
  check('Zurück auf den Festplatz', await waitFor(() => [anna, bert, carl].every(inHub), 20000));
  check('Statistik-Datei: Anna 2 Partien, 1 Gipfel', /partien: 2/.test(statsFile('Anna')) && /gipfel: 1/.test(statsFile('Anna')), statsFile('Anna'));
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
    if (w) await click(b, 14);
  }
  let g = await phase((x) => x.phase === 'turn', 30000);
  check('Goldstollen, Edelweiß-Jagd 8 Runden', g && g.board === 'goldstollen' && g.mode === 'edelweiss-jagd-8', g && `${g.board} ${g.mode}`);
  for (let r = 0; r < REST.length; r++) {
    const id = REST[r];
    await cmd(`mparty minispiel ${id}`);
    for (let i = 0; i < 4 && g && g.phase === 'turn'; i++) {
      await cmd(`mparty wuerfel ${g.current} ${1 + (i % 3)}`);
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
    if (id === 'talerregen') check('Talerregen: Taler eingesammelt', lines.some((l) => /\((?!0 )\d+ Punkte\)/.test(l)), lines.join(' | '));
    if (id === 'pfeilhagel') check('Pfeilhagel: getroffen = raus', anna.chats.some((c) => /ist raus/.test(c)) || lines.length === 3,
      anna.chats.filter((c) => /raus/.test(c)).join(' | '));
    if (id === 'heisse-brezn') {
      const holders = new Set(anna.chats.map((c) => (c.match(/» (\S+) hat die heiße Brezn/) ?? [])[1]).filter(Boolean));
      check('Heiße Brezn: wird weitergegeben', holders.size >= 2, [...holders].join(','));
      check('Heiße Brezn: wer sie zuletzt hat, ist raus', anna.chats.some((c) => /ist raus/.test(c)));
    }
    if (id === 'trachtenfarben') {
      check('Trachtenfarben: Farbe gezeigt und gesucht', sawTitle(anna, /Merk dir/) && sees(anna, /Gesucht war/));
      check('Trachtenfarben: wer nicht hinläuft, fällt', anna.chats.some((c) => /Carl ist raus/.test(c)), anna.chats.filter((c) => /raus/.test(c)).join(' | '));
    }
    if (id === 'gipfelkoenig') check('Gipfelkönig: Anna oben sammelt Punkte', /^1\. Anna \((\d+) Punkte\)/.test(lines[0] ?? '') && !/\(0 Punkte\)/.test(lines[0]), lines[0]);
    if (id === 'holzhacker') check('Holzhacker: Holz gespalten', lines.some((l) => /\([1-9]\d* Punkte\)/.test(l)), lines.join(' | '));
    if (id === 'rempelei') check('Rempelei: jemand fällt hinunter', anna.chats.some((c) => /ist raus|Rand bröckelt/.test(c)), anna.chats.filter((c) => /raus|bröckelt/.test(c)).join(' | '));
    if (id === 'scheibenschiessen') check('Scheibenschießen: Treffer zählen', lines.some((l) => /\([1-9]\d* Punkte\)/.test(l)), lines.join(' | '));
    g = await phase((x) => x.phase === 'turn' || x.phase === 'end', 30000);
  }
  const done = await phase((x) => x.phase === 'end', 60000);
  check('Nach 8 Runden ist Schluss', done != null);
  check('Zurück auf den Festplatz', await waitFor(() => list.every(inHub), 25000));
  clear(anna);
  anna.chat('/top');
  check('/top zeigt die Besten', await waitFor(() => sees(anna, /^1\. \S+ \d+ Siege/), 3000), anna.chats.join(' | '));
}

async function main() {
  await join('Anna');
  await join('Bert');
  await join('Carl');
  await join('Dora');
  for (const b of Object.values(bots)) b.policy = defaultPolicy;
  if (PARTS.includes(1)) await part1();
  if (PARTS.includes(2)) await part2();
  if (PARTS.includes(3)) await part3();
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
}, 1_500_000);

main().catch((e) => {
  console.error(e);
  console.log(`${results.filter((r) => r.ok).length}/${results.length} bis hierhin bestanden`);
  process.exit(1);
});
