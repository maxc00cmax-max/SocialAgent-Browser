package com.example.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.database.entity.BrowserTabEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

data class BrowserTabState(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "صفحة جديدة",
    val url: String = "https://x.com",
    val favicon: Bitmap? = null,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val pageSummary: String = "",
    val detectedElementsCount: Int = 0,
    val analysisStatus: TabAnalysisStatus = TabAnalysisStatus.PENDING,
    val userPrompt: String = "",
    val promptResult: String = "",
    val isExecutingPrompt: Boolean = false,
    val lastActionLog: String = ""
)

enum class TabAnalysisStatus {
    PENDING,
    ANALYZING,
    READY_FOR_PROMPT,
    EXECUTING,
    COMPLETED,
    ERROR
}

class AndroidBrowserManager(private val context: Context) {

    private val _tabs = MutableStateFlow<List<BrowserTabState>>(emptyList())
    val tabs: StateFlow<List<BrowserTabState>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val webViewMap = ConcurrentHashMap<String, WebView>()
    private val snapshotCache = ConcurrentHashMap<String, DomSnapshot>()
    private val mainHandler = Handler(Looper.getMainLooper())
    val interactionEngine = HumanizedInteractionEngine()

    var onPageVisited: ((url: String, title: String) -> Unit)? = null
    var onDomSnapshotCaptured: ((tabId: String, snapshot: DomSnapshot) -> Unit)? = null

    init {
        initDefaultTabs()
    }

    private fun initDefaultTabs() {
        val initialPages = listOf(
            Pair("منصة إكس (Twitter)", "https://x.com/explore"),
            Pair("لينكد إن (LinkedIn)", "https://www.linkedin.com/feed"),
            Pair("إنستغرام (Instagram)", "https://www.instagram.com/explore"),
            Pair("ريديت (Reddit)", "https://www.reddit.com/r/technology")
        )

        val createdTabs = initialPages.map { (title, url) ->
            BrowserTabState(
                id = UUID.randomUUID().toString(),
                title = title,
                url = url,
                pageSummary = "صفحة $title جاهزة للتحليل الشامل واستخراج البيانات.",
                analysisStatus = TabAnalysisStatus.PENDING
            )
        }

        _tabs.value = createdTabs
        _activeTabId.value = createdTabs.firstOrNull()?.id
    }

    /**
     * Restores persisted tab states from Room without clobbering user-created tabs.
     */
    fun restorePersistedTabMetadata(savedTabs: List<BrowserTabEntity>) {
        if (savedTabs.isEmpty()) return
        val savedByUrl = savedTabs.associateBy { it.url }
        _tabs.value = _tabs.value.map { current ->
            val match = savedByUrl[current.url]
            if (match != null && current.analysisStatus == TabAnalysisStatus.PENDING) {
                current.copy(
                    title = match.title.ifBlank { current.title },
                    pageSummary = match.analysisSummary.ifBlank { current.pageSummary },
                    userPrompt = match.userPrompt,
                    promptResult = match.executionResult,
                    detectedElementsCount = match.detectedElementsCount,
                    analysisStatus = when (match.status) {
                        "COMPLETED" -> TabAnalysisStatus.COMPLETED
                        "READY_FOR_PROMPT", "READY" -> TabAnalysisStatus.READY_FOR_PROMPT
                        else -> current.analysisStatus
                    }
                )
            } else {
                current
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(tabId: String): WebView {
        return webViewMap.getOrPut(tabId) {
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    databaseEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        updateTabState(tabId) { it.copy(isLoading = true, url = url ?: it.url) }
                        view?.evaluateJavascript(interactionEngine.buildAntiBotStealthScript(), null)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript(interactionEngine.buildAntiBotStealthScript(), null)
                        val currentTitle = view?.title?.ifBlank { null } ?: url ?: "نافذة متصفح"
                        updateTabState(tabId) {
                            it.copy(
                                isLoading = false,
                                title = currentTitle,
                                url = url ?: it.url
                            )
                        }
                        if (!url.isNullOrBlank() && !url.startsWith("about:")) {
                            onPageVisited?.invoke(url, currentTitle)
                            // Capture live DOM snapshot asynchronously right after page load
                            view?.evaluateJavascript(interactionEngine.buildDomSnapshotScript()) { rawResult ->
                                parseDomSnapshotJson(rawResult, url, currentTitle)?.let { snapshot ->
                                    snapshotCache[tabId] = snapshot
                                    if (snapshot.interactiveElementsCount > 0) {
                                        updateTabState(tabId) {
                                            it.copy(detectedElementsCount = snapshot.interactiveElementsCount)
                                        }
                                    }
                                    onDomSnapshotCaptured?.invoke(tabId, snapshot)
                                }
                            }
                        }
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        updateTabState(tabId) { it.copy(progress = newProgress) }
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        if (!title.isNullOrBlank()) {
                            updateTabState(tabId) { it.copy(title = title) }
                        }
                    }
                }
                val currentTab = _tabs.value.find { it.id == tabId }
                loadUrl(currentTab?.url ?: "about:blank")
            }
        }
    }

