<p align="center"><img src="docs/icon.png" width="96" alt="Manhal"></p>

<h1 align="center">Manhal <sub>منهل</sub></h1>

<p align="center">A calm reader for PDFs, EPUB books, comics, text and paper books, by <a href="https://github.com/zimamlabs">Zimam Labs</a>.<br>Formerly Rukn and Folio. Android.</p>

<p align="center">
  <a href="../../releases/latest"><img alt="Release" src="https://img.shields.io/github/v/release/zimamlabs/manhal?color=FF5A1F&labelColor=1A1613"></a>
  <a href="../../releases"><img alt="Downloads" src="https://img.shields.io/github/downloads/zimamlabs/manhal/total?color=FF5A1F&labelColor=1A1613"></a>
  <a href="LICENSE"><img alt="Licence: MIT" src="https://img.shields.io/badge/licence-MIT-FF5A1F?labelColor=1A1613"></a>
  <img alt="Android" src="https://img.shields.io/badge/Android-8.0%2B-FF5A1F?labelColor=1A1613">
</p>

## What it is

Manhal (منهل, a spring to drink from) keeps your reading in one calm place: a library with real covers, lists inside lists, a reader that remembers where you were, PDF markup that never touches the original until you say so, and a record of your reading time. No account, no telemetry.

## Features

- **Tour and what's new**: a short first-run tour (shown again from Settings) and a one-time note after each update.
- **Home**: a greeting, today's reading and shortcuts to every section, then three sticky tabs you can flick between, round and round. **Home** has Continue reading and the latest three books; **Stats** has your reading time, sittings, the books you read most and recent sittings; **Library** has your shelves, lists and favorites. Order the tabs or hide any but Home in Settings.
- **Library**: real covers, five ways to view it (grid, small covers, large covers, list, details), sorting by last opened, date added, name, size, progress or rating, search, and a side menu for shelves, lists and files.
- **Lists inside lists**: open a list and the lists inside it appear as folders, like a file explorer, with a breadcrumb and back. Hold a book or a folder to open, move, remove, rename, nest or delete it. Select several books to act on them together.
- **File explorer**: browse the phone (Downloads, Documents, SD cards), search a folder and everything below it, filter, sort, and add books straight to the library or to a list. The same file reached by two routes is one book.
- **Reading**: continuous scrolling, pinch and double-tap zoom, tap to hide the bars, a slim progress bar, jump to a page, bookmarks, comments on pages, auto-scroll with adjustable speed, margin cropping, and the book's own theme, brightness and rotation (each book remembers its own).
- **Editing PDFs**: *Edit this PDF* in the reader menu gives a highlighter (four colours), a pen (four colours, three widths), underline, sticky notes, an eraser, and undo and redo. Your marks are a draft kept apart from the file, saved at once, and still there after you close the app (a "draft" tag shows on the book). When you finish or close, choose **Save as a new file** (`name (edited).pdf`, next to the original or in the Manhal folder), **Overwrite the original** (asks twice; the original is copied to an `originals` folder first, the new file is written and checked beside it, and only then swapped in) or **Discard**. Highlights, underlines and drawings are painted into the page so every reader shows them; notes are real PDF comments that keep their text. Files that only come through another app's provider can be saved as new, not replaced.
- **Themes**: Normal, Night, Soft night and Sepia. Night flips lightness and keeps hue, so figures stay readable.
- **EPUB books**: read chapter by chapter with the four reading themes and a text size of your own; your place in the book is remembered.
- **Comics**: `.cbz` files, one page at a time, with a slider to jump.
- **Text, Markdown and code files** (`.txt`, `.md`, `.json`, `.yml`, `Dockerfile`, shell, config and source files): highlighted reading with line numbers, Markdown rendered, an edit mode, save, save as, and a prompt before you lose changes.
- **Ratings, notes and details**: stars, a note for each book, and a details sheet (file name, page size, producer, dates, where it is stored). Long files are read in the background and big books are drawn within a fixed memory budget.
- **Paper books**: add a book you read on paper and log sittings (the page you are on, or the pages you read, and the minutes). Progress, the Stats tab and your sessions follow along.
- **Details and covers**: every book has editable title, author, publisher, year, pages, ISBN and description, and a cover you can change from your gallery. *Find details online* searches Open Library, Google Books and the Internet Archive, and asks Wikidata for the original title of translated books (Arabic titles included), shows each result next to your values and applies only the parts you tick. An optional Google Books key in Settings avoids the shared daily limit.
- **Careful by default**: every clear, delete or remove asks first, saving changes to a book's details says what will change, and opening a book shows a loading screen until it is on the page you were reading.
- **Languages**: English and العربية (Arabic) with a full right-to-left layout; choose the language in Settings (or follow the phone), and optionally show numbers in Arabic-Indic digits.
- **Backups**: reading places, lists, ratings, notes and bookmarks are copied automatically and can be restored, or saved to a file from Settings.

