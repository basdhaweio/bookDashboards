# Both readers (the header's Both)

John and Shereen keep separate registers, and about 60 works sit in both
(the Icewind Dale Trilogy, Mistborn, Lore Olympus…). Under Both, a work
held in both registers is **one work in every total** (John, 2026-11-22
for Completion and the Overview; 2026-11-23 "fix the other tabs too"):

- **owned** if either owns it, **read** if either has read it, with an
  **audio copy** if either owned copy is one;
- counted once, under **John's entry**: his series, type, genre and
  universe decide where it lands in a breakdown;
- a work is its title plus its first author's surname (`bothKey`). Only a
  twin *across* the two registers folds; two entries inside one register
  stay as the register has them (Com's *Doom Brigade* sits in two series).

`bothWorks()` builds it once per data load (`G.bothAll`): the book list
(`works`) and the register rows (`rows`: Shereen's twin leaves her row and
its read/owned/audio join his). Under Both, `applyOwner` hands them out as
`G.works` and `G.workRows`, and `G.books` (calcBooks) is built from the
rows, so every total agrees: 3,411 tracked = Com 3,054 + Shereen 417 − 60
(2026-11-23).

## Totals that count works

Overview (cards, rates, pages, owned by format, by-type chart) · By Type ·
By Genre · By Universe (tiles and a universe's headline) · Completion
(authors, TTRPG) · To Read · By Series · Collecting (Forgotten Realms,
Dragonlance, Shadowrun) · To Acquire (need to buy; a book you have both
read and neither owns is one to buy) · Library Growth.

Pages are summed per register by jerry, so `bothPageOverlap()` takes the
second copy out when both copies were counted with pages.

Library Growth counts **physical copies**: a work you both own is one book
plus a second copy when both copies are physical (a digital copy of a
print book adds nothing — John's Lirael audiobook beside Shereen's print
is one book). `bothWorks().copies` carries that count.

## Views that keep each reader's copy

These are lists of what each register holds, so a shared work shows once
per reader, tinted or labelled by whose it is:

- Register (a tile per reader's series), TBR, Plan, This Year, History,
  Home — per-reader by design;
- Rooms — two copies are two books on two shelves;
- a universe page's series groups (Old Kingdom · Com / Old Kingdom ·
  Shereen) — only the headline counts works;
- To Acquire's read-not-owned lists (one per reader, the other reader's
  copy noted as already in the house).

## Collecting and the reader filter

The universe pages draw once per data build; `buildCollecting` clears
their drawn state, so a new reader filter redraws them (before
2026-11-23 they kept the first reader's numbers until a reload). A reader
with no books in a universe (Shereen has no Dragonlance or Shadowrun) gets
a one-line notice (`colEmpty`) instead of the last reader's numbers.

Tests: the harness checks "Completion on Both…", "Both: a work held in
both registers is one work in every total" (real data across the tabs plus
made-up registers) and "Collecting redraws when the header's reader
changes".
