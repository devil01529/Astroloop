package com.astroloop.game.system

import com.astroloop.game.entity.*
import org.junit.Assert.*
import org.junit.Test

class AsteroidImpactTest {
    @Test fun `asteroid contact follows the larger nose as the hull rotates`() {
        val ship=Ship().apply {position.zero();rotation=0f}
        val rock=Asteroid().apply {radius=10f;position.set(45f,0f)}
        assertFalse(ship.collidesWith(rock));assertTrue(AsteroidImpact.touchesHull(ship,rock))
        ship.rotation=kotlin.math.PI.toFloat()/2f
        assertFalse(AsteroidImpact.touchesHull(ship,rock))
        rock.position.set(0f,45f);assertTrue(AsteroidImpact.touchesHull(ship,rock))
    }
    private fun shot()=Projectile().apply { type=ProjectileType.BULLET;velocity.set(900f,0f);position.set(-20f,-15f) }
    @Test fun `weapon momentum opposes incoming motion and smaller rocks respond more`() {
        val small=Asteroid().apply {radius=25f;velocity.set(-30f,0f)}
        val large=Asteroid().apply {radius=50f;velocity.set(-30f,0f)}
        AsteroidImpact.projectile(small,shot(),20f);AsteroidImpact.projectile(large,shot(),20f)
        assertTrue(small.velocity.x>0f);assertTrue(large.velocity.x> -30f)
        assertEquals((small.velocity.x+30)/4,large.velocity.x+30,.001f)
        assertTrue(small.rotationSpeed>0f)
    }
    @Test fun `ram conserves momentum and dissipates energy along contact normal`() {
        val ship=Ship().apply {velocity.set(200f,20f)}
        val rock=Asteroid().apply {radius=50f;position.set(60f,0f);velocity.set(-80f,10f)}
        val mass=AsteroidImpact.mass(rock)
        val momentum=ship.velocity.x*6+rock.velocity.x*mass
        val energy=ship.velocity.lengthSquared()*6+rock.velocity.lengthSquared()*mass
        AsteroidImpact.ram(ship,rock)
        assertEquals(momentum,ship.velocity.x*6+rock.velocity.x*mass,.01f)
        assertTrue(ship.velocity.lengthSquared()*6+rock.velocity.lengthSquared()*mass<energy)
        assertTrue(rock.velocity.x>ship.velocity.x)
        assertEquals(20f,ship.velocity.y,0f);assertEquals(10f,rock.velocity.y,0f)
    }
    @Test fun `separating and stationary contacts add no energy or invalid velocity`() {
        val ship=Ship();val rock=Asteroid()
        AsteroidImpact.ram(ship,rock)
        assertEquals(0f,ship.velocity.length(),0f);assertEquals(0f,rock.velocity.length(),0f)
        rock.position.set(50f,0f);rock.velocity.set(100f,0f)
        AsteroidImpact.ram(ship,rock)
        assertEquals(100f,rock.velocity.x,0f)
    }
    @Test fun `cosmetic shards inherit velocity in opposite pairs and expire within capacity`() {
        val parent=Asteroid().apply { radius=50f;velocity.set(37f,-22f);tumbleVariant=7 }
        val debris=RockDebris();debris.emit(parent)
        val shards=debris.fragments.filter {it.active}
        assertEquals(10,shards.size)
        assertEquals(37f,shards.map {it.vx}.average().toFloat(),.001f)
        assertEquals(-22f,shards.map {it.vy}.average().toFloat(),.001f)
        assertTrue(shards.map {it.variant}.toSet().size>5)
        assertTrue(shards.all {it.radius<parent.radius*.3f})
        val x=shards[0].x;debris.update(0f);assertEquals(x,shards[0].x,0f)
        repeat(100) {debris.emit(parent)}
        assertEquals(RockDebris.CAPACITY,debris.fragments.count {it.active})
        debris.update(2f);assertTrue(debris.fragments.none {it.active})
        debris.emit(parent);debris.clear();assertTrue(debris.fragments.none {it.active})
    }
}
