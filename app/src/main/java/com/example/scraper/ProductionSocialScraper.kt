package com.example.scraper

import com.example.browser.AndroidBrowserManager
import com.example.browser.DomSnapshot
import com.example.database.entity.ScrapedPostMetadataEntity
import com.example.database.repository.SocialAgentRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Production implementation of SocialMediaScraper.
 * Combines live WebView DOM snapshot extraction, real HTTP OpenGraph/JSON-LD scraping,
 * and cached Room persistence without fabricated mock posts.
 */
class ProductionSocialScraper(
    private val repository: SocialAgentRepository,
    private val openGraphScraper: OpenGraphMetadataScraper,
    private val browserManager: AndroidBrowserManager
) : SocialMediaScraper {

    override suspend fun scrapePlatformPosts(platform: String, limit: Int): List<ScrapedPostMetadataEntity> {
        val collected = mutableListOf<ScrapedPostMetadataEntity>()
        val targetPlatform = platform.uppercase()

        // 1. Extract live posts directly from open WebView tabs matching the platform
        val matchingTabs = browserManager.tabs.value.filter {
            targetPlatform == "ALL" || detectPlatform(it.url) == targetPlatform
        }
        for (tab in matchingTabs) {
            val snapshot = browserManager.extractDomSnapshot(tab.id)
            if (snapshot != null && (snapshot.postSnippets.isNotEmpty() || snapshot.textExcerpt.isNotBlank())) {
                val fromDom = convertSnapshotToPosts(snapshot, detectPlatform(tab.url))
                for (entity in fromDom) {
                    if (collected.size < limit) {
                        repository.upsertScrapedPostByUrl(entity)
                        collected.add(entity)
                    }
                }
            }
        }

        // 2. Scrape real OpenGraph/HTML metadata concurrently from matching tab URLs + stored RecentPages
        val candidateUrls = buildList {
            matchingTabs.forEach { add(it.url) }
            repository.getAllRecentPagesSync()
                .filter { targetPlatform == "ALL" || it.platform == targetPlatform }
                .forEach { add(it.url) }
        }.distinct().take(limit.coerceAtLeast(1))

        if (candidateUrls.isNotEmpty()) {
            val report = openGraphScraper.scrapeBatch(candidateUrls, maxConcurrency = 3)
            for (meta in report.succeeded) {
                val existing = repository.findScrapedPostByUrl(meta.resolvedUrl)
                if (existing != null && collected.none { it.id == existing.id } && collected.size < limit) {
                    collected.add(existing)
                }
            }
        }

        // 3. Fallback to any existing Room posts for that platform when offline/rate-limited
        if (collected.isEmpty()) {
            val cached = repository.getRecentPostsSync(limit)
                .filter { targetPlatform == "ALL" || it.platform == targetPlatform }
            collected.addAll(cached)
        }

        return collected.take(limit)
    }

    override suspend fun scrapeUrl(url: String): ScrapedPostMetadataEntity? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null

        // 1. Check if an open WebView tab already has live DOM content for this URL
        val matchingTab = browserManager.tabs.value.find {
            it.url.equals(trimmed, ignoreCase = true)
        }
        if (matchingTab != null) {
            val dom = browserManager.extractDomSnapshot(matchingTab.id)
            if (dom != null && (dom.postSnippets.isNotEmpty() || !dom.ogDescription.isNullOrBlank())) {
                val domPost = convertSnapshotToPosts(dom, detectPlatform(trimmed)).firstOrNull()
                if (domPost != null) {
                    repository.upsertScrapedPostByUrl(domPost)
                    repository.recordPageVisit(
                        url = dom.url,
                        title = dom.ogTitle ?: dom.title,
                        platform = detectPlatform(dom.url),
                        snippet = dom.ogDescription ?: dom.textExcerpt.take(200),
                        ogImageUrl = dom.ogImageUrl
                    )
                    return domPost
                }
            }
        }

        // 2. Real HTTP OpenGraph + HTML + JSON-LD extraction
        val ogResult = openGraphScraper.scrapeAndSave(trimmed)
        ogResult.onSuccess { meta ->
            return repository.findScrapedPostByUrl(meta.resolvedUrl)
                ?: ScrapedPostMetadataEntity(
                    id = "og-${UUID.randomUUID()}",
                    platform = meta.platform,
                    originalPostUrl = meta.resolvedUrl,
                    authorHandle = meta.siteName ?: "@web",
                    authorDisplayName = meta.author ?: meta.siteName ?: meta.title,
                    authorAvatarUrl = meta.faviconUrl,
                    postContent = buildString {
                        append(meta.title)
                        if (meta.description.isNotBlank()) {
                            append("\n")
                            append(meta.description)
                        }
                    },
                    tagsCsv = (listOf("OpenGraph") + meta.hashtags).distinct().joinToString(","),
                    scrapedTimestamp = meta.scrapedAt
                )
        }

        // 3. Offline fallback: index cached page metadata cleanly without fabricating engagement numbers
        val cachedPage = repository.getRecentPageByUrl(trimmed)
        val platform = detectPlatform(trimmed)
        val fallbackPost = ScrapedPostMetadataEntity(
            id = "cached-${UUID.randomUUID()}",
            platform = platform,
            originalPostUrl = trimmed,
            authorHandle = "@offline_index",
            authorDisplayName = cachedPage?.siteName ?: cachedPage?.title ?: platform,
            postContent = cachedPage?.previewSnippet?.ifBlank { null }
                ?: "تمت فهرسة الرابط في السجل المحلي (أوفلاين): $trimmed",
            tagsCsv = "فهرسة_محلية,$platform",
            scrapedTimestamp = System.currentTimeMillis()
        )
        repository.upsertScrapedPostByUrl(fallbackPost)
        return fallbackPost
    }

    override fun observeScrapedPosts(): Flow<List<ScrapedPostMetadataEntity>> {
        return repository.allScrapedPosts
    }

    suspend fun testDatabasePipeline(): Boolean {
        return repository.verifyDatabaseIntegrity()
    }

    private fun convertSnapshotToPosts(snapshot: DomSnapshot, platform: String): List<ScrapedPostMetadataEntity> {
        val now = System.currentTimeMillis()
        val tags = (listOf("DOM_Live", platform) + snapshot.hashtags.take(5)).distinct().joinToString(",")
        if (snapshot.postSnippets.isNotEmpty()) {
            return snapshot.postSnippets.mapIndexed { idx, snippet ->
                ScrapedPostMetadataEntity(
                    id = "dom-${UUID.randomUUID()}",
                    platform = platform,
                    originalPostUrl = "${snapshot.url}#item-$idx",
                    authorHandle = snapshot.mentions.firstOrNull() ?: "@${platform.lowercase()}",
                    authorDisplayName = snapshot.ogTitle ?: snapshot.title,
                    postContent = snippet,
                    tagsCsv = tags,
                    scrapedTimestamp = now
                )
            }
        }
        val summary = snapshot.ogDescription ?: snapshot.textExcerpt.take(300)
        return if (summary.isNotBlank()) {
            listOf(
                ScrapedPostMetadataEntity(
                    id = "dom-${UUID.randomUUID()}",
                    platform = platform,
                    originalPostUrl = snapshot.url,
                    authorHandle = snapshot.mentions.firstOrNull() ?: "@${platform.lowercase()}",
                    authorDisplayName = snapshot.ogTitle ?: snapshot.title,
                    postContent = summary,
                    tagsCsv = tags,
                    scrapedTimestamp = now
                )
            )
        } else {
            emptyList()
        }
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
}
