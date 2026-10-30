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
class AsteroidRamIntegrationTest {
    @Test fun `real game ram destroys the asteroid once without spawning live copies`() {
        for (protected in listOf(false,true)) {
            val context=ApplicationProvider.getApplicationContext<Context>()
            PersistenceManager(context).resetAllProgress()
            val view=GameSurfaceView(context,"ship_blue","pilot_medic") {_,_->}
            view.surfaceChanged(view.holder,0,1080,2400);view.state.phase=GamePhase.PLAYING
            val ship=ReflectionHelpers.getField<Ship>(view,"ship")
            val effects=ReflectionHelpers.getField<VisualEffectManager>(view,"visualEffects")
            ship.velocity.zero();ship.moveDirection.zero()
            if(protected) ship.makeInvulnerable(2f)
            val shield=ship.currentShield
            val rock=EntityPools.asteroids.obtain().apply {
                initialize(ship.position.x+25f,ship.position.y,AsteroidSize.LARGE,AsteroidType.ROCK)
                velocity.set(-80f,0f)
            }
            view.update(0f)
            assertFalse(rock.isActive)
            assertEquals(0,EntityPools.asteroids.activeCount())
            assertEquals(1,view.state.telemetryAsteroidsDestroyed)
            assertEquals(10,effects.rockDebris.fragments.count {it.active})
            assertEquals(if(protected) shield else shield-rock.getContactDamage(),ship.currentShield,.001f)
            view.update(0f)
            assertEquals(1,view.state.telemetryAsteroidsDestroyed)
            view.pause();EntityPools.resetAll()
        }
    }
}
