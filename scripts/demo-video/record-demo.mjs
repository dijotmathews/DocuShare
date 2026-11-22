// Records a narrated walkthrough of the DocuShare demo (client + admin) as WebM.
// Needs the app running with the demo profile (see DEVELOPMENT.md, "Demo video").
// Usage: BASE=http://localhost:8080 node record-demo.mjs <output-dir>
import { chromium } from 'playwright';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const BASE = process.env.BASE ?? 'http://localhost:8080';
const OUT_DIR = path.resolve(process.argv[2] ?? 'demo-video');
const W = 1280, H = 800;
fs.mkdirSync(OUT_DIR, { recursive: true });

// Caption bar and a visible cursor, re-created on every page load
const overlay = () => {
  const install = () => {
    if (document.getElementById('demo-caption')) return;
    const style = document.createElement('style');
    style.textContent = `
      #demo-caption { position: fixed; left: 50%; bottom: 28px; transform: translateX(-50%);
        max-width: 80%; padding: 12px 22px; border-radius: 12px; z-index: 2147483647;
        background: rgba(17, 24, 39, .88); color: #fff; font: 500 19px/1.4 system-ui, sans-serif;
        text-align: center; box-shadow: 0 8px 24px rgba(0,0,0,.25); transition: opacity .25s; pointer-events: none; }
      #demo-caption:empty { opacity: 0; }
      #demo-title { position: fixed; inset: 0; z-index: 2147483646; display: flex; flex-direction: column;
        align-items: center; justify-content: center; gap: 14px; background: #1e3a8a; color: #fff;
        font-family: system-ui, sans-serif; transition: opacity .5s; pointer-events: none; }
      #demo-title h1 { margin: 0; font-size: 56px; font-weight: 700; color: #fff !important; }
      #demo-title p { margin: 0; font-size: 24px; opacity: .85; color: #fff !important; }
      vaadin-dev-tools, copilot-main { display: none !important; }
      #demo-cursor { position: fixed; width: 22px; height: 22px; margin: -11px 0 0 -11px; border-radius: 50%;
        background: rgba(37, 99, 235, .35); border: 2px solid rgba(37, 99, 235, .9);
        pointer-events: none; transition: transform .12s; z-index: 2147483645; }
      #demo-cursor.down { transform: scale(.7); background: rgba(37, 99, 235, .6); }`;
    document.documentElement.appendChild(style);
    const caption = document.createElement('div');
    caption.id = 'demo-caption';
    caption.textContent = sessionStorage.getItem('demo-caption') ?? '';
    const cursor = document.createElement('div');
    cursor.id = 'demo-cursor';
    const [x, y] = JSON.parse(sessionStorage.getItem('demo-cursor') ?? '[640,400]');
    cursor.style.left = x + 'px'; cursor.style.top = y + 'px';
    document.documentElement.append(caption, cursor);
    addEventListener('mousemove', e => {
      cursor.style.left = e.clientX + 'px'; cursor.style.top = e.clientY + 'px';
      sessionStorage.setItem('demo-cursor', JSON.stringify([e.clientX, e.clientY]));
    }, true);
    addEventListener('mousedown', () => cursor.classList.add('down'), true);
    addEventListener('mouseup', () => cursor.classList.remove('down'), true);
  };
  if (document.readyState === 'loading') addEventListener('DOMContentLoaded', install); else install();
};

const browser = await chromium.launch();
const context = await browser.newContext({
  viewport: { width: W, height: H },
  recordVideo: { dir: OUT_DIR, size: { width: W, height: H } },
  acceptDownloads: true,
});
await context.addInitScript(overlay);
const page = await context.newPage();
const pause = (ms) => page.waitForTimeout(ms);

