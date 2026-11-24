package com.example.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scraped_posts_metadata",
    indices = [
        Index(value = ["platform"]),
        Index(value = ["scrapedTimestamp"]),
        Index(value = ["originalPostUrl"])
    ]
)
data class ScrapedPostMetadataEntity(
    @PrimaryKey
    val id: String,
    val platform: String, // "X_TWITTER", "LINKEDIN", "INSTAGRAM", "TIKTOK", "FACEBOOK", "REDDIT", "WEB"
    val originalPostUrl: String,
    val authorHandle: String = "",
    val authorDisplayName: String = "",
    val authorAvatarUrl: String? = null,
    val postContent: String = "",
    val mediaUrlsJson: String = "[]",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val viewsCount: Int = 0,
    val scrapedTimestamp: Long = System.currentTimeMillis(),
    val postPublishedTimestamp: Long? = null,
    val campaignId: String? = null,
    val sentimentScore: Float? = null,
    val tagsCsv: String = "",
    val isArchived: Boolean = false
)

typealias PostMetadataEntity = ScrapedPostMetadataEntity

@Entity(tableName = "browser_tabs")
data class BrowserTabEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val url: String,
    val faviconUrl: String? = null,
    val analysisSummary: String = "",
    val userPrompt: String = "",
    val executionResult: String = "",
    val status: String = "IDLE", // "IDLE", "ANALYZING", "READY", "EXECUTING", "COMPLETED", "ERROR"
    val detectedElementsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "browser_data")
data class BrowserDataEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val url: String,
    val htmlContent: String = "",
    val textContent: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "token_logs")
data class TokenLogEntity(
    @PrimaryKey
    val id: String,
    val modelName: String,
    val promptTokens: Int,
    val candidateTokens: Int,
    val totalTokens: Int,
    val costUsd: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "orchestration_tasks",
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class OrchestrationTaskEntity(
    @PrimaryKey
    val id: String,
    val taskType: String,
    val prompt: String,
    val status: String, // "PENDING", "RUNNING", "COMPLETED", "FAILED"
    val result: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(tableName = "agent_tools")
data class ToolEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val isEnabled: Boolean = true
)

@Entity(tableName = "research_items")
data class ResearchEntity(
    @PrimaryKey
    val id: String,
    val topic: String,
    val content: String,
    val sourceUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "media_assets")
data class MediaEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val uri: String,
    val mimeType: String,
    val fileSize: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "post_drafts")
data class PostDraftEntity(
    @PrimaryKey
    val id: String,
    val platform: String,
    val content: String,
    val scheduledTimestamp: Long? = null,
    val isPublished: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tab_task_logs",
    indices = [
        Index(value = ["tabId"]),
        Index(value = ["timestamp"]),
        Index(value = ["status"])
    ]
)
data class TabTaskLogEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val tabId: String,
    val tabTitle: String,
    val tabUrl: String,
    val taskPrompt: String,
    val status: String = "COMPLETED", // "IN_PROGRESS", "COMPLETED", "FAILED"
    val outputResult: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L,
    val tokensUsed: Int = 0
)

@Entity(
    tableName = "recent_pages",
    indices = [
        Index(value = ["url"], unique = true),
        Index(value = ["platform"]),
        Index(value = ["lastVisitedTimestamp"]),
        Index(value = ["isPinned"])
    ]
)
data class RecentPageEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val platform: String = "WEB", // "X_TWITTER", "LINKEDIN", "INSTAGRAM", "TIKTOK", "FACEBOOK", "REDDIT", "WEB"
    val faviconUrl: String? = null,
    val ogImageUrl: String? = null,
    val siteName: String? = null,
    val previewSnippet: String = "",
    val visitCount: Int = 1,
    val lastVisitedTimestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isAvailableOffline: Boolean = true
)

@Entity(
    tableName = "cached_scraped_metadata",
    indices = [
        Index(value = ["requestedUrl"]),
        Index(value = ["resolvedUrl"], unique = true),
        Index(value = ["scrapedAt"]),
        Index(value = ["platform"])
    ]
)
data class CachedMetadataEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val requestedUrl: String,
    val resolvedUrl: String,
    val title: String,
    val description: String = "",
    val ogImageUrl: String? = null,
    val siteName: String? = null,
    val ogType: String? = null,
    val faviconUrl: String? = null,
    val platform: String = "WEB",
    val author: String? = null,
    val hashtagsCsv: String = "",
    val extractedTagsCount: Int = 0,
    val scrapedAt: Long = System.currentTimeMillis()
)

