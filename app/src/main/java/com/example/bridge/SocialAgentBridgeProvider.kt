package com.example.bridge

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

/**
 * SocialAgentBridgeProvider is an Android ContentProvider that runs offline
 * inside this application. It provides timeout-guarded local IPC endpoints
 * so companion apps on the same device can ping, query, or push data safely.
 */
class SocialAgentBridgeProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        Log.i(TAG, "Offline IPC Bridge ContentProvider initialized.")
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val appContext = context ?: return errorBundle("Context is null")
        val bridgeManager = AppBridgeManager.getInstance(appContext)
        val callerName = extras?.getString(AppBridgeContract.KEY_SENDER) ?: callingPackage ?: "Companion"

        return when (method) {
            AppBridgeContract.METHOD_PING -> {
                Log.d(TAG, "Received PING from $callerName")
                bridgeManager.recordEvent(
                    BridgeExchangeEvent(
                        direction = ExchangeDirection.INCOMING,
                        dataType = "PING",
                        summary = "استلام إشارة فحص (PING) من $callerName",
                        itemCount = 1,
                        isSuccess = true
                    )
                )
                Bundle().apply {
                    putString(AppBridgeContract.KEY_STATUS, AppBridgeContract.STATUS_SUCCESS)
                    putString(AppBridgeContract.KEY_SENDER, "SocialAgent")
                    putString(AppBridgeContract.KEY_SECRET_TOKEN, bridgeManager.ourPairingToken)
                    putString(AppBridgeContract.KEY_MESSAGE, "جسر التبادل المحلي أوفلاين متصل وجاهز")
                    putLong(AppBridgeContract.KEY_TIMESTAMP, System.currentTimeMillis())
                }
            }

            AppBridgeContract.METHOD_PULL_DATA -> {
                val dataType = extras?.getString(AppBridgeContract.KEY_DATA_TYPE) ?: AppBridgeContract.TYPE_POSTS
                Log.d(TAG, "Companion ($callerName) requested PULL_DATA: $dataType")
                try {
                    val jsonPayload = runBlocking(Dispatchers.IO) {
                        withTimeoutOrNull(IPC_TIMEOUT_MS) {
                            bridgeManager.handleInboundPull(dataType)
                        }
                    } ?: return errorBundle("انتهت مهلة قراءة البيانات من قاعدة البيانات")

                    Bundle().apply {
                        putString(AppBridgeContract.KEY_STATUS, AppBridgeContract.STATUS_SUCCESS)
                        putString(AppBridgeContract.KEY_DATA_TYPE, dataType)
                        putString(AppBridgeContract.KEY_PAYLOAD_JSON, jsonPayload)
                        putLong(AppBridgeContract.KEY_TIMESTAMP, System.currentTimeMillis())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error serving PULL_DATA", e)
                    errorBundle(e.message ?: "خطأ في قراءة البيانات")
                }
            }

            AppBridgeContract.METHOD_PUSH_DATA -> {
                val dataType = extras?.getString(AppBridgeContract.KEY_DATA_TYPE)
                val payloadJson = extras?.getString(AppBridgeContract.KEY_PAYLOAD_JSON)
                Log.d(TAG, "Companion ($callerName) pushed PUSH_DATA: $dataType")
                try {
                    val result = runBlocking(Dispatchers.IO) {
                        withTimeoutOrNull(IPC_TIMEOUT_MS) {
                            bridgeManager.handleInboundPush(dataType, payloadJson)
                        }
                    } ?: return errorBundle("انتهت مهلة حفظ البيانات الواردة")

                    val (success, count) = result
                    Bundle().apply {
                        putString(
                            AppBridgeContract.KEY_STATUS,
                            if (success) AppBridgeContract.STATUS_SUCCESS else AppBridgeContract.STATUS_ERROR
                        )
                        putInt(AppBridgeContract.KEY_ITEM_COUNT, count)
                        putString(AppBridgeContract.KEY_MESSAGE, "تم استلام $count عنصر وتخزينها محلياً")
                        putLong(AppBridgeContract.KEY_TIMESTAMP, System.currentTimeMillis())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error serving PUSH_DATA", e)
                    errorBundle(e.message ?: "خطأ في معالجة البيانات")
                }
            }

            AppBridgeContract.METHOD_GET_CAPABILITIES -> {
                val capabilities = JSONObject().apply {
                    put("app", "SocialAgent")
                    put("version", "2.0")
                    put("shortId", bridgeManager.ourShortId)
                    put("pairingToken", bridgeManager.ourPairingToken)
                    put("supportedTypes", JSONArray(listOf("POSTS", "LEADS", "BROWSER_TABS", "TASKS", "RAW_JSON")))
                    put("offline", true)
                }
                Bundle().apply {
                    putString(AppBridgeContract.KEY_STATUS, AppBridgeContract.STATUS_SUCCESS)
                    putString(AppBridgeContract.KEY_PAYLOAD_JSON, capabilities.toString())
                }
            }

            else -> errorBundle("طريقة غير معروفة: $method")
        }
    }

    private fun errorBundle(message: String): Bundle = Bundle().apply {
        putString(AppBridgeContract.KEY_STATUS, AppBridgeContract.STATUS_ERROR)
        putString(AppBridgeContract.KEY_MESSAGE, message)
        putLong(AppBridgeContract.KEY_TIMESTAMP, System.currentTimeMillis())
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

    companion object {
        private const val TAG = "SocialAgentBridgeProvider"
        private const val IPC_TIMEOUT_MS = 4500L
    }
}
