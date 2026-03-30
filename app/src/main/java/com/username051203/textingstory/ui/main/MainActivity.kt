package com.username051203.textingstory.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.username051203.textingstory.R
import com.username051203.textingstory.TextingStoryApp
import com.username051203.textingstory.data.model.Story
import com.username051203.textingstory.databinding.ActivityMainBinding
import com.username051203.textingstory.ui.settings.SettingsActivity
import com.username051203.textingstory.ui.story.StoryEditorActivity
import com.username051203.textingstory.util.PrefsManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as TextingStoryApp).repository)
    }
    private lateinit var adapter: StoryListAdapter

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { importBackup(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrefsManager.applyTheme(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.my_stories)

        adapter = StoryListAdapter(
            onStoryClick = { story ->
                openStory(story)
            },
            onStoryLongClick = { story ->
                showStoryOptions(story)
            }
        )
        binding.recyclerStories.layoutManager = LinearLayoutManager(this)
        binding.recyclerStories.adapter = adapter

        viewModel.stories.observe(this) { stories ->
            adapter.submitList(stories)
            binding.emptyView.visibility =
                if (stories.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }

        binding.btnNewStory.setOnClickListener {
            showNewStoryDialog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export -> { exportAllStories(); true }
            R.id.action_import -> { importLauncher.launch("application/json"); true }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java)); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showNewStoryDialog() {
        val input = android.widget.EditText(this).apply {
            hint = getString(R.string.story_title_hint)
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.new_story)
            .setView(input)
            .setPositiveButton(R.string.create) { _, _ ->
                val title = input.text.toString().trim()
                if (title.isNotEmpty()) {
                    viewModel.createStory(title) { storyId ->
                        openStoryById(storyId)
                    }
                } else {
                    Toast.makeText(this, R.string.title_required, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openStory(story: Story) {
        openStoryById(story.id)
    }

    private fun openStoryById(storyId: Long) {
        val intent = Intent(this, StoryEditorActivity::class.java)
        intent.putExtra(StoryEditorActivity.EXTRA_STORY_ID, storyId)
        startActivity(intent)
    }

    private fun showStoryOptions(story: Story) {
        val options = arrayOf(
            getString(R.string.rename),
            getString(R.string.delete)
        )
        AlertDialog.Builder(this)
            .setTitle(story.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog(story)
                    1 -> confirmDelete(story)
                }
            }
            .show()
    }

    private fun showRenameDialog(story: Story) {
        val input = android.widget.EditText(this).apply {
            setText(story.title)
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.rename)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val newTitle = input.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    viewModel.renameStory(story, newTitle)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmDelete(story: Story) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_story)
            .setMessage(getString(R.string.delete_story_confirm, story.title))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteStory(story)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun exportAllStories() {
        viewModel.exportAll { json ->
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "textingstory_backup_${System.currentTimeMillis()}.json")
            }
            exportJsonContent = json
            exportLauncher.launch(intent)
        }
    }

    private var exportJsonContent: String = ""
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(exportJsonContent.toByteArray())
                    }
                    Toast.makeText(this, R.string.export_success, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun importBackup(uri: Uri) {
        try {
            val json = contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return
            viewModel.importAll(json) {
                Toast.makeText(this, R.string.import_success, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, R.string.import_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
