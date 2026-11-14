package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ScreenState
import com.example.service.JarvisWakeWordService
import com.example.ui.screens.CommandsScreen
import com.example.ui.screens.LocalAiScreen
import com.example.ui.screens.MainHudScreen
import com.example.ui.screens.NotesAndMemoryScreen
import com.example.ui.screens.PermissionGateScreen
import com.example.ui.screens.PhoneControlScreen
import com.example.ui.screens.VoicesScreen
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.JarvisViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    private val hotwordReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val query = intent?.getStringExtra(JarvisWakeWordService.EXTRA_HOTWORD_QUERY)
            if (!query.isNullOrBlank()) {
                viewModel.processVoiceInput(query)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allow waking up and displaying over lock screen when hotword is triggered
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        enableEdgeToEdge()

        // Handle hotword query passed in initial intent
        handleHotwordIntent(intent)

        setContent {
            MyApplicationTheme {
                JarvisApp(viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(JarvisWakeWordService.ACTION_HOTWORD_TRIGGERED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(hotwordReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(hotwordReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(hotwordReceiver)
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleHotwordIntent(intent)
    }

    private fun handleHotwordIntent(intent: Intent?) {
        val query = intent?.getStringExtra(JarvisWakeWordService.EXTRA_HOTWORD_QUERY)
        if (!query.isNullOrBlank()) {
            viewModel.processVoiceInput(query)
        }
    }
}

@Composable
fun JarvisApp(viewModel: JarvisViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val hasPermissions by viewModel.hasPermissions.collectAsState()

    BackHandler(enabled = currentScreen != ScreenState.MAIN_HUD && currentScreen != ScreenState.PERMISSION_GATE) {
        viewModel.setScreen(ScreenState.MAIN_HUD)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (currentScreen != ScreenState.PERMISSION_GATE) {
                JarvisBottomBar(
                    currentScreen = currentScreen,
                    onNavigate = { screen ->
                        viewModel.deviceManager.performHapticClick()
                        viewModel.setScreen(screen)
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                ScreenState.PERMISSION_GATE -> PermissionGateScreen(viewModel)
                ScreenState.MAIN_HUD -> MainHudScreen(viewModel)
                ScreenState.VOICES -> VoicesScreen(viewModel)
                ScreenState.LOCAL_AI_CORE -> LocalAiScreen(viewModel)
                ScreenState.COMMANDS_LIBRARY -> CommandsScreen(viewModel)
                ScreenState.NOTES_AND_MEMORY -> NotesAndMemoryScreen(viewModel)
                ScreenState.PHONE_CONTROL -> PhoneControlScreen(viewModel)
                else -> MainHudScreen(viewModel)
            }
        }
    }
}

@Composable
fun JarvisBottomBar(
    currentScreen: ScreenState,
    onNavigate: (ScreenState) -> Unit
) {
    NavigationBar(
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = JarvisSurface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == ScreenState.MAIN_HUD,
            onClick = { onNavigate(ScreenState.MAIN_HUD) },
            icon = {
                Icon(
                    if (currentScreen == ScreenState.MAIN_HUD) Icons.Filled.Mic else Icons.Outlined.Mic,
                    contentDescription = "HUD"
                )
            },
            label = { Text("HUD", fontSize = 9.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF001B24),
                indicatorColor = JarvisCyan,
                selectedTextColor = JarvisCyan,
                unselectedIconColor = JarvisTextSecondary,
                unselectedTextColor = JarvisTextSecondary
            )
        )

        NavigationBarItem(
            selected = currentScreen == ScreenState.VOICES,
            onClick = { onNavigate(ScreenState.VOICES) },
            icon = {
                Icon(
                    if (currentScreen == ScreenState.VOICES) Icons.Filled.RecordVoiceOver else Icons.Outlined.RecordVoiceOver,
                    contentDescription = "Голоса"
                )
            },
            label = { Text("Голоса", fontSize = 9.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF001B24),
                indicatorColor = JarvisCyan,
                selectedTextColor = JarvisCyan,
                unselectedIconColor = JarvisTextSecondary,
                unselectedTextColor = JarvisTextSecondary
            )
        )

        NavigationBarItem(
            selected = currentScreen == ScreenState.NOTES_AND_MEMORY,
            onClick = { onNavigate(ScreenState.NOTES_AND_MEMORY) },
            icon = {
                Icon(
                    if (currentScreen == ScreenState.NOTES_AND_MEMORY) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Планы & Память"
                )
            },
            label = { Text("Память", fontSize = 9.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF001B24),
                indicatorColor = JarvisCyan,
                selectedTextColor = JarvisCyan,
                unselectedIconColor = JarvisTextSecondary,
                unselectedTextColor = JarvisTextSecondary
            )
        )

        NavigationBarItem(
            selected = currentScreen == ScreenState.COMMANDS_LIBRARY,
            onClick = { onNavigate(ScreenState.COMMANDS_LIBRARY) },
            icon = {
                Icon(
                    if (currentScreen == ScreenState.COMMANDS_LIBRARY) Icons.Filled.Terminal else Icons.Outlined.Terminal,
                    contentDescription = "Команды"
                )
            },
            label = { Text("Команды", fontSize = 9.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF001B24),
                indicatorColor = JarvisCyan,
                selectedTextColor = JarvisCyan,
                unselectedIconColor = JarvisTextSecondary,
                unselectedTextColor = JarvisTextSecondary
            )
        )

        NavigationBarItem(
            selected = currentScreen == ScreenState.LOCAL_AI_CORE,
            onClick = { onNavigate(ScreenState.LOCAL_AI_CORE) },
            icon = {
                Icon(
                    if (currentScreen == ScreenState.LOCAL_AI_CORE) Icons.Filled.Code else Icons.Outlined.Code,
                    contentDescription = "12GB AI"
                )
            },
            label = { Text("12GB AI", fontSize = 9.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF001B24),
                indicatorColor = JarvisCyan,
                selectedTextColor = JarvisCyan,
                unselectedIconColor = JarvisTextSecondary,
                unselectedTextColor = JarvisTextSecondary
            )
        )

        NavigationBarItem(
            selected = currentScreen == ScreenState.PHONE_CONTROL,
            onClick = { onNavigate(ScreenState.PHONE_CONTROL) },
            icon = {
                Icon(
                    if (currentScreen == ScreenState.PHONE_CONTROL) Icons.Filled.PhoneAndroid else Icons.Outlined.PhoneAndroid,
                    contentDescription = "Контроль"
                )
            },
            label = { Text("Контроль", fontSize = 9.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF001B24),
                indicatorColor = JarvisCyan,
                selectedTextColor = JarvisCyan,
                unselectedIconColor = JarvisTextSecondary,
                unselectedTextColor = JarvisTextSecondary
            )
        )
    }
}
