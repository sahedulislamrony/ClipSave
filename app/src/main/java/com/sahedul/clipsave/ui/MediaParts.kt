package com.sahedul.clipsave.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sahedul.clipsave.ui.theme.ClipType
import com.sahedul.clipsave.ui.theme.LocalClipColors

/** Thumbnail with an iris wash behind it so tiles never look empty while an image loads. */
@Composable
fun Thumb(url: String?, modifier: Modifier = Modifier) {
    val c = LocalClipColors.current
    val wash = Brush.linearGradient(listOf(c.primarySoft, c.surfaceSunken, c.primary.copy(alpha = if (c.dark) 0.35f else 0.22f)))
    Box(modifier.background(wash)) {
        AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

/** Small translucent pill that sits on thumbnails (quality, MP3…). */
@Composable
fun QualityPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.38f))
            .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) { Text(text, style = ClipType.Label, color = Color.White) }
}

/** Glass play dot on a thumbnail. */
@Composable
fun PlayDot(audio: Boolean, modifier: Modifier = Modifier, size: Dp = 34.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.28f))
            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (audio) Icons.Rounded.MusicNote else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(size / 2))
    }
}

/** Pill progress bar; a short stub while the percentage is still unknown. */
@Composable
fun ProgressBar(progress: Float, determinate: Boolean, fill: Color, modifier: Modifier = Modifier, track: Color = fill.copy(alpha = 0.28f)) {
    BoxWithConstraints(modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(track)) {
        if (determinate) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).clip(CircleShape).background(fill))
        } else {
            // Indeterminate: a short segment sweeping across, so waiting reads as working.
            val sweep by rememberInfiniteTransition(label = "sweep").animateFloat(
                initialValue = -0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
                label = "sweep",
            )
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(0.3f)
                    .offset { IntOffset((sweep * constraints.maxWidth).toInt(), 0) }
                    .clip(CircleShape).background(fill),
            )
        }
    }
}

/** Tap target without a ripple (text-like affordances). */
@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
