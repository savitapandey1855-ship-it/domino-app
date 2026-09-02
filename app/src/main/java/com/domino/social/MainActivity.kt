package com.domino.social

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.view.WindowCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var errorLayout: View

    companion object {
        private const val LAUNCH_URL = "https://domino6139socialmedia.edgeone.dev/"
        private const val TAG = "DominoApp"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Set status bar color
        window.statusBarColor = Color.parseColor("#7C3AED")

        val container = FrameLayout(this)

        // ---- WebView ----
        webView = WebView(this)

        // ---- SwipeRefreshLayout (pull-to-refresh) ----
        swipeRefresh = SwipeRefreshLayout(this).apply {
            setOnRefreshListener {
                webView.reload()
            }
            // Purple color scheme for refresh spinner
            setColorSchemeColors(
                Color.parseColor("#7C3AED"),
                Color.parseColor("#EC4899"),
                Color.parseColor("#8B5CF6")
            )
        }
        swipeRefresh.addView(webView)
        container.addView(swipeRefresh)

        // ---- Progress bar (top loading bar) ----
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            isIndeterminate = false
            progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#7C3AED"))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (6 * resources.displayMetrics.density).toInt()
            )
        }
        container.addView(progressBar)

        // ---- Error layout (Something went wrong) ----
        errorLayout = layoutInflater.inflate(R.layout.layout_error, null)
        errorLayout.visibility = View.GONE
        errorLayout.findViewById<android.widget.Button>(R.id.btnRetry).setOnClickListener {
            errorLayout.visibility = View.GONE
            webView.loadUrl(LAUNCH_URL)
        }
        container.addView(errorLayout)

        setContentView(container)

        // ---- FCM Setup ----
        FirebaseMessaging.getInstance().subscribeToTopic("all")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) Log.d(TAG, "Subscribed to 'all' topic")
            }
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) Log.d(TAG, "FCM Token: ${task.result}")
        }

        // ---- WebView Configuration ----
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            // Zoom OFF
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            // Text copy OFF
            textZoom = 100
            // File access
            allowFileAccess = true
            allowContentAccess = true
            // Media
            mediaPlaybackRequiresUserGesture = false
            // Mixed content
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            // User agent
            userAgentString = userAgentString + " DominoApp/1.0"
        }

        // Disable long-click (prevents text selection / copy)
        webView.setOnLongClickListener { true }
        webView.isHapticFeedbackEnabled = false
        webView.isLongClickable = true

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                return if (url.startsWith("http://") || url.startsWith("https://")) {
                    // Internal navigation — load in WebView
                    false
                } else {
                    // External links (tel:, mailto:, whatsapp:, etc.)
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                // Only show error page for main frame
                if (request != null && request.isForMainFrame) {
                    errorLayout.visibility = View.VISIBLE
                    swipeRefresh.isRefreshing = false
                    progressBar.visibility = View.GONE
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                swipeRefresh.isRefreshing = false
                progressBar.progress = 100
                progressBar.visibility = View.GONE

                // Inject CSS to disable text selection
                view?.evaluateJavascript(
                    """
                    (function() {
                        var style = document.createElement('style');
                        style.type = 'text/css';
                        style.innerHTML = '' +
                            '* {' +
                            '  -webkit-user-select: none !important;' +
                            '  -moz-user-select: none !important;' +
                            '  -ms-user-select: none !important;' +
                            '  user-select: none !important;' +
                            '}' +
                            'input, textarea {' +
                            '  -webkit-user-select: text !important;' +
                            '  -moz-user-select: text !important;' +
                            '  -ms-user-select: text !important;' +
                            '  user-select: text !important;' +
                            '}';
                        document.head.appendChild(style);
                    })();
                    """.trimIndent(),
                    null
                )
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.visibility = View.VISIBLE
                progressBar.progress = newProgress
                if (newProgress >= 100) {
                    progressBar.visibility = View.GONE
                }
            }

            // File upload support
            private var filePathCallback: android.webkit.ValueCallback<Array<Uri>>? = null

            override fun onShowFileChooser(
                webView: WebView?,
                callback: android.webkit.ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "*/*"
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                try {
                    @Suppress("DEPRECATION")
                    startActivityForResult(Intent.createChooser(intent, "Select File"), 1001)
                } catch (e: Exception) {
                    filePathCallback = null
                    return false
                }
                return true
            }
        }

        // Restore state on rotation or load URL
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(LAUNCH_URL)
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }
}
