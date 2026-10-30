package com.astroloop.game.entity

import kotlin.math.*
import kotlin.random.Random

/** Cosmetic fracture piece: deliberately not an Entity, with no health, collisions, or loot. */
class RockFragment {
    var x=0f;var y=0f;var vx=0f;var vy=0f
    var radius=0f;var rotation=0f;var rollX=0f;var rollY=0f
    var spin=0f;var variant=0;var type=AsteroidType.ROCK
    var age=0f;var lifetime=0f
    val active get() = age < lifetime
}

/** Fixed-capacity reuse keeps chain explosions from allocating an unbounded debris field. */
class RockDebris {
    companion object { const val CAPACITY = 144 }
    val fragments: List<RockFragment> = List(CAPACITY) { RockFragment() }
    private var cursor=0

    fun emit(parent: Asteroid) {
        val pairs=when(parent.size) { AsteroidSize.LARGE -> 5; AsteroidSize.MEDIUM -> 4; AsteroidSize.SMALL -> 3 }
        for (pair in 0 until pairs) {
            val angle=Random.nextFloat()*2f*PI.toFloat()
            val size=parent.radius*(.13f+Random.nextFloat()*.16f)
            val speed=75f+Random.nextFloat()*100f
            // Equal opposite pairs preserve the parent's centre-of-mass velocity.
            for (side in 0..1) {
                val sign=if(side==0) 1f else -1f
                val dx=cos(angle)*sign;val dy=sin(angle)*sign
                val f=fragments[cursor];cursor=(cursor+1)%CAPACITY
                f.x=parent.position.x+dx*parent.radius*.24f;f.y=parent.position.y+dy*parent.radius*.24f
                f.vx=parent.velocity.x+dx*speed;f.vy=parent.velocity.y+dy*speed
                f.radius=size;f.rotation=angle+side*PI.toFloat()
                f.rollX=parent.tumbleX+pair*.7f;f.rollY=parent.tumbleY+side*.9f
                f.spin=(1.1f+Random.nextFloat()*2f)*sign
                f.variant=(parent.tumbleVariant+pair*2+side)%Asteroid.SHAPE_VARIANTS
                f.type=parent.type;f.age=0f;f.lifetime=.9f+Random.nextFloat()*.6f
            }
        }
    }

    fun update(deltaTime: Float) {
        for(f in fragments) if(f.active) {
            val dt=deltaTime.coerceAtLeast(0f)
            f.age+=dt;f.x+=f.vx*dt;f.y+=f.vy*dt
            f.rotation+=f.spin*dt;f.rollX+=f.spin*.7f*dt;f.rollY+=f.spin*.4f*dt
        }
    }
    fun clear() { for(f in fragments) f.lifetime=0f;cursor=0 }
}
