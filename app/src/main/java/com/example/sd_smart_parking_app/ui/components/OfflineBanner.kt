package com.example.sd_smart_parking_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.Typography
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.sd_smart_parking_app.R

@Composable
fun OfflineBanner(
    message: String,
    subMessage: String = "Showing last available data",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFFFF3E0),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Image(
            painter = painterResource(id = R.drawable.goat_offline),
            contentDescription = "Offline",
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "You're offline",
            style = Typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE65100)
        )
        Text(
            text = message,
            style = Typography.bodySmall,
            color = Color(0xFF5D4037),
            textAlign = TextAlign.Center
        )
        Text(
            text = subMessage,
            style = Typography.bodySmall,
            color = Color(0xFF8D6E63),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}