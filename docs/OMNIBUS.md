# Omnibuses, collections, and copies (Option C)

Decided 2026-09-10 (John). A register row is a **work** — the thing you read,
with a series position. A **copy** is a physical object on the shelf. They
are usually 1:1 and no copy row exists; an omnibus/collection is one object
containing many works, and THAT is what the copies layer records.

Examples that drove it: TBB's Riyria Revelations omnibuses (Theft of Swords
= The Crown Conspiracy + Avempartha), Glen Cook's The Books of the South,
Zelazny's Great Book of Amber (all 10), and short-story anthologies whose
stories are tracked as their own rows.

## Ground rules

- The register stays pure works: reading, series, sequence, Browse — all
  work-level, untouched. An omnibus edition title is NOT a register row.
- `books.owned` stays John-asserted authority. Acquiring an omnibus marks
  every contained work owned through the normal acquired path (one
  acquisition per work, need cleared) — the copy row is provenance, never
  a status source.
- Counts stay work-denominated everywhere (John's call: Buy This Year keeps
  showing the contained works as acquisition lines; the copy records the
  "one object" truth).
- SE-shelf duplicates later = a second copy row for the same work; no
  register impact.

## Tables (jerry, migration 015)

`copies` (id, title = edition title, owner, publisher, format, source_store,
acquired_on, notes, active, origin) and `copy_contents` (copy_id, book_id,
position). `orders.omnibus_contents` (one title per line) lets an order
declare its contents at entry time.

## Entry points (inbox contract — see INBOX.md for payload details)

1. **Add form** — "Omnibus / collection" toggle: the Title field becomes the
   edition title, a textarea lists contained works one per line (optional
   trailing `#seq`). `add_book` payload gains `omnibus: true` +
   `contains: [{title, seq}]`. On approval, apply_fixes creates (or links,
   when a same-owner title already exists) each contained WORK with the
   shared series/author/genre/etc., creates the copy + contents, and applies
   owned/acquisition per work. The edition title never enters the register.
2. **Got it** on a tracked work — "part of an omnibus" toggle: name the
   edition + list the OTHER contained works. Matched register works are all
   marked owned with acquisitions; unmatched titles park as
   add_book_from_inbox proposals carrying the copy id, so approving them
   completes the copy's contents.
3. **Order form** — same toggle + contents list, stored on the order. When
   the order arrives, the contents drive per-work owned/acquisitions and the
   copy row directly; only titles that fail exact match park as proposals.
   (Multi-book orders WITHOUT declared contents keep the old behavior: an
   itemize proposal, never a guess.)

## Editions (migration 018, 2026-09-16)

The same copies layer holds **single-work objects**: a hardback of a book
already owned in paperback, a special edition, a signed copy. A copies row
with one `copy_contents` link is an *edition* of that work; a row with
several is an omnibus/collection. New columns: `edition` (the edition's
name — Illumicrate, Broken Binding exclusive), `let_go_on`, `replaced_by`.

- **Recording one**: 📦 Got it › *Edition?* (format, edition name, the
  edition's publisher, notes, and *Swap?*), or ＋ Add a book › *Edition?*
  for a new work that arrives as a specific edition. The `acquired` /
  `add_book` payloads carry an `edition` object (INBOX.md).
- **Swapping**: *Swap?* (`replaces: true`) retires the book's newest
  still-active single-work copy — `active=false`, `let_go_on` = the
  acquisition date, `replaced_by` = the new copy. When the outgoing copy was
  never itemized (most books predate the copies layer) the new copy's note
  says so and nothing else changes.
- **Letting go**: ✎ › *Copies on the shelf* › **Let go** sends
  `copy_let_go` — the object leaves the list; the work's owned status is
  untouched (it is John-asserted and only `acquired`/`finished` move it).
- **Counts stay work-denominated.** A book with a paperback and a hardback
  is one book. The copies layer is where "how many special editions do I
  own?" gets answered.
- **Surfacing**: Log rows chip single-work editions in gold ("Hardcover ·
  Illumicrate") next to the purple "in: <omnibus>" chips; the Copies bundle
  tab carries `Edition` as its last column.

## Dashboard surfacing (phase 1)

Bundle gains synthetic tab `bookdb|Copies` (one row per copy, Contains =
`|`-joined work titles). Browse/search rows show an "in: <edition title>"
chip when a work lives inside a recorded copy. Nothing about toplines,
tabs, or the publish gate's count invariants changes.
