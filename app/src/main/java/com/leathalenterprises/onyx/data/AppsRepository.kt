package com.leathalenterprises.onyx.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/** A launchable activity installed on the device. */
data class InstalledApp(val label: String, val component: ComponentName)

/** Enumerates launchable apps and stays current across installs/uninstalls. */
class AppsRepository(context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    val installedApps: Flow<List<InstalledApp>> = callbackFlow {
        fun query(): List<InstalledApp> =
            launcherApps.getActivityList(null, Process.myUserHandle())
                .map { InstalledApp(it.label.toString(), it.componentName) }
                .sortedBy { it.label.lowercase() }

        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) {
                trySend(query())
            }

            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                trySend(query())
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {
                trySend(query())
            }

            override fun onPackagesAvailable(
                packageNames: Array<String>,
                user: UserHandle,
                replacing: Boolean,
            ) {
                trySend(query())
            }

            override fun onPackagesUnavailable(
                packageNames: Array<String>,
                user: UserHandle,
                replacing: Boolean,
            ) {
                trySend(query())
            }
        }
        launcherApps.registerCallback(callback)
        trySend(query())
        awaitClose { launcherApps.unregisterCallback(callback) }
    }.conflate()

    fun launch(component: ComponentName) {
        // The app may have been uninstalled between render and tap; a home
        // screen should never crash over that.
        try {
            launcherApps.startMainActivity(component, Process.myUserHandle(), null, null)
        } catch (_: Exception) {
        }
    }
}
