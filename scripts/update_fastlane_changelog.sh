#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_FILE="$ROOT_DIR/app/build.gradle.kts"
CHANGELOG_FILE="$ROOT_DIR/CHANGELOG.md"
FASTLANE_DIR="$ROOT_DIR/fastlane/metadata/android/en-US/changelogs"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/update_fastlane_changelog.sh
  ./scripts/update_fastlane_changelog.sh <version-name> <version-code>

Behavior:
  - Reads the matching section from CHANGELOG.md
  - Writes fastlane/metadata/android/en-US/changelogs/<versionCode>.txt
  - If the section does not exist, creates a placeholder file
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if [[ $# -eq 2 ]]; then
  version_name="$1"
  version_code="$2"
elif [[ $# -eq 0 ]]; then
  version_name="$(sed -n 's/^[[:space:]]*versionName[[:space:]]*=[[:space:]]*"\([^"]*\)"/\1/p' "$BUILD_FILE" | head -n1)"
  version_code="$(sed -n 's/^[[:space:]]*versionCode[[:space:]]*=[[:space:]]*\([0-9][0-9]*\)/\1/p' "$BUILD_FILE" | head -n1)"
else
  usage
  exit 1
fi

if [[ -z "$version_name" || -z "$version_code" ]]; then
  echo "Could not determine versionName/versionCode." >&2
  exit 1
fi

mkdir -p "$FASTLANE_DIR"
output_file="$FASTLANE_DIR/${version_code}.txt"

section="$(awk -v version="$version_name" '
  $0 ~ "^##[[:space:]]+" version "$" { capture=1; next }
  capture && $0 ~ "^##[[:space:]]+" { exit }
  capture { print }
' "$CHANGELOG_FILE" | sed '/^[[:space:]]*$/d')"

if [[ -z "$section" ]]; then
  cat > "$output_file" <<EOF
Release $version_name

- Update CHANGELOG.md section for $version_name.
EOF
  echo "Created placeholder changelog: $output_file"
  exit 0
fi

printf '%s\n' "$section" > "$output_file"
echo "Updated fastlane changelog: $output_file"
