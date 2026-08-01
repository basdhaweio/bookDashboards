# Changelog & handoff — Wishlist / upcoming books

Branch: `claude/upcoming-books-dashboard-hqgre9`
Written 2026-07-27. Everything below is in `index.html`; there is no build step.

This file exists so a later session can pick the work up cold. Read
**[Open work](#open-work)** first — the rest is context for why things are the
way they are.

**Handoff docs:**

| File | For |
|---|---|
| this file | what was built on the dashboard and why |
| [`docs/next-work.md`](docs/next-work.md) | **the backlog — start here to pick up work** |
| [`docs/proposals-contract.md`](docs/proposals-contract.md) | the bookdb/jerry side; self-contained, no dashboard context needed |
| [`docs/sample-proposals.json`](docs/sample-proposals.json) | real generated fixture to code the ingest against |

---

## What was added

Three commits on top of `main`:

| Commit | What |
|---|---|
| `6d584b7` | Wishlist tab — rank what to buy next without trusting publish dates |
| `789a8c8` | Gate Goodreads sequence inference, per-card corrections, restock the flyer |
| `cfe02d5` | Export corrections as bookdb proposals rather than a raw override blob |

### 1. Wishlist tab (Buying → Wishlist)

**The problem.** The *Coming Up* tab answers "what is coming out", which needs
the dates to be right. They aren't: release strings on the Interested sheet are
hand-typed, carry **no year at all**, and use day 1 as a "sometime that month"
placeholder — 14 of 32 dated rows assert a day nobody published.

**The fix.** Wishlist answers "what do I buy next" instead, and ranks on inputs
that don't depend on a date being correct. *Coming Up* was left alone as the
calendar.

Releases are classified into a precision **tier** rather than parsed into a date
(`relTier`):

| Tier | Meaning | UI may print |
|---|---|---|
| `confirmed` | exact day, corroborated by the notification feed | the day |
| `dated` | exact day, current year assumed | the day |
| `month` | day-1 or bare month | **month only, never a day** |
| `tba` | nothing asserted anywhere | "No date yet" |

A `month`-tier book only counts as released once the **whole month** has elapsed
(`relReleased`). That's deliberate: a missing day can make a book surface late,
never early. Buckets are availability-first — On shelves now / Preorder window
(exact-day only, ≤60 days) / Watching / No date yet.

Candidates merge three sources by the job each actually does:

- **Interested sheet** — intent, unreliable dates
- **Book Notification feed** — the only date authority; upgrades a matching
  row's tier instead of duplicating it
- **Catalog** — where Goodreads seeds named future volumes long before a date
  exists. Only the **lowest pending sequence per series** is listed (that's the
  book you actually buy next); later volumes are counted and reported.

Anything owned, read, or matched to a Book Mail order is suppressed before
ranking. Open subscription credits with no title yet **flag** same-month
releases in a matching genre rather than hiding them.

### 2. Sequence trust gate

Inferring "already published" from a shelf gap (an unowned volume sitting below
one you own) only holds when the sequence column is genuinely an order. Often it
isn't. `wlSeqTrust` refuses a series when its numbering:

- repeats — Grand Tour has sixteen entries all numbered `1`
- starts at 0 — side material interleaved
- runs fractional — novellas interleaved
- has unsequenced volumes
- or the series name matches `WL_PSEUDO` (Standalones, Short Story Collections,
  Graphic Novels, …) — a grouping, not a series

This dropped Buy Now from 16 → 9, all genuine backlist, and killed false claims
like "An Arbiter's Gift is out" inferred from a bucket named *Standalones*.

**Known blind spot — read this before touching the rule.** Old Man's War has
seqs 1–9: unique, integer, clean, and completely useless as publication order,
because the short stories and novellas are numbered inline with the novels
(#5 is the second novel). Nothing in the data distinguishes it. That is *why* a
gap is presented as a claim with its assumption printed on the card
("assuming the sequence column is publication order") and the badge reads
**shelf gap**, not "sequence-proven". Don't restore the stronger wording.

Doors of Stone was always handled correctly — Kingkiller #3 against #1 and #2
owned is not a gap — and stays undated.

Goodreads also defers to manual entry: a catalog series is skipped when its
series **or** its next title already appears on the Interested sheet.

### 3. Corrections → proposals

Every card carries **Out now · Not out · Date… · Note… · Hide** (plus Reset).
Corrections beat every inference, survive reloads via `localStorage`
(`WL_OV_KEY`), keep a corrected title visible even when suppression would have
dropped it, and hidden titles get their own restorable section.

They export as a `bookdb.proposals/v1` envelope — typed proposals opened against
the register, **never applied**. Book Mail arrivals (below) share the same
envelope, so one export covers both surfaces:

| Kind | `applies_to_register` |
|---|---|
| `release_date.set` | yes — value + precision |
| `availability.set` | yes — published true/false |
| `annotation.add` | yes — note |
| `order.received` | yes — arrival date + fulfilled status |
| `wishlist.exclude` | **no** — hiding a card is a display preference |

Each carries `current` alongside `proposed` so a reviewer sees the delta, plus
the evidence that produced it, plus `generated_against` (the nightly export's
timestamp) so a stale proposal is visibly stale.

**Machine inferences are not exportable.** A shelf gap is recomputed on every
load and discarded. The only route into a proposal is a human clicking through
it on the card. When a correction contradicts an inference the proposal says so
outright — marking Grave Peril unpublished exports with *"overrides a shelf-gap
inference — the sequence column was not publication order here"*.

### 4. Order arrival dates (Book Mail tab)

Receipt doesn't get recorded the day the box arrives, so arrival is **picked,
not stamped**. Every pending order row carries a native date picker capped at
today — backdating is the normal case, and nothing is ever auto-filled with
today unless you press the *Today* shortcut.

Recording an arrival asserts two things (the date, and that the order is now
fulfilled), so the proposal carries both rather than leaving `Delivered` to be
inferred on ingest. Kind is `order.received`; it targets **Book Mail Orders**
rather than the catalog, so its target block has its own shape.

Order keys are composite — `order + ordered_date + books` — because **12 of 22**
pending rows have no order number at all (subscription credits with no title
assigned yet). Any one part may be empty; the ordered date is what keeps those
rows distinguishable.

An arrival logged against a still-`Unpaid` order gets an extra evidence line
saying so, since it usually means the payment side is stale too.

### 5. Book Club flyer

It was populated (32 items) but stale: a **July** flyer opened with April and
May, every card stamped HAVE IT, because it had no time filter. Now:

- past months already secured are dropped; past months still unacquired are
  relabelled **"Still on the shelf"**
- **Your pending orders** — unfulfilled Book Mail orders, paid vs unpaid
- **Things to buy — out now** — top of the wishlist, deduped against what's
  already printed
- the footer says outright when the notification feed is empty

---

## Open work

### 1. The proposals schema is invented — reconcile it

**This is the main thing.** `bookdb` on jerry wasn't reachable from the session
that wrote this, and there is no proposals table yet. The envelope in
`wlBuildProposals` is **this dashboard's own guess** at your conventions. It is
self-describing and states its policy inline specifically to make remapping
cheap.

The `git log` vocabulary it was modelled on:

```
5e526c1  data: stronger event matching; withdraw 92 duplicate book proposals
b42dd6c  data: batch 4 - 28 proposals closed
cc58499  data: ... all fix proposals closed
af61a0a  data: batch 6 - all proposals closed except Star Wars
```

So proposals get **opened → reviewed → closed or withdrawn**, in batches. The
envelope matches that lifecycle (`status: "open"`) but the field names are
guesses.

The full contract, written for the jerry side, is in
[`docs/proposals-contract.md`](docs/proposals-contract.md), with a real
generated fixture at [`docs/sample-proposals.json`](docs/sample-proposals.json).

**To do:** once the real proposals table exists, remap `wlBuildProposals` to it.
Contained change — one function builds the envelope, one consumes it on import.
Things to settle when you do:

- real field names / table shape, and whether `kind` values match yours
- how a proposal targets a row. Currently `series + title`, normalised
  case- and punctuation-insensitively (`wlNorm`). If bookdb has stable row IDs,
  use those instead — the current key breaks on a rename, which is why
  `counts.unresolved` exists and is surfaced in the Review panel.
- whether `wishlist.exclude` belongs in the register at all. It's currently
  flagged `applies_to_register: false` because hiding a card is a display
  preference, not a claim about a book. It may deserve to be dashboard-only
  forever.
- whether the register wants `precision` as a first-class field. The whole
  date model depends on distinguishing "September 14" from "September", and
  flattening that back to a bare date would undo the point of the tier system.

### 2. The Book Notification feed is empty

The `Book Club` tab in the source sheet is **header-only, zero rows**. It's the
only real date authority in the system, so right now every date is hand-typed
and nothing can reach the `confirmed` tier. The tier and the corroboration bonus
are built and will light up the moment rows land there — no code change needed.

This is also the honest fix for the Old Man's War blind spot: real dates beat
inferred ones.

### 3. Smaller things

- `WL_CAP` is 36 per bucket; overflow is stated in the UI, not silent.
- Corrections are per-browser (`localStorage`). Moving between machines means
  Export → Import. Fine for now; worth revisiting if it gets annoying.
- The genre matcher for sub-credit overlap (`wlGenreTokens`) knows
  fantasy / scifi / horror / history. Add tokens there if the `Sub` column
  grows new values.

---

## Where things live

All in `index.html`, single file, no build. Grep by function name — line numbers
drift.

| Area | Functions |
|---|---|
| Date tiers | `relTier` `relLabel` `relReleased` `relDaysOut` |
| Sequence trust | `wlSeqTrust`, `WL_PSEUDO` |
| Candidate build + scoring | `buildWishlist` |
| Bucket copy | `WL_BUCKETS` |
| Rendering | `renderWishlist` |
| Correction actions | `wlAct` `wlPatch` `wlOvLoad` `wlOvSave` |
| Proposals | `wlBuildProposals` `wlExport` `wlImport` `wlRenderReview` |
| Flyer | `bcBuildItems` `bcPendingOrders` `bcThingsToBuy` `renderBookClub` |

Data comes from `data/bookdb.json` (nightly export from bookdb on jerry), read
by `loadBundle`. Sheet tabs are keyed `<spreadsheetId>|<tabName>`.

**The page has no write path to the register** — two `fetch` calls, both reads
(the nightly export, Open Library covers), no POST/PUT/PATCH, and three
`localStorage` keys (wishlist corrections, book club order checkboxes, cover
cache). Keep it that way: corrections leave as proposals for review, they don't
write anything.

## Testing

No test suite. Verification was done by driving the page with Playwright against
the real `data/bookdb.json` — chromium is at
`/opt/pw-browsers/chromium-1194/chrome-linux/chrome`, served with
`python3 -m http.server`. Worth re-checking after changes:

- all three owner views (`butthead`, `goblin`, `all`) render with no page errors
- no horizontal overflow at 390px wide
- correction lifecycle: mark → set date → hide → reload → restore → clear
- proposal round-trip: export → fresh browser with empty localStorage → import
  reproduces the correction set exactly

Chart.js loads from a CDN, so `Chart is not defined` in a sandboxed run is
expected and unrelated.
