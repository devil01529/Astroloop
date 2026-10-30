package com.astroloop.game.render

import android.graphics.*
import com.astroloop.game.entity.Asteroid
import com.astroloop.game.entity.AsteroidType
import com.astroloop.game.entity.RockFragment
import kotlin.math.*

/** Batched, depth-sorted irregular rock faces with reusable material and projection buffers. */
internal object TumblingAsteroidRenderer {
    private data class Material(val shader: BitmapShader, val width: Int, val height: Int)
    @Volatile private var materials: Array<Array<Material>>? = null
    internal val detailedMeshes = Array(RockMesh.VARIANTS) { RockMesh.boulder(it, 2) }
    private val smallMeshes = Array(RockMesh.VARIANTS) { RockMesh.boulder(it, 1) }
    internal val shardMeshes = Array(RockMesh.VARIANTS) { RockMesh.shard(it) }
    private const val TILE = 18
    private class Scratch {
        val projected = FloatArray(162 * 2)
        val depth = FloatArray(162)
        val light = FloatArray(162)
        val faces = IntArray(320)
        val faceDepth = FloatArray(320)
        val vertices = FloatArray(320 * 6)
        val texture = FloatArray(vertices.size)
        val colors = IntArray(vertices.size / 2)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { color = Color.WHITE }
    }
    private val scratch = object : ThreadLocal<Scratch>() { override fun initialValue() = Scratch() }

    @Synchronized fun initialize(atlas: Bitmap, cells: Array<Rect>) {
        if (materials != null) return
        val sources = Array(3) { index ->
            val tile = cells[index + 3]
            val width = (tile.width() * .48f).toInt(); val height = (tile.height() * .48f).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val materialCanvas = Canvas(bitmap); materialCanvas.drawColor(0xFF302E2B.toInt())
            val left = tile.left + (tile.width() * .26f).toInt(); val top = tile.top + (tile.height() * .26f).toInt()
            materialCanvas.drawBitmap(atlas, Rect(left, top, left + width, top + height), Rect(0, 0, width, height), null)
            val pixels=IntArray(width*height);bitmap.getPixels(pixels,0,width,0,0,width,height)
            bitmap.recycle()
            Surface(pixels,width,height)
        }
        val meshes=detailedMeshes+smallMeshes+shardMeshes
        // Bake continuous triplanar materials once; no seams or per-frame texture generation.
        materials=Array(meshes.size) { mesh -> Array(3) { material -> bake(meshes[mesh],sources[material]) } }
    }

    private class Surface(val pixels: IntArray, val width: Int, val height: Int) {
        fun sample(u: Float,v: Float): Int {
            fun mirror(value: Float,size: Int): Int {
                val repeat=value-floor(value/2f)*2f
                return ((if(repeat<=1f) repeat else 2f-repeat)*(size-1)).toInt().coerceIn(0,size-1)
            }
            return pixels[mirror(v,height)*width+mirror(u,width)]
        }
    }

