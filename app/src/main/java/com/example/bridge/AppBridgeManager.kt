package com.example.bridge

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.database.AppDatabase
import com.example.database.entity.BrowserTabEntity
import com.example.database.entity.RecentPageEntity
import com.example.database.entity.ScrapedPostMetadataEntity
import com.example.database.entity.TabTaskLogEntity
import com.example.database.repository.SocialAgentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * AppBridgeManager manages persistent offline IPC operations between
 * this app and any companion Android application on the same device.
 */
class AppBridgeManager private constructor(private val appContext: Context) {

    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(appContext)
    private val repository = SocialAgentRepository(database)

    // Persisted Target Companion Authority
    private val _targetAuthority = MutableStateFlow(
        prefs.getString(KEY_PREF_AUTHORITY, AppBridgeContract.COMPANION_DEFAULT_AUTHORITY)
            ?: AppBridgeContract.COMPANION_DEFAULT_AUTHORITY
    )
    val targetAuthority: StateFlow<String> = _targetAuthority.asStateFlow()

    val ourShortId: String = AppBridgeContract.OUR_APP_SHORT_ID
    val ourPairingToken: String = AppBridgeContract.generateOurPairingToken()

    private val _pairedPartnerToken = MutableStateFlow<String?>(
        prefs.getString(KEY_PREF_PARTNER_TOKEN, null)
    )
    val pairedPartnerToken: StateFlow<String?> = _pairedPartnerToken.asStateFlow()

    // Connection Status
    private val _connectionStatus = MutableStateFlow(BridgeStatus.DISCONNECTED)
    val connectionStatus: StateFlow<BridgeStatus> = _connectionStatus.asStateFlow()

    private val _statusDetail = MutableStateFlow(
        if (_pairedPartnerToken.value != null) {
            "معرّف الشريك محفوظ (${_pairedPartnerToken.value}) — جاهز للمزامنة أوفلاين"
        } else {
            "الجسر جاهز للاتصال المحلي أوفلاين"
        }
    )
    val statusDetail: StateFlow<String> = _statusDetail.asStateFlow()

    // Last Sync timestamp
    private val _lastSyncTimestamp = MutableStateFlow<Long?>(
        prefs.getLong(KEY_PREF_LAST_SYNC, 0L).takeIf { it > 0L }
    )
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    // Live Exchange Events History
    private val _exchangeEvents = MutableStateFlow<List<BridgeExchangeEvent>>(emptyList())
    val exchangeEvents: StateFlow<List<BridgeExchangeEvent>> = _exchangeEvents.asStateFlow()

    // Background Observation Active State (persisted)
    private val _isBackgroundObservationActive = MutableStateFlow(
        prefs.getBoolean(KEY_PREF_BG_OBSERVER, true)
    )
    val isBackgroundObservationActive: StateFlow<Boolean> = _isBackgroundObservationActive.asStateFlow()

    // Real-time Orchestration Listener for SocialAgent Browser
    var onDataOrchestratedListener: ((count: Int, summary: String) -> Unit)? = null

    private var contentObserver: ContentObserver? = null

    init {
        val authority = _targetAuthority.value
        val providerInfo = resolveProviderSafe(authority)
        if (providerInfo != null && _isBackgroundObservationActive.value) {
            registerObserverForCompanion()
            startBackgroundObservation()
        }
    }

    private fun resolveProviderSafe(authority: String) = try {
        appContext.packageManager.resolveContentProvider(authority, 0)
    } catch (_: Exception) {
        null
    }

    fun setTargetAuthority(authority: String) {
        val trimmed = authority.trim()
        if (trimmed.isNotEmpty() && trimmed != _targetAuthority.value) {
            _targetAuthority.value = trimmed
            prefs.edit().putString(KEY_PREF_AUTHORITY, trimmed).apply()
            registerObserverForCompanion()
            if (_isBackgroundObservationActive.value) {
                startBackgroundObservation()
            }
        }
    }

    fun startBackgroundObservation() {
        _isBackgroundObservationActive.value = true
        prefs.edit().putBoolean(KEY_PREF_BG_OBSERVER, true).apply()
        val authority = _targetAuthority.value
        val providerInfo = resolveProviderSafe(authority)

        if (providerInfo != null) {
            BridgeContentObserverService.startService(appContext, authority)
            BridgeSyncWorker.scheduleContentTrigger(appContext, authority)
            recordEvent(
                BridgeExchangeEvent(
                    direction = ExchangeDirection.INCOMING,
                    dataType = "AUTO_OBSERVER",
                    summary = "تم تفعيل خدمة مراقبة مزود البيانات في الخلفية وWorkManager بنجاح",
                    itemCount = 1
                )
            )
        } else {
            Log.d("AppBridgeManager", "Companion provider ($authority) is not installed yet. Standing by for pairing.")
        }
    }

