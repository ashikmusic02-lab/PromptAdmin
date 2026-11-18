package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.FirebaseConnectionStatus
import com.example.model.ActivityLogEntity
import com.example.model.AdMobSettingsEntity
import com.example.model.AdminSection
import com.example.model.AnalyticsRange
import com.example.model.ImageSortOption
import com.example.model.PromptCategoryEntity
import com.example.model.PromptImageEntity
import com.example.ui.components.EmptyDataStateCard
import com.example.ui.components.EnterpriseStatCard
import com.example.ui.components.MonospaceBadge
import com.example.ui.components.SectionHeaderBar
import com.example.ui.components.StatusPill
import com.example.ui.components.UrlThumbnailBox
import com.example.ui.components.formatShortDate
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneLight
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCyan
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.theme.VioletContainer
import com.example.ui.theme.VioletPrimary
import com.example.viewmodel.LiveDashboardStats
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardOverviewScreen(
    stats: LiveDashboardStats,
    selectedRange: AnalyticsRange,
    onSelectRange: (AnalyticsRange) -> Unit,
    images: List<PromptImageEntity>,
    adMobSettings: AdMobSettingsEntity,
    recentLogs: List<ActivityLogEntity>,
    connectionStatus: FirebaseConnectionStatus,
    onNavigateTo: (AdminSection) -> Unit,
    onSyncFirestore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SectionHeaderBar(
            sectionNumber = 1,
            title = "Executive Dashboard Overview",
            subtitle = "Real-time telemetry across shared Firestore collections. Zero fabricated metrics — all statistics derive strictly from actual stored records.",
            trailingContent = {
                Button(
                    onClick = onSyncFirestore,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VioletPrimary,
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier.testTag("dashboard_sync_firestore_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync Firestore",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync Backend")
                }
            }
        )

        // Date Range Filter Bar (Daily, Weekly, Monthly, All Time)
        Card(
            colors = CardDefaults.cardColors(containerColor = GraphiteCard),
            border = BorderStroke(1.dp, GraphiteBorder),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ANALYTICS WINDOW:",
                    style = MaterialTheme.typography.labelMedium,
                    color = ChampagneLight
                )
                AnalyticsRange.entries.forEach { range ->
                    val selected = selectedRange == range
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectRange(range) },
                        label = { Text(range.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VioletContainer,
                            selectedLabelColor = VioletBright
                        ),
                        modifier = Modifier.testTag("range_chip_${range.name.lowercase()}")
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusPill(
                    label = if (connectionStatus.isGoogleServicesPresent) "FIRESTORE CONNECTED" else "AWAITING google-services.json",
                    tone = if (connectionStatus.isGoogleServicesPresent) "SUCCESS" else "GOLD"
                )
            }
        }

        // Primary 10-Metric Live Grid (Responsive 2-Column Layout for crisp phone & tablet display)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EnterpriseStatCard(
                    title = "Total Images",
                    value = stats.totalImages.toString(),
                    subtitle = "${stats.featuredImages} Featured • ${stats.trendingImages} Trending",
                    icon = Icons.Default.Image,
                    accentColor = VioletPrimary,
                    modifier = Modifier.weight(1f)
                )
                EnterpriseStatCard(
                    title = "Published Images",
                    value = stats.publishedImages.toString(),
                    subtitle = "Live in User App",
                    icon = Icons.Default.Public,
                    accentColor = StatusEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EnterpriseStatCard(
                    title = "Draft Images",
                    value = stats.draftImages.toString(),
                    subtitle = "Staged in Admin",
                    icon = Icons.Default.Drafts,
                    accentColor = StatusAmber,
                    modifier = Modifier.weight(1f)
                )
                EnterpriseStatCard(
                    title = "Total Users",
                    value = stats.totalUsers.toString(),
                    subtitle = "${stats.activeUsers} Active • ${stats.disabledUsers} Disabled",
                    icon = Icons.Default.People,
                    accentColor = StatusCyan,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EnterpriseStatCard(
                    title = "Active Users",
                    value = stats.activeUsers.toString(),
                    subtitle = "Authorized Accounts",
                    icon = Icons.Default.VerifiedUser,
                    accentColor = StatusEmerald,
                    modifier = Modifier.weight(1f)
                )
                EnterpriseStatCard(
                    title = "Total Image Views",
                    value = stats.totalViews.toString(),
                    subtitle = "${stats.uniqueViews} Unique Views",
                    icon = Icons.Default.Visibility,
                    accentColor = ChampagneGold,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EnterpriseStatCard(
                    title = "Total Likes",
                    value = stats.totalLikes.toString(),
                    subtitle = "User Heart Reactions",
                    icon = Icons.Default.Favorite,
                    accentColor = StatusRose,
                    modifier = Modifier.weight(1f)
                )
                EnterpriseStatCard(
                    title = "Total Comments",
                    value = stats.totalComments.toString(),
                    subtitle = "${stats.pendingOrReportedComments} Pending / Reported",
                    icon = Icons.Default.Comment,
                    accentColor = VioletBright,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EnterpriseStatCard(
                    title = "Prompt Unlocks",
                    value = stats.totalPromptUnlocks.toString(),
                    subtitle = "Successful Unlocks",
                    icon = Icons.Default.LockOpen,
                    accentColor = ChampagneGold,
                    modifier = Modifier.weight(1f)
                )
                EnterpriseStatCard(
                    title = "Total Shares",
                    value = stats.totalShares.toString(),
                    subtitle = "Deep Link Shares",
                    icon = Icons.Default.Share,
                    accentColor = StatusCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Advertisement Performance & Quick Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                border = BorderStroke(1.dp, GraphiteBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdsClick,
                                contentDescription = "AdMob Status",
                                tint = ChampagneGold
                            )
                            Text(
                                text = "Google AdMob Configuration Status",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                        }
                        StatusPill(
                            label = if (adMobSettings.globalAdsEnabled) "ADS ENABLED" else "ADS DISABLED",
                            tone = if (adMobSettings.globalAdsEnabled) "SUCCESS" else "DANGER"
                        )
                    }
                    Text(
                        text = "Mode: ${if (adMobSettings.testModeEnabled) "Test Advertisement Mode" else "Production Serving"} • Strategy: ${adMobSettings.placementStrategy} • Reward Policy: ${adMobSettings.rewardPolicy}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusPill(
                            label = "Home Banner: ${if (adMobSettings.homeBannerEnabled) "ON" else "OFF"}",
                            tone = if (adMobSettings.homeBannerEnabled) "SUCCESS" else "NEUTRAL"
                        )
                        StatusPill(
                            label = "Detail Banner: ${if (adMobSettings.detailBannerEnabled) "ON" else "OFF"}",
                            tone = if (adMobSettings.detailBannerEnabled) "SUCCESS" else "NEUTRAL"
                        )
                        StatusPill(
                            label = "Rewarded: ${if (adMobSettings.rewardedAdsEnabled) "ON" else "OFF"}",
                            tone = if (adMobSettings.rewardedAdsEnabled) "GOLD" else "NEUTRAL"
                        )
                        StatusPill(
                            label = "Interstitial: ${if (adMobSettings.interstitialAdsEnabled) "Every ${adMobSettings.interstitialFrequencyActions} actions" else "OFF"}",
                            tone = if (adMobSettings.interstitialAdsEnabled) "VIOLET" else "NEUTRAL"
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onNavigateTo(AdminSection.IMAGE_MANAGEMENT) },
                            border = BorderStroke(1.dp, VioletPrimary)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Manage URL Images")
                        }
                        OutlinedButton(
                            onClick = { onNavigateTo(AdminSection.ADVERTISEMENT_MANAGEMENT) },
                            border = BorderStroke(1.dp, ChampagneGold)
                        ) {
                            Icon(Icons.Default.AdsClick, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Configure AdMob")
                        }
                        OutlinedButton(
                            onClick = { onNavigateTo(AdminSection.ONESIGNAL_NOTIFICATIONS) },
                            border = BorderStroke(1.dp, StatusCyan)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send Push")
                        }
                    }
                }
            }
        }

        // Popular Content Rankings & Recent Administrative Activities
        Card(
            colors = CardDefaults.cardColors(containerColor = GraphiteCard),
            border = BorderStroke(1.dp, GraphiteBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Popular Content Rankings (By Engagement Score)",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "View All in Analytics ->",
                        style = MaterialTheme.typography.labelLarge,
                        color = ChampagneLight,
                        modifier = Modifier.clickable { onNavigateTo(AdminSection.ANALYTICS) }
                    )
                }

                if (images.isEmpty()) {
                    EmptyDataStateCard(
                        title = "No Prompt Images Registered Yet",
                        message = "Add your first AI Photo Inspiration record via direct external URL (.jpg, .png, .webp) or synchronize with your shared Firestore backend.",
                        icon = Icons.Default.Image,
                        actionLabel = "Open Image Management",
                        onActionClick = { onNavigateTo(AdminSection.IMAGE_MANAGEMENT) }
                    )
                } else {
                    val topRanked = remember(images) {
                        images.sortedByDescending {
                            it.viewsCount + (it.likesCount * 3) + (it.promptUnlocksCount * 5)
                        }.take(5)
                    }
                    topRanked.forEachIndexed { idx, img ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GraphiteElevated)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MonospaceBadge("#${idx + 1}")
                            UrlThumbnailBox(imageUrl = img.imageUrl, size = 48.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = img.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${img.categoryName} • ${img.deepLink}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${img.viewsCount} views • ${img.likesCount} likes",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ChampagneLight
                                )
                                Text(
                                    text = "${img.promptUnlocksCount} unlocks • ${img.sharesCount} shares",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Administrative Activities
        Card(
            colors = CardDefaults.cardColors(containerColor = GraphiteCard),
            border = BorderStroke(1.dp, GraphiteBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = VioletBright)
                        Text(
                            text = "Recent Administrative Activities",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Full Audit Trail ->",
                        style = MaterialTheme.typography.labelLarge,
                        color = VioletBright,
                        modifier = Modifier.clickable { onNavigateTo(AdminSection.SECURITY_ACTIVITY_LOGS) }
                    )
                }

                if (recentLogs.isEmpty()) {
                    Text(
                        text = "No administrative actions logged in this session yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                } else {
                    recentLogs.take(6).forEach { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianBg)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatusPill(label = log.actionType, tone = "VIOLET")
                                    MonospaceBadge(log.targetEntity)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = log.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = log.adminEmail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ChampagneLight
                                )
                                Text(
                                    text = formatTimestamp(log.createdAtMillis),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// MODULE 6: LIKES & ENGAGEMENT
// ============================================================================
@Composable
fun LikesAndEngagementScreen(
    images: List<PromptImageEntity>,
    canEditTelemetry: Boolean,
    onUpdateTelemetry: (
        image: PromptImageEntity,
        views: Long,
        uniqueViews: Long,
        likes: Long,
        unlocks: Long,
        shares: Long
    ) -> Unit
) {
    var editingImage by remember { mutableStateOf<PromptImageEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SectionHeaderBar(
            sectionNumber = 6,
            title = "Likes & Image Performance Engagement",
            subtitle = "Per-image telemetry breakdown: Total views, unique authenticated views, likes, comments, successful prompt unlocks, share events, and popularity ranking."
        )

        if (images.isEmpty()) {
            EmptyDataStateCard(
                title = "No Images Available for Engagement Tracking",
                message = "Create or synchronize images in the Image Management module to inspect per-image likes, prompt unlocks, and view metrics.",
                icon = Icons.Default.Favorite
            )
        } else {
            val ranked = remember(images) {
                images.sortedByDescending {
                    it.viewsCount + (it.likesCount * 3) + (it.promptUnlocksCount * 5) + (it.sharesCount * 4)
                }
            }

            ranked.forEachIndexed { index, img ->
                val popularityScore = img.viewsCount + (img.likesCount * 3) + (img.promptUnlocksCount * 5) + (img.sharesCount * 4)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                    border = BorderStroke(1.dp, GraphiteBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatusPill(label = "RANK #${index + 1}", tone = "GOLD")
                            UrlThumbnailBox(imageUrl = img.imageUrl, size = 56.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = img.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${img.categoryName} • Short Name: ${img.shortName} • Score: $popularityScore",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                MonospaceBadge(img.deepLink)
                            }
                            if (canEditTelemetry) {
                                IconButton(
                                    onClick = { editingImage = img },
                                    modifier = Modifier.testTag("sync_telemetry_${img.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Audit Telemetry Counters",
                                        tint = ChampagneLight
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = GraphiteBorder)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            TelemetryMetricColumn("TOTAL VIEWS", img.viewsCount.toString(), ChampagneLight)
                            TelemetryMetricColumn("UNIQUE VIEWS", img.uniqueViewsCount.toString(), StatusCyan)
                            TelemetryMetricColumn("TOTAL LIKES", img.likesCount.toString(), StatusRose)
                            TelemetryMetricColumn("COMMENTS", img.commentsCount.toString(), VioletBright)
                            TelemetryMetricColumn("PROMPT UNLOCKS", img.promptUnlocksCount.toString(), StatusEmerald)
                            TelemetryMetricColumn("SHARES", img.sharesCount.toString(), ChampagneGold)
                        }
                    }
                }
            }
        }
    }

    if (editingImage != null) {
        val target = editingImage!!
        var viewsText by remember(target) { mutableStateOf(target.viewsCount.toString()) }
        var uniqueViewsText by remember(target) { mutableStateOf(target.uniqueViewsCount.toString()) }
        var likesText by remember(target) { mutableStateOf(target.likesCount.toString()) }
        var unlocksText by remember(target) { mutableStateOf(target.promptUnlocksCount.toString()) }
        var sharesText by remember(target) { mutableStateOf(target.sharesCount.toString()) }

        AlertDialog(
            onDismissRequest = { editingImage = null },
            containerColor = GraphiteElevated,
            title = {
                Text("Audit / Sync Engagement Counters", color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Image: ${target.title} (${target.id})",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChampagneLight
                    )
                    OutlinedTextField(
                        value = viewsText,
                        onValueChange = { viewsText = it.filter(Char::isDigit) },
                        label = { Text("Total Views") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uniqueViewsText,
                        onValueChange = { uniqueViewsText = it.filter(Char::isDigit) },
                        label = { Text("Unique Authenticated Views") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = likesText,
                        onValueChange = { likesText = it.filter(Char::isDigit) },
                        label = { Text("Total Likes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = unlocksText,
                        onValueChange = { unlocksText = it.filter(Char::isDigit) },
                        label = { Text("Successful Prompt Unlocks") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sharesText,
                        onValueChange = { sharesText = it.filter(Char::isDigit) },
                        label = { Text("Share Events") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateTelemetry(
                            target,
                            viewsText.toLongOrNull() ?: 0L,
                            uniqueViewsText.toLongOrNull() ?: 0L,
                            likesText.toLongOrNull() ?: 0L,
                            unlocksText.toLongOrNull() ?: 0L,
                            sharesText.toLongOrNull() ?: 0L
                        )
                        editingImage = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
                ) {
                    Text("Save Verified Counters")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingImage = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TelemetryMetricColumn(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianBg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

// ============================================================================
// MODULE 7: ANALYTICS & COMPARISON REPORTS
// ============================================================================
@Composable
fun AnalyticsComparisonScreen(
    images: List<PromptImageEntity>,
    categories: List<PromptCategoryEntity>,
    selectedSort: ImageSortOption,
    onSelectSort: (ImageSortOption) -> Unit,
    selectedRange: AnalyticsRange,
    onSelectRange: (AnalyticsRange) -> Unit
) {
    val cutoffMillis = remember(selectedRange) {
        if (selectedRange == AnalyticsRange.ALL_TIME) 0L
        else System.currentTimeMillis() - selectedRange.days * 24L * 3600L * 1000L
    }

    val filteredImages = remember(images, cutoffMillis, selectedSort) {
        val windowFiltered = images.filter { it.createdAtMillis >= cutoffMillis }
        when (selectedSort) {
            ImageSortOption.MOST_VIEWED -> windowFiltered.sortedByDescending { it.viewsCount }
            ImageSortOption.MOST_LIKED -> windowFiltered.sortedByDescending { it.likesCount }
            ImageSortOption.MOST_COMMENTED -> windowFiltered.sortedByDescending { it.commentsCount }
            ImageSortOption.MOST_UNLOCKED -> windowFiltered.sortedByDescending { it.promptUnlocksCount }
            ImageSortOption.RECENTLY_PUBLISHED -> windowFiltered.sortedByDescending { it.createdAtMillis }
            ImageSortOption.LEAST_PERFORMING -> windowFiltered.sortedBy {
                it.viewsCount + it.likesCount + it.promptUnlocksCount
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SectionHeaderBar(
            sectionNumber = 7,
            title = "Performance Analytics & Comparison Reports",
            subtitle = "Multi-dimensional sorting, visual comparison charts, and category conversion ratios powered strictly by real database records."
        )

        // Date Range + Sort Controls
        Card(
            colors = CardDefaults.cardColors(containerColor = GraphiteCard),
            border = BorderStroke(1.dp, GraphiteBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "DATE WINDOW FILTER",
                    style = MaterialTheme.typography.labelMedium,
                    color = ChampagneLight
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsRange.entries.forEach { range ->
                        FilterChip(
                            selected = selectedRange == range,
                            onClick = { onSelectRange(range) },
                            label = { Text(range.label) }
                        )
                    }
                }

                Text(
                    text = "SORT PERFORMANCE RANKING BY",
                    style = MaterialTheme.typography.labelMedium,
                    color = VioletBright
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ImageSortOption.entries.forEach { option ->
                        FilterChip(
                            selected = selectedSort == option,
                            onClick = { onSelectSort(option) },
                            label = { Text(option.label) },
                            modifier = Modifier.testTag("sort_chip_${option.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Visual Comparison Bar Chart Card
        Card(
            colors = CardDefaults.cardColors(containerColor = GraphiteCard),
            border = BorderStroke(1.dp, GraphiteBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = ChampagneGold)
                    Text(
                        text = "Visual Comparison Report (${selectedSort.label})",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                }

                if (filteredImages.isEmpty()) {
                    EmptyDataStateCard(
                        title = "No Records in Selected Window",
                        message = "No prompt images match the selected date range (${selectedRange.label}).",
                        icon = Icons.Default.Analytics
                    )
                } else {
                    val maxMetric = remember(filteredImages, selectedSort) {
                        filteredImages.maxOfOrNull { img ->
                            when (selectedSort) {
                                ImageSortOption.MOST_VIEWED -> img.viewsCount
                                ImageSortOption.MOST_LIKED -> img.likesCount
                                ImageSortOption.MOST_COMMENTED -> img.commentsCount
                                ImageSortOption.MOST_UNLOCKED -> img.promptUnlocksCount
                                ImageSortOption.RECENTLY_PUBLISHED -> img.viewsCount.coerceAtLeast(1L)
                                ImageSortOption.LEAST_PERFORMING -> (img.viewsCount + img.likesCount + img.promptUnlocksCount).coerceAtLeast(1L)
                            }
                        }?.coerceAtLeast(1L) ?: 1L
                    }

                    filteredImages.take(10).forEachIndexed { idx, img ->
                        val metricVal = when (selectedSort) {
                            ImageSortOption.MOST_VIEWED -> img.viewsCount
                            ImageSortOption.MOST_LIKED -> img.likesCount
                            ImageSortOption.MOST_COMMENTED -> img.commentsCount
                            ImageSortOption.MOST_UNLOCKED -> img.promptUnlocksCount
                            ImageSortOption.RECENTLY_PUBLISHED -> img.viewsCount
                            ImageSortOption.LEAST_PERFORMING -> img.viewsCount + img.likesCount + img.promptUnlocksCount
                        }
                        val fraction = (metricVal.toFloat() / maxMetric.toFloat()).coerceIn(0.04f, 1f)
                        val unlockRate = if (img.viewsCount > 0) {
                            String.format(Locale.US, "%.1f%%", (img.promptUnlocksCount.toDouble() / img.viewsCount.toDouble()) * 100.0)
                        } else "0.0%"

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObsidianBg)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    MonospaceBadge("#${idx + 1}")
                                    Text(
                                        text = img.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    StatusPill(label = img.categoryName.ifBlank { "Uncategorized" }, tone = "VIOLET")
                                }
                                Text(
                                    text = "$metricVal • Unlock Conversion: $unlockRate • Published ${formatShortDate(img.createdAtMillis)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ChampagneLight
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(GraphiteElevated)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(VioletPrimary, ChampagneGold)
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Category Distribution Report
        Card(
            colors = CardDefaults.cardColors(containerColor = GraphiteCard),
            border = BorderStroke(1.dp, GraphiteBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Category Volume & Unlock Efficiency Report",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                if (categories.isEmpty()) {
                    Text(
                        text = "No categories created yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                } else {
                    categories.forEach { cat ->
                        val catImages = images.filter { it.categoryId == cat.id }
                        val catViews = catImages.sumOf { it.viewsCount }
                        val catUnlocks = catImages.sumOf { it.promptUnlocksCount }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianBg)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${catImages.size} images • $catViews total views • $catUnlocks prompt unlocks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            StatusPill(
                                label = if (cat.active) "ACTIVE" else "INACTIVE",
                                tone = if (cat.active) "SUCCESS" else "WARNING"
                            )
                        }
                    }
                }
            }
        }
    }
}
