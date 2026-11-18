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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AdminRole
import com.example.model.AppUserEntity
import com.example.model.CommentEntity
import com.example.model.PromptImageEntity
import com.example.ui.components.DestructiveConfirmationDialog
import com.example.ui.components.EmptyDataStateCard
import com.example.ui.components.MonospaceBadge
import com.example.ui.components.PermissionGuardBanner
import com.example.ui.components.SectionHeaderBar
import com.example.ui.components.StatusPill
import com.example.ui.components.formatShortDate
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneLight
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.GraphiteElevated
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

@Composable
fun UserManagementScreen(
    users: List<AppUserEntity>,
    comments: List<CommentEntity>,
    activeRole: AdminRole,
    onUpsertUser: (
        existingUser: AppUserEntity?,
        username: String,
        email: String,
        accountStatus: String,
        moderationNote: String
    ) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") } // ALL, ACTIVE, DISABLED, FLAGGED
    var inspectingUser by remember { mutableStateOf<AppUserEntity?>(null) }
    var showRegisterUserModal by remember { mutableStateOf(false) }

    val filteredUsers = remember(users, searchQuery, statusFilter) {
        users.filter { u ->
            val matchesQuery = searchQuery.isBlank() ||
                u.username.contains(searchQuery, ignoreCase = true) ||
                u.email.contains(searchQuery, ignoreCase = true) ||
                u.id.contains(searchQuery, ignoreCase = true)
            val matchesStatus = statusFilter == "ALL" || u.accountStatus == statusFilter
            matchesQuery && matchesStatus
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
            sectionNumber = 4,
            title = "User Management & Account Administration",
            subtitle = "Inspect user activity, likes, comments, and reports. Disable or restore accounts via Firebase Auth backend administration without exposing passwords.",
            trailingContent = {
                if (activeRole.canManageUsers) {
                    Button(
                        onClick = { showRegisterUserModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                        modifier = Modifier.testTag("register_user_record_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Register User Record")
                    }
                }
            }
        )

        if (!activeRole.canManageUsers) {
            PermissionGuardBanner(
                requiredRoleLabel = "Owner or Administrator",
                currentRoleLabel = activeRole.displayName
            )
        }

        Surface(
            color = ObsidianBg,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, GraphiteBorder)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = ChampagneLight)
                Text(
                    text = "Zero-Password Security Policy: Authentication credentials and password hashes are never exposed to administrators. Account status changes synchronize with users/{uid} and the privileged manageUserAccount Cloud Function.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

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
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by User ID, Username, or Email") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL", "ACTIVE", "DISABLED", "FLAGGED").forEach { st ->
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = st },
                            label = { Text(st) }
                        )
                    }
                }
            }
        }

        if (filteredUsers.isEmpty()) {
            EmptyDataStateCard(
                title = "No User Accounts Found",
                message = "Users registered in the separate Android User Application appear here via the shared Firestore 'users' collection, or you can register a user record for moderation testing.",
                icon = Icons.Default.People,
                actionLabel = if (activeRole.canManageUsers) "Register User Record" else null,
                onActionClick = { showRegisterUserModal = true }
            )
        } else {
            filteredUsers.forEach { user ->
                val userCommentsCount = comments.count {
                    it.userId == user.id || it.username.equals(user.username, ignoreCase = true)
                }
                val userReportedComments = comments.count {
                    (it.userId == user.id || it.username.equals(user.username, ignoreCase = true)) && it.reported
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "@${user.username}",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary
                                    )
                                    StatusPill(
                                        label = user.accountStatus,
                                        tone = when (user.accountStatus) {
                                            "ACTIVE" -> "SUCCESS"
                                            "DISABLED" -> "DANGER"
                                            else -> "WARNING"
                                        }
                                    )
                                    MonospaceBadge(user.id)
                                }
                                Text(
                                    text = "Email: ${user.email.ifBlank { "Not provided by provider" }} • Registered: ${formatShortDate(user.registeredAtMillis)} • Last Activity: ${formatTimestamp(user.lastActivityAtMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { inspectingUser = user },
                                    border = BorderStroke(1.dp, GraphiteBorder)
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Inspect")
                                }
                                if (activeRole.canManageUsers) {
                                    if (user.accountStatus != "DISABLED") {
                                        Button(
                                            onClick = {
                                                onUpsertUser(
                                                    user,
                                                    user.username,
                                                    user.email,
                                                    "DISABLED",
                                                    "Disabled by administrator"
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = StatusRose)
                                        ) {
                                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Disable")
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                onUpsertUser(
                                                    user,
                                                    user.username,
                                                    user.email,
                                                    "ACTIVE",
                                                    "Account restored by administrator"
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = StatusEmerald)
                                        ) {
                                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Restore")
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatusPill(label = "Likes: ${user.likesCount}", tone = "VIOLET")
                            StatusPill(label = "Comments: ${maxOf(user.commentsCount, userCommentsCount.toLong())}", tone = "CYAN")
                            StatusPill(label = "Prompt Unlocks: ${user.unlocksCount}", tone = "GOLD")
                            StatusPill(
                                label = "Reports: ${maxOf(user.reportedCount, userReportedComments)}",
                                tone = if (maxOf(user.reportedCount, userReportedComments) > 0) "DANGER" else "NEUTRAL"
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRegisterUserModal) {
        var username by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("ACTIVE") }
        var note by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRegisterUserModal = false },
            containerColor = GraphiteElevated,
            title = { Text("Register / Sync User Account Record", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email (when available)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ACTIVE", "FLAGGED", "DISABLED").forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Administrative Review Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpsertUser(null, username, email, status, note)
                        showRegisterUserModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
                ) {
                    Text("Save User Record")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRegisterUserModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (inspectingUser != null) {
        val target = inspectingUser!!
        val userComments = comments.filter {
            it.userId == target.id || it.username.equals(target.username, ignoreCase = true)
        }
        AlertDialog(
            onDismissRequest = { inspectingUser = null },
            containerColor = GraphiteElevated,
            title = { Text("User Profile & Activity Review: @${target.username}", color = TextPrimary) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MonospaceBadge("UID: ${target.id}")
                    Text("Email: ${target.email.ifBlank { "Unavailable" }}", color = TextPrimary)
                    Text("Status: ${target.accountStatus}", color = ChampagneLight)
                    Text("Registered: ${formatTimestamp(target.registeredAtMillis)}", color = TextSecondary)
                    Text("Last Activity: ${formatTimestamp(target.lastActivityAtMillis)}", color = TextSecondary)
                    if (target.moderationNote.isNotBlank()) {
                        Text("Moderation Note: ${target.moderationNote}", color = StatusAmber)
                    }
                    HorizontalDivider(color = GraphiteBorder)
                    Text("Recent User Comments (${userComments.size}):", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    if (userComments.isEmpty()) {
                        Text("No comments authored by this user.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    } else {
                        userComments.forEach { cmt ->
                            Surface(
                                color = ObsidianBg,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, GraphiteBorder)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("On '${cmt.imageTitle}' (${cmt.moderationStatus})", style = MaterialTheme.typography.labelSmall, color = ChampagneLight)
                                    Text(cmt.content, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { inspectingUser = null }) {
                    Text("Close")
                }
            }
        )
    }
}

// ============================================================================
// MODULE 5: COMMENT MODERATION DASHBOARD
// ============================================================================
@Composable
fun CommentModerationScreen(
    comments: List<CommentEntity>,
    images: List<PromptImageEntity>,
    activeRole: AdminRole,
    onModerateComment: (
        comment: CommentEntity,
        newStatus: String,
        adminReply: String,
        clearReportFlag: Boolean
    ) -> Unit,
    onDeleteCommentPermanently: (CommentEntity) -> Unit,
    onCreateCommentEntry: (
        image: PromptImageEntity,
        username: String,
        content: String,
        reported: Boolean,
        reportReason: String
    ) -> Unit
) {
    var searchCommentQuery by remember { mutableStateOf("") }
    var searchUserQuery by remember { mutableStateOf("") }
    var selectedImageIdFilter by remember { mutableStateOf("ALL") }
    var selectedModerationFilter by remember { mutableStateOf("ALL") } // ALL, APPROVED, PENDING, HIDDEN, REPORTED

    var replyingComment by remember { mutableStateOf<CommentEntity?>(null) }
    var deletingComment by remember { mutableStateOf<CommentEntity?>(null) }
    var showAddCommentDialog by remember { mutableStateOf(false) }

    val filteredComments = remember(
        comments,
        searchCommentQuery,
        searchUserQuery,
        selectedImageIdFilter,
        selectedModerationFilter
    ) {
        comments.filter { c ->
            val matchesComment = searchCommentQuery.isBlank() ||
                c.content.contains(searchCommentQuery, ignoreCase = true) ||
                c.imageTitle.contains(searchCommentQuery, ignoreCase = true)
            val matchesUser = searchUserQuery.isBlank() ||
                c.username.contains(searchUserQuery, ignoreCase = true) ||
                c.userId.contains(searchUserQuery, ignoreCase = true)
            val matchesImage = selectedImageIdFilter == "ALL" || c.imageId == selectedImageIdFilter
            val matchesStatus = when (selectedModerationFilter) {
                "REPORTED" -> c.reported
                "ALL" -> true
                else -> c.moderationStatus == selectedModerationFilter
            }
            matchesComment && matchesUser && matchesImage && matchesStatus
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
            sectionNumber = 5,
            title = "Comment Moderation & Community Safety",
            subtitle = "Filter comments by image or user, approve pending comments, hide inappropriate content, restore comments, reply as Administrator, and review reported activity.",
            trailingContent = {
                if (images.isNotEmpty()) {
                    Button(
                        onClick = { showAddCommentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                        modifier = Modifier.testTag("log_comment_entry_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Log Comment Record")
                    }
                }
            }
        )

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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = searchCommentQuery,
                        onValueChange = { searchCommentQuery = it },
                        label = { Text("Search Comment Content") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = searchUserQuery,
                        onValueChange = { searchUserQuery = it },
                        label = { Text("Search Username / User ID") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("STATUS:", style = MaterialTheme.typography.labelMedium, color = ChampagneLight)
                    listOf("ALL", "APPROVED", "PENDING", "HIDDEN", "REPORTED").forEach { st ->
                        FilterChip(
                            selected = selectedModerationFilter == st,
                            onClick = { selectedModerationFilter = st },
                            label = { Text(st) }
                        )
                    }
                }

                if (images.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("FILTER BY IMAGE:", style = MaterialTheme.typography.labelMedium, color = VioletBright)
                        FilterChip(
                            selected = selectedImageIdFilter == "ALL",
                            onClick = { selectedImageIdFilter = "ALL" },
                            label = { Text("All Images") }
                        )
                        images.forEach { img ->
                            FilterChip(
                                selected = selectedImageIdFilter == img.id,
                                onClick = { selectedImageIdFilter = img.id },
                                label = { Text(img.title) }
                            )
                        }
                    }
                }
            }
        }

        if (filteredComments.isEmpty()) {
            EmptyDataStateCard(
                title = "No Comments Match Current Filter",
                message = "Comments posted in the User Application synchronize via the shared 'comments' Firestore collection.",
                icon = Icons.Default.Comment
            )
        } else {
            filteredComments.forEach { cmt ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                    border = BorderStroke(
                        1.dp,
                        if (cmt.reported) StatusRose.copy(alpha = 0.65f) else GraphiteBorder
                    ),
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "@${cmt.username}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                StatusPill(label = cmt.moderationStatus, tone = cmt.moderationStatus)
                                if (cmt.reported) {
                                    StatusPill(label = "REPORTED: ${cmt.reportReason.ifBlank { "Flagged" }}", tone = "DANGER")
                                }
                            }
                            Text(
                                text = formatTimestamp(cmt.createdAtMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        Text(
                            text = "Target Image: ${cmt.imageTitle} (${cmt.imageId})",
                            style = MaterialTheme.typography.labelMedium,
                            color = ChampagneLight
                        )

                        Surface(
                            color = ObsidianBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GraphiteBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = cmt.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        if (cmt.adminReply.isNotBlank()) {
                            Surface(
                                color = VioletContainer.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, VioletPrimary.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "OFFICIAL ADMINISTRATOR REPLY (${cmt.moderatedByEmail})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ChampagneLight
                                    )
                                    Text(
                                        text = cmt.adminReply,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        if (cmt.moderatedByEmail.isNotBlank()) {
                            Text(
                                text = "Moderation History: Last reviewed by ${cmt.moderatedByEmail} at ${formatTimestamp(cmt.updatedAtMillis)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        if (activeRole.canModerateComments) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (cmt.moderationStatus != "APPROVED") {
                                    OutlinedButton(
                                        onClick = {
                                            onModerateComment(cmt, "APPROVED", cmt.adminReply, true)
                                        },
                                        border = BorderStroke(1.dp, StatusEmerald)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusEmerald, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (cmt.moderationStatus == "HIDDEN") "Restore & Approve" else "Approve", color = StatusEmerald)
                                    }
                                }
                                if (cmt.moderationStatus != "HIDDEN") {
                                    OutlinedButton(
                                        onClick = {
                                            onModerateComment(cmt, "HIDDEN", cmt.adminReply, false)
                                        },
                                        border = BorderStroke(1.dp, StatusAmber)
                                    ) {
                                        Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = StatusAmber, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Hide Comment", color = StatusAmber)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { replyingComment = cmt },
                                    border = BorderStroke(1.dp, VioletPrimary)
                                ) {
                                    Icon(Icons.Default.Reply, contentDescription = null, tint = VioletBright, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reply as Admin", color = VioletBright)
                                }
                                OutlinedButton(
                                    onClick = { deletingComment = cmt },
                                    border = BorderStroke(1.dp, StatusRose)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = StatusRose, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete", color = StatusRose)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (replyingComment != null) {
        val target = replyingComment!!
        var replyText by remember(target) { mutableStateOf(target.adminReply) }
        AlertDialog(
            onDismissRequest = { replyingComment = null },
            containerColor = GraphiteElevated,
            title = { Text("Reply as Administrator to @${target.username}", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    label = { Text("Administrator Response") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onModerateComment(target, "APPROVED", replyText, true)
                        replyingComment = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
                ) {
                    Text("Publish Admin Reply")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { replyingComment = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (deletingComment != null) {
        val target = deletingComment!!
        DestructiveConfirmationDialog(
            title = "Permanently Delete Comment?",
            message = "Delete comment by @${target.username} from shared Firestore 'comments/${target.id}'?",
            onConfirm = { onDeleteCommentPermanently(target) },
            onDismiss = { deletingComment = null }
        )
    }

    if (showAddCommentDialog && images.isNotEmpty()) {
        var selectedImage by remember { mutableStateOf(images.first()) }
        var username by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var reported by remember { mutableStateOf(false) }
        var reportReason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCommentDialog = false },
            containerColor = GraphiteElevated,
            title = { Text("Log Comment Record for Moderation", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Target Image:", style = MaterialTheme.typography.labelMedium, color = ChampagneLight)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        images.forEach { img ->
                            FilterChip(
                                selected = selectedImage.id == img.id,
                                onClick = { selectedImage = img },
                                label = { Text(img.title) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Author Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Comment Text") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Flag as Reported Comment", color = TextPrimary)
                        Switch(checked = reported, onCheckedChange = { reported = it })
                    }
                    if (reported) {
                        OutlinedTextField(
                            value = reportReason,
                            onValueChange = { reportReason = it },
                            label = { Text("Report Reason") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateCommentEntry(selectedImage, username, content, reported, reportReason)
                        showAddCommentDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
                ) {
                    Text("Save Comment")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddCommentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
