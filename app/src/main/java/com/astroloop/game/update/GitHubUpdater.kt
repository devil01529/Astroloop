package com.astroloop.game.update

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.ProgressBar
import androidx.core.content.FileProvider
import com.astroloop.game.BuildConfig
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** User-initiated only. No accounts, analytics, or background polling. Android confirms every installation. */
class GitHubUpdater internal constructor(
    private val activity: Activity,
    private val connectionFactory: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }
) {
    private val executor=Executors.newSingleThreadExecutor()
    private val main=Handler(Looper.getMainLooper())
    private var task:Future<*>?=null
    private var dialog:AlertDialog?=null
    private var pendingInstall:Pair<File,UpdateRelease>?=null
    @Volatile private var closed=false
    private var busy=false
    private var operationId=0
    private val repository=BuildConfig.UPDATE_REPOSITORY

    private fun ui(block:()->Unit) { main.post { if(!closed && !activity.isFinishing && !activity.isDestroyed) block() } }
    private fun uiFor(id:Int,block:()->Unit) { ui { if(operationId==id) block() } }
    private fun message(title:String,text:String) {
        if(closed)return
        dialog=AlertDialog.Builder(activity).setTitle(title).setMessage(text).setPositiveButton("OK",null).show()
    }
    fun check() {
        if(busy)return
        busy=true
        val operation=++operationId
        dialog=AlertDialog.Builder(activity).setTitle("Checking for updates")
            .setMessage("Looking for a signed release on GitHub…").setNegativeButton("Cancel") {_,_->operationId++;task?.cancel(true);busy=false}.show()
        dialog?.setOnCancelListener {operationId++;task?.cancel(true);busy=false}
        task=executor.submit {
            try {
                val connection=open("https://github.com/$repository/releases/latest/download/update.json")
                val text=try {
                    connection.inputStream.use { stream ->
                        val buffer=ByteArray(32*1024+1);var used=0
                        while(used<buffer.size){val n=stream.read(buffer,used,buffer.size-used);if(n<0)break;used+=n}
                        require(used<=32*1024){"Update manifest is too large"}
                        String(buffer,0,used,Charsets.UTF_8)
                    }
                } finally {connection.disconnect()}
                val release=UpdateRelease.parse(text,repository)
                if(Thread.currentThread().isInterrupted)return@submit
                uiFor(operation) {
                    busy=false;dialog?.dismiss()
                    if(release.versionCode<=BuildConfig.VERSION_CODE) message("You're up to date","Installed: ${BuildConfig.VERSION_NAME}")
                    else dialog=AlertDialog.Builder(activity).setTitle("Update available")
                        .setMessage("${BuildConfig.VERSION_NAME} → ${release.versionName}\n\nDownload size: ${(release.apkBytes+999999)/1000000} MB\n\nSaved unlocks and settings stay on this phone. Installing restarts the game. Android will ask you to approve installation.")
                        .setNegativeButton("Later",null).setPositiveButton("Download") {_,_->download(release)}.show()
                }
            } catch(e:Exception) {
                if(!Thread.currentThread().isInterrupted)uiFor(operation) {
                    busy=false;dialog?.dismiss()
                    message("Could not check for updates","Check your internet connection and try again. If this is the first release, its update files may not be published yet. You can keep playing offline.")
                }
            }
        }
    }

    private fun download(release:UpdateRelease) {
        if(busy)return
        busy=true
        val operation=++operationId
        val progress=ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal).apply {max=100}
        dialog=AlertDialog.Builder(activity).setTitle("Downloading ${release.versionName}").setView(progress)
            .setNegativeButton("Cancel") {_,_->operationId++;task?.cancel(true);busy=false}.show()
        dialog?.setOnCancelListener {operationId++;task?.cancel(true);busy=false}
        task=executor.submit {
            val directory=File(activity.cacheDir,"updates").apply {mkdirs()}
            val partial=File(directory,"update.part")
            try {
                val digest=MessageDigest.getInstance("SHA-256")
                var received=0L;var lastProgress=-1
                val connection=open(release.apkUrl)
                try {
                    connection.inputStream.use {input->partial.outputStream().use {output->
                        val buffer=ByteArray(64*1024)
                        while(true) {
                            if(Thread.currentThread().isInterrupted)throw InterruptedException()
                            val n=input.read(buffer);if(n<0)break
                            received+=n
                            require(received<=release.apkBytes && received<=UpdateRelease.MAX_APK_BYTES){"Download exceeds declared size"}
                            digest.update(buffer,0,n);output.write(buffer,0,n)
                            val percent=(received*100/release.apkBytes).toInt()
                            if(percent!=lastProgress){lastProgress=percent;uiFor(operation) {progress.progress=percent}}
                        }
                    }}
                } finally {connection.disconnect()}
                require(received==release.apkBytes){"Incomplete download"}
                require(digest.digest().joinToString(""){"%02x".format(it)}==release.sha256){"Checksum mismatch"}
                verifyPackage(partial,release)
                val apk=File(directory,"astro-loop-s24.apk")
                if(apk.exists())require(apk.delete())
                require(partial.renameTo(apk))
                if(Thread.currentThread().isInterrupted)return@submit
                uiFor(operation) {busy=false;dialog?.dismiss();offerInstall(apk,release)}
            } catch(e:Exception) {
                partial.delete()
                if(!Thread.currentThread().isInterrupted)uiFor(operation) {
                    busy=false;dialog?.dismiss()
                    message("Update not installed","The download could not be completed or verified. Your current game and saves have not changed. Try again with a stable connection.")
                }
            }
        }
    }

    private fun verifyPackage(file:File,release:UpdateRelease) {
        val pm=activity.packageManager
        val flags=if(Build.VERSION.SDK_INT>=28)PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        @Suppress("DEPRECATION")
        val candidate=requireNotNull(pm.getPackageArchiveInfo(file.absolutePath,flags)){"Invalid APK"}
        @Suppress("DEPRECATION")
        val installed=pm.getPackageInfo(activity.packageName,flags)
        require(candidate.packageName==activity.packageName){"Wrong application"}
        @Suppress("DEPRECATION")
        val version=if(Build.VERSION.SDK_INT>=28)candidate.longVersionCode else candidate.versionCode.toLong()
        require(version==release.versionCode && version>BuildConfig.VERSION_CODE){"Unexpected version"}
        @Suppress("DEPRECATION")
        fun fingerprints(info:android.content.pm.PackageInfo):Set<String> {
            val signatures=if(Build.VERSION.SDK_INT>=28) info.signingInfo?.apkContentsSigners else info.signatures
            return signatures.orEmpty().map {signature->MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString(""){"%02x".format(it)}}.toSet()
        }
        val trusted=fingerprints(installed)
        require(trusted.isNotEmpty() && fingerprints(candidate)==trusted){"Signing identity does not match"}
    }

    private fun offerInstall(file:File,release:UpdateRelease) {
        dialog=AlertDialog.Builder(activity).setTitle("Update verified")
            .setMessage("${release.versionName} is ready. Installing restarts the game, so finish your current run first. Saved unlocks and settings are kept. Keep the existing app installed.")
            .setNegativeButton("Later",null).setPositiveButton("Install") {_,_->
                if(Build.VERSION.SDK_INT>=26 && !activity.packageManager.canRequestPackageInstalls()) {
                    pendingInstall=file to release
                    try {activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:${activity.packageName}")))}
                    catch(e:Exception){pendingInstall=null;message("Installation permission","Allow installs from Astro Loop S24 in Android settings, then check for updates again.")}
                } else install(file,release)
            }.show()
    }
    fun onResume() {
        val pending=pendingInstall ?: return
        pendingInstall=null
        if(Build.VERSION.SDK_INT<26 || activity.packageManager.canRequestPackageInstalls())install(pending.first,pending.second)
        else message("Installation canceled","Android did not allow installation. Your current version is unchanged.")
    }
    private fun install(file:File,release:UpdateRelease) {
        try {
            verifyPackage(file,release)
            val uri=FileProvider.getUriForFile(activity,"${activity.packageName}.updates",file)
            activity.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch(e:Exception){message("Could not start installation","The package could not be verified or Android's installer is unavailable. Your current version is unchanged.")}
    }
    private fun open(address:String):HttpURLConnection {
        var url=URL(address)
        repeat(6) {
            require(url.protocol=="https" && (url.host=="github.com" || url.host.endsWith(".githubusercontent.com")) && url.userInfo==null && (url.port==-1 || url.port==443))
            val connection=connectionFactory(url).apply {
                connectTimeout=15000;readTimeout=20000;instanceFollowRedirects=false
                useCaches=false
                setRequestProperty("User-Agent","AstroLoopS24/${BuildConfig.VERSION_NAME}")
                setRequestProperty("Cache-Control","no-cache")
            }
            val status=try { connection.responseCode } catch(e:Exception) { connection.disconnect();throw e }
            if(status in listOf(301,302,303,307,308)) {
                val location=connection.getHeaderField("Location");connection.disconnect()
                require(!location.isNullOrBlank());url=URL(url,location)
            } else {
                if(status!=200){connection.disconnect();throw IOException("HTTP $status")}
                return connection
            }
        }
        throw IOException("Too many redirects")
    }
    fun close(){closed=true;task?.cancel(true);executor.shutdownNow();dialog?.dismiss();pendingInstall=null}
}
