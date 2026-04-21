# Hooked Shorts — public marketing site

Static Next.js site for **https://hookedshorts.com**: company presence, **Privacy Policy**, **Terms of Service**, and **Data Deletion** pages for TikTok Developer and similar reviews.

**Legal copy is centralized in** [`lib/site.ts`](lib/site.ts) (`siteUrl`, `brandName`, `legalEntityEn`, `contactEmail`, etc.). If the English name on your business license differs from `legalEntityEn`, update that file and redeploy so the footer, Privacy, Terms, and TikTok form fields stay consistent.

## Official email (`admin@hookedshorts.com`)

The site exposes **`admin@hookedshorts.com`** (see [`lib/site.ts`](lib/site.ts) `contactEmail`) for business, privacy, and platform checks. You need **DNS + a mailbox or forward** so mail to that address reaches a real inbox.

**Common approaches (pick one):**

1. **Cloudflare Email Routing** (free forwarding)  
   - Add `hookedshorts.com` to Cloudflare (nameservers or partial setup as your DNS host).  
   - **Email → Email Routing** → create address `admin@` → forward to your personal/work mailbox.  
   - Cloudflare will show the **MX records** to add (if the domain uses Cloudflare DNS, it applies automatically).

2. **Google Workspace or Microsoft 365** (paid, full mailbox)  
   - Verify the domain, add users, create `admin@hookedshorts.com` as a user or alias.

3. **Zoho Mail** (often has a free tier for one custom domain)  
   - Add domain, set MX per Zoho’s wizard, create `admin@`.

4. **Namecheap** (if the domain stays on Namecheap)  
   - Check **Domain → Email Forwarding** or **Private Email** product; some plans can forward `admin@` to another address.

**Minimum for audits:** ensure a test message to `admin@hookedshorts.com` is **delivered** (not bounced) and that you can **reply** from a company-appropriate address if asked.

**Changing the public address:** edit `contactEmail` in [`lib/site.ts`](lib/site.ts) and run `npm run build` + redeploy. Privacy, Contact, and the footer all read from that field.

## Requirements

- Node.js 20+ (see [`.nvmrc`](.nvmrc) — `nvm use` if you use nvm)

## CI (GitHub)

If this repo is on GitHub, pushing changes under `hookedshorts-site/` runs **lint → build → `npm run verify`** (see [`.github/workflows/hookedshorts-site.yml`](../.github/workflows/hookedshorts-site.yml) at repo root). Fix failures before deploying.

## Go-live checklist (you do this outside the repo)

**Detailed step-by-step in Chinese:** see [`部署与上线步骤.md`](部署与上线步骤.md)（部署、域名、企业邮、TikTok 填表）。

Short version:

1. **Host** — Deploy (e.g. Vercel or Cloudflare Pages) and attach **hookedshorts.com**; confirm `https://hookedshorts.com/`, `/privacy`, `/terms` return **200** over HTTPS.  
2. **Mail** — `admin@hookedshorts.com` can **receive** mail (and reply if reviewers write to you).  
3. **TikTok** — Submit company URL + Privacy + Terms + English usage text consistent with the live site.  
4. **Legal** — If your licence English name ≠ `legalEntityEn` in [`lib/site.ts`](lib/site.ts), update it, rebuild, redeploy.

## Local development

```bash
cd hookedshorts-site
npm install
npm run dev
```

