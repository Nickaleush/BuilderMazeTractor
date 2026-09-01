package com.nickaleush.tractormaze.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val coins: Int = 10000,
    val selectedSkinId: String = "skin_loader",
    val selectedBackgroundId: String = "bg_build_site",
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val soundVolume: Int = 90,
    val musicVolume: Int = 80,
    val maxUnlockedLevel: Int = 1
)
