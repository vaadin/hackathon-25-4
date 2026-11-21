#!/usr/bin/env bash
#
# Refreshes the pinned copies of the published JSON that the tests answer from.
#
#   scripts/refresh-fixtures.sh
#
# This is the only way the files under src/test/resources/pages/ change, and the
# only task in this repository that needs the network. Each fixture is the real
# answer of one endpoint shape of https://vaadin.github.io/cve-status/, byte for
# byte. The content type each one was served with goes to content-types.properties,
# so the fixture transport can answer with what the live host said.
#
# After a refresh the tests that assert on fixture content have to be brought up
# to the new generated_at, which this script prints at the end.

set -euo pipefail

BASE="${PAGES_BASE_URL:-https://vaadin.github.io/cve-status}"
TARGET="$(cd "$(dirname "$0")/.." && pwd)/src/test/resources/pages"

# One path per endpoint shape of specs/02-data-source.md, and for the per
# version and per advisory documents one per state a test needs.
PATHS=(
  cases/index.json
  cases/scan-index.json
  cases/cases.json
  cases/v/25.3.5.json
  cases/v/24.9.21.json
  cases/v/24.4.28.json
  cases/v/24.7.19.json
  cases/cve/CVE-2016-1000027.json
  cases/cve/CVE-2019-25028.json
  cases/cve/CVE-2021-33604.json
  data/front.json
  data/index.json
  data/queue.json
  data/25.3.5.json
  exports/25.3.5.csv
  exports/25.3.5.vex.json
  exports/25.3.5.dependency-check-suppressions.xml
  exports/25.3.5.osv-scanner.toml
  exports/25.3.5.bomber-ignore.txt
)

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# Everything is fetched before anything is replaced, so a failed refresh
# leaves the committed set as it was.
: > "$WORK/content-types.properties"
for path in "${PATHS[@]}"; do
  mkdir -p "$WORK/$(dirname "$path")"
  type="$(curl --fail --silent --show-error --location \
    --output "$WORK/$path" --write-out '%{content_type}' "$BASE/$path")"
  printf '%s=%s\n' "$path" "$type" >> "$WORK/content-types.properties"
  printf 'fetched %s\n' "$path"
done

rm -rf "$TARGET"
mkdir -p "$TARGET"
cp -R "$WORK"/. "$TARGET"/

printf 'pinned generated_at: %s\n' \
  "$(sed -n 's/.*"generated_at": *"\([^"]*\)".*/\1/p' "$TARGET/cases/index.json" | head -1)"
