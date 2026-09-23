# Inbox event schema

The dashboard's **Log** view writes one JSON file per action to `inbox/` in the
inbox repo (configurable in the Log view's setup card; default
`basdhaweio/bookInbox`). jerry polls that directory, applies each event to
bookdb, moves the file to `inbox/processed/YYYY-MM/`, and re-exports
`data/bookdb.json` to the dashboards repo. The dashboard treats any file still
in `inbox/` as "pending".

## Envelope

Every event file:

```json
{
  "v": 1,
  "id": "mdl3k2-x9f2a",
  "at": "2026-07-27T01:23:45.678Z",
  "by": "butthead",
  "type": "finished",
  "bundle_generated_at": "2026-07-26T21:04:31.514777+00:00",
  "payload": { }
}
```

- `v` — schema version, currently 1. Reject unknown versions loudly.
- `id` — unique per event; used by the dashboard to reconcile its local
  optimistic copy with the inbox listing.
- `by` — `butthead` (Com) or `goblin` (Shereen). Decides whose reading log a
  `finished` event lands in.
- `bundle_generated_at` — the `generated_at` of the bundle the picker ran
  against. Book references carry the bundle's exact strings as of that export,
  so title matching should be **exact**; if a book ref doesn't match exactly
  (e.g. renamed since that export), park the event as a proposal rather than
  fuzzy-matching.

## Book reference

Events about an existing book carry the bundle's exact strings:

```json
"book": {
  "title": "The Way of Kings",
  "series": "Stormlight Archives",
  "seq": "1",
  "author": "Sanderson, Brandon",
  "media": "Print",
  "owner": "butthead"
}
```

Match on `(title, series, owner)`; the rest is corroboration.

## Event types

