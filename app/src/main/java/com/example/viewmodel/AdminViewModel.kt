package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminRepository
import com.example.data.FirebaseConnectionStatus
import com.example.model.AdminRole
import com.example.model.AdminSection
import com.example.model.AdvertisementSettingsModel
import com.example.model.AppGlobalSettingsModel
import com.example.model.AuthUiState
import com.example.model.DashboardCounts
import com.example.model.FirestoreCategoryItem
import com.example.model.FirestoreCommentItem
import com.example.model.FirestoreImageItem
import com.example.model.FirestoreNotificationItem
import com.example.model.FirestoreReportItem
import com.example.util.UrlAndDeepLinkValidator
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

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AdminRepository(application)

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _currentSection = MutableStateFlow(AdminSection.DASHBOARD)
    val currentSection: StateFlow<AdminSection> = _currentSection.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isErrorSnackbar = MutableStateFlow(false)
    val isErrorSnackbar: StateFlow<Boolean> = _isErrorSnackbar.asStateFlow()

    private val _connectionStatus = MutableStateFlow(repository.getConnectionStatus())
    val connectionStatus: StateFlow<FirebaseConnectionStatus> = _connectionStatus.asStateFlow()

    // Full-screen Image Add/Edit Form state
    private val _isImageFormOpen = MutableStateFlow(false)
    val isImageFormOpen: StateFlow<Boolean> = _isImageFormOpen.asStateFlow()

    private val _editingImage = MutableStateFlow<FirestoreImageItem?>(null)
    val editingImage: StateFlow<FirestoreImageItem?> = _editingImage.asStateFlow()

    private val _activeFormDocumentId = MutableStateFlow("")
    val activeFormDocumentId: StateFlow<String> = _activeFormDocumentId.asStateFlow()

    private val _loadedPromptText = MutableStateFlow("")
    val loadedPromptText: StateFlow<String> = _loadedPromptText.asStateFlow()

    private val _isLoadingPrompt = MutableStateFlow(false)
    val isLoadingPrompt: StateFlow<Boolean> = _isLoadingPrompt.asStateFlow()

    private val _isImageSaving = MutableStateFlow(false)
    val isImageSaving: StateFlow<Boolean> = _isImageSaving.asStateFlow()

    private val _isActionSubmitting = MutableStateFlow(false)
    val isActionSubmitting: StateFlow<Boolean> = _isActionSubmitting.asStateFlow()

    // Full-screen Notification Add/Edit Form state
    private val _isNotificationFormOpen = MutableStateFlow(false)
    val isNotificationFormOpen: StateFlow<Boolean> = _isNotificationFormOpen.asStateFlow()

    private val _editingNotification = MutableStateFlow<FirestoreNotificationItem?>(null)
    val editingNotification: StateFlow<FirestoreNotificationItem?> = _editingNotification.asStateFlow()

    private val _activeNotificationDocId = MutableStateFlow("")
    val activeNotificationDocId: StateFlow<String> = _activeNotificationDocId.asStateFlow()

    private val _isNotificationSaving = MutableStateFlow(false)
    val isNotificationSaving: StateFlow<Boolean> = _isNotificationSaving.asStateFlow()

    // Shown after a successful notification save so the admin can copy values to OneSignal
    private val _oneSignalHelperNotification = MutableStateFlow<FirestoreNotificationItem?>(null)
    val oneSignalHelperNotification: StateFlow<FirestoreNotificationItem?> = _oneSignalHelperNotification.asStateFlow()

    val images: StateFlow<List<FirestoreImageItem>> = repository.images
    val categories: StateFlow<List<FirestoreCategoryItem>> = repository.categories
    val comments: StateFlow<List<FirestoreCommentItem>> = repository.comments
    val reports: StateFlow<List<FirestoreReportItem>> = repository.reports
    val notifications: StateFlow<List<FirestoreNotificationItem>> = repository.notifications
    val adSettings: StateFlow<AdvertisementSettingsModel> = repository.adSettings
    val appSettings: StateFlow<AppGlobalSettingsModel> = repository.appSettings
    val isLoadingData: StateFlow<Boolean> = repository.isLoadingData

    val dashboardCounts: StateFlow<DashboardCounts> = combine(
        images,
        categories,
        comments,
        reports
    ) { imgs, cats, cmts, rpts ->
        val confirmedImgs = imgs.filter { it.isServerConfirmed }
        val confirmedCats = cats.filter { it.isServerConfirmed }
        val confirmedCmts = cmts.filter { it.isServerConfirmed }
        val confirmedRpts = rpts.filter { it.isServerConfirmed }
        DashboardCounts(
            totalImages = confirmedImgs.size,
            publishedImages = confirmedImgs.count { it.isPublished },
            unlistedImages = confirmedImgs.count { !it.isPublished },
            totalCategories = confirmedCats.size,
            totalComments = confirmedCmts.size,
            openReports = confirmedRpts.count { it.status.equals("open", ignoreCase = true) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardCounts())

    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var verifyingUidInFlight: String? = null

    init {
        observeFirebaseAuthState(application)
    }

    private fun observeFirebaseAuthState(application: Application) {
        try {
            if (FirebaseApp.getApps(application).isEmpty()) {
                _authState.value = AuthUiState.Unauthenticated
                return
            }
            val auth = FirebaseAuth.getInstance()
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user == null) {
                    verifyingUidInFlight = null
                    repository.stopRealtimeListeners()
                    _isImageFormOpen.value = false
                    _isNotificationFormOpen.value = false
                    _authState.value = AuthUiState.Unauthenticated
                } else {
                    verifySignedInUser(user, force = false)
                }
            }
            authStateListener = listener
            auth.addAuthStateListener(listener)
        } catch (_: Exception) {
            _authState.value = AuthUiState.Unauthenticated
        }
    }

    private fun verifySignedInUser(user: FirebaseUser, force: Boolean = false) {
        if (!force) {
            val current = _authState.value
            if (verifyingUidInFlight == user.uid) return
            if (current is AuthUiState.Authorized && current.uid == user.uid) return
        }
        verifyingUidInFlight = user.uid
        _authState.value = AuthUiState.Loading
        viewModelScope.launch {
            val res = repository.verifyAdminUserFromFirestore(
                uid = user.uid,
                email = user.email,
                isEmailVerified = user.isEmailVerified
            )
            verifyingUidInFlight = null
            res.onSuccess { role ->
                if (role != null) {
                    val email = user.email.orEmpty()
                    val displayName = user.displayName?.ifBlank { email.substringBefore("@") }
                        ?: email.substringBefore("@").ifBlank { "অ্যাডমিন" }
                    _authState.value = AuthUiState.Authorized(
                        uid = user.uid,
                        email = email,
                        displayName = displayName,
                        role = role
                    )
                    if (role == AdminRole.MODERATOR) {
                        _currentSection.value = AdminSection.COMMENTS_REPORTS
                    } else if (!role.canAccessSection(_currentSection.value)) {
                        _currentSection.value = AdminSection.DASHBOARD
                    }
                    repository.startRealtimeListeners(role) { errMsg ->
                        showMessage(errMsg, isError = true)
                    }
                } else {
                    repository.stopRealtimeListeners()
                    _authState.value = AuthUiState.NotAdmin(
                        uid = user.uid,
                        email = user.email.orEmpty(),
                        message = "আপনি অ্যাডমিন নন"
                    )
                }
                _connectionStatus.value = repository.getConnectionStatus()
            }.onFailure { err ->
                repository.stopRealtimeListeners()
                _authState.value = AuthUiState.NotAdmin(
                    uid = user.uid,
                    email = user.email.orEmpty(),
                    message = repository.mapErrorToBangla(err)
                )
            }
        }
    }

    fun retryAdminVerification() {
        try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                verifySignedInUser(user, force = true)
            } else {
                _authState.value = AuthUiState.Unauthenticated
            }
        } catch (_: Exception) {
            _authState.value = AuthUiState.Unauthenticated
        }
    }

    fun onGoogleFirebaseUserAuthenticated(user: FirebaseUser?) {
        if (user == null) {
            _authErrorMessage.value = "লগ ইন সম্পন্ন হয়নি"
            return
        }
        _authErrorMessage.value = null
        verifySignedInUser(user, force = false)
    }

    fun setAuthErrorMessage(message: String?) {
        _authErrorMessage.value = message
    }

    fun showMessage(message: String, isError: Boolean = false) {
        _snackbarMessage.value = message
        _isErrorSnackbar.value = isError
    }

    fun dismissMessage() {
        _snackbarMessage.value = null
    }

    fun navigateTo(section: AdminSection) {
        val currentAuth = _authState.value
        if (currentAuth is AuthUiState.Authorized && !currentAuth.role.canAccessSection(section)) {
            showMessage("আপনার কেবল কমেন্ট ও রিপোর্ট দেখার অনুমতি আছে", isError = true)
            return
        }
        _isImageFormOpen.value = false
        _isNotificationFormOpen.value = false
        _currentSection.value = section
    }

    /**
     * Explicit sign-out called ONLY when the user taps "লগ আউট" and confirms.
     */
    fun signOutConfirmed() {
        repository.stopRealtimeListeners()
        _isImageFormOpen.value = false
        _isNotificationFormOpen.value = false
        _oneSignalHelperNotification.value = null
        try {
            if (FirebaseApp.getApps(getApplication()).isNotEmpty()) {
                FirebaseAuth.getInstance().signOut()
            }
        } catch (_: Exception) {
        }
        _authState.value = AuthUiState.Unauthenticated
    }

    // =========================================================================
    // FULL-SCREEN IMAGE ADD / EDIT FORM
    // =========================================================================
    fun openNewImageForm() {
        val currentAuth = _authState.value
        if (currentAuth is AuthUiState.Authorized && !currentAuth.role.canManageContent) {
            showMessage("অনুমতি নেই — আপনি অ্যাডমিন কিনা দেখুন", isError = true)
            return
        }
        // Generate the document ID ONCE when the form opens
        _activeFormDocumentId.value = repository.generateNewImageDocumentId()
        _editingImage.value = null
        _loadedPromptText.value = ""
        _isLoadingPrompt.value = false
        _isImageSaving.value = false
        _isImageFormOpen.value = true
        _currentSection.value = AdminSection.IMAGES
    }

    fun openEditImageForm(image: FirestoreImageItem) {
        val currentAuth = _authState.value
        if (currentAuth is AuthUiState.Authorized && !currentAuth.role.canManageContent) {
            showMessage("অনুমতি নেই — আপনি অ্যাডমিন কিনা দেখুন", isError = true)
            return
        }
        _activeFormDocumentId.value = image.id
        _editingImage.value = image
        _loadedPromptText.value = ""
        _isLoadingPrompt.value = true
        _isImageSaving.value = false
        _isImageFormOpen.value = true
        _currentSection.value = AdminSection.IMAGES

        viewModelScope.launch {
            val promptResult = repository.fetchPromptTextForImage(image.id)
            _isLoadingPrompt.value = false
            promptResult.onSuccess { text ->
                _loadedPromptText.value = text
            }.onFailure { err ->
                showMessage(repository.mapErrorToBangla(err), isError = true)
            }
        }
    }

    fun closeImageForm() {
        if (_isImageSaving.value) return
        _isImageFormOpen.value = false
        _editingImage.value = null
    }

    /**
     * Single-submit atomic image + prompt save.
     * Immediately locks `_isImageSaving = true` on the first tap so repeated taps never create duplicates.
     */
    fun saveImageFromForm(
        rawImageUrl: String,
        title: String,
        description: String,
        selectedCategoryId: String,
        selectedCategoryName: String,
        promptText: String,
        featured: Boolean,
        trending: Boolean,
        status: String,
        measuredAspectRatio: Double,
        isImagePreviewLoaded: Boolean
    ) {
        // Immediate single-submit guard
        if (_isImageSaving.value) return

        val normalizedUrl = UrlAndDeepLinkValidator.normalizePublicImageUrl(rawImageUrl)
        if (normalizedUrl.isBlank()) {
            showMessage("ছবির লিংক দিন", isError = true)
            return
        }
        val urlValidation = UrlAndDeepLinkValidator.validatePublicImageUrl(rawImageUrl)
        if (urlValidation is com.example.util.UrlValidationResult.Invalid) {
            showMessage(urlValidation.reason, isError = true)
            return
        }
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            showMessage("শিরোনাম লিখুন", isError = true)
            return
        }
        if (cleanTitle.length > 150) {
            showMessage("শিরোনাম সর্বোচ্চ ১৫০ অক্ষরের হতে পারে", isError = true)
            return
        }
        val cleanDescription = description.trim()
        if (cleanDescription.length > 1000) {
            showMessage("বিবরণ সর্বোচ্চ ১০০০ অক্ষরের হতে পারে", isError = true)
            return
        }
        val cleanPrompt = promptText.trim()
        if (cleanPrompt.isEmpty()) {
            showMessage("প্রম্পট লিখুন", isError = true)
            return
        }
        if (cleanPrompt.length > 5000) {
            showMessage("প্রম্পট সর্বোচ্চ ৫০০০ অক্ষরের হতে পারে", isError = true)
            return
        }

        val editing = _editingImage.value
        val docId = _activeFormDocumentId.value.ifBlank {
            editing?.id ?: repository.generateNewImageDocumentId()
        }
        _activeFormDocumentId.value = docId

        val finalCategoryId = selectedCategoryId.trim().ifEmpty { "uncategorized" }
        val finalCategoryName = if (finalCategoryId == "uncategorized") {
            "অন্যান্য"
        } else {
            selectedCategoryName.trim().ifEmpty { "অন্যান্য" }
        }

        val autoShortName = UrlAndDeepLinkValidator.generateShortNameFromTitle(cleanTitle)
        val autoKeywords = UrlAndDeepLinkValidator.generateKeywordsFromTitle(cleanTitle)
        val nextDisplayOrder = editing?.displayOrder
            ?: ((images.value.maxOfOrNull { it.displayOrder } ?: 0) + 1)
        val recommendedVal = editing?.recommended ?: true

        _isImageSaving.value = true
        viewModelScope.launch {
            val result = repository.saveImageAndPromptAtomic(
                documentId = docId,
                isEdit = editing != null,
                previousCategoryId = editing?.categoryId,
                title = cleanTitle,
                shortName = autoShortName,
                description = cleanDescription,
                categoryId = finalCategoryId,
                categoryName = finalCategoryName,
                imageUrl = normalizedUrl,
                aspectRatio = measuredAspectRatio.coerceIn(0.2, 5.0),
                featured = featured,
                trending = trending,
                recommended = recommendedVal,
                keywords = autoKeywords,
                displayOrder = nextDisplayOrder,
                status = if (status.equals("draft", ignoreCase = true)) "draft" else "published",
                promptText = cleanPrompt
            )
            _isImageSaving.value = false
            result.onSuccess {
                _isImageFormOpen.value = false
                _editingImage.value = null
                showMessage("ছবি সেভ হয়েছে", isError = false)
            }.onFailure { err ->
                // Stay on the form, re-enable button, show real Bangla error
                showMessage(repository.mapErrorToBangla(err), isError = true)
            }
        }
    }

    fun deleteImageConfirmed(image: FirestoreImageItem) {
        if (_isActionSubmitting.value) return
        _isActionSubmitting.value = true
        viewModelScope.launch {
            val res = repository.deleteImageAtomic(image)
            _isActionSubmitting.value = false
            res.onSuccess {
                showMessage("ছবি মুছে ফেলা হয়েছে", isError = false)
            }.onFailure { err ->
                showMessage(repository.mapErrorToBangla(err), isError = true)
            }
        }
    }

    fun moveImageOrder(image: FirestoreImageItem, moveUp: Boolean) {
        if (_isActionSubmitting.value) return
        val currentList = images.value
        val index = currentList.indexOfFirst { it.id == image.id }
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex !in currentList.indices) return

        val neighbor = currentList[targetIndex]
        _isActionSubmitting.value = true
        viewModelScope.launch {
            val res = repository.swapImageDisplayOrder(image, neighbor)
            _isActionSubmitting.value = false
            res.onFailure { err ->
                showMessage(repository.mapErrorToBangla(err), isError = true)
            }
        }
    }

    // =========================================================================
    // CATEGORIES
    // =========================================================================
    fun saveCategory(
        existingCategory: FirestoreCategoryItem?,
        name: String,
        active: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        if (_isActionSubmitting.value) return
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            showMessage("ক্যাটাগরির নাম লিখুন", isError = true)
            return
        }
        _isActionSubmitting.value = true
        viewModelScope.launch {
            val res = repository.saveCategoryAtomic(
                existingCategory = existingCategory,
                name = cleanName,
                active = active
            )
            _isActionSubmitting.value = false
            res.onSuccess {
                showMessage("ক্যাটাগরি সেভ হয়েছে", isError = false)
                onSuccess()
            }.onFailure { err ->
                showMessage(repository.mapErrorToBangla(err), isError = true)
            }
        }
    }

    fun deleteCategoryConfirmed(category: FirestoreCategoryItem) {
        if (_isActionSubmitting.value) return
        _isActionSubmitting.value = true
        viewModelScope.launch {
            val res = repository.deleteCategoryKeepImages(category)
            _isActionSubmitting.value = false
            res.onSuccess {
                showMessage("ক্যাটাগরি মুছে ফেলা হয়েছে। এই ক্যাটাগরির ছবিগুলো এখন 'অন্যান্য' হিসেবে থাকবে।", isError = false)
            }.onFailure { err ->
                showMessage(repository.mapErrorToBangla(err), isError = true)
            }
        }
    }

    // =========================================================================
    // COMMENTS & REPORTS
    // =========================================================================
    fun toggleCommentVisibility(comment: FirestoreCommentItem) {
        if (_isActionSubmitting.value) return
        _isActionSubmitting.value = true
        val makeVisible = !comment.isVisible
        viewModelScope.launch {
            val res = repository.setCommentVisibility(comment, makeVisible)
            _isActionSubmitting.value = false
            res.onSuccess {
                showMessage(if (makeVisible) "কমেন্ট দৃশ্যমান করা হয়েছে" else "কমেন্ট লুকানো হয়েছে", isError = false)
            }.onFailure { err ->
                showMessage(repository.mapErrorToBangla(err), isError = true)
            }
        }
    }

    fun replyToComment(comment: FirestoreCommentItem, replyText: String, onDone: () -> Unit = {}) {
        if (_isActionSubmitting.value) return
        val cleanReply = replyText.trim()
        if (cleanReply.isEmpty()) {
            showMessage("উত্তর লিখুন", isError = true)
            return
        }
        if (cleanReply.length > 500) {
            showMessage("উত্তর সর্বোচ্চ ৫০০ অক্ষরের হতে পারে", isError = true)
            return
        }
        _isActionSubmitting.value = true
        viewModelScope.launch {
            val res = repository.saveCommentAdminReply(comment, cleanReply)
            _isActionSubmitting.value = false
            res.onSuccess {
                showMessage("সেভ হয়েছে", isError = false)
                onDone()
            }.onFailure { err ->
                showMessage(repository.mapErrorToBangla(err), isError = true)
            }
        }
    }

    fun deleteCommentConfirmed(comment: FirestoreCommentItem) {
        if (_isActionSubmitting.value) return
        _isActionSubmitting.value = true
        viewModelScope.launch {
            val res = repository.deleteCommentAtomic(comment)
            _isActionSubmitting.value = false
            res.onSuccess {
                showMessage("কমেন্ট মুছে ফেলা হয়েছে", isError = false)
            }.onFailure { err ->
                showMessage(repository.mapErrorToBangla(err), isError = true)
            }
        }
    }

    fun setReportStatus(report: FirestoreReportItem, newStatus: String) {
        if (_isActionSubmitting.value) return
        _isActionSubmitting.value = true
        viewModelScope.launch {
            val res = repository.updateReportStatus(report, newStatus)
            _isActionSubmitting.value = false
            res.onSuccess {
                showMessage("রিপোর্টের অবস্থা আপডেট হয়েছে", isError = false)
            }.onFailure { err ->
                showMessage(repository.mapErrorToBangla(err), isError = true)
            }
        }
    }

    // =========================================================================
    // FULL-SCREEN NOTIFICATION ADD / EDIT FORM & ONESIGNAL HELPER
    // =========================================================================
    fun openNewNotificationForm() {
        val currentAuth = _authState.value
        if (currentAuth is AuthUiState.Authorized && !currentAuth.role.canManageContent) {
            showMessage("অনুমতি নেই / ডাটা নিয়মের সাথে মেলেনি", isError = true)
            return
        }
        // Generate the document ID ONCE when the form opens
        _activeNotificationDocId.value = repository.generateNewNotificationDocumentId()
        _editingNotification.value = null
        _isNotificationSaving.value = false
        _isNotificationFormOpen.value = true
        _currentSection.value = AdminSection.NOTIFICATION_HELPER
    }

    fun openEditNotificationForm(notification: FirestoreNotificationItem) {
        val currentAuth = _authState.value
        if (currentAuth is AuthUiState.Authorized && !currentAuth.role.canManageContent) {
            showMessage("অনুমতি নেই / ডাটা নিয়মের সাথে মেলেনি", isError = true)
            return
        }
        _activeNotificationDocId.value = notification.id
        _editingNotification.value = notification
        _isNotificationSaving.value = false
        _isNotificationFormOpen.value = true
        _currentSection.value = AdminSection.NOTIFICATION_HELPER
    }

    fun closeNotificationForm() {
        if (_isNotificationSaving.value) return
        _isNotificationFormOpen.value = false
        _editingNotification.value = null
    }

    fun showOneSignalHelperFor(notification: FirestoreNotificationItem) {
        _oneSignalHelperNotification.value = notification
    }

    fun dismissOneSignalHelper() {
        _oneSignalHelperNotification.value = null
    }

    /**
     * Single-submit server-confirmed notification save (`notifications/{id}`).
     * Writes EXACTLY: title, body, imageId, imageUrl, categoryName, status, createdAt (on create),
     * or updates ONLY: title, body, imageId, imageUrl, categoryName, status (on edit).
     */
    fun saveNotificationFromForm(
        selectedPost: FirestoreImageItem?,
        fallbackImageId: String,
        fallbackImageUrl: String,
        fallbackCategoryName: String,
        title: String,
        body: String,
        status: String
    ) {
        if (_isNotificationSaving.value) return

        val finalImageId = selectedPost?.id?.trim().orEmpty().ifEmpty { fallbackImageId.trim() }
        val finalImageUrl = selectedPost?.imageUrl?.trim().orEmpty().ifEmpty { fallbackImageUrl.trim() }
        val finalCategoryName = selectedPost?.categoryName?.trim().orEmpty()
            .ifEmpty { fallbackCategoryName.trim() }
            .ifEmpty { "অন্যান্য" }

        if (finalImageId.isEmpty() || finalImageUrl.isEmpty()) {
            showMessage("তালিকা থেকে একটি ছবি নির্বাচন করুন", isError = true)
            return
        }

        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            showMessage("শিরোনাম লিখুন", isError = true)
            return
        }
        if (cleanTitle.length > 120) {
            showMessage("শিরোনাম সর্বোচ্চ ১২০ অক্ষরের হতে পারে", isError = true)
            return
        }

        val cleanBody = body.trim()
        if (cleanBody.isEmpty()) {
            showMessage("বার্তা লিখুন", isError = true)
            return
        }
        if (cleanBody.length > 500) {
            showMessage("বার্তা সর্বোচ্চ ৫০০ অক্ষরের হতে পারে", isError = true)
            return
        }

        val editing = _editingNotification.value
        val docId = _activeNotificationDocId.value.ifBlank {
            editing?.id ?: repository.generateNewNotificationDocumentId()
        }
        _activeNotificationDocId.value = docId

        _isNotificationSaving.value = true
        viewModelScope.launch {
            val res = repository.saveNotificationAtomic(
                documentId = docId,
                isEdit = editing != null,
                title = cleanTitle,
                body = cleanBody,
                imageId = finalImageId,
                imageUrl = finalImageUrl,
                categoryName = finalCategoryName,
                status = if (status.equals("draft", ignoreCase = true)) "draft" else "published"
            )
            _isNotificationSaving.value = false
            res.onSuccess { savedItem ->
                _isNotificationFormOpen.value = false
                _editingNotification.value = null
                _oneSignalHelperNotification.value = savedItem
                showMessage("সেভ হয়েছে", isError = false)
            }.onFailure { err ->
                showMessage(repository.mapErrorToBangla(err), isError = true)
            }
        }
    }

    fun deleteNotificationConfirmed(notification: FirestoreNotificationItem) {
        if (_isActionSubmitting.value) return
        _isActionSubmitting.value = true
        viewModelScope.launch {
            val res = repository.deleteNotificationAtomic(notification)
            _isActionSubmitting.value = false
            res.onSuccess {
                if (_oneSignalHelperNotification.value?.id == notification.id) {
                    _oneSignalHelperNotification.value = null
                }
                showMessage("নোটিফিকেশন মুছে ফেলা হয়েছে", isError = false)
            }.onFailure { err ->
                showMessage(repository.mapErrorToBangla(err), isError = true)
            }
        }
    }

    // =========================================================================
    // ADS & SETTINGS
    // =========================================================================
    fun saveAdSettings(updated: AdvertisementSettingsModel) {
        if (_isActionSubmitting.value) return
        _isActionSubmitting.value = true
        viewModelScope.launch {
            val res = repository.saveAdvertisementSettingsGlobal(updated)
            _isActionSubmitting.value = false
            res.onSuccess {
                showMessage("বিজ্ঞাপন সেটিংস সেভ হয়েছে", isError = false)
            }.onFailure { err ->
                showMessage(repository.mapErrorToBangla(err), isError = true)
            }
        }
    }

    fun saveAppSettings(updated: AppGlobalSettingsModel) {
        if (_isActionSubmitting.value) return
        _isActionSubmitting.value = true
        viewModelScope.launch {
            val res = repository.saveAppGlobalSettings(updated)
            _isActionSubmitting.value = false
            res.onSuccess {
                showMessage("সেটিংস সেভ হয়েছে", isError = false)
            }.onFailure { err ->
                showMessage(repository.mapErrorToBangla(err), isError = true)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopRealtimeListeners()
        try {
            authStateListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
        } catch (_: Exception) {
        }
    }
}
