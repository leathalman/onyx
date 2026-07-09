package com.leathalenterprises.onyx.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leathalenterprises.onyx.data.ConfiguredApp
import kotlinx.coroutines.delay
import java.util.Date

private const val APPS_PER_PAGE = 7

val HomeLabelStyle = TextStyle(
    color = Color.White,
    fontFamily = OnyxFontFamily,
    fontSize = 32.sp,
    letterSpacing = 0.5.sp,
)

private val ClockStyle = TextStyle(
    color = Color.White,
    fontFamily = OnyxFontFamily,
    fontSize = 18.sp,
    letterSpacing = 1.sp,
)

/** One row on the home screen; a null [app] is the built-in Onyx entry. */
private data class HomeEntry(val label: String, val app: ConfiguredApp?)

/**
 * The home screen: a clock up top and pages of up to [APPS_PER_PAGE] centered
 * app labels, swiped horizontally, with a page indicator on the right edge.
 * An "Onyx" entry is always appended after the configured apps and opens
 * settings. [apps] is null while the saved configuration is still loading.
 */
@Composable
fun HomeScreen(
    apps: List<ConfiguredApp>?,
    onLaunch: (ConfiguredApp) -> Unit,
    onOpenSettings: () -> Unit,
) {
    if (apps == null) return

    val pages = remember(apps) {
        (apps.map { HomeEntry(it.label, it) } + HomeEntry("Onyx", null))
            .chunked(APPS_PER_PAGE)
    }
    val pagerState = rememberPagerState { pages.size }

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    pages[page].forEach { entry ->
                        OnyxLabel(
                            text = entry.label,
                            style = HomeLabelStyle,
                            onClick = { entry.app?.let(onLaunch) ?: onOpenSettings() },
                        )
                    }
                }
            }
        }
        // Drawn after the pager so pages scroll underneath it, not through it.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Clock(modifier = Modifier.padding(top = 24.dp, bottom = 16.dp))
        }
        if (pages.size > 1) {
            PageIndicator(
                pageCount = pages.size,
                current = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
            )
        }
    }
}

@Composable
private fun Clock(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val formatter = remember { DateFormat.getTimeFormat(context) }
    var time by remember { mutableStateOf(formatter.format(Date())) }
    LaunchedEffect(Unit) {
        while (true) {
            time = formatter.format(Date())
            delay(60_000L - (System.currentTimeMillis() % 60_000L) + 100)
        }
    }
    BasicText(text = time, style = ClockStyle, modifier = modifier)
}

@Composable
private fun PageIndicator(pageCount: Int, current: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(pageCount) { page ->
            val dot = Modifier.size(9.dp)
            Box(
                modifier = if (page == current) {
                    dot.background(Color.White, CircleShape)
                } else {
                    dot.border(1.dp, Color(0xFF8A8A8A), CircleShape)
                },
            )
        }
    }
}

/** A tappable text label that dims while pressed. */
@Composable
fun OnyxLabel(text: String, style: TextStyle, onClick: () -> Unit) {
    BasicText(
        text = text,
        style = style,
        modifier = Modifier
            .pressDimClickable(onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
    )
}
