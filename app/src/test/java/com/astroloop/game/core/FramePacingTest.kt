package com.astroloop.game.core

import org.junit.Assert.assertEquals
import org.junit.Test

class FramePacingTest {
    @Test fun `balanced mode caps a high refresh display`() {
        assertEquals(16_666_666L, frameBudgetNanos(120f, 60))
        assertEquals(33_333_333L, frameBudgetNanos(30f, 60))
    }

    @Test fun `120 Hz retains fractional milliseconds`() {
        assertEquals(8_333_333L, frameBudgetNanos(120f))
    }
    @Test fun `60 Hz avoids rendering twice per display refresh`() {
        assertEquals(16_666_666L, frameBudgetNanos(60f))
    }
    @Test fun `unsupported readings use a safe default`() {
        for (rate in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertEquals(16_666_666L, frameBudgetNanos(rate))
        }
        assertEquals(8_333_333L, frameBudgetNanos(144f))
    }
}
