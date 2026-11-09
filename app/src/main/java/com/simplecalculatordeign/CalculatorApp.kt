package com.simplecalculatordeign

import android.app.Application
import android.content.Context
import com.simplecalculatordeign.data.db.AppDatabase
import com.simplecalculatordeign.data.repository.AiRepository
import com.simplecalculatordeign.data.repository.HistoryRepository
import com.simplecalculatordeign.data.repository.SettingsRepository

class CalculatorApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Manual dependency injection: one instance of each repository for the whole app. */
class AppContainer(context: Context) {
    private val database = AppDatabase.build(context)
    val historyRepository = HistoryRepository(database.historyDao())
    val settingsRepository = SettingsRepository(context)
    val aiRepository = AiRepository(BuildConfig.AI_BACKEND_URL)
}

val Context.appContainer: AppContainer
    get() = (applicationContext as CalculatorApp).container
