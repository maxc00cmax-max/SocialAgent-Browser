package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.database.entity.RecentPageEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineRecentPagesView(
    viewModel: MainViewModel,
    onOpenInBrowser: (url: String, title: String) -> Unit,
    onOpenOpenGraphScraper: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    val context = LocalContext.current
    val allPages by viewModel.allRecentPages.collectAsState()
    val pinnedPages by viewModel.pinnedRecentPages.collectAsState()
    val filterPlatform by viewModel.recentPagesFilterPlatform.collectAsState()
    val searchQuery by viewModel.recentPagesSearchQuery.collectAsState()
    val pinnedOnly by viewModel.recentPagesPinnedOnly.collectAsState()
    val sortByVisits by viewModel.recentPagesSortByVisits.collectAsState()

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showAddPageDialog by remember { mutableStateOf(false) }

    val filteredPages = remember(allPages, filterPlatform, searchQuery, pinnedOnly, sortByVisits) {
        val base = allPages.filter { page ->
            val matchesPlatform = filterPlatform == null || page.platform.equals(filterPlatform, ignoreCase = true)
            val matchesPinned = !pinnedOnly || page.isPinned
            val matchesQuery = searchQuery.isBlank() ||
                    page.title.contains(searchQuery, ignoreCase = true) ||
                    page.url.contains(searchQuery, ignoreCase = true) ||
                    page.previewSnippet.contains(searchQuery, ignoreCase = true) ||
                    (page.siteName?.contains(searchQuery, ignoreCase = true) == true)
            matchesPlatform && matchesPinned && matchesQuery
        }
        if (sortByVisits) {
            base.sortedByDescending { it.visitCount }
        } else {
            base
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.background)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // 1. Header Banner & Quick Actions
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = c.card,
            border = BorderStroke(1.dp, c.borderSubtle),
            shadowElevation = if (c.isDark) 0.dp else 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = c.accentSoft,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Bookmarks,
                                contentDescription = null,
                                tint = c.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "الصفحات المزارة أوفلاين",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = c.textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = c.successSubtle
                            ) {
                                Text(
                                    text = "Room",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = c.success,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${allPages.size} صفحة محفوظة • ${pinnedPages.size} مثبتة",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textMuted
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { onOpenOpenGraphScraper("") },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("open_opengraph_from_recent_btn"),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = c.accentSoft)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TravelExplore,
                            contentDescription = "استخراج OpenGraph",
                            tint = c.accent,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = { showAddPageDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("add_recent_page_btn"),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = c.cardElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "إضافة صفحة",
                            tint = c.textPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    if (allPages.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("clear_recent_pages_btn"),
                            colors = IconButtonDefaults.iconButtonColors(containerColor = c.errorSubtle)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "مسح السجل",
                                tint = c.error,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setRecentPagesSearchQuery(it) },
            placeholder = {
                Text(
                    "ابحث في العناوين، الروابط، أو اسم الموقع...",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textMuted
                )
            },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setRecentPagesSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح", tint = c.textMuted, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = c.card,
                unfocusedContainerColor = c.card,
                focusedBorderColor = c.accent,
                unfocusedBorderColor = c.borderSubtle,
                focusedTextColor = c.textPrimary,
                unfocusedTextColor = c.textPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recent_pages_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Platform Filter Chips
        val platforms = listOf(
            null to "الكل",
            "X_TWITTER" to "X (تويتر)",
            "LINKEDIN" to "LinkedIn",
            "INSTAGRAM" to "Instagram",
            "REDDIT" to "Reddit",
            "TIKTOK" to "TikTok",
            "FACEBOOK" to "Facebook",
            "WEB" to "ويب عام"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            platforms.forEach { (platformKey, label) ->
                val isSelected = filterPlatform == platformKey
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) c.accent else c.card,
                    border = BorderStroke(1.dp, if (isSelected) c.accent else c.borderSubtle),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { viewModel.setRecentPagesFilterPlatform(platformKey) }
                        .testTag("filter_platform_${platformKey ?: "all"}")
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else c.textSecondary,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // Active Secondary-Panel Filters Indicator
        if (pinnedOnly || sortByVisits) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pinnedOnly) {
                    AssistChip(
                        onClick = { viewModel.setRecentPagesPinnedOnly(false) },
                        label = { Text("المثبتة فقط", style = MaterialTheme.typography.labelSmall) },
                        trailingIcon = {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "إلغاء", modifier = Modifier.size(14.dp))
                        }
                    )
                }
                if (sortByVisits) {
                    AssistChip(
                        onClick = { viewModel.setRecentPagesSortByVisits(false) },
                        label = { Text("الأكثر زيارة", style = MaterialTheme.typography.labelSmall) },
                        trailingIcon = {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "إلغاء", modifier = Modifier.size(14.dp))
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Content List
        if (filteredPages.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = c.card,
                border = BorderStroke(1.dp, c.borderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = c.accentSoft,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = c.accent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (searchQuery.isNotBlank() || filterPlatform != null || pinnedOnly)
                            "لا توجد نتائج مطابقة للتصفية" else "لا توجد صفحات مزارة محفوظة بعد",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "يمكنك حفظ أي صفحة تلقائياً من المتصفح أو استخراج OpenGraph لأي رابط.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (searchQuery.isNotBlank() || filterPlatform != null || pinnedOnly) {
                        Button(
                            onClick = {
                                viewModel.setRecentPagesSearchQuery("")
                                viewModel.setRecentPagesFilterPlatform(null)
                                viewModel.setRecentPagesPinnedOnly(false)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White)
                        ) {
                            Text("إعادة ضبط الفلاتر", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        Button(
                            onClick = { onOpenOpenGraphScraper("") },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White)
                        ) {
                            Icon(imageVector = Icons.Default.TravelExplore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استخراج صفحة بـ OpenGraph", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredPages, key = { it.id }) { page ->
                    RecentPageCard(
                        page = page,
                        onOpen = { onOpenInBrowser(page.url, page.title) },
                        onScrapeOpenGraph = { onOpenOpenGraphScraper(page.url) },
                        onTogglePin = { viewModel.togglePinRecentPage(page.id, !page.isPinned) },
                        onDelete = { viewModel.deleteRecentPage(page.id) }
                    )
                }
            }
        }
    }

    // Confirmation Dialog for Clearing All
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = c.card,
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = c.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مسح كل الصفحات المحفوظة؟", color = c.textPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("سيتم حذف جميع الصفحات المسجلة في قاعدة بيانات Room أوفلاين.", color = c.textSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllRecentPages()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "تم مسح جميع الصفحات المسجلة", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = c.error, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("نعم، مسح الكل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("إلغاء", color = c.textSecondary)
                }
            }
        )
    }

    // Add Manual Page Dialog
    if (showAddPageDialog) {
        AddManualPageDialog(
            onDismiss = { showAddPageDialog = false },
            onAdd = { title, url, snippet ->
                viewModel.recordRecentPageVisit(url = url, title = title, snippet = snippet)
                showAddPageDialog = false
                Toast.makeText(context, "تمت إضافة الصفحة إلى قاعدة البيانات أوفلاين", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun RecentPageCard(
    page: RecentPageEntity,
    onOpen: () -> Unit,
    onScrapeOpenGraph: () -> Unit = {},
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val c = AppTheme.colors
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    val platformColor = when (page.platform.uppercase()) {
        "X_TWITTER" -> c.accentSecondary
        "LINKEDIN" -> InfoBlue
        "INSTAGRAM" -> ElectricViolet
        "REDDIT" -> c.warning
        "TIKTOK" -> c.accentSecondary
        "FACEBOOK" -> c.accent
        else -> c.textSecondary
    }

    val platformName = when (page.platform.uppercase()) {
        "X_TWITTER" -> "إكس (X)"
        "LINKEDIN" -> "لينكد إن"
        "INSTAGRAM" -> "إنستغرام"
        "REDDIT" -> "ريديت"
        "TIKTOK" -> "تيك توك"
        "FACEBOOK" -> "فيسبوك"
        else -> "ويب"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = c.card,
        shadowElevation = if (c.isDark) 0.dp else 1.dp,
        border = BorderStroke(
            1.dp,
            if (page.isPinned) c.warning.copy(alpha = 0.5f) else c.borderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_page_card_${page.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Platform Badge + Pinned Indicator + Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = platformColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = platformName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = platformColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (page.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = c.warningSubtle
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = c.warning,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "مثبتة",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = c.warning,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("pin_btn_${page.id}")
                    ) {
                        Icon(
                            imageVector = if (page.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (page.isPinned) "إلغاء التثبيت" else "تثبيت",
                            tint = if (page.isPinned) c.warning else c.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Page URL", page.url)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "تم نسخ الرابط", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "نسخ الرابط",
                            tint = c.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("delete_btn_${page.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "حذف",
                            tint = c.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!page.ogImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = page.ogImageUrl,
                    contentDescription = page.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.cardElevated)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = page.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!page.siteName.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = c.accentSoft
                    ) {
                        Text(
                            text = page.siteName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = c.accent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = page.url,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp
                    ),
                    color = c.accentSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            if (page.previewSnippet.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = c.cardElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = page.previewSnippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = c.cardElevated
                    ) {
                        Text(
                            text = "${page.visitCount} زيارات",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = c.textSecondary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = sdf.format(Date(page.lastVisitedTimestamp)),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = c.textMuted
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onScrapeOpenGraph,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("scrape_og_btn_${page.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TravelExplore,
                            contentDescription = "تحديث OpenGraph",
                            tint = c.accentSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    FilledTonalButton(
                        onClick = onOpen,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = c.accent,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("open_page_btn_${page.id}")
                    ) {
                        Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فتح", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddManualPageDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, url: String, snippet: String) -> Unit
) {
    val c = AppTheme.colors
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var snippet by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = c.card,
            border = BorderStroke(1.dp, c.border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إضافة صفحة أوفلاين يدوياً",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = c.textMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الصفحة") },
                    placeholder = { Text("مثال: صفحة أبحاث الذكاء الاصطناعي") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("رابط الصفحة (URL)") },
                    placeholder = { Text("https://x.com/...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = snippet,
                    onValueChange = { snippet = it },
                    label = { Text("نبذة مختصرة (اختياري)") },
                    placeholder = { Text("أبرز ما تحتويه هذه الصفحة...") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (url.isNotBlank()) {
                            onAdd(title.ifBlank { url }, url, snippet)
                        }
                    },
                    enabled = url.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ في قاعدة البيانات أوفلاين", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
