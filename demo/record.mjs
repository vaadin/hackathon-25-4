// Records the two demo walks as video, one file each, from the plan in PLAN.md.
//
// Not a test: it drives the application the way the presenter will, with pauses
// where a viewer needs to read, and it never presses Open in GitHub.
//
//   node record.mjs [customer|team]
//
// The app must be on 8090 against the fixtures, with the dev tools off.
//
// Two things the browser does not record by itself are added to the page while
// it runs: a caption that says what is being done, and a cursor. A headless
// browser draws no pointer, so a viewer sees fields fill and panels open with
// nothing causing it; the cursor below follows the real mouse events Playwright
// sends, so what the video shows is where the pointer actually was.
import { readFileSync } from 'node:fs';
import { chromium } from 'playwright';

const BASE = 'http://localhost:8090';
const OUT = new URL('./video/', import.meta.url).pathname;
const SIZE = { width: 1600, height: 900 };

// A beat a viewer can read in. Short for a click landing, long for a screenful of prose.
const beat = (ms) => new Promise((r) => setTimeout(r, ms));
const READ = 3600;
const GLANCE = 1600;

// The cursor and the caption, installed on every document the walk opens. It is a file of its
// own rather than a string in here, because a script inside a template literal has to escape its
// own backticks and the first version of it shipped a syntax error the page swallowed in silence.
const OVERLAY = readFileSync(new URL('./overlay.js', import.meta.url), 'utf8');

/** What the viewer is told this step is. Survives a navigation: the overlay is reinstalled. */
async function say(page, text) {
  await page.evaluate((t) => {
    if (!window.__demo) {
      throw new Error('the overlay is not installed, so the video would carry no caption');
    }
    window.__demo.say(t);
  }, text);
}

/** Moves the pointer there before clicking, so the video shows the hand reaching for it. */
async function clickVisibly(page, locator) {
  const target = locator.first();
  await target.scrollIntoViewIfNeeded().catch(() => {});
  const box = await target.boundingBox();
  if (!box) {
    await target.click();
    return;
  }
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2, { steps: 24 });
  await beat(260);
  await page.mouse.down();
  await beat(90);
  await page.mouse.up();
}

async function goto(page, path, caption) {
  await page.goto(`${BASE}${path}`);
  if (caption) {
    await say(page, caption);
  }
}

async function signIn(page, who, caption) {
  await goto(page, '/login');
  await say(page, caption);
  const user = page.locator('vaadin-login-form input[name=username]');
  await clickVisibly(page, user);
  await user.pressSequentially(who, { delay: 90 });
  const password = page.locator('vaadin-login-form input[name=password]');
  await clickVisibly(page, password);
  await password.pressSequentially(who, { delay: 90 });
  await beat(500);
  await clickVisibly(page, page.locator('vaadin-login-form vaadin-button[slot=submit]'));
  await page.waitForURL((url) => !url.pathname.includes('/login'));
}

async function typeSlowly(page, locator, text, delay = 55) {
  await clickVisibly(page, locator);
  await locator.pressSequentially(text, { delay });
}

async function customer(page) {
  await signIn(page, 'guest', 'Signing in as a customer');

  await goto(page, '/', 'Every supported version at a glance, with the state of its newest release');
  await beat(READ + 1400);

  await say(page, 'A customer asks about the version they run');
  const version = page.locator('vaadin-combo-box input').first();
  await typeSlowly(page, version, '24.9.21', 110);
  await beat(GLANCE);
  // Enter, never Escape: Escape in this field withdraws the question and goes home, which is
  // the behaviour the specification asks for and exactly what it did to the first take.
  await page.keyboard.press('Enter');
  await clickVisibly(page, page.locator('vaadin-button:has-text("Check")'));
  await page.waitForURL(/\/version\/24\.9\.21/);
  await say(page, 'Fix available: one maintenance release clears all twelve findings');
  await beat(READ);
  await say(page, 'What the upgrade fixes, advisory by advisory');
  await page.mouse.wheel(0, 700);
  await beat(READ);
  await say(page, 'Spring Boot findings are named as Spring Boot’s, never as Vaadin’s');
  await page.mouse.wheel(0, 900);
  await beat(READ);
  await page.mouse.wheel(0, -1600);
  await beat(GLANCE);

  await goto(page, '/cve/CVE-2026-45292,CVE-2026-102276,CVE-2026-8763,CVE-2021-33604,CVE-2019-25028/24.9.21',
    'Five advisories pasted at once, answered for this version');
  await beat(READ);
  await say(page, 'Each one gets its own answer, and the reader’s line is marked');
  await page.mouse.wheel(0, 800);
  await beat(READ);
  await page.mouse.wheel(0, 800);
  await beat(READ);

  await goto(page, '/scan', 'An auditor uploads the report their scanner produced');
  await beat(GLANCE);
  const chooser = page.waitForEvent('filechooser');
  await clickVisibly(page, page.locator('.lookup-report-choose'));
  (await chooser).setFiles(new URL('../src/test/resources/scan/osv-scanner.json', import.meta.url).pathname);
  await page.waitForURL(/\/scan\/24\.9\.21/);
  await say(page, 'One upgrade clears one of six, and the rest are named with their owner');
  await beat(READ + 1200);
  await say(page, 'Vaadin, Spring Boot, the library, or yours: who has to move');
  await page.mouse.wheel(0, 650);
  await beat(READ + 800);
  await say(page, 'The audit report, as a PDF, previewed before it is taken');
  await clickVisibly(page, page.locator('vaadin-button:has-text("Preview the audit report")'));
  await page.waitForSelector('vaadin-pdf-viewer', { timeout: 20000 }).catch(() => {});
  await beat(READ + 1400);
  await clickVisibly(page, page.locator('vaadin-button:has-text("Close")'));
  await beat(GLANCE);

  await goto(page, '/version/24.4.28', 'A release with nothing to upgrade to says so, and offers the way out');
  await beat(READ);
  await say(page, 'Light and dark, decided before the first paint');
  for (let i = 0; i < 2; i++) {
    await clickVisibly(page, page.locator('vaadin-button.theme-switch'));
    await beat(1100);
  }
  await beat(READ);
  await clickVisibly(page, page.locator('vaadin-button.theme-switch'));
  await say(page, '');
  await beat(GLANCE);
}

