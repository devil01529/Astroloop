package com.astroloop.game.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI

/** Player-only presentation scaling leaves collision and weapon balance unchanged. */
internal class PlayerPresentation {
    companion object {
        const val HULL_SCALE = com.astroloop.game.core.GameConfig.SHIP_VISUAL_SCALE
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val oval = RectF()
    private val glow = RadialGradient(0f, 0f, 1f,
        intArrayOf(0x002188BB, 0x003ABFD4, 0x704ED7EA, 0x004ED7EA),
        floatArrayOf(0f, .63f, .85f, 1f), Shader.TileMode.CLAMP)

    fun shield(canvas: Canvas, x: Float, y: Float, rotation: Float, radius: Float, strength: Float) {
        if (strength <= 0f) return
        val save = canvas.save()
        canvas.translate(x, y)
        canvas.rotate(rotation * 180f / PI.toFloat())
        canvas.scale(radius * 1.24f, radius * .98f)
        paint.shader = glow
        paint.alpha = (strength.coerceIn(0f, 1f) * 150).toInt()
        canvas.drawCircle(0f, 0f, 1f, paint)
        paint.shader = null; paint.alpha = 255
        canvas.restoreToCount(save)
    }

    fun exhaust(canvas: Canvas, x: Float, y: Float, rotation: Float, radius: Float, intensity: Float, hot: Boolean) {
        if (intensity <= 0f) return
        val save = canvas.save()
        canvas.translate(x, y)
        canvas.rotate(rotation * 180f / PI.toFloat())
        val length = radius * (.25f + intensity * .7f)
        val back = -radius * .72f
        paint.color = if (hot) 0x86FF6633.toInt() else 0x8259C7FF.toInt()
        oval.set(back - length, -radius * .13f, back + radius * .08f, radius * .13f)
        canvas.drawOval(oval, paint)
        paint.color = 0xD9E1F5FF.toInt()
        oval.set(back - length * .55f, -radius * .065f, back, radius * .065f)
        canvas.drawOval(oval, paint)
        canvas.restoreToCount(save)
    }
}