## Languages

English and العربية (Arabic) with a full right-to-left layout. Choose the language in Settings, or follow the phone's, and optionally show numbers in Arabic-Indic digits (٠١٢٣).

## Screenshots

Screenshots: coming with the next release.

## Download

Get the file for your phone from the [latest release](../../releases/latest). Each file has a matching `.sha256` checksum.

| System | File |
|---|---|
| Android phone (64-bit, almost every phone since 2016) | `Manhal_<version>_Android_arm64-v8a.apk` |
| Android, older 32-bit | `Manhal_<version>_Android_armeabi-v7a.apk` |
| Android emulator, Chromebook | `Manhal_<version>_Android_x86_64.apk` |

Open the APK and allow installs from your file manager when Android asks.

## Verify

Every APK is signed with Manhal's own key. Android installs it without a store because the signature is valid, and shows its usual "unknown source" notice for anything that did not come from Google Play. To check a file is really ours, compare its signing certificate with this fingerprint:

```
SHA-256: FA:CB:FE:BE:B3:27:A7:F7:AF:17:23:91:60:12:6D:57:59:E9:39:B4:B5:C4:D5:A7:64:42:05:92:47:18:2E:D4
```

```
apksigner verify --print-certs Manhal_<version>_Android_arm64-v8a.apk
sha256sum -c Manhal_<version>_Android_arm64-v8a.apk.sha256
```

## Where your data lives

Manhal keeps its library, backups, covers and your own cover images in one place. By default that is inside the app. If you allow *All files access* (Settings), it uses a visible `Zimam/Manhal` folder on the phone (one `Zimam` folder holds every Zimam app, each in its own folder), which survives reinstalling the app. Earlier versions used a hidden `.folio` folder, a visible `Folio` folder, then a visible `Manhal` folder beside the others. Nothing is ever lost in the move: Manhal copies the old folder into `Zimam/Manhal` through temporary files, never overwrites a file already there (a clash is kept beside it as `name (from Manhal).ext`), checks every file by size and SHA-256, saves the library once before it writes, and leaves the old folder untouched as a backup. If anything does not verify it keeps using the old folder, says so in Settings and tries again later. Settings offers *Remove the old folder*, which asks first and checks every file again before it deletes anything.

## Privacy

No account, no ads, no telemetry. Manhal keeps your library on your phone. The only network use is the book lookup you start yourself. One lookup sends the words you typed (or the ISBN) to Open Library, Google Books and the Internet Archive, and, for Arabic text, to Wikidata; covers are downloaded from Open Library, the Internet Archive or Google. Those services see your phone's IP address. If you add a Google Books key in Settings it is sent with the Google request.

## What is tested

Unit tests cover the library file (damaged, truncated and empty files, backups, restore), the folder move, the PDF edit and save paths (with a stand-in writer and a real PDF writer test), text saving, EPUB package parsing and unzip safety, the translations and the theme choice. The reader, the EPUB and comic views and the screens are checked by hand on a phone; they have no automated tests yet, so a format listed above is supported as far as that hand testing goes.

## Build

You need JDK 17 or newer and the Android SDK (platform 36).

```
./gradlew :app:testDebugUnitTest      # the tests
./gradlew :app:assembleDebug          # debug APKs in app/build/outputs/apk/debug
scripts/release.sh                    # signed release APKs with checksums (see the script for the keystore variables)
```

PDFs are drawn by Android's own `PdfRenderer` on a worker thread; the library is one small JSON file; the rest is plain Compose. The code is split into `data` (the library model and its tests), `pdf` (the renderer) and `ui` (the screens).

## Roadmap

Android today, Linux later. Text search and in-app translation, selecting text to highlight (today a highlight is a box you drag), and editing marks you have already saved into a PDF.

## Licence

MIT. See [LICENSE](LICENSE).
