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
{"book": {…}, "date": "2026-07-26"}
```
May arrive for an already-owned book (new copy/format) — apply your own rule;
the UI warns before sending.

### `add_book`
A book not in the catalog. Create as a **proposal** (existing bookdb review
flow), not a direct catalog insert.
```json
{"title": "…", "series": "", "seq": "", "author": "…", "media": "Print",
 "genre": "Fantasy", "universe": "", "owner": "goblin",
 "owned": true, "read": false}
```
Empty `series` means standalone.

### `order_new`
Append to Book Mail Orders.
```json
{"date": "2026-07-26", "order": "#TBBSUB123456", "books": "The Sun Eater 1-3",
 "series": "Sun Eater", "author": "Christopher Ruocchio", "count": 3,
 "type": "One-Time", "paid": "Paid"}
```

### `order_update`
Update an existing order, referenced by its bundle strings.
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

Steps 1–4 are implemented in `tools/apply_inbox.py`; the bookdb-specific
apply functions are seams to fill in.
