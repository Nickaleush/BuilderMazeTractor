package com.nickaleush.tractormaze.data.repository

import com.nickaleush.tractormaze.data.db.dao.AchievementDao
import com.nickaleush.tractormaze.data.db.dao.GameStatsDao
import com.nickaleush.tractormaze.data.db.dao.InventoryDao
import com.nickaleush.tractormaze.data.db.dao.PlayerDao
import com.nickaleush.tractormaze.data.db.entity.AchievementEntity
import com.nickaleush.tractormaze.data.db.entity.GameStatsEntity
import com.nickaleush.tractormaze.data.db.entity.InventoryItemEntity
import com.nickaleush.tractormaze.data.db.entity.PlayerProfileEntity
import com.nickaleush.tractormaze.domain.model.GameResult
import com.nickaleush.tractormaze.games.maze.MazeGameResult
import kotlin.math.max
import kotlin.math.min

class GameRepository(
    private val scoreRepository: ScoreRepository,
    private val playerDao: PlayerDao,
    private val inventoryDao: InventoryDao,
    private val achievementDao: AchievementDao,
    private val gameStatsDao: GameStatsDao
) {

    suspend fun saveGameResult(result: GameResult) {
        scoreRepository.saveScore(
            gameId = result.gameId,
            score = result.score,
            durationMs = result.durationMs
        )
    }

    suspend fun saveMazeResult(result: MazeGameResult) {
        ensureDefaults()
        saveGameResult(
            GameResult(
                gameId = GAME_ID,
                score = result.score,
                durationMs = result.durationSeconds * 1000L,
                finishedAt = System.currentTimeMillis()
            )
        )
        if (result.rewardCoins > 0) addCoins(result.rewardCoins)
        if (result.passed) unlockUpToLevel(result.level + 1)
        updateAchievements(result)
    }

    suspend fun unlockUpToLevel(level: Int) {
        val profile = getProfile()
        val target = level.coerceIn(1, MAX_LEVEL)
        if (target > profile.maxUnlockedLevel) {
            playerDao.update(profile.copy(maxUnlockedLevel = target))
        }
    }

    suspend fun getMaxUnlockedLevel(): Int = getProfile().maxUnlockedLevel.coerceIn(1, MAX_LEVEL)

    suspend fun getBestScore(gameId: String): Int = scoreRepository.getBestScore(gameId)

    suspend fun getTopScores(limit: Int = 10) = scoreRepository.getBestScores(GAME_ID).take(limit)

    suspend fun getProfile(): PlayerProfileEntity {
        ensureDefaults()
        return requireNotNull(playerDao.getProfile())
    }

    suspend fun getCoins(): Int = getProfile().coins

    suspend fun addCoins(amount: Int) {
        val profile = getProfile()
        playerDao.update(profile.copy(coins = max(0, profile.coins + amount)))
    }

    suspend fun getShopItems(): List<InventoryItemEntity> {
        ensureDefaults()
        return inventoryDao.getAll()
    }

    suspend fun buyOrSelectItem(itemId: String): PurchaseResult {
        ensureDefaults()
        val item = inventoryDao.getById(itemId) ?: return PurchaseResult.NotFound
        val profile = getProfile()
        if (!item.isUnlocked && profile.coins < item.price) return PurchaseResult.NotEnoughCoins

        val unlockedItem = if (item.isUnlocked) item else item.copy(isUnlocked = true)
        if (!item.isUnlocked) {
            playerDao.update(profile.copy(coins = profile.coins - item.price))
            inventoryDao.update(unlockedItem)
        }
        inventoryDao.clearSelected(unlockedItem.type)
        inventoryDao.select(unlockedItem.id)
        val latestProfile = getProfile()
        playerDao.update(
            when (unlockedItem.type) {
                TYPE_SKIN -> latestProfile.copy(selectedSkinId = unlockedItem.id)
                TYPE_BACKGROUND -> latestProfile.copy(selectedBackgroundId = unlockedItem.id)
                else -> latestProfile
            }
        )
        return PurchaseResult.Success
    }

    suspend fun getAchievements(): List<AchievementEntity> {
        ensureDefaults()
        return achievementDao.getAll()
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        val profile = getProfile()
        playerDao.update(profile.copy(soundEnabled = enabled))
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        val profile = getProfile()
        playerDao.update(profile.copy(musicEnabled = enabled))
    }

    suspend fun setSoundVolume(volume: Int) {
        val profile = getProfile()
        playerDao.update(profile.copy(soundVolume = volume.coerceIn(0, 100)))
    }

    suspend fun setMusicVolume(volume: Int) {
        val profile = getProfile()
        playerDao.update(profile.copy(musicVolume = volume.coerceIn(0, 100)))
    }

    suspend fun ensureDefaults() {
        if (playerDao.getProfile() == null) {
            playerDao.insert(PlayerProfileEntity())
        }
        gameStatsDao.insert(GameStatsEntity())
        val shopItems = defaultShopItems()
        inventoryDao.insertDefaults(shopItems)
        shopItems.forEach { item ->
            inventoryDao.updateCatalogFields(
                id = item.id,
                title = item.title,
                description = item.description,
                price = item.price
            )
        }
        achievementDao.insertDefaults(defaultAchievements())
    }

    private suspend fun updateAchievements(result: MazeGameResult) {
        val old = gameStatsDao.getStats() ?: GameStatsEntity()
        val newCleanWinStreak = if (result.passed && result.collisionCount == 0) {
            old.cleanWinStreak + 1
        } else if (!result.passed) {
            0
        } else {
            old.cleanWinStreak
        }
        val stats = old.copy(
            materialsCollected = old.materialsCollected + result.materialsCollected,
            bestMaterialsInLevel = max(old.bestMaterialsInLevel, result.materialsCollected),
            queuedTurns = old.queuedTurns + result.turnCount,
            levelsPassed = old.levelsPassed + if (result.passed) 1 else 0,
            crashes = old.crashes + result.collisionCount,
            bestScoreInRun = max(old.bestScoreInRun, result.score),
            cleanLevelsPassed = old.cleanLevelsPassed + if (result.passed && result.collisionCount == 0) 1 else 0,
            coinsEarned = old.coinsEarned + result.rewardCoins,
            reachedFastPattern = old.reachedFastPattern || result.levelPattern == 5,
            cleanWinStreak = min(newCleanWinStreak, 99)
        )
        gameStatsDao.update(stats)
        val updated = defaultAchievements().map { achievement ->
            val current = when (achievement.id) {
                "first_load" -> stats.materialsCollected
                "site_cleaner" -> stats.levelsPassed
                "route_master" -> stats.queuedTurns
                "careful_driver" -> stats.cleanLevelsPassed
                "coin_contractor" -> stats.coinsEarned
                "high_score" -> stats.bestScoreInRun
                "speed_shift" -> if (stats.reachedFastPattern) 1 else 0
                "perfect_crew" -> stats.cleanWinStreak
                "material_king" -> stats.materialsCollected
                "hard_hat" -> stats.levelsPassed
                else -> 0
            }
            achievement.copy(currentValue = current, isUnlocked = current >= achievement.targetValue)
        }
        achievementDao.update(updated)
    }

    private fun defaultShopItems(): List<InventoryItemEntity> = listOf(
        InventoryItemEntity("skin_loader", TYPE_SKIN, "Classic Tractor", "Balanced starter tractor.", 0, true, true),
        InventoryItemEntity("skin_red_truck", TYPE_SKIN, "Magnet Tractor", "Magnet rig with a little more straight-line speed.", 180, false, false),
        InventoryItemEntity("skin_bulldozer", TYPE_SKIN, "Steel Shield", "Heavy armored tractor for careful turns.", 260, false, false),
        InventoryItemEntity("skin_blue_crane", TYPE_SKIN, "Turbo Tractor", "Fast turbo model for open routes.", 360, false, false),
        InventoryItemEntity("skin_mixer", TYPE_SKIN, "Wide Plow", "Wide front plow with stable handling.", 460, false, false),
        InventoryItemEntity("bg_build_site", TYPE_BACKGROUND, "Build Site", "Sunny construction yard.", 0, true, true),
        InventoryItemEntity("bg_sunset_yard", TYPE_BACKGROUND, "Sunset Yard", "Warm evening site.", 120, false, false),
        InventoryItemEntity("bg_quarry", TYPE_BACKGROUND, "Rock Quarry", "Stone and dust palette.", 220, false, false),
        InventoryItemEntity("bg_night_shift", TYPE_BACKGROUND, "Night Shift", "Dark site with bright signals.", 320, false, false),
        InventoryItemEntity("bg_winter_site", TYPE_BACKGROUND, "Winter Site", "Cold blue construction zone.", 420, false, false),
        InventoryItemEntity("bg_desert_site", TYPE_BACKGROUND, "Desert Site", "Hot sand and orange clay.", 520, false, false)
    )

    private fun defaultAchievements(): List<AchievementEntity> = listOf(
        AchievementEntity("first_load", "First Load", "Collect 20 materials.", 0, 20, false),
        AchievementEntity("site_cleaner", "Site Cleaner", "Pass 5 construction mazes.", 0, 5, false),
        AchievementEntity("route_master", "Route Master", "Make 150 queued turns.", 0, 150, false),
        AchievementEntity("careful_driver", "Careful Driver", "Pass a level without crashing.", 0, 1, false),
        AchievementEntity("coin_contractor", "Coin Contractor", "Earn 250 coins from jobs.", 0, 250, false),
        AchievementEntity("high_score", "High Score", "Reach 2500 points in one run.", 0, 2500, false),
        AchievementEntity("speed_shift", "Speed Shift", "Reach a level-5 maze pattern.", 0, 1, false),
        AchievementEntity("perfect_crew", "Perfect Crew", "Pass 5 levels in a row without crashes.", 0, 5, false),
        AchievementEntity("material_king", "Material King", "Collect 400 total materials.", 0, 400, false),
        AchievementEntity("hard_hat", "Hard Hat Operator", "Complete 20 levels.", 0, 20, false)
    )

    sealed class PurchaseResult {
        data object Success : PurchaseResult()
        data object NotEnoughCoins : PurchaseResult()
        data object NotFound : PurchaseResult()
    }

    companion object {
        const val GAME_ID = "construction_maze"
        const val TYPE_SKIN = "skin"
        const val TYPE_BACKGROUND = "background"
        const val MAX_LEVEL = 45
    }
}
