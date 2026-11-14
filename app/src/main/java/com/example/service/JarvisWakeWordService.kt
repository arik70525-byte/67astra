package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.Locale

class JarvisWakeWordService : Service() {

    companion object {
        const val CHANNEL_ID = "jarvis_wakeword_channel"
        const val NOTIFICATION_ID = 1007
        const val ACTION_START = "ACTION_START_HOTWORD"
        const val ACTION_STOP = "ACTION_STOP_HOTWORD"
        const val ACTION_HOTWORD_TRIGGERED = "com.example.JARVIS_HOTWORD_TRIGGERED"
        const val EXTRA_HOTWORD_QUERY = "EXTRA_HOTWORD_QUERY"

        var isServiceRunning: Boolean = false
            private set
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var tts: TextToSpeech? = null
    private var isListeningLoopActive = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("ru", "RU")
                tts?.setPitch(0.90f)
                tts?.setSpeechRate(1.05f)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        if (action == ACTION_STOP) {
            stopListeningLoop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            isServiceRunning = false
            return START_NOT_STICKY
        }

        // Start Foreground Notification
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        acquireWakeLock()
        isServiceRunning = true
        startListeningLoop()

        return START_STICKY
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Jarvis::AlwaysOnWakeLock"
            )?.apply {
                acquire(10 * 60 * 1000L) // 10 minutes sliding or keep while active
            }
        } catch (e: Exception) {
            Log.e("JarvisWakeWordService", "WakeLock error", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun startListeningLoop() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e("JarvisWakeWordService", "SpeechRecognizer not available")
            return
        }

        isListeningLoopActive = true
        initAndListen()
    }

    private fun initAndListen() {
        if (!isListeningLoopActive) return

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        // Restart listening loop after brief backoff
                        if (isListeningLoopActive) {
                            android.os.Handler(mainLooper).postDelayed({
                                initAndListen()
                            }, 600)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognized = matches[0]
                            handleRecognizedSpeech(recognized)
                        }
                        // Continue loop
                        if (isListeningLoopActive) {
                            android.os.Handler(mainLooper).postDelayed({
                                initAndListen()
                            }, 400)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val partial = matches[0].lowercase(Locale.ROOT)
                            if (partial.contains("джарвис") || partial.contains("jarvis")) {
                                handleRecognizedSpeech(matches[0])
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("JarvisWakeWordService", "Listen cycle error", e)
        }
    }

    private fun handleRecognizedSpeech(text: String) {
        val lower = text.lowercase(Locale.ROOT)
        // Detect wake words: "джарвис", "jarvis", "проснись", "поехали"
        if (lower.contains("джарвис") || lower.contains("jarvis") || lower.contains("поехали") || lower.contains("проснулся")) {
            // Speak confirmation
            tts?.speak("Слушаю вас, сэр.", TextToSpeech.QUEUE_FLUSH, null, "WAKE_CONFIRM")

            // Broadcast to MainActivity / ViewModel or launch App over lockscreen
            val broadcastIntent = Intent(ACTION_HOTWORD_TRIGGERED).apply {
                putExtra(EXTRA_HOTWORD_QUERY, text)
                setPackage(packageName)
            }
            sendBroadcast(broadcastIntent)

            // Bring app to foreground if screen is locked or asleep
            val activityIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_HOTWORD_QUERY, text)
            }
            startActivity(activityIntent)
        }
    }

    private fun stopListeningLoop() {
        isListeningLoopActive = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            // ignore
        }
        releaseWakeLock()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "J.A.R.V.I.S. Фоновое прослушивание (Always-On)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Активный микрофонный сервис ожидания фразы Джарвис"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, JarvisWakeWordService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("J.A.R.V.I.S. Фоновое ядро активно")
            .setContentText("Always-On Hotword: сканирование фразы «Джарвис»...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_jarvis_core, "Отключить", stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListeningLoop()
        tts?.shutdown()
        tts = null
        isServiceRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
