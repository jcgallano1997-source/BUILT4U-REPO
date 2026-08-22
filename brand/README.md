# Brand assets

Raster exports of the Built4U mark, for places that cannot take an SVG —
Facebook, Messenger, printed material, supplier documents.

**Source of truth is `marketing/favicon.svg`.** Change that, then re-export;
never edit these PNGs by hand.

| File | Size | Use |
|---|---|---|
| `facebook-profile-1024.png` | 1024x1024 | Facebook / Messenger profile picture |
| `logo-square-1024.png` | 1024x1024 | rounded blue plate, anywhere the square shows as-is |
| `logo-mark-white-1024.png` | 877x1482 | white mark on transparency, for dark backgrounds |

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
on a dark background. It is never re-coloured and never sits on the orange used
by the marketing site.
