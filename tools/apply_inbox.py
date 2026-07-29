#!/usr/bin/env python3
"""Apply Log-view inbox events to bookdb. Runs on jerry.

Fetch/validate/archive is complete; the five apply_* functions are seams
that need bookdb specifics (see docs/INBOX.md for exact semantics).

Usage:
    GITHUB_TOKEN=github_pat_... ./apply_inbox.py --repo owner/bookInbox [--dry-run]

Cron (poll every 15 min, then re-export if anything applied):
    */15 * * * * cd ~/bookdb && GITHUB_TOKEN=$(cat ~/.inbox-token) \
        ./tools/apply_inbox.py --repo owner/bookInbox && ./export_bundle.sh
Exit codes: 0 = applied at least one event, 3 = inbox empty, 1 = error.
"""
import argparse
import base64
import json
import os
import sys
import urllib.request

API = "https://api.github.com"


def gh(token, method, path, body=None):
    req = urllib.request.Request(
        f"{API}{path}",
        method=method,
        data=json.dumps(body).encode() if body is not None else None,
        headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
        },
    )
    with urllib.request.urlopen(req) as r:
        return json.load(r) if r.status != 204 else None


def list_inbox(token, repo):
    try:
        entries = gh(token, "GET", f"/repos/{repo}/contents/inbox")
    except urllib.error.HTTPError as e:
        if e.code == 404:  # no inbox dir yet
            return []
        raise
    files = [e for e in entries if e["type"] == "file" and e["name"].endswith(".json")]
    return sorted(files, key=lambda e: e["name"])  # timestamps in names → chronological


def fetch_event(token, repo, entry):
    blob = gh(token, "GET", f"/repos/{repo}/contents/{entry['path']}")
    return json.loads(base64.b64decode(blob["content"])), blob["sha"]


def move_file(token, repo, entry, sha, dest_dir, extra=None):
    """Archive = create at dest + delete original (contents API has no move)."""
    blob = gh(token, "GET", f"/repos/{repo}/contents/{entry['path']}")
    gh(token, "PUT", f"/repos/{repo}/contents/{dest_dir}/{entry['name']}",
       {"message": f"archive: {entry['name']}", "content": blob["content"]})
    if extra:
        gh(token, "PUT", f"/repos/{repo}/contents/{dest_dir}/{entry['name']}.reason.txt",
           {"message": f"reason: {entry['name']}",
            "content": base64.b64encode(extra.encode()).decode()})
    gh(token, "DELETE", f"/repos/{repo}/contents/{entry['path']}",
       {"message": f"processed: {entry['name']}", "sha": sha})


def validate(ev):
    if not isinstance(ev, dict):
        return "not an object"
    if ev.get("v") != 1:
        return f"unknown schema version {ev.get('v')!r}"
    for k in ("id", "at", "by", "type", "payload"):
        if k not in ev:
            return f"missing {k}"
    if ev["by"] not in ("butthead", "goblin"):
        return f"unknown owner {ev['by']!r}"
    if ev["type"] not in ("finished", "acquired", "add_book", "order_new", "order_update"):
        return f"unknown type {ev['type']!r}"
    return None


# ── bookdb seams — fill these in against the real schema ────────────────
# Each gets the full event dict. Raise on failure (event goes to failed/);
# return a short human string for the log. Exact-match book refs on
# (title, series, owner); on no-match create a proposal, don't fuzzy-match.

def apply_finished(ev):
    raise NotImplementedError("mark read + append to yearly reading log")


def apply_acquired(ev):
    raise NotImplementedError("mark owned")


def apply_add_book(ev):
    raise NotImplementedError("create catalog proposal")


def apply_order_new(ev):
    raise NotImplementedError("append Book Mail order")


def apply_order_update(ev):
    raise NotImplementedError("update order fields present in payload.set")


APPLY = {
    "finished": apply_finished,
    "acquired": apply_acquired,
    "add_book": apply_add_book,
    "order_new": apply_order_new,
    "order_update": apply_order_update,
}
# ─────────────────────────────────────────────────────────────────────────


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", required=True, help="owner/name of the inbox repo")
    ap.add_argument("--dry-run", action="store_true", help="list and validate only")
    args = ap.parse_args()

    token = os.environ.get("GITHUB_TOKEN")
    if not token:
        sys.exit("GITHUB_TOKEN not set")

    entries = list_inbox(token, args.repo)
    if not entries:
        print("inbox empty")
        sys.exit(3)

    applied = 0
    for entry in entries:
        ev, sha = fetch_event(token, args.repo, entry)
        err = validate(ev)
        if err:
            print(f"INVALID {entry['name']}: {err}")
            if not args.dry_run:
                move_file(token, args.repo, entry, sha, "inbox/failed", extra=err)
            continue
        if args.dry_run:
            print(f"would apply {entry['name']}: {ev['type']} by {ev['by']}")
            continue
        try:
            note = APPLY[ev["type"]](ev)
            month = ev["at"][:7]  # YYYY-MM
            move_file(token, args.repo, entry, sha, f"inbox/processed/{month}")
            print(f"applied {entry['name']}: {note or ev['type']}")
            applied += 1
        except Exception as e:  # noqa: BLE001 — quarantine anything unexpected
            print(f"FAILED {entry['name']}: {e}")
            move_file(token, args.repo, entry, sha, "inbox/failed", extra=str(e))

    sys.exit(0 if applied else 3)


if __name__ == "__main__":
    main()
