package com.jiyi.power

import com.jiyi.power.app.bean.Payload
import com.jiyi.power.app.utils.CmdConstant
import com.jiyi.power.app.utils.MobilePowerProtocolManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PortPowerStatisticsProtocolTest {
    @Test
    fun `power query continuously reads C1 through USB-A power registers`() {
        val command = MobilePowerProtocolManager.buildReadCommand(
            CmdConstant.FunctionCode.CODE_04,
            0x0F,
        )

        assertEquals("AA10040F2355", command)
    }

    @Test
    fun `continuous power response parses all three port powers`() {
        // 0x04=300W, 0x0A=150W, 0x12=20W. Other registers in the range are zero.
        val frame = MobilePowerProtocolManager.parseFrame(
            "AA12040F2C0100000000960000000000000014FC55",
        )
        val snapshot = (frame?.payload as? Payload.RegisterBlock)?.snapshot

        assertNotNull(snapshot)
        assertEquals(300, snapshot?.c1?.powerW)
        assertEquals(150, snapshot?.c2?.powerW)
        assertEquals(20, snapshot?.usbA?.powerW)
    }
}
