package com.nickaleush.tractormaze.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_stats")
data class GameStatsEntity(
    @PrimaryKey val id: Int = 1,
    val materialsCollected: Int = 0,
    val bestMaterialsInLevel: Int = 0,
    val queuedTurns: Int = 0,
    val levelsPassed: Int = 0,
    val crashes: Int = 0,
    val bestScoreInRun: Int = 0,
    val cleanLevelsPassed: Int = 0,
    val coinsEarned: Int = 0,
    val reachedFastPattern: Boolean = false,
    val cleanWinStreak: Int = 0
)
