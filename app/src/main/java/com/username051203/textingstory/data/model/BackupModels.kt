package com.username051203.textingstory.data.model

/**
 * Portable backup format — entire app state in one tiny JSON file.
 * Characters embed their base64 avatar so the backup is fully self-contained.
 */
data class AppBackup(
    val version: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val stories: List<StoryBackup>
)

data class StoryBackup(
    val title: String,
    val createdAt: Long,
    val backgroundImageBase64: String? = null,
    val videoFormat: String,
    val videoSpeed: Int,
    val showTyping: String,
    val typingSoundEnabled: Boolean,
    val keepCorrections: Boolean,
    val characters: List<CharacterBackup>,
    val messages: List<MessageBackup>
)

data class CharacterBackup(
    val localId: Long, // used to re-link messages
    val name: String,
    val avatarBase64: String? = null,
    val bubbleColor: String,
    val textColor: String,
    val side: String,
    val soundAsset: String? = null,
    val sortOrder: Int
)

data class MessageBackup(
    val characterLocalId: Long?,
    val text: String,
    val type: String,
    val sortOrder: Int
)

/** Flat joined model used in RecyclerView */
data class MessageWithCharacter(
    val message: Message,
    val character: Character?
)
