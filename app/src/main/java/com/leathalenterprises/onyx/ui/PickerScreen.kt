package com.leathalenterprises.onyx.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leathalenterprises.onyx.data.ConfiguredApp
import com.leathalenterprises.onyx.data.InstalledApp

private val HeaderStyle = TextStyle(
    color = Color(0xFF6E6E6E),
    fontSize = 13.sp,
    letterSpacing = 2.sp,
)

private val SelectedStyle = TextStyle(
    color = Color.White,
    fontSize = 20.sp,
)

private val UnselectedStyle = TextStyle(
    color = Color(0xFF6E6E6E),
    fontSize = 20.sp,
)

/**
 * Full list of installed apps; tapping toggles an app in or out of the
 * home screen. Selected apps render white, others gray.
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
    ) {
        item {
            BasicText(
                text = "CHOOSE APPS",
                style = HeaderStyle,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
        items(installed, key = { it.component.flattenToString() }) { app ->
            BasicText(
                text = app.label,
                style = if (app.component in selected) SelectedStyle else UnselectedStyle,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onToggle(app) }
                    .padding(vertical = 10.dp),
            )
        }
    }
}
