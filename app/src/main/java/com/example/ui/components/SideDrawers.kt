package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bridge.BridgeStatus
import com.example.browser.TabAnalysisStatus
import com.example.ui.AppDestination
import com.example.ui.MainViewModel
import com.example.ui.screens.BrowserViewMode
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Primary Categorized NavigationDrawer Sheet wrapping the main Scaffold.
 * Organizes all app features into clear categories:
 * Workspaces & Browser, Recent History, Social Scrapers, Automation & Bridge, and Settings.
 */
@Composable
fun MainAppFeaturesDrawerSheet(
    viewModel: MainViewModel,
    currentDestination: AppDestination,
    onSelectDestination: (AppDestination) -> Unit,
    onOpenCommandHub: () -> Unit,
    onOpenAppBridge: () -> Unit,
    onOpenOpenGraphScraper: (String) -> Unit,
    onQuickAddTab: () -> Unit,
    onInstantScrapeAll: () -> Unit,
    onOpenSectionDrawer: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val c = AppTheme.colors
    val isRunning by viewModel.isSystemRunning.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val tabs by viewModel.browserManager.tabs.collectAsState()
    val recentPages by viewModel.allRecentPages.collectAsState()
    val logs by viewModel.allTabTaskLogs.collectAsState()
    val posts by viewModel.allScrapedPosts.collectAsState()
    val totalTokens by viewModel.totalTokens.collectAsState()
    val bridgeStatus by viewModel.bridgeStatus.collectAsState()

    ModalDrawerSheet(
        drawerContainerColor = c.surface,
        drawerContentColor = c.textPrimary,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier
            .widthIn(max = 316.dp)
            .fillMaxHeight()
            .testTag("main_features_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Drawer Hero Brand Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, c.borderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(listOf(c.heroStart, c.heroEnd)),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(11.dp),
                                color = c.accent,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Social Agent PRO",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = c.textPrimary
                                )
                                Text(
                                    text = "مركز الأدوات والأقسام",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.textSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = onCloseDrawer,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق القائمة",
                                tint = c.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DrawerStatPill("النوافذ", "${tabs.size}", c.accent, Modifier.weight(1f))
                        DrawerStatPill("المزارة", "${recentPages.size}", c.success, Modifier.weight(1f))
                        DrawerStatPill("التوكنز", "${totalTokens ?: 0}", c.warning, Modifier.weight(1f))
                    }
                }
            }

            // Category 1: Workspaces & Browser
            DrawerGroupTitle("مساحات العمل والتصفح")

            DrawerNavRow(
                title = "مساحات العمل الذكية",
                subtitle = "لوحة التحكم والنوافذ المفتوحة",
                icon = Icons.Filled.DashboardCustomize,
                badge = "${tabs.size}",
                selected = currentDestination == AppDestination.WORKSPACES,
                onClick = {
                    onSelectDestination(AppDestination.WORKSPACES)
                    onCloseDrawer()
                },
                testTag = "drawer_nav_workspaces"
            )

            DrawerNavRow(
                title = "المتصفح التفاعلي",
                subtitle = "تصفح حي وأتمتة مباشرة",
                icon = Icons.Filled.Language,
                selected = currentDestination == AppDestination.BROWSER,
                onClick = {
                    onSelectDestination(AppDestination.BROWSER)
                    onCloseDrawer()
                },
                testTag = "drawer_nav_browser"
            )

            DrawerFeatureButton(
                title = "إضافة نافذة متصفح جديدة",
                subtitle = "فتح منصة أو رابط جديد فوراً",
                icon = Icons.Filled.AddLink,
                tint = c.accent,
                onClick = {
                    onCloseDrawer()
                    onQuickAddTab()
                },
                testTag = "drawer_feature_add_tab"
            )

            HorizontalDivider(color = c.borderSubtle, modifier = Modifier.padding(vertical = 2.dp))

            // Category 2: Recent History
            DrawerGroupTitle("السجل والمزارة حديثاً (Recent History)")

            DrawerNavRow(
                title = "الصفحات المزارة أوفلاين",
                subtitle = "سجل Room بالعناوين والأوقات",
                icon = Icons.Filled.Bookmarks,
                badge = "${recentPages.size}",
                badgeColor = c.success,
                selected = currentDestination == AppDestination.RECENT_PAGES,
                onClick = {
                    onSelectDestination(AppDestination.RECENT_PAGES)
                    onCloseDrawer()
                },
                testTag = "drawer_nav_recent_pages"
            )

            DrawerNavRow(
                title = "سجل مهام الذكاء الاصطناعي",
                subtitle = "نتائج وتاريخ جميع الأوامر",
                icon = Icons.Filled.HistoryEdu,
                badge = "${logs.size}",
                badgeColor = c.warning,
                selected = currentDestination == AppDestination.LOGS,
                onClick = {
                    onSelectDestination(AppDestination.LOGS)
                    onCloseDrawer()
                },
                testTag = "drawer_nav_logs"
            )

            HorizontalDivider(color = c.borderSubtle, modifier = Modifier.padding(vertical = 2.dp))

            // Category 3: Social Scrapers
            DrawerGroupTitle("أدوات السحب والاستخراج (Social Scrapers)")

            DrawerFeatureButton(
                title = "مستخرج OpenGraph من رابط",
                subtitle = "سحب العنوان والوصف وحفظه في Room",
                icon = Icons.Filled.TravelExplore,
                tint = c.accentSecondary,
                onClick = {
                    onCloseDrawer()
                    onOpenOpenGraphScraper("")
                },
                testTag = "drawer_feature_opengraph"
            )

            DrawerNavRow(
                title = "قاعدة بيانات المنشورات",
                subtitle = "عرض وتصفية البيانات المستخرجة",
                icon = Icons.Filled.Storage,
                badge = "${posts.size}",
                badgeColor = c.accentSecondary,
                selected = currentDestination == AppDestination.DATABASE,
                onClick = {
                    onSelectDestination(AppDestination.DATABASE)
                    onCloseDrawer()
                },
                testTag = "drawer_nav_database"
            )

            DrawerFeatureButton(
                title = "استخراج فوري لكل النوافذ",
                subtitle = "حفظ محتوى كل النوافذ إلى Room",
                icon = Icons.Filled.CloudDownload,
                tint = c.success,
                onClick = {
                    onInstantScrapeAll()
                    onCloseDrawer()
                },
                testTag = "drawer_feature_instant_scrape"
            )

            HorizontalDivider(color = c.borderSubtle, modifier = Modifier.padding(vertical = 2.dp))

            // Category 4: Automation, Bridge & Analytics
            DrawerGroupTitle("الأتمتة والربط")

            DrawerFeatureButton(
                title = if (isRunning) "إيقاف الأتمتة الشاملة" else "تشغيل الأتمتة الشاملة",
                subtitle = if (isRunning) "محرك الأتمتة يعمل الآن" else "ابدأ تحليل جميع النوافذ",
                icon = if (isRunning) Icons.Filled.StopCircle else Icons.Filled.PlayCircle,
                tint = if (isRunning) c.success else c.accent,
                onClick = {
                    viewModel.toggleSystemRunning()
                    onCloseDrawer()
                },
                testTag = "drawer_feature_toggle_system"
            )

            DrawerFeatureButton(
                title = "مركز الأوامر الشاملة",
                subtitle = "إرسال برومبت موحد لجميع النوافذ",
                icon = Icons.Filled.Tune,
                tint = ElectricViolet,
                onClick = {
                    onCloseDrawer()
                    onOpenCommandHub()
                },
                testTag = "drawer_feature_command_hub"
            )

            DrawerFeatureButton(
                title = "جسر الربط أوفلاين (App Bridge)",
                subtitle = if (bridgeStatus == BridgeStatus.CONNECTED) "متصل بالتطبيق الشريك" else "ربط ومزامنة بالمعرّف",
                icon = Icons.Filled.SyncAlt,
                tint = c.success,
                onClick = {
                    onCloseDrawer()
                    onOpenAppBridge()
                },
                testTag = "drawer_feature_bridge"
            )

            DrawerNavRow(
                title = "الإحصاءات والأداء",
                subtitle = "مؤشرات التشغيل والاستهلاك",
                icon = Icons.Filled.Analytics,
                selected = currentDestination == AppDestination.ANALYTICS,
                onClick = {
                    onSelectDestination(AppDestination.ANALYTICS)
                    onCloseDrawer()
                },
                testTag = "drawer_nav_analytics"
            )

            DrawerFeatureButton(
                title = "لوحة أدوات القسم (${currentDestination.title})",
                subtitle = "فتح اللوحة الجانبية المخصصة لهذا القسم",
                icon = Icons.Filled.ViewSidebar,
                tint = c.warning,
                onClick = {
                    onCloseDrawer()
                    onOpenSectionDrawer()
                },
                testTag = "drawer_feature_open_section_drawer"
            )

            HorizontalDivider(color = c.borderSubtle, modifier = Modifier.padding(vertical = 2.dp))

            // Category 5: Settings
            DrawerGroupTitle("الإعدادات (Settings)")

            // Inline Quick Light/Dark Mode Toggle Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = c.cardElevated,
                border = BorderStroke(1.dp, c.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = (if (isDarkMode) c.accent else c.warning).copy(alpha = 0.14f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                    contentDescription = null,
                                    tint = if (isDarkMode) c.accent else c.warning,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isDarkMode) "الوضع الداكن" else "الوضع الفاتح",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = c.textPrimary
                            )
                            Text(
                                text = if (isDarkMode) "تبديل إلى الوضع الفاتح" else "المظهر الأساسي المريح",
                                style = MaterialTheme.typography.labelSmall,
                                color = c.textMuted
                            )
                        }
                    }

                    Switch(
                        checked = !isDarkMode,
                        onCheckedChange = { lightEnabled -> viewModel.setDarkMode(!lightEnabled) },
                        modifier = Modifier.testTag("drawer_light_mode_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = c.accent
                        )
                    )
                }
            }

            DrawerNavRow(
                title = "إعدادات النظام والتخزين",
                subtitle = "المظهر، الأتمتة، وصيانة Room",
                icon = Icons.Filled.Settings,
                selected = currentDestination == AppDestination.SETTINGS,
                onClick = {
                    onSelectDestination(AppDestination.SETTINGS)
                    onCloseDrawer()
                },
                testTag = "drawer_nav_settings"
            )
        }
    }
}

