package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("maxplay_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME = "theme_mode" // "DARK", "LIGHT", "SYSTEM"
        private const val KEY_DYNAMIC_COLORS = "dynamic_colors"
        private const val KEY_PLAYBACK_SPEED = "playback_speed"
        private const val KEY_SUBTITLE_SIZE = "subtitle_size" // "SMALL", "MEDIUM", "LARGE"
        private const val KEY_SUBTITLE_COLOR = "subtitle_color" // hex color
        private const val KEY_GESTURES_ENABLED = "gestures_enabled"
        private const val KEY_PIP_ENABLED = "pip_enabled"
    }

    var themeMode: String
        get() = prefs.getString(KEY_THEME, "DARK") ?: "DARK"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var useDynamicColors: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLORS, false)
        set(value) = prefs.edit().putBoolean(KEY_DYNAMIC_COLORS, value).apply()

    var defaultPlaybackSpeed: Float
        get() = prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_PLAYBACK_SPEED, value).apply()

    var subtitleSize: String
        get() = prefs.getString(KEY_SUBTITLE_SIZE, "MEDIUM") ?: "MEDIUM"
        set(value) = prefs.edit().putString(KEY_SUBTITLE_SIZE, value).apply()

    var subtitleColor: Int
        get() = prefs.getInt(KEY_SUBTITLE_COLOR, 0xFFFFFFFF.toInt())
        set(value) = prefs.edit().putInt(KEY_SUBTITLE_COLOR, value).apply()

    var gesturesEnabled: Boolean
        get() = prefs.getBoolean(KEY_GESTURES_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_GESTURES_ENABLED, value).apply()

    var pipEnabled: Boolean
        get() = prefs.getBoolean(KEY_PIP_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_PIP_ENABLED, value).apply()
}