    fun getCachedDomSnapshot(tabId: String): DomSnapshot? = snapshotCache[tabId]

    /**
     * Evaluates the live DOM extraction script on an active WebView tab, with safe timeout.
     * Returns cached snapshot if the WebView is not currently inflated on screen.
     */
    suspend fun extractDomSnapshot(tabId: String, timeoutMs: Long = 1800L): DomSnapshot? {
        val webView = webViewMap[tabId] ?: return snapshotCache[tabId]
        val tab = _tabs.value.find { it.id == tabId } ?: return snapshotCache[tabId]

        val extracted = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<DomSnapshot?> { continuation ->
                mainHandler.post {
                    try {
                        webView.evaluateJavascript(interactionEngine.buildDomSnapshotScript()) { raw ->
                            val parsed = parseDomSnapshotJson(raw, tab.url, tab.title)
                            if (parsed != null) {
                                snapshotCache[tabId] = parsed
                            }
                            if (continuation.isActive) {
                                continuation.resume(parsed ?: snapshotCache[tabId])
                            }
                        }
                    } catch (_: Exception) {
                        if (continuation.isActive) {
                            continuation.resume(snapshotCache[tabId])
                        }
                    }
                }
            }
        }
        return extracted ?: snapshotCache[tabId]
    }

    suspend fun executeHumanScroll(tabId: String, deltaY: Int = 650): Boolean {
        val webView = webViewMap[tabId] ?: return false
        return withTimeoutOrNull(1500L) {
            suspendCancellableCoroutine { continuation ->
                mainHandler.post {
                    try {
                        webView.evaluateJavascript(interactionEngine.buildHumanScrollScript(deltaY)) {
                            if (continuation.isActive) continuation.resume(true)
                        }
                    } catch (_: Exception) {
                        if (continuation.isActive) continuation.resume(false)
                    }
                }
            }
        } ?: false
    }

    suspend fun executeHumanClick(tabId: String, selector: String): Boolean {
        val webView = webViewMap[tabId] ?: return false
        return withTimeoutOrNull(1500L) {
            suspendCancellableCoroutine { continuation ->
                mainHandler.post {
                    try {
                        webView.evaluateJavascript(interactionEngine.buildHumanClickScript(selector)) { raw ->
                            val ok = raw?.contains("\"success\":true") == true
                            if (continuation.isActive) continuation.resume(ok)
                        }
                    } catch (_: Exception) {
                        if (continuation.isActive) continuation.resume(false)
                    }
                }
            }
        } ?: false
    }

    fun addNewTab(title: String = "صفحة جديدة", url: String = "https://x.com"): String {
        val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else url
        val newTab = BrowserTabState(
            id = UUID.randomUUID().toString(),
            title = title,
            url = formattedUrl,
            analysisStatus = TabAnalysisStatus.PENDING,
            pageSummary = "صفحة متصفح مفتوحة ($formattedUrl)"
        )
        _tabs.value = _tabs.value + newTab
        _activeTabId.value = newTab.id
        return newTab.id
    }

    fun closeTab(tabId: String) {
        val remaining = _tabs.value.filter { it.id != tabId }
        snapshotCache.remove(tabId)
        val removedWebView = webViewMap.remove(tabId)
        if (removedWebView != null) {
            mainHandler.post { removedWebView.destroy() }
        }
        _tabs.value = remaining
        if (_activeTabId.value == tabId) {
            _activeTabId.value = remaining.firstOrNull()?.id
        }
    }

    fun selectTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) {
            _activeTabId.value = tabId
        }
    }

    fun updateTabState(tabId: String, transform: (BrowserTabState) -> BrowserTabState) {
        _tabs.value = _tabs.value.map { if (it.id == tabId) transform(it) else it }
    }

    fun loadUrlInActiveTab(url: String) {
        val activeId = _activeTabId.value ?: return
        val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else url
        updateTabState(activeId) { it.copy(url = formattedUrl, isLoading = true) }
        mainHandler.post {
            webViewMap[activeId]?.loadUrl(formattedUrl)
        }
    }

    /**
     * Real-time orchestration: triggered automatically when the ContentObserver
     * or WorkManager detects that the companion offline app has pushed or updated data.
     */
    fun orchestrateExternalDataArrival(count: Int, summary: String) {
        val currentTabs = _tabs.value
        if (currentTabs.isEmpty()) return

        val targetId = _activeTabId.value ?: currentTabs.first().id
        updateTabState(targetId) { tab ->
            tab.copy(
                pageSummary = "⚡ أتمتة آنية: تم رصد وتنسيق بيانات واردة من التطبيق الشريك ($count عنصر)!\n$summary",
                detectedElementsCount = tab.detectedElementsCount + count,
                analysisStatus = TabAnalysisStatus.READY_FOR_PROMPT,
                lastActionLog = "تم استيعاب $count عنصر أوفلاين في الذاكرة وقاعدة بيانات Room بنجاح."
            )
        }
    }

    private fun parseDomSnapshotJson(rawResult: String?, fallbackUrl: String, fallbackTitle: String): DomSnapshot? {
        if (rawResult.isNullOrBlank() || rawResult == "null") return null
        return try {
            // WebView evaluateJavascript returns a JSON-encoded string literal
            val unescaped = if (rawResult.startsWith("\"") && rawResult.endsWith("\"")) {
                JSONObject("{\"v\":$rawResult}").getString("v")
            } else {
                rawResult
            }
            val json = JSONObject(unescaped)
            if (json.has("error")) return null

            fun JSONArray?.toStringList(): List<String> {
                if (this == null) return emptyList()
                return (0 until length()).mapNotNull { optString(it).takeIf { s -> s.isNotBlank() } }
            }

            DomSnapshot(
                url = json.optString("url", fallbackUrl).ifBlank { fallbackUrl },
                title = json.optString("title", fallbackTitle).ifBlank { fallbackTitle },
                ogTitle = json.optString("ogTitle").takeIf { it.isNotBlank() },
                ogDescription = json.optString("ogDescription").takeIf { it.isNotBlank() },
                ogImageUrl = json.optString("ogImageUrl").takeIf { it.isNotBlank() },
                headings = json.optJSONArray("headings").toStringList(),
                textExcerpt = json.optString("textExcerpt", ""),
                linksCount = json.optInt("linksCount", 0),
                interactiveElementsCount = json.optInt("interactiveElementsCount", 0),
                hashtags = json.optJSONArray("hashtags").toStringList(),
                mentions = json.optJSONArray("mentions").toStringList(),
                postSnippets = json.optJSONArray("postSnippets").toStringList()
            )
        } catch (_: Exception) {
            null
        }
    }
}
