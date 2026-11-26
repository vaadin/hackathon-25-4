// The deck's stills, each with the caption the video gives the same screen, written into
// shots-captioned/ beside the clean ones in shots/. Same overlay as record.mjs, so a slide and
// the video it sits next to say the same words.
//
//   node shots.mjs
//
// The app must be on 8090 against the fixtures, with the dev tools off.
import { readFileSync, writeFileSync } from 'node:fs';
import { mkdir } from 'node:fs/promises';
import { chromium } from 'playwright';

const BASE = 'http://localhost:8090';
const OUT = new URL('./shots-clean/', import.meta.url).pathname;
const CAPTIONS = new URL('./captions.json', import.meta.url).pathname;
const written = {};
const SIZE = { width: 1600, height: 900 };
const OVERLAY = readFileSync(new URL('./overlay.js', import.meta.url), 'utf8');
const REPORT = new URL('../src/test/resources/scan/osv-scanner.json', import.meta.url).pathname;

const settle = (ms = 900) => new Promise((r) => setTimeout(r, ms));

async function signIn(page, who) {
  await page.goto(`${BASE}/login`);
  await page.locator('vaadin-login-form input[name=username]').fill(who);
  const password = page.locator('vaadin-login-form input[name=password]');
  await password.fill(who);
  await password.press('Enter');
  await page.waitForURL((url) => !url.pathname.includes('/login'));
}

/** The still is clean. Its caption goes under it, in a band band.mjs adds, so it covers nothing. */
async function shoot(page, file, caption, { full = false } = {}) {
  await page.evaluate(() => {
    if (!window.__demo) {
      throw new Error('the overlay is not installed');
    }
    window.__demo.say('');
  });
  await settle(300);
  await page.screenshot({ path: `${OUT}${file}`, fullPage: full });
  written[file] = caption;
  process.stdout.write(`${file}\n`);
}

const customerShots = [
  ['C1-home.png', 'Every supported version at a glance', async (page) => {
    await page.goto(`${BASE}/`);
    await settle();
  }],
  ['C2-version-fix-available.png', 'Fix available: one maintenance release clears all twelve findings',
    async (page) => {
      await page.goto(`${BASE}/version/24.9.21`);
      await settle();
    }],
  ['C3-version-one-stuck.png', 'Nothing to upgrade to, so the finding and what to do are in the card',
    async (page) => {
      await page.goto(`${BASE}/version/24.7.19`);
      await settle();
    }],
  ['C4-cve-five-states.png', 'Five advisories pasted at once, each with its own answer', async (page) => {
    await page.goto(`${BASE}/cve/CVE-2026-45292,CVE-2026-102276,CVE-2026-8763,CVE-2021-33604,`
      + `CVE-2019-25028/24.9.21`);
    await settle();
  }],
  ['C5-scan-triage.png', 'A scanner report, triaged: who has to move, and what one upgrade clears',
    async (page) => {
      await page.goto(`${BASE}/scan`);
      const chooser = page.waitForEvent('filechooser');
      await page.locator('.lookup-report-choose').click();
      (await chooser).setFiles(REPORT);
      await page.waitForURL(/\/scan\/24\.9\.21/);
      await settle(1500);
    }],
  ['C6-pdf-preview.png', 'The audit report as a PDF, previewed before it is taken', async (page) => {
    await page.locator('vaadin-button:has-text("Preview the audit report")').click();
    await page.waitForSelector('vaadin-pdf-viewer', { timeout: 20000 }).catch(() => {});
    await settle(1500);
  }],
  ['C7-version-dark.png', 'Light and dark, decided before the first paint', async (page) => {
    await page.goto(`${BASE}/version/24.4.28`);
    await settle();
    for (let i = 0; i < 3; i++) {
      const scheme = await page.locator('vaadin-button.theme-switch').getAttribute('data-scheme');
      if (scheme === 'dark') {
        break;
      }
      await page.locator('vaadin-button.theme-switch').click();
      await settle(600);
    }
  }],
];