/**
 * Secondary, Feature-Specific Side Panel Overlay.
 * Slides in smoothly when a section is active to expose tools, filters, and controls
 * relevant ONLY to the currently selected category.
 */
@Composable
fun SectionSideDrawerOverlay(
    isOpen: Boolean,
    currentDestination: AppDestination,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onNavigateToDestination: (AppDestination) -> Unit,
    onOpenCommandHub: () -> Unit,
    onOpenAppBridge: () -> Unit,
    onOpenOpenGraphScraper: (String) -> Unit,
    onQuickAddTab: () -> Unit
) {
    val c = AppTheme.colors

    BackHandler(enabled = isOpen) {
        onDismiss()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Soft Translucent Scrim
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (c.isDark) 0.58f else 0.32f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        // Slide-in Secondary Panel from End Edge (opposite Main NavigationDrawer)
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                initialOffsetX = { -it }
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { -it }
            ),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                color = c.surface,
                contentColor = c.textPrimary,
                border = BorderStroke(1.dp, c.border),
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                modifier = Modifier
                    .widthIn(min = 276.dp, max = 316.dp)
                    .fillMaxHeight()
                    .testTag("section_specific_drawer_${currentDestination.route}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Section Header Banner
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = c.accentSoft,
                        border = BorderStroke(1.dp, c.accent.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = c.accent,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.ViewSidebar,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "أدوات ${currentDestination.title}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = c.textPrimary
                                    )
                                    Text(
                                        text = "عناصر تحكم مخصصة للقسم الحالي",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = c.accent
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق لوحة القسم",
                                    tint = c.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Render Tailored Content Exclusively for the Active Category
                    when (currentDestination) {
                        AppDestination.WORKSPACES -> WorkspacesSectionDrawerBody(
                            viewModel = viewModel,
                            onDismiss = onDismiss,
                            onNavigateToBrowser = { onNavigateToDestination(AppDestination.BROWSER) },
                            onNavigateToRecent = { onNavigateToDestination(AppDestination.RECENT_PAGES) },
                            onOpenCommandHub = onOpenCommandHub,
                            onQuickAddTab = onQuickAddTab
                        )

                        AppDestination.BROWSER -> BrowserSectionDrawerBody(
                            viewModel = viewModel,
                            onDismiss = onDismiss,
                            onQuickAddTab = onQuickAddTab,
                            onOpenOpenGraphScraper = onOpenOpenGraphScraper
                        )

                        AppDestination.RECENT_PAGES -> RecentPagesSectionDrawerBody(
                            viewModel = viewModel,
                            onDismiss = onDismiss,
                            onOpenOpenGraphScraper = onOpenOpenGraphScraper
                        )

                        AppDestination.LOGS -> LogsSectionDrawerBody(
                            viewModel = viewModel,
                            onDismiss = onDismiss
                        )

                        AppDestination.DATABASE -> DatabaseSectionDrawerBody(
                            viewModel = viewModel,
                            onDismiss = onDismiss,
                            onOpenOpenGraphScraper = onOpenOpenGraphScraper
                        )

                        AppDestination.ANALYTICS -> AnalyticsSectionDrawerBody(
                            viewModel = viewModel,
                            onDismiss = onDismiss,
                            onOpenAppBridge = onOpenAppBridge,
                            onOpenCommandHub = onOpenCommandHub,
                            onOpenOpenGraphScraper = onOpenOpenGraphScraper
                        )

                        AppDestination.SETTINGS -> SettingsSectionDrawerBody(
                            viewModel = viewModel,
                            onDismiss = onDismiss,
                            onOpenAppBridge = onOpenAppBridge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspacesSectionDrawerBody(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onNavigateToBrowser: () -> Unit,
    onNavigateToRecent: () -> Unit,
    onOpenCommandHub: () -> Unit,
    onQuickAddTab: () -> Unit
) {
    val c = AppTheme.colors
    val isRunning by viewModel.isSystemRunning.collectAsState()
    val tabs by viewModel.browserManager.tabs.collectAsState()
    val readyCount = tabs.count {
        it.analysisStatus == TabAnalysisStatus.READY_FOR_PROMPT || it.analysisStatus == TabAnalysisStatus.COMPLETED
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DrawerStatPill("النوافذ", "${tabs.size}", c.accent, Modifier.weight(1f))
        DrawerStatPill("جاهزة", "$readyCount", c.success, Modifier.weight(1f))
    }

    DrawerGroupTitle("تحكم المساحات")

    DrawerFeatureButton(
        title = if (isRunning) "إيقاف محرك المساحات" else "تشغيل محرك المساحات",
        subtitle = "تشغيل أو إيقاف التحليل التلقائي",
        icon = if (isRunning) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
        tint = if (isRunning) c.success else c.accent,
        onClick = {
            viewModel.toggleSystemRunning()
            onDismiss()
        }
    )

    DrawerFeatureButton(
        title = "تحليل كل النوافذ الآن",
        subtitle = "فحص المحتوى والروابط في كل مساحة",
        icon = Icons.Filled.AutoAwesome,
        tint = c.accentSecondary,
        onClick = {
            viewModel.startAutomatedPageAnalysis()
            onDismiss()
        }
    )

    DrawerFeatureButton(
        title = "إضافة مساحة عمل جديدة",
        subtitle = "فتح تبويب ورابط جديد في المتصفح",
        icon = Icons.Filled.Add,
        tint = c.success,
        onClick = {
            onDismiss()
            onQuickAddTab()
        }
    )

    DrawerFeatureButton(
        title = "تنفيذ برومبت موحد",
        subtitle = "فتح مركز الأوامر على كل المساحات",
        icon = Icons.Filled.Send,
        tint = ElectricViolet,
        onClick = {
            onDismiss()
            onOpenCommandHub()
        }
    )

    DrawerFeatureButton(
        title = "استيراد من الصفحات المزارة",
        subtitle = "اختيار صفحة محفوظة في Room",
        icon = Icons.Filled.Bookmarks,
        tint = c.warning,
        onClick = {
            onDismiss()
            onNavigateToRecent()
        }
    )

    if (tabs.isNotEmpty()) {
        DrawerGroupTitle("الانتقال السريع للنوافذ")
        tabs.forEach { tab ->
            DrawerFeatureButton(
                title = tab.title,
                subtitle = tab.url,
                icon = Icons.Filled.Tab,
                tint = c.textSecondary,
                onClick = {
                    viewModel.selectTab(tab.id)
                    viewModel.setBrowserViewMode(BrowserViewMode.LIVE_WEB_VIEW)
                    onDismiss()
                    onNavigateToBrowser()
                }
            )
        }
    }
}

@Composable
private fun BrowserSectionDrawerBody(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onQuickAddTab: () -> Unit,
    onOpenOpenGraphScraper: (String) -> Unit
) {
    val c = AppTheme.colors
    val tabs by viewModel.browserManager.tabs.collectAsState()
    val activeTabId by viewModel.browserManager.activeTabId.collectAsState()
    val viewMode by viewModel.browserViewMode.collectAsState()
    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull()
    val context = LocalContext.current

    DrawerGroupTitle("أوضاع عرض المتصفح")

    DrawerNavRow(
        title = "قائمة النوافذ",
        icon = Icons.Filled.List,
        badge = "${tabs.size}",
        selected = viewMode == BrowserViewMode.BROWSER_LIST,
        onClick = {
            viewModel.setBrowserViewMode(BrowserViewMode.BROWSER_LIST)
            onDismiss()
        }
    )
    DrawerNavRow(
        title = "المتصفح الحي (WebView)",
        icon = Icons.Filled.Language,
        selected = viewMode == BrowserViewMode.LIVE_WEB_VIEW,
        onClick = {
            viewModel.setBrowserViewMode(BrowserViewMode.LIVE_WEB_VIEW)
            onDismiss()
        }
    )
    DrawerNavRow(
        title = "الصفحات المزارة أوفلاين",
        icon = Icons.Filled.Bookmarks,
        selected = viewMode == BrowserViewMode.OFFLINE_RECENT_PAGES,
        onClick = {
            viewModel.setBrowserViewMode(BrowserViewMode.OFFLINE_RECENT_PAGES)
            onDismiss()
        }
    )
    DrawerNavRow(
        title = "سجل مهام المتصفح",
        icon = Icons.Filled.History,
        selected = viewMode == BrowserViewMode.TASK_LOGS,
        onClick = {
            viewModel.setBrowserViewMode(BrowserViewMode.TASK_LOGS)
            onDismiss()
        }
    )

    DrawerGroupTitle("أدوات الصفحة الحالية")

    DrawerFeatureButton(
        title = "استخراج OpenGraph وحفظه في Room",
        subtitle = activeTab?.url ?: "استخراج العنوان والميتاداتا",
        icon = Icons.Filled.TravelExplore,
        tint = c.accentSecondary,
        onClick = {
            onDismiss()
            onOpenOpenGraphScraper(activeTab?.url.orEmpty())
        }
    )

    DrawerFeatureButton(
        title = "حفظ الصفحة في سجل أوفلاين",
        subtitle = "تثبيت الزيارة الحالية في Room",
        icon = Icons.Filled.BookmarkAdd,
        tint = c.success,
        onClick = {
            activeTab?.let {
                viewModel.recordRecentPageVisit(it.url, it.title, snippet = it.pageSummary)
                Toast.makeText(context, "تم حفظ الصفحة في سجل أوفلاين", Toast.LENGTH_SHORT).show()
            }
            onDismiss()
        }
    )

    DrawerFeatureButton(
        title = "فتح نافذة جديدة",
        subtitle = "إضافة رابط أو منصة جديدة",
        icon = Icons.Filled.AddBox,
        tint = c.accent,
        onClick = {
            onDismiss()
            onQuickAddTab()
        }
    )

    if (tabs.isNotEmpty()) {
        DrawerGroupTitle("التبديل بين النوافذ (${tabs.size})")
        tabs.forEach { tab ->
            val isActive = tab.id == activeTab?.id
            DrawerNavRow(
                title = tab.title,
                subtitle = tab.url,
                icon = Icons.Filled.Web,
                selected = isActive,
                onClick = {
                    viewModel.selectTab(tab.id)
                    viewModel.setBrowserViewMode(BrowserViewMode.LIVE_WEB_VIEW)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun RecentPagesSectionDrawerBody(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onOpenOpenGraphScraper: (String) -> Unit
) {
    val c = AppTheme.colors
    val allPages by viewModel.allRecentPages.collectAsState()
    val pinnedPages by viewModel.pinnedRecentPages.collectAsState()
    val filterPlatform by viewModel.recentPagesFilterPlatform.collectAsState()
    val pinnedOnly by viewModel.recentPagesPinnedOnly.collectAsState()
    val sortByVisits by viewModel.recentPagesSortByVisits.collectAsState()
    val context = LocalContext.current

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DrawerStatPill("المحفوظة", "${allPages.size}", c.accent, Modifier.weight(1f))
        DrawerStatPill("المثبتة", "${pinnedPages.size}", c.warning, Modifier.weight(1f))
    }

    DrawerGroupTitle("استخراج وحفظ")

    DrawerFeatureButton(
        title = "سحب OpenGraph من رابط",
        subtitle = "استخراج عنوان ووصف وصورة وحفظها في Room",
        icon = Icons.Filled.TravelExplore,
        tint = c.accentSecondary,
        onClick = {
            onDismiss()
            onOpenOpenGraphScraper("")
        }
    )

    DrawerGroupTitle("خيارات العرض والترتيب")

    DrawerNavRow(
        title = "عرض المثبتة بالمفضلة فقط",
        icon = Icons.Filled.PushPin,
        badge = "${pinnedPages.size}",
        badgeColor = c.warning,
        selected = pinnedOnly,
        onClick = {
            viewModel.setRecentPagesPinnedOnly(!pinnedOnly)
            onDismiss()
        }
    )

    DrawerNavRow(
        title = if (sortByVisits) "الترتيب: الأكثر زيارة" else "الترتيب: الأحدث زيارة",
        icon = Icons.Filled.Sort,
        selected = sortByVisits,
        onClick = {
            viewModel.setRecentPagesSortByVisits(!sortByVisits)
            onDismiss()
        }
    )

    DrawerGroupTitle("تصفية حسب المنصة")

    val platforms = listOf(
        null to "كل المنصات",
        "X_TWITTER" to "إكس (Twitter)",
        "LINKEDIN" to "LinkedIn",
        "INSTAGRAM" to "Instagram",
        "REDDIT" to "Reddit",
        "WEB" to "مواقع ويب عامة"
    )
    platforms.forEach { (key, label) ->
        DrawerNavRow(
            title = label,
            icon = Icons.Filled.FilterList,
            selected = filterPlatform == key,
            onClick = {
                viewModel.setRecentPagesFilterPlatform(key)
                onDismiss()
            }
        )
    }

    if (allPages.isNotEmpty()) {
        HorizontalDivider(color = c.borderSubtle)
        DrawerFeatureButton(
            title = "مسح جميع الصفحات المحفوظة",
            subtitle = "تنظيف جدول recent_pages في Room",
            icon = Icons.Filled.DeleteSweep,
            tint = c.error,
            onClick = {
                viewModel.clearAllRecentPages()
                Toast.makeText(context, "تم مسح سجل الصفحات أوفلاين", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        )
    }
}

@Composable
private fun LogsSectionDrawerBody(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val c = AppTheme.colors
    val logs by viewModel.allTabTaskLogs.collectAsState()
    val tabs by viewModel.browserManager.tabs.collectAsState()
    val statusFilter by viewModel.logsStatusFilter.collectAsState()
    val tabFilter by viewModel.logsTabFilter.collectAsState()
    val context = LocalContext.current

    val completedCount = logs.count { it.status == "COMPLETED" }
    val failedCount = logs.count { it.status == "FAILED" }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DrawerStatPill("مكتمل", "$completedCount", c.success, Modifier.weight(1f))
        DrawerStatPill("أخطاء", "$failedCount", c.error, Modifier.weight(1f))
    }

    DrawerGroupTitle("تصفية حسب الحالة")

    listOf(
        "ALL" to "جميع الحالات (${logs.size})",
        "COMPLETED" to "المكتملة ($completedCount)",
        "IN_PROGRESS" to "قيد التنفيذ",
        "FAILED" to "الأخطاء ($failedCount)"
    ).forEach { (statusKey, label) ->
        DrawerNavRow(
            title = label,
            icon = Icons.Filled.CheckCircleOutline,
            selected = statusFilter == statusKey,
            onClick = {
                viewModel.setLogsStatusFilter(statusKey)
                onDismiss()
            }
        )
    }

    DrawerGroupTitle("تصفية حسب النافذة")

    DrawerNavRow(
        title = "كل النوافذ",
        icon = Icons.Filled.AllInclusive,
        selected = tabFilter == null,
        onClick = {
            viewModel.setLogsTabFilter(null)
            onDismiss()
        }
    )
    tabs.forEach { tab ->
        DrawerNavRow(
            title = tab.title,
            icon = Icons.Filled.Tab,
            selected = tabFilter == tab.id,
            onClick = {
                viewModel.setLogsTabFilter(tab.id)
                onDismiss()
            }
        )
    }

    HorizontalDivider(color = c.borderSubtle)

    DrawerFeatureButton(
        title = "نسخ ملخص السجل",
        subtitle = "نسخ المخرجات إلى الحافظة",
        icon = Icons.Filled.ContentCopy,
        tint = c.accent,
        onClick = {
            val summary = logs.take(15).joinToString("\n---\n") {
                "[${it.tabTitle}] (${it.status}): ${it.taskPrompt}\n${it.outputResult}"
            }
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Task Logs Summary", summary))
            Toast.makeText(context, "تم نسخ ملخص السجل", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    )

    if (logs.isNotEmpty()) {
        DrawerFeatureButton(
            title = "مسح جميع السجلات",
            subtitle = "حذف سجلات التنفيذ من Room",
            icon = Icons.Filled.DeleteSweep,
            tint = c.error,
            onClick = {
                viewModel.clearAllTaskLogs()
                Toast.makeText(context, "تم مسح سجل المهام", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        )
    }
}

@Composable
private fun DatabaseSectionDrawerBody(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onOpenOpenGraphScraper: (String) -> Unit
) {
    val c = AppTheme.colors
    val posts by viewModel.allScrapedPosts.collectAsState()
    val platformFilter by viewModel.databasePlatformFilter.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    DrawerStatPill("المنشورات والبيانات في Room", "${posts.size}", c.success, Modifier.fillMaxWidth())

    DrawerGroupTitle("مستخرج OpenGraph والبيانات")

    DrawerFeatureButton(
        title = "استخراج OpenGraph من رابط",
        subtitle = "سحب الميتاداتا والعنوان وحفظها في Room",
        icon = Icons.Filled.TravelExplore,
        tint = c.accentSecondary,
        onClick = {
            onDismiss()
            onOpenOpenGraphScraper("")
        }
    )

    DrawerGroupTitle("استخراج عينات سريعة")

    listOf(
        "X_TWITTER" to "سحب منشورات X (تويتر)",
        "LINKEDIN" to "سحب منشورات LinkedIn",
        "INSTAGRAM" to "سحب منشورات Instagram",
        "REDDIT" to "سحب منشورات Reddit"
    ).forEach { (platform, label) ->
        DrawerFeatureButton(
            title = label,
            subtitle = "إضافة 5 سجلات جديدة إلى Room",
            icon = Icons.Filled.CloudDownload,
            tint = c.success,
            onClick = {
                scope.launch {
                    viewModel.socialMediaScraper.scrapePlatformPosts(platform, 5)
                    Toast.makeText(context, "تم استخراج البيانات وحفظها", Toast.LENGTH_SHORT).show()
                }
                onDismiss()
            }
        )
    }

    DrawerGroupTitle("تصفية قاعدة البيانات")

    listOf(
        "ALL" to "الكل",
        "X_TWITTER" to "X (تويتر)",
        "LINKEDIN" to "LinkedIn",
        "INSTAGRAM" to "Instagram",
        "REDDIT" to "Reddit",
        "WEB" to "ويب / OpenGraph"
    ).forEach { (code, label) ->
        DrawerNavRow(
            title = label,
            icon = Icons.Filled.FilterAlt,
            selected = platformFilter == code,
            onClick = {
                viewModel.setDatabasePlatformFilter(code)
                onDismiss()
            }
        )
    }

    if (posts.isNotEmpty()) {
        HorizontalDivider(color = c.borderSubtle)
        DrawerFeatureButton(
            title = "مسح البيانات المستخرجة",
            subtitle = "تفريغ جدول scraped_posts_metadata",
            icon = Icons.Filled.DeleteForever,
            tint = c.error,
            onClick = {
                viewModel.clearAllScrapedPosts()
                Toast.makeText(context, "تم تفريغ بيانات المنشورات", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        )
    }
}

@Composable
private fun AnalyticsSectionDrawerBody(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onOpenAppBridge: () -> Unit,
    onOpenCommandHub: () -> Unit,
    onOpenOpenGraphScraper: (String) -> Unit
) {
    val c = AppTheme.colors
    val isObserverActive by viewModel.isBridgeObserverActive.collectAsState()
    val bridgeStatus by viewModel.bridgeStatus.collectAsState()
    val recentCount by viewModel.recentPagesCount.collectAsState()
    val posts by viewModel.allScrapedPosts.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DrawerStatPill("صفحات Room", "$recentCount", c.accent, Modifier.weight(1f))
        DrawerStatPill("منشورات", "${posts.size}", c.success, Modifier.weight(1f))
    }

    DrawerGroupTitle("مراقبة الجسر والمحركات")

    DrawerFeatureButton(
        title = "جسر الربط أوفلاين",
        subtitle = "الحالة: ${bridgeStatus.labelArabic}",
        icon = Icons.Filled.SyncAlt,
        tint = c.accentSecondary,
        onClick = {
            onDismiss()
            onOpenAppBridge()
        }
    )

    DrawerFeatureButton(
        title = if (isObserverActive) "إيقاف مراقب الخلفية للجسر" else "تفعيل مراقب الخلفية للجسر",
        subtitle = "خدمة ContentObserver و WorkManager",
        icon = if (isObserverActive) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
        tint = if (isObserverActive) c.success else c.warning,
        onClick = {
            viewModel.toggleBridgeBackgroundObservation()
            onDismiss()
        }
    )

    DrawerFeatureButton(
        title = "فحص سلامة قاعدة بيانات Room",
        subtitle = "اختبار القراءة والكتابة الآن",
        icon = Icons.Filled.VerifiedUser,
        tint = c.success,
        onClick = {
            scope.launch {
                val ok = viewModel.socialMediaScraper.testDatabasePipeline()
                Toast.makeText(
                    context,
                    if (ok) "✅ قاعدة بيانات Room تعمل بكفاءة تامة" else "❌ تعذر فحص قاعدة البيانات",
                    Toast.LENGTH_SHORT
                ).show()
            }
            onDismiss()
        }
    )

    DrawerFeatureButton(
        title = "مستخرج OpenGraph السريع",
        subtitle = "اختبار استخراج وحفظ ميتاداتا رابط",
        icon = Icons.Filled.TravelExplore,
        tint = ElectricViolet,
        onClick = {
            onDismiss()
            onOpenOpenGraphScraper("")
        }
    )

    DrawerFeatureButton(
        title = "مركز الأوامر الشاملة",
        subtitle = "تشغيل أوامر موحدة على النوافذ",
        icon = Icons.Filled.Tune,
        tint = c.warning,
        onClick = {
            onDismiss()
            onOpenCommandHub()
        }
    )
}

@Composable
private fun SettingsSectionDrawerBody(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onOpenAppBridge: () -> Unit
) {
    val c = AppTheme.colors
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val autoAnalyze by viewModel.autoAnalyzeNewTabs.collectAsState()
    val isObserverActive by viewModel.isBridgeObserverActive.collectAsState()
    val context = LocalContext.current

    DrawerGroupTitle("تبديل سريع للمظهر")

    DrawerNavRow(
        title = "الوضع الفاتح (الأساسي)",
        icon = Icons.Filled.LightMode,
        selected = !isDarkMode,
        onClick = {
            viewModel.setDarkMode(false)
            onDismiss()
        }
    )

    DrawerNavRow(
        title = "الوضع الداكن",
        icon = Icons.Filled.DarkMode,
        selected = isDarkMode,
        onClick = {
            viewModel.setDarkMode(true)
            onDismiss()
        }
    )

    DrawerGroupTitle("خيارات سريعة")

    DrawerFeatureButton(
        title = if (autoAnalyze) "التحليل التلقائي للنوافذ: مفعّل" else "التحليل التلقائي للنوافذ: معطّل",
        subtitle = "تشغيل التحليل فور إضافة نافذة جديدة",
        icon = Icons.Filled.AutoAwesome,
        tint = if (autoAnalyze) c.success else c.textMuted,
        onClick = {
            viewModel.setAutoAnalyzeNewTabs(!autoAnalyze)
            onDismiss()
        }
    )

    DrawerFeatureButton(
        title = if (isObserverActive) "مراقب الجسر بالخلفية: نشط" else "مراقب الجسر بالخلفية: متوقف",
        subtitle = "رصد التغييرات من التطبيق الشريك",
        icon = Icons.Filled.Sensors,
        tint = if (isObserverActive) c.success else c.warning,
        onClick = {
            viewModel.toggleBridgeBackgroundObservation()
            onDismiss()
        }
    )

    DrawerFeatureButton(
        title = "إعادة ضبط جميع الفلاتر",
        subtitle = "مسح البحث والفلاتر النشطة بكل الأقسام",
        icon = Icons.Filled.FilterAltOff,
        tint = c.accent,
        onClick = {
            viewModel.resetAllSectionFilters()
            Toast.makeText(context, "تمت إعادة ضبط جميع الفلاتر", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    )

    DrawerFeatureButton(
        title = "إعدادات جسر الربط أوفلاين",
        subtitle = "إدارة المعرّف والاقتران المحلي",
        icon = Icons.Filled.SyncAlt,
        tint = c.accentSecondary,
        onClick = {
            onDismiss()
            onOpenAppBridge()
        }
    )
}

@Composable
private fun DrawerGroupTitle(text: String) {
    val c = AppTheme.colors
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = c.textMuted,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun DrawerStatPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = c.surface,
        border = BorderStroke(1.dp, c.borderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = c.textSecondary
            )
        }
    }
}

@Composable
private fun DrawerNavRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    badge: String? = null,
    badgeColor: Color? = null,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String? = null
) {
    val c = AppTheme.colors
    val resolvedBadgeColor = badgeColor ?: c.accent

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) c.accentSoft else c.cardElevated,
        border = BorderStroke(1.dp, if (selected) c.accent.copy(alpha = 0.45f) else c.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = if (selected) c.accent else c.surface,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) Color.White else c.textSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) c.accent else c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!badge.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = resolvedBadgeColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = resolvedBadgeColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerFeatureButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    testTag: String? = null
) {
    val c = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = c.card,
        border = BorderStroke(1.dp, c.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = tint.copy(alpha = 0.12f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = null,
                tint = c.textMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
