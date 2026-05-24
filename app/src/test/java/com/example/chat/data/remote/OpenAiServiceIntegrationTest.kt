package com.example.chat.data.remote

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class OpenAiServiceIntegrationTest {

    @Test
    fun deepseek_stream_returns_content() = runBlocking {
        val service = OpenAiService()
        val messages = listOf(
            ChatMessageDto(
                role = "user",
                content = "Reply with exactly: OK",
            )
        )

        val firstToken = withTimeoutOrNull(30_000) {
            service.sendMessageStream(messages).firstOrNull()
        }

        assertNotNull("Expected at least one streamed token from DeepSeek", firstToken)
        assertFalse("First token should not be blank", firstToken!!.isBlank())
    }
}
