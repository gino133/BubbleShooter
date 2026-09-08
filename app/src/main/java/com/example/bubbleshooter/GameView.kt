package com.example.bubbleshooter

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class GameView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint()
    private val bubbles = mutableListOf<Bubble>()
    private var currentBubble: Bubble? = null
    private var aimAngle = 270f // 0 = phải, 90 = xuống, 180 = trái, 270 = lên
    private var score = 0
    private var gameRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    
    companion object {
        private const val ROWS = 8
        private const val COLS = 8
        private const val BUBBLE_RADIUS = 40f
        private val COLORS = intArrayOf(
            Color.RED, Color.BLUE, Color.GREEN, 
            Color.YELLOW, Color.MAGENTA, Color.CYAN
        )
    }
    
    private val screenWidth: Int
        get() = width
    private val screenHeight: Int
        get() = height
    
    init {
        setBackgroundColor(Color.BLACK)
        startGame()
    }
    
    /**
     * Khởi tạo bong bóng ban đầu
     */
    private fun initBubbles() {
        bubbles.clear()
        val startX = BUBBLE_RADIUS + 10
        val startY = BUBBLE_RADIUS + 50
        
        for (row in 0 until ROWS) {
            val offsetX = if (row % 2 == 0) 0f else BUBBLE_RADIUS
            for (col in 0 until COLS) {
                // Tạo hình tam giác bong bóng
                if (row < 4 || (row < 6 && col % 2 == 0)) {
                    val color = COLORS.random()
                    val x = startX + col * (BUBBLE_RADIUS * 2) + offsetX
                    val y = startY + row * (BUBBLE_RADIUS * 1.7f)
                    bubbles.add(Bubble(x, y, BUBBLE_RADIUS, color))
                }
            }
        }
    }
    
    /**
     * Tạo bong bóng hiện tại để bắn
     */
    private fun createCurrentBubble() {
        currentBubble = Bubble(
            screenWidth / 2f,
            screenHeight - 150f,
            BUBBLE_RADIUS,
            COLORS.random()
        )
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Vẽ nền gradient
        val gradient = LinearGradient(
            0f, 0f, 0f, screenHeight.toFloat(),
            Color.rgb(10, 20, 50), Color.rgb(30, 10, 40),
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), paint)
        paint.shader = null
        
        // Vẽ đường kẻ
        paint.color = Color.argb(50, 255, 255, 255)
        paint.strokeWidth = 2f
        canvas.drawLine(0f, screenHeight - 100f, screenWidth.toFloat(), screenHeight - 100f, paint)
        
        // Vẽ tất cả bong bóng
        for (bubble in bubbles) {
            bubble.draw(canvas, paint)
        }
        
        // Vẽ bong bóng đang bắn và đường ngắm
        if (currentBubble != null && gameRunning) {
            // Đường ngắm
            paint.color = Color.argb(100, 255, 255, 255)
            paint.strokeWidth = 3f
            val endX = currentBubble!!.x + cos(Math.toRadians(aimAngle.toDouble())).toFloat() * 500
            val endY = currentBubble!!.y + sin(Math.toRadians(aimAngle.toDouble())).toFloat() * 500
            canvas.drawLine(currentBubble!!.x, currentBubble!!.y, endX, endY, paint)
            
            // Bong bóng hiện tại
            currentBubble!!.draw(canvas, paint)
        }
        
        // Hiển thị điểm
        paint.color = Color.WHITE
        paint.textSize = 40f
        paint.textAlign = Paint.Align.LEFT
        paint.style = Paint.Style.FILL
        canvas.drawText("Score: $score", 20f, 50f, paint)
        
        // Hướng dẫn
        paint.textSize = 20f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Tap and drag to aim, release to shoot", screenWidth / 2f, screenHeight - 40f, paint)
        
        // Màn hình Game Over
        if (!gameRunning && bubbles.isNotEmpty()) {
            paint.color = Color.WHITE
            paint.textSize = 50f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Game Over!", screenWidth / 2f, screenHeight / 2f - 50f, paint)
            paint.textSize = 30f
            canvas.drawText("Score: $score", screenWidth / 2f, screenHeight / 2f + 30f, paint)
            paint.textSize = 25f
            canvas.drawText("Tap to restart", screenWidth / 2f, screenHeight / 2f + 100f, paint)
        }
        
        // Màn hình chiến thắng
        if (!gameRunning && bubbles.isEmpty()) {
            paint.color = Color.YELLOW
            paint.textSize = 50f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("🎉 You Win! 🎉", screenWidth / 2f, screenHeight / 2f - 50f, paint)
            paint.color = Color.WHITE
            paint.textSize = 30f
            canvas.drawText("Score: $score", screenWidth / 2f, screenHeight / 2f + 30f, paint)
            paint.textSize = 25f
            canvas.drawText("Tap to play again", screenWidth / 2f, screenHeight / 2f + 100f, paint)
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!gameRunning) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                restartGame()
            }
            return true
        }
        
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                // Điều chỉnh góc bắn
                val dx = event.x - currentBubble!!.x
                val dy = event.y - currentBubble!!.y
                aimAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                // Giới hạn góc (chỉ bắn lên trên)
                if (aimAngle > 270 || aimAngle < 180) {
                    aimAngle = min(270f, max(180f, aimAngle))
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                shootBubble()
            }
        }
        return true
    }
    
    /**
     * Bắn bong bóng
     */
    private fun shootBubble() {
        currentBubble?.let { bubble ->
            bubble.isMoving = true
            val speed = 20f
            bubble.velocityX = cos(Math.toRadians(aimAngle.toDouble())).toFloat() * speed
            bubble.velocityY = sin(Math.toRadians(aimAngle.toDouble())).toFloat() * speed
            bubbles.add(bubble)
            checkCollision(bubble)
        }
        createCurrentBubble()
        invalidate()
    }
    
    /**
     * Kiểm tra va chạm
     */
    private fun checkCollision(bubble: Bubble) {
        handler.postDelayed({
            if (!bubble.isMoving) return@postDelayed
            
            // Kiểm tra va chạm với bong bóng khác
            for (other in bubbles) {
                if (other == bubble) continue
                val dx = bubble.x - other.x
                val dy = bubble.y - other.y
                val distance = sqrt(dx*dx + dy*dy)
                
                if (distance < BUBBLE_RADIUS * 2) {
                    bubble.isMoving = false
                    // Dính vào bong bóng
                    bubble.x = other.x + dx / distance * BUBBLE_RADIUS * 2
                    bubble.y = other.y + dy / distance * BUBBLE_RADIUS * 2
                    removeBubbles(bubble)
                    return@postDelayed
                }
            }
            
            // Kiểm tra va chạm với tường (trái/phải)
            if (bubble.x - BUBBLE_RADIUS < 0) {
                bubble.x = BUBBLE_RADIUS
                bubble.velocityX = -bubble.velocityX
            } else if (bubble.x + BUBBLE_RADIUS > screenWidth) {
                bubble.x = screenWidth - BUBBLE_RADIUS
                bubble.velocityX = -bubble.velocityX
            }
            
            // Kiểm tra va chạm với đỉnh
            if (bubble.y - BUBBLE_RADIUS < 0) {
                bubble.y = BUBBLE_RADIUS
                bubble.isMoving = false
                removeBubbles(bubble)
            }
            
            // Game Over nếu chạm đáy
            if (bubble.y + BUBBLE_RADIUS > screenHeight - 150) {
                gameOver()
            }
            
            invalidate()
            checkCollision(bubble)
        }, 50)
    }
    
    /**
     * Xóa các bong bóng cùng màu
     */
    private fun removeBubbles(bubble: Bubble) {
        val sameColor = mutableListOf<Bubble>()
        findSameColor(bubble, sameColor)
        
        if (sameColor.size >= 3) {
            bubbles.removeAll(sameColor)
            score += sameColor.size * 10
        }
        
        // Kiểm tra chiến thắng
        if (bubbles.isEmpty()) {
            gameRunning = false
            invalidate()
        }
    }
    
    /**
     * Tìm bong bóng cùng màu (đệ quy)
     */
    private fun findSameColor(bubble: Bubble, result: MutableList<Bubble>) {
        if (bubble in result) return
        result.add(bubble)
        
        for (other in bubbles) {
            if (other in result) continue
            if (other.color == bubble.color && 
                distance(bubble.x, bubble.y, other.x, other.y) < BUBBLE_RADIUS * 2.5) {
                findSameColor(other, result)
            }
        }
    }
    
    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx*dx + dy*dy)
    }
    
    /**
     * Bắt đầu game
     */
    private fun startGame() {
        initBubbles()
        createCurrentBubble()
        gameRunning = true
        score = 0
        
        updateRunnable = object : Runnable {
            override fun run() {
                updateGame()
                handler.postDelayed(this, 30)
            }
        }
        handler.post(updateRunnable!!)
    }
    
    /**
     * Cập nhật game loop
     */
    private fun updateGame() {
        if (!gameRunning) return
        
        for (bubble in bubbles) {
            bubble.update()
        }
        
        // Xóa bong bóng ra khỏi màn hình
        val iterator = bubbles.iterator()
        while (iterator.hasNext()) {
            val bubble = iterator.next()
            if (bubble.isOutOfScreen(screenHeight)) {
                iterator.remove()
            }
        }
        
        invalidate()
    }
    
    private fun gameOver() {
        gameRunning = false
        invalidate()
    }
    
    private fun restartGame() {
        initBubbles()
        createCurrentBubble()
        gameRunning = true
        score = 0
        invalidate()
    }
}