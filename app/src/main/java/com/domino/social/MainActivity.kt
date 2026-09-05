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
        private const val TAG = "DranivoLite"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val container = FrameLayout(this)

        webView = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(0, 0, 0, 0)
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            params.setMargins(0, 0, 0, 0)
            layoutParams = params
        }

        swipeRefresh = SwipeRefreshLayout(this).apply {
            setPadding(0, 0, 0, 0)
            setOnRefreshListener { webView.reload() }
            setColorSchemeColors(
                Color.parseColor("#7C3AED"),
                Color.parseColor("#EC4899"),
                Color.parseColor("#8B5CF6")
            )
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            params.setMargins(0, 0, 0, 0)
            layoutParams = params
        }
        swipeRefresh.addView(webView)
        container.addView(swipeRefresh)

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            isIndeterminate = false
            progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#7C3AED"))
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (6 * resources.displayMetrics.density).toInt()
            )
            params.setMargins(0, 0, 0, 0)
            layoutParams = params
            elevation = 100f
        }
        container.addView(progressBar)

        errorLayout = layoutInflater.inflate(R.layout.layout_error, null)
        errorLayout.visibility = View.GONE
        errorLayout.findViewById<android.widget.Button>(R.id.btnRetry).setOnClickListener {
            errorLayout.visibility = View.GONE
            webView.loadUrl(LAUNCH_URL)
        }
        container.addView(errorLayout)

        setContentView(container)

        try {
            FirebaseMessaging.getInstance().subscribeToTopic("all")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) Log.d(TAG, "Subscribed to 'all' topic")
                }
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) Log.d(TAG, "FCM Token: ${task.result}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "FCM not configured: ${e.message}")
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            offscreenPreRaster = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            textZoom = 100
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }

        webView.setPadding(0, 0, 0, 0)
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        webView.setOnLongClickListener { true }
        webView.isHapticFeedbackEnabled = false
        webView.isLongClickable = true

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                return if (url.startsWith("http://") || url.startsWith("https://")) {
                    false
                } else {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
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

                // Minimal CSS: only disable text selection + tap highlight
                view?.evaluateJavascript(
                    """
                    (function() {
                        var style = document.createElement('style');
                        style.type = 'text/css';
                        style.innerHTML = '' +
                            '* {' +
                            '  -webkit-user-select: none !important;' +
                            '  user-select: none !important;' +
                            '  -webkit-touch-callout: none !important;' +
                            '  -webkit-tap-highlight-color: transparent !important;' +
                            '}' +
                            'input, textarea, [contenteditable="true"] {' +
                            '  -webkit-user-select: text !important;' +
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
                    startActivityForResult(Intent.createChooser(intent, "Select File"), 1001)
                } catch (e: Exception) {
                    filePathCallback = null
                    return false
                }
                return true
            }
        }

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
