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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.ChampagneContainer
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

@Composable
fun BanglaStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color = VioletPrimary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraphiteCard),
        border = BorderStroke(1.dp, GraphiteBorder.copy(alpha = 0.75f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor.copy(alpha = 0.16f))
                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    tone: String = "NEUTRAL"
) {
    val (bg, fg) = when (tone.uppercase()) {
        "SUCCESS", "PUBLISHED", "ACTIVE", "VISIBLE", "REVIEWED" ->
            StatusEmeraldBg to StatusEmerald
        "WARNING", "DRAFT", "UNLISTED", "PENDING", "OPEN" ->
            StatusAmberBg to StatusAmber
        "DANGER", "DISABLED", "HIDDEN", "DELETED", "DISMISSED" ->
            StatusRoseBg to StatusRose
        "GOLD", "OWNER", "FEATURED" ->
            ChampagneContainer to ChampagneLight
        "VIOLET", "ADMIN", "TRENDING" ->
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
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}

private fun extractGoogleDriveFileId(url: String): String? {
    val idMatch = Regex("""[?&]id=([a-zA-Z0-9_-]+)""").find(url)?.groupValues?.getOrNull(1)
    if (!idMatch.isNullOrBlank()) return idMatch
    val pathMatch = Regex("""drive\.google\.com/file/d/([a-zA-Z0-9_-]+)""").find(url)?.groupValues?.getOrNull(1)
    return pathMatch?.takeIf { it.isNotBlank() }
}

/**
 * Live image preview with automatic Google Drive link normalization and Coil intrinsic aspectRatio measurement.
 * Calls [onLoadStateChanged] with `(isLoaded, aspectRatio)` where `aspectRatio` is clamped to `0.2..5.0`.
 */
@Composable
fun CoilLiveImagePreview(
    rawImageUrl: String,
    height: Dp = 200.dp,
    onLoadStateChanged: (isLoaded: Boolean, aspectRatio: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val validation = androidx.compose.runtime.remember(rawImageUrl) {
        UrlAndDeepLinkValidator.validatePublicImageUrl(rawImageUrl)
    }
    val context = LocalContext.current

    var useDriveThumbnailFallback by androidx.compose.runtime.remember(rawImageUrl) {
        androidx.compose.runtime.mutableStateOf(false)
    }

    LaunchedEffect(rawImageUrl) {
        if (validation !is UrlValidationResult.Valid) {
            onLoadStateChanged(false, 1.0)
        }
    }

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
                        .clip(RoundedCornerShape(20.dp))
                        .background(ObsidianBg)
                        .border(1.dp, GraphiteBorder.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "ছবির প্রিভিউ",
                            tint = TextMuted,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "ছবির লিংক দিলে এখানে প্রিভিউ দেখা যাবে",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            is UrlValidationResult.Invalid -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .clip(RoundedCornerShape(20.dp))
                        .background(StatusRoseBg.copy(alpha = 0.65f))
                        .border(1.dp, StatusRose, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "লিংক ত্রুটি",
                            tint = StatusRose,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "এই লিংকে ছবি খোলা যাচ্ছে না। ছবিটি পাবলিক কিনা দেখুন",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StatusRose,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            is UrlValidationResult.Valid -> {
                val driveFileId = androidx.compose.runtime.remember(validation.normalizedUrl) {
                    extractGoogleDriveFileId(validation.normalizedUrl)
                }
                val activePreviewUrl = androidx.compose.runtime.remember(
                    validation.normalizedUrl,
                    useDriveThumbnailFallback,
                    driveFileId
                ) {
                    if (useDriveThumbnailFallback && !driveFileId.isNullOrBlank()) {
                        "https://drive.google.com/thumbnail?id=$driveFileId&sz=w1600"
                    } else {
                        validation.normalizedUrl
                    }
                }
                val imageRequest = androidx.compose.runtime.remember(activePreviewUrl, context) {
                    ImageRequest.Builder(context)
                        .data(activePreviewUrl)
                        .addHeader(
                            "User-Agent",
                            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                        )
                        .crossfade(true)
                        .build()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ObsidianBg)
                        .border(1.dp, GraphiteBorder.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                ) {
                    SubcomposeAsyncImage(
                        model = imageRequest,
                        contentDescription = "ছবির প্রিভিউ",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onSuccess = { state ->
                            val drawable = state.result.drawable
                            val w = drawable.intrinsicWidth
                            val h = drawable.intrinsicHeight
                            val ratio = if (w > 0 && h > 0) {
                                (w.toDouble() / h.toDouble()).coerceIn(0.2, 5.0)
                            } else {
                                1.0
                            }
                            onLoadStateChanged(true, ratio)
                        },
                        onError = {
                            if (!useDriveThumbnailFallback && !driveFileId.isNullOrBlank()) {
                                useDriveThumbnailFallback = true
                            } else {
                                onLoadStateChanged(false, 1.0)
                            }
                        },
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = VioletPrimary,
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Text(
                                        text = "ছবি লোড হচ্ছে...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(StatusRoseBg.copy(alpha = 0.75f))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BrokenImage,
                                        contentDescription = "ছবি লোড ব্যর্থ",
                                        tint = StatusRose,
                                        modifier = Modifier.size(30.dp)
                                    )
                                    Text(
                                        text = "এই লিংকে ছবি খোলা যাচ্ছে না। ছবিটি পাবলিক কিনা দেখুন",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = StatusRose,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.SemiBold
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
                                contentDescription = "লিংক প্রস্তুত",
                                tint = StatusEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = validation.host,
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
    size: Dp = 64.dp
) {
    val context = LocalContext.current
    val normalized = androidx.compose.runtime.remember(imageUrl) {
        UrlAndDeepLinkValidator.normalizePublicImageUrl(imageUrl)
    }
    var useDriveThumbnailFallback by androidx.compose.runtime.remember(normalized) {
        androidx.compose.runtime.mutableStateOf(false)
    }
    val driveFileId = androidx.compose.runtime.remember(normalized) {
        extractGoogleDriveFileId(normalized)
    }
    val activeUrl = androidx.compose.runtime.remember(normalized, useDriveThumbnailFallback, driveFileId) {
        if (useDriveThumbnailFallback && !driveFileId.isNullOrBlank()) {
            "https://drive.google.com/thumbnail?id=$driveFileId&sz=w600"
        } else {
            normalized
        }
    }
    val imageRequest = androidx.compose.runtime.remember(activeUrl, context) {
        ImageRequest.Builder(context)
            .data(activeUrl)
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
            )
            .crossfade(true)
            .build()
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianBg)
            .border(1.dp, GraphiteBorder.copy(alpha = 0.75f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = "ছবি",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onError = {
                if (!useDriveThumbnailFallback && !driveFileId.isNullOrBlank()) {
                    useDriveThumbnailFallback = true
                }
            },
            loading = {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = VioletBright
                )
            },
            error = {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = "ছবি পাওয়া যায়নি",
                    tint = StatusRose,
                    modifier = Modifier.size(22.dp)
                )
            }
        )
    }
}

@Composable
fun DestructiveConfirmationDialog(
    title: String,
    message: String,
    confirmButtonText: String = "মুছুন",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GraphiteElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(24.dp),
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
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("confirm_destructive_action_button")
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, GraphiteBorder),
                shape = RoundedCornerShape(50),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("বাতিল", color = TextSecondary)
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
        border = BorderStroke(1.dp, GraphiteBorder.copy(alpha = 0.75f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
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
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onActionClick,
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(50),
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
fun MonospaceBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = ObsidianBg,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, GraphiteBorder)
    ) {
        Text(
            text = text,
            fontFamily = JetBrainsMonoFamily,
            style = MaterialTheme.typography.labelMedium,
            color = ChampagneLight,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
