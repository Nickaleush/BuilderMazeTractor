package com.nickaleush.tractormaze.core

import android.content.Context
import com.nickaleush.tractormaze.core.audio.SoundManager
import com.nickaleush.tractormaze.core.dispatchers.AppDispatchers
import com.nickaleush.tractormaze.data.db.AppDatabase
import com.nickaleush.tractormaze.data.repository.GameRepository
import com.nickaleush.tractormaze.data.repository.ScoreRepository

class AppServiceLocator(
    private val context: Context
) {

    val database: AppDatabase by lazy {
        AppDatabase.create(context)
    }

    val soundManager: SoundManager by lazy {
        SoundManager(context.applicationContext)
    }

    val scoreRepository: ScoreRepository by lazy {
        ScoreRepository(database.scoreDao())
    }

    val gameRepository: GameRepository by lazy {
        GameRepository(
            scoreRepository = scoreRepository,
            playerDao = database.playerDao(),
            inventoryDao = database.inventoryDao(),
            achievementDao = database.achievementDao(),
            gameStatsDao = database.gameStatsDao()
        )
    }

    val dispatchers: AppDispatchers by lazy {
        AppDispatchers()
    }
}
