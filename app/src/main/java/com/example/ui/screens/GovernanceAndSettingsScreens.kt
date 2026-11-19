package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.example.model.AppGlobalSettingsModel
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VioletPrimary

/**
 * SETTINGS SCREEN (`সেটিংস` — document `appSettings/global`):
 * Edit `playStoreUrl`, `requireLoginForPrompts`, `allowComments`,
 * `welcomeHeadline`, `welcomeSubheadline`, `supportEmail`, `updatedAt`
 * in Bangla with a single-submit save button.
 */
@Composable
fun AppSettingsScreen(
    settings: AppGlobalSettingsModel,
    isSaving: Boolean,
    onSave: (AppGlobalSettingsModel) -> Unit
) {
    var playStoreUrl by remember(settings) { mutableStateOf(settings.playStoreUrl) }
    var requireLoginForPrompts by remember(settings) { mutableStateOf(settings.requireLoginForPrompts) }
    var allowComments by remember(settings) { mutableStateOf(settings.allowComments) }
    var welcomeHeadline by remember(settings) { mutableStateOf(settings.welcomeHeadline) }
    var welcomeSubheadline by remember(settings) { mutableStateOf(settings.welcomeSubheadline) }
    var supportEmail by remember(settings) { mutableStateOf(settings.supportEmail) }

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "প্রম্পট দেখতে লগ ইন বাধ্যতামূলক",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = requireLoginForPrompts,
                        onCheckedChange = { requireLoginForPrompts = it },
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
                        text = "কমেন্ট করার অনুমতি দিন",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = allowComments,
                        onCheckedChange = { allowComments = it },
                        enabled = !isSaving
                    )
                }
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
                OutlinedTextField(
                    value = welcomeHeadline,
                    onValueChange = { welcomeHeadline = it },
                    label = { Text("স্বাগতম শিরোনাম") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = welcomeSubheadline,
                    onValueChange = { welcomeSubheadline = it },
                    label = { Text("স্বাগতম উপ-শিরোনাম") },
                    minLines = 2,
                    maxLines = 4,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = supportEmail,
                    onValueChange = { supportEmail = it },
                    label = { Text("সাপোর্ট ইমেইল") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = playStoreUrl,
                    onValueChange = { playStoreUrl = it },
                    label = { Text("প্লে স্টোর লিংক") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Button(
            onClick = {
                onSave(
                    AppGlobalSettingsModel(
                        playStoreUrl = playStoreUrl.trim(),
                        requireLoginForPrompts = requireLoginForPrompts,
                        allowComments = allowComments,
                        welcomeHeadline = welcomeHeadline.trim(),
                        welcomeSubheadline = welcomeSubheadline.trim(),
                        supportEmail = supportEmail.trim()
                    )
                )
            },
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save_app_settings_button"),
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
