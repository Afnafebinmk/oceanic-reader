package com.example.ui.books

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    viewModel: BookViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activeBook by viewModel.activeBook.collectAsState()
    val pdfFile by viewModel.currentPdfFile.collectAsState()
    val isGenerating by viewModel.isGeneratingPdf.collectAsState()

    val localPdfFile = pdfFile
    val coroutineScope = rememberCoroutineScope()

    var currentPageIndex by remember { mutableStateOf(0) }
    var filterMode by remember { mutableStateOf("Light") } // Light, Sepia, Night
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Prepare PDF if not yet generated or loaded
    LaunchedEffect(activeBook) {
        activeBook?.let { book ->
            viewModel.preparePdfForBook(book) {
                // Restore current reading page index if applicable, default to 0 (cover)
                currentPageIndex = book.currentPage.coerceAtLeast(0)
            }
        }
    }

    val book = activeBook ?: return

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val bookmarkedPages = remember(book.highlightsJson) {
        book.getHighlights()
            .filter { it.chapterIndex == -100 && it.note == "BOOKMARK" }
            .mapNotNull { it.text.replace("Page ", "").toIntOrNull() }
            .sorted()
    }
    val isCurrentPageBookmarked = currentPageIndex in bookmarkedPages

    // Setup native Android PdfRenderer
    val renderer = remember(localPdfFile) {
        if (localPdfFile != null && localPdfFile.exists()) {
            try {
                val input = ParcelFileDescriptor.open(localPdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                PdfRenderer(input)
            } catch (e: Exception) {
                Log.e("PdfReaderScreen", "Failed to open PDF renderer: ${e.message}")
                null
            }
        } else null
    }

    val totalPages = renderer?.pageCount ?: 0

    // Save page progression on close or index change
    LaunchedEffect(currentPageIndex) {
        if (currentPageIndex in 0 until totalPages) {
            viewModel.updatePdfPage(book, currentPageIndex)
        }
    }

    // Load and render page bitmap in a background Thread
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember { mutableStateOf(false) }

    LaunchedEffect(renderer, currentPageIndex) {
        if (renderer != null && currentPageIndex in 0 until totalPages) {
            isLoadingPage = true
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val page = renderer.openPage(currentPageIndex)
                    // Scale factor for rendering Crisp/HD pages
                    val scaleFactor = 2.0f
                    val w = (page.width * scaleFactor).toInt()
                    val h = (page.height * scaleFactor).toInt()
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    bmp
                } catch (e: Exception) {
                    Log.e("PdfReaderScreen", "Error rendering bitmap: ${e.message}")
                    null
                }
            }
            pageBitmap = bitmap
            isLoadingPage = false
        }
    }

    // Define background and container colors depending on Comfort Filters
    val (backgroundColor, panelColor, textColor) = remember(filterMode) {
        when (filterMode) {
            "Sepia" -> Triple(Color(0xFFF4EDDB), Color(0xFFEAE0C8), Color(0xFF5B4031))
            "Night" -> Triple(Color(0xFF14171E), Color(0xFF1E232E), Color(0xFFD3D7E3))
            else -> Triple(Color(0xFFFFFAFA), Color(0xFFF5F5F5), Color(0xFF2C3E50))
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = panelColor,
                modifier = Modifier.width(320.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Bookmarks, contentDescription = null, tint = textColor)
                            Text(
                                text = "Pinned Pages",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                        IconButton(onClick = { coroutineScope.launch { drawerState.close() } }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close bookmarks list", tint = textColor)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp), color = textColor.copy(alpha = 0.1f))

                    if (bookmarkedPages.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = textColor.copy(alpha = 0.3f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "No pinned pages yet.\nTap the Bookmark button in the toolbar to save specific pages for quick access.",
                                    fontSize = 13.sp,
                                    color = textColor.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(bookmarkedPages) { pageIdx ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(backgroundColor.copy(alpha = 0.6f))
                                        .clickable {
                                            currentPageIndex = pageIdx
                                            scale = 1f
                                            offset = Offset.Zero
                                            coroutineScope.launch { drawerState.close() }
                                        }
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                if (currentPageIndex == pageIdx) textColor.copy(alpha = 0.4f) else Color.Transparent
                                            ),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = null,
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Page ${pageIdx + 1}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                            Text(
                                                text = "Tap to jump",
                                                fontSize = 11.sp,
                                                color = textColor.copy(alpha = 0.5f)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.toggleBookmarkedPage(book, pageIdx)
                                        },
                                        modifier = Modifier.size(28.dp).testTag("pdf_unpin_${pageIdx}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove bookmark",
                                            tint = textColor.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = book.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                maxLines = 1,
                                color = textColor
                            )
                            Text(
                                text = "Manuscript PDF Edition",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor.copy(alpha = 0.6f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                        }
                    },
                    actions = {
                        Row(
                            modifier = Modifier.padding(end = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pin/Bookmark Page Toggle
                            IconButton(
                                onClick = { viewModel.toggleBookmarkedPage(book, currentPageIndex) },
                                modifier = Modifier.size(32.dp).testTag("pdf_toggle_bookmark")
                            ) {
                                Icon(
                                    imageVector = if (isCurrentPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = if (isCurrentPageBookmarked) "Unpin Page" else "Pin Page",
                                    tint = if (isCurrentPageBookmarked) Color(0xFFFF5252) else textColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Open Side Drawer
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        drawerState.open()
                                    }
                                },
                                modifier = Modifier.size(32.dp).testTag("pdf_bookmarks_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmarks,
                                    contentDescription = "View Bookmarks list",
                                    tint = textColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            listOf("Light", "Sepia", "Night").forEach { filter ->
                                val isSelected = filter == filterMode
                                IconButton(
                                    onClick = { filterMode = filter },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            if (isSelected) textColor.copy(alpha = 0.15f) else Color.Transparent,
                                            CircleShape
                                        )
                                        .testTag("pdf_filter_${filter.lowercase()}")
                                ) {
                                    val iconColor = if (isSelected) textColor else textColor.copy(alpha = 0.5f)
                                    when (filter) {
                                        "Light" -> Icon(imageVector = Icons.Default.LightMode, contentDescription = "Day Filter", modifier = Modifier.size(16.dp), tint = iconColor)
                                        "Sepia" -> Icon(imageVector = Icons.Default.FilterVintage, contentDescription = "Sepia Filter", modifier = Modifier.size(16.dp), tint = iconColor)
                                        "Night" -> Icon(imageVector = Icons.Default.NightsStay, contentDescription = "Night Filter", modifier = Modifier.size(16.dp), tint = iconColor)
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = panelColor)
                )
            },
        bottomBar = {
            Surface(
                color = panelColor,
                tonalElevation = 4.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (currentPageIndex > 0) {
                                    currentPageIndex--
                                    scale = 1f
                                    offset = Offset.Zero
                                }
                            },
                            enabled = currentPageIndex > 0,
                            modifier = Modifier.testTag("pdf_prev_page")
                        ) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Prev Page", tint = if (currentPageIndex > 0) textColor else textColor.copy(alpha = 0.2f))
                        }

                        Text(
                            text = "Page ${currentPageIndex + 1} of ${totalPages.coerceAtLeast(1)}",
                            fontFamily = FontFamily.Serif,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.testTag("pdf_page_label")
                        )

                        IconButton(
                            onClick = {
                                if (currentPageIndex < totalPages - 1) {
                                    currentPageIndex++
                                    scale = 1f
                                    offset = Offset.Zero
                                }
                            },
                            enabled = currentPageIndex < totalPages - 1,
                            modifier = Modifier.testTag("pdf_next_page")
                        ) {
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Page", tint = if (currentPageIndex < totalPages - 1) textColor else textColor.copy(alpha = 0.2f))
                        }
                    }

                    if (totalPages > 1) {
                        Slider(
                            value = currentPageIndex.toFloat(),
                            onValueChange = {
                                currentPageIndex = it.toInt().coerceIn(0, totalPages - 1)
                                scale = 1f
                                offset = Offset.Zero
                            },
                            valueRange = 0f..(totalPages - 1).toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .testTag("pdf_page_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = textColor,
                                activeTrackColor = textColor,
                                inactiveTrackColor = textColor.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (isGenerating || pdfFile == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    CircularProgressIndicator(color = textColor, modifier = Modifier.size(52.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Typesetting high-fidelity layout details...",
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Compiling chapter boundaries, cover plates and typography grids into PDF format on-device.",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.widthIn(max = 280.dp)
                    )
                }
            } else if (isLoadingPage || pageBitmap == null) {
                CircularProgressIndicator(color = textColor)
            } else {
                // Interactive Zoom & Pan Area for PDF HD Page
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .background(
                            if (filterMode == "Night") Color(0xFF0F1115) else Color(0x10000000),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            BorderStroke(1.dp, textColor.copy(alpha = 0.12f)),
                            RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val colorFilter = remember(filterMode) {
                        when (filterMode) {
                            "Sepia" -> {
                                val sepiaMatrix = ColorMatrix(floatArrayOf(
                                    0.393f, 0.769f, 0.189f, 0f, 0f,
                                    0.349f, 0.686f, 0.168f, 0f, 0f,
                                    0.272f, 0.534f, 0.131f, 0f, 0f,
                                    0f, 0f, 0f, 1f, 0f
                                ))
                                ColorFilter.colorMatrix(sepiaMatrix)
                            }
                            "Night" -> {
                                // Full inversion matrix for comfortable reading in complete darkness
                                val invertMatrix = ColorMatrix(floatArrayOf(
                                    -1.0f, 0f, 0f, 0f, 255f,
                                    0f, -1.0f, 0f, 0f, 255f,
                                    0f, 0f, -1.0f, 0f, 255f,
                                    0f, 0f, 0f, 1f, 0f
                                ))
                                ColorFilter.colorMatrix(invertMatrix)
                            }
                            else -> null
                        }
                    }

                    Image(
                        bitmap = pageBitmap!!.asImageBitmap(),
                        contentDescription = "PDF Page ${currentPageIndex + 1}",
                        contentScale = ContentScale.Fit,
                        colorFilter = colorFilter,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (scale > 1f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.5f
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 6f)
                                    if (scale <= 1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        offset = Offset(offset.x + pan.x, offset.y + pan.y)
                                    }
                                }
                            }
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                            .testTag("pdf_rendered_page")
                    )
                }
            }
        }
    }
}
}
