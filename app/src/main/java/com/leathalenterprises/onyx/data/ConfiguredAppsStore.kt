package com.leathalenterprises.onyx.data

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/** An app the user has chosen to show on the home screen. */
data class ConfiguredApp(val label: String, val component: ComponentName)

private const val PREFS_NAME = "onyx"
private const val APPS_KEY = "configured_apps"
private const val LABELED_SET_KEY = "labeled_set_hash"
private const val FIELD_SEPARATOR = "\t"
private const val ENTRY_SEPARATOR = "\n"

/**
 * Persists the selected apps, their (possibly LLM-generated) labels, and the
 * set hash those labels cover.
 *
 * Deliberately SharedPreferences rather than DataStore: the data is a few
 * hundred bytes, and saveLabeled() relies on multi-key atomic commits, which
 * prefs gives us for free.
 */
class ConfiguredAppsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val apps: Flow<List<ConfiguredApp>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == APPS_KEY) trySend(read())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(read())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()

    /** Hash of the component set that was last labeled by the Labeler. */
    val labeledSetHash: Flow<String?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == LABELED_SET_KEY) trySend(prefs.getString(LABELED_SET_KEY, null))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getString(LABELED_SET_KEY, null))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate()

    fun save(apps: List<ConfiguredApp>) {
        prefs.edit().putString(APPS_KEY, encode(apps)).apply()
    }

    /**
     * Writes labeler results and the set hash they cover in one commit, so
     * observers never see labeled apps without the matching hash.
     */
    fun saveLabeled(apps: List<ConfiguredApp>, setHash: String) {
        prefs.edit()
            .putString(APPS_KEY, encode(apps))
            .putString(LABELED_SET_KEY, setHash)
            .apply()
    }

    private fun encode(apps: List<ConfiguredApp>): String =
        apps.joinToString(ENTRY_SEPARATOR) {
            it.label + FIELD_SEPARATOR + it.component.flattenToString()
        }

    private fun read(): List<ConfiguredApp> =
        prefs.getString(APPS_KEY, null)
            ?.split(ENTRY_SEPARATOR)
            ?.mapNotNull { entry ->
                val fields = entry.split(FIELD_SEPARATOR)
                if (fields.size != 2) return@mapNotNull null
                val component = ComponentName.unflattenFromString(fields[1])
                    ?: return@mapNotNull null
                ConfiguredApp(fields[0], component)
            }
            .orEmpty()
}
