package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiProvider
import com.example.browser.AndroidBrowserManager
import com.example.browser.BrowserTabState
import com.example.browser.HumanBehaviorEngine
import com.example.browser.TabAnalysisStatus
import com.example.database.AppDatabase
import com.example.database.entity.BrowserTabEntity
import com.example.database.entity.TabTaskLogEntity
import com.example.database.entity.TokenLogEntity
import com.example.database.repository.SocialAgentRepository
import com.example.scraper.MockSocialMediaScraper
import com.example.bridge.AppBridgeContract
import com.example.bridge.AppBridgeManager
import com.example.bridge.BridgeStatus
import com.example.bridge.BridgeExchangeEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val database = AppDatabase.getDatabase(application)
    val repository = SocialAgentRepository(database)
    val browserManager = AndroidBrowserManager(application)
    val geminiProvider = GeminiProvider()
    val socialMediaScraper = MockSocialMediaScraper(repository)

    // Master System Running State (زر تشغيل وإيقاف)
    private val _isSystemRunning = MutableStateFlow(false)
    val isSystemRunning: StateFlow<Boolean> = _isSystemRunning.asStateFlow()

    private val _systemStatusMessage = MutableStateFlow("النظام متوقف. اضغط على 'تشغيل النظام' للبدء في تحليل النوافذ المفتوحة.")
    val systemStatusMessage: StateFlow<String> = _systemStatusMessage.asStateFlow()

    // Map of per-tab prompt input text
    private val _tabPromptInputs = MutableStateFlow<Map<String, String>>(emptyMap())
    val tabPromptInputs: StateFlow<Map<String, String>> = _tabPromptInputs.asStateFlow()

    private var autoAnalysisJob: Job? = null

    // Room Database flows
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

    val allTabTaskLogs = repository.allTabTaskLogs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

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

    val recentPagesFilterPlatform = MutableStateFlow<String?>(null)
    val recentPagesSearchQuery = MutableStateFlow("")

    // Local Offline IPC App Bridge
    val bridgeManager = AppBridgeManager.getInstance(application)
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
        // Connect browser page visited events to RecentPage Room entity
        browserManager.onPageVisited = { url, title ->
            recordRecentPageVisit(url, title)
        }

        // Connect real-time ContentObserver & WorkManager events to Browser Orchestration
        bridgeManager.onDataOrchestratedListener = { count, summary ->
            browserManager.orchestrateExternalDataArrival(count, summary)
        }

        // Pre-seed sample past task logs and recent social media pages if empty
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            if (allTabTaskLogs.value.isEmpty()) {
                seedInitialTaskLogs()
            }
            if (allRecentPages.value.isEmpty()) {
                seedInitialRecentPages()
            }
        }
    }

    private suspend fun seedInitialRecentPages() {
        val initialRecentPages = listOf(
            com.example.database.entity.RecentPageEntity(
                id = UUID.randomUUID().toString(),
                title = "OpenAI on X (Twitter)",
                url = "https://x.com/OpenAI",
                platform = "X_TWITTER",
                previewSnippet = "أحدث أبحاث الذكاء الاصطناعي التوليدي، نماذج التفكير والاستدلال، ومستجدات GPT-4o.",
                visitCount = 14,
                lastVisitedTimestamp = System.currentTimeMillis() - 1200000L,
                isPinned = true,
                isAvailableOffline = true
            ),
            com.example.database.entity.RecentPageEntity(
                id = UUID.randomUUID().toString(),
                title = "DeepSeek AI Research on X",
                url = "https://x.com/deepseek_ai",
                platform = "X_TWITTER",
                previewSnippet = "تطورات نماذج التفكير مفتوحة المصدر وبنية الاستدلال السريع للوكلاء الأذكياء.",
                visitCount = 9,
                lastVisitedTimestamp = System.currentTimeMillis() - 3600000L,
                isPinned = true,
                isAvailableOffline = true
            ),
            com.example.database.entity.RecentPageEntity(
                id = UUID.randomUUID().toString(),
                title = "Android Developers Community",
                url = "https://linkedin.com/company/android-developers",
                platform = "LINKEDIN",
                previewSnippet = "دليل المطورين لأحدث ميزات Jetpack Compose وAndroid 15 وهندسة النظم.",
                visitCount = 6,
                lastVisitedTimestamp = System.currentTimeMillis() - 7200000L,
                isPinned = false,
                isAvailableOffline = true
            ),
            com.example.database.entity.RecentPageEntity(
                id = UUID.randomUUID().toString(),
                title = "Tech Trends & AI on Reddit",
                url = "https://reddit.com/r/technology",
                platform = "REDDIT",
                previewSnippet = "نقاشات المجتمع التقني حول الأتمتة المتقدمة، البرمجة بالوكلاء، ومستقبل أدوات التطوير.",
                visitCount = 8,
                lastVisitedTimestamp = System.currentTimeMillis() - 18000000L,
                isPinned = false,
                isAvailableOffline = true
            ),
            com.example.database.entity.RecentPageEntity(
                id = UUID.randomUUID().toString(),
                title = "Google DeepMind on LinkedIn",
                url = "https://linkedin.com/company/google-deepmind",
                platform = "LINKEDIN",
                previewSnippet = "إعلانات نماذج Gemini المتعددة الوسائط والحلول المؤسسية للذكاء الاصطناعي.",
                visitCount = 11,
                lastVisitedTimestamp = System.currentTimeMillis() - 86400000L,
                isPinned = false,
                isAvailableOffline = true
            ),
            com.example.database.entity.RecentPageEntity(
                id = UUID.randomUUID().toString(),
                title = "Instagram Creators Hub",
                url = "https://instagram.com/creators",
                platform = "INSTAGRAM",
                previewSnippet = "استراتيجيات صناعة المحتوى، خوارزميات ريلز الحديثة، وطرق مضاعفة التفاعل والتواصل.",
                visitCount = 5,
                lastVisitedTimestamp = System.currentTimeMillis() - 172800000L,
                isPinned = false,
                isAvailableOffline = true
            )
        )
        repository.insertRecentPages(initialRecentPages)
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

    private suspend fun seedInitialTaskLogs() {
        val currentTabs = browserManager.tabs.value
        val xTab = currentTabs.find { it.url.contains("x.com") }
        val linkedInTab = currentTabs.find { it.url.contains("linkedin.com") }

        if (xTab != null) {
            repository.insertTaskLog(
                TabTaskLogEntity(
                    id = UUID.randomUUID().toString(),
                    tabId = xTab.id,
                    tabTitle = xTab.title,
                    tabUrl = xTab.url,
                    taskPrompt = "استخرج أحدث التغريدات والهاشتاجات الرائجة في التقنية",
                    status = "COMPLETED",
                    outputResult = "✅ تم استخراج 12 منشوراً تفاعلياً، 5 وسوم رائجة (#AI, #TechTrends2026)، وتحديد 8 حسابات مؤثرة.",
                    timestamp = System.currentTimeMillis() - 1800000L,
                    durationMs = 2450L,
                    tokensUsed = 620
                )
            )
        }

        if (linkedInTab != null) {
            repository.insertTaskLog(
                TabTaskLogEntity(
                    id = UUID.randomUUID().toString(),
                    tabId = linkedInTab.id,
                    tabTitle = linkedInTab.title,
                    tabUrl = linkedInTab.url,
                    taskPrompt = "فحص منشورات قادة الأعمال وتحليل معدل المشاركة",
                    status = "COMPLETED",
                    outputResult = "✅ تم رصد 6 منشورات مهنية، متوسط الإعجابات 340، وتحديد أبرز اتجاهات التحول الرقمي.",
                    timestamp = System.currentTimeMillis() - 3600000L,
                    durationMs = 3120L,
                    tokensUsed = 840
                )
            )
        }
    }

    /**
     * زر تشغيل وإيقاف النظام الرئيسي
     */
    fun toggleSystemRunning() {
        val newState = !_isSystemRunning.value
        _isSystemRunning.value = newState

        if (newState) {
            _systemStatusMessage.value = "🚀 النظام قيد التشغيل: جاري فحص وتحليل كل نوافذ المتصفح المفتوحة..."
            startAutomatedPageAnalysis()
        } else {
            autoAnalysisJob?.cancel()
            _systemStatusMessage.value = "⏸️ تم إيقاف النظام. تم حفظ النوافذ ومساحات العمل."
        }
    }

    /**
     * بمجرد تشغيل النظام: يحلل كل الصفحات ويفتح لكل صفحة مساحة ويطلب كتابة برومبت
     */
    fun startAutomatedPageAnalysis() {
        autoAnalysisJob?.cancel()
        autoAnalysisJob = viewModelScope.launch {
            val currentTabs = browserManager.tabs.value

            _systemStatusMessage.value = "🔍 تم العثور على ${currentTabs.size} نافذة مفتوحة. جاري فتح مساحة عمل لكل صفحة..."

            for (tab in currentTabs) {
                if (!_isSystemRunning.value) break

                // Set status to ANALYZING
                browserManager.updateTabState(tab.id) {
                    it.copy(
                        analysisStatus = TabAnalysisStatus.ANALYZING,
                        lastActionLog = "جاري قراءة بنية الصفحة واستخراج العناصر والروابط..."
                    )
                }

                // Natural delay to simulate human perception and DOM scanning
                HumanBehaviorEngine.naturalPause(600, 1200)

                val summary = generateTabSummary(tab.title, tab.url)
                val detectedCount = when {
                    tab.url.contains("x.com") -> 28
                    tab.url.contains("linkedin.com") -> 35
                    tab.url.contains("instagram.com") -> 19
                    tab.url.contains("reddit.com") -> 42
                    else -> 15
                }

                // Update tab state to READY_FOR_PROMPT (يطلب كتابة برومبت)
                browserManager.updateTabState(tab.id) {
                    it.copy(
                        pageSummary = summary,
                        detectedElementsCount = detectedCount,
                        analysisStatus = TabAnalysisStatus.READY_FOR_PROMPT,
                        lastActionLog = "اكتمل تحليل الصفحة بنجاح! المساحة جاهزة لاستقبال أمرك (البرومبت)."
                    )
                }

                // Persist tab state in Room Database
                repository.insertTab(
                    BrowserTabEntity(
                        id = tab.id,
                        title = tab.title,
                        url = tab.url,
                        analysisSummary = summary,
                        status = "READY_FOR_PROMPT",
                        detectedElementsCount = detectedCount
                    )
                )
            }

            if (_isSystemRunning.value) {
                _systemStatusMessage.value = "✅ اكتمل تحليل كافة النوافذ المفتوحة. تم فتح مساحات العمل وبانتظار برومبت لكل صفحة!"
            }
        }
    }

    private fun generateTabSummary(title: String, url: String): String {
        return when {
            url.contains("x.com") || url.contains("twitter.com") ->
                "تم رصد منصة إكس: تم اكتشاف خلاصة المنشورات التفاعلية، أزرار الإعجاب وإعادة النشر، ومؤشرات الهاشتاجات الرائجة."
            url.contains("linkedin.com") ->
                "تم رصد منصة لينكد إن: اكتشاف ملفات شخصية احترافية، منشورات صناعية، وروابط تواصل وشبكات الأعمال."
            url.contains("instagram.com") ->
                "تم رصد إنستغرام: اكتشاف شبكة من الصور والفيديوهات (Reels)، الحسابات المقترحة، والتفاعل مع التعليقات."
            url.contains("reddit.com") ->
                "تم رصد ريديت: اكتشاف خيوط نقاش تقنية (Threads)، تقييمات المنشورات (Upvotes)، ومشاركات تفاعلية."
            else ->
                "تم فحص الصفحة ($title): استخراج العناصر النصية، الروابط الفعالة، وهيكل المحتوى الرئيسي جاهز للأتمتة."
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

    /**
     * تنفيذ البرومبت المكتوب على نافذة/صفحة معينة
     */
    fun executePromptOnTab(tabId: String, promptText: String) {
        val prompt = promptText.ifBlank {
            _tabPromptInputs.value[tabId]?.trim() ?: return
        }

        viewModelScope.launch {
            val tab = browserManager.tabs.value.find { it.id == tabId } ?: return@launch
            val logId = UUID.randomUUID().toString()
            val startTime = System.currentTimeMillis()

            // 1. Insert in-progress log
            repository.insertTaskLog(
                TabTaskLogEntity(
                    id = logId,
                    tabId = tab.id,
                    tabTitle = tab.title,
                    tabUrl = tab.url,
                    taskPrompt = prompt,
                    status = "IN_PROGRESS",
                    outputResult = "جاري تنفيذ المهمة على الصفحة ومحاكاة السلوك البشري...",
                    timestamp = startTime
                )
            )

            browserManager.updateTabState(tabId) {
                it.copy(
                    isExecutingPrompt = true,
                    analysisStatus = TabAnalysisStatus.EXECUTING,
                    userPrompt = prompt,
                    lastActionLog = "جاري تنفيذ الأمر: \"$prompt\" على الصفحة..."
                )
            }

            try {
                // Natural typing and navigation simulation
                HumanBehaviorEngine.humanTypingDelay(prompt.length)
                HumanBehaviorEngine.simulatedScrollStep()

                // AI analysis and command execution
                val aiResponse = geminiProvider.analyzePageAndExecute(
                    pageTitle = tab.title,
                    pageUrl = tab.url,
                    userPrompt = prompt,
                    contextInfo = tab.pageSummary
                )

                val duration = System.currentTimeMillis() - startTime

                // Log Token Usage in Room DB
                repository.insertTokenLog(
                    TokenLogEntity(
                        id = UUID.randomUUID().toString(),
                        modelName = "gemini-2.5-flash",
                        promptTokens = aiResponse.promptTokens,
                        candidateTokens = aiResponse.candidateTokens,
                        totalTokens = aiResponse.totalTokens,
                        costUsd = (aiResponse.totalTokens * 0.0000003)
                    )
                )

                // Update Tab State with Result
                browserManager.updateTabState(tabId) {
                    it.copy(
                        isExecutingPrompt = false,
                        analysisStatus = TabAnalysisStatus.COMPLETED,
                        promptResult = aiResponse.text,
                        lastActionLog = "اكتمل تنفيذ البرومبت بنجاح! تم حفظ النتائج."
                    )
                }

                // Update Room Database for Tab
                repository.insertTab(
                    BrowserTabEntity(
                        id = tab.id,
                        title = tab.title,
                        url = tab.url,
                        analysisSummary = tab.pageSummary,
                        userPrompt = prompt,
                        executionResult = aiResponse.text,
                        status = "COMPLETED",
                        detectedElementsCount = tab.detectedElementsCount
                    )
                )

                // Update Task Execution Log to COMPLETED
                repository.updateTaskLog(
                    TabTaskLogEntity(
                        id = logId,
                        tabId = tab.id,
                        tabTitle = tab.title,
                        tabUrl = tab.url,
                        taskPrompt = prompt,
                        status = "COMPLETED",
                        outputResult = aiResponse.text,
                        timestamp = startTime,
                        durationMs = duration,
                        tokensUsed = aiResponse.totalTokens
                    )
                )

                // Scrape and persist social post metadata if relevant
                socialMediaScraper.scrapeUrl(tab.url)
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                val errorMsg = "حدث خطأ أثناء التنفيذ: ${e.localizedMessage ?: "خطأ غير متوقع"}"

                browserManager.updateTabState(tabId) {
                    it.copy(
                        isExecutingPrompt = false,
                        analysisStatus = TabAnalysisStatus.ERROR,
                        promptResult = errorMsg,
                        lastActionLog = errorMsg
                    )
                }

                repository.updateTaskLog(
                    TabTaskLogEntity(
                        id = logId,
                        tabId = tab.id,
                        tabTitle = tab.title,
                        tabUrl = tab.url,
                        taskPrompt = prompt,
                        status = "FAILED",
                        outputResult = errorMsg,
                        timestamp = startTime,
                        durationMs = duration,
                        tokensUsed = 0
                    )
                )
            }
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

    /**
     * تنفيذ أمر شامل ومرن على كل النوافذ والصفحات المفتوحة دفعة واحدة
     */
    fun executeUniversalPromptOnAllTabs(promptText: String) {
        val prompt = promptText.trim()
        if (prompt.isBlank()) return

        val currentTabs = browserManager.tabs.value
        currentTabs.forEach { tab ->
            updatePromptInput(tab.id, prompt)
            executePromptOnTab(tab.id, prompt)
        }
    }

    /**
     * استخراج فوري للبيانات والمنشورات من كل الصفحات المفتوحة
     */
    fun triggerInstantScrapeAll() {
        viewModelScope.launch {
            val currentTabs = browserManager.tabs.value
            currentTabs.forEach { tab ->
                socialMediaScraper.scrapeUrl(tab.url)
            }
        }
    }

    fun applyPresetPrompt(tabId: String, preset: String) {
        updatePromptInput(tabId, preset)
    }

    fun addNewTab(title: String = "صفحة جديدة", url: String = "https://x.com") {
        val newId = browserManager.addNewTab(title, url)
        if (_isSystemRunning.value) {
            // If system is running, automatically analyze the new window!
            viewModelScope.launch {
                delay(800)
                browserManager.updateTabState(newId) {
                    it.copy(
                        pageSummary = generateTabSummary(title, url),
                        detectedElementsCount = 18,
                        analysisStatus = TabAnalysisStatus.READY_FOR_PROMPT,
                        lastActionLog = "تم فتح وتحليل النافذة الجديدة بنجاح! اكتب برومبت للبدء."
                    )
                }
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
}
