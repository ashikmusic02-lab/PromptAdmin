package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.ActivityLogEntity
import com.example.model.AdMobSettingsEntity
import com.example.model.AdminAccountEntity
import com.example.model.AdminRole
import com.example.model.AppContentSettingsEntity
import com.example.model.AppUserEntity
import com.example.model.CommentEntity
import com.example.model.NotificationEntity
import com.example.model.PromptCategoryEntity
import com.example.model.PromptImageEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class FirebaseConnectionStatus(
    val isGoogleServicesPresent: Boolean,
    val projectId: String,
    val applicationId: String,
    val databaseModeLabel: String,
    val authEmail: String?
)

class AdminRepository(
    private val context: Context,
    private val dao: AdminDao
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val imagesFlow: Flow<List<PromptImageEntity>> = dao.observeImages()
    val categoriesFlow: Flow<List<PromptCategoryEntity>> = dao.observeCategories()
    val usersFlow: Flow<List<AppUserEntity>> = dao.observeUsers()
    val commentsFlow: Flow<List<CommentEntity>> = dao.observeComments()
    val adMobSettingsFlow: Flow<AdMobSettingsEntity?> = dao.observeAdMobSettings()
    val appContentSettingsFlow: Flow<AppContentSettingsEntity?> = dao.observeAppContentSettings()
    val notificationsFlow: Flow<List<NotificationEntity>> = dao.observeNotifications()
    val adminAccountsFlow: Flow<List<AdminAccountEntity>> = dao.observeAdminAccounts()
    val activityLogsFlow: Flow<List<ActivityLogEntity>> = dao.observeActivityLogs()

    fun getConnectionStatus(): FirebaseConnectionStatus {
        return try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isNotEmpty()) {
                val app = FirebaseApp.getInstance()
                val projectId = app.options.projectId ?: "configured-firebase-project"
                val isPlaceholder = projectId == "remixed-project-id" || projectId.isBlank()
                val currentUser = try {
                    FirebaseAuth.getInstance().currentUser
                } catch (_: Exception) {
                    null
                }
                FirebaseConnectionStatus(
                    isGoogleServicesPresent = !isPlaceholder,
                    projectId = if (isPlaceholder) "Awaiting google-services.json" else projectId,
                    applicationId = context.packageName,
                    databaseModeLabel = if (isPlaceholder) {
                        "Local Staging SQLite (Upload app/google-services.json to enable live Cloud Firestore)"
                    } else {
                        "Live Cloud Firestore Connected ($projectId)"
                    },
                    authEmail = currentUser?.email
                )
            } else {
                FirebaseConnectionStatus(
                    isGoogleServicesPresent = false,
                    projectId = "Awaiting google-services.json",
                    applicationId = context.packageName,
                    databaseModeLabel = "Local Staging SQLite (Upload app/google-services.json to enable live Cloud Firestore)",
                    authEmail = null
                )
            }
        } catch (e: Exception) {
            FirebaseConnectionStatus(
                isGoogleServicesPresent = false,
                projectId = "Awaiting google-services.json",
                applicationId = context.packageName,
                databaseModeLabel = "Local Staging SQLite (Awaiting app/google-services.json)",
                authEmail = null
            )
        }
    }

    private fun getFirestoreOrNull(): FirebaseFirestore? {
        return try {
            val status = getConnectionStatus()
            if (!status.isGoogleServicesPresent) return null
            val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
            val customDbId = if (resId != 0) context.getString(resId) else ""
            if (customDbId.isNotBlank() && customDbId != "(default)") {
                FirebaseFirestore.getInstance(customDbId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            Log.w("AdminRepository", "Firestore not yet initialized: ${e.message}")
            null
        }
    }

    suspend fun ensureInitialConfigBootstrap(currentAdminEmail: String, currentAdminUid: String) {
        if (dao.getAdMobSettingsOnce() == null) {
            dao.upsertAdMobSettings(AdMobSettingsEntity(updatedByEmail = currentAdminEmail))
        }
        if (dao.getAppContentSettingsOnce() == null) {
            dao.upsertAppContentSettings(AppContentSettingsEntity(updatedByEmail = currentAdminEmail))
        }
        val existingAdmins = dao.getAllAdminAccountsOnce()
        if (existingAdmins.isEmpty()) {
            dao.upsertAdminAccount(
                AdminAccountEntity(
                    uid = currentAdminUid,
                    email = currentAdminEmail,
                    displayName = "Primary System Owner",
                    role = AdminRole.OWNER.name,
                    active = true,
                    permissionsSummary = "Full Owner Authority • All 14 Modules"
                )
            )
        }
    }

    suspend fun recordAuditLog(
        adminUid: String,
        adminEmail: String,
        adminRole: String,
        actionType: String,
        targetEntity: String,
        summary: String
    ) {
        val log = ActivityLogEntity(
            id = "log_${UUID.randomUUID().toString().take(8)}",
            adminUid = adminUid,
            adminEmail = adminEmail,
            adminRole = adminRole,
            actionType = actionType,
            targetEntity = targetEntity,
            summary = summary,
            createdAtMillis = System.currentTimeMillis()
        )
        dao.insertActivityLog(log)
        getFirestoreOrNull()?.let { db ->
            try {
                db.collection("adminActivityLogs").document(log.id).set(log).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("AdminRepository", "Firestore audit log sync deferred: ${e.message}")
            }
        }
    }

    // =========================================================================
    // IMAGE MANAGEMENT (STRICT URL-ONLY)
    // =========================================================================
    suspend fun saveImage(
        image: PromptImageEntity,
        adminUid: String,
        adminEmail: String,
        adminRole: String,
        isNew: Boolean
    ): Result<PromptImageEntity> {
        return try {
            dao.upsertImage(image)
            getFirestoreOrNull()?.let { db ->
                db.collection("images").document(image.id).set(image, SetOptions.merge()).await()
            }
            recordAuditLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                adminRole = adminRole,
                actionType = if (isNew) "IMAGE_CREATED" else "IMAGE_UPDATED",
                targetEntity = "images/${image.id}",
                summary = "${if (isNew) "Added" else "Updated"} image '${image.title}' (${image.status}) -> ${image.deepLink}"
            )
            Result.success(image)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteImage(
        image: PromptImageEntity,
        adminUid: String,
        adminEmail: String,
        adminRole: String
    ): Result<Unit> {
        return try {
            dao.deleteImageById(image.id)
            getFirestoreOrNull()?.let { db ->
                db.collection("images").document(image.id).delete().await()
            }
            recordAuditLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                adminRole = adminRole,
                actionType = "IMAGE_DELETED",
                targetEntity = "images/${image.id}",
                summary = "Deleted image '${image.title}' (${image.shortName})"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // CATEGORY MANAGEMENT (WITH MIGRATION / DELETION SAFETY)
    // =========================================================================
    suspend fun saveCategory(
        category: PromptCategoryEntity,
        adminUid: String,
        adminEmail: String,
        adminRole: String,
        isNew: Boolean
    ): Result<Unit> {
        return try {
            dao.upsertCategory(category)
            getFirestoreOrNull()?.let { db ->
                db.collection("categories").document(category.id).set(category, SetOptions.merge()).await()
            }
            recordAuditLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                adminRole = adminRole,
                actionType = if (isNew) "CATEGORY_CREATED" else "CATEGORY_UPDATED",
                targetEntity = "categories/${category.id}",
                summary = "${if (isNew) "Created" else "Updated"} category '${category.name}' (Order #${category.displayOrder})"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCategoryWithResolution(
        category: PromptCategoryEntity,
        migrateToCategory: PromptCategoryEntity?,
        deleteContainedImages: Boolean,
        adminUid: String,
        adminEmail: String,
        adminRole: String
    ): Result<Unit> {
        return try {
            val imageCount = dao.countImagesInCategory(category.id)
            if (imageCount > 0) {
                if (migrateToCategory != null) {
                    dao.migrateCategoryImages(
                        sourceCategoryId = category.id,
                        targetCategoryId = migrateToCategory.id,
                        targetCategoryName = migrateToCategory.name,
                        now = System.currentTimeMillis()
                    )
                } else if (deleteContainedImages) {
                    dao.deleteImagesByCategory(category.id)
                } else {
                    return Result.failure(
                        IllegalStateException("Category contains $imageCount image(s). Choose a target migration category or confirm cascade deletion.")
                    )
                }
            }
            dao.deleteCategoryById(category.id)
            getFirestoreOrNull()?.let { db ->
                db.collection("categories").document(category.id).delete().await()
            }
            recordAuditLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                adminRole = adminRole,
                actionType = "CATEGORY_DELETED",
                targetEntity = "categories/${category.id}",
                summary = "Deleted category '${category.name}' ($imageCount images resolved)"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // USER MANAGEMENT
    // =========================================================================
    suspend fun upsertUserRecord(
        user: AppUserEntity,
        adminUid: String,
        adminEmail: String,
        adminRole: String,
        actionSummary: String
    ): Result<Unit> {
        return try {
            dao.upsertUser(user)
            getFirestoreOrNull()?.let { db ->
                db.collection("users").document(user.id).set(user, SetOptions.merge()).await()
            }
            // If an Admin Auth Cloud Function URL is configured, invoke it for server-side Firebase Auth state sync
            val appConfig = dao.getAppContentSettingsOnce()
            val authFnUrl = appConfig?.adminAuthFunctionUrl?.trim().orEmpty()
            if (authFnUrl.startsWith("https://")) {
                invokeAdminAuthCloudFunction(
                    endpoint = authFnUrl,
                    targetUserId = user.id,
                    disableAccount = user.accountStatus == "DISABLED",
                    reason = user.moderationNote
                )
            }
            recordAuditLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                adminRole = adminRole,
                actionType = "USER_STATUS_CHANGED",
                targetEntity = "users/${user.id}",
                summary = actionSummary
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun invokeAdminAuthCloudFunction(
        endpoint: String,
        targetUserId: String,
        disableAccount: Boolean,
        reason: String
    ) = withContext(Dispatchers.IO) {
        try {
            val idToken = try {
                FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
            } catch (_: Exception) {
                null
            }
            val payload = JSONObject().apply {
                put("targetUserId", targetUserId)
                put("disableAccount", disableAccount)
                put("reason", reason)
            }
            val reqBuilder = Request.Builder()
                .url(endpoint)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
            if (!idToken.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $idToken")
            }
            httpClient.newCall(reqBuilder.build()).execute().close()
        } catch (e: Exception) {
            Log.w("AdminRepository", "Admin Auth Function call failed: ${e.message}")
        }
    }

    // =========================================================================
    // COMMENT MODERATION
    // =========================================================================
    suspend fun updateCommentModeration(
        comment: CommentEntity,
        adminUid: String,
        adminEmail: String,
        adminRole: String,
        actionLabel: String
    ): Result<Unit> {
        return try {
            dao.upsertComment(comment)
            getFirestoreOrNull()?.let { db ->
                db.collection("comments").document(comment.id).set(comment, SetOptions.merge()).await()
            }
            recordAuditLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                adminRole = adminRole,
                actionType = "COMMENT_MODERATION",
                targetEntity = "comments/${comment.id}",
                summary = "$actionLabel comment by @${comment.username} on '${comment.imageTitle}'"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCommentPermanently(
        comment: CommentEntity,
        adminUid: String,
        adminEmail: String,
        adminRole: String
    ): Result<Unit> {
        return try {
            dao.deleteCommentById(comment.id)
            getFirestoreOrNull()?.let { db ->
                db.collection("comments").document(comment.id).delete().await()
            }
            recordAuditLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                adminRole = adminRole,
                actionType = "COMMENT_DELETED",
                targetEntity = "comments/${comment.id}",
                summary = "Permanently deleted comment by @${comment.username}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // GOOGLE ADMOB CONFIGURATION
    // =========================================================================
    suspend fun saveAdMobSettings(
        settings: AdMobSettingsEntity,
        adminUid: String,
        adminEmail: String,
        adminRole: String
    ): Result<Unit> {
        return try {
            dao.upsertAdMobSettings(settings)
            getFirestoreOrNull()?.let { db ->
                db.collection("advertisementSettings").document(settings.id).set(settings, SetOptions.merge()).await()
            }
            recordAuditLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                adminRole = adminRole,
                actionType = "ADMOB_CONFIG_UPDATED",
                targetEntity = "advertisementSettings/${settings.id}",
                summary = "Updated AdMob config (Global=${settings.globalAdsEnabled}, TestMode=${settings.testModeEnabled}, Freq=${settings.interstitialFrequencyActions})"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // APP CONTENT & PLATFORM CONFIGURATION
    // =========================================================================
    suspend fun saveAppContentSettings(
        settings: AppContentSettingsEntity,
        adminUid: String,
        adminEmail: String,
        adminRole: String
    ): Result<Unit> {
        return try {
            dao.upsertAppContentSettings(settings)
            getFirestoreOrNull()?.let { db ->
                db.collection("appSettings").document(settings.id).set(settings, SetOptions.merge()).await()
            }
            recordAuditLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                adminRole = adminRole,
                actionType = "APP_CONTENT_UPDATED",
                targetEntity = "appSettings/${settings.id}",
                summary = "Synchronized App Content settings for '${settings.appName}' (Scheme: ${settings.deepLinkScheme}://)"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // ONESIGNAL PUSH NOTIFICATION DISPATCH (SERVER-SIDE CLOUD FUNCTION)
    // =========================================================================
    suspend fun dispatchOneSignalNotification(
        notification: NotificationEntity,
        adminUid: String,
        adminEmail: String,
        adminRole: String
    ): Result<NotificationEntity> = withContext(Dispatchers.IO) {
        try {
            val appSettings = dao.getAppContentSettingsOnce() ?: AppContentSettingsEntity()
            val fnUrl = appSettings.oneSignalFunctionUrl.trim()
            val appId = appSettings.oneSignalAppId.trim()

            var finalRecord = notification

            if (fnUrl.startsWith("https://") && appId.isNotBlank()) {
                val idToken = try {
                    FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                } catch (_: Exception) {
                    null
                }
                val payload = JSONObject().apply {
                    put("notificationId", notification.id)
                    put("oneSignalAppId", appId)
                    put("title", notification.title)
                    put("message", notification.message)
                    put("targetAudience", notification.targetAudience)
                    put("imageId", notification.imageId)
                    put("imageThumbnailUrl", notification.imageThumbnailUrl)
                    put("deepLink", notification.deepLink)
                    if (notification.scheduleType == "SCHEDULE_LATER") {
                        put("scheduledIso", notification.scheduledTimeDisplay)
                    }
                }
                val requestBuilder = Request.Builder()
                    .url(fnUrl)
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                if (!idToken.isNullOrBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $idToken")
                }

                try {
                    httpClient.newCall(requestBuilder.build()).execute().use { response ->
                        val bodyStr = response.body?.string().orEmpty()
                        if (response.isSuccessful) {
                            val json = try { JSONObject(bodyStr) } catch (_: Exception) { JSONObject() }
                            val confirmedStatus = json.optString("status", "DISPATCHED_PROVIDER_CONFIRMED")
                            val details = json.optString("deliveryDetails", "Confirmed by Cloud Function HTTP ${response.code}")
                            finalRecord = notification.copy(
                                sendingStatus = confirmedStatus,
                                deliveryDetails = details
                            )
                        } else {
                            finalRecord = notification.copy(
                                sendingStatus = "FAILED",
                                deliveryDetails = "Cloud Function HTTP ${response.code}: ${bodyStr.take(160)}"
                            )
                        }
                    }
                } catch (netErr: Exception) {
                    finalRecord = notification.copy(
                        sendingStatus = "FAILED",
                        deliveryDetails = "Network error calling Cloud Function: ${netErr.localizedMessage ?: "Unreachable"}"
                    )
                }
            } else {
                // Provider response is not available until Cloud Function URL + OneSignal App ID are configured.
                // Never fabricate a "Delivered" status without provider confirmation.
                val statusWhenUnconfigured = if (notification.scheduleType == "SCHEDULE_LATER") {
                    "SCHEDULED"
                } else {
                    "QUEUED_FOR_SERVER"
                }
                finalRecord = notification.copy(
                    sendingStatus = statusWhenUnconfigured,
                    deliveryDetails = "Stored in Firestore notifications/${notification.id}. Configure OneSignal Cloud Function URL & App ID in Settings to enable live provider dispatch confirmation."
                )
            }

            dao.upsertNotification(finalRecord)
            getFirestoreOrNull()?.let { db ->
                db.collection("notifications").document(finalRecord.id).set(finalRecord, SetOptions.merge()).await()
            }
            recordAuditLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                adminRole = adminRole,
                actionType = "NOTIFICATION_CREATED",
                targetEntity = "notifications/${finalRecord.id}",
                summary = "Notification '${finalRecord.title}' (${finalRecord.sendingStatus}) -> Deep Link: ${finalRecord.deepLink}"
            )
            Result.success(finalRecord)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // ADMINISTRATOR & ROLE MANAGEMENT
    // =========================================================================
    suspend fun saveAdminAccount(
        adminAccount: AdminAccountEntity,
        callerUid: String,
        callerEmail: String,
        callerRole: String
    ): Result<Unit> {
        return try {
            dao.upsertAdminAccount(adminAccount)
            getFirestoreOrNull()?.let { db ->
                db.collection("adminUsers").document(adminAccount.uid).set(adminAccount, SetOptions.merge()).await()
            }
            recordAuditLog(
                adminUid = callerUid,
                adminEmail = callerEmail,
                adminRole = callerRole,
                actionType = "ADMIN_ROLE_UPDATED",
                targetEntity = "adminUsers/${adminAccount.uid}",
                summary = "Configured administrator ${adminAccount.email} as ${adminAccount.role} (Active=${adminAccount.active})"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeAdminAccount(
        targetAdmin: AdminAccountEntity,
        callerUid: String,
        callerEmail: String,
        callerRole: String
    ): Result<Unit> {
        return try {
            dao.deleteAdminAccount(targetAdmin.uid)
            getFirestoreOrNull()?.let { db ->
                db.collection("adminUsers").document(targetAdmin.uid).delete().await()
            }
            recordAuditLog(
                adminUid = callerUid,
                adminEmail = callerEmail,
                adminRole = callerRole,
                actionType = "ADMIN_REMOVED",
                targetEntity = "adminUsers/${targetAdmin.uid}",
                summary = "Revoked administrator access for ${targetAdmin.email}"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // TWO-WAY FIRESTORE BATCH SYNC (WHEN google-services.json IS UPLOADED)
    // =========================================================================
    suspend fun syncAllWithFirestore(
        adminUid: String,
        adminEmail: String,
        adminRole: String
    ): Result<String> {
        val db = getFirestoreOrNull()
            ?: return Result.failure(IllegalStateException("app/google-services.json is not yet uploaded. Place your google-services.json inside the app/ folder and rebuild to connect to live Cloud Firestore."))

        return try {
            // 1. Push local records to Firestore
            val localCategories = dao.getAllCategoriesOnce()
            for (cat in localCategories) {
                db.collection("categories").document(cat.id).set(cat, SetOptions.merge()).await()
            }
            val localImages = dao.getAllImagesOnce()
            for (img in localImages) {
                db.collection("images").document(img.id).set(img, SetOptions.merge()).await()
            }
            dao.getAdMobSettingsOnce()?.let { admob ->
                db.collection("advertisementSettings").document(admob.id).set(admob, SetOptions.merge()).await()
            }
            dao.getAppContentSettingsOnce()?.let { content ->
                db.collection("appSettings").document(content.id).set(content, SetOptions.merge()).await()
            }

            // 2. Pull remote collections from Firestore (paginated / bounded for free-tier safety)
            val remoteCategories = db.collection("categories").limit(100).get().await()
                .toObjects(PromptCategoryEntity::class.java)
            if (remoteCategories.isNotEmpty()) dao.upsertCategories(remoteCategories)

            val remoteImages = db.collection("images").limit(200).get().await()
                .toObjects(PromptImageEntity::class.java)
            if (remoteImages.isNotEmpty()) dao.upsertImages(remoteImages)

            val remoteUsers = db.collection("users").limit(200).get().await()
                .toObjects(AppUserEntity::class.java)
            if (remoteUsers.isNotEmpty()) dao.upsertUsers(remoteUsers)

            val remoteComments = db.collection("comments").limit(200).get().await()
                .toObjects(CommentEntity::class.java)
            if (remoteComments.isNotEmpty()) dao.upsertComments(remoteComments)

            val remoteNotifications = db.collection("notifications").limit(100).get().await()
                .toObjects(NotificationEntity::class.java)
            if (remoteNotifications.isNotEmpty()) dao.upsertNotifications(remoteNotifications)

            val remoteAdmins = db.collection("adminUsers").limit(50).get().await()
                .toObjects(AdminAccountEntity::class.java)
            if (remoteAdmins.isNotEmpty()) dao.upsertAdminAccounts(remoteAdmins)

            val msg = "Synchronized ${localImages.size + remoteImages.size} images, ${localCategories.size + remoteCategories.size} categories, ${remoteUsers.size} users, and ${remoteComments.size} comments with Firestore."
            recordAuditLog(
                adminUid = adminUid,
                adminEmail = adminEmail,
                adminRole = adminRole,
                actionType = "FIRESTORE_FULL_SYNC",
                targetEntity = "firestore/all_collections",
                summary = msg
            )
            Result.success(msg)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
