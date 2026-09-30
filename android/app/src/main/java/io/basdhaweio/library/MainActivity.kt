package io.basdhaweio.library

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

// The Library is the published dashboard, loaded live so pushes reach the
// app without an APK update. The widget's buttons open this activity with a
// URL such as …/#scan; the page's hash actions do the rest (docs/WIDGET.md).
const val LIVE_HOST = "basdhaweio.github.io"
const val LIVE_URL = "https://$LIVE_HOST/bookDashboards/"

private const val BG = "#FFFDF8"   // the page header's colour, so the status-bar band blends in

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var pageReady = false
    private var bottomInsetCss = 0   // system-bar height in CSS px, handed to the page
    private var pendingCamera: PermissionRequest? = null

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val req = pendingCamera
        pendingCamera = null
        if (req == null) return@registerForActivityResult
        if (granted) req.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) else req.deny()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor(BG)
        window.navigationBarColor = Color.parseColor(BG)

        webView = WebView(this).apply {
            setBackgroundColor(Color.parseColor(BG))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true          // the page's setup (inbox token) lives here
            settings.mediaPlaybackRequiresUserGesture = false // the scanner's <video> autoplays
            settings.setSupportZoom(false)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    if (!request.isForMainFrame) return false
                    if (request.url.host == LIVE_HOST) return false
                    return openExternally(request.url)   // Tombolo, Goodreads, Open Library…
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    pageReady = url?.startsWith(LIVE_URL) == true
                    if (pageReady) pushInset()
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    if (request.isForMainFrame && request.url.host == LIVE_HOST) showOffline(view)
                }

                override fun onReceivedHttpError(
                    view: WebView,
                    request: WebResourceRequest,
                    errorResponse: WebResourceResponse
                ) {
                    if (request.isForMainFrame && request.url.host == LIVE_HOST &&
                        errorResponse.statusCode >= 500
                    ) showOffline(view)
                }
            }

            webChromeClient = object : WebChromeClient() {
                // getUserMedia from the page → Android's camera permission
                override fun onPermissionRequest(request: PermissionRequest) {
                    if (request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                        if (hasCameraPermission()) {
                            request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                        } else {
                            pendingCamera = request
                            cameraPermission.launch(Manifest.permission.CAMERA)
                        }
                    } else {
                        request.deny()
                    }
                }
            }
        }

        // Target SDK 35 draws edge-to-edge: pad the top for the status bar /
        // cutout, but NOT the bottom — the page's own fixed menu bar pads
        // itself by --app-inset-bottom (pushInset), so there is no empty band
        // under the menu (John, 2026-09-30).
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor(BG))
            addView(
                webView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }
        setContentView(root)
        val density = resources.displayMetrics.density
        val minTopPx = (24 * density).toInt()
        root.setPadding(0, minTopPx, 0, 0)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(bars.left, maxOf(bars.top, minTopPx), bars.right, 0)
            val css = (bars.bottom / density).toInt()
            if (css != bottomInsetCss) {
                bottomInsetCss = css
                pushInset()
            }
            insets
        }

        onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) webView.goBack() else finish()
        }

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(targetUrl(intent))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val url = targetUrl(intent)
        val hash = url.substringAfter('#', "")
        if (pageReady && hash.isNotEmpty()) {
            // same document: just move the hash — the page listens for hashchange
            webView.evaluateJavascript("location.hash=${jsString("#$hash")}", null)
        } else {
            webView.loadUrl(url)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    // A widget tap carries the page URL as the intent data; anything off-site is ignored.
    private fun targetUrl(intent: Intent?): String {
        val data = intent?.data?.toString() ?: return LIVE_URL
        return if (data.startsWith(LIVE_URL)) data else LIVE_URL
    }

    // The page reads --app-inset-bottom for its fixed menu bar's padding.
    private fun pushInset() {
        if (!pageReady) return
        webView.evaluateJavascript(
            "document.documentElement.style.setProperty('--app-inset-bottom','${bottomInsetCss}px')", null
        )
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun openExternally(url: Uri): Boolean {
        return try {
            startActivity(Intent(Intent.ACTION_VIEW, url))
            true
        } catch (e: Exception) {
            Toast.makeText(this, "No app can open ${url.host}", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun showOffline(view: WebView) {
        pageReady = false
        val html = """
            <!doctype html><meta name="viewport" content="width=device-width, initial-scale=1">
            <body style="font-family:sans-serif;background:$BG;color:#3b3733;padding:32px 20px;text-align:center">
            <h2 style="font-weight:600">The Library is unreachable</h2>
            <p style="color:#8a837a">No connection, or GitHub Pages is having a moment.</p>
            <p><a href="$LIVE_URL" style="display:inline-block;padding:10px 18px;border-radius:8px;background:#2f8f8a;color:#fff;text-decoration:none">Try again</a></p>
            </body>
        """.trimIndent()
        view.loadDataWithBaseURL(LIVE_URL, html, "text/html", "utf-8", null)
    }

    private fun jsString(s: String): String =
        "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'"
}
