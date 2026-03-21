package com.example.sd_smart_parking_app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryYellow,
    secondary = NavigationBlue,
    tertiary = SuccessGreen,
    background = BackgroundWhite,
    surface = BackgroundLightGray,
    error = ErrorRed,
    onPrimary = DarkText,
    onSecondary = White,
    onTertiary = White,
    onBackground = DarkText,
    onSurface = DarkText,
    onError = White
)

@Composable
fun SmartParkingTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}