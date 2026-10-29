package com.sahed.clipsave.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
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
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Someone who only ever shares into ClipSave never opens the app, where this is otherwise asked;
        // without it Android 13+ drops the download's progress and "saved" notifications.
        if (savedInstanceState == null && Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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
