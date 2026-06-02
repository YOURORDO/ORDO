package com.example.ordo

import android.content.Context

object ThemeSettings {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_IS_DARK = "is_dark_mode"

    fun isDark(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // По умолчанию пусть будет темная (true)
        return prefs.getBoolean(KEY_IS_DARK, true)
    }

    fun toggleTheme(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = isDark(context)
        prefs.edit().putBoolean(KEY_IS_DARK, !current).apply()
    }
}