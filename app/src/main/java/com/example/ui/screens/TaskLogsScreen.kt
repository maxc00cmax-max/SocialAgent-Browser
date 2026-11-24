package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.entity.TabTaskLogEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskLogsScreen(
    viewModel: MainViewModel,
    onNavigateToTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    val logs by viewModel.allTabTaskLogs.collectAsState()
    val tabs by viewModel.browserManager.tabs.collectAsState()
    val context = LocalContext.current

    val selectedTabFilter by viewModel.logsTabFilter.collectAsState()
    val selectedStatusFilter by viewModel.logsStatusFilter.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val filteredLogs = remember(logs, selectedTabFilter, selectedStatusFilter, searchQuery) {
        logs.filter { log ->
            val matchesTab = selectedTabFilter == null || log.tabId == selectedTabFilter
            val matchesStatus = when (selectedStatusFilter) {
                "COMPLETED" -> log.status == "COMPLETED"
                "IN_PROGRESS" -> log.status == "IN_PROGRESS"
                "FAILED" -> log.status == "FAILED"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    log.taskPrompt.contains(searchQuery, ignoreCase = true) ||
                    log.outputResult.contains(searchQuery, ignoreCase = true) ||
                    log.tabTitle.contains(searchQuery, ignoreCase = true)

            matchesTab && matchesStatus && matchesSearch
        }
    }

    val completedCount = logs.count { it.status == "COMPLETED" }
    val inProgressCount = logs.count { it.status == "IN_PROGRESS" }
    val failedCount = logs.count { it.status == "FAILED" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.background)
    ) {
        // 1. Header & Metric Badges
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
                    Column {
                        Text(
                            text = "سجل مهام الذكاء الاصطناعي",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = c.textPrimary
                        )
                        Text(
                            text = "تتبع الإجراءات والمخرجات لكل صفحة متصفح",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textSecondary
                        )
                    }

                    if (logs.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("clear_logs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = "مسح السجل",
                                tint = c.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LogStatBadge(label = "الإجمالي", count = logs.size.toString(), color = c.textPrimary, modifier = Modifier.weight(1f))
                    LogStatBadge(label = "مكتملة", count = completedCount.toString(), color = c.success, modifier = Modifier.weight(1f))
                    LogStatBadge(label = "قيد المعالجة", count = inProgressCount.toString(), color = c.warning, modifier = Modifier.weight(1f))
                    if (failedCount > 0) {
                        LogStatBadge(label = "أخطاء", count = failedCount.toString(), color = c.error, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // 2. Filters & Search Section
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
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث في المهام والمخرجات...", style = MaterialTheme.typography.bodySmall, color = c.textMuted) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(16.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = Icons.Filled.Clear, contentDescription = "مسح", tint = c.textMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("logs_search_input"),
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
                    item {
                        val isSelected = selectedTabFilter == null
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) c.accent else c.cardElevated,
                            border = BorderStroke(1.dp, if (isSelected) c.accent else c.borderSubtle),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setLogsTabFilter(null) }
                        ) {
                            Text(
                                text = "جميع النوافذ (${logs.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else c.textSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    items(tabs) { tab ->
                        val isSelected = selectedTabFilter == tab.id
                        val count = logs.count { it.tabId == tab.id }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) c.accent else c.cardElevated,
                            border = BorderStroke(1.dp, if (isSelected) c.accent else c.borderSubtle),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setLogsTabFilter(tab.id) }
                        ) {
                            Text(
                                text = "${tab.title} ($count)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else c.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusFilterPill(
                        label = "الكل",
                        isSelected = selectedStatusFilter == "ALL",
                        onClick = { viewModel.setLogsStatusFilter("ALL") }
                    )
                    StatusFilterPill(
                        label = "مكتملة ($completedCount)",
                        isSelected = selectedStatusFilter == "COMPLETED",
                        onClick = { viewModel.setLogsStatusFilter("COMPLETED") },
                        activeColor = c.success
                    )
                    if (inProgressCount > 0) {
                        StatusFilterPill(
                            label = "قيد التنفيذ ($inProgressCount)",
                            isSelected = selectedStatusFilter == "IN_PROGRESS",
                            onClick = { viewModel.setLogsStatusFilter("IN_PROGRESS") },
                            activeColor = c.warning
                        )
                    }
                    if (failedCount > 0) {
                        StatusFilterPill(
                            label = "أخطاء ($failedCount)",
                            isSelected = selectedStatusFilter == "FAILED",
                            onClick = { viewModel.setLogsStatusFilter("FAILED") },
                            activeColor = c.error
                        )
                    }
                }
            }
        }

        // 3. Logs List
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = c.textMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (logs.isEmpty()) "لا توجد مهام مسجلة حتى الآن" else "لا توجد نتائج مطابقة للتصفية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "عند تنفيذ أي أمر على صفحات المتصفح يتم تسجيل الحالة والمخرجات هنا تلقائياً",
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
                items(filteredLogs, key = { it.id }) { log ->
                    TabTaskLogItemCard(
                        log = log,
                        onNavigateToTab = { onNavigateToTab(log.tabId) },
                        onRerunTask = {
                            viewModel.updatePromptInput(log.tabId, log.taskPrompt)
                            viewModel.executePromptOnTab(log.tabId, log.taskPrompt)
                            onNavigateToTab(log.tabId)
                        },
                        onDeleteLog = { viewModel.deleteTaskLog(log.id) },
                        onCopyOutput = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("AI Task Output", log.outputResult)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "تم نسخ مخرجات المهمة إلى الحافظة", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = c.card,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "مسح سجل المهام بالكامل؟",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = c.textPrimary
                )
            },
            text = {
                Text(
                    text = "سيتم حذف كافة السجلات والمخرجات السابقة لجميع النوافذ.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllTaskLogs()
                        showClearConfirmDialog = false
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.error, contentColor = Color.White)
                ) {
                    Text("مسح الكل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("إلغاء", color = c.textSecondary)
                }
            }
        )
    }
}

