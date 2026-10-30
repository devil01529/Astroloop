package com.astroloop.game.settings

import android.app.Activity
import android.content.Context
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.Switch
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PilotSettingsTest {
    private val ctx get()=ApplicationProvider.getApplicationContext<Context>()
    @Test fun `old handedness preferences migrate to centre without losing other settings`() {
        ctx.getSharedPreferences("s24_pilot_options",Context.MODE_PRIVATE).edit().clear()
            .putBoolean("right_joystick",true).putInt("fps",60).putFloat("sensitivity",1.3f).commit()
        val settings=PilotSettings.load(ctx)
        assertEquals(JoystickPlacement.CENTER,settings.joystickPlacement)
        assertEquals(60,settings.fps);assertEquals(1.3f,settings.sensitivity,.001f)
    }
    @Test fun `handedness and 3D preference survive reload and are applied by real controls`() {
        PilotSettings.save(ctx,PilotOptions())
        val controller=Robolectric.buildActivity(Activity::class.java).setup()
        val dialog=FlightSettingsDialog.create(controller.get(),false){PilotSettings.save(ctx,it)}
        dialog.show()
        val switches=views(dialog.window!!.decorView).filterIsInstance<Switch>()
        views(dialog.window!!.decorView).filterIsInstance<RadioButton>().single {it.text=="Right"}.performClick()
        switches.single {it.text=="3D tumbling asteroids"}.isChecked=false
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        val saved=PilotSettings.load(ctx)
        assertEquals(JoystickPlacement.RIGHT,saved.joystickPlacement);assertFalse(saved.asteroid3d)
        controller.pause().stop().destroy()
    }
    @Test fun `options persist and malformed values are sanitized`() {
        PilotSettings.save(ctx,PilotOptions(60,1.4f,false,false))
        assertEquals(PilotOptions(60,1.4f,false,false),PilotSettings.load(ctx))
        PilotSettings.save(ctx,PilotOptions(999,Float.NaN))
        assertEquals(PilotOptions(),PilotSettings.load(ctx))
    }
    private fun views(v:View):List<View> = listOf(v) + if(v is ViewGroup) (0 until v.childCount).flatMap { views(v.getChildAt(it)) } else emptyList()
    @Test fun `dialog applies the actual chosen controls`() {
        PilotSettings.save(ctx,PilotOptions())
        val controller=Robolectric.buildActivity(Activity::class.java).setup()
        var selected:PilotOptions?=null
        val dialog=FlightSettingsDialog.create(controller.get(),true){selected=it};dialog.show()
        val all=views(dialog.window!!.decorView)
        all.filterIsInstance<RadioButton>().first {it.text.startsWith("Balanced")}.performClick()
        all.filterIsInstance<SeekBar>().single().progress=9
        all.filterIsInstance<Switch>().forEach {it.isChecked=false}
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertNotNull(selected)
        assertEquals(60,selected!!.fps)
        assertEquals(1.6f,selected!!.sensitivity,.0001f)
        assertFalse(selected!!.detailedBackground)
        assertFalse(selected!!.backgroundMotion)
        controller.pause().stop().destroy()
    }
    @Test fun `cancel does not overwrite saved preferences`() {
        val original=PilotOptions(60,1.4f,false,true);PilotSettings.save(ctx,original)
        val controller=Robolectric.buildActivity(Activity::class.java).setup()
        var applied=false
        val dialog=FlightSettingsDialog.create(controller.get(),false){applied=true};dialog.show()
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertFalse(applied);assertEquals(original,PilotSettings.load(ctx))
        controller.pause().stop().destroy()
    }
    @Test fun `updates keep the pause panel open without applying settings`() {
        val controller=Robolectric.buildActivity(Activity::class.java).setup()
        var checked=false
        var applied=false
        val dialog=FlightSettingsDialog.create(controller.get(),true,onCheckUpdates={checked=true}){applied=true}
        dialog.show()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).performClick()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertTrue(checked)
        assertTrue(dialog.isShowing)
        assertFalse(applied)
        dialog.dismiss()
        controller.pause().stop().destroy()
    }

}
