package com.example.sd_smart_parking_app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sd_smart_parking_app.ui.theme.CornerRadius
import com.example.sd_smart_parking_app.ui.theme.DarkText
import com.example.sd_smart_parking_app.ui.theme.PrimaryYellow
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import com.example.sd_smart_parking_app.ui.theme.White

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryYellow,
            contentColor = DarkText,
            disabledContainerColor = PrimaryYellow.copy(alpha = 0.5f),
            disabledContentColor = DarkText.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(CornerRadius.lg),
        enabled = enabled
    ) {
        Text(
            text = text,
            style = Typography.labelLarge
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DarkText,
            contentColor = White,
            disabledContainerColor = DarkText.copy(alpha = 0.5f),
            disabledContentColor = White.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(CornerRadius.lg),
        enabled = enabled
    ) {
        Text(
            text = text,
            style = Typography.labelLarge
        )
    }
}