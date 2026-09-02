# Backups & restore

Everything needed to undo a bad sweeping update — register data or dashboard
rendering — already exists in two places. This file is the how-to.

## What is backed up, automatically

| What | Where | When | Retention |
|---|---|---|---|
| bookdb (full pg_dump) | jerry `/home/jerry/bookdb/backups/books-YYYY-MM-DD.sql.gz` | nightly 03:15 cron | 30 days |
| Published bundle `data/bookdb.json` | this repo's git history (one `data: bookdb export` commit per night) | nightly 03:00 publish | forever |
| Dashboard pages (`index.html` etc.) | this repo's git history | every code commit | forever |
| Nightly library totals | bookdb table `library_snapshots` (owner × collection counts) | nightly 02:45 | forever |

So: **register content** restores from the pg_dumps; **rendering/math/lookup
slip-ups** restore by reverting the repo; and even if the DB restore is
imperfect, every night's exact published bundle is still in git.

## Before a sweeping update (ritual)

Take a labeled dump first — cheap (~320 KB). Note the nightly cleanup
deletes by AGE, not by name, so a labeled dump also expires after 30 days;
copy it off-box if it must outlive that:

```bash
ssh jerry@192.168.4.122 "docker exec bookdb-postgres pg_dump -U books books | gzip > /home/jerry/bookdb/backups/books-pre-<label>-$(date +%F).sql.gz"
```

## Restore the register (bookdb) to a night

```bash
ssh jerry@192.168.4.122
# 1. stop writers so a 15-min inbox poll can't interleave
crontab -l > /tmp/cron.bak && crontab -l | grep -v '# bookdb' | crontab -
# 2. restore into a SCRATCH db first and sanity-check counts
docker exec bookdb-postgres createdb -U books books_restore
zcat /home/jerry/bookdb/backups/books-2026-09-01.sql.gz | docker exec -i bookdb-postgres psql -U books -d books_restore
docker exec bookdb-postgres psql -U books -d books_restore -c "SELECT count(*) FROM books WHERE active"
# 3. only when the scratch looks right, swap it in
docker exec bookdb-postgres psql -U books -d postgres -c "ALTER DATABASE books RENAME TO books_broken; ALTER DATABASE books_restore RENAME TO books;"
# 4. restore cron, republish
crontab /tmp/cron.bak
/home/jerry/bookdb/publish.sh
# 5. once verified, drop books_broken
```

NocoDB may need "Sync Metadata" afterwards.

## Restore the dashboard / published data to a commit

```bash
git log --oneline -- index.html          # or data/bookdb.json
git checkout <good-sha> -- index.html    # take just that file back
git commit -m "revert index.html to <good-sha>" && git push
```

Pushing to main deploys via GitHub Pages in ~1 min. To restore last night's
DATA specifically, `git checkout <sha> -- data/bookdb.json` works the same
way — but remember the 03:00 publish will overwrite it the next night, so a
data revert usually wants the DB restore above too.

## Verify after any restore

- `publish.log` / live-site md5 (publish.sh self-verifies for 5 min).
- Dashboard Overview totals vs `SELECT count(*) FROM books WHERE active`.
- `inbox.log` on the next quarter-hour for a clean poll.
