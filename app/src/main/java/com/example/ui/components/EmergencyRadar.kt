package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Alert
import com.example.ui.theme.CardSteel
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmergencyAmber
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarningYellow
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EmergencyRadar(
    alerts: List<Alert>,
    myLat: Double,
    myLon: Double,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    
    // Sweep rotation angle
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "SweepAngle"
    )

    // Pulsing size modifier for critical blips
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing)
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(CardSteel)
            .border(2.dp, Color(0xFF2C384E), CircleShape)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = Math.min(size.width, size.height) / 2f
            
            // Draw background coordinate grid lines
            drawLine(
                color = Color(0xFF222D3D),
                start = Offset(0f, center.y),
                end = Offset(size.width, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color(0xFF222D3D),
                start = Offset(center.x, 0f),
                end = Offset(center.x, size.height),
                strokeWidth = 1.dp.toPx()
            )

            // Draw concentric rings for 5, 10, 15, 20, 25 mile perimeters
            val ringStep = radius / 5f
            for (i in 1..5) {
                drawCircle(
                    color = if (i == 5) DangerRed.copy(alpha = 0.5f) else Color(0xFF1E2836),
                    radius = ringStep * i,
                    center = center,
                    style = Stroke(width = if (i == 5) 2.dp.toPx() else 1.dp.toPx())
                )
            }

            // Draw sweep arm gradient
            val sweepRad = Math.toRadians(sweepAngle.toDouble())
            val sweepX = center.x + radius * cos(sweepRad).toFloat()
            val sweepY = center.y + radius * sin(sweepRad).toFloat()
            
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        SafeGreen.copy(alpha = 0.0f),
                        SafeGreen.copy(alpha = 0.2f),
                        SafeGreen.copy(alpha = 0.4f),
                        SafeGreen.copy(alpha = 0.0f)
                    ),
                    center = center
                ),
                startAngle = sweepAngle - 45f,
                sweepAngle = 45f,
                useCenter = true
            )

            drawLine(
                color = SafeGreen.copy(alpha = 0.6f),
                start = center,
                end = Offset(sweepX, sweepY),
                strokeWidth = 2.dp.toPx()
            )

            // Draw Alert Blips mapped geographically onto the coordinate circles
            // Maximum distance on radar represents 25 miles
            val maxRadarRangeMiles = 25.0
            
            for (alert in alerts) {
                val alertLat = alert.latitude ?: continue
                val alertLon = alert.longitude ?: continue
                val distMiles = alert.distanceMiles ?: continue
                
                // Keep it on grid, cap at 25 miles
                val clampedDist = Math.min(distMiles, maxRadarRangeMiles)
                val distRatio = clampedDist / maxRadarRangeMiles
                val alertRadius = radius * distRatio.toFloat()
                
                // Calculate bearing from center
                val bearingDegrees = calculateBearing(myLat, myLon, alertLat, alertLon)
                // Convert compass heading to standard coordinate system (bearing 0 is North/UP)
                val angleRad = Math.toRadians(bearingDegrees - 90.0)
                
                val blipX = center.x + alertRadius * cos(angleRad).toFloat()
                val blipY = center.y + alertRadius * sin(angleRad).toFloat()
                
                val blipColor = when (alert.type) {
                    "AMBER" -> EmergencyAmber
                    "WEATHER" -> WarningYellow
                    "HAZMAT" -> DangerRed
                    else -> SafeGreen
                }

                // Pulse aura for critical active alerts
                drawCircle(
                    color = blipColor.copy(alpha = pulseAlpha),
                    radius = 16.dp.toPx(),
                    center = Offset(blipX, blipY)
                )

                // Core alert blip
                drawCircle(
                    color = blipColor,
                    radius = 6.dp.toPx(),
                    center = Offset(blipX, blipY)
                )
            }
        }
        
        // Dynamic labels overlay
        Text(
            text = "25 MI RADIUS DETECTOR",
            color = TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
        )
    }
}

/**
 * Great-circle navigation bearing calculation.
 */
private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLon = Math.toRadians(lon2 - lon1)
    val rLat1 = Math.toRadians(lat1)
    val rLat2 = Math.toRadians(lat2)
    val y = Math.sin(dLon) * Math.cos(rLat2)
    val x = Math.cos(rLat1) * Math.sin(rLat2) - Math.sin(rLat1) * Math.cos(rLat2) * Math.cos(dLon)
    return (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0
}
