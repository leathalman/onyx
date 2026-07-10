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
import com.leathalenterprises.onyx.data.GemmaLabeler
import com.leathalenterprises.onyx.data.LabelerCoordinator
import com.leathalenterprises.onyx.data.StubLabeler
import java.io.File
import com.leathalenterprises.onyx.ui.HomeScreen
import com.leathalenterprises.onyx.ui.PickerScreen
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import android.graphics.Color as AndroidColor

enum class Screen { Home, Picker }

/**
 * Gemma bundle inside filesDir; see GemmaLabeler docs for how to load it.
 * Name is model-agnostic so swapping model sizes never touches code.
 */
private const val MODEL_FILE_NAME = "gemma3.task"

class MainActivity : ComponentActivity() {

    private var screen by mutableStateOf(Screen.Home)
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        val repository = AppsRepository(applicationContext)
        val store = ConfiguredAppsStore(applicationContext)
        // Real model when its file is present; keyword stub otherwise (so
        // the emulator and model-less devices still get basic labels).
        val modelFile = File(filesDir, MODEL_FILE_NAME)
        val labeler =
            if (modelFile.exists()) GemmaLabeler(applicationContext, modelFile.path)
            else StubLabeler()
        val coordinator = LabelerCoordinator(
            context = applicationContext,
            store = store,
            repository = repository,
            labeler = labeler,
            scope = scope,
        ).also { it.start() }
        setContent {
            OnyxApp(
                screen = screen,
                repository = repository,
                store = store,
                coordinator = coordinator,
                onScreenChange = { screen = it },
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
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
    coordinator: LabelerCoordinator,
    onScreenChange: (Screen) -> Unit,
) {
    val configured by store.apps.collectAsState(initial = null)
    val installed by repository.installedApps.collectAsState(initial = emptyList())
    val pending by coordinator.pending.collectAsState()

    // Hide configured apps that have since been uninstalled (once the
    // installed list has actually loaded).
    val visible = configured?.let { apps ->
        if (installed.isEmpty()) apps
        else apps.filter { app -> installed.any { it.component == app.component } }
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
                    pending = pending,
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
