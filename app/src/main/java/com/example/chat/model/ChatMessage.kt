package com.example.chat.model

enum class Role { User, Assistant }

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: Role,
    val content: String,
    val imageDataUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
