package com.astroloop.game.render

import android.graphics.Canvas
import android.graphics.Paint
import com.astroloop.game.input.FixedStickLayout
import com.astroloop.game.input.InputModeState

/** Permanent thumb target; only the knob moves. All dimensions share the actual input geometry. */
class FixedStickRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    fun render(canvas: Canvas, pad: FixedStickLayout, knobX: Float, knobY: Float, active: Boolean) {
        if (!InputModeState.isTouch || pad.radius <= 0f) return
        paint.style = Paint.Style.FILL
        paint.color = 0xA00A1723.toInt()
        canvas.drawCircle(pad.x, pad.y, pad.radius, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = if (active) 0xC68AE8E0.toInt() else 0x707AADC0
        canvas.drawCircle(pad.x, pad.y, pad.radius, paint)
        paint.strokeWidth = 1.5f
        paint.color = 0x477AADC0
        canvas.drawCircle(pad.x, pad.y, pad.deadZone, paint)
        val tick = pad.radius * .10f
        for (side in intArrayOf(-1, 1)) {
            canvas.drawLine(pad.x + side * (pad.radius - tick), pad.y, pad.x + side * pad.radius, pad.y, paint)
            canvas.drawLine(pad.x, pad.y + side * (pad.radius - tick), pad.x, pad.y + side * pad.radius, paint)
        }
        paint.style = Paint.Style.FILL
        paint.color = if (active) 0xB25ED6D0.toInt() else 0x605E8A9D
        canvas.drawCircle(knobX, knobY, pad.radius * .36f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = if (active) 0xE8C4FFFF.toInt() else 0x806CABB9.toInt()
        paint.strokeWidth = 2f
        canvas.drawCircle(knobX, knobY, pad.radius * .36f, paint)
        paint.style = Paint.Style.FILL
        paint.color = 0x8DB5D4DF.toInt()
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = pad.radius * .17f
        canvas.drawText("STEER", pad.x, pad.y + pad.radius + paint.textSize * 1.6f, paint)
    }
}
