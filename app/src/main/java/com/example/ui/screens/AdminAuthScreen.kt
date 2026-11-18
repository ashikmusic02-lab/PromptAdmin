package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.FirebaseConnectionStatus
import com.example.model.AdminRole
import com.example.ui.components.MonospaceBadge
import com.example.ui.components.StatusPill
import com.example.ui.theme.ChampagneContainer
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
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AdminAuthScreen(
    connectionStatus: FirebaseConnectionStatus,
    onAuthenticated: (uid: String, email: String, displayName: String, role: AdminRole, authMethod: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var adminEmail by remember { mutableStateOf("sfforhadvai@gmail.com") }
    var adminName by remember { mutableStateOf("Principal System Owner") }
    var selectedRole by remember { mutableStateOf(AdminRole.OWNER) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp),
                colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                border = BorderStroke(1.dp, GraphiteBorder),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Hero Header Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_admin_hero_1794988005953),
                            contentDescription = "Enterprise Control Panel Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            ObsidianBg.copy(alpha = 0.25f),
                                            GraphiteCard.copy(alpha = 0.96f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatusPill(label = "SEPARATE ADMIN PROJECT", tone = "GOLD")
                                StatusPill(
                                    label = if (connectionStatus.isGoogleServicesPresent) "FIRESTORE LIVE" else "READY FOR google-services.json",
                                    tone = if (connectionStatus.isGoogleServicesPresent) "SUCCESS" else "VIOLET"
                                )
                            }
                            Text(
                                text = "PromptAdmin Enterprise Portal",
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "AI Photo Inspiration & Prompt Library • Dedicated Administrative Console",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Firebase google-services.json Status Banner
                        Surface(
                            color = if (connectionStatus.isGoogleServicesPresent) {
                                StatusEmerald.copy(alpha = 0.12f)
                            } else {
                                VioletContainer.copy(alpha = 0.55f)
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                if (connectionStatus.isGoogleServicesPresent) StatusEmerald.copy(alpha = 0.45f)
                                else ChampagneGold.copy(alpha = 0.45f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (connectionStatus.isGoogleServicesPresent) {
                                            Icons.Default.CloudDone
                                        } else {
                                            Icons.Default.CloudUpload
                                        },
                                        contentDescription = "Backend Connection Status",
                                        tint = if (connectionStatus.isGoogleServicesPresent) StatusEmerald else ChampagneLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = connectionStatus.databaseModeLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "Package Name: ${connectionStatus.applicationId} • SHA-1: 65:FD:A0:D0:AE:1E:18:3A:AC:77:B5:47:78:05:00:3C:E0:C2:E1:CF",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ChampagneLight
                                )
                            }
                        }

                        if (authError != null) {
                            Surface(
                                color = StatusRose.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, StatusRose)
                            ) {
                                Text(
                                    text = authError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusRose,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // Google Sign-In via Credential Manager (Active when google-services.json is present)
                        OutlinedButton(
                            onClick = {
                                authError = null
                                isGoogleLoading = true
                                launchGoogleCredentialManagerSignIn(
                                    context = context,
                                    scope = scope,
                                    onSuccess = { uid, email, name ->
                                        isGoogleLoading = false
                                        onAuthenticated(uid, email, name, selectedRole, "Firebase Google Sign-In")
                                    },
                                    onError = { msg ->
                                        isGoogleLoading = false
                                        authError = msg
                                    },
                                    onCancelled = {
                                        isGoogleLoading = false
                                    }
                                )
                            },
                            enabled = !isGoogleLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("google_sign_in_button"),
                            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.65f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isGoogleLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = ChampagneLight
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = "Google Sign-In",
                                    tint = ChampagneLight,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sign in with Google (Firebase Auth Credential Manager)",
                                    color = ChampagneLight,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = GraphiteBorder)
                            Text(
                                text = "ROLE-BASED ADMIN CONSOLE ACCESS",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = GraphiteBorder)
                        }

                        // Administrator Identity Inputs
                        OutlinedTextField(
                            value = adminEmail,
                            onValueChange = { adminEmail = it },
                            label = { Text("Administrator Email") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_email_input")
                        )

                        OutlinedTextField(
                            value = adminName,
                            onValueChange = { adminName = it },
                            label = { Text("Administrator Display Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_name_input")
                        )

                        // Role Selector Cards (Owner, Administrator, Moderator)
                        Text(
                            text = "Select Administrative Clearance Role",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextSecondary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminRole.entries.forEach { roleOption ->
                                val isSelected = selectedRole == roleOption
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedRole = roleOption }
                                        .testTag("role_option_${roleOption.name.lowercase()}"),
                                    color = if (isSelected) VioletContainer else GraphiteElevated,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) VioletPrimary else GraphiteBorder
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = roleOption.displayName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = TextPrimary
                                                )
                                                StatusPill(
                                                    label = roleOption.badgeLabel,
                                                    tone = when (roleOption) {
                                                        AdminRole.OWNER -> "GOLD"
                                                        AdminRole.ADMINISTRATOR -> "VIOLET"
                                                        AdminRole.MODERATOR -> "CYAN"
                                                    }
                                                )
                                            }
                                            Text(
                                                text = when (roleOption) {
                                                    AdminRole.OWNER -> "Full authority across all 14 modules, Administrator RBAC management, and destructive operations."
                                                    AdminRole.ADMINISTRATOR -> "Manage URL-only images, categories, users, AdMob configs, OneSignal notifications, and app content."
                                                    AdminRole.MODERATOR -> "Focused Trust & Safety access: comment moderation, user reports, and read-only analytics."
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected Role",
                                                tint = ChampagneLight
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val cleanEmail = adminEmail.trim().ifEmpty { "sfforhadvai@gmail.com" }
                                val uid = "adm_${cleanEmail.lowercase().hashCode().toUInt()}"
                                onAuthenticated(
                                    uid,
                                    cleanEmail,
                                    adminName.trim().ifEmpty { "System Owner" },
                                    selectedRole,
                                    "Enterprise RBAC Console"
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("enter_admin_console_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VioletPrimary,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Launch Admin Dashboard"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Launch Enterprise Administrator Console",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun launchGoogleCredentialManagerSignIn(
    context: Context,
    scope: CoroutineScope,
    onSuccess: (uid: String, email: String, name: String) -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit
) {
    val webClientIdRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    val clientId = if (webClientIdRes != 0) {
        runCatching { context.getString(webClientIdRes) }.getOrNull()
    } else null

    if (clientId.isNullOrBlank()) {
        onError("Google Sign-In requires app/google-services.json with a valid OAuth 2.0 Web Client ID. Upload your google-services.json or use the Role-Based Console button below.")
        return
    }

    val credentialManager = CredentialManager.create(context)
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activity = context as? Activity
            if (activity == null) {
                onError("Activity context required for Credential Manager.")
                return@launch
            }
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                val authResult = FirebaseAuth.getInstance()
                    .signInWithCredential(GoogleAuthProvider.getCredential(token, null))
                    .await()
                val user = authResult.user
                onSuccess(
                    user?.uid ?: "adm_google",
                    user?.email ?: "admin@promptvault.app",
                    user?.displayName ?: "Google Administrator"
                )
            } else {
                onError("Unexpected credential type returned.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onError(e.localizedMessage ?: "Google Sign-In failed. Verify SHA-1 in Firebase Console.")
        }
    }
}
