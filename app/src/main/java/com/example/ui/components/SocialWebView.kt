package com.example.ui.components

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun SocialWebView(
    webView: WebView,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = {
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView
        },
        update = { currentView ->
            (currentView.parent as? ViewGroup)?.let { parent ->
                if (parent != currentView) {
                    parent.removeView(currentView)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
