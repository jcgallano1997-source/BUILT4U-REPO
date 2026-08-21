# Built4U POS — marketing site

A single static page for `built4u-pos.com`. One self-contained `index.html`: no
build step, no dependencies, no framework. Edit it in any text editor and
re-deploy.

Aimed at Philippine hardware / construction-supply stores, with one goal —
**getting a demo request**. It uses the same visual identity as the app (amber on
navy, safety stripe), so the site and the product read as one brand.

## Fill these in before publishing

Search `index.html` for each:

| Placeholder | Replace with |
|---|---|
| `https://m.me/YOUR_PAGE` | your Facebook Page's Messenger link |
| `0917 000 0000` | the real mobile number (appears twice: link + label) |
| `hello@built4u-pos.com` | your address, if different (appears three times) |

`hello@built4u-pos.com` doesn't exist yet — set it up free with **Cloudflare
Email Routing** (Cloudflare → Email → Email Routing) forwarding to your Gmail.
That looks far better on a quote than a personal address.

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
