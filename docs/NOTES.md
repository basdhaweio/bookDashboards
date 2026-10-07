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

- **Vinland Saga numbering (settled 2026-10-07).** The register's Vinland
  Saga Vol 1-28 are the tankobon numbers, by design — never renumber them
  (John). Kodansha's two-in-one hardcovers collect two tankobon each (HC 7 =
  Vol 13+14 … HC 14 = 27+28; HC 15, due 2026-10-13, is the last and holds
  Vol 29 — not owned). The history import had linked Tombolo's hardcover
  numbers 7/8/9/12/13 to tankobon rows of the same number; undone, and each
  hardcover is now a copy holding its two tankobon (copies 293-297), with
  acquisitions on those rows dated by the order-date rule and the $22.99
  list price on each hardcover's first work.

- **Tombolo history, settled 2026-10-07:** Moss'd in Space (#15281, dated by
  the order-date rule), Ender's Tribe (#15282, universe Ender, series not
  set) and Legacies of Betrayal (#15283, Witness Trilogy #3) are in hand and
  linked to their lines. The "new Anthony Ryan book from TBB" is *Upon the
  Forge of Battle* (#15130), already arrived 2026-09-30 through TBB order
  #TBBSUB730074 — nothing to do unless John meant another book.

- **Acquisition dating rule (John, 2026-10-06).** An acquisition means the
  book is in hand; an open pre-order gets an order record only. When the day
  a book came home is not known, its ORDER date stands in — the 84
  acquisitions the Tombolo import created are dated that way, and the books
  whose date they filled say `acquired_date_src = order-date`. Release dates
  are never substituted.

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
