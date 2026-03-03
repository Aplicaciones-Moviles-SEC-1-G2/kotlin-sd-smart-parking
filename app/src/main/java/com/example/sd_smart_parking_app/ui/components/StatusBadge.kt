package com.example.sd_smart_parking_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sd_smart_parking_app.ui.theme.CornerRadius
import com.example.sd_smart_parking_app.ui.theme.ErrorRed
import com.example.sd_smart_parking_app.ui.theme.HighAvailabilityGreen
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import com.example.sd_smart_parking_app.ui.theme.White

@Composable
fun StatusBadge(
    text: String,
    status: BadgeStatus,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = Typography.labelMedium.copy(color = White),
        modifier = modifier
            .background(
                color = when (status) {
                    BadgeStatus.SUCCESS -> HighAvailabilityGreen
                    BadgeStatus.ERROR -> ErrorRed
                    BadgeStatus.WARNING -> com.example.sd_smart_parking_app.ui.theme.WarningYellow
                },
                shape = RoundedCornerShape(CornerRadius.md)
            )
            .padding(
                horizontal = Spacing.md,
                vertical = Spacing.sm
            )
    )
}

enum class BadgeStatus {
    SUCCESS,
    ERROR,
    WARNING
}