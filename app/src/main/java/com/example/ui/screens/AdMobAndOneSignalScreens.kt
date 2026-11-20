package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.model.FirestoreNotificationItem
import com.example.ui.components.DestructiveConfirmationDialog
import com.example.ui.components.EmptyDataStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.UrlThumbnailBox
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneLight
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletContainer
import com.example.ui.theme.VioletPrimary
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private fun formatBanglaDate(timestamp: Timestamp?): String {
    if (timestamp == null) return "—"
    val sdf = SimpleDateFormat("dd/MM/yyyy • hh:mm a", Locale("bn", "BD"))
    return sdf.format(Date(timestamp.seconds * 1000L))
}

/**
 * NOTIFICATIONS SCREEN (`নোটিফিকেশন` — collection `notifications`):
 * 1. Lists all notification documents newest first; each card shows thumbnail (`imageUrl`),
 *    title, body (2 lines), status chip (`প্রকাশিত` / `খসড়া`), date, with buttons `সম্পাদনা`
 *    and `মুছুন` (confirmation dialog).
 * 2. Shows the `"OneSignal-এ পাঠাতে"` helper card after a successful save (or when selected)
 *    with copy buttons for শিরোনাম, বার্তা, ইমেজ লিংক, ইমেজ আইডি and step-by-step instructions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationsScreen(
    notifications: List<FirestoreNotificationItem>,
    oneSignalHelperItem: FirestoreNotificationItem?,
    isSubmitting: Boolean,
    onOpenNewNotificationForm: () -> Unit,
    onOpenEditNotificationForm: (FirestoreNotificationItem) -> Unit,
    onDeleteNotification: (FirestoreNotificationItem) -> Unit,
    onSelectForOneSignalHelper: (FirestoreNotificationItem) -> Unit,
    onDismissOneSignalHelper: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    var pendingDeleteNotification by remember { mutableStateOf<FirestoreNotificationItem?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Button(
            onClick = onOpenNewNotificationForm,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("add_new_notification_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = VioletPrimary,
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "নতুন নোটিফিকেশন",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "নতুন নোটিফিকেশন",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Helper card shown after a successful save (or when an item is selected for OneSignal)
        oneSignalHelperItem?.let { helperItem ->
            OneSignalPushHelperCard(
                notification = helperItem,
                onDismiss = onDismissOneSignalHelper,
                onShowMessage = onShowMessage
            )
        }

        if (notifications.isEmpty()) {
            EmptyDataStateCard(
                title = "কোনো নোটিফিকেশন নেই",
                message = "নতুন নোটিফিকেশন তৈরি করতে উপরের 'নতুন নোটিফিকেশন' বাটনে ট্যাপ করুন।",
                icon = Icons.Default.NotificationsActive,
                actionLabel = "নতুন নোটিফিকেশন",
                onActionClick = onOpenNewNotificationForm
            )
        } else {
            notifications.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notification_card_${item.id}"),
                    colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                    border = BorderStroke(1.dp, GraphiteBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            UrlThumbnailBox(imageUrl = item.imageUrl, size = 72.dp)

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = item.title.ifBlank { "শিরোনামহীন" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = item.body,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (!item.isServerConfirmed) {
                                        StatusPill(label = "সেভ হচ্ছে…", tone = "WARNING")
                                    }
                                    StatusPill(
                                        label = if (item.isPublished) "প্রকাশিত" else "খসড়া",
                                        tone = if (item.isPublished) "PUBLISHED" else "DRAFT"
                                    )
                                    Text(
                                        text = formatBanglaDate(item.createdAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = GraphiteBorder)

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onSelectForOneSignalHelper(item) },
                                enabled = !isSubmitting,
                                modifier = Modifier.heightIn(min = 48.dp),
                                border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "OneSignal-এ পাঠাতে",
                                    tint = ChampagneLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "OneSignal-এ পাঠাতে", color = ChampagneLight)
                            }

                            OutlinedButton(
                                onClick = { onOpenEditNotificationForm(item) },
                                enabled = !isSubmitting,
                                modifier = Modifier.heightIn(min = 48.dp),
                                border = BorderStroke(1.dp, GraphiteBorder),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "সম্পাদনা",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "সম্পাদনা", color = TextPrimary)
                            }

                            OutlinedButton(
                                onClick = { pendingDeleteNotification = item },
                                enabled = !isSubmitting,
                                modifier = Modifier.heightIn(min = 48.dp),
                                border = BorderStroke(1.dp, StatusRose.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "মুছুন",
                                    tint = StatusRose,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "মুছুন", color = StatusRose)
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDeleteNotification?.let { target ->
        DestructiveConfirmationDialog(
            title = "নোটিফিকেশন মুছে ফেলবেন?",
            message = "'${target.title}' নোটিফিকেশনটি স্থায়ীভাবে মুছে ফেলা হবে।",
            confirmButtonText = "মুছুন",
            onConfirm = { onDeleteNotification(target) },
            onDismiss = { pendingDeleteNotification = null }
        )
    }
}

/**
 * Helper card titled "OneSignal-এ পাঠাতে" shown after a successful notification save.
 * Includes copy buttons for শিরোনাম, বার্তা, ইমেজ লিংক, ইমেজ আইডি and step-by-step instructions.
 */
