package com.username051203.textingstory.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object PrefsManager {
    private const val PREFS_NAME = "textingstory_prefs"

    const val KEY_DARK_MODE = "dark_mode"
    const val KEY_VIDEO_FORMAT = "video_format"
    const val KEY_VIDEO_SPEED = "video_speed"
    const val KEY_SHOW_TYPING = "show_typing"
    const val KEY_TYPING_SOUND = "typing_sound"
    const val KEY_KEEP_CORRECTIONS = "keep_corrections"

    fun get(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun applyTheme(context: Context) {
        val dark = get(context).getBoolean(KEY_DARK_MODE, true)
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
