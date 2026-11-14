package com.example.model

enum class ScreenState {
    PERMISSION_GATE,
    MAIN_HUD,
    VOICES,
    SYSTEM_DIAGNOSTICS,
    LOCAL_AI_CORE,
    COMMANDS_LIBRARY,
    PHONE_CONTROL,
    NOTES_AND_MEMORY
}

data class JarvisVoice(
    val id: String,
    val name: String,
    val description: String,
    val pitch: Float,
    val speed: Float,
    val accentTag: String,
    val isCustom: Boolean = false
)

data class LocalAiEngine(
    val name: String,
    val category: String,
    val memoryFootprintMb: Int,
    val contextWindow: String,
    val role: String,
    val status: String,
    val isPrimary: Boolean = false
)

data class JarvisCommand(
    val id: String,
    val voiceTrigger: String,
    val actionCategory: String,
    val description: String,
    val simulationAction: String,
    val isCustom: Boolean = false,
    val customActionPayload: String = ""
)

data class HudLogMessage(
    val id: Long = System.currentTimeMillis(),
    val sender: String,
    val message: String,
    val timestamp: String,
    val isJarvis: Boolean = true
)

data class JarvisFact(
    val id: Long = System.currentTimeMillis(),
    val category: String,
    val fact: String,
    val dateRecorded: String
)

data class JarvisNote(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val content: String,
    val dateCreated: String,
    val isHighPriority: Boolean = false
)
