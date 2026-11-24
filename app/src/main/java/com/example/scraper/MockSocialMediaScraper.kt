package com.example.scraper

import com.example.database.entity.ScrapedPostMetadataEntity
import com.example.database.repository.SocialAgentRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Compatibility adapter preserving the existing SocialMediaScraper test & pipeline hooks,
 * backed by real OpenGraphMetadataScraper and Room persistence.
 */
class MockSocialMediaScraper(
    private val repository: SocialAgentRepository,
    private val openGraphScraper: OpenGraphMetadataScraper = OpenGraphMetadataScraper(repository)
) : SocialMediaScraper {

    override suspend fun scrapePlatformPosts(platform: String, limit: Int): List<ScrapedPostMetadataEntity> {
        val targetUrls = repository.getAllRecentPagesSync()
            .filter { platform == "ALL" || it.platform == platform }
            .map { it.url }
            .take(limit)

        if (targetUrls.isNotEmpty()) {
            openGraphScraper.scrapeBatch(targetUrls, maxConcurrency = 3)
        }
        return repository.getRecentPostsSync(limit)
            .filter { platform == "ALL" || it.platform == platform }
    }

    override suspend fun scrapeUrl(url: String): ScrapedPostMetadataEntity? {
        val result = openGraphScraper.scrapeAndSave(url)
        result.onSuccess { meta ->
            return repository.findScrapedPostByUrl(meta.resolvedUrl)
        }

        val platform = when {
            url.contains("x.com") || url.contains("twitter.com") -> "X_TWITTER"
            url.contains("linkedin.com") -> "LINKEDIN"
            url.contains("instagram.com") -> "INSTAGRAM"
            url.contains("reddit.com") -> "REDDIT"
            else -> "WEB"
        }
        val entity = ScrapedPostMetadataEntity(
            id = "url-${UUID.randomUUID()}",
            platform = platform,
            originalPostUrl = url,
            authorHandle = "@web_index",
            authorDisplayName = platform,
            postContent = "فهرسة الرابط محلياً في Room: $url",
            scrapedTimestamp = System.currentTimeMillis()
        )
        repository.upsertScrapedPostByUrl(entity)
        return entity
    }

    override fun observeScrapedPosts(): Flow<List<ScrapedPostMetadataEntity>> {
        return repository.allScrapedPosts
    }

    suspend fun testDatabasePipeline(): Boolean {
        return repository.verifyDatabaseIntegrity()
    }
}
