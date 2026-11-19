package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AdvertisementSettingsModel
import com.example.model.FirestoreImageItem
import com.example.ui.components.EmptyDataStateCard
import com.example.ui.components.MonospaceBadge
import com.example.ui.components.StatusPill
import com.example.ui.components.UrlThumbnailBox
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneLight
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletContainer
import com.example.ui.theme.VioletPrimary
import com.example.util.UrlAndDeepLinkValidator
import kotlin.math.roundToInt

/**
 * NOTIFICATION HELPER (`নোটিফিকেশন সহায়ক`):
 * - List images; tap one to show its ID with a copy button and the text:
 *   "OneSignal-এ Additional Data তে key: imageId, value: (এই আইডি) দিন। Launch URL ফাঁকা রাখুন।"
 * - Deep link everywhere must be `promptlens://image/{imageId}`.
 * - No OneSignal REST API key in the app.
 */
@Composable
fun NotificationHelperScreen(
    images: List<FirestoreImageItem>,
    onShowMessage: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var selectedImageId by remember(images) {
        mutableStateOf(images.firstOrNull()?.id ?: "")
    }
    val selectedImage = images.firstOrNull { it.id == selectedImageId } ?: images.firstOrNull()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (selectedImage != null) {
            val deepLink = UrlAndDeepLinkValidator.buildAutoDeepLink(selectedImage.id)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notification_helper_selected_card"),
                colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                border = BorderStroke(1.dp, VioletPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "নোটিফিকেশন সহায়ক",
                            tint = ChampagneLight,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = selectedImage.title.ifBlank { "নির্বাচিত ছবি" },
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = VioletContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "OneSignal-এ Additional Data তে key: imageId, value: ${selectedImage.id} দিন। Launch URL ফাঁকা রাখুন।",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    MonospaceBadge(
                        text = "imageId: ${selectedImage.id}",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(selectedImage.id))
                            onShowMessage("ছবির আইডি কপি হয়েছে")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("copy_image_id_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VioletPrimary,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "আইডি কপি করুন",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "ছবির আইডি কপি করুন")
                    }

                    MonospaceBadge(
                        text = deepLink,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(deepLink))
                            onShowMessage("ডিপ লিংক কপি হয়েছে")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("copy_deep_link_button"),
                        border = BorderStroke(1.dp, GraphiteBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "ডিপ লিংক কপি করুন",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "ডিপ লিংক কপি করুন", color = TextPrimary)
                    }
                }
            }
        }

        if (images.isEmpty()) {
            EmptyDataStateCard(
                title = "কোনো ছবি নেই",
                message = "ছবি যোগ করার পর এখান থেকে নোটিফিকেশনের আইডি কপি করতে পারবেন।",
                icon = Icons.Default.Collections
            )
        } else {
            Text(
                text = "তালিকা থেকে একটি ছবি নির্বাচন করুন",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            images.forEach { img ->
                val isSelected = selectedImage?.id == img.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedImageId = img.id }
                        .testTag("notification_image_row_${img.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) VioletContainer else GraphiteCard
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) VioletPrimary else GraphiteBorder
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UrlThumbnailBox(imageUrl = img.imageUrl, size = 56.dp)

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = img.title.ifBlank { "শিরোনামহীন" },
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "আইডি: ${img.id}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        StatusPill(
                            label = if (img.isPublished) "প্রকাশিত" else "আনলিস্টেড",
                            tone = if (img.isPublished) "PUBLISHED" else "UNLISTED"
                        )
                    }
                }
            }
        }
    }
}

/**
 * ADS SCREEN (`বিজ্ঞাপন` — document `advertisementSettings/global`):
 * Edit `allAdsEnabled`, `homeBannerEnabled`, `detailBannerEnabled`, `rewardedAdsEnabled`,
 * `interstitialAdsEnabled`, `testAdMode`, `reusePreviousUnlocks`, `interstitialFrequency` (1..50),
 * `adFallbackMessage`, `bannerAdUnitId`, `rewardedAdUnitId`, `interstitialAdUnitId`, `updatedAt`
 * with Bangla switches and single-submit save button.
 */
