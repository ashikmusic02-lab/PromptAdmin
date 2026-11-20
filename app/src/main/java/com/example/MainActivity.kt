package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AdminRole
import com.example.model.AdminSection
import com.example.model.AuthUiState
import com.example.ui.components.DestructiveConfirmationDialog
import com.example.ui.components.StatusPill
import com.example.ui.screens.AdminAuthScreen
import com.example.ui.screens.AdsSettingsScreen
import com.example.ui.screens.AppSettingsScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.CommentsAndReportsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FullScreenImageForm
import com.example.ui.screens.FullScreenNotificationForm
import com.example.ui.screens.ImagesScreen
import com.example.ui.screens.NotAdminScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.SplashLoadingScreen
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PromptAdminTheme
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusEmeraldBg
import com.example.ui.theme.StatusRose
import com.example.ui.theme.StatusRoseBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.theme.VioletContainer
import com.example.ui.theme.VioletPrimary
import com.example.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PromptAdminTheme {
                PromptAdminRootApp()
            }
        }
    }
}

private fun sectionIcon(section: AdminSection): ImageVector {
    return when (section) {
        AdminSection.DASHBOARD -> Icons.Default.Dashboard
        AdminSection.IMAGES -> Icons.Default.Collections
        AdminSection.CATEGORIES -> Icons.Default.Category
        AdminSection.COMMENTS_REPORTS -> Icons.Default.Forum
        AdminSection.NOTIFICATION_HELPER -> Icons.Default.NotificationsActive
        AdminSection.ADS -> Icons.Default.Campaign
        AdminSection.SETTINGS -> Icons.Default.Settings
    }
}

