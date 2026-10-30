package com.sahedul.clipsave.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.material3.HorizontalDivider
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
import com.sahedul.clipsave.core.Platform
import com.sahedul.clipsave.core.Prefs
import com.sahedul.clipsave.ui.theme.ClipType
import com.sahedul.clipsave.ui.theme.GlassButton
import com.sahedul.clipsave.ui.theme.GlassIconButton
import com.sahedul.clipsave.ui.theme.LocalClipColors
import com.sahedul.clipsave.ui.theme.PrimaryButton
import com.sahedul.clipsave.ui.theme.Radius
import com.sahedul.clipsave.ui.theme.panel

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
    Step(Icons.Outlined.IosShare, "Share", "In any app, tap Share and pick ClipSave."),
    Step(Icons.Outlined.ContentPaste, "Or paste", "Copy a link and open ClipSave. It shows up on Home."),
    Step(Icons.Outlined.Tune, "Choose", "Pick a quality and rename the file if you like."),
    Step(Icons.Rounded.Check, "Done", "Find it in Downloads, or in Movies/ClipSave."),
)

private class Tip(val icon: ImageVector, val title: String, val body: String)

private val TIPS = listOf(
    Tip(Icons.AutoMirrored.Outlined.PlaylistPlay, "Playlists", "A whole YouTube playlist, in order."),
    Tip(Icons.Outlined.PauseCircle, "Pause & resume", "Pick up where it stopped."),
    Tip(Icons.Outlined.FolderOpen, "Your folder", "Save anywhere you choose."),
    Tip(Icons.Outlined.Lock, "Private", "No ads. No tracking."),
)

/** Fades and lifts a block in once [shown] flips, [index] steps after the first, so the page assembles top to bottom. */
@Composable
private fun Modifier.reveal(index: Int, shown: Boolean): Modifier {
    val t by animateFloatAsState(if (shown) 1f else 0f, tween(450, delayMillis = 70 * index, easing = FastOutSlowInEasing), label = "reveal")
    return graphicsLayer {
        alpha = t
        translationY = (1f - t) * 22.dp.toPx()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HowItWorksScreen(onBack: () -> Unit) {
    val c = LocalClipColors.current
    val uri = LocalUriHandler.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    PageScaffold("How it works", onBack) {
        // One panel, one timeline: the rail makes the order obvious without numbering every card.
        Column(Modifier.fillMaxWidth().panel(RoundedCornerShape(Radius.Lg)).padding(horizontal = 18.dp, vertical = 22.dp)) {
            STEPS.forEachIndexed { i, step -> TimelineStep(step, last = i == STEPS.lastIndex, Modifier.reveal(i, shown)) }
        }

        SectionLabel("Also handy", Modifier.reveal(STEPS.size, shown))
        TIPS.chunked(2).forEachIndexed { row, pair ->
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Max).reveal(STEPS.size + 1 + row, shown),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                pair.forEach { TipTile(it, Modifier.weight(1f).fillMaxHeight()) }
            }
        }

        SectionLabel("Works with", Modifier.reveal(STEPS.size + 3, shown))
        FlowRow(
            Modifier.reveal(STEPS.size + 3, shown),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Platform.entries.filter { it != Platform.OTHER }.forEach {
                Text(it.label, style = ClipType.Label, color = c.ink, modifier = Modifier.panel(CircleShape).padding(horizontal = 14.dp, vertical = 9.dp))
            }
            Text(
                "1000+ more", style = ClipType.Label, color = c.onPrimarySoft,
                modifier = Modifier.clip(CircleShape).background(c.primarySoft).padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }

        GlassButton(
            "See details on the web", { uri.openUri("$SITE_URL/#how-it-works") },
            Modifier.fillMaxWidth().padding(top = 8.dp).reveal(STEPS.size + 4, shown),
            icon = Icons.Outlined.OpenInNew, height = 48.dp,
        )
        Text("Save only what you have the right to.", style = ClipType.Caption, color = c.inkMuted, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = ClipType.Label,
        color = LocalClipColors.current.inkMuted,
        modifier = modifier.padding(start = 4.dp, top = 12.dp),
    )
}

/** An iris badge on a rail that runs down to the next step. */
@Composable
private fun TimelineStep(step: Step, last: Boolean, modifier: Modifier = Modifier) {
    val c = LocalClipColors.current
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(listOf(c.primary, c.primaryStrong))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(step.icon, null, tint = c.onPrimary, modifier = Modifier.size(22.dp))
            }
            if (!last) {
                Box(
                    Modifier.padding(vertical = 6.dp).width(2.dp).weight(1f).clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(c.primary.copy(alpha = 0.55f), c.primary.copy(alpha = 0.12f)))),
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f).padding(top = 2.dp, bottom = if (last) 0.dp else 24.dp)) {
            Text(step.title, style = ClipType.Headline, color = c.ink)
            Spacer(Modifier.height(2.dp))
            Text(step.body, style = ClipType.BodyStrong, color = c.inkMuted)
        }
    }
}

