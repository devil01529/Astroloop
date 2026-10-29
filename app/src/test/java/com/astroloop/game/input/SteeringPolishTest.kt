package com.astroloop.game.input

import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.settings.PilotOptions
import com.astroloop.game.settings.PilotSettings
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SteeringPolishTest {
    @Before fun defaults() { PilotSettings.save(ApplicationProvider.getApplicationContext(),PilotOptions()) }
    private fun send(c:TouchController,action:Int,x:Float,y:Float,time:Long=10) {
        val event=MotionEvent.obtain(0,time,action,x,y,0)
        try { c.handleTouchEvent(event) } finally {event.recycle()}
    }
    @Test fun `drag returning to origin does not activate a menu`() {
        val c=TouchController();send(c,MotionEvent.ACTION_DOWN,100f,100f)
        send(c,MotionEvent.ACTION_MOVE,140f,100f);send(c,MotionEvent.ACTION_MOVE,100f,100f)
        send(c,MotionEvent.ACTION_UP,100f,100f);assertFalse(c.consumeTap())
    }
    @Test fun `stick follows a long drag and can reverse without crossing the screen`() {
        val c=TouchController();send(c,MotionEvent.ACTION_DOWN,100f,100f)
        send(c,MotionEvent.ACTION_MOVE,900f,100f)
        assertEquals(1f,c.moveMagnitude,.001f)
        val origin=c.getJoystickOrigin().x
        assertTrue(origin>100f)
        send(c,MotionEvent.ACTION_MOVE,origin-30f,100f)
        assertTrue(c.moveDirection.x<0f)
    }
    @Test fun `low sensitivity still permits full speed`() {
        PilotSettings.save(ApplicationProvider.getApplicationContext(),PilotOptions(sensitivity=.7f))
        val c=TouchController();send(c,MotionEvent.ACTION_DOWN,100f,100f)
        send(c,MotionEvent.ACTION_MOVE,100f+c.getMaxRadius()+10f,100f)
        assertEquals(1f,c.moveMagnitude,.001f)
    }
    @Test fun `a held finger is not a tap`() {
        val c=TouchController();send(c,MotionEvent.ACTION_DOWN,100f,100f,0)
        send(c,MotionEvent.ACTION_UP,100f,100f,600)
        assertFalse(c.consumeTap())
    }
}
