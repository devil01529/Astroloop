package com.astroloop.game.render

import android.graphics.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SpaceReadabilityTest {
    @Test fun `large camera jumps wrap every star back into view`() {
        assertEquals(25f,wrapStar(3025f,100f),0f)
        assertEquals(75f,wrapStar(-3025f,100f),0f)
        assertEquals(0f,wrapStar(100f,100f),0f)
        assertEquals(0f,wrapStar(Float.NaN,100f),0f)
    }
    @Test fun `critical warning leaves aiming area transparent and stays off at healthy hull`() {
        val bitmap=Bitmap.createBitmap(300,600,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap);val warning=HullWarning()
        warning.render(canvas,300f,600f,100f,100f,1f)
        assertEquals(0,bitmap.getPixel(100,1))
        warning.render(canvas,300f,600f,20f,100f,1f)
        assertTrue(Color.alpha(bitmap.getPixel(100,1))>0)
        assertEquals(0,Color.alpha(bitmap.getPixel(150,300)))
        bitmap.recycle()
    }
    @Test fun `zero sized surface can recover on rotation`() {
        val stars=StarfieldRenderer();stars.initialize(0f,0f)
        val bitmap=Bitmap.createBitmap(300,600,Bitmap.Config.ARGB_8888)
        stars.render(Canvas(bitmap));stars.initialize(300f,600f);stars.render(Canvas(bitmap))
        bitmap.recycle()
    }
    @Test fun `export scene with actual game art and new backdrop`() {
        val bitmap=Bitmap.createBitmap(1080,1400,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap);canvas.drawColor(0xFF050910.toInt())
        val stars=StarfieldRenderer();stars.initialize(1080f,1400f);stars.render(canvas)
        RealisticArt.initialize(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        RealisticArt.drawShip(canvas,540f,760f,-1.57f,105f,Color.CYAN,Color.BLUE,"pulse_cannon",1f)
        for(i in 0..8) {
            val a=com.astroloop.game.entity.Asteroid().apply {
                radius=40f+(i%3)*16f;position.set(100f+(i%3)*410f,210f+(i/3)*380f)
                rotation=i*.7f;type=com.astroloop.game.entity.AsteroidType.values()[i%6]
            };RealisticArt.drawAsteroid(canvas,a)
        }
        for(i in 0..4)RealisticArt.drawRound(canvas,540f,580f-i*70,-1.57f,5f,23f,Color.CYAN,1f)
        HullWarning().render(canvas,1080f,1400f,20f,100f,1f)
        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.WHITE;textSize=34f }
        canvas.drawText("ASTRO LOOP S24 / FLIGHT POLISH",45f,68f,paint)
        paint.textSize=24f;canvas.drawText("Layered backdrop · steady low-hull warning",45f,1325f,paint)
        val output=File("build/reports/flight-polish-preview.png");output.parentFile!!.mkdirs()
        output.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) };bitmap.recycle()
    }
}
