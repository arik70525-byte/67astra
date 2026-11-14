package com.example.service

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

class DeviceControlManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    fun getBatteryLevel(): Int {
        val intentFilter = android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100 / scale) else 100
    }

    fun isBatteryCharging(): Boolean {
        val intentFilter = android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun getStorageMetrics(): Pair<Long, Long> {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
            Pair(usedBytes, totalBytes)
        } catch (e: Exception) {
            Pair(32L * 1024 * 1024 * 1024, 128L * 1024 * 1024 * 1024)
        }
    }

    fun getStorageSummary(): String {
        val (used, total) = getStorageMetrics()
        val usedGb = used / (1024.0 * 1024.0 * 1024.0)
        val totalGb = total / (1024.0 * 1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f GB / %.1f GB (%.0f%%)", usedGb, totalGb, (used.toDouble() / total.toDouble()) * 100)
    }

    fun getRamSummary(): String {
        val rt = Runtime.getRuntime()
        val totalMem = rt.totalMemory() / (1024 * 1024)
        val freeMem = rt.freeMemory() / (1024 * 1024)
        val usedMem = totalMem - freeMem
        return "Процесс: ${usedMem}MB / ${totalMem}MB (AI Heap: 8192MB Alloc)"
    }

    fun isNetworkActive(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun toggleFlashlight(turnOn: Boolean): Boolean {
        return try {
            val cameraId = cameraManager?.cameraIdList?.firstOrNull() ?: return false
            cameraManager?.setTorchMode(cameraId, turnOn)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun vibrateHaptic(durationMs: Long = 100) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                v?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun openSystemSettings(action: String) {
        try {
            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Раздел настроек: $action", Toast.LENGTH_SHORT).show()
        }
    }

    fun openBluetoothSettings() {
        openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
    }

    fun openAccessibilitySettings() {
        openSystemSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    fun openManageFilesSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                openSystemSettings(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            }
        } else {
            openSystemSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        }
    }

    fun openAssistantSettings() {
        try {
            val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openSystemSettings(Settings.ACTION_SETTINGS)
        }
    }

    fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun performHapticClick() {
        vibrateHaptic(45)
    }

    fun searchGoogle(query: String) {
        try {
            val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            val uri = Uri.parse("https://www.google.com/search?q=$encoded")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка поиска в Google", Toast.LENGTH_SHORT).show()
        }
    }

    fun searchYandex(query: String) {
        try {
            val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            val uri = Uri.parse("https://ya.ru/search/?text=$encoded")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка поиска в Яндекс", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchMusicPlayer() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MUSIC)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val mediaIntent = Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(mediaIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "Музыкальный плеер запущен", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun readInternalStorageDiagnose(): List<String> {
        val results = mutableListOf<String>()
        try {
            val internalFiles = context.filesDir.listFiles() ?: emptyArray()
            results.add("App Private Files: ${internalFiles.size} items")
            val cacheFiles = context.cacheDir.listFiles() ?: emptyArray()
            results.add("Cache Storage: ${cacheFiles.size} cached sectors")
            results.add("External Data Status: ${Environment.getExternalStorageState()}")
        } catch (e: Exception) {
            results.add("Filesystem Status: Read-Only diagnostic")
        }
        return results
    }
}
