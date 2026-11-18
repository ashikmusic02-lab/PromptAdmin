package com.example.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AdminSection(val number: Int, val title: String, val group: String) {
    DASHBOARD(1, "Dashboard", "Overview"),
    IMAGE_MANAGEMENT(2, "Image Management", "Content Library"),
    CATEGORIES(3, "Categories", "Content Library"),
    USER_MANAGEMENT(4, "User Management", "Community & Trust"),
    COMMENT_MODERATION(5, "Comment Moderation", "Community & Trust"),
    LIKES_ENGAGEMENT(6, "Likes & Engagement", "Intelligence"),
    ANALYTICS(7, "Analytics", "Intelligence"),
    ADVERTISEMENT_MANAGEMENT(8, "Advertisement Management", "Monetization & Growth"),
    ONESIGNAL_NOTIFICATIONS(9, "OneSignal Notifications", "Monetization & Growth"),
    NOTIFICATION_HISTORY(10, "Notification History", "Monetization & Growth"),
    APP_CONTENT(11, "App Content", "Platform Configuration"),
    ADMINISTRATOR_MANAGEMENT(12, "Administrator Management", "Governance & Security"),
    SECURITY_ACTIVITY_LOGS(13, "Security & Activity Logs", "Governance & Security"),
    SETTINGS(14, "Settings", "Governance & Security")
}

enum class AdminRole(
    val displayName: String,
    val badgeLabel: String,
    val canManageImages: Boolean,
    val canManageCategories: Boolean,
    val canManageUsers: Boolean,
    val canModerateComments: Boolean,
    val canManageAdMob: Boolean,
    val canSendNotifications: Boolean,
    val canManageAppContent: Boolean,
    val canManageAdmins: Boolean
) {
    OWNER(
        displayName = "Owner",
        badgeLabel = "OWNER • FULL AUTHORITY",
        canManageImages = true,
        canManageCategories = true,
        canManageUsers = true,
        canModerateComments = true,
        canManageAdMob = true,
        canSendNotifications = true,
        canManageAppContent = true,
        canManageAdmins = true
    ),
    ADMINISTRATOR(
        displayName = "Administrator",
        badgeLabel = "ADMIN • CONTENT & OPS",
        canManageImages = true,
        canManageCategories = true,
        canManageUsers = true,
        canModerateComments = true,
        canManageAdMob = true,
        canSendNotifications = true,
        canManageAppContent = true,
        canManageAdmins = false
    ),
    MODERATOR(
        displayName = "Moderator",
        badgeLabel = "MOD • TRUST & SAFETY",
        canManageImages = false,
        canManageCategories = false,
        canManageUsers = false,
        canModerateComments = true,
        canManageAdMob = false,
        canSendNotifications = false,
        canManageAppContent = false,
        canManageAdmins = false
    )
}

enum class AnalyticsRange(val label: String, val days: Int) {
    DAILY("Daily (24h)", 1),
    WEEKLY("Weekly (7d)", 7),
    MONTHLY("Monthly (30d)", 30),
    ALL_TIME("All Time", 3650)
}

enum class ImageSortOption(val label: String) {
    MOST_VIEWED("Most Viewed"),
    MOST_LIKED("Most Liked"),
    MOST_COMMENTED("Most Commented"),
    MOST_UNLOCKED("Most Copied / Unlocked"),
    RECENTLY_PUBLISHED("Recently Published"),
    LEAST_PERFORMING("Least Performing")
}

