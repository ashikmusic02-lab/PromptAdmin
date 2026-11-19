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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FirestoreCategoryItem
import com.example.model.FirestoreImageItem
import com.example.ui.components.CoilLiveImagePreview
import com.example.ui.components.DestructiveConfirmationDialog
import com.example.ui.components.EmptyDataStateCard
import com.example.ui.components.StatusPill
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
    onMoveImageOrder: (FirestoreImageItem, Boolean) -> Unit
) {
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Button(
            onClick = onOpenNewImageForm,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("add_new_image_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = VioletPrimary,
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(14.dp)
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
            border = BorderStroke(1.dp, GraphiteBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        "UNLISTED" to "আনলিস্টেড",
                        "CATEGORY" to "ক্যাটাগরি"
                    )
                    tabs.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedFilterTab == key,
                            onClick = { selectedFilterTab = key },
                            label = { Text(label) },
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
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                        FilterChip(
                            selected = selectedCategoryId == "uncategorized",
                            onClick = { selectedCategoryId = "uncategorized" },
                            label = { Text("অন্যান্য") },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(cat.name) },
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
                border = BorderStroke(1.dp, GraphiteBorder),
                shape = RoundedCornerShape(16.dp)
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
                                    color = TextSecondary
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    StatusPill(
                                        label = if (item.isPublished) "প্রকাশিত" else "আনলিস্টেড",
                                        tone = if (item.isPublished) "PUBLISHED" else "UNLISTED"
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

                        HorizontalDivider(color = GraphiteBorder)

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

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onOpenEditImageForm(item) },
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
                                    onClick = { pendingDeleteImage = item },
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
        mutableStateOf(false)
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
            // 1. ছবির লিংক (required)
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
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("ছবির লিংক *") },
                        singleLine = true,
                        enabled = !isSaving,
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
                        onValueChange = { if (it.length <= 150) title = it },
                        label = { Text("শিরোনাম *") },
                        supportingText = { Text("${title.length} / 150") },
                        singleLine = true,
                        enabled = !isSaving,
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
                            modifier = Modifier.heightIn(min = 48.dp)
                        )

                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = {
                                    if (!isSaving) {
                                        selectedCategoryId = cat.id
                                        selectedCategoryName = cat.name
                                    }
                                },
                                label = { Text(cat.name) },
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
                            shape = RoundedCornerShape(10.dp)
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

                    HorizontalDivider(color = GraphiteBorder)

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

            // 7. অবস্থা: two buttons, "প্রকাশ করুন" (published) and "আনলিস্টেড" (draft)
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
                            label = { Text("আনলিস্টেড") },
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
 * CATEGORIES (collection `categories`):
 * - ONLY a name and an active toggle ("সক্রিয়"), plus hidden sortOrder (auto = next number).
 * - NO image, thumbnail, or description field in the UI.
 * - List with edit (pen) and delete (trash with confirmation) per row.
 * - Deleting a category does NOT delete images; warns that those images become "অন্যান্য".
 */
@Composable
fun CategoriesScreen(
    categories: List<FirestoreCategoryItem>,
    isSubmitting: Boolean,
    onSaveCategory: (FirestoreCategoryItem?, String, Boolean, () -> Unit) -> Unit,
    onDeleteCategory: (FirestoreCategoryItem) -> Unit
) {
    var editingCategory by remember { mutableStateOf<FirestoreCategoryItem?>(null) }
    var categoryName by remember { mutableStateOf("") }
    var categoryActive by remember { mutableStateOf(true) }
    var pendingDeleteCategory by remember { mutableStateOf<FirestoreCategoryItem?>(null) }

    fun resetForm() {
        editingCategory = null
        categoryName = ""
        categoryActive = true
    }

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
                    onValueChange = { categoryName = it },
                    label = { Text("ক্যাটাগরির নাম *") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "সক্রিয়",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Switch(
                        checked = categoryActive,
                        onCheckedChange = { if (!isSubmitting) categoryActive = it },
                        enabled = !isSubmitting
                    )
                }

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
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "বাতিল", color = TextSecondary)
                        }
                    }

                    Button(
                        onClick = {
                            onSaveCategory(
                                editingCategory,
                                categoryName,
                                categoryActive
                            ) {
                                resetForm()
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                            .testTag("save_category_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VioletPrimary,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
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

        if (categories.isEmpty()) {
            EmptyDataStateCard(
                title = "কোনো ক্যাটাগরি নেই",
                message = "উপরের ফর্ম থেকে নতুন ক্যাটাগরি যোগ করুন।",
                icon = Icons.Default.Category
            )
        } else {
            categories.forEach { cat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_row_${cat.id}"),
                    colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                    border = BorderStroke(1.dp, GraphiteBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = cat.name,
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
                                    label = if (cat.isActive) "সক্রিয়" else "নিষ্ক্রিয়",
                                    tone = if (cat.isActive) "ACTIVE" else "DRAFT"
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
                                    categoryName = cat.name
                                    categoryActive = cat.isActive
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

    pendingDeleteCategory?.let { target ->
        DestructiveConfirmationDialog(
            title = "ক্যাটাগরি মুছে ফেলবেন?",
            message = "'${target.name}' ক্যাটাগরি মুছে ফেলা হবে। কোনো ছবি মুছে যাবে না — এই ক্যাটাগরির ছবিগুলো 'অন্যান্য' হিসেবে থাকবে।",
            confirmButtonText = "মুছুন",
            onConfirm = { onDeleteCategory(target) },
            onDismiss = { pendingDeleteCategory = null }
        )
    }
}
