package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.BuildConfig
import com.example.R
import com.example.data.FirebaseConnectionStatus
import com.example.ui.components.MonospaceBadge
import com.example.ui.components.StatusPill
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneLight
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StatusEmerald
import com.example.ui.theme.StatusRose
import com.example.ui.theme.StatusRoseBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletContainer
import com.example.ui.theme.VioletPrimary
import com.example.viewmodel.AdminViewModel
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun AdminAuthScreen(
    connectionStatus: FirebaseConnectionStatus,
    externalAuthError: String?,
    onGoogleFirebaseUserAuthenticated: (FirebaseUser?) -> Unit,
    onSetAuthError: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isGoogleLoading by remember { mutableStateOf(false) }

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
                    .widthIn(max = 560.dp),
                colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                border = BorderStroke(1.dp, GraphiteBorder),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Hero Header Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(185.dp)
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
                                StatusPill(label = "GOOGLE SIGN-IN ONLY", tone = "GOLD")
                                StatusPill(
                                    label = "PROJECT: ${connectionStatus.projectId.uppercase()}",
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
                        // Firebase Project & Package Status Banner
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
                                MonospaceBadge("Package: ${connectionStatus.applicationId}")
                            }
                        }

                        // Strict Allowlist Security Policy Box
                        Surface(
                            color = ObsidianBg,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, GraphiteBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Strict Allowlist Gate",
                                    tint = ChampagneLight,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Zero-Trust Google Allowlist Authentication",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Authentication is restricted exclusively to verified Google accounts listed in ADMIN_EMAIL_ALLOWLIST. Unlisted or unverified accounts are signed out immediately.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        if (!externalAuthError.isNullOrBlank()) {
                            Surface(
                                color = StatusRoseBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, StatusRose),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_error_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Access denied",
                                        tint = StatusRose
                                    )
                                    Text(
                                        text = externalAuthError,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = StatusRose
                                    )
                                }
                            }
                        }

                        // Single Exclusive Authentication Action: Google Sign-In via Credential Manager
                        Button(
                            onClick = {
                                onSetAuthError(null)
                                isGoogleLoading = true
                                launchGoogleSignInOnly(
                                    context = context,
                                    scope = scope,
                                    onAuthenticatedUser = { firebaseUser ->
                                        isGoogleLoading = false
                                        onGoogleFirebaseUserAuthenticated(firebaseUser)
                                    },
                                    onError = { msg ->
                                        isGoogleLoading = false
                                        onSetAuthError(msg)
                                    },
                                    onCancelled = {
                                        isGoogleLoading = false
                                    }
                                )
                            },
                            enabled = !isGoogleLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_sign_in_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VioletPrimary,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isGoogleLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = TextPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = "Sign in with Google",
                                    tint = ChampagneLight,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Sign in with Google",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Resolves the Google Web Client ID (`default_web_client_id`) from `google-services.json` resources,
 * or falls back to the configured project Web Client ID.
 */
private fun resolveWebClientId(context: Context): String {
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    if (resId != 0) {
        val fromRes = runCatching { context.getString(resId) }.getOrNull()
        if (!fromRes.isNullOrBlank()) return fromRes
    }
    val domainPart = listOf("apps", "googleusercontent", "com").joinToString(".")
    return "${BuildConfig.FALLBACK_WEB_CLIENT_PREFIX}.$domainPart"
}

private fun launchGoogleSignInOnly(
    context: Context,
    scope: CoroutineScope,
    onAuthenticatedUser: (FirebaseUser?) -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit
) {
    if (FirebaseApp.getApps(context).isEmpty()) {
        onError("Firebase is not initialized yet. Ensure app/google-services.json is placed in app/ for com.farhadvarietiesstore.promptlensadmin.")
        return
    }

    val clientId = resolveWebClientId(context)
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
                val auth = FirebaseAuth.getInstance()
                val authResult = auth
                    .signInWithCredential(GoogleAuthProvider.getCredential(token, null))
                    .await()
                val user = authResult.user
                if (!AdminViewModel.isAuthorizedFirebaseUser(user)) {
                    withContext(NonCancellable) {
                        try {
                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                        } catch (_: Exception) {
                        }
                        auth.signOut()
                    }
                    onError("Access denied")
                } else {
                    onAuthenticatedUser(user)
                }
            } else {
                onError("Unexpected credential type returned.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onError(e.localizedMessage ?: "Google Sign-In failed")
        }
    }
}
