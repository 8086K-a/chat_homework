package com.example.chat.data.repository

import com.example.chat.data.local.AppDatabase
import com.example.chat.data.local.entity.ConversationEntity
import com.example.chat.data.local.entity.MessageEntity
import com.example.chat.data.remote.OpenAiService
import com.example.chat.data.remote.SimpleInputItem
import com.example.chat.model.ChatMessage
import com.example.chat.model.Role
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepository(
    private val db: AppDatabase,
    private val apiService: OpenAiService,
) {
    private val conversationDao = db.conversationDao()
    private val messageDao = db.messageDao()

    fun getConversations(): Flow<List<ConversationEntity>> = conversationDao.getAll()

    suspend fun createConversation(title: String = "新对话"): ConversationEntity {
        val conv = ConversationEntity(title = title)
        conversationDao.insert(conv)
        return conv
    }

    suspend fun updateConversationTitle(conversationId: String, title: String) {
        val conv = conversationDao.getById(conversationId) ?: return
        conversationDao.update(conv.copy(title = title))
    }

    suspend fun deleteConversation(conversationId: String) {
        conversationDao.deleteById(conversationId)
    }

    fun getMessages(conversationId: String): Flow<List<ChatMessage>> {
        return messageDao.getByConversation(conversationId).map { entities ->
            entities.mapNotNull { entity ->
                try {
                    ChatMessage(
                        id = entity.id,
                        role = if (entity.role == "user") Role.User else Role.Assistant,
                        content = entity.content,
                        imageDataUrl = entity.imageDataUrl,
                        timestamp = entity.timestamp,
                    )
                } catch (_: Exception) { null }
            }
        }
    }

    suspend fun saveMessage(
        conversationId: String,
        role: Role,
        content: String,
        imageDataUrl: String? = null,
    ): ChatMessage {
        val entity = MessageEntity(
            conversationId = conversationId,
            role = if (role == Role.User) "user" else "assistant",
            content = content,
            imageDataUrl = imageDataUrl,
        )
        messageDao.insert(entity)
        return ChatMessage(
            id = entity.id,
            role = role,
            content = content,
            imageDataUrl = imageDataUrl,
            timestamp = entity.timestamp,
        )
    }

    suspend fun updateMessageContent(messageId: String, content: String) {
        messageDao.updateContent(messageId, content)
    }

    fun sendMessageStream(
        input: Any,
        model: String = OpenAiService.DEFAULT_MODEL,
    ): Flow<String> = apiService.sendMessageStream(input, model)
}
