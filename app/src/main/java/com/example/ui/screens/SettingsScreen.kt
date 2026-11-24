package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bridge.BridgeStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onOpenBridge: () -> Unit,
    onOpenOpenGraphScraper: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val autoAnalyzeNewTabs by viewModel.autoAnalyzeNewTabs.collectAsState()
    val isObserverActive by viewModel.isBridgeObserverActive.collectAsState()
    val bridgeStatus by viewModel.bridgeStatus.collectAsState()
    val recentPagesCount by viewModel.recentPagesCount.collectAsState()
    val logs by viewModel.allTabTaskLogs.collectAsState()
    val posts by viewModel.allScrapedPosts.collectAsState()
    val backendHealth by viewModel.backendHealthReport.collectAsState()
    val activeTasksCount by viewModel.activeOrchestrationTasksCount.collectAsState()

    var pendingClearTarget by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(c.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = c.card,
                border = BorderStroke(1.dp, c.borderSubtle),
                shadowElevation = if (c.isDark) 0.dp else 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = c.accentSoft,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = null,
                                    tint = c.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "إعدادات التطبيق والنظام",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = c.textPrimary
                            )
                            Text(
                                text = "تخصيص المظهر، محرك الأتمتة، وقواعد بيانات Room",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary
                            )
                        }
                    }

                    // Live Backend Health Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val dbHealthy = backendHealth?.databaseHealthy != false
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (dbHealthy) c.successSubtle else c.errorSubtle,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (dbHealthy) "Room: سليم" else "Room: خطأ",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (dbHealthy) c.success else c.error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }

                        val aiLive = backendHealth?.liveGeminiConfigured == true
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (aiLive) c.accentSoft else c.cardElevated,
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text(
                                text = if (aiLive) "AI: Gemini السحابي" else "AI: محلل DOM المحلي",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (aiLive) c.accent else c.textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = c.cardElevated,
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text(
                                text = "مهام نشطة: $activeTasksCount",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = c.textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // 1. Appearance / Light-First Theme Picker
        item {
            SettingsSectionLabel(title = "المظهر والعرض")
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = c.card,
                border = BorderStroke(1.dp, c.borderSubtle),
                shadowElevation = if (c.isDark) 0.dp else 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "سمة الواجهة الأساسية",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeModeOptionCard(
                            title = "الوضع الفاتح",
                            subtitle = "الأساسي والأكثر وضوحاً",
                            icon = Icons.Filled.LightMode,
                            selected = !isDarkMode,
                            onClick = { viewModel.setDarkMode(false) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_light_mode_btn")
                        )
                        ThemeModeOptionCard(
                            title = "الوضع الداكن",
                            subtitle = "مريح للإضاءة الخافتة",
                            icon = Icons.Filled.DarkMode,
                            selected = isDarkMode,
                            onClick = { viewModel.setDarkMode(true) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_dark_mode_btn")
                        )
                    }
                }
            }
        }

        // 2. Automation & Bridge Controls
        item {
            SettingsSectionLabel(title = "الأتمتة والربط أوفلاين")
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = c.card,
                border = BorderStroke(1.dp, c.borderSubtle),
                shadowElevation = if (c.isDark) 0.dp else 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SettingsSwitchRow(
                        icon = Icons.Filled.AutoAwesome,
                        iconTint = c.accent,
                        title = "تحليل النوافذ الجديدة تلقائياً",
                        subtitle = "فحص الصفحة واكتشاف العناصر بمجرد إضافتها",
                        checked = autoAnalyzeNewTabs,
                        onCheckedChange = { viewModel.setAutoAnalyzeNewTabs(it) }
                    )

                    HorizontalDivider(
                        color = c.borderSubtle,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    SettingsSwitchRow(
                        icon = Icons.Filled.Sensors,
                        iconTint = if (isObserverActive) c.success else c.textMuted,
                        title = "مراقب الخلفية لجسر التطبيقات",
                        subtitle = "مزامنة فورية عبر ContentObserver و WorkManager",
                        checked = isObserverActive,
                        onCheckedChange = { viewModel.toggleBridgeBackgroundObservation() }
                    )

                    HorizontalDivider(
                        color = c.borderSubtle,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    SettingsActionRow(
                        icon = Icons.Filled.SyncAlt,
                        iconTint = c.accentSecondary,
                        title = "إدارة جسر التبادل أوفلاين",
                        subtitle = "الحالة الحالية: ${bridgeStatus.labelArabic}",
                        badge = if (bridgeStatus == BridgeStatus.CONNECTED) "متصل" else null,
                        onClick = onOpenBridge
                    )

                    HorizontalDivider(
                        color = c.borderSubtle,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    SettingsActionRow(
                        icon = Icons.Filled.TravelExplore,
                        iconTint = c.accent,
                        title = "مستخرج OpenGraph وعناوين الصفحات",
                        subtitle = "اختبار سحب الميتاداتا وحفظها في Room",
                        onClick = { onOpenOpenGraphScraper("") }
                    )
                }
            }
        }

        // 3. Room Storage & Quick Maintenance
        item {
            SettingsSectionLabel(title = "إدارة التخزين المحلي (Room)")
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = c.card,
                border = BorderStroke(1.dp, c.borderSubtle),
                shadowElevation = if (c.isDark) 0.dp else 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    SettingsActionRow(
                        icon = Icons.Filled.VerifiedUser,
                        iconTint = c.success,
                        title = "فحص سلامة قاعدة بيانات Room والباكيند",
                        subtitle = "التحقق من المعاملات والربط ومحرك AI الآن",
                        onClick = {
                            scope.launch {
                                val ok = viewModel.socialMediaScraper.testDatabasePipeline()
                                viewModel.runBackendDiagnostics()
                                Toast.makeText(
                                    context,
                                    if (ok) "✅ قاعدة بيانات Room ومحرك الباكيند يعملان بكفاءة" else "❌ تعذر فحص قاعدة البيانات",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )

                    HorizontalDivider(
                        color = c.borderSubtle,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    SettingsActionRow(
                        icon = Icons.Filled.FilterAltOff,
                        iconTint = c.accentSecondary,
                        title = "إعادة ضبط فلاتر جميع الأقسام",
                        subtitle = "إظهار جميع الصفحات والسجلات والمنشورات",
                        onClick = {
                            viewModel.resetAllSectionFilters()
                            Toast.makeText(context, "تمت إعادة ضبط جميع الفلاتر", Toast.LENGTH_SHORT).show()
                        }
                    )

                    HorizontalDivider(
                        color = c.borderSubtle,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    SettingsActionRow(
                        icon = Icons.Filled.Bookmarks,
                        iconTint = c.error,
                        title = "مسح سجل الصفحات المزارة",
                        subtitle = "$recentPagesCount صفحة محفوظة حالياً",
                        onClick = { pendingClearTarget = "RECENT" }
                    )

                    HorizontalDivider(
                        color = c.borderSubtle,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    SettingsActionRow(
                        icon = Icons.Filled.HistoryEdu,
                        iconTint = c.error,
                        title = "مسح سجل مهام الذكاء الاصطناعي",
                        subtitle = "${logs.size} سجل مهمة محفوظ",
                        onClick = { pendingClearTarget = "LOGS" }
                    )

                    HorizontalDivider(
                        color = c.borderSubtle,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    SettingsActionRow(
                        icon = Icons.Filled.DeleteSweep,
                        iconTint = c.error,
                        title = "مسح المنشورات المستخرجة",
                        subtitle = "${posts.size} منشور محفوظ",
                        onClick = { pendingClearTarget = "POSTS" }
                    )
                }
            }
        }
    }

    if (pendingClearTarget != null) {
        val label = when (pendingClearTarget) {
            "RECENT" -> "سجل الصفحات المزارة أوفلاين"
            "LOGS" -> "سجل مهام الذكاء الاصطناعي"
            else -> "جميع المنشورات المستخرجة"
        }
        AlertDialog(
            onDismissRequest = { pendingClearTarget = null },
            containerColor = c.card,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "تأكيد المسح",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = c.textPrimary
                )
            },
            text = {
                Text(
                    text = "هل تريد حذف $label من قاعدة بيانات Room؟ لا يمكن التراجع عن هذا الإجراء.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (pendingClearTarget) {
                            "RECENT" -> viewModel.clearAllRecentPages()
                            "LOGS" -> viewModel.clearAllTaskLogs()
                            "POSTS" -> viewModel.clearAllScrapedPosts()
                        }
                        Toast.makeText(context, "تم المسح بنجاح", Toast.LENGTH_SHORT).show()
                        pendingClearTarget = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.error, contentColor = Color.White)
                ) {
                    Text("نعم، امسح")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingClearTarget = null }) {
                    Text("إلغاء", color = c.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionLabel(title: String) {
    val c = AppTheme.colors
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = c.textSecondary,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun ThemeModeOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) c.accentSoft else c.cardElevated,
        border = BorderStroke(1.5.dp, if (selected) c.accent else c.borderSubtle),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (selected) c.accent else c.card,
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
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "محدد",
                        tint = c.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) c.accent else c.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = c.textMuted
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val c = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = c.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textMuted
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = c.accent
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    val c = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = c.textPrimary
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = c.successSubtle
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = c.success,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textMuted
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronLeft,
            contentDescription = null,
            tint = c.textMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}
