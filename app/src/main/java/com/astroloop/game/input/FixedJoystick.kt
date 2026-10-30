package com.astroloop.game.input

import android.view.MotionEvent
import com.astroloop.game.core.ScreenLayout
import com.astroloop.game.settings.PilotSettings
import com.astroloop.game.settings.JoystickPlacement
import com.astroloop.game.util.Vector2
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

data class FixedStickLayout(val x: Float, val y: Float, val radius: Float) {
    val deadZone: Float get() = radius * .12f
    companion object {
        fun forScreen(layout: ScreenLayout, placement: JoystickPlacement = JoystickPlacement.CENTER): FixedStickLayout {
            val safe = layout.safe
            val edge = min(safe.width, safe.height).coerceAtLeast(0f)
            val radius = (edge * .145f).coerceAtMost(160f)
            val margin = edge * .04f
            return FixedStickLayout(
                when (placement) {
                    JoystickPlacement.CENTER -> safe.centerX
                    JoystickPlacement.LEFT -> safe.left + radius + margin
                    JoystickPlacement.RIGHT -> safe.right - radius - margin
                },
                safe.bottom - radius - edge * .10f, radius)
        }
    }
}

/** Fixed centre with deliberate handover to a second thumb already resting inside the pad. */
internal class FixedJoystick {
    var layout = FixedStickLayout(0f, 0f, 0f)
        private set
    val origin = Vector2()
    val knob = Vector2()
    val direction = Vector2()
    var magnitude = 0f
        private set
    var consumedGesture = false
        private set
    private var pointer = -1
    private var waitingThumb = -1
    val active: Boolean get() = pointer != -1
    private var screen: ScreenLayout? = null
    private var placement = JoystickPlacement.CENTER

    fun configure(screen: ScreenLayout, placement: JoystickPlacement): Boolean {
        if (this.screen === screen && this.placement == placement) return false
        this.screen = screen; this.placement = placement
        layout = FixedStickLayout.forScreen(screen, placement)
        origin.set(layout.x, layout.y)
        reset()
        return true
    }

    fun handle(event: MotionEvent, scale: Float) {
        if (scale <= 0f || !scale.isFinite()) { reset(); return }
        fun insidePad(index: Int): Boolean {
            val x = event.getX(index) / scale
            val y = event.getY(index) / scale
            val dx = x - origin.x; val dy = y - origin.y
            return layout.radius > 0f && dx * dx + dy * dy <= layout.radius * layout.radius * 1.44f
        }
        fun capture(index: Int) {
            if (index >= 0 && insidePad(index)) {
                pointer = event.getPointerId(index)
                consumedGesture = true
                move(event.getX(index) / scale, event.getY(index) / scale)
            }
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { reset(); capture(0) }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (!active) capture(event.actionIndex)
                else if (insidePad(event.actionIndex)) waitingThumb = event.getPointerId(event.actionIndex)
            }
            MotionEvent.ACTION_MOVE -> if (active) {
                val index = event.findPointerIndex(pointer)
                if (index < 0) release() else move(event.getX(index) / scale, event.getY(index) / scale)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val lifted = event.getPointerId(event.actionIndex)
                if (lifted == pointer) {
                    val next = event.findPointerIndex(waitingThumb)
                    release()
                    if (next != event.actionIndex) capture(next)
                } else if (lifted == waitingThumb) waitingThumb = -1
            }
            MotionEvent.ACTION_UP -> release()
            MotionEvent.ACTION_CANCEL -> { release(); consumedGesture = true }
        }
    }

    private fun move(x: Float, y: Float) {
        val dx = x - origin.x; val dy = y - origin.y
        val distance = sqrt(dx * dx + dy * dy)
        if (distance <= layout.deadZone || distance == 0f) {
            knob.set(origin); direction.zero(); magnitude = 0f
            return
        }
        direction.set(dx / distance, dy / distance)
        val travel = distance.coerceAtMost(layout.radius)
        knob.set(origin.x + direction.x * travel, origin.y + direction.y * travel)
        val fraction = ((travel - layout.deadZone) / (layout.radius - layout.deadZone)).coerceIn(0f, 1f)
        // Sensitivity changes the response curve, never the physical ring or full-speed reach.
        magnitude = fraction.pow(1f / PilotSettings.current.sensitivity)
    }

    private fun release() { pointer = -1; waitingThumb = -1; direction.zero(); magnitude = 0f; knob.set(origin) }
    fun reset() { release(); consumedGesture = false }
}
