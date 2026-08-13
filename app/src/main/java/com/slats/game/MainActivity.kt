package com.slats.game

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(SlatsGameView(this))
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }
}

private class SlatsGameView(context: android.content.Context) : View(context) {
    private enum class Screen {
        Start,
        Game,
        GameOver
    }

    private data class Slat(
        var x: Float,
        var y: Float,
        var size: Float = 0f,
        var growthSpeed: Float = 1f
    )

    private val directions = arrayOf(
        PointF(1f, -1f),
        PointF(-1f, -1f),
        PointF(1f, 1f),
        PointF(-1f, 1f),
        PointF(1f, 0f),
        PointF(-1f, 0f),
        PointF(0f, 1f),
        PointF(0f, -1f)
    )

    private val slatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 2f * resources.displayMetrics.density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 42f * resources.displayMetrics.scaledDensity
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 20f * resources.displayMetrics.scaledDensity
    }

    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT
        textSize = 18f * resources.displayMetrics.scaledDensity
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private var screen = Screen.Start
    private var score = 0
    private var slats = mutableListOf<Slat>()
    private var lastFrameNanos = 0L

    private val density = resources.displayMetrics.density
    private val lineLength = 25f * density
    private val hitDistance = 20f * density
    private val menuTapWidth = 260f * density
    private val menuTapHeight = 88f * density

    init {
        setBackgroundColor(Color.BLACK)
        isFocusable = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)

        when (screen) {
            Screen.Start -> drawStart(canvas)
            Screen.Game -> {
                advanceGame()
                drawGame(canvas)
                postInvalidateOnAnimation()
            }
            Screen.GameOver -> drawGameOver(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) return true

        when (screen) {
            Screen.Start -> {
                if (pointIsInStartTapArea(event.x, event.y)) {
                    startGame()
                }
            }
            Screen.Game -> handleGameTap(event.x, event.y)
            Screen.GameOver -> {
                if (pointIsInRestartTapArea(event.x, event.y)) {
                    screen = Screen.Start
                }
            }
        }

        invalidate()
        return true
    }

    private fun drawStart(canvas: Canvas) {
        val centerY = height / 2f
        canvas.drawText("SLATS", width / 2f, centerY - 36f * density, titlePaint)
        canvas.drawText("Tap to Start", width / 2f, centerY + 24f * density, buttonPaint)
    }

    private fun drawGame(canvas: Canvas) {
        for (slat in slats) {
            drawSlat(canvas, slat)
        }
        canvas.drawText("Score: $score", 16f * density, 32f * density, scorePaint)
    }

    private fun drawGameOver(canvas: Canvas) {
        val centerY = height / 2f
        canvas.drawText("GAME OVER", width / 2f, centerY - 48f * density, titlePaint)
        canvas.drawText("Score: $score", width / 2f, centerY + 8f * density, buttonPaint)
        canvas.drawText("Tap to Restart", width / 2f, centerY + 56f * density, buttonPaint)
    }

    private fun pointIsInStartTapArea(x: Float, y: Float): Boolean {
        val centerY = height / 2f + 24f * density
        return pointIsInCenteredMenuArea(x, y, centerY)
    }

    private fun pointIsInRestartTapArea(x: Float, y: Float): Boolean {
        val centerY = height / 2f + 56f * density
        return pointIsInCenteredMenuArea(x, y, centerY)
    }

    private fun pointIsInCenteredMenuArea(x: Float, y: Float, centerY: Float): Boolean {
        val left = width / 2f - menuTapWidth / 2f
        val right = width / 2f + menuTapWidth / 2f
        val top = centerY - menuTapHeight / 2f
        val bottom = centerY + menuTapHeight / 2f

        return x in left..right && y in top..bottom
    }

    private fun startGame() {
        score = 0
        slats = mutableListOf(
            Slat(
                x = width / 2f,
                y = height / 2f,
                growthSpeed = 1f * density
            )
        )
        lastFrameNanos = 0L
        screen = Screen.Game
    }

    private fun handleGameTap(x: Float, y: Float) {
        val point = PointF(x, y)
        val snapshot = slats.toList()
        for (slat in snapshot) {
            if (touchIsOnWhitePart(point, slat)) {
                slats.add(
                    Slat(
                        x = x,
                        y = y,
                        growthSpeed = currentGrowthSpeed()
                    )
                )
                score += 1
                return
            }
        }
    }

    private fun currentGrowthSpeed(): Float {
        val easing = 1.0 - exp(-score.toDouble() * 0.03)
        return ((1.9 + (1.2 * easing)) * density).toFloat()
    }

    private fun advanceGame() {
        val now = System.nanoTime()
        val deltaFrames =
            if (lastFrameNanos == 0L) 1f else ((now - lastFrameNanos) / 1_000_000_000f) * 60f
        lastFrameNanos = now

        for (slat in slats) {
            slat.size += slat.growthSpeed * deltaFrames
        }

        slats.removeAll { slat ->
            slatIsFullyOffScreen(
                slat = slat,
                screenWidth = width.toFloat(),
                screenHeight = height.toFloat()
            )
        }

        if (slats.isEmpty() && screen == Screen.Game) {
            screen = Screen.GameOver
        }
    }

    private fun slatIsFullyOffScreen(slat: Slat, screenWidth: Float, screenHeight: Float): Boolean {
        for (direction in directions) {
            val startX = slat.x + direction.x * slat.size
            val startY = slat.y + direction.y * slat.size
            val endX = slat.x + direction.x * (slat.size + lineLength)
            val endY = slat.y + direction.y * (slat.size + lineLength)

            val startIsOnScreen =
                startX >= 0f &&
                    startX <= screenWidth &&
                    startY >= 0f &&
                    startY <= screenHeight

            val endIsOnScreen =
                endX >= 0f &&
                    endX <= screenWidth &&
                    endY >= 0f &&
                    endY <= screenHeight

            if (startIsOnScreen || endIsOnScreen) return false
        }

        return true
    }

    private fun drawSlat(canvas: Canvas, slat: Slat) {
        for (direction in directions) {
            canvas.drawLine(
                slat.x + direction.x * slat.size,
                slat.y + direction.y * slat.size,
                slat.x + direction.x * (slat.size + lineLength),
                slat.y + direction.y * (slat.size + lineLength),
                slatPaint
            )
        }
    }

    private fun touchIsOnWhitePart(point: PointF, slat: Slat): Boolean {
        for (direction in directions) {
            val start = PointF(
                slat.x + direction.x * slat.size,
                slat.y + direction.y * slat.size
            )
            val end = PointF(
                slat.x + direction.x * (slat.size + lineLength),
                slat.y + direction.y * (slat.size + lineLength)
            )

            if (pointNearLine(point, start, end)) return true
        }

        return false
    }

    private fun pointNearLine(point: PointF, start: PointF, end: PointF): Boolean {
        val a = point.x - start.x
        val b = point.y - start.y
        val c = end.x - start.x
        val d = end.y - start.y
        val lengthSquared = c * c + d * d

        if (lengthSquared == 0f) return false

        var parameter = (a * c + b * d) / lengthSquared
        parameter = max(0f, min(1f, parameter))

        val closestX = start.x + parameter * c
        val closestY = start.y + parameter * d
        val dx = point.x - closestX
        val dy = point.y - closestY
        val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        return distance < hitDistance
    }
}
