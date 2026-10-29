package com.astroloop.game.render

import android.content.Context
import android.graphics.*
import com.astroloop.game.R
import com.astroloop.game.entity.Asteroid
import com.astroloop.game.entity.AsteroidType
import kotlin.math.PI

/** One offline atlas shared by all scenes. Drawing scratch state is private to each render thread. */
object RealisticArt {
    @Volatile private var atlas: Bitmap? = null
    private val cells = Array(9) { Rect() }
    private class DrawingState {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val path = Path()
        val target = RectF()
    }
    private val drawing = object : ThreadLocal<DrawingState>() {
        override fun initialValue() = DrawingState()
    }
    private val mineralFilters = arrayOf<ColorFilter?>(
        null,
        ColorMatrixColorFilter(floatArrayOf(.8f,0f,0f,0f,28f, 0f,1f,0f,0f,35f, 0f,0f,1.15f,0f,45f, 0f,0f,0f,1f,0f)),
        null,
        PorterDuffColorFilter(0xFFFFB88B.toInt(), PorterDuff.Mode.MULTIPLY),
        PorterDuffColorFilter(0xFFBDB0EB.toInt(), PorterDuff.Mode.MULTIPLY),
        PorterDuffColorFilter(0xFFA9D2B0.toInt(), PorterDuff.Mode.MULTIPLY)
    )

    @Synchronized fun initialize(context: Context) {
        if (atlas != null) return
        val bitmap = requireNotNull(BitmapFactory.decodeResource(context.resources,
            R.drawable.realistic_space_atlas, BitmapFactory.Options().apply { inScaled = false }))
        for (i in cells.indices) {
            val col = i % 3
            val row = i / 3
            cells[i].set(col * bitmap.width / 3, row * bitmap.height / 3,
                (col + 1) * bitmap.width / 3, (row + 1) * bitmap.height / 3)
        }
        // Publish only after the immutable source rectangles are ready.
        atlas = bitmap
    }

    internal fun isLoaded() = atlas != null

    private fun hullIndex(weapon: String): Int = when (weapon) {
        "flak_cannon", "scatter_shot", "cluster_bomb", "space_mines" -> 1
        "railgun", "needle_gun", "energy_saw" -> 2
        else -> 0
    }

    fun drawShip(canvas: Canvas, x: Float, y: Float, rotation: Float, size: Float,
                 shipColor: Int, pilotColor: Int, weapon: String, alpha: Float, design: Int = 0): Boolean {
        val bitmap = atlas ?: return false
        if (size <= 0f || alpha <= 0f) return true
        val d = drawing.get()!!
        val p = d.paint
        p.reset(); p.isAntiAlias = true; p.isFilterBitmap = true
        val opacity = (alpha.coerceIn(0f, 1f) * 255).toInt()
        p.alpha = opacity
        val save = canvas.save()
        try {
            canvas.translate(x, y)
            canvas.rotate(rotation * 180f / PI.toFloat())
            // Preserve cosmetic preview variants as different hull families/proportions.
            val hull = if (design == 0) hullIndex(weapon) else (design - 1) % 3
            val stretch = if (design == 0) 1f else 0.94f + (design % 4) * .04f
            d.target.set(-size * 1.13f * stretch, -size * 1.13f, size * 1.13f * stretch, size * 1.13f)
            canvas.drawBitmap(bitmap, cells[hull], d.target, p)

            // Paint markings keep crew/faction colors legible on the metal hull.
            p.color = shipColor; p.alpha = opacity; p.strokeWidth = size * .045f
            for (side in intArrayOf(-1, 1)) {
                canvas.drawLine(-size * .43f, side * size * .38f, -size * .18f, side * size * .3f, p)
            }
            p.color = pilotColor; p.alpha = opacity
            canvas.drawCircle(size * .42f, 0f, size * .035f, p)

            // A physical gun mount replaces the old line-symbol cannon.
            val gun = when (weapon) {
                "railgun", "needle_gun" -> 8
                "flak_cannon", "scatter_shot", "cluster_bomb" -> 7
                else -> 6
            }
            p.color = Color.WHITE; p.alpha = opacity
            d.target.set(-size * .52f, -size * .36f, size * .52f, size * .36f)
            canvas.drawBitmap(bitmap, cells[gun], d.target, p)
        } finally { canvas.restoreToCount(save) }
        return true
    }

    /** Natural rocky edges, drawn at the existing collision radius without mutating simulation state. */
    fun drawAsteroid(canvas: Canvas, asteroid: Asteroid): Boolean {
        val bitmap = atlas ?: return false
        if (asteroid.radius <= 0f) return true
        val d = drawing.get()!!
        val p = d.paint
        val save = canvas.save()
        try {
            canvas.translate(asteroid.position.x, asteroid.position.y)
            canvas.rotate(asteroid.rotation * 180f / PI.toFloat())
            p.reset(); p.isAntiAlias = true; p.isFilterBitmap = true
            val tile = when (asteroid.type) {
                AsteroidType.ROCK -> 3 + asteroid.shapePoints.size % 3
                AsteroidType.METAL, AsteroidType.MAGNETIC -> 4
                AsteroidType.VOLATILE, AsteroidType.TRAIL -> 5
                else -> 3
            }
            p.color = Color.WHITE
            p.colorFilter = mineralFilters[asteroid.type.ordinal]
            // Atlas rock silhouettes occupy about 84% of a cell. Compensate for
            // that transparent padding, so their visible radius matches gameplay.
            val extent = asteroid.radius * 1.18f
            d.target.set(-extent, -extent, extent, extent)
            canvas.drawBitmap(bitmap, cells[tile], d.target, p)
        } finally { canvas.restoreToCount(save) }
        return true
    }

    /** Solid metal round with a hot tracer; preserves the caller's projectile-fade multiplier. */
    fun drawRound(canvas: Canvas, x: Float, y: Float, rotation: Float, width: Float, length: Float,
                  color: Int, alpha: Float) {
        val d = drawing.get()!!
        val p = d.paint
        p.reset(); p.isAntiAlias = true
        val save = canvas.save()
        try {
            canvas.translate(x, y); canvas.rotate(rotation * 180f / PI.toFloat())
            val w = width.coerceAtLeast(1.5f)
            val len = length.coerceAtLeast(w * 2f)
            p.color = color; p.alpha = (alpha.coerceIn(0f, 1f) * 95).toInt(); p.strokeWidth = w * .7f
            canvas.drawLine(-len * 1.4f, 0f, -len * .55f, 0f, p)
            d.path.rewind(); d.path.moveTo(0f, 0f); d.path.lineTo(-w * .8f, -w * .5f)
            d.path.lineTo(-len * .65f, -w * .5f); d.path.lineTo(-len * .65f, w * .5f)
            d.path.lineTo(-w * .8f, w * .5f); d.path.close()
            p.color = 0xFFB6BFC9.toInt(); p.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
            canvas.drawPath(d.path, p)
            p.color = 0xFFFFE8BA.toInt(); p.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt(); p.strokeWidth = w * .22f
            canvas.drawLine(-w * .7f, -w * .2f, -len * .55f, -w * .2f, p)
        } finally { canvas.restoreToCount(save) }
    }
}
