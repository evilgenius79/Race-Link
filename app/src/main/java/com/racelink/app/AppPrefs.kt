package com.racelink.app

import android.content.Context

/** Lightweight wrapper around SharedPreferences for app-level flags. */
class AppPrefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var tutorialSeen: Boolean
        get() = sp.getBoolean(KEY_TUTORIAL_SEEN, false)
        set(value) { sp.edit().putBoolean(KEY_TUTORIAL_SEEN, value).apply() }

    /** Display name shown to peers and on this user's own race screens. */
    var nickname: String
        get() = sp.getString(KEY_NICKNAME, "").orEmpty()
        set(value) { sp.edit().putString(KEY_NICKNAME, value.trim()).apply() }

    val hasNickname: Boolean get() = nickname.isNotBlank()

    companion object {
        private const val NAME = "race_link_prefs"
        private const val KEY_TUTORIAL_SEEN = "tutorial_seen"
        private const val KEY_NICKNAME = "nickname"
    }
}
