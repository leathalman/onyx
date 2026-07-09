package com.leathalenterprises.onyx.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val FUZZ_FRAME_MS = 55L
private const val RESOLVE_FRAME_MS = 35L
// Lowercase only: every label in the launcher renders lowercase.
private val GLYPHS: List<Char> = ('a'..'z').toList()

/**
 * A home-screen label that scrambles while its text is being generated and
 * decodes left-to-right into the final label when it lands. When [fuzzing]
 * is false and the text hasn't changed, renders as plain static text — the
 * animation only ever appears around a labeling pass. Stays tappable the
 * whole time.
 */
@Composable
fun FuzzLabel(
    text: String,
    fuzzing: Boolean,
    style: TextStyle,
    onClick: () -> Unit,
) {
    var display by remember { mutableStateOf(text) }

    LaunchedEffect(fuzzing, text) {
        if (fuzzing) {
            while (isActive) {
                display = scramble(text)
                delay(FUZZ_FRAME_MS)
            }
        } else if (display != text) {
            for (locked in 0..text.length) {
                display = text.take(locked) + scramble(text.drop(locked))
                delay(RESOLVE_FRAME_MS)
            }
            display = text
        } else {
            display = text
        }
    }

    BasicText(
        text = display,
        style = style,
        modifier = Modifier
            .pressDimClickable(onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
    )
}

private fun scramble(source: String): String =
    source.map { if (it == ' ') ' ' else GLYPHS.random() }.joinToString("")
