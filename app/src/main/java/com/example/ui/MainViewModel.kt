package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.SocialAgentApplication
import com.example.backend.AppContainer
import com.example.backend.BackendHealthReport
import com.example.bridge.AppBridgeContract
import com.example.browser.TabAnalysisStatus
import com.example.database.entity.CachedMetadataEntity
import com.example.scraper.MetadataScrapeWorker
import com.example.scraper.OpenGraphScrapeState
import com.example.scraper.toOpenGraphMetadata
import com.example.ui.screens.BrowserViewMode
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val container: AppContainer =
        (application as? SocialAgentApplication)?.appContainer ?: AppContainer(application)

    val database = container.database
    val repository = container.repository
    val browserManager = container.browserManager
    val geminiProvider = container.geminiProvider
    val openGraphScraper = container.openGraphScraper
    val socialMediaScraper = container.socialScraper
    val orchestrator = container.orchestrator

    private val _openGraphScrapeState = MutableStateFlow<OpenGraphScrapeState>(OpenGraphScrapeState.Idle)
    val openGraphScrapeState: StateFlow<OpenGraphScrapeState> = _openGraphScrapeState.asStateFlow()

    // Master System Running State
    private val _isSystemRunning = MutableStateFlow(false)
    val isSystemRunning: StateFlow<Boolean> = _isSystemRunning.asStateFlow()

    private val _systemStatusMessage = MutableStateFlow("النظام متوقف. اضغط على 'تشغيل النظام' للبدء في تحليل النوافذ المفتوحة.")
    val systemStatusMessage: StateFlow<String> = _systemStatusMessage.asStateFlow()

    // Per-tab prompt input text
    private val _tabPromptInputs = MutableStateFlow<Map<String, String>>(emptyMap())
    val tabPromptInputs: StateFlow<Map<String, String>> = _tabPromptInputs.asStateFlow()

    private var autoAnalysisJob: Job? = null

    // Reactive Room Database Flows
    val allScrapedPosts = repository.allScrapedPosts.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val savedTabs = repository.allTabs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val totalTokens = repository.totalTokens.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    val totalCostUsd = repository.totalCostUsd.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0.0
    )

    val allTabTaskLogs = repository.allTabTaskLogs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allOrchestrationTasks = repository.allOrchestrationTasks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val activeOrchestrationTasksCount = repository.activeOrchestrationTasksCount.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    val backendHealthReport: StateFlow<BackendHealthReport?> = orchestrator.lastHealthReport

    // Offline Recent Pages Room state
    val allRecentPages = repository.allRecentPages.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val pinnedRecentPages = repository.pinnedRecentPages.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val recentPagesCount = repository.recentPagesCount.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    // Instant Offline Cached Scraped Metadata (Room Cache)
    val cachedMetadataList: StateFlow<List<CachedMetadataEntity>> = repository.allCachedMetadata.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val cachedMetadataCount: StateFlow<Int> = repository.cachedMetadataCount.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        0
    )

    // Background WorkManager Metadata Scrape Observation
    val backgroundScrapeWorkInfos: StateFlow<List<WorkInfo>> =
        MetadataScrapeWorker.observeWorkInfos(application).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val recentPagesFilterPlatform = MutableStateFlow<String?>(null)
    val recentPagesSearchQuery = MutableStateFlow("")
    val recentPagesPinnedOnly = MutableStateFlow(false)
    val recentPagesSortByVisits = MutableStateFlow(false)

    // Lifted Section States for Section-Specific Side Drawers
    val browserViewMode = MutableStateFlow(BrowserViewMode.BROWSER_LIST)
    val logsStatusFilter = MutableStateFlow("ALL")
    val logsTabFilter = MutableStateFlow<String?>(null)
    val databasePlatformFilter = MutableStateFlow("ALL")
    val databaseSearchQuery = MutableStateFlow("")

    // App Theme & Settings Preferences (Light Mode Primary)
    private val uiPrefs = application.getSharedPreferences("social_agent_ui_prefs", Context.MODE_PRIVATE)
    private val _isDarkMode = MutableStateFlow(uiPrefs.getBoolean("is_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _autoAnalyzeNewTabs = MutableStateFlow(uiPrefs.getBoolean("auto_analyze_new_tabs", true))
    val autoAnalyzeNewTabs: StateFlow<Boolean> = _autoAnalyzeNewTabs.asStateFlow()

    // Local Offline IPC App Bridge
    val bridgeManager = container.bridgeManager
    val bridgeStatus = bridgeManager.connectionStatus
    val bridgeStatusDetail = bridgeManager.statusDetail
    val bridgeTargetAuthority = bridgeManager.targetAuthority
    val bridgeEvents = bridgeManager.exchangeEvents
    val bridgeLastSync = bridgeManager.lastSyncTimestamp
    val isBridgeObserverActive = bridgeManager.isBackgroundObservationActive
    val ourShortId = bridgeManager.ourShortId
    val ourPairingToken = bridgeManager.ourPairingToken
    val pairedPartnerToken = bridgeManager.pairedPartnerToken

    init {
        // Persist live WebView page visits into Room
        browserManager.onPageVisited = { url, title ->
            recordRecentPageVisit(url, title)
        }

        // Persist captured live DOM OpenGraph enrichment into Room
        browserManager.onDomSnapshotCaptured = { _, snapshot ->
            viewModelScope.launch {
                val snippet = snapshot.ogDescription ?: snapshot.textExcerpt.take(200)
                repository.recordPageVisit(
                    url = snapshot.url,
                    title = snapshot.ogTitle ?: snapshot.title,
                    snippet = snippet,
                    ogImageUrl = snapshot.ogImageUrl
                )
            }
        }

        // Real-time ContentObserver & WorkManager events -> Browser Orchestration
        bridgeManager.onDataOrchestratedListener = { count, summary ->
            browserManager.orchestrateExternalDataArrival(count, summary)
        }

        // Restore persisted tabs & seed initial starter data cleanly via Orchestrator
        viewModelScope.launch {
            orchestrator.initializeAndRestoreState()
            orchestrator.runDiagnostics()
        }
    }

    fun recordRecentPageVisit(url: String, title: String, platform: String? = null, snippet: String = "") {
        viewModelScope.launch {
            if (url.isNotBlank() && !url.startsWith("about:")) {
                repository.recordPageVisit(
                    url = url,
                    title = title,
                    platform = platform ?: detectPlatform(url),
                    snippet = snippet
                )
            }
        }
    }

    fun togglePinRecentPage(id: String, isPinned: Boolean) {
        viewModelScope.launch {
            repository.togglePinRecentPage(id, isPinned)
        }
    }

    fun deleteRecentPage(id: String) {
        viewModelScope.launch {
            repository.deleteRecentPageById(id)
        }
    }

    fun clearAllRecentPages() {
        viewModelScope.launch {
            repository.clearAllRecentPages()
        }
    }

    fun setRecentPagesFilterPlatform(platform: String?) {
        recentPagesFilterPlatform.value = platform
    }

    fun setRecentPagesSearchQuery(query: String) {
        recentPagesSearchQuery.value = query
    }

    fun openRecentPageInBrowser(url: String, title: String) {
        val existingTabs = browserManager.tabs.value
        val match = existingTabs.find { it.url == url }
        if (match != null) {
            browserManager.selectTab(match.id)
        } else {
            val newTabId = browserManager.addNewTab(title, url)
            browserManager.selectTab(newTabId)
        }
        recordRecentPageVisit(url, title)
    }

    private fun detectPlatform(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains("twitter.com") || lower.contains("x.com") -> "X_TWITTER"
            lower.contains("linkedin.com") -> "LINKEDIN"
            lower.contains("instagram.com") -> "INSTAGRAM"
            lower.contains("tiktok.com") -> "TIKTOK"
            lower.contains("facebook.com") || lower.contains("fb.com") -> "FACEBOOK"
            lower.contains("reddit.com") -> "REDDIT"
            else -> "WEB"
        }
    }

    fun toggleSystemRunning() {
        val newState = !_isSystemRunning.value
        _isSystemRunning.value = newState

        if (newState) {
            _systemStatusMessage.value = "🚀 النظام قيد التشغيل: جاري التحليل المتوازي لجميع النوافذ..."
            startAutomatedPageAnalysis()
        } else {
            autoAnalysisJob?.cancel()
            _systemStatusMessage.value = "⏸️ تم إيقاف النظام. تم حفظ النوافذ ومساحات العمل."
        }
    }

    fun startAutomatedPageAnalysis() {
        autoAnalysisJob?.cancel()
        autoAnalysisJob = viewModelScope.launch {
            orchestrator.analyzeTabsConcurrently(
                isStillRunning = { _isSystemRunning.value },
                onStatusUpdate = { msg -> _systemStatusMessage.value = msg }
            )
        }
    }

    fun updatePromptInput(tabId: String, text: String) {
        _tabPromptInputs.value = _tabPromptInputs.value + (tabId to text)
    }

    fun setPageTask(tabId: String, taskText: String) {
        updatePromptInput(tabId, taskText)
        browserManager.updateTabState(tabId) {
            it.copy(userPrompt = taskText)
        }
    }

    fun executePromptOnTab(tabId: String, promptText: String) {
        val prompt = promptText.ifBlank {
            _tabPromptInputs.value[tabId]?.trim() ?: return
        }
        viewModelScope.launch {
            orchestrator.executePromptOnTab(tabId, prompt)
        }
    }

    fun deleteTaskLog(logId: String) {
        viewModelScope.launch {
            repository.deleteTaskLogById(logId)
        }
    }

    fun clearAllTaskLogs() {
        viewModelScope.launch {
            repository.clearAllTaskLogs()
        }
    }

    fun executeUniversalPromptOnAllTabs(promptText: String) {
        val prompt = promptText.trim()
        if (prompt.isBlank()) return

        browserManager.tabs.value.forEach { tab ->
            updatePromptInput(tab.id, prompt)
        }
        viewModelScope.launch {
            orchestrator.executeUniversalPromptConcurrently(prompt)
        }
    }

    fun triggerInstantScrapeAll() {
        viewModelScope.launch {
            orchestrator.scrapeAllOpenTabs()
        }
    }

    fun applyPresetPrompt(tabId: String, preset: String) {
        updatePromptInput(tabId, preset)
    }

    fun addNewTab(title: String = "صفحة جديدة", url: String = "https://x.com") {
        val newId = browserManager.addNewTab(title, url)
        if (_isSystemRunning.value && _autoAnalyzeNewTabs.value) {
            viewModelScope.launch {
                delay(400)
                val tab = browserManager.tabs.value.find { it.id == newId } ?: return@launch
                orchestrator.analyzeSingleTab(tab)
            }
        }
    }

    fun closeTab(tabId: String) {
        browserManager.closeTab(tabId)
        viewModelScope.launch {
            repository.deleteTabById(tabId)
        }
    }

    fun selectTab(tabId: String) {
        browserManager.selectTab(tabId)
    }

    // --- Offline App Bridge Actions ---
    fun testBridgeConnection() {
        viewModelScope.launch {
            bridgeManager.pingCompanion()
        }
    }

    fun pullDataFromCompanion(dataType: String = AppBridgeContract.TYPE_POSTS) {
        viewModelScope.launch {
            bridgeManager.pullDataFromCompanion(dataType)
        }
    }

    fun pushDataToCompanion(dataType: String = AppBridgeContract.TYPE_POSTS) {
        viewModelScope.launch {
            bridgeManager.pushDataToCompanion(dataType)
        }
    }

    fun setBridgeTargetAuthority(authority: String) {
        bridgeManager.setTargetAuthority(authority)
    }

    fun toggleBridgeBackgroundObservation() {
        if (bridgeManager.isBackgroundObservationActive.value) {
            bridgeManager.stopBackgroundObservation()
        } else {
            bridgeManager.startBackgroundObservation()
        }
    }

    fun pairWithPartner(token: String) {
        viewModelScope.launch {
            bridgeManager.pairWithPartner(token)
        }
    }

    fun unpairPartner() {
        bridgeManager.unpairPartner()
    }

    // --- OpenGraph Metadata & WorkManager Cache Actions ---
    fun scrapeUrlWithOpenGraph(url: String, forceRefresh: Boolean = false) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            if (!forceRefresh) {
                // Instant local Room cache lookup without network overhead
                val cached = repository.getCachedMetadataByUrl(trimmed)
                    ?: repository.getCachedMetadataByUrl(if (trimmed.startsWith("http")) trimmed else "https://$trimmed")
                if (cached != null) {
                    _openGraphScrapeState.value = OpenGraphScrapeState.Success(
                        metadata = cached.toOpenGraphMetadata(),
                        fromCache = true
                    )
                    return@launch
                }
            }

            _openGraphScrapeState.value = OpenGraphScrapeState.Loading(trimmed)
            val result = openGraphScraper.scrapeOrGetCached(trimmed, forceRefresh = forceRefresh)
            result.onSuccess { metadata ->
                _openGraphScrapeState.value = OpenGraphScrapeState.Success(
                    metadata = metadata,
                    fromCache = metadata.isFromCache
                )
            }.onFailure { err ->
                _openGraphScrapeState.value = OpenGraphScrapeState.Error(
                    url = trimmed,
                    message = err.localizedMessage ?: err.message ?: "فشل استخراج البيانات الوصفية"
                )
            }
        }
    }

    /**
     * Schedules a non-blocking WorkManager background task to scrape one or more
     * user-provided URLs (comma, whitespace, or newline separated) and cache them in Room.
     */
    fun enqueueBackgroundMetadataScrape(rawUrlsInput: String, forceRefresh: Boolean = true): UUID? {
        val urls = rawUrlsInput
            .split(",", "\n", " ", "\t")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        if (urls.isEmpty()) return null
        return MetadataScrapeWorker.enqueueScrape(
            context = getApplication(),
            urls = urls,
            forceRefresh = forceRefresh
        )
    }

    fun displayCachedMetadata(cached: CachedMetadataEntity) {
        _openGraphScrapeState.value = OpenGraphScrapeState.Success(
            metadata = cached.toOpenGraphMetadata(),
            fromCache = true
        )
    }

    fun deleteCachedMetadata(id: String) {
        viewModelScope.launch {
            repository.deleteCachedMetadataById(id)
        }
    }

    fun clearAllCachedMetadata() {
        viewModelScope.launch {
            repository.clearAllCachedMetadata()
        }
    }

    fun resetOpenGraphScrapeState() {
        _openGraphScrapeState.value = OpenGraphScrapeState.Idle
    }

    // --- Backend Health & Diagnostics ---
    fun runBackendDiagnostics() {
        viewModelScope.launch {
            orchestrator.runDiagnostics()
        }
    }

    // --- Section Side Drawer Filter & Action Helpers ---
    fun setBrowserViewMode(mode: BrowserViewMode) {
        browserViewMode.value = mode
    }

    fun setRecentPagesPinnedOnly(pinnedOnly: Boolean) {
        recentPagesPinnedOnly.value = pinnedOnly
    }

    fun setRecentPagesSortByVisits(byVisits: Boolean) {
        recentPagesSortByVisits.value = byVisits
    }

    fun setLogsStatusFilter(status: String) {
        logsStatusFilter.value = status
    }

    fun setLogsTabFilter(tabId: String?) {
        logsTabFilter.value = tabId
    }

    fun setDatabasePlatformFilter(platform: String) {
        databasePlatformFilter.value = platform
    }

    fun setDatabaseSearchQuery(query: String) {
        databaseSearchQuery.value = query
    }

    fun clearAllScrapedPosts() {
        viewModelScope.launch {
            repository.deleteAllScrapedPosts()
        }
    }

    // --- Settings & Appearance Actions (Light Mode Primary) ---
    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        uiPrefs.edit().putBoolean("is_dark_mode", enabled).apply()
    }

    fun toggleDarkMode() {
        setDarkMode(!_isDarkMode.value)
    }

    fun setAutoAnalyzeNewTabs(enabled: Boolean) {
        _autoAnalyzeNewTabs.value = enabled
        uiPrefs.edit().putBoolean("auto_analyze_new_tabs", enabled).apply()
    }

    fun resetAllSectionFilters() {
        recentPagesFilterPlatform.value = null
        recentPagesSearchQuery.value = ""
        recentPagesPinnedOnly.value = false
        recentPagesSortByVisits.value = false
        logsStatusFilter.value = "ALL"
        logsTabFilter.value = null
        databasePlatformFilter.value = "ALL"
        databaseSearchQuery.value = ""
    }
}
