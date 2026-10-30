package com.astroloop.game.input

import com.astroloop.game.entity.Ship
import org.junit.Assert.*
import org.junit.Test

class PrecisionFlightTest {
    @Test fun `touch acceleration is consistent at 30 60 and 120 fps without increasing top speed`() {
        val speeds = listOf(30, 60, 120).map { fps ->
            val ship = Ship().apply { precisionSteering = true; moveDirection.set(1f, 0f) }
            repeat(fps / 5) { ship.update(1f / fps) }
            assertTrue(ship.velocity.x > ship.speed * .9f)
            assertTrue(ship.velocity.x <= ship.speed)
            ship.velocity.x
        }
        assertEquals(speeds[0], speeds[2], .01f)
    }
    @Test fun `release brakes promptly and reversing cannot cause a speed boost`() {
        val ship = Ship().apply { precisionSteering = true; velocity.set(speed, 0f) }
        repeat(20) { ship.update(1f / 120) }
        assertEquals(0f, ship.velocity.length(), .001f)
        ship.velocity.set(ship.speed, 0f); ship.moveDirection.set(-1f, 0f)
        repeat(24) { ship.update(1f / 120); assertTrue(ship.velocity.length() <= ship.speed + .001f) }
        assertTrue(ship.velocity.x < -ship.speed * .8f)
    }
}
