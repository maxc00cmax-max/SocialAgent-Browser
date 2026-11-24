package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    onOpenCommandHub: () -> Unit = {},
    onOpenOpenGraphScraper: (String) -> Unit = {},
    onOpenAppBridge: () -> Unit = {},
    onViewAllRecentPages: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    val isRunning by viewModel.isSystemRunning.collectAsState()
    val statusMessage by viewModel.systemStatusMessage.collectAsState()
    val tabs by viewModel.browserManager.tabs.collectAsState()
    val promptInputs by viewModel.tabPromptInputs.collectAsState()
    val recentPages by viewModel.allRecentPages.collectAsState()
    val totalTokens by viewModel.totalTokens.collectAsState()
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
            .background(c.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hero Automation Control Card (Light-first airy card with soft gradient)
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent,
                border = BorderStroke(
                    1.dp,
                    if (isRunning) c.success.copy(alpha = 0.45f) else c.border
                ),
                shadowElevation = if (c.isDark) 0.dp else 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(listOf(c.heroStart, c.heroEnd)),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .testTag("system_control_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(
                                            (if (isRunning) c.success else c.textMuted)
                                                .copy(alpha = if (isRunning) pulseAlpha else 1f)
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isRunning) "محرك الأتمتة نشط" else "جاهز للعمل",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRunning) c.success else c.textPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "تحليل الصفحات وتنفيذ الأوامر الذكية على النوافذ",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { viewModel.toggleSystemRunning() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) c.error else c.accent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(42.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("toggle_system_button")
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRunning) "إيقاف" else "تشغيل",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    // Live Overview Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WorkspaceStatChip(
                            label = "النوافذ",
                            value = "${tabs.size}",
                            tint = c.accent,
                            modifier = Modifier.weight(1f)
                        )
                        WorkspaceStatChip(
                            label = "أوفلاين",
                            value = "${recentPages.size}",
                            tint = c.success,
                            modifier = Modifier.weight(1f)
                        )
                        WorkspaceStatChip(
                            label = "التوكنز",
                            value = "${totalTokens ?: 0}",
                            tint = c.warning,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (statusMessage.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = c.surface.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, c.borderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = if (isRunning) c.success else c.accent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = statusMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.textSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1.2 Quick Shortcuts Bar (Easy 1-tap access to key workflows)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionPill(
                    label = "نافذة جديدة",
                    icon = Icons.Filled.Add,
                    tint = c.accent,
                    onClick = { showNewTabDialog = true },
                    testTag = "quick_add_tab_pill"
                )
                QuickActionPill(
                    label = "مستخرج OpenGraph",
                    icon = Icons.Filled.TravelExplore,
                    tint = c.accentSecondary,
                    onClick = { onOpenOpenGraphScraper("") },
                    testTag = "quick_opengraph_pill"
                )
                QuickActionPill(
                    label = "مركز الأوامر",
                    icon = Icons.Filled.Tune,
                    tint = ElectricViolet,
                    onClick = onOpenCommandHub,
                    testTag = "quick_command_hub_pill"
                )
                QuickActionPill(
                    label = "جسر التطبيقات",
                    icon = Icons.Filled.SyncAlt,
                    tint = c.success,
                    onClick = onOpenAppBridge,
                    testTag = "quick_bridge_pill"
                )
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
                                text = "المزارة مؤخراً (أوفلاين)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = c.textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = c.accentSoft
                            ) {
                                Text(
                                    text = "${recentPages.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = c.accent,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        TextButton(
                            onClick = onViewAllRecentPages,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "عرض الكل",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = c.accent
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Filled.ChevronLeft,
                                contentDescription = null,
                                tint = c.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(recentPages.take(8), key = { it.id }) { page ->
                            val platformColor = when (page.platform.uppercase()) {
                                "X_TWITTER" -> c.accentSecondary
                                "LINKEDIN" -> InfoBlue
                                "INSTAGRAM" -> ElectricViolet
                                "REDDIT" -> c.warning
                                else -> c.textSecondary
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = c.card,
                                shadowElevation = if (c.isDark) 0.dp else 1.dp,
                                border = BorderStroke(
                                    1.dp,
                                    if (page.isPinned) c.warning.copy(alpha = 0.5f) else c.borderSubtle
                                ),
                                modifier = Modifier
                                    .width(220.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        viewModel.openRecentPageInBrowser(page.url, page.title)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = platformColor.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = page.platform.replace("_", " "),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = platformColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (page.isPinned) {
                                            Icon(
                                                imageVector = Icons.Default.PushPin,
                                                contentDescription = "مثبتة",
                                                tint = c.warning,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = page.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = c.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = page.url,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp
                                        ),
                                        color = c.textMuted,
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

        // 2. Active Browser Workspaces Header
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
                    color = c.textPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { showNewTabDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = c.card,
                            contentColor = c.textPrimary
                        ),
                        border = BorderStroke(1.dp, c.border),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("add_tab_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة", style = MaterialTheme.typography.labelMedium)
                    }

                    FilledTonalButton(
                        onClick = { viewModel.startAutomatedPageAnalysis() },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = c.accentSoft,
                            contentColor = c.accent
                        ),
                        border = BorderStroke(1.dp, c.accent.copy(alpha = 0.25f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("analyze_all_tabs_button")
                    ) {
                        Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تحليل الكل", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // 3. Tab Cards or Friendly Empty State
        if (tabs.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = c.card,
                    border = BorderStroke(1.dp, c.borderSubtle),
                    shadowElevation = if (c.isDark) 0.dp else 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 38.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = c.accentSoft,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Language,
                                    contentDescription = null,
                                    tint = c.accent,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد نوافذ مفتوحة حالياً",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = c.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "أضف نافذة جديدة لبدء التصفح والأتمتة واستخراج البيانات",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textMuted
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showNewTabDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة نافذة جديدة", style = MaterialTheme.typography.labelLarge)
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
            containerColor = c.card,
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.AddLink, contentDescription = null, tint = c.accent, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إضافة نافذة متصفح",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary
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
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = c.accent,
                            unfocusedBorderColor = c.border,
                            focusedContainerColor = c.cardElevated,
                            unfocusedContainerColor = c.cardElevated,
                            focusedTextColor = c.textPrimary,
                            unfocusedTextColor = c.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTabUrl,
                        onValueChange = { newTabUrl = it },
                        label = { Text("رابط الموقع (URL)") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = c.accent,
                            unfocusedBorderColor = c.border,
                            focusedContainerColor = c.cardElevated,
                            unfocusedContainerColor = c.cardElevated,
                            focusedTextColor = c.textPrimary,
                            unfocusedTextColor = c.textPrimary
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
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White)
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewTabDialog = false }) {
                    Text("إلغاء", color = c.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun WorkspaceStatChip(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = c.surface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, c.borderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = c.textSecondary
            )
        }
    }
}

@Composable
private fun QuickActionPill(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val c = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = c.card,
        border = BorderStroke(1.dp, c.borderSubtle),
        shadowElevation = if (c.isDark) 0.dp else 1.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = c.textPrimary
            )
        }
    }
}

/**
 * Individual Tab Card with crisp Light-first spacing, inline quick prompts, and actions
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
    val c = AppTheme.colors
    val statusColor = when (tab.analysisStatus) {
        TabAnalysisStatus.COMPLETED -> c.success
        TabAnalysisStatus.ANALYZING, TabAnalysisStatus.EXECUTING -> c.warning
        TabAnalysisStatus.ERROR -> c.error
        TabAnalysisStatus.READY_FOR_PROMPT -> c.accent
        else -> c.textMuted
    }

    val statusText = when (tab.analysisStatus) {
        TabAnalysisStatus.COMPLETED -> "مكتمل"
        TabAnalysisStatus.ANALYZING -> "جاري التحليل"
        TabAnalysisStatus.EXECUTING -> "جاري التنفيذ"
        TabAnalysisStatus.ERROR -> "خطأ"
        TabAnalysisStatus.READY_FOR_PROMPT -> "جاهز للمهمة"
        TabAnalysisStatus.PENDING -> "في الانتظار"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = c.card,
        border = BorderStroke(1.dp, c.borderSubtle),
        shadowElevation = if (c.isDark) 0.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tab_card_${tab.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Icon, Title, URL, Status Badge, Close Button
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
                        shape = RoundedCornerShape(10.dp),
                        color = c.accentSoft,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = null,
                                tint = c.accent,
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
                            color = c.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = tab.url,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = c.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onCloseTab,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "إغلاق النافذة",
                            tint = c.textMuted,
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
                        color = c.textMuted
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tab_prompt_input_${tab.id}"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent,
                    unfocusedBorderColor = c.border,
                    focusedContainerColor = c.cardElevated,
                    unfocusedContainerColor = c.cardElevated,
                    focusedTextColor = c.textPrimary,
                    unfocusedTextColor = c.textPrimary
                ),
                trailingIcon = {
                    IconButton(
                        onClick = onExecutePrompt,
                        enabled = promptText.isNotBlank() && !tab.isExecutingPrompt,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "تنفيذ",
                            tint = if (promptText.isNotBlank()) c.accent else c.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                singleLine = true
            )

            // 1-Tap Prompt Presets
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("تلخيص الصفحة", "استخراج المنشورات", "تحليل التفاعل").forEach { preset ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = c.cardElevated,
                        border = BorderStroke(1.dp, c.borderSubtle),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPromptChange(preset) }
                    ) {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Analysis / Execution Result Box
            val resultText = when {
                tab.promptResult.isNotBlank() -> tab.promptResult
                tab.pageSummary.isNotBlank() -> tab.pageSummary
                else -> ""
            }

            if (resultText.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
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
                                text = "مخرجات العملية:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = c.accent
                            )
                            IconButton(
                                onClick = { onCopyResult(resultText) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "نسخ",
                                    tint = c.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = resultText,
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textPrimary,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Actions: Open in Live Browser & Analyze Now
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenInBrowser,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, c.border),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("open_in_browser_button_${tab.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = c.textSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فتح في المتصفح", style = MaterialTheme.typography.labelMedium, color = c.textPrimary)
                }

                Button(
                    onClick = onAnalyzeTab,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("analyze_tab_button_${tab.id}")
                ) {
                    Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تحليل فوري", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
