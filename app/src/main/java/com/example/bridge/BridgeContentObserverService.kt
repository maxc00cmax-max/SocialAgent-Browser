package com.example.bridge

import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Background Service that maintains a persistent ContentObserver on the companion
 * offline app's ContentProvider. When changes occur in the companion app,
 * this service immediately triggers automated data pulling, updates the local Room database,
 * and orchestrates the SocialAgent Browser tabs in real-time.
 */
class BridgeContentObserverService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var contentObserver: ContentObserver? = null
    private var registeredAuthority: String? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "BridgeContentObserverService created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bridgeManager = AppBridgeManager.getInstance(applicationContext)
        val authority = intent?.getStringExtra(EXTRA_AUTHORITY) ?: bridgeManager.targetAuthority.value

        registerContentObserver(authority)

        // Also schedule WorkManager ContentUriTrigger as a resilient fallback
        BridgeSyncWorker.scheduleContentTrigger(applicationContext, authority)

        bridgeManager.recordEvent(
            BridgeExchangeEvent(
                direction = ExchangeDirection.INCOMING,
                dataType = "SERVICE_STATUS",
                summary = "خدمة المراقبة في الخلفية نشطة وتراقب مزود البيانات أوفلاين",
                itemCount = 1,
                isSuccess = true
            )
        )

        return START_STICKY
    }

    private fun registerContentObserver(authority: String) {
        if (contentObserver != null && registeredAuthority == authority) {
            return
        }

        unregisterObserver()
        registeredAuthority = authority

        // Guard: Verify if the ContentProvider is actually installed on this device before registering
        val providerInfo = try {
            packageManager.resolveContentProvider(authority, 0)
        } catch (e: Exception) {
            null
        }

        if (providerInfo == null) {
            Log.d(TAG, "Companion provider ($authority) is not installed on this device yet. Standing by.")
            return
        }

        val targetUri = AppBridgeContract.buildUri(authority)
        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                Log.d(TAG, "⚡ ContentProvider change detected from companion app: $uri")

                serviceScope.launch {
                    val bridgeManager = AppBridgeManager.getInstance(applicationContext)
                    bridgeManager.recordEvent(
                        BridgeExchangeEvent(
                            direction = ExchangeDirection.INCOMING,
                            dataType = "LIVE_TRIGGER",
                            summary = "رصد تحديث فوري من التطبيق الشريك! جاري سحب البيانات ومزامنة Room...",
                            itemCount = 1,
                            isSuccess = true
                        )
                    )

                    val result = bridgeManager.pullDataFromCompanion()
                    val count = result.getOrDefault(0)

                    // Re-schedule WorkManager trigger
                    BridgeSyncWorker.scheduleContentTrigger(applicationContext, authority)
                }
            }
        }

        try {
            contentResolver.registerContentObserver(targetUri, true, contentObserver!!)
            Log.i(TAG, "Registered ContentObserver on $targetUri")
        } catch (e: SecurityException) {
            Log.d(TAG, "Companion provider is not available yet ($authority): ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Could not register ContentObserver on $targetUri: ${e.message}")
        }
    }

    private fun unregisterObserver() {
        contentObserver?.let {
            try {
                contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering observer: ${e.message}")
            }
            contentObserver = null
        }
    }

    override fun onDestroy() {
        unregisterObserver()
        serviceScope.cancel()
        Log.i(TAG, "BridgeContentObserverService destroyed.")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val TAG = "BridgeObserverService"
        const val EXTRA_AUTHORITY = "extra_authority"

        fun startService(context: Context, authority: String? = null) {
            try {
                val intent = Intent(context, BridgeContentObserverService::class.java).apply {
                    if (authority != null) {
                        putExtra(EXTRA_AUTHORITY, authority)
                    }
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Could not start service directly: ${e.message}")
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, BridgeContentObserverService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Could not stop service: ${e.message}")
            }
        }
    }
}