@Composable
private fun OneSignalPushHelperCard(
    notification: FirestoreNotificationItem,
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("onesignal_helper_card"),
        colors = CardDefaults.cardColors(containerColor = GraphiteCard),
        border = BorderStroke(1.5.dp, ChampagneGold),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "OneSignal-এ পাঠাতে",
                        tint = ChampagneLight,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "OneSignal-এ পাঠাতে",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "বন্ধ করুন",
                        tint = TextSecondary
                    )
                }
            }

            Surface(
                color = VioletContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "OneSignal → Messages → New Push → Audience: Subscribed Users → শিরোনাম ও বার্তা পেস্ট করুন → Image-এ ইমেজ লিংক দিন → Additional Data: key = imageId, value = ${notification.imageId} → Launch URL ফাঁকা রাখুন → Send",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(14.dp)
                )
            }

            OneSignalCopyFieldRow(
                label = "শিরোনাম",
                value = notification.title,
                onCopy = {
                    clipboardManager.setText(AnnotatedString(notification.title))
                    onShowMessage("শিরোনাম কপি হয়েছে")
                }
            )

            OneSignalCopyFieldRow(
                label = "বার্তা",
                value = notification.body,
                onCopy = {
                    clipboardManager.setText(AnnotatedString(notification.body))
                    onShowMessage("বার্তা কপি হয়েছে")
                }
            )

            OneSignalCopyFieldRow(
                label = "ইমেজ লিংক",
                value = notification.imageUrl,
                onCopy = {
                    clipboardManager.setText(AnnotatedString(notification.imageUrl))
                    onShowMessage("ইমেজ লিংক কপি হয়েছে")
                }
            )

            OneSignalCopyFieldRow(
                label = "ইমেজ আইডি",
                value = notification.imageId,
                onCopy = {
                    clipboardManager.setText(AnnotatedString(notification.imageId))
                    onShowMessage("ইমেজ আইডি কপি হয়েছে")
                }
            )
        }
    }
}

@Composable
private fun OneSignalCopyFieldRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Surface(
        color = ObsidianBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GraphiteBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = ChampagneLight
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onCopy,
                modifier = Modifier.heightIn(min = 48.dp),
                border = BorderStroke(1.dp, GraphiteBorder),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "$label কপি করুন",
                    tint = TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = label, color = TextPrimary)
            }
        }
    }
}

