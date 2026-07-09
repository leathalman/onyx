package com.leathalenterprises.onyx.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leathalenterprises.onyx.data.ConfiguredApp

val HomeLabelStyle = TextStyle(
    color = Color.White,
    fontSize = 28.sp,
    letterSpacing = 0.5.sp,
)

/**
 * The home screen: a centered column of app labels, or a single "Configure"
 * label when nothing is set up yet. Long-press anywhere opens the picker.
 * [apps] is null while the saved configuration is still loading.
 */
@Composable
fun HomeScreen(
    apps: List<ConfiguredApp>?,
    onLaunch: (ConfiguredApp) -> Unit,
    onOpenPicker: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onOpenPicker() })
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            apps == null -> Unit
            apps.isEmpty() -> OnyxLabel(
                text = "Configure",
                style = HomeLabelStyle,
                onClick = onOpenPicker,
            )
            else -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                apps.forEach { app ->
                    OnyxLabel(
                        text = app.label,
                        style = HomeLabelStyle,
                        onClick = { onLaunch(app) },
                    )
                }
            }
        }
    }
}

/** A tappable text label with no ripple or press indication. */
@Composable
fun OnyxLabel(text: String, style: TextStyle, onClick: () -> Unit) {
    BasicText(
        text = text,
        style = style,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp, vertical = 8.dp),
    )
}
