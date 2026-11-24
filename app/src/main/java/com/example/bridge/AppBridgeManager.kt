package com.example.bridge

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.database.AppDatabase
import com.example.database.entity.ScrapedPostMetadataEntity
import com.example.database.entity.TabTaskLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * AppBridgeManager manages all offline IPC operations between
 * this app and any companion Android application on the same device.
 */
class AppBridgeManager private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val database = AppDatabase.getDatabase(appContext)

    // Target Companion Authority (editable in UI)
    private val _targetAuthority = MutableStateFlow(AppBridgeContract.COMPANION_DEFAULT_AUTHORITY)
    val targetAuthority: StateFlow<String> = _targetAuthority.asStateFlow()

    val ourShortId: String = AppBridgeContract.OUR_APP_SHORT_ID
    val ourPairingToken: String = AppBridgeContract.generateOurPairingToken()

    private val _pairedPartnerToken = MutableStateFlow<String?>(null)
    val pairedPartnerToken: StateFlow<String?> = _pairedPartnerToken.asStateFlow()

    // Connection Status
    private val _connectionStatus = MutableStateFlow(BridgeStatus.DISCONNECTED)
    val connectionStatus: StateFlow<BridgeStatus> = _connectionStatus.asStateFlow()

    private val _statusDetail = MutableStateFlow("الجسر جاهز للاتصال المحلي أوفلاين")
    val statusDetail: StateFlow<String> = _statusDetail.asStateFlow()

    // Last Sync timestamp
    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    // Live Exchange Events History
    private val _exchangeEvents = MutableStateFlow<List<BridgeExchangeEvent>>(emptyList())
    val exchangeEvents: StateFlow<List<BridgeExchangeEvent>> = _exchangeEvents.asStateFlow()

    // Background Observation Active State
    private val _isBackgroundObservationActive = MutableStateFlow(true)
    val isBackgroundObservationActive: StateFlow<Boolean> = _isBackgroundObservationActive.asStateFlow()

    // Real-time Orchestration Listener for SocialAgent Browser
    var onDataOrchestratedListener: ((count: Int, summary: String) -> Unit)? = null

    private var contentObserver: ContentObserver? = null

    init {
        val authority = _targetAuthority.value
        val providerInfo = try {
            appContext.packageManager.resolveContentProvider(authority, 0)
        } catch (e: Exception) {
            null
        }
        if (providerInfo != null) {
            registerObserverForCompanion()
            startBackgroundObservation()
        }
    }

    fun setTargetAuthority(authority: String) {
        val trimmed = authority.trim()
        if (trimmed.isNotEmpty() && trimmed != _targetAuthority.value) {
            _targetAuthority.value = trimmed
            registerObserverForCompanion()
            if (_isBackgroundObservationActive.value) {
                startBackgroundObservation()
            }
        }
    }

    fun startBackgroundObservation() {
        _isBackgroundObservationActive.value = true
        val authority = _targetAuthority.value
        val providerInfo = try {
            appContext.packageManager.resolveContentProvider(authority, 0)
        } catch (e: Exception) {
            null
        }

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
        setTargetAuthority(authority)

        val pingSuccess = pingCompanion()
        if (pingSuccess) {
            startBackgroundObservation()
            recordEvent(
                BridgeExchangeEvent(
                    direction = ExchangeDirection.INCOMING,
                    dataType = "PAIRING",
                    summary = "✅ تم الربط بنجاح مع المعرف: $trimmed",
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
            val providerInfo = try {
                appContext.packageManager.resolveContentProvider(authority, 0)
            } catch (e: Exception) {
                null
            }

            if (providerInfo == null) {
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
            _statusDetail.value = "❌ خطأ صلاحية (SecurityException): تأكد من تصدير Provider التطبيق الآخر (android:exported=\"true\")"
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
     * Pulls data from the companion app and imports it into this app's database.
     */
    suspend fun pullDataFromCompanion(dataType: String = AppBridgeContract.TYPE_POSTS): Result<Int> {
        return try {
            val uri = AppBridgeContract.buildUri(_targetAuthority.value)
            val extras = Bundle().apply {
                putString(AppBridgeContract.KEY_DATA_TYPE, dataType)
                putString(AppBridgeContract.KEY_SENDER, "SocialAgent")
            }

            val result = appContext.contentResolver.call(
                uri,
                AppBridgeContract.METHOD_PULL_DATA,
                dataType,
                extras
            )

            if (result == null) {
                return Result.failure(Exception("لم يرجع التطبيق الآخر أي استجابة"))
            }

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

            val jsonArray = JSONArray(payloadJson)
            var importedCount = 0

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val platform = obj.optString("platform", "OFFLINE_APP")
                val content = obj.optString("content", obj.optString("text", ""))
                val url = obj.optString("url", "app://companion/$id")
                val author = obj.optString("author", "تطبيق مرافق")

                val entity = ScrapedPostMetadataEntity(
                    id = id,
                    platform = platform,
                    originalPostUrl = url,
                    authorDisplayName = author,
                    authorHandle = author.lowercase().replace(" ", "_"),
                    postContent = content,
                    likesCount = obj.optInt("likes", 0),
                    commentsCount = obj.optInt("comments", 0),
                    tagsCsv = obj.optString("tags", "وارد_من_التطبيق_الآخر"),
                    scrapedTimestamp = System.currentTimeMillis()
                )
                database.postMetadataDao().insertPost(entity)
                importedCount++
            }

            // Log import to TabTaskLog
            database.tabTaskLogDao().insertLog(
                TabTaskLogEntity(
                    id = UUID.randomUUID().toString(),
                    tabId = "bridge_import",
                    tabTitle = "جسر التبادل المحلي",
                    tabUrl = "app://bridge/${_targetAuthority.value}",
                    taskPrompt = "سحب بيانات أوفلاين من التطبيق الشريك",
                    status = "COMPLETED",
                    outputResult = "✅ تم استيراد $importedCount عنصر أوفلاين بنجاح وحفظها في قاعدة البيانات المحلية.",
                    tokensUsed = 0
                )
            )

            recordEvent(
                BridgeExchangeEvent(
                    direction = ExchangeDirection.INCOMING,
                    dataType = dataType,
                    summary = "تم استيراد $importedCount عنصر من التطبيق الشريك وحفظها محلياً",
                    itemCount = importedCount,
                    isSuccess = true
                )
            )

            if (importedCount > 0) {
                onDataOrchestratedListener?.invoke(
                    importedCount,
                    "تم استيعاب $importedCount عنصر أوفلاين وتحديث قاعدة بيانات Room والذاكرة في الوقت الفعلي."
                )
            }

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
     * Pushes this app's scraped posts to the companion app.
     */
    suspend fun pushDataToCompanion(dataType: String = AppBridgeContract.TYPE_POSTS): Result<Int> {
        return try {
            val posts = database.postMetadataDao().getRecentPostsSync(30)
            if (posts.isEmpty()) {
                return Result.failure(Exception("لا توجد بيانات منشورات حالياً في التطبيق لإرسالها"))
            }

            val jsonArray = JSONArray()
            for (p in posts) {
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("platform", p.platform)
                    put("content", p.postContent)
                    put("url", p.originalPostUrl)
                    put("author", p.authorDisplayName)
                    put("likes", p.likesCount)
                    put("comments", p.commentsCount)
                    put("timestamp", p.scrapedTimestamp)
                    put("tags", p.tagsCsv)
                }
                jsonArray.put(obj)
            }

            val uri = AppBridgeContract.buildUri(_targetAuthority.value)
            val extras = Bundle().apply {
                putString(AppBridgeContract.KEY_DATA_TYPE, dataType)
                putString(AppBridgeContract.KEY_SENDER, "SocialAgent")
                putString(AppBridgeContract.KEY_PAYLOAD_JSON, jsonArray.toString())
                putInt(AppBridgeContract.KEY_ITEM_COUNT, posts.size)
            }

            val result = appContext.contentResolver.call(
                uri,
                AppBridgeContract.METHOD_PUSH_DATA,
                dataType,
                extras
            )

            val isSuccess = result?.getString(AppBridgeContract.KEY_STATUS) == AppBridgeContract.STATUS_SUCCESS
            val count = result?.getInt(AppBridgeContract.KEY_ITEM_COUNT) ?: posts.size

            recordEvent(
                BridgeExchangeEvent(
                    direction = ExchangeDirection.OUTGOING,
                    dataType = dataType,
                    summary = if (isSuccess) "تم إرسال ${posts.size} منشور إلى التطبيق الآخر بنجاح" else "أرجع التطبيق الآخر استجابة غير مكتملة",
                    itemCount = count,
                    isSuccess = isSuccess
                )
            )

            if (isSuccess) {
                _statusDetail.value = "✅ تم إرسال ${posts.size} عنصر بنجاح للتطبيق الآخر"
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
     * Handles incoming data pushed into this app by the companion app.
     */
    suspend fun handleInboundPush(dataType: String?, payloadJson: String?): Pair<Boolean, Int> {
        if (payloadJson.isNullOrEmpty()) return Pair(false, 0)
        return try {
            val jsonArray = JSONArray(payloadJson)
            var count = 0
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val content = obj.optString("content", obj.optString("title", ""))
                val platform = obj.optString("platform", "COMPANION_IMPORT")
                val author = obj.optString("author", "تطبيق خارجي")

                val post = ScrapedPostMetadataEntity(
                    id = id,
                    platform = platform,
                    originalPostUrl = obj.optString("url", "app://companion/$id"),
                    authorDisplayName = author,
                    authorHandle = author.lowercase().replace(" ", "_"),
                    postContent = content,
                    likesCount = obj.optInt("likes", 0),
                    tagsCsv = "مستلم_أوفلاين",
                    scrapedTimestamp = System.currentTimeMillis()
                )
                database.postMetadataDao().insertPost(post)
                count++
            }

            recordEvent(
                BridgeExchangeEvent(
                    direction = ExchangeDirection.INCOMING,
                    dataType = dataType ?: "UNKNOWN",
                    summary = "استلام $count عنصر أوفلاين من التطبيق الآخر وتخزينها محلياً",
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
     * Generates a JSON array of this app's posts for an inbound pull query.
     */
    suspend fun handleInboundPull(dataType: String?): String {
        val posts = database.postMetadataDao().getRecentPostsSync(25)
        val jsonArray = JSONArray()
        for (p in posts) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("platform", p.platform)
                put("content", p.postContent)
                put("author", p.authorDisplayName)
                put("likes", p.likesCount)
                put("timestamp", p.scrapedTimestamp)
            }
            jsonArray.put(obj)
        }
        recordEvent(
            BridgeExchangeEvent(
                direction = ExchangeDirection.OUTGOING,
                dataType = dataType ?: "POSTS",
                summary = "التطبيق الآخر قام بسحب ${posts.size} منشور من قاعدة بياناتنا",
                itemCount = posts.size,
                isSuccess = true
            )
        )
        return jsonArray.toString()
    }

    companion object {
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
