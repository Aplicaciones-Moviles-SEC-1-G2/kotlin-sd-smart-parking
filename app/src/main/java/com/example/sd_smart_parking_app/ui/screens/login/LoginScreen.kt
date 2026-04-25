package com.example.sd_smart_parking_app.ui.screens.login

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sd_smart_parking_app.ui.components.PrimaryButton
import com.example.sd_smart_parking_app.ui.theme.BackgroundWhite
import com.example.sd_smart_parking_app.ui.theme.BorderGray
import com.example.sd_smart_parking_app.ui.theme.CornerRadius
import com.example.sd_smart_parking_app.ui.theme.DarkText
import com.example.sd_smart_parking_app.ui.theme.MediumGray
import com.example.sd_smart_parking_app.ui.theme.NavigationBlue
import com.example.sd_smart_parking_app.ui.theme.SmartParkingTheme
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import com.example.sd_smart_parking_app.ui.theme.ErrorRed
import com.example.sd_smart_parking_app.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: (String, String) -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel()
) {
    val savedEmail by viewModel.savedEmail.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }

    // Al cargar la pantalla, verificar preferencias de "Remember Me"
    LaunchedEffect(Unit) {
        val preferences = viewModel.getRememberMePreferences()
        if (preferences.isRememberMeEnabled && preferences.savedEmail != null) {
            email = preferences.savedEmail
            rememberMe = true
            
            // Auto-login: refresh the 30-day session timer on each successful entry
            if (viewModel.checkAndAutoLogin()) {
                viewModel.refreshAutoLoginSession()
                onLoginSuccess(email, "auto_login")
            }
        } else if (savedEmail != null) {
            email = savedEmail!!
            rememberMe = false
        }
    }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    val isFormValid = email.isNotBlank() && password.isNotBlank()
    
    // Biometría disponible si hay credenciales guardadas, independientemente de Remember Me
    val canUseBiometrics = savedEmail != null

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Spacing.lg)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Offline Warning
            if (isOffline) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ErrorRed.copy(alpha = 0.1f), RoundedCornerShape(CornerRadius.sm))
                        .padding(Spacing.sm)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WifiOff, contentDescription = null, tint = ErrorRed)
                        Spacer(modifier = Modifier.size(Spacing.xs))
                        Text(
                            text = "Offline mode. Using locally saved credentials.",
                            style = Typography.bodySmall,
                            color = ErrorRed
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            // Header
            Text(
                text = "SD Building Parking",
                style = Typography.displaySmall,
                color = DarkText,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Andes University",
                style = Typography.bodyMedium,
                color = MediumGray,
                modifier = Modifier.padding(top = Spacing.xs)
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            // Login Form
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (errorMessage != null) viewModel.clearError()
                    },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(CornerRadius.md),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkText,
                        unfocusedBorderColor = BorderGray,
                        focusedLabelColor = DarkText,
                        unfocusedLabelColor = MediumGray
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (errorMessage != null) viewModel.clearError()
                    },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(CornerRadius.md),
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkText,
                        unfocusedBorderColor = BorderGray,
                        focusedLabelColor = DarkText,
                        unfocusedLabelColor = MediumGray
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    enabled = !isLoading
                )
            }

            // Remember Me Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { 
                        rememberMe = it
                        viewModel.setRememberMe(email, it)
                    },
                    enabled = !isLoading,
                    colors = CheckboxDefaults.colors(
                        checkedColor = NavigationBlue,
                        uncheckedColor = MediumGray
                    )
                )
                Text(
                    text = "Remember me on this device",
                    style = Typography.bodySmall,
                    color = DarkText,
                    modifier = Modifier.padding(start = Spacing.sm)
                )
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = ErrorRed,
                    style = Typography.bodySmall,
                    modifier = Modifier.padding(top = Spacing.md),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Login Button
            PrimaryButton(
                text = if (isLoading) "Signing in..." else "Log In",
                onClick = {
                    if (isFormValid && !isLoading) {
                        viewModel.setRememberMe(email, rememberMe)
                        viewModel.loginWithEmail(email, password) {
                            viewModel.logLoginMethod("email")
                            onLoginSuccess(email, "manual")
                        }
                    }
                },
                enabled = isFormValid && !isLoading
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Biometric Section
            if (canUseBiometrics) {
                IconButton(
                    onClick = {
                        val activity = context as? FragmentActivity
                        if (activity != null) {
                            viewModel.loginWithBiometrics(activity) {
                                viewModel.logLoginMethod("biometric")
                                onLoginSuccess(savedEmail ?: email, "biometric")
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Login with Fingerprint",
                        tint = NavigationBlue,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else {
                Text(
                    text = "Sign in manually first to enable biometric authentication",
                    style = Typography.bodySmall,
                    color = MediumGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = MediumGray.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp).padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = "Forgot your password?",
                style = Typography.labelMedium,
                color = MediumGray
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Link to Register
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account? ",
                    style = Typography.bodyMedium,
                    color = MediumGray
                )
                Text(
                    text = "Register",
                    style = Typography.labelLarge,
                    color = NavigationBlue,
                    modifier = Modifier.clickable(enabled = !isLoading && !isOffline) { onRegisterClick() }
                )
            }
        }
    }
}
