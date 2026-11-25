// Records a captioned walkthrough of the DocuShare demo on a phone screen (portrait, 780×1688,
// touch), for watching on phones. Captures sharp 2× JPEG frames (about 20 a second; Playwright's
// own video recording is 1× and blurry when scaled up to a phone screen), then, on
// macOS, encodes them to an H.264 MP4 with encode-mp4.swift (elsewhere it prints an ffmpeg command).
// Needs the app running with the demo profile (see DEVELOPMENT.md, "Demo video").
// Usage: BASE=http://localhost:8080 node record-demo-mobile.mjs <output-dir>
import { chromium } from 'playwright';
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const BASE = process.env.BASE ?? 'http://localhost:8080';
const OUT_DIR = path.resolve(process.argv[2] ?? 'demo-video');
const FRAMES = path.join(OUT_DIR, 'mobile-frames');
const W = 390, H = 844, SCALE = 2;
fs.mkdirSync(OUT_DIR, { recursive: true });
fs.rmSync(FRAMES, { recursive: true, force: true });
fs.mkdirSync(FRAMES);

// Caption bar and touch markers, re-created on every page load
const overlay = () => {
  const install = () => {
    if (document.getElementById('demo-caption')) return;
    const style = document.createElement('style');
    style.textContent = `
      #demo-caption { position: fixed; left: 12px; right: 12px; bottom: 22px; padding: 12px 16px;
        border-radius: 14px; z-index: 2147483647; background: rgba(17, 24, 39, .9); color: #fff;
        font: 600 17px/1.35 system-ui, sans-serif; text-align: center; box-shadow: 0 8px 24px rgba(0,0,0,.3);
        transition: opacity .25s; pointer-events: none; }
      #demo-caption:empty { opacity: 0; }
      #demo-title { position: fixed; inset: 0; z-index: 2147483646; display: flex; flex-direction: column;
        align-items: center; justify-content: center; gap: 16px; padding: 0 28px; text-align: center;
        background: #1e3a8a; color: #fff; font-family: system-ui, sans-serif; transition: opacity .5s;
        pointer-events: none; }
      #demo-title h1 { margin: 0; font-size: 42px; font-weight: 700; color: #fff !important; }
      #demo-title p { margin: 0; font-size: 20px; line-height: 1.4; opacity: .88; color: #fff !important; }
      vaadin-dev-tools, copilot-main { display: none !important; }
      .demo-touch { position: fixed; width: 44px; height: 44px; margin: -22px 0 0 -22px; border-radius: 50%;
        background: rgba(37, 99, 235, .3); border: 3px solid rgba(37, 99, 235, .85); z-index: 2147483645;
        pointer-events: none; transition: transform .35s ease-out, opacity .35s ease-out; }
      .demo-touch.release { transform: scale(1.8); opacity: 0; }
      .demo-spot { position: fixed; border-radius: 12px; border: 3px solid #f59e0b; z-index: 2147483645;
        box-shadow: 0 0 0 4px rgba(245, 158, 11, .25); pointer-events: none; transition: opacity .3s; }`;
    document.documentElement.appendChild(style);
    const caption = document.createElement('div');
    caption.id = 'demo-caption';
    caption.textContent = sessionStorage.getItem('demo-caption') ?? '';
    document.documentElement.append(caption);
  };
  if (document.readyState === 'loading') addEventListener('DOMContentLoaded', install); else install();
};

const browser = await chromium.launch();
const context = await browser.newContext({
  viewport: { width: W, height: H }, deviceScaleFactor: SCALE, isMobile: true, hasTouch: true,
  acceptDownloads: true,
});
await context.addInitScript(overlay);
const page = await context.newPage();
const pause = (ms) => page.waitForTimeout(ms);

