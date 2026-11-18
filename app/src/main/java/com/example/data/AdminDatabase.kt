package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.ActivityLogEntity
import com.example.model.AdMobSettingsEntity
import com.example.model.AdminAccountEntity
import com.example.model.AppContentSettingsEntity
import com.example.model.AppUserEntity
import com.example.model.CommentEntity
import com.example.model.NotificationEntity
import com.example.model.PromptCategoryEntity
import com.example.model.PromptImageEntity

@Database(
    entities = [
        PromptImageEntity::class,
        PromptCategoryEntity::class,
        AppUserEntity::class,
        CommentEntity::class,
        AdMobSettingsEntity::class,
        AppContentSettingsEntity::class,
        NotificationEntity::class,
        AdminAccountEntity::class,
        ActivityLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AdminDatabase : RoomDatabase() {
    abstract fun adminDao(): AdminDao

    companion object {
        @Volatile
        private var INSTANCE: AdminDatabase? = null

        fun getInstance(context: Context): AdminDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AdminDatabase::class.java,
                    "prompt_admin_control_panel.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
