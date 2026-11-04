package com.astroloop.game.system

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.core.GamePhase
import com.astroloop.game.core.GameSurfaceView
import com.astroloop.game.data.PersistenceManager
import com.astroloop.game.entity.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class AsteroidContactIntegrationTest {
    private fun scene(phase: GamePhase = GamePhase.PLAYING): Pair<GameSurfaceView, List<Asteroid>> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PersistenceManager(context).resetAllProgress()
        val view = GameSurfaceView(context, "ship_blue", "pilot_medic") { _, _ -> }
        view.surfaceChanged(view.holder, 0, 1080, 2400)
        view.state.phase = phase
        ReflectionHelpers.getField<WeaponSystem>(view, "weaponSystem").reset()
        ReflectionHelpers.setField(ReflectionHelpers.getField<SpawnSystem>(view, "spawnSystem"), "spawnTimer", 100f)
        ReflectionHelpers.setField(view, "deathPlayOutTimer", 2f)
        val ship = ReflectionHelpers.getField<Ship>(view, "ship")
        ship.velocity.zero(); ship.moveDirection.zero()
        val rocks = listOf(-80f, 0f).mapIndexed { index, offset ->
            EntityPools.asteroids.obtain().apply {
                initialize(ship.position.x + offset, ship.position.y - 300f, AsteroidSize.LARGE)
                radius = 40f; rotationSpeed = 0f
                velocity.set(if (index == 0) 100f else -100f, 0f)
            }
        }
        return view to rocks
    }

    @Test fun `real game and post-death updates both collide rocks without kills or shards`() {
        for (phase in listOf(GamePhase.PLAYING, GamePhase.DEATH_PLAY_OUT)) {
            val (view, rocks) = scene(phase)
            try {
                val health = rocks.map { it.health }
                view.update(1f / 60f)
                assertTrue(rocks[0].velocity.x < 0 && rocks[1].velocity.x > 0)
                assertTrue(rocks.all { it.isActive })
                assertEquals(health, rocks.map { it.health })
                assertEquals(0, view.state.telemetryAsteroidsDestroyed)
                val effects = ReflectionHelpers.getField<VisualEffectManager>(view, "visualEffects")
                assertTrue(effects.rockDebris.fragments.none { it.active })
            } finally {
                view.pause(); EntityPools.resetAll()
            }
        }
    }

    @Test fun `game pause freezes collisions and resuming restores them`() {
        val (view, rocks) = scene()
        try {
            val positions = rocks.map { it.position.x }
            view.state.isPaused = true
            repeat(4) { view.update(1f / 30f) }
            assertEquals(positions, rocks.map { it.position.x })
            assertEquals(100f, rocks[0].velocity.x, 0f)
            view.state.isPaused = false
            view.update(1f / 60f)
            assertTrue(rocks[0].velocity.x < 0 && rocks[1].velocity.x > 0)
        } finally {
            view.pause(); EntityPools.resetAll()
        }
    }
}
