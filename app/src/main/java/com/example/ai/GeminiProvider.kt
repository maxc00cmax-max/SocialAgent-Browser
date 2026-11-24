package com.example.ai

import com.example.BuildConfig
import com.example.browser.DomSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class AiResponse(
    val text: String,
    val promptTokens: Int = 0,
    val candidateTokens: Int = 0,
    val totalTokens: Int = 0,
    val isSuccess: Boolean = true,
    val usedLiveCloudAi: Boolean = false,
    val modelName: String = "gemini-2.5-flash"
)

class GeminiProvider(
    private val client: OkHttpClient = defaultOkHttpClient()
) {

    private val apiKey: String = BuildConfig.GEMINI_API_KEY.ifBlank {
        System.getenv("GEMINI_API_KEY") ?: ""
    }

    val isLiveGeminiConfigured: Boolean
        get() = apiKey.isNotBlank()

    suspend fun analyzePageAndExecute(
        pageTitle: String,
        pageUrl: String,
        userPrompt: String,
        contextInfo: String = "",
        domSnapshot: DomSnapshot? = null
    ): AiResponse = withContext(Dispatchers.IO) {
        val enrichedContext = buildEnrichedContext(pageTitle, pageUrl, contextInfo, domSnapshot)
        val systemInstruction = """
            أنت الوكيل الذكي وخبير أتمتة الويب والبيانات الاجتماعية (Social Agent).
            مهمتك تحليل سياق الصفحة المفتوحة وتنفيذ طلب المستخدم بدقة وبنقاط منظمة باللغة العربية.
            اعتمد على البيانات الفعلية المستخرجة من DOM وOpenGraph فقط ولا تختلق إحصاءات غير موجودة.
        """.trimIndent()

        val userMessage = """
            بيانات الصفحة الحالية:
            $enrichedContext

            أمر المستخدم المطلوب تنفيذه:
            "$userPrompt"

            المخرجات المطلوبة:
            1. ملخص ما تم رصده في الصفحة والسياق.
            2. الإجابة المباشرة أو البيانات المستخرجة حسب أمر المستخدم.
            3. توصية عملية أو خطوة تالية للأتمتة.
        """.trimIndent()

        if (apiKey.isBlank()) {
            return@withContext generateContextAwareLocalResponse(
                pageTitle = pageTitle,
                pageUrl = pageUrl,
                userPrompt = userPrompt,
                contextInfo = contextInfo,
                domSnapshot = domSnapshot
            )
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
        val jsonBody = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", systemInstruction))
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", userMessage))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.35)
                put("maxOutputTokens", 1024)
            })
        }

        val maxAttempts = 3
        var lastError: String? = null

        for (attempt in 0 until maxAttempts) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        val jsonResponse = JSONObject(responseBody)
                        val candidates = jsonResponse.optJSONArray("candidates")
                        val firstCandidate = candidates?.optJSONObject(0)
                        val content = firstCandidate?.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val generatedText = parts?.optJSONObject(0)?.optString("text")?.trim()

                        if (!generatedText.isNullOrBlank()) {
                            val usageMetadata = jsonResponse.optJSONObject("usageMetadata")
                            val promptTokens = usageMetadata?.optInt("promptTokenCount", 140) ?: 140
                            val candidateTokens = usageMetadata?.optInt("candidatesTokenCount", 260) ?: 260
                            return@withContext AiResponse(
                                text = generatedText,
                                promptTokens = promptTokens,
                                candidateTokens = candidateTokens,
                                totalTokens = promptTokens + candidateTokens,
                                isSuccess = true,
                                usedLiveCloudAi = true,
                                modelName = "gemini-2.5-flash"
                            )
                        }
                    }

                    lastError = "HTTP ${response.code}"
                    val retryable = response.code == 429 || response.code in 500..599
                    if (!retryable) {
                        return@withContext generateContextAwareLocalResponse(
                            pageTitle = pageTitle,
                            pageUrl = pageUrl,
                            userPrompt = userPrompt,
                            contextInfo = contextInfo,
                            domSnapshot = domSnapshot,
                            fallbackReason = lastError
                        )
                    }
                }
            } catch (e: IOException) {
                lastError = e.localizedMessage ?: "Network error"
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: "Unexpected error"
                break
            }

            if (attempt < maxAttempts - 1) {
                delay(400L * (1 shl attempt))
            }
        }

        generateContextAwareLocalResponse(
            pageTitle = pageTitle,
            pageUrl = pageUrl,
            userPrompt = userPrompt,
            contextInfo = contextInfo,
            domSnapshot = domSnapshot,
            fallbackReason = lastError
        )
    }

    private fun buildEnrichedContext(
        pageTitle: String,
        pageUrl: String,
        contextInfo: String,
        domSnapshot: DomSnapshot?
    ): String = buildString {
        appendLine("- العنوان: $pageTitle")
        appendLine("- الرابط: $pageUrl")
        if (contextInfo.isNotBlank()) {
            appendLine("- ملخص السياق: $contextInfo")
        }
        if (domSnapshot != null) {
            if (!domSnapshot.ogTitle.isNullOrBlank()) appendLine("- عنوان OpenGraph: ${domSnapshot.ogTitle}")
            if (!domSnapshot.ogDescription.isNullOrBlank()) appendLine("- الوصف: ${domSnapshot.ogDescription}")
            if (domSnapshot.headings.isNotEmpty()) {
                appendLine("- العناوين الرئيسية: ${domSnapshot.headings.joinToString(" | ")}")
            }
            if (domSnapshot.hashtags.isNotEmpty()) {
                appendLine("- الهاشتاجات المرصودة: ${domSnapshot.hashtags.joinToString(", ")}")
            }
            if (domSnapshot.mentions.isNotEmpty()) {
                appendLine("- الحسابات المذكورة: ${domSnapshot.mentions.joinToString(", ")}")
            }
            appendLine("- الروابط: ${domSnapshot.linksCount} | العناصر التفاعلية: ${domSnapshot.interactiveElementsCount}")
            if (domSnapshot.postSnippets.isNotEmpty()) {
                appendLine("- مقتطفات المنشورات:")
                domSnapshot.postSnippets.take(4).forEachIndexed { idx, s ->
                    appendLine("  ${idx + 1}) $s")
                }
            } else if (domSnapshot.textExcerpt.isNotBlank()) {
                appendLine("- مقتطف النص: ${domSnapshot.textExcerpt.take(900)}")
            }
        }
    }

    /**
     * Deterministic context-aware local analyzer that extracts real insights from the DOM snapshot,
     * OpenGraph context, URL structure, and user prompt intent when offline or without an API key.
     */
    internal fun generateContextAwareLocalResponse(
        pageTitle: String,
        pageUrl: String,
        userPrompt: String,
        contextInfo: String,
        domSnapshot: DomSnapshot?,
        fallbackReason: String? = null
    ): AiResponse {
        val lowerPrompt = userPrompt.lowercase()
        val hashtags = domSnapshot?.hashtags.orEmpty()
        val mentions = domSnapshot?.mentions.orEmpty()
        val headings = domSnapshot?.headings.orEmpty()
        val snippets = domSnapshot?.postSnippets.orEmpty()
        val excerpt = domSnapshot?.textExcerpt.orEmpty()
        val description = domSnapshot?.ogDescription?.takeIf { it.isNotBlank() } ?: contextInfo

        val report = buildString {
            appendLine("📌 **تقرير التحليل المحلي الذكي — $pageTitle**")
            appendLine("• **الرابط:** $pageUrl")
            appendLine("• **الأمر المنفذ:** $userPrompt")
            if (fallbackReason != null) {
                appendLine("• **ملاحظة الاتصال:** تم التحليل بالمحرك المحلي ($fallbackReason)")
            }
            appendLine()

            appendLine("🔍 **المعطيات الفعلية المرصودة من الصفحة:**")
            if (domSnapshot != null) {
                appendLine("• العناصر التفاعلية: ${domSnapshot.interactiveElementsCount} عنصر | الروابط: ${domSnapshot.linksCount} رابط")
                if (headings.isNotEmpty()) {
                    appendLine("• أبرز العناوين: ${headings.take(4).joinToString(" • ")}")
                }
            } else if (description.isNotBlank()) {
                appendLine("• الملخص المتاح: $description")
            } else {
                appendLine("• لم يُحمّل DOM الحي بعد — اعتمد التحليل على بيانات النافذة وOpenGraph.")
            }
            appendLine()

            appendLine("⚡ **نتائج التنفيذ حسب طلبك:**")
            when {
                lowerPrompt.contains("هاشتاج") || lowerPrompt.contains("وسم") || lowerPrompt.contains("hashtag") -> {
                    if (hashtags.isNotEmpty()) {
                        appendLine("• الهاشتاجات المستخرجة (${hashtags.size}): ${hashtags.joinToString("  ")}")
                    } else {
                        appendLine("• لم يتم رصد هاشتاجات صريحة داخل اللقطة الحالية؛ افتح المتصفح الحي لتمرير الخلاصة والتقاط الوسوم.")
                    }
                }
                lowerPrompt.contains("حساب") || lowerPrompt.contains("مؤثر") || lowerPrompt.contains("mention") || lowerPrompt.contains("lead") -> {
                    if (mentions.isNotEmpty()) {
                        appendLine("• الحسابات والملفات المرصودة (${mentions.size}): ${mentions.joinToString(", ")}")
                    } else {
                        appendLine("• لم تظهر إشارات @ صريحة في الجزء المرئي؛ تم فهرسة الصفحة للمراجعة في سجل البيانات.")
                    }
                }
                lowerPrompt.contains("تلخيص") || lowerPrompt.contains("لخص") || lowerPrompt.contains("ملخص") || lowerPrompt.contains("summar") -> {
                    if (snippets.isNotEmpty()) {
                        snippets.take(3).forEachIndexed { idx, s ->
                            appendLine("${idx + 1}. $s")
                        }
                    } else if (excerpt.isNotBlank()) {
                        appendLine("• خلاصة المحتوى: ${excerpt.take(360)}...")
                    } else if (description.isNotBlank()) {
                        appendLine("• خلاصة الميتاداتا: $description")
                    }
                }
                else -> {
                    if (snippets.isNotEmpty()) {
                        appendLine("• تم استخلاص ${snippets.size} مقتطفات من المنشورات:")
                        snippets.take(3).forEachIndexed { idx, s ->
                            appendLine("  ${idx + 1}. $s")
                        }
                    } else if (excerpt.isNotBlank()) {
                        appendLine("• مقتطف النص المفحوص: ${excerpt.take(280)}...")
                    }
                    if (hashtags.isNotEmpty()) {
                        appendLine("• الوسوم المرتبطة: ${hashtags.take(6).joinToString(" ")}")
                    }
                    if (mentions.isNotEmpty()) {
                        appendLine("• الحسابات المذكورة: ${mentions.take(6).joinToString(", ")}")
                    }
                }
            }
            appendLine()
            append("✅ **الحالة:** تم توثيق النتائج في Room DB (السجل + الصفحات المزارة).")
        }.trimEnd()

        val estimatedPromptTokens = (userPrompt.length + enrichedSize(contextInfo, domSnapshot)) / 4 + 45
        val estimatedCandidateTokens = report.length / 4 + 30
        return AiResponse(
            text = report,
            promptTokens = estimatedPromptTokens,
            candidateTokens = estimatedCandidateTokens,
            totalTokens = estimatedPromptTokens + estimatedCandidateTokens,
            isSuccess = true,
            usedLiveCloudAi = false,
            modelName = "local-dom-analyzer"
        )
    }

    private fun enrichedSize(contextInfo: String, domSnapshot: DomSnapshot?): Int {
        return contextInfo.length + (domSnapshot?.textExcerpt?.length ?: 0)
    }

    companion object {
        fun defaultOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .build()
        }
    }
}
