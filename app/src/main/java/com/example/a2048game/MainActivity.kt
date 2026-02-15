package com.example.a2048game

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity(), GameBoardView.ScoreUpdateListener {
    private lateinit var gameBoardView: GameBoardView
    private lateinit var scoreTextView: TextView
    private lateinit var highScoreTextView: TextView
    private lateinit var newGameButton: Button
    private var highScore = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        gameBoardView = findViewById(R.id.gameBoardView)
        scoreTextView = findViewById(R.id.scoreTextView)
        highScoreTextView = findViewById(R.id.highScoreTextView)
        newGameButton = findViewById(R.id.newGameButton)

        // Set up score update listener
        gameBoardView.setScoreUpdateListener(this)

        // Load high score
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        highScore = sharedPref.getInt("high_score", 0)
        updateHighScore(highScore)

        // Set up new game button
        newGameButton.setOnClickListener {
            showNewGameDialog()
        }

        // Update score when game state changes
        updateScore(0)
    }

    override fun onScoreUpdate(newScore: Int, pointsAdded: Int) {
        // Animate score text color when points are added
        val colorFrom = ContextCompat.getColor(this, R.color.text_dark)
        val colorTo = ContextCompat.getColor(this, R.color.tile_2048)
        
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo, colorFrom)
        colorAnimation.duration = 500
        colorAnimation.interpolator = AccelerateDecelerateInterpolator()
        
        colorAnimation.addUpdateListener { animator ->
            scoreTextView.setTextColor(animator.animatedValue as Int)
        }
        
        colorAnimation.start()
        
        // Update score with animation
        val scoreAnimation = ValueAnimator.ofInt(scoreTextView.text.toString().substringAfter(": ").toInt(), newScore)
        scoreAnimation.duration = 300
        scoreAnimation.interpolator = AccelerateDecelerateInterpolator()
        
        scoreAnimation.addUpdateListener { animator ->
            scoreTextView.text = "Score: ${animator.animatedValue}"
        }
        
        scoreAnimation.start()
        
        // Check and update high score
        if (newScore > highScore) {
            highScore = newScore
            updateHighScore(highScore)
            saveHighScore()
        }
    }

    private fun updateScore(score: Int) {
        scoreTextView.text = "Score: $score"
    }

    private fun updateHighScore(score: Int) {
        highScoreTextView.text = "Best: $score"
    }

    private fun saveHighScore() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("high_score", highScore)
            apply()
        }
    }

    private fun showNewGameDialog() {
        AlertDialog.Builder(this)
            .setTitle("New Game")
            .setMessage("Are you sure you want to start a new game?")
            .setPositiveButton("Yes") { _, _ ->
                gameBoardView.resetGame()
                updateScore(0)
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onPause() {
        super.onPause()
        saveHighScore()
    }
}