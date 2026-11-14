package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HudLogMessage
import com.example.ui.components.HolographicArcReactor
import com.example.ui.components.SimulatedTapOverlay
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisBlue
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCoreGlow
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisRedAlert
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.viewmodel.JarvisViewModel

@Composable
fun MainHudScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsState()
    val rmsDbf by viewModel.voiceManager.rmsDbf.collectAsState()
    val recognizedText by viewModel.voiceManager.recognizedText.collectAsState()
    val isFlashlightOn by viewModel.isFlashlightOn.collectAsState()
    val noiseCancelActive by viewModel.noiseSuppressionActive.collectAsState()
    val noiseCancelLevel by viewModel.noiseSuppressionLevel.collectAsState()
    val spectralGateActive by viewModel.spectralGateActive.collectAsState()
    val wakeWordActive by viewModel.wakeWordBackgroundActive.collectAsState()
    val hudLogs by viewModel.hudLogs.collectAsState()
    val lastTap by viewModel.lastSimulatedTap.collectAsState()

    val batteryLevel = viewModel.deviceManager.getBatteryLevel()
    val isCharging = viewModel.deviceManager.isBatteryCharging()

    var manualSearchQuery by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // Cyberpunk Top HUD Status Bar
            TopHudTelemetryBar(
                batteryLevel = batteryLevel,
                isCharging = isCharging,
                noiseSuppressionLevel = (noiseCancelLevel * 100).toInt(),
                noiseSuppressionActive = noiseCancelActive,
                spectralGateActive = spectralGateActive,
                wakeWordActive = wakeWordActive
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Search Bar for Yandex & Google with "Сделай сам" fast button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualSearchQuery,
                    onValueChange = { manualSearchQuery = it },
                    placeholder = { Text("«Сделай сам открой Яндекс и...»", fontSize = 11.sp, color = JarvisTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisCardBorder,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary,
                        cursorColor = JarvisCyan
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarvisCyan)
                        .clickable {
                            if (manualSearchQuery.isNotBlank()) {
                                viewModel.processVoiceInput(manualSearchQuery)
                                manualSearchQuery = ""
                            }
                        }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("ВВОД", color = Color(0xFF001B24), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Central Holographic Reactor Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                HolographicArcReactor(
                    sizeDp = 180.dp,
                    isActive = true,
                    isListening = isListening,
                    audioEnergy = rmsDbf,
                    onClick = {
                        viewModel.deviceManager.performHapticClick()
                        if (isListening) {
                            viewModel.voiceManager.stopListening()
                        } else {
                            viewModel.voiceManager.startListening()
                        }
                    }
                )

                // State indicator pill below reactor
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isListening) JarvisAmber.copy(alpha = 0.25f)
                            else if (isSpeaking) JarvisCyan.copy(alpha = 0.25f)
                            else JarvisSurfaceVariant.copy(alpha = 0.8f)
                        )
                        .border(
                            1.dp,
                            if (isListening) JarvisAmber
                            else if (isSpeaking) JarvisCyan
                            else JarvisCardBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when {
                            isListening -> "● СЛУШАЮ ВАС, СЭР..."
                            isSpeaking -> "● ДЖАРВИС ОТВЕЧАЕТ"
                            else -> "● ЯДРО В ОЖИДАНИИ (НАЖМИТЕ ДЛЯ ГОЛОСА)"
                        },
                        color = if (isListening) JarvisAmber else if (isSpeaking) JarvisCyan else JarvisTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Active live speech transcript if present
            AnimatedVisibility(visible = recognizedText.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = recognizedText,
                            color = JarvisTextPrimary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Quick Tactical Controls (Autonomous Action, Screen Tap, Noise Cancel)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Autonomous Search demo button
                TacticalActionTile(
                    title = "Сделай сам",
                    subtitle = "Яндекс поиск",
                    icon = Icons.Default.AutoAwesome,
                    isActive = false,
                    activeColor = JarvisCyan,
                    modifier = Modifier.weight(1f),
                    testTag = "auto_action_button",
                    onClick = {
                        viewModel.executeSelfAction("сделай сам открой Яндекс и впиши в поиск привет")
                    }
                )

                // Screen Tap Simulation button
                TacticalActionTile(
                    title = "Клик экрана",
                    subtitle = "Авто-кликер",
                    icon = Icons.Default.TouchApp,
                    isActive = lastTap != null,
                    activeColor = JarvisCyan,
                    modifier = Modifier.weight(1f),
                    testTag = "simulate_screen_tap_button",
                    onClick = { viewModel.simulateTapOnScreen(0.5f, 0.45f) }
                )

                // Always-On Wake Word toggle
                TacticalActionTile(
                    title = if (wakeWordActive) "Always-On ВКЛ" else "Always-On ВЫКЛ",
                    subtitle = "Вызов с экрана",
                    icon = Icons.Default.RecordVoiceOver,
                    isActive = wakeWordActive,
                    activeColor = JarvisAmber,
                    modifier = Modifier.weight(1f),
                    testTag = "toggle_wakeword_button",
                    onClick = { viewModel.toggleWakeWordBackground() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Real-time Cyberpunk Terminal Logs
            Text(
                text = "ТАКТИЧЕСКИЙ ЖУРНАЛ СООБЩЕНИЙ [JARVIS PROTOCOL]",
                color = JarvisTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(JarvisSurface)
                    .border(1.dp, JarvisCardBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(hudLogs, key = { it.id }) { log ->
                    HudLogItem(log)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Mic Trigger button
            Button(
                onClick = {
                    viewModel.deviceManager.performHapticClick()
                    if (isListening) {
                        viewModel.voiceManager.stopListening()
                    } else {
                        viewModel.voiceManager.startListening()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("main_mic_action_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListening) JarvisAmber else JarvisCyan,
                    contentColor = Color(0xFF001B24)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isListening) "ОСТАНОВИТЬ ЗАПИСЬ" else "АКТИВИРОВАТЬ ДЖАРВИСА ГОЛОСОМ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Tap visualization on screen
        SimulatedTapOverlay(tapCoordinates = lastTap)
    }
}

@Composable
fun TopHudTelemetryBar(
    batteryLevel: Int,
    isCharging: Boolean,
    noiseSuppressionLevel: Int,
    noiseSuppressionActive: Boolean,
    spectralGateActive: Boolean,
    wakeWordActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, JarvisCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Power level
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "PWR: $batteryLevel%",
                    color = if (batteryLevel > 20) JarvisCyan else JarvisRedAlert,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                if (isCharging) {
                    Text(text = " ⚡", color = JarvisGold, fontSize = 11.sp)
                }
            }

            // Noise suppression status
            Text(
                text = "ШУМОПОДАВЛЕНИЕ: ${if (noiseSuppressionActive) "$noiseSuppressionLevel%${if (spectralGateActive) " (GATE)" else ""}" else "OFF"}",
                color = if (noiseSuppressionActive) JarvisCyan else JarvisTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )

            // Local AI state
            Text(
                text = "12GB AI: OK",
                color = JarvisCoreGlow,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun TacticalActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(60.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) activeColor.copy(alpha = 0.2f) else JarvisSurfaceVariant
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isActive) activeColor else JarvisCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) activeColor else JarvisTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = title,
                    color = if (isActive) JarvisTextPrimary else JarvisTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = JarvisTextMuted,
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun HudLogItem(log: HudLogMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "[${log.timestamp}]",
            color = JarvisTextMuted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${log.sender}:",
            color = if (log.isJarvis) JarvisCyan else JarvisGold,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = log.message,
            color = JarvisTextPrimary,
            fontSize = 10.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
