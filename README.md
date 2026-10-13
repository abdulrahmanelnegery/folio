# Folio

A calm, fast reader for PDFs and text files on Android, built natively with Kotlin and Jetpack Compose.

Folio is made for long reading: your books live in a library with covers, lists and shelves, every book remembers its place, and the pages follow the light with night and sepia themes.

## What is in it

- **Home**: three sticky tabs you can flick between, round and round. **Home** has Continue reading and the latest three books; **Stats** has your reading time, sittings, the books you read most and recent sittings; **Library** has your shelves, lists and favorites. Order the tabs or hide any but Home in Settings.
- **Library**: real covers, five ways to view it (grid, small covers, large covers, list, details), sorting by last opened, date added, name, size, progress or rating, search, and a side menu for shelves, lists and files.
- **Lists inside lists**: open a list and the lists inside it appear as folders, like a file explorer, with a breadcrumb and back. Hold a book or a folder to open, move, remove, rename, nest or delete it. Select several books to act on them together.
- **File explorer**: browse the phone (Downloads, Documents, SD cards), search a folder and everything below it, filter, sort, and add books straight to the library or to a list. The same file reached by two routes is one book.
- **Reading**: continuous scrolling, pinch and double-tap zoom, tap to hide the bars, a slim progress bar, jump to a page, bookmarks, comments on pages, auto-scroll with adjustable speed, margin cropping, and the book's own theme, brightness and rotation (each book remembers its own).
- **Themes**: Normal, Night, Soft night and Sepia. Night flips lightness and keeps hue, so figures stay readable.
- **Text, Markdown and code files** (`.txt`, `.md`, `.json`, `.yml`, `Dockerfile`, shell, config and source files): highlighted reading with line numbers, Markdown rendered, an edit mode, save, save as, and a prompt before you lose changes.
- **Ratings, notes and details**: stars, a note for each book, and a details sheet (file name, page size, producer, dates, where it is stored). Long files are read in the background and big books are drawn within a fixed memory budget.
- **Backups**: reading places, lists, ratings, notes and bookmarks are copied automatically and can be restored, or saved to a file from Settings.
- **Private**: everything stays on the device. No account, no telemetry, no network access.

## Your data

Folio keeps its library, backups and cover thumbnails in one place. By default that is inside the app. If you allow *All files access* (Settings), it uses a visible `Folio` folder on the phone, which survives reinstalling the app. Earlier versions used a hidden `.folio` folder; it moves into `Folio` automatically.

## Install

Get the file for your phone from the [latest release](../../releases/latest). Each file has a matching `.sha256` checksum.

| System | File |
|---|---|
| Android phone (64-bit, almost every phone since 2016) | `Folio_<version>_Android_arm64-v8a.apk` |
| Android, older 32-bit | `Folio_<version>_Android_armeabi-v7a.apk` |
| Android emulator, Chromebook | `Folio_<version>_Android_x86_64.apk` |

Open the APK and allow installs from your file manager when Android asks.

## Build

You need JDK 17 or newer and the Android SDK (platform 36).

```
./gradlew :app:testDebugUnitTest      # the tests
./gradlew :app:assembleDebug          # debug APKs in app/build/outputs/apk/debug
scripts/release.sh                    # signed release APKs with checksums (see the script for the keystore variables)
```

PDFs are drawn by Android's own `PdfRenderer` on a worker thread; the library is one small JSON file; the rest is plain Compose. The code is split into `data` (the library model and its tests), `pdf` (the renderer) and `ui` (the screens).

## Roadmap

EPUB and comic books, editing PDFs (highlights, drawing, comments) with save-as, text search and in-app translation, and a Linux edition.

## Licence

MIT. See [LICENSE](LICENSE).
