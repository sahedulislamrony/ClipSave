package com.sahedul.clipsave.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Colour tokens from docs/design-system/tokens.json — Light is the default, Dark is opt-in. */
@Immutable
class ClipColors(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceSunken: Color,
    val line: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val primary: Color,
    val primaryStrong: Color,
    val primarySoft: Color,
    val onPrimary: Color,
    val onPrimarySoft: Color,
    val glass: Color,
    val glassEdge: Color,
    val glassHighlight: Color,
    val glassLens: Color,
    /** Tint over the blurred backdrop on floating glass (the dock); dense enough to keep labels legible. */
    val glassFrosted: Color,
    val danger: Color,
    val scrim: Color,
    val shadowGlass: Color,
    /** Translucent card fill for lists and fields, so the page gradient shows through. */
    val panel: Color,
)

private fun rgba(r: Int, g: Int, b: Int, a: Float) = Color(r, g, b, (a * 255).toInt())

val LightClipColors = ClipColors(
    dark = false,
    bg = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFF4F4F6),
    line = Color(0xFFECECEF),
    ink = Color(0xFF0A0A0F),
    inkMuted = Color(0xFF6B6B76),
    inkFaint = Color(0xFFA1A1AA),
    primary = Color(0xFF5B3BF0),
    primaryStrong = Color(0xFF4A2BD0),
    primarySoft = Color(0xFFEFEBFF),
    onPrimary = Color(0xFFFFFFFF),
    onPrimarySoft = Color(0xFF4A2BD0),
    glass = rgba(255, 255, 255, 0.58f),
    glassEdge = rgba(255, 255, 255, 0.75f),
    glassHighlight = rgba(255, 255, 255, 0.95f),
    glassLens = rgba(10, 10, 15, 0.06f),
    glassFrosted = rgba(255, 255, 255, 0.74f),
    danger = Color(0xFFD92D20),
    scrim = rgba(10, 10, 15, 0.28f),
    shadowGlass = rgba(20, 16, 50, 0.22f),
    panel = rgba(255, 255, 255, 0.72f),
)

val DarkClipColors = ClipColors(
    dark = true,
    bg = Color(0xFF0A0A0D),
    surface = Color(0xFF16161B),
    surfaceSunken = Color(0xFF1E1E24),
    line = Color(0xFF2A2A31),
    ink = Color(0xFFF5F5F7),
    inkMuted = Color(0xFF9A9AA5),
    inkFaint = Color(0xFF5E5E68),
    primary = Color(0xFF9C88FF),
    primaryStrong = Color(0xFFB8A9FF),
    primarySoft = Color(0xFF231A4A),
    onPrimary = Color(0xFF120A33),
    onPrimarySoft = Color(0xFFC9BEFF),
    glass = rgba(28, 28, 34, 0.55f),
    glassEdge = rgba(255, 255, 255, 0.12f),
    glassHighlight = rgba(255, 255, 255, 0.22f),
    glassLens = rgba(255, 255, 255, 0.10f),
    glassFrosted = rgba(22, 22, 28, 0.74f),
    danger = Color(0xFFFF7A70),
    scrim = rgba(0, 0, 0, 0.55f),
    shadowGlass = rgba(0, 0, 0, 0.8f),
    panel = rgba(255, 255, 255, 0.07f),
)

val LocalClipColors = staticCompositionLocalOf { LightClipColors }

/** radius-* tokens. Everything interactive is a pill. */
object Radius {
    val Sm = 12.dp
    val Md = 18.dp
    val Lg = 24.dp
    val Xl = 34.dp
}

/** Spacing tokens (space-1 … space-8). */
object Space {
    val S1 = 4.dp
    val S2 = 8.dp
    val S3 = 12.dp
    val S4 = 16.dp
    val S5 = 20.dp
    val S6 = 24.dp
    val S8 = 32.dp
}

/** Type scale. Plus Jakarta Sans isn't bundled, so it falls back to the system sans. */
object ClipType {
    private val Sans = FontFamily.SansSerif

    val Display = TextStyle(fontFamily = Sans, fontSize = 40.sp, lineHeight = 42.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.035).em)
    val Title = TextStyle(fontFamily = Sans, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.03).em)
    val Headline = TextStyle(fontFamily = Sans, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.015).em)
    val BodyStrong = TextStyle(fontFamily = Sans, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em)
    val Label = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
    val Caption = TextStyle(fontFamily = Sans, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    val Tab = TextStyle(fontFamily = Sans, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold)
}
