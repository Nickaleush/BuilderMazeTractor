package com.nickaleush.tractormaze.games.maze

/** Final result emitted by the construction maze arcade. */
data class MazeGameResult(
    val level: Int,
    val passed: Boolean,
    val score: Int,
    val materialsCollected: Int,
    val totalMaterials: Int,
    val rewardCoins: Int,
    val turnCount: Int,
    val collisionCount: Int,
    val durationSeconds: Int,
    val levelPattern: Int
)
