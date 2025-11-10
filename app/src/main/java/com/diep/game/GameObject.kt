package com.diep.game

import android.graphics.Canvas
import kotlin.math.sqrt

abstract class GameObject(
    var x: Float,
    var y: Float,
    var radius: Float
) {
    var velocityX: Float = 0f
    var velocityY: Float = 0f
    var isAlive: Boolean = true

    abstract fun update(deltaTime: Float)
    abstract fun draw(canvas: Canvas, cameraX: Float, cameraY: Float)

    fun distanceTo(other: GameObject): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    fun collidesWith(other: GameObject): Boolean {
        return distanceTo(other) < (radius + other.radius)
    }
}
