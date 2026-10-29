package com.astroloop.game.core

/** Match the active display up to 120 Hz, retaining sub-millisecond precision. */
internal fun frameBudgetNanos(refreshRate: Float, frameCap: Int = 120): Long {
    val fps = if (refreshRate.isFinite() && refreshRate >= 30f) {
        refreshRate.coerceAtMost(if (frameCap == 60) 60f else GameConfig.TARGET_FPS.toFloat())
    } else 60f
    return (1_000_000_000.0 / fps).toLong()
}
