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

### How a contents line may be written (2026-11-16)

The series number can go at the end or in front: `Avempartha #2`,
`1 - The Crown Conspiracy`, `2. Avempartha`, `3) Nyphron Rising`, `#4 The
Emerald Storm`. `_parse_contains` (apply_inbox) reads all of them. An order's
contents are stored as `Title #seq`, one work per line (`contents_text`),
whether they came from the order form or Edit details. The dashboard's
`contentTitle()` reads both shapes too.

A number in front counts only when it has at most three digits and a dash,
`.`, or `)` followed by a space. "2001: A Space Odyssey", "1984" and
"1-2-3 Magic" stay whole titles; no register title starts that way.

Why it matters: John typed the Riyria orders as `1 - The Crown Conspiracy`.
Read whole, that line matches no register book, so Theft of Swords' arrival
would have proposed a new book called "1 - The Crown Conspiracy" instead of
owning #1. `jobs/fix_1116_collection_contents.py` rewrote the three orders.
Its dry run rehearsed all three arrivals: each copy holds its two register
works, owned, with no proposals and no register row named after the volume.

**A collection is never a register row** (John, 2026-11-16: "Theft of
Swords is a collection of other books - so that shouldn't be its own line in
the register the composite books should be"). The TBB check can name a
month's line before anyone knows the book is an omnibus. When contents are
then declared, any add proposal still waiting for that volume's own title is
withdrawn (`withdraw_volume_adds`). A line already linked to a book is left
as it is, and the event's note names that book.

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
- **From a scan** (2026-11-04): when an ISBN scan finds a book you already
  have, the bottom of the result offers **📚 New edition of “…”** — it opens
  that row's Got it with *Edition?* ticked (publisher prefilled when Google
  Books named one) and sends the scanned ISBN with the edition, stored on
  `copies.isbn` (migration 036). A later scan of the same ISBN answers "in
  the register — your <format · edition> copy".
- **From an order** (migration 035, 2026-11-04): an order line's **Edition**
  button names the register book and the edition; the line arrives as that
  book's copy — at once if it already arrived — and a further physical copy
  takes the next `copy_no`. A title that names an edition of exactly one
  register book ("… (Deluxe Hardcover Edition)") is caught on arrival too.
  The Licanius deluxe hardcovers (Tombolo, 2026-10-25) were the first.
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
