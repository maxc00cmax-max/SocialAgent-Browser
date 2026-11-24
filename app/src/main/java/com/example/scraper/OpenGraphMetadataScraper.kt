package com.example.scraper

import com.example.database.entity.BrowserDataEntity
import com.example.database.entity.CachedMetadataEntity
import com.example.database.entity.ScrapedPostMetadataEntity
import com.example.database.repository.SocialAgentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.TimeUnit

data class OpenGraphMetadata(
    val requestedUrl: String,
    val resolvedUrl: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val siteName: String?,
    val ogType: String?,
    val faviconUrl: String?,
    val platform: String,
    val extractedTagsCount: Int,
    val author: String? = null,
    val hashtags: List<String> = emptyList(),
    val scrapedAt: Long = System.currentTimeMillis(),
    val isFromCache: Boolean = false
)

data class BatchScrapeReport(
    val totalRequested: Int,
    val succeeded: List<OpenGraphMetadata>,
    val failures: Map<String, String>
)

sealed class OpenGraphScrapeState {
    data object Idle : OpenGraphScrapeState()
    data class Loading(val url: String) : OpenGraphScrapeState()
    data class Success(val metadata: OpenGraphMetadata, val fromCache: Boolean = metadata.isFromCache) : OpenGraphScrapeState()
    data class Error(val url: String, val message: String) : OpenGraphScrapeState()
}

fun CachedMetadataEntity.toOpenGraphMetadata(): OpenGraphMetadata = OpenGraphMetadata(
    requestedUrl = requestedUrl,
    resolvedUrl = resolvedUrl,
    title = title,
    description = description,
    imageUrl = ogImageUrl,
    siteName = siteName,
    ogType = ogType,
    faviconUrl = faviconUrl,
    platform = platform,
    extractedTagsCount = extractedTagsCount,
    author = author,
    hashtags = hashtagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
    scrapedAt = scrapedAt,
    isFromCache = true
)

/**
 * Production-grade OpenGraph, Twitter Card, JSON-LD & HTML Metadata Scraper
 * backed by OkHttp (bounded payload & SSRF guard) and atomic Room caching.
 */
