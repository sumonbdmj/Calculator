package com.simplecalculatordeign.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.simplecalculatordeign.core.math.AngleMode
import com.simplecalculatordeign.core.math.FormatOptions
import com.simplecalculatordeign.core.math.GroupingStyle
import com.simplecalculatordeign.data.db.HistoryDao
import com.simplecalculatordeign.data.db.HistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// ------------------------------------------------------------------ history

class HistoryRepository(private val dao: HistoryDao) {

    fun observe(query: String): Flow<List<HistoryEntity>> {
        // Stored values are plain (no grouping), so ignore separators the user types.
        val q = query.trim().replace(",", "").replace(" ", "")
            .replace('*', '×').replace('/', '÷').replace('−', '-')
        return if (q.isEmpty()) dao.observeAll() else dao.search(q)
    }

    val count: Flow<Int> = dao.observeCount()

    suspend fun add(expression: String, result: String, approximate: Boolean, source: String) {
        dao.insert(HistoryEntity(expression = expression, result = result, approximate = approximate, source = source))
    }

    suspend fun addAll(items: List<HistoryEntity>) = dao.insertAll(items)

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun clear() = dao.clear()
}

// ------------------------------------------------------------------ settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    /** Significant digits for non-terminating results (1 ÷ 3, √2, sin...). */
    val precision: Int = 50,
    /** -1 = show all digits, otherwise fixed decimal places. */
    val decimalPlaces: Int = -1,
    val grouping: GroupingStyle = GroupingStyle.INTERNATIONAL,
    val angleMode: AngleMode = AngleMode.DEGREES,
    val onboardingDone: Boolean = false,
) {
    val formatOptions: FormatOptions get() = FormatOptions(grouping, decimalPlaces)
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.settingsDataStore

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val SOUND = booleanPreferencesKey("sound")
        val VIBRATION = booleanPreferencesKey("vibration")
        val PRECISION = intPreferencesKey("precision")
        val DECIMALS = intPreferencesKey("decimals")
        val GROUPING = stringPreferencesKey("grouping")
        val ANGLE = stringPreferencesKey("angle")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
    }

    val settings: Flow<AppSettings> = store.data
        .catch { e -> if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e }
        .map { p ->
            AppSettings(
                themeMode = enumOr(p[Keys.THEME], ThemeMode.SYSTEM),
                soundEnabled = p[Keys.SOUND] ?: true,
                vibrationEnabled = p[Keys.VIBRATION] ?: true,
                precision = (p[Keys.PRECISION] ?: 50).coerceIn(10, 500),
                decimalPlaces = (p[Keys.DECIMALS] ?: -1).coerceIn(-1, 20),
                grouping = enumOr(p[Keys.GROUPING], GroupingStyle.INTERNATIONAL),
                angleMode = enumOr(p[Keys.ANGLE], AngleMode.DEGREES),
                onboardingDone = p[Keys.ONBOARDING] ?: false,
            )
        }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        name?.let { n -> enumValues<T>().firstOrNull { it.name == n } } ?: default

    suspend fun setTheme(mode: ThemeMode) = store.edit { it[Keys.THEME] = mode.name }
    suspend fun setSound(enabled: Boolean) = store.edit { it[Keys.SOUND] = enabled }
    suspend fun setVibration(enabled: Boolean) = store.edit { it[Keys.VIBRATION] = enabled }
    suspend fun setPrecision(digits: Int) = store.edit { it[Keys.PRECISION] = digits }
    suspend fun setDecimalPlaces(places: Int) = store.edit { it[Keys.DECIMALS] = places }
    suspend fun setGrouping(style: GroupingStyle) = store.edit { it[Keys.GROUPING] = style.name }
    suspend fun setAngleMode(mode: AngleMode) = store.edit { it[Keys.ANGLE] = mode.name }
    suspend fun setOnboardingDone() = store.edit { it[Keys.ONBOARDING] = true }
}
