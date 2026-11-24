package com.example.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserTabDao {
    @Query("SELECT * FROM browser_tabs ORDER BY timestamp DESC")
    fun getAllTabs(): Flow<List<BrowserTabEntity>>

    @Query("SELECT * FROM browser_tabs ORDER BY timestamp ASC")
    suspend fun getAllTabsSync(): List<BrowserTabEntity>

    @Query("SELECT * FROM browser_tabs WHERE id = :id")
    suspend fun getTabById(id: String): BrowserTabEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: BrowserTabEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTabs(tabs: List<BrowserTabEntity>)

    @Update
    suspend fun updateTab(tab: BrowserTabEntity)

    @Delete
    suspend fun deleteTab(tab: BrowserTabEntity)

    @Query("DELETE FROM browser_tabs WHERE id = :id")
    suspend fun deleteTabById(id: String)

    @Query("DELETE FROM browser_tabs")
    suspend fun deleteAllTabs()
}

@Dao
interface PostMetadataDao {
    @Query("SELECT * FROM scraped_posts_metadata ORDER BY scrapedTimestamp DESC")
    fun getAllScrapedPosts(): Flow<List<ScrapedPostMetadataEntity>>

    @Query("SELECT * FROM scraped_posts_metadata ORDER BY scrapedTimestamp DESC")
    fun getAllPosts(): Flow<List<ScrapedPostMetadataEntity>>

    @Query("SELECT * FROM scraped_posts_metadata WHERE platform = :platform ORDER BY scrapedTimestamp DESC")
    fun getPostsByPlatform(platform: String): Flow<List<ScrapedPostMetadataEntity>>

    @Query("SELECT * FROM scraped_posts_metadata WHERE scrapedTimestamp BETWEEN :startTimestamp AND :endTimestamp ORDER BY scrapedTimestamp DESC")
    fun getPostsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<ScrapedPostMetadataEntity>>

    @Query("SELECT * FROM scraped_posts_metadata WHERE scrapedTimestamp >= :afterTimestamp ORDER BY scrapedTimestamp DESC")
    fun getPostsAfterDate(afterTimestamp: Long): Flow<List<ScrapedPostMetadataEntity>>

    @Query("SELECT * FROM scraped_posts_metadata WHERE scrapedTimestamp <= :beforeTimestamp ORDER BY scrapedTimestamp DESC")
    fun getPostsBeforeDate(beforeTimestamp: Long): Flow<List<ScrapedPostMetadataEntity>>

    @Query("SELECT * FROM scraped_posts_metadata WHERE postPublishedTimestamp IS NOT NULL AND postPublishedTimestamp BETWEEN :startTimestamp AND :endTimestamp ORDER BY postPublishedTimestamp DESC")
    fun getPostsByPublishedDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<ScrapedPostMetadataEntity>>

    @Query("SELECT * FROM scraped_posts_metadata WHERE platform = :platform AND scrapedTimestamp BETWEEN :startTimestamp AND :endTimestamp ORDER BY scrapedTimestamp DESC")
    fun getPostsByPlatformAndDateRange(platform: String, startTimestamp: Long, endTimestamp: Long): Flow<List<ScrapedPostMetadataEntity>>

    @Query("""
        SELECT * FROM scraped_posts_metadata 
        WHERE (:platform IS NULL OR platform = :platform)
          AND (:startTimestamp IS NULL OR scrapedTimestamp >= :startTimestamp)
          AND (:endTimestamp IS NULL OR scrapedTimestamp <= :endTimestamp)
        ORDER BY scrapedTimestamp DESC
    """)
    fun queryPostsByPlatformOrDate(
        platform: String? = null,
        startTimestamp: Long? = null,
        endTimestamp: Long? = null
    ): Flow<List<ScrapedPostMetadataEntity>>

    @Query("SELECT * FROM scraped_posts_metadata WHERE campaignId = :campaignId ORDER BY scrapedTimestamp DESC")
    fun getPostsByCampaign(campaignId: String): Flow<List<ScrapedPostMetadataEntity>>

    @Query("SELECT * FROM scraped_posts_metadata WHERE id = :id")
    suspend fun getPostById(id: String): ScrapedPostMetadataEntity?

    @Query("SELECT * FROM scraped_posts_metadata WHERE originalPostUrl = :url ORDER BY scrapedTimestamp DESC LIMIT 1")
    suspend fun findByOriginalUrl(url: String): ScrapedPostMetadataEntity?

