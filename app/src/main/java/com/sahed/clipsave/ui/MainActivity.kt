package com.sahed.clipsave.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sahed.clipsave.ClipSaveApp
import com.sahed.clipsave.ui.theme.ClipSaveTheme
import com.sahed.clipsave.ui.theme.clipBackground
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

class MainActivity : ComponentActivity() {
    private val vm: ExtractViewModel by viewModels()
    private val requestedTab = mutableIntStateOf(-1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedTab.intValue = intent.getIntExtra(EXTRA_TAB, -1)
        val container = (application as ClipSaveApp).container

        setContent {
            ClipSaveTheme {
                var welcomed by remember { mutableStateOf(container.prefs.onboarded) }
                var tab by rememberSaveable { mutableIntStateOf(TAB_HOME) }
                var link by rememberSaveable { mutableStateOf("") }
                var page by rememberSaveable { mutableStateOf<String?>(null) }
                val active by container.downloads.items.collectAsStateWithLifecycle()
                val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

                LaunchedEffect(welcomed) {
                    if (!welcomed) return@LaunchedEffect
                    if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    container.engine.updateIfStale(active.any { it.isActive })
                }
                LaunchedEffect(requestedTab.intValue) {
                    if (requestedTab.intValue >= 0) { tab = requestedTab.intValue; requestedTab.intValue = -1 }
                }

                if (!welcomed) {
                    WelcomeScreen(onStart = { container.prefs.onboarded = true; welcomed = true })
                    return@ClipSaveTheme
                }

                BackHandler(enabled = page != null) { page = null }

                val insets = PaddingValues(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + DockClearance,
                )
                val hazeState = rememberHazeState()
                Box(Modifier.clipBackground()) {
                    when (page) {
                        PAGE_HOW -> HowItWorksScreen(onBack = { page = null })
                        PAGE_THEME -> ThemeScreen(container.prefs, onBack = { page = null })
                        PAGE_TERMS -> TermsScreen(onBack = { page = null })
                        else -> {
                            // The page (backdrop included) is the blur source the dock frosts over.
                            Box(Modifier.hazeSource(hazeState).clipBackground()) {
                                when (tab) {
                                    TAB_HOME -> HomeScreen(
                                        vm, insets,
                                        text = link,
                                        onText = { link = it },
                                        onOpenSettings = { tab = TAB_SETTINGS },
                                        onStarted = { tab = TAB_DOWNLOADS },
                                        onHowItWorks = { page = PAGE_HOW },
                                    )
                                    TAB_DOWNLOADS -> DownloadsScreen(container, insets)
                                    else -> SettingsScreen(container, insets, onOpenPage = { page = it })
                                }
                            }
                            Dock(
                                tab = tab,
                                running = active.any { it.isActive },
                                onTab = { tab = it },
                                haze = hazeState,
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        requestedTab.intValue = intent.getIntExtra(EXTRA_TAB, -1)
    }

    companion object {
        const val EXTRA_TAB = "tab"
        const val TAB_HOME = 0
        const val TAB_DOWNLOADS = 1
        const val TAB_SETTINGS = 2
        const val PAGE_HOW = "how"
        const val PAGE_THEME = "theme"
        const val PAGE_TERMS = "terms"
    }
}
