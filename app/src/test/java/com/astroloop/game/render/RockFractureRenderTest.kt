package com.astroloop.game.render

import android.content.Context
import android.graphics.*
import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.core.GameState
import com.astroloop.game.core.GamePhase
import com.astroloop.game.core.GameSurfaceView
import com.astroloop.game.core.ScreenLayout
import com.astroloop.game.data.PersistenceManager
import com.astroloop.game.entity.*
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
import org.robolectric.util.ReflectionHelpers
import java.io.File
import kotlin.math.sqrt

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RockFractureRenderTest {
    @Before fun setup() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        PilotSettings.save(context,PilotOptions());InputModeState.reset();RealisticArt.initialize(context)
    }
    @Test fun `boulders have irregular radial profiles and shards have independent fracture geometry`() {
        for(mesh in TumblingAsteroidRenderer.detailedMeshes) {
            val radii=mesh.positions.indices.step(3).map {p -> sqrt(mesh.positions[p]*mesh.positions[p]+mesh.positions[p+1]*mesh.positions[p+1]+mesh.positions[p+2]*mesh.positions[p+2]) }
            assertTrue("not a smooth spherical shell",radii.max()/radii.min()>1.4f)
            assertTrue(radii.max()<=.9401f)
        }
        assertTrue(TumblingAsteroidRenderer.shardMeshes.all {it.fragment && it.faceCount==8})
        assertTrue(TumblingAsteroidRenderer.detailedMeshes.all {!it.fragment && it.faceCount==320})
    }
    @Test fun `fracture pieces fade without losing their texture or leaking paint state`() {
        val f=RockFragment().apply {x=100f;y=100f;radius=50f;rollX=.4f;rollY=.5f;lifetime=1f}
        val b=Bitmap.createBitmap(200,200,Bitmap.Config.ARGB_8888);val c=Canvas(b)
        fun opacity(): Long { val pixels=IntArray(40000);b.getPixels(pixels,0,200,0,0,200,200);return pixels.sumOf {Color.alpha(it).toLong()} }
        TumblingAsteroidRenderer.drawFragment(c,f);val opaque=opacity();assertTrue(opaque>100000)
        b.eraseColor(Color.TRANSPARENT);f.age=.8f;TumblingAsteroidRenderer.drawFragment(c,f)
        val faded=opacity();assertTrue(faded>0 && faded<opaque*.6f)
        b.eraseColor(Color.TRANSPARENT)
        RealisticArt.drawAsteroid(c,Asteroid().apply {position.set(100f,100f);radius=50f})
        assertEquals(255,Color.alpha(b.getPixel(100,100)))
        b.recycle()
    }
    @Test fun `native preview shows asymmetric rocks distinct shards larger hull and centre pad`() {
        val b=Bitmap.createBitmap(1080,2280,Bitmap.Config.ARGB_8888);val c=Canvas(b)
        c.drawColor(0xFF080F1C.toInt())
        val text=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=0xFFD6EAF3.toInt();textSize=34f}
        c.drawText("S24 / CENTRE FLIGHT + ROCK FRACTURE",40f,65f,text)
        text.textSize=24f;c.drawText("12 asymmetric rock shapes · multi-axis rolling",40f,115f,text)
        for(row in 0..3) for(col in 0..2) {
            val a=Asteroid().apply {
                position.set(180f+col*360,265f+row*235);radius=115f
                tumbleVariant=row*3+col;tumbleX=row*.53f;tumbleY=col*.37f;rotation=.2f*row
                type=AsteroidType.ROCK
            }
            RealisticArt.drawAsteroid(c,a)
        }
        c.drawText("Broken slabs and splinters · harmless, fading debris",40f,1155f,text)
        for(i in 0..5) {
            val f=RockFragment().apply {
                x=100f+i*176;y=1280f;radius=66f;variant=i;rollX=.4f+i*.3f;rollY=.3f
                lifetime=1f;type=AsteroidType.ROCK
            }
            TumblingAsteroidRenderer.drawFragment(c,f)
        }
        val ship=Ship().apply {position.set(540f,1540f);radius=25f;rotation=-1.57f;velocity.set(0f,-200f)}
        VectorRenderer(ShapeRenderer()).renderShip(c,ship,GameState().apply {survivalTime=3f})
        text.textAlign=Paint.Align.CENTER
        c.drawText("Ship at gameplay size · 40% larger than the previous APK",540f,1660f,text)
        val pad=FixedStickLayout.forScreen(ScreenLayout.compute(1080f,2280f))
        FixedStickRenderer().render(c,pad,pad.x,pad.y,false)
        c.drawText("Either thumb · controlled handover",540f,1810f,text)
        val file=File("build/reports/s24-centre-fracture-preview.png");file.parentFile!!.mkdirs()
        file.outputStream().use {assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,it))};b.recycle()
    }
    @Test fun `actual game renderer draws centre controls rocks and debris at phone dimensions`() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        PersistenceManager(context).resetAllProgress()
        val view=GameSurfaceView(context,"ship_blue","pilot_medic") {_,_->}
        view.surfaceChanged(view.holder,0,1080,2400);view.state.phase=GamePhase.PLAYING
        view.state.survivalTime=20f
        val ship=ReflectionHelpers.getField<Ship>(view,"ship")
        val offsets=arrayOf(-240f to -450f,220f to -300f,-190f to -80f,250f to 180f,-240f to 420f,80f to 480f)
        for((i,offset) in offsets.withIndex()) EntityPools.asteroids.obtain().apply {
            initialize(ship.position.x+offset.first,ship.position.y+offset.second,AsteroidSize.LARGE,AsteroidType.ROCK)
            tumbleVariant=i;tumbleX=i*.4f;tumbleY=i*.3f
        }
        view.update(0f)
        val effects=ReflectionHelpers.getField<VisualEffectManager>(view,"visualEffects")
        effects.addAsteroidBreak(Asteroid().apply {position.set(ship.position.x+180f,ship.position.y+380f);radius=45f})
        effects.rockDebris.update(.3f)
        val b=Bitmap.createBitmap(1080,2400,Bitmap.Config.ARGB_8888)
        view.render(Canvas(b))
        val file=File("build/reports/s24-centre-gameplay-preview.png");file.parentFile!!.mkdirs()
        file.outputStream().use {assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,it))}
        b.recycle();view.pause();EntityPools.resetAll()
    }
}
