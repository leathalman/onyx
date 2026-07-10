package com.leathalenterprises.onyx

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.leathalenterprises.onyx.data.AppsRepository
import com.leathalenterprises.onyx.data.ConfiguredApp
import com.leathalenterprises.onyx.data.ConfiguredAppsStore
import com.leathalenterprises.onyx.data.StatusMonitor
import com.leathalenterprises.onyx.ui.HomeScreen
import com.leathalenterprises.onyx.ui.PickerScreen
import android.graphics.Color as AndroidColor

enum class Screen { Home, Picker }

class MainActivity : ComponentActivity() {

    private var screen by mutableStateOf(Screen.Home)
    private var homeResetSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        // No system status bar on launcher screens; Onyx draws its own
        // status readout. Swiping down from the top reveals it transiently.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.statusBars())
        }
        val repository = AppsRepository(applicationContext)
        val store = ConfiguredAppsStore(applicationContext)
        val statusMonitor = StatusMonitor(applicationContext)
        setContent {
            OnyxApp(
                screen = screen,
                homeResetSignal = homeResetSignal,
                repository = repository,
                store = store,
                statusMonitor = statusMonitor,
                onScreenChange = { screen = it },
            )
        }
    }

    // The home gesture re-delivers the HOME intent to this singleTask
    // activity; snap back to the home screen's first page.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        screen = Screen.Home
        homeResetSignal++
    }
}

@Composable
private fun OnyxApp(
    screen: Screen,
    homeResetSignal: Int,
    repository: AppsRepository,
    store: ConfiguredAppsStore,
    statusMonitor: StatusMonitor,
    onScreenChange: (Screen) -> Unit,
) {
    val configured by store.apps.collectAsState(initial = null)
    val installed by repository.installedApps.collectAsState(initial = emptyList())
    val battery by statusMonitor.battery.collectAsState(initial = null)
    val signalAlert by statusMonitor.signalAlert.collectAsState(initial = null)

    // Show live labels from the installed list, and hide configured apps
    // that have since been uninstalled (once the installed list has
    // actually loaded). The stored label is just a startup fallback.
    val visible = configured?.let { apps ->
        if (installed.isEmpty()) apps
        else apps.mapNotNull { app ->
            installed.firstOrNull { it.component == app.component }
                ?.let { app.copy(label = it.label) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
            // The hidden status bar no longer reserves the camera-cutout
            // region; pad for the cutout itself so nothing renders under it.
            .displayCutoutPadding(),
    ) {
        Crossfade(
            targetState = screen,
            animationSpec = tween(durationMillis = 180),
            label = "screen",
        ) { target ->
            when (target) {
                Screen.Home -> HomeScreen(
                    apps = visible,
                    resetSignal = homeResetSignal,
                    battery = battery,
                    connectivity = signalAlert,
                    onLaunch = { repository.launch(it.component) },
                    onOpenSettings = { onScreenChange(Screen.Picker) },
                )
                Screen.Picker -> PickerScreen(
                    installed = installed,
                    configured = configured.orEmpty(),
                    onToggle = { app ->
                        val currentApps = configured.orEmpty()
                        val without = currentApps.filterNot { it.component == app.component }
                        val updated =
                            if (without.size < currentApps.size) without
                            else currentApps + ConfiguredApp(app.label, app.component)
                        store.save(updated)
                    },
                    onClose = { onScreenChange(Screen.Home) },
                )
            }
        }
    }
}