@Composable
fun AdsSettingsScreen(
    settings: AdvertisementSettingsModel,
    isSaving: Boolean,
    onSave: (AdvertisementSettingsModel) -> Unit
) {
    var allAdsEnabled by remember(settings) { mutableStateOf(settings.allAdsEnabled) }
    var homeBannerEnabled by remember(settings) { mutableStateOf(settings.homeBannerEnabled) }
    var detailBannerEnabled by remember(settings) { mutableStateOf(settings.detailBannerEnabled) }
    var rewardedAdsEnabled by remember(settings) { mutableStateOf(settings.rewardedAdsEnabled) }
    var interstitialAdsEnabled by remember(settings) { mutableStateOf(settings.interstitialAdsEnabled) }
    var testAdMode by remember(settings) { mutableStateOf(settings.testAdMode) }
    var reusePreviousUnlocks by remember(settings) { mutableStateOf(settings.reusePreviousUnlocks) }
    var interstitialFrequency by remember(settings) {
        mutableIntStateOf(settings.interstitialFrequency.coerceIn(1, 50))
    }
    var adFallbackMessage by remember(settings) { mutableStateOf(settings.adFallbackMessage) }
    var bannerAdUnitId by remember(settings) { mutableStateOf(settings.bannerAdUnitId) }
    var rewardedAdUnitId by remember(settings) { mutableStateOf(settings.rewardedAdUnitId) }
    var interstitialAdUnitId by remember(settings) { mutableStateOf(settings.interstitialAdUnitId) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                BanglaSwitchRow(
                    label = "সব বিজ্ঞাপন চালু",
                    checked = allAdsEnabled,
                    enabled = !isSaving,
                    onCheckedChange = { allAdsEnabled = it }
                )
                HorizontalDivider(color = GraphiteBorder)

                BanglaSwitchRow(
                    label = "হোম ব্যানার বিজ্ঞাপন",
                    checked = homeBannerEnabled,
                    enabled = !isSaving,
                    onCheckedChange = { homeBannerEnabled = it }
                )
                HorizontalDivider(color = GraphiteBorder)

                BanglaSwitchRow(
                    label = "ছবির বিস্তারিত পেজে ব্যানার",
                    checked = detailBannerEnabled,
                    enabled = !isSaving,
                    onCheckedChange = { detailBannerEnabled = it }
                )
                HorizontalDivider(color = GraphiteBorder)

                BanglaSwitchRow(
                    label = "রিওয়ার্ড বিজ্ঞাপন (প্রম্পট আনলক)",
                    checked = rewardedAdsEnabled,
                    enabled = !isSaving,
                    onCheckedChange = { rewardedAdsEnabled = it }
                )
                HorizontalDivider(color = GraphiteBorder)

                BanglaSwitchRow(
                    label = "ইন্টারস্টিশিয়াল বিজ্ঞাপন",
                    checked = interstitialAdsEnabled,
                    enabled = !isSaving,
                    onCheckedChange = { interstitialAdsEnabled = it }
                )
                HorizontalDivider(color = GraphiteBorder)

                BanglaSwitchRow(
                    label = "টেস্ট বিজ্ঞাপন মোড",
                    checked = testAdMode,
                    enabled = !isSaving,
                    onCheckedChange = { testAdMode = it }
                )
                HorizontalDivider(color = GraphiteBorder)

                BanglaSwitchRow(
                    label = "আগের আনলক পুনরায় ব্যবহার",
                    checked = reusePreviousUnlocks,
                    enabled = !isSaving,
                    onCheckedChange = { reusePreviousUnlocks = it }
                )
            }
        }

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
                Text(
                    text = "ইন্টারস্টিশিয়াল বিজ্ঞাপনের বিরতি: $interstitialFrequency",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = interstitialFrequency.toFloat(),
                    onValueChange = { interstitialFrequency = it.roundToInt().coerceIn(1, 50) },
                    valueRange = 1f..50f,
                    enabled = !isSaving
                )

                OutlinedTextField(
                    value = adFallbackMessage,
                    onValueChange = { adFallbackMessage = it },
                    label = { Text("বিজ্ঞাপন না থাকলে বিকল্প বার্তা") },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bannerAdUnitId,
                    onValueChange = { bannerAdUnitId = it },
                    label = { Text("ব্যানার অ্যাড ইউনিট আইডি") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rewardedAdUnitId,
                    onValueChange = { rewardedAdUnitId = it },
                    label = { Text("রিওয়ার্ড অ্যাড ইউনিট আইডি") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = interstitialAdUnitId,
                    onValueChange = { interstitialAdUnitId = it },
                    label = { Text("ইন্টারস্টিশিয়াল অ্যাড ইউনিট আইডি") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Button(
            onClick = {
                onSave(
                    AdvertisementSettingsModel(
                        allAdsEnabled = allAdsEnabled,
                        homeBannerEnabled = homeBannerEnabled,
                        detailBannerEnabled = detailBannerEnabled,
                        rewardedAdsEnabled = rewardedAdsEnabled,
                        interstitialAdsEnabled = interstitialAdsEnabled,
                        testAdMode = testAdMode,
                        reusePreviousUnlocks = reusePreviousUnlocks,
                        interstitialFrequency = interstitialFrequency.coerceIn(1, 50),
                        adFallbackMessage = adFallbackMessage.trim(),
                        bannerAdUnitId = bannerAdUnitId.trim(),
                        rewardedAdUnitId = rewardedAdUnitId.trim(),
                        interstitialAdUnitId = interstitialAdUnitId.trim()
                    )
                )
            },
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save_ad_settings_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = VioletPrimary,
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "সেভ হচ্ছে...", style = MaterialTheme.typography.titleMedium)
            } else {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "সেভ করুন",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "সেভ করুন", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun BanglaSwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
