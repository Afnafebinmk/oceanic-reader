package com.example.ui.books

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Book

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    viewModel: BookViewModel,
    bookId: String,
    onNavigateBack: () -> Unit,
    onNavigateToReader: () -> Unit,
    onNavigateToPdfReader: () -> Unit
) {
    val books by viewModel.dbBooks.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val downloadingState by viewModel.downloadingState.collectAsState()
    val useOceanScraper by viewModel.useOceanScraper.collectAsState()

    // Find book matching this ID
    val book = remember(books, searchResults, bookId) {
        books.find { it.id == bookId } ?: searchResults.find { it.id == bookId }
    }

    if (book == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Volume not found")
        }
        return
    }

    val isDownloaded = book.isDownloaded
    val downloadProgress = downloadingState[book.id]
    var showShelfMenu by remember { mutableStateOf(false) }

    LaunchedEffect(bookId) {
        viewModel.markBookAsOpened(book)
    }

    val shelves = listOf("To Read", "Currently Reading", "Completed")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Volume Details", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleFavorite(book) },
                        modifier = Modifier.testTag("favorite_button")
                    ) {
                        Icon(
                            imageVector = if (book.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite toggle",
                            tint = if (book.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Book Header Aspect
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Styled Card Book jacket
                    Box(
                        modifier = Modifier
                            .size(130.dp, 190.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (book.coverUrl.isNotEmpty()) {
                            AsyncImage(
                                model = book.coverUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(5.dp)
                                .background(Color.Black.copy(0.3f))
                                .align(Alignment.CenterStart)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.height(190.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = book.title,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "By ${book.author}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = book.genre,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Shelf Organization Controls
                        Box {
                            Button(
                                onClick = { showShelfMenu = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("shelf_organization_dropdown")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shelf: ${book.shelf}", fontSize = 12.sp)
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showShelfMenu,
                                onDismissRequest = { showShelfMenu = false }
                            ) {
                                shelves.forEach { shelf ->
                                    DropdownMenuItem(
                                        text = { Text(shelf) },
                                        onClick = {
                                            viewModel.updateBookShelf(book, shelf)
                                            showShelfMenu = false
                                        },
                                        modifier = Modifier.testTag("dropdown_item_${shelf.lowercase().replace(" ", "_")}")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Stats Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isDownloaded) "${book.getChapters().size}" else "N/A",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(text = "Downloaded Chapters", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isDownloaded) "Offline Safe" else "Cloud Stream",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDownloaded) Color(0xFF4CAF50) else Color(0xFFFF9800)
                        )
                        Text(text = "Reading Source", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Primary Downloads & Reading Action Bar
            item {
                Spacer(modifier = Modifier.height(10.dp))
                if (downloadProgress != null && downloadProgress < 100) {
                    // Downloading State Progress Meter
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (useOceanScraper) "Scraping mirror from oceanofpdf.com..." else "Downloading manuscript chapters...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("$downloadProgress%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress.toFloat() / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isDownloaded) {
                            Button(
                                onClick = { viewModel.downloadBookForOffline(book) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("download_offline_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download Offline", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Read Now (Standard reflowable text layout with chatbot)
                            Button(
                                onClick = {
                                    viewModel.selectBookToRead(book)
                                    onNavigateToReader()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("read_now_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(imageVector = Icons.Default.ChromeReaderMode, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("EPUB Flow", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Read as PDF Layout (typesetting-perfect layout reader)
                            Button(
                                onClick = {
                                    viewModel.selectBookToRead(book)
                                    onNavigateToPdfReader()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("read_pdf_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF Page", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Delete offline files button
                            IconButton(
                                onClick = { viewModel.deleteBookOffline(book) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .testTag("delete_offline_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Delete Offline Download",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Book Description Synopsis
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Description",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = book.description,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Library Tagging & Custom Categorization Section
            item {
                var newTagText by remember { mutableStateOf("") }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("tags_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Label,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Library Tagging",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Serif
                            )
                        }
                        
                        Text(
                            text = "Add custom topics or labels to organize and multi-filter this book on your library bookshelf.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                        )

                        // Current tags flow row
                        val currentTags = book.getTags()
                        if (currentTags.isEmpty()) {
                            Text(
                                text = "No custom tags assigned yet. Type a tag below starting with or without #.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                currentTags.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "#$tag",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove tag $tag",
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clickable { viewModel.removeBookTag(book, tag) }
                                                    .testTag("remove_tag_icon_$tag")
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Add new tag input box
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newTagText,
                                onValueChange = { newTagText = it },
                                placeholder = { Text("e.g. history, philosophy, sci-fi", fontSize = 12.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("new_tag_input_field"),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                            )

                            Button(
                                onClick = {
                                    val cleaned = newTagText.trim().replace("#", "")
                                    if (cleaned.isNotEmpty()) {
                                        viewModel.addBookTag(book, cleaned)
                                        newTagText = ""
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("add_tag_submit_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add custom tag", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Chapters vertical indices
            if (isDownloaded) {
                val chapters = book.getChapters()
                if (chapters.isNotEmpty()) {
                    item {
                        Text(
                            text = "Downloaded Chapters (${chapters.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    itemsIndexed(chapters) { index, chapter ->
                        Card(
                            onClick = {
                                viewModel.selectBookToRead(book)
                                viewModel.changeChapter(index)
                                onNavigateToReader()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = chapter.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(15.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Offline Chapters Locked",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Tap 'Download Offline' at the top to compile this book's chapter manuscripts and save them to local storage.",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
