package com.username051203.textingstory.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.username051203.textingstory.data.model.Story
import com.username051203.textingstory.databinding.ItemStoryBinding
import java.text.SimpleDateFormat
import java.util.*

class StoryListAdapter(
    private val onStoryClick: (Story) -> Unit,
    private val onStoryLongClick: (Story) -> Unit
) : ListAdapter<Story, StoryListAdapter.StoryViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val binding = ItemStoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StoryViewHolder(private val binding: ItemStoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(story: Story) {
            binding.tvStoryTitle.text = story.title
            val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
            binding.tvStoryDate.text = sdf.format(Date(story.updatedAt))
            binding.root.setOnClickListener { onStoryClick(story) }
            binding.root.setOnLongClickListener { onStoryLongClick(story); true }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Story>() {
            override fun areItemsTheSame(oldItem: Story, newItem: Story) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Story, newItem: Story) = oldItem == newItem
        }
    }
}
