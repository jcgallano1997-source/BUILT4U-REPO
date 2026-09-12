// Builds marketing/og-image.png — the 1200x630 card Facebook, Messenger,
// WhatsApp, LinkedIn and X show when built4u-pos.com is shared.
//
// Why this exists: og:image used to point at icon-512.png, a 512x512 square.
// Every platform crops or letterboxes a square into a 1.91:1 slot, so the link
// preview showed a floating logo and no message. This draws a real card.
//
// Text is converted to outlines rather than set as SVG <text>. The renderer has
// no webfonts, and Archivo/IBM Plex are variable fonts whose default weight is
// 600/400 — asking for 800 by CSS name would silently give the wrong weight.
// Outlines make the result identical on any machine.
//
//   npm i --prefix brand      (sharp + fontkit)
//   node brand/og-card.js     (downloads the fonts once, into brand/.fonts)

const fs = require('fs');
const path = require('path');
const sharp = require('sharp');
const fontkit = require('fontkit');

const ROOT = path.join(__dirname, '..');
const OUT = path.join(ROOT, 'marketing', 'og-image.png');
const FONT_DIR = path.join(__dirname, '.fonts');

const W = 1200, H = 630, PAD = 80;

// Straight from the design system in marketing/README.md.
const INK = '#F3F6FC', BODY = '#AEBBD4', MUTED = '#8494B2';
const BRAND = '#2563eb', CYAN = '#22D3EE', PURPLE = '#A855F7', BLUE_LT = '#3B82F6';

const FONTS = {
  archivo: {
    file: 'Archivo.ttf',
    url: 'https://raw.githubusercontent.com/google/fonts/main/ofl/archivo/Archivo%5Bwdth,wght%5D.ttf',
  },
  sans: {
    file: 'IBMPlexSans.ttf',
    url: 'https://raw.githubusercontent.com/google/fonts/main/ofl/ibmplexsans/IBMPlexSans%5Bwdth,wght%5D.ttf',
  },
  mono: {
    file: 'IBMPlexMono-Medium.ttf',
    url: 'https://raw.githubusercontent.com/google/fonts/main/ofl/ibmplexmono/IBMPlexMono-Medium.ttf',
  },
};

async function loadFonts() {
  fs.mkdirSync(FONT_DIR, { recursive: true });
  const out = {};
  for (const [key, f] of Object.entries(FONTS)) {
    const dest = path.join(FONT_DIR, f.file);
    if (!fs.existsSync(dest)) {
      const res = await fetch(f.url);
      if (!res.ok) throw new Error(f.file + ': ' + res.status + ' ' + res.statusText);
      fs.writeFileSync(dest, Buffer.from(await res.arrayBuffer()));
      console.log('fetched', f.file);
    }
    out[key] = fontkit.openSync(dest);
  }
  return out;
}

// Lay a string out and return it as SVG outlines plus its measured width, so a
// caller can right-align or continue a line in another colour without guessing.
//
// Glyphs stay in font units inside one <g>; the group does the scaling and the
// Y-flip (fonts are Y-up, SVG is Y-down). Per-glyph transforms would mean
// mutating fontkit's cached Path objects, which corrupts the next call.
function text(font, str, opts) {
  const { size, x, y, fill, weight, tracking = 0, anchor = 'start', opacity = 1 } = opts;
  const f = weight ? font.getVariation({ wght: weight, wdth: 100 }) : font;
  const run = f.layout(str);
  const scale = size / f.unitsPerEm;
  const trackUnits = tracking / scale;

  const width = run.positions.reduce((sum, p) => sum + p.xAdvance, 0) * scale
    + tracking * Math.max(0, run.glyphs.length - 1);

  let originX = x;
  if (anchor === 'end') originX = x - width;
  else if (anchor === 'middle') originX = x - width / 2;

  let pen = 0;
  const glyphs = run.glyphs.map(function (g, i) {
    const p = run.positions[i];
    const d = g.path.toSVG();
    const at = pen + p.xOffset;
    pen += p.xAdvance + trackUnits;
    // A space has no outline; skip it rather than emit an empty path.
    if (!d) return '';
    return '<path transform="translate(' + at.toFixed(1) + ',' + (p.yOffset || 0).toFixed(1) + ')" d="' + d + '"/>';
  }).join('');

  const o = opacity === 1 ? '' : ' opacity="' + opacity + '"';
  const svg = '<g fill="' + fill + '"' + o + ' transform="translate(' + originX.toFixed(1) + ','
    + y.toFixed(1) + ') scale(' + scale.toFixed(6) + ',' + (-scale).toFixed(6) + ')">' + glyphs + '</g>';
  return { svg: svg, width: width };
}

