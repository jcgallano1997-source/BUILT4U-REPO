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
| Ticker | the capability list slides on a 52s loop and **pauses on hover** so a name can be read |
| Hardware tiles | a glow that follows the pointer, positioned from `--mx`/`--my` |
| Promise block | pulses travelling the circuit traces; `pathLength="100"` normalises every path so one dash animation fits all of them |
| Stat band | `3,000+` counts up the first time it scrolls into view |

**`prefers-reduced-motion` switches the whole lot off** in one block at the end
of the stylesheet — the beam, row highlight, caret and sheen are removed
outright rather than merely paused, and the count-up never starts, so the
number is simply the value in the markup. If you add motion, add it to that
block too.

One layout constraint worth keeping: `overflow-x:hidden` belongs on `html`,
**not** `body`. On `body` it makes the scroll container ambiguous — `scrollY`
reads 0 and the progress bar never moves.

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

## Contact details

Live in the page already:

| Channel | Value |
|---|---|
| Facebook Page | `facebook.com/profile.php?id=61582125780879` |
| Phone / WhatsApp | `+63 992 286 2068` |
| Email | `customer_service@built4u-pos.com` |

To change any of them, search `index.html` — the email appears five times
(contact tile label, its `mailto:`, the form script, and the footer).

The domain address runs on **Cloudflare Email Routing** (free), which forwards
mail to `built4usolutions@gmail.com`. It is receive-only — replies go out from
Gmail unless you configure Gmail's "Send mail as".

More addresses can be added any time (Cloudflare → Email → Email Routing →
Routing rules), all forwarding to the same inbox.

## Deploy on Cloudflare Pages

1. Cloudflare → **Workers & Pages → Create → Pages → Connect to Git** → this repo
2. Build settings:
   - **Root directory:** `marketing`
   - **Build command:** *(leave empty)*
   - **Output directory:** `marketing` — or `/` with the root set as above
3. **Custom domains** → add `built4u-pos.com` and `www.built4u-pos.com`

Cloudflare issues the TLS certificate itself. Nothing to configure in DNS by
hand — Pages adds the records when the domain is in the same account.

## Note on the POS app

Keep the app off the root domain. `built4u-pos.com` is for selling; put the
product on a subdomain when you're ready:

```
built4u-pos.com       -> this marketing site
app.built4u-pos.com   -> the POS (Render static site)
api.built4u-pos.com   -> the POS API (Render web service)
```

## The demo form

It composes a `mailto:` — no server, nothing to break, works from a static host.
The trade-off is that it opens the visitor's mail app, which some people abandon,
so the direct Messenger / call / email buttons sit right beside it. In the
Philippines most enquiries will come through Messenger anyway.

If you later want a proper form that posts silently, the options are a Cloudflare
Pages Function calling Resend (you already have a verified sending domain), or a
free service like Formspree. Not needed to launch.

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
