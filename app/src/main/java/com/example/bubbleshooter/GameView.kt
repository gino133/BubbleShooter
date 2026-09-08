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
    private var aimAngle = 270f
    private var score = 0
    private var gameRunning = false
    private var gameOver = false
    private val handler = Handler(Looper.getMainLooper())
    private var isMoving = false
    
    companion object {
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
    
    private fun initBubbles() {
        bubbles.clear()
        val startX = BUBBLE_RADIUS + 10
        val startY = BUBBLE_RADIUS + 50
        
        for (row in 0..6) {
            val offsetX = if (row % 2 == 0) 0f else BUBBLE_RADIUS
            for (col in 0..7) {
                if (row < 4 || (row < 6 && col % 2 == 0)) {
                    val color = COLORS.random()
                    val x = startX + col * (BUBBLE_RADIUS * 2) + offsetX
                    val y = startY + row * (BUBBLE_RADIUS * 1.7f)
                    bubbles.add(Bubble(x, y, BUBBLE_RADIUS, color))
                }
            }
        }
    }
    
    private fun createCurrentBubble() {
        if (!gameRunning || gameOver) return
        currentBubble = Bubble(
            (screenWidth / 2).toFloat(),
            (screenHeight - 150).toFloat(),
            BUBBLE_RADIUS,
            COLORS.random()
        )
        isMoving = false
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Background
        canvas.drawColor(Color.rgb(20, 20, 40))
        
        // Draw all bubbles
        for (bubble in bubbles) {
            bubble.draw(canvas, paint)
        }
        
        // Draw current bubble
        if (currentBubble != null && gameRunning && !gameOver) {
            // Aim line
            if (!isMoving) {
                paint.color = Color.argb(100, 255, 255, 255)
                paint.strokeWidth = 3f
                val endX = currentBubble!!.x + cos(Math.toRadians(aimAngle.toDouble())).toFloat() * 500
                val endY = currentBubble!!.y + sin(Math.toRadians(aimAngle.toDouble())).toFloat() * 500
                canvas.drawLine(currentBubble!!.x, currentBubble!!.y, endX, endY, paint)
            }
            
            // Draw current bubble
            currentBubble!!.draw(canvas, paint)
        }
        
        // Score
        paint.color = Color.WHITE
        paint.textSize = 40f
        paint.textAlign = Paint.Align.LEFT
        paint.style = Paint.Style.FILL
        canvas.drawText("Score: $score", 20f, 50f, paint)
        
        // Instructions
        if (gameRunning && !gameOver) {
            paint.textSize = 20f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Drag to aim, release to shoot", screenWidth / 2f, screenHeight - 40f, paint)
        }
        
        // Game Over
        if (gameOver) {
            paint.color = Color.WHITE
            paint.textSize = 50f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Game Over!", screenWidth / 2f, screenHeight / 2f - 50f, paint)
            paint.textSize = 30f
            canvas.drawText("Score: $score", screenWidth / 2f, screenHeight / 2f + 30f, paint)
            paint.textSize = 25f
            canvas.drawText("Tap to restart", screenWidth / 2f, screenHeight / 2f + 100f, paint)
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameOver) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                restartGame()
            }
            return true
        }
        
        if (!gameRunning) {
            startGame()
            return true
        }
        
        if (isMoving) return true
        
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                currentBubble?.let { bubble ->
                    val dx = event.x - bubble.x
                    val dy = event.y - bubble.y
                    aimAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    // Chỉ bắn lên (180-270 độ)
                    if (aimAngle > 270 || aimAngle < 180) {
                        aimAngle = min(270f, max(180f, aimAngle))
                    }
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                shootBubble()
            }
        }
        return true
    }
    
    private fun shootBubble() {
        val bubble = currentBubble ?: return
        if (isMoving) return
        
        isMoving = true
        val speed = 25f
        bubble.velocityX = cos(Math.toRadians(aimAngle.toDouble())).toFloat() * speed
        bubble.velocityY = sin(Math.toRadians(aimAngle.toDouble())).toFloat() * speed
        bubble.isMoving = true
        bubbles.add(bubble)
        currentBubble = null
        
        // Check collision after each frame
        handler.postDelayed({
            updateBubbleMovement()
        }, 20)
        
        createCurrentBubble()
        invalidate()
    }
    
    private fun updateBubbleMovement() {
        if (!gameRunning || gameOver) return
        
        val bubble = bubbles.lastOrNull { it.isMoving }
        if (bubble == null) {
            isMoving = false
            return
        }
        
        // Update position
        bubble.x += bubble.velocityX
        bubble.y += bubble.velocityY
        
        // Check wall collision
        if (bubble.x - BUBBLE_RADIUS < 0) {
            bubble.x = BUBBLE_RADIUS
            bubble.velocityX = -bubble.velocityX
        } else if (bubble.x + BUBBLE_RADIUS > screenWidth) {
            bubble.x = screenWidth - BUBBLE_RADIUS
            bubble.velocityX = -bubble.velocityX
        }
        
        // Check top collision
        if (bubble.y - BUBBLE_RADIUS < 0) {
            bubble.y = BUBBLE_RADIUS
            bubble.isMoving = false
            isMoving = false
            checkMatch(bubble)
            invalidate()
            return
        }
        
        // Check collision with other bubbles
        for (other in bubbles) {
            if (other == bubble || !other.isMoving) continue
            val dx = bubble.x - other.x
            val dy = bubble.y - other.y
            val dist = sqrt(dx*dx + dy*dy)
            
            if (dist < BUBBLE_RADIUS * 2) {
                // Stick to the bubble
                bubble.x = other.x + dx / dist * BUBBLE_RADIUS * 2
                bubble.y = other.y + dy / dist * BUBBLE_RADIUS * 2
                bubble.isMoving = false
                isMoving = false
                checkMatch(bubble)
                invalidate()
                return
            }
        }
        
        // Check bottom (Game Over)
        if (bubble.y + BUBBLE_RADIUS > screenHeight - 100) {
            bubble.isMoving = false
            isMoving = false
            gameOver()
            invalidate()
            return
        }
        
        invalidate()
        handler.postDelayed({
            updateBubbleMovement()
        }, 20)
    }
    
    private fun checkMatch(bubble: Bubble) {
        val matched = mutableListOf<Bubble>()
        findMatches(bubble, matched)
        
        if (matched.size >= 3) {
            bubbles.removeAll(matched)
            score += matched.size * 10
            
            // Check win
            if (bubbles.isEmpty()) {
                gameOver = true
                gameRunning = false
                invalidate()
                return
            }
        }
        
        // Check for floating bubbles
        checkFloatingBubbles()
        invalidate()
    }
    
    private fun findMatches(bubble: Bubble, result: MutableList<Bubble>) {
        if (bubble in result) return
        result.add(bubble)
        
        for (other in bubbles) {
            if (other in result) continue
            if (other.color == bubble.color && 
                distance(bubble.x, bubble.y, other.x, other.y) < BUBBLE_RADIUS * 2.5) {
                findMatches(other, result)
            }
        }
    }
    
    private fun checkFloatingBubbles() {
        val connected = mutableSetOf<Bubble>()
        // Find all bubbles connected to top row
        for (bubble in bubbles) {
            if (bubble.y < 150) {
                findConnected(bubble, connected)
            }
        }
        
        // Remove floating bubbles
        val toRemove = mutableListOf<Bubble>()
        for (bubble in bubbles) {
            if (bubble !in connected) {
                toRemove.add(bubble)
                score += 5
            }
        }
        bubbles.removeAll(toRemove)
        
        if (bubbles.isEmpty()) {
            gameOver = true
            gameRunning = false
        }
    }
    
    private fun findConnected(bubble: Bubble, result: MutableSet<Bubble>) {
        if (bubble in result) return
        result.add(bubble)
        
        for (other in bubbles) {
            if (other in result) continue
            if (distance(bubble.x, bubble.y, other.x, other.y) < BUBBLE_RADIUS * 2.5) {
                findConnected(other, result)
            }
        }
    }
    
    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx*dx + dy*dy)
    }
    
    private fun startGame() {
        initBubbles()
        createCurrentBubble()
        gameRunning = true
        gameOver = false
        score = 0
        isMoving = false
        invalidate()
    }
    
    private fun gameOver() {
        gameRunning = false
        gameOver = true
        isMoving = false
        invalidate()
    }
    
    private fun restartGame() {
        startGame()
    }
}