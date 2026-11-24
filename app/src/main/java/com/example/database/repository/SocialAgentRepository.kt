package com.example.database.repository

import androidx.room.withTransaction
import com.example.database.AppDatabase
import com.example.database.entity.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class SocialAgentRepository(private val db: AppDatabase) {

    // Browser Tabs
    val allTabs: Flow<List<BrowserTabEntity>> = db.browserTabDao().getAllTabs()
    suspend fun getAllTabsSync(): List<BrowserTabEntity> = db.browserTabDao().getAllTabsSync()
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
    val totalCostUsd: Flow<Double?> = db.tokenLogDao().getTotalCostUsd()
    suspend fun insertTokenLog(log: TokenLogEntity) = db.tokenLogDao().insertTokenLog(log)
    suspend fun clearAllTokenLogs() = db.tokenLogDao().deleteAllTokenLogs()

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
    suspend fun findScrapedPostByUrl(url: String) = postMetadataDao.findByOriginalUrl(url)
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
    suspend fun getRecentPostsSync(limit: Int = 30) = postMetadataDao.getRecentPostsSync(limit)
    val scrapedPostsCount: Flow<Int> = postMetadataDao.getPostsCount()

    suspend fun upsertScrapedPostByUrl(post: ScrapedPostMetadataEntity) {
        db.withTransaction {
            val existing = postMetadataDao.findByOriginalUrl(post.originalPostUrl)
            if (existing != null) {
                postMetadataDao.updatePost(
                    post.copy(
                        id = existing.id,
                        likesCount = maxOf(existing.likesCount, post.likesCount),
                        commentsCount = maxOf(existing.commentsCount, post.commentsCount),
                        sharesCount = maxOf(existing.sharesCount, post.sharesCount),
                        viewsCount = maxOf(existing.viewsCount, post.viewsCount)
                    )
                )
            } else {
                postMetadataDao.insertPost(post)
            }
        }
    }

    // Orchestration Tasks
    val allOrchestrationTasks: Flow<List<OrchestrationTaskEntity>> = db.orchestrationTaskDao().getAllTasks()
    val activeOrchestrationTasksCount: Flow<Int> = db.orchestrationTaskDao().getActiveTasksCount()
    suspend fun getOrchestrationTaskById(id: String) = db.orchestrationTaskDao().getTaskById(id)
    suspend fun insertOrchestrationTask(task: OrchestrationTaskEntity) = db.orchestrationTaskDao().insertTask(task)
    suspend fun updateOrchestrationTask(task: OrchestrationTaskEntity) = db.orchestrationTaskDao().updateTask(task)
    suspend fun deleteOrchestrationTask(id: String) = db.orchestrationTaskDao().deleteTask(id)
    suspend fun clearCompletedOrchestrationTasks() = db.orchestrationTaskDao().clearCompletedTasks()

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
    val pinnedRecentPagesCount: Flow<Int> = recentPageDao.getPinnedCount()
    val totalPageVisitsSum: Flow<Int?> = recentPageDao.getTotalVisitsSum()

    suspend fun getAllRecentPagesSync(): List<RecentPageEntity> =
        recentPageDao.getAllRecentPagesSync()

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
        faviconUrl: String? = null,
        ogImageUrl: String? = null,
        siteName: String? = null
    ) {
        db.withTransaction {
            upsertRecentPageInternal(url, title, platform, snippet, faviconUrl, ogImageUrl, siteName)
        }
    }

    // Offline Cached Scraped Metadata
    val cachedMetadataDao = db.cachedMetadataDao()
    val allCachedMetadata: Flow<List<CachedMetadataEntity>> = cachedMetadataDao.getAllCachedMetadata()
    val cachedMetadataCount: Flow<Int> = cachedMetadataDao.getCount()

    fun searchCachedMetadata(query: String): Flow<List<CachedMetadataEntity>> =
        cachedMetadataDao.searchCachedMetadata(query)

    suspend fun getCachedMetadataByUrl(url: String): CachedMetadataEntity? =
        cachedMetadataDao.getByUrl(url)

    suspend fun getAllCachedMetadataSync(): List<CachedMetadataEntity> =
        cachedMetadataDao.getAllCachedMetadataSync()

    suspend fun upsertCachedMetadata(metadata: CachedMetadataEntity) {
        db.withTransaction {
            val existing = cachedMetadataDao.getByUrl(metadata.resolvedUrl)
                ?: cachedMetadataDao.getByUrl(metadata.requestedUrl)
            cachedMetadataDao.upsert(
                if (existing != null) metadata.copy(id = existing.id) else metadata
            )
        }
    }

    suspend fun deleteCachedMetadataById(id: String) =
        cachedMetadataDao.deleteById(id)

    suspend fun clearAllCachedMetadata() =
        cachedMetadataDao.clearAll()

    suspend fun togglePinRecentPage(id: String, isPinned: Boolean) =
        recentPageDao.togglePin(id, isPinned)

    suspend fun deleteRecentPageById(id: String) =
        recentPageDao.deleteRecentPageById(id)

    suspend fun clearAllRecentPages() =
        recentPageDao.clearAllRecentPages()

    /**
     * Atomically writes OpenGraph / scraped page metadata across CachedMetadataEntity,
     * RecentPageEntity, ScrapedPostMetadataEntity, and BrowserDataEntity within a single SQLite transaction.
     */
    suspend fun persistOpenGraphBundle(
        url: String,
        title: String,
        platform: String,
        snippet: String,
        faviconUrl: String?,
        ogImageUrl: String?,
        siteName: String?,
        postEntity: ScrapedPostMetadataEntity,
        browserDataEntity: BrowserDataEntity,
        cachedMetadataEntity: CachedMetadataEntity? = null
    ) {
        db.withTransaction {
            upsertRecentPageInternal(url, title, platform, snippet, faviconUrl, ogImageUrl, siteName)

            if (cachedMetadataEntity != null) {
                val existingCache = cachedMetadataDao.getByUrl(cachedMetadataEntity.resolvedUrl)
                    ?: cachedMetadataDao.getByUrl(cachedMetadataEntity.requestedUrl)
                cachedMetadataDao.upsert(
                    if (existingCache != null) cachedMetadataEntity.copy(id = existingCache.id) else cachedMetadataEntity
                )
            }

            val existingPost = postMetadataDao.findByOriginalUrl(postEntity.originalPostUrl)
            if (existingPost != null) {
                postMetadataDao.updatePost(
                    postEntity.copy(
                        id = existingPost.id,
                        likesCount = maxOf(existingPost.likesCount, postEntity.likesCount),
                        commentsCount = maxOf(existingPost.commentsCount, postEntity.commentsCount),
                        sharesCount = maxOf(existingPost.sharesCount, postEntity.sharesCount),
                        viewsCount = maxOf(existingPost.viewsCount, postEntity.viewsCount)
                    )
                )
            } else {
                postMetadataDao.insertPost(postEntity)
            }

            val existingBrowserData = db.browserDataDao().getByUrl(browserDataEntity.url)
            if (existingBrowserData != null) {
                db.browserDataDao().insertBrowserData(
                    browserDataEntity.copy(id = existingBrowserData.id)
                )
            } else {
                db.browserDataDao().insertBrowserData(browserDataEntity)
            }
        }
    }

    private suspend fun upsertRecentPageInternal(
        url: String,
        title: String,
        platform: String,
        snippet: String,
        faviconUrl: String?,
        ogImageUrl: String?,
        siteName: String?
    ) {
        val existing = recentPageDao.getRecentPageByUrl(url)
        if (existing != null) {
            recentPageDao.updateRecentPage(
                existing.copy(
                    title = if (title.isNotBlank()) title else existing.title,
                    platform = platform,
                    previewSnippet = if (snippet.isNotBlank()) snippet else existing.previewSnippet,
                    faviconUrl = faviconUrl ?: existing.faviconUrl,
                    ogImageUrl = ogImageUrl ?: existing.ogImageUrl,
                    siteName = siteName ?: existing.siteName,
                    visitCount = existing.visitCount + 1,
                    lastVisitedTimestamp = System.currentTimeMillis()
                )
            )
        } else {
            recentPageDao.insertRecentPage(
                RecentPageEntity(
                    id = UUID.randomUUID().toString(),
                    url = url,
                    title = if (title.isNotBlank()) title else url,
                    platform = platform,
                    faviconUrl = faviconUrl,
                    ogImageUrl = ogImageUrl,
                    siteName = siteName,
                    previewSnippet = snippet,
                    visitCount = 1,
                    lastVisitedTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Verifies transactional read/write/delete integrity on Room SQLite.
     */
    suspend fun verifyDatabaseIntegrity(): Boolean {
        return try {
            db.withTransaction {
                val probeId = "db-health-probe"
                val probe = ScrapedPostMetadataEntity(
                    id = probeId,
                    platform = "WEB",
                    originalPostUrl = "https://internal.health/check",
                    authorHandle = "@system",
                    authorDisplayName = "Health Probe",
                    postContent = "Room transactional integrity check"
                )
                postMetadataDao.insertPost(probe)
                val fetched = postMetadataDao.getPostById(probeId)
                postMetadataDao.deletePostById(probeId)
                fetched != null && fetched.id == probeId
            }
        } catch (_: Exception) {
            false
        }
    }

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
