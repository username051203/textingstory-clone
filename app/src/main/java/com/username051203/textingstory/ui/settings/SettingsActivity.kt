package com.username051203.textingstory.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.username051203.textingstory.R
import com.username051203.textingstory.databinding.ActivitySettingsBinding
import com.username051203.textingstory.util.PrefsManager

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrefsManager.applyTheme(this)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        // Load current prefs
        val prefs = PrefsManager.get(this)
        val isDark = prefs.getBoolean(PrefsManager.KEY_DARK_MODE, true)
        val videoFormat = prefs.getString(PrefsManager.KEY_VIDEO_FORMAT, "VERTICAL") ?: "VERTICAL"
        val videoSpeed = prefs.getInt(PrefsManager.KEY_VIDEO_SPEED, 2)
        val showTyping = prefs.getString(PrefsManager.KEY_SHOW_TYPING, "ALL") ?: "ALL"
        val typingSound = prefs.getBoolean(PrefsManager.KEY_TYPING_SOUND, true)
        val keepCorrections = prefs.getBoolean(PrefsManager.KEY_KEEP_CORRECTIONS, true)

        // Mode
        binding.btnLight.isSelected = !isDark
        binding.btnDark.isSelected = isDark
        binding.btnLight.setOnClickListener { setDarkMode(false) }
        binding.btnDark.setOnClickListener { setDarkMode(true) }

        // Video format
        updateFormatButtons(videoFormat)
        binding.btnSquare.setOnClickListener { setFormat("SQUARE") }
        binding.btnVertical.setOnClickListener { setFormat("VERTICAL") }
        binding.btnVerticalKeyboard.setOnClickListener { setFormat("VERTICAL_KEYBOARD") }

        // Video speed
        updateSpeedButtons(videoSpeed)
        binding.btnX1.setOnClickListener { setSpeed(1) }
        binding.btnX2.setOnClickListener { setSpeed(2) }
        binding.btnX3.setOnClickListener { setSpeed(3) }

        // Show typing
        updateTypingButtons(showTyping)
        binding.btnTypingOn.setOnClickListener { setTyping("ALL") }
        binding.btnTypingOff.setOnClickListener { setTyping("OFF") }
        binding.btnTypingLeft.setOnClickListener { setTyping("LEFT") }
        binding.btnTypingRight.setOnClickListener { setTyping("RIGHT") }

        // Typing sound
        binding.btnSoundOn.isSelected = typingSound
        binding.btnSoundOff.isSelected = !typingSound
        binding.btnSoundOn.setOnClickListener { setTypingSound(true) }
        binding.btnSoundOff.setOnClickListener { setTypingSound(false) }

        // Keep corrections
        binding.btnCorrectionsOn.isSelected = keepCorrections
        binding.btnCorrectionsOff.isSelected = !keepCorrections
        binding.btnCorrectionsOn.setOnClickListener { setKeepCorrections(true) }
        binding.btnCorrectionsOff.setOnClickListener { setKeepCorrections(false) }
    }

    private fun setDarkMode(dark: Boolean) {
        PrefsManager.get(this).edit().putBoolean(PrefsManager.KEY_DARK_MODE, dark).apply()
        binding.btnLight.isSelected = !dark
        binding.btnDark.isSelected = dark
        PrefsManager.applyTheme(this)
        recreate()
    }

    private fun setFormat(fmt: String) {
        PrefsManager.get(this).edit().putString(PrefsManager.KEY_VIDEO_FORMAT, fmt).apply()
        updateFormatButtons(fmt)
    }

    private fun updateFormatButtons(fmt: String) {
        binding.btnSquare.isSelected = fmt == "SQUARE"
        binding.btnVertical.isSelected = fmt == "VERTICAL"
        binding.btnVerticalKeyboard.isSelected = fmt == "VERTICAL_KEYBOARD"
    }

    private fun setSpeed(speed: Int) {
        PrefsManager.get(this).edit().putInt(PrefsManager.KEY_VIDEO_SPEED, speed).apply()
        updateSpeedButtons(speed)
    }

    private fun updateSpeedButtons(speed: Int) {
        binding.btnX1.isSelected = speed == 1
        binding.btnX2.isSelected = speed == 2
        binding.btnX3.isSelected = speed == 3
    }

    private fun setTyping(mode: String) {
        PrefsManager.get(this).edit().putString(PrefsManager.KEY_SHOW_TYPING, mode).apply()
        updateTypingButtons(mode)
    }

    private fun updateTypingButtons(mode: String) {
        binding.btnTypingOn.isSelected = mode == "ALL"
        binding.btnTypingOff.isSelected = mode == "OFF"
        binding.btnTypingLeft.isSelected = mode == "LEFT"
        binding.btnTypingRight.isSelected = mode == "RIGHT"
    }

    private fun setTypingSound(on: Boolean) {
        PrefsManager.get(this).edit().putBoolean(PrefsManager.KEY_TYPING_SOUND, on).apply()
        binding.btnSoundOn.isSelected = on
        binding.btnSoundOff.isSelected = !on
    }

    private fun setKeepCorrections(on: Boolean) {
        PrefsManager.get(this).edit().putBoolean(PrefsManager.KEY_KEEP_CORRECTIONS, on).apply()
        binding.btnCorrectionsOn.isSelected = on
        binding.btnCorrectionsOff.isSelected = !on
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
