#!/usr/bin/env bash
# Posts the drafts listed in TO-POST.md. Dry run by default: shows what would
# be posted and where. --confirm sends. Refuses a dirty working tree so what
# is posted is what is committed.
set -euo pipefail
cd "$(dirname "$0")"

CONFIRM=${1:-}
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "Working tree is dirty. Commit first: what is posted must be what is committed." >&2
  exit 1
fi

grep -E '^\| [0-9]' TO-POST.md | while IFS='|' read -r _ draft repo _rest; do
  draft=$(echo "$draft" | xargs)
  repo=$(echo "$repo" | xargs)
  file="$draft"
  [[ -f $file ]] || { echo "Missing draft $file" >&2; exit 1; }
  title=$(sed -n 's/^TITLE: //p' "$file" | head -1)
  target=$(sed -n 's/^REPO: //p' "$file" | head -1)
  [[ $target == "$repo" ]] || { echo "$file: REPO header ($target) disagrees with TO-POST.md ($repo)" >&2; exit 1; }
  body=$(sed '1,/^$/d' "$file")
  if [[ $CONFIRM == "--confirm" ]]; then
    url=$(gh issue create --repo "$repo" --title "$title" --body "$body")
    echo "$draft -> $url" | tee -a posted.txt
  else
    echo "WOULD POST to $repo: $title  (from $draft)"
  fi
done

[[ $CONFIRM == "--confirm" ]] || echo "Dry run. Re-run with --confirm to post."
