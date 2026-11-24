package com.example

import com.example.browser.BrowserTabState
import com.example.browser.TabAnalysisStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserPageTaskTest {

    @Test
    fun testBrowserTabSpecificTaskDefinition() {
        val tab = BrowserTabState(
            id = "tab-x-123",
            title = "منصة إكس (Twitter)",
            url = "https://x.com/explore",
            analysisStatus = TabAnalysisStatus.READY_FOR_PROMPT,
            userPrompt = "استخرج أحدث 10 تغريدات تقنية وحلل الردود"
        )

        assertEquals("tab-x-123", tab.id)
        assertEquals("استخرج أحدث 10 تغريدات تقنية وحلل الردود", tab.userPrompt)
        assertEquals(TabAnalysisStatus.READY_FOR_PROMPT, tab.analysisStatus)
    }

    @Test
    fun testPerTabTasksMapping() {
        val taskMap = mutableMapOf<String, String>()

        taskMap["tab-1"] = "استخرج الروابط والمنشورات من صفحة إكس"
        taskMap["tab-2"] = "لخص المقالات التقنية من صفحة ريديت"
        taskMap["tab-3"] = "اجمع الحسابات المستهدفة من لينكد إن"

        assertEquals(3, taskMap.size)
        assertEquals("استخرج الروابط والمنشورات من صفحة إكس", taskMap["tab-1"])
        assertTrue(taskMap.containsKey("tab-2"))
        assertEquals("لخص المقالات التقنية من صفحة ريديت", taskMap["tab-2"])
    }
}