    @Query("SELECT * FROM scraped_posts_metadata WHERE postContent LIKE '%' || :query || '%' OR authorHandle LIKE '%' || :query || '%'")
    fun searchPosts(query: String): Flow<List<ScrapedPostMetadataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: ScrapedPostMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<ScrapedPostMetadataEntity>)

    @Update
    suspend fun updatePost(post: ScrapedPostMetadataEntity)

    @Delete
    suspend fun deletePost(post: ScrapedPostMetadataEntity)

    @Query("DELETE FROM scraped_posts_metadata WHERE id = :id")
    suspend fun deletePost(id: String)

    @Query("DELETE FROM scraped_posts_metadata WHERE id = :id")
    suspend fun deletePostById(id: String)

    @Query("DELETE FROM scraped_posts_metadata WHERE platform = :platform")
    suspend fun deletePostsByPlatform(platform: String)

    @Query("DELETE FROM scraped_posts_metadata WHERE scrapedTimestamp < :timestamp")
    suspend fun deletePostsOlderThan(timestamp: Long)

    @Query("DELETE FROM scraped_posts_metadata WHERE scrapedTimestamp BETWEEN :startTimestamp AND :endTimestamp")
    suspend fun deletePostsByDateRange(startTimestamp: Long, endTimestamp: Long)

    @Query("DELETE FROM scraped_posts_metadata")
    suspend fun deleteAllPosts()

    @Query("SELECT COUNT(*) FROM scraped_posts_metadata")
    fun getPostsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scraped_posts_metadata WHERE platform = :platform")
    fun getPostsCountByPlatform(platform: String): Flow<Int>

    @Query("SELECT * FROM scraped_posts_metadata ORDER BY scrapedTimestamp DESC LIMIT :limit")
    suspend fun getRecentPostsSync(limit: Int): List<ScrapedPostMetadataEntity>
}

typealias ScrapedPostMetadataDao = PostMetadataDao

@Dao
interface BrowserDataDao {
    @Query("SELECT * FROM browser_data ORDER BY timestamp DESC")
    fun getAllBrowserData(): Flow<List<BrowserDataEntity>>

    @Query("SELECT * FROM browser_data WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): BrowserDataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrowserData(data: BrowserDataEntity)

    @Query("DELETE FROM browser_data")
    suspend fun deleteAll()
}

@Dao
interface TokenLogDao {
    @Query("SELECT * FROM token_logs ORDER BY timestamp DESC")
    fun getAllTokenLogs(): Flow<List<TokenLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTokenLog(log: TokenLogEntity)

    @Query("SELECT SUM(totalTokens) FROM token_logs")
    fun getTotalTokens(): Flow<Int?>

    @Query("SELECT SUM(costUsd) FROM token_logs")
    fun getTotalCostUsd(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM token_logs")
    fun getTokenLogCount(): Flow<Int>

    @Query("DELETE FROM token_logs")
    suspend fun deleteAllTokenLogs()
}

@Dao
interface ToolDao {
    @Query("SELECT * FROM agent_tools")
    fun getAllTools(): Flow<List<ToolEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTool(tool: ToolEntity)

    @Query("DELETE FROM agent_tools WHERE id = :id")
    suspend fun deleteTool(id: String)
}

@Dao
interface OrchestrationTaskDao {
    @Query("SELECT * FROM orchestration_tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<OrchestrationTaskEntity>>

    @Query("SELECT * FROM orchestration_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): OrchestrationTaskEntity?

    @Query("SELECT COUNT(*) FROM orchestration_tasks WHERE status IN ('PENDING', 'RUNNING')")
    fun getActiveTasksCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: OrchestrationTaskEntity)

    @Update
    suspend fun updateTask(task: OrchestrationTaskEntity)

    @Query("DELETE FROM orchestration_tasks WHERE id = :id")
    suspend fun deleteTask(id: String)

    @Query("DELETE FROM orchestration_tasks WHERE status IN ('COMPLETED', 'FAILED')")
    suspend fun clearCompletedTasks()
}

@Dao
interface ResearchDao {
    @Query("SELECT * FROM research_items ORDER BY timestamp DESC")
    fun getAllResearch(): Flow<List<ResearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResearch(item: ResearchEntity)
}

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_assets ORDER BY timestamp DESC")
    fun getAllMedia(): Flow<List<MediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaEntity)

    @Query("DELETE FROM media_assets WHERE id = :id")
    suspend fun deleteMedia(id: String)
}

@Dao
interface PostDraftDao {
    @Query("SELECT * FROM post_drafts ORDER BY createdAt DESC")
    fun getAllDrafts(): Flow<List<PostDraftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: PostDraftEntity)

    @Update
    suspend fun updateDraft(draft: PostDraftEntity)

    @Query("DELETE FROM post_drafts WHERE id = :id")
    suspend fun deleteDraft(id: String)
}

