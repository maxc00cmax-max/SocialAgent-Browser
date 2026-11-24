package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allPages by viewModel.allRecentPages.collectAsState()
    val pinnedPages by viewModel.pinnedRecentPages.collectAsState()
    val filterPlatform by viewModel.recentPagesFilterPlatform.collectAsState()
    val searchQuery by viewModel.recentPagesSearchQuery.collectAsState()

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showAddPageDialog by remember { mutableStateOf(false) }

    val filteredPages = remember(allPages, filterPlatform, searchQuery) {
        allPages.filter { page ->
            val matchesPlatform = filterPlatform == null || page.platform.equals(filterPlatform, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    page.title.contains(searchQuery, ignoreCase = true) ||
                    page.url.contains(searchQuery, ignoreCase = true) ||
                    page.previewSnippet.contains(searchQuery, ignoreCase = true)
            matchesPlatform && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // 1. Header Banner & Quick Summary
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CardSurface,
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AccentIndigo.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, CardBorderSubtle),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Bookmarks,
                                contentDescription = null,
                                tint = NeonCyan,
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
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldGreen.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "Room DB 💾",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = EmeraldGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${allPages.size} صفحة محفوظة محلياً • ${pinnedPages.size} مثبتة بالمفضلة",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Add Page Button
                    IconButton(
                        onClick = { showAddPageDialog = true },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("add_recent_page_btn"),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = CardSurfaceElevated)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة صفحة", tint = NeonCyan, modifier = Modifier.size(18.dp))
                    }

                    // Clear All Button
                    if (allPages.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("clear_recent_pages_btn"),
                            colors = IconButtonDefaults.iconButtonColors(containerColor = CardSurfaceElevated)
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "مسح السجل", tint = AlertRed, modifier = Modifier.size(18.dp))
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
            placeholder = { Text("ابحث في عناوين وروابط الصفحات المحفوظة...", style = MaterialTheme.typography.bodySmall, color = TextMuted) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setRecentPagesSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardSurface,
                unfocusedContainerColor = CardSurface,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = CardBorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("recent_pages_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Platform Filter Chips (Horizontal Scroll)
        val platforms = listOf(
            null to "الكل",
            "X_TWITTER" to "إكس (Twitter)",
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
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) AccentIndigo else CardSurface,
                    border = BorderStroke(1.dp, if (isSelected) NeonCyan.copy(alpha = 0.6f) else CardBorderSubtle),
                    modifier = Modifier
                        .clickable { viewModel.setRecentPagesFilterPlatform(platformKey) }
                        .testTag("filter_platform_${platformKey ?: "all"}")
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Content List
        if (filteredPages.isEmpty()) {
            // Empty State
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CardSurface,
                border = BorderStroke(1.dp, CardBorderSubtle),
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
                        color = CardSurfaceElevated,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (searchQuery.isNotBlank() || filterPlatform != null) "لا توجد نتائج مطابقة للبحث أو الفلتر" else "لا توجد صفحات مزارة محفوظة بعد",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "عند تصفح أي حساب أو صفحة في المتصفح سيتم حفظها تلقائياً أوفلاين في قاعدة بيانات Room للرجوع إليها لاحقاً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (searchQuery.isNotBlank() || filterPlatform != null) {
                        Button(
                            onClick = {
                                viewModel.setRecentPagesSearchQuery("")
                                viewModel.setRecentPagesFilterPlatform(null)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                        ) {
                            Text("إعادة ضبط الفلاتر", style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Button(
                            onClick = { showAddPageDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة صفحة الآن", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredPages, key = { it.id }) { page ->
                    RecentPageCard(
                        page = page,
                        onOpen = { onOpenInBrowser(page.url, page.title) },
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
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = AlertRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مسح كل الصفحات المحفوظة؟", color = TextPrimary)
                }
            },
            text = {
                Text("سيتم حذف جميع الصفحات المسجلة في قاعدة بيانات Room أوفلاين. هل أنت متأكد؟", color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllRecentPages()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "تم مسح جميع الصفحات المسجلة", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("نعم، مسح الكل", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirmDialog = false }) {
                    Text("إلغاء", color = TextPrimary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
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
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    val platformColor = when (page.platform.uppercase()) {
        "X_TWITTER" -> NeonCyan
        "LINKEDIN" -> InfoBlue
        "INSTAGRAM" -> ElectricViolet
        "REDDIT" -> AccentAmber
        "TIKTOK" -> NeonCyan
        "FACEBOOK" -> AccentIndigo
        else -> TextSecondary
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
        shape = RoundedCornerShape(12.dp),
        color = CardSurface,
        border = BorderStroke(
            1.dp,
            if (page.isPinned) AccentAmber.copy(alpha = 0.5f) else CardBorderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_page_card_${page.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top Row: Platform Badge + Pinned Indicator + Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = platformColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, platformColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = platformName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = platformColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (page.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AccentAmber.copy(alpha = 0.15f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "مُثبتة",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = AccentAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Pin Button
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("pin_btn_${page.id}")
                    ) {
                        Icon(
                            imageVector = if (page.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (page.isPinned) "إلغاء التثبيت" else "تثبيت",
                            tint = if (page.isPinned) AccentAmber else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Copy URL Button
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Page URL", page.url)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "تم نسخ الرابط", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "نسخ الرابط",
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("delete_btn_${page.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "حذف",
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = page.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // URL
            Text(
                text = page.url,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                ),
                color = NeonCyan.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Snippet if available
            if (page.previewSnippet.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DarkBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = page.previewSnippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Bar: Metadata + Open Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Visits counter
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = CardSurfaceElevated
                    ) {
                        Text(
                            text = "${page.visitCount} زيارات",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Timestamp
                    Text(
                        text = sdf.format(Date(page.lastVisitedTimestamp)),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextMuted
                    )
                }

                FilledTonalButton(
                    onClick = onOpen,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AccentIndigo,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .defaultMinSize(minWidth = 48.dp, minHeight = 30.dp)
                        .testTag("open_page_btn_${page.id}")
                ) {
                    Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("فتح في المتصفح", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var snippet by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, CardBorder),
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
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الصفحة") },
                    placeholder = { Text("مثال: صفحة أبحاث الذكاء الاصطناعي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("رابط الصفحة (URL)") },
                    placeholder = { Text("https://x.com/...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = snippet,
                    onValueChange = { snippet = it },
                    label = { Text("نبذة مختصرة / ملاحظة (اختياري)") },
                    placeholder = { Text("أبرز ما تحتويه هذه الصفحة...") },
                    maxLines = 3,
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
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ في قاعدة البيانات أوفلاين", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
