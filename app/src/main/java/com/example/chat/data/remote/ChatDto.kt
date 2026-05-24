package com.example.chat.data.remote

data class SimpleInputItem(
    val role: String,
    val content: String,
)

data class ToolDef(
    val type: String,
)

data class ResponseRequest(
    val model: String = "qwen3.5-flash",
    val input: Any,
    val stream: Boolean = true,
    val tools: List<ToolDef>? = null,
)
