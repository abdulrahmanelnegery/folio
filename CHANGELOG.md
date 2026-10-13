# Changelog

## 0.8.4

- **Find details online** now asks Wikidata what an Arabic title is called in its original language (for example a translated novel), and searches the catalogues with that too. When nothing is found, a button uses what you typed as the title. (Issue #8)

## 0.8.3

- Rukn wears Zimam's colours: warm paper in the light theme, near-black in the dark theme, and an orange accent instead of teal.
- The progress bars under the books on a shelf line up: every title takes two lines, so the bars sit on the same line.

## 0.8.2

- The icon, splash and About page use the exact dot of the Arabic ز in the Zimam mark (a soft diamond with one corner cut off) instead of a rounded square.

## 0.8.1

- **Folio is now Rukn** (ركن, a corner or a pillar), made by [Zimam](https://github.com/zimamlabs). The package id stays the same, so it updates in place.
- A new launcher icon (a corner and a semi-square) that fits Android's icon shapes and themed icons, a short splash screen, "by Zimam" in the side menu, and an **About** page (version, links, licence).
- Opening a book is smoother: the book is built once the opening glide has finished, and closing is a short cross-fade.
- The visible data folder is now `Rukn`. On first start the old `Folio` or `.folio` folder moves into it. New backups are named `rukn-...`; older `folio-...` backups are still listed and restorable.

## 0.8.0

- **Paper books.** Add a book you read on paper ("+" then "Add a paper book"), then log sittings: the page you are on now, or the pages you read, and optionally the minutes. Progress, the Stats tab and your sessions update as you log.
- **Edit details and cover** for any book: title, author, publisher, year, pages, ISBN and description are all editable, and the cover can be changed from your gallery or removed. Details live in your Folio library; PDF files are not changed.
- **Find details online** searches Open Library, Google Books and the Internet Archive together, retries word by word for Arabic titles, shows each result against your values and lets you tick only the parts to use before anything is applied. An optional Google Books key is in Settings.
- Press and hold a custom list (in the side menu or on Home) for rename, move and delete.
- Back from a shelf returns to the Library tab, and back closes the side menu first.
- The side menu opens with a rightward swipe from the left 80dp; the middle button is gone and nothing underneath is blocked any more, so the menu button works again.
- One finger moves a zoomed page; the "+" menu folds back on an outside tap; a rating clears by tapping the same star or Clear.

## 0.7.5

- Opening and closing a book has a short, soft transition: the book grows and fades in from just under full size, and eases out as the library appears beneath.
- The side menu's edge swipe is easier to hit: a wider strip, the middle 200dp of the left edge excluded from Android's own back gesture, a small grip to show where to start, and a tap on the grip opens it too.
- Releases are signed with every APK signature scheme (v1 to v4), and the README says how to check the signing fingerprint.

## 0.7.4

- Zoom is dynamic: pinch zooms around the point between your fingers and follows them as you pan, up to 5x; a double tap glides in on the spot you tapped and back out. Pages sharpen in steps as you zoom.
- Back steps out one level at a time and remembers where you were: a book opened from Reading now (or a list, or a search, or the file explorer) returns there, then to Home, then asks to close.

## 0.7.3

- After a jump (the slider, Go to page, a bookmark) a **Back to page N** button appears for a while, so one tap returns you to where you were.
- The reading bar floats above the bottom edge instead of sitting on it, so reaching for the slider is not mistaken for Android's swipe to switch apps, and the slider is excluded from the system gesture.
- Pages draw faster: a quick small draw shows at once, the pages just ahead and behind are drawn quietly once you stop scrolling, and every page you have seen is kept as a small image on disk, so reopening a book or coming back to a page is nearly instant. Settings can clear them.

## 0.7.2

- Folio's data folder on the phone is now a visible `Folio` folder instead of the hidden `.folio`. The first start moves the old folder in (one rename, or a careful copy if both exist), so the library, backups and covers carry over.
- A new README.
- The same book listed twice is merged even when one entry is an older one with no stored file name (the case of a book first opened from the system picker and later from the explorer). It keeps the progress, lists, rating and notes, and takes the readable file path.
- The location of a book opened with the system picker reads "Downloads (opened with the file picker)" instead of an id.

## 0.7.1

- The home page is three real tabs under a sticky header: **Home** (continue reading and the latest three), **Stats** (reading time, sittings, most read, recent sittings) and **Library** (shelves, lists, favorites). Flick sideways to move between them, round and round. Order them or hide any but Home in Settings.
- The tools in the top bar follow the page: Home has none; shelves and lists have search, view, sort and select.
- The side menu closes when you tap outside it or swipe it left, and still opens only from the far left edge.

## 0.7.0

- **Large books**: page drawing no longer runs on the main thread, which was the cause of the stalls and of Android closing a heavy book. Pages are drawn on a worker, within a fixed pixel budget (a zoomed page can no longer take tens of megabytes), page sizes are learned as pages are drawn instead of opening every page up front, work for pages you scrolled past is dropped, and the page cache is sized to the phone and cleared when Android asks for memory.
- **Margins**: a crop button removes the blank margins around the text. It samples pages through the book and crops them all the same way, so the layout does not jump. Per book, on or off.
- The reading controls (theme, margins, brightness, rotation) are icons on a second row under the title. Theme is a dropdown that shows the paper and ink of each choice; rotation is a dropdown with an icon for each kind (normal, portrait, landscape, dynamic, locked), and the button shows the current one.
- On the home page, flick sideways to move between the headers, round and round with no end. The side menu opens only from a swipe that starts at the far left edge, so flicking across the page never opens it.

## 0.6.2

- The same file opened by two routes (the system picker and the explorer) is now one book, not two. Existing doubles are merged on start, keeping progress, lists, ratings, notes, bookmarks and reading time.
- A book's Location shows a readable path ("Phone › Download › name.pdf") with Arabic and other names decoded.
- Back goes to Home from any shelf (Reading now, To read, Finished, Favorites, All); inside lists it still goes up one list at a time; the exit question is asked only on Home.
- Short sittings show seconds instead of "0 min".

## 0.6.1

- The reading bar is slim: the page, a slider and the percentage in one row. The page name shows above it while you drag.
- Rotation and brightness are buttons next to the theme button, and they belong to the book: each book remembers its own, and the rest of the app is not affected.
- The home page has headers to switch between (Overview, Continue reading, Latest three, Reading stats, Most read, Lists, Favorites) so only the section you pick is shown.
- Opening a book shows its cover straight away and draws each page at half size first, so it feels faster.

## 0.6.0

- A home page you arrange: Continue reading, the latest three books, reading stats (today, week, streak, sittings, average sitting, a 7-day chart), most read this week, your lists and favorites. Switch sections on or off and reorder them from the page itself.
- Every time you open and close a book is kept as a sitting, so the stats can count them.
- Screen rotation choices: Normal, Portrait, Landscape, Dynamic (follows the sensor even when the phone's rotation is locked) or Locked.

## 0.5.0

- A file explorer of our own: breadcrumb, quick places (Phone, Downloads, Documents, SD cards), search in a folder and everything below it, filters (All, Books, Folders), sorting, hidden files, multi-select, and a mark on files already in the library. Tap a book to read it, or pick several and add them to the library or to a list.
- The side menu has a Files section: Browse files, Downloads, Documents.
- The round + button replaces Open PDF. On the shelves it browses files, picks a file, or creates a list; inside a list it adds books from the library or from the phone, or creates a list there.

## 0.4.4

- Inside a list, hold a book to open it, take it out of the list, move it to another list or add it to more lists.
- Hold a folder (a list inside a list) to open, rename, move, add a list inside, or delete it. Lists can be moved into other lists from Manage lists too.

## 0.4.3

- Take a book out of the list it is opened in, from its sheet or for several books at once.

## 0.4.2

- A list shows only its own books. The lists inside it appear as folder tiles (or rows) with a count, like a file explorer.

## 0.4.1

- Smoother opening and closing of books: page sizes are read in the background, covers show on the first frame, saving happens off the main thread.

## 0.4.0

- Lists you can walk into, with a breadcrumb and back; search inside a list.
- Select several books and add them to lists, favorite, finish or remove them together.
- Reading time and pages per day, with a streak and a 7-day chart on the home screen.

## 0.3.0

- Text, Markdown and code files: highlighted reading, an edit mode, save, save as and a close prompt.
- Back walks out one screen at a time and asks before closing Folio.

## 0.2.0

- Nested lists, five library views, a details sheet, ratings, notes, page comments, backups, auto-scroll, brightness and a page slider.

## 0.1.0

- Library with covers and shelves, a PDF reader with themes and a remembered place in every book.
