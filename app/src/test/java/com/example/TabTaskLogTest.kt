package com.example

import com.example.database.entity.TabTaskLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class TabTaskLogTest {

    @Test
    fun testTabTaskLogEntityCreationAndStatus() {
        val id = UUID.randomUUID().toString()
        val log = TabTaskLogEntity(
            id = id,
            tabId = "tab-x-twitter",
            tabTitle = "منصة إكس (Twitter)",
            tabUrl = "https://x.com/explore",
            taskPrompt = "استخرج الهاشتاجات الرائجة وأهم المنشورات",
            status = "COMPLETED",
            outputResult = "تم استخراج 10 تغريدات و3 وسوم رائجة.",
            timestamp = System.currentTimeMillis(),
            durationMs = 2150L,
            tokensUsed = 540
        )

        assertNotNull(log)
        assertEquals("tab-x-twitter", log.tabId)
        assertEquals("COMPLETED", log.status)
        assertEquals(540, log.tokensUsed)
        assertTrue(log.outputResult.contains("10 تغريدات"))
    }

    @Test
    fun testFilterLogsByTabId() {
        val logs = listOf(
            TabTaskLogEntity(
                id = "1",
                tabId = "tab-twitter",
                tabTitle = "Twitter",
                tabUrl = "https://x.com",
                taskPrompt = "Prompt 1",
                status = "COMPLETED"
            ),
            TabTaskLogEntity(
                id = "2",
                tabId = "tab-linkedin",
                tabTitle = "LinkedIn",
                tabUrl = "https://linkedin.com",
                taskPrompt = "Prompt 2",
                status = "COMPLETED"
            ),
            TabTaskLogEntity(
                id = "3",
                tabId = "tab-twitter",
                tabTitle = "Twitter",
                tabUrl = "https://x.com",
                taskPrompt = "Prompt 3",
                status = "FAILED"
            )
        )

        val twitterLogs = logs.filter { it.tabId == "tab-twitter" }
        assertEquals(2, twitterLogs.size)

        val linkedInLogs = logs.filter { it.tabId == "tab-linkedin" }
        assertEquals(1, linkedInLogs.size)

        val completedLogs = logs.filter { it.status == "COMPLETED" }
        assertEquals(2, completedLogs.size)
    }
}
