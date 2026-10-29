package com.sahed.clipsave.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.sahed.clipsave.core.UrlParser
import com.sahed.clipsave.ui.theme.ClipSaveTheme

/**
 * Target of Android's share sheet. From Instagram/Facebook/etc: ••• → Share → ClipSave.
 * Shows the quality picker over the source app and finishes as soon as the download is queued.
 */
class ShareReceiverActivity : ComponentActivity() {
    private val vm: ExtractViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClipSaveTheme {
                ExtractSheet(
                    vm = vm,
                    onDismiss = { finish() },
                    onOpenSettings = {
                        startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_TAB, MainActivity.TAB_SETTINGS))
                        finish()
                    },
                )
            }
        }
        if (savedInstanceState == null) handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent) {
        val shared = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
        val url = UrlParser.firstUrl(shared)
        if (url == null) {
            Toast.makeText(this, "No link found in what you shared", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        vm.load(url)
    }
}
