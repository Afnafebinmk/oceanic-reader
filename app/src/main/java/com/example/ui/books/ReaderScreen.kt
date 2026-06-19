package com.example.ui.books

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookChapter
import com.example.data.BookHighlight
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: BookViewModel,
    onNavigateBack: () -> Unit
) {
    val activeBook by viewModel.activeBook.collectAsState()
    val activeChapterIndex by viewModel.activeChapterIndex.collectAsState()
    val readingTheme by viewModel.activeReadingTheme.collectAsState()
    val fontSizeScale by viewModel.activeFontSize.collectAsState()
    val aiDiscussion by viewModel.activeAiDiscussion.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val book = activeBook ?: return
    val chapters = book.getChapters()
    val activeChapter = chapters.getOrNull(activeChapterIndex)

    // Layout configuration depending on selected theme
    val (backgroundColor, textColor) = remember(readingTheme) {
        when (readingTheme) {
            "Parchment" -> ReadingThemeParchmentBg to ReadingThemeParchmentText
            "Sepia" -> ReadingThemeSepiaBg to ReadingThemeSepiaText
            "Dark" -> ReadingThemeDarkBg to ReadingThemeDarkText
            else -> Color.White to Color.Black
        }
    }

    // Modal view triggers
    var showFontMenu by remember { mutableStateOf(false) }
    var showAiBuddyPanel by remember { mutableStateOf(false) }
    var showHighlightsPanel by remember { mutableStateOf(false) }

    // Active paragraph select for highlight interaction
    var selectedParagraphText by remember { mutableStateOf<String?>(null) }
    var userAiQuestionInput by remember { mutableStateOf("") }

    if (activeChapter == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Chapter manuscript missing.")
        }
        return
    }

    // Break the text up into paragraphs nicely
    val paragraphs = remember(activeChapter) {
        activeChapter.content.split("\n\n").filter { it.isNotBlank() }
    }

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
                            text = activeChapter.title,
                            fontSize = 11.sp,
                            color = textColor.copy(alpha = 0.6f),
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                actions = {
                    // Highlights panel button
                    IconButton(
                        onClick = { showHighlightsPanel = true },
                        modifier = Modifier.testTag("highlights_sidebar_button")
                    ) {
                        Icon(imageVector = Icons.Default.BorderColor, contentDescription = "My Highlights", tint = textColor)
                    }
                    // Font sizes and Theme button
                    IconButton(
                        onClick = { showFontMenu = true },
                        modifier = Modifier.testTag("reader_settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.TextFields, contentDescription = "Reader Style", tint = textColor)
                    }
                    // AI Book Buddy chatbot toggle
                    IconButton(
                        onClick = { 
                            showAiBuddyPanel = !showAiBuddyPanel
                            selectedParagraphText = null // Reset selected highlight
                        },
                        modifier = Modifier.testTag("ai_buddy_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology, 
                            contentDescription = "AI Book Buddy", 
                            tint = if (showAiBuddyPanel) MaterialTheme.colorScheme.primary else textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        bottomBar = {
            // Immersive Reader Chapter Navigator Bar
            Surface(
                color = backgroundColor,
                tonalElevation = 4.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { 
                            if (activeChapterIndex > 0) {
                                viewModel.changeChapter(activeChapterIndex - 1)
                                coroutineScope.launch { listState.scrollToItem(0) }
                            }
                        },
                        enabled = activeChapterIndex > 0,
                        modifier = Modifier.testTag("prev_chapter_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft, 
                            contentDescription = "Previous Chapter",
                            tint = if (activeChapterIndex > 0) textColor else textColor.copy(alpha = 0.2f)
                        )
                    }

                    Text(
                        text = "Chapter ${activeChapterIndex + 1} of ${chapters.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.testTag("chapter_progress_indicator")
                    )

                    IconButton(
                        onClick = { 
                            if (activeChapterIndex < chapters.size - 1) {
                                viewModel.changeChapter(activeChapterIndex + 1)
                                coroutineScope.launch { listState.scrollToItem(0) }
                            }
                        },
                        enabled = activeChapterIndex < chapters.size - 1,
                        modifier = Modifier.testTag("next_chapter_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight, 
                            contentDescription = "Next Chapter",
                            tint = if (activeChapterIndex < chapters.size - 1) textColor else textColor.copy(alpha = 0.2f)
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
                .padding(innerPadding)
        ) {
            // Readable Book Content Workspace
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header spacer / decorative flourish
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Divider(
                            color = textColor.copy(alpha = 0.2f),
                            thickness = 1.dp,
                            modifier = Modifier
                                .width(60.dp)
                                .padding(vertical = 12.dp)
                        )
                        Text(
                            text = activeChapter.title,
                            fontSize = (fontSizeScale + 4).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // Beautiful Klikable paragraph list
                items(paragraphs) { paragraphText ->
                    // See if this paragraph is already highlighted in this active chapter
                    val highlight = book.getHighlights().find { 
                        it.chapterIndex == activeChapterIndex && it.text == paragraphText 
                    }
                    val isSelected = selectedParagraphText == paragraphText

                    val paragraphColor = when {
                        highlight != null -> Color(android.graphics.Color.parseColor(highlight.colorHex)).copy(alpha = 0.25f)
                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else -> Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(paragraphColor, RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                selectedParagraphText = if (isSelected) null else paragraphText
                            }
                            .padding(8.dp)
                            .testTag("paragraph_block")
                    ) {
                        Text(
                            text = paragraphText,
                            fontSize = fontSizeScale.sp,
                            fontFamily = FontFamily.Serif,
                            lineHeight = (fontSizeScale * 1.6f).sp,
                            color = textColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Elegant spacing at end
                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            // Paragraph Contextual Action Overlay Card (shows up on selection!)
            AnimatedVisibility(
                visible = selectedParagraphText != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                val pText = selectedParagraphText ?: ""
                val existingHighlight = book.getHighlights().find { 
                    it.chapterIndex == activeChapterIndex && it.text == pText 
                }

                Card(
                    modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Paragraph Interaction",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { selectedParagraphText = null }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close overlay", modifier = Modifier.size(18.dp))
                            }
                        }

                        Text(
                            text = pText,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            if (existingHighlight == null) {
                                // Highlights trigger
                                Button(
                                    onClick = {
                                        viewModel.addHighlight(pText)
                                        selectedParagraphText = null
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                    modifier = Modifier.testTag("highlight_selection_button")
                                ) {
                                    Icon(imageVector = Icons.Default.BorderColor, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Highlight", fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.deleteHighlight(existingHighlight.id)
                                        selectedParagraphText = null
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                                    modifier = Modifier.testTag("remove_highlight_button")
                                ) {
                                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove Color", fontSize = 12.sp)
                                }
                            }

                            // Ask AI custom interaction button
                            Button(
                                onClick = {
                                    showAiBuddyPanel = true
                                    viewModel.askAiBookBuddy("Analyze this selected passage in detail: characters, symbols, and literary context.", pText)
                                    selectedParagraphText = null
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("ask_companion_button")
                            ) {
                                Icon(imageVector = Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ask Buddy", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Expanding sliding Drawer overlay for Reader Comfort settings (Font Resizing, Themes Selection)
            AnimatedVisibility(
                visible = showFontMenu,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reader Controls",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { showFontMenu = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close Menu")
                            }
                        }

                        // Font sizing adjustment row
                        Text(
                            text = "Text Font Sizing",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { viewModel.setFontSize(fontSizeScale - 2) },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).testTag("decrease_font_button")
                            ) {
                                Text("A-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            
                            Text(
                                text = "FontSize: ${fontSizeScale}sp",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )

                            IconButton(
                                onClick = { viewModel.setFontSize(fontSizeScale + 2) },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).testTag("increase_font_button")
                            ) {
                                Text("A+", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Themes swapping list
                        Text(
                            text = "Aesthetic Background Theme",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                        
                        val themes = listOf("Parchment", "Sepia", "Dark")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            themes.forEach { themeName ->
                                val isSelected = themeName == readingTheme
                                val bColor = when (themeName) {
                                    "Parchment" -> ReadingThemeParchmentBg
                                    "Sepia" -> ReadingThemeSepiaBg
                                    "Dark" -> ReadingThemeDarkBg
                                    else -> Color.White
                                }
                                val borderStroke = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null

                                Card(
                                    border = borderStroke,
                                    onClick = { viewModel.setReadingTheme(themeName) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("theme_btn_${themeName.lowercase()}"),
                                    colors = CardDefaults.cardColors(containerColor = bColor),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = themeName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = when (themeName) {
                                                "Parchment" -> ReadingThemeParchmentText
                                                "Sepia" -> ReadingThemeSepiaText
                                                "Dark" -> ReadingThemeDarkText
                                                else -> Color.Black
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Gemini AI Book Buddy interaction companion panel
            AnimatedVisibility(
                visible = showAiBuddyPanel,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .align(Alignment.CenterEnd)
                    .shadow(16.dp, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    // Header AI Section
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Book Buddy",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                        IconButton(onClick = { showAiBuddyPanel = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close budget", modifier = Modifier.size(20.dp))
                        }
                    }

                    // Predefined summarizing helpers
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { viewModel.getSummaries() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).height(32.dp).testTag("summarize_chapter_btn")
                        ) {
                            Text("Summarize", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.askAiBookBuddy("Explain the character relations and conflict present in this chapter of the text.") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f).height(32.dp).testTag("explain_characters_btn")
                        ) {
                            Text("Characters", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Discussion scrolling workspace
                    val buddyScrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(buddyScrollState)
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (aiDiscussion.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(top = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Ask anything about this chapter! Highlight a paragraph and tap 'Ask Buddy' or type below.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        } else {
                            aiDiscussion.forEach { msg ->
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
                                ) {
                                    // Highlight passage quote block inside chat if present
                                    if (msg.highlightedPassage != null && msg.highlightedPassage.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .padding(bottom = 4.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f)), RoundedCornerShape(4.dp))
                                                .padding(6.dp)
                                        ) {
                                            Text(
                                                text = "“${msg.highlightedPassage}”",
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Balloon Chat bubbles
                                    val bubbleBg = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    val bubbleContentColor = if (msg.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    val bubbleShape = if (msg.isUser) RoundedCornerShape(12.dp, 1.dp, 12.dp, 12.dp) else RoundedCornerShape(1.dp, 12.dp, 12.dp, 12.dp)

                                    Box(
                                        modifier = Modifier
                                            .background(bubbleBg, bubbleShape)
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp,
                                            color = bubbleContentColor
                                        )
                                    }
                                }
                            }
                        }

                        // Thinking/Typing companion indicator
                        if (isAiThinking) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(1.dp, 12.dp, 12.dp, 12.dp))
                                    .padding(10.dp)
                                    .align(Alignment.Start)
                            ) {
                                Text(
                                    text = "Buddy is analyzing manuscript properties...",
                                    fontSize = 10.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LaunchedEffect(aiDiscussion.size, isAiThinking) {
                                buddyScrollState.animateScrollTo(buddyScrollState.maxValue)
                            }
                        }
                    }

                    // Input Textfield row
                    OutlinedTextField(
                        value = userAiQuestionInput,
                        onValueChange = { userAiQuestionInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 80.dp)
                            .testTag("ai_questions_input"),
                        placeholder = { Text("Ask your reader buddy...", fontSize = 11.sp) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (userAiQuestionInput.isNotBlank()) {
                                        viewModel.askAiBookBuddy(userAiQuestionInput)
                                        userAiQuestionInput = ""
                                    }
                                },
                                enabled = userAiQuestionInput.isNotBlank() && !isAiThinking
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send, 
                                    contentDescription = "Send prompt", 
                                    tint = if (userAiQuestionInput.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        textStyle = TextStyle(fontSize = 12.sp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)
                        )
                    )
                }
            }

            // Dedicated highlights list drawer (view, organize, delete existing highlights in the book!)
            AnimatedVisibility(
                visible = showHighlightsPanel,
                enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .align(Alignment.CenterStart)
                    .shadow(16.dp, RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.BorderColor, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Margin Highlights",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        IconButton(onClick = { showHighlightsPanel = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close panel")
                        }
                    }

                    val bookHighlights = book.getHighlights()
                    if (bookHighlights.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                "No highlighted content in this edition yet.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(bookHighlights) { highlight ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "“${highlight.text}”",
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Ch ${highlight.chapterIndex + 1}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            IconButton(
                                                onClick = { viewModel.deleteHighlight(highlight.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline, 
                                                    contentDescription = "Delete highlight",
                                                    tint = MaterialTheme.colorScheme.error,
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
            }
        }
    }
}
