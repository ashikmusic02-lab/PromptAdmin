package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AdminSection
import com.example.model.DashboardCounts
import com.example.ui.components.BanglaStatCard
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusCyan
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VioletPrimary

@Composable
fun DashboardScreen(
    counts: DashboardCounts,
    onNavigate: (AdminSection) -> Unit,
    onAddNewImage: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onAddNewImage,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .testTag("dashboard_add_image_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = VioletPrimary,
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(50)
        ) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = "নতুন ছবি",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "নতুন ছবি যোগ করুন",
                style = MaterialTheme.typography.titleMedium
            )
        }

        BanglaStatCard(
            title = "মোট ছবি",
            value = counts.totalImages.toString(),
            icon = Icons.Default.Collections,
            accentColor = VioletPrimary
        )

        BanglaStatCard(
            title = "প্রকাশিত",
            value = counts.publishedImages.toString(),
            icon = Icons.Default.CheckCircle,
            accentColor = StatusEmerald
        )

        BanglaStatCard(
            title = "অপ্রকাশিত",
            value = counts.unlistedImages.toString(),
            icon = Icons.Default.VisibilityOff,
            accentColor = StatusAmber
        )

        BanglaStatCard(
            title = "ক্যাটাগরি",
            value = counts.totalCategories.toString(),
            icon = Icons.Default.Category,
            accentColor = ChampagneGold
        )

        BanglaStatCard(
            title = "কমেন্ট",
            value = counts.totalComments.toString(),
            icon = Icons.Default.Forum,
            accentColor = StatusCyan
        )

        BanglaStatCard(
            title = "ওপেন রিপোর্ট",
            value = counts.openReports.toString(),
            icon = Icons.Default.ReportProblem,
            accentColor = StatusRose
        )

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
                OutlinedButton(
                    onClick = { onNavigate(AdminSection.IMAGES) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                    border = BorderStroke(1.dp, GraphiteBorder),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(text = "ছবি পরিচালনা", color = TextPrimary)
                }

                OutlinedButton(
                    onClick = { onNavigate(AdminSection.CATEGORIES) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                    border = BorderStroke(1.dp, GraphiteBorder),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(text = "ক্যাটাগরি পরিচালনা", color = TextPrimary)
                }

                OutlinedButton(
                    onClick = { onNavigate(AdminSection.AI_TOOLS) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("dashboard_ai_tools_button"),
                    border = BorderStroke(1.dp, GraphiteBorder),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(text = "এআই টুল", color = TextPrimary)
                }

                OutlinedButton(
                    onClick = { onNavigate(AdminSection.COMMENTS_REPORTS) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                    border = BorderStroke(1.dp, GraphiteBorder),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(text = "কমেন্ট ও রিপোর্ট দেখুন", color = TextPrimary)
                }

                OutlinedButton(
                    onClick = { onNavigate(AdminSection.NOTIFICATION_HELPER) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                    border = BorderStroke(1.dp, GraphiteBorder),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(text = "নোটিফিকেশন", color = TextPrimary)
                }

                OutlinedButton(
                    onClick = { onNavigate(AdminSection.PUSH_RULES) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("dashboard_push_rules_button"),
                    border = BorderStroke(1.dp, GraphiteBorder),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(text = "পুশ পাঠানোর নিয়ম", color = TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
