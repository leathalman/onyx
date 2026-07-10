package com.leathalenterprises.onyx.ui

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
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
private const val HINT_STEP_MS = 750L
private const val HINT_WAVE_PAUSE_MS = 750L

val HomeLabelStyle = TextStyle(
    color = Color.White,
    fontFamily = OnyxFontFamily,
    fontSize = 32.sp,
    letterSpacing = 0.5.sp,
)

private val ClockStyle = TextStyle(
    color = OnyxChrome,
    fontFamily = OnyxFontFamily,
    fontSize = 26.sp,
)

private val SettingsButtonStyle = TextStyle(
    color = OnyxChrome,
    fontFamily = OnyxFontFamily,
    fontSize = 26.sp,
)

/**
 * The home screen: a clock up top, pages of up to [APPS_PER_PAGE] centered
 * app labels sorted A-Z and swiped vertically, a page indicator on the right
 * edge, and a fixed "onyx" button at the bottom right that opens settings
 * (mirroring the picker's back button). [apps] is null while the saved
 * configuration is still loading.
 */
@Composable
fun HomeScreen(
    apps: List<ConfiguredApp>?,
    resetSignal: Int,
    onLaunch: (ConfiguredApp) -> Unit,
    onOpenSettings: () -> Unit,
) {
    if (apps == null) return

    // Swallow the system back gesture: left/right edge swipes must do
    // nothing on a home screen. (Android ignores back on the *default*
    // launcher anyway; this makes Onyx behave the same before it is one.)
    BackHandler {}

    val pages = remember(apps) {
        apps.sortedBy { it.label.lowercase() }.chunked(APPS_PER_PAGE)
    }
    val pagerState = rememberPagerState { pages.size }

    // The home gesture means "take me to the top": snap to the first page
    // whenever the HOME intent is re-delivered.
    LaunchedEffect(resetSignal) {
        pagerState.scrollToPage(0)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    pages[page].forEach { app ->
                        AppLabel(
                            text = app.label.lowercase(),
                            onClick = { onLaunch(app) },
                        )
                    }
                }
            }
        }
        // Overlays are drawn after the pager (with black backing) so pages
        // scroll underneath them, not through them.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Clock(modifier = Modifier.padding(top = 24.dp, bottom = 16.dp))
        }
        SettingsButton(
            showHint = apps.isEmpty(),
            onClick = onOpenSettings,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
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

/** A tappable home-screen app label that dims while pressed. */
@Composable
private fun AppLabel(text: String, onClick: () -> Unit) {
    BasicText(
        text = text,
        style = HomeLabelStyle,
        modifier = Modifier
            .pressDimClickable(onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
    )
}

/**
 * The fixed settings button. On a fresh install (no apps configured yet) the
 * home screen is otherwise blank, so chevrons march toward the word —
 * `> onyx`, `>> onyx`, `>>> onyx` — echoing the picker's `< back` grammar.
 * Once anything is configured it renders as plain "onyx".
 */
@Composable
private fun SettingsButton(
    showHint: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var chevrons by remember { mutableStateOf(1) }
    if (showHint) {
        LaunchedEffect(Unit) {
            while (true) {
                chevrons = 1
                delay(HINT_STEP_MS)
                chevrons = 2
                delay(HINT_STEP_MS)
                chevrons = 3
                delay(HINT_STEP_MS + HINT_WAVE_PAUSE_MS)
            }
        }
    }
    BasicText(
        text = if (showHint) ">".repeat(chevrons) + " onyx" else "onyx",
        style = SettingsButtonStyle,
        modifier = modifier
            .padding(end = 24.dp, bottom = 32.dp)
            .background(Color.Black)
            .pressDimClickable(onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    )
}

@Composable
private fun Clock(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val formatter = remember { DateFormat.getTimeFormat(context) }
    var time by remember { mutableStateOf(formatter.format(Date()).lowercase()) }
    LaunchedEffect(Unit) {
        while (true) {
            time = formatter.format(Date()).lowercase()
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
                    dot.border(1.dp, OnyxOff, CircleShape)
                },
            )
        }
    }
}
