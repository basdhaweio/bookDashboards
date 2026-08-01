# Proposals contract — dashboard → bookdb

**Audience: a session working on `bookdb` on jerry, not on this repo.**

This document is self-contained. You do not need to read the dashboard source to
implement against it. A real generated example lives beside this file at
[`sample-proposals.json`](./sample-proposals.json) — code against that fixture.

The dashboard-side backlog is in [`next-work.md`](./next-work.md); items 7, 8
and 9 there are the ones waiting on decisions made here.

---

## Why this exists

The dashboard (`bookDashboards`, served from GitHub Pages) surfaces a **Wishlist**
that answers "what's the next book to buy". To do that it infers things: that a
release string means a certain month, that an unowned volume sitting below an
owned one must already be published, and so on. **Some of those inferences are
wrong**, and the register must never absorb a wrong one silently.

So the dashboard lets a human correct any card by hand, and those corrections
leave as **proposals** — opened against the register, never applied. They join
the same review queue as every other bookdb change, and get closed or withdrawn
in batches, exactly as in the existing history:

```
5e526c1  data: stronger event matching; withdraw 92 duplicate book proposals
b42dd6c  data: batch 4 - 28 proposals closed
af61a0a  data: batch 6 - all proposals closed except Star Wars
```

The bar is: **nothing automated or non-manual enters the register without
review.**

## Current data flow

```
bookdb (jerry)  ──nightly export──▶  bookDashboards/data/bookdb.json  ──▶  GitHub Pages
                                     pushed by bookdb-publish <bookdb@jerry-eq.local>
```

That is the only automated channel, and it runs **one way**. The dashboard has
no write path back: two `fetch` calls, both reads, no POST/PUT/PATCH.

**Proposals travel by hand.** A human clicks *Export proposals* in the browser,
gets a JSON file, and carries it to jerry. That is deliberate — do not build an
automated ingest endpoint. The manual step *is* the review gate.

---

## The envelope

One file per export. Top level:

| Field | Type | Notes |
|---|---|---|
| `schema` | string | `"bookdb.proposals/v1"` — version and reject anything else |
| `generated_at` | ISO 8601 | when the export was taken |
| `generated_by` | string | `"The Library dashboard · Wishlist tab"` |
| `generated_against` | ISO 8601 \| null | `generated_at` of the `bookdb.json` the human was looking at |
| `owner` | string | `butthead` \| `goblin` \| `all` — the owner filter that was active |
| `policy` | string | human-readable statement of the never-applied rule |
| `counts` | object | `total`, `applies_to_register`, `by_kind`, `unresolved` |
| `proposals` | array | see below |

`generated_against` is the staleness check. If it is older than the current
register state, the `current` blocks inside may describe a register that has
since moved — **compare before applying**, don't assume.

### A proposal

