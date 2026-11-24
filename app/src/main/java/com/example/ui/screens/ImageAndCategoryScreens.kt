package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FirestoreAiToolItem
import com.example.model.FirestoreCategoryItem
import com.example.model.FirestoreImageItem
import com.example.ui.components.CoilLiveImagePreview
import com.example.ui.components.DestructiveConfirmationDialog
import com.example.ui.components.EmptyDataStateCard
import com.example.ui.components.StatusPill
import java.net.URI
import com.example.ui.components.UrlThumbnailBox
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImagesScreen(
    images: List<FirestoreImageItem>,
    categories: List<FirestoreCategoryItem>,
    isLoading: Boolean,
    isSubmitting: Boolean,
    onOpenNewImageForm: () -> Unit,
    onOpenEditImageForm: (FirestoreImageItem) -> Unit,
    onDeleteImage: (FirestoreImageItem) -> Unit,
    onMoveImageOrder: (FirestoreImageItem, Boolean) -> Unit,
    onTogglePublishedStatus: (FirestoreImageItem, Boolean) -> Unit = { _, _ -> },
    onShowMessage: (String) -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    var searchQuery by remember { mutableStateOf("") }
    // Status filter: "ALL", "PUBLISHED", "UNLISTED", "CATEGORY"
    var selectedFilterTab by remember { mutableStateOf("ALL") }
    var selectedCategoryId by remember { mutableStateOf("ALL") }
    var pendingDeleteImage by remember { mutableStateOf<FirestoreImageItem?>(null) }

    val filteredImages = remember(images, searchQuery, selectedFilterTab, selectedCategoryId) {
        images.filter { item ->
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                item.title.lowercase().contains(q) ||
                item.categoryName.lowercase().contains(q) ||
                item.keywords.lowercase().contains(q)
            val matchesStatus = when (selectedFilterTab) {
                "PUBLISHED" -> item.isPublished
                "UNLISTED" -> !item.isPublished
                "CATEGORY" -> selectedCategoryId == "ALL" || item.categoryId == selectedCategoryId
                else -> true
            }
            matchesSearch && matchesStatus
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onOpenNewImageForm,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .testTag("add_new_image_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = VioletPrimary,
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(50)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "নতুন ছবি",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "নতুন ছবি",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GraphiteCard),
            border = BorderStroke(1.dp, GraphiteBorder.copy(alpha = 0.75f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("ছবি খুঁজুন") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "খুঁজুন")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "মুছুন")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("image_search_input")
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        "ALL" to "সব",
                        "PUBLISHED" to "প্রকাশিত",
                        "UNLISTED" to "অপ্রকাশিত",
                        "CATEGORY" to "ক্যাটাগরি"
                    )
                    tabs.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedFilterTab == key,
                            onClick = { selectedFilterTab = key },
                            label = { Text(label) },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }

                if (selectedFilterTab == "CATEGORY") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategoryId == "ALL",
                            onClick = { selectedCategoryId = "ALL" },
                            label = { Text("সব ক্যাটাগরি") },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                        FilterChip(
                            selected = selectedCategoryId == "uncategorized",
                            onClick = { selectedCategoryId = "uncategorized" },
                            label = { Text("অন্যান্য") },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(cat.name.ifBlank { "অন্যান্য" }) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isLoading && images.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                border = BorderStroke(1.dp, GraphiteBorder.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = VioletPrimary)
                        Text(
                            text = "লোড হচ্ছে...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else if (filteredImages.isEmpty()) {
            EmptyDataStateCard(
                title = "কোনো ছবি পাওয়া যায়নি",
                message = "নতুন ছবি যোগ করতে উপরের 'নতুন ছবি' বাটনে ট্যাপ করুন।",
                icon = Icons.Default.Collections,
                actionLabel = "নতুন ছবি",
                onActionClick = onOpenNewImageForm
            )
        } else {
            filteredImages.forEachIndexed { idx, item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("image_card_${item.id}"),
                    colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                    border = BorderStroke(1.dp, GraphiteBorder.copy(alpha = 0.75f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            UrlThumbnailBox(imageUrl = item.imageUrl, size = 76.dp)

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = item.title.ifBlank { "শিরোনামহীন" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "ক্যাটাগরি: ${item.categoryName.ifBlank { "অন্যান্য" }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (!item.isServerConfirmed) {
                                        StatusPill(label = "সেভ হচ্ছে…", tone = "WARNING")
                                    }
                                    StatusPill(
                                        label = if (item.isPublished) "প্রকাশিত" else "অপ্রকাশিত",
                                        tone = if (item.isPublished) "PUBLISHED" else "UNLISTED"
                                    )
                                    StatusPill(
                                        label = "ভিউ: ${item.viewCount}",
                                        tone = "NEUTRAL"
                                    )
                                    StatusPill(
                                        label = "লাইক: ${item.likeCount}",
                                        tone = "NEUTRAL"
                                    )
                                    if (item.featured) {
                                        StatusPill(label = "ফিচার্ড", tone = "FEATURED")
                                    }
                                    if (item.trending) {
                                        StatusPill(label = "ট্রেন্ডিং", tone = "TRENDING")
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (item.isPublished) "অবস্থা: প্রকাশিত" else "অবস্থা: অপ্রকাশিত",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextSecondary
                            )
                            Switch(
                                checked = item.isPublished,
                                onCheckedChange = { publish ->
                                    if (!isSubmitting) {
                                        onTogglePublishedStatus(item, publish)
                                    }
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier.testTag("quick_publish_switch_${item.id}")
                            )
                        }

                        HorizontalDivider(color = GraphiteBorder.copy(alpha = 0.7f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onMoveImageOrder(item, true) },
                                    enabled = !isSubmitting && idx > 0,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "উপরে তুলুন",
                                        tint = if (idx > 0) TextPrimary else TextMuted
                                    )
                                }
                                IconButton(
                                    onClick = { onMoveImageOrder(item, false) },
                                    enabled = !isSubmitting && idx < filteredImages.lastIndex,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "নিচে নামান",
                                        tint = if (idx < filteredImages.lastIndex) TextPrimary else TextMuted
                                    )
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(
                                            AnnotatedString("promptlens://image/${item.id}")
                                        )
                                        onShowMessage("লিংক কপি হয়েছে")
                                    },
                                    enabled = !isSubmitting,
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("copy_image_link_${item.id}"),
                                    border = BorderStroke(1.dp, GraphiteBorder),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "লিংক কপি",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "লিংক কপি", color = TextPrimary)
                                }

                                OutlinedButton(
                                    onClick = { onOpenEditImageForm(item) },
                                    enabled = !isSubmitting,
                                    modifier = Modifier.heightIn(min = 48.dp),
                                    border = BorderStroke(1.dp, GraphiteBorder),
                                    shape = RoundedCornerShape(50)
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
                                    onClick = { pendingDeleteImage = item },
                                    enabled = !isSubmitting,
                                    modifier = Modifier.heightIn(min = 48.dp),
                                    border = BorderStroke(1.dp, StatusRose.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(50)
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
    }

    pendingDeleteImage?.let { target ->
        DestructiveConfirmationDialog(
            title = "ছবি মুছে ফেলবেন?",
            message = "'${target.title}' এবং এর প্রম্পট স্থায়ীভাবে মুছে ফেলা হবে।",
            confirmButtonText = "মুছুন",
            onConfirm = { onDeleteImage(target) },
            onDismiss = { pendingDeleteImage = null }
        )
    }
}

/**
 * Full-screen form for creating or editing an image + prompt in one atomic batch.
 * Fields in strict order:
 * 1. ছবির লিংক + hint + live Coil preview + aspectRatio calculation
 * 2. শিরোনাম (required, max 150)
 * 3. বিবরণ (optional, max 1000)
 * 4. ক্যাটাগরি (optional — chips of existing categories + "কোনোটি নয়")
 * 5. প্রম্পট (required, max 5000) — multiline with copy button + character counter
 * 6. Two toggles: ফিচার্ড, ট্রেন্ডিং
 * 7. অবস্থা: two buttons, "প্রকাশ করুন" (published) and "আনলিস্টেড" (draft)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FullScreenImageForm(
    editingImage: FirestoreImageItem?,
    activeDocumentId: String = "",
    loadedPromptText: String,
    isLoadingPrompt: Boolean,
    isSaving: Boolean,
    categories: List<FirestoreCategoryItem>,
    onClose: () -> Unit,
    onSave: (
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
    ) -> Unit,
    onShowMessage: (String) -> Unit
) {
    BackHandler(enabled = !isSaving) {
        onClose()
    }

    val clipboardManager = LocalClipboardManager.current
    val resolvedImageId = editingImage?.id?.ifBlank { activeDocumentId } ?: activeDocumentId

    var imageUrl by remember(editingImage?.id) {
        mutableStateOf(editingImage?.imageUrl ?: "")
    }
    var title by remember(editingImage?.id) {
        mutableStateOf(editingImage?.title ?: "")
    }
    var description by remember(editingImage?.id) {
        mutableStateOf(editingImage?.description ?: "")
    }
    var selectedCategoryId by remember(editingImage?.id) {
        mutableStateOf(editingImage?.categoryId?.ifBlank { "uncategorized" } ?: "uncategorized")
    }
    var selectedCategoryName by remember(editingImage?.id) {
        mutableStateOf(editingImage?.categoryName?.ifBlank { "অন্যান্য" } ?: "অন্যান্য")
    }
    var promptText by remember(editingImage?.id) {
        mutableStateOf("")
    }
    var featured by remember(editingImage?.id) {
        mutableStateOf(editingImage?.featured ?: false)
    }
    var trending by remember(editingImage?.id) {
        mutableStateOf(editingImage?.trending ?: false)
    }
    var status by remember(editingImage?.id) {
        mutableStateOf(
            if (editingImage?.status?.equals("draft", ignoreCase = true) == true) "draft" else "published"
        )
    }

    var isImagePreviewLoaded by remember(editingImage?.id) {
        mutableStateOf(editingImage != null && editingImage.imageUrl.isNotBlank())
    }
    var measuredAspectRatio by remember(editingImage?.id) {
        mutableDoubleStateOf(editingImage?.aspectRatio ?: 1.0)
    }

    LaunchedEffect(loadedPromptText, editingImage?.id) {
        if (editingImage != null && loadedPromptText.isNotEmpty() && promptText.isEmpty()) {
            promptText = loadedPromptText
        }
    }

    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (editingImage == null) "নতুন ছবি" else "ছবি সম্পাদনা",
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
            if (editingImage != null && resolvedImageId.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                    border = BorderStroke(1.dp, GraphiteBorder.copy(alpha = 0.75f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "ছবির আইডি",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                            SelectionContainer {
                                Text(
                                    text = resolvedImageId,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    modifier = Modifier.testTag("edit_image_selectable_id")
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(
                                    AnnotatedString("promptlens://image/$resolvedImageId")
                                )
                                onShowMessage("লিংক কপি হয়েছে")
                            },
                            enabled = !isSaving,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("edit_image_copy_link_button"),
                            border = BorderStroke(1.dp, GraphiteBorder),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "লিংক কপি",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "লিংক কপি", color = TextPrimary)
                        }
                    }
                }
            }

            // 1. ছবির লিংক (required)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                border = BorderStroke(1.dp, GraphiteBorder.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("ছবির লিংক *") },
                        singleLine = true,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_image_url_input")
                    )

                    Text(
                        text = "গুগল ড্রাইভের ছবি 'যে কেউ লিংক দিয়ে দেখতে পারবে' করে লিংক দিন",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    CoilLiveImagePreview(
                        rawImageUrl = imageUrl,
                        height = 210.dp,
                        onLoadStateChanged = { loaded, ratio ->
                            isImagePreviewLoaded = loaded
                            if (loaded) {
                                measuredAspectRatio = ratio
                            }
                        }
                    )
                }
            }

            // 2. শিরোনাম (required, max 150) & 3. বিবরণ (optional, max 1000)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                border = BorderStroke(1.dp, GraphiteBorder.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 150) title = it },
                        label = { Text("শিরোনাম *") },
                        supportingText = { Text("${title.length} / 150") },
                        singleLine = true,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_title_input")
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 1000) description = it },
                        label = { Text("বিবরণ") },
                        supportingText = { Text("${description.length} / 1000") },
                        minLines = 2,
                        maxLines = 5,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_description_input")
                    )
                }
            }

            // 4. ক্যাটাগরি (optional) — existing categories + "কোনোটি নয়"
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                border = BorderStroke(1.dp, GraphiteBorder.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ক্যাটাগরি",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategoryId == "uncategorized" || selectedCategoryId.isBlank(),
                            onClick = {
                                if (!isSaving) {
                                    selectedCategoryId = "uncategorized"
                                    selectedCategoryName = "অন্যান্য"
                                }
                            },
                            label = { Text("কোনোটি নয়") },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.heightIn(min = 48.dp)
                        )

                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = {
                                    if (!isSaving) {
                                        selectedCategoryId = cat.id
                                        selectedCategoryName = cat.name.ifBlank { "অন্যান্য" }
                                    }
                                },
                                label = { Text(cat.name.ifBlank { "অন্যান্য" }) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                }
            }

            // 5. প্রম্পট (required, max 5000) — big multiline field with copy button & character counter
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                border = BorderStroke(1.dp, GraphiteBorder.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "প্রম্পট *",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        OutlinedButton(
                            onClick = {
                                if (promptText.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(promptText))
                                    onShowMessage("প্রম্পট কপি হয়েছে")
                                }
                            },
                            enabled = promptText.isNotBlank(),
                            modifier = Modifier.heightIn(min = 48.dp),
                            border = BorderStroke(1.dp, GraphiteBorder),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "কপি",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "কপি", color = TextPrimary)
                        }
                    }

                    if (isLoadingPrompt) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = VioletPrimary
                            )
                            Text(
                                text = "প্রম্পট লোড হচ্ছে...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { if (it.length <= 5000) promptText = it },
                        label = { Text("প্রম্পট লিখুন") },
                        supportingText = { Text("${promptText.length} / 5000") },
                        minLines = 6,
                        maxLines = 14,
                        enabled = !isSaving && !isLoadingPrompt,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_prompt_input")
                    )
                }
            }

            // 6. Two toggles: ফিচার্ড, ট্রেন্ডিং
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                border = BorderStroke(1.dp, GraphiteBorder.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ফিচার্ড",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Switch(
                            checked = featured,
                            onCheckedChange = { if (!isSaving) featured = it },
                            enabled = !isSaving
                        )
                    }

                    HorizontalDivider(color = GraphiteBorder.copy(alpha = 0.7f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ট্রেন্ডিং",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Switch(
                            checked = trending,
                            onCheckedChange = { if (!isSaving) trending = it },
                            enabled = !isSaving
                        )
                    }
                }
            }

            // 7. অবস্থা: two buttons, "প্রকাশ করুন" (published) and "অপ্রকাশিত" (draft)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                border = BorderStroke(1.dp, GraphiteBorder.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
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
                            shape = RoundedCornerShape(50),
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
                            label = { Text("অপ্রকাশিত") },
                            shape = RoundedCornerShape(50),
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

            // Save & Cancel Buttons
            Button(
                onClick = {
                    onSave(
                        imageUrl,
                        title,
                        description,
                        selectedCategoryId,
                        selectedCategoryName,
                        promptText,
                        featured,
                        trending,
                        status,
                        measuredAspectRatio,
                        isImagePreviewLoaded
                    )
                },
                enabled = !isSaving && !isLoadingPrompt,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_image_form_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VioletPrimary,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(50)
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
                shape = RoundedCornerShape(50)
            ) {
                Text(text = "বাতিল", color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * CATEGORIES (collection `categories`):
 * - Name only + optional reorder number (`sortOrder`).
 * - Safe handling of the blank / "অন্যান্য" category.
 * - List with edit and delete (with confirmation dialog).
 * - Deleting a category does NOT delete images; warns that those images become "অন্যান্য".
 */
@Composable
fun CategoriesScreen(
    categories: List<FirestoreCategoryItem>,
    uncategorizedImageCount: Int = 0,
    isSubmitting: Boolean,
    onSaveCategory: (FirestoreCategoryItem?, String, Boolean, String, () -> Unit) -> Unit,
    onDeleteCategory: (FirestoreCategoryItem) -> Unit
) {
    var editingCategory by remember { mutableStateOf<FirestoreCategoryItem?>(null) }
    var categoryName by remember { mutableStateOf("") }
    var categorySortOrderText by remember { mutableStateOf("") }
    var pendingDeleteCategory by remember { mutableStateOf<FirestoreCategoryItem?>(null) }

    fun resetForm() {
        editingCategory = null
        categoryName = ""
        categorySortOrderText = ""
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GraphiteCard),
            border = BorderStroke(1.dp, GraphiteBorder.copy(alpha = 0.75f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (editingCategory == null) "নতুন ক্যাটাগরি" else "ক্যাটাগরি সম্পাদনা",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { if (it.length <= 80) categoryName = it },
                    label = { Text("ক্যাটাগরির নাম *") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_input")
                )

                OutlinedTextField(
                    value = categorySortOrderText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { it.isDigit() || it == '-' }) {
                            categorySortOrderText = input
                        }
                    },
                    label = { Text("ক্রম (ঐচ্ছিক)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_sort_order_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (editingCategory != null) {
                        OutlinedButton(
                            onClick = { resetForm() },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            border = BorderStroke(1.dp, GraphiteBorder),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(text = "বাতিল", color = TextSecondary)
                        }
                    }

                    Button(
                        onClick = {
                            onSaveCategory(
                                editingCategory,
                                categoryName,
                                editingCategory?.isActive ?: true,
                                categorySortOrderText
                            ) {
                                resetForm()
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .testTag("save_category_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VioletPrimary,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = TextPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "সেভ করুন",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "সেভ করুন")
                        }
                    }
                }
            }
        }

        // Safe handling of the built-in blank / "অন্যান্য" category
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("category_row_uncategorized"),
            colors = CardDefaults.cardColors(containerColor = GraphiteCard),
            border = BorderStroke(1.dp, GraphiteBorder.copy(alpha = 0.75f)),
            shape = RoundedCornerShape(22.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "অন্যান্য",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "ডিফল্ট ক্যাটাগরি • ছবি: $uncategorizedImageCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                StatusPill(label = "ডিফল্ট", tone = "NEUTRAL")
            }
        }

        if (categories.isEmpty()) {
            EmptyDataStateCard(
                title = "কোনো ক্যাটাগরি নেই",
                message = "উপরের ফর্ম থেকে নতুন ক্যাটাগরি যোগ করুন।",
                icon = Icons.Default.Category
            )
        } else {
            categories.forEach { cat ->
                val isBuiltInOther = cat.id.equals("uncategorized", ignoreCase = true) ||
                    cat.name.trim() == "অন্যান্য"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_row_${cat.id}"),
                    colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                    border = BorderStroke(1.dp, GraphiteBorder.copy(alpha = 0.75f)),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = cat.name.ifBlank { "অন্যান্য" },
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!cat.isServerConfirmed) {
                                    StatusPill(
                                        label = "সেভ হচ্ছে…",
                                        tone = "WARNING"
                                    )
                                }
                                StatusPill(
                                    label = "ক্রম: ${cat.sortOrder}",
                                    tone = "NEUTRAL"
                                )
                                Text(
                                    text = "ছবি: ${cat.imageCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    editingCategory = cat
                                    categoryName = cat.name.ifBlank { "অন্যান্য" }
                                    categorySortOrderText = cat.sortOrder.toString()
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "সম্পাদনা",
                                    tint = TextPrimary
                                )
                            }

                            if (!isBuiltInOther) {
                                IconButton(
                                    onClick = { pendingDeleteCategory = cat },
                                    enabled = !isSubmitting,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "মুছুন",
                                        tint = StatusRose
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDeleteCategory?.let { target ->
        DestructiveConfirmationDialog(
            title = "ক্যাটাগরি মুছে ফেলবেন?",
            message = "'${target.name.ifBlank { "অন্যান্য" }}' ক্যাটাগরি মুছে ফেলা হবে। কোনো ছবি মুছে যাবে না — এই ক্যাটাগরির ছবিগুলো 'অন্যান্য' হিসেবে থাকবে।",
            confirmButtonText = "মুছুন",
            onConfirm = { onDeleteCategory(target) },
            onDismiss = { pendingDeleteCategory = null }
        )
    }
}

private fun resolveAiToolLogoUrl(logoUrl: String, siteUrl: String): String {
    val cleanLogo = logoUrl.trim()
    if (cleanLogo.isNotEmpty()) return cleanLogo
    val host = try {
        URI(siteUrl.trim()).host?.trim().orEmpty()
    } catch (_: Exception) {
        ""
    }
    return if (host.isNotEmpty()) {
        "https://www.google.com/s2/favicons?domain=$host&sz=128"
    } else {
        ""
    }
}

/**
 * AI TOOLS SCREEN (`এআই টুল` — collection `aiTools`):
 * - Each document has EXACTLY: name (1-60), url (https://, max 500), logoUrl (optional, max 500),
 *   enabled (bool), sortOrder (int), updatedAt (server timestamp).
 * - Rounded cards with logo (or Google s2 favicon when logoUrl is empty), name, url, enabled switch.
 * - Add, edit, delete with confirmation dialog, reorder by editing a number.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiToolsScreen(
    aiTools: List<FirestoreAiToolItem>,
    isSubmitting: Boolean,
    onGenerateNewDocId: () -> String,
    onSaveAiTool: (
        documentId: String,
        isEdit: Boolean,
        name: String,
        url: String,
        logoUrl: String,
        enabled: Boolean,
        sortOrderText: String,
        onSuccess: () -> Unit
    ) -> Unit,
    onToggleEnabled: (FirestoreAiToolItem, Boolean) -> Unit,
    onUpdateSortOrder: (FirestoreAiToolItem, String, () -> Unit) -> Unit,
    onDeleteAiTool: (FirestoreAiToolItem) -> Unit
) {
    var editingTool by remember { mutableStateOf<FirestoreAiToolItem?>(null) }
    var activeDocId by remember { mutableStateOf(onGenerateNewDocId()) }
    var toolName by remember { mutableStateOf("") }
    var toolUrl by remember { mutableStateOf("") }
    var toolLogoUrl by remember { mutableStateOf("") }
    var toolEnabled by remember { mutableStateOf(true) }
    var toolSortOrderText by remember(aiTools.size) {
        mutableStateOf(((aiTools.maxOfOrNull { it.sortOrder } ?: 0) + 1).toString())
    }

    var pendingDeleteTool by remember { mutableStateOf<FirestoreAiToolItem?>(null) }
    var reorderingTool by remember { mutableStateOf<FirestoreAiToolItem?>(null) }
    var reorderInputText by remember { mutableStateOf("") }

    fun resetForm() {
        editingTool = null
        activeDocId = onGenerateNewDocId()
        toolName = ""
        toolUrl = ""
        toolLogoUrl = ""
        toolEnabled = true
        toolSortOrderText = ((aiTools.maxOfOrNull { it.sortOrder } ?: 0) + 1).toString()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GraphiteCard),
            border = BorderStroke(1.dp, GraphiteBorder.copy(alpha = 0.75f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (editingTool == null) "নতুন এআই টুল" else "এআই টুল সম্পাদনা",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = toolName,
                    onValueChange = { if (it.length <= 60) toolName = it },
                    label = { Text("নাম *") },
                    supportingText = { Text("${toolName.length} / 60") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_tool_name_input")
                )

                OutlinedTextField(
                    value = toolUrl,
                    onValueChange = { if (it.length <= 500) toolUrl = it },
                    label = { Text("সাইট লিংক (https://) *") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_tool_url_input")
                )

                OutlinedTextField(
                    value = toolLogoUrl,
                    onValueChange = { if (it.length <= 500) toolLogoUrl = it },
                    label = { Text("লোগো লিংক (ঐচ্ছিক)") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_tool_logo_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = toolSortOrderText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() || it == '-' }) {
                                toolSortOrderText = input
                            }
                        },
                        label = { Text("ক্রম") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_tool_sort_order_input")
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "চালু",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Switch(
                            checked = toolEnabled,
                            onCheckedChange = { if (!isSubmitting) toolEnabled = it },
                            enabled = !isSubmitting
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (editingTool != null) {
                        OutlinedButton(
                            onClick = { resetForm() },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            border = BorderStroke(1.dp, GraphiteBorder),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(text = "বাতিল", color = TextSecondary)
                        }
                    }

                    Button(
                        onClick = {
                            onSaveAiTool(
                                editingTool?.id ?: activeDocId,
                                editingTool != null,
                                toolName,
                                toolUrl,
                                toolLogoUrl,
                                toolEnabled,
                                toolSortOrderText
                            ) {
                                resetForm()
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .testTag("save_ai_tool_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VioletPrimary,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = TextPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "সেভ করুন",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "সেভ করুন")
                        }
                    }
                }
            }
        }

        if (aiTools.isEmpty()) {
            EmptyDataStateCard(
                title = "কোনো এআই টুল নেই",
                message = "উপরের ফর্ম থেকে নতুন এআই টুল যোগ করুন।",
                icon = Icons.Default.AutoAwesome
            )
        } else {
            aiTools.forEach { tool ->
                val resolvedLogo = remember(tool.logoUrl, tool.url) {
                    resolveAiToolLogoUrl(tool.logoUrl, tool.url)
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_tool_card_${tool.id}"),
                    colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                    border = BorderStroke(1.dp, GraphiteBorder.copy(alpha = 0.75f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UrlThumbnailBox(
                                imageUrl = resolvedLogo,
                                size = 56.dp
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = tool.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = tool.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (!tool.isServerConfirmed) {
                                        StatusPill(label = "সেভ হচ্ছে…", tone = "WARNING")
                                    }
                                    StatusPill(
                                        label = "ক্রম: ${tool.sortOrder}",
                                        tone = "NEUTRAL"
                                    )
                                }
                            }

                            Switch(
                                checked = tool.enabled,
                                onCheckedChange = { newEnabled ->
                                    if (!isSubmitting) {
                                        onToggleEnabled(tool, newEnabled)
                                    }
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier.testTag("ai_tool_switch_${tool.id}")
                            )
                        }

                        HorizontalDivider(color = GraphiteBorder.copy(alpha = 0.7f))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    reorderingTool = tool
                                    reorderInputText = tool.sortOrder.toString()
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier.heightIn(min = 48.dp),
                                border = BorderStroke(1.dp, GraphiteBorder),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(text = "ক্রম: ${tool.sortOrder}", color = TextPrimary)
                            }

                            OutlinedButton(
                                onClick = {
                                    editingTool = tool
                                    activeDocId = tool.id
                                    toolName = tool.name
                                    toolUrl = tool.url
                                    toolLogoUrl = tool.logoUrl
                                    toolEnabled = tool.enabled
                                    toolSortOrderText = tool.sortOrder.toString()
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier.heightIn(min = 48.dp),
                                border = BorderStroke(1.dp, GraphiteBorder),
                                shape = RoundedCornerShape(50)
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
                                onClick = { pendingDeleteTool = tool },
                                enabled = !isSubmitting,
                                modifier = Modifier.heightIn(min = 48.dp),
                                border = BorderStroke(1.dp, StatusRose.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(50)
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

    reorderingTool?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) reorderingTool = null },
            containerColor = GraphiteCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "ক্রম পরিবর্তন",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = reorderInputText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { it.isDigit() || it == '-' }) {
                            reorderInputText = input
                        }
                    },
                    label = { Text("নতুন ক্রম সংখ্যা") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateSortOrder(target, reorderInputText) {
                            reorderingTool = null
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VioletPrimary,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("সেভ করুন")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { reorderingTool = null },
                    enabled = !isSubmitting,
                    border = BorderStroke(1.dp, GraphiteBorder),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("বাতিল", color = TextSecondary)
                }
            }
        )
    }

    pendingDeleteTool?.let { target ->
        DestructiveConfirmationDialog(
            title = "এআই টুল মুছে ফেলবেন?",
            message = "'${target.name}' স্থায়ীভাবে মুছে ফেলা হবে।",
            confirmButtonText = "মুছুন",
            onConfirm = { onDeleteAiTool(target) },
            onDismiss = { pendingDeleteTool = null }
        )
    }
}
