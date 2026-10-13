<p align="center"><img src="docs/icon.svg" width="120" alt="Rukn"></p>

<h1 align="center">Rukn</h1>

<p align="center">A calm reader for PDFs, text and paper books, by <a href="https://github.com/zimamlabs">Zimam</a>. Formerly Folio.</p>

A calm, fast reader for PDFs and text files on Android, built natively with Kotlin and Jetpack Compose.

Rukn is made for long reading: your books live in a library with covers, lists and shelves, every book remembers its place, and the pages follow the light with night and sepia themes.

## What is in it

- **Home**: three sticky tabs you can flick between, round and round. **Home** has Continue reading and the latest three books; **Stats** has your reading time, sittings, the books you read most and recent sittings; **Library** has your shelves, lists and favorites. Order the tabs or hide any but Home in Settings.
- **Library**: real covers, five ways to view it (grid, small covers, large covers, list, details), sorting by last opened, date added, name, size, progress or rating, search, and a side menu for shelves, lists and files.
- **Lists inside lists**: open a list and the lists inside it appear as folders, like a file explorer, with a breadcrumb and back. Hold a book or a folder to open, move, remove, rename, nest or delete it. Select several books to act on them together.
- **File explorer**: browse the phone (Downloads, Documents, SD cards), search a folder and everything below it, filter, sort, and add books straight to the library or to a list. The same file reached by two routes is one book.
- **Reading**: continuous scrolling, pinch and double-tap zoom, tap to hide the bars, a slim progress bar, jump to a page, bookmarks, comments on pages, auto-scroll with adjustable speed, margin cropping, and the book's own theme, brightness and rotation (each book remembers its own).
- **Themes**: Normal, Night, Soft night and Sepia. Night flips lightness and keeps hue, so figures stay readable.
- **Text, Markdown and code files** (`.txt`, `.md`, `.json`, `.yml`, `Dockerfile`, shell, config and source files): highlighted reading with line numbers, Markdown rendered, an edit mode, save, save as, and a prompt before you lose changes.
- **Ratings, notes and details**: stars, a note for each book, and a details sheet (file name, page size, producer, dates, where it is stored). Long files are read in the background and big books are drawn within a fixed memory budget.
- **Paper books**: add a book you read on paper and log sittings (the page you are on, or the pages you read, and the minutes). Progress, the Stats tab and your sessions follow along.
- **Details and covers**: every book has editable title, author, publisher, year, pages, ISBN and description, and a cover you can change from your gallery. *Find details online* searches Open Library, Google Books and the Internet Archive, and asks Wikidata for the original title of translated books (Arabic titles included), shows each result next to your values and applies only the parts you tick. An optional Google Books key in Settings avoids the shared daily limit.
- **Backups**: reading places, lists, ratings, notes and bookmarks are copied automatically and can be restored, or saved to a file from Settings.
- **Private**: everything stays on the device. No account and no telemetry. The only network use is the lookup you start yourself, and only the words you type are sent.

## Your data

Rukn keeps its library, backups, covers and your own cover images in one place. By default that is inside the app. If you allow *All files access* (Settings), it uses a visible `Rukn` folder on the phone, which survives reinstalling the app. Earlier versions called the app Folio and used a hidden `.folio` folder, then a visible `Folio` folder; either one moves into `Rukn` automatically.

## Install

Get the file for your phone from the [latest release](../../releases/latest). Each file has a matching `.sha256` checksum.

| System | File |
|---|---|
| Android phone (64-bit, almost every phone since 2016) | `Rukn_<version>_Android_arm64-v8a.apk` |
| Android, older 32-bit | `Rukn_<version>_Android_armeabi-v7a.apk` |
| Android emulator, Chromebook | `Rukn_<version>_Android_x86_64.apk` |

Open the APK and allow installs from your file manager when Android asks.

### Verify the download

Every APK is signed with Rukn's own key. Android installs it without a store because the signature is valid, and shows its usual "unknown source" notice for anything that did not come from Google Play. To check a file is really ours, compare its signing certificate with this fingerprint:

```
SHA-256: FA:CB:FE:BE:B3:27:A7:F7:AF:17:23:91:60:12:6D:57:59:E9:39:B4:B5:C4:D5:A7:64:42:05:92:47:18:2E:D4
```

```
apksigner verify --print-certs Rukn_<version>_Android_arm64-v8a.apk
sha256sum -c Rukn_<version>_Android_arm64-v8a.apk.sha256
```

## Build

You need JDK 17 or newer and the Android SDK (platform 36).

```
./gradlew :app:testDebugUnitTest      # the tests
./gradlew :app:assembleDebug          # debug APKs in app/build/outputs/apk/debug
scripts/release.sh                    # signed release APKs with checksums (see the script for the keystore variables)
```

PDFs are drawn by Android's own `PdfRenderer` on a worker thread; the library is one small JSON file; the rest is plain Compose. The code is split into `data` (the library model and its tests), `pdf` (the renderer) and `ui` (the screens).

## Roadmap

EPUB and comic books, editing PDFs (highlights, drawing, comments) with save-as, text search and in-app translation. A Linux edition comes after version 1.

## Licence

MIT. See [LICENSE](LICENSE).
