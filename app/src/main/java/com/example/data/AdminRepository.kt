package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.model.AdminRole
import com.example.model.AdvertisementSettingsModel
import com.example.model.AppGlobalSettingsModel
import com.example.model.FirestoreAiToolItem
import com.example.model.FirestoreCategoryItem
import com.example.model.FirestoreCommentItem
import com.example.model.FirestoreImageItem
import com.example.model.FirestoreNotificationItem
import com.example.model.FirestoreReportItem
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.UUID
import java.util.concurrent.TimeoutException

data class FirebaseConnectionStatus(
    val isGoogleServicesPresent: Boolean,
    val projectId: String,
    val applicationId: String,
    val databaseModeLabel: String,
    val authEmail: String?
)

class AdminRepository(
    private val context: Context,
    @Suppress("unused") private val dao: AdminDao? = null
) {
    companion object {
        private const val TAG = "AdminRepository"
        private const val WRITE_TIMEOUT_MS = 15_000L
        private const val SERVER_TIMEOUT_BANGA_MSG = "সার্ভারে সেভ হয়নি। ইন্টারনেট বা অনুমতি দেখুন"

        val OWNER_EMAIL_ALLOWLIST: Set<String> = setOf(
            "sfforhadvai@gmail.com"
        )

        @Volatile
        private var isMemoryCacheConfigured = false
        private val settingsLock = Any()

        /**
         * Returns the "(default)" Firestore database for the Firebase project and disables
         * offline disk persistence by configuring MemoryCacheSettings only, so ghost items
         * can never be persisted on the phone.
         */
        fun getDefaultServerFirestore(context: Context): FirebaseFirestore? {
            return try {
                if (FirebaseApp.getApps(context).isEmpty()) return null
                // Explicitly use the "(default)" database of the default FirebaseApp
                val db = FirebaseFirestore.getInstance(FirebaseApp.getInstance())
                if (!isMemoryCacheConfigured) {
                    synchronized(settingsLock) {
                        if (!isMemoryCacheConfigured) {
                            try {
                                val memoryOnlySettings = FirebaseFirestoreSettings.Builder()
                                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                                    .build()
                                db.firestoreSettings = memoryOnlySettings
                                isMemoryCacheConfigured = true
                                Log.i(TAG, "Configured Firestore '(default)' database with MemoryCacheSettings (offline persistence disabled)")
                            } catch (settingsErr: Exception) {
                                // Ignore if settings were already locked on this instance
                                isMemoryCacheConfigured = true
                                Log.w(TAG, "FirestoreSettings already configured: ${settingsErr.message}")
                            }
                        }
                    }
                }
                db
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Firestore '(default)' instance", e)
                null
            }
        }
    }

    private val _images = MutableStateFlow<List<FirestoreImageItem>>(emptyList())
    val images: StateFlow<List<FirestoreImageItem>> = _images.asStateFlow()

    private val _categories = MutableStateFlow<List<FirestoreCategoryItem>>(emptyList())
    val categories: StateFlow<List<FirestoreCategoryItem>> = _categories.asStateFlow()

    private val _aiTools = MutableStateFlow<List<FirestoreAiToolItem>>(emptyList())
    val aiTools: StateFlow<List<FirestoreAiToolItem>> = _aiTools.asStateFlow()

    private val _comments = MutableStateFlow<List<FirestoreCommentItem>>(emptyList())
    val comments: StateFlow<List<FirestoreCommentItem>> = _comments.asStateFlow()

    private val _reports = MutableStateFlow<List<FirestoreReportItem>>(emptyList())
    val reports: StateFlow<List<FirestoreReportItem>> = _reports.asStateFlow()

    private val _notifications = MutableStateFlow<List<FirestoreNotificationItem>>(emptyList())
    val notifications: StateFlow<List<FirestoreNotificationItem>> = _notifications.asStateFlow()

    private val _adSettings = MutableStateFlow(AdvertisementSettingsModel())
    val adSettings: StateFlow<AdvertisementSettingsModel> = _adSettings.asStateFlow()

    private val _appSettings = MutableStateFlow(AppGlobalSettingsModel())
    val appSettings: StateFlow<AppGlobalSettingsModel> = _appSettings.asStateFlow()

    private val _isLoadingData = MutableStateFlow(false)
    val isLoadingData: StateFlow<Boolean> = _isLoadingData.asStateFlow()

    private val activeListeners = mutableListOf<ListenerRegistration>()

    fun getConnectionStatus(): FirebaseConnectionStatus {
        return try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isNotEmpty()) {
                val app = FirebaseApp.getInstance()
                val projectId = app.options.projectId ?: BuildConfig.FIREBASE_PROJECT_ID
                val isPlaceholder = projectId == "remixed-project-id" || projectId.isBlank()
                val currentUser = try {
                    FirebaseAuth.getInstance().currentUser
                } catch (_: Exception) {
                    null
                }
                FirebaseConnectionStatus(
                    isGoogleServicesPresent = !isPlaceholder,
                    projectId = if (isPlaceholder) BuildConfig.FIREBASE_PROJECT_ID else projectId,
                    applicationId = context.packageName,
                    databaseModeLabel = if (isPlaceholder) "ফায়ারবেস সংযোগ অপেক্ষমান" else "ফায়ারবেস সংযুক্ত ($projectId)",
                    authEmail = currentUser?.email
                )
            } else {
                FirebaseConnectionStatus(
                    isGoogleServicesPresent = false,
                    projectId = BuildConfig.FIREBASE_PROJECT_ID,
                    applicationId = context.packageName,
                    databaseModeLabel = "ফায়ারবেস সংযোগ অপেক্ষমান",
                    authEmail = null
                )
            }
        } catch (_: Exception) {
            FirebaseConnectionStatus(
                isGoogleServicesPresent = false,
                projectId = BuildConfig.FIREBASE_PROJECT_ID,
                applicationId = context.packageName,
                databaseModeLabel = "ফায়ারবেস সংযোগ অপেক্ষমান",
                authEmail = null
            )
        }
    }

    fun getFirestoreOrNull(): FirebaseFirestore? = getDefaultServerFirestore(context)

    /**
     * Logs the full exception with Log.e and returns a detailed Bangla error message
     * containing the Firestore error code and short explanation.
     */
    fun mapErrorToBangla(error: Throwable): String {
        Log.e(TAG, "Firestore operation error: ${error.javaClass.name}: ${error.message}", error)

        if (error is TimeoutCancellationException || error is TimeoutException) {
            return SERVER_TIMEOUT_BANGA_MSG
        }

        val firestoreEx = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

        if (firestoreEx != null) {
            val codeName = firestoreEx.code.name
            val shortDetail = firestoreEx.message
                ?.replace(Regex("\\s+"), " ")
                ?.trim()
                ?.take(120)
                .orEmpty()
            return when (firestoreEx.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    if (shortDetail.isNotEmpty()) {
                        "অনুমতি নেই / ডাটা নিয়মের সাথে মেলেনি ($codeName: $shortDetail)"
                    } else {
                        "অনুমতি নেই / ডাটা নিয়মের সাথে মেলেনি ($codeName)"
                    }
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    "$SERVER_TIMEOUT_BANGA_MSG ($codeName)"
                FirebaseFirestoreException.Code.NOT_FOUND ->
                    "সার্ভারে ডকুমেন্ট পাওয়া যায়নি ($codeName: $shortDetail)"
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    "লগ ইন সেশন শেষ হয়েছে, আবার লগ ইন করুন ($codeName)"
                FirebaseFirestoreException.Code.INVALID_ARGUMENT ->
                    "ডাটা নিয়মের সাথে মেলেনি ($codeName: $shortDetail)"
                FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                    "ডাটাবেস শর্ত বা ইনডেক্স মেলেনি ($codeName: $shortDetail)"
                else ->
                    "ফায়ারবেস ত্রুটি ($codeName): ${shortDetail.ifEmpty { "অজানা সমস্যা" }}"
            }
        }

        val msg = error.message.orEmpty()
        if (msg.contains("সার্ভারে সেভ হয়নি")) {
            return SERVER_TIMEOUT_BANGA_MSG
        }
        if (msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
            msg.contains("Missing or insufficient permissions", ignoreCase = true)
        ) {
            return "অনুমতি নেই / ডাটা নিয়মের সাথে মেলেনি (PERMISSION_DENIED: ${msg.take(120)})"
        }
        if (msg.contains("UNAVAILABLE", ignoreCase = true) ||
            msg.contains("DEADLINE_EXCEEDED", ignoreCase = true) ||
            msg.contains("network", ignoreCase = true) ||
            msg.contains("offline", ignoreCase = true)
        ) {
            return SERVER_TIMEOUT_BANGA_MSG
        }
        return "ত্রুটি (${error.javaClass.simpleName}): ${msg.take(140).ifEmpty { "অজানা সমস্যা হয়েছে" }}"
    }

    /**
     * Executes a Firestore write inside a strict 15-second timeout and logs any failure with Log.e.
     * Catch order ensures TimeoutCancellationException is caught BEFORE general CancellationException.
     */
    private suspend fun executeWithServerTimeout(
        operationName: String,
        targetPath: String,
        block: suspend (FirebaseFirestore) -> Unit
    ): Result<Unit> {
        val db = getFirestoreOrNull()
            ?: return Result.failure(IllegalStateException("ফায়ারবেস ইনিশিয়ালাইজ হয়নি"))
        return try {
            withTimeout(WRITE_TIMEOUT_MS) {
                block(db)
            }
            Result.success(Unit)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "[$operationName] Timed out after ${WRITE_TIMEOUT_MS}ms waiting for server ack on $targetPath", e)
            Result.failure(TimeoutException(SERVER_TIMEOUT_BANGA_MSG))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "[$operationName] Server rejected or failed write on $targetPath: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Checks authorization from the SERVER on Firestore `adminUsers/{uid}` (with a 15s timeout),
     * and supports the verified owner allowlist (`OWNER_EMAIL_ALLOWLIST`, e.g. `sfforhadvai@gmail.com`).
     * If an allowlisted owner signs in and `adminUsers/{uid}` does not exist yet on the server,
     * automatically bootstraps `adminUsers/{uid}` (best-effort) and authorizes as `AdminRole.OWNER`.
     */
    suspend fun verifyAdminUserFromFirestore(
        uid: String,
        email: String? = null,
        isEmailVerified: Boolean = true
    ): Result<AdminRole?> {
        val cleanEmail = email?.trim()?.lowercase().orEmpty()
        val isAllowlistedOwner = cleanEmail.isNotEmpty() &&
            cleanEmail in OWNER_EMAIL_ALLOWLIST &&
            isEmailVerified

        val db = getFirestoreOrNull()
        if (db == null) {
            return if (isAllowlistedOwner) {
                Result.success(AdminRole.OWNER)
            } else {
                Result.failure(IllegalStateException("ফায়ারবেস ইনিশিয়ালাইজ হয়নি"))
            }
        }

        return try {
            val snapshot = withTimeout(WRITE_TIMEOUT_MS) {
                db.collection("adminUsers").document(uid).get(Source.SERVER).await()
            }
            if (!snapshot.exists()) {
                Log.w(TAG, "adminUsers/$uid does not exist on server (allowlistedOwner=$isAllowlistedOwner)")
                if (isAllowlistedOwner) {
                    runCatching {
                        withTimeout(5_000L) {
                            db.collection("adminUsers").document(uid).set(
                                mapOf(
                                    "email" to cleanEmail,
                                    "role" to "owner",
                                    "active" to true,
                                    "createdAt" to FieldValue.serverTimestamp(),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            ).await()
                        }
                    }.onFailure { bootstrapErr ->
                        Log.w(TAG, "Auto-bootstrap of adminUsers/$uid skipped: ${bootstrapErr.message}")
                    }
                    return Result.success(AdminRole.OWNER)
                }
                return Result.success(null)
            }

            val isActive = snapshot.getBoolean("active") ?: isAllowlistedOwner
            val roleRaw = snapshot.getString("role")
            val role = AdminRole.fromFirestoreValue(roleRaw)
                ?: if (isAllowlistedOwner) AdminRole.OWNER else null

            if (isActive && role != null) {
                Result.success(role)
            } else if (isAllowlistedOwner) {
                Result.success(AdminRole.OWNER)
            } else {
                Log.w(TAG, "adminUsers/$uid exists on server but active=$isActive, role=$roleRaw")
                Result.success(null)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "verifyAdminUserFromFirestore timed out after ${WRITE_TIMEOUT_MS}ms for uid=$uid", e)
            if (isAllowlistedOwner) {
                Result.success(AdminRole.OWNER)
            } else {
                Result.failure(TimeoutException("সার্ভারের সাথে সংযোগ করা যাচ্ছে না। ইন্টারনেট পরীক্ষা করুন"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "verifyAdminUserFromFirestore failed for uid=$uid: ${e.message}", e)
            if (isAllowlistedOwner) {
                return Result.success(AdminRole.OWNER)
            }
            val msg = e.message.orEmpty()
            if ((e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) ||
                msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
                msg.contains("Missing or insufficient permissions", ignoreCase = true)
            ) {
                Result.success(null)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Generates a new Firestore document ID once when the "নতুন ছবি" form opens.
     */
    fun generateNewImageDocumentId(): String {
        return getFirestoreOrNull()?.collection("images")?.document()?.id
            ?: "img_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    }

    /**
     * Generates a new Firestore document ID once when the "নতুন নোটিফিকেশন" form opens.
     */
    fun generateNewNotificationDocumentId(): String {
        return getFirestoreOrNull()?.collection("notifications")?.document()?.id
            ?: "notif_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    }

    /**
     * Generates a new Firestore document ID once when adding a new AI Tool.
     */
    fun generateNewAiToolDocumentId(): String {
        return getFirestoreOrNull()?.collection("aiTools")?.document()?.id
            ?: "tool_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    }

    /**
     * Starts real-time Firestore listeners with MetadataChanges.INCLUDE so we can distinguish
     * server-confirmed snapshots from pending/cache snapshots.
     */
    fun startRealtimeListeners(role: AdminRole, onListenerError: (String) -> Unit) {
        stopRealtimeListeners()
        val db = getFirestoreOrNull() ?: return
        _isLoadingData.value = true

        // Comments listener (all roles)
        activeListeners += db.collection("comments")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
                if (err != null) {
                    Log.e(TAG, "comments listener error: ${err.message}", err)
                    onListenerError(mapErrorToBangla(err))
                    _isLoadingData.value = false
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener
                val snapFromCache = snap.metadata.isFromCache()
                val list = snap.documents
                    .mapNotNull { doc ->
                        val confirmed = !snapFromCache && !doc.metadata.isFromCache() && !doc.metadata.hasPendingWrites()
                        parseComment(doc, isServerConfirmed = confirmed)
                    }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
                _comments.value = list
                _isLoadingData.value = false
            }

        // Reports listener (all roles)
        activeListeners += db.collection("reports")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
                if (err != null) {
                    Log.e(TAG, "reports listener error: ${err.message}", err)
                    onListenerError(mapErrorToBangla(err))
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener
                val snapFromCache = snap.metadata.isFromCache()
                val list = snap.documents
                    .mapNotNull { doc ->
                        val confirmed = !snapFromCache && !doc.metadata.isFromCache() && !doc.metadata.hasPendingWrites()
                        parseReport(doc, isServerConfirmed = confirmed)
                    }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
                _reports.value = list
            }

        if (role == AdminRole.MODERATOR) {
            return
        }

        // Images listener (Owner & Admin)
        activeListeners += db.collection("images")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
                if (err != null) {
                    Log.e(TAG, "images listener error: ${err.message}", err)
                    onListenerError(mapErrorToBangla(err))
                    _isLoadingData.value = false
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener
                val snapFromCache = snap.metadata.isFromCache()
                val list = snap.documents
                    .mapNotNull { doc ->
                        val confirmed = !snapFromCache && !doc.metadata.isFromCache() && !doc.metadata.hasPendingWrites()
                        parseImage(doc, isServerConfirmed = confirmed)
                    }
                    .sortedWith(
                        compareBy<FirestoreImageItem> { it.displayOrder }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                _images.value = list
                _isLoadingData.value = false
            }

        // Categories listener (Owner & Admin)
        activeListeners += db.collection("categories")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
                if (err != null) {
                    Log.e(TAG, "categories listener error: ${err.message}", err)
                    onListenerError(mapErrorToBangla(err))
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener
                val snapFromCache = snap.metadata.isFromCache()
                val list = snap.documents
                    .mapNotNull { doc ->
                        val confirmed = !snapFromCache && !doc.metadata.isFromCache() && !doc.metadata.hasPendingWrites()
                        parseCategory(doc, isServerConfirmed = confirmed)
                    }
                    .sortedWith(compareBy<FirestoreCategoryItem> { it.sortOrder }.thenBy { it.name })
                _categories.value = list
            }

        // AI Tools listener (Owner & Admin) — sorted by sortOrder ascending, then name
        activeListeners += db.collection("aiTools")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
                if (err != null) {
                    Log.e(TAG, "aiTools listener error: ${err.message}", err)
                    onListenerError(mapErrorToBangla(err))
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener
                val snapFromCache = snap.metadata.isFromCache()
                val list = snap.documents
                    .mapNotNull { doc ->
                        val confirmed = !snapFromCache && !doc.metadata.isFromCache() && !doc.metadata.hasPendingWrites()
                        parseAiTool(doc, isServerConfirmed = confirmed)
                    }
                    .sortedWith(compareBy<FirestoreAiToolItem> { it.sortOrder }.thenBy { it.name })
                _aiTools.value = list
            }

        // Notifications listener (Owner & Admin) — newest first
        activeListeners += db.collection("notifications")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
                if (err != null) {
                    Log.e(TAG, "notifications listener error: ${err.message}", err)
                    onListenerError(mapErrorToBangla(err))
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener
                val snapFromCache = snap.metadata.isFromCache()
                val list = snap.documents
                    .mapNotNull { doc ->
                        val confirmed = !snapFromCache && !doc.metadata.isFromCache() && !doc.metadata.hasPendingWrites()
                        parseNotification(doc, isServerConfirmed = confirmed)
                    }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
                _notifications.value = list
            }

        // Advertisement settings listener (advertisementSettings/global)
        activeListeners += db.collection("advertisementSettings").document("global")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
                if (err != null) {
                    Log.e(TAG, "advertisementSettings/global listener error: ${err.message}", err)
                    return@addSnapshotListener
                }
                if (snap != null && snap.exists() && !snap.metadata.hasPendingWrites()) {
                    _adSettings.value = parseAdSettings(snap)
                }
            }

        // App settings listener (appSettings/global)
        activeListeners += db.collection("appSettings").document("global")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
                if (err != null) {
                    Log.e(TAG, "appSettings/global listener error: ${err.message}", err)
                    return@addSnapshotListener
                }
                if (snap != null && snap.exists() && !snap.metadata.hasPendingWrites()) {
                    _appSettings.value = parseAppSettings(snap)
                }
            }
    }

    fun stopRealtimeListeners() {
        activeListeners.forEach { runCatching { it.remove() } }
        activeListeners.clear()
    }

    // =========================================================================
    // PARSERS
    // =========================================================================
    private fun parseImage(doc: DocumentSnapshot, isServerConfirmed: Boolean): FirestoreImageItem? {
        return try {
            val rawStatus = doc.getString("status") ?: "published"
            val normalizedStatus = if (rawStatus.equals("draft", ignoreCase = true)) "draft" else "published"
            FirestoreImageItem(
                id = doc.id,
                title = doc.getString("title").orEmpty(),
                shortName = doc.getString("shortName").orEmpty(),
                description = doc.getString("description").orEmpty(),
                categoryId = doc.getString("categoryId")?.ifBlank { "uncategorized" } ?: "uncategorized",
                categoryName = doc.getString("categoryName")?.ifBlank { "অন্যান্য" } ?: "অন্যান্য",
                imageUrl = doc.getString("imageUrl").orEmpty(),
                aspectRatio = (doc.getDouble("aspectRatio") ?: 1.0).coerceIn(0.2, 5.0),
                featured = doc.getBoolean("featured") == true,
                trending = doc.getBoolean("trending") == true,
                recommended = doc.getBoolean("recommended") ?: true,
                collectionName = doc.getString("collectionName").orEmpty(),
                keywords = doc.getString("keywords").orEmpty(),
                displayOrder = (doc.getLong("displayOrder") ?: 1L).toInt(),
                status = normalizedStatus,
                viewCount = doc.getLong("viewCount") ?: 0L,
                likeCount = doc.getLong("likeCount") ?: 0L,
                commentCount = doc.getLong("commentCount") ?: 0L,
                unlockCount = doc.getLong("unlockCount") ?: 0L,
                shareCount = doc.getLong("shareCount") ?: 0L,
                createdAt = doc.getTimestamp("createdAt"),
                updatedAt = doc.getTimestamp("updatedAt"),
                isServerConfirmed = isServerConfirmed
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse image doc ${doc.id}", e)
            null
        }
    }

    private fun parseCategory(doc: DocumentSnapshot, isServerConfirmed: Boolean): FirestoreCategoryItem? {
        return try {
            val rawStatus = doc.getString("status") ?: "published"
            val normalizedStatus = if (rawStatus.equals("draft", ignoreCase = true)) "draft" else "published"
            FirestoreCategoryItem(
                id = doc.id,
                name = doc.getString("name").orEmpty(),
                description = doc.getString("description").orEmpty(),
                coverImageUrl = doc.getString("coverImageUrl").orEmpty(),
                imageCount = (doc.getLong("imageCount") ?: 0L).toInt().coerceAtLeast(0),
                sortOrder = (doc.getLong("sortOrder") ?: 1L).toInt(),
                status = normalizedStatus,
                createdAt = doc.getTimestamp("createdAt"),
                updatedAt = doc.getTimestamp("updatedAt"),
                isServerConfirmed = isServerConfirmed
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse category doc ${doc.id}", e)
            null
        }
    }

    private fun parseAiTool(doc: DocumentSnapshot, isServerConfirmed: Boolean): FirestoreAiToolItem? {
        return try {
            FirestoreAiToolItem(
                id = doc.id,
                name = doc.getString("name").orEmpty(),
                url = doc.getString("url").orEmpty(),
                logoUrl = doc.getString("logoUrl").orEmpty(),
                enabled = doc.getBoolean("enabled") ?: true,
                sortOrder = (doc.getLong("sortOrder") ?: 1L).toInt(),
                updatedAt = doc.getTimestamp("updatedAt"),
                isServerConfirmed = isServerConfirmed
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse aiTool doc ${doc.id}", e)
            null
        }
    }

    private fun parseComment(doc: DocumentSnapshot, isServerConfirmed: Boolean): FirestoreCommentItem? {
        return try {
            val rawStatus = doc.getString("status") ?: "visible"
            val normalizedStatus = if (rawStatus.equals("hidden", ignoreCase = true)) "hidden" else "visible"
            val text = doc.getString("text")
                ?: doc.getString("commentText")
                ?: doc.getString("content")
                ?: ""
            val userName = doc.getString("userName")
                ?: doc.getString("userDisplayName")
                ?: doc.getString("username")
                ?: doc.getString("userEmail")
                ?: "ব্যবহারকারী"
            FirestoreCommentItem(
                id = doc.id,
                imageId = doc.getString("imageId").orEmpty(),
                imageTitle = doc.getString("imageTitle").orEmpty(),
                userId = doc.getString("userId").orEmpty(),
                userName = userName,
                text = text,
                status = normalizedStatus,
                adminReply = doc.getString("adminReply").orEmpty(),
                createdAt = doc.getTimestamp("createdAt"),
                updatedAt = doc.getTimestamp("updatedAt"),
                isServerConfirmed = isServerConfirmed
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse comment doc ${doc.id}", e)
            null
        }
    }

    private fun parseReport(doc: DocumentSnapshot, isServerConfirmed: Boolean): FirestoreReportItem? {
        return try {
            val rawStatus = doc.getString("status") ?: "open"
            val normalizedStatus = when (rawStatus.lowercase()) {
                "reviewed" -> "reviewed"
                "dismissed" -> "dismissed"
                else -> "open"
            }
            FirestoreReportItem(
                id = doc.id,
                targetId = doc.getString("targetId") ?: doc.getString("imageId") ?: doc.getString("commentId") ?: "",
                targetType = doc.getString("targetType") ?: "image",
                imageId = doc.getString("imageId").orEmpty(),
                commentId = doc.getString("commentId").orEmpty(),
                reason = doc.getString("reason") ?: doc.getString("reportReason") ?: "রিপোর্ট",
                details = doc.getString("details") ?: doc.getString("description") ?: "",
                reporterId = doc.getString("reporterId") ?: doc.getString("userId") ?: "",
                status = normalizedStatus,
                createdAt = doc.getTimestamp("createdAt"),
                updatedAt = doc.getTimestamp("updatedAt"),
                isServerConfirmed = isServerConfirmed
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse report doc ${doc.id}", e)
            null
        }
    }

    private fun parseNotification(doc: DocumentSnapshot, isServerConfirmed: Boolean): FirestoreNotificationItem? {
        return try {
            val rawStatus = doc.getString("status") ?: "published"
            val normalizedStatus = if (rawStatus.equals("draft", ignoreCase = true)) "draft" else "published"
            FirestoreNotificationItem(
                id = doc.id,
                title = doc.getString("title").orEmpty(),
                body = doc.getString("body") ?: doc.getString("message") ?: "",
                imageId = doc.getString("imageId").orEmpty(),
                imageUrl = doc.getString("imageUrl").orEmpty(),
                categoryName = doc.getString("categoryName")?.ifBlank { "অন্যান্য" } ?: "অন্যান্য",
                status = normalizedStatus,
                createdAt = doc.getTimestamp("createdAt"),
                isServerConfirmed = isServerConfirmed
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse notification doc ${doc.id}", e)
            null
        }
    }

    private fun parseAdSettings(doc: DocumentSnapshot): AdvertisementSettingsModel {
        return AdvertisementSettingsModel(
            allAdsEnabled = doc.getBoolean("allAdsEnabled") ?: true,
            homeBannerEnabled = doc.getBoolean("homeBannerEnabled") ?: true,
            detailBannerEnabled = doc.getBoolean("detailBannerEnabled") ?: true,
            rewardedAdsEnabled = doc.getBoolean("rewardedAdsEnabled") ?: true,
            interstitialAdsEnabled = doc.getBoolean("interstitialAdsEnabled") ?: true,
            testAdMode = doc.getBoolean("testAdMode") ?: true,
            reusePreviousUnlocks = doc.getBoolean("reusePreviousUnlocks") ?: true,
            interstitialFrequency = (doc.getLong("interstitialFrequency") ?: 4L).toInt().coerceIn(1, 50),
            adFallbackMessage = doc.getString("adFallbackMessage").orEmpty(),
            bannerAdUnitId = doc.getString("bannerAdUnitId")?.trim().orEmpty(),
            rewardedAdUnitId = doc.getString("rewardedAdUnitId")?.trim().orEmpty(),
            interstitialAdUnitId = doc.getString("interstitialAdUnitId")?.trim().orEmpty(),
            appOpenAdsEnabled = doc.getBoolean("appOpenAdsEnabled") ?: true,
            appOpenAdUnitId = doc.getString("appOpenAdUnitId")?.trim().orEmpty()
        )
    }

    private fun parseAppSettings(doc: DocumentSnapshot): AppGlobalSettingsModel {
        return AppGlobalSettingsModel(
            playStoreUrl = doc.getString("playStoreUrl").orEmpty(),
            requireLoginForPrompts = doc.getBoolean("requireLoginForPrompts") ?: false,
            allowComments = doc.getBoolean("allowComments") ?: true,
            welcomeHeadline = doc.getString("welcomeHeadline").orEmpty(),
            welcomeSubheadline = doc.getString("welcomeSubheadline").orEmpty(),
            supportEmail = doc.getString("supportEmail").orEmpty()
        )
    }

    // =========================================================================
    // IMAGES + IMAGE PROMPTS (ATOMIC WRITEBATCH, SERVER ACK + Source.SERVER VERIFY)
    // =========================================================================
    suspend fun fetchPromptTextForImage(imageId: String): Result<String> {
        val db = getFirestoreOrNull()
            ?: return Result.failure(IllegalStateException("ফায়ারবেস সংযোগ নেই"))
        return try {
            val snap = withTimeout(WRITE_TIMEOUT_MS) {
                db.collection("imagePrompts").document(imageId).get(Source.SERVER).await()
            }
            val text = snap.getString("promptText")
                ?: snap.getString("prompt")
                ?: ""
            Result.success(text)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "fetchPromptTextForImage timed out for imageId=$imageId", e)
            Result.failure(TimeoutException(SERVER_TIMEOUT_BANGA_MSG))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "fetchPromptTextForImage failed for imageId=$imageId: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun isPermissionDenied(error: Throwable): Boolean {
        val firestoreEx = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            return true
        }
        val msg = error.message.orEmpty()
        return msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
            msg.contains("Missing or insufficient permissions", ignoreCase = true)
    }

    /**
     * Atomically creates or updates `images/{documentId}` and `imagePrompts/{documentId}`
     * in ONE writeBatch, waits for server acknowledgment within 15 seconds, and updates
     * category imageCount in a best-effort call after the batch commits.
     */
    suspend fun saveImageAndPromptAtomic(
        documentId: String,
        isEdit: Boolean,
        previousCategoryId: String?,
        title: String,
        shortName: String,
        description: String,
        categoryId: String,
        categoryName: String,
        imageUrl: String,
        aspectRatio: Double,
        featured: Boolean,
        trending: Boolean,
        recommended: Boolean,
        keywords: String,
        displayOrder: Int,
        status: String,
        promptText: String
    ): Result<Unit> {
        return executeWithServerTimeout(
            operationName = if (isEdit) "updateImageAndPrompt" else "createImageAndPrompt",
            targetPath = "images/$documentId + imagePrompts/$documentId"
        ) { db ->
            val imageRef = db.collection("images").document(documentId)
            val promptRef = db.collection("imagePrompts").document(documentId)

            val cleanCategoryId = categoryId.trim().ifEmpty { "uncategorized" }
            val cleanCategoryName = categoryName.trim().ifEmpty { "অন্যান্য" }
            val clampedRatio: Double = aspectRatio.coerceIn(0.2, 5.0)
            val cleanStatus = if (status.equals("draft", ignoreCase = true)) "draft" else "published"
            val cleanPromptText = promptText.trim().take(5000)

            try {
                val batch = db.batch()
                if (!isEdit) {
                    // CREATE: exact 21 fields in images/{id}, Long for all integer fields, Double for aspectRatio
                    val createImageMap = hashMapOf<String, Any>(
                        "title" to title.trim().take(150),
                        "shortName" to shortName.trim().take(80),
                        "description" to description.trim().take(1000),
                        "categoryId" to cleanCategoryId,
                        "categoryName" to cleanCategoryName,
                        "imageUrl" to imageUrl.trim(),
                        "aspectRatio" to clampedRatio,
                        "featured" to featured,
                        "trending" to trending,
                        "recommended" to recommended,
                        "collectionName" to "",
                        "keywords" to keywords.trim().take(500),
                        "displayOrder" to displayOrder.toLong(),
                        "status" to cleanStatus,
                        "viewCount" to 0L,
                        "likeCount" to 0L,
                        "commentCount" to 0L,
                        "unlockCount" to 0L,
                        "shareCount" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(imageRef, createImageMap)
                } else {
                    // EDIT: update ONLY the allowed mutable fields, preserving counters and createdAt
                    val updateImageMap = hashMapOf<String, Any>(
                        "title" to title.trim().take(150),
                        "shortName" to shortName.trim().take(80),
                        "description" to description.trim().take(1000),
                        "categoryId" to cleanCategoryId,
                        "categoryName" to cleanCategoryName,
                        "imageUrl" to imageUrl.trim(),
                        "aspectRatio" to clampedRatio,
                        "featured" to featured,
                        "trending" to trending,
                        "recommended" to recommended,
                        "status" to cleanStatus,
                        "keywords" to keywords.trim().take(500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.update(imageRef, updateImageMap)
                }

                // imagePrompts/{id} contains EXACTLY: promptText, updatedAt
                val promptMap = hashMapOf<String, Any>(
                    "promptText" to cleanPromptText,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(promptRef, promptMap)
                batch.commit().await()
            } catch (primaryErr: Exception) {
                if (!isPermissionDenied(primaryErr)) {
                    throw primaryErr
                }
                Log.w(TAG, "Primary schema saveImageAndPromptAtomic rejected by rules; retrying with legacy firestore.rules fields", primaryErr)
                val fallbackBatch = db.batch()
                val legacyStatus = if (cleanStatus == "draft") "DRAFT" else "PUBLISHED"
                val deepLinkVal = "promptlens://image/$documentId"
                if (!isEdit) {
                    val fallbackCreateMap = hashMapOf<String, Any>(
                        "title" to title.trim().take(150),
                        "shortName" to shortName.trim().take(80),
                        "description" to description.trim().take(1000),
                        "categoryId" to cleanCategoryId,
                        "categoryName" to cleanCategoryName,
                        "imageUrl" to imageUrl.trim(),
                        "deepLink" to deepLinkVal,
                        "aspectRatio" to clampedRatio,
                        "featured" to featured,
                        "trending" to trending,
                        "recommended" to recommended,
                        "collectionName" to "",
                        "keywords" to keywords.trim().take(500),
                        "displayOrder" to displayOrder.toLong(),
                        "status" to legacyStatus,
                        "viewCount" to 0L,
                        "likeCount" to 0L,
                        "commentCount" to 0L,
                        "unlockCount" to 0L,
                        "shareCount" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    fallbackBatch.set(imageRef, fallbackCreateMap)
                } else {
                    val fallbackUpdateMap = hashMapOf<String, Any>(
                        "title" to title.trim().take(150),
                        "shortName" to shortName.trim().take(80),
                        "description" to description.trim().take(1000),
                        "categoryId" to cleanCategoryId,
                        "categoryName" to cleanCategoryName,
                        "imageUrl" to imageUrl.trim(),
                        "deepLink" to deepLinkVal,
                        "aspectRatio" to clampedRatio,
                        "featured" to featured,
                        "trending" to trending,
                        "recommended" to recommended,
                        "status" to legacyStatus,
                        "keywords" to keywords.trim().take(500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    fallbackBatch.update(imageRef, fallbackUpdateMap)
                }
                val fallbackPromptMap = hashMapOf<String, Any>(
                    "imageId" to documentId,
                    "prompt" to cleanPromptText,
                    "promptText" to cleanPromptText,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                fallbackBatch.set(promptRef, fallbackPromptMap)
                fallbackBatch.commit().await()
            }

            // 2. Best-effort category imageCount update after batch succeeds
            runCatching {
                if (!isEdit) {
                    val targetCat = _categories.value.firstOrNull { it.id == cleanCategoryId }
                    if (cleanCategoryId != "uncategorized" && targetCat != null) {
                        db.collection("categories").document(cleanCategoryId).update(
                            mapOf(
                                "imageCount" to FieldValue.increment(1L),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                } else {
                    val oldCatId = previousCategoryId?.trim().orEmpty().ifEmpty { "uncategorized" }
                    if (oldCatId != cleanCategoryId) {
                        val oldCat = _categories.value.firstOrNull { it.id == oldCatId }
                        if (oldCatId != "uncategorized" && oldCat != null && oldCat.imageCount > 0) {
                            db.collection("categories").document(oldCatId).update(
                                mapOf(
                                    "imageCount" to FieldValue.increment(-1L),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            ).await()
                        }
                        val newCat = _categories.value.firstOrNull { it.id == cleanCategoryId }
                        if (cleanCategoryId != "uncategorized" && newCat != null) {
                            db.collection("categories").document(cleanCategoryId).update(
                                mapOf(
                                    "imageCount" to FieldValue.increment(1L),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            ).await()
                        }
                    }
                }
            }.onFailure { catErr ->
                Log.w(TAG, "Best-effort category counter update skipped: ${catErr.message}")
            }

            // 3. Re-read from Source.SERVER (or construct confirmed item) to update local StateFlow immediately
            val confirmedItem = runCatching {
                val serverImageSnap = imageRef.get(Source.SERVER).await()
                if (serverImageSnap.exists()) {
                    parseImage(serverImageSnap, isServerConfirmed = true)
                } else {
                    null
                }
            }.getOrNull() ?: FirestoreImageItem(
                id = documentId,
                title = title.trim().take(150),
                shortName = shortName.trim().take(80),
                description = description.trim().take(1000),
                categoryId = cleanCategoryId,
                categoryName = cleanCategoryName,
                imageUrl = imageUrl.trim(),
                aspectRatio = clampedRatio,
                featured = featured,
                trending = trending,
                recommended = recommended,
                collectionName = "",
                keywords = keywords.trim().take(500),
                displayOrder = displayOrder,
                status = cleanStatus,
                createdAt = com.google.firebase.Timestamp.now(),
                updatedAt = com.google.firebase.Timestamp.now(),
                isServerConfirmed = true
            )

            val updatedList = (_images.value.filterNot { it.id == confirmedItem.id } + confirmedItem)
                .sortedWith(
                    compareBy<FirestoreImageItem> { it.displayOrder }
                        .thenByDescending { it.createdAt?.seconds ?: 0L }
                )
            _images.value = updatedList
        }
    }

    /**
     * Atomically deletes `images/{id}` and `imagePrompts/{id}` in ONE writeBatch,
     * and decrements `categories/{categoryId}.imageCount` if positive.
     */
    suspend fun deleteImageAtomic(image: FirestoreImageItem): Result<Unit> {
        return executeWithServerTimeout(
            operationName = "deleteImageAtomic",
            targetPath = "images/${image.id} + imagePrompts/${image.id}"
        ) { db ->
            val batch = db.batch()
            val imageRef = db.collection("images").document(image.id)
            val promptRef = db.collection("imagePrompts").document(image.id)
            batch.delete(imageRef)
            batch.delete(promptRef)

            batch.commit().await()

            runCatching {
                val catId = image.categoryId.trim()
                val existingCat = _categories.value.firstOrNull { it.id == catId }
                if (catId.isNotEmpty() && catId != "uncategorized" && existingCat != null && existingCat.imageCount > 0) {
                    db.collection("categories").document(catId).update(
                        mapOf(
                            "imageCount" to FieldValue.increment(-1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }.onFailure { catErr ->
                Log.w(TAG, "Best-effort category counter decrement skipped: ${catErr.message}")
            }

            _images.value = _images.value.filterNot { it.id == image.id }
        }
    }

    /**
     * Swaps `displayOrder` (as Long) of two images in a single writeBatch and awaits server confirmation.
     */
    suspend fun swapImageDisplayOrder(first: FirestoreImageItem, second: FirestoreImageItem): Result<Unit> {
        return executeWithServerTimeout(
            operationName = "swapImageDisplayOrder",
            targetPath = "images/${first.id}, images/${second.id}"
        ) { db ->
            val batch = db.batch()
            val firstOrder = first.displayOrder.toLong()
            val secondOrder = if (second.displayOrder.toLong() == firstOrder) {
                firstOrder + 1L
            } else {
                second.displayOrder.toLong()
            }
            val firstRef = db.collection("images").document(first.id)
            val secondRef = db.collection("images").document(second.id)
            batch.update(
                firstRef,
                mapOf(
                    "displayOrder" to secondOrder,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                secondRef,
                mapOf(
                    "displayOrder" to firstOrder,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        }
    }

    /**
     * Quick publish/unlisted switch for an image card (`images/{id}`), waiting for server confirmation.
     */
    suspend fun setImagePublishedStatus(image: FirestoreImageItem, publish: Boolean): Result<Unit> {
        val cleanStatus = if (publish) "published" else "draft"
        return executeWithServerTimeout(
            operationName = "setImagePublishedStatus",
            targetPath = "images/${image.id}"
        ) { db ->
            val imageRef = db.collection("images").document(image.id)
            try {
                imageRef.update(
                    mapOf(
                        "status" to cleanStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (primaryErr: Exception) {
                if (!isPermissionDenied(primaryErr)) throw primaryErr
                val legacyStatus = if (publish) "PUBLISHED" else "DRAFT"
                imageRef.update(
                    mapOf(
                        "status" to legacyStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val confirmedItem = runCatching {
                val serverSnap = imageRef.get(Source.SERVER).await()
                if (serverSnap.exists()) parseImage(serverSnap, isServerConfirmed = true) else null
            }.getOrNull() ?: image.copy(
                status = cleanStatus,
                updatedAt = com.google.firebase.Timestamp.now(),
                isServerConfirmed = true
            )

            val updatedList = (_images.value.filterNot { it.id == confirmedItem.id } + confirmedItem)
                .sortedWith(
                    compareBy<FirestoreImageItem> { it.displayOrder }
                        .thenByDescending { it.createdAt?.seconds ?: 0L }
                )
            _images.value = updatedList
        }
    }

    // =========================================================================
    // CATEGORIES (EXACT SCHEMA FIELDS: name, description: "", coverImageUrl: "",
    // imageCount (Long), sortOrder (Long), status, createdAt, updatedAt)
    // =========================================================================
    suspend fun saveCategoryAtomic(
        existingCategory: FirestoreCategoryItem?,
        name: String,
        active: Boolean,
        customSortOrder: Int? = null
    ): Result<Unit> {
        val targetId = existingCategory?.id ?: "new_category"
        return executeWithServerTimeout(
            operationName = if (existingCategory == null) "createCategory" else "updateCategory",
            targetPath = "categories/$targetId"
        ) { db ->
            val cleanName = name.trim()
            val statusStr = if (active) "published" else "draft"
            val sortOrderLong = (customSortOrder
                ?: existingCategory?.sortOrder
                ?: ((_categories.value.maxOfOrNull { it.sortOrder } ?: 0) + 1)).toLong()

            val docRef = if (existingCategory == null) {
                val ref = db.collection("categories").document()
                val createMap = hashMapOf<String, Any>(
                    "name" to cleanName,
                    "description" to "",
                    "coverImageUrl" to "",
                    "imageCount" to 0L,
                    "sortOrder" to sortOrderLong,
                    "status" to statusStr,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                try {
                    ref.set(createMap).await()
                } catch (primaryErr: Exception) {
                    if (!isPermissionDenied(primaryErr)) throw primaryErr
                    Log.w(TAG, "Primary schema saveCategoryAtomic rejected by rules; retrying with legacy firestore.rules fields", primaryErr)
                    val fallbackCreateMap = hashMapOf<String, Any>(
                        "name" to cleanName,
                        "description" to "",
                        "coverImageUrl" to "",
                        "thumbnailUrl" to "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600",
                        "active" to active,
                        "imageCount" to 0L,
                        "sortOrder" to sortOrderLong,
                        "displayOrder" to sortOrderLong,
                        "status" to statusStr,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    ref.set(fallbackCreateMap).await()
                }
                ref
            } else {
                val ref = db.collection("categories").document(existingCategory.id)
                val updateMap = hashMapOf<String, Any>(
                    "name" to cleanName,
                    "description" to existingCategory.description,
                    "coverImageUrl" to existingCategory.coverImageUrl,
                    "sortOrder" to sortOrderLong,
                    "status" to statusStr,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                try {
                    ref.update(updateMap).await()
                } catch (primaryErr: Exception) {
                    if (!isPermissionDenied(primaryErr)) throw primaryErr
                    Log.w(TAG, "Primary schema updateCategory rejected by rules; retrying with legacy firestore.rules fields", primaryErr)
                    val fallbackUpdateMap = hashMapOf<String, Any>(
                        "name" to cleanName,
                        "description" to existingCategory.description,
                        "coverImageUrl" to existingCategory.coverImageUrl,
                        "thumbnailUrl" to existingCategory.coverImageUrl.ifBlank {
                            "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600"
                        },
                        "active" to active,
                        "sortOrder" to sortOrderLong,
                        "displayOrder" to sortOrderLong,
                        "status" to statusStr,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    ref.update(fallbackUpdateMap).await()
                }
                ref
            }

            val confirmedCat = runCatching {
                val serverSnap = docRef.get(Source.SERVER).await()
                if (serverSnap.exists()) parseCategory(serverSnap, isServerConfirmed = true) else null
            }.getOrNull() ?: FirestoreCategoryItem(
                id = docRef.id,
                name = cleanName,
                description = existingCategory?.description ?: "",
                coverImageUrl = existingCategory?.coverImageUrl ?: "",
                imageCount = existingCategory?.imageCount ?: 0,
                sortOrder = sortOrderLong.toInt(),
                status = statusStr,
                createdAt = existingCategory?.createdAt ?: com.google.firebase.Timestamp.now(),
                updatedAt = com.google.firebase.Timestamp.now(),
                isServerConfirmed = true
            )

            val updatedCats = (_categories.value.filterNot { it.id == confirmedCat.id } + confirmedCat)
                .sortedWith(compareBy<FirestoreCategoryItem> { it.sortOrder }.thenBy { it.name })
            _categories.value = updatedCats
        }
    }

    /**
     * Deletes a category WITHOUT deleting any images, reassigns affected images to
     * `categoryId = "uncategorized"` and `categoryName = "অন্যান্য"`.
     */
    suspend fun deleteCategoryKeepImages(category: FirestoreCategoryItem): Result<Unit> {
        return executeWithServerTimeout(
            operationName = "deleteCategoryKeepImages",
            targetPath = "categories/${category.id}"
        ) { db ->
            val catRef = db.collection("categories").document(category.id)
            catRef.delete().await()

            runCatching {
                val affectedImages = _images.value.filter { it.categoryId == category.id }
                if (affectedImages.isNotEmpty()) {
                    val batch = db.batch()
                    for (img in affectedImages) {
                        batch.update(
                            db.collection("images").document(img.id),
                            mapOf(
                                "categoryId" to "uncategorized",
                                "categoryName" to "অন্যান্য",
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    batch.commit().await()
                }
            }.onFailure { imgErr ->
                Log.w(TAG, "Best-effort reassignment of images after category deletion skipped: ${imgErr.message}")
            }

            _categories.value = _categories.value.filterNot { it.id == category.id }
        }
    }

    // =========================================================================
    // AI TOOLS (aiTools/{id} — EXACT SCHEMA FIELDS:
    // name (string, 1-60), url (string, https://, max 500), logoUrl (string, max 500),
    // enabled (bool), sortOrder (int/Long), updatedAt (server timestamp))
    // =========================================================================
    suspend fun saveAiToolAtomic(
        documentId: String,
        isEdit: Boolean,
        name: String,
        url: String,
        logoUrl: String,
        enabled: Boolean,
        sortOrder: Int
    ): Result<Unit> {
        return executeWithServerTimeout(
            operationName = if (isEdit) "updateAiTool" else "createAiTool",
            targetPath = "aiTools/$documentId"
        ) { db ->
            val docRef = db.collection("aiTools").document(documentId)
            val cleanName = name.trim().take(60)
            val cleanUrl = url.trim().take(500)
            val cleanLogoUrl = logoUrl.trim().take(500)
            val sortOrderLong = sortOrder.toLong()

            // Exact 6 fields: name, url, logoUrl, enabled, sortOrder, updatedAt
            val payload = hashMapOf<String, Any>(
                "name" to cleanName,
                "url" to cleanUrl,
                "logoUrl" to cleanLogoUrl,
                "enabled" to enabled,
                "sortOrder" to sortOrderLong,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            docRef.set(payload).await()

            val confirmedTool = runCatching {
                val serverSnap = docRef.get(Source.SERVER).await()
                if (serverSnap.exists()) parseAiTool(serverSnap, isServerConfirmed = true) else null
            }.getOrNull() ?: FirestoreAiToolItem(
                id = documentId,
                name = cleanName,
                url = cleanUrl,
                logoUrl = cleanLogoUrl,
                enabled = enabled,
                sortOrder = sortOrder,
                updatedAt = com.google.firebase.Timestamp.now(),
                isServerConfirmed = true
            )

            val updatedTools = (_aiTools.value.filterNot { it.id == confirmedTool.id } + confirmedTool)
                .sortedWith(compareBy<FirestoreAiToolItem> { it.sortOrder }.thenBy { it.name })
            _aiTools.value = updatedTools
        }
    }

    suspend fun deleteAiToolAtomic(tool: FirestoreAiToolItem): Result<Unit> {
        return executeWithServerTimeout(
            operationName = "deleteAiToolAtomic",
            targetPath = "aiTools/${tool.id}"
        ) { db ->
            val docRef = db.collection("aiTools").document(tool.id)
            docRef.delete().await()
            _aiTools.value = _aiTools.value.filterNot { it.id == tool.id }
        }
    }

    // =========================================================================
    // COMMENTS & REPORTS (WITH 15S TIMEOUT + SERVER ACKNOWLEDGMENT)
    // =========================================================================
    suspend fun setCommentVisibility(comment: FirestoreCommentItem, makeVisible: Boolean): Result<Unit> {
        return executeWithServerTimeout(
            operationName = "setCommentVisibility",
            targetPath = "comments/${comment.id}"
        ) { db ->
            val newStatus = if (makeVisible) "visible" else "hidden"
            val docRef = db.collection("comments").document(comment.id)
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val confirmedComment = runCatching {
                val serverSnap = docRef.get(Source.SERVER).await()
                if (serverSnap.exists()) parseComment(serverSnap, isServerConfirmed = true) else null
            }.getOrNull() ?: comment.copy(
                status = newStatus,
                updatedAt = com.google.firebase.Timestamp.now(),
                isServerConfirmed = true
            )

            val updatedList = (_comments.value.filterNot { it.id == confirmedComment.id } + confirmedComment)
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            _comments.value = updatedList
        }
    }

    suspend fun saveCommentAdminReply(comment: FirestoreCommentItem, reply: String): Result<Unit> {
        return executeWithServerTimeout(
            operationName = "saveCommentAdminReply",
            targetPath = "comments/${comment.id}"
        ) { db ->
            val cleanReply = reply.trim().take(500)
            val docRef = db.collection("comments").document(comment.id)
            docRef.update(
                mapOf(
                    "adminReply" to cleanReply,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val confirmedComment = runCatching {
                val serverSnap = docRef.get(Source.SERVER).await()
                if (serverSnap.exists()) parseComment(serverSnap, isServerConfirmed = true) else null
            }.getOrNull() ?: comment.copy(
                adminReply = cleanReply,
                updatedAt = com.google.firebase.Timestamp.now(),
                isServerConfirmed = true
            )

            val updatedList = (_comments.value.filterNot { it.id == confirmedComment.id } + confirmedComment)
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            _comments.value = updatedList
        }
    }

    suspend fun deleteCommentAtomic(comment: FirestoreCommentItem): Result<Unit> {
        return executeWithServerTimeout(
            operationName = "deleteCommentAtomic",
            targetPath = "comments/${comment.id}"
        ) { db ->
            val docRef = db.collection("comments").document(comment.id)
            docRef.delete().await()
            _comments.value = _comments.value.filterNot { it.id == comment.id }
        }
    }

    suspend fun updateReportStatus(report: FirestoreReportItem, newStatus: String): Result<Unit> {
        return executeWithServerTimeout(
            operationName = "updateReportStatus",
            targetPath = "reports/${report.id}"
        ) { db ->
            val validStatus = when (newStatus.lowercase()) {
                "reviewed" -> "reviewed"
                "dismissed" -> "dismissed"
                else -> "open"
            }
            val docRef = db.collection("reports").document(report.id)
            docRef.update(
                mapOf(
                    "status" to validStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val confirmedReport = runCatching {
                val serverSnap = docRef.get(Source.SERVER).await()
                if (serverSnap.exists()) parseReport(serverSnap, isServerConfirmed = true) else null
            }.getOrNull() ?: report.copy(
                status = validStatus,
                updatedAt = com.google.firebase.Timestamp.now(),
                isServerConfirmed = true
            )

            val updatedList = (_reports.value.filterNot { it.id == confirmedReport.id } + confirmedReport)
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            _reports.value = updatedList
        }
    }

    /**
     * Blocks a user in `users/{userId}` (sets `blocked = true`, `status = "blocked"`, `accountStatus = "BLOCKED"`)
     * and hides their comments in `comments`, waiting for server confirmation.
     */
    suspend fun blockUserAtomic(userId: String, userName: String = ""): Result<Unit> {
        val cleanUserId = userId.trim()
        if (cleanUserId.isEmpty()) {
            return Result.failure(IllegalArgumentException("ব্যবহারকারীর আইডি পাওয়া যায়নি"))
        }
        return executeWithServerTimeout(
            operationName = "blockUserAtomic",
            targetPath = "users/$cleanUserId"
        ) { db ->
            val userRef = db.collection("users").document(cleanUserId)
            val blockPayload = hashMapOf<String, Any>(
                "blocked" to true,
                "isBlocked" to true,
                "status" to "blocked",
                "accountStatus" to "BLOCKED",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (userName.isNotBlank()) {
                blockPayload["userName"] = userName.trim()
            }
            userRef.set(blockPayload, com.google.firebase.firestore.SetOptions.merge()).await()

            // Best-effort hide any visible comments from this user
            runCatching {
                val userComments = _comments.value.filter { it.userId == cleanUserId && it.isVisible }
                if (userComments.isNotEmpty()) {
                    val batch = db.batch()
                    for (cmt in userComments) {
                        batch.update(
                            db.collection("comments").document(cmt.id),
                            mapOf(
                                "status" to "hidden",
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    batch.commit().await()
                    _comments.value = _comments.value.map {
                        if (it.userId == cleanUserId) it.copy(status = "hidden") else it
                    }
                }
            }.onFailure { err ->
                Log.w(TAG, "Best-effort comment hide on user block skipped: ${err.message}")
            }
        }
    }

    // =========================================================================
    // NOTIFICATIONS (notifications/{id} — EXACT SCHEMA FIELDS:
    // title, body, imageId, imageUrl, categoryName, status, createdAt)
    // =========================================================================
    suspend fun saveNotificationAtomic(
        documentId: String,
        isEdit: Boolean,
        title: String,
        body: String,
        imageId: String,
        imageUrl: String,
        categoryName: String,
        status: String
    ): Result<FirestoreNotificationItem> {
        val db = getFirestoreOrNull()
            ?: return Result.failure(IllegalStateException("ফায়ারবেস ইনিশিয়ালাইজ হয়নি"))
        val opName = if (isEdit) "updateNotification" else "createNotification"
        val targetPath = "notifications/$documentId"

        return try {
            val confirmedItem = withTimeout(WRITE_TIMEOUT_MS) {
                val docRef = db.collection("notifications").document(documentId)
                val cleanTitle = title.trim().take(120)
                val cleanBody = body.trim().take(500)
                val cleanImageId = imageId.trim()
                val cleanImageUrl = imageUrl.trim()
                val cleanCategoryName = categoryName.trim().ifEmpty { "অন্যান্য" }
                val cleanStatus = if (status.equals("draft", ignoreCase = true)) "draft" else "published"

                try {
                    if (!isEdit) {
                        // Create writes EXACTLY: title, body, imageId, imageUrl, categoryName, status, createdAt
                        val createMap = hashMapOf<String, Any>(
                            "title" to cleanTitle,
                            "body" to cleanBody,
                            "imageId" to cleanImageId,
                            "imageUrl" to cleanImageUrl,
                            "categoryName" to cleanCategoryName,
                            "status" to cleanStatus,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        docRef.set(createMap).await()
                    } else {
                        // Edit updates ONLY: title, body, imageId, imageUrl, categoryName, status (never changing createdAt)
                        val updateMap = hashMapOf<String, Any>(
                            "title" to cleanTitle,
                            "body" to cleanBody,
                            "imageId" to cleanImageId,
                            "imageUrl" to cleanImageUrl,
                            "categoryName" to cleanCategoryName,
                            "status" to cleanStatus
                        )
                        docRef.update(updateMap).await()
                    }
                } catch (primaryErr: Exception) {
                    if (!isPermissionDenied(primaryErr)) throw primaryErr
                    Log.w(TAG, "Primary schema saveNotificationAtomic rejected by rules; retrying with legacy firestore.rules fields", primaryErr)
                    val deepLinkVal = "promptlens://image/$cleanImageId"
                    if (!isEdit) {
                        val fallbackCreateMap = hashMapOf<String, Any>(
                            "title" to cleanTitle,
                            "body" to cleanBody,
                            "message" to cleanBody,
                            "deepLink" to deepLinkVal,
                            "imageId" to cleanImageId,
                            "imageUrl" to cleanImageUrl,
                            "categoryName" to cleanCategoryName,
                            "status" to cleanStatus,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        docRef.set(fallbackCreateMap).await()
                    } else {
                        val fallbackUpdateMap = hashMapOf<String, Any>(
                            "title" to cleanTitle,
                            "body" to cleanBody,
                            "message" to cleanBody,
                            "deepLink" to deepLinkVal,
                            "imageId" to cleanImageId,
                            "imageUrl" to cleanImageUrl,
                            "categoryName" to cleanCategoryName,
                            "status" to cleanStatus
                        )
                        docRef.update(fallbackUpdateMap).await()
                    }
                }

                // Re-read from Source.SERVER if available, or construct confirmed item after server commit
                val parsed = runCatching {
                    val serverSnap = docRef.get(Source.SERVER).await()
                    if (serverSnap.exists()) parseNotification(serverSnap, isServerConfirmed = true) else null
                }.getOrNull() ?: FirestoreNotificationItem(
                    id = documentId,
                    title = cleanTitle,
                    body = cleanBody,
                    imageId = cleanImageId,
                    imageUrl = cleanImageUrl,
                    categoryName = cleanCategoryName,
                    status = cleanStatus,
                    createdAt = _notifications.value.firstOrNull { it.id == documentId }?.createdAt
                        ?: com.google.firebase.Timestamp.now(),
                    isServerConfirmed = true
                )

                val updatedList = (_notifications.value.filterNot { it.id == parsed.id } + parsed)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
                _notifications.value = updatedList

                parsed
            }
            Result.success(confirmedItem)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "[$opName] Timed out after ${WRITE_TIMEOUT_MS}ms waiting for server ack on $targetPath", e)
            Result.failure(TimeoutException(SERVER_TIMEOUT_BANGA_MSG))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "[$opName] Server rejected or failed write on $targetPath: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteNotificationAtomic(notification: FirestoreNotificationItem): Result<Unit> {
        return executeWithServerTimeout(
            operationName = "deleteNotificationAtomic",
            targetPath = "notifications/${notification.id}"
        ) { db ->
            val docRef = db.collection("notifications").document(notification.id)
            docRef.delete().await()
            _notifications.value = _notifications.value.filterNot { it.id == notification.id }
        }
    }

    // =========================================================================
    // ADVERTISEMENT SETTINGS (advertisementSettings/global — EXACT 15 FIELDS)
    // =========================================================================
    suspend fun saveAdvertisementSettingsGlobal(model: AdvertisementSettingsModel): Result<Unit> {
        return executeWithServerTimeout(
            operationName = "saveAdvertisementSettingsGlobal",
            targetPath = "advertisementSettings/global"
        ) { db ->
            val docRef = db.collection("advertisementSettings").document("global")
            val cleanedModel = model.copy(
                interstitialFrequency = model.interstitialFrequency.coerceIn(1, 50),
                adFallbackMessage = model.adFallbackMessage.trim(),
                bannerAdUnitId = model.bannerAdUnitId.trim().take(120),
                rewardedAdUnitId = model.rewardedAdUnitId.trim().take(120),
                interstitialAdUnitId = model.interstitialAdUnitId.trim().take(120),
                appOpenAdUnitId = model.appOpenAdUnitId.trim().take(120)
            )
            val payload = hashMapOf<String, Any>(
                "allAdsEnabled" to cleanedModel.allAdsEnabled,
                "homeBannerEnabled" to cleanedModel.homeBannerEnabled,
                "detailBannerEnabled" to cleanedModel.detailBannerEnabled,
                "rewardedAdsEnabled" to cleanedModel.rewardedAdsEnabled,
                "interstitialAdsEnabled" to cleanedModel.interstitialAdsEnabled,
                "testAdMode" to cleanedModel.testAdMode,
                "reusePreviousUnlocks" to cleanedModel.reusePreviousUnlocks,
                "interstitialFrequency" to cleanedModel.interstitialFrequency.toLong(),
                "adFallbackMessage" to cleanedModel.adFallbackMessage,
                "bannerAdUnitId" to cleanedModel.bannerAdUnitId,
                "rewardedAdUnitId" to cleanedModel.rewardedAdUnitId,
                "interstitialAdUnitId" to cleanedModel.interstitialAdUnitId,
                "appOpenAdsEnabled" to cleanedModel.appOpenAdsEnabled,
                "appOpenAdUnitId" to cleanedModel.appOpenAdUnitId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            val updatedModel = runCatching {
                val serverSnap = docRef.get(Source.SERVER).await()
                if (serverSnap.exists()) parseAdSettings(serverSnap) else null
            }.getOrNull() ?: cleanedModel
            _adSettings.value = updatedModel
        }
    }

    // =========================================================================
    // APP SETTINGS (appSettings/global — EXACT 7 FIELDS)
    // =========================================================================
    suspend fun saveAppGlobalSettings(model: AppGlobalSettingsModel): Result<Unit> {
        return executeWithServerTimeout(
            operationName = "saveAppGlobalSettings",
            targetPath = "appSettings/global"
        ) { db ->
            val docRef = db.collection("appSettings").document("global")
            val payload = hashMapOf<String, Any>(
                "playStoreUrl" to model.playStoreUrl.trim(),
                "requireLoginForPrompts" to model.requireLoginForPrompts,
                "allowComments" to model.allowComments,
                "welcomeHeadline" to model.welcomeHeadline.trim(),
                "welcomeSubheadline" to model.welcomeSubheadline.trim(),
                "supportEmail" to model.supportEmail.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            val updatedModel = runCatching {
                val serverSnap = docRef.get(Source.SERVER).await()
                if (serverSnap.exists()) parseAppSettings(serverSnap) else null
            }.getOrNull() ?: model
            _appSettings.value = updatedModel
        }
    }
}
