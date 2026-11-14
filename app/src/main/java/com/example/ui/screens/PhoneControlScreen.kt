package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.viewmodel.JarvisViewModel
import java.util.Locale

@Composable
fun PhoneControlScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val noiseCancelActive by viewModel.noiseSuppressionActive.collectAsState()
    val noiseCancelLevel by viewModel.noiseSuppressionLevel.collectAsState()
    val spectralGateActive by viewModel.spectralGateActive.collectAsState()
    val wakeWordActive by viewModel.wakeWordBackgroundActive.collectAsState()

    val storageSummary = viewModel.deviceManager.getStorageSummary()
    val ramSummary = viewModel.deviceManager.getRamSummary()
    val fileSectors = viewModel.deviceManager.readInternalStorageDiagnose()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "УПРАВЛЕНИЕ ТЕЛЕФОНОМ И ШУМОПОДАВЛЕНИЕ",
            color = JarvisCyan,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Глубокие системные интерфейсы, шумоподавление микрофона и кликер экрана.",
            color = JarvisTextSecondary,
            fontSize = 12.sp
        )

        // 1. Upgraded Noise Suppression Tuning
        ControlGroupCard(
            title = "АКУСТИЧЕСКИЙ ФИЛЬТР И МАКСИМАЛЬНОЕ ШУМОПОДАВЛЕНИЕ",
            icon = Icons.Default.VolumeUp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Аппаратное + Программное подавление",
                        color = JarvisTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Отсекает гул, фоновые голоса и эхо",
                        color = JarvisTextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = noiseCancelActive,
                    onCheckedChange = { viewModel.setNoiseSuppressionActive(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = JarvisCyan,
                        checkedTrackColor = JarvisCyan.copy(alpha = 0.4f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Спектральный порог (Spectral Noise Gate)",
                        color = JarvisTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Интеллектуальная фильтрация фонового гула",
                        color = JarvisTextSecondary,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = spectralGateActive,
                    onCheckedChange = { viewModel.toggleSpectralGate() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = JarvisGold,
                        checkedTrackColor = JarvisGold.copy(alpha = 0.4f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = String.format(Locale.US, "Уровень подавления шума: %.0f%%", noiseCancelLevel * 100),
                color = JarvisGold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Slider(
                value = noiseCancelLevel,
                onValueChange = { viewModel.setNoiseSuppressionLevel(it) },
                valueRange = 0.5f..0.99f,
                colors = SliderDefaults.colors(
                    thumbColor = JarvisGold,
                    activeTrackColor = JarvisGold,
                    inactiveTrackColor = JarvisCardBorder
                )
            )
        }

        // 2. Search Shortcuts (Yandex & Google)
        ControlGroupCard(
            title = "ПОИСКОВЫЕ ШЛЮЗЫ: ЯНДЕКС & GOOGLE",
            icon = Icons.Default.Search
        ) {
            Text(
                text = "Быстрый запуск поисковых систем или директивы «Сделай сам открой Яндекс и найди...».",
                color = JarvisTextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.deviceManager.searchYandex("привет") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisAmber, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Яндекс Поиск", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.deviceManager.searchGoogle("погода") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Google Поиск", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. Screen Tap & Accessibility Control
        ControlGroupCard(
            title = "КЛИКИ ПО ЭКРАНУ И СЕРВИС ДОСТУПА",
            icon = Icons.Default.TouchApp
        ) {
            Text(
                text = "ИИ использует координаты и AccessibilityService для взаимодействия с элементами экрана без физических касаний.",
                color = JarvisTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.simulateTapOnScreen(0.5f, 0.4f) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("simulate_tap_center"),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Клик по центру", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.simulateTapOnScreen(0.85f, 0.15f) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("simulate_tap_corner"),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisAmber, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Клик в угол", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedButton(
                onClick = { viewModel.deviceManager.openAccessibilitySettings() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manage_accessibility_btn"),
                border = BorderStroke(1.dp, JarvisCardBorder),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Accessibility, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Открыть настройки спец. возможностей (Accessibility)", color = JarvisCyan, fontSize = 11.sp)
            }
        }

        // 4. Bluetooth & Music Shortcuts
        ControlGroupCard(
            title = "БЛЮТУЗ & МУЗЫКА (ПРОТОКОЛ «ПОЕХАЛИ»)",
            icon = Icons.Default.Bluetooth
        ) {
            Text(
                text = "Быстрое переключение Bluetooth соединения и открытие музыкального плеера.",
                color = JarvisTextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.deviceManager.openBluetoothSettings() },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, JarvisCardBorder),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bluetooth", color = JarvisCyan, fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.deviceManager.launchMusicPlayer() },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, JarvisCardBorder),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = JarvisGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Музыка", color = JarvisGold, fontSize = 11.sp)
                }
            }
        }

        // 5. Storage and Files Access
        ControlGroupCard(
            title = "РЕАЛЬНЫЙ ДОСТУП К ФАЙЛАМ И ПАМЯТИ",
            icon = Icons.Default.Folder
        ) {
            Text(
                text = "Хранилище: $storageSummary",
                color = JarvisGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = ramSummary,
                color = JarvisTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(6.dp))

            fileSectors.forEach { sector ->
                Text(
                    text = "• $sector",
                    color = JarvisTextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { viewModel.deviceManager.openManageFilesSettings() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manage_files_access_btn"),
                border = BorderStroke(1.dp, JarvisCardBorder),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Storage, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Системный доступ ко всем файлам (All Files Access)", color = JarvisCyan, fontSize = 11.sp)
            }
        }

        // 6. Default Assistant Setup & Always-On Screen Off
        ControlGroupCard(
            title = "НАЗНАЧЕНИЕ ОСНОВНЫМ АССИСТЕНТОМ И ВЫЗОВ",
            icon = Icons.Default.Assistant
        ) {
            Text(
                text = "Сделайте Джарвиса главным ассистентом Android для вызова голосом и работы при заблокированном телефоне.",
                color = JarvisTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.deviceManager.openAssistantSettings() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("set_default_assistant_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("НАЗНАЧИТЬ ДЖАРВИСА ОСНОВНЫМ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedButton(
                onClick = { viewModel.deviceManager.openOverlaySettings() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("overlay_permission_btn"),
                border = BorderStroke(1.dp, JarvisCardBorder),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Разрешить отображение поверх других окон (Overlay)", color = JarvisGold, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun ControlGroupCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, JarvisCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = JarvisCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