async function team(page) {
  await signIn(page, 'team', 'Signing in as the security team');

  await goto(page, '/findings', 'Every maintained line, measured, with what each product brings');
  await beat(READ + 1400);

  await say(page, 'Narrowing the list to the products an application actually uses');
  const chip = () => page.locator('a.explorer-chip', { hasText: 'kubernetes-kit-starter' }).first();
  await clickVisibly(page, chip());
  await beat(READ);
  await say(page, 'The selection is written into the address, so it can be sent to somebody');
  await beat(GLANCE);
  await clickVisibly(page, chip());
  await beat(GLANCE);

  await goto(page, '/findings?open=24.9', 'One line opened: what is ours, what is Spring Boot’s, what is answered');
  await beat(READ + 1000);
  await page.mouse.wheel(0, 600);
  await beat(READ);

  await goto(page, '/findings/workbench', 'The verdict queue, ranked by what one verdict would close');
  await beat(READ);
  await clickVisibly(page, page.locator('text=com.fasterxml.jackson.core:jackson-core').first());
  await say(page, 'Every route the package arrives by, and the version each line resolves');
  await beat(READ);
  await clickVisibly(page, page.locator('.workbench-fold-label', { hasText: /^Write a verdict$/ }));
  await say(page, 'A security report is pasted in, as it arrives by mail');
  await beat(GLANCE);
  await typeSlowly(page, page.locator('vaadin-text-area textarea').first(),
    'GHSA-h46c-h94j-95f3: jackson-core: ReDoS through quadratic backtracking in number parsing.'
    + ' Package: com.fasterxml.jackson.core:jackson-core (Maven). Patched: 2.21.7. Tracked as CVE-2026-89407.', 14);
  await beat(GLANCE);
  await say(page, 'The AI fills the fields it can support, and marks each one');
  await clickVisibly(page, page.locator('vaadin-button:has-text("Fill the form")'));
  await page.waitForSelector('[class*=marker]', { timeout: 60000 }).catch(() => {});
  await beat(READ + 1500);
  await say(page, 'The YAML follows the typing, and nothing is saved until a person says so');
  await page.mouse.wheel(0, 500);
  await beat(READ + 800);

  await goto(page, '/advisories', 'The whole catalogue, narrowed in plain words');
  await beat(GLANCE);
  await typeSlowly(page, page.locator('vaadin-text-area textarea').first(),
    'show the critical ones still open on 24.9', 45);
  await page.keyboard.press('Enter');
  await page.waitForFunction(() => /\d+ of \d+ advisories/.test(document.body.innerText), null, { timeout: 60000 })
    .catch(() => {});
  await say(page, 'Eighteen of five hundred and thirty five, worst first, and a way back');
  await beat(READ + 1200);

  await goto(page, '/findings', 'The assistant answers from the measurements, never from itself');
  await clickVisibly(page, page.locator('vaadin-button.assistant-toggle'));
  await beat(GLANCE);
  await typeSlowly(page, page.locator('.assistant-panel vaadin-message-input textarea'), 'Is 24.9.21 safe?', 60);
  await page.keyboard.press('Enter');
  await say(page, 'It names the file it read and the date it was scanned');
  await page.waitForFunction(() => {
    const items = document.querySelectorAll('.assistant-panel vaadin-message');
    return items.length >= 2 && !items[items.length - 1].textContent.includes('Typing');
  }, null, { timeout: 90000 }).catch(() => {});
  await beat(READ + 1800);

  await goto(page, '/diagnostics', 'And the board that counted this very walk, live');
  await beat(READ + 1800);
  await page.mouse.wheel(0, 600);
  await beat(READ + 1200);
  await say(page, '');
  await beat(900);
}

const which = process.argv[2] === 'team' ? 'team' : 'customer';
const browser = await chromium.launch({ headless: true });
const context = await browser.newContext({
  viewport: SIZE,
  recordVideo: { dir: OUT, size: SIZE },
});
await context.addInitScript(OVERLAY);
const page = await context.newPage();
// Somewhere to start from, so the first move is a move and not an appearance.
await page.mouse.move(SIZE.width / 2, SIZE.height - 120);
try {
  await (which === 'team' ? team(page) : customer(page));
} finally {
  await context.close();
  await browser.close();
  console.log(`${which}: written to ${OUT}`);
}
