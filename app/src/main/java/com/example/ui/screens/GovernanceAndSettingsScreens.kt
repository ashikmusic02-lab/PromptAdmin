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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.FirebaseConnectionStatus
import com.example.model.ActivityLogEntity
import com.example.model.AdminAccountEntity
import com.example.model.AdminRole
import com.example.model.AppContentSettingsEntity
import com.example.model.PromptCategoryEntity
import com.example.model.PromptImageEntity
import com.example.ui.components.EmptyDataStateCard
import com.example.ui.components.LiveExternalImagePreview
import com.example.ui.components.MonospaceBadge
import com.example.ui.components.PermissionGuardBanner
import com.example.ui.components.SectionHeaderBar
import com.example.ui.components.StatusPill
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
import com.example.ui.theme.StatusRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.theme.VioletContainer
import com.example.ui.theme.VioletPrimary
import com.example.viewmodel.AdminSession

// ============================================================================
// MODULE 11: APP CONTENT MANAGEMENT
// ============================================================================
@Composable
fun AppContentManagementScreen(
    settings: AppContentSettingsEntity,
    images: List<PromptImageEntity>,
    categories: List<PromptCategoryEntity>,
    activeRole: AdminRole,
    onSaveSettings: (AppContentSettingsEntity) -> Unit
) {
    var appName by remember(settings) { mutableStateOf(settings.appName) }
    var appLogoUrl by remember(settings) { mutableStateOf(settings.appLogoUrl) }
    var featuredSubtitle by remember(settings) { mutableStateOf(settings.featuredBannerSubtitle) }
    var showFeatured by remember(settings) { mutableStateOf(settings.showFeaturedSection) }
    var showTrending by remember(settings) { mutableStateOf(settings.showTrendingSection) }
    var showLatest by remember(settings) { mutableStateOf(settings.showLatestSection) }
    var showPopularCategories by remember(settings) { mutableStateOf(settings.showPopularCategoriesSection) }
    var maxFeaturedText by remember(settings) { mutableStateOf(settings.maxFeaturedItems.toString()) }
    var maxTrendingText by remember(settings) { mutableStateOf(settings.maxTrendingItems.toString()) }
    var aboutInfo by remember(settings) { mutableStateOf(settings.aboutInformation) }
    var privacyUrl by remember(settings) { mutableStateOf(settings.privacyPolicyUrl) }
    var termsUrl by remember(settings) { mutableStateOf(settings.termsAndConditionsUrl) }
    var contactEmail by remember(settings) { mutableStateOf(settings.contactEmail) }
    var playStoreUrl by remember(settings) { mutableStateOf(settings.playStoreUrl) }

    val featuredCount = images.count { it.status == "PUBLISHED" && it.featured }
    val trendingCount = images.count { it.status == "PUBLISHED" && it.trending }
    val latestCount = images.count { it.status == "PUBLISHED" }
    val activeCategoryCount = categories.count { it.active }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SectionHeaderBar(
            sectionNumber = 11,
            title = "App Content & Home Feed Configuration",
            subtitle = "Manage User App branding, direct Logo URL, home page sections (Featured, Trending, Latest, Popular Categories), legal URLs, and Play Store metadata.",
            trailingContent = {
                if (activeRole.canManageAppContent) {
                    Button(
                        onClick = {
                            onSaveSettings(
                                settings.copy(
                                    appName = appName.trim(),
                                    appLogoUrl = appLogoUrl.trim(),
                                    featuredBannerSubtitle = featuredSubtitle.trim(),
                                    showFeaturedSection = showFeatured,
                                    showTrendingSection = showTrending,
                                    showLatestSection = showLatest,
                                    showPopularCategoriesSection = showPopularCategories,
                                    maxFeaturedItems = (maxFeaturedText.toIntOrNull() ?: 10).coerceAtLeast(1),
                                    maxTrendingItems = (maxTrendingText.toIntOrNull() ?: 16).coerceAtLeast(1),
                                    aboutInformation = aboutInfo.trim(),
                                    privacyPolicyUrl = privacyUrl.trim(),
                                    termsAndConditionsUrl = termsUrl.trim(),
                                    contactEmail = contactEmail.trim(),
                                    playStoreUrl = playStoreUrl.trim()
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                        modifier = Modifier.testTag("save_app_content_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save App Content")
                    }
                }
            }
        )

        if (!activeRole.canManageAppContent) {
            PermissionGuardBanner(
                requiredRoleLabel = "Owner or Administrator",
                currentRoleLabel = activeRole.displayName
            )
        }

        // Live Feed Curation Summary
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
                    text = "Synchronized User App Home Sections",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatusPill(label = "Featured Images: $featuredCount", tone = "GOLD")
                    StatusPill(label = "Trending Images: $trendingCount", tone = "VIOLET")
                    StatusPill(label = "Latest Published: $latestCount", tone = "SUCCESS")
                    StatusPill(label = "Popular Categories: $activeCategoryCount", tone = "CYAN")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Featured Carousel on Home Page", color = TextPrimary)
                    Switch(checked = showFeatured, onCheckedChange = { if (activeRole.canManageAppContent) showFeatured = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Trending Prompts Section", color = TextPrimary)
                    Switch(checked = showTrending, onCheckedChange = { if (activeRole.canManageAppContent) showTrending = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Latest Images Feed", color = TextPrimary)
                    Switch(checked = showLatest, onCheckedChange = { if (activeRole.canManageAppContent) showLatest = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Popular Categories Bar", color = TextPrimary)
                    Switch(checked = showPopularCategories, onCheckedChange = { if (activeRole.canManageAppContent) showPopularCategories = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = maxFeaturedText,
                        onValueChange = { maxFeaturedText = it.filter(Char::isDigit) },
                        enabled = activeRole.canManageAppContent,
                        label = { Text("Max Featured Carousel Items") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxTrendingText,
                        onValueChange = { maxTrendingText = it.filter(Char::isDigit) },
                        enabled = activeRole.canManageAppContent,
                        label = { Text("Max Trending Section Items") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Branding, Legal & Store Links
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
                    text = "Branding, Legal Policies & Store Metadata",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    enabled = activeRole.canManageAppContent,
                    label = { Text("User Application Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = appLogoUrl,
                    onValueChange = { appLogoUrl = it },
                    enabled = activeRole.canManageAppContent,
                    label = { Text("Direct App Logo URL (.png, .webp, .jpg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (appLogoUrl.isNotBlank()) {
                    LiveExternalImagePreview(imageUrl = appLogoUrl, height = 120.dp)
                }

                OutlinedTextField(
                    value = featuredSubtitle,
                    onValueChange = { featuredSubtitle = it },
                    enabled = activeRole.canManageAppContent,
                    label = { Text("Featured Hero Tagline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = aboutInfo,
                    onValueChange = { aboutInfo = it },
                    enabled = activeRole.canManageAppContent,
                    label = { Text("About Application Information") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = privacyUrl,
                    onValueChange = { privacyUrl = it },
                    enabled = activeRole.canManageAppContent,
                    label = { Text("Privacy Policy URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = termsUrl,
                    onValueChange = { termsUrl = it },
                    enabled = activeRole.canManageAppContent,
                    label = { Text("Terms & Conditions URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contactEmail,
                    onValueChange = { contactEmail = it },
                    enabled = activeRole.canManageAppContent,
                    label = { Text("Support & Contact Information") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = playStoreUrl,
                    onValueChange = { playStoreUrl = it },
                    enabled = activeRole.canManageAppContent,
                    label = { Text("Google Play Store Listing URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ============================================================================
// MODULE 12: ADMINISTRATOR & PERMISSION MANAGEMENT
// ============================================================================
@Composable
fun AdministratorManagementScreen(
    adminAccounts: List<AdminAccountEntity>,
    session: AdminSession,
    onSaveAdmin: (uid: String, email: String, displayName: String, role: AdminRole, active: Boolean) -> Unit,
    onRemoveAdmin: (AdminAccountEntity) -> Unit,
    onSwitchPreviewRole: (AdminRole) -> Unit
) {
    var showAddAdminDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SectionHeaderBar(
            sectionNumber = 12,
            title = "Administrator & RBAC Permission Management",
            subtitle = "Enforce role-based access control across Owner, Administrator, and Moderator clearances. Synchronized with Firestore adminUsers/{uid} and enforced by Security Rules.",
            trailingContent = {
                if (session.role.canManageAdmins) {
                    Button(
                        onClick = { showAddAdminDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                        modifier = Modifier.testTag("add_admin_account_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Authorize Administrator")
                    }
                }
            }
        )

        // Active Session & RBAC Clearance Switcher
        Card(
            colors = CardDefaults.cardColors(containerColor = GraphiteCard),
            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.55f)),
            shape = RoundedCornerShape(16.dp)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Active Administrator Session: ${session.displayName} (${session.email})",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "UID: ${session.uid} • Auth Method: ${session.authMethod}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    StatusPill(label = session.role.badgeLabel, tone = "GOLD")
                }

                Text(
                    text = "Test Role-Based Access Enforcement (Switch Active Clearance):",
                    style = MaterialTheme.typography.labelMedium,
                    color = ChampagneLight
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminRole.entries.forEach { roleOption ->
                        FilterChip(
                            selected = session.role == roleOption,
                            onClick = { onSwitchPreviewRole(roleOption) },
                            label = { Text("Preview as ${roleOption.displayName}") }
                        )
                    }
                }
            }
        }

        // Permission Matrix
        Card(
            colors = CardDefaults.cardColors(containerColor = GraphiteCard),
            border = BorderStroke(1.dp, GraphiteBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Role-Based Permission Matrix (Backend Enforced in firestore.rules)",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                PermissionRow("Image & Prompt CRUD (URL-Only)", "Allowed", "Allowed", "Read-Only")
                PermissionRow("Category Management & Migration", "Allowed", "Allowed", "Read-Only")
                PermissionRow("User Account Disable / Restore", "Allowed", "Allowed", "Read-Only")
                PermissionRow("Comment Moderation & Admin Reply", "Allowed", "Allowed", "Allowed")
                PermissionRow("Google AdMob Unit & Policy Config", "Allowed", "Allowed", "Read-Only")
                PermissionRow("OneSignal Push Notification Dispatch", "Allowed", "Allowed", "Denied")
                PermissionRow("Administrator & Role Provisioning", "Allowed", "Denied", "Denied")
            }
        }

        // Registered Administrators List
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
                    text = "Authorized Administrator Accounts (${adminAccounts.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )

                adminAccounts.forEach { adm ->
                    val admRole = runCatching { AdminRole.valueOf(adm.role) }.getOrDefault(AdminRole.MODERATOR)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianBg, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = adm.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                StatusPill(label = admRole.displayName.uppercase(), tone = admRole.name)
                                StatusPill(
                                    label = if (adm.active) "ACTIVE" else "REVOKED",
                                    tone = if (adm.active) "SUCCESS" else "DANGER"
                                )
                            }
                            Text(
                                text = "${adm.email} • UID: ${adm.uid}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        if (session.role.canManageAdmins && adm.uid != session.uid) {
                            IconButton(onClick = { onRemoveAdmin(adm) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Admin", tint = StatusRose)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddAdminDialog) {
        var uid by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var displayName by remember { mutableStateOf("") }
        var selectedRole by remember { mutableStateOf(AdminRole.ADMINISTRATOR) }
        var active by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddAdminDialog = false },
            containerColor = GraphiteElevated,
            title = { Text("Authorize Administrator Account", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Administrator Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uid,
                        onValueChange = { uid = it },
                        label = { Text("Firebase Auth UID (Optional — auto-derived if blank)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Select Role:", style = MaterialTheme.typography.labelMedium, color = ChampagneLight)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminRole.entries.forEach { r ->
                            FilterChip(
                                selected = selectedRole == r,
                                onClick = { selectedRole = r },
                                label = { Text(r.displayName) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Account Active", color = TextPrimary)
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveAdmin(uid, email, displayName, selectedRole, active)
                        showAddAdminDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
                ) {
                    Text("Save Administrator")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddAdminDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PermissionRow(capability: String, owner: String, admin: String, mod: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianBg, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = capability, style = MaterialTheme.typography.bodySmall, color = TextPrimary, modifier = Modifier.weight(1.6f))
        Text(text = "Owner: $owner", style = MaterialTheme.typography.labelSmall, color = ChampagneLight, modifier = Modifier.weight(0.8f))
        Text(text = "Admin: $admin", style = MaterialTheme.typography.labelSmall, color = VioletBright, modifier = Modifier.weight(0.8f))
        Text(text = "Mod: $mod", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.weight(0.8f))
    }
}

// ============================================================================
// MODULE 13: SECURITY & ACTIVITY LOGS
// ============================================================================
@Composable
fun SecurityActivityLogsScreen(
    logs: List<ActivityLogEntity>
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredLogs = remember(logs, searchQuery) {
        if (searchQuery.isBlank()) logs
        else logs.filter {
            it.actionType.contains(searchQuery, ignoreCase = true) ||
                it.summary.contains(searchQuery, ignoreCase = true) ||
                it.adminEmail.contains(searchQuery, ignoreCase = true) ||
                it.targetEntity.contains(searchQuery, ignoreCase = true)
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
            sectionNumber = 13,
            title = "Security & Immutable Activity Logs",
            subtitle = "Complete tamper-evident audit trail of all administrative mutations across images, categories, users, comments, AdMob, OneSignal, and RBAC."
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Filter Audit Trail by Action Type, Admin Email, Target Document, or Summary") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (filteredLogs.isEmpty()) {
            EmptyDataStateCard(
                title = "No Matching Audit Entries",
                message = "Every administrative operation is automatically recorded in adminActivityLogs.",
                icon = Icons.Default.History
            )
        } else {
            filteredLogs.forEach { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                    border = BorderStroke(1.dp, GraphiteBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                StatusPill(label = log.actionType, tone = "VIOLET")
                                MonospaceBadge(log.targetEntity)
                            }
                            Text(
                                text = formatTimestamp(log.createdAtMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        Text(
                            text = log.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Actor: ${log.adminEmail} (${log.adminRole}) • UID: ${log.adminUid}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChampagneLight
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// MODULE 14: SETTINGS & EXTERNAL SERVICE INTEGRATION GUIDE
// ============================================================================
@Composable
fun PlatformSettingsScreen(
    settings: AppContentSettingsEntity,
    connectionStatus: FirebaseConnectionStatus,
    isSyncing: Boolean,
    activeRole: AdminRole,
    onSaveSettings: (AppContentSettingsEntity) -> Unit,
    onTriggerFirestoreSync: () -> Unit
) {
    var deepLinkScheme by remember(settings) { mutableStateOf(settings.deepLinkScheme) }
    var verifiedDomainHost by remember(settings) { mutableStateOf(settings.verifiedDomainHost) }
    var oneSignalAppId by remember(settings) { mutableStateOf(settings.oneSignalAppId) }
    var oneSignalFnUrl by remember(settings) { mutableStateOf(settings.oneSignalFunctionUrl) }
    var adminAuthFnUrl by remember(settings) { mutableStateOf(settings.adminAuthFunctionUrl) }
    var requirePreModeration by remember(settings) { mutableStateOf(settings.requireCommentPreModeration) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SectionHeaderBar(
            sectionNumber = 14,
            title = "System Settings, Firebase Sync & Credentials Guide",
            subtitle = "Configure deep-link schemes, OneSignal server endpoints, and synchronize local staging data with your shared Firebase project once app/google-services.json is uploaded.",
            trailingContent = {
                if (activeRole.canManageAppContent) {
                    Button(
                        onClick = {
                            onSaveSettings(
                                settings.copy(
                                    deepLinkScheme = deepLinkScheme.trim().ifEmpty { "promptvault" },
                                    verifiedDomainHost = verifiedDomainHost.trim().ifEmpty { "promptvault.app" },
                                    oneSignalAppId = oneSignalAppId.trim(),
                                    oneSignalFunctionUrl = oneSignalFnUrl.trim(),
                                    adminAuthFunctionUrl = adminAuthFnUrl.trim(),
                                    requireCommentPreModeration = requirePreModeration
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                        modifier = Modifier.testTag("save_system_settings_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Settings")
                    }
                }
            }
        )

        // 1. Firebase google-services.json Readiness & Two-Way Sync Card
        Card(
            colors = CardDefaults.cardColors(containerColor = GraphiteCard),
            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.6f)),
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
                        text = "Shared Firebase Backend & google-services.json Status",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    StatusPill(
                        label = if (connectionStatus.isGoogleServicesPresent) "LIVE FIRESTORE READY" else "READY FOR JSON UPLOAD",
                        tone = if (connectionStatus.isGoogleServicesPresent) "SUCCESS" else "GOLD"
                    )
                }

                Text(
                    text = connectionStatus.databaseModeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChampagneLight
                )

                Surface(
                    color = ObsidianBg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GraphiteBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "ADD THIS ANDROID ADMIN APP IN YOUR FIREBASE CONSOLE:",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChampagneLight
                        )
                        Text(
                            text = "• Android Package Name: com.aistudio.promptadmin.vqxkrm",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "• Debug SHA-1: 65:FD:A0:D0:AE:1E:18:3A:AC:77:B5:47:78:05:00:3C:E0:C2:E1:CF",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "• Debug SHA-256: F8:65:1C:AA:7D:4B:7D:96:73:83:BC:2E:F0:91:D0:26:CA:22:7E:27:26:39:5B:CD:83:9D:53:AA:A6:D1:11:38",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "• Upload Destination in File Explorer: /app/google-services.json",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusEmerald
                        )
                    }
                }

                Button(
                    onClick = onTriggerFirestoreSync,
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = TextPrimary
                        )
                    } else {
                        Icon(Icons.Default.CloudSync, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Push Local Staging Data & Synchronize All Collections with Firestore")
                }
            }
        }

        // 2. Deep Link & Server Endpoint Configuration
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
                    text = "Deep Link Scheme & Privileged Cloud Function Endpoints",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = deepLinkScheme,
                        onValueChange = { deepLinkScheme = it },
                        label = { Text("Internal Deep Link Scheme (e.g. promptvault)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = verifiedDomainHost,
                        onValueChange = { verifiedDomainHost = it },
                        label = { Text("Verified App Links Host (e.g. promptvault.app)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = oneSignalAppId,
                    onValueChange = { oneSignalAppId = it },
                    label = { Text("OneSignal Public App ID (UUID)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = oneSignalFnUrl,
                    onValueChange = { oneSignalFnUrl = it },
                    label = { Text("Secure OneSignal Cloud Function URL (sendOneSignalNotification)") },
                    placeholder = { Text("https://us-central1-your-project.cloudfunctions.net/sendOneSignalNotification") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = adminAuthFnUrl,
                    onValueChange = { adminAuthFnUrl = it },
                    label = { Text("Firebase Auth User Admin Cloud Function URL (manageUserAccount)") },
                    placeholder = { Text("https://us-central1-your-project.cloudfunctions.net/manageUserAccount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Require Pre-Moderation for New User Comments", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                        Text("When enabled, new user comments enter PENDING state until approved by a Moderator/Admin.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = requirePreModeration, onCheckedChange = { requirePreModeration = it })
                }
            }
        }

        // 3. Firebase Free-Tier vs Blaze Plan Considerations
        Card(
            colors = CardDefaults.cardColors(containerColor = GraphiteCard),
            border = BorderStroke(1.dp, GraphiteBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Firebase Free-Tier (Spark) vs. Paid (Blaze) Transparency",
                    style = MaterialTheme.typography.titleLarge,
                    color = ChampagneLight
                )
                Text(
                    text = "• Free Tier (Spark Plan) Included: Cloud Firestore (50,000 reads/day, 20,000 writes/day, 1 GiB storage), Firebase Authentication (Google Sign-In), and Firestore Security Rules (/firestore.rules). All image and category records store external URLs only, consuming zero Firebase Storage bandwidth.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
                Text(
                    text = "• Requires Blaze (Pay-As-You-Go) Plan if Deployed: Deploying outbound HTTPS Cloud Functions (/cloud-functions/onesignal-notify/index.js) and Google Cloud Secret Manager (for ONESIGNAL_REST_API_KEY) requires the Firebase Blaze plan. No paid services are enabled automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
