package com.astroloop.game.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.entity.Asteroid
import com.astroloop.game.entity.AsteroidType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RealisticArtTest {
    @Before fun loadArt() {
        RealisticArt.initialize(ApplicationProvider.getApplicationContext<Context>())
        assertTrue(RealisticArt.isLoaded())
    }

    @Test fun `zero opacity leaves the canvas transparent`() {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        assertTrue(RealisticArt.drawShip(canvas, 100f, 100f, 0f, 60f, Color.CYAN, Color.BLUE, "pulse_cannon", 0f))
        assertEquals(0, bitmap.getPixel(100, 100))
        bitmap.recycle()
    }

    @Test fun `textured asteroid respects collision bounds and game state`() {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val asteroid = Asteroid().apply {
            position.set(100f,100f); radius=40f; health=17f; rotation=0f
            shapePoints=floatArrayOf(40f,0f,0f,40f,-40f,0f,0f,-40f)
        }
        val original = asteroid.shapePoints.copyOf()
        for (type in AsteroidType.values()) {
            bitmap.eraseColor(Color.TRANSPARENT); asteroid.type=type
            assertTrue(RealisticArt.drawAsteroid(canvas, asteroid))
            assertTrue("Rock center must be visibly solid", Color.alpha(bitmap.getPixel(100,100)) >= 240)
            assertEquals(0, Color.alpha(bitmap.getPixel(135,135)))
            assertArrayEquals(original, asteroid.shapePoints, 0f)
            assertEquals(17f, asteroid.health, 0f)
            assertEquals(40f, asteroid.radius, 0f)
        }
        bitmap.recycle()
    }

    @Test fun `all hull and gun families render and restore canvas state`() {
        val bitmap=Bitmap.createBitmap(400,400,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap)
        for (weapon in listOf("pulse_cannon","flak_cannon","railgun","homing_missiles")) {
            val count=canvas.saveCount
            assertTrue(RealisticArt.drawShip(canvas,200f,200f,1.2f,100f,Color.CYAN,Color.BLUE,weapon,1f))
            assertEquals(count,canvas.saveCount)
        }
        assertTrue(Color.alpha(bitmap.getPixel(200,200)) > 0)
        bitmap.recycle()
    }

    @Test fun `export actual Android renderer preview`() {
        val bitmap=Bitmap.createBitmap(1080,1000,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap)
        canvas.drawColor(0xFF080D17.toInt())
        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=0xFFDEE8F5.toInt(); textSize=32f }
        canvas.drawText("ASTRO LOOP S24 / REALISTIC ART",40f,55f,paint)
        val shape=ShapeRenderer()
        for ((i,weapon) in listOf("pulse_cannon","flak_cannon","railgun").withIndex()) {
            ShipRenderer.drawShip(canvas,shape,180f+i*360f,245f,0f,132f,Color.CYAN,Color.BLUE,weapon)
            paint.textSize=23f; canvas.drawText(weapon.replace('_',' '),75f+i*360f,435f,paint)
        }
        for ((i,type) in AsteroidType.values().withIndex()) {
            val asteroid=Asteroid().apply {
                position.set(180f+(i%3)*360f,570f+(i/3)*270f);radius=85f;this.type=type
                shapePoints=floatArrayOf(86f,0f,68f,55f,15f,87f,-55f,68f,-87f,10f,-68f,-57f,-10f,-86f,64f,-61f)
            }
            RealisticArt.drawAsteroid(canvas,asteroid)
            paint.textSize=21f;canvas.drawText(type.name,125f+(i%3)*360f,690f+(i/3)*270f,paint)
        }
        val output=File("build/reports/realistic-art-preview.png")
        output.parentFile!!.mkdirs()
        output.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }
        bitmap.recycle()
    }
}
