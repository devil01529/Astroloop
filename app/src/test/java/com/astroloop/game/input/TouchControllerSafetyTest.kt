package com.astroloop.game.input

import android.view.MotionEvent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TouchControllerSafetyTest {
    private fun send(controller: TouchController, action: Int, x: Float, y: Float) {
        val event = MotionEvent.obtain(0, 10, action, x, y, 0)
        try { controller.handleTouchEvent(event) } finally { event.recycle() }
    }

    @Test fun `system cancellation never selects a menu item`() {
        val controller = TouchController()
        send(controller, MotionEvent.ACTION_DOWN, 100f, 100f)
        send(controller, MotionEvent.ACTION_CANCEL, 100f, 100f)
        assertFalse(controller.consumeTap())
        assertFalse(controller.isJoystickActive())
        assertEquals(0f, controller.moveMagnitude, 0f)
    }

    @Test fun `release coordinates prevent a drag being mistaken for a tap`() {
        val controller = TouchController()
        send(controller, MotionEvent.ACTION_DOWN, 100f, 100f)
        send(controller, MotionEvent.ACTION_UP, 500f, 100f)
        assertFalse(controller.consumeTap())
    }

    @Test fun `ordinary scaled tap still works`() {
        val controller = TouchController().apply { renderScale = 2f }
        send(controller, MotionEvent.ACTION_DOWN, 100f, 100f)
        send(controller, MotionEvent.ACTION_UP, 100f, 100f)
        assertTrue(controller.consumeTap())
        assertEquals(50f, controller.lastTapX, 0f)
        assertFalse(controller.consumeTap())
    }

    @Test fun `pause reset discards pending taps and movement`() {
        val controller = TouchController()
        send(controller, MotionEvent.ACTION_DOWN, 100f, 100f)
        send(controller, MotionEvent.ACTION_UP, 100f, 100f)
        controller.reset()
        assertFalse(controller.consumeTap())
        send(controller, MotionEvent.ACTION_UP, 100f, 100f)
        assertFalse(controller.consumeTap())
    }

    @Test fun `remaining finger takes control without jumping or tapping`() {
        val controller = TouchController()
        send(controller, MotionEvent.ACTION_DOWN, 100f, 100f)
        fun multi(action: Int, secondX: Float) {
            val props = Array(2) { i -> MotionEvent.PointerProperties().apply { id = i; toolType = MotionEvent.TOOL_TYPE_FINGER } }
            val coords = Array(2) { i -> MotionEvent.PointerCoords().apply { x = if (i == 0) 100f else secondX; y = 100f } }
            val event = MotionEvent.obtain(0, 20, action, 2, props, coords, 0, 0, 1f, 1f, 0, 0, 0, 0)
            try { controller.handleTouchEvent(event) } finally { event.recycle() }
        }
        multi(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 300f)
        multi(MotionEvent.ACTION_POINTER_UP, 300f)
        assertTrue(controller.isJoystickActive())
        assertEquals(0f, controller.moveMagnitude, 0f)
        multi(MotionEvent.ACTION_MOVE, 500f)
        assertTrue(controller.moveMagnitude > 0f)
        send(controller, MotionEvent.ACTION_UP, 300f, 100f)
        assertFalse(controller.consumeTap())
        assertFalse(controller.isJoystickActive())
    }
}