class OpenGraphMetadataScraper(
    private val repository: SocialAgentRepository,
    private val client: OkHttpClient = defaultOkHttpClient()
) {
    /**
     * Cache-first metadata retrieval: returns locally cached metadata from Room instantly
     * without making a network call unless [forceRefresh] is true or no cache entry exists.
     */
    suspend fun scrapeOrGetCached(
        rawUrl: String,
        forceRefresh: Boolean = false
    ): Result<OpenGraphMetadata> = withContext(Dispatchers.IO) {
        val normalizedUrl = validateAndNormalizeUrl(rawUrl)
            ?: return@withContext Result.failure(
                IllegalArgumentException("يرجى إدخال رابط HTTP/HTTPS صالح وآمن")
            )

        if (!forceRefresh) {
            val cached = repository.getCachedMetadataByUrl(normalizedUrl)
                ?: repository.getCachedMetadataByUrl(rawUrl.trim())
            if (cached != null) {
                return@withContext Result.success(cached.toOpenGraphMetadata())
            }
        }

        scrapeAndSave(normalizedUrl)
    }

    suspend fun scrapeAndSave(rawUrl: String): Result<OpenGraphMetadata> = withContext(Dispatchers.IO) {
        val normalizedUrl = validateAndNormalizeUrl(rawUrl)
            ?: return@withContext Result.failure(
                IllegalArgumentException("يرجى إدخال رابط HTTP/HTTPS صالح وآمن")
            )

        try {
            val request = Request.Builder()
                .url(normalizedUrl)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                )
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ar,en-US;q=0.9,en;q=0.8")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("تعذر جلب الصفحة (HTTP ${response.code}): ${response.message}")
                    )
                }

                val contentType = response.header("Content-Type")?.lowercase().orEmpty()
                if (contentType.isNotBlank() &&
                    !contentType.contains("text/") &&
                    !contentType.contains("html") &&
                    !contentType.contains("xml")
                ) {
                    return@withContext Result.failure(
                        Exception("نوع المحتوى غير نصي ($contentType)")
                    )
                }

                val bodyString = readBodySafely(response.body, MAX_HTML_BYTES)
                if (bodyString.isBlank()) {
                    return@withContext Result.failure(
                        Exception("الصفحة أرجعت محتوى فارغاً")
                    )
                }

                val metadata = parseHtmlAndSave(finalUrl, bodyString, normalizedUrl)
                Result.success(metadata)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Scrapes multiple URLs concurrently using bounded parallelism (Semaphore)
     * and returns an aggregated report of successes and per-URL failures.
     */
    suspend fun scrapeBatch(
        urls: List<String>,
        maxConcurrency: Int = 3
    ): BatchScrapeReport = coroutineScope {
        val uniqueUrls = urls.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (uniqueUrls.isEmpty()) {
            return@coroutineScope BatchScrapeReport(0, emptyList(), emptyMap())
        }

        val semaphore = Semaphore(maxConcurrency.coerceIn(1, 6))
        val results = uniqueUrls.map { targetUrl ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    targetUrl to scrapeAndSave(targetUrl)
                }
            }
        }.awaitAll()

        val succeeded = mutableListOf<OpenGraphMetadata>()
        val failures = linkedMapOf<String, String>()
        results.forEach { (url, res) ->
            res.onSuccess { succeeded.add(it) }
                .onFailure { failures[url] = it.localizedMessage ?: "خطأ غير متوقع" }
        }

        BatchScrapeReport(
            totalRequested = uniqueUrls.size,
            succeeded = succeeded,
            failures = failures
        )
    }

    suspend fun parseHtmlAndSave(
        resolvedUrl: String,
        html: String,
        requestedUrl: String = resolvedUrl
    ): OpenGraphMetadata = withContext(Dispatchers.IO) {
        val metaMap = extractMetaTags(html)
        val jsonLd = extractJsonLdInfo(html)
        val htmlTitle = extractHtmlTitle(html)
        val faviconRaw = extractFaviconUrl(html)
        val canonicalHref = extractCanonicalUrl(html)

        val hostFallback = try {
            URI(resolvedUrl).host ?: resolvedUrl
        } catch (_: Exception) {
            resolvedUrl
        }

        val ogTitle = metaMap["og:title"]
            ?: metaMap["twitter:title"]
            ?: jsonLd.headline
            ?: htmlTitle
            ?: hostFallback

        val visibleFallback = extractReadableTextSnippet(html, 260)
        val ogDescription = metaMap["og:description"]
            ?: metaMap["twitter:description"]
            ?: metaMap["description"]
            ?: jsonLd.description
            ?: visibleFallback
            ?: ""

        val ogImageRaw = metaMap["og:image:secure_url"]
            ?: metaMap["og:image"]
            ?: metaMap["twitter:image"]
            ?: metaMap["twitter:image:src"]
            ?: jsonLd.imageUrl

        val author = metaMap["author"]
            ?: metaMap["article:author"]
            ?: metaMap["twitter:creator"]
            ?: jsonLd.author

        val ogSiteName = metaMap["og:site_name"]
        val ogType = metaMap["og:type"]
        val canonicalUrl = resolveAbsoluteUrl(resolvedUrl, metaMap["og:url"] ?: canonicalHref) ?: resolvedUrl
        val resolvedImageUrl = resolveAbsoluteUrl(resolvedUrl, ogImageRaw)
        val resolvedFaviconUrl = resolveAbsoluteUrl(resolvedUrl, faviconRaw)
            ?: resolveAbsoluteUrl(resolvedUrl, "/favicon.ico")

        val platform = detectPlatform(canonicalUrl)
        val extractedHashtags = extractHashtags("$ogTitle $ogDescription ${metaMap["keywords"].orEmpty()}")
        val now = System.currentTimeMillis()

        val metadata = OpenGraphMetadata(
            requestedUrl = requestedUrl,
            resolvedUrl = canonicalUrl,
            title = ogTitle.trim(),
            description = ogDescription.trim(),
            imageUrl = resolvedImageUrl,
            siteName = ogSiteName?.trim(),
            ogType = ogType?.trim(),
            faviconUrl = resolvedFaviconUrl,
            platform = platform,
            extractedTagsCount = metaMap.size + if (htmlTitle != null) 1 else 0,
            author = author?.trim(),
            hashtags = extractedHashtags,
            scrapedAt = now
        )

        persistMetadataToRoom(metadata, html)
        metadata
    }

    private suspend fun persistMetadataToRoom(metadata: OpenGraphMetadata, rawHtml: String) {
        val snippet = buildString {
            if (metadata.description.isNotBlank()) {
                append(metadata.description)
            } else if (!metadata.siteName.isNullOrBlank()) {
                append("المصدر: ${metadata.siteName}")
            }
        }

        val mediaJson = if (!metadata.imageUrl.isNullOrBlank()) {
            JSONArray().put(metadata.imageUrl).toString()
        } else {
            "[]"
        }
        val contentSummary = buildString {
            append(metadata.title)
            if (metadata.description.isNotBlank()) {
                append("\n")
                append(metadata.description)
            }
        }
        val tags = (listOfNotNull(
            "OpenGraph",
            metadata.ogType,
            metadata.siteName
        ) + metadata.hashtags.take(5)).distinct().joinToString(",")

        val authorDisplay = metadata.author
            ?: metadata.siteName
            ?: metadata.title.take(40)
        val authorHandle = metadata.author
            ?.trim()
            ?.let { if (it.startsWith("@")) it else "@${it.lowercase().replace(" ", "_")}" }
            ?: metadata.siteName
            ?: metadata.platform.lowercase()

        val postEntity = ScrapedPostMetadataEntity(
            id = "og-${UUID.randomUUID()}",
            platform = metadata.platform,
            originalPostUrl = metadata.resolvedUrl,
            authorHandle = authorHandle,
            authorDisplayName = authorDisplay,
            authorAvatarUrl = metadata.faviconUrl,
            postContent = contentSummary,
            mediaUrlsJson = mediaJson,
            scrapedTimestamp = metadata.scrapedAt,
            tagsCsv = tags
        )

        val browserDataEntity = BrowserDataEntity(
            id = "bdata-${UUID.randomUUID()}",
            title = metadata.title,
            url = metadata.resolvedUrl,
            htmlContent = rawHtml.take(25_000),
            textContent = contentSummary,
            timestamp = metadata.scrapedAt
        )

        val cachedEntity = com.example.database.entity.CachedMetadataEntity(
            requestedUrl = metadata.requestedUrl,
            resolvedUrl = metadata.resolvedUrl,
            title = metadata.title,
            description = metadata.description,
            ogImageUrl = metadata.imageUrl,
            siteName = metadata.siteName,
            ogType = metadata.ogType,
            faviconUrl = metadata.faviconUrl,
            platform = metadata.platform,
            author = metadata.author,
            hashtagsCsv = metadata.hashtags.joinToString(","),
            extractedTagsCount = metadata.extractedTagsCount,
            scrapedAt = metadata.scrapedAt
        )

        repository.persistOpenGraphBundle(
            url = metadata.resolvedUrl,
            title = metadata.title,
            platform = metadata.platform,
            snippet = snippet,
            faviconUrl = metadata.faviconUrl,
            ogImageUrl = metadata.imageUrl,
            siteName = metadata.siteName,
            postEntity = postEntity,
            browserDataEntity = browserDataEntity,
            cachedMetadataEntity = cachedEntity
        )
    }

    internal fun validateAndNormalizeUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.contains(" ")) return null

        val lower = trimmed.lowercase()
        if (lower.startsWith("javascript:") || lower.startsWith("file:") ||
            lower.startsWith("content:") || lower.startsWith("data:")
        ) {
            return null
        }

        val candidate = if (lower.startsWith("http://") || lower.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }

        return try {
            val uri = URI(candidate)
            val scheme = uri.scheme?.lowercase()
            val host = uri.host?.lowercase() ?: return null
            if (scheme != "http" && scheme != "https") return null
            if (isUnsafeLocalOrPrivateHost(host)) return null
            candidate
        } catch (_: Exception) {
            null
        }
    }

    private fun isUnsafeLocalOrPrivateHost(host: String): Boolean {
        if (host == "localhost" || host.endsWith(".local") || host == "0.0.0.0") return true
        if (host == "127.0.0.1" || host == "::1") return true
        // Fast literal IP check without triggering blocking DNS lookup on malformed names
        val ipv4Regex = Regex("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$")
        val match = ipv4Regex.matchEntire(host) ?: return false
        val octets = match.groupValues.drop(1).mapNotNull { it.toIntOrNull() }
        if (octets.size != 4 || octets.any { it !in 0..255 }) return true
        val (a, b) = octets[0] to octets[1]
        return a == 10 || a == 127 || a == 0 ||
            (a == 169 && b == 254) ||
            (a == 172 && b in 16..31) ||
            (a == 192 && b == 168)
    }

    private fun readBodySafely(body: ResponseBody?, maxBytes: Int): String {
        if (body == null) return ""
        val buffer = ByteArrayOutputStream()
        body.byteStream().use { input ->
            val chunk = ByteArray(8192)
            var total = 0
            while (total < maxBytes) {
                val read = input.read(chunk, 0, minOf(chunk.size, maxBytes - total))
                if (read <= 0) break
                buffer.write(chunk, 0, read)
                total += read
            }
        }
        return buffer.toString(StandardCharsets.UTF_8.name())
    }

    private data class JsonLdInfo(
        val headline: String? = null,
        val description: String? = null,
        val author: String? = null,
        val imageUrl: String? = null
    )

    private fun extractJsonLdInfo(html: String): JsonLdInfo {
        val scriptRegex = Regex(
            "<script[^>]*type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        for (match in scriptRegex.findAll(html)) {
            val rawJson = match.groupValues.getOrNull(1)?.trim() ?: continue
            if (rawJson.isBlank()) continue
            try {
                val obj = if (rawJson.startsWith("[")) {
                    JSONArray(rawJson).optJSONObject(0)
                } else {
                    JSONObject(rawJson)
                } ?: continue

                val headline = obj.optString("headline").ifBlank { obj.optString("name") }.ifBlank { null }
                val description = obj.optString("description").ifBlank { null }
                val authorObj = obj.opt("author")
                val authorName = when (authorObj) {
                    is JSONObject -> authorObj.optString("name").ifBlank { null }
                    is JSONArray -> authorObj.optJSONObject(0)?.optString("name")?.ifBlank { null }
                    is String -> authorObj.ifBlank { null }
                    else -> null
                }
                val imageField = obj.opt("image")
                val imageUrl = when (imageField) {
                    is String -> imageField.ifBlank { null }
                    is JSONObject -> imageField.optString("url").ifBlank { null }
                    is JSONArray -> imageField.optString(0).ifBlank { null }
                    else -> null
                }
                if (headline != null || description != null || imageUrl != null) {
                    return JsonLdInfo(
                        headline = headline?.let { decodeHtmlEntities(it) },
                        description = description?.let { decodeHtmlEntities(it) },
                        author = authorName?.let { decodeHtmlEntities(it) },
                        imageUrl = imageUrl
                    )
                }
            } catch (_: Exception) {
                // Skip invalid JSON-LD blocks
            }
        }
        return JsonLdInfo()
    }

    private fun extractMetaTags(html: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        val metaTagRegex = Regex("<meta\\s+([^>]*?)>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val attrRegex = Regex("([a-zA-Z0-9_:-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"'=<>`]+))")

        for (match in metaTagRegex.findAll(html)) {
            val attrsText = match.groupValues[1]
            val attrs = mutableMapOf<String, String>()
            for (attrMatch in attrRegex.findAll(attrsText)) {
                val key = attrMatch.groupValues[1].lowercase()
                val value = attrMatch.groupValues[2].ifEmpty {
                    attrMatch.groupValues[3].ifEmpty { attrMatch.groupValues[4] }
                }
                attrs[key] = decodeHtmlEntities(value)
            }

            val metaKey = (attrs["property"] ?: attrs["name"] ?: attrs["itemprop"])?.lowercase()?.trim()
            val content = attrs["content"]?.trim()
            if (!metaKey.isNullOrBlank() && !content.isNullOrBlank() && !result.containsKey(metaKey)) {
                result[metaKey] = content
            }
        }
        return result
    }

    private fun extractHtmlTitle(html: String): String? {
        val titleRegex = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val raw = titleRegex.find(html)?.groupValues?.getOrNull(1)?.trim() ?: return null
        val cleaned = decodeHtmlEntities(raw).replace(Regex("\\s+"), " ").trim()
        return cleaned.ifBlank { null }
    }

    private fun extractCanonicalUrl(html: String): String? {
        val linkRegex = Regex("<link\\s+([^>]*?)>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val attrRegex = Regex("([a-zA-Z0-9_:-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')")
        for (match in linkRegex.findAll(html)) {
            val attrs = mutableMapOf<String, String>()
            for (attrMatch in attrRegex.findAll(match.groupValues[1])) {
                val key = attrMatch.groupValues[1].lowercase()
                val value = attrMatch.groupValues[2].ifEmpty { attrMatch.groupValues[3] }
                attrs[key] = value
            }
            if (attrs["rel"]?.lowercase()?.contains("canonical") == true) {
                return attrs["href"]?.trim()?.takeIf { it.isNotBlank() }
            }
        }
        return null
    }

    private fun extractFaviconUrl(html: String): String? {
        val linkRegex = Regex("<link\\s+([^>]*?)>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val attrRegex = Regex("([a-zA-Z0-9_:-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')")

        for (match in linkRegex.findAll(html)) {
            val attrsText = match.groupValues[1]
            val attrs = mutableMapOf<String, String>()
            for (attrMatch in attrRegex.findAll(attrsText)) {
                val key = attrMatch.groupValues[1].lowercase()
                val value = attrMatch.groupValues[2].ifEmpty { attrMatch.groupValues[3] }
                attrs[key] = value
            }
            val rel = attrs["rel"]?.lowercase().orEmpty()
            val href = attrs["href"]?.trim()
            if ((rel.contains("icon") || rel.contains("apple-touch-icon")) && !href.isNullOrBlank()) {
                return decodeHtmlEntities(href)
            }
        }
        return null
    }

    private fun extractReadableTextSnippet(html: String, maxLength: Int): String? {
        val stripped = html
            .replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("<noscript[^>]*>.*?</noscript>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("<svg[^>]*>.*?</svg>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("<[^>]+>"), " ")
        val cleaned = decodeHtmlEntities(stripped).replace(Regex("\\s+"), " ").trim()
        return if (cleaned.length > 20) cleaned.take(maxLength) else null
    }

    private fun extractHashtags(text: String): List<String> {
        val regex = Regex("#[\\p{L}\\p{N}_]{2,32}")
        return regex.findAll(text).map { it.value }.distinct().take(8).toList()
    }

    private fun resolveAbsoluteUrl(baseUrl: String, relativeOrAbsolute: String?): String? {
        val target = relativeOrAbsolute?.trim()
        if (target.isNullOrBlank()) return null
        return try {
            URI(baseUrl).resolve(target).toString()
        } catch (_: Exception) {
            if (target.startsWith("http://") || target.startsWith("https://")) target else null
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

    private fun decodeHtmlEntities(input: String): String {
        var text = input
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")

        val hexRegex = Regex("&#x([0-9a-fA-F]+);")
        text = hexRegex.replace(text) { m ->
            val code = m.groupValues[1].toIntOrNull(16)
            if (code != null) String(Character.toChars(code)) else m.value
        }

        val decRegex = Regex("&#([0-9]+);")
        text = decRegex.replace(text) { m ->
            val code = m.groupValues[1].toIntOrNull()
            if (code != null) String(Character.toChars(code)) else m.value
        }
        return text
    }

    companion object {
        private const val MAX_HTML_BYTES = 1_500_000

        fun defaultOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}
