package com.lumaplay.tv.player.diagnostics

import androidx.media3.ui.AspectRatioFrameLayout
import org.junit.Assert.assertEquals
import org.junit.Test

class AspectRatioModeTest {

    @Test
    fun testModeLabels() {
        assertEquals("Fit", AspectRatioMode.FIT.label)
        assertEquals("Fill", AspectRatioMode.FILL.label)
        assertEquals("Zoom", AspectRatioMode.ZOOM.label)
    }

    @Test
    fun testResizeModeMappings() {
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FIT, AspectRatioMode.FIT.resizeMode)
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FILL, AspectRatioMode.FILL.resizeMode)
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, AspectRatioMode.ZOOM.resizeMode)
    }

    @Test
    fun testModeCyclingOrder() {
        assertEquals(AspectRatioMode.FILL, AspectRatioMode.FIT.next())
        assertEquals(AspectRatioMode.ZOOM, AspectRatioMode.FILL.next())
        assertEquals(AspectRatioMode.FIT, AspectRatioMode.ZOOM.next())
    }
}