private fun roleTone(role: AdminRole): String {
    return when (role) {
        AdminRole.OWNER -> "OWNER"
        AdminRole.ADMIN -> "ADMIN"
        AdminRole.MODERATOR -> "MODERATOR"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptAdminRootApp(viewModel: AdminViewModel = viewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val authErrorMessage by viewModel.authErrorMessage.collectAsStateWithLifecycle()
    val currentSection by viewModel.currentSection.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val isErrorSnackbar by viewModel.isErrorSnackbar.collectAsStateWithLifecycle()

    val isImageFormOpen by viewModel.isImageFormOpen.collectAsStateWithLifecycle()
    val editingImage by viewModel.editingImage.collectAsStateWithLifecycle()
    val loadedPromptText by viewModel.loadedPromptText.collectAsStateWithLifecycle()
    val isLoadingPrompt by viewModel.isLoadingPrompt.collectAsStateWithLifecycle()
    val isImageSaving by viewModel.isImageSaving.collectAsStateWithLifecycle()

    val isNotificationFormOpen by viewModel.isNotificationFormOpen.collectAsStateWithLifecycle()
    val editingNotification by viewModel.editingNotification.collectAsStateWithLifecycle()
    val isNotificationSaving by viewModel.isNotificationSaving.collectAsStateWithLifecycle()
    val oneSignalHelperNotification by viewModel.oneSignalHelperNotification.collectAsStateWithLifecycle()

    val isActionSubmitting by viewModel.isActionSubmitting.collectAsStateWithLifecycle()

    val dashboardCounts by viewModel.dashboardCounts.collectAsStateWithLifecycle()
    val images by viewModel.images.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val adSettings by viewModel.adSettings.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val isLoadingData by viewModel.isLoadingData.collectAsStateWithLifecycle()

    var showSignOutConfirmation by remember { mutableStateOf(false) }

    when (val state = authState) {
        is AuthUiState.Loading -> {
            SplashLoadingScreen()
        }
        is AuthUiState.Unauthenticated -> {
            AdminAuthScreen(
                externalAuthError = authErrorMessage,
                onGoogleFirebaseUserAuthenticated = { firebaseUser ->
                    viewModel.onGoogleFirebaseUserAuthenticated(firebaseUser)
                },
                onSetAuthError = { err ->
                    viewModel.setAuthErrorMessage(err)
                }
            )
        }
        is AuthUiState.NotAdmin -> {
            NotAdminScreen(
                uid = state.uid,
                email = state.email,
                message = state.message,
                onRetryVerification = { viewModel.retryAdminVerification() },
                onSignOutClick = { showSignOutConfirmation = true }
            )
        }
        is AuthUiState.Authorized -> {
            val allowedSections = remember(state.role) {
                AdminSection.entries.filter { state.role.canAccessSection(it) }
            }

            if (isImageFormOpen) {
                Box(modifier = Modifier.fillMaxSize()) {
                    FullScreenImageForm(
                        editingImage = editingImage,
                        loadedPromptText = loadedPromptText,
                        isLoadingPrompt = isLoadingPrompt,
                        isSaving = isImageSaving,
                        categories = categories,
                        onClose = { viewModel.closeImageForm() },
                        onSave = { rawUrl, title, desc, catId, catName, prompt, featured, trending, status, ratio, previewLoaded ->
                            viewModel.saveImageFromForm(
                                rawImageUrl = rawUrl,
                                title = title,
                                description = desc,
                                selectedCategoryId = catId,
                                selectedCategoryName = catName,
                                promptText = prompt,
                                featured = featured,
                                trending = trending,
                                status = status,
                                measuredAspectRatio = ratio,
                                isImagePreviewLoaded = previewLoaded
                            )
                        },
                        onShowMessage = { msg ->
                            viewModel.showMessage(msg, isError = false)
                        }
                    )

                    if (snackbarMessage != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        ) {
                            BanglaFeedbackBanner(
                                message = snackbarMessage ?: "",
                                isError = isErrorSnackbar,
                                onDismiss = { viewModel.dismissMessage() }
                            )
                        }
                    }
                }
            } else if (isNotificationFormOpen) {
                Box(modifier = Modifier.fillMaxSize()) {
                    FullScreenNotificationForm(
                        editingNotification = editingNotification,
                        images = images,
                        isSaving = isNotificationSaving,
                        onClose = { viewModel.closeNotificationForm() },
                        onSave = { selectedPost, fallbackImageId, fallbackImageUrl, fallbackCategoryName, title, body, status ->
                            viewModel.saveNotificationFromForm(
                                selectedPost = selectedPost,
                                fallbackImageId = fallbackImageId,
                                fallbackImageUrl = fallbackImageUrl,
                                fallbackCategoryName = fallbackCategoryName,
                                title = title,
                                body = body,
                                status = status
                            )
                        }
                    )

                    if (snackbarMessage != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        ) {
                            BanglaFeedbackBanner(
                                message = snackbarMessage ?: "",
                                isError = isErrorSnackbar,
                                onDismiss = { viewModel.dismissMessage() }
                            )
                        }
                    }
                }
            } else {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                BackHandler(
                    enabled = state.role != AdminRole.MODERATOR && currentSection != AdminSection.DASHBOARD
                ) {
                    viewModel.navigateTo(AdminSection.DASHBOARD)
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = CharcoalSurface,
                            drawerContentColor = TextPrimary
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "PromptAdmin",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    StatusPill(
                                        label = state.role.banglaBadge,
                                        tone = roleTone(state.role)
                                    )
                                }
                                if (state.email.isNotBlank()) {
                                    Text(
                                        text = state.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            HorizontalDivider(color = GraphiteBorder)
                            Spacer(modifier = Modifier.height(8.dp))

                            allowedSections.forEach { section ->
                                val selected = currentSection == section
                                NavigationDrawerItem(
                                    label = {
                                        Text(
                                            text = section.titleBangla,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    },
                                    selected = selected,
                                    onClick = {
                                        viewModel.navigateTo(section)
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = sectionIcon(section),
                                            contentDescription = section.titleBangla
                                        )
                                    },
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = VioletContainer,
                                        selectedIconColor = VioletBright,
                                        selectedTextColor = TextPrimary,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 2.dp)
                                        .testTag("drawer_nav_${section.name.lowercase()}")
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))
                            HorizontalDivider(color = GraphiteBorder)

                            OutlinedButton(
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    showSignOutConfirmation = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .heightIn(min = 48.dp)
                                    .testTag("drawer_sign_out_button"),
                                border = BorderStroke(1.dp, StatusRose.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "লগ আউট",
                                    tint = StatusRose,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "লগ আউট",
                                    color = StatusRose,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                ) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ObsidianBg),
                        containerColor = ObsidianBg,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = currentSection.titleBangla,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                navigationIcon = {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("open_drawer_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "মেনু",
                                            tint = TextPrimary
                                        )
                                    }
                                },
                                actions = {
                                    StatusPill(
                                        label = state.role.banglaBadge,
                                        tone = roleTone(state.role)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { showSignOutConfirmation = true },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("top_bar_sign_out_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Logout,
                                            contentDescription = "লগ আউট",
                                            tint = StatusRose
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = CharcoalSurface,
                                    titleContentColor = TextPrimary
                                )
                            )
                        },
                        bottomBar = {
                            if (state.role != AdminRole.MODERATOR) {
                                val bottomItems = listOf(
                                    AdminSection.DASHBOARD,
                                    AdminSection.IMAGES,
                                    AdminSection.CATEGORIES,
                                    AdminSection.COMMENTS_REPORTS,
                                    AdminSection.NOTIFICATION_HELPER
                                )
                                NavigationBar(
                                    containerColor = CharcoalSurface,
                                    contentColor = TextPrimary
                                ) {
                                    bottomItems.forEach { section ->
                                        NavigationBarItem(
                                            selected = currentSection == section,
                                            onClick = { viewModel.navigateTo(section) },
                                            icon = {
                                                Icon(
                                                    imageVector = sectionIcon(section),
                                                    contentDescription = section.titleBangla
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = section.titleBangla,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = VioletBright,
                                                selectedTextColor = TextPrimary,
                                                indicatorColor = VioletContainer,
                                                unselectedIconColor = TextSecondary,
                                                unselectedTextColor = TextSecondary
                                            ),
                                            modifier = Modifier.testTag("bottom_nav_${section.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            AnimatedVisibility(visible = snackbarMessage != null) {
                                snackbarMessage?.let { msg ->
                                    BanglaFeedbackBanner(
                                        message = msg,
                                        isError = isErrorSnackbar,
                                        onDismiss = { viewModel.dismissMessage() }
                                    )
                                }
                            }

                            when (currentSection) {
                                AdminSection.DASHBOARD -> {
                                    DashboardScreen(
                                        counts = dashboardCounts,
                                        onNavigate = { viewModel.navigateTo(it) },
                                        onAddNewImage = { viewModel.openNewImageForm() }
                                    )
                                }
                                AdminSection.IMAGES -> {
                                    ImagesScreen(
                                        images = images,
                                        categories = categories,
                                        isLoading = isLoadingData,
                                        isSubmitting = isActionSubmitting,
                                        onOpenNewImageForm = { viewModel.openNewImageForm() },
                                        onOpenEditImageForm = { img -> viewModel.openEditImageForm(img) },
                                        onDeleteImage = { img -> viewModel.deleteImageConfirmed(img) },
                                        onMoveImageOrder = { img, up -> viewModel.moveImageOrder(img, up) }
                                    )
                                }
                                AdminSection.CATEGORIES -> {
                                    CategoriesScreen(
                                        categories = categories,
                                        isSubmitting = isActionSubmitting,
                                        onSaveCategory = { existing, name, active, onDone ->
                                            viewModel.saveCategory(existing, name, active, onDone)
                                        },
                                        onDeleteCategory = { cat ->
                                            viewModel.deleteCategoryConfirmed(cat)
                                        }
                                    )
                                }
                                AdminSection.COMMENTS_REPORTS -> {
                                    CommentsAndReportsScreen(
                                        comments = comments,
                                        reports = reports,
                                        isSubmitting = isActionSubmitting,
                                        onToggleCommentVisibility = { cmt ->
                                            viewModel.toggleCommentVisibility(cmt)
                                        },
                                        onReplyToComment = { cmt, reply, onDone ->
                                            viewModel.replyToComment(cmt, reply, onDone)
                                        },
                                        onDeleteComment = { cmt ->
                                            viewModel.deleteCommentConfirmed(cmt)
                                        },
                                        onUpdateReportStatus = { rpt, st ->
                                            viewModel.setReportStatus(rpt, st)
                                        }
                                    )
                                }
                                AdminSection.NOTIFICATION_HELPER -> {
                                    NotificationsScreen(
                                        notifications = notifications,
                                        oneSignalHelperItem = oneSignalHelperNotification,
                                        isSubmitting = isActionSubmitting,
                                        onOpenNewNotificationForm = { viewModel.openNewNotificationForm() },
                                        onOpenEditNotificationForm = { item ->
                                            viewModel.openEditNotificationForm(item)
                                        },
                                        onDeleteNotification = { item ->
                                            viewModel.deleteNotificationConfirmed(item)
                                        },
                                        onSelectForOneSignalHelper = { item ->
                                            viewModel.showOneSignalHelperFor(item)
                                        },
                                        onDismissOneSignalHelper = {
                                            viewModel.dismissOneSignalHelper()
                                        },
                                        onShowMessage = { msg ->
                                            viewModel.showMessage(msg, isError = false)
                                        }
                                    )
                                }
                                AdminSection.ADS -> {
                                    AdsSettingsScreen(
                                        settings = adSettings,
                                        isSaving = isActionSubmitting,
                                        onSave = { updated ->
                                            viewModel.saveAdSettings(updated)
                                        }
                                    )
                                }
                                AdminSection.SETTINGS -> {
                                    AppSettingsScreen(
                                        settings = appSettings,
                                        isSaving = isActionSubmitting,
                                        onSave = { updated ->
                                            viewModel.saveAppSettings(updated)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSignOutConfirmation) {
        DestructiveConfirmationDialog(
            title = "লগ আউট করবেন?",
            message = "আপনি কি নিশ্চিতভাবে লগ আউট করতে চান?",
            confirmButtonText = "লগ আউট",
            onConfirm = { viewModel.signOutConfirmed() },
            onDismiss = { showSignOutConfirmation = false }
        )
    }
}

@Composable
private fun BanglaFeedbackBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("feedback_banner"),
        color = if (isError) StatusRoseBg else StatusEmeraldBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isError) StatusRose else StatusEmerald
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                    contentDescription = if (isError) "ত্রুটি" else "সফল",
                    tint = if (isError) StatusRose else StatusEmerald
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "বন্ধ করুন",
                    tint = TextPrimary
                )
            }
        }
    }
}