    private fun bake(mesh: RockMesh, source: Surface): Material {
        val columns=ceil(sqrt(mesh.faceCount.toFloat())).toInt()
        val width=columns*TILE;val height=((mesh.faceCount+columns-1)/columns)*TILE
        val pixels=IntArray(width*height) { Color.BLACK }
        val scale=if(mesh.fragment) .40f else .85f
        val offsetU=.8f+(mesh.variant*.271f)%1f;val offsetV=.8f+(mesh.variant*.173f)%1f
        for(f in 0 until mesh.faceCount) {
            val a=mesh.indices[f*3]*3;val b=mesh.indices[f*3+1]*3;val c=mesh.indices[f*3+2]*3
            val tileX=(f%columns)*TILE;val tileY=(f/columns)*TILE
            for(py in 0 until TILE) for(px in 0 until TILE) {
                // One-pixel extrapolated gutters keep bilinear filtering off other faces.
                val wb=(px-1).toFloat()/(TILE-3);val wc=(py-1).toFloat()/(TILE-3);val wa=1f-wb-wc
                val x=mesh.positions[a]*wa+mesh.positions[b]*wb+mesh.positions[c]*wc
                val y=mesh.positions[a+1]*wa+mesh.positions[b+1]*wb+mesh.positions[c+1]*wc
                val z=mesh.positions[a+2]*wa+mesh.positions[b+2]*wb+mesh.positions[c+2]*wc
                var nx=abs(mesh.normals[a]*wa+mesh.normals[b]*wb+mesh.normals[c]*wc)
                var ny=abs(mesh.normals[a+1]*wa+mesh.normals[b+1]*wb+mesh.normals[c+1]*wc)
                var nz=abs(mesh.normals[a+2]*wa+mesh.normals[b+2]*wb+mesh.normals[c+2]*wc)
                nx*=nx;nx*=nx;ny*=ny;ny*=ny;nz*=nz;nz*=nz
                val total=(nx+ny+nz).coerceAtLeast(.00001f);nx/=total;ny/=total;nz/=total
                val yz=source.sample(z*scale+offsetU,y*scale+offsetV)
                val xz=source.sample(x*scale+offsetU,z*scale+offsetV)
                val xy=source.sample(x*scale+offsetU,y*scale+offsetV)
                pixels[(tileY+py)*width+tileX+px]=Color.rgb(
                    (Color.red(yz)*nx+Color.red(xz)*ny+Color.red(xy)*nz).toInt().coerceIn(0,255),
                    (Color.green(yz)*nx+Color.green(xz)*ny+Color.green(xy)*nz).toInt().coerceIn(0,255),
                    (Color.blue(yz)*nx+Color.blue(xz)*ny+Color.blue(xy)*nz).toInt().coerceIn(0,255))
            }
        }
        val bitmap=Bitmap.createBitmap(pixels,width,height,Bitmap.Config.ARGB_8888)
        return Material(BitmapShader(bitmap,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP),width,height)
    }

    fun draw(canvas: Canvas, asteroid: Asteroid, materialIndex: Int): Boolean = drawMesh(canvas,
        (if (asteroid.radius < 28f) smallMeshes else detailedMeshes)[asteroid.tumbleVariant.mod(RockMesh.VARIANTS)],
        asteroid.position.x, asteroid.position.y, asteroid.radius, asteroid.tumbleX, asteroid.tumbleY,
        asteroid.rotation, asteroid.type, materialIndex, 1f)

    fun drawFragment(canvas: Canvas, fragment: RockFragment) {
        drawMesh(canvas, shardMeshes[fragment.variant.mod(RockMesh.VARIANTS)], fragment.x, fragment.y,
            fragment.radius, fragment.rollX, fragment.rollY, fragment.rotation, fragment.type,
            when (fragment.type) { AsteroidType.METAL, AsteroidType.MAGNETIC -> 1; AsteroidType.VOLATILE, AsteroidType.TRAIL -> 2; else -> 0 },
            ((fragment.lifetime - fragment.age) / .5f).coerceIn(0f, 1f))
    }

    private fun sortFaces(order: IntArray, depth: FloatArray, low: Int, high: Int) {
        var a=low;var b=high;val pivot=depth[order[(low+high)/2]]
        while (a<=b) {
            while (depth[order[a]]<pivot) a++
            while (depth[order[b]]>pivot) b--
            if (a<=b) { val t=order[a];order[a]=order[b];order[b]=t;a++;b-- }
        }
        if (low<b) sortFaces(order,depth,low,b)
        if (a<high) sortFaces(order,depth,a,high)
    }

