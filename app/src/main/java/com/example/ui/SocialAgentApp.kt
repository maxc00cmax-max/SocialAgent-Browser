package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppBridgeDialog
import com.example.ui.components.MainAppFeaturesDrawerSheet
import com.example.ui.components.OfflineRecentPagesView
import com.example.ui.components.OpenGraphScraperDialog
import com.example.ui.components.SectionSideDrawerOverlay
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.BrowserViewMode
import com.example.ui.screens.DataMarketingTabScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TaskLogsScreen
import com.example.ui.screens.WorkspacesScreen
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class AppDestination(val route: String, val title: String) {
    WORKSPACES("workspaces", "المساحات"),
    BROWSER("browser", "المتصفح"),
    RECENT_PAGES("recent_pages", "المزارة"),
    DATABASE("database", "البيانات"),
    LOGS("logs", "السجل"),
    ANALYTICS("analytics", "الإحصاءات"),
    SETTINGS("settings", "الإعدادات")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialAgentApp(viewModel: MainViewModel) {
    val c = AppTheme.colors
    var currentDestination by remember { mutableStateOf(AppDestination.WORKSPACES) }
    val isRunning by viewModel.isSystemRunning.collectAsState()
    val totalTokens by viewModel.totalTokens.collectAsState()
    val tabs by viewModel.browserManager.tabs.collectAsState()
    val recentPages by viewModel.allRecentPages.collectAsState()
    val logs by viewModel.allTabTaskLogs.collectAsState()
    val posts by viewModel.allScrapedPosts.collectAsState()

    var showCommandHubSheet by remember { mutableStateOf(false) }
    var showAppBridgeDialog by remember { mutableStateOf(false) }
    var showQuickAddTabDialog by remember { mutableStateOf(false) }
    var showOpenGraphDialog by remember { mutableStateOf(false) }
    var openGraphInitialUrl by remember { mutableStateOf("") }
    var quickTabTitle by remember { mutableStateOf("") }
    var quickTabUrl by remember { mutableStateOf("") }

    val mainDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var isSectionDrawerOpen by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val openOpenGraphScraper: (String) -> Unit = { url ->
        viewModel.resetOpenGraphScrapeState()
        openGraphInitialUrl = url
        showOpenGraphDialog = true
    }

    BackHandler(enabled = mainDrawerState.isOpen) {
        coroutineScope.launch { mainDrawerState.close() }
    }

    BackHandler(enabled = !isSectionDrawerOpen && !mainDrawerState.isOpen && currentDestination != AppDestination.WORKSPACES) {
        currentDestination = AppDestination.WORKSPACES
    }

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

    val activeColor by animateColorAsState(
        targetValue = if (isRunning) c.success else c.textMuted,
        animationSpec = tween(200),
        label = "activeColor"
    )

    ModalNavigationDrawer(
        drawerState = mainDrawerState,
        drawerContent = {
            MainAppFeaturesDrawerSheet(
                viewModel = viewModel,
                currentDestination = currentDestination,
                onSelectDestination = { destination ->
                    currentDestination = destination
                },
                onOpenCommandHub = { showCommandHubSheet = true },
                onOpenAppBridge = { showAppBridgeDialog = true },
                onOpenOpenGraphScraper = openOpenGraphScraper,
                onQuickAddTab = { showQuickAddTabDialog = true },
                onInstantScrapeAll = {
                    viewModel.triggerInstantScrapeAll()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("تم استخراج البيانات وتحديث قاعدة البيانات")
                    }
                    currentDestination = AppDestination.DATABASE
                },
                onOpenSectionDrawer = { isSectionDrawerOpen = true },
                onCloseDrawer = {
                    coroutineScope.launch { mainDrawerState.close() }
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = c.background,
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = c.card,
                            contentColor = c.textPrimary,
                            actionColor = c.accent,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                topBar = {
                    Surface(
                        color = c.surface,
                        shadowElevation = if (c.isDark) 0.dp else 2.dp,
                        border = BorderStroke(1.dp, c.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Start: NavigationDrawer Trigger + Clean Brand & Active Section Title
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = {
                                        isSectionDrawerOpen = false
                                        coroutineScope.launch { mainDrawerState.open() }
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("open_main_drawer_button")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(c.cardElevated)
                                            .border(1.dp, c.borderSubtle, RoundedCornerShape(11.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Menu,
                                            contentDescription = "فتح القائمة الرئيسية للأقسام والإعدادات",
                                            tint = c.textPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { showCommandHubSheet = true }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                        .testTag("open_command_hub_button")
                                ) {
                                    Text(
                                        text = currentDestination.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = c.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(activeColor.copy(alpha = if (isRunning) pulseAlpha else 1f))
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (isRunning) "نشط • ⚡${totalTokens ?: 0}" else "Social Agent • ⚡${totalTokens ?: 0}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isRunning) activeColor else c.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // End: Master Play/Stop + Secondary Section-Specific Side Panel Button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        viewModel.toggleSystemRunning()
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = if (!isRunning) "تم تشغيل الأتمتة على جميع النوافذ" else "تم إيقاف الأتمتة مؤقتاً",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (isRunning) c.successSubtle else c.cardElevated,
                                        contentColor = if (isRunning) c.success else c.textPrimary
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isRunning) c.success.copy(alpha = 0.45f) else c.borderSubtle
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .minimumInteractiveComponentSize()
                                        .testTag("master_system_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isRunning) "إيقاف" else "تشغيل",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Secondary Section-Specific Panel Button
                                FilledTonalButton(
                                    onClick = { isSectionDrawerOpen = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = c.accentSoft,
                                        contentColor = c.accent
                                    ),
                                    border = BorderStroke(1.dp, c.accent.copy(alpha = 0.25f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .minimumInteractiveComponentSize()
                                        .testTag("open_section_drawer_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ViewSidebar,
                                        contentDescription = "فتح لوحة أدوات ${currentDestination.title}",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "أدوات القسم",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    Surface(
                        color = c.surface,
                        shadowElevation = if (c.isDark) 0.dp else 4.dp,
                        border = BorderStroke(1.dp, c.borderSubtle)
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .navigationBarsPadding()
                                .testTag("main_navigation_bar")
                        ) {
                            // 1. Workspaces
                            NavigationBarItem(
                                selected = currentDestination == AppDestination.WORKSPACES,
                                onClick = { currentDestination = AppDestination.WORKSPACES },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (tabs.isNotEmpty()) {
                                                Badge(containerColor = c.accent, contentColor = Color.White) {
                                                    Text("${tabs.size}", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (currentDestination == AppDestination.WORKSPACES)
                                                Icons.Filled.DashboardCustomize else Icons.Outlined.DashboardCustomize,
                                            contentDescription = "المساحات",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = "المساحات",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (currentDestination == AppDestination.WORKSPACES) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = c.accent,
                                    selectedTextColor = c.accent,
                                    indicatorColor = c.accentSoft,
                                    unselectedIconColor = c.textMuted,
                                    unselectedTextColor = c.textMuted
                                )
                            )

                            // 2. Browser
                            NavigationBarItem(
                                selected = currentDestination == AppDestination.BROWSER,
                                onClick = { currentDestination = AppDestination.BROWSER },
                                icon = {
                                    Icon(
                                        imageVector = if (currentDestination == AppDestination.BROWSER)
                                            Icons.Filled.Language else Icons.Outlined.Language,
                                        contentDescription = "المتصفح",
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "المتصفح",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (currentDestination == AppDestination.BROWSER) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = c.accent,
                                    selectedTextColor = c.accent,
                                    indicatorColor = c.accentSoft,
                                    unselectedIconColor = c.textMuted,
                                    unselectedTextColor = c.textMuted
                                )
                            )

                            // 3. Offline Recent History
                            NavigationBarItem(
                                selected = currentDestination == AppDestination.RECENT_PAGES,
                                onClick = { currentDestination = AppDestination.RECENT_PAGES },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (recentPages.isNotEmpty()) {
                                                Badge(containerColor = c.success, contentColor = Color.White) {
                                                    Text("${recentPages.size}", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (currentDestination == AppDestination.RECENT_PAGES)
                                                Icons.Filled.Bookmarks else Icons.Outlined.Bookmarks,
                                            contentDescription = "المزارة",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = "المزارة",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (currentDestination == AppDestination.RECENT_PAGES) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = c.accent,
                                    selectedTextColor = c.accent,
                                    indicatorColor = c.accentSoft,
                                    unselectedIconColor = c.textMuted,
                                    unselectedTextColor = c.textMuted
                                )
                            )

                            // 4. Scraped Data / OpenGraph
                            NavigationBarItem(
                                selected = currentDestination == AppDestination.DATABASE,
                                onClick = { currentDestination = AppDestination.DATABASE },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (posts.isNotEmpty()) {
                                                Badge(containerColor = c.accentSecondary, contentColor = Color.White) {
                                                    Text("${posts.size}", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (currentDestination == AppDestination.DATABASE)
                                                Icons.Filled.Storage else Icons.Outlined.Storage,
                                            contentDescription = "البيانات",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = "البيانات",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (currentDestination == AppDestination.DATABASE) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = c.accent,
                                    selectedTextColor = c.accent,
                                    indicatorColor = c.accentSoft,
                                    unselectedIconColor = c.textMuted,
                                    unselectedTextColor = c.textMuted
                                )
                            )

                            // 5. Task Logs
                            NavigationBarItem(
                                selected = currentDestination == AppDestination.LOGS,
                                onClick = { currentDestination = AppDestination.LOGS },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (logs.isNotEmpty()) {
                                                Badge(containerColor = c.warning, contentColor = Color.White) {
                                                    Text("${logs.size}", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (currentDestination == AppDestination.LOGS)
                                                Icons.Filled.HistoryEdu else Icons.Outlined.HistoryEdu,
                                            contentDescription = "السجل",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = "السجل",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (currentDestination == AppDestination.LOGS) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = c.accent,
                                    selectedTextColor = c.accent,
                                    indicatorColor = c.accentSoft,
                                    unselectedIconColor = c.textMuted,
                                    unselectedTextColor = c.textMuted
                                )
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (currentDestination) {
                        AppDestination.WORKSPACES -> WorkspacesScreen(
                            viewModel = viewModel,
                            onNavigateToBrowserTab = { tabId ->
                                viewModel.selectTab(tabId)
                                currentDestination = AppDestination.BROWSER
                            },
                            onOpenCommandHub = { showCommandHubSheet = true },
                            onOpenOpenGraphScraper = openOpenGraphScraper,
                            onOpenAppBridge = { showAppBridgeDialog = true },
                            onViewAllRecentPages = { currentDestination = AppDestination.RECENT_PAGES }
                        )
                        AppDestination.BROWSER -> BrowserScreen(
                            viewModel = viewModel,
                            onNavigateToWorkspaces = {
                                currentDestination = AppDestination.WORKSPACES
                            },
                            onNavigateToLogs = {
                                currentDestination = AppDestination.LOGS
                            },
                            onOpenOpenGraphScraper = openOpenGraphScraper
                        )
                        AppDestination.RECENT_PAGES -> OfflineRecentPagesView(
                            viewModel = viewModel,
                            onOpenInBrowser = { url, title ->
                                viewModel.openRecentPageInBrowser(url, title)
                                viewModel.setBrowserViewMode(BrowserViewMode.LIVE_WEB_VIEW)
                                currentDestination = AppDestination.BROWSER
                            },
                            onOpenOpenGraphScraper = openOpenGraphScraper
                        )
                        AppDestination.LOGS -> TaskLogsScreen(
                            viewModel = viewModel,
                            onNavigateToTab = { tabId ->
                                viewModel.selectTab(tabId)
                                currentDestination = AppDestination.BROWSER
                            }
                        )
                        AppDestination.DATABASE -> DataMarketingTabScreen(
                            viewModel = viewModel,
                            onOpenOpenGraphScraper = openOpenGraphScraper
                        )
                        AppDestination.ANALYTICS -> AnalyticsTabScreen(
                            viewModel = viewModel
                        )
                        AppDestination.SETTINGS -> SettingsScreen(
                            viewModel = viewModel,
                            onOpenBridge = { showAppBridgeDialog = true },
                            onOpenOpenGraphScraper = openOpenGraphScraper
                        )
                    }
                }
            }

            // Secondary Feature-Specific Side Panel Overlay
            SectionSideDrawerOverlay(
                isOpen = isSectionDrawerOpen,
                currentDestination = currentDestination,
                viewModel = viewModel,
                onDismiss = { isSectionDrawerOpen = false },
                onNavigateToDestination = { destination ->
                    currentDestination = destination
                },
                onOpenCommandHub = { showCommandHubSheet = true },
                onOpenAppBridge = { showAppBridgeDialog = true },
                onOpenOpenGraphScraper = openOpenGraphScraper,
                onQuickAddTab = { showQuickAddTabDialog = true }
            )
        }
    }

    if (showCommandHubSheet) {
        CommandHubBottomSheet(
            onDismiss = { showCommandHubSheet = false },
            onExecuteUniversalPrompt = { prompt ->
                viewModel.executeUniversalPromptOnAllTabs(prompt)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("جاري تنفيذ المهمة على ${tabs.size} نوافذ")
                }
                showCommandHubSheet = false
                currentDestination = AppDestination.LOGS
            },
            onTriggerInstantScrape = {
                viewModel.triggerInstantScrapeAll()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("تم استخراج البيانات وتحديث قاعدة البيانات")
                }
                showCommandHubSheet = false
                currentDestination = AppDestination.DATABASE
            },
            onQuickAddPage = {
                showCommandHubSheet = false
                showQuickAddTabDialog = true
            },
            onToggleSystem = {
                viewModel.toggleSystemRunning()
                showCommandHubSheet = false
            },
            onOpenBridge = {
                showCommandHubSheet = false
                showAppBridgeDialog = true
            },
            onOpenOpenGraph = {
                showCommandHubSheet = false
                openOpenGraphScraper("")
            },
            isRunning = isRunning,
            openTabsCount = tabs.size
        )
    }

    if (showAppBridgeDialog) {
        AppBridgeDialog(
            viewModel = viewModel,
            onDismissRequest = { showAppBridgeDialog = false }
        )
    }

    if (showQuickAddTabDialog) {
        AlertDialog(
            onDismissRequest = { showQuickAddTabDialog = false },
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
                        value = quickTabTitle,
                        onValueChange = { quickTabTitle = it },
                        label = { Text("عنوان النافذة") },
                        placeholder = { Text("مثال: تويتر، لينكد إن...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = quickTabUrl,
                        onValueChange = { quickTabUrl = it },
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
                        val title = quickTabTitle.ifBlank { "صفحة جديدة" }
                        val url = quickTabUrl.ifBlank { "https://x.com" }
                        viewModel.addNewTab(title, url)
                        quickTabTitle = ""
                        quickTabUrl = ""
                        showQuickAddTabDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("تمت إضافة النافذة بنجاح")
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White)
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickAddTabDialog = false }) {
                    Text("إلغاء", color = c.textSecondary)
                }
            }
        )
    }

    if (showOpenGraphDialog) {
        OpenGraphScraperDialog(
            viewModel = viewModel,
            initialUrl = openGraphInitialUrl,
            onDismiss = { showOpenGraphDialog = false },
            onViewRecentPages = {
                currentDestination = AppDestination.RECENT_PAGES
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandHubBottomSheet(
    onDismiss: () -> Unit,
    onExecuteUniversalPrompt: (String) -> Unit,
    onTriggerInstantScrape: () -> Unit,
    onQuickAddPage: () -> Unit,
    onToggleSystem: () -> Unit,
    onOpenBridge: () -> Unit,
    onOpenOpenGraph: () -> Unit = {},
    isRunning: Boolean,
    openTabsCount: Int
) {
    val c = AppTheme.colors
    var universalPrompt by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(c.border)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = c.accentSoft,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                tint = c.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "مركز الأوامر والتحكم",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = c.textPrimary
                        )
                        Text(
                            text = "تنفيذ أوامر مباشرة على $openTabsCount نوافذ مفتوحة",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textSecondary
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = c.card,
                border = BorderStroke(1.dp, c.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "مهمة شاملة لكافة النوافذ:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = universalPrompt,
                        onValueChange = { universalPrompt = it },
                        placeholder = { Text("اكتب الأمر لتنفيذه على جميع النوافذ...", style = MaterialTheme.typography.bodySmall, color = c.textMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("universal_prompt_input"),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = c.accent,
                            unfocusedBorderColor = c.border,
                            focusedContainerColor = c.cardElevated,
                            unfocusedContainerColor = c.cardElevated,
                            focusedTextColor = c.textPrimary,
                            unfocusedTextColor = c.textPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val presets = listOf(
                            "استخراج المنشورات والتفاعل الحالي",
                            "رصد الحسابات والوسوم الشائعة",
                            "تلخيص وتحليل محتوى الصفحة",
                            "استخراج روابط المقالات والبيانات"
                        )
                        items(presets) { preset ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = c.cardElevated,
                                border = BorderStroke(1.dp, c.borderSubtle),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { universalPrompt = preset }
                            ) {
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.textSecondary,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onExecuteUniversalPrompt(universalPrompt) },
                        enabled = universalPrompt.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("execute_universal_prompt_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تنفيذ الأمر على النوافذ", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = c.card,
                    border = BorderStroke(1.dp, c.borderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onTriggerInstantScrape() }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Filled.CloudDownload, contentDescription = null, tint = c.accentSecondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("استخراج فوري", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = c.textPrimary)
                        Text("لكافة الصفحات", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = c.card,
                    border = BorderStroke(1.dp, c.borderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpenOpenGraph() }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Filled.TravelExplore, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("OpenGraph", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = c.textPrimary)
                        Text("سحب رابط", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = c.card,
                    border = BorderStroke(1.dp, c.borderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onToggleSystem() }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                            contentDescription = null,
                            tint = if (isRunning) c.success else c.warning,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(if (isRunning) "إيقاف مؤقت" else "تشغيل الأتمتة", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = c.textPrimary)
                        Text(if (isRunning) "نشط" else "متوقف", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = c.card,
                border = BorderStroke(1.dp, c.borderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        onDismiss()
                        onOpenBridge()
                    }
                    .testTag("command_hub_open_bridge")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = c.accentSoft,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SyncAlt,
                                contentDescription = null,
                                tint = c.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "جسر التبادل مع التطبيق الآخر (Offline IPC)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = c.textPrimary
                        )
                        Text(
                            text = "تبادل فوري للبيانات محلياً بدون إنترنت",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textMuted
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = c.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun AnalyticsTabScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    val totalTokens by viewModel.totalTokens.collectAsState()
    val isRunning by viewModel.isSystemRunning.collectAsState()
    val tabs by viewModel.browserManager.tabs.collectAsState()
    val logs by viewModel.allTabTaskLogs.collectAsState()
    val posts by viewModel.allScrapedPosts.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "مؤشرات الأداء والاستهلاك",
                style = MaterialTheme.typography.headlineSmall,
                color = c.textPrimary
            )
            Text(
                text = "متابعة كفاءة الاستهلاك ومعدل الإنجاز التراكمي",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = c.card,
                shadowElevation = if (c.isDark) 0.dp else 1.dp,
                border = BorderStroke(1.dp, c.borderSubtle),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("التوكنز التراكمي", style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
                        Icon(imageVector = Icons.Filled.Bolt, contentDescription = null, tint = c.warning, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${totalTokens ?: 0}", style = MaterialTheme.typography.headlineMedium, color = c.textPrimary)
                    Text("Gemini 2.5 Flash", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = c.card,
                shadowElevation = if (c.isDark) 0.dp else 1.dp,
                border = BorderStroke(1.dp, c.borderSubtle),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("المنشورات المستخرجة", style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
                        Icon(imageVector = Icons.Filled.Storage, contentDescription = null, tint = c.success, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${posts.size}", style = MaterialTheme.typography.headlineMedium, color = c.success)
                    Text("سجلات محفوظة", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = c.card,
                shadowElevation = if (c.isDark) 0.dp else 1.dp,
                border = BorderStroke(1.dp, c.borderSubtle),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("المهام المنفذة", style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
                        Icon(imageVector = Icons.Filled.HistoryEdu, contentDescription = null, tint = c.accent, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${logs.size}", style = MaterialTheme.typography.headlineMedium, color = c.textPrimary)
                    Text("عمليات مكتملة", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = c.card,
                shadowElevation = if (c.isDark) 0.dp else 1.dp,
                border = BorderStroke(1.dp, c.borderSubtle),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("النوافذ النشطة", style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
                        Icon(imageVector = Icons.Filled.Language, contentDescription = null, tint = c.accentSecondary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${tabs.size}", style = MaterialTheme.typography.headlineMedium, color = c.accentSecondary)
                    Text("صفحات متصلة", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = c.card,
            shadowElevation = if (c.isDark) 0.dp else 1.dp,
            border = BorderStroke(1.dp, c.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "بيئة التشغيل والأتمتة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = c.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("حالة النظام:", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                    Text(
                        if (isRunning) "نشط ومستمر" else "متوقف مؤقتاً",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isRunning) c.success else c.textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("محاكاة السلوك البشري:", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                    Text("مفعّلة (HumanBehaviorEngine)", style = MaterialTheme.typography.labelLarge, color = c.accentSecondary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("محرك الاستخراج (Scraper):", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                    Text("جاهز ومتصل", style = MaterialTheme.typography.labelLarge, color = c.success)
                }
            }
        }
    }
}
