package com.sahed.clipsave.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.sahed.clipsave.R
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Liquid glass: translucent fill, soft-light diagonal sheen, a 1px rim that is brightest at the
 * top (the specular line), and a floating shadow. With [haze] the content behind is blurred
 * (backdrop-filter style) under a denser frosted tint — for surfaces that float over scrolling
 * content, like the dock; without it the translucent fill alone carries the material.
 */
@Composable
fun Modifier.glass(shape: Shape = CircleShape, shadow: Boolean = true, haze: HazeState? = null): Modifier {
    val c = LocalClipColors.current
    val sheen = Brush.linearGradient(listOf(Color.White.copy(alpha = if (c.dark) 0.07f else 0.35f), Color.Transparent))
    val rim = Brush.verticalGradient(listOf(c.glassHighlight, c.glassEdge))
    return this
        .then(if (shadow) Modifier.shadow(14.dp, shape, ambientColor = c.shadowGlass, spotColor = c.shadowGlass) else Modifier)
        .clip(shape)
        .then(
            if (haze != null) {
                Modifier.hazeEffect(haze, HazeStyle(backgroundColor = c.bg, tint = HazeTint(c.glassFrosted), blurRadius = 24.dp, noiseFactor = 0f))
            } else {
                Modifier.background(c.glass)
            },
        )
        .background(sheen)
        .border(1.dp, rim, shape)
}

/** Page backdrop: iris light pooling from the top-left and right edge, fading to the base colour. */
@Composable
fun Modifier.clipBackground(): Modifier {
    val c = LocalClipColors.current
    val iris = if (c.dark) Color(0xFF5B3BF0) else c.primary
    val top = if (c.dark) 0.55f else 0.34f
    val mid = if (c.dark) 0.35f else 0.20f
    val low = if (c.dark) 0.25f else 0.12f
    return this.fillMaxSize().background(c.bg).drawBehind {
        drawRect(Brush.radialGradient(listOf(iris.copy(alpha = top), Color.Transparent), center = Offset(size.width * 0.1f, 0f), radius = size.width * 1.25f))
        drawRect(Brush.radialGradient(listOf(iris.copy(alpha = mid), Color.Transparent), center = Offset(size.width * 1.05f, size.height * 0.52f), radius = size.width * 1.0f))
        drawRect(Brush.radialGradient(listOf(iris.copy(alpha = low), Color.Transparent), center = Offset(size.width * 0.05f, size.height * 0.98f), radius = size.width * 0.9f))
    }
}

/** Translucent rounded card with a hairline rim; lists, fields and tiles sit on this. */
@Composable
fun Modifier.panel(shape: Shape): Modifier {
    val c = LocalClipColors.current
    return this.clip(shape).background(c.panel).border(1.dp, c.glassEdge.copy(alpha = if (c.dark) 0.08f else 0.9f), shape)
}

/** The one primary action on a screen: solid iris pill, top highlight, iris glow. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val c = LocalClipColors.current
    val glow = c.primary.copy(alpha = if (c.dark) 0.45f else 0.6f)
    Row(
        modifier
            .height(56.dp)
            .then(if (enabled) Modifier.shadow(18.dp, CircleShape, ambientColor = glow, spotColor = glow) else Modifier)
            .clip(CircleShape)
            .background(if (enabled) c.primary else c.panel)
            .then(
                if (enabled) Modifier.border(1.dp, Brush.verticalGradient(listOf(c.glassHighlight.copy(alpha = 0.5f), Color.Transparent)), CircleShape)
                else Modifier,
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (enabled) c.onPrimary else c.inkFaint
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
            Box(Modifier.width(8.dp))
        }
        Text(text, style = ClipType.BodyStrong, color = fg)
    }
}

/** Every other button: glass pill. */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 40.dp,
    shadow: Boolean = true,
) {
    val c = LocalClipColors.current
    Row(
        modifier
            .height(height)
            .glass(CircleShape, shadow)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (enabled) c.ink else c.inkFaint
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
            Box(Modifier.width(6.dp))
        }
        Text(text, style = ClipType.Label, color = fg)
    }
}

/** Round glass icon button (paste, close, new download…). */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = LocalClipColors.current.ink,
    shadow: Boolean = true,
) {
    Box(
        modifier
            .size(size)
            .glass(CircleShape, shadow)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(if (size >= 48.dp) 20.dp else 18.dp))
    }
}

class SegOption<T>(val value: T, val label: String, val icon: ImageVector? = null)

/**
 * Segmented control. [glass] sits over content (Downloads filters); the solid variant sits inside
 * sheets. The active segment is a lens capsule, never iris.
 */
@Composable
fun <T> Segmented(
    options: List<SegOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    glass: Boolean = false,
    fill: Boolean = false,
    track: Color = LocalClipColors.current.surfaceSunken,
    itemPadding: Dp = 20.dp,
) {
    val c = LocalClipColors.current
    Row(
        modifier
            .then(if (fill) Modifier.fillMaxWidth() else Modifier)
            .then(if (glass) Modifier.glass(CircleShape) else Modifier.clip(CircleShape).background(track))
            .padding(4.dp),
    ) {
        options.forEach { opt ->
            val active = opt.value == selected
            val segShape = CircleShape
            Row(
                Modifier
                    .then(if (fill) Modifier.weight(1f) else Modifier)
                    .height(44.dp)
                    .then(
                        if (active) {
                            if (glass) Modifier.clip(segShape).background(c.glassLens)
                            else Modifier.shadow(2.dp, segShape).clip(segShape).background(c.surface)
                        } else Modifier.clip(segShape),
                    )
                    .clickable(role = Role.Tab) { onSelect(opt.value) }
                    .padding(horizontal = itemPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val fg = if (active) c.ink else c.inkMuted
                if (opt.icon != null) {
                    Icon(opt.icon, null, tint = fg, modifier = Modifier.size(16.dp))
                    Box(Modifier.width(8.dp))
                }
                Text(opt.label, style = ClipType.BodyStrong, color = fg, maxLines = 1)
            }
        }
    }
}

/** The mark: the two-card glyph on a tile that is near-white in light and near-black in dark, like the launcher icon. */
@Composable
fun AppMark(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val c = LocalClipColors.current
    val shape = RoundedCornerShape(size * 0.3f)
    Box(
        modifier.size(size).clip(shape)
            .background(if (c.dark) Color(0xFF0B0A0F) else Color(0xFFFAF8FF))
            .border(1.dp, if (c.dark) c.primary.copy(alpha = 0.45f) else c.line, shape),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.logo_glyph), "ClipSave", Modifier.size(size * 0.66f))
    }
}
