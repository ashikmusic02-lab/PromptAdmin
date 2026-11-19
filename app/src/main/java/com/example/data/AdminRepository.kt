package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.model.AdminRole
import com.example.model.AdvertisementSettingsModel
import com.example.model.AppGlobalSettingsModel
import com.example.model.FirestoreCategoryItem
import com.example.model.FirestoreCommentItem
import com.example.model.FirestoreImageItem
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

    private val _comments = MutableStateFlow<List<FirestoreCommentItem>>(emptyList())
    val comments: StateFlow<List<FirestoreCommentItem>> = _comments.asStateFlow()

    private val _reports = MutableStateFlow<List<FirestoreReportItem>>(emptyList())
    val reports: StateFlow<List<FirestoreReportItem>> = _reports.asStateFlow()

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
     * Checks authorization strictly from the SERVER on Firestore `adminUsers/{uid}` (with a 15s timeout).
     * Requires role in (owner | admin | moderator) and active == true.
     */
    suspend fun verifyAdminUserFromFirestore(uid: String): Result<AdminRole?> {
        val db = getFirestoreOrNull()
            ?: return Result.failure(IllegalStateException("ফায়ারবেস ইনিশিয়ালাইজ হয়নি"))
        return try {
            val snapshot = withTimeout(WRITE_TIMEOUT_MS) {
                db.collection("adminUsers").document(uid).get(Source.SERVER).await()
            }
            if (!snapshot.exists()) {
                Log.w(TAG, "adminUsers/$uid does not exist on server")
                return Result.success(null)
            }
            val isActive = snapshot.getBoolean("active") == true
            val roleRaw = snapshot.getString("role")
            val role = AdminRole.fromFirestoreValue(roleRaw)
            if (isActive && role != null) {
                Result.success(role)
            } else {
                Log.w(TAG, "adminUsers/$uid exists on server but active=$isActive, role=$roleRaw")
                Result.success(null)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "verifyAdminUserFromFirestore timed out after ${WRITE_TIMEOUT_MS}ms for uid=$uid", e)
            Result.failure(TimeoutException("সার্ভারের সাথে সংযোগ করা যাচ্ছে না। ইন্টারনেট পরীক্ষা করুন"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "verifyAdminUserFromFirestore failed for uid=$uid: ${e.message}", e)
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
                if (snapFromCache && !snap.metadata.hasPendingWrites()) {
                    return@addSnapshotListener
                }
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
                if (snapFromCache && !snap.metadata.hasPendingWrites()) {
                    return@addSnapshotListener
                }
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
                if (snapFromCache && !snap.metadata.hasPendingWrites()) {
                    // Ignore pure cache snapshot; wait for server response
                    return@addSnapshotListener
                }
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
                if (snapFromCache && !snap.metadata.hasPendingWrites()) {
                    return@addSnapshotListener
                }
                val list = snap.documents
                    .mapNotNull { doc ->
                        val confirmed = !snapFromCache && !doc.metadata.isFromCache() && !doc.metadata.hasPendingWrites()
                        parseCategory(doc, isServerConfirmed = confirmed)
                    }
                    .sortedWith(compareBy<FirestoreCategoryItem> { it.sortOrder }.thenBy { it.name })
                _categories.value = list
            }

        // Advertisement settings listener (advertisementSettings/global)
        activeListeners += db.collection("advertisementSettings").document("global")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
                if (err != null) {
                    Log.e(TAG, "advertisementSettings/global listener error: ${err.message}", err)
                    return@addSnapshotListener
                }
                if (snap != null && snap.exists() && !snap.metadata.hasPendingWrites() && !snap.metadata.isFromCache()) {
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
                if (snap != null && snap.exists() && !snap.metadata.hasPendingWrites() && !snap.metadata.isFromCache()) {
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
            bannerAdUnitId = doc.getString("bannerAdUnitId").orEmpty(),
            rewardedAdUnitId = doc.getString("rewardedAdUnitId").orEmpty(),
            interstitialAdUnitId = doc.getString("interstitialAdUnitId").orEmpty()
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
            val text = snap.getString("promptText") ?: ""
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

    /**
     * Atomically creates or updates `images/{documentId}` and `imagePrompts/{documentId}`
     * in ONE writeBatch, waits for server acknowledgment within 15 seconds, and then
     * re-reads BOTH documents from `Source.SERVER` to confirm they exist on the server.
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
            val batch = db.batch()
            val imageRef = db.collection("images").document(documentId)
            val promptRef = db.collection("imagePrompts").document(documentId)

            val cleanCategoryId = categoryId.trim().ifEmpty { "uncategorized" }
            val cleanCategoryName = categoryName.trim().ifEmpty { "অন্যান্য" }
            val clampedRatio: Double = aspectRatio.coerceIn(0.2, 5.0)
            val cleanStatus = if (status.equals("draft", ignoreCase = true)) "draft" else "published"

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

                // Increment category imageCount (as Long) if it exists on the server
                val targetCat = _categories.value.firstOrNull { it.id == cleanCategoryId && it.isServerConfirmed }
                if (cleanCategoryId != "uncategorized" && targetCat != null) {
                    val catRef = db.collection("categories").document(cleanCategoryId)
                    batch.update(
                        catRef,
                        mapOf(
                            "imageCount" to FieldValue.increment(1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
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

                val oldCatId = previousCategoryId?.trim().orEmpty().ifEmpty { "uncategorized" }
                if (oldCatId != cleanCategoryId) {
                    val oldCat = _categories.value.firstOrNull { it.id == oldCatId && it.isServerConfirmed }
                    if (oldCatId != "uncategorized" && oldCat != null && oldCat.imageCount > 0) {
                        val oldCatRef = db.collection("categories").document(oldCatId)
                        batch.update(
                            oldCatRef,
                            mapOf(
                                "imageCount" to FieldValue.increment(-1L),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    val newCat = _categories.value.firstOrNull { it.id == cleanCategoryId && it.isServerConfirmed }
                    if (cleanCategoryId != "uncategorized" && newCat != null) {
                        val newCatRef = db.collection("categories").document(cleanCategoryId)
                        batch.update(
                            newCatRef,
                            mapOf(
                                "imageCount" to FieldValue.increment(1L),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }

            // imagePrompts/{id} contains EXACTLY: promptText, updatedAt
            val promptMap = hashMapOf<String, Any>(
                "promptText" to promptText.trim().take(5000),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.set(promptRef, promptMap)

            // 1. Await server commit acknowledgment
            batch.commit().await()

            // 2. Re-read BOTH documents directly from Source.SERVER to confirm they exist on the server
            val serverImageSnap = imageRef.get(Source.SERVER).await()
            val serverPromptSnap = promptRef.get(Source.SERVER).await()

            if (!serverImageSnap.exists() ||
                !serverPromptSnap.exists() ||
                serverImageSnap.metadata.hasPendingWrites() ||
                serverPromptSnap.metadata.hasPendingWrites()
            ) {
                throw IllegalStateException(SERVER_TIMEOUT_BANGA_MSG)
            }

            // 3. Immediately update local StateFlow with the confirmed Source.SERVER document
            parseImage(serverImageSnap, isServerConfirmed = true)?.let { confirmedItem ->
                val updatedList = (_images.value.filterNot { it.id == confirmedItem.id } + confirmedItem)
                    .sortedWith(
                        compareBy<FirestoreImageItem> { it.displayOrder }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                _images.value = updatedList
            }
        }
    }

    /**
     * Atomically deletes `images/{id}` and `imagePrompts/{id}` in ONE writeBatch,
     * decrements `categories/{categoryId}.imageCount` if positive, and confirms deletion via `Source.SERVER`.
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

            val catId = image.categoryId.trim()
            val existingCat = _categories.value.firstOrNull { it.id == catId && it.isServerConfirmed }
            if (catId.isNotEmpty() && catId != "uncategorized" && existingCat != null && existingCat.imageCount > 0) {
                val catRef = db.collection("categories").document(catId)
                batch.update(
                    catRef,
                    mapOf(
                        "imageCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()

            val checkSnap = imageRef.get(Source.SERVER).await()
            if (checkSnap.exists()) {
                throw IllegalStateException(SERVER_TIMEOUT_BANGA_MSG)
            }

            _images.value = _images.value.filterNot { it.id == image.id }
        }
    }

    /**
     * Swaps `displayOrder` (as Long) of two images in a single writeBatch and verifies via `Source.SERVER`.
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

            val verifyFirst = firstRef.get(Source.SERVER).await()
            if (!verifyFirst.exists() || verifyFirst.metadata.hasPendingWrites()) {
                throw IllegalStateException(SERVER_TIMEOUT_BANGA_MSG)
            }
        }
    }

    // =========================================================================
    // CATEGORIES (EXACT SCHEMA FIELDS: name, description: "", coverImageUrl: "",
    // imageCount (Long), sortOrder (Long), status, createdAt, updatedAt)
    // =========================================================================
    suspend fun saveCategoryAtomic(
        existingCategory: FirestoreCategoryItem?,
        name: String,
        active: Boolean
    ): Result<Unit> {
        val targetId = existingCategory?.id ?: "new_category"
        return executeWithServerTimeout(
            operationName = if (existingCategory == null) "createCategory" else "updateCategory",
            targetPath = "categories/$targetId"
        ) { db ->
            val cleanName = name.trim()
            val statusStr = if (active) "published" else "draft"

            val docRef = if (existingCategory == null) {
                val nextSortOrder = ((_categories.value.maxOfOrNull { it.sortOrder } ?: 0) + 1).toLong()
                val ref = db.collection("categories").document()
                val createMap = hashMapOf<String, Any>(
                    "name" to cleanName,
                    "description" to "",
                    "coverImageUrl" to "",
                    "imageCount" to 0L,
                    "sortOrder" to nextSortOrder,
                    "status" to statusStr,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(createMap).await()
                ref
            } else {
                val ref = db.collection("categories").document(existingCategory.id)
                val updateMap = hashMapOf<String, Any>(
                    "name" to cleanName,
                    "description" to "",
                    "coverImageUrl" to "",
                    "sortOrder" to existingCategory.sortOrder.toLong(),
                    "status" to statusStr,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.update(updateMap).await()
                ref
            }

            // Re-read from Source.SERVER to confirm the category exists on the server
            val serverSnap = docRef.get(Source.SERVER).await()
            if (!serverSnap.exists() || serverSnap.metadata.hasPendingWrites()) {
                throw IllegalStateException(SERVER_TIMEOUT_BANGA_MSG)
            }

            parseCategory(serverSnap, isServerConfirmed = true)?.let { confirmedCat ->
                val updatedCats = (_categories.value.filterNot { it.id == confirmedCat.id } + confirmedCat)
                    .sortedWith(compareBy<FirestoreCategoryItem> { it.sortOrder }.thenBy { it.name })
                _categories.value = updatedCats
            }
        }
    }

    /**
     * Deletes a category WITHOUT deleting any images, reassigns affected images to
     * `categoryId = "uncategorized"` and `categoryName = "অন্যান্য"`, and verifies via `Source.SERVER`.
     */
    suspend fun deleteCategoryKeepImages(category: FirestoreCategoryItem): Result<Unit> {
        return executeWithServerTimeout(
            operationName = "deleteCategoryKeepImages",
            targetPath = "categories/${category.id}"
        ) { db ->
            val batch = db.batch()
            val catRef = db.collection("categories").document(category.id)
            batch.delete(catRef)

            val affectedImages = _images.value.filter { it.categoryId == category.id && it.isServerConfirmed }
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

            val checkSnap = catRef.get(Source.SERVER).await()
            if (checkSnap.exists()) {
                throw IllegalStateException(SERVER_TIMEOUT_BANGA_MSG)
            }

            _categories.value = _categories.value.filterNot { it.id == category.id }
        }
    }

    // =========================================================================
    // COMMENTS & REPORTS (WITH 15S TIMEOUT + Source.SERVER VERIFICATION)
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

            val serverSnap = docRef.get(Source.SERVER).await()
            if (!serverSnap.exists() || serverSnap.metadata.hasPendingWrites()) {
                throw IllegalStateException(SERVER_TIMEOUT_BANGA_MSG)
            }
        }
    }

    suspend fun saveCommentAdminReply(comment: FirestoreCommentItem, reply: String): Result<Unit> {
        return executeWithServerTimeout(
            operationName = "saveCommentAdminReply",
            targetPath = "comments/${comment.id}"
        ) { db ->
            val docRef = db.collection("comments").document(comment.id)
            docRef.update(
                mapOf(
                    "adminReply" to reply.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val serverSnap = docRef.get(Source.SERVER).await()
            if (!serverSnap.exists() || serverSnap.metadata.hasPendingWrites()) {
                throw IllegalStateException(SERVER_TIMEOUT_BANGA_MSG)
            }
        }
    }

    suspend fun deleteCommentAtomic(comment: FirestoreCommentItem): Result<Unit> {
        return executeWithServerTimeout(
            operationName = "deleteCommentAtomic",
            targetPath = "comments/${comment.id}"
        ) { db ->
            val docRef = db.collection("comments").document(comment.id)
            docRef.delete().await()

            val checkSnap = docRef.get(Source.SERVER).await()
            if (checkSnap.exists()) {
                throw IllegalStateException(SERVER_TIMEOUT_BANGA_MSG)
            }
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

            val serverSnap = docRef.get(Source.SERVER).await()
            if (!serverSnap.exists() || serverSnap.metadata.hasPendingWrites()) {
                throw IllegalStateException(SERVER_TIMEOUT_BANGA_MSG)
            }
        }
    }

    // =========================================================================
    // ADVERTISEMENT SETTINGS (advertisementSettings/global — EXACT 13 FIELDS)
    // =========================================================================
    suspend fun saveAdvertisementSettingsGlobal(model: AdvertisementSettingsModel): Result<Unit> {
        return executeWithServerTimeout(
            operationName = "saveAdvertisementSettingsGlobal",
            targetPath = "advertisementSettings/global"
        ) { db ->
            val docRef = db.collection("advertisementSettings").document("global")
            val payload = hashMapOf<String, Any>(
                "allAdsEnabled" to model.allAdsEnabled,
                "homeBannerEnabled" to model.homeBannerEnabled,
                "detailBannerEnabled" to model.detailBannerEnabled,
                "rewardedAdsEnabled" to model.rewardedAdsEnabled,
                "interstitialAdsEnabled" to model.interstitialAdsEnabled,
                "testAdMode" to model.testAdMode,
                "reusePreviousUnlocks" to model.reusePreviousUnlocks,
                "interstitialFrequency" to model.interstitialFrequency.coerceIn(1, 50).toLong(),
                "adFallbackMessage" to model.adFallbackMessage.trim(),
                "bannerAdUnitId" to model.bannerAdUnitId.trim(),
                "rewardedAdUnitId" to model.rewardedAdUnitId.trim(),
                "interstitialAdUnitId" to model.interstitialAdUnitId.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            // Re-read from Source.SERVER to confirm the document exists on the server
            val serverSnap = docRef.get(Source.SERVER).await()
            if (!serverSnap.exists() || serverSnap.metadata.hasPendingWrites()) {
                throw IllegalStateException(SERVER_TIMEOUT_BANGA_MSG)
            }
            _adSettings.value = parseAdSettings(serverSnap)
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

            // Re-read from Source.SERVER to confirm the document exists on the server
            val serverSnap = docRef.get(Source.SERVER).await()
            if (!serverSnap.exists() || serverSnap.metadata.hasPendingWrites()) {
                throw IllegalStateException(SERVER_TIMEOUT_BANGA_MSG)
            }
            _appSettings.value = parseAppSettings(serverSnap)
        }
    }
}
