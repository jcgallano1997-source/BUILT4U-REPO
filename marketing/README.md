# Built4U POS — marketing site

A single static page for `built4u-pos.com`. One self-contained `index.html`: no
build step, no dependencies, no framework. Edit it in any text editor and
re-deploy.

Aimed at Philippine hardware / construction-supply stores, with one goal —
**getting a demo request**.

## Design system

Matches the calling card, the banner and the desktop system's login screen: a
near-black navy ground with soft cyan/purple glows.

| Token | Value | Used for |
|---|---|---|
| `--bg` | `#070B16` | page ground |
| `--bg-2` | `#0B1122` | raised bands — stat band, hardware, promise, footer |
| `--bg-3` / `--bg-4` | `#0E1628` / `#131D33` | hardware tiles, terminal bezel, floating stat |
| `--ink` → `--dim` | `#F3F6FC` `#AEBBD4` `#8494B2` `#7B8AA6` `#5C6A85` | five text steps, brightest first |
| `--brand` | `#2563eb` | **the logo plate, and the primary button — fixed** |
| `--cyan` | `#22D3EE` | eyebrows, small-caps labels, link and hover states |
| `--purple` | `#A855F7` | the far end of the gradient; glows only |
| `--grad` | `#22D3EE → #3B82F6 → #A855F7` | the signature device (see below) |
| `--green` | `#34D399` | ticks, shift-open, in-stock states |
| `--screen-*` | `#FBFCFE` … `#0F172A` | the **light** palette inside the POS mockups |

**The gradient is the signature.** It runs as a 3px bar across the top of the
page (`header::before`), as a 2px hairline on the terminal mock and the promise
block, and through the words "should too." in the H1 and the step numbers. Used
anywhere else it stops being a signature. Gradient-filled text carries an
`@supports` fallback to flat cyan — without it, engines that cannot clip a
background to glyphs render the words invisible.

**The screen stays light.** The hero terminal keeps a light UI inside a dark
bezel, because the real app is light and that is what a lit counter screen
looks like. It has its own `--screen-*` scale; do not paint it with the page
tokens.

## Motion

All of it is CSS keyframes, one inline SVG grain, and three short scripts —
no GIFs, no video, no library, nothing downloaded.

| Where | What |
|---|---|
| Whole page | a fixed SVG grain at 4% so the dark ground is not one dead colour |
| Header bar | the gradient dims to a track and fills left-to-right with scroll |
| Hero + contact | the glows drift on a ~20s `aurora` cycle |
| Hero terminal | a beam crossing the screen, a highlight walking the four cart rows on a shared 7s cycle, a blinking caret, a sheen on the pay button, and a bobbing stock badge |
| Conversion block | a 9s loop: a box of ten arrives, ten pieces pop out on staggered delays, the last three dim as sold, and the on-hand figure swaps 10 → 7. Same item and price as the hero terminal, so the two agree |
| Feature spotlight | six capabilities cross-fading on a 27s cycle, 4.5s each, with a progress dot filling per slide |
| Counter schematic | a 12s loop running the sequence a sale actually takes — scanner beam, terminal rows, receipt printing with a torn edge, drawer sliding open on cash — with pulses travelling the traces between devices. Hidden below 640px, where 900 units of viewBox shrink the devices past legibility; the six tiles carry the content there |
| Hardware tiles | a glow that follows the pointer, positioned from `--mx`/`--my` |
| Promise block | pulses travelling the circuit traces; `pathLength="100"` normalises every path so one dash animation fits all of them |
| Stat band | `3,000+` counts up the first time it scrolls into view |

**`prefers-reduced-motion` switches the whole lot off** in one block at the end
of the stylesheet — the beam, row highlight, caret and sheen are removed
outright rather than merely paused, and the count-up never starts, so the
number is simply the value in the markup. If you add motion, add it to that
block too.

Two layout constraints worth keeping:

- `overflow-x:hidden` belongs on `html`, **not** `body`. On `body` it makes the
  scroll container ambiguous — `scrollY` reads 0 and the progress bar never
  moves.
- The spotlight slides are **grid-stacked** (`.stage{display:grid}` with every
  slide at `grid-area:1/1`), not absolutely positioned over a fixed height. The
  row then sizes itself to the tallest slide at every width. Absolute
  positioning needs a `min-height` guess, which was overflowing below ~620px
  and again under 360px — and would break again the next time the copy is
  edited. Don't reintroduce it.

Type: **Archivo** 700/800 for headings, **IBM Plex Sans** for body, **IBM Plex
Mono** for labels, item codes and figures. All three come from Google Fonts; if
that request fails the page falls back to system sans and still reads fine.

Everything is real CSS classes in one `<style>` block at the top — no framework,
no inline-style soup. Sections use a `96px` top rhythm, and the raised `--bg-2`
bands (`.band`, `.dark`, `.promise`, `footer`) break up the run of page-ground
sections.

Every text/background pair on the page clears WCAG AA (4.5:1 body, 3:1 large).
If you darken a text token, re-check it.

The scroll reveal is decoration only. If `IntersectionObserver` never fires
(throttled tab, headless renderer, odd browser) a 1.5s fail-safe drops the
`js` class and everything appears — the page is never left blank.

## What is in this folder

| File | What it is |
|---|---|
| `index.html` | the entire site — markup, styles and scripts in one file |
| `og-image.png` | 1200x630 social card; generated, never hand-edited |
| `robots.txt` | opens the site to crawlers and points at the sitemap |
| `sitemap.xml` | one entry; bump `<lastmod>` when the copy changes |
| `favicon.*`, `apple-touch-icon.png`, `icon-*.png` | the icon set |

## Search and social