/**
 * Full-screen Create/Edit form for `notifications/{id}`:
 * (a) Choose a post from the existing `images` collection with a searchable list showing thumbnail and title (required);
 * (b) শিরোনাম (required, max 120);
 * (c) বার্তা (required, max 500);
 * (d) অবস্থা: two buttons "প্রকাশ করুন" (status 'published') and "খসড়া" (status 'draft').
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenNotificationForm(
    editingNotification: FirestoreNotificationItem?,
    images: List<FirestoreImageItem>,
    isSaving: Boolean,
    onClose: () -> Unit,
    onSave: (
        selectedPost: FirestoreImageItem?,
        fallbackImageId: String,
        fallbackImageUrl: String,
        fallbackCategoryName: String,
        title: String,
        body: String,
        status: String
    ) -> Unit
) {
    BackHandler(enabled = !isSaving) {
        onClose()
    }

    var postSearchQuery by remember { mutableStateOf("") }
    var selectedImageId by remember(editingNotification?.id) {
        mutableStateOf(editingNotification?.imageId ?: "")
    }
    var title by remember(editingNotification?.id) {
        mutableStateOf(editingNotification?.title ?: "")
    }
    var body by remember(editingNotification?.id) {
        mutableStateOf(editingNotification?.body ?: "")
    }
    var status by remember(editingNotification?.id) {
        mutableStateOf(
            if (editingNotification?.status?.equals("draft", ignoreCase = true) == true) "draft" else "published"
        )
    }

    val selectedPost = remember(images, selectedImageId) {
        images.firstOrNull { it.id == selectedImageId }
    }

    val filteredPosts = remember(images, postSearchQuery) {
        val q = postSearchQuery.trim().lowercase()
        if (q.isEmpty()) {
            images
        } else {
            images.filter {
                it.title.lowercase().contains(q) ||
                    it.categoryName.lowercase().contains(q) ||
                    it.id.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (editingNotification == null) "নতুন নোটিফিকেশন" else "নোটিফিকেশন সম্পাদনা",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { if (!isSaving) onClose() },
                        enabled = !isSaving,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CharcoalSurface,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // (a) Choose a post from the existing images collection with a searchable list showing thumbnail and title (required)
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
                        text = "ছবি নির্বাচন করুন *",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (selectedPost != null) {
                        Surface(
                            color = VioletContainer,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, VioletPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UrlThumbnailBox(imageUrl = selectedPost.imageUrl, size = 56.dp)
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = selectedPost.title.ifBlank { "শিরোনামহীন" },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "ক্যাটাগরি: ${selectedPost.categoryName.ifBlank { "অন্যান্য" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "নির্বাচিত",
                                    tint = StatusEmerald
                                )
                            }
                        }
                    } else if (editingNotification != null && editingNotification.imageId.isNotBlank()) {
                        Surface(
                            color = VioletContainer,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, VioletPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UrlThumbnailBox(imageUrl = editingNotification.imageUrl, size = 56.dp)
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "নির্বাচিত ছবির আইডি: ${editingNotification.imageId}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "ক্যাটাগরি: ${editingNotification.categoryName.ifBlank { "অন্যান্য" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = postSearchQuery,
                        onValueChange = { postSearchQuery = it },
                        label = { Text("ছবি খুঁজুন") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "খুঁজুন")
                        },
                        trailingIcon = {
                            if (postSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { postSearchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "মুছুন")
                                }
                            }
                        },
                        singleLine = true,
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("notification_post_search_input")
                    )

                    if (filteredPosts.isEmpty()) {
                        Text(
                            text = "কোনো ছবি পাওয়া যায়নি",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filteredPosts.take(25).forEach { post ->
                                val isSelected = post.id == selectedImageId
                                Surface(
                                    color = if (isSelected) VioletContainer else ObsidianBg,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) VioletPrimary else GraphiteBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isSaving) {
                                            selectedImageId = post.id
                                            if (title.isBlank()) {
                                                title = post.title.take(120)
                                            }
                                        }
                                        .testTag("select_post_row_${post.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        UrlThumbnailBox(imageUrl = post.imageUrl, size = 50.dp)
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = post.title.ifBlank { "শিরোনামহীন" },
                                                style = MaterialTheme.typography.titleSmall,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = post.categoryName.ifBlank { "অন্যান্য" },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "নির্বাচিত",
                                                tint = StatusEmerald
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // (b) শিরোনাম (required, max 120) & (c) বার্তা (required, max 500)
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
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 120) title = it },
                        label = { Text("শিরোনাম *") },
                        supportingText = { Text("${title.length} / 120") },
                        singleLine = true,
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("notification_title_input")
                    )

                    OutlinedTextField(
                        value = body,
                        onValueChange = { if (it.length <= 500) body = it },
                        label = { Text("বার্তা *") },
                        supportingText = { Text("${body.length} / 500") },
                        minLines = 3,
                        maxLines = 7,
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("notification_body_input")
                    )
                }
            }

            // (d) অবস্থা: two buttons "প্রকাশ করুন" (status 'published') and "খসড়া" (status 'draft')
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
                    Text(
                        text = "অবস্থা",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isPublishedSelected = !status.equals("draft", ignoreCase = true)
                        FilterChip(
                            selected = isPublishedSelected,
                            onClick = { if (!isSaving) status = "published" },
                            label = { Text("প্রকাশ করুন") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StatusEmerald.copy(alpha = 0.25f),
                                selectedLabelColor = StatusEmerald
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        )

                        FilterChip(
                            selected = !isPublishedSelected,
                            onClick = { if (!isSaving) status = "draft" },
                            label = { Text("খসড়া") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VioletContainer,
                                selectedLabelColor = TextPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        )
                    }
                }
            }

            Button(
                onClick = {
                    onSave(
                        selectedPost,
                        editingNotification?.imageId.orEmpty(),
                        editingNotification?.imageUrl.orEmpty(),
                        editingNotification?.categoryName.orEmpty(),
                        title,
                        body,
                        status
                    )
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_notification_form_button"),
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
                    Text(
                        text = "সেভ হচ্ছে...",
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "সেভ করুন",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "সেভ করুন",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            OutlinedButton(
                onClick = { if (!isSaving) onClose() },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                border = BorderStroke(1.dp, GraphiteBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = "বাতিল", color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(16.dp))
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
