package com.auraflow.domain.physics

import kotlin.math.abs
import kotlin.math.sqrt

class Spring(
    position: Float,
    val stiffness: Float = 170f,
    val damping: Float = 14f
) {
    var position: Float = position
        private set
    var velocity: Float = 0f
        private set
    var target: Float = position

    val dampingRatio: Float get() = damping / (2f * sqrt(stiffness))

    fun step(dt: Float) {
        val acceleration = stiffness * (target - position) - damping * velocity
        velocity += acceleration * dt
        position += velocity * dt
    }

    fun isAtRest(epsilon: Float = 1e-3f): Boolean =
        abs(target - position) < epsilon && abs(velocity) < epsilon
}
