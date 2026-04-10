package com.example.sd_smart_parking_app.ui.screens.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    // Al cargar la pantalla, si existe un email guardado, lo autocompletamos
    LaunchedEffect(savedEmail) {
        if (savedEmail != null) {
            email = savedEmail!!
        }
    }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    val isFormValid = email.isNotBlank() && password.isNotBlank()
    val hasBiometricsEnabled = savedEmail != null

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
            // Header
            Text(
                text = "SD Building Parking",
                style = Typography.displaySmall,
                color = DarkText,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Universidad de los Andes",
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

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = ErrorRed,
                    style = Typography.bodySmall,
                    modifier = Modifier.padding(top = Spacing.md)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Login Button
            PrimaryButton(
                text = if (isLoading) "Signing in..." else "Log In",
                onClick = { 
                    if (isFormValid && !isLoading) {
                        viewModel.loginWithEmail(email, password) {
                            viewModel.logLoginMethod("email") // <--- Registro de analítica
                            onLoginSuccess(email, "password_hidden")
                        }
                    }
                },
                enabled = isFormValid && !isLoading
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Biometric Section
            if (hasBiometricsEnabled) {
                IconButton(
                    onClick = {
                        val activity = context as? FragmentActivity
                        if (activity != null) {
                            viewModel.loginWithBiometrics(activity) {
                                viewModel.logLoginMethod("biometric") // <--- Registro de analítica
                                onLoginSuccess(savedEmail ?: "biometric_user", "biometric_token")
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
                    text = "Inicia sesión manual para activar biometría",
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
                    modifier = Modifier.clickable(enabled = !isLoading) { onRegisterClick() }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    SmartParkingTheme {
        LoginScreen(
            onLoginSuccess = { _, _ -> },
            onRegisterClick = {}
        )
    }
}
