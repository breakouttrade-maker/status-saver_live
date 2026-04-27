package com.breakout.statussaver.ui.adapter

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.breakout.statussaver.R
import com.breakout.statussaver.databinding.ItemStatusBinding

class StatusAdapter(
    private val onClick: (DocumentFile) -> Unit,
    private val onLongClick: () -> Unit
) : ListAdapter<DocumentFile, StatusAdapter.StatusViewHolder>(DiffCallback) {

    private val selectedItems = mutableSetOf<Int>()
    var isSelectMode = false
        private set

    fun getSelectedItems(): List<DocumentFile> {
        return selectedItems.mapNotNull { pos -> 
            if (pos in 0 until itemCount) getItem(pos) else null 
        }
    }

    fun clearSelection() {
        selectedItems.clear()
        isSelectMode = false
        notifyDataSetChanged()
    }

    inner class StatusViewHolder(val binding: ItemStatusBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusViewHolder {
        val binding = ItemStatusBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StatusViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int) {
        val file = getItem(position)
        val isSelected = selectedItems.contains(position)

        // BULLETPROOF VISUAL: Put a blue tint directly on the image itself
        if (isSelected) {
            holder.binding.ivThumbnail.setColorFilter(Color.parseColor("#663399FF"), PorterDuff.Mode.SRC_ATOP)
            holder.binding.ivPlayIcon.setColorFilter(Color.WHITE)
        } else {
            holder.binding.ivThumbnail.clearColorFilter()
            holder.binding.ivPlayIcon.clearColorFilter()
        }

        Glide.with(holder.itemView.context)
            .load(file.uri)
            .placeholder(R.color.card_bg)
            .centerCrop()
            .into(holder.binding.ivThumbnail)

        holder.binding.ivPlayIcon.visibility =
            if (file.type?.startsWith("video/") == true) View.VISIBLE else View.GONE

        holder.binding.root.setOnClickListener {
            if (isSelectMode) {
                toggleSelection(position)
            } else {
                onClick(file)
            }
        }

        holder.binding.root.setOnLongClickListener {
            isSelectMode = true
            toggleSelection(position)
            Toast.makeText(holder.itemView.context, "Item Selected!", Toast.LENGTH_SHORT).show()
            onLongClick()
            true
        }
    }

    private fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
            if (selectedItems.isEmpty()) isSelectMode = false
        } else {
            selectedItems.add(position)
        }
        notifyItemChanged(position)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<DocumentFile>() {
        override fun areItemsTheSame(a: DocumentFile, b: DocumentFile) = a.uri == b.uri
        override fun areContentsTheSame(a: DocumentFile, b: DocumentFile) = a.uri == b.uri
    }
}
