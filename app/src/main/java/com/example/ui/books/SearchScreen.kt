package com.example.ui.books

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Book
import com.example.data.api.GeminiApiClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: BookViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBookDetail: (Book) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val downloadingState by viewModel.downloadingState.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Oceanic Catalog",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        val useOceanScraper by viewModel.useOceanScraper.collectAsState()
        val scraperLogs by viewModel.scraperLogs.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Live Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_text_input"),
                placeholder = { Text("Search title or author...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { keyboardController?.hide() }
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Ocean of PDF Scraper Connection Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("ocean_scraper_config_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ocean of PDF Live Index Scraper",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Scrape and resolve titles, authors, cover metadata and e-text from oceanofpdf.com catalogs.",
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = useOceanScraper,
                        onCheckedChange = { viewModel.setUseOceanScraper(it) },
                        modifier = Modifier.testTag("ocean_scraper_toggle")
                    )
                }
            }

            // Online indicator state notice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (useOceanScraper) "● Live Ocean Scraper Connected" else if (GeminiApiClient.isConfigured) "● Connected to Oceanic API" else "● Offline Classic Mode active",
                    color = if (useOceanScraper || GeminiApiClient.isConfigured) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Search Content / Grid
            if (isSearching) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (useOceanScraper) "Connecting to oceanofpdf.com..." else "Querying Oceanic manuscripts...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold
                        )

                        if (useOceanScraper && scraperLogs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            // Terminal style logging box
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 220.dp)
                                    .testTag("ocean_scraper_console"),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF15181E)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color(0xFF333F53)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                                ) {
                                    Text(
                                        text = "OCEANOFPDF-SCRAPER>_ v2.1-STABLE",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00FFCC),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    scraperLogs.forEach { log ->
                                        Text(
                                            text = ">>> $log",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = if (log.contains("Error", ignoreCase = true)) Color(0xFFFF5252) else if (log.contains("Successfully", ignoreCase = true) || log.contains("synchronized", ignoreCase = true)) Color(0xFF00FFAC) else Color(0xFFD4D7DF),
                                            lineHeight = 14.sp,
                                            modifier = Modifier.padding(vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (searchResults.isEmpty()) {
                // Empty state or recommendations
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
                            imageVector = Icons.Default.HistoryEdu,
                            contentDescription = "Search placeholder",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "Explore Ocean of PDF" else "No matching volumes found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) {
                                if (useOceanScraper) {
                                    "Live scraping mode active. Enter any book title or author to crawl, parse cover details and index full matches from the e-text repository."
                                } else {
                                    "Enter keywords above to search for classic literature, epic novels, philosophy, or sci-fi masterwork volumes."
                                }
                            } else {
                                "Try adjusting your spelling, or disable the Live Index Scraper toggle to look for locally embedded publications."
                            },
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 155.dp),
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(searchResults, key = { it.id }) { book ->
                        BookCoverItem(
                            book = book,
                            loadingProgress = downloadingState[book.id],
                            onClick = {
                                keyboardController?.hide()
                                onNavigateToBookDetail(book)
                            },
                            modifier = Modifier.testTag("search_card_${book.id}")
                        )
                    }
                }
            }
        }
    }
}
