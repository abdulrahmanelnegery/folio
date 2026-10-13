# Folio

A calm, fast PDF reader for Android, built natively with Kotlin and Jetpack Compose.

Folio is made for long reading: your books live in a library with covers, you pick up exactly where you stopped, and the pages can follow the time of day with night and sepia themes.

## Features

- **Library** with real covers. Five ways to look at it: grid, small covers, large covers, list and details. Sort by last opened, date added, name, size, progress or rating.
- **Shelves** in a side menu: Reading now, To read, Finished, Favorites, All books, and **your own lists, which can hold other lists**. Reorder, rename and delete them from *Manage lists*.
- **Continue reading** card that opens the last book at the page and spot you left, and a progress bar on every book.
- **Hold a book** to see everything about it: file name, pages, page size, size, PDF version, title, author, producer and dates read from the file, where it is stored, when you added and last opened it.
- **Ratings, notes and comments**: stars for each book, a note for the whole book, and comments pinned to pages. Bookmarks too.
- **Reading themes**: Normal, Night, Soft night and Sepia. One tap on the sun or moon flips the page; hold it to pick any theme.
- **Reading tools**: auto-scroll with adjustable speed, brightness control for the book, a page slider that names the page while you drag, jump to a page by number, pinch and double-tap zoom, tap to hide the bars, screen stays awake.
- **Its own `.folio` folder** (optional, with *All files access*): the library, backups and cover thumbnails live in a hidden folder on the phone and survive reinstalling the app, like other readers. Without the permission Folio keeps them inside the app.
- **Backups**: reading places, lists, ratings, notes and bookmarks are copied automatically when you close a book (the last 40 are kept). Make one on demand, save one to a file, or restore any of them from Settings.
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

Text search, in-app translation of selected text (Google Translate), table of contents, drawing and highlighting on pages, a share sheet, a Linux desktop edition (`folio-linux`).

## Licence

MIT. See [LICENSE](LICENSE).
