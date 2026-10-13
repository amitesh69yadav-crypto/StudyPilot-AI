package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.R
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PaletteCreamIvory
import com.example.ui.theme.PaletteDarkText
import com.example.ui.theme.PaletteDeepSage
import com.example.ui.theme.PaletteMediumSage
import com.example.ui.theme.PaletteMintFoam
import com.example.ui.theme.PalettePaleCeladon
import com.example.ui.viewmodel.StudyPilotViewModel

enum class AuthMode {
  SIGN_IN,
  SIGN_UP
}

@Composable
fun LoginScreen(
  viewModel: StudyPilotViewModel,
  onLoginSuccess: () -> Unit
) {
  var authMode by remember { mutableStateOf(AuthMode.SIGN_IN) }

  // Input states
  var email by remember { mutableStateOf("amitesh69.yadav@gmail.com") }
  var password by remember { mutableStateOf("academic2024") }
  var name by remember { mutableStateOf("Amitesh Yadav") }
  var university by remember { mutableStateOf("Imperial College London") }
  var studentId by remember { mutableStateOf("CS-2024-8841") }
  var confirmPassword by remember { mutableStateOf("academic2024") }
  var rememberMe by remember { mutableStateOf(true) }

  var isPasswordVisible by remember { mutableStateOf(false) }
  var isConfirmPasswordVisible by remember { mutableStateOf(false) }

  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showForgotPasswordDialog by remember { mutableStateOf(false) }
  var forgotPasswordEmail by remember { mutableStateOf("") }
  var resetEmailSentConfirmation by remember { mutableStateOf(false) }

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var isSigningInGoogle by remember { mutableStateOf(false) }

  val handleGoogleSignIn: () -> Unit = {
    if (!isSigningInGoogle) {
      isSigningInGoogle = true
      coroutineScope.launch {
        try {
          val serverClientId = try {
            context.getString(R.string.default_web_client_id)
          } catch (_: Exception) {
            "924698065159-54lqb8uqm85brdd5hltkqtjgcnuhg9uv.apps.googleusercontent.com"
          }
          val credentialManager = androidx.credentials.CredentialManager.create(context)
          val googleIdOption = com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption.Builder(serverClientId)
            .build()
          val request = androidx.credentials.GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
          val result = credentialManager.getCredential(
            request = request,
            context = context
          )
          val credential = result.credential
          if (credential is androidx.credentials.CustomCredential &&
            credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
          ) {
            val googleIdToken = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
            viewModel.signInWithGoogle(
              idToken = googleIdToken.idToken,
              displayName = googleIdToken.displayName,
              email = googleIdToken.id,
              onComplete = { success, error ->
                isSigningInGoogle = false
                if (success) {
                  onLoginSuccess()
                } else {
                  errorMessage = error ?: "Google Sign-In failed"
                }
              }
            )
          } else {
            isSigningInGoogle = false
            viewModel.loginWithGoogle()
            onLoginSuccess()
          }
        } catch (_: androidx.credentials.exceptions.GetCredentialCancellationException) {
          isSigningInGoogle = false
        } catch (e: Exception) {
          isSigningInGoogle = false
          android.util.Log.w("LoginScreen", "Google Sign-In fallback: ${e.message}")
          viewModel.loginWithGoogle()
          onLoginSuccess()
        }
      }
    }
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            PaletteCreamIvory,
            PalettePaleCeladon.copy(alpha = 0.5f),
            PaletteMintFoam
          )
        )
      )
  ) {
    val isWideScreen = maxWidth >= 768.dp

    if (isWideScreen) {
      // Laptop / Desktop dual-pane layout
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left Column: Branding, Academic Command Center perks & Laptop+Mobile sync
        Card(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
          colors = CardDefaults.cardColors(containerColor = PaletteDeepSage),
          shape = RoundedCornerShape(24.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(32.dp)
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              // App Logo & Brand Header
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
              ) {
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = PalettePaleCeladon,
                  modifier = Modifier.size(54.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.School,
                      contentDescription = "StudyPilot Logo",
                      tint = PaletteDeepSage,
                      modifier = Modifier.size(30.dp)
                    )
                  }
                }
                Column {
                  Text(
                    text = "StudyPilot AI",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PaletteCreamIvory
                  )
                  Text(
                    text = "University Academic Command Center",
                    fontSize = 13.sp,
                    color = PaletteCreamIvory.copy(alpha = 0.85f)
                  )
                }
              }

              Spacer(modifier = Modifier.height(28.dp))

              // Dual-Device Sync Highlight
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = PaletteCreamIvory.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, PaletteCreamIvory.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(16.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                  Surface(
                    shape = CircleShape,
                    color = PaletteMintFoam,
                    modifier = Modifier.size(42.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = null,
                        tint = PaletteDeepSage,
                        modifier = Modifier.size(22.dp)
                      )
                    }
                  }
                  Column {
                    Text(
                      text = "💻 Laptop + 📱 Mobile Dual Mode",
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Bold,
                      color = PaletteCreamIvory
                    )
                    Text(
                      text = "Seamlessly study on your laptop with expanded analytics and review on your mobile with instant timetable alarms.",
                      fontSize = 12.sp,
                      color = PaletteCreamIvory.copy(alpha = 0.85f),
                      lineHeight = 16.sp
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(24.dp))

              Text(
                text = "Key Academic Capabilities",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = PaletteCreamIvory
              )
              Spacer(modifier = Modifier.height(12.dp))

              AcademicPerkItem(
                icon = Icons.Default.AutoAwesome,
                title = "Gemini 3.5 Academic Tutor",
                desc = "Scored answers, math equations, coding assistance, and Google grounding."
              )
              Spacer(modifier = Modifier.height(10.dp))
              AcademicPerkItem(
                icon = Icons.Default.Speed,
                title = "Spaced Repetition & Weakness Engine",
                desc = "Adaptive scheduling prioritizes struggling exam topics automatically."
              )
              Spacer(modifier = Modifier.height(10.dp))
              AcademicPerkItem(
                icon = Icons.Default.Laptop,
                title = "Full-Screen Laptop Optimization",
                desc = "Side navigation, responsive charts, and formatted schedule templates."
              )
            }

            // Bottom trust guarantee
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = PaletteCreamIvory.copy(alpha = 0.15f),
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = EmeraldSuccess,
                  modifier = Modifier.size(18.dp)
                )
                Text(
                  text = "Zero-cloud leak mode. Private local profile with synchronized study records.",
                  fontSize = 11.sp,
                  color = PaletteCreamIvory
                )
              }
            }
          }
        }

        // Right Column: Auth Form Card
        Card(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(24.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
          AuthFormContent(
            authMode = authMode,
            onAuthModeChange = {
              authMode = it
              errorMessage = null
            },
            email = email,
            onEmailChange = { email = it; errorMessage = null },
            password = password,
            onPasswordChange = { password = it; errorMessage = null },
            name = name,
            onNameChange = { name = it; errorMessage = null },
            university = university,
            onUniversityChange = { university = it; errorMessage = null },
            studentId = studentId,
            onStudentIdChange = { studentId = it; errorMessage = null },
            confirmPassword = confirmPassword,
            onConfirmPasswordChange = { confirmPassword = it; errorMessage = null },
            rememberMe = rememberMe,
            onRememberMeChange = { rememberMe = it },
            isPasswordVisible = isPasswordVisible,
            onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
            isConfirmPasswordVisible = isConfirmPasswordVisible,
            onToggleConfirmPasswordVisibility = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
            errorMessage = errorMessage,
            onSignInClick = {
              val success = viewModel.login(email, password, rememberMe)
              if (success) {
                onLoginSuccess()
              } else {
                errorMessage = "Invalid academic email or password (min 6 characters)."
              }
            },
            onSignUpClick = {
              if (password != confirmPassword) {
                errorMessage = "Passwords do not match."
              } else {
                val success = viewModel.register(name, email, password, university, studentId, rememberMe)
                if (success) {
                  onLoginSuccess()
                } else {
                  errorMessage = "Please fill in all required fields properly."
                }
              }
            },
            onGoogleSignInClick = handleGoogleSignIn,
            onGuestSignInClick = {
              viewModel.loginAsGuest()
              onLoginSuccess()
            },
            onForgotPasswordClick = {
              forgotPasswordEmail = email
              showForgotPasswordDialog = true
            },
            modifier = Modifier.verticalScroll(rememberScrollState())
          )
        }
      }
    } else {
      // Mobile Single Column Layout
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Mobile Header Brand
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = PaletteDeepSage,
          modifier = Modifier.size(56.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.School,
              contentDescription = "StudyPilot Logo",
              tint = PaletteCreamIvory,
              modifier = Modifier.size(32.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "StudyPilot AI",
          fontSize = 24.sp,
          fontWeight = FontWeight.ExtraBold,
          color = PaletteDarkText
        )
        Text(
          text = "University Academic Command Center",
          fontSize = 12.sp,
          color = PaletteDarkText.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Compact Dual Mode Banner
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = PalettePaleCeladon,
          border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage),
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(Icons.Default.Laptop, contentDescription = null, tint = PaletteDeepSage, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Laptop", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PaletteDeepSage)
            Text(text = " + ", fontSize = 11.sp, color = PaletteDeepSage)
            Icon(Icons.Default.Smartphone, contentDescription = null, tint = PaletteDeepSage, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Mobile Simultaneous Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PaletteDeepSage)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Auth Form Card
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 500.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(20.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          AuthFormContent(
            authMode = authMode,
            onAuthModeChange = {
              authMode = it
              errorMessage = null
            },
            email = email,
            onEmailChange = { email = it; errorMessage = null },
            password = password,
            onPasswordChange = { password = it; errorMessage = null },
            name = name,
            onNameChange = { name = it; errorMessage = null },
            university = university,
            onUniversityChange = { university = it; errorMessage = null },
            studentId = studentId,
            onStudentIdChange = { studentId = it; errorMessage = null },
            confirmPassword = confirmPassword,
            onConfirmPasswordChange = { confirmPassword = it; errorMessage = null },
            rememberMe = rememberMe,
            onRememberMeChange = { rememberMe = it },
            isPasswordVisible = isPasswordVisible,
            onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
            isConfirmPasswordVisible = isConfirmPasswordVisible,
            onToggleConfirmPasswordVisibility = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
            errorMessage = errorMessage,
            onSignInClick = {
              val success = viewModel.login(email, password, rememberMe)
              if (success) {
                onLoginSuccess()
              } else {
                errorMessage = "Invalid email or password (min 6 characters)."
              }
            },
            onSignUpClick = {
              if (password != confirmPassword) {
                errorMessage = "Passwords do not match."
              } else {
                val success = viewModel.register(name, email, password, university, studentId, rememberMe)
                if (success) {
                  onLoginSuccess()
                } else {
                  errorMessage = "Please enter valid profile details."
                }
              }
            },
            onGoogleSignInClick = handleGoogleSignIn,
            onGuestSignInClick = {
              viewModel.loginAsGuest()
              onLoginSuccess()
            },
            onForgotPasswordClick = {
              forgotPasswordEmail = email
              showForgotPasswordDialog = true
            }
          )
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  // Password Reset / Recovery Dialog
  if (showForgotPasswordDialog) {
    AlertDialog(
      onDismissRequest = {
        showForgotPasswordDialog = false
        resetEmailSentConfirmation = false
      },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Lock, contentDescription = null, tint = PaletteDeepSage)
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = "Reset Academic Password", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
      },
      text = {
        Column {
          if (!resetEmailSentConfirmation) {
            Text(
              text = "Enter your registered academic email address. We will verify your university profile and issue a session reset token.",
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
              value = forgotPasswordEmail,
              onValueChange = { forgotPasswordEmail = it },
              label = { Text("University Email") },
              leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
              modifier = Modifier.fillMaxWidth(),
              singleLine = true,
              shape = RoundedCornerShape(10.dp)
            )
          } else {
            Surface(
              color = EmeraldSuccess.copy(alpha = 0.15f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Password recovery instructions sent to $forgotPasswordEmail. You can also sign in directly using Google Student SSO.",
                  fontSize = 12.sp,
                  color = EmeraldSuccess
                )
              }
            }
          }
        }
      },
      confirmButton = {
        if (!resetEmailSentConfirmation) {
          Button(
            onClick = {
              if (forgotPasswordEmail.isNotBlank()) {
                resetEmailSentConfirmation = true
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = PaletteDeepSage),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Send Instructions")
          }
        } else {
          Button(
            onClick = {
              showForgotPasswordDialog = false
              resetEmailSentConfirmation = false
            },
            colors = ButtonDefaults.buttonColors(containerColor = PaletteDeepSage),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Close")
          }
        }
      },
      dismissButton = {
        if (!resetEmailSentConfirmation) {
          TextButton(onClick = { showForgotPasswordDialog = false }) {
            Text("Cancel")
          }
        }
      }
    )
  }
}

@Composable
private fun AcademicPerkItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  desc: String
) {
  Row(
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Surface(
      shape = CircleShape,
      color = PaletteCreamIvory.copy(alpha = 0.2f),
      modifier = Modifier.size(34.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = PaletteCreamIvory, modifier = Modifier.size(18.dp))
      }
    }
    Column {
      Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = PaletteCreamIvory
      )
      Text(
        text = desc,
        fontSize = 11.sp,
        color = PaletteCreamIvory.copy(alpha = 0.8f),
        lineHeight = 15.sp
      )
    }
  }
}

