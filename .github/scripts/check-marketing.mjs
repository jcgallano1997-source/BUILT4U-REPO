// Guards the marketing page's machine-readable parts, which no human notices
// when they break: the JSON-LD block, the head tags search engines and
// Facebook read, and the sitemap.
//
//   node .github/scripts/check-marketing.mjs
//
// Everything here is a parse or a string match against files in the repo — no
// network, no dependencies.

import { readFileSync, statSync } from 'node:fs';

const DIR = 'marketing';
const html = readFileSync(`${DIR}/index.html`, 'utf8');
const fail = [];
const ok = [];

function check(label, condition, detail) {
  (condition ? ok : fail).push(condition ? label : `${label}${detail ? ' — ' + detail : ''}`);
}

// ── head tags ──────────────────────────────────────────────────────────────
for (const [label, re] of [
  ['canonical', /<link rel="canonical" href="https:\/\/built4u-pos\.com\/">/],
  ['og:image is the 1200x630 card', /<meta property="og:image" content="https:\/\/built4u-pos\.com\/og-image\.png">/],
  ['og:image:width 1200', /<meta property="og:image:width" content="1200">/],
  ['og:image:height 630', /<meta property="og:image:height" content="630">/],
  ['twitter:card', /<meta name="twitter:card" content="summary_large_image">/],
  ['lang is en-PH', /<html lang="en-PH">/],
]) check(label, re.test(html));

// ── JSON-LD ────────────────────────────────────────────────────────────────
const blocks = [...html.matchAll(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/g)];
check('one JSON-LD block', blocks.length === 1, `found ${blocks.length}`);

let faq = null;
if (blocks.length) {
  try {
    const graph = JSON.parse(blocks[0][1])['@graph'] ?? [];
    const types = graph.map((n) => n['@type']);
    for (const t of ['Organization', 'WebSite', 'SoftwareApplication', 'FAQPage']) {
      check(`JSON-LD has ${t}`, types.includes(t));
    }
    faq = graph.find((n) => n['@type'] === 'FAQPage');

    // Google treats structured data that contradicts the visible page as a
    // violation, not an oversight — so an invented price would cost more than
    // it wins. Keep these absent until pricing and reviews are real.
    const app = graph.find((n) => n['@type'] === 'SoftwareApplication') ?? {};
    check('no fabricated offers', !('offers' in app));
    check('no fabricated aggregateRating', !('aggregateRating' in app));
  } catch (e) {
    check('JSON-LD parses', false, e.message);
  }
}

// Each marked-up answer must appear word for word in the page body, or the
// markup is describing a page that no longer exists.
//
// The JSON-LD has to be cut out of the haystack first. It lives in the same
// file, so searching the raw HTML finds every answer inside the block that
// declared it and the check passes no matter what the visible page says.
if (faq) {
  const body = html
    .replace(/<script type="application\/ld\+json">[\s\S]*?<\/script>/g, '')
    .replace(/<meta[^>]*>/g, '')
    .replace(/&rsquo;/g, '’')
    .replace(/&amp;/g, '&');

  for (const q of faq.mainEntity ?? []) {
    const text = q.acceptedAnswer?.text ?? '';
    check(`FAQ answer on page: "${q.name}"`, text.length > 0 && body.includes(text));
    check(`FAQ question on page: "${q.name}"`, body.includes(q.name));
  }
}

// ── companion files ────────────────────────────────────────────────────────
const robots = readFileSync(`${DIR}/robots.txt`, 'utf8');
check('robots.txt points at the sitemap', robots.includes('Sitemap: https://built4u-pos.com/sitemap.xml'));

const sitemap = readFileSync(`${DIR}/sitemap.xml`, 'utf8');
check('sitemap lists the homepage', sitemap.includes('<loc>https://built4u-pos.com/</loc>'));

// A PNG this small would mean the generator wrote a blank or half-drawn card.
const og = statSync(`${DIR}/og-image.png`);
check('og-image.png looks rendered', og.size > 20_000, `${(og.size / 1024).toFixed(1)} KB`);

// ── report ─────────────────────────────────────────────────────────────────
console.log(`${ok.length} passed`);
if (fail.length) {
  console.error(`\n${fail.length} failed:`);
  for (const f of fail) console.error('  ✗ ' + f);
  process.exit(1);
}
console.log('marketing page OK');
