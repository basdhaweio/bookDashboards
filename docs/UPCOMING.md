
## Household rule (2026-09-27)

A book one reader already **owns** is not proposed for the other — one copy
in the house is enough. Both "you might want this" jobs apply it
(`scan_upcoming.py`, `propose_series_completion.py`) through
`scan_upcoming.household_has(cur, owner, title, series)`; read-shelf
proposals (a reader's own Goodreads history) are not affected. Title
matching is series-aware: Wikidata's "Mistborn: The Well of Ascension" is the
register's "The Well of Ascension". A target that is already open, or was
decided before (added or dropped, see `fix_proposals_history`), is never
raised again — `proposal_blocked(cur, kind, target)`.

## Home: New to get (2026-11-05)

John asked: "can you add something to home of newly announced books or books
added that I need?" He chose every book not owned. Only 2 books carried the
Need flag, because Track and Add never set it. The card sits on Home between
Arriving this week and New from Book Notification, built by
`homeNewToGet(owners, td)` in index.html.

**What it lists.** Register books no one in the view owns that are either:
- **new to the register:** `AddedOn` (bundle column 24, from
  `books.added_on`, migration 038) falls in the last 30 days
  (`HOME_NEW_DAYS`);
- **newly announced:** out in that window, dated ahead, or TBA.

**What it leaves out:**
- anything on an open Book Mail line, since Arriving and Orders show it (a
  book shows in one place only);
- anything the other reader owns (the household rule above).

**How it lays out.**
- Two or more books from one series share a row, such as "Dungeons &
  Dragons Novels · 9 books · added Oct 24".
- Rows run newest first (an add or a release), then the soonest release
  ahead. Ten are shown, and the rest are counted.
- A single book gets **Got it**: a date and an optional shop. It sends the
  same `acquired` event as the By Universe drill-down.

**Dates.** `books.added_on` defaults to New York's date for every new row.
For books added through the Proposed tab since 2026-08-02, migration 038
backfilled it from the proposal's `applied_at`. Older books are blank and
never count as new.

**Test.** The harness check "Home: New to get" runs the card on stand-in
books. It covers the folding, the exclusions, the order and the Got it form,
and sends nothing.
