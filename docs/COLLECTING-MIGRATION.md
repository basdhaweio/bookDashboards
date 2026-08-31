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

## Phase 1 — DONE (jerry, 2026-08-20)

**Source of truth: bookdb, not the sheet.** `publish.py` never fetched the
collecting spreadsheet — it builds all four `1_vivOgW…` tabs from `books`
(the sheet id survives only as a bundle *key* the dashboard's parsers are
named after). `import_dnd.py` is a one-shot tool and its cron was removed on
2026-07-24 with the other sheet imports. So the 549 rows were already register
rows in the DB; the "import" was a no-op and the real gap was the export —
`Butthead Individual` filtered on `collection='main'`, which is precisely why
the register tab showed zero collectible books. The July→August row drift the
brief noticed was DB edits, not a live sheet. **The old spreadsheet is already
dead; Phase 3 has nothing to retire but the four tabs.**

Schema (`migrations/009_collecting.sql`, applied):

- `books.pub_date` — ISO *prefix*, `CHECK`-constrained to `YYYY`, `YYYY-MM`,
  or `YYYY-MM-DD`. Not a `date` column: most FR/DL rows only ever knew month
  and year, and a real date would force an invented day. `publish_text` keeps
  the raw string untouched. Backfilled by `backfill_pub_date.py` — 557/557
  parsed, 0 unparsed (507 month+year, 49 full dates, 1 year-only; the extra 8
  beyond the 549 are Star Wars rows that also had publish_text).
- `books.publisher`, `books.need` — already existed and were already carried
  by the import; they had simply never been exported. No DDL.
