package com.astroloop.game.update

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpdateReleaseTest {
    private val repository="devil01529/Astroloop"
    private fun manifest()=JSONObject().put("schemaVersion",1).put("versionCode",10)
        .put("versionName","1.5-s24.5").put("apkUrl","https://github.com/$repository/releases/download/v1.5-s24.5/astro-loop-s24.apk")
        .put("sha256","a".repeat(64)).put("apkBytes",55_000_000)
    @Test fun `accept correct versioned release`() {
        val release=UpdateRelease.parse(manifest().toString(),repository)
        assertEquals(10L,release.versionCode);assertEquals(55_000_000L,release.apkBytes)
        assertTrue(release.versionCode>9)
    }
    @Test fun `reject foreign repositories insecure URLs and invalid checksums`() {
        for(url in listOf("http://github.com/$repository/releases/download/v1/astro-loop-s24.apk",
            "https://evil.example/$repository/releases/download/v1/astro-loop-s24.apk",
            "https://github.com/attacker/game/releases/download/v1/astro-loop-s24.apk",
            "https://github.com/$repository/releases/download/../astro-loop-s24.apk")) {
            assertThrows(IllegalArgumentException::class.java) {UpdateRelease.parse(manifest().put("apkUrl",url).toString(),repository)}
        }
        assertThrows(IllegalArgumentException::class.java) {UpdateRelease.parse(manifest().put("sha256","invalid").toString(),repository)}
    }
    @Test fun `reject oversized or invalid manifest`() {
        for(size in listOf(0L,-1L,UpdateRelease.MAX_APK_BYTES+1)) {
            assertThrows(IllegalArgumentException::class.java) {UpdateRelease.parse(manifest().put("apkBytes",size).toString(),repository)}
        }
        assertThrows(IllegalArgumentException::class.java) {UpdateRelease.parse(manifest().put("schemaVersion",2).toString(),repository)}
        assertThrows(IllegalArgumentException::class.java) {UpdateRelease.parse(manifest().put("versionCode",0).toString(),repository)}
    }
}
