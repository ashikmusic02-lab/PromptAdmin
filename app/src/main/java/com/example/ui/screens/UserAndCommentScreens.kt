package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.unit.dp
import com.example.model.FirestoreCommentItem
import com.example.model.FirestoreReportItem
import com.example.ui.components.DestructiveConfirmationDialog
import com.example.ui.components.EmptyDataStateCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StatusRose
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.theme.VioletContainer
import com.example.ui.theme.VioletPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommentsAndReportsScreen(
    comments: List<FirestoreCommentItem>,
    reports: List<FirestoreReportItem>,
    isSubmitting: Boolean,
    onToggleCommentVisibility: (FirestoreCommentItem) -> Unit,
    onReplyToComment: (FirestoreCommentItem, String, () -> Unit) -> Unit,
    onDeleteComment: (FirestoreCommentItem) -> Unit,
    onUpdateReportStatus: (FirestoreReportItem, String) -> Unit
) {
    // Tab: "COMMENTS" or "REPORTS"
    var activeTab by remember { mutableStateOf("COMMENTS") }
    var replyingToCommentId by remember { mutableStateOf<String?>(null) }
    var replyDraft by remember { mutableStateOf("") }
    var pendingDeleteComment by remember { mutableStateOf<FirestoreCommentItem?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = activeTab == "COMMENTS",
                onClick = { activeTab = "COMMENTS" },
                label = { Text("কমেন্ট (${comments.size})") },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("tab_comments")
            )
            FilterChip(
                selected = activeTab == "REPORTS",
                onClick = { activeTab = "REPORTS" },
                label = { Text("রিপোর্ট (${reports.size})") },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("tab_reports")
            )
        }

        if (activeTab == "COMMENTS") {
            if (comments.isEmpty()) {
                EmptyDataStateCard(
                    title = "কোনো কমেন্ট নেই",
                    message = "ব্যবহারকারীদের কমেন্ট এখানে দেখা যাবে।",
                    icon = Icons.Default.Forum
                )
            } else {
                comments.forEach { comment ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("comment_card_${comment.id}"),
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
                                    Text(
                                        text = comment.userName.ifBlank { "ব্যবহারকারী" },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (comment.imageTitle.isNotBlank()) {
                                        Text(
                                            text = "ছবি: ${comment.imageTitle}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!comment.isServerConfirmed) {
                                        StatusPill(label = "সেভ হচ্ছে…", tone = "WARNING")
                                    }
                                    StatusPill(
                                        label = if (comment.isVisible) "দৃশ্যমান" else "লুকানো",
                                        tone = if (comment.isVisible) "VISIBLE" else "HIDDEN"
                                    )
                                }
                            }

                            Surface(
                                color = ObsidianBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, GraphiteBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = comment.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }

                            if (comment.adminReply.isNotBlank()) {
                                Surface(
                                    color = VioletContainer.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, VioletPrimary.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "অ্যাডমিনের উত্তর:",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = VioletBright
                                        )
                                        Text(
                                            text = comment.adminReply,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }

                            if (replyingToCommentId == comment.id) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = replyDraft,
                                        onValueChange = { replyDraft = it },
                                        label = { Text("অ্যাডমিনের উত্তর লিখুন") },
                                        minLines = 2,
                                        maxLines = 4,
                                        enabled = !isSubmitting,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                replyingToCommentId = null
                                                replyDraft = ""
                                            },
                                            enabled = !isSubmitting,
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp),
                                            border = BorderStroke(1.dp, GraphiteBorder)
                                        ) {
                                            Text("বাতিল", color = TextSecondary)
                                        }
                                        Button(
                                            onClick = {
                                                onReplyToComment(comment, replyDraft) {
                                                    replyingToCommentId = null
                                                    replyDraft = ""
                                                }
                                            },
                                            enabled = !isSubmitting,
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = VioletPrimary,
                                                contentColor = TextPrimary
                                            )
                                        ) {
                                            Text("উত্তর সেভ করুন")
                                        }
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
                                    onClick = { onToggleCommentVisibility(comment) },
                                    enabled = !isSubmitting,
                                    modifier = Modifier.heightIn(min = 48.dp),
                                    border = BorderStroke(1.dp, GraphiteBorder),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (comment.isVisible) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = if (comment.isVisible) "লুকান" else "দেখান",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (comment.isVisible) "লুকান" else "দেখান",
                                        color = TextPrimary
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        replyingToCommentId = comment.id
                                        replyDraft = comment.adminReply
                                    },
                                    enabled = !isSubmitting,
                                    modifier = Modifier.heightIn(min = 48.dp),
                                    border = BorderStroke(1.dp, GraphiteBorder),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Reply,
                                        contentDescription = "উত্তর দিন",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "উত্তর দিন", color = TextPrimary)
                                }

                                OutlinedButton(
                                    onClick = { pendingDeleteComment = comment },
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
        } else {
            if (reports.isEmpty()) {
                EmptyDataStateCard(
                    title = "কোনো রিপোর্ট নেই",
                    message = "ব্যবহারকারীদের পাঠানো রিপোর্ট এখানে দেখা যাবে।",
                    icon = Icons.Default.ReportProblem
                )
            } else {
                reports.forEach { report ->
                    val statusBangla = when (report.status.lowercase()) {
                        "reviewed" -> "পর্যালোচিত"
                        "dismissed" -> "বাতিল"
                        else -> "ওপেন"
                    }
                    val statusTone = when (report.status.lowercase()) {
                        "reviewed" -> "REVIEWED"
                        "dismissed" -> "DISMISSED"
                        else -> "OPEN"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_card_${report.id}"),
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
                                    text = report.reason.ifBlank { "রিপোর্ট" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!report.isServerConfirmed) {
                                        StatusPill(label = "সেভ হচ্ছে…", tone = "WARNING")
                                    }
                                    StatusPill(label = statusBangla, tone = statusTone)
                                }
                            }

                            if (report.details.isNotBlank()) {
                                Text(
                                    text = report.details,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }

                            HorizontalDivider(color = GraphiteBorder)

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = report.status.equals("open", ignoreCase = true),
                                    onClick = { onUpdateReportStatus(report, "open") },
                                    enabled = !isSubmitting,
                                    label = { Text("ওপেন") },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                                FilterChip(
                                    selected = report.status.equals("reviewed", ignoreCase = true),
                                    onClick = { onUpdateReportStatus(report, "reviewed") },
                                    enabled = !isSubmitting,
                                    label = { Text("পর্যালোচিত") },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                                FilterChip(
                                    selected = report.status.equals("dismissed", ignoreCase = true),
                                    onClick = { onUpdateReportStatus(report, "dismissed") },
                                    enabled = !isSubmitting,
                                    label = { Text("বাতিল") },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDeleteComment?.let { target ->
        DestructiveConfirmationDialog(
            title = "কমেন্ট মুছে ফেলবেন?",
            message = "এই কমেন্টটি স্থায়ীভাবে মুছে ফেলা হবে।",
            confirmButtonText = "মুছুন",
            onConfirm = { onDeleteComment(target) },
            onDismiss = { pendingDeleteComment = null }
        )
    }
}
