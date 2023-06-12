package com.rittmann.mediacontrol.create

class Circle(val x: Float, val y: Float, val radius: Float) {

    fun intersect(
        x: Float, y: Float
    ): Boolean {
        return (x - this.x) * (x - this.x) + (y - this.y) * (y - this.y) <= this.radius * this.radius
    }
}