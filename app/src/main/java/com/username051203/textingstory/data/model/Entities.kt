package com.username051203.textingstory.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "stories")
data class Story(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val backgroundImagePath: String? = null,
    val videoFormat: String = "VERTICAL", // SQUARE, VERTICAL, VERTICAL_KEYBOARD
    val videoSpeed: Int = 2, // 1, 2, 3
    val showTyping: String = "ALL", // ALL, OFF, LEFT, RIGHT
    val typingSoundEnabled: Boolean = true,
    val keepCorrections: Boolean = true
)

@Entity(
    tableName = "characters",
    foreignKeys = [
        ForeignKey(
            entity = Story::class,
            parentColumns = ["id"],
            childColumns = ["storyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("storyId")]
)
data class Character(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val storyId: Long,
    val name: String,
    val avatarPath: String? = null, // null = default colored circle
    val bubbleColor: String = "#FF6FD8", // hex color
    val textColor: String = "#FFFFFF",
    val side: String = "LEFT", // LEFT or RIGHT (RIGHT = "Self")
    val soundAsset: String? = null, // asset name in /raw
    val sortOrder: Int = 0
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = Story::class,
            parentColumns = ["id"],
            childColumns = ["storyId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Character::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("storyId"), Index("characterId")]
)
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val storyId: Long,
    val characterId: Long?,
    val text: String,
    val type: String = "TEXT", // TEXT, COMMENT
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