@Dao
interface TabTaskLogDao {
    @Query("SELECT * FROM tab_task_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<TabTaskLogEntity>>

    @Query("SELECT * FROM tab_task_logs WHERE tabId = :tabId ORDER BY timestamp DESC")
    fun getLogsByTabId(tabId: String): Flow<List<TabTaskLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TabTaskLogEntity)

    @Update
    suspend fun updateLog(log: TabTaskLogEntity)

    @Query("DELETE FROM tab_task_logs WHERE id = :id")
    suspend fun deleteLogById(id: String)

    @Query("DELETE FROM tab_task_logs WHERE tabId = :tabId")
    suspend fun deleteLogsByTabId(tabId: String)

    @Query("DELETE FROM tab_task_logs")
    suspend fun clearAllLogs()
}

@Dao
interface RecentPageDao {
    @Query("SELECT * FROM recent_pages ORDER BY isPinned DESC, lastVisitedTimestamp DESC")
    fun getAllRecentPages(): Flow<List<RecentPageEntity>>

    @Query("SELECT * FROM recent_pages ORDER BY isPinned DESC, lastVisitedTimestamp DESC")
    suspend fun getAllRecentPagesSync(): List<RecentPageEntity>

    @Query("SELECT * FROM recent_pages WHERE platform = :platform ORDER BY isPinned DESC, lastVisitedTimestamp DESC")
    fun getRecentPagesByPlatform(platform: String): Flow<List<RecentPageEntity>>

    @Query("SELECT * FROM recent_pages WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' OR previewSnippet LIKE '%' || :query || '%' ORDER BY isPinned DESC, lastVisitedTimestamp DESC")
    fun searchRecentPages(query: String): Flow<List<RecentPageEntity>>

    @Query("SELECT * FROM recent_pages WHERE isPinned = 1 ORDER BY lastVisitedTimestamp DESC")
    fun getPinnedRecentPages(): Flow<List<RecentPageEntity>>

    @Query("SELECT * FROM recent_pages WHERE url = :url LIMIT 1")
    suspend fun getRecentPageByUrl(url: String): RecentPageEntity?

    @Query("SELECT * FROM recent_pages WHERE id = :id LIMIT 1")
    suspend fun getRecentPageById(id: String): RecentPageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentPage(page: RecentPageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentPages(pages: List<RecentPageEntity>)

    @Update
    suspend fun updateRecentPage(page: RecentPageEntity)

    @Query("UPDATE recent_pages SET isPinned = :isPinned WHERE id = :id")
    suspend fun togglePin(id: String, isPinned: Boolean)

    @Delete
    suspend fun deleteRecentPage(page: RecentPageEntity)

    @Query("DELETE FROM recent_pages WHERE id = :id")
    suspend fun deleteRecentPageById(id: String)

    @Query("DELETE FROM recent_pages")
    suspend fun clearAllRecentPages()

    @Query("SELECT COUNT(*) FROM recent_pages")
    fun getRecentPagesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM recent_pages WHERE isPinned = 1")
    fun getPinnedCount(): Flow<Int>

    @Query("SELECT SUM(visitCount) FROM recent_pages")
    fun getTotalVisitsSum(): Flow<Int?>
}

@Dao
interface CachedMetadataDao {
    @Query("SELECT * FROM cached_scraped_metadata ORDER BY scrapedAt DESC")
    fun getAllCachedMetadata(): Flow<List<CachedMetadataEntity>>

    @Query("SELECT * FROM cached_scraped_metadata ORDER BY scrapedAt DESC")
    suspend fun getAllCachedMetadataSync(): List<CachedMetadataEntity>

    @Query(
        """
        SELECT * FROM cached_scraped_metadata
        WHERE title LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
           OR resolvedUrl LIKE '%' || :query || '%'
           OR siteName LIKE '%' || :query || '%'
        ORDER BY scrapedAt DESC
        """
    )
    fun searchCachedMetadata(query: String): Flow<List<CachedMetadataEntity>>

    @Query("SELECT * FROM cached_scraped_metadata WHERE resolvedUrl = :url OR requestedUrl = :url ORDER BY scrapedAt DESC LIMIT 1")
    suspend fun getByUrl(url: String): CachedMetadataEntity?

    @Query("SELECT * FROM cached_scraped_metadata WHERE resolvedUrl = :url OR requestedUrl = :url ORDER BY scrapedAt DESC LIMIT 1")
    fun observeByUrl(url: String): Flow<CachedMetadataEntity?>

    @Query("SELECT COUNT(*) FROM cached_scraped_metadata")
    fun getCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: CachedMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CachedMetadataEntity>)

    @Query("DELETE FROM cached_scraped_metadata WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM cached_scraped_metadata")
    suspend fun clearAll()
}

