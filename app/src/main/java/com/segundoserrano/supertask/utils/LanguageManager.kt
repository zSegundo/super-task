package com.segundoserrano.supertask.utils

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.*

object LanguageManager {

    private const val PREF_NAME = "language_preference"
    private const val KEY_LANGUAGE = "selected_language"

    fun setLanguage(context: Context, languageCode: String) {
        // Guardar preferencia
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()

        // Aplicar idioma usando AndroidX
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, getSystemLanguage()) ?: "en"
    }

    private fun getSystemLanguage(): String {
        val locale = Locale.getDefault()
        return when (locale.language) {
            "es" -> "es"
            else -> "en"
        }
    }

    fun applyLanguage(context: Context) {
        val savedLanguage = getLanguage(context)

        // Obtener el idioma actual de AppCompat
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val currentLanguage = if (currentLocales.isEmpty) {
            getSystemLanguage()
        } else {
            currentLocales[0]?.language ?: getSystemLanguage()
        }

        // Solo aplicar si es diferente al actual
        if (currentLanguage != savedLanguage) {
            val localeList = LocaleListCompat.forLanguageTags(savedLanguage)
            AppCompatDelegate.setApplicationLocales(localeList)
        }
    }

    fun wrapContext(context: Context): Context {
        val languageCode = getLanguage(context)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}