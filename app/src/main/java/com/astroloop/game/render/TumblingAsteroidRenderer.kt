package com.astroloop.game.render

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import com.astroloop.game.entity.Asteroid
import com.astroloop.game.entity.AsteroidType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** Textured, lit 3D geometry projected into the existing top-down scene.
 * One batched draw per rock; immutable meshes/materials and per-thread scratch arrays. */
internal object TumblingAsteroidRenderer {
    internal class Mesh(columns: Int, rows: Int, variant: Int) {
        val positions = FloatArray((columns + 1) * (rows + 1) * 3)
        val normals = FloatArray(positions.size)
        val uv = FloatArray((columns + 1) * (rows + 1) * 2)
        val indices = IntArray(columns * rows * 6)
        val vertexCount: Int get() = positions.size / 3
        init {
            val ax = floatArrayOf(.97f, .84f, 1f, .93f)[variant]
            val ay = floatArrayOf(.83f, 1f, .92f, .88f)[variant]
            val az = floatArrayOf(1f, .93f, .78f, 1f)[variant]
            var maximum = 0f
            for (row in 0..rows) for (col in 0..columns) {
                val u = col.toFloat() / columns
                val v = row.toFloat() / rows
                val theta = u * (2f * PI.toFloat())
                val phi = v * PI.toFloat()
                val x = sin(phi) * cos(theta); val y = cos(phi); val z = sin(phi) * sin(theta)
                val roughness = .90f + .045f * sin(theta * 3f + variant) * sin(phi) * sin(phi) +
                    .025f * cos(phi * 4f + variant) * sin(theta * 2f)
                val index = row * (columns + 1) + col
                val p = index * 3
                positions[p] = x * ax * roughness
                positions[p + 1] = y * ay * roughness
                positions[p + 2] = z * az * roughness
                maximum = max(maximum, sqrt(positions[p] * positions[p] + positions[p + 1] * positions[p + 1] + positions[p + 2] * positions[p + 2]))
                val nx = x / ax; val ny = y / ay; val nz = z / az
                val length = sqrt(nx * nx + ny * ny + nz * nz)
                normals[p] = nx / length; normals[p + 1] = ny / length; normals[p + 2] = nz / length
                // Two mirrored material repeats close the longitude seam without a texture jump.
                uv[index * 2] = u * 2f; uv[index * 2 + 1] = v
            }
            // Perspective projection at distance 4 keeps this bounded inside the collision circle.
            for (i in positions.indices) positions[i] *= .94f / maximum
            var n = 0
            for (row in 0 until rows) for (col in 0 until columns) {
                val a = row * (columns + 1) + col; val b = a + columns + 1
                indices[n++] = a; indices[n++] = a + 1; indices[n++] = b
                indices[n++] = a + 1; indices[n++] = b + 1; indices[n++] = b
            }
        }
    }

    private data class Material(val shader: BitmapShader, val width: Int, val height: Int)
    @Volatile private var materials: Array<Material>? = null
    internal val detailedMeshes = Array(4) { Mesh(16, 12, it) }
    private val smallMeshes = Array(4) { Mesh(10, 8, it) }
    private class Scratch {
        val projected = FloatArray(221 * 2)
        val light = IntArray(221)
        val vertices = FloatArray(16 * 12 * 6 * 2)
        val texture = FloatArray(vertices.size)
        val colors = IntArray(vertices.size / 2)
        // Shared triangle edges must remain opaque. Texture filtering handles material detail.
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { color = Color.WHITE }
    }
    private val scratch = object : ThreadLocal<Scratch>() { override fun initialValue() = Scratch() }

    @Synchronized fun initialize(atlas: Bitmap, cells: Array<Rect>) {
        if (materials != null) return
        materials = Array(3) { index ->
            val tile = cells[index + 3]
            // Use the opaque centre as a rock material, not the original flat silhouette.
            val width = (tile.width() * .48f).toInt()
            val height = (tile.height() * .48f).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val materialCanvas = Canvas(bitmap)
            materialCanvas.drawColor(0xFF302E2B.toInt())
            val left = tile.left + (tile.width() * .26f).toInt()
            val top = tile.top + (tile.height() * .26f).toInt()
            materialCanvas.drawBitmap(atlas, Rect(left, top, left + width, top + height),
                Rect(0, 0, width, height), null)
            Material(BitmapShader(bitmap, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR), bitmap.width, bitmap.height)
        }
    }

