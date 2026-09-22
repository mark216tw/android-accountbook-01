package com.example.simpleaccountbook.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "appearance")

class SettingsStore(private val context: Context) {
    private object Keys {
        val displayMode = stringPreferencesKey("display_mode")
        val themeId = stringPreferencesKey("theme_id")
        val customHue = floatPreferencesKey("custom_hue")
        val lastEntryType = stringPreferencesKey("last_entry_type")
        val lastExpenseCategoryId = longPreferencesKey("last_expense_category_id")
        val lastIncomeCategoryId = longPreferencesKey("last_income_category_id")
    }

    val settings: Flow<ThemeSettings> = context.dataStore.data.map { preferences ->
        ThemeSettings(
            displayMode = preferences[Keys.displayMode] ?: "SYSTEM",
            themeId = preferences[Keys.themeId] ?: "PURPLE",
            customHue = preferences[Keys.customHue] ?: 275f,
            lastEntryType = preferences[Keys.lastEntryType] ?: EntryType.EXPENSE.name,
            lastExpenseCategoryId = preferences[Keys.lastExpenseCategoryId] ?: 0,
            lastIncomeCategoryId = preferences[Keys.lastIncomeCategoryId] ?: 0,
        )
    }

    suspend fun setDisplayMode(mode: String) {
        context.dataStore.edit { it[Keys.displayMode] = mode }
    }

    suspend fun setPresetTheme(id: String, hue: Float) {
        context.dataStore.edit {
            it[Keys.themeId] = id
            it[Keys.customHue] = hue
        }
    }

    suspend fun setCustomHue(hue: Float) {
        context.dataStore.edit {
            it[Keys.themeId] = "CUSTOM"
            it[Keys.customHue] = hue
        }
    }

    suspend fun setRecentEntry(type: EntryType, categoryId: Long) {
        context.dataStore.edit {
            it[Keys.lastEntryType] = type.name
            it[if (type == EntryType.EXPENSE) Keys.lastExpenseCategoryId else Keys.lastIncomeCategoryId] = categoryId
        }
    }

    suspend fun clearRecentCategories() {
        context.dataStore.edit {
            it.remove(Keys.lastExpenseCategoryId)
            it.remove(Keys.lastIncomeCategoryId)
        }
    }
}
