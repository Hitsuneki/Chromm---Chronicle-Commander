package com.example.projectthree

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectthree.game.GameEngine
import com.example.projectthree.model.Order
import com.example.projectthree.ui.OrderAdapter
import com.example.projectthree.ui.TimelineAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class MainActivity : AppCompatActivity() {
    private lateinit var gameEngine: GameEngine
    private lateinit var timelineAdapter: TimelineAdapter
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var timelineRecyclerView: RecyclerView
    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var resolveButton: Button
    
    private lateinit var hpValue: TextView
    private lateinit var suppliesValue: TextView
    private lateinit var intelValue: TextView
    private lateinit var waveValue: TextView
    private lateinit var backButton: Button
    
    private var selectedOrder: Order? = null
    private var isResolving = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        initializeViews()
        initializeGame()
        setupRecyclerViews()
        updateUI() // Update UI after adapters are initialized
        setupClickListeners()
        
        // Show tutorial on first run
        showTutorialIfNeeded()
    }
    
    private fun initializeViews() {
        timelineRecyclerView = findViewById(R.id.timelineRecyclerView)
        ordersRecyclerView = findViewById(R.id.ordersRecyclerView)
        resolveButton = findViewById(R.id.resolveButton)
        backButton = findViewById(R.id.backButton)
        
        hpValue = findViewById(R.id.hpValue)
        suppliesValue = findViewById(R.id.suppliesValue)
        intelValue = findViewById(R.id.intelValue)
        waveValue = findViewById(R.id.waveValue)
    }
    
    private fun initializeGame() {
        // Get difficulty from intent, default to NORMAL
        val difficultyName = intent.getStringExtra("difficulty") ?: "NORMAL"
        val difficulty = try {
            com.example.projectthree.model.Difficulty.valueOf(difficultyName)
        } catch (e: IllegalArgumentException) {
            com.example.projectthree.model.Difficulty.NORMAL
        }
        
        gameEngine = GameEngine(difficulty)
        // Don't call updateUI() here - adapters aren't initialized yet
    }
    
    private fun setupRecyclerViews() {
        // Timeline RecyclerView
        timelineAdapter = TimelineAdapter(
            gameEngine.timeline.toMutableList(),
            onSlotClick = { slotIndex ->
                handleSlotClick(slotIndex)
            }
        )
        timelineRecyclerView.layoutManager = LinearLayoutManager(this)
        timelineRecyclerView.adapter = timelineAdapter
        
        // Orders RecyclerView
        orderAdapter = OrderAdapter(
            orders = gameEngine.getAvailableOrders(),
            canAfford = { order -> gameEngine.gameState.canAffordOrder(order) },
            onOrderClick = { order ->
                selectedOrder = order
                Toast.makeText(this, "Selected ${order.name}. Tap a timeline slot to place it.", Toast.LENGTH_SHORT).show()
            }
        )
        ordersRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        ordersRecyclerView.adapter = orderAdapter
    }
    
    private fun setupClickListeners() {
        resolveButton.setOnClickListener {
            if (!isResolving) {
                resolveTurns()
            }
        }
        
        backButton.setOnClickListener {
            // Return to home screen
            val intent = android.content.Intent(this, HomeActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }
    
    private fun handleSlotClick(slotIndex: Int) {
        if (isResolving) return
        
        val slot = gameEngine.timeline[slotIndex]
        
        // If slot already has an order, remove it
        if (slot.order != null) {
            gameEngine.removeOrder(slotIndex)
            selectedOrder = null
            updateUI()
            Toast.makeText(this, "Order removed", Toast.LENGTH_SHORT).show()
            return
        }
        
        // If we have a selected order, try to place it
        if (selectedOrder != null) {
            val success = gameEngine.placeOrder(slotIndex, selectedOrder!!)
            if (success) {
                Toast.makeText(this, "${selectedOrder!!.name} placed on turn ${slot.turnNumber}", Toast.LENGTH_SHORT).show()
                selectedOrder = null
                updateUI()
            } else {
                val reason = when {
                    gameEngine.gameState.ordersPlacedThisRound >= gameEngine.gameState.maxOrdersPerRound -> 
                        "Maximum orders per round reached"
                    !gameEngine.gameState.canAffordOrder(selectedOrder!!) -> 
                        "Not enough resources"
                    !slot.canAcceptOrder(selectedOrder!!) -> 
                        "Cannot place this order here"
                    else -> "Cannot place order"
                }
                Toast.makeText(this, reason, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Select an order first, then tap a slot", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun resolveTurns() {
        if (isResolving) return
        isResolving = true
        resolveButton.isEnabled = false
        
        // Animate through each turn
        lifecycleScope.launch(Dispatchers.Main) {
            val results = gameEngine.resolveAllTurns()
            
            // Animate each turn resolution
            for (i in results.indices) {
                // Highlight the current slot
                highlightSlot(i)
                
                // Show result message
                val result = results[i]
                if (result.message.isNotEmpty()) {
                    showFloatingMessage(result.message)
                }
                
                // Animate resource changes
                animateResourceChange(result)
                
                delay(800) // Wait before next turn
            }
            
            // Check game over
            if (gameEngine.isGameOver()) {
                showGameOverDialog()
            } else {
                // Update UI and continue
                updateUI()
                isResolving = false
                resolveButton.isEnabled = true
                Toast.makeText(this@MainActivity, "Wave ${gameEngine.gameState.wave} complete!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun highlightSlot(index: Int) {
        val viewHolder = timelineRecyclerView.findViewHolderForAdapterPosition(index)
        viewHolder?.itemView?.let { view ->
            val animator = ValueAnimator.ofFloat(1.0f, 1.3f, 1.0f)
            animator.duration = 400
            animator.addUpdateListener { animation ->
                val scale = animation.animatedValue as Float
                view.scaleX = scale
                view.scaleY = scale
            }
            animator.start()
        }
    }
    
    private fun showFloatingMessage(message: String) {
        // Simple toast for now - could be enhanced with custom floating view
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun animateResourceChange(result: com.example.projectthree.game.TurnResult) {
        // Animate HP change
        if (result.hpChange != 0) {
            animateTextViewChange(hpValue, result.hpChange, isNegative = result.hpChange < 0)
        }
        
        // Animate Supplies change
        if (result.suppliesChange != 0) {
            animateTextViewChange(suppliesValue, result.suppliesChange, isNegative = false)
        }
        
        // Animate Intel change
        if (result.intelChange != 0) {
            animateTextViewChange(intelValue, result.intelChange, isNegative = false)
        }
    }
    
    private fun animateTextViewChange(textView: TextView, change: Int, isNegative: Boolean) {
        val currentValue = textView.text.toString().toIntOrNull() ?: 0
        val newValue = currentValue + change
        
        val animator = ValueAnimator.ofInt(currentValue, newValue)
        animator.duration = 500
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            textView.text = value.toString()
            
            // Flash color
            val color = if (isNegative) {
                android.graphics.Color.parseColor("#E53935")
            } else {
                android.graphics.Color.parseColor("#4CAF50")
            }
            textView.setTextColor(color)
        }
        
        animator.start()
        
        // Reset color after animation
        Handler(Looper.getMainLooper()).postDelayed({
            val defaultColor = when (textView.id) {
                R.id.hpValue -> android.graphics.Color.parseColor("#E53935")
                R.id.suppliesValue -> android.graphics.Color.parseColor("#FF9800")
                R.id.intelValue -> android.graphics.Color.parseColor("#2196F3")
                else -> android.graphics.Color.parseColor("#000000")
            }
            textView.setTextColor(defaultColor)
        }, 500)
    }
    
    private fun updateUI() {
        // Update stats
        hpValue.text = gameEngine.gameState.hp.toString()
        suppliesValue.text = gameEngine.gameState.supplies.toString()
        intelValue.text = gameEngine.gameState.intel.toString()
        waveValue.text = gameEngine.gameState.wave.toString()
        
        // Update timeline
        timelineAdapter.updateSlots(gameEngine.timeline)
        
        // Update orders (refresh affordability)
        orderAdapter.notifyDataSetChanged()
        
        // Update resolve button (no limit on orders)
        resolveButton.text = "RESOLVE TURNS"
    }
    
    private fun showGameOverDialog() {
        AlertDialog.Builder(this)
            .setTitle("Game Over")
            .setMessage("You survived ${gameEngine.gameState.wave - 1} waves!\n\nFinal Stats:\nHP: ${gameEngine.gameState.hp}\nSupplies: ${gameEngine.gameState.supplies}\nIntel: ${gameEngine.gameState.intel}")
            .setPositiveButton("Play Again") { _, _ ->
                restartGame()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun restartGame() {
        gameEngine = GameEngine()
        selectedOrder = null
        isResolving = false
        updateUI()
        resolveButton.isEnabled = true
    }
    
    private fun showTutorialIfNeeded() {
        // Simple tutorial - could be enhanced with SharedPreferences to track first run
        Handler(Looper.getMainLooper()).postDelayed({
            AlertDialog.Builder(this)
                .setTitle("Welcome to Timeline Commander!")
                .setMessage(
                    """
                    You can see future events on the timeline.
                    
                    • Drag orders from the bottom to timeline slots
                    • Defend blocks enemy attacks
                    • Harvest increases supply gains
                    • Delay pushes events later
                    • Scout reveals fog events (costs Intel)
                    
                    Press RESOLVE to watch turns play out.
                    Survive as long as possible!
                    """.trimIndent()
                )
                .setPositiveButton("Got it!", null)
                .show()
        }, 500)
    }
}
