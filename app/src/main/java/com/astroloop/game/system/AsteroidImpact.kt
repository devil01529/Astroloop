package com.astroloop.game.system

import com.astroloop.game.entity.*
import kotlin.math.*

/** Mass-sensitive arcade impulses; visual rotation and debris inherit the resulting momentum. */
object AsteroidImpact {
    /** Oriented capsule follows the enlarged hull's nose and wings for asteroid contact. */
    fun touchesHull(ship: Ship, asteroid: Asteroid): Boolean {
        val dx=asteroid.position.x-ship.position.x;val dy=asteroid.position.y-ship.position.y
        val cosine=cos(ship.rotation);val sine=sin(ship.rotation)
        val along=dx*cosine+dy*sine;val across=-dx*sine+dy*cosine
        val visual=ship.radius*com.astroloop.game.core.GameConfig.SHIP_VISUAL_SCALE
        val halfWidth=visual*.60f;val segment=visual*.29f
        val nearest=along.coerceIn(-segment,segment)
        val radius=halfWidth+asteroid.radius
        return (along-nearest)*(along-nearest)+across*across<=radius*radius
    }

    fun mass(asteroid: Asteroid): Float = (asteroid.radius * asteroid.radius / 625f).coerceAtLeast(.25f) *
        when(asteroid.type) { AsteroidType.METAL -> 1.8f; AsteroidType.ICE -> .7f; else -> 1f }

    fun projectile(asteroid: Asteroid, projectile: Projectile, damage: Float) {
        if (!asteroid.isActive || damage<=0f) return
        // Stationary area fields already have their own force/slow rules.
        when (projectile.type) {
            ProjectileType.GRAVITY, ProjectileType.MINE, ProjectileType.ORBITER, ProjectileType.LIGHTNING -> return
            else -> Unit
        }
        val speed=projectile.velocity.length()
        val dx=if(speed>.01f) projectile.velocity.x/speed else cos(projectile.rotation)
        val dy=if(speed>.01f) projectile.velocity.y/speed else sin(projectile.rotation)
        val impulse=damage.coerceAtMost(120f)*3.5f/mass(asteroid)
        asteroid.velocity.add(dx*impulse,dy*impulse)
        val velocity=asteroid.velocity.length()
        if(velocity>650f) asteroid.velocity.mul(650f/velocity)
        val leverX=projectile.position.x-asteroid.position.x
        val leverY=projectile.position.y-asteroid.position.y
        val torque=(leverX*dy-leverY*dx)*impulse/(asteroid.radius*asteroid.radius).coerceAtLeast(1f)
        asteroid.rotationSpeed=(asteroid.rotationSpeed+torque*.3f).coerceIn(-3f,3f)
    }

    /** Resolve a low-restitution contact before the asteroid fractures. */
    fun ram(ship: Ship, asteroid: Asteroid) {
        var nx=asteroid.position.x-ship.position.x;var ny=asteroid.position.y-ship.position.y
        var length=sqrt(nx*nx+ny*ny)
        if(length<.001f) {
            nx=ship.velocity.x-asteroid.velocity.x;ny=ship.velocity.y-asteroid.velocity.y
            length=sqrt(nx*nx+ny*ny)
        }
        if(length<.001f) return
        nx/=length;ny/=length
        val approach=(asteroid.velocity.x-ship.velocity.x)*nx+(asteroid.velocity.y-ship.velocity.y)*ny
        if(approach>=0f) return
        val inverseShipMass=1f/6f;val inverseRockMass=1f/mass(asteroid)
        val impulse=-1.12f*approach/(inverseShipMass+inverseRockMass)
        asteroid.velocity.add(nx*impulse*inverseRockMass,ny*impulse*inverseRockMass)
        ship.velocity.add(-nx*impulse*inverseShipMass,-ny*impulse*inverseShipMass)
    }
}