- `universes.collecting boolean NOT NULL DEFAULT false`, true for the three.
- `books_resolved` gains `pub_date` (distinct from the enrichment
  `publish_date`, which is a third party's claim about the same book).
- Media vocabulary collapsed to one ebook spelling: FR said `Epub`, SR said
  `eBook`, `meta_vocab` seeded `eBook`, and the canonical value is `EBook`.
  Harmless while the catalogs published alone; folding them into the register
  put all of them in the same picklist. 40 book rows + 1 vocab row fixed.

Verification — register rows vs the catalog tabs, per universe, all match:

| universe | tracked | owned | read | need | pub_date |
|---|---|---|---|---|---|
| Forgotten Realms | 307 | 298 | 138 | 9 | 307 |
| Dragonlance | 156 | 51 | 1 | 105 | 156 |
| Shadowrun | 86 | 49 | 43 | 0 | 86 |

Register grew 2,393 → 2,942 rows (+549). The three title collisions each
appear exactly twice under different authors *and* different universes
("Death Masks" Butcher/Greenwood, "City of the Dead" Perry-Resident Evil/
Jones-FR, "Extinction" DeCandido-Resident Evil/Smedman-FR). `need=x` appears
on 151 rows (9 FR + 105 DL + 37 SR after the seeding below), all of them
collecting rows.

**One correction to the brief, resolved same day:** the SR sheet has no Need
column (`idx, Title, Author, Publication Date, Owned, Read, Format,
Publisher`), so 37 of SR's 86 rows initially imported with no flag at all.
John (2026-08-20): in that catalog "need" simply meant "not owned" — the
unowned rows WERE the needed list. `migrations/010_sr_need_seed.sql` seeded
those 37 rows `need=true` as a one-time statement of what the source meant.
SR's register counts are now 86 tracked / 49 owned / 43 read / 37 need, and
every collecting row carries at least one flag. This does not soften the
going-forward rule: need is still never *inferred* from ownership.

### The Phase 2 contract

**Register tab** — `…|Butthead Individual`, 17 columns. Indices 0–14 are
unchanged, so existing parsers keep working; 15 and 16 are new:

| # | column | notes |
|---|---|---|
| 0 | Title | |
| 1 | Series | `Standalones` for books with no series (all 86 SR rows) |
| 2 | Sequence | SR carries the catalog's `idx` here |
| 3 | Author | |
| 4 | Type | genre |
| 5 | Media | canonical vocab; `EBook` not `Epub`/`eBook` |
| 6 | Owned | `x` / empty |
| 7 | Read | `x` / empty |
| 8 | Universe | the routing key — join to the Universes tab |
| 9 | Notes | |
| 10 | Owner | `butthead` / `goblin` |
| 11 | SubGenre | |
| 12 | Publisher | **already exported at 12** — 86/86 SR, 0 FR/DL |
| 13 | ReadOn | |
| 14 | AcquiredOn | |
| **15** | **PubDate** | **new** — `YYYY` \| `YYYY-MM` \| `YYYY-MM-DD`, or empty |
| **16** | **Need** | **new** — `x` / empty |

Year charts should slice `PubDate[0:4]` rather than reading the Year Catalog's
interleaved header rows; every collecting row has a PubDate, so no row is lost.

**Collecting flag** — a new synthetic tab `bookdb|Universes`, columns
`['Universe', 'Collecting']`, one row per universe in use (76 today),
`Collecting` = `x` or empty. All 76 are listed, not just the flagged ones, so
"not collecting" is distinguishable from "not exported". Flagging a fourth
universe in the DB routes it with no dashboard change — that is the whole
point of putting the flag here instead of in a hardcoded list.

`dndPages` and the four catalog tabs now select on `u.collecting` rather than
`collection='dnd'` too, so a collecting book added through the Log view
(which creates rows as `collection='main'`) still reaches them.

**Interim state to expect, and why it is fine.** The 549 books are now in the
register *and* still in the catalog tabs, so they are double-represented until
Phase 2 lands. Concretely: By Series went from ~2,284 to 2,833 books and now
nearly reconciles with Tracking's 2,834 (it was under-counting before — the
collectible books simply weren't in the register); the Log view's book search
now finds them, so Quick Log and register editing work for collectible books
for the first time; the missing-metadata warning rose to 563 because FR/DL/SR
rows carry no genre; and the dupe check will hit both the register and the
catalog sweep for the same book. Topline Tracking/Reading numbers are
**unchanged** (2,834 / 1,185), because those come from `Butthead Series`,
which already counted the whole library and which Phase 1 did not touch.
Verified in the browser against the real bundle: all five sections render,
zero console errors, FR overview still reads 307/138/298/9.

## Phase 2 — dashboard (repoint DONE 2026-08-20; topline exclusion HELD)

The Collecting section now derives everything from the register:
`buildCollecting()` in index.html filters `G.allIndBooks` by the universes
flagged in `bookdb|Universes` and rebuilds the exact structures the renderers
always consumed (`G.frBooks`, `G.frSeries`, `G.dlData`, `G.srData`) — no
renderer changed. The five catalog parsers, the four catalog-tab fetches, and
the dupe-check catalog sweep are deleted; the dupe panel now lists each
collectible book once instead of twice. The ✎ edit form gained Published
(`pub_date`) and Need fields, so register editing covers the new columns.

**Count verification (John's requirement: nothing shifts).** A parity harness
replicated the old JS parsers over the four catalog tabs and the new register
derivation over the same bundle, comparing every renderer-visible number:
per-universe totals, FR per-series stats incl. the fuzzy-enrichment path and
the status pie, FR/DL/SR per-year chart buckets, SR per-publisher splits, and
the needed/owned-unread row sets — all identical. In-browser, the rendered
number streams of the topline and all nine Collecting views were captured
before and after: seven of ten pixel-identical, three differ only by (a)
equal-size DL series bars permuting order (same multiset), (b) within-year
tie order in the DL owned-unread queue (same rows), and (c) SR's 46
month-precision dates now displaying "May 2006" instead of the raw "2006-05"
(the 40 full dates keep the "2010-May-14" style). Zero console errors.

**Topline exclusion: CANCELLED (John, 2026-08-20).** "Total tracked really is
that high — being part of a collection doesn't exclude them from being
tracked." The toplines stay whole-library; the original brief's exclusion
bullet is void. Phase 2 is therefore complete.

**Enrichment applied (011, 2026-08-20, John-approved):** genre on all 549
(FR/DL Fantasy, SR Scifi — needs-data warning 563 → 14); sub_genre='TTRPG'
as real data on the 549 books and 119 collecting series, and publish.py's
export-time TTRPG tag was removed (Log-added collecting books now tag
correctly); publisher by era — TSR <2000 (185), Wizards of the Coast
2000-2015 (255), Del Rey for the 2022+ DL Destinies (3). The 20 modern FR
books (2016+) stay blank: they're split across WotC / Harper Voyager /
Random House Worlds and need OL or manual assignment. Only-fills-blanks,
idempotent. Export ORDER BYs also gained unique tie-break keys — re-exports
of unchanged data are now byte-identical (ties used to flip row order run to
run). Visibility note: By Genre skips blank genres, so it gains the 87
series-less collecting books (Scifi +86, Fantasy +1); the 462 series-linked
ones stay out until series.genre is filled — a separate decision.

## Phase 3 — jerry: retire the old shape (after Phase 2 merges)

- **Stop publishing the four catalog tabs: DONE (2026-08-31).** Phase 2
  merged to main as 84a8458 and deployed; the four `1_vivOgW…` tab blocks
  (and the now-unused `DND_ID` constant) are deleted from `publish.py`.
  Bundle went 27 → 23 tabs; both `index.html` and the vestigial `db.html`
  verified rendering the slimmed bundle with zero console errors and
  unchanged counts (FR 307/298/138/9, DL 156/54/1/105, SR 86/49/43/37).
  `dndPages` stays — the Collecting overview page tiles still read it
  (re-point them at pageStats-style register queries before deleting it).
- ~~If the collecting sheet was still a live source, archive it read-only.~~
  Not needed — Phase 1 established the sheet has not been a source since
  2026-07-24.
- **Open Library pages/covers pass: DONE (2026-08-20).** `enrich.py` gained
  `--collecting` (restrict to collecting universes) and a contradiction
  guard: an OL `first_publish_year` more than 2 years from the register's
  `pub_date` means a wrong-book match — skipped and flagged, never written.
  Three passes wrote 117 `book_meta` rows (source=openlibrary, medium
  confidence, register columns untouched). Pages coverage now FR 278/307,
  DL 117/156, SR 54/86; 88 books are permanent OL no-datas (mostly SR
  novellas and anthology oddities). Three stable conflicts, all cases where
  OL matched a reissue or a similar title and the register is right:
  Lord of Stormweather (2003 vs OL 2008 reissue), The Titan of Twilight
  (1995 vs OL 2005 reissue), Lost Leaves from the Inn of the Last Home
  (2007 vs OL's 1987 "Leaves from the Inn of the Last Home"). No action.
- **seq (reading order) for FR/DL: dropped (2026-08-20).** Open Library's
  search API returns `series=None` even for Homeland and Dragons of Autumn
  Twilight, so there is no automated source. It also would not change what
  anyone sees: the Collecting series views sort books chronologically by
  `pub_date`, which for these catalogs is the reading order. Revisit only if
  a series view ever needs an order that differs from publication order
  (prequels), and then by hand.
- Remove the now-dead `dndPages` map from publish.py only if the dashboard's
  per-universe page tiles are re-pointed at pageStats-style register queries
  first — today the Collecting overviews still read `BUNDLE.dndPages`.
- Open question for John, not blocking: `collection='dnd'` is now pure
  provenance. It still drives the `Sub-Genre='TTRPG'` tagging in
  `Butthead Series`, `pageStats`, and the `library_snapshots` history, so it
  was deliberately left alone — retiring it would break the snapshot series'
  comparability.

## Sequencing rule

Phases must land in order — the dashboard must never be pointed at columns
that aren't exporting yet, and the catalog tabs must never disappear before
the dashboard stops reading them.
