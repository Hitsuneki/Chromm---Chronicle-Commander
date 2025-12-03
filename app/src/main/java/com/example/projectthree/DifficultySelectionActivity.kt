package com.example.projectthree

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.projectthree.model.Difficulty

class DifficultySelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_difficulty_selection)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        val easyButton: Button = findViewById(R.id.easyButton)
        val normalButton: Button = findViewById(R.id.normalButton)
        val hardButton: Button = findViewById(R.id.hardButton)
        val rtsButton: Button = findViewById(R.id.rtsButton)
        val backButton: Button = findViewById(R.id.backButton)
        
        easyButton.setOnClickListener {
            startGame(Difficulty.EASY)
        }
        
        normalButton.setOnClickListener {
            startGame(Difficulty.NORMAL)
        }
        
        hardButton.setOnClickListener {
            startGame(Difficulty.HARD)
        }
        
        rtsButton.setOnClickListener {
            startGame(Difficulty.RTS)
        }
        
        backButton.setOnClickListener {
            finish()
        }
    }
    
    private fun startGame(difficulty: Difficulty) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("difficulty", difficulty.name)
        startActivity(intent)
        finish()
    }
}

