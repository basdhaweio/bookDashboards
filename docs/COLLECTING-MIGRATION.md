# Collecting catalogs → one register

**Decision (John, 2026-08-20):** there is one book register, complete and
total for all books tracked and owned. The Forgotten Realms / Dragonlance /
Shadowrun collecting lists are not separate catalogs — they are register rows
whose attributes (universe, publication date, publisher, need) define where
they show up. The dashboard's Collecting section becomes a *view* over the
register filtered by universe, and the topline Tracking/Reading numbers
*exclude* the collecting universes so they don't skew reading stats.

This file is the working brief for the jerry-side session (Phase 1 and 3)
and the coordination contract with the dashboard session (Phase 2).

## Facts established from the published bundle (2026-08-20)

- The register tab (`…|Butthead Individual`, 2,382 rows) contains **none** of
  the collectible books: zero rows tagged with FR/DL/SR universes, and only 3
  title-string collisions with the 549 catalog titles ("Death Masks", "City
  of the Dead", "Extinction") — different books by different authors, keep
  them separate on import.
- The collecting data publishes as four tabs sourced from the old collecting
  spreadsheet (`1_vivOgWjSRnPO5doL_GIb33PD8sPO0Ya1MM_rf293lg`):
  - `Year Catalog` — Forgotten Realms, 307 books under interleaved 4-digit
    year header rows: `Title, Author, Date Published, Format, Read, Owned,
    Need, Published`
  - `DL` — Dragonlance, 156 books: `Title, Author, Publication date, Series,
    Read, Owned, Need, Published`, plus an embedded series-metrics section
    (rows with blank Title/Author and a series name in col D)
  - `SR` — Shadowrun, 86 books: `idx, Title, Author, Publication Date, Owned,
    Read, Format, Publisher` (publishers: FASA Books, WizKid Books, Catalyst)
  - `Series` — FR title→series mapping: `Title, Series, Dupe`
- Flags are complete: every DL row has at least one of Read/Owned/Need
  (51 owned). Assume the same discipline for FR/SR; verify during import.
- Row counts have drifted since July (Year Catalog 346→345, DL 157→156,
  SR 87→86), so this data is actively edited somewhere. **First task: find
  the current source of truth** — bookdb tables, or publish.py fetching the
  Google Sheet live. If it's still the sheet, the import reads the sheet
  once and the sheet is then retired (same as the deprecated dashboards).

## Phase 1 — jerry: schema, import, export (do now)

1. **Register fields.** Add to the book register (and to meta as
   appropriate):
   - `pub_date` — publication date; year-only for most DL/FR rows, fuller
     dates where the sheet has them (SR has e.g. "2010-May-14"). Store what
     exists; don't invent precision.
   - `publisher` — needed for the Shadowrun publisher view. Publisher vocab
     already exists in meta_vocab.
   - `need` — explicit boolean, carried from the sheets' Need column. Do NOT
     infer it from "not owned": the register's need-to-buy concept and the
     checklists' Need flag must stay distinguishable from
     tracked-but-not-sought rows.
2. **Universe flagging.** Universe values `Forgotten Realms`, `Dragonlance`,
   `Shadowrun` as normal universe attributes, but mark them as *collecting*
   universes somewhere the bundle exports (a flag on the universe vocab, not
   hardcoded in the dashboard) — the dashboard will use that flag to route
   these rows to the Collecting section and exclude them from topline stats.
   Future collecting universes then need no dashboard change.
3. **Import mapping.**
   - FR: `Year Catalog` rows → register rows, universe=Forgotten Realms;
     year header rows give `pub_date` year when the row's own date is blank;
     Format→media; series from the `Series` tab mapping (rows absent from
     the mapping are standalone); respect its Dupe column (flagged rows are
     intentional duplicates in the sheet — collapse or keep, but don't
     double-import silently).
   - DL: universe=Dragonlance; Series column carries straight over; skip the
     embedded series-metrics rows (they're derivable).
   - SR: universe=Shadowrun; Format→media; Publisher→publisher.
   - All rows owner=butthead, Read/Owned/Need flags carried as-is.
4. **Verification.** Post-import counts by universe must be 307/156/86 (or
   the sheet's current values); owned/read totals per universe must match
   the sheets; the 3 title collisions must exist twice (once per author).
5. **Export.** Extend the published register tab with the new columns
   (`pub_date`, `publisher`, `need`) and export the collecting flag on
   universes. **Keep the four old catalog tabs publishing during the
   transition** so the live dashboard keeps working until Phase 2 lands.
6. **INBOX.md.** Add `pub_date`, `publisher`, `need` to `add_book` payload
   and `book_update` allowed keys, so entry and editing cover the new
   fields.

## Phase 2 — dashboard session (after Phase 1 exports)

Blocked on Phase 1. The jerry session should report back: the exact final
column layout of the extended register tab, and how the collecting flag is
exported. Then the dashboard session will:

- Re-point the Collecting section (FR/DL/SR views, year charts, needed
  lists, SR publisher view) at register queries.
- Exclude collecting universes from topline Tracking/Reading stats.
- Let Quick Log, needs-data, and register editing work for collectible
  books (currently impossible — they're not in the register).
- Remove the catalog tab parsers and the dupe-check catalog sweep (both
  become dead code).

## Phase 3 — jerry: retire the old shape (after Phase 2 merges)

- Stop publishing the four catalog tabs.
- If the collecting sheet was still a live source, archive it read-only.

## Sequencing rule

Phases must land in order — the dashboard must never be pointed at columns
that aren't exporting yet, and the catalog tabs must never disappear before
the dashboard stops reading them.
