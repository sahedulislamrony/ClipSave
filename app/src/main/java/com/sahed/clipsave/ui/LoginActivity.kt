package com.sahed.clipsave.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sahed.clipsave.ui.theme.ClipType
import com.sahed.clipsave.ui.theme.GlassButton
import com.sahed.clipsave.ui.theme.GlassIconButton
import com.sahed.clipsave.ui.theme.LocalClipColors
import androidx.compose.ui.viewinterop.AndroidView
import com.sahed.clipsave.ClipSaveApp
import com.sahed.clipsave.core.CookieStore
import com.sahed.clipsave.ui.theme.ClipSaveTheme

/**
 * In-app sign-in so yt-dlp can read posts that need an account. The password never touches
 * ClipSave — only the resulting session cookies are copied to a private file.
 */
class LoginActivity : ComponentActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val site = CookieStore.Site.entries.firstOrNull { it.key == intent.getStringExtra(EXTRA_SITE) }
            ?: return finish()
        val store = (application as ClipSaveApp).container.cookies

        setContent {
            ClipSaveTheme {
                Column(Modifier.fillMaxSize().background(LocalClipColors.current.bg).statusBarsPadding()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GlassIconButton(Icons.Rounded.Close, "Close", { finish() }, size = 44.dp)
                        Text(
                            site.key.replaceFirstChar { it.uppercase() },
                            style = ClipType.Headline,
                            color = LocalClipColors.current.ink,
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        )
                        GlassButton("Done", {
                            val ok = store.captureFromWebView(site)
                            Toast.makeText(
                                this@LoginActivity,
                                if (ok) "Signed in" else "No session found — finish signing in first",
                                Toast.LENGTH_SHORT,
                            ).show()
                            if (ok) finish()
                        }, icon = Icons.Rounded.Check, height = 44.dp)
                    }
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                // AndroidView defaults to WRAP_CONTENT, which makes WebView report a 0px-tall
                                // viewport. Instagram/Facebook size their pages with height:100%, so they'd
                                // collapse to a blank white screen.
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.userAgentString =
                                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                webViewClient = object : WebViewClient() {
                                    // Facebook bounces to app-handoff schemes (e.g. sfilvavs://login_via_app)
                                    // that WebView can't load. Swallow them so the web login form stays up.
                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) =
                                        request.url.scheme !in setOf("http", "https")
                                }
                                loadUrl(site.loginUrl)
                            }
                        },
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_SITE = "site"
    }
}
