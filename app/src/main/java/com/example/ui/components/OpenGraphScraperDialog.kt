package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.work.WorkInfo
import coil.compose.AsyncImage
import com.example.database.entity.CachedMetadataEntity
import com.example.scraper.MetadataScrapeWorker
import com.example.scraper.OpenGraphScrapeState
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OpenGraphScraperDialog(
    viewModel: MainViewModel,
    initialUrl: String = "",
    onDismiss: () -> Unit,
    onViewRecentPages: () -> Unit
) {
    val c = AppTheme.colors
    val tabs by viewModel.browserManager.tabs.collectAsState()
    val scrapeState by viewModel.openGraphScrapeState.collectAsState()
    val cachedList by viewModel.cachedMetadataList.collectAsState()
    val workInfos by viewModel.backgroundScrapeWorkInfos.collectAsState()

    var urlInput by remember(initialUrl) {
        mutableStateOf(
            initialUrl.ifBlank { tabs.firstOrNull()?.url ?: "https://github.com" }
        )
    }

    LaunchedEffect(initialUrl, cachedList) {
        if (initialUrl.isNotBlank() && scrapeState is OpenGraphScrapeState.Idle) {
            val match = cachedList.firstOrNull {
                it.resolvedUrl.equals(initialUrl.trim(), ignoreCase = true) ||
                    it.requestedUrl.equals(initialUrl.trim(), ignoreCase = true)
            }
            if (match != null) {
                viewModel.displayCachedMetadata(match)
            }
        }
    }

    val activeWork = workInfos.firstOrNull {
        it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
    }
    val latestWork = activeWork ?: workInfos.firstOrNull()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(22.dp),
            color = c.surface,
            border = BorderStroke(1.dp, c.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = c.accentSoft,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TravelExplore,
                                    contentDescription = null,
                                    tint = c.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "مستخرج OpenGraph والكاش الذكي",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = c.textPrimary
                            )
                            Text(
                                text = "عرض فوري من Room + مهام WorkManager بالخلفية",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textMuted
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = c.textMuted
                        )
                    }
                }

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("رابط أو عدة روابط (افصل بفاصلة أو سطر)") },
                    placeholder = { Text("https://example.com/...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = c.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (urlInput.isNotEmpty()) {
                            IconButton(onClick = { urlInput = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "مسح",
                                    tint = c.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    minLines = 1,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = c.accent,
                        unfocusedBorderColor = c.border,
                        focusedContainerColor = c.cardElevated,
                        unfocusedContainerColor = c.cardElevated,
                        focusedTextColor = c.textPrimary,
                        unfocusedTextColor = c.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("opengraph_url_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "روابط سريعة من النوافذ أو مقترحة:",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tabs.forEach { tab ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = c.accentSoft,
                                border = BorderStroke(1.dp, c.accent.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        urlInput = tab.url
                                        viewModel.scrapeUrlWithOpenGraph(tab.url, forceRefresh = false)
                                    }
                            ) {
                                Text(
                                    text = tab.title.take(18),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = c.accent,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                        listOf(
                            "GitHub" to "https://github.com",
                            "Kotlin" to "https://kotlinlang.org",
                            "Android" to "https://developer.android.com"
                        ).forEach { (label, presetUrl) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = c.cardElevated,
                                border = BorderStroke(1.dp, c.borderSubtle),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { urlInput = presetUrl }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.textSecondary,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // Dual Actions: Instant Cache-First Fetch + Background WorkManager Queue
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.scrapeUrlWithOpenGraph(urlInput, forceRefresh = false) },
                        enabled = urlInput.isNotBlank() && scrapeState !is OpenGraphScrapeState.Loading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = c.accent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("opengraph_scrape_button")
                    ) {
                        if (scrapeState is OpenGraphScrapeState.Loading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("جاري السحب...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("جلب فوري", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    FilledTonalButton(
                        onClick = { viewModel.enqueueBackgroundMetadataScrape(urlInput, forceRefresh = true) },
                        enabled = urlInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = c.accentSecondary.copy(alpha = 0.14f),
                            contentColor = c.accentSecondary
                        ),
                        border = BorderStroke(1.dp, c.accentSecondary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("opengraph_background_worker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScheduleSend,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("بالخلفية", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Live WorkManager Background Job Progress Strip
                if (latestWork != null) {
                    BackgroundWorkStatusBanner(workInfo = latestWork)
                }

                when (val state = scrapeState) {
                    is OpenGraphScrapeState.Success -> {
                        val meta = state.metadata
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = c.cardElevated,
                            border = BorderStroke(1.dp, c.success.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = if (state.fromCache) Icons.Default.OfflineBolt else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (state.fromCache) c.accentSecondary else c.success,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (state.fromCache) "عرض فوري من الكاش المحلي" else "تم السحب والحفظ في Room",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (state.fromCache) c.accentSecondary else c.success,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (state.fromCache) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = c.accentSoft,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        viewModel.scrapeUrlWithOpenGraph(meta.resolvedUrl, forceRefresh = true)
                                                    }
                                            ) {
                                                Text(
                                                    text = "تحديث",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = c.accent,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = c.accentSoft
                                        ) {
                                            Text(
                                                text = "${meta.extractedTagsCount} وسوم",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = c.accent,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                if (!meta.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = meta.imageUrl,
                                        contentDescription = meta.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(c.card)
                                    )
                                }

                                Text(
                                    text = meta.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = c.textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (meta.description.isNotBlank()) {
                                    Text(
                                        text = meta.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = c.textSecondary,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = meta.resolvedUrl,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp
                                    ),
                                    color = c.accentSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                OutlinedButton(
                                    onClick = {
                                        onDismiss()
                                        onViewRecentPages()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, c.accent.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmarks,
                                        contentDescription = null,
                                        tint = c.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("عرض في الصفحات المزارة أوفلاين", color = c.accent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    is OpenGraphScrapeState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = c.errorSubtle,
                            border = BorderStroke(1.dp, c.error.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = c.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.textPrimary
                                )
                            }
                        }
                    }

                    else -> Unit
                }

                // Instant Cached Previous Scrapes Shelf
                if (cachedList.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "السجل المخزن بالكاش (${cachedList.size}) — عرض فوري",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = c.textPrimary
                            )
                            Text(
                                text = "مسح الكاش",
                                style = MaterialTheme.typography.labelSmall,
                                color = c.error,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { viewModel.clearAllCachedMetadata() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        cachedList.take(5).forEach { item ->
                            CachedMetadataMiniCard(
                                item = item,
                                onSelect = {
                                    urlInput = item.resolvedUrl
                                    viewModel.displayCachedMetadata(item)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundWorkStatusBanner(workInfo: WorkInfo) {
    val c = AppTheme.colors
    val progressTotal = workInfo.progress.getInt(MetadataScrapeWorker.KEY_PROGRESS_TOTAL, 0)
    val progressCompleted = workInfo.progress.getInt(MetadataScrapeWorker.KEY_PROGRESS_COMPLETED, 0)
    val currentUrl = workInfo.progress.getString(MetadataScrapeWorker.KEY_CURRENT_URL).orEmpty()
    val succeeded = workInfo.outputData.getInt(MetadataScrapeWorker.KEY_SUCCEEDED_COUNT, 0)
    val failed = workInfo.outputData.getInt(MetadataScrapeWorker.KEY_FAILED_COUNT, 0)

    val (bg, tint, label) = when (workInfo.state) {
        WorkInfo.State.ENQUEUED -> Triple(c.cardElevated, c.accentSecondary, "مجدولة في WorkManager للعمل بالخلفية...")
        WorkInfo.State.RUNNING -> Triple(
            c.accentSoft,
            c.accent,
            if (progressTotal > 0) "WorkManager يعمل بالخلفية ($progressCompleted/$progressTotal): ${currentUrl.take(32)}"
            else "WorkManager يقوم بسحب الميتاداتا بالخلفية..."
        )
        WorkInfo.State.SUCCEEDED -> Triple(
            c.successSubtle,
            c.success,
            "✅ اكتملت مهمة الخلفية: حفظ $succeeded رابط بالكاش" + if (failed > 0) " ($failed تعذر)" else ""
        )
        WorkInfo.State.FAILED -> Triple(
            c.errorSubtle,
            c.error,
            "⚠️ تعذر إكمال مهمة الخلفية: ${workInfo.outputData.getString(MetadataScrapeWorker.KEY_LAST_ERROR) ?: "تحقق من الرابط"}"
        )
        else -> Triple(c.cardElevated, c.textMuted, "حالة المهمة: ${workInfo.state.name}")
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("workmanager_status_banner")
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (workInfo.state == WorkInfo.State.RUNNING || workInfo.state == WorkInfo.State.ENQUEUED) {
                    CircularProgressIndicator(color = tint, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                } else {
                    Icon(imageVector = Icons.Default.WorkHistory, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = tint,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (workInfo.state == WorkInfo.State.RUNNING && progressTotal > 0) {
                LinearProgressIndicator(
                    progress = { (progressCompleted.toFloat() / progressTotal.toFloat()).coerceIn(0f, 1f) },
                    color = tint,
                    trackColor = c.card,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun CachedMetadataMiniCard(
    item: CachedMetadataEntity,
    onSelect: () -> Unit
) {
    val c = AppTheme.colors
    val formattedDate = remember(item.scrapedAt) {
        SimpleDateFormat("HH:mm • dd/MM", Locale.getDefault()).format(Date(item.scrapedAt))
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = c.cardElevated,
        border = BorderStroke(1.dp, c.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!item.ogImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.ogImageUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(c.card)
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = c.accentSoft,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdded,
                            contentDescription = null,
                            tint = c.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "$formattedDate • ${item.resolvedUrl}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp
                    ),
                    color = c.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "عرض",
                tint = c.accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
