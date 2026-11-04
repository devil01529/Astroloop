package com.astroloop.game.owner

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.spec.ECGenParameterSpec

object OwnerAccess {
    const val WALLET = 1_000_000_000
    private const val ALIAS = "astro_loop_owner_installation_v1"
    private const val PREFS = "owner_access_v1"
    @Volatile private var permitted = false
    @Volatile private var unlimited = false
    val authorized: Boolean get() = permitted
    val moneyEnabled: Boolean get() = permitted && unlimited

    /** The private installation key is non-exportable and is not included in backups. */
    fun installationCode(create: Boolean = true): String? = try {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!store.containsAlias(ALIAS) && create) {
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
                initialize(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256).build())
            }.generateKeyPair()
        }
        store.getCertificate(ALIAS)?.publicKey?.encoded?.let { bytes ->
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        }
    } catch (_: Exception) { null }

    fun load(context: Context) {
        permitted = false; unlimited = false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val grant = prefs.getString("grant", null) ?: return
        val id = installationCode(false) ?: return
        permitted = OwnerGrant.verify(id, grant)
        unlimited = permitted && prefs.getBoolean("unlimited", false)
    }

    fun activate(context: Context, grant: String): Boolean {
        val id = installationCode(false) ?: return false
        if (!OwnerGrant.verify(id, grant)) return false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("grant", grant).apply()
        load(context)
        return permitted
    }

    /** Called by the credential-confirmed owner dialog; never accepts an arbitrary command string. */
    fun setMoney(context: Context, enabled: Boolean): Boolean {
        if (!permitted) return false
        unlimited = enabled
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("unlimited", enabled).apply()
        return true
    }
}
