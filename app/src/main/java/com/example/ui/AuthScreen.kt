package com.example.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.supabase.SupabaseSyncRepository
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldTeal
import com.example.ui.theme.OxfordNavy
import com.example.ui.theme.OxfordNavyDark
import com.example.ui.theme.WarmAmber
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    supabaseRepository: SupabaseSyncRepository,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (supabaseRepository.sessionFlow.value != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                val googleCredential = GoogleIdTokenCredential.createFrom(cred.data)
                val result = supabaseRepository.signInWithGoogleIdToken(
                    idToken = token,
                    email = googleCredential.id,
                    displayName = googleCredential.displayName
                )
                if (result.isSuccess) onAuthSuccess() else onUnauthenticated()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    supabaseRepository: SupabaseSyncRepository,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activity = context as? Activity
                ?: throw IllegalStateException("Activity context required for Google Sign-In.")
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleCredential = GoogleIdTokenCredential.createFrom(cred.data)
                val result = supabaseRepository.signInWithGoogleIdToken(
                    idToken = googleCredential.idToken,
                    email = googleCredential.id,
                    displayName = googleCredential.displayName
                )
                result.fold(
                    onSuccess = { onAuthSuccess() },
                    onFailure = { onAuthError(it.localizedMessage ?: "Supabase Google sign-in failed.") }
                )
            } else {
                onAuthError("Unexpected credential type received.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed. Please try again.")
        }
    }
}

fun performSignOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun AuthScreen(
    supabaseRepository: SupabaseSyncRepository = SupabaseSyncRepository.shared,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var autoChecking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    var isCreateAccount by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(450L)
        autoChecking = true
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            supabaseRepository = supabaseRepository,
            onAuthSuccess = {
                autoChecking = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                autoChecking = false
            },
            scope = scope
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(26.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_campus_hero),
                    contentDescription = "GSMS Global Campus Courtyard",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    OxfordNavyDark.copy(alpha = 0.25f),
                                    OxfordNavy.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Surface(
                        color = EmeraldBright.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "MULTI-TENANT AI SCHOOL ERP",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldBright,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Global School Management System",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "19 Role Portals • 21 Modular Engines • Gemini AI Layer",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Primary Email & Password Authentication Card (Supabase GoTrue Auth)
                ElevatedCard(
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = if (isCreateAccount) "Create Email Account" else "Sign in with Email",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Access your assigned school portal via verified email",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusPill(
                                text = if (supabaseRepository.isConfigured) "EMAIL READY" else "SETUP NEEDED",
                                color = if (supabaseRepository.isConfigured) EmeraldTeal else WarmAmber
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !isCreateAccount,
                                onClick = {
                                    isCreateAccount = false
                                    errorMessage = null
                                    infoMessage = null
                                },
                                label = { Text("Sign In") },
                                modifier = Modifier.testTag("auth_mode_signin")
                            )
                            FilterChip(
                                selected = isCreateAccount,
                                onClick = {
                                    isCreateAccount = true
                                    errorMessage = null
                                    infoMessage = null
                                },
                                label = { Text("Create Account") },
                                modifier = Modifier.testTag("auth_mode_signup")
                            )
                        }

                        if (!supabaseRepository.isConfigured) {
                            Surface(
                                color = WarmAmber.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Add SUPABASE_URL & SUPABASE_ANON_KEY in AI Studio Secrets (and run supabase/schema.sql) to enable Email Login, or continue with Google below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        if (isCreateAccount) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full Name") },
                                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_fullname_input")
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("School / Work Email") },
                            placeholder = { Text("name@school.edu") },
                            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input")
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password (min 6 characters)") },
                            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input")
                        )

                        if (!isCreateAccount) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        errorMessage = null
                                        infoMessage = null
                                        isLoading = true
                                        scope.launch {
                                            supabaseRepository.sendPasswordReset(email)
                                                .onSuccess { msg -> infoMessage = msg }
                                                .onFailure { err -> errorMessage = err.localizedMessage ?: "Reset failed." }
                                            isLoading = false
                                        }
                                    },
                                    enabled = !isLoading && email.isNotBlank(),
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text("Forgot password?")
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.ErrorOutline,
                                        contentDescription = "Sign-in error",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = errorMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        if (infoMessage != null) {
                            Surface(
                                color = EmeraldTeal.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.MarkEmailRead,
                                        contentDescription = "Email confirmation",
                                        tint = EmeraldTeal
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = infoMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                errorMessage = null
                                infoMessage = null
                                isLoading = true
                                scope.launch {
                                    val result = if (isCreateAccount) {
                                        supabaseRepository.signUp(
                                            email = email,
                                            password = password,
                                            fullName = fullName
                                        )
                                    } else {
                                        supabaseRepository.signIn(email = email, password = password)
                                    }
                                    result.onSuccess { msg ->
                                        if (supabaseRepository.sessionFlow.value != null) {
                                            onAuthSuccess()
                                        } else {
                                            infoMessage = msg
                                        }
                                    }.onFailure { err ->
                                        errorMessage = err.localizedMessage ?: "Email authentication failed."
                                    }
                                    isLoading = false
                                }
                            },
                            enabled = !isLoading && !autoChecking && email.isNotBlank() && password.length >= 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("email_sign_in_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(if (isCreateAccount) "Creating Account..." else "Signing in...")
                            } else {
                                Icon(Icons.Rounded.Login, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isCreateAccount) "Create Email Account" else "Sign in with Email",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "OR",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                FilledTonalButton(
                    onClick = {
                        errorMessage = null
                        infoMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            supabaseRepository = supabaseRepository,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading && !autoChecking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (autoChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Checking Google Session...")
                    } else {
                        Icon(Icons.Rounded.AccountCircle, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Sign in with Google",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                AuthFeatureRow(
                    icon = Icons.Rounded.Domain,
                    title = "Multi-School & Branch Tenancy",
                    subtitle = "Strict tenant isolation with academic years, terms, and white-label module toggles."
                )
                AuthFeatureRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Role-Scoped Gemini AI Copilot",
                    subtitle = "Lesson plans, report summaries, early-warning risk detection, and route optimization."
                )
                AuthFeatureRow(
                    icon = Icons.Rounded.VerifiedUser,
                    title = "End-to-End Approvals & PDF Reports",
                    subtitle = "Report card template governance, leave balance automation, and immutable audit logs."
                )

                Text(
                    text = "Protected by Supabase Auth, Google Identity Services, PostgreSQL Row Level Security, and row-level tenancy rules.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun AuthFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