// The mark out of marketing/favicon.svg, minus its blue plate — this card draws
// its own plate so the corner radius can match the card's scale.
const MARK = '<path d="M1.1,1C.1,2.6.4,302.4,1.4,302.8c.8.2,23.5-20.3,50.2-45.5l8.4-8,.2-47.9c.1-26.3.3-48.2.3-48.7.1-1,136.1-.9,136.1.2,0,.4-2.6,3.2-5.7,6.2-3.2,2.9-13.2,12.3-22.3,20.9s-22.6,21.2-30,28.1c-7.4,6.8-18.4,17.1-24.5,22.9s-14.4,13.7-18.5,17.5c-4.1,3.9-14.9,14-24,22.5s-20.5,19.3-25.5,24.1c-4.9,4.7-14.8,14-22,20.6-7.1,6.6-15.5,14.5-18.6,17.4l-5.5,5.4.3,13c.1,7.1.2,18.1.3,24.2v11.3l114.3.2,114.2.3.5,99.5c.4,75.4.8,100.2,1.7,102.3l1.3,2.7h29.4c22.5,0,29.7-.3,30.3-1.3.4-.6,1-46.7,1.3-102.2l.5-101,7.5-.6c9.5-.8,18.1-3.3,20.5-5.9,3.1-3.4,3.8-32.5.9-37.2-1.5-2.4-10.1-5-19-5.8-5.1-.4-9.8-1.1-10.3-1.5-.8-.4-1.1-22.6-1.1-72,0-39.3-.3-71.6-.8-71.8-.4-.3-3.9,2.5-7.8,6.1-15,14.1-37.1,35.1-45.6,43.2l-8.8,8.5v86.5h-72.5c-41.4,0-72.5-.4-72.5-.9,0-.8,6.8-6.9,43.4-39,7.2-6.3,16.9-14.9,21.6-19,4.7-4.2,9.8-8.7,11.4-10.1s6.6-5.7,11-9.6c4.5-4,14.6-12.9,22.6-19.9,32.9-28.9,74.6-66.2,87.5-78.3l8.9-8.4,19.1.4c18.7.3,19.1.4,24.8,3.3,6.4,3.2,12.4,9.1,15.8,15.7,2.1,3.9,2.4,6,2.9,24.3.6,22.8-.3,29.2-4.8,35.7-6.8,9.8-16.1,15.2-29.9,17.3l-4.3.7-.1,14.6c-.1,8.1-.3,18.1-.5,22.2-.5,11.2-.7,11,8.9,11,9,0,22-2.5,34-6.4,28.2-9.3,47.3-27.3,54.9-51.6,2.2-7,2.3-9.1,2.3-33.5,0-21.9-.3-27-1.8-32.5-3.9-14.3-15.7-28.9-28.5-35.3-8.2-4.1-8.4-3.2,2-9.1s20.2-19.1,24.2-32.5c2.2-7.5,2.2-29.1,0-38-.9-3.6-3.7-10.7-6.2-15.6-3.7-7.2-6.3-10.7-13.3-17.7-7.4-7.3-10.2-9.4-19.1-13.7-11.3-5.6-17.3-7.4-32-9.6-8.3-1.3-34.2-1.5-173.7-1.5C61.1,0,1.5.4,1.1,1ZM333.8,49.1c14.8,3.2,20.8,11.1,20.8,27.8,0,13.3-5.2,21.1-17,25.3l-6.5,2.3h-134.7c-74,0-134.9-.2-135.2-.5-.4-.4-1-48.9-.7-54.8.1-1.7,265.2-1.8,273.3-.1Z"/><path d="M405.6,289.1c-12,11.1-36.4,26.5-50.7,32l-2.2.8-.3,131.3-.3,131.3-2.3,7.9c-5.3,18.9-18.1,34.1-35.4,42-15,6.9-12,6.8-104.3,6.4l-83-.4-7-2.8c-11.3-4.4-18.1-8.5-25-14.8-11.2-10.3-18.5-24.5-20.5-40.1-.5-4-1-43.1-1-86.9,0-54.4-.3-79.9-1-80.3-1.5-.9-44-1.4-59.3-.6l-12.8.6v85.5c.1,51,.5,87.6,1,90.5,6,30.3,17.8,53.4,37.6,73.2,14,13.9,29.9,23.7,49,30.1,21.5,7.2,12.3,6.7,121,6.7h98l9.4-2.4c41.5-10.6,69-32.9,86.6-70.5,5.3-11.2,6.3-14.2,9.2-27.6,1.5-6.7,1.7-23.1,2.2-158.1.6-148.9.5-160.9-.7-160.9-.4.1-4.1,3.2-8.2,7.1Z"/>';

