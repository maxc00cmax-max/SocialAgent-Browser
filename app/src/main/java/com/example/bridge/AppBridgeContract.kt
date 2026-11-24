package com.example.bridge

import android.net.Uri

/**
 * Android Inter-Process Communication (IPC) Contract.
 * Allows this app and an offline companion app on the same device
 * to exchange data seamlessly and instantly with zero internet connectivity.
 */
object AppBridgeContract {

    // Action identifier for discovery
    const val ACTION_DATA_EXCHANGE = "com.aistudio.bridge.ACTION_DATA_EXCHANGE"

    // Default Authority for THIS app (Social Agent)
    const val OUR_DEFAULT_AUTHORITY = "com.aistudio.socialagent.kzxmbq.bridge.provider"

    // Default Authority expected for the COMPANION app
    const val COMPANION_DEFAULT_AUTHORITY = "com.example.companion.bridge.provider"

    // Methods supported via ContentResolver.call()
    const val METHOD_PING = "ping"
    const val METHOD_PULL_DATA = "pull_data"
    const val METHOD_PUSH_DATA = "push_data"
    const val METHOD_EXECUTE_ACTION = "execute_action"
    const val METHOD_GET_CAPABILITIES = "get_capabilities"

    // Bundle Keys for Arguments and Results
    const val KEY_STATUS = "status"
    const val STATUS_SUCCESS = "SUCCESS"
    const val STATUS_ERROR = "ERROR"

    const val KEY_MESSAGE = "message"
    const val KEY_SENDER = "sender"
    const val KEY_TIMESTAMP = "timestamp"
    const val KEY_DATA_TYPE = "data_type"
    const val KEY_PAYLOAD_JSON = "payload_json"
    const val KEY_ITEM_COUNT = "item_count"
    const val KEY_ACTION_NAME = "action_name"
    const val KEY_SECRET_TOKEN = "secret_token"

    // Supported Data Types
    const val TYPE_POSTS = "POSTS"
    const val TYPE_TASKS = "TASKS"
    const val TYPE_BROWSER_TABS = "BROWSER_TABS"
    const val TYPE_LEADS = "LEADS"
    const val TYPE_RAW_JSON = "RAW_JSON"
    const val TYPE_PROMPTS = "PROMPTS"

    // Short ID and Pairing Tokens
    const val OUR_APP_SHORT_ID = "SA-KZXMBQ"
    const val PAIRING_PREFIX = "BRIDGE-ID:"

    fun generateOurPairingToken(): String {
        return "$PAIRING_PREFIX$OUR_APP_SHORT_ID#$OUR_DEFAULT_AUTHORITY"
    }

    /**
     * Resolves any user input (short ID, pairing token, package name, or authority)
     * into a valid Android ContentProvider authority string.
     */
    fun resolveAuthorityFromInput(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return COMPANION_DEFAULT_AUTHORITY

        // Case 1: Full pairing token: "BRIDGE-ID:CP-1234#com.example.companion.bridge.provider"
        if (trimmed.contains("#")) {
            val parts = trimmed.split("#")
            if (parts.size >= 2 && parts[1].isNotBlank()) {
                return parts[1].trim()
            }
        }

        // Case 2: Prefixed with BRIDGE-ID:
        val clean = if (trimmed.startsWith(PAIRING_PREFIX)) {
            trimmed.removePrefix(PAIRING_PREFIX).trim()
        } else {
            trimmed
        }

        // Case 3: Already an authority with .bridge.provider or .provider
        if (clean.contains(".provider")) {
            return clean
        }

        // Case 4: Package name without .provider (e.g. "com.example.companion")
        if (clean.contains(".")) {
            return if (clean.endsWith(".bridge.provider")) clean else "$clean.bridge.provider"
        }

        // Case 5: Short ID like "CRM-XXXXXXXX", "COMPANION", or "CP-123"
        if (clean.startsWith("CRM", ignoreCase = true) || clean.startsWith("CP", ignoreCase = true)) {
            return COMPANION_DEFAULT_AUTHORITY
        }
        return "com.example.${clean.lowercase().replace("-", "").replace("_", "")}.bridge.provider"
    }

    fun buildUri(authority: String): Uri {
        return Uri.parse("content://$authority")
    }
}

data class BridgeExchangeEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val direction: ExchangeDirection, // INCOMING or OUTGOING
    val dataType: String,
    val summary: String,
    val itemCount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true
)

enum class ExchangeDirection {
    INCOMING,
    OUTGOING
}
