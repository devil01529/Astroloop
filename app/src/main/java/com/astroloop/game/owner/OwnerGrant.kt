package com.astroloop.game.owner

import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import android.util.Base64

/** Only the verification key ships. The issuer key must stay outside source/APKs. */
object OwnerGrant {
    private const val ISSUER = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEz3GCIhLBoTBuRnumreLO6FeiD8ks7Y3Xj5ISDf2O3jmfxLaRKeiWRP4grzOyWcOpT/GmRdwHOq7obwzxKT5cKw=="
    fun payload(installation: String) = "astro-loop-s24:owner:v1:$installation".toByteArray(Charsets.UTF_8)
    fun issuer(): PublicKey = KeyFactory.getInstance("EC").generatePublic(
        X509EncodedKeySpec(Base64.decode(ISSUER, Base64.NO_WRAP)))
    fun verify(installation: String, grant: String, key: PublicKey = issuer()): Boolean = try {
        installation.matches(Regex("[0-9a-f]{64}")) && grant.length in 80..160 &&
            Signature.getInstance("SHA256withECDSA").run {
                initVerify(key); update(payload(installation))
                verify(Base64.decode(grant, Base64.NO_WRAP))
            }
    } catch (_: Exception) { false }
}
