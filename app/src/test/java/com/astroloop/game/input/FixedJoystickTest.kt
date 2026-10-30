package com.astroloop.game.input

import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.core.ScreenLayout
import com.astroloop.game.settings.PilotOptions
import com.astroloop.game.settings.PilotSettings
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FixedJoystickTest {
    @Before fun defaults() { PilotSettings.save(ApplicationProvider.getApplicationContext(), PilotOptions()) }
    private fun controller() = TouchController().apply {
        configureFixedJoystick(ScreenLayout.compute(960f, 2142f, 20f, 80f, 20f, 80f), false)
        setFixedJoystickEnabled(true)
    }
    private fun send(c: TouchController, action: Int, x: Float, y: Float, ids: IntArray = intArrayOf(0), extraX: Float = 800f, extraY: Float = 100f) {
        val props = Array(ids.size) { i -> MotionEvent.PointerProperties().apply { id = ids[i]; toolType = MotionEvent.TOOL_TYPE_FINGER } }
        val coords = Array(ids.size) { i -> MotionEvent.PointerCoords().apply { this.x = if (i == 0) x else extraX; this.y = if (i == 0) y else extraY } }
        val event = MotionEvent.obtain(0, 20, action, ids.size, props, coords, 0, 0, 1f, 1f, 0, 0, 0, 0)
        try { c.handleTouchEvent(event) } finally { event.recycle() }
    }
    @Test fun `long drags never move the fixed centre and reversal remains predictable`() {
        val c = controller(); val pad = c.fixedLayout
        send(c, MotionEvent.ACTION_DOWN, pad.x, pad.y)
        send(c, MotionEvent.ACTION_MOVE, 2000f, pad.y)
        assertEquals(pad.x, c.getJoystickOrigin().x, 0f)
        assertEquals(pad.x + pad.radius, c.getJoystickPosition().x, .001f)
        assertEquals(1f, c.moveMagnitude, .001f)
        send(c, MotionEvent.ACTION_MOVE, pad.x - pad.radius, pad.y)
        assertEquals(-1f, c.moveDirection.x, .001f)
        assertEquals(1f, c.moveMagnitude, .001f)
    }
    @Test fun `dead zone eliminates drift and ring edge reaches full speed at every sensitivity`() {
        for (sensitivity in listOf(.7f, 1f, 1.6f)) {
            PilotSettings.save(ApplicationProvider.getApplicationContext(), PilotOptions(sensitivity = sensitivity))
            val c = controller(); val p = c.fixedLayout
            send(c, MotionEvent.ACTION_DOWN, p.x, p.y)
            send(c, MotionEvent.ACTION_MOVE, p.x + p.deadZone * .9f, p.y)
            assertEquals(0f, c.moveMagnitude, 0f)
            send(c, MotionEvent.ACTION_MOVE, p.x + p.radius, p.y)
            assertEquals(1f, c.moveMagnitude, .0001f)
            assertEquals(p.radius, c.getMaxRadius(), 0f)
        }
    }
    @Test fun `touching and dragging outside the pad never steers`() {
        val c = controller()
        send(c, MotionEvent.ACTION_DOWN, 600f, 400f)
        send(c, MotionEvent.ACTION_MOVE, 900f, 900f)
        assertFalse(c.isJoystickActive()); assertEquals(0f, c.moveMagnitude, 0f)
    }
    @Test fun `pad release cannot select a card or become double tap pause`() {
        val c = controller(); val p = c.fixedLayout
        send(c, MotionEvent.ACTION_DOWN, p.x, p.y)
        val up = MotionEvent.obtain(0, 30, MotionEvent.ACTION_UP, p.x, p.y, 0)
        try { assertFalse(c.isTapCandidate(up)); c.handleTouchEvent(up) } finally { up.recycle() }
        assertFalse(c.consumeTap()); assertFalse(c.isJoystickActive())
        assertEquals(p.x, c.getJoystickPosition().x, 0f)
    }
    @Test fun `other fingers cannot steal or sustain thrust when the thumb lifts`() {
        val c = controller(); val p = c.fixedLayout
        send(c, MotionEvent.ACTION_DOWN, p.x, p.y)
        send(c, MotionEvent.ACTION_MOVE, p.x + p.radius, p.y)
        send(c, MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), p.x + p.radius, p.y, intArrayOf(0, 1))
        assertEquals(1f, c.moveDirection.x, .001f)
        send(c, MotionEvent.ACTION_POINTER_UP, p.x + p.radius, p.y, intArrayOf(0, 1))
        send(c, MotionEvent.ACTION_MOVE, 800f, 100f, intArrayOf(1))
        assertFalse(c.isJoystickActive()); assertEquals(0f, c.moveMagnitude, 0f)
        send(c, MotionEvent.ACTION_UP, 800f, 100f, intArrayOf(1))
        assertFalse(c.consumeTap())
    }
    @Test fun `thumb can start while an unrelated finger is already touching`() {
        val c = controller(); val p = c.fixedLayout
        send(c, MotionEvent.ACTION_DOWN, 800f, 100f)
        send(c, MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 800f, 100f,
            intArrayOf(0, 1), p.x + p.radius, p.y)
        assertTrue(c.isJoystickActive()); assertEquals(1f, c.moveMagnitude, .001f)
    }
    @Test fun `cancel resize and handedness changes reset input while preserving the anchored pad`() {
        val c = controller(); val p = c.fixedLayout
        send(c, MotionEvent.ACTION_DOWN, p.x + p.radius, p.y)
        send(c, MotionEvent.ACTION_CANCEL, p.x + p.radius, p.y)
        assertEquals(0f, c.moveMagnitude, 0f); assertFalse(c.consumeTap())
        send(c, MotionEvent.ACTION_DOWN, p.x + p.radius, p.y)
        val landscape = ScreenLayout.compute(2142f, 960f, 80f, 20f, 80f, 40f)
        c.configureFixedJoystick(landscape, true)
        assertFalse(c.isJoystickActive()); assertEquals(0f, c.moveMagnitude, 0f)
        val right = c.fixedLayout
        assertTrue(right.x - right.radius > landscape.safe.centerX)
        assertTrue(right.x + right.radius < landscape.safe.right)
        assertTrue(right.y + right.radius < landscape.safe.bottom)
    }
    @Test fun `scaled touch aligns with drawn pad and menus still accept ordinary taps`() {
        val c = controller().apply { renderScale = 2f }; val p = c.fixedLayout
        send(c, MotionEvent.ACTION_DOWN, (p.x + p.radius) * 2, p.y * 2)
        assertEquals(1f, c.moveMagnitude, .001f)
        c.setFixedJoystickEnabled(false)
        send(c, MotionEvent.ACTION_DOWN, 400f, 300f)
        send(c, MotionEvent.ACTION_UP, 400f, 300f)
        assertTrue(c.consumeTap()); assertEquals(200f, c.lastTapX, 0f)
    }
}
