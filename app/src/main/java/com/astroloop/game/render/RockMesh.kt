package com.astroloop.game.render

import kotlin.math.*

/** Irregular geodesic boulders and separate angular fracture meshes, built once offline. */
internal class RockMesh private constructor(
    val positions: FloatArray, val indices: IntArray, val variant: Int, val fragment: Boolean
) {
    val vertexCount get() = positions.size / 3
    val faceCount get() = indices.size / 3
    val normals = FloatArray(positions.size)
    val faceNormals = FloatArray(indices.size)
    init {
        var maxLength = 0f
        for (i in positions.indices step 3) maxLength = max(maxLength,
            sqrt(positions[i].pow(2) + positions[i + 1].pow(2) + positions[i + 2].pow(2)))
        // At camera distance 4 the projected model fits within the collision circle.
        for (i in positions.indices) positions[i] *= .94f / maxLength
        for (f in 0 until faceCount) {
            val a = indices[f * 3] * 3; var b = indices[f * 3 + 1] * 3; var c = indices[f * 3 + 2] * 3
            val ux = positions[b] - positions[a]; val uy = positions[b + 1] - positions[a + 1]; val uz = positions[b + 2] - positions[a + 2]
            val vx = positions[c] - positions[a]; val vy = positions[c + 1] - positions[a + 1]; val vz = positions[c + 2] - positions[a + 2]
            var nx = uy * vz - uz * vy; var ny = uz * vx - ux * vz; var nz = ux * vy - uy * vx
            if (nx * positions[a] + ny * positions[a + 1] + nz * positions[a + 2] < 0f) {
                val swap = b; b = c; c = swap
                indices[f * 3 + 1] = b / 3; indices[f * 3 + 2] = c / 3
                nx = -nx; ny = -ny; nz = -nz
            }
            val length = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(.00001f)
            nx /= length; ny /= length; nz /= length
            faceNormals[f * 3] = nx; faceNormals[f * 3 + 1] = ny; faceNormals[f * 3 + 2] = nz
            for (corner in 0..2) {
                val index = indices[f * 3 + corner]; val p = index * 3
                normals[p] += nx; normals[p + 1] += ny; normals[p + 2] += nz

            }
        }
        for (p in normals.indices step 3) {
            val length = sqrt(normals[p].pow(2) + normals[p + 1].pow(2) + normals[p + 2].pow(2)).coerceAtLeast(.00001f)
            normals[p] /= length; normals[p + 1] /= length; normals[p + 2] /= length
        }
    }

    companion object {
        const val VARIANTS = com.astroloop.game.entity.Asteroid.SHAPE_VARIANTS
        private data class V(val x: Float, val y: Float, val z: Float) {
            fun unit(): V { val n = sqrt(x * x + y * y + z * z); return V(x / n, y / n, z / n) }
        }

        fun boulder(variant: Int, subdivisions: Int): RockMesh {
            val t = (1f + sqrt(5f)) / 2f
            val vertices = arrayListOf(V(-1f,t,0f),V(1f,t,0f),V(-1f,-t,0f),V(1f,-t,0f),
                V(0f,-1f,t),V(0f,1f,t),V(0f,-1f,-t),V(0f,1f,-t),
                V(t,0f,-1f),V(t,0f,1f),V(-t,0f,-1f),V(-t,0f,1f)).map { it.unit() }.toMutableList()
            var faces = intArrayOf(0,11,5, 0,5,1, 0,1,7, 0,7,10, 0,10,11,
                1,5,9, 5,11,4, 11,10,2, 10,7,6, 7,1,8,
                3,9,4, 3,4,2, 3,2,6, 3,6,8, 3,8,9,
                4,9,5, 2,4,11, 6,2,10, 8,6,7, 9,8,1)
            repeat(subdivisions) {
                val mids = HashMap<Long,Int>()
                fun midpoint(a: Int, b: Int): Int {
                    val key = (min(a,b).toLong() shl 32) or max(a,b).toLong()
                    return mids.getOrPut(key) {
                        val x = vertices[a]; val y = vertices[b]
                        vertices.add(V(x.x + y.x, x.y + y.y, x.z + y.z).unit()); vertices.lastIndex
                    }
                }
                val next = IntArray(faces.size * 4); var offset = 0
                for (f in faces.indices step 3) {
                    val a=faces[f];val b=faces[f+1];val c=faces[f+2]
                    val ab=midpoint(a,b);val bc=midpoint(b,c);val ca=midpoint(c,a)
                    for (i in intArrayOf(a,ab,ca, b,bc,ab, c,ca,bc, ab,bc,ca)) next[offset++] = i
                }
                faces = next
            }
            val phase = variant * 1.73f
            val shapes = arrayOf(floatArrayOf(1.35f,.76f,.83f),floatArrayOf(.84f,1.3f,.73f),
                floatArrayOf(1.1f,.85f,.95f),floatArrayOf(1.22f,.82f,1.05f))
            val axes = shapes[variant % shapes.size]
            val points = FloatArray(vertices.size * 3)
            for ((i,v) in vertices.withIndex()) {
                // Broad lobes, scooped faces, and smaller ridges break the round silhouette.
                val rough = .86f + .18f * sin(v.x * 4.6f + phase) * cos(v.y * 3.5f - phase) +
                    .13f * sin(v.z * 5.2f + v.x * 2.1f + phase) +
                    .055f * sin(v.x * 11f + phase) * sin(v.y * 8f + v.z * 6f)
                val dent = .21f * max(0f, v.x * .67f + v.y * .3f + v.z * .68f).pow(12)
                val r = rough - dent
                points[i*3] = v.x * axes[0] * r + .12f * v.y * v.z
                points[i*3+1] = v.y * axes[1] * r + .09f * v.x * v.x - .04f
                points[i*3+2] = v.z * axes[2] * r
            }
            return RockMesh(points, faces, variant, false)
        }

        fun shard(variant: Int): RockMesh {
            val width = .45f + (variant % 4) * .12f
            val depth = .25f + (variant % 3) * .13f
            // Broken triangular prism: a slab, splinter, or wedge, never a smaller boulder.
            val points = floatArrayOf(-.95f,-width,-depth, .85f,-width*.65f,-depth*.7f,
                .15f,width,-depth*.85f, -.6f,-width*.65f,depth, .72f,-width*.35f,depth*.65f,
                -.05f,width*.65f,depth*.9f)
            return RockMesh(points, intArrayOf(0,2,1, 3,4,5, 0,1,4, 0,4,3,
                1,2,5, 1,5,4, 2,0,3, 2,3,5), variant, true)
        }
    }
}
