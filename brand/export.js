const sharp = require('sharp');
const fs = require('fs');
const SRC = 'C:/CLAUDE CODE/NEW_POS/marketing/favicon.svg';
const OUT = 'C:/CLAUDE CODE/NEW_POS/brand';
const BLUE = '#2563eb';
fs.mkdirSync(OUT, { recursive: true });

const svg = fs.readFileSync(SRC, 'utf8');
const markOnly = svg.replace(/<rect[^>]*\/?>(?:<\/rect>)?/, '');

// The mark is not centred in the 64x64 artboard — it sits ~2 units left and
// high. Trim to the real ink bounds so it can be placed on true centre.
async function trimmedMark(px) {
  const big = await sharp(Buffer.from(markOnly), { density: 2400 })
    .resize(px * 2, px * 2, { fit: 'contain', background: { r:0,g:0,b:0,alpha:0 } })
    .png().toBuffer();
  return sharp(big).trim({ threshold: 1 }).png().toBuffer();
}

// `scale` is the fraction of the canvas the mark's longest side should occupy.
// 0.58 keeps it clear of Facebook's circular crop with room to breathe.
async function plate(size, scale, radius) {
  const mark = await trimmedMark(size);
  const meta = await sharp(mark).metadata();
  const target = Math.round(size * scale);
  const ratio = meta.width / meta.height;
  const w = ratio >= 1 ? target : Math.round(target * ratio);
  const h = ratio >= 1 ? Math.round(target / ratio) : target;
  const resized = await sharp(mark).resize(w, h).png().toBuffer();

  const bg = radius > 0
    ? `<svg width="${size}" height="${size}"><rect width="${size}" height="${size}" rx="${radius}" fill="${BLUE}"/></svg>`
    : `<svg width="${size}" height="${size}"><rect width="${size}" height="${size}" fill="${BLUE}"/></svg>`;

  return sharp(Buffer.from(bg))
    .composite([{ input: resized, left: Math.round((size - w) / 2), top: Math.round((size - h) / 2) }])
    .png({ compressionLevel: 9 }).toBuffer();
}

(async () => {
  const out = {
    // Facebook crops Page pictures to a circle, so square corners never show.
    'facebook-profile-1024.png': await plate(1024, 0.58, 0),
    // Rounded plate for anywhere the square is shown as-is.
    'logo-square-1024.png':      await plate(1024, 0.58, 192),
    'logo-mark-white-1024.png':  await trimmedMark(1024),
  };
  for (const [name, buf] of Object.entries(out)) {
    fs.writeFileSync(OUT + '/' + name, buf);
    const m = await sharp(buf).metadata();
    console.log(name.padEnd(30), m.width + 'x' + m.height, (buf.length/1024).toFixed(1) + ' KB');
  }
})();
