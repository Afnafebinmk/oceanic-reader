package com.example.data

import androidx.room.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val genre: String,
    val description: String,
    var shelf: String = "To Read", // "Currently Reading", "Favorites", "To Read", "Completed"
    var isDownloaded: Boolean = false,
    var downloadProgress: Int = 0,
    var currentPage: Int = 0,
    var currentChapterIndex: Int = 0,
    var totalPages: Int = 0,
    var chaptersListJson: String = "[]",
    var highlightsJson: String = "[]",
    var tagsJson: String = "[]",
    var isFavorite: Boolean = false,
    var addedTime: Long = System.currentTimeMillis(),
    var lastReadTime: Long = System.currentTimeMillis()
) {
    @Ignore
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    @Ignore
    fun getChapters(): List<BookChapter> {
        val type = Types.newParameterizedType(List::class.java, BookChapter::class.java)
        return try {
            moshi.adapter<List<BookChapter>>(type).fromJson(chaptersListJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Ignore
    fun setChapters(chapters: List<BookChapter>) {
        val type = Types.newParameterizedType(List::class.java, BookChapter::class.java)
        chaptersListJson = moshi.adapter<List<BookChapter>>(type).toJson(chapters)
    }

    @Ignore
    fun getHighlights(): List<BookHighlight> {
        val type = Types.newParameterizedType(List::class.java, BookHighlight::class.java)
        return try {
            moshi.adapter<List<BookHighlight>>(type).fromJson(highlightsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Ignore
    fun setHighlights(highlights: List<BookHighlight>) {
        val type = Types.newParameterizedType(List::class.java, BookHighlight::class.java)
        highlightsJson = moshi.adapter<List<BookHighlight>>(type).toJson(highlights)
    }

    @Ignore
    fun getTags(): List<String> {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        return try {
            moshi.adapter<List<String>>(type).fromJson(tagsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Ignore
    fun setTags(tags: List<String>) {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        tagsJson = moshi.adapter<List<String>>(type).toJson(tags)
    }
}

data class BookChapter(
    val title: String,
    val content: String,
    val pageCount: Int = 1
)

data class BookHighlight(
    val id: String,
    val chapterIndex: Int,
    val text: String,
    val note: String = "",
    val colorHex: String = "#FFFF8D", // Default yellow highlight
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastReadTime DESC")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: String): Book?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book)

    @Update
    suspend fun updateBook(book: Book)

    @Delete
    suspend fun deleteBook(book: Book)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: String)
}

@Database(entities = [Book::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
}
