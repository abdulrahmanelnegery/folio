# Folio

A calm, fast PDF reader for Android, built natively with Kotlin and Jetpack Compose.

Folio is made for long reading: your books live in a library with covers, you pick up exactly where you stopped, and the pages can follow the time of day with night and sepia themes.

## Features

- **Library** with covers, in a grid or a list. Shelves for *Reading now*, *To read*, *Finished* and *Favorites*, plus lists of your own.
- **Continue reading** card that opens the last book at the page and spot you left.
- **Search and sort** by title, last opened or size.
- **Reading themes**: Normal, Night, Soft night and Sepia. Night flips lightness and keeps hue, so figures stay readable.
- **Smooth reading**: continuous vertical scroll, pinch to zoom, double-tap to zoom, tap to show or hide the bars, slider to jump to a page.
- **Real Android behaviour**: the system file picker, "Open with" from other apps, edge-to-edge screens that respect the status and navigation bars, dark mode, and Arabic or any other file name.
- **Private**: everything stays on the device. No account, no telemetry, no network access.

## Install

Download the APK for your phone from the [releases](../../releases) page. Most phones from 2016 on need `arm64-v8a`. Open the file and allow installs from your file manager when Android asks.

## Build

Needs JDK 17 or newer and the Android SDK (platform 36).

```
./gradlew :app:assembleDebug        # debug APK per CPU type in app/build/outputs/apk/debug
```

For a signed release build set `FOLIO_KEYSTORE`, `FOLIO_KEYSTORE_PASSWORD` (and optionally `FOLIO_KEY_ALIAS`) and run `./gradlew :app:assembleRelease`.

## How it works

PDF pages are drawn by Android's own `PdfRenderer` (the system's PDFium), so there is no native code to ship. The library is one small JSON file in the app's private storage, and your place in each book is saved a moment after you stop scrolling.

## Roadmap

Bookmarks, text search and selection, table of contents, annotations, a Linux desktop edition (`folio-linux`) and a share sheet.

## Licence

MIT. See [LICENSE](LICENSE).
