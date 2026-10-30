package com.sahedul.clipsave.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sahedul.clipsave.BuildConfig
import com.sahedul.clipsave.core.UpdateState
import com.sahedul.clipsave.ui.theme.AppMark
import com.sahedul.clipsave.ui.theme.ClipType
import com.sahedul.clipsave.ui.theme.GlassButton
import com.sahedul.clipsave.ui.theme.LocalClipColors
import com.sahedul.clipsave.ui.theme.PrimaryButton
import com.sahedul.clipsave.ui.theme.clipBackground

/** Offers the pending release: update now, or put it off and keep using the app (Back does the same). */
@Composable
fun UpdateScreen(state: UpdateState.Pending, onUpdate: () -> Unit, onLater: () -> Unit) {
    val c = LocalClipColors.current
    BackHandler(enabled = state is UpdateState.Available, onBack = onLater)
    val sizeMb = state.release.asset.size / (1024 * 1024)
    Box(Modifier.fillMaxSize().clipBackground()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AppMark(size = 88.dp)
                Spacer(Modifier.height(24.dp))
                Text("Update available", style = ClipType.Title, color = c.ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    "ClipSave ${state.release.version} is out. You're on v${BuildConfig.VERSION_NAME}.",
                    style = ClipType.BodyStrong,
                    color = c.inkMuted,
                    textAlign = TextAlign.Center,
                )
            }

            when (state) {
                is UpdateState.Downloading -> {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = c.primary,
                        trackColor = c.line,
                        strokeCap = StrokeCap.Round,
                        drawStopIndicator = {},
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Downloading… ${(state.progress * 100).toInt()}% of $sizeMb MB", style = ClipType.Label, color = c.inkMuted)
                    Spacer(Modifier.height(20.dp))
                }
                is UpdateState.Installing -> {
                    Text("Installing…", style = ClipType.Label, color = c.inkMuted)
                    Spacer(Modifier.height(20.dp))
                }
                is UpdateState.Available -> {
                    if (state.error != null) {
                        Text(state.error, style = ClipType.Label, color = c.danger, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                    }
                    PrimaryButton(
                        text = if (state.error != null) "Try again" else "Update now · $sizeMb MB",
                        onClick = onUpdate,
                        icon = Icons.Rounded.Download,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    GlassButton(
                        text = "Update later",
                        onClick = onLater,
                        modifier = Modifier.fillMaxWidth(),
                        height = 56.dp,
                    )
                }
            }
        }
    }
}
