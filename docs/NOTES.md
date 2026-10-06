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

- **Tombolo orders #14567211 and #17708327 — dates.** Logged 2026-10-06 from
  Tombolo's order-history exports (one order per file), which carry no order
  date: both orders are undated in Buying › Book Orders, and the six
  acquisitions of the arrived one (#14567211, the August-2025 releases) are
  undated too. John: the dates are on Tombolo's "Previous Orders" page —
  Log › Orders ✎ on each order, or tell Claude and the acquisitions get
  dated as well.

- **Moss'd in Space (Rebecca Thorne) — in hand?** On Tombolo order
  #17708327 with three pre-orders, out since 2026-06-30, still *ordered*. If
  it arrived, mark the line Arrived in Log › Orders: the arrival creates the
  register row owned. The order is John's, so that row lands under Com even
  though the other Rebecca Thorne books are Shereen's — flip the owner if it
  is hers.

- **The 2026 Old Farmer's Almanac** rode on order #14567211 and is not a
  register book; its line is arrived and unlinked on purpose. Say so if it
  should be in the register after all.

- **Book Orders — duplicate Sun Eater order.** Two active unfulfilled orders
  both say "The Sun Eater 6-7": #TBBSUB751375 (July 1, 2026, order id 64) and
  #TBBSUB795281 (2026-08-03, order id 165). Different order numbers, so one
  was probably re-placed or double-logged. John: tap the order in
  Log → Orders and use **Cancel order** (added 2026-09-09) on whichever
  isn't the live one — it drops from the open list and shows Cancelled in
  history.
