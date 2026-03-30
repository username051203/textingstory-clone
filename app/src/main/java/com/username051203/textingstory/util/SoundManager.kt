package com.username051203.textingstory.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.username051203.textingstory.R

class SoundManager(private val context: Context) {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var defaultSoundId: Int = 0

    init {
        try {
            defaultSoundId = soundPool.load(context, R.raw.typing_sound, 1)
        } catch (e: Exception) { /* raw file may be placeholder */ }
    }

    fun playTypingSound(soundAsset: String? = null) {
        if (!PrefsManager.get(context).getBoolean(PrefsManager.KEY_TYPING_SOUND, true)) return
        if (defaultSoundId > 0) soundPool.play(defaultSoundId, 0.7f, 0.7f, 1, 0, 1.0f)
    }

    fun release() { soundPool.release() }
}
