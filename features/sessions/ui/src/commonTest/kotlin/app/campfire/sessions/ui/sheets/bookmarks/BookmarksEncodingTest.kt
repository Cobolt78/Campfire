package app.campfire.sessions.ui.sheets.bookmarks

import kotlin.test.Test
import kotlin.test.assertEquals

class BookmarksEncodingTest {

  @Test
  fun decodeReturnsPlainTitleAndEmptyNotesWhenNoNewlinePresent() {
    val (title, notes) = decodeBookmarkTitleAndNotes("Chapter 3 key clue")
    assertEquals("Chapter 3 key clue", title)
    assertEquals("", notes)
  }

  @Test
  fun encodeUsesDefaultTitleWhenTitleInputIsBlank() {
    val encoded = encodeBookmarkTitleAndNotes(
      titleInput = "   ",
      notesInput = "",
      defaultTitle = "Bookmark @ 12:34",
    )
    assertEquals("Bookmark @ 12:34", encoded)
  }

  @Test
  fun encodeAndDecodePreserveMultilineNotes() {
    val encoded = encodeBookmarkTitleAndNotes(
      titleInput = "Important\nquote",
      notesInput = "Line one\nLine two",
      defaultTitle = "Bookmark @ 01:02",
    )
    assertEquals("Important quote\nLine one\nLine two", encoded)

    val (decodedTitle, decodedNotes) = decodeBookmarkTitleAndNotes(encoded)
    assertEquals("Important quote", decodedTitle)
    assertEquals("Line one\nLine two", decodedNotes)
  }

  @Test
  fun decodeNormalizesCrlfLineEndings() {
    val (title, notes) = decodeBookmarkTitleAndNotes("My bookmark\r\nNote A\r\nNote B")
    assertEquals("My bookmark", title)
    assertEquals("Note A\nNote B", notes)
  }

  @Test
  fun encodeUsesChapterDefaultWithNotesWhenTitleInputIsBlank() {
    val encoded = encodeBookmarkTitleAndNotes(
      titleInput = "   ",
      notesInput = "  Character note  ",
      defaultTitle = "Chapter 4",
    )
    assertEquals("Chapter 4\nCharacter note", encoded)
  }
}
