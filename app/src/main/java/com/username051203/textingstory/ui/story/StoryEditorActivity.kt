package com.username051203.textingstory.ui.story

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.username051203.textingstory.R
import com.username051203.textingstory.TextingStoryApp
import com.username051203.textingstory.data.model.Character
import com.username051203.textingstory.data.model.Message
import com.username051203.textingstory.databinding.ActivityStoryEditorBinding
import com.username051203.textingstory.ui.characters.CharactersActivity
import com.username051203.textingstory.ui.export.ExportPreviewActivity
import com.username051203.textingstory.util.PrefsManager
import com.username051203.textingstory.util.SoundManager

class StoryEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_STORY_ID = "extra_story_id"
    }

    private lateinit var binding: ActivityStoryEditorBinding
    private val viewModel: StoryEditorViewModel by viewModels {
        StoryEditorViewModelFactory(
            (application as TextingStoryApp).repository,
            intent.getLongExtra(EXTRA_STORY_ID, -1L)
        )
    }
    private lateinit var messageAdapter: MessageListAdapter
    private lateinit var soundManager: SoundManager
    private var selectedCharacter: Character? = null
    private var characters: List<Character> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrefsManager.applyTheme(this)
        binding = ActivityStoryEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        soundManager = SoundManager(this)

        // RecyclerView setup
        messageAdapter = MessageListAdapter(
            onAddBelow = { message -> viewModel.insertMessageAfter(message) },
            onDelete = { message -> confirmDeleteMessage(message) },
            onEdit = { message -> showEditMessageDialog(message) }
        )
        binding.recyclerMessages.layoutManager = LinearLayoutManager(this).also {
            it.stackFromEnd = true
        }
        binding.recyclerMessages.adapter = messageAdapter

        // Observe story
        viewModel.story.observe(this) { story ->
            story ?: return@observe
            supportActionBar?.title = story.title
        }

        // Observe characters
        viewModel.characters.observe(this) { chars ->
            characters = chars
            if (chars.isNotEmpty() && selectedCharacter == null) {
                selectedCharacter = chars.firstOrNull { it.side == "RIGHT" } ?: chars.first()
                updateSelectedCharacterUI()
            }
            updateCharacterChips(chars)
        }

        // Observe messages
        viewModel.messages.observe(this) { messages ->
            messageAdapter.submitData(messages, characters)
            if (messages.isNotEmpty()) {
                binding.recyclerMessages.scrollToPosition(messages.size - 1)
            }
        }

        // Send button
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                val character = selectedCharacter
                viewModel.addMessage(text, character?.id, "TEXT")
                binding.etMessage.text?.clear()
                soundManager.playTypingSound(character?.soundAsset)
            }
        }

        // Comment button
        binding.btnComment.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.addMessage(text, null, "COMMENT")
                binding.etMessage.text?.clear()
            }
        }

        // Character selector tap
        binding.chipSelectedChar.setOnClickListener {
            showCharacterPicker()
        }

        // Bottom nav buttons
        binding.btnEditNames.setOnClickListener {
            val intent = Intent(this, CharactersActivity::class.java)
            intent.putExtra(CharactersActivity.EXTRA_STORY_ID, viewModel.storyId)
            startActivity(intent)
        }

        binding.btnCreateVideo.setOnClickListener {
            launchExport()
        }

        binding.btnSettingsNav.setOnClickListener { startActivity(Intent(this, com.username051203.textingstory.ui.settings.SettingsActivity::class.java)) }
        binding.btnClearStory.setOnClickListener {
            finish() // goes back to My Stories
        }
        binding.btnClearStory.setOnLongClickListener {
            showClearStoryDialog(); true
        }
    }

    private fun updateSelectedCharacterUI() {
        val char = selectedCharacter ?: return
        binding.chipSelectedChar.text = char.name
        try {
            val color = android.graphics.Color.parseColor(char.bubbleColor)
            binding.chipSelectedChar.setChipBackgroundColorResource(android.R.color.transparent)
            binding.chipSelectedChar.chipBackgroundColor =
                android.content.res.ColorStateList.valueOf(color)
        } catch (e: Exception) { /* ignore */ }
    }

    private fun updateCharacterChips(chars: List<Character>) {
        binding.characterChipsGroup.removeAllViews()
        chars.forEach { char ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = char.name
                isCheckable = true
                isChecked = char.id == selectedCharacter?.id
                setOnClickListener {
                    selectedCharacter = char
                    updateSelectedCharacterUI()
                    updateCharacterChips(characters)
                }
                try {
                    val color = android.graphics.Color.parseColor(char.bubbleColor)
                    chipBackgroundColor = android.content.res.ColorStateList.valueOf(color)
                    setTextColor(android.graphics.Color.parseColor(char.textColor))
                } catch (e: Exception) { /* ignore */ }
            }
            binding.characterChipsGroup.addView(chip)
        }
    }

    private fun showCharacterPicker() {
        if (characters.isEmpty()) {
            Toast.makeText(this, R.string.no_characters, Toast.LENGTH_SHORT).show()
            return
        }
        val names = characters.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.select_character)
            .setItems(names) { _, which ->
                selectedCharacter = characters[which]
                updateSelectedCharacterUI()
                updateCharacterChips(characters)
            }
            .show()
    }

    private fun confirmDeleteMessage(message: Message) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_message)
            .setMessage(R.string.delete_message_confirm)
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteMessage(message) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditMessageDialog(message: Message) {
        val input = android.widget.EditText(this).apply {
            setText(message.text)
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.edit_message)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val newText = input.text.toString().trim()
                if (newText.isNotEmpty()) {
                    viewModel.updateMessageText(message, newText)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showClearStoryDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_story)
            .setMessage(R.string.clear_story_confirm)
            .setPositiveButton(R.string.ok) { _, _ -> viewModel.clearAllMessages() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun launchExport() {
        val intent = Intent(this, ExportPreviewActivity::class.java)
        intent.putExtra(ExportPreviewActivity.EXTRA_STORY_ID, viewModel.storyId)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { onBackPressedDispatcher.onBackPressed(); true }
            R.id.action_characters -> {
                val intent = Intent(this, CharactersActivity::class.java)
                intent.putExtra(CharactersActivity.EXTRA_STORY_ID, viewModel.storyId)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}
