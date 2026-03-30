package com.username051203.textingstory.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.username051203.textingstory.data.model.*

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories ORDER BY updatedAt DESC")
    fun getAllStories(): LiveData<List<Story>>

    @Query("SELECT * FROM stories ORDER BY updatedAt DESC")
    suspend fun getAllStoriesOnce(): List<Story>

    @Query("SELECT * FROM stories WHERE id = :id")
    suspend fun getStoryById(id: Long): Story?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: Story): Long

    @Update
    suspend fun updateStory(story: Story)

    @Delete
    suspend fun deleteStory(story: Story)
}

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters WHERE storyId = :storyId ORDER BY sortOrder ASC")
    fun getCharactersForStory(storyId: Long): LiveData<List<Character>>

    @Query("SELECT * FROM characters WHERE storyId = :storyId ORDER BY sortOrder ASC")
    suspend fun getCharactersForStoryOnce(storyId: Long): List<Character>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: Character): Long

    @Update
    suspend fun updateCharacter(character: Character)

    @Delete
    suspend fun deleteCharacter(character: Character)

    @Query("DELETE FROM characters WHERE storyId = :storyId")
    suspend fun deleteAllForStory(storyId: Long)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE storyId = :storyId ORDER BY sortOrder ASC")
    fun getMessagesForStory(storyId: Long): LiveData<List<Message>>

    @Query("SELECT * FROM messages WHERE storyId = :storyId ORDER BY sortOrder ASC")
    suspend fun getMessagesForStoryOnce(storyId: Long): List<Message>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Update
    suspend fun updateMessage(message: Message)

    @Delete
    suspend fun deleteMessage(message: Message)

    @Query("SELECT MAX(sortOrder) FROM messages WHERE storyId = :storyId")
    suspend fun getMaxSortOrder(storyId: Long): Int?

    @Query("UPDATE messages SET sortOrder = sortOrder - 1 WHERE storyId = :storyId AND sortOrder > :deletedOrder")
    suspend fun reorderAfterDelete(storyId: Long, deletedOrder: Int)

    @Query("DELETE FROM messages WHERE storyId = :storyId")
    suspend fun deleteAllForStory(storyId: Long)
}
