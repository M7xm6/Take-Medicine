package com.takeamedicine.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(
    name = "user_preferences"
)

class UserPreferences(
    private val context: Context
) {
    private val firstLaunchKey = booleanPreferencesKey("first_launch")

    val firstLaunch: Flow<Boolean> = context.userPreferencesDataStore.data
        .map { preferences -> preferences[firstLaunchKey] ?: true }

    suspend fun setFirstLaunch(value: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[firstLaunchKey] = value
        }
    }
}