    fun stopBackgroundObservation() {
        _isBackgroundObservationActive.value = false
        prefs.edit().putBoolean(KEY_PREF_BG_OBSERVER, false).apply()
        BridgeContentObserverService.stopService(appContext)
        BridgeSyncWorker.cancelObservation(appContext)
        recordEvent(
            BridgeExchangeEvent(
                direction = ExchangeDirection.INCOMING,
                dataType = "AUTO_OBSERVER",
                summary = "تم إيقاف خدمة المراقبة في الخلفية مؤقتاً",
                itemCount = 0
            )
        )
    }

    suspend fun pairWithPartner(inputToken: String): Boolean {
        val trimmed = inputToken.trim()
        if (trimmed.isEmpty()) return false

        val authority = AppBridgeContract.resolveAuthorityFromInput(trimmed)
        _pairedPartnerToken.value = trimmed
        prefs.edit().putString(KEY_PREF_PARTNER_TOKEN, trimmed).apply()
        setTargetAuthority(authority)

        val pingSuccess = pingCompanion()
        if (pingSuccess) {
            startBackgroundObservation()
            recordEvent(
                BridgeExchangeEvent(
                    direction = ExchangeDirection.INCOMING,
                    dataType = "PAIRING",
                    summary = "✅ تم الربط والحفظ الدائم مع المعرف: $trimmed",
                    itemCount = 1,
                    isSuccess = true
                )
            )
        } else {
            recordEvent(
                BridgeExchangeEvent(
                    direction = ExchangeDirection.INCOMING,
                    dataType = "PAIRING",
                    summary = "⚠️ تم حفظ المعرف: $trimmed، بانتظار تشغيل التطبيق الآخر",
                    itemCount = 0,
                    isSuccess = false
                )
            )
        }
        return pingSuccess
    }

    fun unpairPartner() {
        _pairedPartnerToken.value = null
        prefs.edit().remove(KEY_PREF_PARTNER_TOKEN).apply()
        stopBackgroundObservation()
        _connectionStatus.value = BridgeStatus.DISCONNECTED
        _statusDetail.value = "تم فك الربط، أدخل معرّف التطبيق الآخر لإعادة التفعيل"
    }

