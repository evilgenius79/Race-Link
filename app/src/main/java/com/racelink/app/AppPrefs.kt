package com.racelink.app

import android.content.Context

/** Lightweight wrapper around SharedPreferences for app-level flags. */
class AppPrefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var tutorialSeen: Boolean
        get() = sp.getBoolean(KEY_TUTORIAL_SEEN, false)
        set(value) { sp.edit().putBoolean(KEY_TUTORIAL_SEEN, value).apply() }

    companion object {
        private const val NAME = "race_link_prefs"
        private const val KEY_TUTORIAL_SEEN = "tutorial_seen"
    }
}
