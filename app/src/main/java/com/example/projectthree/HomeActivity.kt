package com.example.projectthree

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        val playButton: Button = findViewById(R.id.playButton)
        val howToPlayButton: Button = findViewById(R.id.howToPlayButton)
        
        playButton.setOnClickListener {
            val intent = Intent(this, DifficultySelectionActivity::class.java)
            startActivity(intent)
        }
        
        howToPlayButton.setOnClickListener {
            val intent = Intent(this, HowToPlayActivity::class.java)
            startActivity(intent)
        }
    }
}

