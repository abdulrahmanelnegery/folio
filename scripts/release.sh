#!/usr/bin/env bash
# Build the release APKs, name them, write a checksum next to each, and optionally publish.
#
#   scripts/release.sh            build into dist/<version>
#   scripts/release.sh --publish  also create the GitHub release for that version
#
# Signing: FOLIO_KEYSTORE (path), FOLIO_KEYSTORE_PASSWORD, optional FOLIO_KEY_ALIAS.
# Publishing needs the GitHub CLI, logged in with a token that can write to this repository.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

VERSION="$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts | head -1)"
: "${VERSION:?could not read versionName}"
[ -n "${FOLIO_KEYSTORE:-}" ] || echo "note: FOLIO_KEYSTORE is not set, the APKs will be unsigned" >&2

./gradlew --no-daemon :app:testDebugUnitTest :app:assembleRelease

OUT="dist/$VERSION"
rm -rf "$OUT"; mkdir -p "$OUT"
for abi in arm64-v8a armeabi-v7a x86_64; do
  cp "app/build/outputs/apk/release/app-$abi-release.apk" "$OUT/Folio_${VERSION}_Android_$abi.apk"
done

# One checksum file per release file, named after it, so `sha256sum -c <file>.sha256` works.
(cd "$OUT" && for f in Folio_*; do case "$f" in *.sha256) ;; *) sha256sum -- "$f" > "$f.sha256" ;; esac; done)
ls -l "$OUT"

if [ "${1:-}" = "--publish" ]; then
  NOTES="$(awk -v v="## $VERSION" '$0==v{p=1;next} /^## /{p=0} p' CHANGELOG.md)"
  gh release create "v$VERSION" "$OUT"/* --title "Folio $VERSION" --notes "${NOTES:-Folio $VERSION}"
fi
