#!/usr/bin/env bash
#
# Posts the drafts whose row in TO-POST.md has the status "ready".
#
# Nothing is sent without --confirm. Run it once without, read what it prints,
# and only then run it again with.
#
#   ./specs/issues/post.sh            # prints what it would do
#   ./specs/issues/post.sh --confirm  # creates the issues and the comments
#
# A draft starts with REPO: and TITLE: lines followed by ---. Those lines are
# instructions and are stripped from what is posted. A draft with a
# COMMENT-ON: <number> line instead of TITLE: is posted as a comment on that
# issue.
#
# A draft links its reproducer zip by commit SHA, written as the placeholder
# COMMIT_SHA. This substitutes the SHA of HEAD, so the zips have to be
# committed and pushed first, which is what the checks below insist on.

set -euo pipefail

cd "$(dirname "$0")/../.."

CONFIRM=no
[ "${1:-}" = "--confirm" ] && CONFIRM=yes

# A finding that is not new in this release can go out as "PiT 25.4: " instead.
PREFIX="${PREFIX:-Hackathon 25.4: }"
TRACKER=9642
TRACKER_REPO=vaadin/platform
TRACKER_HEADING="## Hackathon"
DIR=specs/issues

header() {
    sed -n "1,/^---\$/s/^$2: //p" "$1" | head -1
}

add_to_tracker() {
    local url=$1 body
    body=$(mktemp)
    gh issue view "$TRACKER" --repo "$TRACKER_REPO" --json body --jq .body > "$body"
    if grep -qF "$url" "$body"; then
        echo "    already in platform#$TRACKER"
    else
        # $(cat) strips the trailing newlines gh adds, so the list does not
        # grow a blank line before every entry.
        if grep -qxF "$TRACKER_HEADING" "$body"; then
            printf -- "%s\n- [ ] %s\n" "$(cat "$body")" "$url" > "$body.new"
        else
            printf -- "%s\n\n%s\n\n- [ ] %s\n" "$(cat "$body")" "$TRACKER_HEADING" "$url" > "$body.new"
        fi
        gh issue edit "$TRACKER" --repo "$TRACKER_REPO" --body-file "$body.new" > /dev/null
        echo "    added to platform#$TRACKER"
        rm -f "$body.new"
    fi
    rm -f "$body"
}

DRAFTS=()
while IFS='|' read -r _ draft _ _ _ _ _ status _; do
    draft=$(echo "$draft" | tr -d '`[]' | xargs)
    status=$(echo "$status" | xargs)
    [ "$status" = "ready" ] && DRAFTS+=("$draft")
done < <(grep -E '^\| `?[0-9]' "$DIR/TO-POST.md" || true)

if [ ${#DRAFTS[@]} -eq 0 ]; then
    echo "No row in $DIR/TO-POST.md has the status ready."
    exit 0
fi

SHA=$(git rev-parse HEAD)
echo "HEAD is $SHA"
echo "origin is $(git config --get remote.origin.url || echo 'no remote')"
echo

# A link by SHA only works if the SHA is on the remote. These checks are the
# difference between an issue somebody can act on and one whose only useful
# link is a 404.
problems=0

if [ -n "$(git status --porcelain)" ]; then
    echo "! The working tree is dirty. Commit first, or the SHA is not what the zips contain."
    problems=1
fi

for draft in "${DRAFTS[@]}"; do
    file="$DIR/$draft"
    if [ ! -f "$file" ]; then
        echo "! $file is listed and does not exist."
        problems=1
        continue
    fi
    [ -n "$(header "$file" REPO)" ] || { echo "! $draft has no REPO: line."; problems=1; }
    [ -n "$(header "$file" TITLE)$(header "$file" COMMENT-ON)" ] \
        || { echo "! $draft has neither TITLE: nor COMMENT-ON:."; problems=1; }
    for zip in $(grep -oE "$DIR/projects/[A-Za-z0-9._-]+\.(zip|png)" "$file" | sort -u); do
        if ! git cat-file -e "$SHA:$zip" 2>/dev/null; then
            echo "! $zip, linked from $draft, is not in HEAD. Commit it, or its link will 404."
            problems=1
        fi
    done
done

if ! git branch -r --contains "$SHA" 2>/dev/null | grep -q .; then
    echo "! HEAD is on no remote branch. Push first, or every project link will 404."
    problems=1
fi

if [ "$problems" -ne 0 ] && [ "$CONFIRM" = yes ]; then
    echo
    echo "Nothing was sent."
    exit 1
fi
[ "$problems" -ne 0 ] && echo

for draft in "${DRAFTS[@]}"; do
    file="$DIR/$draft"
    [ -f "$file" ] || continue
    repo=$(header "$file" REPO)
    title=$(header "$file" TITLE)
    target=$(header "$file" COMMENT-ON)
    body=$(mktemp)
    sed '1,/^---$/d' "$file" | sed "s/COMMIT_SHA/$SHA/g" > "$body"

    if [ -n "$target" ]; then
        if [ "$CONFIRM" = yes ]; then
            url=$(gh issue comment "$target" --repo "$repo" --body-file "$body")
            echo "$draft -> $url" | tee -a "$DIR/posted.txt"
        else
            echo "WOULD COMMENT on $repo#$target  (from $draft, $(wc -c < "$body" | xargs) bytes)"
        fi
    else
        if [ "$CONFIRM" = yes ]; then
            url=$(gh issue create --repo "$repo" --title "$PREFIX$title" --body-file "$body")
            echo "$draft -> $url" | tee -a "$DIR/posted.txt"
            add_to_tracker "$url"
        else
            echo "WOULD POST to $repo: $PREFIX$title  (from $draft, $(wc -c < "$body" | xargs) bytes)"
            echo "    and add it to platform#$TRACKER under \"$TRACKER_HEADING\""
        fi
    fi
    rm -f "$body"
done

echo
if [ "$CONFIRM" = yes ]; then
    echo "Done. Put each link at the end of its feedback row and set its TO-POST.md status to posted."
else
    echo "Dry run. Nothing was sent. Re-run with --confirm to post."
fi
