package com.example.projectthree.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.projectthree.R
import com.example.projectthree.model.Order

class OrderAdapter(
    private val orders: List<Order>,
    private val canAfford: (Order) -> Boolean,
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {
    
    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderIcon: TextView = itemView.findViewById(R.id.orderIcon)
        val orderName: TextView = itemView.findViewById(R.id.orderName)
        val orderCost: TextView = itemView.findViewById(R.id.orderCost)
        val orderCard: View = itemView.findViewById(R.id.orderCard)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_card, parent, false)
        return OrderViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        
        holder.orderIcon.text = order.icon
        holder.orderName.text = order.name
        
        // Show costs
        val costParts = mutableListOf<String>()
        if (order.suppliesCost > 0) {
            costParts.add("📦${order.suppliesCost}")
        }
        if (order.intelCost > 0) {
            costParts.add("🔍${order.intelCost}")
        }
        holder.orderCost.text = if (costParts.isEmpty()) {
            "Free"
        } else {
            costParts.joinToString(" ")
        }
        
        // Dim if can't afford
        val affordable = canAfford(order)
        holder.orderCard.alpha = if (affordable) 1.0f else 0.5f
        holder.orderCard.isEnabled = affordable
        
        holder.itemView.setOnClickListener {
            if (affordable) {
                onOrderClick(order)
            }
        }
    }
    
    override fun getItemCount(): Int = orders.size
}

