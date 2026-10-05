package com.linguaflix.app.data
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import java.io.IOException
private val Context.dataStore by preferencesDataStore("settings")
data class Settings(val onboarded: Boolean = false, val level: String = "B1", val theme: String = "system", val fontScale: Float = 1f, val dailyGoal: Int = 15, val accent: String = "US", val audio: Boolean = true)
class SettingsStore(private val context: Context) {
 private val onboard = booleanPreferencesKey("onboarded")
 private val level = stringPreferencesKey("level")
 private val theme = stringPreferencesKey("theme")
 private val font = floatPreferencesKey("font")
 private val goal = intPreferencesKey("goal")
 private val accent = stringPreferencesKey("accent")
 private val audio = booleanPreferencesKey("audio")
 val settings = context.dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { Settings(it[onboard] ?: false, it[level] ?: "B1", it[theme] ?: "system", it[font] ?: 1f, it[goal] ?: 15, it[accent] ?: "US", it[audio] ?: true) }
 suspend fun save(s: Settings) { context.dataStore.edit { it[onboard] = s.onboarded; it[level] = s.level; it[theme] = s.theme; it[font] = s.fontScale.coerceIn(.85f, 1.4f); it[goal] = s.dailyGoal.coerceIn(5, 120); it[accent] = s.accent; it[audio] = s.audio } }
}