@Composable
fun TabTaskLogItemCard(
    log: TabTaskLogEntity,
    onNavigateToTab: () -> Unit,
    onRerunTask: () -> Unit,
    onDeleteLog: () -> Unit,
    onCopyOutput: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    val statusColor = when (log.status) {
        "COMPLETED" -> c.success
        "IN_PROGRESS" -> c.warning
        "FAILED" -> c.error
        else -> c.textMuted
    }

    val statusLabel = when (log.status) {
        "COMPLETED" -> "مكتمل"
        "IN_PROGRESS" -> "قيد المعالجة"
        "FAILED" -> "خطأ"
        else -> log.status
    }

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val formattedTime = remember(log.timestamp) { dateFormat.format(Date(log.timestamp)) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("log_item_${log.id}"),
        shape = RoundedCornerShape(16.dp),
        color = c.card,
        shadowElevation = if (c.isDark) 0.dp else 1.dp,
        border = BorderStroke(1.dp, c.borderSubtle)
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
                        shape = RoundedCornerShape(8.dp),
                        color = c.accentSoft,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when {
                                    log.tabUrl.contains("x.com") || log.tabUrl.contains("twitter.com") -> Icons.Filled.Tag
                                    log.tabUrl.contains("linkedin.com") -> Icons.Filled.Work
                                    log.tabUrl.contains("instagram.com") -> Icons.Filled.PhotoCamera
                                    else -> Icons.Filled.Language
                                },
                                contentDescription = null,
                                tint = c.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onNavigateToTab() }
                        ) {
                            Text(
                                text = log.tabTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = c.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.OpenInNew,
                                contentDescription = "فتح النافذة",
                                tint = c.textMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textMuted
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onDeleteLog, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "حذف السجل", tint = c.textMuted, modifier = Modifier.size(15.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = c.cardElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "المهمة المطلوبة:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = c.textSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = log.taskPrompt,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textPrimary
                    )
                }
            }

            if (log.outputResult.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = c.cardElevated,
                    border = BorderStroke(1.dp, c.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المخرجات والنتيجة:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = c.accent
                            )
                            IconButton(onClick = onCopyOutput, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = "نسخ", tint = c.textSecondary, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = log.outputResult,
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (log.durationMs > 0) {
                        Text(
                            text = "${log.durationMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textMuted
                        )
                    }
                    if (log.tokensUsed > 0) {
                        Text(
                            text = "${log.tokensUsed} token",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textMuted
                        )
                    }
                }

                TextButton(
                    onClick = onRerunTask,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Replay, contentDescription = null, tint = c.accent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إعادة التنفيذ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = c.accent)
                }
            }
        }
    }
}

@Composable
fun LogStatBadge(
    label: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = c.cardElevated,
        border = BorderStroke(1.dp, c.borderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = c.textMuted)
        }
    }
}

@Composable
fun StatusFilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    activeColor: Color? = null
) {
    val c = AppTheme.colors
    val resolvedColor = activeColor ?: c.accent

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) resolvedColor.copy(alpha = 0.14f) else c.cardElevated,
        border = BorderStroke(1.dp, if (isSelected) resolvedColor else c.borderSubtle),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .defaultMinSize(minHeight = 30.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) resolvedColor else c.textSecondary,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}
