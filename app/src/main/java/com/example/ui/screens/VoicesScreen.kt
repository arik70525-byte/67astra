package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.JarvisVoice
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBlue
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.viewmodel.JarvisViewModel
import java.util.Locale

@Composable
fun VoicesScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val voices by viewModel.voicesList.collectAsState()
    val selectedVoiceId by viewModel.selectedVoiceId.collectAsState()
    val customPitch by viewModel.customPitch.collectAsState()
    val customSpeed by viewModel.customSpeed.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(14.dp)
    ) {
        // Section Header
        Text(
            text = "ГОЛОСОВЫЕ МОДУЛИ ДЖАРВИСА",
            color = JarvisCyan,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "10 фирменных кибер-пресетов речи + точный кастомный синтезатор частот.",
            color = JarvisTextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(voices, key = { it.id }) { voice ->
                VoiceItemCard(
                    voice = voice,
                    isSelected = voice.id == selectedVoiceId,
                    onSelect = { viewModel.selectVoice(voice.id) }
                )
            }

            // Custom Voice Tuning Panel
            item {
                CustomVoiceTunerCard(
                    pitch = customPitch,
                    speed = customSpeed,
                    onPitchChange = { viewModel.updateCustomVoice(it, customSpeed) },
                    onSpeedChange = { viewModel.updateCustomVoice(customPitch, it) },
                    onTestClick = { viewModel.testCustomVoice() }
                )
            }
        }
    }
}

@Composable
fun VoiceItemCard(
    voice: JarvisVoice,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("voice_item_${voice.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) JarvisCyan.copy(alpha = 0.12f) else JarvisSurfaceVariant
        ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.5.dp,
            if (isSelected) JarvisCyan else JarvisCardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) JarvisCyan else JarvisSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFF001F26) else JarvisTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = voice.name,
                        color = if (isSelected) JarvisCyan else JarvisTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(JarvisCardBorder)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = voice.accentTag,
                            color = JarvisTextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = voice.description,
                    color = JarvisTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            IconButton(
                onClick = onSelect,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Прослушать",
                    tint = if (isSelected) JarvisCyan else JarvisTextSecondary
                )
            }
        }
    }
}

@Composable
fun CustomVoiceTunerCard(
    pitch: Float,
    speed: Float,
    onPitchChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onTestClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, JarvisAmber.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = JarvisAmber, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ПАРАМЕТРЫ КАСТОМНОГО ГОЛОСА",
                    color = JarvisAmber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pitch slider
            Text(
                text = String.format(Locale.US, "Тональность / Высота тона: %.2fx", pitch),
                color = JarvisTextPrimary,
                fontSize = 12.sp
            )
            Slider(
                value = pitch,
                onValueChange = onPitchChange,
                valueRange = 0.5f..1.8f,
                colors = SliderDefaults.colors(
                    thumbColor = JarvisAmber,
                    activeTrackColor = JarvisAmber,
                    inactiveTrackColor = JarvisCardBorder
                )
            )

            // Speed slider
            Text(
                text = String.format(Locale.US, "Скорость / Темп речи: %.2fx", speed),
                color = JarvisTextPrimary,
                fontSize = 12.sp
            )
            Slider(
                value = speed,
                onValueChange = onSpeedChange,
                valueRange = 0.6f..1.8f,
                colors = SliderDefaults.colors(
                    thumbColor = JarvisCyan,
                    activeTrackColor = JarvisCyan,
                    inactiveTrackColor = JarvisCardBorder
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onTestClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("test_custom_voice_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JarvisAmber,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ТЕСТИРОВАТЬ КАСТОМНЫЙ СИНТЕЗ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
