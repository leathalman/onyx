package com.leathalenterprises.onyx.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Onyx's only press feedback: no ripple, just a quick dim while touched.
 * Used for every tappable thing in the launcher so the feel is consistent.
 */
@Composable
fun Modifier.pressDimClickable(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue = if (pressed) 0.4f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "pressDim",
    )
    return this
        .graphicsLayer { this.alpha = alpha }
        .clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick,
        )
}
