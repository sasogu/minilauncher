#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_FILE="$ROOT_DIR/app/build.gradle.kts"
RELEASE_APK="$ROOT_DIR/app/build/outputs/apk/release/app-release.apk"
CREATE_TAG=0
CREATE_GITHUB_RELEASE=0
SKIP_TESTS=0
ALLOW_DIRTY=0

usage() {
  cat <<'EOF'
Usage:
  ./scripts/release.sh [options]

Options:
  --tag               Create git tag v<versionName> if missing
  --github-release    Create GitHub release from tag using gh CLI
  --skip-tests        Skip unit tests
  --allow-dirty       Allow running with uncommitted changes
  -h, --help          Show this help

Steps:
  1. Validate git status
  2. Sync fastlane changelog for current versionCode
  3. Run unit tests (unless skipped)
  4. Build release APK
  5. Optionally create git tag and GitHub release
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --tag) CREATE_TAG=1 ;;
    --github-release) CREATE_GITHUB_RELEASE=1 ;;
    --skip-tests) SKIP_TESTS=1 ;;
    --allow-dirty) ALLOW_DIRTY=1 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage; exit 1 ;;
  esac
  shift
done

version_name="$(sed -n 's/^[[:space:]]*versionName[[:space:]]*=[[:space:]]*"\([^"]*\)"/\1/p' "$BUILD_FILE" | head -n1)"
version_code="$(sed -n 's/^[[:space:]]*versionCode[[:space:]]*=[[:space:]]*\([0-9][0-9]*\)/\1/p' "$BUILD_FILE" | head -n1)"
tag_name="v${version_name}"

if [[ -z "$version_name" || -z "$version_code" ]]; then
  echo "Could not determine versionName/versionCode from $BUILD_FILE" >&2
  exit 1
fi

cd "$ROOT_DIR"

if [[ $ALLOW_DIRTY -ne 1 ]] && [[ -n "$(git status --short)" ]]; then
  echo "Working tree is not clean. Commit or stash changes first, or use --allow-dirty." >&2
  exit 1
fi

echo "==> Syncing fastlane changelog for version $version_name ($version_code)"
"$ROOT_DIR/scripts/update_fastlane_changelog.sh" "$version_name" "$version_code"

if [[ $SKIP_TESTS -ne 1 ]]; then
  echo "==> Running unit tests"
  ./gradlew :app:testDebugUnitTest
fi

echo "==> Building release APK"
./gradlew :app:assembleRelease

if [[ ! -f "$RELEASE_APK" ]]; then
  echo "Release APK not found at $RELEASE_APK" >&2
  exit 1
fi

if [[ $CREATE_TAG -eq 1 ]]; then
  if git rev-parse "$tag_name" >/dev/null 2>&1; then
    echo "==> Tag $tag_name already exists"
  else
    echo "==> Creating tag $tag_name"
    git tag "$tag_name"
  fi
fi

if [[ $CREATE_GITHUB_RELEASE -eq 1 ]]; then
  if ! command -v gh >/dev/null 2>&1; then
    echo "gh CLI is required for --github-release" >&2
    exit 1
  fi
  if ! git rev-parse "$tag_name" >/dev/null 2>&1; then
    echo "Tag $tag_name does not exist. Run with --tag or create it first." >&2
    exit 1
  fi

  notes_file="$ROOT_DIR/fastlane/metadata/android/en-US/changelogs/${version_code}.txt"
  echo "==> Creating GitHub release $tag_name"
  gh release view "$tag_name" >/dev/null 2>&1 || gh release create "$tag_name" --title "$tag_name" --notes-file "$notes_file"
fi

echo
echo "Release checks finished successfully."
echo "Version name: $version_name"
echo "Version code: $version_code"
echo "Release APK: $RELEASE_APK"
