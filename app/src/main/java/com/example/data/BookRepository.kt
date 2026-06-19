package com.example.data

import com.example.data.api.GeminiApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class BookRepository(private val bookDao: BookDao) {

    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()

    private val _downloadingState = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadingState: StateFlow<Map<String, Int>> = _downloadingState.asStateFlow()

    suspend fun getBookById(id: String): Book? {
        return bookDao.getBookById(id)
    }

    suspend fun insertBook(book: Book) {
        bookDao.insertBook(book)
    }

    suspend fun updateBook(book: Book) {
        bookDao.updateBook(book)
    }

    suspend fun deleteBook(book: Book) {
        bookDao.deleteBook(book)
    }

    /**
     * Search books via Gemini API (or classic fallbacks) and merge with existing saved/downloaded book states!
     */
    suspend fun searchBooks(query: String, useOceanScraper: Boolean = false): List<Book> {
        val searchResults = GeminiApiClient.searchBooks(query, useOceanScraper)
        // Check if any search results are already saved in the database
        return searchResults.map { result ->
            val existing = bookDao.getBookById(result.id)
            existing ?: result
        }
    }

    /**
     * Download chapters for offline reading, update progress periodically, and save to local Room db.
     */
    suspend fun downloadBook(book: Book, onProgress: (Int) -> Unit = {}) {
        _downloadingState.value = _downloadingState.value + (book.id to 0)
        onProgress(0)

        // Mock progressive updates for download experience
        updateDownloadProgressInDb(book, 15)
        delay(400)
        
        updateDownloadProgressInDb(book, 45)
        delay(400)

        // Fetch actual chapters from Gemini API or offline templates
        val chapters = GeminiApiClient.downloadBookChapters(book.title, book.author)
        
        updateDownloadProgressInDb(book, 80)
        delay(300)

        // Save downloaded book to local db
        book.isDownloaded = true
        book.downloadProgress = 100
        book.setChapters(chapters)
        book.totalPages = chapters.size // Total chapters is its length
        book.shelf = "Bookshelf" // Default shelf upon download
        book.addedTime = System.currentTimeMillis()
        book.lastReadTime = System.currentTimeMillis()
        
        bookDao.insertBook(book)

        _downloadingState.value = _downloadingState.value - book.id
        onProgress(100)
    }

    private suspend fun updateDownloadProgressInDb(book: Book, progress: Int) {
        _downloadingState.value = _downloadingState.value + (book.id to progress)
        val currentBook = book.copy(
            isDownloaded = false,
            downloadProgress = progress
        )
        bookDao.insertBook(currentBook)
    }

    suspend fun updateBookProgress(bookId: String, currentChapter: Int, currentPage: Int) {
        bookDao.getBookById(bookId)?.let { book ->
            book.currentChapterIndex = currentChapter
            book.currentPage = currentPage
            book.lastReadTime = System.currentTimeMillis()
            bookDao.updateBook(book)
        }
    }

    suspend fun toggleFavorite(bookId: String) {
        bookDao.getBookById(bookId)?.let { book ->
            book.isFavorite = !book.isFavorite
            bookDao.updateBook(book)
        }
    }

    suspend fun updateBookShelf(bookId: String, shelf: String) {
        bookDao.getBookById(bookId)?.let { book ->
            book.shelf = shelf
            bookDao.updateBook(book)
        }
    }

    suspend fun addHighlight(bookId: String, chapterIndex: Int, text: String, note: String = "", colorHex: String = "#FFFF8D") {
        bookDao.getBookById(bookId)?.let { book ->
            val highlights = book.getHighlights().toMutableList()
            val newHighlight = BookHighlight(
                id = UUID.randomUUID().toString(),
                chapterIndex = chapterIndex,
                text = text,
                note = note,
                colorHex = colorHex
            )
            highlights.add(newHighlight)
            book.setHighlights(highlights)
            bookDao.updateBook(book)
        }
    }

    suspend fun deleteHighlight(bookId: String, highlightId: String) {
        bookDao.getBookById(bookId)?.let { book ->
            val highlights = book.getHighlights().filterNot { it.id == highlightId }
            book.setHighlights(highlights)
            bookDao.updateBook(book)
        }
    }
}
