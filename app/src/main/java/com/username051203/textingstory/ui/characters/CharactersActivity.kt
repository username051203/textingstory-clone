package com.username051203.textingstory.ui.characters

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.username051203.textingstory.R
import com.username051203.textingstory.TextingStoryApp
import com.username051203.textingstory.data.model.Character
import com.username051203.textingstory.data.repository.StoryRepository
import com.username051203.textingstory.databinding.ActivityCharactersBinding
import com.username051203.textingstory.databinding.ItemCharacterBinding
import com.username051203.textingstory.util.PrefsManager
import kotlinx.coroutines.launch
import java.io.File

class CharactersActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_STORY_ID = "extra_story_id"
    }

    private lateinit var binding: ActivityCharactersBinding
    private val viewModel: CharactersViewModel by viewModels {
        CharactersViewModelFactory(
            (application as TextingStoryApp).repository,
            intent.getLongExtra(EXTRA_STORY_ID, -1L)
        )
    }
    private lateinit var adapter: CharacterAdapter
    private var pendingAvatarCharId: Long = -1L

    private val avatarPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        viewModel.setAvatar(pendingAvatarCharId, uri, contentResolver, filesDir)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrefsManager.applyTheme(this)
        binding = ActivityCharactersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.characters)

        adapter = CharacterAdapter(
            onColorClick = { char -> showColorPicker(char) },
            onAvatarClick = { char -> pendingAvatarCharId = char.id; avatarPicker.launch("image/*") },
            onNameEdit = { char, name -> viewModel.rename(char, name) },
            onDelete = { char -> viewModel.delete(char) },
            onSideToggle = { char -> viewModel.toggleSide(char) }
        )
        binding.recyclerCharacters.layoutManager = LinearLayoutManager(this)
        binding.recyclerCharacters.adapter = adapter

        viewModel.characters.observe(this) { adapter.submitList(it) }

        binding.btnAddCharacter.setOnClickListener { showAddCharacterDialog() }
    }

    private fun showAddCharacterDialog() {
        val input = EditText(this).apply { hint = "Character name"; setPadding(48, 24, 48, 24) }
        AlertDialog.Builder(this)
            .setTitle(R.string.add_character)
            .setView(input)
            .setPositiveButton(R.string.add) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) viewModel.addCharacter(name)
                else Toast.makeText(this, R.string.title_required, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showColorPicker(char: Character) {
        ColorPickerDialog.Builder(this)
            .setTitle(getString(R.string.pick_color))
            .setPreferenceName("ColorPicker_${char.id}")
            .setPositiveButton(getString(R.string.ok), ColorEnvelopeListener { envelope, _ ->
                val hex = "#${envelope.hexCode.substring(2)}" // drop alpha
                viewModel.updateColor(char, hex)
            })
            .setNegativeButton(getString(R.string.cancel)) { d, _ -> d.dismiss() }
            .attachAlphaSlideBar(false)
            .attachBrightnessSlideBar(true)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}

// -------- ViewModel --------
class CharactersViewModel(private val repo: StoryRepository, val storyId: Long) : ViewModel() {
    val characters = repo.getCharactersForStory(storyId)

    fun addCharacter(name: String) = viewModelScope.launch {
        val count = repo.getCharactersForStoryOnce(storyId).size
        repo.insertCharacter(
            Character(storyId = storyId, name = name,
                bubbleColor = listOf("#FF6FD8", "#00B2FF", "#9B59B6", "#2ECC71").random(),
                side = if (count == 0) "RIGHT" else "LEFT",
                sortOrder = count)
        )
    }

    fun rename(char: Character, name: String) = viewModelScope.launch {
        repo.updateCharacter(char.copy(name = name))
    }

    fun delete(char: Character) = viewModelScope.launch { repo.deleteCharacter(char) }

    fun toggleSide(char: Character) = viewModelScope.launch {
        repo.updateCharacter(char.copy(side = if (char.side == "LEFT") "RIGHT" else "LEFT"))
    }

    fun updateColor(char: Character, hex: String) = viewModelScope.launch {
        repo.updateCharacter(char.copy(bubbleColor = hex))
    }

    fun setAvatar(charId: Long, uri: Uri, cr: android.content.ContentResolver, filesDir: java.io.File) = viewModelScope.launch {
        val char = repo.getCharactersForStoryOnce(storyId).find { it.id == charId } ?: return@launch
        val bytes = cr.openInputStream(uri)?.readBytes() ?: return@launch
        val dir = java.io.File(filesDir, "images").also { it.mkdirs() }
        val file = java.io.File(dir, "avatar_${charId}_${System.currentTimeMillis()}.jpg")
        file.writeBytes(bytes)
        repo.updateCharacter(char.copy(avatarPath = file.absolutePath))
    }
}

class CharactersViewModelFactory(private val repo: StoryRepository, private val storyId: Long) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CharactersViewModel(repo, storyId) as T
    }
}

// -------- Adapter --------
class CharacterAdapter(
    private val onColorClick: (Character) -> Unit,
    private val onAvatarClick: (Character) -> Unit,
    private val onNameEdit: (Character, String) -> Unit,
    private val onDelete: (Character) -> Unit,
    private val onSideToggle: (Character) -> Unit
) : RecyclerView.Adapter<CharacterAdapter.VH>() {

    private var list: List<Character> = emptyList()
    fun submitList(l: List<Character>) { list = l; notifyDataSetChanged() }

    override fun getItemCount() = list.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCharacterBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(list[position])

    inner class VH(private val b: ItemCharacterBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(char: Character) {
            b.tvCharName.text = char.name
            b.tvSide.text = char.side
            try {
                b.viewColorSwatch.setBackgroundColor(Color.parseColor(char.bubbleColor))
            } catch (e: Exception) {}
            if (char.avatarPath != null)
                Glide.with(b.ivAvatar).load(char.avatarPath).circleCrop().into(b.ivAvatar)
            else
                b.ivAvatar.setImageResource(R.drawable.ic_person)
            b.btnColor.setOnClickListener { onColorClick(char) }
            b.ivAvatar.setOnClickListener { onAvatarClick(char) }
            b.btnSide.setOnClickListener { onSideToggle(char) }
            b.btnDelete.setOnClickListener { onDelete(char) }
            b.tvCharName.setOnClickListener {
                val input = EditText(b.root.context).apply {
                    setText(char.name); setPadding(48, 24, 48, 24)
                }
                AlertDialog.Builder(b.root.context)
                    .setTitle("Rename")
                    .setView(input)
                    .setPositiveButton("Save") { _, _ ->
                        val n = input.text.toString().trim()
                        if (n.isNotEmpty()) onNameEdit(char, n)
                    }
                    .setNegativeButton("Cancel", null).show()
            }
        }
    }
}
