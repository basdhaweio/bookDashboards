# Register (Tracking › Register)

John, 2026-10-07: "I bought a book today that's book 3 in a series but it
wasn't tracked and I don't have an easy way to find that out." Log › Books
searches the world (Open Library, Google Books) to add a book; the universe
pages show a universe's series in full; nothing showed an ordinary series
on demand. Register does.

## What it searches

The register only — the books of the current reader filter (Both / Com /
Shereen), by title (title aliases count), author and series name, from
three letters on. Results, in this order:

- **Series** named by the query, or home to a matching title: one block per
  series (per reader when the header says Both), every volume in sequence
  order with the read · owned · need · unpublished dots and the same
  one-tap actions as the universe pages — ✓ Read, 📦 Got it, 🚩 Need,
  📅 TBA — which send the usual inbox events and show as pending until
  jerry syncs. Series that belong to no universe are the point.
- **Standalones** whose title or author matches.
- **Authors** whose name matches (people, split from co-author strings),
  each with buttons for their series.

Nothing matched: a button hands the words to Log › Books, which asks the
outside sources and offers the add form. Adding stays an individual act
(John, 2026-10-07: "add this book individually to start").

## Finding the rest of a series

Every series block carries **🔍 Look for missing volumes**: the existing
`track_series` event (docs/INBOX.md). jerry's sweep (Wikidata, then Google
Books) parks every volume it finds on the Proposed tab — proposals, never
direct adds. "Queued" shows while a request is in flight.

The nightly `propose_series_completion.py` and the Wednesday
`scan_upcoming.py` do the same unasked for series in progress; they depend
on Google Books and Wikidata answering, and on the register's series name
matching Wikidata's (the Talisman books are filed as "Jack Sawyer").

## Jumps

- A register hit in Log › Books shows its series name as a link into
  Register.
- The series name on a TBR card opens Register (it used to open the old
  browse mode in Log › Books, which is gone — Register absorbed it).
- `#register/<words>` deep-links a search (the widget and bookmarks).

## Shared code

`uvCtx` / `uvRowHtml` render the rows for both the universe drill-down and
Register; `uvAct` / `uvSet` / `uvRerender` act on whichever rendered last
(`G._uvHost`). A fix to a row lands in both places.