| Field | Type | Notes |
|---|---|---|
| `id` | string | `wl-0001` — unique **within this file only**, not a global ID |
| `status` | string | always `"open"` on export |
| `origin` | string | always `"manual"` — see [Machine inferences](#machine-inferences-are-never-exported) |
| `applies_to_register` | bool | `false` means dashboard-only; do not write it to the register |
| `kind` | string | one of the five below |
| `target` | object | which row this is about |
| `proposed` | object | the asserted value |
| `current` | object | what the dashboard believed instead |
| `evidence` | string[] | why, in plain language — carry this into the review queue |

### Kinds

| `kind` | `applies_to_register` | `proposed` |
|---|---|---|
| `release_date.set` | true | `{release_date, precision, as_entered}` |
| `availability.set` | true | `{published: bool}` |
| `annotation.add` | true | `{note: string}` |
| `order.received` | true | `{received_date, received_date_long, delivered}` |
| `wishlist.exclude` | **false** | `{excluded_from_wishlist: true}` |

### `order.received` — arrival dates

Targets **Book Mail Orders**, not the book catalog, so its `target` block has a
different shape (see below). Recording an arrival asserts two things at once —
the date, and that the order is now fulfilled — so the proposal carries both
rather than leaving `Delivered` to be inferred on ingest:

```json
"proposed": { "received_date": "2026-07-14",
              "received_date_long": "July 14, 2026",
              "delivered": "Fulfilled" },
"current":  { "received_date": null, "delivered": "Unfulfilled", "paid": "Paid" }
```

`received_date` is ISO `YYYY-MM-DD`. `received_date_long` is the same date
preformatted to match the sheet's existing `Fulfil` column style
(`"June 3, 2025"`) — use whichever your storage wants; they are the same day.
The conversion is done by string parts, not `new Date()`, because parsing a bare
ISO date shifts it a day through UTC.

**These dates are asserted, not observed.** Receipt gets recorded days after the
box actually turns up, so the picker accepts any past date and is never
auto-stamped with today. Every one of these proposals carries the evidence line
*"receipt is recorded after the fact, so this date is asserted rather than
observed"*. An arrival on a still-`Unpaid` order gets an extra line saying so —
worth a second look before closing, since it may mean the payment side is also
out of date.

All 57 currently-fulfilled orders have a `Fulfil` date, so treat that column as
required once `Delivered` flips to `Fulfilled`.

### Targeting

```json
"target": {
  "register": "Butthead Individual / Interested",
  "series": "Kingkiller Chronicles",
  "title": "The Doors of Stone",
  "match": "series+title, case- and punctuation-insensitive",
  "key": "kingkiller chronicles::the doors of stone"
}
```

`key` is `normalise(series) + "::" + normalise(title)` where normalise is
lowercase, all non-alphanumerics collapsed to single spaces, trimmed.

`order.received` targets an order row instead, and its key is
`normalise(order) + "|" + normalise(ordered_date) + "|" + normalise(books)`:

```json
"target": {
  "register": "Book Mail Orders",
  "order": "#TBBSUB669328",
  "ordered_date": "May 1, 2026",
  "books": "Ruin",
  "match": "order number + ordered date + books, case- and punctuation-insensitive",
  "key": "tbbsub669328|may 1 2026|ruin"
}
```

It is composite because **12 of 22 pending orders have no order number** — they
are subscription credits with no title assigned yet. Any one part may be empty;
the ordered date is what makes those rows distinguishable.

**This is the weakest part of the contract.** It breaks on a rename. If bookdb
has stable row IDs, switching to them is the single highest-value change you can
make — and it needs a matching change on the dashboard side so the IDs come
through in the export. When the dashboard can't resolve a key against the
current export it sets `target.unresolved` with an explanation and counts it in
`counts.unresolved`; treat those as needing a human.

---

## Things to settle when you build the table

These were left open deliberately — they need a decision on the bookdb side.

1. **Field names are a guess.** No proposals table existed when this was
   written, and jerry wasn't reachable. Rename freely; the dashboard side is one
   function (`wlBuildProposals`) plus its inverse in `wlImport`.

2. **`precision` must survive.** The date model's whole point is distinguishing
   `"September 14"` from `"September"` — the sheets use day-1 as a "sometime that
   month" placeholder, and 14 of 32 dated rows on the Interested sheet assert a
   day nobody published. If the register flattens `precision` into a bare date,
   the tier system is pointless and the dashboard will start printing days that
   were never published. Store it as a first-class field.

   `precision` is `day` | `month` | `none`. `release_date` is `YYYY-MM-DD` for
   `day`, `YYYY-MM` for `month`, `null` for `none`. `as_entered` preserves what
   the human actually typed.

3. **Does `wishlist.exclude` belong at all?** It's currently marked
   `applies_to_register: false` because hiding a card is a display preference,
   not a claim about a book. It may deserve to stay dashboard-only forever — in
   which case just drop those on ingest.

4. **Should open proposals flow back out?** Right now the dashboard can't tell
   that a title already has an open proposal, so a human could propose the same
   thing twice. If the nightly export carried open proposals keyed the same way,
   the dashboard could mark those cards and stop the duplicate. Worth doing;
   needs a field in `bookdb.json` and a read on the dashboard side.

5. **`id` is file-local.** If you want stable proposal identity across exports,
   that has to come from the register after ingest.

---

## Machine inferences are never exported

This is the part to preserve if you change anything else.

The dashboard infers "this book is already published" from sequence position — a
volume sitting below one you already own. That inference is **recomputed on every
page load and discarded**. It cannot become a proposal on its own. The only route
is a human clicking through it on the card, which converts it into a manual
assertion.

When a human correction *contradicts* an inference, the proposal records both:

```json
"kind": "availability.set",
"proposed": { "published": false },
"current":  { "published_inferred": true,
              "basis": "shelf gap — sequence position below an owned volume" },
"evidence": [ "marked not-yet-published by hand on the Wishlist tab",
              "overrides a shelf-gap inference — the sequence column
               was not publication order here" ]
```

That `evidence` is the whole value of the format — keep it in the review queue
where a reviewer will see it.

**Why it matters concretely:** Old Man's War numbers its short stories and
novellas inline with the novels, so #5 is the second novel and the sequence
column is not publication order at all. The dashboard already refuses to infer
from series whose numbering repeats, starts at 0, runs fractional, or is a
grouping — but Old Man's War is clean, unique, integer, and still wrong, and no
data signal distinguishes it. Human override is the only correction available,
so the record of that override needs to survive into the register.

---

## The other open item: the notification feed is empty

Not proposals, but it's the highest-value thing on the jerry side.

The `Book Club` tab in the source spreadsheet is **header-only, zero rows**. It
is the only reliable date source in the whole system — publisher/retailer feed,
exact dates, ISBNs. With it empty:

- every date in the dashboard is hand-typed off the Interested sheet
- nothing can reach the `confirmed` precision tier
- the Book Club flyer runs on one source instead of two

The dashboard already handles this feed — parsing, tier upgrading, and
corroboration scoring are all built and dormant. Populating the tab lights them
up with **no dashboard change required**.

Expected columns: `Title, Author, Series, Release, Genre, Notes, ISBN`.

This is also the real fix for the Old Man's War class of problem: an actual
publication date beats any inference from sequence position.

---

## Checklist for the jerry session

- [ ] Decide the proposals table shape; reconcile names with this envelope
- [ ] Keep `precision` first-class on release dates
- [ ] Ingest: validate `schema`, check `generated_against` for staleness, open
      every row as `status: open`, carry `evidence` into the queue
- [ ] Skip or drop `applies_to_register: false` rows
- [ ] Route `target.unresolved` rows to a human
- [ ] Decide on stable row IDs — and if yes, say so, because the dashboard
      export needs the matching change
- [ ] Consider echoing open proposals back in `bookdb.json` to prevent duplicates
- [ ] Populate the `Book Club` notification feed tab
- [ ] Do **not** build an automated ingest endpoint — the manual carry is the gate
