package com.example.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.HudLogMessage
import com.example.model.JarvisCommand
import com.example.model.JarvisFact
import com.example.model.JarvisNote
import com.example.model.JarvisVoice
import com.example.model.LocalAiEngine
import com.example.model.ScreenState
import com.example.service.DeviceControlManager
import com.example.service.JarvisVoiceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    val deviceManager = DeviceControlManager(application)
    val voiceManager = JarvisVoiceManager(application)

    // Current Screen
    private val _currentScreen = MutableStateFlow(ScreenState.PERMISSION_GATE)
    val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

    // Permissions granted state
    private val _hasPermissions = MutableStateFlow(false)
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    // Noise suppression enabled and level (0.0 to 1.0)
    private val _noiseSuppressionActive = MutableStateFlow(true)
    val noiseSuppressionActive: StateFlow<Boolean> = _noiseSuppressionActive.asStateFlow()

    private val _noiseSuppressionLevel = MutableStateFlow(0.98f)
    val noiseSuppressionLevel: StateFlow<Float> = _noiseSuppressionLevel.asStateFlow()

    private val _spectralGateActive = MutableStateFlow(true)
    val spectralGateActive: StateFlow<Boolean> = _spectralGateActive.asStateFlow()

    // Screen-off background listening simulation toggle
    private val _wakeWordBackgroundActive = MutableStateFlow(false)
    val wakeWordBackgroundActive: StateFlow<Boolean> = _wakeWordBackgroundActive.asStateFlow()

    // Flashlight state
    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    // Active Voice ID
    private val _selectedVoiceId = MutableStateFlow("jarvis_stark_classic")
    val selectedVoiceId: StateFlow<String> = _selectedVoiceId.asStateFlow()

    // Custom voice tuning
    private val _customPitch = MutableStateFlow(1.0f)
    val customPitch: StateFlow<Float> = _customPitch.asStateFlow()

    private val _customSpeed = MutableStateFlow(1.05f)
    val customSpeed: StateFlow<Float> = _customSpeed.asStateFlow()

    // 10 built-in voices + 1 custom voice
    private val _voicesList = MutableStateFlow(
        listOf(
            JarvisVoice("jarvis_stark_classic", "Джарвис Оригинал", "Британский благородный тон, спокойствие и преданность", 0.90f, 1.05f, "UK / Stark Prime"),
            JarvisVoice("friday_warm", "ПЯТНИЦА (F.R.I.D.A.Y.)", "Ирландский мягкий тембр, тактический советник", 1.25f, 1.10f, "Warm AI"),
            JarvisVoice("edith_tactical", "Э.Д.И.Ф. (E.D.I.T.H.)", "Высокоточный тактический расчет дронов и спутников", 1.15f, 1.15f, "Tactical Net"),
            JarvisVoice("cyber_synth", "Киберпанк 2077 Синт", "Низкий металлический голос с резонансом неонового города", 0.70f, 0.95f, "Neon Rogue"),
            JarvisVoice("quantum_deep", "Квантовый Бас", "Сверхглубокий бархатистый голос серверной стойки", 0.60f, 0.90f, "Sub-Bass Core"),
            JarvisVoice("solaris_calm", "Солярис Медитатор", "Спокойный размеренный голос космической станции", 0.95f, 0.85f, "Orbital Zen"),
            JarvisVoice("valkyrie_sharp", "Валькирия Протокол", "Командный, резкий, молниеносный военный ИИ", 1.30f, 1.20f, "Command Elite"),
            JarvisVoice("glados_neuro", "Нейро-Аналитик", "Холодный расчетливый тон с легкой иронией", 1.40f, 1.00f, "Analytical"),
            JarvisVoice("kronos_titan", "Кронос Титан", "Монументальный мощный голос тяжелой брони", 0.55f, 0.80f, "Heavy Armored"),
            JarvisVoice("hologram_echo", "Голографический Эхо", "Голос с пространственной реверберацией", 1.05f, 1.00f, "Holo Spectral"),
            JarvisVoice("custom_user_voice", "Пользовательский (Custom)", "Настройте частоту и скорость под себя", 1.00f, 1.05f, "User Calibrated", isCustom = true)
        )
    )
    val voicesList: StateFlow<List<JarvisVoice>> = _voicesList.asStateFlow()

    // Local AI models setup (Max 8GB main coding/reasoning, totaling <= 12GB)
    private val _localAiEngines = MutableStateFlow(
        listOf(
            LocalAiEngine(
                name = "DeepSeek-R1-Coder-Q4",
                category = "Основной Логический & Код ИИ",
                memoryFootprintMb = 7680,
                contextWindow = "32K Tokens",
                role = "Архитектура, анализ кода, логические цепочки рассуждений (Chain-of-Thought)",
                status = "АКТИВЕН / В ПАМЯТИ",
                isPrimary = true
            ),
            LocalAiEngine(
                name = "Whisper-Cyber-Small (V3)",
                category = "Распознавание речи (STT)",
                memoryFootprintMb = 960,
                contextWindow = "Аудиопоток 16kHz",
                role = "Быстрое распознавание голоса без интернета с шумоподавлением",
                status = "АКТИВЕН"
            ),
            LocalAiEngine(
                name = "Piper-Neural-TTS",
                category = "Синтез голоса (TTS)",
                memoryFootprintMb = 512,
                contextWindow = "Фонемы / Эмоции",
                role = "10 голосовых пресетов в реальном времени, 0ms латентность",
                status = "АКТИВЕН"
            ),
            LocalAiEngine(
                name = "Stark-Vision-Tiny",
                category = "Визуальный анализ UI / Экрана",
                memoryFootprintMb = 1450,
                contextWindow = "1024x1024 Frame",
                role = "Анализ элементов экрана, распознавание кнопок и координат для тапов",
                status = "ГОТОВ"
            ),
            LocalAiEngine(
                name = "CyberOS-Action-Controller",
                category = "Исполнитель системных команд",
                memoryFootprintMb = 680,
                contextWindow = "JSON Action Graph",
                role = "Преобразование голосовой команды в нажатия, интенты и доступ к файлам",
                status = "АКТИВЕН"
            )
        )
    )
    val localAiEngines: StateFlow<List<LocalAiEngine>> = _localAiEngines.asStateFlow()

    // Commands Library (Built-in + Custom user commands)
    private val _commandsList = MutableStateFlow(
        listOf(
            JarvisCommand("cmd_wake", "Джарвис, я проснулся", "Утренний брифинг", "Озвучивает точное время, планы на день и сводку погоды", "WAKE_UP_BRIEFING"),
            JarvisCommand("cmd_drive", "Джарвис, поехали", "Автопилот", "Включает блютуз и запускает музыку", "DRIVE_PROTOCOL", isCustom = true, customActionPayload = "BLUETOOTH_AND_MUSIC"),
            JarvisCommand("cmd_auto_demo", "Сделай сам открой Яндекс и впиши в поиск привет", "Автономность", "Джарвис самостоятельно запускает поиск «привет» в Яндекс", "AUTO_SEARCH_YANDEX"),
            JarvisCommand("cmd_1", "Джарвис, полный статус систем", "Диагностика", "Выводит батарею, память, датчики и состояние AI ядра", "DIAGNOSTIC_FULL"),
            JarvisCommand("cmd_2", "Джарвис, включи фонарик", "Оборудование", "Активирует вспышку камеры на максимальной мощности", "TORCH_ON"),
            JarvisCommand("cmd_3", "Джарвис, выключи фонарик", "Оборудование", "Деактивирует вспышку камеры", "TORCH_OFF"),
            JarvisCommand("cmd_4", "Джарвис, проверь файлы и память", "Файлы", "Анализирует занятое дисковое пространство и сектора", "STORAGE_CHECK"),
            JarvisCommand("cmd_5", "Джарвис, режим максимальной тишины", "Аудио", "Включает шумоподавление микрофона на 99% + Spectral Gate", "NOISE_CANCEL_MAX"),
            JarvisCommand("cmd_6", "Джарвис, кликни по экрану", "Управление", "Симуляция тач-скрина и взаимодействие с интерфейсом", "TAP_SIMULATION"),
            JarvisCommand("cmd_7", "Джарвис, напиши код на Python", "Код & ИИ", "Генерирует алгоритм с помощью DeepSeek 8GB ядра", "GENERATE_CODE"),
            JarvisCommand("cmd_8", "Джарвис, найди в Google погода", "Поиск", "Мгновенный поиск информации в поисковой системе Google", "SEARCH_GOOGLE"),
            JarvisCommand("cmd_9", "Джарвис, открой системные настройки", "Система", "Открывает панель параметров Android", "OPEN_SETTINGS"),
            JarvisCommand("cmd_10", "Джарвис, протокол скрытности", "Безопасность", "Вибрация, очистка временных буферов памяти", "STEALTH_PROTOCOL"),
            JarvisCommand("cmd_11", "Джарвис, назначь себя основным ассистентом", "Ассистент", "Перенаправляет в настройки Default Assistant App", "SET_DEFAULT_ASSISTANT")
        )
    )
    val commandsList: StateFlow<List<JarvisCommand>> = _commandsList.asStateFlow()

    // Notes state
    private val _notesList = MutableStateFlow(
        listOf(
            JarvisNote(
                id = 1L,
                title = "Проект Старк Индастриз",
                content = "Завершить калибровку тактического интерфейса Mark 85 к 18:00.",
                dateCreated = "Сегодня, 09:30",
                isHighPriority = true
            ),
            JarvisNote(
                id = 2L,
                title = "Тестирование авто-кликера",
                content = "Проверить корректность симуляции экранных жестов и тапов в приложении.",
                dateCreated = "Сегодня, 10:15"
            ),
            JarvisNote(
                id = 3L,
                title = "Список задач на день",
                content = "1. Зарядить дуговой реактор. 2. Оптимизировать локальный DeepSeek 8GB.",
                dateCreated = "Сегодня, 11:00"
            )
        )
    )
    val notesList: StateFlow<List<JarvisNote>> = _notesList.asStateFlow()

    // Memory Facts about User
    private val _factsList = MutableStateFlow(
        listOf(
            JarvisFact(1L, "Обращение", "Пользователь предпочитает статусное обращение «Сэр».", "11 Ноября"),
            JarvisFact(2L, "Автопилот", "При фразе «поехали» требуется включение Bluetooth и запуск музыки.", "11 Ноября"),
            JarvisFact(3L, "Интеллект", "Приоритетная модель для рассуждений и кода — DeepSeek R1 8GB.", "11 Ноября"),
            JarvisFact(4L, "Акустика", "Использовать максимальное 99% шумоподавление микрофона со спектральным фильтром.", "11 Ноября")
        )
    )
    val factsList: StateFlow<List<JarvisFact>> = _factsList.asStateFlow()

    // HUD Logs
    private val _hudLogs = MutableStateFlow<List<HudLogMessage>>(emptyList())
    val hudLogs: StateFlow<List<HudLogMessage>> = _hudLogs.asStateFlow()

    // Screen click coordinates simulation
    private val _lastSimulatedTap = MutableStateFlow<Pair<Float, Float>?>(null)
    val lastSimulatedTap: StateFlow<Pair<Float, Float>?> = _lastSimulatedTap.asStateFlow()

    init {
        voiceManager.onSpeechRecognized = { speech ->
            processVoiceInput(speech)
        }
        addHudLog("J.A.R.V.I.S.", "Все системы инициализированы, сэр. Я готов к исполнению протоколов.")
    }

    fun setScreen(screen: ScreenState) {
        _currentScreen.value = screen
    }

    fun setPermissionsGranted(granted: Boolean) {
        _hasPermissions.value = granted
        if (granted) {
            _currentScreen.value = ScreenState.MAIN_HUD
            deviceManager.performHapticClick()
            speakJarvis("Доступ подтвержден, сэр. Протокол безопасности активирован. Полный контроль над системами предоставлен.")
        }
    }

    fun setNoiseSuppressionActive(active: Boolean) {
        _noiseSuppressionActive.value = active
        voiceManager.noiseSuppressionLevel = if (active) _noiseSuppressionLevel.value else 0.1f
        deviceManager.performHapticClick()
        val statusText = if (active) "активно (${(_noiseSuppressionLevel.value * 100).toInt()}%)" else "отключено"
        addHudLog("СИСТЕМА ШУМОПОДАВЛЕНИЯ", "Аппаратное и программное подавление шумов: $statusText")
    }

    fun setNoiseSuppressionLevel(level: Float) {
        _noiseSuppressionLevel.value = level
        if (_noiseSuppressionActive.value) {
            voiceManager.noiseSuppressionLevel = level
        }
    }

    fun toggleSpectralGate() {
        val next = !_spectralGateActive.value
        _spectralGateActive.value = next
        voiceManager.spectralGateActive = next
        deviceManager.performHapticClick()
        val text = if (next) "Спектральный фильтр шумов активирован, сэр." else "Спектральный фильтр деактивирован."
        speakJarvis(text)
        addHudLog("ШУМОПОДАВЛЕНИЕ", text)
    }

    fun toggleWakeWordBackground() {
        val next = !_wakeWordBackgroundActive.value
        _wakeWordBackgroundActive.value = next
        deviceManager.performHapticClick()

        val appContext = getApplication<Application>()
        val serviceIntent = Intent(appContext, com.example.service.JarvisWakeWordService::class.java)

        if (next) {
            serviceIntent.action = com.example.service.JarvisWakeWordService.ACTION_START
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                appContext.startForegroundService(serviceIntent)
            } else {
                appContext.startService(serviceIntent)
            }
            speakJarvis("Сэр, фоновый режим Always-On активирован. Я непрерывно слушаю кодовые фразы даже при заблокированном дисплее.")
            addHudLog("J.A.R.V.I.S.", "Фоновый Always-On Hotword сервис запущен в статусе Foreground. Микрофон активен.")
        } else {
            serviceIntent.action = com.example.service.JarvisWakeWordService.ACTION_STOP
            appContext.startService(serviceIntent)
            speakJarvis("Фоновый режим ожидания деактивирован, сэр.")
            addHudLog("J.A.R.V.I.S.", "Фоновый сервис переведен в спящий режим.")
        }
    }

    fun selectVoice(voiceId: String) {
        _selectedVoiceId.value = voiceId
        deviceManager.performHapticClick()
        val voice = _voicesList.value.firstOrNull { it.id == voiceId }
        val testText = when (voiceId) {
            "friday_warm" -> "Привет, босс. ПЯТНИЦА к вашим услугам. Чем займемся?"
            "edith_tactical" -> "Э.Д.И.Ф. на связи. Тактическая сетка развернута."
            "cyber_synth" -> "Системы онлайн. Город не спит, сэр."
            "quantum_deep" -> "Квантовое ядро зафиксировано на частоте 12 терагерц."
            else -> "Голосовой модуль перекалиброван, сэр. Как вам такое звучание?"
        }
        val pitch = if (voice?.isCustom == true) _customPitch.value else voice?.pitch ?: 1.0f
        val speed = if (voice?.isCustom == true) _customSpeed.value else voice?.speed ?: 1.0f
        voiceManager.speak(testText, pitch, speed)
        addHudLog("КАЛИБРОВКА ГОЛОСА", "Выбран профиль: ${voice?.name ?: voiceId}")
    }

    fun updateCustomVoice(pitch: Float, speed: Float) {
        _customPitch.value = pitch
        _customSpeed.value = speed
    }

    fun testCustomVoice() {
        deviceManager.performHapticClick()
        voiceManager.speak("Тестирование пользовательского голоса, сэр. Частота ${_customPitch.value}, темп ${_customSpeed.value}.", _customPitch.value, _customSpeed.value)
    }

    fun toggleFlashlight() {
        val next = !_isFlashlightOn.value
        val success = deviceManager.toggleFlashlight(next)
        if (success) {
            _isFlashlightOn.value = next
            deviceManager.performHapticClick()
            val text = if (next) "Освещение активировано, сэр." else "Освещение отключено, сэр."
            speakJarvis(text)
            addHudLog("ОБОРУДОВАНИЕ", text)
        }
    }

    fun simulateTapOnScreen(xRatio: Float = 0.5f, yRatio: Float = 0.5f) {
        _lastSimulatedTap.value = Pair(xRatio, yRatio)
        deviceManager.performHapticClick()
        viewModelScope.launch {
            delay(1200)
            _lastSimulatedTap.value = null
        }
        speakJarvis("Выполняю клик по заданным координатам дисплея, сэр.")
        addHudLog("АВТО-КЛИКЕР UI", "Симулировано нажатие по координатам [X: ${(xRatio*100).toInt()}%, Y: ${(yRatio*100).toInt()}%]")
    }

    // Morning Briefing: "Джарвис я проснулся"
    fun executeWakeUpBriefing() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = timeFormat.format(Date())
        val notesCount = _notesList.value.size
        val highPriority = _notesList.value.count { it.isHighPriority }

        val briefing = "Доброе утро, сэр. Текущее время $currentTime. Погода в норме: плюс девятнадцать градусов, переменная облачность, видимость отличная. В вашем списке планов $notesCount задач, из них $highPriority повышенной важности. Все системы корабля готовы к работе."

        speakJarvis(briefing)
        addHudLog("УТРЕННИЙ БРИФИНГ", briefing)
    }

    // Drive Protocol: "поехали" -> Bluetooth + Music
    fun executeDriveProtocol() {
        val response = "Протокол «Поехали» активирован, сэр. Включаю Bluetooth интерфейс и запускаю музыкальный плеер. Приятной дороги."
        speakJarvis(response)
        addHudLog("ПРОТОКОЛ «ПОЕХАЛИ»", response)
        deviceManager.openBluetoothSettings()
        viewModelScope.launch {
            delay(1500)
            deviceManager.launchMusicPlayer()
        }
    }

    // "Сделай сам" Autonomous command parser and executor
    fun executeSelfAction(rawQuery: String) {
        val clean = rawQuery.replace(Regex("(?i)^(джарвис,?\\s*)?(сделай сам\\s*)"), "").trim()
        val lower = clean.lowercase(Locale.ROOT)

        addHudLog("АВТОНОМНЫЙ РЕЖИМ", "Директива «Сделай сам»: $clean")

        when {
            lower.contains("яндекс") && (lower.contains("поиск") || lower.contains("впиши") || lower.contains("найди")) -> {
                // Extract search term e.g. "впиши в поиск привет"
                val searchTerm = extractSearchQuery(clean, "яндекс")
                speakJarvis("Самостоятельно открываю Яндекс и ввожу поисковый запрос: $searchTerm, сэр.")
                simulateTapOnScreen(0.5f, 0.45f)
                viewModelScope.launch {
                    delay(800)
                    deviceManager.searchYandex(searchTerm)
                }
            }
            lower.contains("гугл") || lower.contains("google") -> {
                val searchTerm = extractSearchQuery(clean, "гугл")
                speakJarvis("Самостоятельно открываю Google и выполняю поиск: $searchTerm, сэр.")
                simulateTapOnScreen(0.5f, 0.45f)
                viewModelScope.launch {
                    delay(800)
                    deviceManager.searchGoogle(searchTerm)
                }
            }
            lower.contains("фонарик") -> {
                toggleFlashlight()
            }
            lower.contains("клик") || lower.contains("нажми") -> {
                simulateTapOnScreen(0.5f, 0.5f)
            }
            lower.contains("блютуз") || lower.contains("bluetooth") -> {
                speakJarvis("Активирую модуль Bluetooth, сэр.")
                deviceManager.openBluetoothSettings()
            }
            lower.contains("музык") || lower.contains("трек") -> {
                speakJarvis("Запускаю воспроизведение музыки, сэр.")
                deviceManager.launchMusicPlayer()
            }
            else -> {
                // If it's a general request, search in Yandex or Google automatically
                speakJarvis("Выполняю задачу самостоятельно через поисковый шлюз, сэр: $clean")
                deviceManager.searchYandex(clean)
            }
        }
    }

    private fun extractSearchQuery(text: String, engineKeyword: String): String {
        val regex = Regex("(?i)(?:впиши в поиск|найди в $engineKeyword|впиши|поиск|найди|запрос)\\s+(.+)")
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.trim() ?: text.replace(Regex("(?i)(открой|яндекс|гугл|google|впиши|в|поиск)"), "").trim().ifEmpty { "привет" }
    }

    fun addCustomCommand(trigger: String, description: String, actionType: String) {
        val newCmd = JarvisCommand(
            id = "custom_cmd_${System.currentTimeMillis()}",
            voiceTrigger = trigger,
            actionCategory = "Пользовательская",
            description = description,
            simulationAction = "CUSTOM_TRIGGER",
            isCustom = true,
            customActionPayload = actionType
        )
        val current = _commandsList.value.toMutableList()
        current.add(0, newCmd)
        _commandsList.value = current
        deviceManager.performHapticClick()
        val notify = "Новая пользовательская директива зарегистрирована, сэр: «$trigger»."
        speakJarvis(notify)
        addHudLog("КОМАНДНЫЙ ЦЕНТР", notify)
    }

    fun addNote(title: String, content: String, isHighPriority: Boolean = false) {
        val dateFormat = SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault())
        val newNote = JarvisNote(
            id = System.currentTimeMillis(),
            title = title,
            content = content,
            dateCreated = dateFormat.format(Date()),
            isHighPriority = isHighPriority
        )
        val current = _notesList.value.toMutableList()
        current.add(0, newNote)
        _notesList.value = current
        deviceManager.performHapticClick()
        val speech = "Заметка «$title» сохранена в защищенный сектор памяти, сэр."
        speakJarvis(speech)
        addHudLog("ЗАМЕТКИ", speech)
    }

    fun deleteNote(noteId: Long) {
        _notesList.value = _notesList.value.filter { it.id != noteId }
        deviceManager.performHapticClick()
        addHudLog("ЗАМЕТКИ", "Заметка удалена из секторов данных.")
    }

    fun addFact(category: String, fact: String) {
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val newFact = JarvisFact(
            id = System.currentTimeMillis(),
            category = category,
            fact = fact,
            dateRecorded = dateFormat.format(Date())
        )
        val current = _factsList.value.toMutableList()
        current.add(0, newFact)
        _factsList.value = current
        deviceManager.performHapticClick()
        val msg = "Новый факт о вас сохранен в долговременную память, сэр."
        speakJarvis(msg)
        addHudLog("ПАМЯТЬ ДЖАРВИСА", "$category: $fact")
    }

    fun deleteFact(factId: Long) {
        _factsList.value = _factsList.value.filter { it.id != factId }
        deviceManager.performHapticClick()
    }

    fun executeCommand(command: JarvisCommand) {
        deviceManager.performHapticClick()
        addHudLog("ПОЛЬЗОВАТЕЛЬ", command.voiceTrigger, isJarvis = false)

        when {
            command.simulationAction == "WAKE_UP_BRIEFING" -> {
                executeWakeUpBriefing()
            }
            command.simulationAction == "DRIVE_PROTOCOL" || command.customActionPayload == "BLUETOOTH_AND_MUSIC" -> {
                executeDriveProtocol()
            }
            command.simulationAction == "AUTO_SEARCH_YANDEX" -> {
                executeSelfAction("сделай сам открой яндекс и впиши в поиск привет")
            }
            command.simulationAction == "SEARCH_GOOGLE" -> {
                speakJarvis("Выполняю поиск в Google, сэр.")
                deviceManager.searchGoogle("погода")
            }
            command.simulationAction == "DIAGNOSTIC_FULL" -> {
                val battery = deviceManager.getBatteryLevel()
                val charging = if (deviceManager.isBatteryCharging()) "подключена к дуговому реактору" else "автономный режим"
                val storage = deviceManager.getStorageSummary()
                val ram = deviceManager.getRamSummary()
                val response = "Сэр, уровень заряда батареи $battery%, $charging. Хранилище: $storage. $ram. Все локальные 12GB нейромодели функционируют штатно."
                speakJarvis(response)
                addHudLog("J.A.R.V.I.S.", response)
            }
            command.simulationAction == "TORCH_ON" -> {
                deviceManager.toggleFlashlight(true)
                _isFlashlightOn.value = true
                speakJarvis("Фонарик включен на полную мощность, сэр.")
                addHudLog("J.A.R.V.I.S.", "Фотонный луч активирован.")
            }
            command.simulationAction == "TORCH_OFF" -> {
                deviceManager.toggleFlashlight(false)
                _isFlashlightOn.value = false
                speakJarvis("Фонарик выключен, сэр.")
                addHudLog("J.A.R.V.I.S.", "Фотонный луч деактивирован.")
            }
            command.simulationAction == "STORAGE_CHECK" -> {
                val items = deviceManager.readInternalStorageDiagnose()
                val storageStr = deviceManager.getStorageSummary()
                val response = "Сэр, доступ к файловой структуре проверен. Накопитель: $storageStr. ${items.joinToString(", ")}."
                speakJarvis(response)
                addHudLog("ФАЙЛОВЫЙ СКАНЕР", response)
            }
            command.simulationAction == "NOISE_CANCEL_MAX" -> {
                setNoiseSuppressionActive(true)
                setNoiseSuppressionLevel(0.99f)
                _spectralGateActive.value = true
                voiceManager.spectralGateActive = true
                speakJarvis("Максимальное шумоподавление 99% со спектральным гейтом активировано, сэр.")
                addHudLog("J.A.R.V.I.S.", "Шумоподавление переведено в боевой режим (99% + Spectral Gate).")
            }
            command.simulationAction == "TAP_SIMULATION" -> {
                simulateTapOnScreen(0.5f, 0.45f)
            }
            command.simulationAction == "GENERATE_CODE" -> {
                val codeSnippet = "def stark_arc_reactor():\n    return {'core': 'Palladium/Vibranium', 'output': '3.2 GW', 'status': 'STABLE'}"
                speakJarvis("Сэр, логическое ядро DeepSeek R1 сгенерировало требуемый блок кода.")
                addHudLog("DEEPSEEK 8GB ЯДРО", "Сгенерирован код:\n$codeSnippet")
            }
            command.simulationAction == "OPEN_SETTINGS" -> {
                speakJarvis("Открываю панель параметров операционной системы, сэр.")
                deviceManager.openSystemSettings(android.provider.Settings.ACTION_SETTINGS)
            }
            command.simulationAction == "STEALTH_PROTOCOL" -> {
                deviceManager.vibrateHaptic(200)
                speakJarvis("Протокол скрытности запущен, сэр. Журналы зашифрованы.")
                addHudLog("J.A.R.V.I.S.", "Режим скрытности активирован.")
            }
            command.simulationAction == "SET_DEFAULT_ASSISTANT" -> {
                speakJarvis("Сэр, открываю меню настройки цифрового ассистента по умолчанию.")
                deviceManager.openAssistantSettings()
                addHudLog("J.A.R.V.I.S.", "Запрос на назначение Jarvis основным голосовым помощником системы.")
            }
            else -> {
                speakJarvis("Команда принята к исполнению, сэр.")
            }
        }
    }

    fun processVoiceInput(text: String) {
        val lower = text.lowercase(Locale.ROOT)
        addHudLog("ВЫ", text, isJarvis = false)

        // 1. Check for autonomous "сделай сам" prefix
        if (lower.contains("сделай сам")) {
            executeSelfAction(text)
            return
        }

        // 2. Check for morning phrase "джарвис я проснулся" or "я проснулся"
        if (lower.contains("проснулся") || lower.contains("доброе утро")) {
            executeWakeUpBriefing()
            return
        }

        // 3. Check for drive phrase "поехали"
        if (lower.contains("поехали")) {
            executeDriveProtocol()
            return
        }

        // 4. Check for direct search queries in Yandex or Google
        if (lower.contains("найди в яндекс") || lower.contains("яндекс найди") || lower.contains("поиск в яндекс")) {
            val q = text.replace(Regex("(?i)(джарвис,?\\s*)?(найди в яндекс[е]?|яндекс найди|поиск в яндекс[е]?)"), "").trim()
            speakJarvis("Ищу в Яндекс: $q, сэр.")
            deviceManager.searchYandex(q.ifEmpty { "погода" })
            return
        }
        if (lower.contains("найди в гугл") || lower.contains("гугл найди") || lower.contains("поиск в гугл") || lower.contains("google")) {
            val q = text.replace(Regex("(?i)(джарвис,?\\s*)?(найди в гугл[е]?|гугл найди|поиск в google|поиск в гугл[е]?)"), "").trim()
            speakJarvis("Ищу в Google: $q, сэр.")
            deviceManager.searchGoogle(q.ifEmpty { "погода" })
            return
        }

        // 5. Check for memorizing facts about user
        if (lower.contains("запомни") || lower.contains("запиши факт")) {
            val factContent = text.replace(Regex("(?i)(джарвис,?\\s*)?(запомни,?\\s*что|запомни|запиши факт)"), "").trim()
            addFact("Личные данные", factContent.ifEmpty { "Пользователь отдал директиву Джарвису." })
            return
        }

        // 6. Check custom commands registered by user
        val customCmd = _commandsList.value.firstOrNull { cmd ->
            val triggerWords = cmd.voiceTrigger.lowercase().replace("джарвис,", "").replace("когда я говорю", "").trim()
            triggerWords.isNotEmpty() && lower.contains(triggerWords)
        }
        if (customCmd != null) {
            executeCommand(customCmd)
            return
        }

        // 7. General Stark responses
        val response = when {
            lower.contains("привет") || lower.contains("здравствуй") -> {
                "Всегда к вашим услугам, сэр. Чем могу быть полезен?"
            }
            lower.contains("кто ты") -> {
                "Я Джарвис — автономный кибернетический помощник и интеллектуальная система Тони Старка на базе Android."
            }
            lower.contains("код") || lower.contains("программир") -> {
                "Локальное 8-гигабайтное ядро рассуждений DeepSeek готово к проектированию систем и анализу кода, сэр."
            }
            lower.contains("батаре") || lower.contains("заряд") -> {
                val bat = deviceManager.getBatteryLevel()
                "Заряд аккумулятора составляет $bat процентов, сэр."
            }
            lower.contains("фонарик") || lower.contains("свет") -> {
                toggleFlashlight()
                return
            }
            lower.contains("клик") || lower.contains("нажми") || lower.contains("тап") -> {
                simulateTapOnScreen(0.5f, 0.5f)
                return
            }
            else -> {
                "Ваш запрос обработан нейросетью, сэр: «$text». Приступаю к выполнению директивы."
            }
        }

        speakJarvis(response)
        addHudLog("J.A.R.V.I.S.", response)
    }

    fun speakJarvis(text: String) {
        val currentVoice = _voicesList.value.firstOrNull { it.id == _selectedVoiceId.value }
        val pitch = if (currentVoice?.isCustom == true) _customPitch.value else currentVoice?.pitch ?: 0.90f
        val speed = if (currentVoice?.isCustom == true) _customSpeed.value else currentVoice?.speed ?: 1.05f
        voiceManager.speak(text, pitch, speed)
    }

    fun addHudLog(sender: String, message: String, isJarvis: Boolean = true) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeStr = timeFormat.format(Date())
        val newLog = HudLogMessage(
            sender = sender,
            message = message,
            timestamp = timeStr,
            isJarvis = isJarvis
        )
        val current = _hudLogs.value.toMutableList()
        current.add(0, newLog)
        if (current.size > 60) {
            current.removeAt(current.size - 1)
        }
        _hudLogs.value = current
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
