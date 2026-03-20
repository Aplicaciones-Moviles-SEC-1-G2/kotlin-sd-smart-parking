package com.example.sd_smart_parking_app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sd_smart_parking_app.ui.theme.DarkText
import com.example.sd_smart_parking_app.ui.theme.NavigationBlue
import com.example.sd_smart_parking_app.ui.theme.Spacing
import com.example.sd_smart_parking_app.ui.theme.White

@Composable
fun CompassArrow(
    bearing: Float,
    modifier: Modifier = Modifier
) {
    // Animar la rotación de la flecha
    val animatedBearing by animateFloatAsState(targetValue = bearing, label = "bearing")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Caja circular con la brújula
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(
                    color = NavigationBlue,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Círculo interior (fondo blanco)
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .background(
                        color = White,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Flecha que rota
                Box(
                    modifier = Modifier.rotate(animatedBearing)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Flecha hacia arriba
                        Text(
                            text = "↑",
                            fontSize = 60.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavigationBlue
                        )
                    }
                }

                // Texto del bearing en grados
                Text(
                    text = "${bearing.toInt()}°",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // Texto indicador
        Text(
            text = "Moving Direction",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(Spacing.md)
        )
    }
}