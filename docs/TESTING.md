# Test harnesses

Three layers, cheapest first. None need node.

## 1. Publish gate — `jobs/check_bundle.py` (jerry, automatic)

Runs inside `publish.sh` between the export and the site-clone copy, every
publish (nightly 03:00 and each inbox cycle that republishes). Compares the
fresh `bookdb.json` against the DB and the dashboard's parser contracts:
required tabs and exact header rows, register row count vs active books,
media vocabulary and the canonical `eBook` casing, Book Mail date order,
Proposals row count, `universePages` vs the collecting flags. Any failure
aborts the publish before anything ships — the reasons land in
`publish.log`.

## 2. Render harness — `tests/harness.html` (browser, on demand)

End-to-end render assertions: loads the real `index.html` in an iframe and
checks the numbers on screen against expectations recomputed from the same
bundle — Overview totals, History media columns, This Year targets, Buy This
Year and Book Mail row counts and ordering, Collecting FR tiles, the Proposed
count, and captured runtime errors. Data drift can't fail it; only a
rendering/math/lookup slip can.

Run it with the dev server:

```bash
py -m http.server 8123 --directory bookDashboards
```

then open <http://localhost:8123/tests/harness.html>. Green list = pass; the
tab title shows ❌ on any failure. It also runs fine against the live site
copy — anything that serves the repo root.

## 3. Inbox seam harness — `jobs/test_apply_inbox.py` (jerry, on demand)

Exercises every inbox event seam against real rows, then rolls back —
nothing commits. Deliberate-error checks print as `***` lines and count in
the "failed" tally by design; the tally line names the expected split.

```bash
cd /home/jerry/bookdb && PW=$(cat .pgpass_gen) && docker run --rm \
  --network bookdb_default \
  -e DATABASE_URL=postgresql://books:$PW@bookdb-postgres:5432/books \
  -v /home/jerry/bookdb/jobs:/app bookdb-jobs python test_apply_inbox.py
```
