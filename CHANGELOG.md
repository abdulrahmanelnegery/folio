# Changelog

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