// Frame capture: screenshots in a loop, each stamped with when it was taken
// (paused while a page loads, so the previous frame stays on screen instead of a blank page)
const frames = [];
let capturing = true;
let holding = true;
const capture = (async () => {
  while (capturing) {
    if (holding) {
      await new Promise(r => setTimeout(r, 30));
      continue;
    }
    const t = Date.now() / 1000;
    try {
      const jpeg = await page.screenshot({ type: 'jpeg', quality: 90, caret: 'initial', timeout: 2000 });
      const file = `f${String(frames.length + 1).padStart(5, '0')}.jpg`;
      fs.writeFileSync(path.join(FRAMES, file), jpeg);
      frames.push({ file, t });
    } catch {
      await new Promise(r => setTimeout(r, 30)); // mid-navigation; try again
    }
  }
})();
async function caption(text, hold = 2800) {
  await page.evaluate(t => {
    sessionStorage.setItem('demo-caption', t);
    const el = document.getElementById('demo-caption');
    if (el) el.textContent = t;
  }, text);
  if (hold) await pause(hold);
}
async function title(heading, sub, hold = 3000) {
  await page.evaluate(([h, s]) => {
    const el = document.createElement('div');
    el.id = 'demo-title';
    el.innerHTML = `<h1></h1><p></p>`;
    el.querySelector('h1').textContent = h; el.querySelector('p').textContent = s;
    document.documentElement.appendChild(el);
  }, [heading, sub]);
  holding = false;
  await pause(hold);
  await page.evaluate(() => { const el = document.getElementById('demo-title'); el.style.opacity = '0'; });
  await pause(600);
  await page.evaluate(() => document.getElementById('demo-title')?.remove());
}
/** Scrolls the element into the middle of the screen, smoothly. */
async function reveal(locator) {
  await locator.evaluate(el => el.scrollIntoView({ behavior: 'smooth', block: 'center' }));
  await pause(700);
}
/** A visible finger press, then the tap itself. */
async function tap(locator, after = 1000) {
  await reveal(locator);
  const box = await locator.boundingBox();
  const x = box.x + box.width / 2, y = box.y + box.height / 2;
  await page.evaluate(([x, y]) => {
    const dot = document.createElement('div');
    dot.className = 'demo-touch';
    dot.style.left = x + 'px'; dot.style.top = y + 'px';
    document.documentElement.appendChild(dot);
    setTimeout(() => dot.classList.add('release'), 380);
    setTimeout(() => dot.remove(), 900);
  }, [x, y]);
  await pause(380);
  await page.touchscreen.tap(x, y);
  await pause(after);
}
async function type(locator, text) {
  await tap(locator, 250);
  await locator.pressSequentially(text, { delay: 85 });
  await pause(350);
}
/** An outline around an element, to point at it without tapping. */
async function spotlight(locator, hold) {
  await reveal(locator);
  const box = await locator.boundingBox();
  await page.evaluate(b => {
    const el = document.createElement('div');
    el.className = 'demo-spot';
    Object.assign(el.style, { left: b.x - 6 + 'px', top: b.y - 6 + 'px', width: b.width + 12 + 'px',
      height: b.height + 12 + 'px' });
    document.documentElement.appendChild(el);
  }, box);
  await pause(hold);
  await page.evaluate(() => document.querySelectorAll('.demo-spot').forEach(e => e.remove()));
}
async function scrollBy(dy, wait = 1400) {
  await page.evaluate(dy => window.scrollBy({ top: dy, behavior: 'smooth' }), dy);
  await pause(wait);
}
/** Runs a step that loads another page, without capturing the page while it loads. */
async function loading(step, readySelector, resume = true) {
  await step();
  holding = true;
  await page.waitForSelector(readySelector);
  await pause(500);
  holding = !resume;
}
/** Opens a page behind a title card: capture resumes once title() has covered it. */
async function open(url) {
  holding = true;
  await loading(() => page.goto(`${BASE}/${url}`), 'vaadin-login-form', false);
}
async function login(user, pass, readySelector) {
  await pause(700);
  await type(page.locator('vaadin-text-field[name="username"] input'), user);
  await type(page.locator('vaadin-password-field[name="password"] input'), pass);
  await loading(() => tap(page.locator('vaadin-button[slot="submit"]'), 0), readySelector);
  await pause(800);
}
const navItem = (label) => page.locator('vaadin-side-nav-item', { hasText: label });
const groupTitle = () => page.locator('.my-documents__group-title');

