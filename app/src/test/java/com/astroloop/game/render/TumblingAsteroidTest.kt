package com.astroloop.game.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.core.GameState
import com.astroloop.game.core.ScreenLayout
import com.astroloop.game.entity.Asteroid
import com.astroloop.game.entity.AsteroidSize
import com.astroloop.game.entity.AsteroidType
import com.astroloop.game.entity.Ship
import com.astroloop.game.input.FixedStickLayout
import com.astroloop.game.input.InputModeState
import com.astroloop.game.settings.PilotOptions
import com.astroloop.game.settings.PilotSettings
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.sqrt

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TumblingAsteroidTest {
    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PilotSettings.save(context, PilotOptions()); InputModeState.reset(); RealisticArt.initialize(context)
    }
    private fun rock() = Asteroid().apply { position.set(100f, 100f); radius = 50f; rotation = 0f }
    @Test fun `multi axis tumble advances with simulation time and resets for pooled rocks`() {
        val a = rock(); val b = rock()
        repeat(60) { a.update(1f / 60) }; b.update(1f)
        assertEquals(a.tumbleX, b.tumbleX, .0001f); assertEquals(a.tumbleY, b.tumbleY, .0001f)
        assertNotEquals(0f, a.tumbleX); assertNotEquals(0f, a.tumbleY)
        val paused = a.tumbleX; a.update(0f); assertEquals(paused, a.tumbleX, 0f)
        a.reset(); assertEquals(0f, a.tumbleX, 0f); assertEquals(0, a.tumbleVariant)
    }
    @Test fun `all mesh variants stay bounded even with perspective`() {
        for (mesh in TumblingAsteroidRenderer.detailedMeshes) {
            for (p in mesh.positions.indices step 3) {
                val length = sqrt(mesh.positions[p] * mesh.positions[p] + mesh.positions[p + 1] * mesh.positions[p + 1] + mesh.positions[p + 2] * mesh.positions[p + 2])
                assertTrue(length <= .9401f)
            }
        }
    }
    @Test fun `rolling changes visible faces without flat screen rotation or gameplay mutation`() {
        val a = rock(); a.health = 17f
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888); val canvas = Canvas(bitmap)
        RealisticArt.drawAsteroid(canvas, a)
        val first = IntArray(40000); bitmap.getPixels(first, 0, 200, 0, 0, 200, 200)
        a.tumbleX = 1.1f; a.tumbleY = .7f
        bitmap.eraseColor(Color.TRANSPARENT); RealisticArt.drawAsteroid(canvas, a)
        val second = IntArray(40000); bitmap.getPixels(second, 0, 200, 0, 0, 200, 200)
        assertTrue(first.indices.count { first[it] != second[it] } > 1000)
        assertTrue(Color.alpha(bitmap.getPixel(100, 100)) > 240)
        for (y in 0 until 200) for (x in 0 until 200) {
            if ((x - 100) * (x - 100) + (y - 100) * (y - 100) > 51 * 51) assertEquals(0, Color.alpha(bitmap.getPixel(x, y)))
        }
        assertEquals(0f, a.rotation, 0f); assertEquals(50f, a.radius, 0f); assertEquals(17f, a.health, 0f)
        bitmap.recycle()
    }
    @Test fun `native renderer preview shows rolling poses and fixed controls`() {
        val bitmap = Bitmap.createBitmap(960, 1800, Bitmap.Config.ARGB_8888); val canvas = Canvas(bitmap)
        canvas.drawColor(0xFF080F1C.toInt())
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD6EAF3.toInt(); textSize = 32f }
        canvas.drawText("S24 / FIXED FLIGHT + 3D TUMBLING", 40f, 65f, text)
        text.textSize = 22f; canvas.drawText("Same rock, three different roll poses", 40f, 115f, text)
        for (row in 0..2) for (col in 0..2) {
            val a = rock().apply {
                position.set(170f + col * 310f, 270f + row * 280f); radius = 100f
                tumbleX = col * .85f; tumbleY = col * .55f; tumbleVariant = row
                type = listOf(AsteroidType.ROCK, AsteroidType.METAL, AsteroidType.VOLATILE)[row]
            }
            RealisticArt.drawAsteroid(canvas, a)
        }
        val ship = Ship().apply { position.set(480f, 1160f); radius = 48f; velocity.set(180f, -80f); rotation = -.7f }
        val state = GameState().apply { survivalTime = 3f }
        val renderer = VectorRenderer(ShapeRenderer()); renderer.renderShip(canvas, ship, state)
        text.textSize = 24f; canvas.drawText("Larger hull · soft shield · rounded exhaust", 190f, 1290f, text)
        val pad = FixedStickLayout.forScreen(ScreenLayout.compute(960f, 1800f), false)
        FixedStickRenderer().render(canvas, pad, pad.x, pad.y, false)
        canvas.drawText("Fixed centre / idle", 350f, 1570f, text)
        val out = File("build/reports/s24-flight-3d-preview.png"); out.parentFile!!.mkdirs()
        out.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
    }
}
