package com.jiyi.power

import com.jiyi.power.app.utils.ScreenTextProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTextProtocolTest {
    @Test
    fun buildsReadCommandAndParsesText() {
        val protocol = ScreenTextProtocol()

        assertEquals("AA00C200C255", protocol.readCommand())
        assertEquals(listOf("Hello"), protocol.accept(frame("Hello")))
    }

    @Test
    fun fragmentedReplyIsReassembled() {
        val protocol = ScreenTextProtocol()
        val reply = frame("Screen text")

        assertTrue(protocol.accept(reply.take(12)).isEmpty())
        assertEquals(listOf("Screen text"), protocol.accept(reply.drop(12)))
    }

    private fun frame(text: String): String {
        val data = text.toByteArray(Charsets.UTF_8).toList()
        val body = listOf(2, 0xC2, data.size) + data.map { it.toInt() and 0xFF }
        return (listOf(0xAA) + body + listOf(body.sum() and 0xFF, 0x55))
            .joinToString("") { "%02X".format(it) }
    }
}
