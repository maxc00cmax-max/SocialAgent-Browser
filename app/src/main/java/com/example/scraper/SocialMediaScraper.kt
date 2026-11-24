package com.example.scraper

import com.example.database.entity.ScrapedPostMetadataEntity
import kotlinx.coroutines.flow.Flow

interface SocialMediaScraper {
    suspend fun scrapePlatformPosts(platform: String, limit: Int = 10): List<ScrapedPostMetadataEntity>
    suspend fun scrapeUrl(url: String): ScrapedPostMetadataEntity?
    fun observeScrapedPosts(): Flow<List<ScrapedPostMetadataEntity>>
}
