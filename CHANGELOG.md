# Changelog

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
