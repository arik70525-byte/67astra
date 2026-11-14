package com.example.ui.components

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBlue
import com.example.ui.theme.JarvisCoreGlow
import com.example.ui.theme.JarvisCyan
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HolographicArcReactor(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 220.dp,
    isActive: Boolean = true,
    isListening: Boolean = false,
    audioEnergy: Float = 0f,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reactor_spin")

    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 4000 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rot"
    )

    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 3000 else 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rot"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 500 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    Box(
        modifier = modifier
            .size(sizeDp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.90f
            val audioScale = 1f + (audioEnergy * 0.08f).coerceAtMost(0.4f)
            val currentPulse = corePulse * audioScale

            val primaryCyan = if (isListening) JarvisAmber else JarvisCyan
            val secondaryBlue = if (isListening) JarvisCyan else JarvisBlue

            // 1. Outer Tech Ring with Tick marks
            rotate(outerRotation, pivot = center) {
                drawCircle(
                    color = primaryCyan.copy(alpha = 0.25f),
                    radius = baseRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // 24 Tech ticks
                val tickCount = 24
                for (i in 0 until tickCount) {
                    val angleRad = (i * (360f / tickCount)) * (Math.PI.toFloat() / 180f)
                    val r1 = baseRadius - 10.dp.toPx()
                    val r2 = baseRadius - 2.dp.toPx()
                    val start = Offset(center.x + r1 * cos(angleRad), center.y + r1 * sin(angleRad))
                    val end = Offset(center.x + r2 * cos(angleRad), center.y + r2 * sin(angleRad))
                    val isMainTick = i % 4 == 0
                    drawLine(
                        color = if (isMainTick) primaryCyan else secondaryBlue.copy(alpha = 0.5f),
                        start = start,
                        end = end,
                        strokeWidth = if (isMainTick) 3.dp.toPx() else 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 2. Middle segmented arcs
            rotate(innerRotation, pivot = center) {
                val midRadius = baseRadius * 0.72f
                val sweepSegments = 6
                val sweepAngle = 360f / sweepSegments
                for (s in 0 until sweepSegments) {
                    val startAngle = s * sweepAngle
                    drawArc(
                        color = secondaryBlue.copy(alpha = 0.8f),
                        startAngle = startAngle + 8f,
                        sweepAngle = sweepAngle - 16f,
                        useCenter = false,
                        topLeft = Offset(center.x - midRadius, center.y - midRadius),
                        size = androidx.compose.ui.geometry.Size(midRadius * 2f, midRadius * 2f),
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // 3. Inner Glowing Reactor Core
            val coreRadius = baseRadius * 0.42f * currentPulse
            val gradientBrush = Brush.radialGradient(
                colors = listOf(
                    JarvisCoreGlow.copy(alpha = 0.9f),
                    primaryCyan.copy(alpha = 0.6f),
                    secondaryBlue.copy(alpha = 0.2f),
                    Color.Transparent
                ),
                center = center,
                radius = coreRadius * 1.4f
            )
            drawCircle(
                brush = gradientBrush,
                radius = coreRadius * 1.3f,
                center = center
            )

            // 4. Central Reactor Triangle (Iron Man Arc Reactor trademark)
            val triSize = coreRadius * 0.75f
            val triPath = Path().apply {
                val p1 = Offset(center.x, center.y - triSize)
                val p2 = Offset(center.x + triSize * 0.866f, center.y + triSize * 0.5f)
                val p3 = Offset(center.x - triSize * 0.866f, center.y + triSize * 0.5f)
                moveTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                close()
            }

            drawPath(
                path = triPath,
                color = primaryCyan.copy(alpha = 0.35f)
            )
            drawPath(
                path = triPath,
                color = JarvisCoreGlow,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Central Core LED
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx() * currentPulse,
                center = center
            )
        }
    }
}
