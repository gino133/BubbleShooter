package com.example.bubbleshooter

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/**
 * Class đại diện cho bong bóng trong game
 */
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
    
    /**
     * Vẽ bong bóng lên canvas
     */
    fun draw(canvas: Canvas, paint: Paint) {
        // Vẽ thân bong bóng
        paint.color = color
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x, y, radius, paint)
        
        // Vẽ hiệu ứng ánh sáng
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawCircle(x - radius * 0.3f, y - radius * 0.3f, radius * 0.2f, paint)
        
        // Vẽ viền
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawCircle(x, y, radius, paint)
    }
    
    /**
     * Cập nhật vị trí bong bóng
     */
    fun update() {
        if (isMoving) {
            x += velocityX
            y += velocityY
            velocityY += 0.2f // Trọng lực
        }
        if (isFalling) {
            y += 5f // Rơi xuống
        }
    }
    
    /**
     * Kiểm tra bong bóng đã ra khỏi màn hình chưa
     */
    fun isOutOfScreen(screenHeight: Int): Boolean {
        return y > screenHeight + radius
    }
}