@Composable
private fun TipTile(tip: Tip, modifier: Modifier = Modifier) {
    val c = LocalClipColors.current
    Column(modifier.panel(RoundedCornerShape(Radius.Lg)).padding(16.dp)) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(c.primarySoft), contentAlignment = Alignment.Center) {
            Icon(tip.icon, null, tint = c.onPrimarySoft, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(tip.title, style = ClipType.BodyStrong, color = c.ink)
        Spacer(Modifier.height(2.dp))
        Text(tip.body, style = ClipType.Caption, color = c.inkMuted)
    }
}

private class Term(val icon: ImageVector, val title: String, val body: String)

/** Kept short on purpose; the full text is on the website. */
private val TERMS = listOf(
    Term(Icons.Outlined.Person, "Your choice", "You choose what to save, and you are responsible for it."),
    Term(Icons.Outlined.VerifiedUser, "Only what's yours to save", "Only save content you own, that is public domain or openly licensed, or that you have permission to save."),
    Term(Icons.Outlined.Public, "Copyright and local law", "Follow copyright law, the terms of the sites you use, and the laws where you live. They differ by country."),
    Term(Icons.Outlined.Block, "No bypassing", "Don't use ClipSave to bypass DRM, paywalls or access controls, or to share other people's work without permission."),
    Term(Icons.Outlined.Info, "No warranty", "ClipSave is free software provided \"as is\", without warranty. The developers are not liable for how it is used, to the extent the law allows."),
    Term(Icons.Outlined.LinkOff, "Not affiliated", "ClipSave is not affiliated with Instagram, Facebook, YouTube, TikTok, X or any other service."),
)

/** The terms a user accepts on first launch, as a plain list; shown in the Welcome dialog. */
@Composable
fun TermsBody() {
    val c = LocalClipColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TERMS.forEach { term ->
            Row {
                Text("•", style = ClipType.BodyStrong, color = c.primary, modifier = Modifier.width(16.dp))
                Text(term.body, style = ClipType.BodyStrong, color = c.ink)
            }
        }
    }
}

/** Settings › Other › Terms: the same points as cards with a heading each, then the full text on the website. */
@Composable
fun TermsScreen(onBack: () -> Unit) {
    val c = LocalClipColors.current
    val uri = LocalUriHandler.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    PageScaffold("Terms", onBack) {
        Text(
            "The short version. By using ClipSave you agree to these.",
            style = ClipType.BodyStrong, color = c.inkMuted,
            modifier = Modifier.padding(horizontal = 4.dp).reveal(0, shown),
        )
        Column(Modifier.fillMaxWidth().panel(RoundedCornerShape(Radius.Lg)).padding(horizontal = 18.dp, vertical = 6.dp)) {
            TERMS.forEachIndexed { i, term ->
                if (i > 0) HorizontalDivider(color = c.line)
                TermRow(term, Modifier.reveal(i + 1, shown))
            }
        }
        Spacer(Modifier.height(4.dp))
        PrimaryButton(
            "Read the full terms", { uri.openUri("$SITE_URL/terms") },
            Modifier.fillMaxWidth().reveal(TERMS.size + 1, shown), icon = Icons.Outlined.OpenInNew,
        )
        GlassButton(
            "Source code & license", { uri.openUri("$SITE_URL/license") },
            Modifier.fillMaxWidth().reveal(TERMS.size + 2, shown), height = 48.dp,
        )
    }
}

@Composable
private fun TermRow(term: Term, modifier: Modifier = Modifier) {
    val c = LocalClipColors.current
    Row(modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(c.primarySoft), contentAlignment = Alignment.Center) {
            Icon(term.icon, null, tint = c.onPrimarySoft, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(term.title, style = ClipType.Headline, color = c.ink)
            Spacer(Modifier.height(2.dp))
            Text(term.body, style = ClipType.BodyStrong, color = c.inkMuted)
        }
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
