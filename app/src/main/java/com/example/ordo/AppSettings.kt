package com.example.ordo

import android.content.Context

object AppSettings {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_BACKGROUND_MODE = "background_mode_enabled"
    private const val KEY_APP_LANGUAGE = "app_language"
    private const val KEY_SYNTHESIS_ENABLED = "synthesis_enabled"


    fun isSynthesisEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SYNTHESIS_ENABLED, false)
    }


    fun setSynthesisEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SYNTHESIS_ENABLED, enabled).apply()
    }

    // По умолчанию включаем фоновый режим (true)
    fun isBackgroundModeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BACKGROUND_MODE, true)
    }

    fun setBackgroundMode(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BACKGROUND_MODE, enabled).apply()
    }

    // --- НОВЫЕ НАСТРОЙКИ ФОНОВОЙ ЧАСТОТЫ ---
    fun getBackgroundServer(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("bg_server", "") ?: ""
    }

    fun setBackgroundServer(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("bg_server", url).apply()
    }



    /**
     * Читает выбранный язык из настроек. По умолчанию возвращает английский ("en").
     */
    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_APP_LANGUAGE, "en") ?: "en" // По умолчанию ставим "en"
    }

    /**
     * Записывает выбранный язык ("ru" или "en") в настройки устройства.
     */
    fun setLanguage(context: Context, lang: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_APP_LANGUAGE, lang).apply()
    }
}