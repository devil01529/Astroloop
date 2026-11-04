package com.astroloop.game.system

import com.astroloop.game.entity.Asteroid
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Solid, mass-sensitive rock contacts. No damage, loot, or cosmetic fragments live here. */
class AsteroidContactSystem {
    companion object {
        private const val PASSES = 3
        private const val RESTITUTION = .55f
        private const val FRICTION = .18f
        private const val RESTING_SPEED = 4f
        private const val POSITION_SLOP = .02f
    }

    // Centre-only grid: cells are as wide as the largest diameter, so contacts
    // must be in the same cell or one of its eight neighbours. Reused primitive
    // buffers avoid allocating buckets, pair sets, or vectors every substep.
    private var keys = LongArray(32)
    private var heads = IntArray(32) { -1 }
    private var next = IntArray(16)
    private var cellX = IntArray(16)
    private var cellY = IntArray(16)

    internal var candidateChecks = 0
        private set

    /** Resolve once per movement substep, never while simulation time is paused. */
    fun resolve(asteroids: List<Asteroid>) {
        candidateChecks = 0
        if (asteroids.size < 2) return
        var diameter = 1f
        for (rock in asteroids) if (rock.isActive) diameter = max(diameter, rock.radius * 2f)
        ensureCapacity(asteroids.size)
        repeat(PASSES) {
            buildGrid(asteroids, diameter)
            for (i in asteroids.indices) {
                val a = asteroids[i]
                if (!a.isActive) continue
                // Query the cell at insertion time; corrections can move a into
                // another cell, which the next rebuilt solver pass will cover.
                for (x in cellX[i] - 1..cellX[i] + 1) {
                    for (y in cellY[i] - 1..cellY[i] + 1) {
                        var j = heads[slot(key(x, y))]
                        while (j >= 0) {
                            if (j > i) {
                                candidateChecks++
                                resolvePair(a, asteroids[j])
                            }
                            j = next[j]
                        }
                    }
                }
            }
        }
    }

    private fun buildGrid(asteroids: List<Asteroid>, cellSize: Float) {
        heads.fill(-1)
        for (i in asteroids.indices) {
            val rock = asteroids[i]
            if (!rock.isActive) continue
            cellX[i] = floor(rock.position.x / cellSize).toInt()
            cellY[i] = floor(rock.position.y / cellSize).toInt()
            val key = key(cellX[i], cellY[i])
            val slot = slot(key)
            keys[slot] = key
            next[i] = heads[slot]
            heads[slot] = i
        }
    }

    private fun ensureCapacity(count: Int) {
        if (next.size >= count) return
        var capacity = next.size
        while (capacity < count) capacity *= 2
        next = IntArray(capacity)
        cellX = IntArray(capacity)
        cellY = IntArray(capacity)
        keys = LongArray(capacity * 2)
        heads = IntArray(capacity * 2) { -1 }
    }

    private fun key(x: Int, y: Int) = (x.toLong() shl 32) or (y.toLong() and 0xffffffffL)

    private fun slot(key: Long): Int {
        val mask = heads.size - 1
        val mixed = (key xor (key ushr 32)) * -7046029254386353131L
        var slot = (mixed ushr 32).toInt() and mask
        while (heads[slot] >= 0 && keys[slot] != key) slot = (slot + 1) and mask
        return slot
    }

    private fun resolvePair(a: Asteroid, b: Asteroid) {
        val dx = b.position.x - a.position.x
        val dy = b.position.y - a.position.y
        val radius = a.radius + b.radius
        val distanceSquared = dx * dx + dy * dy
        if (distanceSquared > radius * radius || radius <= 0f) return
        val distance = sqrt(distanceSquared)
        val relativeX = b.velocity.x - a.velocity.x
        val relativeY = b.velocity.y - a.velocity.y
        var nx: Float
        var ny: Float
        if (distance > .0001f) {
            nx = dx / distance
            ny = dy / distance
        } else {
            // Coincident spawns have no geometric normal. Oppose relative
            // motion, or choose a stable axis for a stationary pair.
            val relativeSpeed = sqrt(relativeX * relativeX + relativeY * relativeY)
            nx = if (relativeSpeed > .0001f) -relativeX / relativeSpeed else 1f
            ny = if (relativeSpeed > .0001f) -relativeY / relativeSpeed else 0f
        }
        val inverseA = 1f / AsteroidImpact.mass(a)
        val inverseB = 1f / AsteroidImpact.mass(b)
        val inverseSum = inverseA + inverseB
        // Positional correction is separate from velocity: overlapping spawns
        // separate without launching each other or gaining kinetic energy.
        val correction = max(radius - distance - POSITION_SLOP, 0f) * .8f / inverseSum
        a.position.add(-nx * correction * inverseA, -ny * correction * inverseA)
        b.position.add(nx * correction * inverseB, ny * correction * inverseB)

        val normalSpeed = relativeX * nx + relativeY * ny
        if (normalSpeed >= 0f) return
        val restitution = if (normalSpeed < -RESTING_SPEED) RESTITUTION else 0f
        val normalImpulse = -(1f + restitution) * normalSpeed / inverseSum
        a.velocity.add(-nx * normalImpulse * inverseA, -ny * normalImpulse * inverseA)
        b.velocity.add(nx * normalImpulse * inverseB, ny * normalImpulse * inverseB)

        // Disc inertias approximate the rough 3D rocks in this top-down world.
        // Coulomb friction at the contact trades tangential motion with spin;
        // it cannot add energy or apply an unbounded sideways impulse.
        val ra = max(a.radius, .01f)
        val rb = max(b.radius, .01f)
        val inverseInertiaA = 2f * inverseA / (ra * ra)
        val inverseInertiaB = 2f * inverseB / (rb * rb)
        val tx = -ny
        val ty = nx
        val slip = relativeX * tx + relativeY * ty - a.rotationSpeed * ra - b.rotationSpeed * rb
        val tangentMass = inverseSum + ra * ra * inverseInertiaA + rb * rb * inverseInertiaB
        val frictionLimit = FRICTION * normalImpulse
        val tangentImpulse = (-slip / tangentMass).coerceIn(-frictionLimit, frictionLimit)
        a.velocity.add(-tx * tangentImpulse * inverseA, -ty * tangentImpulse * inverseA)
        b.velocity.add(tx * tangentImpulse * inverseB, ty * tangentImpulse * inverseB)
        a.rotationSpeed -= ra * tangentImpulse * inverseInertiaA
        b.rotationSpeed -= rb * tangentImpulse * inverseInertiaB
    }

    /** 120 Hz at ordinary speeds; shorten further for fast, small rocks after weapon impacts. */
    fun stepDuration(asteroids: List<Asteroid>, remaining: Float, stepsLeft: Int): Float {
        var smallestRadius = Float.MAX_VALUE
        var fastestSpeed = 0f
        for (rock in asteroids) if (rock.isActive) {
            smallestRadius = min(smallestRadius, max(rock.radius, 1f))
            fastestSpeed = max(fastestSpeed, rock.velocity.length())
        }
        val travelStep = if (fastestSpeed > 0f) .35f * smallestRadius / fastestSpeed else remaining
        // The game thread caps a frame at 33 ms. A 32-step budget also bounds
        // work if extreme Lab/debug values request much faster motion.
        return min(remaining, max(remaining / stepsLeft, min(1f / 120f, travelStep)))
    }
}
