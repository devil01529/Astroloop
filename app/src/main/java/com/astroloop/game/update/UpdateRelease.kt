package com.astroloop.game.update

import org.json.JSONObject
import java.net.URI

/** Only this repository's versioned HTTPS release assets may be offered to the installer. */
data class UpdateRelease(val versionCode: Long, val versionName: String, val apkUrl: String,
                         val sha256: String, val apkBytes: Long) {
    companion object {
        const val MAX_APK_BYTES = 150L * 1024 * 1024
        fun parse(json: String, repository: String): UpdateRelease {
            require(repository.matches(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")))
            val obj=JSONObject(json)
            require(obj.getInt("schemaVersion")==1) { "Unsupported update manifest" }
            val version=obj.getLong("versionCode")
            val name=obj.getString("versionName")
            val url=obj.getString("apkUrl")
            val digest=obj.getString("sha256").lowercase()
            val size=obj.getLong("apkBytes")
            val uri=URI(url)
            require(version>0 && name.isNotBlank() && name.length<=80)
            require(uri.scheme=="https" && uri.host=="github.com" && uri.userInfo==null && uri.port==-1 && uri.query==null && uri.fragment==null)
            require(uri.path.startsWith("/$repository/releases/download/") && uri.path.endsWith("/astro-loop-s24.apk") && !uri.path.contains(".."))
            require(digest.matches(Regex("[a-f0-9]{64}")))
            require(size in 1..MAX_APK_BYTES)
            return UpdateRelease(version,name,url,digest,size)
        }
    }
}
