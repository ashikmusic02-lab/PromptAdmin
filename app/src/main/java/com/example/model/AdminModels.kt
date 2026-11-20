package com.example.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp

enum class AdminSection(val titleBangla: String) {
    DASHBOARD("ড্যাশবোর্ড"),
    IMAGES("ছবি"),
    CATEGORIES("ক্যাটাগরি"),
    COMMENTS_REPORTS("কমেন্ট ও রিপোর্ট"),
    NOTIFICATION_HELPER("নোটিফিকেশন"),
    ADS("বিজ্ঞাপন"),
    SETTINGS("সেটিংস")
}

enum class AdminRole(
    val firestoreRole: String,
    val banglaBadge: String
) {
    OWNER(
        firestoreRole = "owner",
        banglaBadge = "মালিক"
    ),
    ADMIN(
        firestoreRole = "admin",
        banglaBadge = "অ্যাডমিন"
    ),
    MODERATOR(
        firestoreRole = "moderator",
        banglaBadge = "মডারেটর"
    );

    fun canAccessSection(section: AdminSection): Boolean {
        return when (this) {
            OWNER, ADMIN -> true
            MODERATOR -> section == AdminSection.COMMENTS_REPORTS
        }
    }

    val canManageContent: Boolean
        get() = this == OWNER || this == ADMIN

    companion object {
        fun fromFirestoreValue(raw: String?): AdminRole? {
            return when (raw?.trim()?.lowercase()) {
                "owner" -> OWNER
                "admin", "administrator" -> ADMIN
                "moderator" -> MODERATOR
                else -> null
            }
        }
    }
}

sealed class AuthUiState {
    data object Loading : AuthUiState()
    data object Unauthenticated : AuthUiState()
    data class NotAdmin(
        val uid: String,
        val email: String,
        val message: String = "আপনি অ্যাডমিন নন"
    ) : AuthUiState()
    data class Authorized(
        val uid: String,
        val email: String,
        val displayName: String,
        val role: AdminRole
    ) : AuthUiState()
}

data class DashboardCounts(
    val totalImages: Int = 0,
    val publishedImages: Int = 0,
    val unlistedImages: Int = 0,
    val totalCategories: Int = 0,
    val totalComments: Int = 0,
    val openReports: Int = 0
)

@Keep
data class FirestoreImageItem(
    val id: String = "",
    val title: String = "",
    val shortName: String = "",
    val description: String = "",
    val categoryId: String = "uncategorized",
    val categoryName: String = "অন্যান্য",
    val imageUrl: String = "",
    val aspectRatio: Double = 1.0,
    val featured: Boolean = false,
    val trending: Boolean = false,
    val recommended: Boolean = true,
    val collectionName: String = "",
    val keywords: String = "",
    val displayOrder: Int = 1,
    val status: String = "published", // "published" or "draft"
    val viewCount: Long = 0L,
    val likeCount: Long = 0L,
    val commentCount: Long = 0L,
    val unlockCount: Long = 0L,
    val shareCount: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val isServerConfirmed: Boolean = true
) {
    val isPublished: Boolean
        get() = status.equals("published", ignoreCase = true)
}

@Keep
data class FirestoreCategoryItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val coverImageUrl: String = "",
    val imageCount: Int = 0,
    val sortOrder: Int = 1,
    val status: String = "published", // "published" or "draft"
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val isServerConfirmed: Boolean = true
) {
    val isActive: Boolean
        get() = status.equals("published", ignoreCase = true)
}

@Keep
data class FirestoreCommentItem(
    val id: String = "",
    val imageId: String = "",
    val imageTitle: String = "",
    val userId: String = "",
    val userName: String = "",
    val text: String = "",
    val status: String = "visible", // "visible" or "hidden"
    val adminReply: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val isServerConfirmed: Boolean = true
) {
    val isVisible: Boolean
        get() = !status.equals("hidden", ignoreCase = true)
}

