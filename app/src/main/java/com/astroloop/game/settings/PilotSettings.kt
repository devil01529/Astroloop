package com.astroloop.game.settings

import android.content.Context

data class PilotOptions(
    val fps: Int = 120,
    val sensitivity: Float = 1f,
    val detailedBackground: Boolean = true,
    val backgroundMotion: Boolean = true
) {
    fun sanitized() = copy(fps = if (fps == 60) 60 else 120,
        sensitivity = if (sensitivity.isFinite()) sensitivity.coerceIn(.7f, 1.6f) else 1f)
}

/** Preferences are separate from run progress, and a single immutable snapshot is shared by render threads. */
object PilotSettings {
    @Volatile var current = PilotOptions()
        private set
    fun load(context: Context): PilotOptions {
        val p = context.getSharedPreferences("s24_pilot_options", Context.MODE_PRIVATE)
        return PilotOptions(p.getInt("fps",120),p.getFloat("sensitivity",1f),
            p.getBoolean("background_detail",true),p.getBoolean("background_motion",true))
            .sanitized().also { current=it }
    }
    fun save(context: Context, options: PilotOptions) {
        val clean=options.sanitized()
        context.getSharedPreferences("s24_pilot_options",Context.MODE_PRIVATE).edit()
            .putInt("fps",clean.fps).putFloat("sensitivity",clean.sensitivity)
            .putBoolean("background_detail",clean.detailedBackground)
            .putBoolean("background_motion",clean.backgroundMotion).apply()
        current=clean
    }
}
