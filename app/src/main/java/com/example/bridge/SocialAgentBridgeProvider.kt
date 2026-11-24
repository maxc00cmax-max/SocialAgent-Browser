package com.example.bridge

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.runBlocking

/**
 * SocialAgentBridgeProvider is an Android ContentProvider that runs offline
 * inside this application. It provides local IPC endpoints so companion apps
 * on the same device can ping, query data, or push data to this app.
 */
class SocialAgentBridgeProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        Log.i("SocialAgentBridgeProvider", "Offline IPC Bridge ContentProvider initialized.")
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val appContext = context ?: return Bundle().apply {
            putString(AppBridgeContract.KEY_STATUS, AppBridgeContract.STATUS_ERROR)
            putString(AppBridgeContract.KEY_MESSAGE, "Context is null")
        }

        val bridgeManager = AppBridgeManager.getInstance(appContext)

        return when (method) {
            AppBridgeContract.METHOD_PING -> {
                Log.d("SocialAgentBridgeProvider", "Received PING from companion app")
                bridgeManager.recordEvent(
                    BridgeExchangeEvent(
                        direction = ExchangeDirection.INCOMING,
                        dataType = "PING",
                        summary = "استلام إشارة فحص (PING) من التطبيق المرافق على الهاتف",
                        itemCount = 1,
                        isSuccess = true
                    )
                )
                Bundle().apply {
                    putString(AppBridgeContract.KEY_STATUS, AppBridgeContract.STATUS_SUCCESS)
                    putString(AppBridgeContract.KEY_SENDER, "SocialAgent")
                    putString(AppBridgeContract.KEY_MESSAGE, "جسر التبادل المحلي أوفلاين متصل وجاهز")
                    putLong(AppBridgeContract.KEY_TIMESTAMP, System.currentTimeMillis())
                }
            }

            AppBridgeContract.METHOD_PULL_DATA -> {
                val dataType = extras?.getString(AppBridgeContract.KEY_DATA_TYPE) ?: AppBridgeContract.TYPE_POSTS
                Log.d("SocialAgentBridgeProvider", "Companion requested PULL_DATA: $dataType")
                try {
                    val jsonPayload = runBlocking {
                        bridgeManager.handleInboundPull(dataType)
                    }
                    Bundle().apply {
                        putString(AppBridgeContract.KEY_STATUS, AppBridgeContract.STATUS_SUCCESS)
                        putString(AppBridgeContract.KEY_DATA_TYPE, dataType)
                        putString(AppBridgeContract.KEY_PAYLOAD_JSON, jsonPayload)
                        putLong(AppBridgeContract.KEY_TIMESTAMP, System.currentTimeMillis())
                    }
                } catch (e: Exception) {
                    Log.e("SocialAgentBridgeProvider", "Error serving PULL_DATA", e)
                    Bundle().apply {
                        putString(AppBridgeContract.KEY_STATUS, AppBridgeContract.STATUS_ERROR)
                        putString(AppBridgeContract.KEY_MESSAGE, e.message ?: "خطأ في قراءة البيانات")
                    }
                }
            }

            AppBridgeContract.METHOD_PUSH_DATA -> {
                val dataType = extras?.getString(AppBridgeContract.KEY_DATA_TYPE)
                val payloadJson = extras?.getString(AppBridgeContract.KEY_PAYLOAD_JSON)
                Log.d("SocialAgentBridgeProvider", "Companion pushed PUSH_DATA: $dataType")
                try {
                    val (success, count) = runBlocking {
                        bridgeManager.handleInboundPush(dataType, payloadJson)
                    }
                    Bundle().apply {
                        putString(AppBridgeContract.KEY_STATUS, if (success) AppBridgeContract.STATUS_SUCCESS else AppBridgeContract.STATUS_ERROR)
                        putInt(AppBridgeContract.KEY_ITEM_COUNT, count)
                        putString(AppBridgeContract.KEY_MESSAGE, "تم استلام $count عنصر وتخزينها محلياً")
                        putLong(AppBridgeContract.KEY_TIMESTAMP, System.currentTimeMillis())
                    }
                } catch (e: Exception) {
                    Log.e("SocialAgentBridgeProvider", "Error serving PUSH_DATA", e)
                    Bundle().apply {
                        putString(AppBridgeContract.KEY_STATUS, AppBridgeContract.STATUS_ERROR)
                        putString(AppBridgeContract.KEY_MESSAGE, e.message ?: "خطأ في معالجة البيانات")
                    }
                }
            }

            AppBridgeContract.METHOD_GET_CAPABILITIES -> {
                Bundle().apply {
                    putString(AppBridgeContract.KEY_STATUS, AppBridgeContract.STATUS_SUCCESS)
                    putString(AppBridgeContract.KEY_PAYLOAD_JSON, """
                        {
                            "app": "SocialAgent",
                            "version": "1.0",
                            "supportedTypes": ["POSTS", "TASKS", "BROWSER_TABS", "LEADS", "RAW_JSON"],
                            "offline": true
                        }
                    """.trimIndent())
                }
            }

            else -> {
                Bundle().apply {
                    putString(AppBridgeContract.KEY_STATUS, AppBridgeContract.STATUS_ERROR)
                    putString(AppBridgeContract.KEY_MESSAGE, "طريقة غير معروفة: $method")
                }
            }
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/vnd.com.example.bridge.data"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
