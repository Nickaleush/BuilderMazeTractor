package com.nickaleush.tractormaze.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nickaleush.tractormaze.data.db.dao.AchievementDao
import com.nickaleush.tractormaze.data.db.dao.GameStatsDao
import com.nickaleush.tractormaze.data.db.dao.InventoryDao
import com.nickaleush.tractormaze.data.db.dao.PlayerDao
import com.nickaleush.tractormaze.data.db.dao.ScoreDao
import com.nickaleush.tractormaze.data.db.entity.AchievementEntity
import com.nickaleush.tractormaze.data.db.entity.GameStatsEntity
import com.nickaleush.tractormaze.data.db.entity.InventoryItemEntity
import com.nickaleush.tractormaze.data.db.entity.PlayerProfileEntity
import com.nickaleush.tractormaze.data.db.entity.ScoreEntity

@Database(
    entities = [
        ScoreEntity::class,
        PlayerProfileEntity::class,
        InventoryItemEntity::class,
        AchievementEntity::class,
        GameStatsEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scoreDao(): ScoreDao
    abstract fun playerDao(): PlayerDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun achievementDao(): AchievementDao
    abstract fun gameStatsDao(): GameStatsDao

    companion object {

        /** Adds the level-unlock progress column for older local databases. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE player_profile " +
                        "ADD COLUMN maxUnlockedLevel INTEGER NOT NULL DEFAULT 1"
                )
            }
        }


        /** Adds independent music and sound effect volume sliders. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE player_profile " +
                        "ADD COLUMN soundVolume INTEGER NOT NULL DEFAULT 90"
                )
                db.execSQL(
                    "ALTER TABLE player_profile " +
                        "ADD COLUMN musicVolume INTEGER NOT NULL DEFAULT 80"
                )
            }
        }

        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "builder_maze.db"
            )
                .addMigrations(MIGRATION_2_3, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
