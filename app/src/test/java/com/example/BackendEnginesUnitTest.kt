package com.example

import com.example.ai.GeminiProvider
import com.example.bridge.AppBridgeContract
import com.example.browser.DomSnapshot
import com.example.browser.HumanizedInteractionEngine
import com.example.scraper.toOpenGraphMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendEnginesUnitTest {

    @Test
    fun `gemini context aware analyzer extracts hashtags and mentions from dom snapshot`() {
        val provider = GeminiProvider()
        val snapshot = DomSnapshot(
            url = "https://x.com/explore",
            title = "Explore X",
            ogTitle = "AI Trends on X",
            ogDescription = "Latest updates in AI Agents",
            headings = listOf("Trending Now", "AI Breakthroughs"),
            textExcerpt = "Big launch today #AI #AndroidAgents with @OpenAI and @AndroidDev",
            linksCount = 24,
            interactiveElementsCount = 41,
            hashtags = listOf("#AI", "#AndroidAgents"),
            mentions = listOf("@OpenAI", "@AndroidDev"),
            postSnippets = listOf("Post 1: Exploring autonomous browser agents on Android.")
        )

        val hashtagResponse = provider.generateContextAwareLocalResponse(
            pageTitle = "Explore X",
            pageUrl = "https://x.com/explore",
            userPrompt = "استخرج الهاشتاجات الرائجة",
            contextInfo = "",
            domSnapshot = snapshot
        )
        assertTrue(hashtagResponse.isSuccess)
        assertFalse(hashtagResponse.usedLiveCloudAi)
        assertTrue(hashtagResponse.text.contains("#AI"))
        assertTrue(hashtagResponse.text.contains("#AndroidAgents"))
        assertTrue(hashtagResponse.text.contains("41"))

        val mentionsResponse = provider.generateContextAwareLocalResponse(
            pageTitle = "Explore X",
            pageUrl = "https://x.com/explore",
            userPrompt = "رصد الحسابات المذكورة",
            contextInfo = "",
            domSnapshot = snapshot
        )
        assertTrue(mentionsResponse.text.contains("@OpenAI"))
        assertTrue(mentionsResponse.text.contains("@AndroidDev"))
    }

    @Test
    fun `app bridge contract resolves short ids, packages, and full pairing tokens`() {
        assertEquals(
            "com.example.companion.bridge.provider",
            AppBridgeContract.resolveAuthorityFromInput("")
        )
        assertEquals(
            "com.custom.partner.bridge.provider",
            AppBridgeContract.resolveAuthorityFromInput("BRIDGE-ID:CP-88#com.custom.partner.bridge.provider")
        )
        assertEquals(
            "com.acme.crm.bridge.provider",
            AppBridgeContract.resolveAuthorityFromInput("com.acme.crm")
        )
        assertEquals(
            "com.example.companion.bridge.provider",
            AppBridgeContract.resolveAuthorityFromInput("CRM-1234")
        )
    }

    @Test
    fun `humanized interaction engine builds stealth, scroll, click, and dom snapshot scripts safely`() {
        val engine = HumanizedInteractionEngine()

        val stealth = engine.buildAntiBotStealthScript()
        assertTrue(stealth.contains("webdriver"))
        assertTrue(stealth.contains("maxTouchPoints"))

        val domScript = engine.buildDomSnapshotScript()
        assertTrue(domScript.contains("og:title"))
        assertTrue(domScript.contains("interactiveElementsCount"))
        assertTrue(domScript.contains("postSnippets"))

        val clickScript = engine.buildHumanClickScript("button[data-id='submit']")
        assertTrue(clickScript.contains("\\'submit\\'"))
        assertNotNull(engine.buildHumanScrollScript(500))
    }

    @Test
    fun `url safety guard rejects unsafe schemes and loopback hosts`() {
        assertNull(validateTestUrl("javascript:alert(1)"))
        assertNull(validateTestUrl("file:///etc/passwd"))
        assertNull(validateTestUrl("http://127.0.0.1/admin"))
        assertNull(validateTestUrl("https://localhost:8080/api"))
        assertNull(validateTestUrl("http://192.168.1.1/router"))
        assertEquals("https://x.com/explore", validateTestUrl("x.com/explore"))
        assertEquals("https://linkedin.com/feed", validateTestUrl("https://linkedin.com/feed"))
    }

    @Test
    fun `cached metadata entity converts cleanly to OpenGraphMetadata with fromCache flag`() {
        val entity = com.example.database.entity.CachedMetadataEntity(
            id = "cache-1",
            requestedUrl = "https://github.com",
            resolvedUrl = "https://github.com/",
            title = "GitHub: Let's build from here",
            description = "GitHub is where over 100 million developers shape the future of software.",
            ogImageUrl = "https://github.githubassets.com/images/modules/site/social-cards/campaign-social.png",
            siteName = "GitHub",
            ogType = "object",
            platform = "WEB",
            hashtagsCsv = "#dev,#opensource",
            extractedTagsCount = 12,
            scrapedAt = 1732480000000L
        )

        val og = entity.toOpenGraphMetadata()
        assertTrue(og.isFromCache)
        assertEquals("GitHub: Let's build from here", og.title)
        assertEquals("https://github.githubassets.com/images/modules/site/social-cards/campaign-social.png", og.imageUrl)
        assertEquals(2, og.hashtags.size)
        assertEquals("#dev", og.hashtags[0])
    }

    private fun validateTestUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.contains(" ")) return null
        val lower = trimmed.lowercase()
        if (lower.startsWith("javascript:") || lower.startsWith("file:") ||
            lower.startsWith("content:") || lower.startsWith("data:")
        ) {
            return null
        }
        val candidate = if (lower.startsWith("http://") || lower.startsWith("https://")) trimmed else "https://$trimmed"
        val uri = java.net.URI(candidate)
        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null
        if (host == "localhost" || host == "127.0.0.1" || host.startsWith("192.168.")) return null
        return candidate
    }
}
