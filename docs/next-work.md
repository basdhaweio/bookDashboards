# Next work — dashboard side

Companion to [`../CHANGELOG.md`](../CHANGELOG.md) (what was built and why) and
[`proposals-contract.md`](./proposals-contract.md) (what the bookdb/jerry side
needs to build).

This file is the backlog for **this repo**. Items are ordered so that the ones
unblocking other work come first. Each says plainly whether it is ready to pick
up or waiting on something.

Counts below were measured against `data/bookdb.json` generated
`2026-07-26T21:04:31Z`, owner filter `butthead`. Re-measure before trusting them.

---

## 1. There is no test harness in the repo — **ready, do this first**

Everything on this branch was verified by driving the page with Playwright, but
those scripts lived in a session scratchpad and are gone. The repo has no way to
check a change beyond opening the page and looking at it, which makes every item
below riskier than it needs to be.

**Build:** a small Node script that serves the repo and drives `index.html`
against the real `data/bookdb.json`. Chromium is already on the box at
`/opt/pw-browsers/chromium-1194/chrome-linux/chrome` — pass it as
`executablePath` and do **not** run `playwright install`.

What it needs to assert, because these are the things that actually broke during
development:

- all three owner views (`butthead`, `goblin`, `all`) render with zero
  `pageerror` events
- no horizontal overflow at 390px wide on Wishlist and on the flyer
- correction lifecycle survives a reload: mark → set date → hide → reload →
  restore → clear
- proposal round-trip: export → fresh browser context with empty localStorage →
  import reproduces the correction set **exactly** (compare normalised, not by
  string — key insertion order differs and will produce a false failure)
- the Wishlist buckets are non-empty where expected, so a silent regression in
  `buildWishlist` fails loudly

`Chart is not defined` in a sandboxed run is expected — Chart.js comes from a
CDN. Don't chase it, and don't assert on it.

## 2. Three date parsers now coexist — **ready**

| Function | Used by | Behaviour |
|---|---|---|
| `relTier` | Wishlist | precision tiers, year inference, day-1 handling |
| `relSort` | `renderComingUp` | month only, ignores the day entirely |
| `bcRelParts` | flyer | month + day, no precision concept |

`relSort` is the one that matters: **14 of 32** dated Interested rows are day-1
placeholders, and Coming Up sorts them purely by month with no notion that the
day is fabricated. It doesn't currently *print* a false day, so this is not
user-visible breakage — it's two competing models of the same data in one file,
and the next person to touch either will have to work out which is authoritative.

**Do:** move `renderComingUp` and `bcRelParts` onto `relTier`, delete `relSort`.
Keep the flyer's `Date TBA` label and its month-name grouping — only the parsing
underneath should change. Watch for `bcRelParts` returning `mo: 99` as its
sentinel; `relTier` uses `tier: 'tba'` instead.

## 3. Wishlist now fully overlaps three existing tabs — **needs your decision**

Measured, not estimated:

| Tab | Rows | Also in Wishlist |
|---|---|---|
| Someday | 47 | **47 (100%)** |
| Coming Up | 32 | **32 (100%)** |
| To Acquire | series-level | ranking overlaps |

Every Someday row is in the Wishlist's *No date yet* bucket. Every Coming Up row
is in one of the dated buckets. Four tabs under Buying are now answering
versions of the same question with different framing and no cross-links.

This is a product call, not a refactor — don't let an agent decide it. The
plausible shapes:

- **Keep all four, cross-link them.** Least disruptive. Coming Up stays the
  calendar, Wishlist stays the decision board, Someday and To Acquire keep their
  own framing. Add "see this in the Wishlist" links.
- **Fold Someday into Wishlist.** Its 47 rows are already the *No date yet*
  bucket. Someday's only unique signal is the `Someday` flag in the `Ordered`
  column, which the Wishlist already scores (`-35`).
- **Fold Coming Up in too**, leaving Wishlist as the single Buying surface.
  Loses the calendar view, which is genuinely a different job.

My read: fold Someday, keep Coming Up. But it's your dashboard.

## 4. Wishlist cards have no cover art — **ready, cheap**

The flyer already fetches Open Library covers with a localStorage cache
(`bcFetchCovers`, 30-day hits / 3-day misses). The Wishlist shows none. Reusing
that path on the buy-now bucket would make the board scannable.

Keep the existing cache TTLs — the short miss TTL exists because a cover often
appears only at release.

## 5. Corrections are per-browser — **accepted for now, revisit if it bites**

`localStorage` only. Moving machines means Export → Import by hand. That is a
consequence of the no-write-path rule and is currently fine.

If it becomes annoying, the option that does **not** break the rule is loading a
committed corrections file as a read-only baseline that localStorage overlays.
Do not add a write path to the register to solve this.

## 6. "Already proposed" state — **blocked on jerry**

The Wishlist can't tell that a title already has an open proposal, so the same
correction can be exported twice. Fixing it needs open proposals echoed back in
`bookdb.json` — item 4 in
[`proposals-contract.md`](./proposals-contract.md#things-to-settle-when-you-build-the-table).

Once that field exists, the dashboard side is small: read it in `loadBundle`,
match on the same key, badge the card.

## 7. Schema reconcile — **blocked on jerry**

`wlBuildProposals` invents `bookdb.proposals/v1` because no proposals table
existed and jerry wasn't reachable. When the real table lands, remap it — one
function out, one (`wlImport`) in.

**If the envelope changes, regenerate [`sample-proposals.json`](./sample-proposals.json).**
A stale fixture is worse than no fixture, because the jerry side will code
against it.

## 8. Notification feed — **blocked on jerry, highest value there**

The `Book Club` source tab is header-only. It is the only reliable date source
in the system. Parsing, tier upgrading and corroboration scoring are already
built and dormant on this side — populating the tab needs **no dashboard
change**. See the contract for expected columns.

---

## Smaller items

- `WL_CAP` is 36 per bucket. Overflow is stated in the UI, never silent — keep
  it that way if you change the number.
- `wlGenreTokens` knows fantasy / scifi / horror / history. Add tokens if the
  `Sub` column in Book Mail Orders grows new values, otherwise sub-credit
  overlap flags will silently stop firing for them.
- `WL_PSEUDO` is the list of series names treated as groupings rather than
  ordered series. If a real series name ever matches it (something containing
  "Collection", say), it will be excluded from shelf-gap inference — which fails
  safe, but check here first if a gap you expect isn't appearing.

## Do not undo these

Both look like over-caution until you know what they prevent. Both are explained
at length in [`../CHANGELOG.md`](../CHANGELOG.md).

- **Month-precision releases wait for the whole month to elapse** before counting
  as out. This is what stops an unknown day from surfacing a book early.
- **"Shelf gap", not "sequence-proven".** The wording is deliberately weak
  because Old Man's War numbers short stories inline with novels — clean, unique,
  integer sequences that are still not publication order, with no data signal to
  detect it.