// The blue plate with the mark centred on it, as a PNG buffer.
//
// Mirrors plate() in export.js, and for the same reason: the mark is NOT
// centred inside favicon.svg's 64x64 artboard — it sits ~2 units left and high.
// Dropping the raw SVG into a square reproduces that lean, which is invisible
// at favicon size and obvious at 74px. Rendering big, trimming to real ink
// bounds and re-centring is the only way to place it honestly.
async function logoPlate(size, scale, radius) {
  const markOnly = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 520 660" width="520" height="660">'
    + '<g fill="#ffffff">' + MARK + '</g></svg>';

  const big = await sharp(Buffer.from(markOnly), { density: 1200 })
    .resize(size * 4, size * 4, { fit: 'contain', background: { r: 0, g: 0, b: 0, alpha: 0 } })
    .png().toBuffer();
  const mark = await sharp(big).trim({ threshold: 1 }).png().toBuffer();

  const meta = await sharp(mark).metadata();
  const target = Math.round(size * scale);
  const ratio = meta.width / meta.height;
  const w = ratio >= 1 ? target : Math.round(target * ratio);
  const h = ratio >= 1 ? Math.round(target / ratio) : target;
  const resized = await sharp(mark).resize(w, h).png().toBuffer();

  const bg = '<svg xmlns="http://www.w3.org/2000/svg" width="' + size + '" height="' + size + '">'
    + '<rect width="' + size + '" height="' + size + '" rx="' + radius + '" fill="' + BRAND + '"/></svg>';

  return sharp(Buffer.from(bg))
    .composite([{ input: resized, left: Math.round((size - w) / 2), top: Math.round((size - h) / 2) }])
    .png().toBuffer();
}

