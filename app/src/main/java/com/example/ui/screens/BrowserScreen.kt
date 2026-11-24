package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.browser.BrowserTabState
import com.example.browser.TabAnalysisStatus
import com.example.database.entity.TabTaskLogEntity
import com.example.ui.MainViewModel
import com.example.ui.components.SocialWebView
import com.example.ui.components.OfflineRecentPagesView
import com.example.ui.theme.*

enum class BrowserViewMode {
    BROWSER_LIST,
    LIVE_WEB_VIEW,
    OFFLINE_RECENT_PAGES,
    TASK_LOGS
}

@Composable
fun BrowserScreen(
    viewModel: MainViewModel,
    onNavigateToWorkspaces: () -> Unit,
    onNavigateToLogs: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tabs by viewModel.browserManager.tabs.collectAsState()
    val activeTabId by viewModel.browserManager.activeTabId.collectAsState()
    val promptInputs by viewModel.tabPromptInputs.collectAsState()
    val allLogs by viewModel.allTabTaskLogs.collectAsState()
    val recentPages by viewModel.allRecentPages.collectAsState()
    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull()

    var viewMode by remember { mutableStateOf(BrowserViewMode.BROWSER_LIST) }
    var urlInput by remember(activeTab?.url) { mutableStateOf(activeTab?.url ?: "") }
    var showNewTabDialog by remember { mutableStateOf(false) }
    var newTabTitle by remember { mutableStateOf("") }
    var newTabUrl by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // 1. Top Segmented Navigation (Linear style)
        Surface(
            color = DarkSurface,
            border = BorderStroke(1.dp, CardBorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Segmented Mode Selector
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardSurface)
                        .border(1.dp, CardBorderSubtle, RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Browser List Mode
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = if (viewMode == BrowserViewMode.BROWSER_LIST) AccentIndigo else Color.Transparent,
                        modifier = Modifier
                            .clickable { viewMode = BrowserViewMode.BROWSER_LIST }
                            .testTag("browser_list_mode_tab")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.List,
                                contentDescription = null,
                                tint = if (viewMode == BrowserViewMode.BROWSER_LIST) Color.White else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "القائمة (${tabs.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (viewMode == BrowserViewMode.BROWSER_LIST) FontWeight.Bold else FontWeight.Medium,
                                color = if (viewMode == BrowserViewMode.BROWSER_LIST) Color.White else TextSecondary
                            )
                        }
                    }

                    // Live Web View Mode
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = if (viewMode == BrowserViewMode.LIVE_WEB_VIEW) AccentIndigo else Color.Transparent,
                        modifier = Modifier
                            .clickable { viewMode = BrowserViewMode.LIVE_WEB_VIEW }
                            .testTag("browser_live_mode_tab")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = null,
                                tint = if (viewMode == BrowserViewMode.LIVE_WEB_VIEW) Color.White else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "المتصفح الحي",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (viewMode == BrowserViewMode.LIVE_WEB_VIEW) FontWeight.Bold else FontWeight.Medium,
                                color = if (viewMode == BrowserViewMode.LIVE_WEB_VIEW) Color.White else TextSecondary
                            )
                        }
                    }

                    // Offline Recent Pages Mode
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = if (viewMode == BrowserViewMode.OFFLINE_RECENT_PAGES) AccentIndigo else Color.Transparent,
                        modifier = Modifier
                            .clickable { viewMode = BrowserViewMode.OFFLINE_RECENT_PAGES }
                            .testTag("browser_recent_pages_mode_tab")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bookmarks,
                                contentDescription = null,
                                tint = if (viewMode == BrowserViewMode.OFFLINE_RECENT_PAGES) Color.White else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "أوفلاين (${recentPages.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (viewMode == BrowserViewMode.OFFLINE_RECENT_PAGES) FontWeight.Bold else FontWeight.Medium,
                                color = if (viewMode == BrowserViewMode.OFFLINE_RECENT_PAGES) Color.White else TextSecondary
                            )
                        }
                    }

                    // Task Logs Mode
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = if (viewMode == BrowserViewMode.TASK_LOGS) AccentIndigo else Color.Transparent,
                        modifier = Modifier
                            .clickable { viewMode = BrowserViewMode.TASK_LOGS }
                            .testTag("browser_logs_mode_tab")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = if (viewMode == BrowserViewMode.TASK_LOGS) Color.White else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "السجل (${allLogs.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (viewMode == BrowserViewMode.TASK_LOGS) FontWeight.Bold else FontWeight.Medium,
                                color = if (viewMode == BrowserViewMode.TASK_LOGS) Color.White else TextSecondary
                            )
                        }
                    }
                }

                // Add Page Button
                FilledTonalButton(
                    onClick = { showNewTabDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CardSurfaceElevated,
                        contentColor = TextPrimary
                    ),
                    border = BorderStroke(1.dp, CardBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("browser_add_page_button")
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نافذة", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // 2. View Mode Content
        when (viewMode) {
            BrowserViewMode.BROWSER_LIST -> {
                BrowserListPageEntriesView(
                    tabs = tabs,
                    activeTabId = activeTabId,
                    promptInputs = promptInputs,
                    allLogs = allLogs,
                    onSelectTab = { tabId ->
                        viewModel.selectTab(tabId)
                        viewMode = BrowserViewMode.LIVE_WEB_VIEW
                    },
                    onCloseTab = { viewModel.closeTab(it) },
                    onTaskChange = { tabId, taskText ->
                        viewModel.updatePromptInput(tabId, taskText)
                    },
                    onExecuteTask = { tabId, taskText ->
                        viewModel.executePromptOnTab(tabId, taskText)
                    },
                    onApplyPreset = { tabId, preset ->
                        viewModel.updatePromptInput(tabId, preset)
                    },
                    onAddNewTab = { showNewTabDialog = true },
                    onViewTabLogs = { tabId ->
                        viewModel.selectTab(tabId)
                        viewMode = BrowserViewMode.TASK_LOGS
                    },
                    onExecuteBatchTask = { prompt ->
                        viewModel.executeUniversalPromptOnAllTabs(prompt)
                    }
                )
            }

            BrowserViewMode.LIVE_WEB_VIEW -> {
                BrowserLiveWebView(
                    tabs = tabs,
                    activeTab = activeTab,
                    urlInput = urlInput,
                    onUrlChange = { urlInput = it },
                    onNavigateUrl = { viewModel.browserManager.loadUrlInActiveTab(urlInput) },
                    onSelectTab = { viewModel.selectTab(it) },
                    onCloseTab = { viewModel.closeTab(it) },
                    onAddNewTab = { showNewTabDialog = true },
                    onSwitchToBrowserList = { viewMode = BrowserViewMode.BROWSER_LIST },
                    activeTabTask = if (activeTab != null) promptInputs[activeTab.id] ?: "" else "",
                    onActiveTabTaskChange = { task ->
                        activeTab?.let { viewModel.updatePromptInput(it.id, task) }
                    },
                    onExecuteActiveTabTask = { task ->
                        activeTab?.let { viewModel.executePromptOnTab(it.id, task) }
                    },
                    viewModel = viewModel
                )
            }

            BrowserViewMode.OFFLINE_RECENT_PAGES -> {
                OfflineRecentPagesView(
                    viewModel = viewModel,
                    onOpenInBrowser = { url, title ->
                        viewModel.openRecentPageInBrowser(url, title)
                        viewMode = BrowserViewMode.LIVE_WEB_VIEW
                    }
                )
            }

            BrowserViewMode.TASK_LOGS -> {
                TaskLogsScreen(
                    viewModel = viewModel,
                    onNavigateToTab = { tabId ->
                        viewModel.selectTab(tabId)
                        viewMode = BrowserViewMode.LIVE_WEB_VIEW
                    }
                )
            }
        }
    }

    // Quick Add Tab Dialog
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
                        text = "إضافة نافذة جديدة",
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
                        placeholder = { Text("مثال: تويتر، لينكد إن...") },
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

@Composable
fun BrowserListPageEntriesView(
    tabs: List<BrowserTabState>,
    activeTabId: String?,
    promptInputs: Map<String, String>,
    allLogs: List<TabTaskLogEntity>,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onTaskChange: (String, String) -> Unit,
    onExecuteTask: (String, String) -> Unit,
    onApplyPreset: (String, String) -> Unit,
    onAddNewTab: () -> Unit,
    onViewTabLogs: (String) -> Unit,
    onExecuteBatchTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPlatformFilter by remember { mutableStateOf("ALL") }

    val filteredTabs = remember(tabs, searchQuery, selectedPlatformFilter) {
        tabs.filter { tab ->
            val matchesSearch = searchQuery.isBlank() ||
                    tab.title.contains(searchQuery, ignoreCase = true) ||
                    tab.url.contains(searchQuery, ignoreCase = true)

            val matchesPlatform = when (selectedPlatformFilter) {
                "X" -> tab.url.contains("x.com") || tab.url.contains("twitter.com")
                "LINKEDIN" -> tab.url.contains("linkedin.com")
                "INSTAGRAM" -> tab.url.contains("instagram.com")
                "REDDIT" -> tab.url.contains("reddit.com")
                else -> true
            }

            matchesSearch && matchesPlatform
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search & Platform Filters Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث في النوافذ المفتوحة...", style = MaterialTheme.typography.bodySmall, color = TextMuted) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Filled.Clear, contentDescription = "مسح", tint = TextMuted, modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentIndigo,
                            unfocusedBorderColor = CardBorder,
                            focusedContainerColor = CardSurfaceElevated,
                            unfocusedContainerColor = CardSurfaceElevated
                        )
                    )

                    // Platform Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filters = listOf(
                            "ALL" to "الكل (${tabs.size})",
                            "X" to "X (تويتر)",
                            "LINKEDIN" to "LinkedIn",
                            "INSTAGRAM" to "Instagram",
                            "REDDIT" to "Reddit"
                        )
                        items(filters) { (key, label) ->
                            val isSelected = selectedPlatformFilter == key
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) AccentIndigo.copy(alpha = 0.2f) else CardSurfaceElevated,
                                border = BorderStroke(1.dp, if (isSelected) AccentIndigo else CardBorderSubtle),
                                modifier = Modifier
                                    .clickable { selectedPlatformFilter = key }
                                    .defaultMinSize(minHeight = 32.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) NeonCyan else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tabs List or Empty State
        if (filteredTabs.isEmpty()) {
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
                            .padding(vertical = 32.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Outlined.SearchOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا توجد نوافذ مطابقة لنتائج البحث", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        } else {
            items(filteredTabs, key = { it.id }) { tab ->
                val isSelected = tab.id == activeTabId
                val currentTask = promptInputs[tab.id] ?: ""

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = BorderStroke(1.dp, if (isSelected) AccentIndigo.copy(alpha = 0.5f) else CardBorder),
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
                                    shape = RoundedCornerShape(8.dp),
                                    color = CardSurfaceElevated,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Language,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tab.title,
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
                                IconButton(
                                    onClick = { onSelectTab(tab.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.OpenInBrowser,
                                        contentDescription = "فتح في المتصفح",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onCloseTab(tab.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "إغلاق",
                                        tint = TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Task input for this page
                        OutlinedTextField(
                            value = currentTask,
                            onValueChange = { onTaskChange(tab.id, it) },
                            placeholder = { Text("اكتب مهمة الذكاء الاصطناعي لهذه الصفحة...", style = MaterialTheme.typography.bodySmall, color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentIndigo,
                                unfocusedBorderColor = CardBorder,
                                focusedContainerColor = CardSurfaceElevated,
                                unfocusedContainerColor = CardSurfaceElevated
                            ),
                            trailingIcon = {
                                IconButton(
                                    onClick = { onExecuteTask(tab.id, currentTask) },
                                    enabled = currentTask.isNotBlank() && !tab.isExecutingPrompt,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Send,
                                        contentDescription = "تنفيذ",
                                        tint = if (currentTask.isNotBlank()) NeonCyan else TextMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        )

                        // Latest Result Box if any
                        val resultText = when {
                            tab.promptResult.isNotBlank() -> tab.promptResult
                            tab.pageSummary.isNotBlank() -> tab.pageSummary
                            else -> ""
                        }

                        if (resultText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CardSurfaceElevated,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = resultText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Live WebView with refined address bar & docked bottom action bar
 */
@Composable
fun BrowserLiveWebView(
    tabs: List<BrowserTabState>,
    activeTab: BrowserTabState?,
    urlInput: String,
    onUrlChange: (String) -> Unit,
    onNavigateUrl: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onAddNewTab: () -> Unit,
    onSwitchToBrowserList: () -> Unit,
    activeTabTask: String,
    onActiveTabTaskChange: (String) -> Unit,
    onExecuteActiveTabTask: (String) -> Unit,
    viewModel: MainViewModel
) {
    var isTaskBarExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Strip Header
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(tabs, key = { it.id }) { tab ->
                val isSelected = tab.id == activeTab?.id
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) CardSurfaceElevated else CardSurface,
                    border = BorderStroke(1.dp, if (isSelected) AccentIndigo else CardBorderSubtle),
                    modifier = Modifier.clickable { onSelectTab(tab.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) NeonCyan else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 110.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { onCloseTab(tab.id) },
                            modifier = Modifier.size(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "إغلاق",
                                tint = TextMuted,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }

            item {
                IconButton(
                    onClick = onAddNewTab,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "نافذة جديدة",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // URL Address Bar
        Surface(
            color = CardSurface,
            border = BorderStroke(1.dp, CardBorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = onUrlChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentIndigo,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = CardSurfaceElevated,
                        unfocusedContainerColor = CardSurfaceElevated
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = onNavigateUrl,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = "انتقال",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onSwitchToBrowserList,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("المهام", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Active Tab Prompt Drawer (Docked)
        if (activeTab != null) {
            Surface(
                color = CardSurfaceElevated,
                border = BorderStroke(1.dp, CardBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مهمة الصفحة: ${activeTab.title}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        TextButton(
                            onClick = { isTaskBarExpanded = !isTaskBarExpanded },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isTaskBarExpanded) "تصغير" else "تعديل المهمة",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                        }
                    }

                    AnimatedVisibility(visible = isTaskBarExpanded) {
                        Column(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = activeTabTask,
                                    onValueChange = onActiveTabTaskChange,
                                    placeholder = { Text("اكتب مهمة الذكاء الاصطناعي لهذه الصفحة...", style = MaterialTheme.typography.bodySmall, color = TextMuted) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("active_tab_task_input"),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentIndigo,
                                        unfocusedBorderColor = CardBorder,
                                        focusedContainerColor = CardSurface,
                                        unfocusedContainerColor = CardSurface
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onExecuteActiveTabTask(activeTabTask) },
                                    enabled = activeTabTask.isNotBlank() && !activeTab.isExecutingPrompt,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text("تنفيذ", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live WebView Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (activeTab != null) {
                val webView = viewModel.browserManager.getOrCreateWebView(activeTab.id)
                SocialWebView(webView = webView)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد صفحات مفتوحة", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
        }
    }
}