### `finished`
Mark the book read and append to the reader's yearly log.
```json
{"book": {…}, "date": "2026-07-26", "pages": 412}
```
`pages` may be null. `by` picks the yearly log (Com's Metrics sheet vs
Shereen's). Idempotency: skip if that (title, date) pair is already logged.

### `acquired`
Mark the book owned.
```json
{"book": {…}, "date": "2026-07-26", "source": "The Broken Binding"}
```
May arrive for an already-owned book (new copy/format) — apply your own rule;
the UI warns before sending. `source` is the shop the copy came from (the
acquisition's `source_store`, NOT the publisher) — optional, vetted against
the `source` vocabulary client-side. Acquiring also clears the book's `need`
flag: the hunt is over.

**Omnibus** (docs/OMNIBUS.md): when the acquired object is one volume
containing several works, add `omnibus_title` (the edition on the spine) and
`contains` (all works in the volume, in order — strings with optional
trailing ` #seq`; listing the anchor book itself is fine and fixes its
position):
```json
{"book": {…The Crown Conspiracy…}, "date": "2026-09-15",
 "source": "The Broken Binding", "omnibus_title": "Theft of Swords",
 "contains": ["The Crown Conspiracy #1", "Avempartha #2"]}
```
The consumer records one `copies` row, links the anchor, and runs every
other listed work through the same owned+dated-acquisition logic (exact
title match per owner; misses park as `add_book_from_inbox` proposals
carrying the copy id, so approving them completes the copy's contents).

**Edition** (docs/OMNIBUS.md, Editions): a single-work object — a hardback
of a book already owned in paperback, a special edition — rides the same
event as an `edition` object. `format` or `edition` (the edition's name)
is required; `replaces: true` retires the book's newest still-active
single-work copy (sold, donated, swapped out) and points it at the new one:
```json
{"book": {…}, "date": "2026-09-16", "source": "The Broken Binding",
 "edition": {"format": "Special Edition", "edition": "Broken Binding",
             "publisher": "Orbit", "replaces": true, "notes": "signed"}}
```
Counts stay work-denominated — the book is still one book; the copies
layer just knows one more object. `add_book` accepts the same `edition`
object (without `replaces`) for a new work that arrives as a specific
edition; the copy is recorded alongside the work on approval.

### `copy_let_go`
Retire a physical copy — sold, donated, swapped out — without touching the
work's owned status (that stays John-asserted through `acquired`).
```json
{"copy_id": 41, "date": "2026-09-16", "reason": "donated"}
```
`copy_id` is the Copies tab's Id; the copy must belong to `by`. Already-
retired copies are a no-op.

### `email_forward`
A newsletter forwarded raw by the Gmail Apps Script in `tools/bn_forwarder.gs`
(Book Notification's weekly digest). Stored verbatim in `email_forwards`
(unique on `message_id`); a separate parser job turns rows into the Book
Club feed and proposals — nothing is interpreted at intake.
```json
{"from": "Book Notification <…@booknotification.com>", "subject": "…",
 "date": "2026-09-08T12:00:00.000Z", "message_id": "18f…",
 "html": "<html>…</html>", "text": "…"}
```

`meta_add` also accepts kind `bn_author`: an author John follows on Book
Notification, recorded from the Book Club tab's "Authors to follow" list so
the dashboard can keep that list reconciled against the register.

### `add_book`
A book not in the catalog. Create as a **proposal** (existing bookdb review
flow), not a direct catalog insert.
```json
{"title": "…", "series": "", "seq": "", "author": "…", "media": "Print",
 "genre": "Fantasy", "sub_genre": "", "universe": "", "publisher": "",
 "pub_date": "", "need": false,
 "notes": "", "owner": "goblin", "owned": true, "read": false,
 "acquired_on": "2026-08-03", "read_on": "", "source_store": "Tombolo"}
```
Empty `series` means standalone. `acquired_on`/`read_on` are optional ISO
dates; when the approved proposal is applied they create the matching
acquisition/read events alongside the book, dated. Blank `media` defaults
to **Print** at apply time (John, 2026-09-12: "it's print unless it's a
comic or manga") — set it explicitly for anything that isn't. `source_store` is the
shop the copy came from (never the publisher); it rides the acquisition,
which is created when a date OR a shop is present — a shop with no date
records an undated acquisition (year NULL) rather than dropping the
provenance.

`universe` was accepted by the form but silently dropped when the approved
proposal was applied; since the collecting migration it decides whether a book
reaches the Collecting section, so it is now resolved (and created if new)
just like `series`.

Optional `isbn` (10 or 13 digits, hyphens tolerated): set by the Log view's
ISBN lookup/barcode scan — the edition actually in hand. On approval it is
written to `book_meta` (source `inbox`, confidence high), which makes the
new book cover-capable and directly scannable from the next publish on.
The nightly hydrator also stashes a Google Books ISBN into payloads it
touches, so Proposed cards can show covers.

`author` is free text as typed — the entry forms hint natural order
("Sophie Jordan"), not the catalog's "Last, First". Normalize to catalog
convention during proposal review; the form does not auto-invert because
that mangles names like "Ursula K. Le Guin" or "SenLinYu".

**Omnibus** (docs/OMNIBUS.md): `omnibus: true` flips the payload's meaning —
`title` becomes the EDITION title (never a register row) and `contains`
lists the works, one entry each (optional trailing ` #seq`). Shared fields
(series/author/genre/media/universe/…) apply to every contained work. On
approval, each work is created — or linked when a same-owner title already
exists — with owned/read and dated events applied per work, and the copy +
contents recorded. `copy_id`/`copy_position` appear only on jerry-parked
proposals from an omnibus Got-it or arrival; clients never send them.

### `book_update`
Edit register fields on an existing book — the Log view's ✎ action. Same
`book` reference as `finished`/`acquired`; `set` carries only changed fields.
```json
{"book": {…}, "set": {"genre": "Fantasy", "publisher": "Tor", "series": "…"}}
```
Allowed keys: `title, series, seq, author, genre, sub_genre, media, universe,
publisher, pub_date, need, notes, read_on, acquired_on`. `series`/`universe`
are names — resolved to rows, created if new; blank clears them.
`read_on`/`acquired_on` are ISO date corrections: they set the book's date
(+year, src) and, when the book has exactly one matching event row, sync that
row too — with several rows none are touched and the result note says so.
Status (`owned`/`read`) is NOT settable here — that flows through
`finished`/`acquired` so the event feeds stay truthful. A retitle that
collides with an existing book's title parks as a proposal instead of
applying (duplicate guard).

**`pub_date`** — publication date, kept at whatever precision the source
actually had: `YYYY`, `YYYY-MM`, or `YYYY-MM-DD`. Input is normalized, so
`July 1990`, `1990-07`, and `2010-May-14` all work; a year-only source stays
year-only rather than gaining an invented month. Anything unparseable is
rejected with an error rather than guessed at. Distinct from the enrichment
`publish_date` in `book_meta`, which is a third party's claim about the same
book.

**`need`** — an explicit boolean, and the one flag that IS settable here while
`owned`/`read` are not. It is a want, not a status: no event feed records
"started needing this", so `book_update` is its only editing path. It has to
stay distinguishable from "not owned" — a book tracked but not being hunted
for has `need: false`, and inferring one from the other would erase exactly
the difference the collecting checklists depend on. Accepts JSON booleans and
the sheets' own vocabulary (`x`, `yes`, `1`).

### `order_new` / `order_update` — omnibus contents
A Book Mail order may declare its contained works up front: `contains` on
`order_new` (list of titles, optional trailing ` #seq`), stored on the order
and editable later via `order_update` `set.contains` (newline-separated).
When such an order is marked arrived, the arrival records the copy (titled
by the order's Books field) and marks each declared work owned with a dated
acquisition — no itemize proposal needed; only titles that fail exact match
park as proposals. Multi-book orders WITHOUT declared contents keep the old
behavior: an itemize proposal, never a guess.

### `proposal_decide`
Add/Drop from the Proposed tab. Sets `approved` on the `fix_proposals` row;
execution happens via `apply_fixes.py`, which the inbox runner invokes right
after events apply, so an Add lands in the register within the same cycle.
```json
{"id": 3312, "kind": "add_book_from_inbox", "target": "Blacktongue / The Daughters' War", "decision": "approve"}
```
`decision` is `approve` | `reject`. `target` is echoed from the bundle and
must still match the row — a proposal that changed since the bundle was
published is skipped for re-review rather than decided blind. Already-decided
or archived proposals are no-ops.

An approve may carry an optional `set` — the Proposed tab's ✎ action — so
the reviewer can adjust values in the same tap that accepts them:
```json
{"id": 3415, "kind": "add_book_from_inbox",
 "target": "- / Song of Silver, Flame Like Night", "decision": "approve",
 "set": {"author": "Zhao, Amelie Wen", "series": "Song of the Last Kingdom",
         "seq": "1", "genre": "Fantasy, Young Adult"}}
```
Allowed keys are `book_update`'s: `title, series, seq, author, genre,
sub_genre, media, universe, publisher, notes, read_on, acquired_on,
pub_date, need, room`. (`room` is the shelf location — one of the eight
Rooms-tab names; rule-filled on 2026-09-13, and ✎ is how a book that
moved house gets its new room.) The
`target` guard is evaluated against the **unedited** proposal row first; the
consumer then merges `set` into the proposal's payload before marking it
`approved`, so `apply_fixes.py` runs unchanged and the add lands with the
adjusted values. `series`/`universe` are names — resolved to rows, created
if new; blank clears; the UI snaps vocab fields (`genre, sub_genre, media,
publisher`) to canonical casing before sending. Status (`owned`/`read`) is
not settable here, same as `book_update`. `set` on a `reject` is ignored.

`decision: "edit"` (the ✎'s **Save without adding**) merges `set` into the
payload and LEAVES THE PROPOSAL OPEN — same key rules, note-tagged
`[EDITED …]`; a later plain approve applies the saved values. An `edit`
with no `set` is a no-op.
Consumers predating this field silently apply the add unedited — update
jerry before relying on ✎; a plain approve (no `set`) is byte-compatible
with the old shape.

### `track_series`
Start tracking a whole series — the Books tab's 🔍 action.
```json
{"series": "Gods of the Ragnarok Era", "author": "Matt Larkin"}
```
PROPOSALS only, never register writes: jerry looks the series up on
Wikidata (an entity roster with ordinals = confident, ordered proposals)
and falls back to a Google Books author sweep (unordered CANDIDATES —
each proposal's note says so; Drop the noise, set seq via ✎ on approval).
Titles already in the register are skipped; everything proposes as
owned=false; the hydrator fills pub_date/genre in the same cycle. `author`
is optional but strongly recommended — it filters both lookups.

### `sync_request`
Ask jerry to pull an external source now. Results arrive as PROPOSALS on the
Proposed tab — a sync never writes the register directly (Goodreads burned it
once; the 2026-07-24 rule stands).
```json
{"source": "goodreads"}
```
`source` is `goodreads` | `libib` | `boxes` | `upcoming` | `all`. `boxes`
(2026-09-23) re-reads the Book Boxes tab of Shereen's sheet into
`book_boxes` (`sync_book_boxes.py`; also nightly at 02:25) — her system of
record for subscription boxes; the dashboard treats rows marked Ordered and
not yet Received as her open orders (TBR chips, Coming Up, Log › Orders).
Rows the Log view created or edited keep their app values. Goodreads needs the
My Books RSS URL (shelf=read, with `key=`) in `jobs/.goodreads_rss` on jerry
(Shereen's in `jobs/.goodreads_rss_goblin`; each file is optional and every
row and proposal is scoped to its owner); it also runs nightly at 02:20.
Libib has no API — its "sync" is the dashboard's Import Libib CSV button,
which diffs a fresh export client-side and queues new titles as `add_book`
events. `upcoming` (2026-09-23) runs `scan_upcoming.py` — Google Books
newest-first per author of every series with a book read, plus Wikidata
series rosters with ordinals and dates — and parks the finds as
`add_book_from_inbox` proposals (owned false, pub_date when known); it is
slow (minutes) so it never rides along with `all`. Optional `owner`
(`butthead` | `goblin` | `all`, default `all`). It also runs weekly
(Wednesday 04:30, `install_scan_cron.sh`).

### `goodreads_link`
A hand match for a currently-reading shelf row the sync could not place —
the TBR card's "It's in the register as…" action (2026-09-23, migration 026).
`gr_book_id` is the Goodreads book id from the `bookdb|Goodreads` tab's
`GrId` column; `book` is the usual exact reference (or `copy_id` for a
collection on the copies layer). jerry stores it in `goodreads_links`, updates
the shelf row at once, and consults the table before its own title/ISBN
ladder on every later sync, so the match survives re-syncs.
```json
{"gr_book_id": "230170279", "gr_title": "Bedside Companion to Folklore & Magic",
 "book": {"title": "Bedside Companion to Folklore and Magic", "series": "", "owner": "butthead"}}
{"gr_book_id": "230170279", "action": "unlink"}
```
`action` is `link` (default) or `unlink`. `gr_title` is display-only.

### `meta_add`
Add a value to a picklist vocabulary (the Metadata tab). Applies to
`meta_vocab` on jerry; the published bundle's `bookdb|Meta Vocab` tab is the
union of values in use and these additions, so a value exists in the
dropdowns before any row uses it.
```json
{"kind": "genre", "value": "Romantasy"}
```
`kind` is one of `genre, sub_genre, media, publisher, vendor, box`.
Duplicate adds are no-ops.

`pub_date` accepts `YYYY`, `YYYY-MM`, `YYYY-MM-DD`, month-name forms, and
`TBA` (2026-09-22: announced, no date — Doors of Stone, The Queens); the TBR
tab files TBA and far-off dates under Waiting for.

### Authors (phase 2, 2026-09-23)
`book_update` accepts `set.authors`, a list of people in credit order with
roles (`author`, `editor`, `artist`, `translator`, `illustrator`); jerry
rewrites the book's credits and regenerates the display string from them.
`set.author` (a string) still works: it is split the register's way into
credits. Send one or the other, not both.
```json
{"book": {"title": "Brona", "series": "First Druids of Shannara", "owner": "butthead"},
 "set": {"authors": [{"name": "Terry Brooks", "role": "author"},
                     {"name": "Delilah S. Dawson", "role": "author"}]}}
```

### `author_update` — people-level edits
```json
{"action": "merge", "from": "David Wong", "into": "Jason Pargin", "kind": "pseudonym"}
{"action": "rename", "name": "Justin Pargin", "new_name": "Jason Pargin"}
{"action": "alias", "name": "R.A. Salvatore", "alias": "RA Salvatore", "kind": "variant"}
```
`merge` folds one person into another: spellings become aliases of the
survivor, credits re-point (the printed name is kept as `credited_as`),
display strings regenerate. `kind` is `variant`, `inverted`, `initials`,
`typo`, or `pseudonym`. `rename` changes the canonical spelling and keeps
the old one as an alias. `alias` adds a spelling without merging anything.

### `plan_set` — a year's reading plan (migration 027)
Rough intentions for a year ("long hard sci-fi", "cozy fantasy novella"),
optionally pinned to a register book later; the Reading › Plan tab checks
them against the year's targets. `by` owns the slot.
```json
{"year": 2027, "action": "add",
 "set": {"label": "long, hard sci-fi", "media": "Print", "genre": "Scifi", "length": "long"}}
{"year": 2027, "action": "update", "id": 12,
 "set": {"book": {"title": "Blindsight", "series": "", "owner": "butthead"}, "done": true}}
{"year": 2027, "action": "remove", "id": 12}
```
`action` is `add` (position appends when absent), `update` or `remove`
(both need the bundle's `Id`). `set` keys: `label, media, genre, length
(short|medium|long|doorstop), book (exact ref, or blank to unpin), position,
done, notes`. A slot needs a label or a book.

### `queue_set` — the hand-picked TBR ("Next up")
One register book in, out, or moved within the reader's own queue
(`reading_queue`, migration 023). `position` is a float the dashboard
computes between the neighbours it can see; an `add` without one appends.
A `finished` event on the book removes it from the queue.
```json
{"book": {"title": "Persepolis Rising", "series": "Expanse", "owner": "butthead"},
 "action": "move", "position": 2.5}
```
`action` is `add`, `move`, or `remove`.

### `order_new`
Two shapes, discriminated by `list`.

`list: "bookmail"` — Com's model: one row per order, appended to Book Mail
Orders (`by` is `butthead`):
```json
{"list": "bookmail", "date": "2026-07-26", "order": "#TBBSUB123456",
 "books": "The Sun Eater 1-3", "series": "Sun Eater",
 "author": "Christopher Ruocchio", "count": 3, "type": "One-Time", "paid": "Paid",
 "store": "The Broken Binding"}
```
`store` (optional, migration 020) names the shop. Blank + a `#TBB…` order
number derives The Broken Binding; the Book Club flyer's order form logs one
`bookmail` order per checked title with `store: "Tombolo"` and no order
number. The store rides onto the acquisition when the order arrives and is
updatable via `order_update` `set.store`.

`list: "bookboxes"` — Shereen's model: one row per book in a subscription-box
lifecycle, matching her Book Boxes sheet (`by` is `goblin`). `title` may be
empty — she pre-logs boxes before titles are announced. `want` is
Yes/Unsure/No; there is no paid field because subscriptions auto-charge
(`date` is the charge date):
```json
{"list": "bookboxes", "date": "2026-08-01", "vendor": "FairyLoot",
 "box": "Adult", "title": "Alchemised", "author": "SenLinYu",
 "want": "Unsure", "eta": "2026-09-01", "order": "3660411", "notes": ""}
```

### `order_update`
Update an existing order, referenced by its bundle strings. Carries the same
`list` discriminator; currently the UI only emits updates for `bookmail`
(Book Boxes rows aren't in the bundle yet, so there is nothing to tap).
Expected `set` keys for `bookboxes` once wired: `received` (date or "Yes"),
`want` (Yes/Unsure/No), `ordered` ("CANCELLED"), `tracking`, `eta`.
```json
{"ref": {"date": "December 9, 2024", "order": "#TBBSUB247048", "books": "The Sun Eater 1-3",
         "series": "Sun Eater", "author": "Christopher Ruocchio"},
 "set": {"paid": "Paid", "delivered": "Fulfilled", "fulfil": "2026-07-26", "tracking": "1Z…"}}
```
Only keys present in `set` change. Values follow the sheet's vocabulary:
`paid` is `Paid`/`Unpaid`, `delivered` is `Fulfilled`/`Unfulfilled`, `fulfil`
is the arrival date. `delivered: "Fulfilled"` + a `fulfil` date is the UI's
"Arrived today" action.

## Consumer contract

1. List `inbox/*.json`, oldest first (filenames sort chronologically).
2. Validate the envelope; quarantine invalid files to `inbox/failed/` with a
   `.reason.txt` beside them — never delete silently.
3. Apply. Exact-match book/order refs; on no-match, convert to a proposal and
   still archive the event.
4. Move the file to `inbox/processed/YYYY-MM/` (same name).
5. If anything was applied, re-export the bundle and push to the dashboards
   repo as usual.

The live consumer implementing this contract runs on jerry inside the bookdb
codebase (the original `tools/apply_inbox.py` skeleton in this repo was
superseded by it and has been removed). This document is the contract of
record: dashboard-side event changes land here first.
