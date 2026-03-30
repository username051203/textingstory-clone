package com.username051203.textingstory.ui.story

import androidx.lifecycle.*
import com.username051203.textingstory.data.model.Message
import com.username051203.textingstory.data.repository.StoryRepository
import kotlinx.coroutines.launch

class StoryEditorViewModel(
    private val repository: StoryRepository,
    val storyId: Long
) : ViewModel() {

    val story: LiveData<com.username051203.textingstory.data.model.Story?> = liveData {
        emit(repository.getStoryById(storyId))
    }

    val characters = repository.getCharactersForStory(storyId)
    val messages = repository.getMessagesForStory(storyId)

    fun addMessage(text: String, characterId: Long?, type: String) {
        viewModelScope.launch {
            val order = repository.getNextSortOrder(storyId)
            repository.insertMessage(
                Message(
                    storyId = storyId,
                    characterId = characterId,
                    text = text,
                    type = type,
                    sortOrder = order
                )
            )
        }
    }

    fun deleteMessage(message: Message) {
        viewModelScope.launch {
            repository.deleteMessage(message)
        }
    }

    fun updateMessageText(message: Message, newText: String) {
        viewModelScope.launch {
            repository.updateMessage(message.copy(text = newText))
        }
    }

    fun insertMessageAfter(message: Message) {
        // Insert an empty message at message.sortOrder + 1 (shifts others)
        viewModelScope.launch {
            val msgs = repository.getMessagesForStoryOnce(storyId).toMutableList()
            // Shift all messages after the target
            msgs.filter { it.sortOrder > message.sortOrder }.forEach {
                repository.updateMessage(it.copy(sortOrder = it.sortOrder + 1))
            }
            repository.insertMessage(
                Message(
                    storyId = storyId,
                    characterId = message.characterId,
                    text = "",
                    type = message.type,
                    sortOrder = message.sortOrder + 1
                )
            )
        }
    }

    fun clearAllMessages() {
        viewModelScope.launch {
            val msgs = repository.getMessagesForStoryOnce(storyId)
            msgs.forEach { repository.deleteMessage(it) }
        }
    }
}

class StoryEditorViewModelFactory(
    private val repository: StoryRepository,
    private val storyId: Long
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoryEditorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StoryEditorViewModel(repository, storyId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
