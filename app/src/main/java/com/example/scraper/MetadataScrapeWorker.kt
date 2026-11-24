package com.example.scraper

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.SocialAgentApplication
import com.example.backend.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * WorkManager CoroutineWorker that executes network metadata scraping off the main thread
 * for user-provided URLs and caches results (titles, og:images, descriptions) in Room,
 * ensuring the UI stays responsive during network requests.
 */
class MetadataScrapeWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val urls = inputData.getStringArray(KEY_URLS_ARRAY)
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            .orEmpty()

        if (urls.isEmpty()) {
            return@withContext Result.failure(
                workDataOf(KEY_LAST_ERROR to "لم يتم تزويد أي روابط صالحة للاستخراج")
            )
        }

        val forceRefresh = inputData.getBoolean(KEY_FORCE_REFRESH, false)
        val container = (applicationContext as? SocialAgentApplication)?.appContainer
            ?: AppContainer(applicationContext as android.app.Application)
        val scraper = container.openGraphScraper

        val total = urls.size
        val completedCounter = AtomicInteger(0)
        val succeededCounter = AtomicInteger(0)
        val cacheHitCounter = AtomicInteger(0)
        val networkFailedCounter = AtomicInteger(0)
        var lastTitle = ""
        var lastResolvedUrl = ""
        var lastError = ""

        setProgress(
            workDataOf(
                KEY_PROGRESS_TOTAL to total,
                KEY_PROGRESS_COMPLETED to 0,
                KEY_CURRENT_URL to urls.first()
            )
        )

        val semaphore = Semaphore(3)
        coroutineScope {
            urls.map { url ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        val outcome = scraper.scrapeOrGetCached(url, forceRefresh = forceRefresh)
                        outcome.onSuccess { meta ->
                            succeededCounter.incrementAndGet()
                            if (meta.isFromCache) {
                                cacheHitCounter.incrementAndGet()
                            }
                            lastTitle = meta.title
                            lastResolvedUrl = meta.resolvedUrl
                        }.onFailure { err ->
                            Log.w(TAG, "Failed scraping $url: ${err.message}")
                            lastError = err.localizedMessage ?: err.message ?: "تعذر جلب الرابط"
                            if (err is IOException) {
                                networkFailedCounter.incrementAndGet()
                            }
                        }

                        val done = completedCounter.incrementAndGet()
                        setProgress(
                            workDataOf(
                                KEY_PROGRESS_TOTAL to total,
                                KEY_PROGRESS_COMPLETED to done,
                                KEY_CURRENT_URL to url
                            )
                        )
                    }
                }
            }.awaitAll()
        }

        val succeeded = succeededCounter.get()
        val failed = total - succeeded

        if (succeeded == 0 && networkFailedCounter.get() > 0 && runAttemptCount < 2) {
            return@withContext Result.retry()
        }

        val output = workDataOf(
            KEY_SUCCEEDED_COUNT to succeeded,
            KEY_FAILED_COUNT to failed,
            KEY_CACHED_COUNT to cacheHitCounter.get(),
            KEY_LAST_TITLE to lastTitle,
            KEY_LAST_URL to lastResolvedUrl,
            KEY_LAST_ERROR to lastError
        )

        if (succeeded > 0) Result.success(output) else Result.failure(output)
    }

    companion object {
        const val TAG = "MetadataScrapeWorker"
        const val TAG_METADATA_SCRAPE = "tag_metadata_scrape"

        const val KEY_URLS_ARRAY = "urls_array"
        const val KEY_FORCE_REFRESH = "force_refresh"

        const val KEY_PROGRESS_TOTAL = "progress_total"
        const val KEY_PROGRESS_COMPLETED = "progress_completed"
        const val KEY_CURRENT_URL = "current_url"

        const val KEY_SUCCEEDED_COUNT = "succeeded_count"
        const val KEY_FAILED_COUNT = "failed_count"
        const val KEY_CACHED_COUNT = "cached_count"
        const val KEY_LAST_TITLE = "last_title"
        const val KEY_LAST_URL = "last_url"
        const val KEY_LAST_ERROR = "last_error"

        fun enqueueScrape(
            context: Context,
            urls: List<String>,
            forceRefresh: Boolean = true
        ): UUID {
            val sanitized = urls.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            val input = Data.Builder()
                .putStringArray(KEY_URLS_ARRAY, sanitized.toTypedArray())
                .putBoolean(KEY_FORCE_REFRESH, forceRefresh)
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (forceRefresh) NetworkType.CONNECTED else NetworkType.NOT_REQUIRED)
                .build()

            val request = OneTimeWorkRequestBuilder<MetadataScrapeWorker>()
                .setInputData(input)
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .addTag(TAG_METADATA_SCRAPE)
                .build()

            val uniqueKey = "scrape_${sanitized.joinToString("|").hashCode()}"
            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueKey,
                ExistingWorkPolicy.REPLACE,
                request
            )
            return request.id
        }

        fun observeWorkInfos(context: Context): Flow<List<WorkInfo>> {
            return WorkManager.getInstance(context).getWorkInfosByTagFlow(TAG_METADATA_SCRAPE)
        }
    }
}
