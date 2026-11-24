package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.browser.BrowserTabState
import com.example.database.entity.TabTaskLogEntity
import com.example.ui.MainViewModel
import com.example.ui.components.OfflineRecentPagesView
import com.example.ui.components.SocialWebView
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
    onOpenOpenGraphScraper: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    val tabs by viewModel.browserManager.tabs.collectAsState()
    val activeTabId by viewModel.browserManager.activeTabId.collectAsState()
    val promptInputs by viewModel.tabPromptInputs.collectAsState()
    val allLogs by viewModel.allTabTaskLogs.collectAsState()
    val recentPages by viewModel.allRecentPages.collectAsState()
    val viewMode by viewModel.browserViewMode.collectAsState()
    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull()

    var urlInput by remember(activeTab?.url) { mutableStateOf(activeTab?.url ?: "") }
    var showNewTabDialog by remember { mutableStateOf(false) }
    var newTabTitle by remember { mutableStateOf("") }
    var newTabUrl by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.background)
    ) {
        // 1. Clean Horizontally-Scrollable Segmented Mode Header
        Surface(
            color = c.surface,
            border = BorderStroke(1.dp, c.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BrowserModePill(
                        label = "القائمة (${tabs.size})",
                        icon = Icons.Filled.List,
                        selected = viewMode == BrowserViewMode.BROWSER_LIST,
                        onClick = { viewModel.setBrowserViewMode(BrowserViewMode.BROWSER_LIST) },
                        testTag = "browser_list_mode_tab"
                    )
                    BrowserModePill(
                        label = "المتصفح الحي",
                        icon = Icons.Filled.Language,
                        selected = viewMode == BrowserViewMode.LIVE_WEB_VIEW,
                        onClick = { viewModel.setBrowserViewMode(BrowserViewMode.LIVE_WEB_VIEW) },
                        testTag = "browser_live_mode_tab"
                    )
                    BrowserModePill(
                        label = "أوفلاين (${recentPages.size})",
                        icon = Icons.Filled.Bookmarks,
                        selected = viewMode == BrowserViewMode.OFFLINE_RECENT_PAGES,
                        onClick = { viewModel.setBrowserViewMode(BrowserViewMode.OFFLINE_RECENT_PAGES) },
                        testTag = "browser_recent_pages_mode_tab"
                    )
                    BrowserModePill(
                        label = "السجل (${allLogs.size})",
                        icon = Icons.Filled.History,
                        selected = viewMode == BrowserViewMode.TASK_LOGS,
                        onClick = { viewModel.setBrowserViewMode(BrowserViewMode.TASK_LOGS) },
                        testTag = "browser_logs_mode_tab"
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = { showNewTabDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = c.accentSoft,
                        contentColor = c.accent
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("browser_add_page_button")
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("نافذة", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
                        viewModel.setBrowserViewMode(BrowserViewMode.LIVE_WEB_VIEW)
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
                        viewModel.setBrowserViewMode(BrowserViewMode.TASK_LOGS)
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
                    onSwitchToBrowserList = { viewModel.setBrowserViewMode(BrowserViewMode.BROWSER_LIST) },
                    onOpenOpenGraphScraper = {
                        onOpenOpenGraphScraper(activeTab?.url ?: urlInput)
                    },
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
                        viewModel.setBrowserViewMode(BrowserViewMode.LIVE_WEB_VIEW)
                    },
                    onOpenOpenGraphScraper = onOpenOpenGraphScraper
                )
            }

            BrowserViewMode.TASK_LOGS -> {
                TaskLogsScreen(
                    viewModel = viewModel,
                    onNavigateToTab = { tabId ->
                        viewModel.selectTab(tabId)
                        viewModel.setBrowserViewMode(BrowserViewMode.LIVE_WEB_VIEW)
                    }
                )
            }
        }
    }

    // Quick Add Tab Dialog
    if (showNewTabDialog) {
        AlertDialog(
            onDismissRequest = { showNewTabDialog = false },
            containerColor = c.card,
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.AddLink, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إضافة نافذة جديدة",
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
                        placeholder = { Text("مثال: تويتر، لينكد إن...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTabUrl,
                        onValueChange = { newTabUrl = it },
                        label = { Text("رابط الموقع (URL)") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
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
private fun BrowserModePill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val c = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) c.accent else c.cardElevated,
        border = BorderStroke(1.dp, if (selected) c.accent else c.borderSubtle),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else c.textSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else c.textSecondary
            )
        }
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
    val c = AppTheme.colors
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
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = c.card,
                border = BorderStroke(1.dp, c.borderSubtle),
                shadowElevation = if (c.isDark) 0.dp else 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث في النوافذ المفتوحة...", style = MaterialTheme.typography.bodySmall, color = c.textMuted) },
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
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) c.accent else c.cardElevated,
                                border = BorderStroke(1.dp, if (isSelected) c.accent else c.borderSubtle),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedPlatformFilter = key }
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
        }

        if (filteredTabs.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = c.card,
                    border = BorderStroke(1.dp, c.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Outlined.SearchOff, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(34.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("لا توجد نوافذ مطابقة لنتائج البحث", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                    }
                }
            }
        } else {
            items(filteredTabs, key = { it.id }) { tab ->
                val isSelected = tab.id == activeTabId
                val currentTask = promptInputs[tab.id] ?: ""

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = c.card,
                    shadowElevation = if (c.isDark) 0.dp else 1.dp,
                    border = BorderStroke(1.dp, if (isSelected) c.accent else c.borderSubtle),
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
                                    shape = RoundedCornerShape(10.dp),
                                    color = c.accentSoft,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Language,
                                            contentDescription = null,
                                            tint = c.accent,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tab.title,
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
                                IconButton(
                                    onClick = { onSelectTab(tab.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.OpenInBrowser,
                                        contentDescription = "فتح في المتصفح",
                                        tint = c.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onCloseTab(tab.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "إغلاق",
                                        tint = c.textMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = currentTask,
                            onValueChange = { onTaskChange(tab.id, it) },
                            placeholder = { Text("اكتب مهمة الذكاء الاصطناعي لهذه الصفحة...", style = MaterialTheme.typography.bodySmall, color = c.textMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
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
                                    onClick = { onExecuteTask(tab.id, currentTask) },
                                    enabled = currentTask.isNotBlank() && !tab.isExecutingPrompt,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Send,
                                        contentDescription = "تنفيذ",
                                        tint = if (currentTask.isNotBlank()) c.accent else c.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )

                        val resultText = when {
                            tab.promptResult.isNotBlank() -> tab.promptResult
                            tab.pageSummary.isNotBlank() -> tab.pageSummary
                            else -> ""
                        }

                        if (resultText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = c.cardElevated,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = resultText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.textPrimary,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

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
    onOpenOpenGraphScraper: () -> Unit = {},
    activeTabTask: String,
    onActiveTabTaskChange: (String) -> Unit,
    onExecuteActiveTabTask: (String) -> Unit,
    viewModel: MainViewModel
) {
    val c = AppTheme.colors
    var isTaskBarExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Strip Header
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(tabs, key = { it.id }) { tab ->
                val isSelected = tab.id == activeTab?.id
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) c.accentSoft else c.cardElevated,
                    border = BorderStroke(1.dp, if (isSelected) c.accent else c.borderSubtle),
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
                            color = if (isSelected) c.accent else c.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 110.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { onCloseTab(tab.id) },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "إغلاق",
                                tint = c.textMuted,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            item {
                IconButton(
                    onClick = onAddNewTab,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "نافذة جديدة",
                        tint = c.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // URL Address Bar + 1-Tap OpenGraph Extract
        Surface(
            color = c.surface,
            border = BorderStroke(1.dp, c.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = onUrlChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = c.accent,
                        unfocusedBorderColor = c.border,
                        focusedContainerColor = c.cardElevated,
                        unfocusedContainerColor = c.cardElevated,
                        focusedTextColor = c.textPrimary,
                        unfocusedTextColor = c.textPrimary
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = c.success,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = onNavigateUrl,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = "انتقال",
                                tint = c.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onOpenOpenGraphScraper,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.accentSoft)
                        .testTag("live_browser_opengraph_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.TravelExplore,
                        contentDescription = "استخراج OpenGraph وحفظه",
                        tint = c.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = onSwitchToBrowserList,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("المهام", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Active Tab Prompt Bar
        if (activeTab != null) {
            Surface(
                color = c.cardElevated,
                border = BorderStroke(1.dp, c.borderSubtle),
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
                                tint = c.accent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مهمة الصفحة: ${activeTab.title}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = c.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        TextButton(
                            onClick = { isTaskBarExpanded = !isTaskBarExpanded },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isTaskBarExpanded) "تصغير" else "تعديل المهمة",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = c.accent
                            )
                        }
                    }

                    AnimatedVisibility(visible = isTaskBarExpanded) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = activeTabTask,
                                onValueChange = onActiveTabTaskChange,
                                placeholder = { Text("اكتب مهمة الذكاء الاصطناعي لهذه الصفحة...", style = MaterialTheme.typography.bodySmall, color = c.textMuted) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("active_tab_task_input"),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = c.accent,
                                    unfocusedBorderColor = c.border,
                                    focusedContainerColor = c.card,
                                    unfocusedContainerColor = c.card,
                                    focusedTextColor = c.textPrimary,
                                    unfocusedTextColor = c.textPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onExecuteActiveTabTask(activeTabTask) },
                                enabled = activeTabTask.isNotBlank() && !activeTab.isExecutingPrompt,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Text("تنفيذ", style = MaterialTheme.typography.labelMedium)
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
                    Text("لا توجد صفحات مفتوحة", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                }
            }
        }
    }
}
