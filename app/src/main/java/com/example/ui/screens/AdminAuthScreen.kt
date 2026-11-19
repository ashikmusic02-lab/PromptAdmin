package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.BuildConfig
import com.example.ui.components.MonospaceBadge
import com.example.ui.theme.ChampagneLight
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StatusRose
import com.example.ui.theme.StatusRoseBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPrimary
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Splash/loading screen displayed until FirebaseAuth state and Firestore `adminUsers/{uid}` are resolved.
 */
@Composable
fun SplashLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("splash_loading_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            CircularProgressIndicator(
                color = VioletPrimary,
                modifier = Modifier.size(44.dp),
                strokeWidth = 3.5.dp
            )
            Text(
                text = "PromptAdmin",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "লোড হচ্ছে...",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }
    }
}

/**
 * Displayed when a Google account is signed in, but `adminUsers/{uid}` does not exist or `active != true`.
 * Shows "আপনি অ্যাডমিন নন" with an explicit sign-out button.
 */
@Composable
fun NotAdminScreen(
    uid: String,
    email: String,
    message: String,
    onRetryVerification: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(20.dp)
            .testTag("not_admin_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            colors = CardDefaults.cardColors(containerColor = GraphiteCard),
            border = BorderStroke(1.dp, GraphiteBorder),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    color = StatusRoseBg,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, StatusRose)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "আপনি অ্যাডমিন নন",
                        tint = StatusRose,
                        modifier = Modifier
                            .padding(14.dp)
                            .size(32.dp)
                    )
                }

                Text(
                    text = "আপনি অ্যাডমিন নন",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                if (message.isNotBlank() && message != "আপনি অ্যাডমিন নন") {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StatusRose,
                        textAlign = TextAlign.Center
                    )
                }

                if (email.isNotBlank()) {
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                if (uid.isNotBlank()) {
                    MonospaceBadge(text = "UID: $uid")
                }

                OutlinedButton(
                    onClick = onRetryVerification,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("retry_admin_check_button"),
                    border = BorderStroke(1.dp, GraphiteBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "পুনরায় যাচাই করুন",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "পুনরায় যাচাই করুন",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Button(
                    onClick = onSignOutClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("not_admin_sign_out_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusRose,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "লগ আউট",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "লগ আউট",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

/**
 * Login screen shown ONLY when there is no signed-in Firebase user.
 */
@Composable
fun AdminAuthScreen(
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
                    .widthIn(max = 480.dp),
                colors = CardDefaults.cardColors(containerColor = GraphiteCard),
                border = BorderStroke(1.dp, GraphiteBorder),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text = "PromptAdmin",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "অ্যাডমিন প্যানেলে প্রবেশ করতে গুগল অ্যাকাউন্ট দিয়ে লগ ইন করুন",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

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
                                    contentDescription = "লগ ইন ত্রুটি",
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
                                contentDescription = "গুগল দিয়ে লগ ইন করুন",
                                tint = ChampagneLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "গুগল দিয়ে লগ ইন করুন",
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
        onError("ফায়ারবেস সংযোগ পাওয়া যায়নি")
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
                onError("লগ ইন উইন্ডো খোলা যায়নি")
                return@launch
            }
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                val auth = FirebaseAuth.getInstance()
                val authResult = auth
                    .signInWithCredential(GoogleAuthProvider.getCredential(token, null))
                    .await()
                onAuthenticatedUser(authResult.user)
            } else {
                onError("গুগল ক্রেডেনশিয়াল যাচাই করা যায়নি")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled: ${e.message}", e)
            onCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onError("গুগল লগ ইন ব্যর্থ হয়েছে")
        }
    }
}
