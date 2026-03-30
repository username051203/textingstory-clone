package com.username051203.textingstory.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.username051203.textingstory.data.db.AppDatabase
import com.username051203.textingstory.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class StoryRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val storyDao = db.storyDao()
    private val characterDao = db.characterDao()
    private val messageDao = db.messageDao()
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    // ---- Stories ----
    fun getAllStories() = storyDao.getAllStories()
    suspend fun getStoryById(id: Long) = storyDao.getStoryById(id)
    suspend fun insertStory(story: Story) = storyDao.insertStory(story)
    suspend fun updateStory(story: Story) = storyDao.updateStory(story.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deleteStory(story: Story) = storyDao.deleteStory(story)

    // ---- Characters ----
    fun getCharactersForStory(storyId: Long) = characterDao.getCharactersForStory(storyId)
    suspend fun getCharactersForStoryOnce(storyId: Long) = characterDao.getCharactersForStoryOnce(storyId)
    suspend fun insertCharacter(character: Character) = characterDao.insertCharacter(character)
    suspend fun updateCharacter(character: Character) = characterDao.updateCharacter(character)
    suspend fun deleteCharacter(character: Character) = characterDao.deleteCharacter(character)

    // ---- Messages ----
    fun getMessagesForStory(storyId: Long) = messageDao.getMessagesForStory(storyId)
    suspend fun getMessagesForStoryOnce(storyId: Long) = messageDao.getMessagesForStoryOnce(storyId)
    suspend fun insertMessage(message: Message) = messageDao.insertMessage(message)
    suspend fun updateMessage(message: Message) = messageDao.updateMessage(message)
    suspend fun deleteMessage(message: Message) {
        messageDao.deleteMessage(message)
        messageDao.reorderAfterDelete(message.storyId, message.sortOrder)
    }
    suspend fun getNextSortOrder(storyId: Long): Int = (messageDao.getMaxSortOrder(storyId) ?: -1) + 1

    // ---- FULL BACKUP (export all stories → one tiny JSON) ----
    suspend fun exportAllToJson(): String = withContext(Dispatchers.IO) {
        val stories = storyDao.getAllStoriesOnce()
        val storyBackups = stories.map { story ->
            val chars = characterDao.getCharactersForStoryOnce(story.id)
            val msgs = messageDao.getMessagesForStoryOnce(story.id)
            StoryBackup(
                title = story.title,
                createdAt = story.createdAt,
                backgroundImageBase64 = story.backgroundImagePath?.let { readFileAsBase64(it) },
                videoFormat = story.videoFormat,
                videoSpeed = story.videoSpeed,
                showTyping = story.showTyping,
                typingSoundEnabled = story.typingSoundEnabled,
                keepCorrections = story.keepCorrections,
                characters = chars.map { c ->
                    CharacterBackup(
                        localId = c.id,
                        name = c.name,
                        avatarBase64 = c.avatarPath?.let { readFileAsBase64(it) },
                        bubbleColor = c.bubbleColor,
                        textColor = c.textColor,
                        side = c.side,
                        soundAsset = c.soundAsset,
                        sortOrder = c.sortOrder
                    )
                },
                messages = msgs.map { m ->
                    MessageBackup(
                        characterLocalId = m.characterId,
                        text = m.text,
                        type = m.type,
                        sortOrder = m.sortOrder
                    )
                }
            )
        }
        gson.toJson(AppBackup(stories = storyBackups))
    }

    // ---- RESTORE (import JSON → Room) ----
    suspend fun importFromJson(json: String) = withContext(Dispatchers.IO) {
        val backup = gson.fromJson(json, AppBackup::class.java)
        backup.stories.forEach { sb ->
            val storyId = storyDao.insertStory(
                Story(
                    title = sb.title,
                    createdAt = sb.createdAt,
                    backgroundImagePath = sb.backgroundImageBase64?.let { saveBase64ToFile(it, "bg_${System.currentTimeMillis()}.jpg") },
                    videoFormat = sb.videoFormat,
                    videoSpeed = sb.videoSpeed,
                    showTyping = sb.showTyping,
                    typingSoundEnabled = sb.typingSoundEnabled,
                    keepCorrections = sb.keepCorrections
                )
            )
            // map old localId → new DB id
            val idMap = mutableMapOf<Long, Long>()
            sb.characters.forEach { cb ->
                val newId = characterDao.insertCharacter(
                    Character(
                        storyId = storyId,
                        name = cb.name,
                        avatarPath = cb.avatarBase64?.let { saveBase64ToFile(it, "avatar_${System.currentTimeMillis()}.jpg") },
                        bubbleColor = cb.bubbleColor,
                        textColor = cb.textColor,
                        side = cb.side,
                        soundAsset = cb.soundAsset,
                        sortOrder = cb.sortOrder
                    )
                )
                idMap[cb.localId] = newId
            }
            sb.messages.forEach { mb ->
                messageDao.insertMessage(
                    Message(
                        storyId = storyId,
                        characterId = mb.characterLocalId?.let { idMap[it] },
                        text = mb.text,
                        type = mb.type,
                        sortOrder = mb.sortOrder
                    )
                )
            }
        }
    }

    private fun readFileAsBase64(path: String): String? {
        return try {
            val file = File(path)
            if (!file.exists()) return null
            val bytes = file.readBytes()
            // Compress images to keep backup small
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bmp != null) {
                val out = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 60, out)
                Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            } else {
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (e: Exception) { null }
    }

    private fun saveBase64ToFile(base64: String, filename: String): String {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        val dir = File(context.filesDir, "images")
        dir.mkdirs()
        val file = File(dir, filename)
        file.writeBytes(bytes)
        return file.absolutePath
    }
}
