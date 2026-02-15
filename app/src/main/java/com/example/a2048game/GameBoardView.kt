package com.example.a2048game

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import kotlin.math.min

class GameBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var cellSize = 0f
    private var padding = 10f
    private val paint = Paint()
    private val board = Array(4) { Array(4) { 0 } }
    private var score = 0
    private var gameOver = false

    // Add score update callback interface
    interface ScoreUpdateListener {
        fun onScoreUpdate(newScore: Int, pointsAdded: Int)
    }

    private var scoreUpdateListener: ScoreUpdateListener? = null

    fun setScoreUpdateListener(listener: ScoreUpdateListener) {
        scoreUpdateListener = listener
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?, // Make e1 nullable
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null){
                return false // handle the case where e1 is null
            }
            val dx = e2.x - e1.x
            val dy = e2.y - e1.y

            if (Math.abs(dx) > Math.abs(dy)) {
                if (dx > 0) {
                    moveRight()
                } else {
                    moveLeft()
                }
            } else {
                if (dy > 0) {
                    moveDown()
                } else {
                    moveUp()
                }
            }
            return true
        }
    })

    init {
        initBoard()
    }

    private fun initBoard() {
        for (i in 0..3) {
            for (j in 0..3) {
                board[i][j] = 0
            }
        }
        addNewTile()
        addNewTile()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = min(measuredWidth, measuredHeight)
        cellSize = (size - 5 * padding) / 4
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBoard(canvas)
        drawTiles(canvas)
    }

    private fun drawBoard(canvas: Canvas) {
        paint.color = 0xFFBBADA0.toInt()
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), 8f, 8f, paint)

        paint.color = 0xFFCDC1B4.toInt()
        for (i in 0..3) {
            for (j in 0..3) {
                val left = padding + j * (cellSize + padding)
                val top = padding + i * (cellSize + padding)
                canvas.drawRoundRect(
                    left, top, left + cellSize, top + cellSize,
                    8f, 8f, paint
                )
            }
        }
    }

    private fun drawTiles(canvas: Canvas) {
        for (i in 0..3) {
            for (j in 0..3) {
                if (board[i][j] != 0) {
                    drawTile(canvas, i, j, board[i][j])
                }
            }
        }
    }

    private fun drawTile(canvas: Canvas, row: Int, col: Int, value: Int) {
        val left = padding + col * (cellSize + padding)
        val top = padding + row * (cellSize + padding)
        
        paint.color = getTileColor(value)
        canvas.drawRoundRect(
            left, top, left + cellSize, top + cellSize,
            8f, 8f, paint
        )

        paint.color = if (value <= 4) 0xFF776E65.toInt() else 0xFFF9F6F2.toInt()
        paint.textSize = cellSize / 3
        paint.textAlign = Paint.Align.CENTER
        
        val text = value.toString()
        val textX = left + cellSize / 2
        val textY = top + cellSize / 2 - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(text, textX, textY, paint)
    }

    private fun getTileColor(value: Int): Int {
        return when (value) {
            2 -> 0xFFEEE4DA.toInt()
            4 -> 0xFFEDE0C8.toInt()
            8 -> 0xFFF2B179.toInt()
            16 -> 0xFFF59563.toInt()
            32 -> 0xFFF67C5F.toInt()
            64 -> 0xFFF65E3B.toInt()
            128 -> 0xFFEDCF72.toInt()
            256 -> 0xFFEDCC61.toInt()
            512 -> 0xFFEDC850.toInt()
            1024 -> 0xFFEDC53F.toInt()
            2048 -> 0xFFEDC22E.toInt()
            else -> 0xFFCDC1B4.toInt()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (gameOver) {
            false
        } else {
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun moveLeft() {
        var moved = false
        for (i in 0..3) {
            var lastMergePos = -1
            for (j in 1..3) {
                if (board[i][j] != 0) {
                    var newPos = j
                    while (newPos > 0 && (board[i][newPos - 1] == 0 || 
                           (board[i][newPos - 1] == board[i][newPos] && lastMergePos != newPos - 1))) {
                        if (board[i][newPos - 1] == 0) {
                            board[i][newPos - 1] = board[i][newPos]
                            board[i][newPos] = 0
                            newPos--
                            moved = true
                        } else if (board[i][newPos - 1] == board[i][newPos]) {
                            val pointsAdded = board[i][newPos - 1] * 2
                            board[i][newPos - 1] *= 2
                            score += pointsAdded
                            scoreUpdateListener?.onScoreUpdate(score, pointsAdded)
                            board[i][newPos] = 0
                            lastMergePos = newPos - 1
                            moved = true
                            break
                        }
                    }
                }
            }
        }
        if (moved) {
            addNewTile()
            invalidate()
        }
    }

    private fun moveRight() {
        var moved = false
        for (i in 0..3) {
            var lastMergePos = 4
            for (j in 2 downTo 0) {
                if (board[i][j] != 0) {
                    var newPos = j
                    while (newPos < 3 && (board[i][newPos + 1] == 0 || 
                           (board[i][newPos + 1] == board[i][newPos] && lastMergePos != newPos + 1))) {
                        if (board[i][newPos + 1] == 0) {
                            board[i][newPos + 1] = board[i][newPos]
                            board[i][newPos] = 0
                            newPos++
                            moved = true
                        } else if (board[i][newPos + 1] == board[i][newPos]) {
                            val pointsAdded = board[i][newPos + 1] * 2
                            board[i][newPos + 1] *= 2
                            score += pointsAdded
                            scoreUpdateListener?.onScoreUpdate(score, pointsAdded)
                            board[i][newPos] = 0
                            lastMergePos = newPos + 1
                            moved = true
                            break
                        }
                    }
                }
            }
        }
        if (moved) {
            addNewTile()
            invalidate()
        }
    }

    private fun moveUp() {
        var moved = false
        for (j in 0..3) {
            var lastMergePos = -1
            for (i in 1..3) {
                if (board[i][j] != 0) {
                    var newPos = i
                    while (newPos > 0 && (board[newPos - 1][j] == 0 || 
                           (board[newPos - 1][j] == board[newPos][j] && lastMergePos != newPos - 1))) {
                        if (board[newPos - 1][j] == 0) {
                            board[newPos - 1][j] = board[newPos][j]
                            board[newPos][j] = 0
                            newPos--
                            moved = true
                        } else if (board[newPos - 1][j] == board[newPos][j]) {
                            val pointsAdded = board[newPos - 1][j] * 2
                            board[newPos - 1][j] *= 2
                            score += pointsAdded
                            scoreUpdateListener?.onScoreUpdate(score, pointsAdded)
                            board[newPos][j] = 0
                            lastMergePos = newPos - 1
                            moved = true
                            break
                        }
                    }
                }
            }
        }
        if (moved) {
            addNewTile()
            invalidate()
        }
    }

    private fun moveDown() {
        var moved = false
        for (j in 0..3) {
            var lastMergePos = 4
            for (i in 2 downTo 0) {
                if (board[i][j] != 0) {
                    var newPos = i
                    while (newPos < 3 && (board[newPos + 1][j] == 0 || 
                           (board[newPos + 1][j] == board[newPos][j] && lastMergePos != newPos + 1))) {
                        if (board[newPos + 1][j] == 0) {
                            board[newPos + 1][j] = board[newPos][j]
                            board[newPos][j] = 0
                            newPos++
                            moved = true
                        } else if (board[newPos + 1][j] == board[newPos][j]) {
                            val pointsAdded = board[newPos + 1][j] * 2
                            board[newPos + 1][j] *= 2
                            score += pointsAdded
                            scoreUpdateListener?.onScoreUpdate(score, pointsAdded)
                            board[newPos][j] = 0
                            lastMergePos = newPos + 1
                            moved = true
                            break
                        }
                    }
                }
            }
        }
        if (moved) {
            addNewTile()
            invalidate()
        }
    }

    private fun addNewTile() {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (i in 0..3) {
            for (j in 0..3) {
                if (board[i][j] == 0) {
                    emptyCells.add(Pair(i, j))
                }
            }
        }
        
        if (emptyCells.isNotEmpty()) {
            val (row, col) = emptyCells.random()
            board[row][col] = if (Math.random() < 0.9) 2 else 4
        }
        
        checkGameOver()
    }

    private fun checkGameOver() {
        gameOver = true
        
        // Check for empty cells
        for (i in 0..3) {
            for (j in 0..3) {
                if (board[i][j] == 0) {
                    gameOver = false
                    return
                }
            }
        }
        
        // Check for possible merges
        for (i in 0..3) {
            for (j in 0..3) {
                if (i < 3 && board[i][j] == board[i + 1][j] ||
                    j < 3 && board[i][j] == board[i][j + 1]) {
                    gameOver = false
                    return
                }
            }
        }

        if (gameOver) {
            showGameOverDialog()
        }
    }

    private fun showGameOverDialog() {
        val dialog = AlertDialog.Builder(context).create()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.game_over_dialog, null)
        
        val finalScoreTextView = dialogView.findViewById<TextView>(R.id.finalScoreTextView)
        val highScoreTextView = dialogView.findViewById<TextView>(R.id.highScoreTextView)
        val newGameButton = dialogView.findViewById<Button>(R.id.newGameButton)
        
        finalScoreTextView.text = "Final Score: $score"
        
        // Get the best score ever from SharedPreferences
        val sharedPref = context.getSharedPreferences("2048_prefs", Context.MODE_PRIVATE)
        val bestScore = sharedPref.getInt("high_score", 0)
        
        // Update the high score if current score is higher
        if (score > bestScore) {
            with(sharedPref.edit()) {
                putInt("high_score", score)
                apply()
            }
            highScoreTextView.text = "Best Score: $score (New Record!)"
        } else {
            highScoreTextView.text = "Best Score: $bestScore"
        }
        
        newGameButton.setOnClickListener {
            resetGame()
            dialog.dismiss()
        }
        
        dialog.setView(dialogView)
        dialog.setCancelable(false)
        dialog.show()
    }

    fun resetGame() {
        score = 0
        gameOver = false
        initBoard()
    }

    fun getScore(): Int = score
} 