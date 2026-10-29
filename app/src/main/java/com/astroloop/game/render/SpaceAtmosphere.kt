package com.astroloop.game.render

import android.graphics.*

/** Restrained, static gas clouds: cached shaders, two draws, no frame-by-frame bitmap allocation. */
class SpaceAtmosphere {
    private val blue = RadialGradient(.24f,.38f,.75f,intArrayOf(0xFF122333.toInt(),0x00122333),null,Shader.TileMode.CLAMP)
    private val violet = RadialGradient(.86f,.74f,.62f,intArrayOf(0xFF191426.toInt(),0x00191426),null,Shader.TileMode.CLAMP)
    private val red = RadialGradient(.5f,.45f,.8f,intArrayOf(0xFF2A1117.toInt(),0x002A1117),null,Shader.TileMode.CLAMP)
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    fun render(canvas:Canvas,width:Float,height:Float,boss:Boolean) {
        if(width<=0f || height<=0f)return
        val save=canvas.save()
        canvas.scale(width,height)
        paint.shader=if(boss)red else blue;canvas.drawRect(0f,0f,1f,1f,paint)
        if(!boss){paint.shader=violet;canvas.drawRect(0f,0f,1f,1f,paint)}
        canvas.restoreToCount(save)
    }
}

/** A steady edge warning avoids flashing and never obscures the central aiming area. */
class HullWarning {
    private val shader=LinearGradient(0f,0f,0f,1f,
        intArrayOf(0x88E35360.toInt(),0x00E35360,0x00E35360,0x66E35360),
        floatArrayOf(0f,.075f,.925f,1f),Shader.TileMode.CLAMP)
    private val paint=Paint().apply { this.shader=this@HullWarning.shader }
    fun render(canvas:Canvas,width:Float,height:Float,health:Float,maxHealth:Float,opacity:Float) {
        if(maxHealth<=0f || health<=0f || health/maxHealth>.25f || width<=0f || height<=0f)return
        paint.alpha=(opacity.coerceIn(0f,1f)*255).toInt()
        val save=canvas.save();canvas.scale(width,height)
        canvas.drawRect(0f,0f,1f,1f,paint)
        canvas.restoreToCount(save)
    }
}
