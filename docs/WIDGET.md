# Phone widget (Android, KWGT)

John's home-screen widget for The Library (2026-09-30). A browser can't put a
web page on the Android home screen as a *widget*, so the widget is built in
**KWGT (Kustom Widget)** from two things this repo publishes, and every button
is a link into the installed app (Firefox "add to home screen").

Nothing on the phone holds the inbox token: all writes still happen inside the
dashboard after a tap.

## What jerry publishes (every publish, nightly + within a minute of any inbox event)

`jobs/publish_widget.py` on jerry (called from `publish.sh`, never blocks the
bundle) writes beside `data/bookdb.json`:

- **`data/widget.json`** — small (a few KB), Com's numbers:
  - `year`: `read`, `target`, `pace` (target × day-of-year ÷ 365), `last_year_by_now`, `last_year_total`, `this_month`, `bought`
  - `reading_now[]`: Goodreads currently-reading — `title`, `author`, `since`, `cover`, `register` (the register title it matched, or "")
  - `next_up[]`: the hand-picked queue in order — `title`, `series`, `seq`, `owned`
  - `orders`: `open`, `shipped`, `lines[]` (`title`, `store`, `status`, `eta`, `tracking`, `order_id`) — shipped first, then by ETA
  - `proposals.open`
  - `charts[]`: `id`, `title`, `light`, `dark` (PNG URLs)
  - `links`: `scan`, `add`, `order`, `tbr`, `orders`, `proposed` (full URLs)
- **`data/widget/<chart>.png` and `<chart>-dark.png`** — 960×520 charts, the
  dashboard's palette: `progress` (cumulative reads this year vs last, target
  pace line), `history` (books per year), `months` (this year vs last by
  month), `buying` (bought per month + hatched on-order ETAs).

Everything is public, like the bundle it is drawn from.

## Action links the dashboard understands

| link | lands on |
|---|---|
| `…/#scan` | Log › Books with the ISBN camera open (Firefox has no `BarcodeDetector`; the page loads ZXing from jsDelivr on first use) |
| `…/#add` | Log › Books, search box focused — type or paste a title, pick from the register or the live web results |
| `…/#order` | Log › Orders, new-order form at the top (store chips) |
| `…/#order/184` | Log › Orders with order 184's card expanded (Arrived today / Shipped per line) |
| `…/#tbr` (or `#read`) | Reading › TBR (✓ Read on the cards) |

`…` = `https://basdhaweio.github.io/bookDashboards/`. The hash is also
honoured when the app is already open (a `hashchange` listener), then settles
to the ordinary `#section/tab` form.

## The app (recommended) — `android/`

A native Android app with a real home-screen widget, built by GitHub Actions
(`.github/workflows/android-build.yml`) on every push to `android/` and kept at
a fixed URL:

**https://github.com/basdhaweio/bookDashboards/releases/download/android-latest/library.apk**

Open that on the phone, allow installs from the browser once, install, then
long-press the home screen → Widgets → **Library**. The widget shows the four
buttons, reading now, the year line, three order lines (tap → the order card),
the chart (tap → next chart) and a status line (tap → refresh); it follows the
system light/dark theme and refreshes every 30 minutes. Details in
`android/README.md`. The KWGT route below still works if you prefer it.

## KWGT recipe

Install **KWGT Kustom Widget Maker** from the Play Store (the Pro key, a few
dollars, may be needed for the web-fetch formula — check in the app). Place a
KWGT widget on the home screen (4×3 or 4×4 works for the layout below), tap it
to open the editor, then build:

**Globals** (tab "Globals"): add a **List** global named `chart` with items
`progress`, `history`, `months`, `buying` — this is the chart picker.

**Formulas.** `WJ` below stands for the JSON URL:
`https://basdhaweio.github.io/bookDashboards/data/widget.json`

1. **Button row** — four Text items (or Shapes with text) in an Overlap/Stack
   layer, each with *Touch → Open Link*:
   - `Scan` → `https://basdhaweio.github.io/bookDashboards/#scan`
   - `Add` → `…/#add`
   - `Order` → `…/#order`
   - `Read` → `…/#tbr`
2. **Reading now** — Text item:
   `Reading: $wg(WJ, json, "$.reading_now[0].title")$`
   (a second line for `$.reading_now[1].title` if you read two at once)
3. **Year line** — Text item:
   `$wg(WJ, json, "$.year.read")$ read · pace $wg(WJ, json, "$.year.pace")$ · $wg(WJ, json, "$.year.last_year_by_now")$ this time last year`
4. **On order** — one Text item per line (3–4 rows):
   `$wg(WJ, json, "$.orders.lines[0].title")$ · $wg(WJ, json, "$.orders.lines[0].status")$ $wg(WJ, json, "$.orders.lines[0].eta")$`
   with *Touch → Open Link*:
   `https://basdhaweio.github.io/bookDashboards/#order/$wg(WJ, json, "$.orders.lines[0].order_id")$`
   — tapping the row opens that order's card. Header text:
   `On order: $wg(WJ, json, "$.orders.open")$ ($wg(WJ, json, "$.orders.shipped")$ shipped)`
5. **Chart** — an Image item whose Bitmap is a formula:
   `https://basdhaweio.github.io/bookDashboards/data/widget/$gv(chart)$$if(si(darkmode)=1, "-dark", "")$.png`
   *Touch → Change Global → chart → next* cycles progress → history → months
   → buying. If `si(darkmode)` errors on your KWGT version, drop the `$if(...)$`
   part and choose `-dark` or not by hand.
6. **Proposals badge** (optional) — Text `$wg(WJ, json, "$.proposals.open")$ proposed`
   with *Touch → Open Link* `…/#log/logproposed`.

KWGT re-fetches web content on its own schedule (roughly every 15–60 minutes
while the screen is on); the JSON and PNGs change only when jerry publishes.

## Notes

- Buttons open the URL in the default browser; with Firefox as default and the
  site installed as an app, Firefox opens it in the app window.
- Same data, different owner: `publish_widget.py` has `OWNER = "butthead"`.
  A `widget-goblin.json` for Shereen is a small change if she wants one.
- Server one-off: the `bookdb-jobs` image gained matplotlib (2026-09-30,
  `jobs/Dockerfile`); the previous image is tagged `bookdb-jobs:pre-mpl-20260930`.
