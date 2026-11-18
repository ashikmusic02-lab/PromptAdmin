package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminDatabase
import com.example.data.AdminRepository
import com.example.data.FirebaseConnectionStatus
import com.example.model.ActivityLogEntity
import com.example.model.AdMobSettingsEntity
import com.example.model.AdminAccountEntity
import com.example.model.AdminRole
import com.example.model.AdminSection
import com.example.model.AnalyticsRange
import com.example.model.AppContentSettingsEntity
import com.example.model.AppUserEntity
import com.example.model.CommentEntity
import com.example.model.ImageSortOption
import com.example.model.NotificationEntity
import com.example.model.PromptCategoryEntity
import com.example.model.PromptImageEntity
import com.example.util.UrlAndDeepLinkValidator
import com.example.util.UrlValidationResult
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

data class AdminSession(
    val isAuthenticated: Boolean = false,
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: AdminRole = AdminRole.OWNER,
    val authMethod: String = "Google Sign-In"
)

data class LiveDashboardStats(
    val totalImages: Int = 0,
    val publishedImages: Int = 0,
    val draftImages: Int = 0,
    val featuredImages: Int = 0,
    val trendingImages: Int = 0,
    val totalCategories: Int = 0,
    val activeCategories: Int = 0,
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val disabledUsers: Int = 0,
    val totalViews: Long = 0L,
    val uniqueViews: Long = 0L,
    val totalLikes: Long = 0L,
    val totalComments: Long = 0L,
    val pendingOrReportedComments: Int = 0,
    val totalPromptUnlocks: Long = 0L,
    val totalShares: Long = 0L,
    val totalNotificationsSent: Int = 0
)

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        /**
         * Administrator Gmail Allowlist.
         * Only verified Google accounts in this list may enter the administrator control panel.
         * Add additional emails here in the future as needed.
         */
        val ADMIN_EMAIL_ALLOWLIST: List<String> = listOf(
            "sfforhadvai@gmail.com"
        )

        fun isAuthorizedAdminEmail(email: String?, isEmailVerified: Boolean): Boolean {
            if (!isEmailVerified || email.isNullOrBlank()) return false
            val normalized = email.trim().lowercase(Locale.US)
            return ADMIN_EMAIL_ALLOWLIST.any { it.trim().lowercase(Locale.US) == normalized }
        }

        fun isAuthorizedFirebaseUser(user: FirebaseUser?): Boolean {
            if (user == null) return false
            val isGoogleProvider = user.providerData.any { it.providerId == "google.com" }
            return isGoogleProvider && isAuthorizedAdminEmail(user.email, user.isEmailVerified)
        }
    }

    private val database = AdminDatabase.getInstance(application)
    private val repository = AdminRepository(application, database.adminDao())

    private val _session = MutableStateFlow(AdminSession(isAuthenticated = false))
    val session: StateFlow<AdminSession> = _session.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _currentSection = MutableStateFlow(AdminSection.DASHBOARD)
    val currentSection: StateFlow<AdminSection> = _currentSection.asStateFlow()

    private val _analyticsRange = MutableStateFlow(AnalyticsRange.ALL_TIME)
    val analyticsRange: StateFlow<AnalyticsRange> = _analyticsRange.asStateFlow()

    private val _imageSortOption = MutableStateFlow(ImageSortOption.MOST_VIEWED)
    val imageSortOption: StateFlow<ImageSortOption> = _imageSortOption.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _isErrorBanner = MutableStateFlow(false)
    val isErrorBanner: StateFlow<Boolean> = _isErrorBanner.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _preselectedNotificationImage = MutableStateFlow<PromptImageEntity?>(null)
    val preselectedNotificationImage: StateFlow<PromptImageEntity?> = _preselectedNotificationImage.asStateFlow()

    private val _connectionStatus = MutableStateFlow(repository.getConnectionStatus())
    val connectionStatus: StateFlow<FirebaseConnectionStatus> = _connectionStatus.asStateFlow()

    init {
        checkExistingFirebaseSession(application)
    }

    private fun checkExistingFirebaseSession(application: Application) {
        try {
            if (FirebaseApp.getApps(application).isNotEmpty()) {
                val auth = FirebaseAuth.getInstance()
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    if (isAuthorizedFirebaseUser(currentUser)) {
                        val email = currentUser.email.orEmpty()
                        val uid = currentUser.uid
                        viewModelScope.launch {
                            repository.ensureInitialConfigBootstrap(email, uid)
                            _session.value = AdminSession(
                                isAuthenticated = true,
                                uid = uid,
                                email = email,
                                displayName = currentUser.displayName?.ifBlank { email.substringBefore("@") }
                                    ?: email.substringBefore("@"),
                                role = AdminRole.OWNER,
                                authMethod = "Firebase Google Sign-In"
                            )
                            refreshConnectionStatus()
                        }
                    } else {
                        auth.signOut()
                        _session.value = AdminSession(isAuthenticated = false)
                        _authErrorMessage.value = "Access denied"
                    }
                }
            }
        } catch (_: Exception) {
            _session.value = AdminSession(isAuthenticated = false)
        }
    }

    val images: StateFlow<List<PromptImageEntity>> = repository.imagesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<PromptCategoryEntity>> = repository.categoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<AppUserEntity>> = repository.usersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val comments: StateFlow<List<CommentEntity>> = repository.commentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adMobSettings: StateFlow<AdMobSettingsEntity> = repository.adMobSettingsFlow
        .combine(MutableStateFlow(AdMobSettingsEntity())) { dbVal, defaultVal -> dbVal ?: defaultVal }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdMobSettingsEntity())

    val appContentSettings: StateFlow<AppContentSettingsEntity> = repository.appContentSettingsFlow
        .combine(MutableStateFlow(AppContentSettingsEntity())) { dbVal, defaultVal -> dbVal ?: defaultVal }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppContentSettingsEntity())

    val notifications: StateFlow<List<NotificationEntity>> = repository.notificationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminAccounts: StateFlow<List<AdminAccountEntity>> = repository.adminAccountsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activityLogs: StateFlow<List<ActivityLogEntity>> = repository.activityLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveStats: StateFlow<LiveDashboardStats> = combine(
        images,
        categories,
        users,
        comments,
        notifications
    ) { imgs, cats, usrs, cmts, notifs ->
        val range = _analyticsRange.value
        val cutoffMillis = if (range == AnalyticsRange.ALL_TIME) {
            0L
        } else {
            System.currentTimeMillis() - range.days * 24L * 3600L * 1000L
        }
        val filteredImgs = imgs.filter { it.createdAtMillis >= cutoffMillis }

        LiveDashboardStats(
            totalImages = filteredImgs.size,
            publishedImages = filteredImgs.count { it.status == "PUBLISHED" },
            draftImages = filteredImgs.count { it.status == "DRAFT" },
            featuredImages = filteredImgs.count { it.featured },
            trendingImages = filteredImgs.count { it.trending },
            totalCategories = cats.size,
            activeCategories = cats.count { it.active },
            totalUsers = usrs.size,
            activeUsers = usrs.count { it.accountStatus == "ACTIVE" },
            disabledUsers = usrs.count { it.accountStatus == "DISABLED" },
            totalViews = filteredImgs.sumOf { it.viewsCount },
            uniqueViews = filteredImgs.sumOf { it.uniqueViewsCount },
            totalLikes = filteredImgs.sumOf { it.likesCount },
            totalComments = cmts.count { it.moderationStatus != "DELETED" }.toLong(),
            pendingOrReportedComments = cmts.count { it.moderationStatus == "PENDING" || it.reported },
            totalPromptUnlocks = filteredImgs.sumOf { it.promptUnlocksCount },
            totalShares = filteredImgs.sumOf { it.sharesCount },
            totalNotificationsSent = notifs.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LiveDashboardStats())

    fun refreshConnectionStatus() {
        _connectionStatus.value = repository.getConnectionStatus()
    }

    fun setAuthErrorMessage(message: String?) {
        _authErrorMessage.value = message
    }

    fun showFeedback(message: String, isError: Boolean = false) {
        _statusBannerMessage.value = message
        _isErrorBanner.value = isError
    }

    fun dismissFeedback() {
        _statusBannerMessage.value = null
    }

    /**
     * Completes login ONLY if the Google-authenticated FirebaseUser passes the strict email allowlist
     * and email verification check. Otherwise signs out immediately and shows "Access denied".
     */
    fun onGoogleFirebaseUserAuthenticated(user: FirebaseUser?) {
        viewModelScope.launch {
            if (!isAuthorizedFirebaseUser(user)) {
                try {
                    FirebaseAuth.getInstance().signOut()
                } catch (_: Exception) {
                }
                _session.value = AdminSession(isAuthenticated = false)
                _authErrorMessage.value = "Access denied"
                return@launch
            }

            val verifiedUser = user!!
            val cleanEmail = verifiedUser.email!!.trim()
            val cleanUid = verifiedUser.uid
            _authErrorMessage.value = null

            repository.ensureInitialConfigBootstrap(cleanEmail, cleanUid)

            _session.value = AdminSession(
                isAuthenticated = true,
                uid = cleanUid,
                email = cleanEmail,
                displayName = verifiedUser.displayName?.ifBlank { cleanEmail.substringBefore("@") }
                    ?: cleanEmail.substringBefore("@"),
                role = AdminRole.OWNER,
                authMethod = "Firebase Google Sign-In"
            )
            refreshConnectionStatus()
            repository.recordAuditLog(
                adminUid = cleanUid,
                adminEmail = cleanEmail,
                adminRole = AdminRole.OWNER.name,
                actionType = "ADMIN_GOOGLE_LOGIN",
                targetEntity = "adminUsers/$cleanUid",
                summary = "Authenticated allowlisted Google Owner session ($cleanEmail)"
            )
        }
    }

    fun switchActiveRoleForPreview(newRole: AdminRole) {
        val cur = _session.value
        if (!cur.isAuthenticated) return
        _session.value = cur.copy(role = newRole)
        showFeedback("Active RBAC role switched to ${newRole.displayName} (${newRole.badgeLabel})")
    }

    fun signOutAdmin() {
        val cur = _session.value
        viewModelScope.launch {
            if (cur.isAuthenticated) {
                repository.recordAuditLog(
                    adminUid = cur.uid,
                    adminEmail = cur.email,
                    adminRole = cur.role.name,
                    actionType = "ADMIN_SESSION_END",
                    targetEntity = "adminUsers/${cur.uid}",
                    summary = "Signed out administrator ${cur.email}"
                )
            }
            try {
                if (FirebaseApp.getApps(getApplication()).isNotEmpty()) {
                    FirebaseAuth.getInstance().signOut()
                }
            } catch (_: Exception) {
            }
            _session.value = AdminSession(isAuthenticated = false)
        }
    }

    fun navigateTo(section: AdminSection) {
        _currentSection.value = section
    }

    fun openNotificationComposerForImage(image: PromptImageEntity) {
        _preselectedNotificationImage.value = image
        _currentSection.value = AdminSection.ONESIGNAL_NOTIFICATIONS
    }

    fun clearPreselectedNotificationImage() {
        _preselectedNotificationImage.value = null
    }

    fun setAnalyticsRange(range: AnalyticsRange) {
        _analyticsRange.value = range
    }

    fun setImageSortOption(sortOption: ImageSortOption) {
        _imageSortOption.value = sortOption
    }

    // =========================================================================
    // MODULE 3 & 4: IMAGE MANAGEMENT (STRICT DIRECT EXTERNAL URL ONLY)
    // =========================================================================
    fun saveImageRecord(
        existingId: String?,
        imageUrl: String,
        shortName: String,
        title: String,
        description: String,
        categoryId: String,
        categoryName: String,
        prompt: String,
        featured: Boolean,
        trending: Boolean,
        status: String,
        displayOrder: Int,
        onSuccess: () -> Unit = {}
    ) {
        val cur = _session.value
        if (!cur.role.canManageImages) {
            showFeedback("Permission Denied: ${cur.role.displayName} role cannot modify images.", isError = true)
            return
        }

        val urlValidation = UrlAndDeepLinkValidator.validateDirectImageUrl(imageUrl)
        if (urlValidation !is UrlValidationResult.Valid) {
            val reason = (urlValidation as? UrlValidationResult.Invalid)?.reason
                ?: "A valid direct external image URL (.jpg, .jpeg, .png, .webp) is required."
            showFeedback(reason, isError = true)
            return
        }

        if (shortName.isBlank() || title.isBlank() || prompt.isBlank() || categoryId.isBlank()) {
            showFeedback("Short Name, Title, Category, and AI Generation Prompt are required.", isError = true)
            return
        }

        viewModelScope.launch {
            val isNew = existingId.isNullOrBlank()
            val finalId = if (isNew) {
                UrlAndDeepLinkValidator.generateUniqueImageId(shortName)
            } else {
                existingId!!
            }
            val scheme = appContentSettings.value.deepLinkScheme
            val host = appContentSettings.value.verifiedDomainHost
            val autoDeepLink = UrlAndDeepLinkValidator.buildAutoDeepLink(scheme, finalId)
            val verifiedLink = UrlAndDeepLinkValidator.buildVerifiedAppLink(host, finalId)

            val existing = images.value.firstOrNull { it.id == finalId }
            val now = System.currentTimeMillis()

            val record = PromptImageEntity(
                id = finalId,
                imageUrl = urlValidation.normalizedUrl,
                shortName = shortName.trim(),
                title = title.trim(),
                description = description.trim(),
                categoryId = categoryId,
                categoryName = categoryName,
                prompt = prompt.trim(),
                featured = featured,
                trending = trending,
                status = status,
                displayOrder = displayOrder,
                deepLink = autoDeepLink,
                verifiedAppLink = verifiedLink,
                viewsCount = existing?.viewsCount ?: 0L,
                uniqueViewsCount = existing?.uniqueViewsCount ?: 0L,
                likesCount = existing?.likesCount ?: 0L,
                commentsCount = existing?.commentsCount ?: 0L,
                promptUnlocksCount = existing?.promptUnlocksCount ?: 0L,
                sharesCount = existing?.sharesCount ?: 0L,
                createdByUid = existing?.createdByUid ?: cur.uid,
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now
            )

            val result = repository.saveImage(
                image = record,
                adminUid = cur.uid,
                adminEmail = cur.email,
                adminRole = cur.role.name,
                isNew = isNew
            )
            result.onSuccess {
                showFeedback("${if (isNew) "Created" else "Updated"} '${record.title}' • Prompt isolated in imagePrompts/${record.id}")
                onSuccess()
            }.onFailure { err ->
                showFeedback("Failed to save image: ${err.localizedMessage}", isError = true)
            }
        }
    }

    fun toggleImageQuickFlag(
        image: PromptImageEntity,
        togglePublish: Boolean = false,
        toggleFeatured: Boolean = false,
        toggleTrending: Boolean = false,
        newDisplayOrder: Int? = null
    ) {
        val cur = _session.value
        if (!cur.role.canManageImages) {
            showFeedback("Permission Denied: ${cur.role.displayName} role cannot modify images.", isError = true)
            return
        }
        viewModelScope.launch {
            val updated = image.copy(
                status = if (togglePublish) {
                    if (image.status == "PUBLISHED") "DRAFT" else "PUBLISHED"
                } else image.status,
                featured = if (toggleFeatured) !image.featured else image.featured,
                trending = if (toggleTrending) !image.trending else image.trending,
                displayOrder = newDisplayOrder ?: image.displayOrder,
                updatedAtMillis = System.currentTimeMillis()
            )
            repository.saveImage(updated, cur.uid, cur.email, cur.role.name, isNew = false)
                .onSuccess {
                    showFeedback("Updated '${updated.title}' (Status=${updated.status}, Featured=${updated.featured}, Trending=${updated.trending})")
                }
        }
    }

    fun deleteImageConfirmed(image: PromptImageEntity) {
        val cur = _session.value
        if (!cur.role.canManageImages) {
            showFeedback("Permission Denied: Only Owner or Administrator can delete images.", isError = true)
            return
        }
        viewModelScope.launch {
            repository.deleteImage(image, cur.uid, cur.email, cur.role.name)
                .onSuccess {
                    showFeedback("Deleted image '${image.title}' (${image.id})")
                }
                .onFailure {
                    showFeedback("Failed to delete image: ${it.localizedMessage}", isError = true)
                }
        }
    }

    // =========================================================================
    // MODULE 5: CATEGORY MANAGEMENT
    // =========================================================================
    fun saveCategoryRecord(
        existingId: String?,
        name: String,
        description: String,
        thumbnailUrl: String,
        displayOrder: Int,
        active: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        val cur = _session.value
        if (!cur.role.canManageCategories) {
            showFeedback("Permission Denied: ${cur.role.displayName} role cannot modify categories.", isError = true)
            return
        }
        val urlCheck = UrlAndDeepLinkValidator.validateDirectImageUrl(thumbnailUrl)
        if (urlCheck !is UrlValidationResult.Valid) {
            val reason = (urlCheck as? UrlValidationResult.Invalid)?.reason
                ?: "Category thumbnail requires a valid direct external image URL."
            showFeedback(reason, isError = true)
            return
        }
        if (name.isBlank()) {
            showFeedback("Category Name is required.", isError = true)
            return
        }
        viewModelScope.launch {
            val isNew = existingId.isNullOrBlank()
            val finalId = existingId ?: "cat_${name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').take(16)}_${UUID.randomUUID().toString().take(5)}"
            val now = System.currentTimeMillis()
            val record = PromptCategoryEntity(
                id = finalId,
                name = name.trim(),
                description = description.trim(),
                thumbnailUrl = urlCheck.normalizedUrl,
                displayOrder = displayOrder,
                active = active,
                createdAtMillis = now,
                updatedAtMillis = now
            )
            repository.saveCategory(record, cur.uid, cur.email, cur.role.name, isNew)
                .onSuccess {
                    showFeedback("${if (isNew) "Created" else "Updated"} category '${record.name}'")
                    onSuccess()
                }
                .onFailure {
                    showFeedback("Failed to save category: ${it.localizedMessage}", isError = true)
                }
        }
    }

    fun deleteCategoryProtected(
        category: PromptCategoryEntity,
        migrateToCategory: PromptCategoryEntity?,
        deleteContainedImages: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        val cur = _session.value
        if (!cur.role.canManageCategories) {
            showFeedback("Permission Denied: ${cur.role.displayName} role cannot delete categories.", isError = true)
            return
        }
        viewModelScope.launch {
            repository.deleteCategoryWithResolution(
                category = category,
                migrateToCategory = migrateToCategory,
                deleteContainedImages = deleteContainedImages,
                adminUid = cur.uid,
                adminEmail = cur.email,
                adminRole = cur.role.name
            ).onSuccess {
                showFeedback("Deleted category '${category.name}'")
                onSuccess()
            }.onFailure { err ->
                showFeedback(err.localizedMessage ?: "Category deletion blocked", isError = true)
            }
        }
    }

    // =========================================================================
    // MODULE 6: USER MANAGEMENT
    // =========================================================================
    fun registerOrUpdateUser(
        existingUser: AppUserEntity?,
        username: String,
        email: String,
        accountStatus: String,
        moderationNote: String
    ) {
        val cur = _session.value
        if (!cur.role.canManageUsers) {
            showFeedback("Permission Denied: ${cur.role.displayName} role cannot modify user accounts.", isError = true)
            return
        }
        if (username.isBlank()) {
            showFeedback("Username is required.", isError = true)
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val userRecord = existingUser?.copy(
                username = username.trim(),
                email = email.trim(),
                accountStatus = accountStatus,
                moderationNote = moderationNote.trim(),
                lastActivityAtMillis = now
            ) ?: AppUserEntity(
                id = "usr_${UUID.randomUUID().toString().replace("-", "").take(10)}",
                username = username.trim(),
                email = email.trim(),
                accountStatus = accountStatus,
                moderationNote = moderationNote.trim(),
                registeredAtMillis = now,
                lastActivityAtMillis = now
            )
            repository.upsertUserRecord(
                user = userRecord,
                adminUid = cur.uid,
                adminEmail = cur.email,
                adminRole = cur.role.name,
                actionSummary = "Set user @${userRecord.username} (${userRecord.id}) status to ${userRecord.accountStatus}"
            ).onSuccess {
                showFeedback("Updated user @${userRecord.username} (${userRecord.accountStatus})")
            }.onFailure {
                showFeedback("Failed to update user: ${it.localizedMessage}", isError = true)
            }
        }
    }

    // =========================================================================
    // MODULE 7: COMMENT MODERATION (CREATES FORCE moderationStatus == 'PENDING')
    // =========================================================================
    fun moderateComment(
        comment: CommentEntity,
        newStatus: String,
        adminReply: String = comment.adminReply,
        clearReportFlag: Boolean = false
    ) {
        val cur = _session.value
        if (!cur.role.canModerateComments) {
            showFeedback("Permission Denied: Cannot moderate comments.", isError = true)
            return
        }
        viewModelScope.launch {
            val updated = comment.copy(
                moderationStatus = newStatus,
                adminReply = adminReply.trim(),
                reported = if (clearReportFlag) false else comment.reported,
                moderatedByEmail = cur.email,
                updatedAtMillis = System.currentTimeMillis()
            )
            repository.updateCommentModeration(
                comment = updated,
                adminUid = cur.uid,
                adminEmail = cur.email,
                adminRole = cur.role.name,
                actionLabel = "Set status=$newStatus"
            ).onSuccess {
                showFeedback("Comment by @${comment.username} updated ($newStatus)")
            }
        }
    }

    fun createAdminCommentEntry(
        image: PromptImageEntity,
        username: String,
        content: String,
        reported: Boolean,
        reportReason: String
    ) {
        if (content.isBlank() || username.isBlank()) {
            showFeedback("Username and comment text are required.", isError = true)
            return
        }
        val cur = _session.value
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            // Forced to PENDING on creation to match firestore.rules
            val comment = CommentEntity(
                id = "cmt_${UUID.randomUUID().toString().take(8)}",
                imageId = image.id,
                imageTitle = image.title,
                userId = cur.uid,
                username = username.trim(),
                content = content.trim(),
                moderationStatus = "PENDING",
                reported = reported,
                reportReason = reportReason.trim(),
                moderatedByEmail = cur.email,
                createdAtMillis = now,
                updatedAtMillis = now
            )
            repository.updateCommentModeration(comment, cur.uid, cur.email, cur.role.name, "Created PENDING comment record")
            val updatedImage = image.copy(commentsCount = image.commentsCount + 1, updatedAtMillis = now)
            repository.saveImage(updatedImage, cur.uid, cur.email, cur.role.name, isNew = false)
            showFeedback("Recorded comment on '${image.title}' (Status: PENDING)")
        }
    }

    fun deleteCommentPermanently(comment: CommentEntity) {
        val cur = _session.value
        if (!cur.role.canModerateComments) return
        viewModelScope.launch {
            repository.deleteCommentPermanently(comment, cur.uid, cur.email, cur.role.name)
                .onSuccess { showFeedback("Deleted comment ${comment.id}") }
        }
    }

    // =========================================================================
    // MODULE 8: ENGAGEMENT VERIFICATION / TELEMETRY SYNC
    // =========================================================================
    fun updateImageTelemetryCounters(
        image: PromptImageEntity,
        viewsCount: Long,
        uniqueViewsCount: Long,
        likesCount: Long,
        promptUnlocksCount: Long,
        sharesCount: Long
    ) {
        val cur = _session.value
        if (!cur.role.canManageImages) {
            showFeedback("Permission Denied: Only Owner/Admin can adjust verified telemetry counters.", isError = true)
            return
        }
        viewModelScope.launch {
            val safeUnique = uniqueViewsCount.coerceAtMost(viewsCount).coerceAtLeast(0L)
            val updated = image.copy(
                viewsCount = viewsCount.coerceAtLeast(0L),
                uniqueViewsCount = safeUnique,
                likesCount = likesCount.coerceAtLeast(0L),
                promptUnlocksCount = promptUnlocksCount.coerceAtLeast(0L),
                sharesCount = sharesCount.coerceAtLeast(0L),
                updatedAtMillis = System.currentTimeMillis()
            )
            repository.saveImage(updated, cur.uid, cur.email, cur.role.name, isNew = false)
                .onSuccess {
                    showFeedback("Synchronized engagement counters for '${image.title}'")
                }
        }
    }

    // =========================================================================
    // MODULE 9: GOOGLE ADMOB CONFIGURATION
    // =========================================================================
    fun updateAdMobConfiguration(updated: AdMobSettingsEntity) {
        val cur = _session.value
        if (!cur.role.canManageAdMob) {
            showFeedback("Permission Denied: ${cur.role.displayName} role cannot modify AdMob settings.", isError = true)
            return
        }
        viewModelScope.launch {
            val stamped = updated.copy(
                updatedByEmail = cur.email,
                updatedAtMillis = System.currentTimeMillis()
            )
            repository.saveAdMobSettings(stamped, cur.uid, cur.email, cur.role.name)
                .onSuccess {
                    showFeedback("Saved Google AdMob configuration & placement switches")
                }
                .onFailure {
                    showFeedback("Failed to save AdMob configuration: ${it.localizedMessage}", isError = true)
                }
        }
    }

    // =========================================================================
    // MODULE 10 & 12: ONESIGNAL PUSH NOTIFICATIONS & HISTORY
    // =========================================================================
    fun sendOrScheduleNotification(
        title: String,
        message: String,
        targetAudience: String,
        selectedImage: PromptImageEntity?,
        customDeepLink: String,
        scheduleType: String,
        scheduledTimeDisplay: String,
        onComplete: () -> Unit = {}
    ) {
        val cur = _session.value
        if (!cur.role.canSendNotifications) {
            showFeedback("Permission Denied: ${cur.role.displayName} role cannot send push notifications.", isError = true)
            return
        }
        if (title.isBlank() || message.isBlank()) {
            showFeedback("Notification Title and Message are required.", isError = true)
            return
        }
        val scheme = appContentSettings.value.deepLinkScheme
        val finalDeepLink = selectedImage?.deepLink?.ifBlank {
            UrlAndDeepLinkValidator.buildAutoDeepLink(scheme, selectedImage.id)
        } ?: customDeepLink.trim().ifBlank { "$scheme://home" }

        if (finalDeepLink.startsWith("http://") || finalDeepLink.startsWith("https://")) {
            val allowedHost = appContentSettings.value.verifiedDomainHost
            if (!finalDeepLink.contains(allowedHost)) {
                showFeedback("Security Policy: Notifications must never send users to an unrelated third-party website.", isError = true)
                return
            }
        }

        viewModelScope.launch {
            val notification = NotificationEntity(
                id = "ntf_${UUID.randomUUID().toString().take(8)}",
                title = title.trim(),
                message = message.trim(),
                targetAudience = targetAudience,
                imageId = selectedImage?.id.orEmpty(),
                imageTitle = selectedImage?.title ?: "App Home Destination",
                imageThumbnailUrl = selectedImage?.imageUrl.orEmpty(),
                deepLink = finalDeepLink,
                scheduleType = scheduleType,
                scheduledTimeDisplay = if (scheduleType == "SEND_NOW") "Immediate Dispatch" else scheduledTimeDisplay.ifBlank { "Scheduled" },
                createdByEmail = cur.email,
                createdAtMillis = System.currentTimeMillis()
            )
            repository.dispatchOneSignalNotification(
                notification = notification,
                adminUid = cur.uid,
                adminEmail = cur.email,
                adminRole = cur.role.name
            ).onSuccess { saved ->
                showFeedback("Notification '${saved.title}' recorded (${saved.sendingStatus}) • Deep Link: ${saved.deepLink}")
                onComplete()
            }.onFailure { err ->
                showFeedback("Notification failed: ${err.localizedMessage}", isError = true)
            }
        }
    }

    // =========================================================================
    // MODULE 11: APP CONTENT & PLATFORM CONFIGURATION
    // =========================================================================
    fun updateAppContentConfiguration(updated: AppContentSettingsEntity) {
        val cur = _session.value
        if (!cur.role.canManageAppContent) {
            showFeedback("Permission Denied: ${cur.role.displayName} role cannot modify App Content.", isError = true)
            return
        }
        if (updated.appLogoUrl.isNotBlank()) {
            val check = UrlAndDeepLinkValidator.validateDirectImageUrl(updated.appLogoUrl)
            if (check is UrlValidationResult.Invalid) {
                showFeedback("App Logo URL invalid: ${check.reason}", isError = true)
                return
            }
        }
        viewModelScope.launch {
            val stamped = updated.copy(
                updatedByEmail = cur.email,
                updatedAtMillis = System.currentTimeMillis()
            )
            repository.saveAppContentSettings(stamped, cur.uid, cur.email, cur.role.name)
                .onSuccess {
                    showFeedback("App Content & Deep Link Scheme (${stamped.deepLinkScheme}://) saved")
                }
                .onFailure {
                    showFeedback("Failed to save App Content: ${it.localizedMessage}", isError = true)
                }
        }
    }

    // =========================================================================
    // MODULE 13: ADMINISTRATOR & PERMISSION MANAGEMENT
    // =========================================================================
    fun saveAdministratorAccount(
        uid: String,
        email: String,
        displayName: String,
        role: AdminRole,
        active: Boolean
    ) {
        val cur = _session.value
        if (!cur.role.canManageAdmins) {
            showFeedback("Permission Denied: Only Owner role can manage administrator accounts.", isError = true)
            return
        }
        if (email.isBlank() || displayName.isBlank()) {
            showFeedback("Administrator Email and Display Name are required.", isError = true)
            return
        }
        viewModelScope.launch {
            val cleanUid = uid.trim().ifBlank { "adm_${email.trim().lowercase().hashCode().toUInt()}" }
            val record = AdminAccountEntity(
                uid = cleanUid,
                email = email.trim(),
                displayName = displayName.trim(),
                role = role.name,
                active = active,
                permissionsSummary = role.badgeLabel,
                createdAtMillis = System.currentTimeMillis(),
                lastLoginAtMillis = System.currentTimeMillis()
            )
            repository.saveAdminAccount(record, cur.uid, cur.email, cur.role.name)
                .onSuccess {
                    showFeedback("Saved administrator ${record.email} (${role.displayName})")
                }
        }
    }

    fun removeAdministratorAccount(target: AdminAccountEntity) {
        val cur = _session.value
        if (!cur.role.canManageAdmins) {
            showFeedback("Permission Denied: Only Owner role can remove administrators.", isError = true)
            return
        }
        if (target.uid == cur.uid) {
            showFeedback("Safety Guard: You cannot delete your own active Owner account.", isError = true)
            return
        }
        viewModelScope.launch {
            repository.removeAdminAccount(target, cur.uid, cur.email, cur.role.name)
                .onSuccess {
                    showFeedback("Removed administrator ${target.email}")
                }
        }
    }

    // =========================================================================
    // MODULE 14: FIREBASE CLOUD SYNCHRONIZATION
    // =========================================================================
    fun triggerFirestoreFullSync() {
        val cur = _session.value
        viewModelScope.launch {
            _isSyncing.value = true
            refreshConnectionStatus()
            val res = repository.syncAllWithFirestore(cur.uid, cur.email, cur.role.name)
            _isSyncing.value = false
            res.onSuccess { msg ->
                showFeedback(msg, isError = false)
            }.onFailure { err ->
                showFeedback(err.localizedMessage ?: "Firestore sync requires app/google-services.json", isError = true)
            }
        }
    }
}
