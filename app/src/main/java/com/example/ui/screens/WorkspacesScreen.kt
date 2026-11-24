package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.browser.BrowserTabState
import com.example.browser.TabAnalysisStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun WorkspacesScreen(
    viewModel: MainViewModel,
    onNavigateToBrowserTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning by viewModel.isSystemRunning.collectAsState()
    val statusMessage by viewModel.systemStatusMessage.collectAsState()
    val tabs by viewModel.browserManager.tabs.collectAsState()
    val promptInputs by viewModel.tabPromptInputs.collectAsState()
    val recentPages by viewModel.allRecentPages.collectAsState()
    val context = LocalContext.current

    var showNewTabDialog by remember { mutableStateOf(false) }
    var newTabTitle by remember { mutableStateOf("") }
    var newTabUrl by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Master System Control Card (Linear / Apple style)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(
                    1.dp,
                    if (isRunning) EmeraldGreen.copy(alpha = 0.4f) else CardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("system_control_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            (if (isRunning) EmeraldGreen else AlertRed)
                                                .copy(alpha = if (isRunning) pulseAlpha else 1f)
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isRunning) "نظام الأتمتة: نشط" else "نظام الأتمتة: متوقف",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRunning) EmeraldGreen else TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "تحليل الصفحات وتنفيذ المهام التلقائية لكل نافذة",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }

                        Button(
                            onClick = { viewModel.toggleSystemRunning() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) AlertRedDark else EmeraldDark,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("toggle_system_button")
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRunning) "إيقاف" else "تشغيل",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    if (statusMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CardSurfaceElevated,
                            border = BorderStroke(1.dp, CardBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = if (isRunning) EmeraldGreen else TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = statusMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1.5. Offline Recent Pages Carousel (Room DB)
        if (recentPages.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "صفحات التواصل المزارة أوفلاين",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AccentIndigo.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, CardBorderSubtle)
                            ) {
                                Text(
                                    text = "${recentPages.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = NeonCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Room DB 💾",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(recentPages.take(8), key = { it.id }) { page ->
                            val platformColor = when (page.platform.uppercase()) {
                                "X_TWITTER" -> NeonCyan
                                "LINKEDIN" -> InfoBlue
                                "INSTAGRAM" -> ElectricViolet
                                "REDDIT" -> AccentAmber
                                else -> TextSecondary
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CardSurface,
                                border = BorderStroke(1.dp, if (page.isPinned) AccentAmber.copy(alpha = 0.4f) else CardBorderSubtle),
                                modifier = Modifier
                                    .width(220.dp)
                                    .clickable {
                                        viewModel.openRecentPageInBrowser(page.url, page.title)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = platformColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = page.platform.replace("_", " "),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = platformColor,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (page.isPinned) {
                                            Icon(
                                                imageVector = Icons.Default.PushPin,
                                                contentDescription = null,
                                                tint = AccentAmber,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = page.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = page.url,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Quick Actions Toolbar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "نوافذ المتصفح النشطة (${tabs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { showNewTabDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = CardSurfaceElevated,
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, CardBorder),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("add_tab_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة نافذة", style = MaterialTheme.typography.labelSmall)
                    }

                    FilledTonalButton(
                        onClick = { viewModel.startAutomatedPageAnalysis() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AccentIndigo.copy(alpha = 0.15f),
                            contentColor = NeonCyan
                        ),
                        border = BorderStroke(1.dp, AccentIndigo.copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("analyze_all_tabs_button")
                    ) {
                        Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تحليل الكل", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // 3. Tab Cards or Designed Empty State
        if (tabs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CardSurfaceElevated,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Language,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد نوافذ مفتوحة حالياً",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "أضف نافذة جديدة لبدء التصفح والأتمتة واستخراج البيانات",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showNewTabDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة نافذة جديدة", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        } else {
            items(tabs, key = { it.id }) { tab ->
                val promptText = promptInputs[tab.id] ?: ""

                TabWorkspaceCard(
                    tab = tab,
                    promptText = promptText,
                    onPromptChange = { newText ->
                        viewModel.updatePromptInput(tab.id, newText)
                    },
                    onExecutePrompt = {
                        viewModel.executePromptOnTab(tab.id, promptText)
                    },
                    onOpenInBrowser = {
                        onNavigateToBrowserTab(tab.id)
                    },
                    onAnalyzeTab = {
                        viewModel.executePromptOnTab(tab.id, "تحليل محتوى وتفاعل هذه الصفحة")
                    },
                    onCloseTab = {
                        viewModel.closeTab(tab.id)
                    },
                    onCopyResult = { result ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("مخرجات المهمة", result)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "تم نسخ المخرجات إلى الحافظة", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // New Tab Dialog
    if (showNewTabDialog) {
        AlertDialog(
            onDismissRequest = { showNewTabDialog = false },
            containerColor = CardSurfaceElevated,
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.AddLink, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إضافة نافذة متصفح",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newTabTitle,
                        onValueChange = { newTabTitle = it },
                        label = { Text("اسم النافذة") },
                        placeholder = { Text("مثال: تويتر، إنستغرام، لينكد إن...") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentIndigo,
                            unfocusedBorderColor = CardBorder,
                            focusedContainerColor = CardSurface,
                            unfocusedContainerColor = CardSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTabUrl,
                        onValueChange = { newTabUrl = it },
                        label = { Text("رابط الموقع (URL)") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentIndigo,
                            unfocusedBorderColor = CardBorder,
                            focusedContainerColor = CardSurface,
                            unfocusedContainerColor = CardSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = newTabTitle.ifBlank { "صفحة جديدة" }
                        val url = newTabUrl.ifBlank { "https://x.com" }
                        viewModel.addNewTab(title, url)
                        newTabTitle = ""
                        newTabUrl = ""
                        showNewTabDialog = false
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewTabDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            }
        )
    }
}

/**
 * Individual Tab Card with high-polish status badges and inline execution
 */
@Composable
fun TabWorkspaceCard(
    tab: BrowserTabState,
    promptText: String,
    onPromptChange: (String) -> Unit,
    onExecutePrompt: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onAnalyzeTab: () -> Unit,
    onCloseTab: () -> Unit,
    onCopyResult: (String) -> Unit
) {
    val statusColor = when (tab.analysisStatus) {
        TabAnalysisStatus.COMPLETED -> EmeraldGreen
        TabAnalysisStatus.ANALYZING, TabAnalysisStatus.EXECUTING -> AccentAmber
        TabAnalysisStatus.ERROR -> AlertRed
        else -> TextMuted
    }

    val statusText = when (tab.analysisStatus) {
        TabAnalysisStatus.COMPLETED -> "مكتمل"
        TabAnalysisStatus.ANALYZING -> "جاري التحليل"
        TabAnalysisStatus.EXECUTING -> "جاري التنفيذ"
        TabAnalysisStatus.ERROR -> "خطأ"
        TabAnalysisStatus.READY_FOR_PROMPT -> "جاهز للمهمة"
        TabAnalysisStatus.PENDING -> "في الانتظار"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tab_card_${tab.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Title, URL, Status Badge, Close Button
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
                        color = CardSurfaceElevated,
                        border = BorderStroke(1.dp, CardBorderSubtle),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tab.title.ifBlank { "نافذة تصفح" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = tab.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onCloseTab,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "إغلاق النافذة",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Inline Task Prompt Input Box
            OutlinedTextField(
                value = promptText,
                onValueChange = onPromptChange,
                placeholder = {
                    Text(
                        text = "اكتب مهمة الذكاء الاصطناعي لهذه الصفحة...",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tab_prompt_input_${tab.id}"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentIndigo,
                    unfocusedBorderColor = CardBorder,
                    focusedContainerColor = CardSurfaceElevated,
                    unfocusedContainerColor = CardSurfaceElevated
                ),
                trailingIcon = {
                    IconButton(
                        onClick = onExecutePrompt,
                        enabled = promptText.isNotBlank() && !tab.isExecutingPrompt,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "تنفيذ",
                            tint = if (promptText.isNotBlank()) NeonCyan else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                singleLine = true
            )

            // Analysis / Execution Result Box
            val resultText = when {
                tab.promptResult.isNotBlank() -> tab.promptResult
                tab.pageSummary.isNotBlank() -> tab.pageSummary
                else -> ""
            }

            if (resultText.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CardSurfaceElevated,
                    border = BorderStroke(1.dp, CardBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مخرجات العملية:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            IconButton(
                                onClick = { onCopyResult(resultText) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "نسخ",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = resultText,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Actions: Open in Live Browser & Analyze Now
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenInBrowser,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("open_in_browser_button_${tab.id}")
                ) {
                    Icon(imageVector = Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("عرض في المتصفح", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }

                Button(
                    onClick = onAnalyzeTab,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("analyze_tab_button_${tab.id}")
                ) {
                    Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تحليل فوري", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