Open **[http://localhost:3210](http://localhost:3210)** — this project’s default dev port is **3210** (avoids 3000/3100 and other crowded ports).

**If the terminal still shows `EADDRINUSE` or your `package.json` has an old port like 3100:** run **`npm run dev:force`** — it will free 3100 / 3005 / 3210 and start `next dev` on **3210** (or install the latest `package.json` from the repo and try again).

**Dev server:** `npm run dev` does **not** use Turbopack. If a React dev overlay still errors, stop the server, run `rm -rf .next` in `hookedshorts-site`, then `npm run dev` again.

### Port stuck / `EADDRINUSE` / browser shows 500 on an old `localhost:PORT`

If **`npm run dev` prints `EADDRINUSE`**, that port is still held by a **zombie `node` process** (often an old `next dev` you closed the terminal for). The browser can still “open” that port and show a blank or **500 Internal Server Error**, but that is **not** your new project — it is the old broken server.

**On macOS, free a port and restart** (only kill what you intend — copy one line at a time):

```bash
# see what is listening (example: port 3100)
lsof -nP -iTCP:3100

# stop everything listening on 3100 (use after you confirm the PID is node/next)
lsof -ti :3100 | xargs kill -9
# repeat for 3005 or 3210 if needed
lsof -ti :3210 | xargs kill -9
```

Then in `hookedshorts-site`:

```bash
rm -rf .next
npm run dev
```

**One-off custom port** (if 3210 is also taken): `npx next dev -p 4000` and open `http://localhost:4000`.

### Automated smoke test (build + static files)

```bash
npm run verify
```

Runs `next build` and checks `GET /` and `GET /privacy.html` on a short-lived static server. Pass = production export is healthy.

## Production build (static export)

```bash
npm run build
```

Outputs static files to `out/`. This project uses `output: 'export'` in [`next.config.ts`](next.config.ts); there is no Node server. Preview locally with any static file server, for example:

```bash
npx serve out
```

## Deploying to hookedshorts.com

### Option A — Vercel

1. Create a Vercel project from this directory (or connect the Git repo and set **Root Directory** to `hookedshorts-site`).
2. **Framework preset:** Next.js. **Build command:** `npm run build`. This repo uses `output: 'export'`, so the static files are written to `out/`. In **Project → Settings → Build &amp; Development**, if the UI asks for an **Output Directory** (or the deployment 404s), set it to **`out`**.
3. In the Vercel project → **Domains**, add `hookedshorts.com` and `www.hookedshorts.com` if you use it.
4. At your DNS registrar, set the records Vercel shows (typically **A** / **CNAME** for apex or `www`).

### Option B — Cloudflare Pages

1. Connect the repo (or upload `out/` from CI) with **Root directory** `hookedshorts-site`.
2. **Build command:** `npm run build`. **Build output directory:** `out`.
3. Attach the custom domain `hookedshorts.com` in Cloudflare Pages and follow DNS instructions (often CNAME to `*.pages.dev` or A/AAAA as documented).

### DNS checklist (registrar)

- Point the **apex** (`hookedshorts.com`) and **`www`** as required by your host (Vercel: A/ALIAS or CNAME; Cloudflare: CNAME flattening or A records).
- Wait for propagation; use `dig hookedshorts.com` or an online DNS checker if needed.

### HTTPS and review checks

After deploy:

```bash
curl -sI https://hookedshorts.com | head -n 5
curl -sI https://hookedshorts.com/privacy | head -n 5
curl -sI https://hookedshorts.com/terms | head -n 5
curl -sI https://hookedshorts.com/data-deletion | head -n 5
```

Expect HTTP `200` and no certificate errors in the browser. TikTok Developer forms typically need **stable HTTPS URLs** for website, privacy policy, and terms.

## TikTok Developer — suggested URLs

| Field | URL |
|-------|-----|
| Website / company site | `https://hookedshorts.com/` |
| Privacy Policy | `https://hookedshorts.com/privacy` |
| Terms of Service | `https://hookedshorts.com/terms` |
| Data deletion (if required) | `https://hookedshorts.com/data-deletion` |

## Project layout

| Path | Purpose |
|------|---------|
| `app/` | Routes: home, about, services, contact, legal pages, `sitemap.xml`, `robots.txt` |
| `components/` | `SiteHeader`, `SiteFooter`, `Prose` |
| `lib/site.ts` | **Edit here** for brand, legal entity, email, base URL |

## License

Private; content © the legal entity named in `lib/site.ts`.
