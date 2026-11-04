package com.astroloop.game.owner

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.astroloop.game.data.PersistenceManager
import com.astroloop.game.hangar.HangarState
import org.junit.Assert.*
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.KeyPairGenerator
import java.security.Signature

@RunWith(RobolectricTestRunner::class)
class OwnerAccessTest {
    private fun flags(allowed: Boolean, enabled: Boolean) {
        for ((name, value) in listOf("permitted" to allowed, "unlimited" to enabled)) {
            OwnerAccess::class.java.getDeclaredField(name).apply { isAccessible = true }.setBoolean(null, value)
        }
    }
    @After fun cleanup() { OwnerMenu.dismiss(); org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle(); flags(false, false) }
    @Test fun `signed grants are bound to one installation and one issuer`() {
        val key = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val id = "a".repeat(64)
        val grant = Base64.encodeToString(Signature.getInstance("SHA256withECDSA").run {
            initSign(key.private); update(OwnerGrant.payload(id)); sign()
        }, Base64.NO_WRAP)
        assertTrue(OwnerGrant.verify(id, grant, key.public))
        assertFalse(OwnerGrant.verify("b".repeat(64), grant, key.public))
        assertFalse(OwnerGrant.verify(id, grant))
        assertFalse(OwnerGrant.verify(id, grant.dropLast(4)+"AAAA", key.public))
        assertFalse(OwnerGrant.verify(id, "not an activation"))
        assertFalse(OwnerGrant.verify("short", grant, key.public))
    }
    @Test fun `persisted enabled flag alone cannot enable money`() {
        flags(false, true)
        val c = ApplicationProvider.getApplicationContext<Context>()
        assertFalse(OwnerAccess.moneyEnabled)
        assertFalse(OwnerAccess.setMoney(c, true))
        c.getSharedPreferences("owner_access_v1", Context.MODE_PRIVATE).edit()
            .putBoolean("unlimited", true).putString("grant", "forged").commit()
        OwnerAccess.load(c)
        assertFalse(OwnerAccess.authorized)
        assertFalse(OwnerAccess.moneyEnabled)
        assertFalse(OwnerAccess.activate(c, "forged"))
    }
    @Test fun `unlimited purchases preserve wallet and restore ordinary money`() {
        val c = ApplicationProvider.getApplicationContext<Context>()
        val p = PersistenceManager(c)
        flags(false, false); p.setYen(370)
        flags(true, true)
        repeat(20) { p.setYen(p.getYen()-100000); p.addYen(1000) }
        assertEquals(OwnerAccess.WALLET, p.getYen())
        assertTrue(OwnerAccess.setMoney(c, false))
        assertEquals(370, p.getYen())
        p.setYen(p.getYen()-100); assertEquals(270, p.getYen())
    }
    @Test fun `ordinary earnings saturate safely instead of overflowing`() {
        flags(false, false)
        val p = PersistenceManager(ApplicationProvider.getApplicationContext<Context>())
        p.setYen(Int.MAX_VALUE-10); p.addYen(100)
        assertEquals(Int.MAX_VALUE, p.getYen())
    }
    @Test fun `unauthorized callers cannot open the command menu`() {
        flags(false, false)
        val activity = org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup().get()
        OwnerMenu.showAuthenticated(activity)
        assertNull(org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog())
    }
    @Test fun `owner command dialog rejects unknown commands and toggles money`() {
        flags(true, false)
        val activity = org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup().get()
        OwnerMenu.showAuthenticated(activity)
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        val dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog()
        fun input(view: android.view.View): android.widget.EditText? {
            if (view is android.widget.EditText) return view
            if (view is android.view.ViewGroup) for (i in 0 until view.childCount) input(view.getChildAt(i))?.let { return it }
            return null
        }
        val field = input(dialog.window!!.decorView)!!
        field.setText("/money anything")
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        assertFalse(OwnerAccess.moneyEnabled); assertTrue(dialog.isShowing)
        field.setText("/money unlimited")
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick()
        assertTrue(OwnerAccess.moneyEnabled); assertFalse(dialog.isShowing)
        OwnerMenu.dismiss()
    }

}
