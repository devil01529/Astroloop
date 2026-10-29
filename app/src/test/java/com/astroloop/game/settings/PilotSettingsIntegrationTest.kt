package com.astroloop.game.settings

import android.app.AlertDialog
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import com.astroloop.game.MainActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
class PilotSettingsIntegrationTest {
    private fun views(v:View):List<View> = listOf(v) + if(v is ViewGroup) (0 until v.childCount).flatMap {views(v.getChildAt(it))} else emptyList()
    @Test fun `visible game menu opens real settings and survives backgrounding`() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        val activity=controller.get()
        val menu=views(activity.window.decorView).single {it.contentDescription=="Pause and pilot settings"}
        assertTrue(menu.performClick())
        val dialog=ShadowDialog.getLatestDialog() as AlertDialog
        assertTrue(dialog.isShowing)
        controller.pause().stop().start().resume()
        assertTrue(dialog.isShowing)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(dialog.isShowing)
        controller.pause().stop().destroy()
    }
}