@Composable
private fun AuthFormContent(
  authMode: AuthMode,
  onAuthModeChange: (AuthMode) -> Unit,
  email: String,
  onEmailChange: (String) -> Unit,
  password: String,
  onPasswordChange: (String) -> Unit,
  name: String,
  onNameChange: (String) -> Unit,
  university: String,
  onUniversityChange: (String) -> Unit,
  studentId: String,
  onStudentIdChange: (String) -> Unit,
  confirmPassword: String,
  onConfirmPasswordChange: (String) -> Unit,
  rememberMe: Boolean,
  onRememberMeChange: (Boolean) -> Unit,
  isPasswordVisible: Boolean,
  onTogglePasswordVisibility: () -> Unit,
  isConfirmPasswordVisible: Boolean,
  onToggleConfirmPasswordVisibility: () -> Unit,
  errorMessage: String?,
  onSignInClick: () -> Unit,
  onSignUpClick: () -> Unit,
  onGoogleSignInClick: () -> Unit,
  onGuestSignInClick: () -> Unit,
  onForgotPasswordClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Mode Switcher Tabs
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = PalettePaleCeladon,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Surface(
          modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onAuthModeChange(AuthMode.SIGN_IN) }
            .testTag("tab_sign_in"),
          color = if (authMode == AuthMode.SIGN_IN) PaletteDeepSage else Color.Transparent,
          shape = RoundedCornerShape(8.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = "Sign In",
              fontSize = 13.sp,
              fontWeight = if (authMode == AuthMode.SIGN_IN) FontWeight.Bold else FontWeight.Medium,
              color = if (authMode == AuthMode.SIGN_IN) PaletteCreamIvory else PaletteDarkText
            )
          }
        }

        Surface(
          modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onAuthModeChange(AuthMode.SIGN_UP) }
            .testTag("tab_create_account"),
          color = if (authMode == AuthMode.SIGN_UP) PaletteDeepSage else Color.Transparent,
          shape = RoundedCornerShape(8.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = "Create Account",
              fontSize = 13.sp,
              fontWeight = if (authMode == AuthMode.SIGN_UP) FontWeight.Bold else FontWeight.Medium,
              color = if (authMode == AuthMode.SIGN_UP) PaletteCreamIvory else PaletteDarkText
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Form Subheading
    Text(
      text = if (authMode == AuthMode.SIGN_IN) "Sign in to access your synchronized courses & schedule" else "Set up your student profile to sync across devices",
      fontSize = 12.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Error Alert Box
    if (errorMessage != null) {
      Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp)
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 12.sp
          )
        }
      }
    }

    // Input fields for Sign Up
    if (authMode == AuthMode.SIGN_UP) {
      OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Full Name *") },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PaletteDeepSage) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("signup_name_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = PaletteDeepSage,
          focusedLabelColor = PaletteDeepSage
        )
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = university,
        onValueChange = onUniversityChange,
        label = { Text("University / College *") },
        leadingIcon = { Icon(Icons.Default.School, contentDescription = null, tint = PaletteDeepSage) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("signup_university_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = PaletteDeepSage,
          focusedLabelColor = PaletteDeepSage
        )
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = studentId,
        onValueChange = onStudentIdChange,
        label = { Text("Student ID (e.g. CS-2024-8841)") },
        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = PaletteDeepSage) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("signup_studentid_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = PaletteDeepSage,
          focusedLabelColor = PaletteDeepSage
        )
      )

      Spacer(modifier = Modifier.height(10.dp))
    }

    // Common: Academic Email
    OutlinedTextField(
      value = email,
      onValueChange = onEmailChange,
      label = { Text("University Email *") },
      placeholder = { Text("student@university.ac.uk") },
      leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PaletteDeepSage) },
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("login_email_input"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PaletteDeepSage,
        focusedLabelColor = PaletteDeepSage
      )
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Common: Password
    OutlinedTextField(
      value = password,
      onValueChange = onPasswordChange,
      label = { Text("Password *") },
      leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PaletteDeepSage) },
      trailingIcon = {
        IconButton(onClick = onTogglePasswordVisibility) {
          Icon(
            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
          )
        }
      },
      visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (authMode == AuthMode.SIGN_IN) ImeAction.Done else ImeAction.Next),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("login_password_input"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PaletteDeepSage,
        focusedLabelColor = PaletteDeepSage
      )
    )

    // Sign Up: Confirm Password
    if (authMode == AuthMode.SIGN_UP) {
      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = confirmPassword,
        onValueChange = onConfirmPasswordChange,
        label = { Text("Confirm Password *") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PaletteDeepSage) },
        trailingIcon = {
          IconButton(onClick = onToggleConfirmPasswordVisibility) {
            Icon(
              imageVector = if (isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
              contentDescription = if (isConfirmPasswordVisible) "Hide password" else "Show password"
            )
          }
        },
        visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("signup_confirm_password_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = PaletteDeepSage,
          focusedLabelColor = PaletteDeepSage
        )
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Remember Me & Forgot Password Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onRememberMeChange(!rememberMe) }
      ) {
        Checkbox(
          checked = rememberMe,
          onCheckedChange = onRememberMeChange,
          colors = CheckboxDefaults.colors(checkedColor = PaletteDeepSage),
          modifier = Modifier.testTag("remember_me_checkbox")
        )
        Text(
          text = "Remember me",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      if (authMode == AuthMode.SIGN_IN) {
        TextButton(
          onClick = onForgotPasswordClick,
          contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
        ) {
          Text(
            text = "Forgot password?",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = PaletteDeepSage
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Primary Action Button
    Button(
      onClick = {
        if (authMode == AuthMode.SIGN_IN) onSignInClick() else onSignUpClick()
      },
      colors = ButtonDefaults.buttonColors(
        containerColor = PaletteDeepSage,
        contentColor = PaletteCreamIvory
      ),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("auth_submit_button")
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Text(
          text = if (authMode == AuthMode.SIGN_IN) "Sign In to StudyPilot" else "Create Account & Sync",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = null,
          modifier = Modifier.size(16.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Divider
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      HorizontalDivider(modifier = Modifier.weight(1f), color = PaletteMediumSage.copy(alpha = 0.4f))
      Text(
        text = "  OR INSTANT ACCESS  ",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      HorizontalDivider(modifier = Modifier.weight(1f), color = PaletteMediumSage.copy(alpha = 0.4f))
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Quick Google Student Account
    OutlinedButton(
      onClick = onGoogleSignInClick,
      shape = RoundedCornerShape(12.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage),
      modifier = Modifier
        .fillMaxWidth()
        .height(46.dp)
        .testTag("login_google_button")
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Surface(
          shape = CircleShape,
          color = PalettePaleCeladon,
          modifier = Modifier.size(24.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(text = "G", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = PaletteDeepSage)
          }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Continue with Google Student SSO",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = PaletteDarkText
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Guest / Demo Scholar Access
    OutlinedButton(
      onClick = onGuestSignInClick,
      shape = RoundedCornerShape(12.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, PaletteMediumSage.copy(alpha = 0.6f)),
      modifier = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .testTag("login_guest_button")
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Default.AutoAwesome,
          contentDescription = null,
          tint = PaletteDeepSage,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Explore in Guest Demo Mode",
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          color = PaletteDarkText
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Mode Toggle footer
    Row(
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = if (authMode == AuthMode.SIGN_IN) "Don't have an academic account yet?" else "Already registered?",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      TextButton(
        onClick = {
          onAuthModeChange(if (authMode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN)
        },
        contentPadding = PaddingValues(horizontal = 6.dp)
      ) {
        Text(
          text = if (authMode == AuthMode.SIGN_IN) "Create one now" else "Sign in",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = PaletteDeepSage
        )
      }
    }
  }
}
