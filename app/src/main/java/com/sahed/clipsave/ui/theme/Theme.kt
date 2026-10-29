package com.sahed.clipsave.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sahed.clipsave.ClipSaveApp
import com.sahed.clipsave.core.Prefs

private fun ClipColors.toMaterial() = if (dark) {
    darkColorScheme(
        primary = primary, onPrimary = onPrimary, primaryContainer = primarySoft, onPrimaryContainer = onPrimarySoft,
        secondary = inkMuted, background = bg, onBackground = ink, surface = surface, onSurface = ink,
        surfaceVariant = surfaceSunken, onSurfaceVariant = inkMuted, surfaceContainer = surface,
        surfaceContainerHigh = surface, surfaceContainerHighest = surfaceSunken,
        outline = inkFaint, outlineVariant = line, error = danger, scrim = scrim,
    )
} else {
    lightColorScheme(
        primary = primary, onPrimary = onPrimary, primaryContainer = primarySoft, onPrimaryContainer = onPrimarySoft,
        secondary = inkMuted, background = bg, onBackground = ink, surface = surface, onSurface = ink,
        surfaceVariant = surfaceSunken, onSurfaceVariant = inkMuted, surfaceContainer = surface,
        surfaceContainerHigh = surface, surfaceContainerHighest = surfaceSunken,
        outline = inkFaint, outlineVariant = line, error = danger, scrim = scrim,
    )
}

private val ClipTypography = Typography().copy(
    headlineSmall = ClipType.Title,
    titleLarge = ClipType.Headline,
    titleMedium = ClipType.Headline,
    titleSmall = ClipType.BodyStrong,
    bodyLarge = ClipType.BodyStrong,
    bodyMedium = ClipType.BodyStrong,
    bodySmall = ClipType.Caption,
    labelLarge = ClipType.BodyStrong,
    labelMedium = ClipType.Label,
    labelSmall = ClipType.Caption,
)

@Composable
fun ClipSaveTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = (context.applicationContext as ClipSaveApp).container.prefs
    val mode by prefs.themeFlow.collectAsStateWithLifecycle()
    val dark = when (mode) {
        Prefs.THEME_DARK -> true
        Prefs.THEME_SYSTEM -> isSystemInDarkTheme()
        else -> false
    }
    val colors = if (dark) DarkClipColors else LightClipColors

    // Status/navigation bar icons follow the app theme, not the system one.
    val activity = context.findActivity() as? ComponentActivity
    DisposableEffect(activity, dark) {
        activity?.enableEdgeToEdge(
            statusBarStyle = if (dark) SystemBarStyle.dark(AndroidColor.TRANSPARENT) else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = if (dark) SystemBarStyle.dark(AndroidColor.TRANSPARENT) else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
        onDispose {}
    }

    CompositionLocalProvider(LocalClipColors provides colors, LocalContentColor provides colors.ink) {
        MaterialTheme(colorScheme = colors.toMaterial(), typography = ClipTypography, content = content)
    }
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
