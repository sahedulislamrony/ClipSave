package com.sahed.clipsave.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sahed.clipsave.core.Platform
import com.sahed.clipsave.core.Prefs
import com.sahed.clipsave.ui.theme.ClipType
import com.sahed.clipsave.ui.theme.GlassButton
import com.sahed.clipsave.ui.theme.GlassIconButton
import com.sahed.clipsave.ui.theme.LocalClipColors
import com.sahed.clipsave.ui.theme.Radius
import com.sahed.clipsave.ui.theme.panel

/** A pushed page: glass back button, big title, scrolling body. The dock is hidden while one is open. */
@Composable
fun PageScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    val c = LocalClipColors.current
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
            GlassIconButton(Icons.Rounded.ArrowBack, "Back", onBack)
            Spacer(Modifier.height(20.dp))
            Text(title, style = ClipType.Title, color = c.ink)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }
}

private class Step(val icon: ImageVector, val title: String, val body: String)

private val STEPS = listOf(
    Step(Icons.Outlined.IosShare, "Share", "In any app, tap Share on a video and pick ClipSave."),
    Step(Icons.Outlined.ContentPaste, "Or paste", "Copy a link, open ClipSave, paste it on Home."),
    Step(Icons.Outlined.Tune, "Choose", "Pick a quality and rename the file if you like."),
    Step(Icons.Outlined.Folder, "Done", "Find it in Downloads, or in Movies/ClipSave."),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HowItWorksScreen(onBack: () -> Unit) {
    val c = LocalClipColors.current
    PageScaffold("How it works", onBack) {
        STEPS.forEachIndexed { i, step ->
            Row(
                Modifier.fillMaxWidth().panel(RoundedCornerShape(Radius.Lg)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(c.primarySoft), contentAlignment = Alignment.Center) {
                    Icon(step.icon, null, tint = c.onPrimarySoft, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("${i + 1}. ${step.title}", style = ClipType.Headline, color = c.ink)
                    Spacer(Modifier.height(2.dp))
                    Text(step.body, style = ClipType.BodyStrong, color = c.inkMuted)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Works with", style = ClipType.Headline, color = c.ink)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (Platform.entries.filter { it != Platform.OTHER }.map { it.label } + "1000+ more").forEach {
                Text(
                    it, style = ClipType.Label, color = c.ink,
                    modifier = Modifier.panel(CircleShape).padding(horizontal = 14.dp, vertical = 9.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Save only what you have the right to.", style = ClipType.Caption, color = c.inkMuted)
    }
}

private const val REPO_URL = "https://github.com/sahedulislamrony/ClipSave"

/** Kept short on purpose; the full text is TERMS.md in the repository. */
private val TERMS = listOf(
    "You choose what to save, and you are responsible for it.",
    "Only save content you own, that is public domain or openly licensed, or that you have permission to save.",
    "Follow copyright law, the terms of the sites you use, and the laws where you live. They differ by country.",
    "Don't use ClipSave to bypass DRM, paywalls or access controls, or to share other people's work without permission.",
    "ClipSave is free software provided \"as is\", without warranty. The developers are not liable for how it is used, to the extent the law allows.",
    "ClipSave is not affiliated with Instagram, Facebook, YouTube, TikTok, X or any other service.",
)

/** The terms a user accepts on first launch; shown on Welcome and under Settings › Other › Terms. */
@Composable
fun TermsBody() {
    val c = LocalClipColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TERMS.forEach { line ->
            Row {
                Text("•", style = ClipType.BodyStrong, color = c.primary, modifier = Modifier.width(16.dp))
                Text(line, style = ClipType.BodyStrong, color = c.ink)
            }
        }
    }
}

@Composable
fun TermsScreen(onBack: () -> Unit) {
    val uri = LocalUriHandler.current
    PageScaffold("Terms", onBack) {
        TermsBody()
        Spacer(Modifier.height(8.dp))
        GlassButton("Full terms", { uri.openUri("$REPO_URL/blob/main/TERMS.md") }, Modifier.fillMaxWidth(), height = 48.dp)
        GlassButton("Source code & license", { uri.openUri(REPO_URL) }, Modifier.fillMaxWidth(), height = 48.dp)
    }
}

/** Light · Dark · System, each with a mini preview; the selected one gets an iris ring and a tick. */
@Composable
fun ThemeScreen(prefs: Prefs, onBack: () -> Unit) {
    val c = LocalClipColors.current
    val selected by prefs.themeFlow.collectAsStateWithLifecycle()
    PageScaffold("Theme", onBack) {
        listOf(Prefs.THEME_LIGHT to "Light", Prefs.THEME_DARK to "Dark", Prefs.THEME_SYSTEM to "System").forEach { (key, label) ->
            val active = selected == key
            val shape = RoundedCornerShape(Radius.Lg)
            Row(
                Modifier.fillMaxWidth().clip(shape)
                    .background(if (active) c.primarySoft else c.panel)
                    .then(if (active) Modifier.border(2.dp, c.primary, shape) else Modifier)
                    .clickable { prefs.theme = key }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(width = 76.dp, height = 56.dp).clip(RoundedCornerShape(Radius.Sm)).border(1.dp, c.line, RoundedCornerShape(Radius.Sm))) {
                    when (key) {
                        Prefs.THEME_LIGHT -> MiniPreview(Color.White, Color(0xFFECECEF), Color(0xFF5B3BF0))
                        Prefs.THEME_DARK -> MiniPreview(Color(0xFF0A0A0D), Color(0xFF2A2A31), Color(0xFF9C88FF))
                        else -> Row(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(1f).fillMaxHeight()) { MiniPreview(Color.White, Color(0xFFECECEF), Color(0xFF5B3BF0)) }
                            Box(Modifier.weight(1f).fillMaxHeight()) { MiniPreview(Color(0xFF0A0A0D), Color(0xFF2A2A31), Color(0xFF9C88FF)) }
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                Text(label, style = ClipType.Headline, color = c.ink, modifier = Modifier.weight(1f))
                if (active) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(c.primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, null, tint = c.onPrimary, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPreview(bg: Color, bar: Color, accent: Color) {
    Column(Modifier.fillMaxSize().background(bg).padding(7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.fillMaxWidth(0.5f).height(4.dp).clip(CircleShape).background(bar))
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(bar))
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(accent))
    }
}
