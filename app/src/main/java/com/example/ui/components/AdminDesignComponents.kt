package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.ui.theme.ChampagneContainer
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneLight
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberBg
import com.example.ui.theme.StatusCyan
import com.example.ui.theme.StatusCyanBg
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusEmeraldBg
import com.example.ui.theme.StatusRose
import com.example.ui.theme.StatusRoseBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletBright
import com.example.ui.theme.VioletContainer
import com.example.ui.theme.VioletPrimary
import com.example.util.UrlAndDeepLinkValidator
import com.example.util.UrlValidationResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatTimestamp(millis: Long): String {
    if (millis <= 0L) return "—"
    val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US)
    return sdf.format(Date(millis))
}

fun formatShortDate(millis: Long): String {
    if (millis <= 0L) return "—"
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    return sdf.format(Date(millis))
}

@Composable
fun EnterpriseStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color = VioletPrimary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                    text = title.uppercase(Locale.US),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                        .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    tone: String = "NEUTRAL" // SUCCESS, WARNING, DANGER, GOLD, VIOLET, CYAN, NEUTRAL
) {
    val (bg, fg) = when (tone.uppercase(Locale.US)) {
        "SUCCESS", "PUBLISHED", "ACTIVE", "APPROVED", "DISPATCHED_PROVIDER_CONFIRMED" ->
            StatusEmeraldBg to StatusEmerald
        "WARNING", "DRAFT", "PENDING", "SCHEDULED", "QUEUED_FOR_SERVER", "FLAGGED" ->
            StatusAmberBg to StatusAmber
        "DANGER", "DISABLED", "HIDDEN", "DELETED", "FAILED", "REPORTED" ->
            StatusRoseBg to StatusRose
        "GOLD", "OWNER", "FEATURED" ->
            ChampagneContainer to ChampagneLight
        "VIOLET", "ADMINISTRATOR", "TRENDING" ->
            VioletContainer to VioletBright
        "CYAN", "MODERATOR" ->
            StatusCyanBg to StatusCyan
        else -> GraphiteElevated to TextSecondary
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}

@Composable
fun SectionHeaderBar(
    sectionNumber: Int,
    title: String,
    subtitle: String,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraphiteCard),
        border = BorderStroke(1.dp, GraphiteBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            VioletContainer.copy(alpha = 0.55f),
                            GraphiteCard,
                            ChampagneContainer.copy(alpha = 0.30f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = ChampagneContainer,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "MODULE ${sectionNumber.toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ChampagneLight,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "ENTERPRISE ADMIN CONTROL PANEL",
                            style = MaterialTheme.typography.labelSmall,
                            color = VioletBright
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                if (trailingContent != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    trailingContent()
                }
            }
        }
    }
}

@Composable
fun UrlOnlyComplianceBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = VioletContainer.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, VioletPrimary.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = "Direct URL Policy",
                tint = ChampagneLight,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Strict Direct Image URL Architecture (Zero Storage Uploads)",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = "Images and thumbnails are referenced strictly via external direct links (.JPG, .JPEG, .PNG, .WEBP). File uploads and Firebase Storage are permanently disabled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun LiveExternalImagePreview(
    imageUrl: String,
    height: Dp = 180.dp,
    modifier: Modifier = Modifier
) {
    val validation = UrlAndDeepLinkValidator.validateDirectImageUrl(imageUrl)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (validation) {
            is UrlValidationResult.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianBg)
                        .border(1.dp, GraphiteBorder, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Enter URL",
                            tint = TextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Supply a direct external image URL (.jpg, .jpeg, .png, .webp) to preview",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
            is UrlValidationResult.Invalid -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .clip(RoundedCornerShape(14.dp))
                        .background(StatusRoseBg.copy(alpha = 0.55f))
                        .border(1.dp, StatusRose, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Invalid URL",
                            tint = StatusRose,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "URL Validation Error",
                            style = MaterialTheme.typography.titleSmall,
                            color = StatusRose
                        )
                        Text(
                            text = validation.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
            }
            is UrlValidationResult.Valid -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianBg)
                        .border(1.dp, StatusEmerald.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                ) {
                    SubcomposeAsyncImage(
                        model = validation.normalizedUrl,
                        contentDescription = "Live URL Image Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = VioletPrimary,
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 2.5.dp
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(StatusRoseBg.copy(alpha = 0.65f))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BrokenImage,
                                        contentDescription = "Failed to load remote image",
                                        tint = StatusRose,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = "Remote Image Failed to Load",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = StatusRose
                                    )
                                    Text(
                                        text = "The supplied URL syntax is valid, but the remote server did not return a readable image.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    )
                    Surface(
                        color = ObsidianBg.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, StatusEmerald.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified format",
                                tint = StatusEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${validation.detectedFormat} • ${validation.host}",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusEmerald
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UrlThumbnailBox(
    imageUrl: String,
    size: Dp = 52.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianBg)
            .border(1.dp, GraphiteBorder, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = "Image Thumbnail",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = VioletBright
                )
            },
            error = {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = "Broken Thumbnail",
                    tint = StatusRose,
                    modifier = Modifier.size(20.dp)
                )
            }
        )
    }
}

@Composable
fun DestructiveConfirmationDialog(
    title: String,
    message: String,
    confirmButtonText: String = "Confirm Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GraphiteElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusRose,
                    contentColor = TextPrimary
                ),
                modifier = Modifier.testTag("confirm_destructive_action_button")
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, GraphiteBorder)
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun EmptyDataStateCard(
    title: String,
    message: String,
    icon: ImageVector,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
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
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(VioletContainer)
                    .border(1.dp, VioletPrimary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = ChampagneLight,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VioletPrimary,
                        contentColor = TextPrimary
                    )
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun PermissionGuardBanner(requiredRoleLabel: String, currentRoleLabel: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = StatusAmberBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, StatusAmber.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Read-Only RBAC Mode",
                tint = StatusAmber
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Role-Based Access Control: Read-Only Mode ($currentRoleLabel)",
                    style = MaterialTheme.typography.titleSmall,
                    color = StatusAmber
                )
                Text(
                    text = "Write actions in this module require $requiredRoleLabel privileges.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
fun MonospaceBadge(text: String) {
    Surface(
        color = ObsidianBg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, GraphiteBorder)
    ) {
        Text(
            text = text,
            fontFamily = JetBrainsMonoFamily,
            style = MaterialTheme.typography.labelSmall,
            color = ChampagneLight,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
