package com.example.projectthree.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectthree.R
import com.example.projectthree.model.Lane
import com.example.projectthree.model.TimelineSlot

class FrontPagerAdapter(
    private var topLane: MutableList<TimelineSlot>,
    private var bottomLane: MutableList<TimelineSlot>,
    private val onSlotClickTop: (Int) -> Unit,
    private val onSlotClickBottom: (Int) -> Unit
) : RecyclerView.Adapter<FrontPagerAdapter.PageViewHolder>() {

    class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recyclerView: RecyclerView = itemView.findViewById(R.id.frontTimelineRecyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_front_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val isTop = position == 0
        val slots = if (isTop) topLane else bottomLane
        val lane = if (isTop) Lane.TOP else Lane.BOTTOM
        val adapter = TimelineAdapter(slots.toMutableList()) { index ->
            if (isTop) onSlotClickTop(index) else onSlotClickBottom(index)
        }
        holder.recyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.recyclerView.adapter = adapter
    }

    override fun getItemCount(): Int = 2

    fun updatePages(newTop: List<TimelineSlot>, newBottom: List<TimelineSlot>) {
        topLane.clear(); topLane.addAll(newTop)
        bottomLane.clear(); bottomLane.addAll(newBottom)
        notifyDataSetChanged()
    }
}

