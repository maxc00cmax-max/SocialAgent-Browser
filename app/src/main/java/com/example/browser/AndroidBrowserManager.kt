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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

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

    private val webViewMap = mutableMapOf<String, WebView>()
    private val mainHandler = Handler(Looper.getMainLooper())

    var onPageVisited: ((url: String, title: String) -> Unit)? = null

    init {
        // Initialize default tabs (social media pages)
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
            val id = UUID.randomUUID().toString()
            BrowserTabState(
                id = id,
                title = title,
                url = url,
                pageSummary = "صفحة $title جاهزة للتحليل الشامل واستخراج البيانات.",
                analysisStatus = TabAnalysisStatus.PENDING
            )
        }

        _tabs.value = createdTabs
        _activeTabId.value = createdTabs.firstOrNull()?.id
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
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
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

    fun addNewTab(title: String = "صفحة جديدة", url: String = "https://x.com"): String {
        val newTab = BrowserTabState(
            id = UUID.randomUUID().toString(),
            title = title,
            url = url,
            analysisStatus = TabAnalysisStatus.PENDING,
            pageSummary = "صفحة متصفح مفتوحة ($url)"
        )
        _tabs.value = _tabs.value + newTab
        _activeTabId.value = newTab.id
        return newTab.id
    }

    fun closeTab(tabId: String) {
        val remaining = _tabs.value.filter { it.id != tabId }
        webViewMap.remove(tabId)?.destroy()
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

        // Update active tab or target first tab with real-time orchestration results
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
}
