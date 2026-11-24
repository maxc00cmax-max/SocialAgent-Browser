package com.example.scraper

import com.example.database.entity.ScrapedPostMetadataEntity
import com.example.database.repository.SocialAgentRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MockSocialMediaScraper(
    private val repository: SocialAgentRepository
) : SocialMediaScraper {

    override suspend fun scrapePlatformPosts(platform: String, limit: Int): List<ScrapedPostMetadataEntity> {
        val now = System.currentTimeMillis()
        val posts = (1..limit).map { index ->
            ScrapedPostMetadataEntity(
                id = "scraped-${platform.lowercase()}-${UUID.randomUUID()}",
                platform = platform,
                originalPostUrl = "https://$platform.com/post/$index",
                authorHandle = "@user_$index",
                authorDisplayName = "خبير $platform #$index",
                postContent = "منشور تحليلي رقم $index عن الذكاء الاصطناعي والتسويق الإلكتروني عبر $platform.",
                likesCount = 150 * index,
                commentsCount = 20 * index,
                sharesCount = 10 * index,
                viewsCount = 1200 * index,
                scrapedTimestamp = now - (index * 3600_000L),
                postPublishedTimestamp = now - (index * 7200_000L)
            )
        }
        repository.insertScrapedPosts(posts)
        return posts
    }

    override suspend fun scrapeUrl(url: String): ScrapedPostMetadataEntity? {
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
            authorHandle = "@web_author",
            authorDisplayName = "ناشر المحتوى",
            postContent = "تم استخراج محتوى الصفحة والبيانات الوصفية بنجاح من الرابط: $url",
            scrapedTimestamp = System.currentTimeMillis()
        )
        repository.insertScrapedPost(entity)
        return entity
    }

    override fun observeScrapedPosts(): Flow<List<ScrapedPostMetadataEntity>> {
        return repository.allScrapedPosts
    }

    suspend fun testDatabasePipeline(): Boolean {
        val sample = ScrapedPostMetadataEntity(
            id = "pipeline-test-id",
            platform = "X_TWITTER",
            originalPostUrl = "https://x.com/pipeline/test",
            authorHandle = "@tester",
            authorDisplayName = "Pipeline Tester",
            postContent = "Verifying database pipeline write and read integrity.",
            scrapedTimestamp = System.currentTimeMillis()
        )
        repository.insertScrapedPost(sample)
        val fetched = repository.getScrapedPostById("pipeline-test-id")
        return fetched != null && fetched.id == sample.id
    }
}
