# Brand assets

Raster exports of the Built4U mark, for places that cannot take an SVG —
Facebook, Messenger, printed material, supplier documents.

**Source of truth is `marketing/favicon.svg`.** Change that, then re-export;
never edit these PNGs by hand.

| File | Size | Built by | Use |
|---|---|---|---|
| `facebook-profile-1024.png` | 1024x1024 | `export.js` | Facebook / Messenger profile picture |
| `logo-square-1024.png` | 1024x1024 | `export.js` | rounded blue plate, anywhere the square shows as-is |
| `logo-mark-white-1024.png` | 877x1482 | `export.js` | white mark on transparency, for dark backgrounds |
| `../marketing/og-image.png` | 1200x630 | `og-card.js` | the link preview for built4u-pos.com |

Both scripts need `sharp`, and `og-card.js` also needs `fontkit`:

```
npm i --prefix brand
node brand/export.js
node brand/og-card.js
```

Run them from the repo root. Paths resolve from the script's own location, so
neither cares where the repo is checked out.

`og-card.js` downloads Archivo and IBM Plex on first run into `brand/.fonts/`
(gitignored) and converts the headline to outlines, because the renderer has no
webfonts and both families are variable fonts whose default weight is not the
800 the card needs.

## Notes

Facebook crops Page profile pictures to a **circle**, so the square version
has no rounded corners — they would never be seen. The mark is set at 58% of
the canvas so nothing clips and it still reads at the 176px Facebook actually
displays.

The mark is not centred inside `favicon.svg` — it sits about 2 units left and
high in the 64x64 artboard, which is invisible at favicon size but obvious as
a profile picture. The export script trims to the real ink bounds and re-centres,
so do not assume the raw SVG can be dropped straight into a square.

**Brand blue is `#2563eb` and is fixed.** The mark is white on blue, or white
on a dark background. It is never re-coloured.

In particular it never carries the cyan → blue → purple gradient. That gradient
is a *section* device on the marketing site — a hairline across the top of a
surface — and running it through the mark turns a fixed identity into
decoration. On `og-image.png` the two sit on the same canvas and stay separate:
the gradient is the 7px bar at the very top, the mark keeps its blue plate.

> An earlier version of this note warned against placing the mark on "the orange
> used by the marketing site". There is no orange anywhere any more — the site
> was rebuilt on the navy/cyan/purple system in `a3cea5d`.
