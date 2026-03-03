package com.example.sd_smart_parking_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sd_smart_parking_app.ui.theme.BorderGray
import com.example.sd_smart_parking_app.ui.theme.CornerRadius
import com.example.sd_smart_parking_app.ui.theme.DarkText
import com.example.sd_smart_parking_app.ui.theme.HighAvailabilityGreen
import com.example.sd_smart_parking_app.ui.theme.MediumAvailabilityOrange
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography

@Composable
fun AvailabilityProgressBar(
    percentage: Int,
    modifier: Modifier = Modifier
) {
    val progressColor = when {
        percentage >= 50 -> HighAvailabilityGreen
        percentage >= 25 -> MediumAvailabilityOrange
        else -> com.example.sd_smart_parking_app.ui.theme.ErrorRed
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(
                color = BorderGray,
                shape = RoundedCornerShape(CornerRadius.sm)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = percentage / 100f)
                .height(8.dp)
                .background(
                    color = progressColor,
                    shape = RoundedCornerShape(CornerRadius.sm)
                )
        )
    }
}

@Composable
fun AvailabilityProgressBarWithLabel(
    percentage: Int,
    label: String = "Availability",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = Typography.bodySmall
            )
            Text(
                text = "$percentage%",
                style = Typography.bodySmall
            )
        }
        AvailabilityProgressBar(
            percentage = percentage,
            modifier = Modifier.padding(top = Spacing.sm)
        )
    }
}