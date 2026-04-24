package com.example.sd_smart_parking_app.ui.screens.register

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: (String, String, String, String, String, String, String) -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var vehicleModel by remember { mutableStateOf("") }
    var vehiclePlate by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    var expanded by remember { mutableStateOf(false) }
    val roles = listOf("Driver", "Manager")

    // Validation Regex
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]{2,}$".toRegex()
    val plateRegex = "^[A-Z]{3}[0-9]{3}$".toRegex()
    val phoneRegex = "^[0-9]{8,15}$".toRegex()

    // Validation logic
    val isEmailValid = email.matches(emailRegex)
    val isPlateValid = vehiclePlate.uppercase().matches(plateRegex)
    val isPhoneValid = phone.matches(phoneRegex)
    val isPasswordValid = password.length >= 6
    val isConfirmPasswordValid = password == confirmPassword && confirmPassword.isNotEmpty()

    val isFormValid = name.isNotBlank() && 
                      isEmailValid && 
                      isPhoneValid &&
                      vehicleModel.isNotBlank() &&
                      isPlateValid &&
                      role.isNotBlank() &&
                      isPasswordValid && 
                      isConfirmPasswordValid

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
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(Spacing.xl))

            // Header
            Text(
                text = "SD Building Parking",
                style = Typography.displaySmall,
                color = DarkText,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Create your account",
                style = Typography.bodyMedium,
                color = MediumGray,
                modifier = Modifier.padding(top = Spacing.xs)
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Register Form
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        if (it.length <= 50) name = it
                        if (errorMessage != null) viewModel.clearError()
                    },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(CornerRadius.md),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkText,
                        unfocusedBorderColor = BorderGray,
                        focusedLabelColor = DarkText,
                        unfocusedLabelColor = MediumGray
                    ),
                    singleLine = true,
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        if (it.length <= 50) email = it
                        if (errorMessage != null) viewModel.clearError()
                    },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(CornerRadius.md),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (email.isEmpty() || isEmailValid) DarkText else ErrorRed,
                        unfocusedBorderColor = if (email.isEmpty() || isEmailValid) BorderGray else ErrorRed,
                        focusedLabelColor = DarkText,
                        unfocusedLabelColor = MediumGray
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    enabled = !isLoading,
                    supportingText = {
                        if (email.isNotEmpty() && !isEmailValid) {
                            Text("Invalid email format", color = ErrorRed)
                        }
                    }
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { 
                        if (it.length <= 15 && it.all { char -> char.isDigit() }) phone = it
                        if (errorMessage != null) viewModel.clearError()
                    },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(CornerRadius.md),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (phone.isEmpty() || isPhoneValid) DarkText else ErrorRed,
                        unfocusedBorderColor = if (phone.isEmpty() || isPhoneValid) BorderGray else ErrorRed,
                        focusedLabelColor = DarkText,
                        unfocusedLabelColor = MediumGray
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    enabled = !isLoading,
                    supportingText = {
                        if (phone.isNotEmpty() && !isPhoneValid) {
                            Text("Use 8-15 digits", color = ErrorRed)
                        }
                    }
                )

                OutlinedTextField(
                    value = vehicleModel,
                    onValueChange = { 
                        if (it.length <= 30) vehicleModel = it
                        if (errorMessage != null) viewModel.clearError()
                    },
                    label = { Text("Vehicle Model") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(CornerRadius.md),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkText,
                        unfocusedBorderColor = BorderGray,
                        focusedLabelColor = DarkText,
                        unfocusedLabelColor = MediumGray
                    ),
                    singleLine = true,
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = vehiclePlate,
                    onValueChange = { 
                        if (it.length <= 6) vehiclePlate = it.uppercase()
                        if (errorMessage != null) viewModel.clearError()
                    },
                    label = { Text("Vehicle Plate (e.g. ASD123)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(CornerRadius.md),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (vehiclePlate.isEmpty() || isPlateValid) DarkText else ErrorRed,
                        unfocusedBorderColor = if (vehiclePlate.isEmpty() || isPlateValid) BorderGray else ErrorRed,
                        focusedLabelColor = DarkText,
                        unfocusedLabelColor = MediumGray
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    singleLine = true,
                    enabled = !isLoading,
                    supportingText = {
                        if (vehiclePlate.isNotEmpty() && !isPlateValid) {
                            Text("Format: 3 letters + 3 numbers", color = ErrorRed)
                        }
                    }
                )

                // Role Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (!isLoading) expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkText,
                            unfocusedBorderColor = BorderGray,
                            focusedLabelColor = DarkText,
                            unfocusedLabelColor = MediumGray
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(CornerRadius.md)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        roles.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    role = selectionOption
                                    expanded = false
                                    if (errorMessage != null) viewModel.clearError()
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        if (it.length <= 20) password = it
                        if (errorMessage != null) viewModel.clearError()
                    },
                    label = { Text("Password (min. 6 chars)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(CornerRadius.md),
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (password.isEmpty() || isPasswordValid) DarkText else ErrorRed,
                        unfocusedBorderColor = if (password.isEmpty() || isPasswordValid) BorderGray else ErrorRed,
                        focusedLabelColor = DarkText,
                        unfocusedLabelColor = MediumGray
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    enabled = !isLoading,
                    supportingText = {
                        if (password.isNotEmpty() && !isPasswordValid) {
                            Text("Password too short", color = ErrorRed)
                        }
                    }
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { 
                        if (it.length <= 20) confirmPassword = it
                        if (errorMessage != null) viewModel.clearError()
                    },
                    label = { Text("Confirm Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(CornerRadius.md),
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (confirmPassword.isEmpty() || isConfirmPasswordValid) DarkText else ErrorRed,
                        unfocusedBorderColor = if (confirmPassword.isEmpty() || isConfirmPasswordValid) BorderGray else ErrorRed,
                        focusedLabelColor = DarkText,
                        unfocusedLabelColor = MediumGray
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    enabled = !isLoading,
                    supportingText = {
                        if (confirmPassword.isNotEmpty() && !isConfirmPasswordValid) {
                            Text("Passwords do not match", color = ErrorRed)
                        }
                    }
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

            // Register Button
            PrimaryButton(
                text = if (isLoading) "Registering..." else "Register",
                onClick = {
                    if (isFormValid && !isLoading) {
                        viewModel.register(name, email, password) {
                            onRegisterSuccess(name, email, phone, vehicleModel, vehiclePlate, role, password)
                        }
                    }
                },
                enabled = isFormValid && !isLoading
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = Typography.bodyMedium,
                    color = MediumGray
                )
                Text(
                    text = "Sign In",
                    style = Typography.labelLarge,
                    color = NavigationBlue,
                    modifier = Modifier.clickable(enabled = !isLoading) { onLoginClick() }
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    SmartParkingTheme {
        RegisterScreen(onRegisterSuccess = { _, _, _, _, _, _, _ -> }, onLoginClick = {})
    }
}
