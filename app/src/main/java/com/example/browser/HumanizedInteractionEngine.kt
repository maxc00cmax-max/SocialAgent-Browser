package com.example.browser

import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Random
import kotlin.math.pow
import kotlin.math.roundToInt

data class HumanizedTypingConfig(
    val baseKeystrokeDelayMs: Long = 65L,
    val jitterRangeMs: Long = 35L,
    val wordPauseDelayMs: Long = 180L,
    val simulateHumanTypos: Boolean = false,
    val humanClickHoldMs: Long = 85L
)

class HumanizedInteractionEngine(
    var config: HumanizedTypingConfig = HumanizedTypingConfig()
) {
    private val random = Random()

    /**
     * Injects humanized typing script that mimics natural human keyboard entry:
     * dispatches KeyboardEvent sequences with natural velocity, jitter, and focus handling.
     */
    fun buildHumanTypingScript(selector: String, text: String): String {
        val escapedText = JSONObject.quote(text)
        val baseDelay = config.baseKeystrokeDelayMs
        val jitter = config.jitterRangeMs
        val wordPause = config.wordPauseDelayMs

        return """
            (async function() {
                try {
                    let el = document.querySelector('$selector');
                    if (!el) {
                        return { success: false, reason: 'Element not found' };
                    }
                    el.focus();
                    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    await new Promise(r => setTimeout(r, 120));

                    // Clear previous content if needed
                    if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
                        el.value = '';
                    } else if (el.isContentEditable) {
                        el.innerText = '';
                    }

                    const textToType = $escapedText;
                    for (let i = 0; i < textToType.length; i++) {
                        const char = textToType[i];
                        
                        // Dispatch keydown
                        el.dispatchEvent(new KeyboardEvent('keydown', { key: char, bubbles: true }));
                        
                        if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
                            el.value += char;
                        } else if (el.isContentEditable) {
                            el.innerText += char;
                        }

                        // Dispatch input & keyup
                        el.dispatchEvent(new Event('input', { bubbles: true }));
                        el.dispatchEvent(new KeyboardEvent('keyup', { key: char, bubbles: true }));

                        // Human delay calculation
                        let delay = $baseDelay + (Math.random() * $jitter * 2 - $jitter);
                        if (char === ' ' || char === '.' || char === '،' || char === ',') {
                            delay += $wordPause;
                        }
                        await new Promise(r => setTimeout(r, Math.max(25, delay)));
                    }

                    // Dispatch change event
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
        val absDelta = Math.abs(deltaY)

        return """
            (async function() {
                try {
                    const totalDistance = $absDelta * $direction;
                    const duration = Math.min(800, Math.max(300, Math.abs(totalDistance) * 0.8));
                    const startTime = performance.now();
                    const startY = window.scrollY;

                    // Ease-Out Cubic physics
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

                    // Natural reading pause
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
        return """
            (async function() {
                try {
                    let el = document.querySelector('$selector');
                    if (!el) return { success: false, reason: 'Element not found' };

                    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    await new Promise(r => setTimeout(r, 180));

                    const rect = el.getBoundingClientRect();
                    // Gaussian touch spread (avoid clicking exact center)
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

                    return { success: true, clickedSelector: '$selector' };
                } catch(e) {
                    return { success: false, reason: e.message };
                }
            })();
        """.trimIndent()
    }

    /**
     * Injects anti-bot stealth & modern mobile identity (iPhone Safari touch events).
     */
    fun buildAntiBotStealthScript(): String {
        return """
            (function() {
                try {
                    // Conceal navigator webdriver
                    Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
                    
                    // Emulate iPhone touch support
                    window.ontouchstart = window.ontouchstart || function() {};
                    navigator.maxTouchPoints = 5;

                    // Spoof modern mobile languages
                    Object.defineProperty(navigator, 'languages', { get: () => ['ar', 'en-US', 'en'] });
                } catch(e) {}
            })();
        """.trimIndent()
    }
}
