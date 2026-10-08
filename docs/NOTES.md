# Notes / open questions

Running list of things to verify against the real world. bookdb on jerry is
the source of truth (the Google Sheets are stale artifacts — historical
reference only, never a correcting mechanism); fixes land in bookdb via the
dashboard's Books tab or NocoDB and reach `data/bookdb.json` on the next
publish.

## To check

- **Hunger Games — second prequel ownership.** Verify whether *Sunrise on the
  Reaping* (Suzanne Collins, Hunger Games Prequels #2) is actually owned. The
  register marks it Owned, Media Print, owner butthead, not read — and the
  Hunger Games Prequels count of 2 owned depends on that being right. If it
  isn't on the shelf, clear Owned via the Books tab ✎.
  *(2026-09-02: John will check once the books are out of moving transit.)*

- **Vinland Saga (settled 2026-10-07).** The register's Vinland Saga Vol
  1-29 are the tankobon numbers, by design — never renumber them (John).
  Kodansha's two-in-one hardcovers are omnibus copies "Vinland Saga N"
  holding tankobon 2N-1 and 2N, filed like the Gantz omnibuses: hardcovers
  1-14 are recorded (the five from Tombolo order #14587773 dated 2025-08-13,
  the other nine undated); hardcover 15, due 2026-10-13, holds Vol 29, which
  is tracked but not owned so it shows on Coming Up. The series row carries
  the convention in its notes. (The history import had briefly linked the
  Tombolo hardcover numbers to tankobon rows of the same number; undone.)

- **Tombolo history, settled 2026-10-07:** Moss'd in Space (#15281, dated by
  the order-date rule), Ender's Tribe (#15282, universe Ender, series not
  set) and Legacies of Betrayal (#15283, Witness Trilogy #3) are in hand and
  linked to their lines. The "new Anthony Ryan book from TBB" is *Upon the
  Forge of Battle* (#15130), already arrived 2026-09-30 through TBB order
  #TBBSUB730074 — nothing to do unless John meant another book.

- **Acquisition dating rule (John, 2026-10-06/07).** An acquisition means the
  book is in hand; an open pre-order gets an order record only. When the day
  a book came home is not known, its ORDER date stands in — but only for a
  book that was in print when ordered. A pre-order's order date is months
  before the book existed, so pre-ordered books keep an UNDATED acquisition
  (owned, no date) unless John names the pickup day. The Tombolo import's
  acquisitions follow this (61 dated 2025, 10 dated 2026, 21 undated
  pre-orders); books whose date it filled say `acquired_date_src =
  order-date`. Release dates are never substituted. Legacies of Betrayal
  (Witness Trilogy #3, Malazan) is back to an open order — not in hand yet.

- **Spawn Origins numbering (settled 2026-10-06).** The register's Spawn
  Origins Vol N follow Wikipedia's *Spawn (character) › Collected editions*
  hardcover list — Spawn: Origins Collection, Book N (Book 1 = #1-12 …
  Book 16 = #189-200, Book 17 = #201-212), not the paperbacks and not the
  Deluxe Editions, which cut the run 25 issues at a time. The Tombolo Deluxe
  Volume 8 (SPAWN #176-200) is therefore a copy holding Vol 15 and Vol 16
  whole plus #176 from Vol 14; Vol 15/16 carry that in their notes. The
  other deluxe editions from the history are resolved the same way: the Pink
  Ranger Deluxe holds MMPR: Pink + The Return; the Recharged Deluxe holds
  Recharged Vol 1-3 (MMPR #101-110); the Image Die hardcover holds Die Vol
  1-4 (DIE #1-20). Darkest Hour Deluxe (#15272, John's read item) carries
  MMPR #111-122, the issues of Recharged Vol 4-6 — left as John set it.

- **Register spellings fixed from Tombolo's catalogue (2026-10-06):**
  "Pretenders to the Trhone of God" → Throne, "Batman 89 Echos" → Echoes,
  "In the Shadows of their Dying" → Shadow; old spellings kept as title
  aliases. Also seen: Shereen's "Lore Olympus: Volume Eleven" row (#15126)
  breaks the "Lore Olympus Vol N" naming of the rest of the series.

- **Book Orders — duplicate Sun Eater order.** Two active unfulfilled orders
  both say "The Sun Eater 6-7": #TBBSUB751375 (July 1, 2026, order id 64) and
  #TBBSUB795281 (2026-08-03, order id 165). Different order numbers, so one
  was probably re-placed or double-logged. John: tap the order in
  Log → Orders and use **Cancel order** (added 2026-09-09) on whichever
  isn't the live one — it drops from the open list and shows Cancelled in
  history.

- **TBR: In progress layout, Reading now vs Next up, Sync Goodreads
  (2026-10-08).** In progress is the same grid as the other TBR sections
  again; a universe box spans one column per series (capped at the row —
  `--span` is set by renderTBR from the grid's width, and a resize that
  changes the column count re-renders). The flex flow of 09-24/27 left an
  empty slot beside every box and ragged rows ("still kinda looks off"). A
  queued book that is on the Goodreads currently-reading shelf, or already
  read, shows under Reading now only; its queue row stays until the finish
  lands — apply_finished and an approved sync_finished proposal both drop
  it now (Glinda of Oz sat under both). The TBR heading carries the same
  ⟳ Sync Goodreads as Log › Books (a sync_request event; jerry runs both
  feeds within a minute or two, then publishes — reload after). Also:
  index.html carried a raw NUL byte (a heredoc had turned a `'\0'` literal
  into the byte — the same JS string) that made grep call the file binary;
  it is the escape again.

- **Owned by format, the Digital room, physical copies (2026-10-08).** The
  bundle's Books rows carry `Formats` (the book's media plus every other
  format among its acquisitions — an Audible copy of a print book reads
  "Print+Audio"), `Copies` (1 plus the second physical copies on record:
  acquisitions.copy_no > 1, migration 034) and `Id`; the Series rows carry
  `Audio` (owned books in the series with an audio copy); Valuation carries
  `ExtraCopies`. The Overview shows Owned by format in exclusive buckets
  (print only · print + digital · audio only · ebook only — a book counts
  once) and the By Type table an "Also audio" column. Rooms has a Digital
  room for audiobooks and ebooks (media Audio/eBook → Digital, in
  room_rules.py and assignRoom; no spines). Library Growth's live owned
  figure counts physical copies: owned books plus second copies; a digital
  copy of a print book adds nothing, and the history rows keep the numbers
  they were kept with. Copies come only from rows that say so — 723 of
  John's books carry two snapshot acquisition rows for one copy, so the
  acquisitions table is never counted raw.
