package com.aibuilt.claudeclone

import com.aibuilt.claudeclone.model.ChatMessage
import com.aibuilt.claudeclone.model.Conversation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun userMessage_isMarkedAsUser() {
        val message = ChatMessage(role = ChatMessage.ROLE_USER, content = "Hello")
        assertTrue(message.isUser)
    }

    @Test
    fun assistantMessage_isNotMarkedAsUser() {
        val message = ChatMessage(role = ChatMessage.ROLE_ASSISTANT, content = "Hi")
        assertFalse(message.isUser)
    }

    @Test
    fun conversationPreview_usesLastUserMessage() {
        val conversation = Conversation(
            title = "Test",
            messages = listOf(
                ChatMessage(role = ChatMessage.ROLE_USER, content = "first"),
                ChatMessage(role = ChatMessage.ROLE_ASSISTANT, content = "reply"),
                ChatMessage(role = ChatMessage.ROLE_USER, content = "second line")
            )
        )
        assertEquals("second line", conversation.preview)
    }
}
