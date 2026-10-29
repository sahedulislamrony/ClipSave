package com.sahed.clipsave.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sahed.clipsave.ui.theme.AppMark
import com.sahed.clipsave.ui.theme.ClipType
import com.sahed.clipsave.ui.theme.LocalClipColors
import com.sahed.clipsave.ui.theme.PrimaryButton
import com.sahed.clipsave.ui.theme.Radius
import com.sahed.clipsave.ui.theme.glass

/** First launch: art, one line, one button. */
@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    val c = LocalClipColors.current
    Box(Modifier.fillMaxSize().background(c.bg)) {
        // Two soft iris orbs give the glass card something to refract.
        Box(
            Modifier.size(420.dp).offset(x = (-120).dp, y = (-140).dp)
                .background(Brush.radialGradient(listOf(c.primary.copy(alpha = if (c.dark) 0.35f else 0.22f), Color.Transparent))),
        )
        Box(
            Modifier.align(Alignment.CenterEnd).size(320.dp).offset(x = 120.dp, y = (-60).dp)
                .background(Brush.radialGradient(listOf(c.primary.copy(alpha = if (c.dark) 0.30f else 0.18f), Color.Transparent))),
        )

        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
                Cover(
                    Brush.linearGradient(listOf(Color(0xFF0A0A0F), Color(0xFF2A1D7A), Color(0xFF5B3BF0))),
                    Modifier.align(Alignment.CenterStart).offset(x = (-4).dp, y = 30.dp).size(width = 150.dp, height = 190.dp).rotate(-6f),
                )
                Cover(
                    Brush.linearGradient(listOf(Color(0xFF5B3BF0), Color(0xFFEFEBFF))),
                    Modifier.align(Alignment.CenterEnd).offset(x = (-24).dp, y = (-10).dp).size(width = 140.dp, height = 170.dp).rotate(7f),
                )
                Column(
                    Modifier.align(Alignment.BottomCenter).padding(horizontal = 8.dp).fillMaxWidth().glass(RoundedCornerShape(Radius.Lg)).padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppMark(size = 40.dp)
                        Text("Saving", style = ClipType.BodyStrong, modifier = Modifier.padding(start = 12.dp).weight(1f))
                        Text("68%", style = ClipType.BodyStrong, color = c.primary)
                    }
                    Spacer(Modifier.height(12.dp))
                    ProgressBar(0.68f, true, c.primary, track = c.primarySoft)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Save any clip.\nOne tap.", style = ClipType.Display, color = c.ink)
            Spacer(Modifier.height(12.dp))
            Text("Share. Pick quality. Done.", style = ClipType.BodyStrong, color = c.inkMuted)
            Spacer(Modifier.height(40.dp))
            PrimaryButton("Get started", onStart, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Text(
                "Save only what you have the right to.",
                style = ClipType.Caption,
                color = c.inkMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Cover(brush: Brush, modifier: Modifier) {
    Box(modifier.shadow(12.dp, RoundedCornerShape(Radius.Lg)).clip(RoundedCornerShape(Radius.Lg)).background(brush)) {
        PlayDot(false, Modifier.align(Alignment.Center))
    }
}
