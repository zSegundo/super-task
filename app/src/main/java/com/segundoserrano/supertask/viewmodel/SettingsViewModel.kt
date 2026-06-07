package com.segundoserrano.supertask.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = application.dataStore

    // Keys
    private object PreferencesKeys {
        val USER_NAME = stringPreferencesKey("user_name")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val LANGUAGE = stringPreferencesKey("language")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    // Flows
    val userName: StateFlow<String> = dataStore.data
        .map { preferences -> preferences[PreferencesKeys.USER_NAME] ?: "User" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "User")

    val isDarkTheme: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[PreferencesKeys.IS_DARK_THEME] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val language: StateFlow<String> = dataStore.data
        .map { preferences -> preferences[PreferencesKeys.LANGUAGE] ?: "en" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val notificationsEnabled: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Update functions
    fun setUserName(name: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.USER_NAME] = name
            }
        }
    }

    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.IS_DARK_THEME] = isDark
            }
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.LANGUAGE] = lang
            }
            // Aplicar el cambio de idioma
            com.segundoserrano.supertask.utils.LanguageManager.setLanguage(
                getApplication<Application>().applicationContext,
                lang
            )
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
            }

            val context = getApplication<Application>().applicationContext
            if (enabled) {
                com.segundoserrano.supertask.utils.NotificationHelper.scheduleDailyNotification(context)
            } else {
                com.segundoserrano.supertask.utils.NotificationHelper.cancelDailyNotification(context)
            }
        }
    }
}