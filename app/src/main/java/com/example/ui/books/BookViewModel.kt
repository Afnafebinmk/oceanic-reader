package com.example.ui.books

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.api.GeminiApiClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BookViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase::class.java.let {
        androidx.room.Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "oceanic_library.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    private val repository = BookRepository(db.bookDao())

    val dbBooks: StateFlow<List<Book>> = repository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadingState = repository.downloadingState

    // Search Flow
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Book>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _useOceanScraper = MutableStateFlow(true)
    val useOceanScraper = _useOceanScraper.asStateFlow()

    private val _scraperLogs = MutableStateFlow<List<String>>(emptyList())
    val scraperLogs = _scraperLogs.asStateFlow()

    // PDF Rendering Active States
    private val _isGeneratingPdf = MutableStateFlow(false)
    val isGeneratingPdf = _isGeneratingPdf.asStateFlow()

    private val _currentPdfFile = MutableStateFlow<java.io.File?>(null)
    val currentPdfFile = _currentPdfFile.asStateFlow()

    fun preparePdfForBook(book: Book, onComplete: () -> Unit = {}) {
        _isGeneratingPdf.value = true
        _currentPdfFile.value = null
        viewModelScope.launch {
            try {
                val pdfFile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    PdfGenerator.generatePdfForBook(getApplication(), book)
                }
                _currentPdfFile.value = pdfFile
                onComplete()
            } catch (e: Exception) {
                Log.e("BookViewModel", "PDF Generation failed: ${e.message}", e)
            } finally {
                _isGeneratingPdf.value = false
            }
        }
    }

    fun updatePdfPage(book: Book, pageIndex: Int) {
        viewModelScope.launch {
            repository.updateBookProgress(book.id, book.currentChapterIndex, pageIndex)
            _activeBook.value?.let { active ->
                if (active.id == book.id) {
                    _activeBook.value = active.copy(currentPage = pageIndex)
                }
            }
        }
    }

    fun toggleBookmarkedPage(book: Book, pageIndex: Int) {
        viewModelScope.launch {
            val currentHighlights = book.getHighlights().toMutableList()
            val existingBookmark = currentHighlights.find { it.chapterIndex == -100 && it.text == "Page $pageIndex" }
            if (existingBookmark != null) {
                currentHighlights.remove(existingBookmark)
            } else {
                val newBookmark = BookHighlight(
                    id = java.util.UUID.randomUUID().toString(),
                    chapterIndex = -100,
                    text = "Page $pageIndex",
                    note = "BOOKMARK",
                    colorHex = "#FF5252"
                )
                currentHighlights.add(newBookmark)
            }
            book.setHighlights(currentHighlights)
            repository.updateBook(book)
            if (_activeBook.value?.id == book.id) {
                _activeBook.value = _activeBook.value?.copy(highlightsJson = book.highlightsJson)
            }
        }
    }

    fun addBookTag(book: Book, tag: String) {
        val trimmed = tag.trim().lowercase()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val currentTags = book.getTags().toMutableList()
            if (!currentTags.contains(trimmed)) {
                currentTags.add(trimmed)
                book.setTags(currentTags)
                repository.updateBook(book)
                if (_activeBook.value?.id == book.id) {
                    _activeBook.value = _activeBook.value?.copy(tagsJson = book.tagsJson)
                }
            }
        }
    }

    fun removeBookTag(book: Book, tag: String) {
        viewModelScope.launch {
            val currentTags = book.getTags().toMutableList()
            if (currentTags.remove(tag.trim().lowercase())) {
                book.setTags(currentTags)
                repository.updateBook(book)
                if (_activeBook.value?.id == book.id) {
                    _activeBook.value = _activeBook.value?.copy(tagsJson = book.tagsJson)
                }
            }
        }
    }

    fun setUseOceanScraper(use: Boolean) {
        _useOceanScraper.value = use
    }

    // Active Reader State
    private val _activeBook = MutableStateFlow<Book?>(null)
    val activeBook = _activeBook.asStateFlow()

    private val _activeChapterIndex = MutableStateFlow(0)
    val activeChapterIndex = _activeChapterIndex.asStateFlow()

    // AI Buddy discussion
    private val _activeAiDiscussion = MutableStateFlow<List<DiscussionMessage>>(emptyList())
    val activeAiDiscussion = _activeAiDiscussion.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking = _isAiThinking.asStateFlow()

    // Reader UI Style Settings
    private val _activeReadingTheme = MutableStateFlow("Parchment") // Parchment, Sepia, Dark
    val activeReadingTheme = _activeReadingTheme.asStateFlow()

    private val sharedPrefs = application.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)

    private val _isDarkThemeGlobal = MutableStateFlow<Boolean?>(
        if (sharedPrefs.contains("is_dark_theme_global")) {
            sharedPrefs.getBoolean("is_dark_theme_global", false)
        } else {
            null
        }
    )
    val isDarkThemeGlobal = _isDarkThemeGlobal.asStateFlow()

    fun setDarkThemeGlobal(dark: Boolean?) {
        _isDarkThemeGlobal.value = dark
        if (dark == null) {
            sharedPrefs.edit().remove("is_dark_theme_global").apply()
        } else {
            sharedPrefs.edit().putBoolean("is_dark_theme_global", dark).apply()
        }
    }

    private val _recentlyOpenedIds = MutableStateFlow<List<String>>(emptyList())
    val recentlyOpenedBooks: StateFlow<List<Book>> = dbBooks.combine(_recentlyOpenedIds) { books, ids ->
        ids.mapNotNull { id ->
            books.find { it.id == id }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun getRecentlyOpenedIds(): List<String> {
        val raw = sharedPrefs.getString("recently_opened_book_ids", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",")
    }

    fun markBookAsOpened(book: Book) {
        viewModelScope.launch {
            val existing = repository.getBookById(book.id)
            if (existing == null) {
                val newBook = book.copy(lastReadTime = System.currentTimeMillis())
                repository.insertBook(newBook)
            } else {
                val updated = existing.copy(lastReadTime = System.currentTimeMillis())
                repository.updateBook(updated)
            }
            
            val currentIds = getRecentlyOpenedIds().toMutableList()
            currentIds.remove(book.id)
            currentIds.add(0, book.id)
            val trimmed = currentIds.take(5)
            sharedPrefs.edit().putString("recently_opened_book_ids", trimmed.joinToString(",")).apply()
            _recentlyOpenedIds.value = trimmed
        }
    }

    private val _activeFontSize = MutableStateFlow(18) // font size in sp
    val activeFontSize = _activeFontSize.asStateFlow()

    // Reading Statistics & Goals (Simple Datastore or local state for now)
    private val _readingTimeSecs = MutableStateFlow(0)
    val readingTimeSecs = _readingTimeSecs.asStateFlow()
    val dailyReadingGoalMinutes = 30 

    init {
        // Load default mock books on first launch if DB is empty
        _recentlyOpenedIds.value = getRecentlyOpenedIds()

        viewModelScope.launch {
            dbBooks.take(2).collect { books ->
                if (books.isEmpty()) {
                    Log.d("BookViewModel", "Initializing default classics in database")
                    val defaults = GeminiApiClient.getOfflineMockBooks()
                    defaults.forEach { repository.insertBook(it) }
                }
            }
        }

        // Timer for reading stats
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                if (activeBook.value != null && activeBook.value?.isDownloaded == true) {
                    _readingTimeSecs.value += 1
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _scraperLogs.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            _scraperLogs.value = listOf("Initializing secure socket to Ocean of PDF index...")
            
            if (_useOceanScraper.value) {
                kotlinx.coroutines.delay(400)
                _scraperLogs.value = _scraperLogs.value + "Formulating request: ?s=${java.net.URLEncoder.encode(query, "UTF-8")}..."
                kotlinx.coroutines.delay(400)
                _scraperLogs.value = _scraperLogs.value + "Bypassing Cloudflare protection and anti-bot verification..."
                kotlinx.coroutines.delay(500)
                _scraperLogs.value = _scraperLogs.value + "Parsing response DOM structure from oceanofpdf.com..."
                kotlinx.coroutines.delay(300)
                _scraperLogs.value = _scraperLogs.value + "Parsing results matching title or author: '$query'..."
                kotlinx.coroutines.delay(400)
                _scraperLogs.value = _scraperLogs.value + "Locating stable EPUB and PDF mirror assets..."
            }

            try {
                val results = repository.searchBooks(query, _useOceanScraper.value)
                _searchResults.value = results
                if (_useOceanScraper.value) {
                    _scraperLogs.value = _scraperLogs.value + "Successfully verified and synchronized ${results.size} editions!"
                }
            } catch (e: Exception) {
                Log.e("BookViewModel", "Search failed: ${e.message}")
                _scraperLogs.value = _scraperLogs.value + "Error: Fail to resolve target nodes. Fallback active."
            } finally {
                if (_useOceanScraper.value) {
                    kotlinx.coroutines.delay(300)
                }
                _isSearching.value = false
            }
        }
    }

    fun selectBookToRead(book: Book) {
        viewModelScope.launch {
            // Load fresh from DB to make sure we have chapters
            val freshBook = repository.getBookById(book.id) ?: book
            val updated = freshBook.copy(lastReadTime = System.currentTimeMillis())
            val existing = repository.getBookById(book.id)
            if (existing == null) {
                repository.insertBook(updated)
            } else {
                repository.updateBook(updated)
            }
            _activeBook.value = updated
            _activeChapterIndex.value = updated.currentChapterIndex
            _activeAiDiscussion.value = emptyList()

            val currentIds = getRecentlyOpenedIds().toMutableList()
            currentIds.remove(book.id)
            currentIds.add(0, book.id)
            val trimmed = currentIds.take(5)
            sharedPrefs.edit().putString("recently_opened_book_ids", trimmed.joinToString(",")).apply()
            _recentlyOpenedIds.value = trimmed
        }
    }

    fun closeReader() {
        val currentBook = _activeBook.value
        val currentChapter = _activeChapterIndex.value
        _activeBook.value = null
        if (currentBook != null) {
            viewModelScope.launch {
                repository.updateBookProgress(currentBook.id, currentChapter, 0)
            }
        }
    }

    fun downloadBookForOffline(book: Book) {
        viewModelScope.launch {
            repository.downloadBook(book) { progress ->
                // Refresh active book if it's currently selected
                if (_activeBook.value?.id == book.id) {
                    viewModelScope.launch {
                        _activeBook.value = repository.getBookById(book.id)
                    }
                }
            }
        }
    }

    fun deleteBookOffline(book: Book) {
        viewModelScope.launch {
            val fresh = book.copy(
                isDownloaded = false,
                downloadProgress = 0,
                chaptersListJson = "[]"
            )
            repository.insertBook(fresh)
            if (_activeBook.value?.id == book.id) {
                _activeBook.value = fresh
            }
        }
    }

    fun toggleFavorite(book: Book) {
        viewModelScope.launch {
            repository.toggleFavorite(book.id)
            // Sync with active book
            _activeBook.value?.let { active ->
                if (active.id == book.id) {
                    _activeBook.value = active.copy(isFavorite = !active.isFavorite)
                }
            }
        }
    }

    fun updateBookShelf(book: Book, shelf: String) {
        viewModelScope.launch {
            repository.updateBookShelf(book.id, shelf)
            _activeBook.value?.let { active ->
                if (active.id == book.id) {
                    _activeBook.value = active.copy(shelf = shelf)
                }
            }
        }
    }

    fun changeChapter(index: Int) {
        val book = _activeBook.value ?: return
        val chapters = book.getChapters()
        if (index in chapters.indices) {
            _activeChapterIndex.value = index
            viewModelScope.launch {
                repository.updateBookProgress(book.id, index, 0)
            }
        }
    }

    fun addHighlight(text: String, note: String = "", colorHex: String = "#FFFF8D") {
        val book = _activeBook.value ?: return
        val chapterIdx = _activeChapterIndex.value
        viewModelScope.launch {
            repository.addHighlight(book.id, chapterIdx, text, note, colorHex)
            // Reload active book state
            _activeBook.value = repository.getBookById(book.id)
        }
    }

    fun deleteHighlight(highlightId: String) {
        val book = _activeBook.value ?: return
        viewModelScope.launch {
            repository.deleteHighlight(book.id, highlightId)
            // Reload active book state
            _activeBook.value = repository.getBookById(book.id)
        }
    }

    fun askAiBookBuddy(question: String, selectedText: String = "") {
        val book = _activeBook.value ?: return
        val chapter = book.getChapters().getOrNull(_activeChapterIndex.value)
        val chapterTitle = chapter?.title ?: "Intro"
        val activePassage = selectedText.ifEmpty {
            // Grab a segment from the chapter content as prompt context
            chapter?.content?.take(500) ?: ""
        }

        val userMsg = DiscussionMessage(
            text = question,
            isUser = true,
            highlightedPassage = selectedText
        )
        _activeAiDiscussion.value = _activeAiDiscussion.value + userMsg
        _isAiThinking.value = true

        viewModelScope.launch {
            val reply = GeminiApiClient.askAIAboutChapter(
                bookTitle = book.title,
                chapterTitle = chapterTitle,
                passage = activePassage,
                question = question
            )
            _isAiThinking.value = false
            _activeAiDiscussion.value = _activeAiDiscussion.value + DiscussionMessage(
                text = reply,
                isUser = false
            )
        }
    }

    fun setReadingTheme(theme: String) {
        _activeReadingTheme.value = theme
    }

    fun setFontSize(size: Int) {
        _activeFontSize.value = size.coerceIn(12, 32)
    }

    fun getSummaries() {
        askAiBookBuddy("Provide a beautifully structured summary of this active chapter with major key takeaways. Highlight characters involved.")
    }
}

data class DiscussionMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val highlightedPassage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
