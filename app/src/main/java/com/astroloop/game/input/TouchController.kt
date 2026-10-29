package com.astroloop.game.input

import android.view.MotionEvent
import com.astroloop.game.core.GameConfig
import com.astroloop.game.util.Vector2
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

class TouchController {

    // Joystick state
    private var joystickActive = false
    private val joystickOrigin = Vector2()
    private val joystickPosition = Vector2()
    private var joystickPointerId = -1
    private var tapEligible = false
    private var downTime = 0L

    // Output direction (normalized, 0-1 magnitude)
    val moveDirection = Vector2()
    var moveMagnitude: Float = 0f
        private set

    // For UI interactions (upgrade selection, restart, etc.)
    var lastTapX: Float = 0f
        private set
    var lastTapY: Float = 0f
        private set
    private val hasTap = AtomicBoolean(false)

    var renderScale: Float = 1f

    fun handleTouchEvent(event: MotionEvent): Boolean {
        InputModeState.markTouch()

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Start joystick at touch location
                joystickOrigin.set(event.x / renderScale, event.y / renderScale)
                joystickPosition.set(event.x / renderScale, event.y / renderScale)
                joystickActive = true
                joystickPointerId = event.getPointerId(0)
                tapEligible = true
                downTime = event.eventTime
                updateMoveDirection()
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                tapEligible = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (joystickActive) {
                    val pointerIndex = event.findPointerIndex(joystickPointerId)
                    if (pointerIndex >= 0) {
                        joystickPosition.set(event.getX(pointerIndex) / renderScale, event.getY(pointerIndex) / renderScale)
                        updateMoveDirection()
                    }
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                // System gestures and interruptions must never select a menu item.
                reset()
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!joystickActive) return true
                joystickPosition.set(event.x / renderScale, event.y / renderScale)
                // Check for tap (short movement)
                val dx = joystickPosition.x - joystickOrigin.x
                val dy = joystickPosition.y - joystickOrigin.y
                val dist = sqrt(dx * dx + dy * dy)

                if (tapEligible && event.eventTime - downTime <= 350L && dist < GameConfig.JOYSTICK_DEAD_ZONE) {
                    // This was a tap, not a drag
                    lastTapX = joystickOrigin.x
                    lastTapY = joystickOrigin.y
                    hasTap.set(true)
                }

                // Release joystick
                joystickActive = false
                joystickPointerId = -1
                moveDirection.zero()
                moveMagnitude = 0f
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)

                if (pointerId == joystickPointerId) {
                    // Hand control to the remaining finger without a sudden movement.
                    val nextIndex = if (pointerIndex == 0) 1 else 0
                    joystickPointerId = event.getPointerId(nextIndex)
                    joystickOrigin.set(event.getX(nextIndex) / renderScale, event.getY(nextIndex) / renderScale)
                    joystickPosition.set(joystickOrigin)
                    moveDirection.zero()
                    moveMagnitude = 0f
                }
                return true
            }
        }

        return false
    }

    private fun updateMoveDirection() {
        val dx = joystickPosition.x - joystickOrigin.x
        val dy = joystickPosition.y - joystickOrigin.y
        val distance = sqrt(dx * dx + dy * dy)

        if (distance < GameConfig.JOYSTICK_DEAD_ZONE) {
            moveDirection.zero()
            moveMagnitude = 0f
            return
        }

        tapEligible = false // Returning a drag to its origin must never become a menu tap.
        val maxRadius = getMaxRadius()
        if (distance > maxRadius) {
            joystickOrigin.set(joystickPosition.x - dx / distance * maxRadius,
                joystickPosition.y - dy / distance * maxRadius)
        }
        val clampedDist = distance.coerceAtMost(maxRadius)
        moveMagnitude = (clampedDist - GameConfig.JOYSTICK_DEAD_ZONE) /
                        (maxRadius - GameConfig.JOYSTICK_DEAD_ZONE)
        moveMagnitude = moveMagnitude.coerceIn(0f, 1f)

        // Normalize direction
        moveDirection.set(dx / distance, dy / distance)
    }

    fun isTapCandidate(event: MotionEvent): Boolean {
        val dx = event.x / renderScale - joystickOrigin.x
        val dy = event.y / renderScale - joystickOrigin.y
        return joystickActive && tapEligible && event.eventTime - downTime <= 350L &&
            dx * dx + dy * dy < GameConfig.JOYSTICK_DEAD_ZONE * GameConfig.JOYSTICK_DEAD_ZONE
    }

    fun getMaxRadius(): Float = GameConfig.JOYSTICK_DEAD_ZONE +
        (GameConfig.JOYSTICK_MAX_RADIUS - GameConfig.JOYSTICK_DEAD_ZONE) /
        com.astroloop.game.settings.PilotSettings.current.sensitivity

    fun consumeTap(): Boolean = hasTap.getAndSet(false)

    fun isJoystickActive(): Boolean = joystickActive

    fun getJoystickOrigin(): Vector2 = joystickOrigin

    fun getJoystickPosition(): Vector2 = joystickPosition

    fun reset() {
        joystickActive = false
        joystickPointerId = -1
        tapEligible = false
        joystickOrigin.zero()
        joystickPosition.zero()
        moveDirection.zero()
        moveMagnitude = 0f
        hasTap.set(false)
    }
}