    private fun registerObserverForCompanion() {
        try {
            contentObserver?.let {
                appContext.contentResolver.unregisterContentObserver(it)
            }
            contentObserver = null

            val authority = _targetAuthority.value
            val providerInfo = resolveProviderSafe(authority) ?: run {
                Log.d("AppBridgeManager", "Companion provider ($authority) is not installed on this device yet. Standing by.")
                return
            }

            val targetUri = AppBridgeContract.buildUri(authority)
            contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    Log.d("AppBridgeManager", "Received ContentObserver change notification from companion app: $uri")
                    recordEvent(
                        BridgeExchangeEvent(
                            direction = ExchangeDirection.INCOMING,
                            dataType = "NOTIFICATION",
                            summary = "إشعار فوري: التطبيق الآخر قام بتحديث بيانات أوفلاين",
                            itemCount = 1
                        )
                    )
                }
            }
            appContext.contentResolver.registerContentObserver(targetUri, true, contentObserver!!)
        } catch (e: SecurityException) {
            Log.d("AppBridgeManager", "Companion provider is not available yet: ${e.message}")
        } catch (e: Exception) {
            Log.w("AppBridgeManager", "Could not register observer: ${e.message}")
        }
    }

    fun recordEvent(event: BridgeExchangeEvent) {
        _exchangeEvents.value = (listOf(event) + _exchangeEvents.value).take(50)
        _lastSyncTimestamp.value = event.timestamp
        prefs.edit().putLong(KEY_PREF_LAST_SYNC, event.timestamp).apply()
    }

    /**
     * Pings the companion application using ContentResolver.call()
     */
    suspend fun pingCompanion(): Boolean {
        _connectionStatus.value = BridgeStatus.CONNECTING
        _statusDetail.value = "جاري فحص الاتصال مع التطبيق الآخر محلياً..."
        return try {
            val uri = AppBridgeContract.buildUri(_targetAuthority.value)
            val result: Bundle? = appContext.contentResolver.call(
                uri,
                AppBridgeContract.METHOD_PING,
                null,
                Bundle().apply {
                    putString(AppBridgeContract.KEY_SENDER, "SocialAgent")
                    putString(AppBridgeContract.KEY_SECRET_TOKEN, ourPairingToken)
                }
            )

            if (result != null && result.getString(AppBridgeContract.KEY_STATUS) == AppBridgeContract.STATUS_SUCCESS) {
                val companionName = result.getString(AppBridgeContract.KEY_SENDER) ?: "التطبيق الآخر"
                val message = result.getString(AppBridgeContract.KEY_MESSAGE) ?: "متصل"
                _connectionStatus.value = BridgeStatus.CONNECTED
                _statusDetail.value = "✅ متصل بنجاح مع [$companionName]: $message"

                recordEvent(
                    BridgeExchangeEvent(
                        direction = ExchangeDirection.INCOMING,
                        dataType = "PING_RESPONSE",
                        summary = "استجابة فحص الاتصال من $companionName",
                        itemCount = 1,
                        isSuccess = true
                    )
                )
                true
            } else {
                _connectionStatus.value = BridgeStatus.ERROR
                _statusDetail.value = "⚠️ تعذر الاتصال: لم يرد التطبيق الآخر بنجاح"
                false
            }
        } catch (e: SecurityException) {
            _connectionStatus.value = BridgeStatus.ERROR
            _statusDetail.value = "❌ خطأ صلاحية (SecurityException): تأكد من تصدير Provider التطبيق الآخر"
            false
        } catch (e: IllegalArgumentException) {
            _connectionStatus.value = BridgeStatus.NOT_INSTALLED
            _statusDetail.value = "⚠️ التطبيق الآخر غير مثبت أو اسم الـ Authority غير متطابق (${_targetAuthority.value})"
            false
        } catch (e: Exception) {
            _connectionStatus.value = BridgeStatus.ERROR
            _statusDetail.value = "❌ خطأ في الاتصال: ${e.localizedMessage ?: e.message}"
            false
        }
    }

    /**
     * Pulls data from the companion app and imports it atomically into Room.
     */
    suspend fun pullDataFromCompanion(dataType: String = AppBridgeContract.TYPE_POSTS): Result<Int> {
        return try {
            val uri = AppBridgeContract.buildUri(_targetAuthority.value)
            val extras = Bundle().apply {
                putString(AppBridgeContract.KEY_DATA_TYPE, dataType)
                putString(AppBridgeContract.KEY_SENDER, "SocialAgent")
                putString(AppBridgeContract.KEY_SECRET_TOKEN, ourPairingToken)
            }

            val result = appContext.contentResolver.call(
                uri,
                AppBridgeContract.METHOD_PULL_DATA,
                dataType,
                extras
            ) ?: return Result.failure(Exception("لم يرجع التطبيق الآخر أي استجابة"))

            val payloadJson = result.getString(AppBridgeContract.KEY_PAYLOAD_JSON)
            if (payloadJson.isNullOrEmpty()) {
                recordEvent(
                    BridgeExchangeEvent(
                        direction = ExchangeDirection.INCOMING,
                        dataType = dataType,
                        summary = "سحب بيانات: لا توجد عناصر جديدة في التطبيق الآخر",
                        itemCount = 0,
                        isSuccess = true
                    )
                )
                return Result.success(0)
            }

            val (ok, importedCount) = handleInboundPush(dataType, payloadJson)
            if (!ok) {
                return Result.failure(Exception("تنسيق البيانات المستلمة غير صالح"))
            }

            database.tabTaskLogDao().insertLog(
                TabTaskLogEntity(
                    id = UUID.randomUUID().toString(),
                    tabId = "bridge_import",
                    tabTitle = "جسر التبادل المحلي",
                    tabUrl = "app://bridge/${_targetAuthority.value}",
                    taskPrompt = "سحب بيانات أوفلاين ($dataType) من التطبيق الشريك",
                    status = "COMPLETED",
                    outputResult = "✅ تم استيراد $importedCount عنصر أوفلاين بنجاح وحفظها في Room DB.",
                    tokensUsed = 0
                )
            )

            _connectionStatus.value = BridgeStatus.CONNECTED
            _statusDetail.value = "✅ تم استيراد $importedCount عنصر بنجاح من التطبيق الآخر"
            Result.success(importedCount)
        } catch (e: Exception) {
            recordEvent(
                BridgeExchangeEvent(
                    direction = ExchangeDirection.INCOMING,
                    dataType = dataType,
                    summary = "فشل سحب البيانات: ${e.message}",
                    itemCount = 0,
                    isSuccess = false
                )
            )
            Result.failure(e)
        }
    }

    /**
     * Pushes this app's scraped posts or recent leads to the companion app.
     */
    suspend fun pushDataToCompanion(dataType: String = AppBridgeContract.TYPE_POSTS): Result<Int> {
        return try {
            val payloadJson = handleInboundPull(dataType)
            val parsedArray = JSONArray(payloadJson)
            if (parsedArray.length() == 0) {
                return Result.failure(Exception("لا توجد بيانات متاحة حالياً من النوع ($dataType) لإرسالها"))
            }

            val uri = AppBridgeContract.buildUri(_targetAuthority.value)
            val extras = Bundle().apply {
                putString(AppBridgeContract.KEY_DATA_TYPE, dataType)
                putString(AppBridgeContract.KEY_SENDER, "SocialAgent")
                putString(AppBridgeContract.KEY_SECRET_TOKEN, ourPairingToken)
                putString(AppBridgeContract.KEY_PAYLOAD_JSON, payloadJson)
                putInt(AppBridgeContract.KEY_ITEM_COUNT, parsedArray.length())
            }

            val result = appContext.contentResolver.call(
                uri,
                AppBridgeContract.METHOD_PUSH_DATA,
                dataType,
                extras
            )

            val isSuccess = result?.getString(AppBridgeContract.KEY_STATUS) == AppBridgeContract.STATUS_SUCCESS
            val count = result?.getInt(AppBridgeContract.KEY_ITEM_COUNT) ?: parsedArray.length()

            recordEvent(
                BridgeExchangeEvent(
                    direction = ExchangeDirection.OUTGOING,
                    dataType = dataType,
                    summary = if (isSuccess) "تم إرسال ${parsedArray.length()} عنصر ($dataType) إلى التطبيق الآخر بنجاح" else "أرجع التطبيق الآخر استجابة غير مكتملة",
                    itemCount = count,
                    isSuccess = isSuccess
                )
            )

            if (isSuccess) {
                _statusDetail.value = "✅ تم إرسال ${parsedArray.length()} عنصر بنجاح للتطبيق الآخر"
                Result.success(count)
            } else {
                val msg = result?.getString(AppBridgeContract.KEY_MESSAGE) ?: "فشل في معالجة البيانات لدى التطبيق الآخر"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            recordEvent(
                BridgeExchangeEvent(
                    direction = ExchangeDirection.OUTGOING,
                    dataType = dataType,
                    summary = "فشل إرسال البيانات: ${e.message}",
                    itemCount = 0,
                    isSuccess = false
                )
            )
            Result.failure(e)
        }
    }

    /**
     * Handles incoming data pushed into this app by the companion app across multiple entity types.
     */
    suspend fun handleInboundPush(dataType: String?, payloadJson: String?): Pair<Boolean, Int> {
        if (payloadJson.isNullOrEmpty() || payloadJson.length > MAX_PAYLOAD_CHARS) {
            return Pair(false, 0)
        }
        return try {
            val jsonArray = JSONArray(payloadJson)
            var count = 0
            val normalizedType = dataType?.uppercase() ?: AppBridgeContract.TYPE_POSTS

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val id = obj.optString("id").ifBlank { UUID.randomUUID().toString() }

                when (normalizedType) {
                    AppBridgeContract.TYPE_BROWSER_TABS -> {
                        val url = obj.optString("url").ifBlank { continue }
                        val title = obj.optString("title", url)
                        repository.insertTab(
                            BrowserTabEntity(
                                id = id,
                                title = title,
                                url = url,
                                analysisSummary = obj.optString("summary", ""),
                                userPrompt = obj.optString("prompt", ""),
                                status = obj.optString("status", "READY_FOR_PROMPT")
                            )
                        )
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            repository.recordPageVisit(url = url, title = title)
                        }
                        count++
                    }

                    AppBridgeContract.TYPE_LEADS -> {
                        val url = obj.optString("url", "app://companion/lead/$id")
                        val title = obj.optString("title", obj.optString("name", "عميل محتمل"))
                        val snippet = obj.optString("content", obj.optString("notes", ""))
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            repository.recordPageVisit(
                                url = url,
                                title = title,
                                snippet = snippet
                            )
                        } else {
                            repository.insertRecentPage(
                                RecentPageEntity(
                                    id = id,
                                    url = url,
                                    title = title,
                                    platform = obj.optString("platform", "OFFLINE_APP"),
                                    previewSnippet = snippet
                                )
                            )
                        }
                        count++
                    }

                    else -> {
                        val content = obj.optString("content", obj.optString("text", obj.optString("title", "")))
                        val platform = obj.optString("platform", "COMPANION_IMPORT")
                        val author = obj.optString("author", "تطبيق مرافق")
                        val url = obj.optString("url", "app://companion/$id")

                        val post = ScrapedPostMetadataEntity(
                            id = id,
                            platform = platform,
                            originalPostUrl = url,
                            authorDisplayName = author,
                            authorHandle = author.lowercase().replace(" ", "_"),
                            postContent = content,
                            likesCount = obj.optInt("likes", 0),
                            commentsCount = obj.optInt("comments", 0),
                            tagsCsv = obj.optString("tags", "مستلم_أوفلاين"),
                            scrapedTimestamp = System.currentTimeMillis()
                        )
                        repository.upsertScrapedPostByUrl(post)
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            repository.recordPageVisit(
                                url = url,
                                title = author.ifBlank { content.take(50) },
                                platform = platform,
                                snippet = content.take(180)
                            )
                        }
                        count++
                    }
                }
            }

            recordEvent(
                BridgeExchangeEvent(
                    direction = ExchangeDirection.INCOMING,
                    dataType = normalizedType,
                    summary = "استلام $count عنصر ($normalizedType) أوفلاين وتخزينها في Room",
                    itemCount = count,
                    isSuccess = true
                )
            )

            if (count > 0) {
                onDataOrchestratedListener?.invoke(
                    count,
                    "تم استقبال $count عنصر أوفلاين وتخزينها في قاعدة بيانات Room بنجاح."
                )
            }
            Pair(true, count)
        } catch (e: Exception) {
            Log.e("AppBridgeManager", "Error handling inbound push", e)
            Pair(false, 0)
        }
    }

    /**
     * Generates a JSON array of this app's data for an inbound pull query or outbound push.
     */
    suspend fun handleInboundPull(dataType: String?): String {
        val normalizedType = dataType?.uppercase() ?: AppBridgeContract.TYPE_POSTS
        val jsonArray = JSONArray()

        when (normalizedType) {
            AppBridgeContract.TYPE_BROWSER_TABS -> {
                val tabs = repository.getAllTabsSync().take(25)
                for (tab in tabs) {
                    jsonArray.put(JSONObject().apply {
                        put("id", tab.id)
                        put("title", tab.title)
                        put("url", tab.url)
                        put("summary", tab.analysisSummary)
                        put("prompt", tab.userPrompt)
                        put("status", tab.status)
                        put("timestamp", tab.timestamp)
                    })
                }
            }

            AppBridgeContract.TYPE_LEADS -> {
                val pages = repository.getAllRecentPagesSync().take(30)
                for (page in pages) {
                    jsonArray.put(JSONObject().apply {
                        put("id", page.id)
                        put("title", page.title)
                        put("url", page.url)
                        put("platform", page.platform)
                        put("content", page.previewSnippet)
                        put("visitCount", page.visitCount)
                        put("timestamp", page.lastVisitedTimestamp)
                    })
                }
            }

            else -> {
                val posts = database.postMetadataDao().getRecentPostsSync(30)
                for (p in posts) {
                    jsonArray.put(JSONObject().apply {
                        put("id", p.id)
                        put("platform", p.platform)
                        put("url", p.originalPostUrl)
                        put("content", p.postContent)
                        put("author", p.authorDisplayName)
                        put("likes", p.likesCount)
                        put("comments", p.commentsCount)
                        put("tags", p.tagsCsv)
                        put("timestamp", p.scrapedTimestamp)
                    })
                }
            }
        }

        recordEvent(
            BridgeExchangeEvent(
                direction = ExchangeDirection.OUTGOING,
                dataType = normalizedType,
                summary = "تجهيز وتصدير ${jsonArray.length()} عنصر ($normalizedType) عبر الجسر المحلي",
                itemCount = jsonArray.length(),
                isSuccess = true
            )
        )
        return jsonArray.toString()
    }

    companion object {
        private const val PREFS_NAME = "social_agent_bridge_prefs"
        private const val KEY_PREF_AUTHORITY = "target_authority"
        private const val KEY_PREF_PARTNER_TOKEN = "paired_partner_token"
        private const val KEY_PREF_BG_OBSERVER = "bg_observer_active"
        private const val KEY_PREF_LAST_SYNC = "last_sync_ts"
        private const val MAX_PAYLOAD_CHARS = 2_000_000

        @Volatile
        private var instance: AppBridgeManager? = null

        fun getInstance(context: Context): AppBridgeManager {
            return instance ?: synchronized(this) {
                instance ?: AppBridgeManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

enum class BridgeStatus(val labelArabic: String) {
    DISCONNECTED("غير متصل"),
    CONNECTING("جاري الفحص..."),
    CONNECTED("متصل محلياً (أوفلاين)"),
    NOT_INSTALLED("التطبيق غير متوفر أو مسجل"),
    ERROR("تعذر الاتصال")
}
