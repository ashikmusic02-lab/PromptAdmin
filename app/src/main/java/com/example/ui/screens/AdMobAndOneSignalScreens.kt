package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.AdMobSettingsEntity
import com.example.model.AdminRole
import com.example.model.NotificationEntity
import com.example.model.PromptImageEntity
import com.example.ui.components.EmptyDataStateCard
import com.example.ui.components.MonospaceBadge
import com.example.ui.components.PermissionGuardBanner
import com.example.ui.components.SectionHeaderBar
import com.example.ui.components.StatusPill
import com.example.ui.components.UrlThumbnailBox
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.ChampagneContainer
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneLight
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.theme.VioletContainer
import com.example.ui.theme.VioletPrimary
import com.example.util.UrlAndDeepLinkValidator

@Composable
fun AdvertisementManagementScreen(
    adMobSettings: AdMobSettingsEntity,
    activeRole: AdminRole,
    onSaveAdMobSettings: (AdMobSettingsEntity) -> Unit
) {
    var admobAppId by remember(adMobSettings) { mutableStateOf(adMobSettings.admobAppId) }
    var bannerUnitId by remember(adMobSettings) { mutableStateOf(adMobSettings.bannerAdUnitId) }
    var rewardedUnitId by remember(adMobSettings) { mutableStateOf(adMobSettings.rewardedAdUnitId) }
    var interstitialUnitId by remember(adMobSettings) { mutableStateOf(adMobSettings.interstitialAdUnitId) }
    var testModeEnabled by remember(adMobSettings) { mutableStateOf(adMobSettings.testModeEnabled) }

    var globalAdsEnabled by remember(adMobSettings) { mutableStateOf(adMobSettings.globalAdsEnabled) }
    var homeBannerEnabled by remember(adMobSettings) { mutableStateOf(adMobSettings.homeBannerEnabled) }
    var detailBannerEnabled by remember(adMobSettings) { mutableStateOf(adMobSettings.detailBannerEnabled) }
    var rewardedAdsEnabled by remember(adMobSettings) { mutableStateOf(adMobSettings.rewardedAdsEnabled) }
    var interstitialAdsEnabled by remember(adMobSettings) { mutableStateOf(adMobSettings.interstitialAdsEnabled) }

    var freqText by remember(adMobSettings) { mutableStateOf(adMobSettings.interstitialFrequencyActions.toString()) }
    var minIntervalText by remember(adMobSettings) { mutableStateOf(adMobSettings.minInterstitialIntervalSeconds.toString()) }
    var placementStrategy by remember(adMobSettings) { mutableStateOf(adMobSettings.placementStrategy) }
    var rewardPolicy by remember(adMobSettings) { mutableStateOf(adMobSettings.rewardPolicy) }
    var fallbackBehavior by remember(adMobSettings) { mutableStateOf(adMobSettings.fallbackBehavior) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SectionHeaderBar(
            sectionNumber = 8,
            title = "Google AdMob Configuration & Availability",
            subtitle = "Control AdMob unit IDs, independent placement switches, interstitial frequency, reward policies, and fallback behavior synchronized via Firestore advertisementSettings.",
            trailingContent = {
                if (activeRole.canManageAdMob) {
                    Button(
                        onClick = {
                            onSaveAdMobSettings(
                                adMobSettings.copy(
                                    admobAppId = admobAppId.trim(),
                                    bannerAdUnitId = bannerUnitId.trim(),
                                    rewardedAdUnitId = rewardedUnitId.trim(),
                                    interstitialAdUnitId = interstitialUnitId.trim(),
                                    testModeEnabled = testModeEnabled,
                                    globalAdsEnabled = globalAdsEnabled,
                                    homeBannerEnabled = homeBannerEnabled,
                                    detailBannerEnabled = detailBannerEnabled,
                                    rewardedAdsEnabled = rewardedAdsEnabled,
                                    interstitialAdsEnabled = interstitialAdsEnabled,
                                    interstitialFrequencyActions = (freqText.toIntOrNull() ?: 4).coerceAtLeast(1),
                                    minInterstitialIntervalSeconds = (minIntervalText.toIntOrNull() ?: 90).coerceAtLeast(15),
                                    placementStrategy = placementStrategy,
                                    rewardPolicy = rewardPolicy,
                                    fallbackBehavior = fallbackBehavior
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                        modifier = Modifier.testTag("save_admob_settings_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save AdMob Config")
                    }
                }
            }
        )

        if (!activeRole.canManageAdMob) {
            PermissionGuardBanner(
                requiredRoleLabel = "Owner or Administrator",
                currentRoleLabel = activeRole.displayName
            )
        }

        // AdMob Governance Notice (No fabricated revenue)
        Surface(
            color = ChampagneContainer.copy(alpha = 0.35f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.AdsClick, contentDescription = null, tint = ChampagneLight)
                Text(
                    text = "Actual advertisements are served by the Google Mobile Ads SDK inside the separate User Application. This console governs unit IDs, placement availability, frequency caps, and fallback policies without fabricating revenue projections.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
        }

        // 1. AdMob Unit Identifiers & Test Mode
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
                        text = "AdMob Application & Unit Identifiers",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    OutlinedButton(
                        onClick = {
                            admobAppId = "ca-app-pub-3940256099942544~3347511713"
                            bannerUnitId = "ca-app-pub-3940256099942544/6300978111"
                            rewardedUnitId = "ca-app-pub-3940256099942544/5224354917"
                            interstitialUnitId = "ca-app-pub-3940256099942544/1033173712"
                            testModeEnabled = true
                        },
                        enabled = activeRole.canManageAdMob,
                        border = BorderStroke(1.dp, ChampagneGold)
                    ) {
                        Text("Load Official Google Test IDs", color = ChampagneLight)
                    }
                }

                OutlinedTextField(
                    value = admobAppId,
                    onValueChange = { admobAppId = it },
                    enabled = activeRole.canManageAdMob,
                    label = { Text("AdMob App ID (e.g. ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bannerUnitId,
                    onValueChange = { bannerUnitId = it },
                    enabled = activeRole.canManageAdMob,
                    label = { Text("Banner Ad Unit ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rewardedUnitId,
                    onValueChange = { rewardedUnitId = it },
                    enabled = activeRole.canManageAdMob,
                    label = { Text("Rewarded Ad Unit ID (Prompt Unlock Gate)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = interstitialUnitId,
                    onValueChange = { interstitialUnitId = it },
                    enabled = activeRole.canManageAdMob,
                    label = { Text("Interstitial Ad Unit ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Test Advertisement Mode", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Forces test ad requests in the user application to prevent invalid traffic during QA.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = testModeEnabled,
                        onCheckedChange = { testModeEnabled = it },
                        enabled = activeRole.canManageAdMob
                    )
                }
            }
        }

        // 2. Independent Placement Switches
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
                    text = "Independent Placement Availability Switches",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )

                PlacementSwitchRow(
                    title = "Global Advertisement Enable / Disable",
                    subtitle = "Master kill-switch for all AdMob placements across the user application",
                    checked = globalAdsEnabled,
                    enabled = activeRole.canManageAdMob,
                    onCheckedChange = { globalAdsEnabled = it }
                )
                HorizontalDivider(color = GraphiteBorder)
                PlacementSwitchRow(
                    title = "Home Banner Ads",
                    subtitle = "Display anchored adaptive banner on the User App home feed",
                    checked = homeBannerEnabled,
                    enabled = activeRole.canManageAdMob && globalAdsEnabled,
                    onCheckedChange = { homeBannerEnabled = it }
                )
                PlacementSwitchRow(
                    title = "Image Detail Banner Ads",
                    subtitle = "Display banner placement below image metadata on the detail screen",
                    checked = detailBannerEnabled,
                    enabled = activeRole.canManageAdMob && globalAdsEnabled,
                    onCheckedChange = { detailBannerEnabled = it }
                )
                PlacementSwitchRow(
                    title = "Rewarded Ads (Prompt Unlock)",
                    subtitle = "Require watching a rewarded video ad before unlocking / copying AI generation prompts",
                    checked = rewardedAdsEnabled,
                    enabled = activeRole.canManageAdMob && globalAdsEnabled,
                    onCheckedChange = { rewardedAdsEnabled = it }
                )
                PlacementSwitchRow(
                    title = "Interstitial Ads",
                    subtitle = "Full-screen transition ads governed by frequency and minimum interval rules below",
                    checked = interstitialAdsEnabled,
                    enabled = activeRole.canManageAdMob && globalAdsEnabled,
                    onCheckedChange = { interstitialAdsEnabled = it }
                )
            }
        }

        // 3. Frequency, Interval, Reward Policy & Fallback Controls
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
                Text(
                    text = "Frequency, Reward Policy & Fallback Behavior",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = freqText,
                        onValueChange = { freqText = it.filter(Char::isDigit) },
                        enabled = activeRole.canManageAdMob,
                        label = { Text("Interstitial Frequency (Actions)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minIntervalText,
                        onValueChange = { minIntervalText = it.filter(Char::isDigit) },
                        enabled = activeRole.canManageAdMob,
                        label = { Text("Min Interstitial Interval (Seconds)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Advertisement Placement Density", style = MaterialTheme.typography.labelMedium, color = ChampagneLight)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("CONSERVATIVE", "BALANCED_UX", "HIGH_VISIBILITY").forEach { strategy ->
                        FilterChip(
                            selected = placementStrategy == strategy,
                            onClick = { if (activeRole.canManageAdMob) placementStrategy = strategy },
                            label = { Text(strategy) }
                        )
                    }
                }

                Text("Reward Policy for Prompt Unlocks", style = MaterialTheme.typography.labelMedium, color = VioletBright)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("SINGLE_AD_PER_PROMPT_UNLOCK", "SESSION_PASS_15M").forEach { policy ->
                        FilterChip(
                            selected = rewardPolicy == policy,
                            onClick = { if (activeRole.canManageAdMob) rewardPolicy = policy },
                            label = { Text(policy) }
                        )
                    }
                }

                Text("Advertisement Fallback Behavior (When No Fill / Network Error)", style = MaterialTheme.typography.labelMedium, color = ChampagneLight)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("GRANT_UNLOCK_ON_NO_FILL", "RETRY_THEN_INTERSTITIAL", "STRICT_REQUIRE_AD").forEach { fb ->
                        FilterChip(
                            selected = fallbackBehavior == fb,
                            onClick = { if (activeRole.canManageAdMob) fallbackBehavior = fb },
                            label = { Text(fb) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlacementSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

// ============================================================================
// MODULE 9: ONESIGNAL PUSH NOTIFICATION MANAGEMENT (AUTO DEEP LINK GENERATION)
// ============================================================================
@Composable
fun OneSignalNotificationScreen(
    publishedImages: List<PromptImageEntity>,
    preselectedImage: PromptImageEntity?,
    deepLinkScheme: String,
    oneSignalAppId: String,
    oneSignalFunctionUrl: String,
    activeRole: AdminRole,
    onSendOrSchedule: (
        title: String,
        message: String,
        targetAudience: String,
        selectedImage: PromptImageEntity?,
        customDeepLink: String,
        scheduleType: String,
        scheduledTimeDisplay: String,
        onComplete: () -> Unit
    ) -> Unit,
    onNavigateToHistory: () -> Unit
) {
    var selectedImage by remember(publishedImages, preselectedImage) {
        mutableStateOf(preselectedImage ?: publishedImages.firstOrNull())
    }
    var notificationTitle by remember { mutableStateOf("") }
    var notificationMessage by remember { mutableStateOf("") }
    var targetAudience by remember { mutableStateOf("ALL_SUBSCRIBED") }
    var scheduleType by remember { mutableStateOf("SEND_NOW") }
    var scheduledTimeText by remember { mutableStateOf("2026-11-18 18:00 UTC") }

    // Automatically populate deep link whenever an image is selected
    val autoDeepLink = remember(selectedImage, deepLinkScheme) {
        if (selectedImage != null) {
            selectedImage!!.deepLink.ifBlank {
                UrlAndDeepLinkValidator.buildAutoDeepLink(deepLinkScheme, selectedImage!!.id)
            }
        } else {
            "$deepLinkScheme://home"
        }
    }

    LaunchedEffect(preselectedImage) {
        if (preselectedImage != null) {
            selectedImage = preselectedImage
            if (notificationTitle.isBlank()) {
                notificationTitle = "New AI Prompt Inspiration: ${preselectedImage.title}"
            }
            if (notificationMessage.isBlank()) {
                notificationMessage = "Tap to view '${preselectedImage.title}' and unlock the full generation prompt!"
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
            sectionNumber = 9,
            title = "OneSignal Push Notification Composer",
            subtitle = "Select a published image to automatically populate its internal deep link ($deepLinkScheme://image/IMAGE_ID), preview the push card, and dispatch via your secure Firebase Cloud Function.",
            trailingContent = {
                OutlinedButton(
                    onClick = onNavigateToHistory,
                    border = BorderStroke(1.dp, ChampagneGold)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Notification History", color = ChampagneLight)
                }
            }
        )

        if (!activeRole.canSendNotifications) {
            PermissionGuardBanner(
                requiredRoleLabel = "Owner or Administrator",
                currentRoleLabel = activeRole.displayName
            )
        }

        // OneSignal Server-Side Security Architecture Callout
        Surface(
            color = ObsidianBg,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, VioletPrimary.copy(alpha = 0.55f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = ChampagneLight)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "OneSignal Zero-Trust Credential Protection",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "Your private OneSignal REST API Key is NEVER stored in this Android app or frontend code. This dashboard authenticates your admin role and calls sendOneSignalNotification (/cloud-functions/onesignal-notify/index.js). Status: ${if (oneSignalFunctionUrl.isNotBlank() && oneSignalAppId.isNotBlank()) "Cloud Function Endpoint Configured" else "Configure OneSignal App ID & Cloud Function URL in Settings for live provider confirmation"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Step 1: Select Published Image -> Auto-Populate Deep Link
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
                    text = "1. Select Published Image (Auto-Populates Internal Deep Link)",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )

                if (publishedImages.isEmpty()) {
                    Text(
                        text = "No PUBLISHED images yet. Publish an image in Image Management to generate an image-specific deep link.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StatusAmber
                    )
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        publishedImages.forEach { img ->
                            val isSelected = selectedImage?.id == img.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedImage = img
                                    if (notificationTitle.isBlank()) {
                                        notificationTitle = "New Prompt: ${img.title}"
                                    }
                                    if (notificationMessage.isBlank()) {
                                        notificationMessage = "Tap to explore '${img.title}' and copy its AI generation prompt."
                                    }
                                },
                                label = { Text("${img.title} (${img.id.take(12)})") }
                            )
                        }
                    }
                }

                // Read-Only Auto-Generated Deep Link Box
                Surface(
                    color = ObsidianBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StatusEmerald.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = StatusEmerald)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AUTOMATICALLY GENERATED APPLICATION DEEP LINK DESTINATION",
                                style = MaterialTheme.typography.labelSmall,
                                color = ChampagneLight
                            )
                            Text(
                                text = autoDeepLink,
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.titleMedium,
                                color = StatusEmerald
                            )
                            Text(
                                text = "When the user taps the notification, the user application opens directly to the selected image detail screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Step 2: Compose Notification Title, Message, Audience & Schedule
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
                Text(
                    text = "2. Notification Content, Audience Segment & Schedule",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = notificationTitle,
                    onValueChange = { notificationTitle = it },
                    enabled = activeRole.canSendNotifications,
                    label = { Text("Notification Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onesignal_title_input")
                )

                OutlinedTextField(
                    value = notificationMessage,
                    onValueChange = { notificationMessage = it },
                    enabled = activeRole.canSendNotifications,
                    label = { Text("Notification Message") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onesignal_message_input")
                )

                Text("Target Audience Segment", style = MaterialTheme.typography.labelMedium, color = ChampagneLight)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL_SUBSCRIBED" to "All Subscribed Users",
                        "ACTIVE_USERS" to "Active Users (7d)",
                        "HIGH_ENGAGEMENT" to "Frequent Prompt Unlockers",
                        "NEW_USERS" to "Newly Registered Users"
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = targetAudience == code,
                            onClick = { if (activeRole.canSendNotifications) targetAudience = code },
                            label = { Text(label) }
                        )
                    }
                }

                Text("Delivery Schedule", style = MaterialTheme.typography.labelMedium, color = VioletBright)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = scheduleType == "SEND_NOW",
                        onClick = { if (activeRole.canSendNotifications) scheduleType = "SEND_NOW" },
                        label = { Text("Send Now (Immediate)") }
                    )
                    FilterChip(
                        selected = scheduleType == "SCHEDULE_LATER",
                        onClick = { if (activeRole.canSendNotifications) scheduleType = "SCHEDULE_LATER" },
                        label = { Text("Schedule Later") }
                    )
                }

                if (scheduleType == "SCHEDULE_LATER") {
                    OutlinedTextField(
                        value = scheduledTimeText,
                        onValueChange = { scheduledTimeText = it },
                        label = { Text("Scheduled Delivery Timestamp (UTC ISO / Date-Time)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Step 3: Live Push Notification Preview Before Sending
        Card(
            colors = CardDefaults.cardColors(containerColor = GraphiteElevated),
            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f)),
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
                        text = "3. Live Push Notification Preview",
                        style = MaterialTheme.typography.titleMedium,
                        color = ChampagneLight
                    )
                    StatusPill(label = targetAudience, tone = "VIOLET")
                }

                Surface(
                    color = ObsidianBg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GraphiteBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (selectedImage != null) {
                            UrlThumbnailBox(imageUrl = selectedImage!!.imageUrl, size = 58.dp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = notificationTitle.ifBlank { "Notification Title Preview" },
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = notificationMessage.ifBlank { "Notification message body will appear here..." },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap Action -> $autoDeepLink",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusEmerald
                            )
                        }
                    }
                }

                if (activeRole.canSendNotifications) {
                    Button(
                        onClick = {
                            onSendOrSchedule(
                                notificationTitle,
                                notificationMessage,
                                targetAudience,
                                selectedImage,
                                autoDeepLink,
                                scheduleType,
                                scheduledTimeText
                            ) {
                                notificationTitle = ""
                                notificationMessage = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("dispatch_onesignal_notification_button")
                    ) {
                        Icon(
                            imageVector = if (scheduleType == "SEND_NOW") Icons.Default.Send else Icons.Default.Schedule,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (scheduleType == "SEND_NOW") {
                                "Dispatch Notification via Secure Backend"
                            } else {
                                "Schedule Notification via Secure Backend"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// MODULE 10: NOTIFICATION HISTORY SCREEN
// ============================================================================
@Composable
fun NotificationHistoryScreen(
    notifications: List<NotificationEntity>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SectionHeaderBar(
            sectionNumber = 10,
            title = "OneSignal Notification History & Delivery Audit",
            subtitle = "Inspect all dispatched and scheduled notifications, target audiences, associated images, deep links, and verified provider responses."
        )

        if (notifications.isEmpty()) {
            EmptyDataStateCard(
                title = "No Push Notifications Recorded Yet",
                message = "Notifications created in the OneSignal Notifications module are logged here with their exact provider confirmation status.",
                icon = Icons.Default.NotificationsActive
            )
        } else {
            notifications.forEach { item ->
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (item.imageThumbnailUrl.isNotBlank()) {
                                    UrlThumbnailBox(imageUrl = item.imageThumbnailUrl, size = 48.dp)
                                }
                                Column {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Associated Image: ${item.imageTitle} (${item.imageId.ifBlank { "N/A" }})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ChampagneLight
                                    )
                                }
                            }
                            StatusPill(
                                label = item.sendingStatus,
                                tone = item.sendingStatus
                            )
                        }

                        Text(
                            text = item.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MonospaceBadge("Deep Link: ${item.deepLink}")
                            StatusPill(label = "Audience: ${item.targetAudience}", tone = "VIOLET")
                            StatusPill(label = "Schedule: ${item.scheduledTimeDisplay}", tone = "CYAN")
                        }

                        Surface(
                            color = ObsidianBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GraphiteBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "PROVIDER DELIVERY DIAGNOSTICS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ChampagneLight
                                )
                                Text(
                                    text = item.deliveryDetails,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Created by ${item.createdByEmail} on ${formatTimestamp(item.createdAtMillis)}",
                                    style = MaterialTheme.typography.labelSmall,
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
