package com.example.database.repository

import com.example.database.AppDatabase
import com.example.database.entity.*
import kotlinx.coroutines.flow.Flow

class SocialAgentRepository(private val db: AppDatabase) {

    // Browser Tabs
    val allTabs: Flow<List<BrowserTabEntity>> = db.browserTabDao().getAllTabs()
    suspend fun insertTab(tab: BrowserTabEntity) = db.browserTabDao().insertTab(tab)
    suspend fun insertTabs(tabs: List<BrowserTabEntity>) = db.browserTabDao().insertTabs(tabs)
    suspend fun updateTab(tab: BrowserTabEntity) = db.browserTabDao().updateTab(tab)
    suspend fun deleteTab(tab: BrowserTabEntity) = db.browserTabDao().deleteTab(tab)
    suspend fun deleteTabById(id: String) = db.browserTabDao().deleteTabById(id)
    suspend fun deleteAllTabs() = db.browserTabDao().deleteAllTabs()

    // Browser Data
    val allBrowserData: Flow<List<BrowserDataEntity>> = db.browserDataDao().getAllBrowserData()
    suspend fun insertBrowserData(data: BrowserDataEntity) = db.browserDataDao().insertBrowserData(data)

    // Token Logs
    val allTokenLogs: Flow<List<TokenLogEntity>> = db.tokenLogDao().getAllTokenLogs()
    val totalTokens: Flow<Int?> = db.tokenLogDao().getTotalTokens()
    suspend fun insertTokenLog(log: TokenLogEntity) = db.tokenLogDao().insertTokenLog(log)

    // Tools
    val allTools: Flow<List<ToolEntity>> = db.toolDao().getAllTools()
    suspend fun insertTool(tool: ToolEntity) = db.toolDao().insertTool(tool)
    suspend fun deleteTool(id: String) = db.toolDao().deleteTool(id)

    // Scraped Post Metadata
    val postMetadataDao = db.postMetadataDao()
    val allScrapedPosts: Flow<List<ScrapedPostMetadataEntity>> = postMetadataDao.getAllScrapedPosts()
    fun getScrapedPostsByPlatform(platform: String): Flow<List<ScrapedPostMetadataEntity>> = postMetadataDao.getPostsByPlatform(platform)
    fun getScrapedPostsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<ScrapedPostMetadataEntity>> = postMetadataDao.getPostsByDateRange(startTimestamp, endTimestamp)
    fun getScrapedPostsAfterDate(afterTimestamp: Long): Flow<List<ScrapedPostMetadataEntity>> = postMetadataDao.getPostsAfterDate(afterTimestamp)
    fun getScrapedPostsBeforeDate(beforeTimestamp: Long): Flow<List<ScrapedPostMetadataEntity>> = postMetadataDao.getPostsBeforeDate(beforeTimestamp)
    fun getScrapedPostsByPlatformAndDateRange(platform: String, startTimestamp: Long, endTimestamp: Long): Flow<List<ScrapedPostMetadataEntity>> = postMetadataDao.getPostsByPlatformAndDateRange(platform, startTimestamp, endTimestamp)
    fun queryPostsByPlatformOrDate(platform: String? = null, startTimestamp: Long? = null, endTimestamp: Long? = null): Flow<List<ScrapedPostMetadataEntity>> = postMetadataDao.queryPostsByPlatformOrDate(platform, startTimestamp, endTimestamp)
    fun getScrapedPostsByCampaign(campaignId: String): Flow<List<ScrapedPostMetadataEntity>> = postMetadataDao.getPostsByCampaign(campaignId)
    suspend fun getScrapedPostById(id: String) = postMetadataDao.getPostById(id)
    fun searchScrapedPosts(query: String) = postMetadataDao.searchPosts(query)
    suspend fun insertScrapedPost(post: ScrapedPostMetadataEntity) = postMetadataDao.insertPost(post)
    suspend fun insertScrapedPosts(posts: List<ScrapedPostMetadataEntity>) = postMetadataDao.insertPosts(posts)
    suspend fun updateScrapedPost(post: ScrapedPostMetadataEntity) = postMetadataDao.updatePost(post)
    suspend fun deleteScrapedPost(id: String) = postMetadataDao.deletePost(id)
    suspend fun deleteScrapedPost(post: ScrapedPostMetadataEntity) = postMetadataDao.deletePost(post)
    suspend fun deletePostsByPlatform(platform: String) = postMetadataDao.deletePostsByPlatform(platform)
    suspend fun deletePostsOlderThan(timestamp: Long) = postMetadataDao.deletePostsOlderThan(timestamp)
    suspend fun deletePostsByDateRange(startTimestamp: Long, endTimestamp: Long) = postMetadataDao.deletePostsByDateRange(startTimestamp, endTimestamp)
    suspend fun deleteAllScrapedPosts() = postMetadataDao.deleteAllPosts()
    val scrapedPostsCount: Flow<Int> = postMetadataDao.getPostsCount()

