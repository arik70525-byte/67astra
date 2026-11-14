package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGold

@Composable
fun SimulatedTapOverlay(
    tapCoordinates: Pair<Float, Float>?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = tapCoordinates != null,
        enter = fadeIn() + scaleIn(initialScale = 0.5f),
        exit = fadeOut() + scaleOut(targetScale = 1.8f),
        modifier = modifier.fillMaxSize()
    ) {
        if (tapCoordinates != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val targetX = size.width * tapCoordinates.first
                val targetY = size.height * tapCoordinates.second
                val tapCenter = Offset(targetX, targetY)

                // Expanding sonar rings
                drawCircle(
                    color = JarvisCyan.copy(alpha = 0.8f),
                    radius = 32.dp.toPx(),
                    center = tapCenter,
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = JarvisGold.copy(alpha = 0.6f),
                    radius = 54.dp.toPx(),
                    center = tapCenter,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // Center pinpoint
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = tapCenter
                )
            }
        }
    }
}
