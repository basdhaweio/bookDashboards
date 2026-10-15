# Awards (Collecting › Awards)

John used to keep the Hugo Best Novel nominees and winners as a list of
"what to track / read" (2026-10-06: "would like to get that back"), later
adding Best Series and Best Novella. The recurring chore was: a nominee is
one book out of a series — how many books are there, and which do we have?

## Source

Wikidata, read by `jobs/fetch_awards.py` on jerry (weekly, Sunday 02:50;
`awards.log`). For each category item — Best Novel **Q255032** (1953–),
Best Novella **Q549884** (1968–), Best Series **Q29479284** (2017–) — every
work with `P166` award received (winner) or `P1411` nominated for (finalist),
dated by the `P585` qualifier (the ceremony year). Humans are excluded (the
award items also hang off authors). Coverage checked 2026-10-06: every year
since 1959 carries its full slate (one winner + five finalists); 2015's
Novella has no winner because "No Award" won. Also read: author (`P50`),
first publication year (`P577`), the work's series (`P179`, position
`P1545`), and how many works Wikidata files under that series.

Works with no English/mul label are skipped rather than guessed. Nothing
here writes to `books`.

## Register match

Recomputed on every fetch into `awards.book_id` / `reg_series` / `reg_owner`:
the exact normalised title, with the author's surname agreeing when both
sides name one (Com's row first, then Shereen's). The bundle tab
`bookdb|Awards` carries the roster with the matched book's owner, read and
owned flags.

Series are matched **through their books** (2026-10-15, John: "go with
matching on the books"), because the names drift: Wikidata's "The
Stormlight Archive" is the register's "Stormlight Archives", "Children of
Time" is "Children Series", "The Final Architecture" is "Final Architects".
For each series, Wikidata's works (P179) are matched to register books like
any nominee, and the register series holding most of them wins (Com's on a
tie). Fallbacks, in order: the series name against `series` with articles
and a trailing "series/saga/trilogy/sequence/cycle" dropped; then, for a
series Wikidata files no works under, the closest name (similarity ≥ 0.75)
among the series the author's own register books sit in. Matching by books
also fixed a false hit: Jemisin's Inheritance trilogy had matched Paolini's
"Inheritance" by name alone.

Pen names and duos are dropped from the rosters like people (James S. A.
Corey and Mark Phillips sat on them as nominees, duplicating their works),
and a fetch removes rows Wikidata no longer returns, unless the fetch looks
truncated (under 80% of the stored rows).

## The tab

Chips: Best Novel / Best Novella / Best Series, 🏆 Winners only, Not in the
register, plus a title/author/series filter. Rows by year, winner first:
title, author, publication year; the series line ("Teixcalaan #2 · 3 on
Wikidata · register: 1 read, 2 owned of 2 (Com)"); a status badge (✓ read ·
on the shelf · tracked, with whose row); and two actions that never write
directly:

- **🔍 Track series** — `track_series {series, author}`; jerry's scan parks
  every book it finds on the Proposed tab (docs/INBOX.md). Shown while the
  register holds fewer books of the series than Wikidata knows; "queued"
  while a request is in flight.
- **＋ Track book** — a standalone (or a book whose series isn't ours yet)
  opens the Books add form prefilled from Wikidata as tracked, not owned.

Metrics: the roster size and span, how many are in the register, how many
read (winners read of winners), and the candidates not yet tracked.
