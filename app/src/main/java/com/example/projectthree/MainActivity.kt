package com.example.projectthree

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectthree.game.GameEngine
import com.example.projectthree.model.*
import com.example.projectthree.ui.OrderAdapter
import com.example.projectthree.ui.TimelineAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class MainActivity : AppCompatActivity() {
    private lateinit var gameEngine: GameEngine
    private var topLaneAdapter: TimelineAdapter? = null
    private var bottomLaneAdapter: TimelineAdapter? = null
    private var rtsAdapter: com.example.projectthree.ui.RTSTimelineAdapter? = null
    private var frontPagerAdapter: com.example.projectthree.ui.FrontPagerAdapter? = null
    private lateinit var orderAdapter: OrderAdapter
    private var topLaneRecyclerView: RecyclerView? = null
    private var bottomLaneRecyclerView: RecyclerView? = null
    private lateinit var rtsTimelineRecyclerView: RecyclerView
    private lateinit var frontPagerRecyclerView: RecyclerView
    private lateinit var front1Tab: TextView
    private lateinit var front2Tab: TextView
    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var resolveButton: Button
    private lateinit var tacticalPauseButton: Button
    private lateinit var airstrikeButton: Button
    
    private lateinit var hpValue: TextView
    private lateinit var armorValue: TextView
    private lateinit var suppliesValue: TextView
    private lateinit var intelValue: TextView
    private lateinit var waveValue: TextView
    private lateinit var threatValue: TextView
    private lateinit var backButton: Button
    private lateinit var timerText: TextView
    private lateinit var timerProgress: ProgressBar
    private lateinit var endButton: Button
    private lateinit var tooltipText: TextView
    
    private var selectedOrder: Order? = null
    private var isResolving = false
    private var planningTimer: CountDownTimer? = null
    private val planningTimeSeconds = 60L // 1 minute
    private var tacticalPauseActive = false
    private var airstrikeMode = false  // When true, next slot click will use Airstrike
    private val tacticalPausePower = CommanderPower.TacticalPause
    private val airstrikePower = CommanderPower.Airstrike
    private var isRTSMode = false
    private var activeFrontPage = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize game first to check difficulty
        initializeGame()
        
        // Set layout based on difficulty
        if (isRTSMode) {
            setContentView(R.layout.activity_main_rts)
        } else {
            setContentView(R.layout.activity_main)
        }
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        initializeViews()
        setupRecyclerViews()
        updateUI() // Update UI after adapters are initialized
        setupClickListeners()
        startPlanningTimer(planningTimeSeconds)
        updateCommanderPowersUI()
        
        // Show tutorial on first run
        showTutorialIfNeeded()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        planningTimer?.cancel()
    }
    
    private fun initializeViews() {
        if (isRTSMode) {
            frontPagerRecyclerView = findViewById(R.id.frontPagerRecyclerView)
            tooltipText = findViewById(R.id.tooltipText)
            front1Tab = findViewById(R.id.front1Tab)
            front2Tab = findViewById(R.id.front2Tab)
        } else {
            frontPagerRecyclerView = findViewById(R.id.frontPagerRecyclerView)
            front1Tab = findViewById(R.id.front1Tab)
            front2Tab = findViewById(R.id.front2Tab)
        }
        
        ordersRecyclerView = findViewById(R.id.ordersRecyclerView)
        resolveButton = findViewById(R.id.resolveButton)
        backButton = findViewById(R.id.backButton)
        tacticalPauseButton = findViewById(R.id.tacticalPauseButton)
        airstrikeButton = findViewById(R.id.airstrikeButton)
        
        hpValue = findViewById(R.id.hpValue)
        armorValue = findViewById(R.id.armorValue)
        suppliesValue = findViewById(R.id.suppliesValue)
        intelValue = findViewById(R.id.intelValue)
        waveValue = findViewById(R.id.waveValue)
        threatValue = findViewById(R.id.threatValue)
        timerText = findViewById(R.id.timerText)
        timerProgress = findViewById(R.id.timerProgress)
        endButton = findViewById(R.id.endButton)
        
        // Hide commander powers in classic mode
        if (!isRTSMode) {
            tacticalPauseButton.visibility = View.GONE
            airstrikeButton.visibility = View.GONE
        }
    }
    
    private fun initializeGame() {
        // Get difficulty from intent, default to NORMAL
        val difficultyName = intent.getStringExtra("difficulty") ?: "NORMAL"
        val difficulty = try {
            com.example.projectthree.model.Difficulty.valueOf(difficultyName)
        } catch (e: IllegalArgumentException) {
            com.example.projectthree.model.Difficulty.NORMAL
        }
        
        isRTSMode = com.example.projectthree.model.DifficultyConfig.isRTSMode(difficulty)
        gameEngine = GameEngine(difficulty)
        // Don't call updateUI() here - adapters aren't initialized yet
    }
    
    private fun setupRecyclerViews() {
        if (isRTSMode) {
            // RTS Mode: Swipeable fronts
            frontPagerAdapter = com.example.projectthree.ui.FrontPagerAdapter(
                topLane = gameEngine.topLane.toMutableList(),
                bottomLane = gameEngine.bottomLane.toMutableList(),
                onSlotClickTop = { slotIndex -> handleSlotClick(Lane.TOP, slotIndex) },
                onSlotClickBottom = { slotIndex -> handleSlotClick(Lane.BOTTOM, slotIndex) }
            )
            frontPagerRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            frontPagerRecyclerView.adapter = frontPagerAdapter
            val snapHelper = androidx.recyclerview.widget.PagerSnapHelper()
            snapHelper.attachToRecyclerView(frontPagerRecyclerView)
        } else {
            // Classic Mode: Swipeable fronts too
            frontPagerAdapter = com.example.projectthree.ui.FrontPagerAdapter(
                topLane = gameEngine.topLane.toMutableList(),
                bottomLane = gameEngine.bottomLane.toMutableList(),
                onSlotClickTop = { slotIndex -> handleSlotClick(Lane.TOP, slotIndex) },
                onSlotClickBottom = { slotIndex -> handleSlotClick(Lane.BOTTOM, slotIndex) }
            )
            frontPagerRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            frontPagerRecyclerView.adapter = frontPagerAdapter
            val snapHelper = androidx.recyclerview.widget.PagerSnapHelper()
            snapHelper.attachToRecyclerView(frontPagerRecyclerView)
        }
        
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
        
        tacticalPauseButton.setOnClickListener {
            useTacticalPause()
        }
        
        airstrikeButton.setOnClickListener {
            if (!airstrikePower.isReady()) {
                Toast.makeText(this, "Airstrike on cooldown (${airstrikePower.currentCooldown} waves)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Enable airstrike targeting mode
            airstrikeMode = true
            Toast.makeText(this, "Airstrike ready! Tap an Enemy Attack slot to target it.", Toast.LENGTH_SHORT).show()
        }
        
        endButton.setOnClickListener {
            if (!isResolving) {
                resolveTurns()
            }
        }

        if (true) {
            front1Tab.setOnClickListener {
                frontPagerRecyclerView.smoothScrollToPosition(0)
                highlightActiveFront(0)
            }
            front2Tab.setOnClickListener {
                frontPagerRecyclerView.smoothScrollToPosition(1)
                highlightActiveFront(1)
            }
            frontPagerRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val lm = recyclerView.layoutManager as LinearLayoutManager
                        val pos = lm.findFirstCompletelyVisibleItemPosition().takeIf { it != RecyclerView.NO_POSITION }
                            ?: lm.findFirstVisibleItemPosition()
                        highlightActiveFront(pos)
                    }
                }
            })
            highlightActiveFront(0)
        }
    }
    
    private fun useTacticalPause() {
        if (!tacticalPausePower.isReady()) {
            Toast.makeText(this, "Tactical Pause on cooldown (${tacticalPausePower.currentCooldown} waves)", Toast.LENGTH_SHORT).show()
            return
        }
        
        tacticalPausePower.use()
        tacticalPauseActive = true
        gameEngine.tacticalPauseActive = true
        
        // Extend timer by 10 seconds and slow it down (2x slower)
        planningTimer?.cancel()
        startPlanningTimer((planningTimeSeconds + GameConfig.TACTICAL_PAUSE_EXTENSION_SECONDS) * 2)
        
        Toast.makeText(this, "Tactical Pause activated! Timer extended.", Toast.LENGTH_SHORT).show()
        updateCommanderPowersUI()
    }
    
    private fun useAirstrike(lane: Lane, slotIndex: Int) {
        val success = gameEngine.useAirstrike(lane, slotIndex)
        if (success) {
            airstrikePower.use()
            Toast.makeText(this, "Airstrike! Enemy Attack cancelled.", Toast.LENGTH_SHORT).show()
            updateUI()
            updateCommanderPowersUI()
        }
    }
    
    private fun updateCommanderPowersUI() {
        // Update Tactical Pause button
        if (tacticalPausePower.isReady()) {
            tacticalPauseButton.text = "⏸️ Tactical Pause"
            tacticalPauseButton.alpha = 1.0f
        } else {
            tacticalPauseButton.text = "⏸️ Cooldown: ${tacticalPausePower.currentCooldown}"
            tacticalPauseButton.alpha = 0.5f
        }
        
        // Update Airstrike button
        if (airstrikePower.isReady()) {
            airstrikeButton.text = "✈️ Airstrike"
            airstrikeButton.alpha = 1.0f
        } else {
            airstrikeButton.text = "✈️ Cooldown: ${airstrikePower.currentCooldown}"
            airstrikeButton.alpha = 0.5f
        }
    }
    
    private fun startPlanningTimer(seconds: Long) {
        planningTimer?.cancel()
        
        val totalMillis = seconds * 1000
        timerProgress.max = totalMillis.toInt()
        timerProgress.progress = totalMillis.toInt()
        
        val tickInterval = if (tacticalPauseActive) 200L else 100L  // Slower updates during tactical pause
        
        planningTimer = object : CountDownTimer(totalMillis, tickInterval) {
            override fun onTick(millisUntilFinished: Long) {
                val totalSeconds = (millisUntilFinished / 1000).toInt()
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                timerText.text = if (minutes > 0) {
                    String.format("%d:%02d", minutes, seconds)
                } else {
                    "${seconds}s"
                }
                timerProgress.progress = millisUntilFinished.toInt()
                
                // Flash red when time is running out
                if (totalSeconds <= 5 && !tacticalPauseActive) {
                    timerText.setTextColor(android.graphics.Color.parseColor("#E53935"))
                } else {
                    timerText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                }
            }
            
            override fun onFinish() {
                timerText.text = "0:00"
                timerProgress.progress = 0
                // Auto-resolve when timer ends
                if (!isResolving) {
                    resolveTurns()
                }
            }
        }.start()
    }
    
    private fun handleSlotClick(lane: Lane, slotIndex: Int) {
        if (isResolving) return
        
        val slot = gameEngine.getSlot(lane, slotIndex) ?: return
        
        // Show tooltip in RTS mode
        if (isRTSMode) {
            showTooltip(lane, slotIndex, slot)
        }
        
        // Check if Airstrike mode is active
        if (airstrikeMode) {
            val event = slot.getDisplayEvent()
            if (event is Event.EnemyAttack || event is Event.BossRaid) {
                useAirstrike(lane, slotIndex)
                airstrikeMode = false
                return
            } else {
                Toast.makeText(this, "Airstrike can only target Enemy Attacks", Toast.LENGTH_SHORT).show()
                airstrikeMode = false
                return
            }
        }
        
        // If slot already has an order, remove it
        if (slot.order != null) {
            gameEngine.removeOrder(lane, slotIndex)
            selectedOrder = null
            updateUI()
            Toast.makeText(this, "Order removed", Toast.LENGTH_SHORT).show()
            return
        }
        
        // If we have a selected order, try to place it
        if (selectedOrder != null) {
            val success = gameEngine.placeOrder(lane, slotIndex, selectedOrder!!)
            if (success) {
                Toast.makeText(this, "${selectedOrder!!.name} placed on ${lane.name.lowercase()} lane", Toast.LENGTH_SHORT).show()
                selectedOrder = null
                updateUI()
            } else {
                val reason = when {
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
        planningTimer?.cancel()
        resolveButton.isEnabled = false
        
        // Animate through each turn
        lifecycleScope.launch(Dispatchers.Main) {
            val results = gameEngine.resolveAllTurns()
            
            // Skip per-turn popups; accumulate summary for the whole wave
            val totalDamageTaken = results.sumOf { if (it.hpChange < 0) -it.hpChange else 0 }
            val totalSuppliesGained = results.sumOf { if (it.suppliesChange > 0) it.suppliesChange else 0 }
            val totalIntelGained = results.sumOf { if (it.intelChange > 0) it.intelChange else 0 }
            
            // Optional: animate resource updates progressively
            for (result in results) {
                animateResourceChange(result)
                delay(300)
            }
            
            // Reduce commander power cooldowns
            tacticalPausePower.reduceCooldown()
            airstrikePower.reduceCooldown()
            
            // Check game over
            if (gameEngine.isGameOver()) {
                showGameOverDialog()
            } else {
                // Update UI and start next planning phase
                updateUI()
                isResolving = false
                tacticalPauseActive = false
                gameEngine.tacticalPauseActive = false
                
                // Start new planning timer
                startPlanningTimer(planningTimeSeconds)
                updateCommanderPowersUI()
                
                // Show compiled summary after wave computation
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Wave Summary")
                    .setMessage("Damage taken: $totalDamageTaken\nSupplies gained: $totalSuppliesGained\nIntel gained: $totalIntelGained\n\nNext wave")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    
    private fun highlightSlot(recyclerView: RecyclerView, index: Int) {
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(index)
        viewHolder?.itemView?.let { view: View ->
            val animator = ValueAnimator.ofFloat(1.0f, 1.3f, 1.0f)
            animator.duration = 400
            animator.addUpdateListener { animation: ValueAnimator ->
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
        val laneList = if (activeFrontPage == 0) gameEngine.topLane else gameEngine.bottomLane
        val currentArmor = laneList.firstOrNull { it.armorBonus > 0 }?.armorBonus ?: 0
        val plannedFortifyBonus = laneList.indexOfFirst { it.order is Order.Fortify }.let { idx ->
            if (idx >= 0) Order.Fortify.armorBonus else 0
        }
        armorValue.text = maxOf(currentArmor, plannedFortifyBonus).toString()
        suppliesValue.text = gameEngine.gameState.supplies.toString()
        intelValue.text = gameEngine.gameState.intel.toString()
        waveValue.text = gameEngine.gameState.wave.toString()
        val threat = gameEngine.calculateThreat()
        val stars = "★".repeat(threat) + "☆".repeat(5 - threat)
        threatValue.text = stars
        
        // Update timelines
        frontPagerAdapter?.updatePages(gameEngine.topLane, gameEngine.bottomLane)
        
        // Update orders (refresh affordability)
        orderAdapter.notifyDataSetChanged()
        
        // Update resolve button (no limit on orders)
        resolveButton.text = "RESOLVE TURNS"
    }

    private fun highlightActiveFront(position: Int) {
        val activeColor = android.graphics.Color.parseColor("#FFFFFF")
        val inactiveColor = android.graphics.Color.parseColor("#CCCCCC")
        activeFrontPage = position
        front1Tab.setTextColor(if (position == 0) activeColor else inactiveColor)
        front2Tab.setTextColor(if (position == 1) activeColor else inactiveColor)
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
    
    private fun showTooltip(lane: Lane, slotIndex: Int, slot: TimelineSlot) {
        if (!isRTSMode) return
        
        val event = slot.getDisplayEvent()
        val eventDetails = when (event) {
            is Event.EnemyAttack -> "Attack (${event.damage} damage unless Defended)"
            is Event.BossRaid -> "Boss Raid (${event.damage} damage, Defend reduces by 50%)"
            is Event.SupplyDrop -> "Supply Drop (Receive ${event.baseAmount} supplies)"
            is Event.FieldHospital -> "Field Hospital (Restore ${event.healAmount} HP)"
            is Event.Fog -> if (slot.isRevealed) {
                when (val hidden = event.hiddenEvent) {
                    is Event.EnemyAttack -> "Fog: Attack (${hidden.damage} damage)"
                    is Event.BossRaid -> "Fog: Boss Raid (${hidden.damage} damage)"
                    else -> "Fog (Revealed)"
                }
            } else {
                "Fog (Hidden event - use Scout/Analyze to reveal)"
            }
            is Event.DelayField -> "Delay Field (Shifts events by ${event.shiftAmount} turn)"
        }
        
        val orderText = if (slot.order != null) {
            " | Order: ${slot.order!!.name}"
        } else {
            ""
        }
        
        tooltipText.text = "${lane.name} Lane - Turn ${slot.turnNumber}: $eventDetails$orderText"
        tooltipText.visibility = View.VISIBLE
        
        // Hide tooltip after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            tooltipText.visibility = View.GONE
        }, 3000)
    }
    
    private fun showTutorialIfNeeded() {
        // Simple tutorial - could be enhanced with SharedPreferences to track first run
        Handler(Looper.getMainLooper()).postDelayed({
            val message = if (isRTSMode) {
                """
                Welcome to RTS Mode!
                
                Swipe between Front 1 and Front 2 timelines.
                The HUD (HP, Supplies, Intel, Wave, Timer) is shared.
                
                • Use Defend on the current front
                • Use commander powers (Tactical Pause, Airstrike)
                • Tap slots to see details
                
                Press RESOLVE to watch turns play out.
                Survive as long as possible!
                """.trimIndent()
            } else {
                """
                Welcome to Timeline Commander!
                
                You can see future events on the timeline.
                
                • Drag orders from the bottom to timeline slots
                • Defend blocks enemy attacks
                • Harvest increases supply gains
                • Delay pushes events later
                • Scout reveals fog events (costs Intel)
                
                Press RESOLVE to watch turns play out.
                Survive as long as possible!
                """.trimIndent()
            }
            
            AlertDialog.Builder(this)
                .setTitle("Welcome to Timeline Commander!")
                .setMessage(message)
                .setPositiveButton("Got it!", null)
                .show()
        }, 500)
    }
}