@Keep
data class FirestoreReportItem(
    val id: String = "",
    val targetId: String = "",
    val targetType: String = "",
    val imageId: String = "",
    val commentId: String = "",
    val reason: String = "",
    val details: String = "",
    val reporterId: String = "",
    val status: String = "open", // "open" | "reviewed" | "dismissed"
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val isServerConfirmed: Boolean = true
)

@Keep
data class FirestoreNotificationItem(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val imageId: String = "",
    val imageUrl: String = "",
    val categoryName: String = "অন্যান্য",
    val status: String = "published", // "published" or "draft"
    val createdAt: Timestamp? = null,
    val isServerConfirmed: Boolean = true
) {
    val isPublished: Boolean
        get() = status.equals("published", ignoreCase = true)
}

@Keep
data class AdvertisementSettingsModel(
    val allAdsEnabled: Boolean = true,
    val homeBannerEnabled: Boolean = true,
    val detailBannerEnabled: Boolean = true,
    val rewardedAdsEnabled: Boolean = true,
    val interstitialAdsEnabled: Boolean = true,
    val testAdMode: Boolean = true,
    val reusePreviousUnlocks: Boolean = true,
    val interstitialFrequency: Int = 4, // 1..50
    val adFallbackMessage: String = "",
    val bannerAdUnitId: String = "",
    val rewardedAdUnitId: String = "",
    val interstitialAdUnitId: String = ""
)

@Keep
data class AppGlobalSettingsModel(
    val playStoreUrl: String = "",
    val requireLoginForPrompts: Boolean = false,
    val allowComments: Boolean = true,
    val welcomeHeadline: String = "",
    val welcomeSubheadline: String = "",
    val supportEmail: String = ""
)

// Retained Room entities for existing AdminDao / AdminDatabase compilation
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
    val status: String = "published",
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
    val accountStatus: String = "ACTIVE",
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
    val moderationStatus: String = "PENDING",
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
    val placementStrategy: String = "BALANCED_UX",
    val rewardPolicy: String = "SINGLE_AD_PER_PROMPT_UNLOCK",
    val fallbackBehavior: String = "GRANT_UNLOCK_ON_NO_FILL",
    val adImpressionsTracked: Long = 0L,
    val rewardedCompletionsTracked: Long = 0L,
    val updatedByEmail: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
@Entity(tableName = "app_settings")
data class AppContentSettingsEntity(
    @PrimaryKey val id: String = "global_app_content",
    val appName: String = "PromptAdmin",
    val appLogoUrl: String = "",
    val deepLinkScheme: String = "promptlens",
    val verifiedDomainHost: String = "promptlens.app",
    val featuredBannerSubtitle: String = "",
    val showFeaturedSection: Boolean = true,
    val showTrendingSection: Boolean = true,
    val showLatestSection: Boolean = true,
    val showPopularCategoriesSection: Boolean = true,
    val maxFeaturedItems: Int = 10,
    val maxTrendingItems: Int = 16,
    val aboutInformation: String = "",
    val privacyPolicyUrl: String = "",
    val termsAndConditionsUrl: String = "",
    val contactEmail: String = "",
    val playStoreUrl: String = "",
    val oneSignalAppId: String = "",
    val oneSignalFunctionUrl: String = "",
    val adminAuthFunctionUrl: String = "",
    val requireCommentPreModeration: Boolean = true,
    val updatedByEmail: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Keep
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val message: String = "",
    val targetAudience: String = "ALL_SUBSCRIBED",
    val imageId: String = "",
    val imageTitle: String = "",
    val imageThumbnailUrl: String = "",
    val deepLink: String = "",
    val scheduleType: String = "SEND_NOW",
    val scheduledTimeDisplay: String = "",
    val sendingStatus: String = "",
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
    val role: String = "owner",
    val active: Boolean = true,
    val permissionsSummary: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastLoginAtMillis: Long = System.currentTimeMillis()
)

@Keep
@Entity(tableName = "admin_activity_logs")
data class ActivityLogEntity(
    @PrimaryKey val id: String = "",
    val actorUid: String = "",
    val adminEmail: String = "",
    val adminRole: String = "",
    val actionType: String = "",
    val targetEntity: String = "",
    val summary: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)
