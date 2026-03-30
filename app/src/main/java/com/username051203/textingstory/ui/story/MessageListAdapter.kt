package com.username051203.textingstory.ui.story

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.username051203.textingstory.data.model.Character
import com.username051203.textingstory.data.model.Message
import com.username051203.textingstory.databinding.ItemMessageBubbleBinding
import com.username051203.textingstory.databinding.ItemMessageCommentBinding

class MessageListAdapter(
    private val onAddBelow: (Message) -> Unit,
    private val onDelete: (Message) -> Unit,
    private val onEdit: (Message) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var messages: List<Message> = emptyList()
    private var charMap: Map<Long, Character> = emptyMap()
    private var editMode = false

    companion object {
        const val TYPE_BUBBLE = 0
        const val TYPE_COMMENT = 1
    }

    fun submitData(msgs: List<Message>, chars: List<Character>) {
        charMap = chars.associateBy { it.id }
        messages = msgs
        notifyDataSetChanged()
    }

    fun setEditMode(enabled: Boolean) {
        editMode = enabled
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) =
        if (messages[position].type == "COMMENT") TYPE_COMMENT else TYPE_BUBBLE

    override fun getItemCount() = messages.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_COMMENT) {
            val b = ItemMessageCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            CommentViewHolder(b)
        } else {
            val b = ItemMessageBubbleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            BubbleViewHolder(b)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        val char = msg.characterId?.let { charMap[it] }
        when (holder) {
            is BubbleViewHolder -> holder.bind(msg, char)
            is CommentViewHolder -> holder.bind(msg)
        }
    }

    inner class BubbleViewHolder(private val b: ItemMessageBubbleBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(msg: Message, char: Character?) {
            val isRight = char?.side == "RIGHT"
            b.tvMessage.text = msg.text

            // Positioning
            if (isRight) {
                b.layoutLeft.visibility = View.GONE
                b.layoutRight.visibility = View.VISIBLE
                b.tvMessageRight.text = msg.text
                val color = try { Color.parseColor(char?.bubbleColor ?: "#00B2FF") } catch (e: Exception) { Color.parseColor("#00B2FF") }
                val textColor = try { Color.parseColor(char?.textColor ?: "#FFFFFF") } catch (e: Exception) { Color.WHITE }
                setRoundedBackground(b.tvMessageRight, color)
                b.tvMessageRight.setTextColor(textColor)
                b.tvMessage.visibility = View.GONE
                // Avatar right
                b.ivAvatarRight.visibility = View.VISIBLE
                loadAvatar(b.ivAvatarRight, char)
            } else {
                b.layoutRight.visibility = View.GONE
                b.layoutLeft.visibility = View.VISIBLE
                b.tvMessage.text = msg.text
                b.tvMessage.visibility = View.VISIBLE
                val color = try { Color.parseColor(char?.bubbleColor ?: "#FF6FD8") } catch (e: Exception) { Color.parseColor("#FF6FD8") }
                val textColor = try { Color.parseColor(char?.textColor ?: "#FFFFFF") } catch (e: Exception) { Color.WHITE }
                setRoundedBackground(b.tvMessage, color)
                b.tvMessage.setTextColor(textColor)
                b.ivAvatarLeft.visibility = View.VISIBLE
                loadAvatar(b.ivAvatarLeft, char)
            }

            // Edit/delete controls
            val showControls = editMode
            b.btnAdd.visibility = if (showControls) View.VISIBLE else View.GONE
            b.btnDelete.visibility = if (showControls) View.VISIBLE else View.GONE
            b.btnAdd.setOnClickListener { onAddBelow(msg) }
            b.btnDelete.setOnClickListener { onDelete(msg) }
            b.root.setOnLongClickListener { onEdit(msg); true }
        }

        private fun loadAvatar(iv: android.widget.ImageView, char: Character?) {
            if (char?.avatarPath != null) {
                Glide.with(iv).load(char.avatarPath).circleCrop().into(iv)
            } else {
                val color = try { Color.parseColor(char?.bubbleColor ?: "#888888") } catch (e: Exception) { Color.GRAY }
                iv.setImageDrawable(createCircleDrawable(color))
            }
        }

        private fun createCircleDrawable(color: Int): android.graphics.drawable.Drawable {
            val gd = GradientDrawable()
            gd.shape = GradientDrawable.OVAL
            gd.setColor(color)
            return gd
        }

        private fun setRoundedBackground(view: android.widget.TextView, color: Int) {
            val gd = GradientDrawable()
            gd.setColor(color)
            gd.cornerRadius = 40f
            view.background = gd
        }
    }

    inner class CommentViewHolder(private val b: ItemMessageCommentBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(msg: Message) {
            b.tvComment.text = msg.text
            b.btnAdd.visibility = if (editMode) View.VISIBLE else View.GONE
            b.btnDelete.visibility = if (editMode) View.VISIBLE else View.GONE
            b.btnAdd.setOnClickListener { onAddBelow(msg) }
            b.btnDelete.setOnClickListener { onDelete(msg) }
            b.root.setOnLongClickListener { onEdit(msg); true }
        }
    }
}
