package com.leathalenterprises.onyx.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import android.telecom.TelecomManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Watches the configured-app set and relabels it whenever membership changes.
 *
 * Labels are recomputed only when the set of selected components differs from
 * the set that was last labeled (tracked by a persisted hash), so after
 * initial setup inference effectively never runs. While a pass is in flight,
 * the affected components are exposed via [pending] so the home screen can
 * fuzz their labels; results land in the store and the UI settles on its own.
 */
class LabelerCoordinator(
    context: Context,
    private val store: ConfiguredAppsStore,
    private val repository: AppsRepository,
    private val labeler: Labeler,
    private val scope: CoroutineScope,
) {

    private val context = context.applicationContext

    private val _pending = MutableStateFlow<Set<ComponentName>>(emptySet())
    val pending: StateFlow<Set<ComponentName>> = _pending.asStateFlow()

    fun start() {
        scope.launch {
            combine(store.apps, store.labeledSetHash, repository.installedApps, ::Triple)
                // collectLatest: a new toggle cancels an in-flight pass, so
                // rapid picker changes coalesce into one relabel.
                .collectLatest { (apps, labeledHash, installed) ->
                    relabel(apps, labeledHash, installed)
                }
        }
    }

    private suspend fun relabel(
        apps: List<ConfiguredApp>,
        labeledHash: String?,
        installed: List<InstalledApp>,
    ) {
        if (apps.isEmpty()) {
            _pending.value = emptySet()
            return
        }
        val hash = setHash(apps)
        if (hash == labeledHash) {
            _pending.value = emptySet()
            return
        }

        _pending.value = apps.map { it.component }.toSet()
        delay(DEBOUNCE_MS)

        val requests = apps.map { app ->
            LabelRequest(
                packageName = app.component.packageName,
                // The store label may already be generic from a previous
                // pass; the app's real name comes from the installed list.
                originalLabel = installed
                    .firstOrNull { it.component == app.component }
                    ?.label
                    ?: app.label,
                category = categoryName(app.component.packageName),
                role = roleName(app.component.packageName),
            )
        }

        val proposals = try {
            labeler.label(requests)
        } catch (_: Exception) {
            null
        }
        val finals = LabelValidator.finalize(requests, proposals)

        store.saveLabeled(
            apps.map { app ->
                app.copy(label = finals[app.component.packageName] ?: app.label)
            },
            setHash = hash,
        )
        _pending.value = emptySet()
    }

    private fun setHash(apps: List<ConfiguredApp>): String =
        apps.map { it.component.flattenToString() }.sorted().joinToString("|")

    private fun categoryName(packageName: String): String? = try {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        when (info.category) {
            ApplicationInfo.CATEGORY_GAME -> "GAME"
            ApplicationInfo.CATEGORY_AUDIO -> "AUDIO"
            ApplicationInfo.CATEGORY_VIDEO -> "VIDEO"
            ApplicationInfo.CATEGORY_IMAGE -> "IMAGE"
            ApplicationInfo.CATEGORY_SOCIAL -> "SOCIAL"
            ApplicationInfo.CATEGORY_NEWS -> "NEWS"
            ApplicationInfo.CATEGORY_MAPS -> "MAPS"
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> "PRODUCTIVITY"
            else -> null
        }
    } catch (_: Exception) {
        null
    }

    private fun roleName(packageName: String): String? = when (packageName) {
        defaultSmsPackage() -> "default messaging app"
        defaultDialerPackage() -> "default phone app"
        defaultBrowserPackage() -> "default web browser"
        else -> null
    }

    private fun defaultSmsPackage(): String? = try {
        Telephony.Sms.getDefaultSmsPackage(context)
    } catch (_: Exception) {
        null
    }

    private fun defaultDialerPackage(): String? = try {
        context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
    } catch (_: Exception) {
        null
    }

    private fun defaultBrowserPackage(): String? = try {
        context.packageManager
            .resolveActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com")),
                PackageManager.MATCH_DEFAULT_ONLY,
            )
            ?.activityInfo
            ?.packageName
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val DEBOUNCE_MS = 1_000L
    }
}