    fun draw(canvas: Canvas, asteroid: Asteroid, materialIndex: Int): Boolean {
        val textures = materials ?: return false
        val radius = asteroid.radius
        if (radius <= 0f) return true
        @Suppress("DEPRECATION")
        if (canvas.quickReject(asteroid.position.x - radius, asteroid.position.y - radius,
                asteroid.position.x + radius, asteroid.position.y + radius, Canvas.EdgeType.AA)) return true
        val mesh = (if (radius < 28f) smallMeshes else detailedMeshes)[asteroid.tumbleVariant.coerceIn(0, 3)]
        val d = scratch.get()!!
        val cx = cos(asteroid.tumbleX); val sx = sin(asteroid.tumbleX)
        val cy = cos(asteroid.tumbleY); val sy = sin(asteroid.tumbleY)
        val cz = cos(asteroid.rotation); val sz = sin(asteroid.rotation)
        // Rz * Ry * Rx, reused for both vertices and world-space lighting normals.
        val m00 = cz * cy; val m01 = cz * sy * sx - sz * cx; val m02 = cz * sy * cx + sz * sx
        val m10 = sz * cy; val m11 = sz * sy * sx + cz * cx; val m12 = sz * sy * cx - cz * sx
        val m20 = -sy; val m21 = cy * sx; val m22 = cy * cx
        // Bake mineral tint into opaque vertex colours. Color-matrix filters on textured
        // Canvas meshes are not consistent across Android's hardware/software renderers.
        val tint = when (asteroid.type) {
            AsteroidType.ICE -> 0xFFAAE1FF.toInt()
            AsteroidType.METAL -> 0xFFE4F0FF.toInt()
            AsteroidType.VOLATILE -> 0xFFFFB080.toInt()
            AsteroidType.MAGNETIC -> 0xFFBDA8F2.toInt()
            AsteroidType.TRAIL -> 0xFFA2DBAC.toInt()
            else -> Color.WHITE
        }
        for (i in 0 until mesh.vertexCount) {
            val p = i * 3
            val x = mesh.positions[p]; val y = mesh.positions[p + 1]; val z = mesh.positions[p + 2]
            val depth = m20 * x + m21 * y + m22 * z
            val perspective = 4f / (4f - depth)
            d.projected[i * 2] = asteroid.position.x + (m00 * x + m01 * y + m02 * z) * radius * perspective
            d.projected[i * 2 + 1] = asteroid.position.y + (m10 * x + m11 * y + m12 * z) * radius * perspective
            val nx = mesh.normals[p]; val ny = mesh.normals[p + 1]; val nz = mesh.normals[p + 2]
            val light = ((m00 * nx + m01 * ny + m02 * nz) * -.35f +
                (m10 * nx + m11 * ny + m12 * nz) * -.45f + (m20 * nx + m21 * ny + m22 * nz) * .82f).coerceAtLeast(0f)
            val shade = ((.43f + .67f * light).coerceAtMost(1f) * 255).toInt()
            d.light[i] = Color.rgb(shade * Color.red(tint) / 255,
                shade * Color.green(tint) / 255, shade * Color.blue(tint) / 255)
        }
        val material = textures[materialIndex.coerceIn(0, 2)]
        var count = 0
        var face = 0
        while (face < mesh.indices.size) {
            val a = mesh.indices[face]; val b = mesh.indices[face + 1]; val c = mesh.indices[face + 2]
            val ax = d.projected[a * 2]; val ay = d.projected[a * 2 + 1]
            val area = (d.projected[b * 2] - ax) * (d.projected[c * 2 + 1] - ay) -
                (d.projected[b * 2 + 1] - ay) * (d.projected[c * 2] - ax)
            if (area > .00001f) {
                for (corner in 0..2) {
                    val index = mesh.indices[face + corner]
                    d.vertices[count * 2] = d.projected[index * 2]
                    d.vertices[count * 2 + 1] = d.projected[index * 2 + 1]
                    d.texture[count * 2] = mesh.uv[index * 2] * material.width
                    d.texture[count * 2 + 1] = mesh.uv[index * 2 + 1] * material.height
                    d.colors[count] = d.light[index]
                    count++
                }
            }
            face += 3
        }
        if (count > 0) {
            d.paint.shader = material.shader
            canvas.drawVertices(Canvas.VertexMode.TRIANGLES, count * 2, d.vertices, 0,
                d.texture, 0, d.colors, 0, null, 0, 0, d.paint)
        }
        return true
    }
}
