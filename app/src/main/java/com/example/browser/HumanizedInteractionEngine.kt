package com.example.browser

import org.json.JSONObject

data class HumanizedTypingConfig(
    val baseKeystrokeDelayMs: Long = 65L,
    val jitterRangeMs: Long = 35L,
    val wordPauseDelayMs: Long = 180L,
    val simulateHumanTypos: Boolean = false,
    val humanClickHoldMs: Long = 85L
)

data class DomSnapshot(
    val url: String,
    val title: String,
    val ogTitle: String? = null,
    val ogDescription: String? = null,
    val ogImageUrl: String? = null,
    val headings: List<String> = emptyList(),
    val textExcerpt: String = "",
    val linksCount: Int = 0,
    val interactiveElementsCount: Int = 0,
    val hashtags: List<String> = emptyList(),
    val mentions: List<String> = emptyList(),
    val postSnippets: List<String> = emptyList(),
    val capturedAt: Long = System.currentTimeMillis()
)

class HumanizedInteractionEngine(
    var config: HumanizedTypingConfig = HumanizedTypingConfig()
) {
    private fun sanitizeCssSelector(selector: String): String {
        return selector
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "")
            .replace("\r", "")
    }

    /**
     * Injects humanized typing script that mimics natural human keyboard entry:
     * dispatches KeyboardEvent sequences with natural velocity, jitter, and focus handling.
     */
    fun buildHumanTypingScript(selector: String, text: String): String {
        val safeSelector = sanitizeCssSelector(selector)
        val escapedText = JSONObject.quote(text)
        val baseDelay = config.baseKeystrokeDelayMs
        val jitter = config.jitterRangeMs
        val wordPause = config.wordPauseDelayMs

        return """
            (async function() {
                try {
                    let el = document.querySelector('$safeSelector');
                    if (!el) {
                        return { success: false, reason: 'Element not found' };
                    }
                    el.focus();
                    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    await new Promise(r => setTimeout(r, 120));

                    if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
                        el.value = '';
                    } else if (el.isContentEditable) {
                        el.innerText = '';
                    }

                    const textToType = $escapedText;
                    for (let i = 0; i < textToType.length; i++) {
                        const char = textToType[i];
                        el.dispatchEvent(new KeyboardEvent('keydown', { key: char, bubbles: true }));

                        if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
                            el.value += char;
                        } else if (el.isContentEditable) {
                            el.innerText += char;
                        }

                        el.dispatchEvent(new Event('input', { bubbles: true }));
                        el.dispatchEvent(new KeyboardEvent('keyup', { key: char, bubbles: true }));

                        let delay = $baseDelay + (Math.random() * $jitter * 2 - $jitter);
                        if (char === ' ' || char === '.' || char === '،' || char === ',') {
                            delay += $wordPause;
                        }
                        await new Promise(r => setTimeout(r, Math.max(25, delay)));
                    }

                    el.dispatchEvent(new Event('change', { bubbles: true }));
                    return { success: true, typedLength: textToType.length };
                } catch(err) {
                    return { success: false, reason: err.message };
                }
            })();
        """.trimIndent()
    }

    /**
     * Builds smooth humanized scrolling script with ease-out cubic deceleration physics.
     */
    fun buildHumanScrollScript(deltaY: Int): String {
        val direction = if (deltaY >= 0) 1 else -1
        val absDelta = kotlin.math.abs(deltaY)

        return """
            (async function() {
                try {
                    const totalDistance = $absDelta * $direction;
                    const duration = Math.min(800, Math.max(300, Math.abs(totalDistance) * 0.8));
                    const startTime = performance.now();
                    const startY = window.scrollY;

                    function easeOutCubic(t) {
                        return (--t) * t * t + 1;
                    }

                    await new Promise(resolve => {
                        function step(currentTime) {
                            const elapsed = currentTime - startTime;
                            const progress = Math.min(elapsed / duration, 1);
                            const easedProgress = easeOutCubic(progress);
                            window.scrollTo(0, startY + (totalDistance * easedProgress));
                            if (progress < 1) {
                                requestAnimationFrame(step);
                            } else {
                                resolve();
                            }
                        }
                        requestAnimationFrame(step);
                    });

                    await new Promise(r => setTimeout(r, 150 + Math.random() * 200));
                    return { success: true, currentScrollY: window.scrollY };
                } catch(e) {
                    return { success: false, reason: e.message };
                }
            })();
        """.trimIndent()
    }

    /**
     * Builds realistic touch/click script with natural Gaussian coordinate jitter.
     */
    fun buildHumanClickScript(selector: String): String {
        val safeSelector = sanitizeCssSelector(selector)
        return """
            (async function() {
                try {
                    let el = document.querySelector('$safeSelector');
                    if (!el) return { success: false, reason: 'Element not found' };

                    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    await new Promise(r => setTimeout(r, 180));

                    const rect = el.getBoundingClientRect();
                    const offsetX = rect.width * (0.35 + Math.random() * 0.3);
                    const offsetY = rect.height * (0.35 + Math.random() * 0.3);
                    const clientX = rect.left + offsetX;
                    const clientY = rect.top + offsetY;

                    const downEvent = new MouseEvent('mousedown', {
                        bubbles: true, cancelable: true, view: window, clientX: clientX, clientY: clientY
                    });
                    const upEvent = new MouseEvent('mouseup', {
                        bubbles: true, cancelable: true, view: window, clientX: clientX, clientY: clientY
                    });

                    el.dispatchEvent(downEvent);
                    await new Promise(r => setTimeout(r, 65 + Math.random() * 40));
                    el.dispatchEvent(upEvent);
                    el.click();

                    return { success: true, clickedSelector: '$safeSelector' };
                } catch(e) {
                    return { success: false, reason: e.message };
                }
            })();
        """.trimIndent()
    }

    /**
     * Injects anti-bot stealth & modern mobile touch capabilities.
     */
    fun buildAntiBotStealthScript(): String {
        return """
            (function() {
                try {
                    Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
                    window.ontouchstart = window.ontouchstart || function() {};
                    if (!navigator.maxTouchPoints) {
                        Object.defineProperty(navigator, 'maxTouchPoints', { get: () => 5 });
                    }
                    Object.defineProperty(navigator, 'languages', { get: () => ['ar', 'en-US', 'en'] });
                } catch(e) {}
            })();
        """.trimIndent()
    }

    /**
     * Single-pass DOM snapshot extractor returning structured JSON with OpenGraph metadata,
     * headings, interactive element count, hashtags, mentions, and post snippets.
     */
    fun buildDomSnapshotScript(): String {
        return """
            (function() {
                try {
                    const getMeta = (keys) => {
                        for (const k of keys) {
                            const el = document.querySelector('meta[property="' + k + '"], meta[name="' + k + '"]');
                            if (el && el.content && el.content.trim()) return el.content.trim();
                        }
                        return null;
                    };

                    const headings = Array.from(document.querySelectorAll('h1, h2, h3'))
                        .map(h => (h.innerText || '').trim())
                        .filter(Boolean)
                        .slice(0, 8);

                    const rawText = ((document.body && document.body.innerText) || '').replace(/\s+/g, ' ').trim();
                    const textExcerpt = rawText.slice(0, 2800);

                    const linksCount = document.querySelectorAll('a[href]').length;
                    const interactiveElementsCount = document.querySelectorAll('a[href], button, input, textarea, [role="button"]').length;

                    const hashtagSet = new Set();
                    const mentionSet = new Set();
                    const hashMatches = textExcerpt.match(/#[\p{L}\p{N}_]{2,40}/gu) || [];
                    hashMatches.slice(0, 15).forEach(h => hashtagSet.add(h));
                    const mentionMatches = textExcerpt.match(/@[A-Za-z0-9_.]{2,40}/g) || [];
                    mentionMatches.slice(0, 15).forEach(m => mentionSet.add(m));

                    const postSnippets = Array.from(document.querySelectorAll('article, [data-testid="tweetText"], .feed-shared-update-v2'))
                        .map(el => (el.innerText || '').replace(/\s+/g, ' ').trim())
                        .filter(t => t.length > 25)
                        .slice(0, 6)
                        .map(t => t.slice(0, 240));

                    return JSON.stringify({
                        url: location.href,
                        title: document.title || '',
                        ogTitle: getMeta(['og:title', 'twitter:title']),
                        ogDescription: getMeta(['og:description', 'twitter:description', 'description']),
                        ogImageUrl: getMeta(['og:image:secure_url', 'og:image', 'twitter:image']),
                        headings: headings,
                        textExcerpt: textExcerpt,
                        linksCount: linksCount,
                        interactiveElementsCount: interactiveElementsCount,
                        hashtags: Array.from(hashtagSet),
                        mentions: Array.from(mentionSet),
                        postSnippets: postSnippets
                    });
                } catch (e) {
                    return JSON.stringify({ error: e.message || 'dom_error' });
                }
            })();
        """.trimIndent()
    }
}
