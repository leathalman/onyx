package com.leathalenterprises.onyx.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leathalenterprises.onyx.data.ConfiguredApp
import com.leathalenterprises.onyx.data.InstalledApp

private val SelectedColor = Color.White
private val UnselectedColor = Color(0xFF6E6E6E)

private val BackStyle = TextStyle(
    color = Color.White,
    fontFamily = OnyxFontFamily,
    fontSize = 20.sp,
)

private val HeaderStyle = TextStyle(
    color = UnselectedColor,
    fontFamily = OnyxFontFamily,
    fontSize = 14.sp,
    letterSpacing = 2.sp,
)

private val RowStyle = TextStyle(
    fontFamily = OnyxFontFamily,
    fontSize = 24.sp,
)

/**
 * Full list of installed apps; tapping toggles an app in or out of the
 * home screen. Selected apps render white, others gray, with a soft fade
 * between the two states.
 */
@Composable
fun PickerScreen(
    installed: List<InstalledApp>,
    configured: List<ConfiguredApp>,
    onToggle: (InstalledApp) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)

    val selected = remember(configured) { configured.map { it.component }.toSet() }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Extra bottom padding so the last rows can scroll clear of the
            // floating back button.
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 32.dp,
                bottom = 120.dp,
            ),
        ) {
            item {
                BasicText(
                    text = "CHOOSE APPS",
                    style = HeaderStyle,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }
            items(installed, key = { it.component.flattenToString() }) { app ->
                val color by animateColorAsState(
                    targetValue = if (app.component in selected) SelectedColor else UnselectedColor,
                    animationSpec = tween(durationMillis = 150),
                    label = "selection",
                )
                BasicText(
                    text = app.label.lowercase(),
                    style = RowStyle.copy(color = color),
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressDimClickable { onToggle(app) }
                        .padding(vertical = 14.dp),
                )
            }
        }
        // Drawn after the list so rows scroll underneath it, matching the
        // clock treatment on the home screen.
        BasicText(
            text = "< back",
            style = BackStyle,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 32.dp)
                .background(Color.Black)
                .pressDimClickable(onClose)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        )
    }
}