async function caption(text, hold = 2600) {
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
  await pause(hold);
  await page.evaluate(() => { const el = document.getElementById('demo-title'); el.style.opacity = '0'; });
  await pause(600);
  await page.evaluate(() => document.getElementById('demo-title')?.remove());
}
async function moveTo(locator) {
  await locator.scrollIntoViewIfNeeded();
  const box = await locator.boundingBox();
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2, { steps: 25 });
  await pause(250);
}
async function click(locator, after = 900) {
  await moveTo(locator);
  await page.mouse.down(); await pause(90); await page.mouse.up();
  await pause(after);
}
async function type(locator, text) {
  await click(locator, 200);
  await locator.pressSequentially(text, { delay: 70 });
  await pause(300);
}
async function scroll(dy, steps = 12) {
  for (let i = 0; i < steps; i++) { await page.mouse.wheel(0, dy / steps); await pause(45); }
  await pause(500);
}
async function login(url, user, pass) {
  if (new URL(page.url()).pathname !== '/' + url) {
    await page.goto(`${BASE}/${url}`);
  }
  await page.waitForSelector('vaadin-login-form');
  await pause(700);
  await type(page.locator('vaadin-text-field[name="username"] input'), user);
  await type(page.locator('vaadin-password-field[name="password"] input'), pass);
  await click(page.locator('vaadin-button[slot="submit"]'), 1800);
}
const navItem = (label) => page.locator('vaadin-side-nav-item', { hasText: label });

// ---------- Client ----------
await page.goto(`${BASE}/login`);
await page.waitForSelector('vaadin-login-form');
await title('DocuShare', 'Tax documents, organised for every client');
await caption('A client signs in with their PAN and password', 600);
await login('login', 'DEMOC1234D', 'DEMOC1234D@01042015');

await caption('My Documents shows only this client\'s documents, grouped in a side nav', 3200);
await moveTo(page.locator('vaadin-side-nav'));
await caption('Filings, Financials, Computations and Misc, each with its document count', 3200);
await caption('Inside a group, documents are listed by financial year, latest first', 3000);

await caption('Financials: balance sheet, profit & loss and both audit reports', 0);
await click(navItem('Financials'), 2200);
await caption('Three financial years of documents, each with when it was uploaded', 1500);
await scroll(520, 16);
await pause(1200);
await scroll(-520, 10);

await caption('Computations', 0);
await click(navItem('Computations'), 2000);
await caption('Misc', 0);
await click(navItem('Misc'), 2000);
await caption('Back to Filings: the income tax return acknowledgements', 0);
await click(navItem('Filings'), 2200);

await caption('Download saves the original PDF; every download is recorded for audit', 0);
const download = page.waitForEvent('download');
await click(page.locator('.my-documents__download').first(), 600);
await (await download).saveAs(path.join(os.tmpdir(), 'docushare-demo-download.pdf'));
await pause(2200);

await caption('Each group has its own address, so the browser\'s back button and bookmarks work', 3000);
await caption('', 0);
await click(page.locator('vaadin-button', { hasText: 'Logout' }), 1500);

// ---------- Admin ----------
await title('Behind the scenes', 'The consultant\'s admin view', 2600);
await caption('The consultant signs in to the admin portal', 600);
await login('admin/login', 'admin', 'admin');
await caption('The admin dashboard links to every admin section', 2800);
await moveTo(page.locator('.admin-nav__item', { hasText: 'Upload Documents' }));
await caption('Uploading a document: the PAN is picked from the client list', 0);
await click(page.locator('.admin-nav__item', { hasText: 'Upload Documents' }), 1500);
const pan = page.locator('vaadin-combo-box').filter({ has: page.locator('label:text-is("PAN")') }).locator('input');
await type(pan, 'demo');
await pause(1600);
await pan.press('ArrowDown'); await pause(400); await pan.press('Enter');
await caption('The client\'s name confirms it\'s the right one, and a folder is suggested', 0);
const ay = page.locator('vaadin-text-field').filter({ has: page.locator('label:text-is("Assessment Year")') }).locator('input');
await type(ay, '2026');
await page.keyboard.press('Tab');
await pause(2800);

await caption('All Documents lists every client\'s documents, newest first', 0);
await click(page.locator('.app-header__nav a', { hasText: /^Documents$/ }), 2600);
await caption('Filters narrow the list, for example by document type', 0);
await click(page.locator('.all-documents__filters vaadin-select'), 700);
await click(page.locator('vaadin-select-item', { hasText: 'Tax Audit Report' }).first(), 2600);
await caption('One tax audit report for each of the three years', 2600);
await caption('Clear filters shows everything again', 0);
await click(page.locator('.all-documents__filters vaadin-button', { hasText: 'Clear filters' }), 2200);
await caption('', 0);

await title('DocuShare', 'Filings · Financials · Computations · Misc, by financial year', 3200);

const video = page.video();
await context.close();
await browser.close();
const recorded = await video.path();
const target = path.join(OUT_DIR, 'docushare-demo.webm');
fs.renameSync(recorded, target);
console.log(target);
