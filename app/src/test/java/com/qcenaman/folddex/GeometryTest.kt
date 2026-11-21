package com.qcenaman.folddex

import com.qcenaman.folddex.shell.Flags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryTest {

    @Test
    fun fullScaleMatchesView() {
        val g = Geometry.compute(2504, 2256, 100, 320)
        assertEquals(2504, g.width)
        assertEquals(2256, g.height)
        assertEquals(320, g.dpi)
        assertEquals(1f, g.scaleX, 0.0001f)
    }

    @Test
    fun halfScaleKeepsDexSameSize() {
        val g = Geometry.compute(2504, 2256, 50, 320)
        assertEquals(1252, g.width)
        assertEquals(1128, g.height)
        assertEquals(160, g.dpi)
        assertEquals(0.5f, g.scaleX, 0.001f)
        assertEquals(0.5f, g.scaleY, 0.001f)
    }

    @Test
    fun sizesAreEvenAndClamped() {
        val g = Geometry.compute(2255, 1001, 75, 5000)
        assertTrue(g.width % 2 == 0 && g.height % 2 == 0)
        assertEquals((Geometry.MAX_DPI * 0.75f).toInt(), g.dpi)
        val tiny = Geometry.compute(10, 10, 1, 1)
        assertTrue(tiny.width >= 2 && tiny.dpi >= 60)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyView() {
        Geometry.compute(0, 100, 100, 320)
    }

    @Test
    fun flagsMatchScrcpyNewDisplay() {
        val f = Flags.forSdk(36)
        assertTrue(f and Flags.PUBLIC != 0)
        assertTrue(f and Flags.SHOULD_SHOW_SYSTEM_DECORATIONS != 0)
        assertTrue(f and Flags.TRUSTED != 0)
        assertTrue(f and Flags.OWN_FOCUS != 0)
        assertEquals(0, Flags.forSdk(30) and Flags.TRUSTED)
    }
}
