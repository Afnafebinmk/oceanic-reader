package com.example.ui.books

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.shadow
import coil.compose.AsyncImage
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Book
import com.example.data.api.GeminiApiClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfScreen(
    viewModel: BookViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToBookDetail: (Book) -> Unit
) {
    val books by viewModel.dbBooks.collectAsState()
    val downloadingState by viewModel.downloadingState.collectAsState()
    val readingTimeSecs by viewModel.readingTimeSecs.collectAsState()
    val recentlyOpenedBooks by viewModel.recentlyOpenedBooks.collectAsState()

    var selectedShelfFilter by remember { mutableStateOf("All") }
    val shelfFilters = listOf("All", "Currently Reading", "Want to Read", "Completed", "Favorites", "Offline")

    var selectedGenreFilter by remember { mutableStateOf("All") }
    var selectedTagFilter by remember { mutableStateOf("All") }

    val allGenres = remember(books) {
        listOf("All") + books.map { it.genre }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val allTags = remember(books) {
        listOf("All") + books.flatMap { it.getTags() }.filter { it.isNotBlank() }.distinct().sorted()
    }

    // Reset sub-filters if they no longer exist in current books
    LaunchedEffect(books) {
        if (selectedGenreFilter != "All" && !books.map { it.genre }.contains(selectedGenreFilter)) {
            selectedGenreFilter = "All"
        }
        if (selectedTagFilter != "All" && !books.flatMap { it.getTags() }.contains(selectedTagFilter)) {
            selectedTagFilter = "All"
        }
    }

    val filteredBooks = remember(books, selectedShelfFilter, selectedGenreFilter, selectedTagFilter) {
        val baseBooks = when (selectedShelfFilter) {
            "All" -> books
            "Favorites" -> books.filter { it.isFavorite }
            "Offline" -> books.filter { it.isDownloaded }
            else -> books.filter { it.shelf == selectedShelfFilter }
        }
        baseBooks.filter { book ->
            val matchGenre = selectedGenreFilter == "All" || book.genre.equals(selectedGenreFilter, ignoreCase = true)
            val matchTag = selectedTagFilter == "All" || book.getTags().any { it.equals(selectedTagFilter, ignoreCase = true) }
            matchGenre && matchTag
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp).padding(end = 6.dp)
                        )
                        Text(
                            text = "Oceanic Reader",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Global Theme Toggle
                        var showThemeMenu by remember { mutableStateOf(false) }
                        val isDarkThemeGlobal by viewModel.isDarkThemeGlobal.collectAsState()

                        Box {
                            IconButton(
                                onClick = { showThemeMenu = true },
                                modifier = Modifier
                                    .testTag("theme_toggle_button")
                                    .background(
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                val currentIcon = when (isDarkThemeGlobal) {
                                    true -> Icons.Default.NightsStay
                                    false -> Icons.Default.LightMode
                                    null -> Icons.Default.Settings
                                }
                                val iconDesc = when (isDarkThemeGlobal) {
                                    true -> "Dark Theme Active"
                                    false -> "Light Theme Active"
                                    null -> "System Theme Active"
                                }
                                Icon(
                                    imageVector = currentIcon,
                                    contentDescription = iconDesc,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }

                            DropdownMenu(
                                expanded = showThemeMenu,
                                onDismissRequest = { showThemeMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Light Mode") },
                                    leadingIcon = { Icon(Icons.Default.LightMode, contentDescription = null) },
                                    onClick = {
                                        viewModel.setDarkThemeGlobal(false)
                                        showThemeMenu = false
                                    },
                                    modifier = Modifier.testTag("theme_option_light")
                                )
                                DropdownMenuItem(
                                    text = { Text("Dark Mode") },
                                    leadingIcon = { Icon(Icons.Default.NightsStay, contentDescription = null) },
                                    onClick = {
                                        viewModel.setDarkThemeGlobal(true)
                                        showThemeMenu = false
                                    },
                                    modifier = Modifier.testTag("theme_option_dark")
                                )
                                DropdownMenuItem(
                                    text = { Text("System Default") },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    onClick = {
                                        viewModel.setDarkThemeGlobal(null)
                                        showThemeMenu = false
                                    },
                                    modifier = Modifier.testTag("theme_option_system")
                                )
                            }
                        }

                        IconButton(
                            onClick = onNavigateToSearch,
                            modifier = Modifier
                                .testTag("search_entrance_button")
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search books",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Secret Key Setup Notice
            if (!GeminiApiClient.isConfigured) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pro Tip: Set GEMINI_API_KEY in the Secrets panel to unlock live book searching and downloads for ANY book in the world!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Daily Reading Stat Header Board
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("reading_goal_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Reading Goal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val elapsedMinutes = readingTimeSecs / 60
                        val elapsedSeconds = readingTimeSecs % 60
                        Text(
                            text = String.format("%02d:%02d of %d mins", elapsedMinutes, elapsedSeconds, viewModel.dailyReadingGoalMinutes),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Keep reading to increase focus!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.61f)
                        )
                    }
                    
                    Box(contentAlignment = Alignment.Center) {
                        val progressNormalized = (readingTimeSecs.toFloat() / (viewModel.dailyReadingGoalMinutes * 60f)).coerceIn(0f, 1f)
                        CircularProgressIndicator(
                            progress = { progressNormalized },
                            modifier = Modifier.size(54.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 5.dp,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Recently Read Section
            if (recentlyOpenedBooks.isNotEmpty()) {
                Text(
                    text = "Recently Read",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recently_read_row")
                ) {
                    items(recentlyOpenedBooks, key = { "recent_${it.id}" }) { book ->
                        Card(
                            onClick = { onNavigateToBookDetail(book) },
                            modifier = Modifier
                                .width(110.dp)
                                .testTag("recently_read_book_${book.id}"),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(145.dp)
                                        .shadow(4.dp, RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                ) {
                                    if (book.coverUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = book.coverUrl,
                                            contentDescription = "Cover for ${book.title}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MenuBook,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }

                                    // Spines & decorative overlay
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(4.dp)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color.Black.copy(0.3f), Color.White.copy(0.15f), Color.Transparent)
                                                )
                                            )
                                            .align(Alignment.CenterStart)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = book.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontFamily = FontFamily.Serif
                                )
                                Text(
                                    text = book.author,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.61f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Bookshelves Horizontal Filter row
            Text(
                text = "Bookshelves",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.onBackground
            )

            ScrollableTabRow(
                selectedTabIndex = shelfFilters.indexOf(selectedShelfFilter).coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {},
                indicator = {}
            ) {
                shelfFilters.forEach { filter ->
                    val isSelected = filter == selectedShelfFilter
                    val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp, bottom = 8.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(bg)
                            .clickable { selectedShelfFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("shelf_filter_${filter.lowercase().replace(" ", "_")}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter,
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Genre & Custom Tags Sub-Filtering Rows
            if (allGenres.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Filter by Genre / Topic",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                    if (selectedGenreFilter != "All") {
                        Text(
                            text = "Clear",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { selectedGenreFilter = "All" }
                        )
                    }
                }

                ScrollableTabRow(
                    selectedTabIndex = allGenres.indexOf(selectedGenreFilter).coerceAtLeast(0),
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {}
                ) {
                    allGenres.forEach { genre ->
                        val isSelected = genre == selectedGenreFilter
                        val bg = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        val textColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp, bottom = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .clickable { selectedGenreFilter = genre }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("genre_filter_${genre.lowercase().replace(" ", "_")}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = genre,
                                color = textColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            if (allTags.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Filter by Custom Tag",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                    if (selectedTagFilter != "All") {
                        Text(
                            text = "Clear",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { selectedTagFilter = "All" }
                        )
                    }
                }

                ScrollableTabRow(
                    selectedTabIndex = allTags.indexOf(selectedTagFilter).coerceAtLeast(0),
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {}
                ) {
                    allTags.forEach { tag ->
                        val isSelected = tag == selectedTagFilter
                        val bg = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        val textColor = if (isSelected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp, bottom = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .clickable { selectedTagFilter = tag }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("tag_filter_${tag.lowercase().replace(" ", "_")}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#$tag",
                                color = textColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Book Shelf Grid
            if (filteredBooks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LayersClear,
                            contentDescription = "Empty Shelf",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Shelf is empty",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedShelfFilter == "Offline") 
                                "Search for classic titles or modern works and download them for immersive offline reading."
                                else "Move downloaded books here or tap the search icon to add more books.",
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToSearch,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("explore_books_button")
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Find Books to Read")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredBooks, key = { it.id }) { book ->
                        BookCoverItem(
                            book = book,
                            loadingProgress = downloadingState[book.id],
                            onClick = { onNavigateToBookDetail(book) },
                            modifier = Modifier.testTag("book_card_${book.id}")
                        )
                    }
                }
            }
        }
    }
}