(async function () {
  const font = await loadFonts();

  // ── Identity block, top-left ────────────────────────────────────────────
  // 0.62 of the plate: a touch tighter than the 0.58 used for the Facebook
  // profile picture, which needs slack for a circular crop. Nothing crops here.
  const PLATE = 74, LOGO_X = PAD, LOGO_Y = 72;
  const plate = await logoPlate(PLATE, 0.62, Math.round(14 * (PLATE / 64)));

  const nameX = LOGO_X + PLATE + 22;
  const wordmark = text(font.archivo, 'Built4U', { size: 36, x: nameX, y: LOGO_Y + 33, fill: INK, weight: 800 });
  const kicker = text(font.mono, 'POS SYSTEM', { size: 14, x: nameX + 2, y: LOGO_Y + 60, fill: MUTED, tracking: 3.2 });

  // ── Headline ────────────────────────────────────────────────────────────
  // The page's own strongest line: it states the product's reason to exist in
  // six words, which is all a feed thumbnail has room for.
  const HEAD = 78, L1 = 306, L2 = L1 + 92;
  const line1 = text(font.archivo, 'Buy by the box.', { size: HEAD, x: PAD, y: L1, fill: INK, weight: 800 });
  const sell = text(font.archivo, 'Sell by ', { size: HEAD, x: PAD, y: L2, fill: INK, weight: 800 });
  // Flat cyan, not the brand gradient — the README notes gradient text falls
  // back to flat cyan anyway, and a flat fill renders identically everywhere.
  const piece = text(font.archivo, 'the piece.', { size: HEAD, x: PAD + sell.width, y: L2, fill: CYAN, weight: 800 });

  const sub = text(font.sans, 'Point of sale and inventory for Philippine hardware stores.', {
    size: 27, x: PAD, y: L2 + 62, fill: BODY, weight: 400,
  });

  // ── Footer row ──────────────────────────────────────────────────────────
  const FOOT = 556;
  const chips = text(font.mono, 'BOX → PIECE  /  UTANG TRACKING  /  3,000+ ITEMS', {
    size: 16, x: PAD, y: FOOT, fill: MUTED, tracking: 1.7,
  });
  const url = text(font.mono, 'built4u-pos.com', { size: 19, x: W - PAD, y: FOOT, fill: CYAN, anchor: 'end' });

  const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="' + W + '" height="' + H + '" viewBox="0 0 ' + W + ' ' + H + '">'
    + '<defs>'
    + '<linearGradient id="bar" x1="0" y1="0" x2="1" y2="0">'
    + '<stop offset="0" stop-color="' + CYAN + '"/><stop offset="0.52" stop-color="' + BLUE_LT + '"/><stop offset="1" stop-color="' + PURPLE + '"/>'
    + '</linearGradient>'
    + '<radialGradient id="gc" cx="0.5" cy="0.5" r="0.5">'
    + '<stop offset="0" stop-color="' + CYAN + '" stop-opacity="0.20"/><stop offset="1" stop-color="' + CYAN + '" stop-opacity="0"/>'
    + '</radialGradient>'
    + '<radialGradient id="gp" cx="0.5" cy="0.5" r="0.5">'
    + '<stop offset="0" stop-color="' + PURPLE + '" stop-opacity="0.20"/><stop offset="1" stop-color="' + PURPLE + '" stop-opacity="0"/>'
    + '</radialGradient>'
    + '<radialGradient id="gb" cx="0.5" cy="0.5" r="0.5">'
    + '<stop offset="0" stop-color="' + BLUE_LT + '" stop-opacity="0.16"/><stop offset="1" stop-color="' + BLUE_LT + '" stop-opacity="0"/>'
    + '</radialGradient>'
    + '</defs>'
    + '<rect width="' + W + '" height="' + H + '" fill="#070B16"/>'
    + '<ellipse cx="150" cy="90" rx="620" ry="520" fill="url(#gc)"/>'
    + '<ellipse cx="1120" cy="600" rx="640" ry="520" fill="url(#gp)"/>'
    + '<ellipse cx="640" cy="330" rx="560" ry="420" fill="url(#gb)"/>'
    // The signature gradient hairline, same device as the site header.
    + '<rect width="' + W + '" height="7" fill="url(#bar)"/>'
    + wordmark.svg + kicker.svg
    + line1.svg + sell.svg + piece.svg
    + sub.svg
    + '<rect x="' + PAD + '" y="' + (FOOT - 40) + '" width="' + (W - PAD * 2) + '" height="1" fill="#ffffff" opacity="0.09"/>'
    + chips.svg + url.svg
    + '</svg>';

  fs.writeFileSync(path.join(FONT_DIR, 'og-card.svg'), svg);
  await sharp(Buffer.from(svg))
    .composite([{ input: plate, left: LOGO_X, top: LOGO_Y }])
    .png({ compressionLevel: 9 }).toFile(OUT);

  const m = await sharp(OUT).metadata();
  console.log('og-image.png'.padEnd(20), m.width + 'x' + m.height,
    (fs.statSync(OUT).size / 1024).toFixed(1) + ' KB');
})();
