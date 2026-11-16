# Log › Books search: title and ISBN

John, 2026-11-16: "I keep trying to search for the title or the isbn and it
just doesn't go through". The book was *Code and Codex* (Yoon Ha Lee, Saga
Press, out 2026-10-27).
- Open Library had no record of it, by title or by ISBN.
- Keyless Google Books refused every call. Its quota for requests without a
  key is now 0, so it fails every time, not just after a busy day.
- Google's own record for the hardcover ISBN has no title.

His Book Notification list had the book all along. Apple Books had it too.

## Title search, in this order

1. **The register** for the current reader filter: title (aliases count),
   series, author, universe. Instant.
2. **Your Book Club list**: the Book Notification rows already in the bundle
   that this reader's register lacks. Instant, no web.
   - ＋ Add prefills title, author and series (`LOG.bcAdd`).
3. **Elsewhere**, asked live once typing pauses (600 ms). Each service gets
   at most 7 s.
   - **Open Library**: a title search, then a general search if that finds
     nothing. A name of two or three words is also tried as an author.
   - **Google Books**: only with this device's key (below).
   - **Apple Books**, through the iTunes Search API with `media=ebook`.
     - It needs no key, pages may call it, and it carries new releases
       before Open Library catalogues them.
     - Its listings are ebooks: title, author, release day and cover, but no
       ISBN.
     - A title like "Title (Series, Book n)" is split into title, series and
       number.

   A shared ISBN, or the same title by the same surname, makes one card.

   Ranking puts the exact title typed first, then titles that start with it,
   then titles that contain it. Within each group, the card with the most
   sources comes first. The list holds at most 12 cards.

## ISBN lookup and scan

1. An edition already on file (`G.isbnMap`, from `book_meta`): instant.
2. A proposal carrying the ISBN: instant.
3. Open Library and Google Books (with the key), asked in parallel. The first
   answer wins.
4. A miss offers 📦 Got it — I'll type it, or 👁 Track it.
   - Apple Books can't look up a print ISBN.
   - Some new books' print ISBNs have no title anywhere yet. For Code and
     Codex's 9781668059975, Google's record has no title and Open Library has
     no record. Type the title instead.
   - Once the book is in the register its ISBN is on file, so the next scan
     finds it at once.

## The Google Books key (one per device)

- **Make the key in Google Cloud.** Go to APIs & Services › Credentials ›
  Create credentials › API key, then restrict it:
  - Application restrictions: Websites, set to
    `https://basdhaweio.github.io/bookDashboards/*`. Add
    `http://localhost:8123/*` if the local preview should use it too.
  - API restrictions: Books API, which must be enabled in that project.
- **Paste it** under Log › Metadata › Google Books key and tap Save & test.
  The test runs one search.
  - The key is stored in this browser's `localStorage` under
    `library-gb-key`.
  - It never goes in the page source, the bundle or the inbox.
  - It is sent only to `www.googleapis.com/books`. Forget key removes it.
- **Quota:** a key in the same project as jerry's shares that project's daily
  quota. A separate project keeps the two apart.
- **jerry's own key is never used here.** It is unrestricted and stays on
  jerry.

Harness checks:
- "Books: a title search asks Open Library, Google Books and Apple Books and
  merges them"
- "Books: a new release on the Book Club list is found and prefills the add
  form"
- "Google Books key: kept on this device and sent only to Google Books"
