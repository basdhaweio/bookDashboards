# Jerry-side to-do

Work that needs bookdb, its jobs, or `publish`, which a cloud session can't
reach. Delete each item when it's done.

Last updated 2026-11-15 by the local session. Items 2 and 3 from the cloud
session's list (2026-11-16 UTC) are done; see "Done" at the end.

## 1. Goodreads: the shelf feeds are gone, so a replacement is needed

**Finding.** It isn't a key problem. Since about late October, every feed the
nightly sync and the Sync button read answers HTTP 404:
- John's and Shereen's read, currently-reading and to-read shelves (19 runs
  in `sync.log` before 2026-11-15);
- the same `review/list_rss` address with no key, for any profile. In July
  that address answered 401 ("needs the key"); now it answers 404;
- the `user/updates_rss` status feed.

The key files are unchanged since September. Shelf pages and the library
export need a signed-in browser. The old developer API (`review/list.xml`)
answers 401 and has issued no keys since 2020.

**Done for it (2026-11-15).**
- `sync_goodreads.py` records every run in `import_runs` (job
  `sync_goodreads`: entries read, proposals, per-feed errors, `failed`).
- publish adds those runs to `bookdb|Scan Runs` with the same columns as
  `scan_upcoming`. Proposed = mark-read + add proposals; Searches = feed
  entries; Failed = feeds that errored; Blind = no feed answered; Note = the
  errors.
- The Sync button's inbox reply says "FAILED - …" instead of "0 recent".
- Dashboard: `grHealth()`. A failing run shows as a Home chip ("Goodreads
  sync failing") and a line beside both Sync Goodreads buttons (TBR tab,
  Proposed tab). `scanHealth()` reads only `scan_upcoming` rows now.

**Open: John to choose a replacement.**
- *Goodreads library export:* John downloads the CSV (My Books › Import and
  export › Export library). An "Import Goodreads CSV" button like the Libib
  one sends it to jerry, and `import_goodreads.py` already reads that format.
  It is manual but complete: read dates, currently-reading, to-read.
- *Hardcover (hardcover.app):* an official GraphQL API with a personal token.
  It imports a Goodreads library, and jerry could sync it nightly. John would
  track reads there instead of on Goodreads.
- *Neither:* log reads on the dashboard only. Then retire the nightly job and
  the Sync buttons, and clear the frozen currently-reading mirror. Today Fall
  of Light still shows as reading from the last good sync.

## Done 2026-11-15 (local session)

**Order lines now get register rows.**
- `apply_inbox.ensure_line_book` covers new orders and a line named later.
  The line links the register book it names. That match now ignores a
  subtitle the line leaves off, and an edition title links its base book.
  Otherwise the book is added tracked and not owned.
- Adds go through an add proposal carrying `order_line_id`; `apply_fixes`
  links the line once the book exists.
- John's own entries are approved at once. Lines the TBB check creates wait in
  Proposed, since TBB product names aren't always titles.
- Left alone: placeholders, series-only lines, omnibus orders (their contents
  are owned on arrival), and edition titles whose base book the register
  lacks.

**The listed lines** (`jobs/fix_1115_order_rows.py`):
- Protectorate: Velocity Weapon #1; Chaos Vector #2 and Catalyst Gate #3
  added.
- The Dispossessed links to the register's "The Dispossessed: An Ambiguous
  Utopia" (634) as a Special Edition.
- Added as standalones: The Blade That Remains, Moss Effect, I Need My Space.
- Those Who Prepare the Way is The Bird That Drinks Tears #2. The Heart of the
  Nhaga, already owned, is #1.
- Shattered Sea – Foiled Signature: TBB's page says "Shattered Sea Trilogy
  Books 1, 2 & 3", Royal Hardbacks. The line became three, Half a King / Half
  the World / Half a War, series Shattered Sea Trilogy 1–3, like the First Law
  Trilogy.
- Theft of Swords, Rise of Empire and Heir of Novron were already fine. Their
  orders list their contents (Riyria Revelations 1–6, all in the register).

**Series universe.** publish fills a series' universe from its books when all
of them agree. That covers Ronin of Okane, Landover shorts, Tales I and II
Trilogy, Green Bone Saga and Wax and Wayne.

**Harness.** "TBR: a queued series is not under New book or Waiting for" now
passes. A Book Notification announcement in a queued series (Spawn Origins
Volume 33) is held back like the series' register books are. 81/81.
