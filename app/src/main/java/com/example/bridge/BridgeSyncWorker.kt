package com.example.bridge

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * BridgeSyncWorker is a WorkManager CoroutineWorker that observes ContentProvider changes
 * via ContentUriTrigger and synchronizes data into the Room database.
 */
class BridgeSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "BridgeSyncWorker executing ContentUriTrigger event...")
        val bridgeManager = AppBridgeManager.getInstance(applicationContext)

        return try {
            val result = bridgeManager.pullDataFromCompanion()
            val count = result.getOrDefault(0)

            Log.i(TAG, "BridgeSyncWorker synchronized $count elements from companion offline app.")

            // Re-schedule the ContentUriTrigger worker to keep continuous observation alive
            scheduleContentTrigger(applicationContext, bridgeManager.targetAuthority.value)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "BridgeSyncWorker failed to sync companion data", e)
            scheduleContentTrigger(applicationContext, bridgeManager.targetAuthority.value)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "BridgeSyncWorker"
        const val UNIQUE_WORK_NAME = "companion_content_provider_observer"

        /**
         * Enqueues a OneTimeWorkRequest with ContentUriTrigger on the companion ContentProvider URI.
         * Whenever the offline app calls notifyChange() on its ContentProvider, Android OS wakes up
         * this Worker to process the update and sync into Room.
         */
        fun scheduleContentTrigger(context: Context, authority: String) {
            try {
                // Guard: Only schedule ContentUriTrigger if provider actually exists on device
                val providerInfo = try {
                    context.packageManager.resolveContentProvider(authority, 0)
                } catch (e: Exception) {
                    null
                }

                if (providerInfo == null) {
                    Log.d(TAG, "Companion provider ($authority) is not installed on this device yet. Skipping WorkManager trigger.")
                    return
                }

                val targetUri = AppBridgeContract.buildUri(authority)
                val constraints = Constraints.Builder()
                    .addContentUriTrigger(targetUri, true)
                    .setTriggerContentMaxDelay(800, TimeUnit.MILLISECONDS)
                    .setTriggerContentUpdateDelay(400, TimeUnit.MILLISECONDS)
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<BridgeSyncWorker>()
                    .setConstraints(constraints)
                    .addTag(TAG)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
                Log.d(TAG, "Successfully scheduled ContentUriTrigger WorkManager task for $targetUri")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to schedule ContentUriTrigger WorkManager task: ${e.message}")
            }
        }

        fun cancelObservation(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
