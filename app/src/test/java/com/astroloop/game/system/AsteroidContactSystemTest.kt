package com.astroloop.game.system

import com.astroloop.game.entity.Asteroid
import com.astroloop.game.entity.AsteroidType
import com.astroloop.game.entity.Ship
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class AsteroidContactSystemTest {
    private fun rock(x: Float, y: Float, vx: Float = 0f, vy: Float = 0f, radius: Float = 25f) =
        Asteroid().apply {
            position.set(x, y); velocity.set(vx, vy); this.radius = radius
            health = 50f; maxHealth = 50f; rotationSpeed = 0f
        }

    private fun momentum(rocks: List<Asteroid>, y: Boolean = false) = rocks.sumOf {
        (AsteroidImpact.mass(it) * if (y) it.velocity.y else it.velocity.x).toDouble()
    }

    private fun energy(rocks: List<Asteroid>) = rocks.sumOf {
        val mass = AsteroidImpact.mass(it)
        (.5f * mass * it.velocity.lengthSquared() +
            .25f * mass * it.radius * it.radius * it.rotationSpeed * it.rotationSpeed).toDouble()
    }

    private fun angularMomentum(rocks: List<Asteroid>) = rocks.sumOf {
        val mass = AsteroidImpact.mass(it)
        (mass * (it.position.x * it.velocity.y - it.position.y * it.velocity.x) +
            .5f * mass * it.radius * it.radius * it.rotationSpeed).toDouble()
    }

    private fun assertPhysical(beforeX: Double, beforeY: Double, beforeEnergy: Double, rocks: List<Asteroid>) {
        assertEquals(beforeX, momentum(rocks), .01)
        assertEquals(beforeY, momentum(rocks, true), .01)
        assertTrue("Contact must not add kinetic energy", energy(rocks) <= beforeEnergy + .01)
        assertTrue(rocks.all { it.velocity.x.isFinite() && it.velocity.y.isFinite() && it.rotationSpeed.isFinite() })
    }

    @Test fun `head-on rocks rebound once with conserved momentum and reduced energy`() {
        val rocks = listOf(rock(-25f, 0f, 100f), rock(25f, 0f, -100f))
        val solver = AsteroidContactSystem()
        solver.resolve(rocks)
        assertEquals(-55f, rocks[0].velocity.x, .001f)
        assertEquals(55f, rocks[1].velocity.x, .001f)
        assertPhysical(0.0, 0.0, 10000.0, rocks)
        solver.resolve(rocks)
        assertEquals(-55f, rocks[0].velocity.x, .001f)
        assertTrue(rocks.all { it.health == 50f && it.isActive })
    }

    @Test fun `large dense rocks deflect less than small icy rocks`() {
        val heavy = rock(0f, 0f, 50f, radius = 50f).apply { type = AsteroidType.METAL }
        val light = rock(75f, 0f, -150f).apply { type = AsteroidType.ICE }
        val rocks = listOf(heavy, light)
        val px = momentum(rocks); val e = energy(rocks)
        AsteroidContactSystem().resolve(rocks)
        assertTrue(heavy.velocity.x > 0f)
        assertTrue(light.velocity.x > heavy.velocity.x)
        assertTrue(abs(light.velocity.x + 150f) > abs(heavy.velocity.x - 50f) * 9f)
        assertPhysical(px, 0.0, e, rocks)
    }

    @Test fun `glancing impact redirects both trajectories and trades slip for spin`() {
        val rocks = listOf(rock(0f, 0f, 110f, 10f), rock(40f, 30f, -20f))
        val px = momentum(rocks); val py = momentum(rocks, true)
        val e = energy(rocks); val angular = angularMomentum(rocks)
        AsteroidContactSystem().resolve(rocks)
        assertTrue(rocks[0].velocity.y < 0f)
        assertTrue(rocks[1].velocity.y > 0f)
        assertTrue(abs(rocks[0].rotationSpeed) > .01f)
        assertTrue(abs(rocks[1].rotationSpeed) > .01f)
        assertEquals(angular, angularMomentum(rocks), .02)
        assertPhysical(px, py, e, rocks)
    }

    @Test fun `overlapping separating rocks are repositioned without another bounce`() {
        val a = rock(0f, 0f, -30f, radius = 50f)
        val b = rock(55f, 0f, 60f)
        val centre = a.position.x * AsteroidImpact.mass(a) + b.position.x * AsteroidImpact.mass(b)
        AsteroidContactSystem().resolve(listOf(a, b))
        assertEquals(-30f, a.velocity.x, 0f); assertEquals(60f, b.velocity.x, 0f)
        assertTrue(b.position.x - a.position.x > 74.5f)
        assertEquals(centre, a.position.x * AsteroidImpact.mass(a) + b.position.x * AsteroidImpact.mass(b), .001f)
        assertTrue(abs(a.position.x) < abs(b.position.x - 55f))
    }

    @Test fun `coincident and slow contacts settle without jitter or an energy injection`() {
        val still = listOf(rock(0f, 0f), rock(0f, 0f))
        val solver = AsteroidContactSystem()
        repeat(8) { solver.resolve(still) }
        assertTrue(still[0].distanceTo(still[1]) >= 49.97f)
        assertPhysical(0.0, 0.0, 0.0, still)
        val slow = listOf(rock(-25f, 0f, 1f), rock(25f, 0f, -1f))
        repeat(8) { solver.resolve(slow) }
        assertEquals(0.0, energy(slow), .0001)
        val moving = listOf(rock(0f, 0f, 80f), rock(0f, 0f, -80f))
        solver.resolve(moving)
        assertTrue(moving[0].velocity.x < 0f && moving[1].velocity.x > 0f)
        assertPhysical(0.0, 0.0, 6400.0, moving)
    }

    @Test fun `contacts propagate through a chain without creating energy or destroying rocks`() {
        val rocks = listOf(rock(0f, 0f, 200f), rock(50f, 0f), rock(100f, 0f))
        val movement = MovementSystem()
        repeat(120) { movement.updateAsteroids(rocks, Ship(), 1f / 120f) }
        assertTrue(rocks[2].velocity.x > 100f)
        assertTrue(rocks.zipWithNext().all { (a, b) -> a.position.x + 49.9f <= b.position.x })
        assertPhysical(200.0, 0.0, 20000.0, rocks)
        assertTrue(rocks.all { it.isActive && it.health == 50f })
    }

    @Test fun `nearby grid pairs work across negative coordinates with varied radii`() {
        for ((x, y) in listOf(-1000f to -1000f, -2f to -3f, 199f to 199f)) {
            val a = rock(x, y, 60f, radius = 80f)
            val b = rock(x + 90f, y, -60f, radius = 10f)
            AsteroidContactSystem().resolve(listOf(a, b))
            assertTrue(b.velocity.x > 60f)
            assertTrue(a.velocity.x < 60f)
        }
    }

    @Test fun `grid avoids all-pairs scans and clears stale pooled entries`() {
        val rocks = (0 until 400).flatMap { i ->
            val x = (i % 20 - 10) * 150f
            val y = (i / 20 - 10) * 150f
            listOf(rock(x - 11f, y, 60f, radius = 12f), rock(x + 11f, y, -60f, radius = 12f))
        }
        val solver = AsteroidContactSystem()
        solver.resolve(rocks)
        assertTrue(rocks.chunked(2).all { (a, b) -> a.velocity.x < 0 && b.velocity.x > 0 })
        assertTrue("Sparse field must not test every pair", solver.candidateChecks < rocks.size * 12)
        val removed = rock(0f, 0f, 100f).apply { isActive = false }
        val active = rock(0f, 0f, 30f)
        solver.resolve(listOf(removed, active))
        assertEquals(30f, active.velocity.x, 0f)
        assertEquals(0f, active.position.x, 0f)
        assertEquals(0, solver.candidateChecks)
    }

    @Test fun `substeps catch small fast rocks that would cross completely in one frame`() {
        val rocks = listOf(rock(-8f, 0f, 650f, radius = 3f), rock(8f, 0f, -650f, radius = 3f))
        val before = energy(rocks)
        MovementSystem().updateAsteroids(rocks, Ship(), .033f)
        assertTrue(rocks[0].position.x < rocks[1].position.x)
        assertTrue(rocks[0].velocity.x < 0f && rocks[1].velocity.x > 0f)
        assertPhysical(0.0, 0.0, before, rocks)
    }

    @Test fun `collision trajectories agree at 30 60 and 120 frames per second`() {
        fun simulate(fps: Int): List<Asteroid> {
            val rocks = listOf(rock(-110f, -12f, 160f), rock(110f, 12f, -140f))
            val movement = MovementSystem(); val ship = Ship()
            repeat(fps) { movement.updateAsteroids(rocks, ship, 1f / fps) }
            return rocks
        }
        val reference = simulate(120)
        for (fps in listOf(30, 60)) for ((actual, expected) in simulate(fps).zip(reference)) {
            assertEquals(expected.position.x, actual.position.x, .05f)
            assertEquals(expected.position.y, actual.position.y, .05f)
            assertEquals(expected.velocity.x, actual.velocity.x, .05f)
            assertEquals(expected.velocity.y, actual.velocity.y, .05f)
        }
        assertTrue(reference[0].velocity.y < 0 && reference[1].velocity.y > 0)
    }

    @Test fun `paused or invalid time does not move separate or spin rocks`() {
        val rocks = listOf(rock(0f, 0f, 80f), rock(0f, 0f, -80f))
        rocks[0].fragmentImmunityTimer = .1f
        val movement = MovementSystem()
        for (dt in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            movement.updateAsteroids(rocks, Ship(), dt)
        }
        assertTrue(rocks.all { it.position.x == 0f && it.tumbleX == 0f })
        assertEquals(80f, rocks[0].velocity.x, 0f)
        assertEquals(.1f, rocks[0].fragmentImmunityTimer, 0f)
    }

    @Test fun `sustained crowded collisions remain finite and dissipative`() {
        val rocks = (0 until 48).map { i ->
            rock((i % 8) * 40f, (i / 8) * 40f,
                if (i % 2 == 0) 100f else -100f, if (i % 3 == 0) 70f else -40f)
        }
        val px = momentum(rocks); val py = momentum(rocks, true); val initial = energy(rocks)
        val movement = MovementSystem(); val ship = Ship()
        repeat(600) { movement.updateAsteroids(rocks, ship, 1f / 120f) }
        assertPhysical(px, py, initial, rocks)
        assertTrue(rocks.all { it.position.x.isFinite() && it.position.y.isFinite() })
        for (i in rocks.indices) for (j in i + 1 until rocks.size) {
            assertTrue("Rocks must not stay embedded", rocks[i].distanceTo(rocks[j]) > 49.9f)
        }
    }
}
