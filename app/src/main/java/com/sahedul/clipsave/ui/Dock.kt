package com.sahedul.clipsave.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sahedul.clipsave.ui.theme.ClipType
import com.sahedul.clipsave.ui.theme.LocalClipColors
import com.sahedul.clipsave.ui.theme.glass
import dev.chrisbanes.haze.HazeState

/** Space content needs at the bottom so it can scroll under the floating dock. */
val DockClearance = 116.dp

/**
 * Floating liquid-glass dock: a pill tab bar, 16dp from the sides and 26dp above the bottom.
 * It blurs the screen behind it ([haze] is the screen's source) so labels stay readable over content.
 */
@Composable
fun Dock(
    tab: Int,
    running: Boolean,
    onTab: (Int) -> Unit,
    haze: HazeState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 26.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f).glass(CircleShape, haze = haze).padding(6.dp)) {
            DockTab(tab == MainActivity.TAB_HOME, Icons.Outlined.Home, "Home", false) { onTab(MainActivity.TAB_HOME) }
            DockTab(tab == MainActivity.TAB_DOWNLOADS, Icons.Outlined.Download, "Downloads", running) { onTab(MainActivity.TAB_DOWNLOADS) }
            DockTab(tab == MainActivity.TAB_SETTINGS, Icons.Outlined.Tune, "Settings", false) { onTab(MainActivity.TAB_SETTINGS) }
        }
    }
}

@Composable
private fun RowScope.DockTab(selected: Boolean, icon: ImageVector, label: String, dot: Boolean, onClick: () -> Unit) {
    val c = LocalClipColors.current
    Column(
        Modifier
            .weight(1f)
            .clip(CircleShape)
            .then(if (selected) Modifier.background(c.glassLens) else Modifier)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Tab, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val tint = if (selected) c.primary else c.ink
        Box {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            if (dot) Box(Modifier.align(Alignment.TopEnd).size(8.dp).clip(CircleShape).background(c.primary))
        }
        Text(label, style = ClipType.Tab, color = tint)
    }
}