@Keep
@Entity(tableName = "images")
data class PromptImageEntity(
    @PrimaryKey val id: String = "",
    val imageUrl: String = "",
    val shortName: String = "",
    val title: String = "",
    val description: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val prompt: String = "",
    val featured: Boolean = false,
    val trending: Boolean = false,
    val status: String = "PUBLISHED", // PUBLISHED or DRAFT
    val displayOrder: Int = 1,
    val deepLink: String = "",
    val verifiedAppLink: String = "",
    val viewsCount: Long = 0L,
    val uniqueViewsCount: Long = 0L,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val promptUnlocksCount: Long = 0L,
    val sharesCount: Long = 0L,
    val createdByUid: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
@Entity(tableName = "categories")
data class PromptCategoryEntity(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val displayOrder: Int = 1,
    val active: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
@Entity(tableName = "users")
data class AppUserEntity(
    @PrimaryKey val id: String = "",
    val username: String = "",
    val email: String = "",
    val accountStatus: String = "ACTIVE", // ACTIVE, DISABLED, FLAGGED
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val unlocksCount: Long = 0L,
    val reportedCount: Int = 0,
    val moderationNote: String = "",
    val registeredAtMillis: Long = System.currentTimeMillis(),
    val lastActivityAtMillis: Long = System.currentTimeMillis()
)

@Keep
@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String = "",
    val imageId: String = "",
    val imageTitle: String = "",
    val userId: String = "",
    val username: String = "",
    val content: String = "",
    val moderationStatus: String = "APPROVED", // APPROVED, PENDING, HIDDEN, DELETED
    val reported: Boolean = false,
    val reportReason: String = "",
    val adminReply: String = "",
    val moderatedByEmail: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
@Entity(tableName = "advertisement_settings")
data class AdMobSettingsEntity(
    @PrimaryKey val id: String = "global_admob_config",
    val admobAppId: String = "",
    val bannerAdUnitId: String = "",
    val rewardedAdUnitId: String = "",
    val interstitialAdUnitId: String = "",
    val testModeEnabled: Boolean = true,
    val globalAdsEnabled: Boolean = true,
    val homeBannerEnabled: Boolean = true,
    val detailBannerEnabled: Boolean = true,
    val rewardedAdsEnabled: Boolean = true,
    val interstitialAdsEnabled: Boolean = true,
    val interstitialFrequencyActions: Int = 4,
    val minInterstitialIntervalSeconds: Int = 90,
    val placementStrategy: String = "BALANCED_UX", // CONSERVATIVE, BALANCED_UX, HIGH_VISIBILITY
    val rewardPolicy: String = "SINGLE_AD_PER_PROMPT_UNLOCK", // SINGLE_AD_PER_PROMPT_UNLOCK, SESSION_PASS_15M
    val fallbackBehavior: String = "GRANT_UNLOCK_ON_NO_FILL", // GRANT_UNLOCK_ON_NO_FILL, RETRY_THEN_INTERSTITIAL, STRICT_REQUIRE_AD
    val adImpressionsTracked: Long = 0L,
    val rewardedCompletionsTracked: Long = 0L,
    val updatedByEmail: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
@Entity(tableName = "app_settings")
data class AppContentSettingsEntity(
    @PrimaryKey val id: String = "global_app_content",
    val appName: String = "PromptVault AI",
    val appLogoUrl: String = "",
    val deepLinkScheme: String = "promptvault",
    val verifiedDomainHost: String = "promptvault.app",
    val featuredBannerSubtitle: String = "Curated Masterpiece Prompts & Visual Inspiration",
    val showFeaturedSection: Boolean = true,
    val showTrendingSection: Boolean = true,
    val showLatestSection: Boolean = true,
    val showPopularCategoriesSection: Boolean = true,
    val maxFeaturedItems: Int = 10,
    val maxTrendingItems: Int = 16,
    val aboutInformation: String = "Premium curated AI photo inspiration and prompt engineering reference library for mobile creators.",
    val privacyPolicyUrl: String = "https://promptvault.app/privacy",
    val termsAndConditionsUrl: String = "https://promptvault.app/terms",
    val contactEmail: String = "support@promptvault.app",
    val playStoreUrl: String = "https://play.google.com/store/apps/details?id=com.aistudio.promptvault.user",
    val oneSignalAppId: String = "",
    val oneSignalFunctionUrl: String = "",
    val adminAuthFunctionUrl: String = "",
    val requireCommentPreModeration: Boolean = false,
    val updatedByEmail: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val message: String = "",
    val targetAudience: String = "ALL_SUBSCRIBED", // ALL_SUBSCRIBED, ACTIVE_USERS, HIGH_ENGAGEMENT, NEW_USERS
    val imageId: String = "",
    val imageTitle: String = "",
    val imageThumbnailUrl: String = "",
    val deepLink: String = "",
    val scheduleType: String = "SEND_NOW", // SEND_NOW, SCHEDULE_LATER
    val scheduledTimeDisplay: String = "Immediate Dispatch",
    val sendingStatus: String = "QUEUED_FOR_SERVER", // QUEUED_FOR_SERVER, SCHEDULED, DISPATCHED_PROVIDER_CONFIRMED, FAILED
    val deliveryDetails: String = "",
    val createdByEmail: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Keep
@Entity(tableName = "admin_users")
data class AdminAccountEntity(
    @PrimaryKey val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = AdminRole.OWNER.name, // OWNER, ADMINISTRATOR, MODERATOR
    val active: Boolean = true,
    val permissionsSummary: String = "Full System Access",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastLoginAtMillis: Long = System.currentTimeMillis()
)

@Keep
@Entity(tableName = "admin_activity_logs")
data class ActivityLogEntity(
    @PrimaryKey val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val adminRole: String = "",
    val actionType: String = "",
    val targetEntity: String = "",
    val summary: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)
