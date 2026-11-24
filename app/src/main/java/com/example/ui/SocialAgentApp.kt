package com.example.ui

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
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.DataMarketingTabScreen
import com.example.ui.screens.TaskLogsScreen
import com.example.ui.screens.WorkspacesScreen
import com.example.ui.components.AppBridgeDialog
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class AppDestination(val route: String, val title: String) {
    WORKSPACES("workspaces", "المساحات"),
    BROWSER("browser", "المتصفح"),
    LOGS("logs", "السجل"),
    DATABASE("database", "البيانات"),
    ANALYTICS("analytics", "الإحصاءات")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialAgentApp(viewModel: MainViewModel) {
    var currentDestination by remember { mutableStateOf(AppDestination.WORKSPACES) }
    val isRunning by viewModel.isSystemRunning.collectAsState()
    val totalTokens by viewModel.totalTokens.collectAsState()
    val tabs by viewModel.browserManager.tabs.collectAsState()
    val logs by viewModel.allTabTaskLogs.collectAsState()
    val posts by viewModel.allScrapedPosts.collectAsState()

    var showCommandHubSheet by remember { mutableStateOf(false) }
    var showAppBridgeDialog by remember { mutableStateOf(false) }
    var showQuickAddTabDialog by remember { mutableStateOf(false) }
    var quickTabTitle by remember { mutableStateOf("") }
    var quickTabUrl by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Status indicator subtle pulse animation (Linear style)
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
        targetValue = if (isRunning) EmeraldGreen else TextMuted,
        animationSpec = tween(200),
        label = "activeColor"
    )

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = CardSurfaceElevated,
                    contentColor = TextPrimary,
                    actionColor = NeonCyan,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        topBar = {
            Surface(
                color = DarkSurface,
                border = BorderStroke(1.dp, CardBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Brand Logo & System Status Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showCommandHubSheet = true }
                            .padding(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AccentIndigo.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Social Agent",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AccentIndigo.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "PRO",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            // Status Dot & Indicator
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(activeColor.copy(alpha = if (isRunning) pulseAlpha else 1f))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isRunning) "أتمتة نشطة" else "في الانتظار",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = activeColor
                                )
                            }
                        }
                    }

                    // Right Actions: Live Tokens Badge + Master Switch + Command Hub
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Tokens Chip
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CardSurface,
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${totalTokens ?: 0}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                        }

                        // Master Play/Stop Button
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
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isRunning) EmeraldSubtle else CardSurface,
                                contentColor = if (isRunning) EmeraldGreen else TextSecondary
                            ),
                            border = BorderStroke(1.dp, if (isRunning) EmeraldGreen.copy(alpha = 0.5f) else CardBorder),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("master_system_toggle_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isRunning) "إيقاف" else "تشغيل",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Command Hub Trigger
                        IconButton(
                            onClick = { showCommandHubSheet = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardSurface)
                                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                .testTag("open_command_hub_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = "مركز الأوامر والتحكم",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Offline App Bridge Trigger
                        IconButton(
                            onClick = { showAppBridgeDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardSurface)
                                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                .testTag("open_app_bridge_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SyncAlt,
                                contentDescription = "جسر التبادل المحلي مع التطبيق الآخر",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = DarkSurface,
                border = BorderStroke(1.dp, CardBorderSubtle)
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
                                        Badge(
                                            containerColor = AccentIndigo,
                                            contentColor = Color.White
                                        ) {
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
                            selectedIconColor = TextPrimary,
                            selectedTextColor = TextPrimary,
                            indicatorColor = AccentIndigo.copy(alpha = 0.25f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
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
                            selectedIconColor = TextPrimary,
                            selectedTextColor = TextPrimary,
                            indicatorColor = AccentIndigo.copy(alpha = 0.25f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )

                    // 3. Task Logs
                    NavigationBarItem(
                        selected = currentDestination == AppDestination.LOGS,
                        onClick = { currentDestination = AppDestination.LOGS },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (logs.isNotEmpty()) {
                                        Badge(
                                            containerColor = AccentAmber,
                                            contentColor = Color.Black
                                        ) {
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
                            selectedIconColor = TextPrimary,
                            selectedTextColor = TextPrimary,
                            indicatorColor = AccentIndigo.copy(alpha = 0.25f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )

                    // 4. Data
                    NavigationBarItem(
                        selected = currentDestination == AppDestination.DATABASE,
                        onClick = { currentDestination = AppDestination.DATABASE },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (posts.isNotEmpty()) {
                                        Badge(
                                            containerColor = EmeraldGreen,
                                            contentColor = Color.White
                                        ) {
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
                            selectedIconColor = TextPrimary,
                            selectedTextColor = TextPrimary,
                            indicatorColor = AccentIndigo.copy(alpha = 0.25f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )

                    // 5. Analytics
                    NavigationBarItem(
                        selected = currentDestination == AppDestination.ANALYTICS,
                        onClick = { currentDestination = AppDestination.ANALYTICS },
                        icon = {
                            Icon(
                                imageVector = if (currentDestination == AppDestination.ANALYTICS)
                                    Icons.Filled.Analytics else Icons.Outlined.Analytics,
                                contentDescription = "الإحصاءات",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "الإحصاءات",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (currentDestination == AppDestination.ANALYTICS) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TextPrimary,
                            selectedTextColor = TextPrimary,
                            indicatorColor = AccentIndigo.copy(alpha = 0.25f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
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
                    }
                )
                AppDestination.BROWSER -> BrowserScreen(
                    viewModel = viewModel,
                    onNavigateToWorkspaces = {
                        currentDestination = AppDestination.WORKSPACES
                    },
                    onNavigateToLogs = {
                        currentDestination = AppDestination.LOGS
                    }
                )
                AppDestination.LOGS -> TaskLogsScreen(
                    viewModel = viewModel,
                    onNavigateToTab = { tabId ->
                        viewModel.selectTab(tabId)
                        currentDestination = AppDestination.BROWSER
                    }
                )
                AppDestination.DATABASE -> DataMarketingTabScreen(
                    viewModel = viewModel
                )
                AppDestination.ANALYTICS -> AnalyticsTabScreen(
                    viewModel = viewModel
                )
            }
        }
    }

    // Command Hub Sheet
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
            isRunning = isRunning,
            openTabsCount = tabs.size
        )
    }

    // App Bridge Dialog (Offline IPC with Companion App)
    if (showAppBridgeDialog) {
        AppBridgeDialog(
            viewModel = viewModel,
            onDismissRequest = { showAppBridgeDialog = false }
        )
    }

    // Quick Add Tab Dialog
    if (showQuickAddTabDialog) {
        AlertDialog(
            onDismissRequest = { showQuickAddTabDialog = false },
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
                        value = quickTabTitle,
                        onValueChange = { quickTabTitle = it },
                        label = { Text("عنوان النافذة") },
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
                        value = quickTabUrl,
                        onValueChange = { quickTabUrl = it },
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
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickAddTabDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            }
        )
    }
}

/**
 * Command Hub Bottom Sheet - Linear/Stripe design polish
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandHubBottomSheet(
    onDismiss: () -> Unit,
    onExecuteUniversalPrompt: (String) -> Unit,
    onTriggerInstantScrape: () -> Unit,
    onQuickAddPage: () -> Unit,
    onToggleSystem: () -> Unit,
    onOpenBridge: () -> Unit,
    isRunning: Boolean,
    openTabsCount: Int
) {
    var universalPrompt by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CardBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AccentIndigo.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "مركز الأوامر والتحكم",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "تنفيذ أوامر مباشرة على $openTabsCount نوافذ مفتوحة",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Universal Prompt Input Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "مهمة شاملة لكافة النوافذ:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = universalPrompt,
                        onValueChange = { universalPrompt = it },
                        placeholder = { Text("اكتب الأمر لتنفيذه في آن واحد على جميع النوافذ...", style = MaterialTheme.typography.bodySmall, color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("universal_prompt_input"),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 2,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentIndigo,
                            unfocusedBorderColor = CardBorder,
                            focusedContainerColor = CardSurfaceElevated,
                            unfocusedContainerColor = CardSurfaceElevated
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Quick Prompts
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
                                color = CardSurfaceElevated,
                                border = BorderStroke(1.dp, CardBorderSubtle),
                                modifier = Modifier.clickable { universalPrompt = preset }
                            ) {
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onExecuteUniversalPrompt(universalPrompt) },
                        enabled = universalPrompt.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
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

            // Quick Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Instant Scrape
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTriggerInstantScrape() }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Filled.CloudDownload, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("استخراج فوري", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("لكافة الصفحات", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }

                // Add Page
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onQuickAddPage() }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Filled.AddLink, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("إضافة نافذة", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("فتح عنوان جديد", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }

                // Master Toggle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleSystem() }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                            contentDescription = null,
                            tint = if (isRunning) EmeraldGreen else AccentAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(if (isRunning) "إيقاف مؤقت" else "تشغيل الأتمتة", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(if (isRunning) "النظام نشط" else "النظام متوقف", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Offline App Bridge Entry Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardSurface,
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
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
                        shape = RoundedCornerShape(8.dp),
                        color = NeonCyan.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SyncAlt,
                                contentDescription = null,
                                tint = NeonCyan,
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
                            color = TextPrimary
                        )
                        Text(
                            text = "تبادل فوري للبيانات محلياً على نفس الهاتف بدون إنترنت",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Analytics Dashboard Screen - Apple / Linear executive clean presentation
 */
@Composable
fun AnalyticsTabScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val totalTokens by viewModel.totalTokens.collectAsState()
    val isRunning by viewModel.isSystemRunning.collectAsState()
    val tabs by viewModel.browserManager.tabs.collectAsState()
    val logs by viewModel.allTabTaskLogs.collectAsState()
    val posts by viewModel.allScrapedPosts.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "مؤشرات الأداء والاستهلاك",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
            Text(
                text = "متابعة كفاءة الاستهلاك ومعدل الإنجاز التراكمي",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // Summary Metric Cards 2x2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Tokens Metric
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("التوكنز التراكمي", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Icon(imageVector = Icons.Filled.Bolt, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${totalTokens ?: 0}", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text("Gemini 2.5 Flash", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }

            // Scraped Posts Metric
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("المنشورات المستخرجة", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Icon(imageVector = Icons.Filled.Storage, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${posts.size}", style = MaterialTheme.typography.headlineMedium, color = EmeraldGreen)
                    Text("سجلات محفوظة", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Task Executions Metric
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("المهام المنفذة", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Icon(imageVector = Icons.Filled.HistoryEdu, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${logs.size}", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text("عمليات مكتملة", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }

            // Open Pages Metric
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("النوافذ النشطة", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Icon(imageVector = Icons.Filled.Language, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${tabs.size}", style = MaterialTheme.typography.headlineMedium, color = NeonCyan)
                    Text("صفحات متصلة", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        }

        // Engine Status Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "بيئة التشغيل والأتمتة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("حالة النظام:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(
                        if (isRunning) "نشط ومستمر" else "متوقف مؤقتاً",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isRunning) EmeraldGreen else TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("محاكاة السلوك البشري:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("مفعّلة (HumanBehaviorEngine)", style = MaterialTheme.typography.labelLarge, color = NeonCyan)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("محرك الاستخراج (Scraper):", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("جاهز ومتصل", style = MaterialTheme.typography.labelLarge, color = EmeraldGreen)
                }
            }
        }
    }
}
