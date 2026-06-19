package com.example.ui.books

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Book

@Composable
fun BookCoverItem(
    book: Book,
    modifier: Modifier = Modifier,
    loadingProgress: Int? = null,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .padding(4.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                // Book Cover Cover
                if (book.coverUrl.isNotEmpty()) {
                    AsyncImage(
                        model = book.coverUrl,
                        contentDescription = "Cover for ${book.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    )
                }

                // Decorative Fallback/Overlaid Jacket (for rich branding)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                startY = 120f
                            )
                        )
                )

                // Classic spine accent line on the left to make it look like a real physical book
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(6.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Black.copy(0.4f), Color.White.copy(0.2f), Color.Transparent)
                            )
                        )
                        .align(Alignment.CenterStart)
                )

                // Downloading progress overlay indicator
                if (loadingProgress != null && loadingProgress < 100) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                progress = { loadingProgress.toFloat() / 100f },
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Downloading $loadingProgress%",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // If not downloaded and not downloading, show a little download badge/icon
                if (!book.isDownloaded && (loadingProgress == null || loadingProgress >= 100)) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Available for offline download",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Book Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = book.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = book.author,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                // Shelf Category Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = book.genre,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    
                    if (book.isFavorite) {
                        Text(
                            text = "★ Favorite",
                            color = Color(0xFFFFB300),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShelfDivider(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(end = 8.dp)
            )
            Divider(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                thickness = 1.dp,
                modifier = Modifier.weight(1f)
            )
        }
        // Aesthetic wooden/glass shelf shadow effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}