The `<head>` carries, in order: a **canonical** URL, the Open Graph block, the
Twitter card, then one **JSON-LD** `@graph`.

**The canonical matters more than it looks.** Facebook appends `?fbclid=…` to
every link someone clicks from the Page. Without a canonical tag each of those
is a separate URL to a crawler, splitting whatever ranking the page earns across
hundreds of near-duplicates.

**`og-image.png` is generated by `brand/og-card.js`** — text is converted to
outlines there, so the card renders identically on any machine without webfonts.
Edit the script and re-run it; do not touch the PNG. It replaced the old square
`icon-512.png`, which every platform letterboxed into its 1.91:1 slot.

**The JSON-LD FAQ block repeats the five on-page questions verbatim.** If you
edit a question or an answer in the markup, edit it in the JSON too. Structured
data that does not match the visible page is treated by Google as a violation
rather than an oversight, and the penalty is losing rich results sitewide.

Two things are deliberately missing from the `SoftwareApplication` node:
`offers` and `aggregateRating`. Prices are not published and there are no
collected reviews, and inventing either to win a star rating would be
fabrication. Add `offers` the day pricing goes on the page.

## Analytics

A short block near the top of the `<head>` holds three empty constants:

| Constant | Tool | Cookies? |
|---|---|---|
| `CF_BEACON` | Cloudflare Web Analytics token | no |
| `GA4_ID` | Google Analytics `G-…` | yes |
| `META_PIXEL` | Meta Pixel ID — required for Facebook ad retargeting | yes |

**Nothing loads while they are empty.** No ID, no request, no third-party script,
no cookie banner owed. Fill one in and only that one starts.

Start with Cloudflare: it is cookieless, so it needs no consent notice under the
Data Privacy Act. GA4 and the Meta Pixel both set cookies — switching either on
means adding a consent notice to the page.

The same block records a `contact_click` whenever someone hits a `tel:`,
`mailto:`, WhatsApp or Facebook link. Every contact route here is an outbound
link, so that click is the closest thing this page has to a conversion. It is a
no-op until a tool is switched on.

## Contact details

Live in the page already:

| Channel | Value |
|---|---|
| Facebook Page | `facebook.com/profile.php?id=61582125780879` |
| Phone / WhatsApp | `+63 992 286 2068` |
| Email | `customer_service@built4u-pos.com` |

To change any of them, search `index.html`. The email appears five times: twice
in the JSON-LD block (`Organization.email` and the `contactPoint`), then the
contact tile's `mailto:`, the label inside that tile, and the footer. The phone
number appears in the JSON-LD, the contact tiles and the footer — note the
JSON-LD uses the `+63-992-286-2068` dash form, not the spaced one.

The domain address runs on **Cloudflare Email Routing** (free), which forwards
mail to `built4usolutions@gmail.com`. It is receive-only — replies go out from
Gmail unless you configure Gmail's "Send mail as".

More addresses can be added any time (Cloudflare → Email → Email Routing →
Routing rules), all forwarding to the same inbox.

## Deploy

**The site runs on Render, not Cloudflare Pages.** It is the
`built4u-pos-marketing` service in `render.yaml` at the repo root — `runtime:
static`, `rootDir: marketing`, `autoDeploy: true`. Push to `main` and Render
republishes. Cloudflare still sits in front of it for DNS and CDN, which is why
responses carry both a `cf-cache-status` and an `rndr-id` header.

There is nothing to build. Render is pointed at this folder and serves it as-is.

> Earlier revisions of this file described a Cloudflare Pages setup. That was
> the plan, not what shipped — if you are ever unsure which is live, `curl -I
> https://built4u-pos.com/` and look for `rndr-id`.

Because a static site is served from Render's CDN rather than an instance, it
has **no spin-down** — the 15-minute idle sleep on Render's free tier applies to
the `built4u-pos-api` web service, never to this page.

## Note on the POS app

Keep the app off the root domain. `built4u-pos.com` is for selling; put the
product on a subdomain when you're ready:

```
built4u-pos.com       -> this marketing site
app.built4u-pos.com   -> the POS (Render static site)
api.built4u-pos.com   -> the POS API (Render web service)
```

## Lead capture

**There is no form on the page.** One existed early on — it composed a `mailto:`
— and was removed in `9d56f6b` for a plain Contact us section, because a mailto
form opens the visitor's mail app and a good share of people abandon it there.
Every route on the page is now outbound: Messenger, WhatsApp, call, email.

The cost of that is real and worth stating plainly: a visitor who is interested
but not ready to open a chat has nothing to leave behind, so the page cannot
convert them at all. Three ways to get capture back without running a server:

- **A WhatsApp prefill.** Ordinary inputs (store name, branches, phone), then
  JavaScript builds a `wa.me/639922862068?text=…` link with the answers already
  written into the message. No server, no third party, no account — and it lands
  the visitor where enquiries already come from.
- **Formspree or Web3Forms.** A plain `<form action="https://…">` that someone
  else's server turns into an email. Free tiers cover far more than this page
  will see.
- **A Cloudflare Pages Function calling Resend.** The sending domain is already
  verified for the backend's report email. This means moving the site off
  Render, so only worth it if you want Pages for other reasons.

## Logo

The mark in the nav and the favicon come from `favicon.svg` — the same file the
app uses, so both carry one identity.

**Brand blue `#2563eb` is fixed.** The logo sits on its own blue plate and must
not be re-coloured or placed on a coloured tile. The cyan/purple gradient is a
section device — never run it through the mark, and never tint the plate with
it.

The logo SVG is inlined in three places in `index.html` — the nav `.mark`, the
`.promise .who .av` avatar, and the footer `.mark`. To update it, replace
`favicon.svg` here and all three inline copies.
