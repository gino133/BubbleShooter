package com.example.bubbleshooter

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class Bubble(
    var x: Float,
    var y: Float,
    var radius: Float = 40f,
    var color: Int = Color.RED,
    var isFalling: Boolean = false
) {
    var velocityX: Float = 0f
    var velocityY: Float = 0f
    var isMoving: Boolean = false
    
    fun draw(canvas: Canvas, paint: Paint) {
        // Main circle
        paint.color = color
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x, y, radius, paint)
        
        // Highlight
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawCircle(x - radius * 0.3f, y - radius * 0.3f, radius * 0.2f, paint)
        
        // Border
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawCircle(x, y, radius, paint)
    }
    
    fun update() {
        if (isMoving) {
            x += velocityX
            y += velocityY
        }
        if (isFalling) {
            y += 5f
        }
    }
    
    fun isOutOfScreen(screenHeight: Int): Boolean {
        return y > screenHeight + radius
    }
}