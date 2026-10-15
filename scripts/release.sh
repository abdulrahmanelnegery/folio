#!/usr/bin/env bash
# Build the release APKs, name them, write a checksum next to each, and optionally publish.
#
#   scripts/release.sh            build into dist/<version>
#   scripts/release.sh --publish  also create the GitHub release for that version
#
# Signing: MANHAL_KEYSTORE (path), MANHAL_KEYSTORE_PASSWORD, optional MANHAL_KEY_ALIAS.
# --publish refuses to run unsigned, with an empty changelog section, or with an APK whose signature does not verify.
# Publishing needs the GitHub CLI, logged in with a token that can write to this repository.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

VERSION="$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts | head -1)"
: "${VERSION:?could not read versionName}"
PUBLISH=0; [ "${1:-}" = "--publish" ] && PUBLISH=1
CHANGELOG_FILE="${CHANGELOG_FILE:-CHANGELOG.md}"
NOTES="$(awk -v v="## $VERSION" '$0==v{p=1;next} /^## /{p=0} p' "$CHANGELOG_FILE")"

if [ -z "${MANHAL_KEYSTORE:-}" ]; then
  [ "$PUBLISH" = 1 ] && { echo "error: --publish needs MANHAL_KEYSTORE (and MANHAL_KEYSTORE_PASSWORD); refusing to publish unsigned APKs" >&2; exit 1; }
  echo "note: MANHAL_KEYSTORE is not set, the APKs will be unsigned" >&2
fi
if [ "$PUBLISH" = 1 ] && [ -z "$(printf '%s' "$NOTES" | tr -d '[:space:]')" ]; then
  echo "error: $CHANGELOG_FILE has no \"## $VERSION\" section with text; refusing to publish" >&2; exit 1
fi

./gradlew --no-daemon :app:testDebugUnitTest :app:assembleRelease

OUT="dist/$VERSION"
rm -rf "$OUT"; mkdir -p "$OUT"
for abi in arm64-v8a armeabi-v7a x86_64; do
  cp "app/build/outputs/apk/release/app-$abi-release.apk" "$OUT/Manhal_${VERSION}_Android_$abi.apk"
done

# One checksum file per release file, named after it, so `sha256sum -c <file>.sha256` works.
(cd "$OUT" && for f in Manhal_*; do case "$f" in *.sha256) ;; *) sha256sum -- "$f" > "$f.sha256" ;; esac; done)
ls -l "$OUT"

if [ "$PUBLISH" = 1 ]; then
  # Every APK must carry a valid signature before anything is uploaded.
  APKSIGNER="$(ls -d "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1)"
  [ -n "$APKSIGNER" ] || { echo "error: apksigner not found (set ANDROID_HOME); cannot verify the signatures" >&2; exit 1; }
  for apk in "$OUT"/*.apk; do "$APKSIGNER" verify "$apk" >/dev/null || { echo "error: $apk is not validly signed; nothing published" >&2; exit 1; }; done
  gh release create "v$VERSION" "$OUT"/* --title "Manhal $VERSION" --notes "$NOTES"
fi
