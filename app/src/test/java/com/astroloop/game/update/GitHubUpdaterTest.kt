package com.astroloop.game.update

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.os.Looper
import android.provider.Settings
import com.astroloop.game.BuildConfig
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.util.ReflectionHelpers
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Exercises the real dialogs, downloader, package checks, and installer handoff.
 * Network responses and Android package parsing are controlled; no live APK is installed. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GitHubUpdaterTest {
    private val apk = "test APK payload".toByteArray()
    private val repository = BuildConfig.UPDATE_REPOSITORY
    private fun manifest(version: Long = BuildConfig.VERSION_CODE + 1L, digest: String? = null) = JSONObject()
        .put("schemaVersion", 1).put("versionCode", version).put("versionName", "next-s24")
        .put("apkUrl", "https://github.com/$repository/releases/download/v-next/astro-loop-s24.apk")
        .put("apkBytes", apk.size)
        .put("sha256", digest ?: MessageDigest.getInstance("SHA-256").digest(apk).joinToString("") { "%02x".format(it) })
        .toString().toByteArray()

    private open class Response(url: URL, private val bytes: ByteArray) : HttpURLConnection(url) {
        override fun connect() {}
        override fun disconnect() {}
        override fun usingProxy() = false
        override fun getResponseCode() = 200
        override fun getInputStream() = ByteArrayInputStream(bytes)
    }
    private fun awaitDialog(title: String): AlertDialog {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            val dialog = ShadowAlertDialog.getLatestAlertDialog()
            if (dialog != null && dialog.isShowing && shadowOf(dialog).title.toString() == title) return dialog
            Thread.sleep(5)
        }
        error("Timed out waiting for dialog: $title; latest: ${ShadowAlertDialog.getLatestAlertDialog()?.let { shadowOf(it).title }}")
    }
    private fun withUpdater(response: (URL) -> HttpURLConnection, action: (Activity, GitHubUpdater) -> Unit) {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = controller.get()
        File(activity.cacheDir, "updates").deleteRecursively()
        val updater = GitHubUpdater(activity, response)
        try { action(activity, updater) }
        finally { updater.close(); controller.pause().stop().destroy() }
    }
    private fun registerPackages(activity: Activity, candidateSigner: String = "trusted", candidatePackage: String = activity.packageName,
                                 candidateVersion: Long = BuildConfig.VERSION_CODE + 1L) {
        fun signing(value: String) = ReflectionHelpers.newInstance(SigningInfo::class.java).also {
            shadowOf(it).setSignatures(arrayOf(Signature(value.toByteArray())))
        }
        val pm = activity.packageManager
        val installed = pm.getPackageInfo(activity.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_PROVIDERS or PackageManager.GET_META_DATA)
        installed.signingInfo = signing("trusted")
        shadowOf(pm).installPackage(installed)
        val candidate = PackageInfo().apply {
            packageName = candidatePackage
            longVersionCode = candidateVersion
            signingInfo = signing(candidateSigner)
        }
        for (name in listOf("update.part", "astro-loop-s24.apk")) {
            shadowOf(pm).setPackageArchiveInfo(File(activity.cacheDir, "updates/$name").absolutePath, candidate)
        }
    }

    @Test fun `current release shows up to date without downloading`() {
        var requests = 0
        withUpdater({ requests++; Response(it, manifest(BuildConfig.VERSION_CODE.toLong())) }) { activity, updater ->
            updater.check()
            awaitDialog("You're up to date")
            assertEquals(1, requests)
            assertNull(shadowOf(activity).nextStartedActivity)
        }
    }

    @Test fun `verified update requests permission then grants installer access to cached APK`() {
        withUpdater({ Response(it, if (it.path.endsWith("update.json")) manifest() else apk) }) { activity, updater ->
            registerPackages(activity)
            shadowOf(activity.packageManager).setCanRequestPackageInstalls(false)
            updater.check()
            awaitDialog("Update available").getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            val verified = awaitDialog("Update verified")
            assertTrue(shadowOf(verified).message.toString().contains("finish your current run first"))
            verified.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            val permission = shadowOf(activity).nextStartedActivity
            assertEquals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, permission.action)
            assertEquals("package:${activity.packageName}", permission.data.toString())
            shadowOf(activity.packageManager).setCanRequestPackageInstalls(true)
            // Resolve the actual manifest provider, including its restricted cache path.
            androidx.core.content.FileProvider.getUriForFile(activity, "${activity.packageName}.updates",
                File(activity.cacheDir, "updates/astro-loop-s24.apk"))
            updater.onResume()
            val install = shadowOf(activity).nextStartedActivity
            assertEquals(Intent.ACTION_VIEW, install.action)
            assertEquals("content", install.data!!.scheme)
            assertEquals("${activity.packageName}.updates", install.data!!.authority)
            assertEquals("application/vnd.android.package-archive", install.type)
            assertTrue(install.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            assertArrayEquals(apk, File(activity.cacheDir, "updates/astro-loop-s24.apk").readBytes())
        }
    }

    @Test fun `corrupted download never reaches installer`() {
        withUpdater({ Response(it, if (it.path.endsWith("update.json")) manifest(digest = "0".repeat(64)) else apk) }) { activity, updater ->
            registerPackages(activity)
            updater.check()
            awaitDialog("Update available").getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            awaitDialog("Update not installed")
            assertNull(shadowOf(activity).nextStartedActivity)
            assertFalse(File(activity.cacheDir, "updates/update.part").exists())
        }
    }

    @Test fun `foreign signing certificate is rejected even when checksum matches`() {
        withUpdater({ Response(it, if (it.path.endsWith("update.json")) manifest() else apk) }) { activity, updater ->
            registerPackages(activity, candidateSigner = "untrusted")
            updater.check()
            awaitDialog("Update available").getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            awaitDialog("Update not installed")
            assertNull(shadowOf(activity).nextStartedActivity)
        }
    }

    @Test fun `wrong application and mismatched version are rejected`() {
        for ((packageName, version) in listOf("other.application" to BuildConfig.VERSION_CODE + 1L,
            BuildConfig.APPLICATION_ID to BuildConfig.VERSION_CODE.toLong())) {
            withUpdater({ Response(it, if (it.path.endsWith("update.json")) manifest() else apk) }) { activity, updater ->
                registerPackages(activity, candidatePackage = packageName, candidateVersion = version)
                updater.check()
                awaitDialog("Update available").getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                awaitDialog("Update not installed")
                assertNull(shadowOf(activity).nextStartedActivity)
            }
        }
    }

    @Test fun `offline failure allows retry`() {
        var offline = true
        withUpdater({ if (offline) throw IOException("Offline") else Response(it, manifest(BuildConfig.VERSION_CODE.toLong())) }) { _, updater ->
            updater.check()
            awaitDialog("Could not check for updates").getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            offline = false
            updater.check()
            awaitDialog("You're up to date")
        }
    }

    @Test fun `cancelled check cannot replace a later result`() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        var requests = 0
        withUpdater({ url ->
            if (++requests == 1) object : Response(url, manifest()) {
                override fun getInputStream(): ByteArrayInputStream {
                    started.countDown()
                    // A network implementation can finish even after interruption.
                    var done = false
                    while (!done) try { done = release.await(5, TimeUnit.SECONDS) } catch (_: InterruptedException) {}
                    return super.getInputStream()
                }
            } else Response(url, manifest(BuildConfig.VERSION_CODE.toLong()))
        }) { _, updater ->
            try {
                updater.check()
                assertTrue(started.await(5, TimeUnit.SECONDS))
                awaitDialog("Checking for updates").getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
                shadowOf(Looper.getMainLooper()).idle()
                updater.check()
                release.countDown()
                awaitDialog("You're up to date")
                assertEquals(2, requests)
            } finally { release.countDown() }
        }
    }
}
