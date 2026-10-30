package com.astroloop.game.input

import android.content.Context
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.core.DesignSpace
import com.astroloop.game.core.GamePhase
import com.astroloop.game.core.GameSurfaceView
import com.astroloop.game.data.PersistenceManager
import com.astroloop.game.entity.Ship
import com.astroloop.game.settings.PilotOptions
import com.astroloop.game.settings.PilotSettings
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class FixedFlightIntegrationTest {
    @Test fun `real flight uses fixed controls and pause switches back to menu taps`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PersistenceManager(context).resetAllProgress(); PilotSettings.save(context, PilotOptions())
        val view = GameSurfaceView(context, "ship_blue", "pilot_medic") { _, _ -> }
        view.surfaceChanged(view.holder, 0, 1080, 2400)
        view.state.phase = GamePhase.PLAYING
        val scale = DesignSpace.metricsFor(1080f, 2400f).renderScale
        val pad = FixedStickLayout.forScreen(view.layout)
        fun send(action: Int, x: Float, y: Float) {
            val event = MotionEvent.obtain(0, 20, action, x * scale, y * scale, 0)
            try { view.onTouchEvent(event) } finally { event.recycle() }
        }
        send(MotionEvent.ACTION_DOWN, pad.x + pad.radius, pad.y)
        view.update(1f / 60)
        val input = ReflectionHelpers.getField<TouchController>(view, "touchController")
        val ship = ReflectionHelpers.getField<Ship>(view, "ship")
        assertTrue(input.isFixedJoystickEnabled()); assertTrue(ship.precisionSteering)
        assertTrue(ship.velocity.x > 0f)
        send(MotionEvent.ACTION_UP, pad.x + pad.radius, pad.y)
        send(MotionEvent.ACTION_DOWN, pad.x, pad.y); send(MotionEvent.ACTION_UP, pad.x, pad.y)
        assertFalse(view.state.isPaused)
        view.pause()
        assertEquals(0f, input.moveMagnitude, 0f)
        send(MotionEvent.ACTION_DOWN, 500f, 500f); send(MotionEvent.ACTION_UP, 500f, 500f)
        assertFalse(input.isFixedJoystickEnabled())
        assertTrue(input.consumeTap())
    }
}
