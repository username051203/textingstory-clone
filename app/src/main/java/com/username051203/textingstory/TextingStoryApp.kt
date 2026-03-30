package com.username051203.textingstory

import android.app.Application
import com.username051203.textingstory.data.repository.StoryRepository

class TextingStoryApp : Application() {
    val repository by lazy { StoryRepository(this) }
}
