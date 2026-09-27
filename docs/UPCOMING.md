
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
