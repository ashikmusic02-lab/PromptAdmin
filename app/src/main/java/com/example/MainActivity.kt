package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AppSettingsAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AdminSection
import com.example.ui.components.StatusPill
import com.example.ui.screens.AdminAuthScreen
import com.example.ui.screens.AdministratorManagementScreen
import com.example.ui.screens.AdvertisementManagementScreen
import com.example.ui.screens.AnalyticsComparisonScreen
import com.example.ui.screens.AppContentManagementScreen
import com.example.ui.screens.CategoryManagementScreen
import com.example.ui.screens.CommentModerationScreen
import com.example.ui.screens.DashboardOverviewScreen
import com.example.ui.screens.ImageManagementScreen
import com.example.ui.screens.LikesAndEngagementScreen
import com.example.ui.screens.NotificationHistoryScreen
import com.example.ui.screens.OneSignalNotificationScreen
import com.example.ui.screens.PlatformSettingsScreen
import com.example.ui.screens.SecurityActivityLogsScreen
import com.example.ui.screens.UserManagementScreen
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneLight
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PromptAdminTheme
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.theme.VioletContainer
import com.example.ui.theme.VioletPrimary
import com.example.viewmodel.AdminSession
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

@Composable
fun PromptAdminRootApp(
    adminViewModel: AdminViewModel = viewModel()
) {
    val session by adminViewModel.session.collectAsStateWithLifecycle()
    val currentSection by adminViewModel.currentSection.collectAsStateWithLifecycle()
    val connectionStatus by adminViewModel.connectionStatus.collectAsStateWithLifecycle()
    val stats by adminViewModel.liveStats.collectAsStateWithLifecycle()
    val analyticsRange by adminViewModel.analyticsRange.collectAsStateWithLifecycle()
    val imageSortOption by adminViewModel.imageSortOption.collectAsStateWithLifecycle()
    val images by adminViewModel.images.collectAsStateWithLifecycle()
    val categories by adminViewModel.categories.collectAsStateWithLifecycle()
    val users by adminViewModel.users.collectAsStateWithLifecycle()
    val comments by adminViewModel.comments.collectAsStateWithLifecycle()
    val adMobSettings by adminViewModel.adMobSettings.collectAsStateWithLifecycle()
    val appContentSettings by adminViewModel.appContentSettings.collectAsStateWithLifecycle()
    val notifications by adminViewModel.notifications.collectAsStateWithLifecycle()
    val adminAccounts by adminViewModel.adminAccounts.collectAsStateWithLifecycle()
    val activityLogs by adminViewModel.activityLogs.collectAsStateWithLifecycle()
    val preselectedImage by adminViewModel.preselectedNotificationImage.collectAsStateWithLifecycle()
    val bannerMessage by adminViewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val isErrorBanner by adminViewModel.isErrorBanner.collectAsStateWithLifecycle()
    val isSyncing by adminViewModel.isSyncing.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        color = ObsidianBg
    ) {
        if (!session.isAuthenticated) {
            AdminAuthScreen(
                connectionStatus = connectionStatus,
                onAuthenticated = { uid, email, name, role, method ->
                    adminViewModel.signInAdmin(uid, email, name, role, method)
                }
            )
        } else {
            // BackHandler returns from any secondary module to Dashboard Overview
            BackHandler(enabled = currentSection != AdminSection.DASHBOARD) {
                adminViewModel.navigateTo(AdminSection.DASHBOARD)
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 760.dp
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                val mainWorkspace: @Composable () -> Unit = {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Enterprise Command Bar
                        TopEnterpriseHeaderBar(
                            currentSection = currentSection,
                            session = session,
                            showMenuButton = !isWideScreen,
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onSignOut = { adminViewModel.signOutAdmin() }
                        )

                        // Always-visible 14-Module Quick Switcher Strip for effortless navigation on mobile & tablet
                        QuickModuleNavigationStrip(
                            currentSection = currentSection,
                            onSelectSection = { adminViewModel.navigateTo(it) }
                        )

                        // Toast / Feedback Status Banner
                        AnimatedVisibility(
                            visible = bannerMessage != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            bannerMessage?.let { msg ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 8.dp),
                                    color = if (isErrorBanner) StatusRose.copy(alpha = 0.18f) else StatusEmerald.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isErrorBanner) StatusRose else StatusEmerald
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { adminViewModel.dismissFeedback() },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Dismiss notification",
                                                tint = TextPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Active Module Screen Content
                        Box(modifier = Modifier.weight(1f)) {
                            when (currentSection) {
                                AdminSection.DASHBOARD -> DashboardOverviewScreen(
                                    stats = stats,
                                    selectedRange = analyticsRange,
                                    onSelectRange = adminViewModel::setAnalyticsRange,
                                    images = images,
                                    adMobSettings = adMobSettings,
                                    recentLogs = activityLogs,
                                    connectionStatus = connectionStatus,
                                    onNavigateTo = adminViewModel::navigateTo,
                                    onSyncFirestore = adminViewModel::triggerFirestoreFullSync
                                )
                                AdminSection.IMAGE_MANAGEMENT -> ImageManagementScreen(
                                    images = images,
                                    categories = categories,
                                    deepLinkScheme = appContentSettings.deepLinkScheme,
                                    activeRole = session.role,
                                    onSaveImage = adminViewModel::saveImageRecord,
                                    onToggleQuickFlag = { img, pub, feat, trend ->
                                        adminViewModel.toggleImageQuickFlag(img, pub, feat, trend)
                                    },
                                    onDeleteImage = adminViewModel::deleteImageConfirmed,
                                    onSendPushForImage = adminViewModel::openNotificationComposerForImage,
                                    onNavigateToCategories = {
                                        adminViewModel.navigateTo(AdminSection.CATEGORIES)
                                    }
                                )
                                AdminSection.CATEGORIES -> CategoryManagementScreen(
                                    categories = categories,
                                    images = images,
                                    activeRole = session.role,
                                    onSaveCategory = adminViewModel::saveCategoryRecord,
                                    onDeleteCategoryProtected = adminViewModel::deleteCategoryProtected
                                )
                                AdminSection.USER_MANAGEMENT -> UserManagementScreen(
                                    users = users,
                                    comments = comments,
                                    activeRole = session.role,
                                    onUpsertUser = adminViewModel::registerOrUpdateUser
                                )
                                AdminSection.COMMENT_MODERATION -> CommentModerationScreen(
                                    comments = comments,
                                    images = images,
                                    activeRole = session.role,
                                    onModerateComment = adminViewModel::moderateComment,
                                    onDeleteCommentPermanently = adminViewModel::deleteCommentPermanently,
                                    onCreateCommentEntry = adminViewModel::createAdminCommentEntry
                                )
                                AdminSection.LIKES_ENGAGEMENT -> LikesAndEngagementScreen(
                                    images = images,
                                    canEditTelemetry = session.role.canManageImages,
                                    onUpdateTelemetry = adminViewModel::updateImageTelemetryCounters
                                )
                                AdminSection.ANALYTICS -> AnalyticsComparisonScreen(
                                    images = images,
                                    categories = categories,
                                    selectedSort = imageSortOption,
                                    onSelectSort = adminViewModel::setImageSortOption,
                                    selectedRange = analyticsRange,
                                    onSelectRange = adminViewModel::setAnalyticsRange
                                )
                                AdminSection.ADVERTISEMENT_MANAGEMENT -> AdvertisementManagementScreen(
                                    adMobSettings = adMobSettings,
                                    activeRole = session.role,
                                    onSaveAdMobSettings = adminViewModel::updateAdMobConfiguration
                                )
                                AdminSection.ONESIGNAL_NOTIFICATIONS -> OneSignalNotificationScreen(
                                    publishedImages = images.filter { it.status == "PUBLISHED" },
                                    preselectedImage = preselectedImage,
                                    deepLinkScheme = appContentSettings.deepLinkScheme,
                                    oneSignalAppId = appContentSettings.oneSignalAppId,
                                    oneSignalFunctionUrl = appContentSettings.oneSignalFunctionUrl,
                                    activeRole = session.role,
                                    onSendOrSchedule = { title, msg, aud, img, dl, schedType, schedTime, done ->
                                        adminViewModel.sendOrScheduleNotification(
                                            title, msg, aud, img, dl, schedType, schedTime
                                        ) {
                                            adminViewModel.clearPreselectedNotificationImage()
                                            done()
                                        }
                                    },
                                    onNavigateToHistory = {
                                        adminViewModel.navigateTo(AdminSection.NOTIFICATION_HISTORY)
                                    }
                                )
                                AdminSection.NOTIFICATION_HISTORY -> NotificationHistoryScreen(
                                    notifications = notifications
                                )
                                AdminSection.APP_CONTENT -> AppContentManagementScreen(
                                    settings = appContentSettings,
                                    images = images,
                                    categories = categories,
                                    activeRole = session.role,
                                    onSaveSettings = adminViewModel::updateAppContentConfiguration
                                )
                                AdminSection.ADMINISTRATOR_MANAGEMENT -> AdministratorManagementScreen(
                                    adminAccounts = adminAccounts,
                                    session = session,
                                    onSaveAdmin = adminViewModel::saveAdministratorAccount,
                                    onRemoveAdmin = adminViewModel::removeAdministratorAccount,
                                    onSwitchPreviewRole = adminViewModel::switchActiveRoleForPreview
                                )
                                AdminSection.SECURITY_ACTIVITY_LOGS -> SecurityActivityLogsScreen(
                                    logs = activityLogs
                                )
                                AdminSection.SETTINGS -> PlatformSettingsScreen(
                                    settings = appContentSettings,
                                    connectionStatus = connectionStatus,
                                    isSyncing = isSyncing,
                                    activeRole = session.role,
                                    onSaveSettings = adminViewModel::updateAppContentConfiguration,
                                    onTriggerFirestoreSync = adminViewModel::triggerFirestoreFullSync
                                )
                            }
                        }
                    }
                }

                if (isWideScreen) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        EnterpriseSidebarContent(
                            currentSection = currentSection,
                            session = session,
                            onSelectSection = { adminViewModel.navigateTo(it) },
                            onSignOut = { adminViewModel.signOutAdmin() },
                            modifier = Modifier
                                .width(276.dp)
                                .fillMaxHeight()
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            mainWorkspace()
                        }
                    }
                } else {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                drawerContainerColor = CharcoalSurface,
                                modifier = Modifier.width(292.dp)
                            ) {
                                EnterpriseSidebarContent(
                                    currentSection = currentSection,
                                    session = session,
                                    onSelectSection = { section ->
                                        adminViewModel.navigateTo(section)
                                        scope.launch { drawerState.close() }
                                    },
                                    onSignOut = {
                                        scope.launch { drawerState.close() }
                                        adminViewModel.signOutAdmin()
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    ) {
                        mainWorkspace()
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickModuleNavigationStrip(
    currentSection: AdminSection,
    onSelectSection: (AdminSection) -> Unit
) {
    Surface(
        color = ObsidianBg,
        border = BorderStroke(1.dp, GraphiteBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdminSection.entries.forEach { section ->
                val selected = currentSection == section
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectSection(section) }
                        .testTag("quick_nav_${section.number}"),
                    color = if (selected) VioletPrimary else GraphiteCard,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selected) ChampagneGold else GraphiteBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = iconForSection(section),
                            contentDescription = section.title,
                            tint = if (selected) ChampagneLight else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "${section.number}. ${section.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) TextPrimary else TextSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopEnterpriseHeaderBar(
    currentSection: AdminSection,
    session: AdminSession,
    showMenuButton: Boolean,
    onOpenDrawer: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(
        color = CharcoalSurface,
        border = BorderStroke(1.dp, GraphiteBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (showMenuButton) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("open_sidebar_drawer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Navigation Sidebar",
                            tint = ChampagneLight
                        )
                    }
                }
                Column {
                    Text(
                        text = "${currentSection.number.toString().padStart(2, '0')} • ${currentSection.title}",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "PromptAdmin Control Panel • ${session.email}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusPill(
                    label = session.role.displayName.uppercase(),
                    tone = when (session.role.name) {
                        "OWNER" -> "GOLD"
                        "ADMINISTRATOR" -> "VIOLET"
                        else -> "CYAN"
                    }
                )
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier.testTag("header_sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign Out",
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun EnterpriseSidebarContent(
    currentSection: AdminSection,
    session: AdminSession,
    onSelectSection: (AdminSection) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(CharcoalSurface)
            .border(1.dp, GraphiteBorder)
            .padding(vertical = 16.dp)
    ) {
        // Brand Identity Block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(VioletContainer)
                    .border(1.dp, ChampagneGold, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = "PromptAdmin Emblem",
                    tint = ChampagneLight
                )
            }
            Column {
                Text(
                    text = "PROMPTADMIN",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ENTERPRISE CMS",
                    style = MaterialTheme.typography.labelSmall,
                    color = ChampagneLight
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = GraphiteBorder)

        // All 14 Required Navigation Items
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AdminSection.entries.forEach { section ->
                val selected = currentSection == section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) VioletContainer else CharcoalSurface)
                        .border(
                            width = if (selected) 1.dp else 0.dp,
                            color = if (selected) VioletPrimary else CharcoalSurface,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectSection(section) }
                        .padding(horizontal = 12.dp, vertical = 11.dp)
                        .testTag("nav_item_${section.number}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = iconForSection(section),
                        contentDescription = section.title,
                        tint = if (selected) ChampagneLight else TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = "${section.number}. ${section.title}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) TextPrimary else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(ChampagneGold)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = GraphiteBorder)

        // Active Session Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = session.role.badgeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = ChampagneLight
                )
            }
            IconButton(onClick = onSignOut) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Sign Out",
                    tint = StatusRose
                )
            }
        }
    }
}

private fun iconForSection(section: AdminSection): ImageVector {
    return when (section) {
        AdminSection.DASHBOARD -> Icons.Default.Dashboard
        AdminSection.IMAGE_MANAGEMENT -> Icons.Default.Image
        AdminSection.CATEGORIES -> Icons.Default.Category
        AdminSection.USER_MANAGEMENT -> Icons.Default.People
        AdminSection.COMMENT_MODERATION -> Icons.Default.Comment
        AdminSection.LIKES_ENGAGEMENT -> Icons.Default.Favorite
        AdminSection.ANALYTICS -> Icons.Default.Analytics
        AdminSection.ADVERTISEMENT_MANAGEMENT -> Icons.Default.AdsClick
        AdminSection.ONESIGNAL_NOTIFICATIONS -> Icons.Default.NotificationsActive
        AdminSection.NOTIFICATION_HISTORY -> Icons.Default.History
        AdminSection.APP_CONTENT -> Icons.Default.AppSettingsAlt
        AdminSection.ADMINISTRATOR_MANAGEMENT -> Icons.Default.AdminPanelSettings
        AdminSection.SECURITY_ACTIVITY_LOGS -> Icons.Default.Security
        AdminSection.SETTINGS -> Icons.Default.Settings
    }
}