    private fun drawMesh(canvas: Canvas, mesh: RockMesh, x: Float, y: Float, radius: Float,
        rollX: Float, rollY: Float, rotation: Float, type: AsteroidType, materialIndex: Int, alpha: Float): Boolean {
        val textures = materials ?: return false
        if (radius <= 0f || alpha <= 0f) return true
        @Suppress("DEPRECATION")
        if (canvas.quickReject(x-radius,y-radius,x+radius,y+radius,Canvas.EdgeType.AA)) return true
        val d=scratch.get()!!
        val cx=cos(rollX);val sx=sin(rollX);val cy=cos(rollY);val sy=sin(rollY);val cz=cos(rotation);val sz=sin(rotation)
        val m00=cz*cy;val m01=cz*sy*sx-sz*cx;val m02=cz*sy*cx+sz*sx
        val m10=sz*cy;val m11=sz*sy*sx+cz*cx;val m12=sz*sy*cx-cz*sx
        val m20=-sy;val m21=cy*sx;val m22=cy*cx
        // Rotate the world light into object space once, rather than rotating every normal.
        val lx=-.35f*m00-.45f*m10+.82f*m20
        val ly=-.35f*m01-.45f*m11+.82f*m21
        val lz=-.35f*m02-.45f*m12+.82f*m22
        val tint=when(type) {
            AsteroidType.ICE -> 0xFFAAE1FF.toInt(); AsteroidType.METAL -> 0xFFE4F0FF.toInt()
            AsteroidType.VOLATILE -> 0xFFFFB080.toInt(); AsteroidType.MAGNETIC -> 0xFFBDA8F2.toInt()
            AsteroidType.TRAIL -> 0xFFA2DBAC.toInt(); else -> Color.WHITE
        }
        for (i in 0 until mesh.vertexCount) {
            val p=i*3;val vx=mesh.positions[p];val vy=mesh.positions[p+1];val vz=mesh.positions[p+2]
            val depth=m20*vx+m21*vy+m22*vz;d.depth[i]=depth
            val perspective=4f/(4f-depth)
            d.projected[i*2]=x+(m00*vx+m01*vy+m02*vz)*radius*perspective
            d.projected[i*2+1]=y+(m10*vx+m11*vy+m12*vz)*radius*perspective
            d.light[i]=max(0f,mesh.normals[p]*lx+mesh.normals[p+1]*ly+mesh.normals[p+2]*lz)
        }
        var visible=0
        for (f in 0 until mesh.faceCount) {
            val a=mesh.indices[f*3];val b=mesh.indices[f*3+1];val c=mesh.indices[f*3+2]
            val ax=d.projected[a*2];val ay=d.projected[a*2+1]
            val area=(d.projected[b*2]-ax)*(d.projected[c*2+1]-ay)-(d.projected[b*2+1]-ay)*(d.projected[c*2]-ax)
            if (area>.00001f) { d.faces[visible++]=f;d.faceDepth[f]=(d.depth[a]+d.depth[b]+d.depth[c])/3f }
        }
        if (visible==0) return true
        // Dented models are not convex: paint rear faces first to avoid faces showing through.
        sortFaces(d.faces,d.faceDepth,0,visible-1)
        val slot=mesh.variant+if(mesh.fragment) RockMesh.VARIANTS*2 else if(mesh.faceCount==320) 0 else RockMesh.VARIANTS
        val material=textures[slot][materialIndex.coerceIn(0,2)];var count=0
        val columns=material.width/TILE
        for (j in 0 until visible) {
            val f=d.faces[j];val n=f*3
            val faceLight=max(0f,mesh.faceNormals[n]*lx+mesh.faceNormals[n+1]*ly+mesh.faceNormals[n+2]*lz)
            for (corner in 0..2) {
                val index=mesh.indices[n+corner]
                d.vertices[count*2]=d.projected[index*2];d.vertices[count*2+1]=d.projected[index*2+1]
                d.texture[count*2]=(f%columns)*TILE+1.5f+if(corner==1) TILE-3f else 0f
                d.texture[count*2+1]=(f/columns)*TILE+1.5f+if(corner==2) TILE-3f else 0f
                val light=if(mesh.fragment) faceLight else faceLight*.65f+d.light[index]*.35f
                val shade=(.28f+.85f*light+if(mesh.fragment && f<2) .18f else 0f).coerceIn(0f,1f)
                d.colors[count]=Color.rgb((Color.red(tint)*shade).toInt(),(Color.green(tint)*shade).toInt(),(Color.blue(tint)*shade).toInt())
                count++
            }
        }
        d.paint.shader=material.shader;d.paint.alpha=(alpha*255).toInt()
        canvas.drawVertices(Canvas.VertexMode.TRIANGLES,count*2,d.vertices,0,d.texture,0,d.colors,0,null,0,0,d.paint)
        return true
    }
}
