package com.sahedul.clipsave.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sahedul.clipsave.core.UrlParser
import com.sahedul.clipsave.ui.theme.AppMark
import com.sahedul.clipsave.ui.theme.ClipType
import com.sahedul.clipsave.ui.theme.GlassButton
import com.sahedul.clipsave.ui.theme.GlassIconButton
import com.sahedul.clipsave.ui.theme.LocalClipColors
import com.sahedul.clipsave.ui.theme.PrimaryButton

@Composable
fun HomeScreen(
    vm: ExtractViewModel,
    insets: PaddingValues,
    text: String,
    onText: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onStarted: () -> Unit,
    onHowItWorks: () -> Unit,
) {
    val c = LocalClipColors.current
    val context = LocalContext.current
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var lastClip by rememberSaveable { mutableStateOf("") }
    val focused = LocalWindowInfo.current.isWindowFocused

    // Copy a link anywhere, open the app: pick up the copied link once the window is focused.
    LaunchedEffect(focused) {
        if (!focused) return@LaunchedEffect
        val clip = readClipboardUrl(context)
        if (clip != null && clip != lastClip && text.isBlank()) {
            onText(clip)
            lastClip = clip
        }
    }

    fun submit() {
        val url = UrlParser.firstUrl(text) ?: return
        vm.load(url)
        sheetOpen = true
    }

    // Everything sits in the middle of the screen: field, action, then a break and the help link.
    Box(
        Modifier.fillMaxSize().padding(bottom = insets.calculateBottomPadding()).statusBarsPadding().imePadding().padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppMark(size = 76.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                buildAnnotatedString {
                    append("Clip")
                    withStyle(SpanStyle(color = c.primary)) { append("Save") }
                },
                style = ClipType.Display,
                color = c.ink,
            )
            Spacer(Modifier.height(6.dp))
            Text("Freedom of saving any clip", style = ClipType.BodyStrong, color = c.inkMuted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            LinkField(text, onText, onGo = ::submit, onPaste = { readClipboardUrl(context)?.let(onText) })
            Spacer(Modifier.height(10.dp))
            PrimaryButton("Download", ::submit, Modifier.fillMaxWidth(), icon = Icons.Rounded.Download, enabled = UrlParser.firstUrl(text) != null)
            Spacer(Modifier.height(32.dp))
            HorizontalDivider(color = c.line)
            Spacer(Modifier.height(24.dp))
            GlassButton("How it works", onHowItWorks, icon = Icons.Outlined.HelpOutline, height = 48.dp)
        }
    }

    if (sheetOpen) {
        ExtractSheet(
            vm = vm,
            onDismiss = {
                val wasStarted = vm.state.value is ExtractState.Started
                sheetOpen = false
                vm.reset()
                if (wasStarted) { onText(""); onStarted() }
            },
            onOpenSettings = { sheetOpen = false; vm.reset(); onOpenSettings() },
        )
    }
}

/** Pill link field with a glass paste button. Focus: surface fill with a 2dp iris ring. */
@Composable
private fun LinkField(value: String, onValue: (String) -> Unit, onGo: () -> Unit, onPaste: () -> Unit) {
    val c = LocalClipColors.current
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(CircleShape)
            .background(if (focused) c.surface else c.panel)
            .then(if (focused) Modifier.border(2.dp, c.primary, CircleShape) else Modifier)
            .padding(start = 18.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Link, null, tint = c.inkMuted, modifier = Modifier.padding(end = 12.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text("Link", style = ClipType.BodyStrong, color = c.inkFaint)
            BasicTextField(
                value = value,
                onValueChange = onValue,
                singleLine = true,
                textStyle = ClipType.BodyStrong.copy(color = c.ink),
                cursorBrush = SolidColor(c.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onGo() }),
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            )
        }
        if (value.isNotEmpty()) {
            GlassIconButton(Icons.Rounded.Close, "Clear", { onValue("") }, size = 44.dp, shadow = false)
        } else {
            GlassIconButton(Icons.Outlined.ContentPaste, "Paste", onPaste, size = 44.dp, shadow = false)
        }
    }
}

fun readClipboardUrl(context: Context): String? {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val item = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0) ?: return null
    return UrlParser.firstUrl(item.coerceToText(context))
}
