package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AssistantStatus
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MatrixGreen
import com.example.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OrbVisualizer(
    status: AssistantStatus,
    audioRms: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbital_rotation"
    )

    val waveHeight by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_height"
    )

    val orbColor = when (status) {
        AssistantStatus.LISTENING -> CyanNeon
        AssistantStatus.PROCESSING -> MatrixGreen
        AssistantStatus.SPEAKING -> ElectricBlue
        AssistantStatus.IDLE -> CyanNeon
    }

    Box(
        modifier = modifier
            .size(130.dp)
            .testTag("ai_orb_visualizer")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(130.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.width * 0.28f

            // Dynamic amplification based on audioRms or speaking
            val dynamicScale = when (status) {
                AssistantStatus.LISTENING -> (1f + (audioRms.coerceIn(0f, 10f) / 15f)) * pulseScale
                AssistantStatus.SPEAKING -> (1f + (waveHeight * 0.15f))
                AssistantStatus.PROCESSING -> 1.05f
                AssistantStatus.IDLE -> pulseScale
            }

            // Outer subtle energy rings
            drawCircle(
                color = orbColor.copy(alpha = 0.08f),
                radius = baseRadius * dynamicScale * 1.6f,
                center = center
            )
            drawCircle(
                color = orbColor.copy(alpha = 0.15f),
                radius = baseRadius * dynamicScale * 1.3f,
                center = center
            )

            // Orbital arc ring
            val orbitalRadius = baseRadius * 1.25f
            val radAngle = Math.toRadians(rotationAngle.toDouble())
            val arcStart = (rotationAngle * 1.5f) % 360f
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color.Transparent,
                        orbColor.copy(alpha = 0.8f),
                        Color.Transparent
                    ),
                    center = center
                ),
                startAngle = arcStart,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - orbitalRadius, center.y - orbitalRadius),
                size = androidx.compose.ui.geometry.Size(orbitalRadius * 2, orbitalRadius * 2),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Orbiting particle dot
            val dotX = center.x + (orbitalRadius * cos(radAngle)).toFloat()
            val dotY = center.y + (orbitalRadius * sin(radAngle)).toFloat()
            drawCircle(
                color = orbColor,
                radius = 3.5.dp.toPx(),
                center = Offset(dotX, dotY)
            )

            // Core Gradient Glow Orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        orbColor.copy(alpha = 0.95f),
                        orbColor.copy(alpha = 0.6f),
                        orbColor.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * dynamicScale
                ),
                radius = baseRadius * dynamicScale,
                center = center
            )

            // Inner crisp contour ring
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = baseRadius * 0.75f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Center Icon based on status
        val icon = when (status) {
            AssistantStatus.LISTENING -> Icons.Default.Mic
            AssistantStatus.SPEAKING -> Icons.Default.RecordVoiceOver
            AssistantStatus.PROCESSING -> Icons.Default.SmartToy
            AssistantStatus.IDLE -> Icons.Default.Mic
        }

        Icon(
            imageVector = icon,
            contentDescription = "MAX Assistant Status: $status",
            tint = TextPrimary,
            modifier = Modifier.size(28.dp)
        )
    }
}
