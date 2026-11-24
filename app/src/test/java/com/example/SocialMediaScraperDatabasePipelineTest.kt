package com.example

import com.example.database.entity.ScrapedPostMetadataEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.UUID

class SocialMediaScraperDatabasePipelineTest {

    @Test
    fun testScrapedPostMetadataEntityCreation() {
        val id = UUID.randomUUID().toString()
        val post = ScrapedPostMetadataEntity(
            id = id,
            platform = "X_TWITTER",
            originalPostUrl = "https://x.com/user/status/123456789",
            authorHandle = "@marketing_pro",
            authorDisplayName = "Marketing Pro",
            postContent = "Exploring AI Agents for automated social media growth!",
            likesCount = 250,
            commentsCount = 45,
            sharesCount = 30,
            viewsCount = 5000,
            scrapedTimestamp = 1726000000000L
        )

        assertNotNull(post)
        assertEquals("X_TWITTER", post.platform)
        assertEquals(250, post.likesCount)
        assertEquals(1726000000000L, post.scrapedTimestamp)
    }

    @Test
    fun testPlatformFilteringLogic() {
        val posts = listOf(
            ScrapedPostMetadataEntity("1", "X_TWITTER", "https://x.com/1"),
            ScrapedPostMetadataEntity("2", "LINKEDIN", "https://linkedin.com/1"),
            ScrapedPostMetadataEntity("3", "X_TWITTER", "https://x.com/2"),
            ScrapedPostMetadataEntity("4", "INSTAGRAM", "https://instagram.com/1")
        )

        val twitterPosts = posts.filter { it.platform == "X_TWITTER" }
        assertEquals(2, twitterPosts.size)

        val linkedInPosts = posts.filter { it.platform == "LINKEDIN" }
        assertEquals(1, linkedInPosts.size)
    }
}
