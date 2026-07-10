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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.leathalenterprises.onyx.data.AppsRepository
import com.leathalenterprises.onyx.data.ConfiguredApp
import com.leathalenterprises.onyx.data.ConfiguredAppsStore
import com.leathalenterprises.onyx.ui.HomeScreen
import com.leathalenterprises.onyx.ui.PickerScreen
import android.graphics.Color as AndroidColor

enum class Screen { Home, Picker }

class MainActivity : ComponentActivity() {

    private var screen by mutableStateOf(Screen.Home)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        val repository = AppsRepository(applicationContext)
        val store = ConfiguredAppsStore(applicationContext)
        setContent {
            OnyxApp(
                screen = screen,
                repository = repository,
                store = store,
                onScreenChange = { screen = it },
            )
        }
    }

    // The home button re-delivers the HOME intent to this singleTask
    // activity; snap back to the home screen.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        screen = Screen.Home
    }
}

@Composable
private fun OnyxApp(
    screen: Screen,
    repository: AppsRepository,
    store: ConfiguredAppsStore,
    onScreenChange: (Screen) -> Unit,
) {
    val configured by store.apps.collectAsState(initial = null)
    val installed by repository.installedApps.collectAsState(initial = emptyList())

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
            .systemBarsPadding(),
    ) {
        Crossfade(
            targetState = screen,
            animationSpec = tween(durationMillis = 180),
            label = "screen",
        ) { target ->
            when (target) {
                Screen.Home -> HomeScreen(
                    apps = visible,
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
