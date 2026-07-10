package com.leathalenterprises.onyx.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.leathalenterprises.onyx.R

/**
 * Analogue OS font (SIL OFL 1.1, see licenses/AnalogueOS-font-LICENSE.md).
 * https://github.com/AbFarid/analogue-os-font
 */
val OnyxFontFamily = FontFamily(Font(R.font.analogue_os))

/**
 * The whole launcher is exactly three text colors:
 * white = content / selected, [OnyxChrome] = utility chrome,
 * [OnyxOff] = not selected / inactive.
 */
val OnyxChrome = Color(0xFFB4B4B4)
val OnyxOff = Color(0xFF6E6E6E)
