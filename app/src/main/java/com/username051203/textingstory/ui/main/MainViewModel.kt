package com.username051203.textingstory.ui.main

import androidx.lifecycle.*
import com.username051203.textingstory.data.model.Story
import com.username051203.textingstory.data.repository.StoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(private val repository: StoryRepository) : ViewModel() {
    val stories = repository.getAllStories()

    fun createStory(title: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.insertStory(Story(title = title))
            onCreated(id)
        }
    }

    fun renameStory(story: Story, newTitle: String) {
        viewModelScope.launch {
            repository.updateStory(story.copy(title = newTitle))
        }
    }

    fun deleteStory(story: Story) {
        viewModelScope.launch {
            repository.deleteStory(story)
        }
    }

    fun exportAll(onDone: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val json = repository.exportAllToJson()
            launch(Dispatchers.Main) { onDone(json) }
        }
    }

    fun importAll(json: String, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.importFromJson(json)
            launch(Dispatchers.Main) { onDone() }
        }
    }
}

class MainViewModelFactory(private val repository: StoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
