package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.ActivityLogEntity
import com.example.model.AdMobSettingsEntity
import com.example.model.AdminAccountEntity
import com.example.model.AppContentSettingsEntity
import com.example.model.AppUserEntity
import com.example.model.CommentEntity
import com.example.model.NotificationEntity
import com.example.model.PromptCategoryEntity
import com.example.model.PromptImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdminDao {

    // 1. Images
    @Query("SELECT * FROM images ORDER BY displayOrder ASC, createdAtMillis DESC")
    fun observeImages(): Flow<List<PromptImageEntity>>

    @Query("SELECT * FROM images")
    suspend fun getAllImagesOnce(): List<PromptImageEntity>

    @Query("SELECT COUNT(*) FROM images WHERE categoryId = :categoryId")
    suspend fun countImagesInCategory(categoryId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImage(image: PromptImageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImages(images: List<PromptImageEntity>)

    @Query("UPDATE images SET categoryId = :targetCategoryId, categoryName = :targetCategoryName, updatedAtMillis = :now WHERE categoryId = :sourceCategoryId")
    suspend fun migrateCategoryImages(
        sourceCategoryId: String,
        targetCategoryId: String,
        targetCategoryName: String,
        now: Long
    )

    @Query("DELETE FROM images WHERE categoryId = :categoryId")
    suspend fun deleteImagesByCategory(categoryId: String)

    @Query("DELETE FROM images WHERE id = :imageId")
    suspend fun deleteImageById(imageId: String)

    // 2. Categories
    @Query("SELECT * FROM categories ORDER BY displayOrder ASC, name ASC")
    fun observeCategories(): Flow<List<PromptCategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesOnce(): List<PromptCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: PromptCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<PromptCategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: String)

    // 3. Users
    @Query("SELECT * FROM users ORDER BY lastActivityAtMillis DESC")
    fun observeUsers(): Flow<List<AppUserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsersOnce(): List<AppUserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: AppUserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsers(users: List<AppUserEntity>)

    // 4. Comments
    @Query("SELECT * FROM comments ORDER BY createdAtMillis DESC")
    fun observeComments(): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments")
    suspend fun getAllCommentsOnce(): List<CommentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComment(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertComments(comments: List<CommentEntity>)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteCommentById(commentId: String)

    // 5. AdMob Settings
    @Query("SELECT * FROM advertisement_settings WHERE id = 'global_admob_config' LIMIT 1")
    fun observeAdMobSettings(): Flow<AdMobSettingsEntity?>

    @Query("SELECT * FROM advertisement_settings WHERE id = 'global_admob_config' LIMIT 1")
    suspend fun getAdMobSettingsOnce(): AdMobSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAdMobSettings(settings: AdMobSettingsEntity)

    // 6. App Content Settings
    @Query("SELECT * FROM app_settings WHERE id = 'global_app_content' LIMIT 1")
    fun observeAppContentSettings(): Flow<AppContentSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 'global_app_content' LIMIT 1")
    suspend fun getAppContentSettingsOnce(): AppContentSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppContentSettings(settings: AppContentSettingsEntity)

    // 7. Notifications
    @Query("SELECT * FROM notifications ORDER BY createdAtMillis DESC")
    fun observeNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications")
    suspend fun getAllNotificationsOnce(): List<NotificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotifications(notifications: List<NotificationEntity>)

    // 8. Admin Users
    @Query("SELECT * FROM admin_users ORDER BY createdAtMillis ASC")
    fun observeAdminAccounts(): Flow<List<AdminAccountEntity>>

    @Query("SELECT * FROM admin_users")
    suspend fun getAllAdminAccountsOnce(): List<AdminAccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAdminAccount(admin: AdminAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAdminAccounts(admins: List<AdminAccountEntity>)

    @Query("DELETE FROM admin_users WHERE uid = :uid")
    suspend fun deleteAdminAccount(uid: String)

    // 9. Activity Logs
    @Query("SELECT * FROM admin_activity_logs ORDER BY createdAtMillis DESC LIMIT 250")
    fun observeActivityLogs(): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLogEntity)
}
