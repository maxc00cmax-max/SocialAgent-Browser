package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.entity.ScrapedPostMetadataEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DataMarketingTabScreen(
    viewModel: MainViewModel,
    onOpenOpenGraphScraper: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    val posts by viewModel.allScrapedPosts.collectAsState()
    val cachedMetadata by viewModel.cachedMetadataList.collectAsState()
    val scope = rememberCoroutineScope()
    val selectedPlatform by viewModel.databasePlatformFilter.collectAsState()
    val searchQuery by viewModel.databaseSearchQuery.collectAsState()

    val filteredPosts = remember(posts, selectedPlatform, searchQuery) {
        posts.filter { post ->
            val platformMatch = selectedPlatform == "ALL" || post.platform.equals(selectedPlatform, ignoreCase = true)
            val searchMatch = searchQuery.isBlank() ||
                    post.postContent.contains(searchQuery, ignoreCase = true) ||
                    post.authorHandle.contains(searchQuery, ignoreCase = true) ||
                    post.authorDisplayName.contains(searchQuery, ignoreCase = true)
            platformMatch && searchMatch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.background)
    ) {
        // Top Header
        Surface(
            color = c.surface,
            border = BorderStroke(1.dp, c.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "بيانات المنشورات و OpenGraph",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = c.textPrimary
                        )
                        Text(
                            text = "${posts.size} منشور • ${cachedMetadata.size} كاش فوري في Room",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = { onOpenOpenGraphScraper("") },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = c.accentSoft,
                                contentColor = c.accent
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("database_open_opengraph_btn")
                        ) {
                            Icon(imageVector = Icons.Filled.TravelExplore, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("OpenGraph", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.socialMediaScraper.scrapePlatformPosts("X_TWITTER", 5)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = c.success, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تحديث", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Search & Platform Filters Section
        Surface(
            color = c.card,
            border = BorderStroke(1.dp, c.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setDatabaseSearchQuery(it) },
                    placeholder = { Text("بحث في المنشورات أو أسماء الحسابات...", style = MaterialTheme.typography.bodySmall, color = c.textMuted) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(16.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setDatabaseSearchQuery("") }, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = Icons.Filled.Clear, contentDescription = "مسح", tint = c.textMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = c.accent,
                        unfocusedBorderColor = c.border,
                        focusedContainerColor = c.cardElevated,
                        unfocusedContainerColor = c.cardElevated,
                        focusedTextColor = c.textPrimary,
                        unfocusedTextColor = c.textPrimary
                    )
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val platforms = listOf(
                        "ALL" to "الكل (${posts.size})",
                        "X_TWITTER" to "X (تويتر)",
                        "LINKEDIN" to "LinkedIn",
                        "INSTAGRAM" to "Instagram",
                        "REDDIT" to "Reddit",
                        "WEB" to "ويب / OpenGraph"
                    )
                    items(platforms) { (code, label) ->
                        val isSelected = selectedPlatform == code
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) c.accent else c.cardElevated,
                            border = BorderStroke(1.dp, if (isSelected) c.accent else c.borderSubtle),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setDatabasePlatformFilter(code) }
                                .defaultMinSize(minHeight = 30.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else c.textSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Instant Cached Metadata Strip (zero network calls)
        if (cachedMetadata.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الكاش الفوري للروابط (أوفلاين)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary
                    )
                    Text(
                        text = "${cachedMetadata.size} محفوظ",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.accent,
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(cachedMetadata.take(10), key = { it.id }) { cached ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = c.card,
                            border = BorderStroke(1.dp, c.borderSubtle),
                            shadowElevation = if (c.isDark) 0.dp else 1.dp,
                            modifier = Modifier
                                .width(220.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.displayCachedMetadata(cached)
                                    onOpenOpenGraphScraper(cached.resolvedUrl)
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = c.accentSoft,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.OfflineBolt,
                                                contentDescription = null,
                                                tint = c.accent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = cached.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = c.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (cached.description.isNotBlank()) {
                                    Text(
                                        text = cached.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = c.textSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = cached.resolvedUrl,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.accentSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Posts List or Empty State
        if (filteredPosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Storage,
                        contentDescription = null,
                        tint = c.textMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (posts.isEmpty()) "قاعدة البيانات فارغة حالياً" else "لا توجد منشورات مطابقة للبحث",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "استخدم مستخرج OpenGraph أو شغّل مهمة الخلفية لجلب المحتوى وحفظه",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredPosts, key = { it.id }) { post ->
                    ScrapedPostCard(post = post)
                }
            }
        }
    }
}

@Composable
fun ScrapedPostCard(post: ScrapedPostMetadataEntity) {
    val c = AppTheme.colors
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = c.card,
        shadowElevation = if (c.isDark) 0.dp else 1.dp,
        border = BorderStroke(1.dp, c.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = c.accentSoft
                    ) {
                        Text(
                            text = when (post.platform) {
                                "X_TWITTER" -> "X / Twitter"
                                "LINKEDIN" -> "LinkedIn"
                                "INSTAGRAM" -> "Instagram"
                                "REDDIT" -> "Reddit"
                                else -> post.platform
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = c.accent,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = post.authorDisplayName.ifBlank { post.authorHandle },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = c.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = dateFormat.format(Date(post.scrapedTimestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = post.postContent,
                style = MaterialTheme.typography.bodyMedium,
                color = c.textPrimary,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (post.likesCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Favorite, contentDescription = null, tint = c.error, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${post.likesCount}", style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
                    }
                }
                if (post.commentsCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Comment, contentDescription = null, tint = c.accentSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${post.commentsCount}", style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
                    }
                }
                if (post.sharesCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = null, tint = c.accent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${post.sharesCount}", style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
                    }
                }
                if (post.viewsCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Visibility, contentDescription = null, tint = c.success, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${post.viewsCount}", style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
                    }
                }
            }
        }
    }
}
