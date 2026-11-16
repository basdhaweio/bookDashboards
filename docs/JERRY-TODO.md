# Jerry-side to-do (from the cloud dashboard session, 2026-11-16)

Three gaps that need bookdb, its jobs, or `publish` — none can be fixed from
the dashboard. Evidence is from the bundle generated 2026-11-16T02:17Z and
the bookInbox repo. Delete each item when done.

## 1. Goodreads sync has pulled nothing since 2026-10-19

- Two `sync_request {source: goodreads}` events on 2026-11-15 (22:11Z and
  23:13Z) were moved to `processed/`, but the bundle published afterwards
  still lists *Fall of Light* on Com's currently-reading shelf. John had
  marked it read on Goodreads before syncing, then logged it by hand.
- `bookdb|Goodreads` has no `Added` date newer than 2026-10-19 (Com) /
  2026-09-23 (Shereen).
- Three open `track_series_miss` proposals say "Google Books did not answer
  for 3 days", so outbound lookups may have wider trouble.

To do: run the Goodreads sync by hand and read its output; check the RSS URL
and key in `jobs/.goodreads_rss` and the 02:20 nightly job.

Then make the failure visible: record every sync run (job name, finished
time, items found, error) in `bookdb|Scan Runs`, which today only carries
`scan_upcoming`. "Processed" in the inbox only means the request was
accepted. Once those rows publish, the dashboard session will show "last
Goodreads sync" beside the button.

## 2. Books on order have no register row

John's rule: the register is complete for every book tracked or owned. A book
on order is tracked, but an order line only gets a register row on arrival —
so looking up Protectorate shows *Velocity Weapon* alone, with books 2 and 3
invisible.

Open order lines (`ordered`) with a title and no `BookId`:

| Title | Series | Order |
|---|---|---|
| Chaos Vector | Protectorate | #TBBSUB831681 |
| Catalyst Gate | Protectorate | (no order number yet) |
| Theft of Swords | Riyria Revelations | |
| Rise of Empire | Riyria Revelations | |
| Heir of Novron | Riyria Revelations | |
| Shattered Sea | Shattered Sea | |
| The Dispossessed | | |
| The Blade That Remains | | |
| Moss Effect | | |
| I Need My Space | | |
| Those Who Prepare the Way | | |

Most carry the note "from the order's Books text, migration 029". Thirteen
other open lines (Sun Eater, Malazan) are linked, so this is inconsistent,
not designed. The remaining unlinked open lines are "Subscription credit"
placeholders and two blank-title Echoes Saga lines — leave those.

To do: create a register row (tracked, not owned) for each and link it via
`BookId`, with series and seq where known; and create the row at order time
from now on, so arrival flips it to owned instead of creating it.

## 3. A series row doesn't carry its books' universe

`Butthead Series` rows with an empty Universe although every book in the
series has one:

- Ronin of Okane (butthead) — set on all five books 2026-11-16 via `book_update`
- Landover shorts, Tales I Trilogy, Tales II Trilogy (butthead)
- Green Bone Saga, Wax and Wayne (goblin)

The Universe tab's tiles are built from these series rows, so those series
are missing from their universe's count. To do: in publish, roll a series'
universe up from its books when they all agree (or set it on the series).

## For awareness

- Dashboard changes merged 2026-11-16: register search now matches universe,
  and the Universe tab counts universe-tagged standalones (they arrive as
  type `Individual` and were dropped). A universe with no series-level tile
  gets one sized from its books.
- Queued through the inbox by the cloud session at John's request
  (2026-11-16T01:46Z, all applied): `meta_add` publisher "Cyber Sheep
  Studios"; `book_update` universe "Ronin of Okane" on Vol 1–5; `add_book`
  *Me & My Nekomata!* (approved by John, book 15465).
- `tests/harness.html` has one failing check that predates these changes:
  "TBR: a queued series is not under New book or Waiting for".