const teamShots = [
  ['T1-explorer.png', 'Every maintained line, and what each product brings to it', async (page) => {
    await page.goto(`${BASE}/findings`);
    await settle(1200);
  }],
  ['T2-explorer-open.png', 'One line opened: ours, Spring Boot’s, and what is already answered',
    async (page) => {
      await page.goto(`${BASE}/findings?open=24.9`);
      await settle(1200);
    }],
  ['T3-release.png', 'One release in full, with the files a machine can read', async (page) => {
    await page.goto(`${BASE}/findings/release/25.3/25.3.5`);
    await settle();
  }],
  ['T4-spring.png', 'What Spring Boot brings, release by release, with its supply status', async (page) => {
    await page.goto(`${BASE}/findings/spring`);
    await settle();
  }],
  ['T5-workbench-queue.png', 'The verdict queue, ranked by what one verdict would close', async (page) => {
    await page.goto(`${BASE}/findings/workbench`);
    await settle();
  }],
  ['T6-workbench-ai-fill.png', 'A pasted report fills the form, and the YAML follows the typing',
    async (page) => {
      await page.locator('text=com.fasterxml.jackson.core:jackson-core').first().click();
      await settle();
      await page.locator('.workbench-fold-label', { hasText: /^Write a verdict$/ }).click();
      await settle();
      await page.locator('vaadin-text-area textarea').first().fill(
        'GHSA-h46c-h94j-95f3: jackson-core: ReDoS through quadratic backtracking in number parsing.'
        + ' Package: com.fasterxml.jackson.core:jackson-core (Maven). Patched: 2.21.7.'
        + ' Tracked as CVE-2026-89407.');
      await page.locator('vaadin-button:has-text("Fill the form")').click();
      await page.waitForSelector('[class*=marker]', { timeout: 60000 }).catch(() => {});
      await settle(1500);
    }],
  ['T7-advisories-gridai.png', 'The catalogue narrowed in plain words, with a way back', async (page) => {
    await page.goto(`${BASE}/advisories`);
    await settle();
    await page.locator('vaadin-text-area textarea').first().fill('show the critical ones still open on 24.9');
    await page.keyboard.press('Enter');
    await page.waitForFunction(() => /\d+ of \d+ advisories/.test(document.body.innerText), null,
      { timeout: 60000 }).catch(() => {});
    await settle(1200);
  }],
  ['T9-assistant.png', 'The assistant answers from the measurements, and names what it read',
    async (page) => {
      await page.goto(`${BASE}/findings`);
      await page.locator('vaadin-button.assistant-toggle').click();
      await settle();
      await page.locator('.assistant-panel vaadin-message-input textarea').fill('Is 24.9.21 safe?');
      await page.keyboard.press('Enter');
      await page.waitForFunction(() => {
        const items = document.querySelectorAll('.assistant-panel vaadin-message');
        return items.length >= 2 && !items[items.length - 1].textContent.includes('Typing');
      }, null, { timeout: 90000 }).catch(() => {});
      await settle(1200);
    }],
  ['T8-diagnostics.png', 'The board that counted this very walk, live', async (page) => {
    await page.goto(`${BASE}/diagnostics`);
    await settle(1200);
  }],
];

await mkdir(OUT, { recursive: true });
const browser = await chromium.launch();
const context = await browser.newContext({ viewport: SIZE });
await context.addInitScript(OVERLAY);
const page = await context.newPage();
try {
  await signIn(page, 'guest');
  for (const [file, caption, prepare] of customerShots) {
    await prepare(page);
    await shoot(page, file, caption);
  }
  await page.goto(`${BASE}/`);
  await page.locator('.sign-out').click();
  await page.waitForURL(/\/login/);
  await signIn(page, 'team');
  for (const [file, caption, prepare] of teamShots) {
    await prepare(page);
    await shoot(page, file, caption);
  }
} finally {
  await context.close();
  await browser.close();
  writeFileSync(CAPTIONS, JSON.stringify(written, null, 2));
  console.log(`written to ${OUT}`);
}
