package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.theme.AppTheme
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBridgeDialog(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit
) {
    val c = AppTheme.colors
    val context = LocalContext.current
    val connectionStatus by viewModel.bridgeStatus.collectAsState()
    val statusDetail by viewModel.bridgeStatusDetail.collectAsState()
    val exchangeEvents by viewModel.bridgeEvents.collectAsState()
    val isObserverActive by viewModel.isBridgeObserverActive.collectAsState()
    val pairedPartnerToken by viewModel.pairedPartnerToken.collectAsState()

    var partnerIdInput by remember { mutableStateOf(pairedPartnerToken ?: "") }
    var showPromptCopyDialog by remember { mutableStateOf(false) }
    var selectedDataType by remember { mutableStateOf(AppBridgeContract.TYPE_POSTS) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(22.dp),
            color = c.background,
            border = BorderStroke(1.dp, c.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = c.accentSoft,
                            modifier = Modifier.size(40.dp)
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
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "جسر الربط السريع بالمعرّف (App ID)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = c.textPrimary
                            )
                            Text(
                                text = "ربط وتزامن تلقائي أوفلاين على نفس الهاتف",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textMuted
                            )
                        }
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = c.textMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card 1: My App Pairing ID
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = c.card,
                    border = BorderStroke(1.dp, c.borderSubtle)
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
                                color = c.textSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = c.accentSoft
                            ) {
                                Text(
                                    text = "جاهز للمشاركة",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = c.accent,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = c.cardElevated,
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = viewModel.ourShortId,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = c.textPrimary
                                    )
                                    Text(
                                        text = "رمز الربط: ${viewModel.ourPairingToken.take(24)}...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = c.textMuted,
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
                                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp)
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

                // Card 2: Partner App ID Input
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = c.card,
                    border = BorderStroke(1.dp, if (pairedPartnerToken != null) c.success.copy(alpha = 0.5f) else c.borderSubtle)
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
                                color = c.textSecondary
                            )
                            if (pairedPartnerToken != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = c.successSubtle
                                ) {
                                    Text(
                                        text = "مرتبط بنجاح",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = c.success,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
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
                                        color = c.textMuted
                                    )
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = c.accent,
                                    unfocusedBorderColor = c.border,
                                    focusedTextColor = c.textPrimary,
                                    unfocusedTextColor = c.textPrimary,
                                    focusedContainerColor = c.cardElevated,
                                    unfocusedContainerColor = c.cardElevated
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))

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
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(c.cardElevated)
                                    .border(1.dp, c.border, RoundedCornerShape(10.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "لصق",
                                    tint = c.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

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
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White)
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
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, c.error.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = c.error)
                                ) {
                                    Text("فك الربط", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Connection Status & Sync Controls
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = c.card,
                    border = BorderStroke(1.dp, c.borderSubtle)
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
                                    BridgeStatus.CONNECTED -> c.success
                                    BridgeStatus.CONNECTING -> c.warning
                                    BridgeStatus.DISCONNECTED -> c.textMuted
                                    BridgeStatus.ERROR, BridgeStatus.NOT_INSTALLED -> c.error
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

                            Button(
                                onClick = { viewModel.testBridgeConnection() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = c.accentSoft,
                                    contentColor = c.accent
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فحص", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = statusDetail,
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = c.borderSubtle, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

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
                                        tint = if (isObserverActive) c.success else c.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "المراقبة اللحظية في الخلفية",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = c.textPrimary
                                    )
                                }
                                Text(
                                    text = if (isObserverActive) "نشطة: ترصد التحديثات وتزامن Room تلقائياً" else "المراقبة في الخلفية متوقفة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isObserverActive) c.success else c.textMuted
                                )
                            }
                            Switch(
                                checked = isObserverActive,
                                onCheckedChange = { viewModel.toggleBridgeBackgroundObservation() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = c.accent
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.pullDataFromCompanion(selectedDataType) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = c.successSubtle,
                                    contentColor = c.success
                                ),
                                border = BorderStroke(1.dp, c.success.copy(alpha = 0.35f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("سحب البيانات", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.pushDataToCompanion(selectedDataType) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = c.accentSoft,
                                    contentColor = c.accent
                                ),
                                border = BorderStroke(1.dp, c.accent.copy(alpha = 0.35f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Upload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إرسال البيانات", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedButton(
                            onClick = { showPromptCopyDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, c.border),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = c.textPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = c.accent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "نسخ برومبت التنسيق للتطبيق الآخر",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "سجل تبادل البيانات المباشر (${exchangeEvents.size}):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = c.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (exchangeEvents.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = c.card,
                        border = BorderStroke(1.dp, c.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    tint = c.textMuted,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "لا توجد عمليات تبادل حتى الآن",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.textMuted
                                )
                                Text(
                                    text = "الصق معرّف التطبيق الآخر واضغط 'إتمام الربط' للبدء.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.textMuted
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
    val c = AppTheme.colors
    val isIncoming = event.direction == ExchangeDirection.INCOMING
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = c.card,
        border = BorderStroke(1.dp, if (event.isSuccess) c.borderSubtle else c.error.copy(alpha = 0.5f))
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
                    color = if (isIncoming) c.successSubtle else c.accentSoft,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isIncoming) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (isIncoming) c.success else c.accent,
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
                        color = c.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${event.dataType} • ${event.itemCount} عنصر • ${sdf.format(Date(event.timestamp))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textMuted
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (event.isSuccess) c.successSubtle else c.errorSubtle
            ) {
                Text(
                    text = if (event.isSuccess) "نجاح" else "فشل",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (event.isSuccess) c.success else c.error,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
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
    val c = AppTheme.colors
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
            shape = RoundedCornerShape(18.dp),
            color = c.surface,
            border = BorderStroke(1.dp, c.border),
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
                        text = "برومبت التنسيق للتطبيق الآخر",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = c.textPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = c.textMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "انسخ هذا النص لتطبيق نفس نظام تبادل الـ ID في التطبيق الشريك:",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = c.cardElevated,
                    border = BorderStroke(1.dp, c.borderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(10.dp)) {
                        item {
                            Text(
                                text = promptText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp
                                ),
                                color = c.textPrimary
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
                        Toast.makeText(context, "تم نسخ البرومبت بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("نسخ البرومبت إلى الحافظة", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