    // Orchestration Tasks
    val allOrchestrationTasks: Flow<List<OrchestrationTaskEntity>> = db.orchestrationTaskDao().getAllTasks()
    suspend fun insertOrchestrationTask(task: OrchestrationTaskEntity) = db.orchestrationTaskDao().insertTask(task)
    suspend fun updateOrchestrationTask(task: OrchestrationTaskEntity) = db.orchestrationTaskDao().updateTask(task)
    suspend fun deleteOrchestrationTask(id: String) = db.orchestrationTaskDao().deleteTask(id)

    // Research
    val allResearch: Flow<List<ResearchEntity>> = db.researchDao().getAllResearch()
    suspend fun insertResearch(item: ResearchEntity) = db.researchDao().insertResearch(item)

    // Media
    val allMedia: Flow<List<MediaEntity>> = db.mediaDao().getAllMedia()
    suspend fun insertMedia(media: MediaEntity) = db.mediaDao().insertMedia(media)
    suspend fun deleteMedia(id: String) = db.mediaDao().deleteMedia(id)

    // Drafts
    val allDrafts: Flow<List<PostDraftEntity>> = db.postDraftDao().getAllDrafts()
    suspend fun insertDraft(draft: PostDraftEntity) = db.postDraftDao().insertDraft(draft)
    suspend fun updateDraft(draft: PostDraftEntity) = db.postDraftDao().updateDraft(draft)
    suspend fun deleteDraft(id: String) = db.postDraftDao().deleteDraft(id)

    // Tab Task Logs
    val allTabTaskLogs: Flow<List<TabTaskLogEntity>> = db.tabTaskLogDao().getAllLogs()
    fun getLogsByTabId(tabId: String): Flow<List<TabTaskLogEntity>> = db.tabTaskLogDao().getLogsByTabId(tabId)
    suspend fun insertTaskLog(log: TabTaskLogEntity) = db.tabTaskLogDao().insertLog(log)
    suspend fun updateTaskLog(log: TabTaskLogEntity) = db.tabTaskLogDao().updateLog(log)
    suspend fun deleteTaskLogById(id: String) = db.tabTaskLogDao().deleteLogById(id)
    suspend fun deleteLogsByTabId(tabId: String) = db.tabTaskLogDao().deleteLogsByTabId(tabId)
    suspend fun clearAllTaskLogs() = db.tabTaskLogDao().clearAllLogs()

    // Offline Recent Pages
    val recentPageDao = db.recentPageDao()
    val allRecentPages: Flow<List<RecentPageEntity>> = recentPageDao.getAllRecentPages()
    val pinnedRecentPages: Flow<List<RecentPageEntity>> = recentPageDao.getPinnedRecentPages()
    val recentPagesCount: Flow<Int> = recentPageDao.getRecentPagesCount()

    fun getRecentPagesByPlatform(platform: String): Flow<List<RecentPageEntity>> =
        recentPageDao.getRecentPagesByPlatform(platform)

    fun searchRecentPages(query: String): Flow<List<RecentPageEntity>> =
        recentPageDao.searchRecentPages(query)

    suspend fun getRecentPageByUrl(url: String): RecentPageEntity? =
        recentPageDao.getRecentPageByUrl(url)

    suspend fun insertRecentPage(page: RecentPageEntity) =
        recentPageDao.insertRecentPage(page)

    suspend fun insertRecentPages(pages: List<RecentPageEntity>) =
        recentPageDao.insertRecentPages(pages)

    suspend fun recordPageVisit(
        url: String,
        title: String,
        platform: String = detectPlatformFromUrl(url),
        snippet: String = "",
        faviconUrl: String? = null
    ) {
        val existing = recentPageDao.getRecentPageByUrl(url)
        if (existing != null) {
            recentPageDao.updateRecentPage(
                existing.copy(
                    title = if (title.isNotBlank()) title else existing.title,
                    platform = platform,
                    previewSnippet = if (snippet.isNotBlank()) snippet else existing.previewSnippet,
                    visitCount = existing.visitCount + 1,
                    lastVisitedTimestamp = System.currentTimeMillis()
                )
            )
        } else {
            recentPageDao.insertRecentPage(
                RecentPageEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    url = url,
                    title = if (title.isNotBlank()) title else url,
                    platform = platform,
                    faviconUrl = faviconUrl,
                    previewSnippet = snippet,
                    visitCount = 1,
                    lastVisitedTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun togglePinRecentPage(id: String, isPinned: Boolean) =
        recentPageDao.togglePin(id, isPinned)

    suspend fun deleteRecentPageById(id: String) =
        recentPageDao.deleteRecentPageById(id)

    suspend fun clearAllRecentPages() =
        recentPageDao.clearAllRecentPages()

    private fun detectPlatformFromUrl(url: String): String {
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
}

