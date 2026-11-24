package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bridge.AppBridgeContract
import com.example.bridge.BridgeExchangeEvent
import com.example.bridge.BridgeStatus
import com.example.bridge.ExchangeDirection
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBridgeDialog(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val connectionStatus by viewModel.bridgeStatus.collectAsState()
    val statusDetail by viewModel.bridgeStatusDetail.collectAsState()
    val targetAuthority by viewModel.bridgeTargetAuthority.collectAsState()
    val exchangeEvents by viewModel.bridgeEvents.collectAsState()
    val lastSync by viewModel.bridgeLastSync.collectAsState()
    val isObserverActive by viewModel.isBridgeObserverActive.collectAsState()
    val pairedPartnerToken by viewModel.pairedPartnerToken.collectAsState()

    var partnerIdInput by remember { mutableStateOf(pairedPartnerToken ?: "") }
    var authorityInput by remember(targetAuthority) { mutableStateOf(targetAuthority) }
    var showPromptCopyDialog by remember { mutableStateOf(false) }
    var showAdvancedSettings by remember { mutableStateOf(false) }
    var selectedDataType by remember { mutableStateOf(AppBridgeContract.TYPE_POSTS) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(20.dp),
            color = DarkBackground,
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
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
                            color = AccentIndigo.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, CardBorderSubtle),
                            modifier = Modifier.size(38.dp)
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
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "جسر الربط السريع بالمعرّف (App ID)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "ربط وتزامن تلقائي أوفلاين بين تطبيقين على نفس الهاتف",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = TextMuted)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card 1: My App Pairing ID (الخطوة 1: معرّف هذا التطبيق)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "معرّف هذا التطبيق (My App ID):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NeonCyan.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "جاهز للمشاركة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Box showing the App ID with 1-click copy
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkBackground,
                            border = BorderStroke(1.dp, CardBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = viewModel.ourShortId,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "رمز الربط: ${viewModel.ourPairingToken.take(24)}...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Social Agent Bridge ID", viewModel.ourPairingToken)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "تم نسخ معرّف الربط! الصقه في التطبيق الآخر", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("نسخ معرّفي", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Card 2: Partner App ID Input (الخطوة 2: إدخال معرّف التطبيق الآخر للربط)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, if (pairedPartnerToken != null) EmeraldGreen.copy(alpha = 0.5f) else CardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "معرّف التطبيق الآخر (Partner App ID):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            if (pairedPartnerToken != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EmeraldGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "مرتبط بنجاح 🔗",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = partnerIdInput,
                                onValueChange = { partnerIdInput = it },
                                placeholder = {
                                    Text(
                                        "الصق معرّف التطبيق الآخر هنا...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = DarkBackground,
                                    unfocusedContainerColor = DarkBackground
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))

                            // Paste Button
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                    if (clipText.isNotBlank()) {
                                        partnerIdInput = clipText.trim()
                                        Toast.makeText(context, "تم لصق المعرف", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "الحافظة فارغة", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CardSurfaceElevated)
                                    .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "لصق",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Pair / Unpair Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (partnerIdInput.isNotBlank()) {
                                        viewModel.pairWithPartner(partnerIdInput)
                                    } else {
                                        Toast.makeText(context, "يرجى كتابة أو لصق معرّف التطبيق الآخر أولاً", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إتمام الربط (Pair)", fontWeight = FontWeight.Bold)
                            }

                            if (pairedPartnerToken != null) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.unpairPartner()
                                        partnerIdInput = ""
                                        Toast.makeText(context, "تم فك الربط", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed)
                                ) {
                                    Text("فك الربط", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Connection Status & Background Sync Controls
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val statusColor = when (connectionStatus) {
                                    BridgeStatus.CONNECTED -> EmeraldGreen
                                    BridgeStatus.CONNECTING -> AccentAmber
                                    BridgeStatus.DISCONNECTED -> TextMuted
                                    BridgeStatus.ERROR, BridgeStatus.NOT_INSTALLED -> AlertRed
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = connectionStatus.labelArabic,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { viewModel.testBridgeConnection() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CardSurfaceElevated,
                                        contentColor = NeonCyan
                                    ),
                                    border = BorderStroke(1.dp, CardBorder),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("فحص", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = statusDetail,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = CardBorderSubtle, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Background Service & WorkManager auto-observation toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Sensors,
                                        contentDescription = null,
                                        tint = if (isObserverActive) EmeraldGreen else TextMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "المراقبة اللحظية في الخلفية (Background Observer)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = if (isObserverActive) "نشطة: ترصد التحديثات وتزامن Room وتوجه المتصفح تلقائياً" else "المراقبة في الخلفية متوقفة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isObserverActive) EmeraldGreen else TextMuted
                                )
                            }
                            Switch(
                                checked = isObserverActive,
                                onCheckedChange = { viewModel.toggleBridgeBackgroundObservation() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonCyan,
                                    checkedTrackColor = AccentIndigo
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Manual Data Exchange Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.pullDataFromCompanion(selectedDataType) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldGreen.copy(alpha = 0.15f),
                                    contentColor = EmeraldGreen
                                ),
                                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("سحب البيانات", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.pushDataToCompanion(selectedDataType) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan.copy(alpha = 0.15f),
                                    contentColor = NeonCyan
                                ),
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Upload,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إرسال البيانات", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Button to view/copy prompt for Gemini in the other app
                        OutlinedButton(
                            onClick = { showPromptCopyDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CardBorderSubtle),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "نسخ برومبت التنسيق لجيمناي في التطبيق الآخر 📋",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Exchange History / Audit Log
                Text(
                    text = "سجل تبادل البيانات المباشر (${exchangeEvents.size}):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (exchangeEvents.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CardSurface,
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "لا توجد عمليات تبادل حتى الآن",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                                Text(
                                    text = "الصق معرّف التطبيق الآخر واضغط 'إتمام الربط' للبدء.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(exchangeEvents, key = { it.id }) { event ->
                            ExchangeEventItem(event)
                        }
                    }
                }
            }
        }
    }

    if (showPromptCopyDialog) {
        GeminiPromptDialog(
            ourShortId = viewModel.ourShortId,
            ourPairingToken = viewModel.ourPairingToken,
            ourAuthority = AppBridgeContract.OUR_DEFAULT_AUTHORITY,
            onDismiss = { showPromptCopyDialog = false }
        )
    }
}

@Composable
fun ExchangeEventItem(event: BridgeExchangeEvent) {
    val isIncoming = event.direction == ExchangeDirection.INCOMING
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, if (event.isSuccess) CardBorderSubtle else AlertRed.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isIncoming) EmeraldGreen.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isIncoming) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (isIncoming) EmeraldGreen else NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = event.summary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${event.dataType} • ${event.itemCount} عنصر • ${sdf.format(Date(event.timestamp))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (event.isSuccess) EmeraldGreen.copy(alpha = 0.15f) else AlertRed.copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (event.isSuccess) "نجاح" else "فشل",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (event.isSuccess) EmeraldGreen else AlertRed,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun GeminiPromptDialog(
    ourShortId: String,
    ourPairingToken: String,
    ourAuthority: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val promptText = remember {
        """
أريد تطبيق نظام "الربط السريع بالمعرّف الفريد (Pair by App ID)" للربط محلياً أوفلاين مع تطبيق Social Agent المثبت على نفس الهاتف:

1. معرّف تطبيق Social Agent هو:
   $ourPairingToken (أو الكود القصير: $ourShortId)
   مزود الخدمة الخاص به (Authority):
   $ourAuthority

2. المطلوب تنفيذه في هذا التطبيق:
   أ) أن يكون لهذا التطبيق أيضاً معرّف ربط فريد خاص به، ومزود خدمة ContentProvider:
      Authority: com.example.companion.bridge.provider
      في AndroidManifest.xml:
      <manifest>
          <queries>
              <package android:name="com.aistudio.socialagent.kzxmbq" />
              <provider android:authorities="$ourAuthority" />
          </queries>
          <application>
              <provider
                  android:name=".bridge.CompanionBridgeProvider"
                  android:authorities="com.example.companion.bridge.provider"
                  android:exported="true" />
          </application>
      </manifest>

   ب) واجهة مستخدم مبسطة جداً:
      - زر "نسخ معرّفي (Copy My ID)": ينسخ معرّف هذا التطبيق للحافظة لمشاركته مع Social Agent.
      - خانة "لصق معرّف Social Agent": يضع فيها المستخدم معرّف Social Agent ($ourShortId أو $ourPairingToken).
      - زر "إتمام الربط (Pair)": يستخرج الـ Authority ويقوم بفحص الاتصال (Ping) فوراً.

   ج) دعم أساليب التبادل (call methods):
      - "ping": ترجع نجاح واسم التطبيق.
      - "pull_data": ترجع بيانات هذا التطبيق بصيغة JSON في payload_json.
      - "push_data": تستقبل JSON من Social Agent وتحفظه في Room محلياً.

   د) المزامنة اللحظية (Real-time Sync):
      - عند حفظ أي بيانات جديدة محلياً، استدعِ فوراً:
        context.contentResolver.notifyChange(Uri.parse("content://com.example.companion.bridge.provider"), null)
      - لتقوم خدمة الخلفية في Social Agent برصد التعديل وتوجيه المتصفح تلقائياً.
        """.trimIndent()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "برومبت التنسيق لجيمناي",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "انسخ هذا النص والصقه لجيمناي في التطبيق الآخر ليتفق مع هذا التطبيق على نفس نظام تبادل الـ ID:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkBackground,
                    border = BorderStroke(1.dp, CardBorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(10.dp)) {
                        item {
                            Text(
                                text = promptText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Gemini Bridge Prompt", promptText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "تم نسخ البرومبت بنجاح! جاهز للصق في التطبيق الآخر", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("نسخ البرومبت إلى الحافظة", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
