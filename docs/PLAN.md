# Reading › Plan — ideas, themes, slots

John, 2026-11-23: "in the past I've just kept a bullet list or google keep
somewhere with general ideas, themes, etc and curious about what I can do
here" — then "yes, let's start with 1 and 2": **themes with live progress**
and **an ideas inbox** to replace the Keep list. The tab, top to bottom:

1. **Ideas** — any year. The Keep list lives here.
2. The year toggle (from September it opens on next year), then the year's
   tiles, **themes**, the by-media / by-genre bars against the targets, the
   **slots** (this year: the Next up list), the shelf pool and the shape form.

One reader at a time, like the rest of the tab: under Both it is
`soloOwner()` (Com unless the pill says Shereen).

## Ideas

`reading_ideas` (migration 040), bundle tab `bookdb|Ideas`
(`Id|Owner|Position|Text|Notes|Used|UsedOn|Key|Added`), event `idea_set`.

- **Jot** one line (Enter or ＋ Idea), or **Paste a list…** — one idea per
  line; Keep's bullets and checkboxes (`•`, `☐`, `[ ]`, `1.`) are stripped,
  and the pasted order is kept at the top.
- Open ideas show newest first, six at a time ("show all N").
- Each idea says what it looks like (`ideaGuess`), and its first button
  follows from that:
  - on this reader's register → **→ Next up** (this year) or **→ Slot Y**;
    a book already queued or pinned says so instead;
  - words that describe a theme → **→ Theme Y** (the rule read from the
    words, see below), plus **→ Slot** when the rule is only a shape;
  - "Title by Author" not on the register → **＋ Track it** (the Books add
    form, prefilled, tracked not owned);
  - a book only the other reader has → **＋ Borrow it** (prefilled from
    their copy);
  - anything else, a note → **→ Theme** (counted by hand) or **→ Slot**.
- ✓ marks it done, ✎ edits the words and a note, ✕ drops it. A used idea
  folds away under "N used — what each became" (`used` + `used_on`), where
  ↺ reopens it. Track / Borrow leave the idea open: once jerry has the book
  it reads "on your register" and can be planned or ticked.
- Shapes belong to a later year (this year's plan is Next up), so **→ Slot**
  on this year's page lands in next year.

## Themes

`reading_themes` (migration 040), bundle tab `bookdb|Themes`
(`Id|Owner|Year|Position|Label|Rule|Target|Tally|Notes|Key`), event
`theme_set`. A theme is one year's goal: a label, an optional target, and a
**rule** — what counts. The dashboard counts the year's reads against the
rule, so progress is never typed in; a theme with no rule is counted by hand
(＋1 / −).

**What a read is checked against** (`thContext`): Com's reads come from the
year tabs, Shereen's from `bookdb|Reads`. Each read is tied to the register
book it was — exact title, then a known alias, then the series the read
names, then the copy read that year. Genre, format and pages are judged on
the read as logged (or the book when the read has none); everything else
(series, author, awards, shelf age…) on the register book.

**Rule keys** (whitelisted by jerry, `THEME_RULE`):

| key | counts a read when… |
|---|---|
| `genre` | the genre fits, family included: Fantasy takes Dark Fantasy, Horror the Survival/Cosmic kinds, Nonfiction History/Philosophy/Memoir; "Sci Fi" = "Scifi" |
| `media` | it was read in that format (Audio, eBook, Manga…) |
| `length` | short < 250 pages · medium 250–499 · long 500+ · doorstop 700+ |
| `series`, `universe` | the book is in it |
| `author` | a credited person matches (a surname alone works: "Le Guin") |
| `award` + `won` | a Hugo winner (`won`) or finalist — the only award on file |
| `shelf_years` | owned and on the shelf N+ years by the theme's year (AcquiredOn, else AcquiredYear) |
| `before`, `after` | first published before / from that year |
| `owned` | from your own shelf |
| `shared` | a work both registers hold (the Both rule's `bothKey`) |
| `new_author` | nobody credited had been read by this reader before that year |
| `series_goal` | the unit becomes a **series**: `finish` = a series of 2+ volumes whose last unread volume was read that year; `start` = its first read that year |

All keys combine with AND. Each card shows read (solid bar) and planned
(faint) against the target: **planned** is this year's Next up books that
fit, or a later year's slots — a pinned book by the book, a shape-only slot
by its shape. "Still to plan" = target − read − planned. **Fits** lists
shelf books that would count — the Plan pool's books (the next volume of
each series, the first of an unstarted one, unread standalones; never what
is already on Next up or pinned; a later year leaves out series under way
this year), owned first; a series theme offers that series in order; a
series goal offers the series closest to done (finish) or ready to start.
＋ Next up / ＋ Slot plans one (a slot made this way carries the theme's
label). The Themes met tile counts themes that reached their target.

**Words → rule** (`themeGuess`), shown as chips before anything is saved;
✎ on the card fixes whatever it got wrong:

- a number is the target — "6 nonfiction", "twelve audiobooks";
  "a month" ×12, "a week" ×52, "a quarter" ×4 ("one doorstop a month" = 12);
- genre and format use the slot parser's words (sci-fi, fantasy, nonfiction,
  audio, manga, short stories…); "classics" = published before 1970;
  "1980s" = 1980–1989; "before / after 1970"; "new releases" = the year;
- length: doorstop/brick/tome, long books, novellas/short reads, "700 pages";
- "finish / start … series" → a series goal; a named series or universe
  ("finish the Expanse", "finish Malazan") → that series, with the target
  set to its unread count; the reader's authors by full name or surname;
- "Hugo winners", "I own / on my shelf", "on my shelf 3+ years",
  "buddy reads with Shereen", "new-to-me authors".
- A one-word series, universe or surname ("Space", "Culture", "King")
  counts only when plainly used as a name — the whole line, followed by
  "series"/"books", or capitalised mid-line — so "space opera" stays a genre.

## Tests

jerry: `jobs/test_apply_inbox.py` — theme_set / idea_set add, keys, update,
rule canonicalisation and refusals, move, remove. Dashboard harness: "Plan
themes: a theme counts the year's reads against its rule" (bundle-derived
expectations), "words become a rule", "shelf suggestions fit the rule and
are not already planned", "Plan ideas: jot one, turn it into a theme,
reopen it — all before jerry answers" (stubbed inbox, nothing leaves the
page).
