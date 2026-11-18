package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AdminRole
import com.example.model.PromptCategoryEntity
import com.example.model.PromptImageEntity
import com.example.ui.components.DestructiveConfirmationDialog
import com.example.ui.components.EmptyDataStateCard
import com.example.ui.components.LiveExternalImagePreview
import com.example.ui.components.MonospaceBadge
import com.example.ui.components.PermissionGuardBanner
import com.example.ui.components.SectionHeaderBar
import com.example.ui.components.StatusPill
import com.example.ui.components.UrlOnlyComplianceBanner
import com.example.ui.components.UrlThumbnailBox
import com.example.ui.components.formatShortDate
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneLight
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.theme.VioletContainer
import com.example.ui.theme.VioletPrimary
import com.example.util.UrlAndDeepLinkValidator
import com.example.util.UrlValidationResult

@Composable
fun ImageManagementScreen(
    images: List<PromptImageEntity>,
    categories: List<PromptCategoryEntity>,
    deepLinkScheme: String,
    activeRole: AdminRole,
    onSaveImage: (
        existingId: String?,
        imageUrl: String,
        shortName: String,
        title: String,
        description: String,
        categoryId: String,
        categoryName: String,
        prompt: String,
        featured: Boolean,
        trending: Boolean,
        status: String,
        displayOrder: Int,
        onSuccess: () -> Unit
    ) -> Unit,
    onToggleQuickFlag: (
        image: PromptImageEntity,
        togglePublish: Boolean,
        toggleFeatured: Boolean,
        toggleTrending: Boolean
    ) -> Unit,
    onDeleteImage: (PromptImageEntity) -> Unit,
    onSendPushForImage: (PromptImageEntity) -> Unit,
    onNavigateToCategories: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // ALL, PUBLISHED, DRAFT, FEATURED, TRENDING
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 8

    var showEditorModal by remember { mutableStateOf(false) }
    var editingImage by remember { mutableStateOf<PromptImageEntity?>(null) }
    var previewingImage by remember { mutableStateOf<PromptImageEntity?>(null) }
    var deletingImage by remember { mutableStateOf<PromptImageEntity?>(null) }

    val filteredImages = remember(images, searchQuery, selectedStatusFilter, selectedCategoryFilter) {
        images.filter { img ->
            val matchesSearch = searchQuery.isBlank() ||
                img.title.contains(searchQuery, ignoreCase = true) ||
                img.shortName.contains(searchQuery, ignoreCase = true) ||
                img.prompt.contains(searchQuery, ignoreCase = true) ||
                img.categoryName.contains(searchQuery, ignoreCase = true)
            val matchesStatus = when (selectedStatusFilter) {
                "PUBLISHED" -> img.status == "PUBLISHED"
                "DRAFT" -> img.status == "DRAFT"
                "FEATURED" -> img.featured
                "TRENDING" -> img.trending
                else -> true
            }
            val matchesCat = selectedCategoryFilter == "ALL" || img.categoryId == selectedCategoryFilter
            matchesSearch && matchesStatus && matchesCat
        }
    }

    val totalPages = ((filteredImages.size + pageSize - 1) / pageSize).coerceAtLeast(1)
    val safePage = currentPage.coerceIn(0, totalPages - 1)
    val pageItems = remember(filteredImages, safePage) {
        filteredImages.drop(safePage * pageSize).take(pageSize)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SectionHeaderBar(
            sectionNumber = 2,
            title = "Image & Prompt Library Management",
            subtitle = "Manage AI photo inspiration items, generation prompts, categories, display order, and publication states via direct external URLs.",
            trailingContent = {
                if (activeRole.canManageImages) {
                    Button(
                        onClick = {
                            editingImage = null
                            showEditorModal = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VioletPrimary,
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier.testTag("add_new_image_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add New URL Image")
                    }
                }
            }
        )

        if (!activeRole.canManageImages) {
            PermissionGuardBanner(
                requiredRoleLabel = "Owner or Administrator",
                currentRoleLabel = activeRole.displayName
            )
        }

        UrlOnlyComplianceBanner()

        // Search & Filter Controls
        Card(
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
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        currentPage = 0
                    },
                    label = { Text("Search by Image Name, Short Slug, Prompt Keywords, or Category") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("image_search_input")
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STATUS:",
                        style = MaterialTheme.typography.labelMedium,
                        color = ChampagneLight
                    )
                    listOf("ALL", "PUBLISHED", "DRAFT", "FEATURED", "TRENDING").forEach { filter ->
                        FilterChip(
                            selected = selectedStatusFilter == filter,
                            onClick = {
                                selectedStatusFilter = filter
                                currentPage = 0
                            },
                            label = { Text(filter) }
                        )
                    }
                }

                if (categories.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CATEGORY:",
                            style = MaterialTheme.typography.labelMedium,
                            color = VioletBright
                        )
                        FilterChip(
                            selected = selectedCategoryFilter == "ALL",
                            onClick = {
                                selectedCategoryFilter = "ALL"
                                currentPage = 0
                            },
                            label = { Text("All Categories") }
                        )
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryFilter == cat.id,
                                onClick = {
                                    selectedCategoryFilter = cat.id
                                    currentPage = 0
                                },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                }
            }
        }

        // Paginated 10-Column Image Management Table
        Card(
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Image Management Table (${filteredImages.size} matching records)",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { if (safePage > 0) currentPage = safePage - 1 },
                            enabled = safePage > 0
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Page")
                        }
                        Text(
                            text = "Page ${safePage + 1} of $totalPages",
                            style = MaterialTheme.typography.labelMedium,
                            color = ChampagneLight
                        )
                        IconButton(
                            onClick = { if (safePage < totalPages - 1) currentPage = safePage + 1 },
                            enabled = safePage < totalPages - 1
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Page")
                        }
                    }
                }

                if (pageItems.isEmpty()) {
                    EmptyDataStateCard(
                        title = "No Image Records Found",
                        message = if (categories.isEmpty()) {
                            "First create at least one Category in the Categories module, then add images using direct external URLs (.jpg, .jpeg, .png, .webp)."
                        } else {
                            "Click 'Add New URL Image' to add an AI Photo Inspiration & Prompt record via direct external URL."
                        },
                        icon = Icons.Default.Image,
                        actionLabel = if (categories.isEmpty()) "Create Category First" else if (activeRole.canManageImages) "Add New URL Image" else null,
                        onActionClick = {
                            if (categories.isEmpty()) {
                                onNavigateToCategories()
                            } else {
                                editingImage = null
                                showEditorModal = true
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Column(
                            modifier = Modifier.width(1180.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 10-Column Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ObsidianBg)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TableHeaderCell("THUMBNAIL", Modifier.width(74.dp))
                                TableHeaderCell("IMAGE NAME & SLUG", Modifier.width(210.dp))
                                TableHeaderCell("CATEGORY", Modifier.width(130.dp))
                                TableHeaderCell("VIEWS", Modifier.width(78.dp))
                                TableHeaderCell("LIKES", Modifier.width(78.dp))
                                TableHeaderCell("COMMENTS", Modifier.width(90.dp))
                                TableHeaderCell("UNLOCKS", Modifier.width(90.dp))
                                TableHeaderCell("STATUS & FLAGS", Modifier.width(150.dp))
                                TableHeaderCell("CREATED DATE", Modifier.width(110.dp))
                                TableHeaderCell("ACTIONS", Modifier.width(170.dp))
                            }

                            pageItems.forEach { img ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(GraphiteElevated)
                                        .border(1.dp, GraphiteBorder, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.width(74.dp)) {
                                        UrlThumbnailBox(imageUrl = img.imageUrl, size = 54.dp)
                                    }
                                    Column(
                                        modifier = Modifier
                                            .width(210.dp)
                                            .padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = img.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${img.shortName} • Order #${img.displayOrder}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ChampagneLight,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = img.deepLink,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Box(modifier = Modifier.width(130.dp)) {
                                        StatusPill(label = img.categoryName.ifBlank { "General" }, tone = "VIOLET")
                                    }
                                    Text(
                                        text = img.viewsCount.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        modifier = Modifier.width(78.dp)
                                    )
                                    Text(
                                        text = img.likesCount.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = StatusRose,
                                        modifier = Modifier.width(78.dp)
                                    )
                                    Text(
                                        text = img.commentsCount.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = VioletBright,
                                        modifier = Modifier.width(90.dp)
                                    )
                                    Text(
                                        text = img.promptUnlocksCount.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = StatusEmerald,
                                        modifier = Modifier.width(90.dp)
                                    )
                                    Column(
                                        modifier = Modifier.width(150.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        StatusPill(
                                            label = img.status,
                                            tone = if (img.status == "PUBLISHED") "SUCCESS" else "WARNING"
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (img.featured) StatusPill(label = "FEATURED", tone = "GOLD")
                                            if (img.trending) StatusPill(label = "TRENDING", tone = "VIOLET")
                                        }
                                    }
                                    Text(
                                        text = formatShortDate(img.createdAtMillis),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        modifier = Modifier.width(110.dp)
                                    )
                                    Row(
                                        modifier = Modifier.width(170.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { previewingImage = img },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Visibility,
                                                contentDescription = "Preview Detail",
                                                tint = TextPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        if (activeRole.canManageImages) {
                                            IconButton(
                                                onClick = {
                                                    editingImage = img
                                                    showEditorModal = true
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Image",
                                                    tint = ChampagneLight,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    onToggleQuickFlag(img, true, false, false)
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Public,
                                                    contentDescription = "Toggle Publish/Draft",
                                                    tint = if (img.status == "PUBLISHED") StatusEmerald else StatusAmber,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onSendPushForImage(img) },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.NotificationsActive,
                                                    contentDescription = "Send OneSignal Push for Image",
                                                    tint = VioletBright,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { deletingImage = img },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Image",
                                                    tint = StatusRose,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Image Modal (All 10 Required Fields + Live External URL Preview)
    if (showEditorModal) {
        val target = editingImage
        var imageUrl by remember(target) { mutableStateOf(target?.imageUrl.orEmpty()) }
        var shortName by remember(target) { mutableStateOf(target?.shortName.orEmpty()) }
        var title by remember(target) { mutableStateOf(target?.title.orEmpty()) }
        var description by remember(target) { mutableStateOf(target?.description.orEmpty()) }
        var selectedCatId by remember(target, categories) {
            mutableStateOf(target?.categoryId?.ifBlank { categories.firstOrNull()?.id.orEmpty() } ?: categories.firstOrNull()?.id.orEmpty())
        }
        var prompt by remember(target) { mutableStateOf(target?.prompt.orEmpty()) }
        var featured by remember(target) { mutableStateOf(target?.featured ?: false) }
        var trending by remember(target) { mutableStateOf(target?.trending ?: false) }
        var status by remember(target) { mutableStateOf(target?.status ?: "PUBLISHED") }
        var displayOrderText by remember(target) { mutableStateOf((target?.displayOrder ?: (images.size + 1)).toString()) }

        val selectedCategoryObj = categories.firstOrNull { it.id == selectedCatId }
        val previewId = target?.id ?: "img_${shortName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifEmpty { "auto_id" }}"
        val previewDeepLink = UrlAndDeepLinkValidator.buildAutoDeepLink(deepLinkScheme, previewId)

        AlertDialog(
            onDismissRequest = { showEditorModal = false },
            containerColor = GraphiteElevated,
            title = {
                Text(
                    text = if (target == null) "Add New Image (Direct External URL Only)" else "Edit Image & Prompt Metadata",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Automatic Internal Deep Link: $previewDeepLink",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = ChampagneLight
                    )

                    // 1. Direct Image URL + Live Preview
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("1. Direct Image URL (.jpg, .jpeg, .png, .webp)") },
                        placeholder = { Text("https://images.unsplash.com/photo-...jpg") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_image_url_input")
                    )

                    LiveExternalImagePreview(imageUrl = imageUrl, height = 155.dp)

                    // 2. Image Short Name
                    OutlinedTextField(
                        value = shortName,
                        onValueChange = { shortName = it },
                        label = { Text("2. Image Short Name (Identifier Slug)") },
                        placeholder = { Text("cyberpunk_neon_portrait") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_image_short_name_input")
                    )

                    // 3. Image Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("3. Image Title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_image_title_input")
                    )

                    // 4. Image Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("4. Image Description") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 5. Image Category Selector
                    Text(
                        text = "5. Select Image Category",
                        style = MaterialTheme.typography.labelMedium,
                        color = ChampagneLight
                    )
                    if (categories.isEmpty()) {
                        Surface(
                            color = StatusAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, StatusAmber)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = StatusAmber)
                                Text(
                                    text = "No categories exist yet. Create a category first in the Categories tab.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = selectedCatId == cat.id,
                                    onClick = { selectedCatId = cat.id },
                                    label = { Text(cat.name) }
                                )
                            }
                        }
                    }

                    // 6. Image Generation Prompt
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text("6. AI Image Generation Prompt") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_image_prompt_input")
                    )

                    // 7. Featured Status & 8. Trending Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("7. Featured Status", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = featured, onCheckedChange = { featured = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("8. Trending Status", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = trending, onCheckedChange = { trending = it })
                    }

                    // 9. Published / Draft Status
                    Text(
                        text = "9. Publication Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = ChampagneLight
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("PUBLISHED", "DRAFT").forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st) }
                            )
                        }
                    }

                    // 10. Display Order
                    OutlinedTextField(
                        value = displayOrderText,
                        onValueChange = { displayOrderText = it.filter(Char::isDigit) },
                        label = { Text("10. Display Order") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveImage(
                            target?.id,
                            imageUrl,
                            shortName,
                            title,
                            description,
                            selectedCatId,
                            selectedCategoryObj?.name ?: "General",
                            prompt,
                            featured,
                            trending,
                            status,
                            displayOrderText.toIntOrNull() ?: 1
                        ) {
                            showEditorModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                    modifier = Modifier.testTag("save_image_submit_button")
                ) {
                    Text(if (target == null) "Save Image to Library" else "Update Image")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditorModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Image Detail Preview Modal
    if (previewingImage != null) {
        val img = previewingImage!!
        AlertDialog(
            onDismissRequest = { previewingImage = null },
            containerColor = GraphiteElevated,
            title = {
                Text(img.title, color = TextPrimary, style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiveExternalImagePreview(imageUrl = img.imageUrl, height = 200.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(label = img.status, tone = if (img.status == "PUBLISHED") "SUCCESS" else "WARNING")
                        StatusPill(label = img.categoryName, tone = "VIOLET")
                        if (img.featured) StatusPill(label = "FEATURED", tone = "GOLD")
                        if (img.trending) StatusPill(label = "TRENDING", tone = "VIOLET")
                    }
                    Text("Document ID: ${img.id}", fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelSmall, color = ChampagneLight)
                    Text("Auto Deep Link: ${img.deepLink}", fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelSmall, color = StatusEmerald)
                    Text("Verified App Link: ${img.verifiedAppLink}", fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text("Short Name: ${img.shortName} • Display Order: #${img.displayOrder}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    if (img.description.isNotBlank()) {
                        Text(img.description, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                    Surface(
                        color = ObsidianBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GraphiteBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("GENERATION PROMPT", style = MaterialTheme.typography.labelSmall, color = ChampagneLight)
                            Text(img.prompt, fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                    }
                    Text(
                        text = "Telemetry: ${img.viewsCount} Views (${img.uniqueViewsCount} unique) • ${img.likesCount} Likes • ${img.commentsCount} Comments • ${img.promptUnlocksCount} Prompt Unlocks • ${img.sharesCount} Shares",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "Created: ${formatTimestamp(img.createdAtMillis)} • Updated: ${formatTimestamp(img.updatedAtMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            },
            confirmButton = {
                if (activeRole.canSendNotifications) {
                    Button(
                        onClick = {
                            val target = img
                            previewingImage = null
                            onSendPushForImage(target)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Push Notification")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { previewingImage = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (deletingImage != null) {
        val target = deletingImage!!
        DestructiveConfirmationDialog(
            title = "Delete Image Record?",
            message = "Are you sure you want to permanently delete '${target.title}' (${target.id}) from the shared prompt library? This action is logged and cannot be undone.",
            onConfirm = { onDeleteImage(target) },
            onDismiss = { deletingImage = null }
        )
    }
}

@Composable
private fun TableHeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = ChampagneLight,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

// ============================================================================
// MODULE 3: CATEGORY MANAGEMENT (WITH MIGRATION / DELETION PROTECTION)
// ============================================================================
@Composable
fun CategoryManagementScreen(
    categories: List<PromptCategoryEntity>,
    images: List<PromptImageEntity>,
    activeRole: AdminRole,
    onSaveCategory: (
        existingId: String?,
        name: String,
        description: String,
        thumbnailUrl: String,
        displayOrder: Int,
        active: Boolean,
        onSuccess: () -> Unit
    ) -> Unit,
    onDeleteCategoryProtected: (
        category: PromptCategoryEntity,
        migrateToCategory: PromptCategoryEntity?,
        deleteContainedImages: Boolean,
        onSuccess: () -> Unit
    ) -> Unit
) {
    var showCategoryEditor by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<PromptCategoryEntity?>(null) }
    var deletingCategory by remember { mutableStateOf<PromptCategoryEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SectionHeaderBar(
            sectionNumber = 3,
            title = "Category Management",
            subtitle = "Organize prompt collections with direct thumbnail URLs, display ordering, active/inactive toggles, and accidental-deletion protection.",
            trailingContent = {
                if (activeRole.canManageCategories) {
                    Button(
                        onClick = {
                            editingCategory = null
                            showCategoryEditor = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                        modifier = Modifier.testTag("add_category_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Category")
                    }
                }
            }
        )

        if (!activeRole.canManageCategories) {
            PermissionGuardBanner(
                requiredRoleLabel = "Owner or Administrator",
                currentRoleLabel = activeRole.displayName
            )
        }

        UrlOnlyComplianceBanner()

        if (categories.isEmpty()) {
            EmptyDataStateCard(
                title = "No Categories Defined Yet",
                message = "Create categories (e.g., Cinematic Portraits, Cyberpunk, Architectural Luxury) using direct external thumbnail URLs.",
                icon = Icons.Default.Category,
                actionLabel = if (activeRole.canManageCategories) "Create First Category" else null,
                onActionClick = {
                    editingCategory = null
                    showCategoryEditor = true
                }
            )
        } else {
            categories.forEach { cat ->
                val count = images.count { it.categoryId == cat.id }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                    border = BorderStroke(1.dp, GraphiteBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        UrlThumbnailBox(imageUrl = cat.thumbnailUrl, size = 64.dp)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                                StatusPill(
                                    label = if (cat.active) "ACTIVE" else "INACTIVE",
                                    tone = if (cat.active) "SUCCESS" else "WARNING"
                                )
                                MonospaceBadge("Order #${cat.displayOrder}")
                                StatusPill(label = "$count Images", tone = "VIOLET")
                            }
                            if (cat.description.isNotBlank()) {
                                Text(
                                    text = cat.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "ID: ${cat.id} • Thumbnail URL: ${cat.thumbnailUrl}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (activeRole.canManageCategories) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        editingCategory = cat
                                        showCategoryEditor = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Category", tint = ChampagneLight)
                                }
                                IconButton(
                                    onClick = { deletingCategory = cat }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Category", tint = StatusRose)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCategoryEditor) {
        val target = editingCategory
        var name by remember(target) { mutableStateOf(target?.name.orEmpty()) }
        var description by remember(target) { mutableStateOf(target?.description.orEmpty()) }
        var thumbnailUrl by remember(target) { mutableStateOf(target?.thumbnailUrl.orEmpty()) }
        var displayOrderText by remember(target) { mutableStateOf((target?.displayOrder ?: (categories.size + 1)).toString()) }
        var active by remember(target) { mutableStateOf(target?.active ?: true) }

        AlertDialog(
            onDismissRequest = { showCategoryEditor = false },
            containerColor = GraphiteElevated,
            title = {
                Text(
                    text = if (target == null) "Add New Category" else "Edit Category",
                    color = TextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_name_input")
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Category Description") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = thumbnailUrl,
                        onValueChange = { thumbnailUrl = it },
                        label = { Text("Direct Thumbnail Image URL (.jpg, .png, .webp)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_thumbnail_url_input")
                    )
                    LiveExternalImagePreview(imageUrl = thumbnailUrl, height = 140.dp)
                    OutlinedTextField(
                        value = displayOrderText,
                        onValueChange = { displayOrderText = it.filter(Char::isDigit) },
                        label = { Text("Display Order") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active in User Application", color = TextPrimary)
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveCategory(
                            target?.id,
                            name,
                            description,
                            thumbnailUrl,
                            displayOrderText.toIntOrNull() ?: 1,
                            active
                        ) {
                            showCategoryEditor = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                    modifier = Modifier.testTag("save_category_submit_button")
                ) {
                    Text("Save Category")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCategoryEditor = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (deletingCategory != null) {
        val cat = deletingCategory!!
        val containedImagesCount = images.count { it.categoryId == cat.id }
        val otherCategories = categories.filter { it.id != cat.id }
        var selectedMigrationTarget by remember(cat) { mutableStateOf(otherCategories.firstOrNull()) }
        var resolutionMode by remember(cat) {
            mutableStateOf(if (otherCategories.isNotEmpty()) "MIGRATE" else "CASCADE_DELETE")
        }

        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            containerColor = GraphiteElevated,
            title = {
                Text("Delete Category '${cat.name}'?", color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (containedImagesCount == 0) {
                        Text(
                            text = "This category currently contains 0 images and can be safely removed.",
                            color = TextSecondary
                        )
                    } else {
                        Surface(
                            color = StatusAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StatusAmber)
                        ) {
                            Text(
                                text = "Safety Protection: '${cat.name}' contains $containedImagesCount image(s). Choose an explicit migration or deletion resolution:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        if (otherCategories.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { resolutionMode = "MIGRATE" }
                            ) {
                                RadioButton(
                                    selected = resolutionMode == "MIGRATE",
                                    onClick = { resolutionMode = "MIGRATE" }
                                )
                                Text("Migrate $containedImagesCount image(s) to another category", color = TextPrimary)
                            }
                            if (resolutionMode == "MIGRATE") {
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    otherCategories.forEach { candidate ->
                                        FilterChip(
                                            selected = selectedMigrationTarget?.id == candidate.id,
                                            onClick = { selectedMigrationTarget = candidate },
                                            label = { Text(candidate.name) }
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { resolutionMode = "CASCADE_DELETE" }
                        ) {
                            RadioButton(
                                selected = resolutionMode == "CASCADE_DELETE",
                                onClick = { resolutionMode = "CASCADE_DELETE" }
                            )
                            Text("Permanently delete category AND its $containedImagesCount image(s)", color = StatusRose)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategoryProtected(
                            cat,
                            if (containedImagesCount > 0 && resolutionMode == "MIGRATE") selectedMigrationTarget else null,
                            containedImagesCount > 0 && resolutionMode == "CASCADE_DELETE"
                        ) {
                            deletingCategory = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRose)
                ) {
                    Text("Confirm Category Resolution")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