// ---------- Client ----------
await open('login');
await title('DocuShare', 'Your tax documents, on your phone', 3200);
await caption('Clients sign in with their PAN and password', 600);
await login('DEMOC1234D', 'DEMOC1234D@01042015', '.my-documents__row');

await caption('My Documents shows only this client\'s documents', 2600);
await spotlight(page.locator('vaadin-side-nav'), 0);
await caption('Four groups: Filings, Financials, Computations and Misc', 3000);
await page.evaluate(() => document.querySelectorAll('.demo-spot').forEach(e => e.remove()));

await caption('Tap a group to open it', 0);
await tap(navItem('Financials'), 1200);
await reveal(groupTitle());
await caption('Financials, by financial year, latest first', 2400);
await scrollBy(420, 1800);
await caption('Balance sheet, profit & loss and both audit reports for each year', 2200);
await scrollBy(520, 2200);
await scrollBy(-2000, 1200);

const actions = page.locator('.my-documents__actions').first();
await reveal(actions);
await caption('Every document has Download and Print', 0);
await spotlight(actions, 2600);
await caption('Download saves the PDF to the phone', 0);
const download = page.waitForEvent('download');
await tap(page.locator('.my-documents__download').first(), 600);
await (await download).saveAs(path.join(os.tmpdir(), 'docushare-demo-download.pdf'));
await pause(1600);
await caption('Print opens the document, ready to print or share', 0);
await spotlight(page.locator('.my-documents__print').first(), 2800);

await caption('Computations', 0);
await tap(navItem('Computations'), 900);
await reveal(groupTitle());
await pause(1500);
await caption('Filings: income tax return acknowledgements', 0);
await tap(navItem('Filings'), 900);
await reveal(groupTitle());
await pause(2000);
await caption('', 0);
await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'smooth' }));
await pause(800);
await loading(() => tap(page.locator('vaadin-button', { hasText: 'Logout' }), 0), 'vaadin-login-form', false);

// ---------- Admin ----------
await open('admin/login');
await title('For the consultant', 'Uploading documents, from the admin portal', 2800);
await caption('The consultant signs in to the admin portal', 600);
await login('admin', 'admin', '.admin-nav__item');
await caption('The dashboard links to every admin section', 2600);
await caption('Upload Documents', 0);
await tap(page.locator('.admin-nav__item', { hasText: 'Upload Documents' }), 1500);
await caption('Pick the client by PAN or name', 0);
const pan = page.locator('vaadin-combo-box').filter({ has: page.locator('label:text-is("PAN")') }).locator('input');
await type(pan, 'demo');
await pause(1500);
await tap(page.locator('vaadin-combo-box-item').first(), 1200);
await caption('The client\'s name confirms it\'s the right one', 2600);
await caption('', 0);

await title('DocuShare', 'Filings · Financials · Computations · Misc, by financial year', 3400);

const end = Date.now() / 1000;
capturing = false;
await capture;
await context.close();
await browser.close();

// How long each frame stays on screen: until the next one (the last one until the end)
const lines = ['ffconcat version 1.0'];
frames.forEach((f, i) => {
  const next = i + 1 < frames.length ? frames[i + 1].t : Math.max(end, f.t + 0.5);
  lines.push(`file '${f.file}'`, `duration ${(next - f.t).toFixed(4)}`);
});
lines.push(`file '${frames.at(-1).file}'`);
fs.writeFileSync(path.join(FRAMES, 'frames.ffconcat'), lines.join('\n') + '\n');

const mp4 = path.join(OUT_DIR, 'docushare-demo-mobile.mp4');
if (process.platform === 'darwin') {
  const encoder = path.join(path.dirname(fileURLToPath(import.meta.url)), 'encode-mp4.swift');
  execFileSync('swift', [encoder, FRAMES, mp4], { stdio: 'inherit' });
  fs.rmSync(FRAMES, { recursive: true, force: true });
} else {
  console.log(`Frames are in ${FRAMES}. Encode them with:\n  ffmpeg -f concat -i ${path.join(FRAMES, 'frames.ffconcat')} `
    + `-vf fps=30,format=yuv420p -c:v libx264 -crf 20 -movflags +faststart ${mp4}`);
}
