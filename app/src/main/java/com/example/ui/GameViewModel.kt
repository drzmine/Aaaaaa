package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Screen {
    MENU, GAME, PROFILE, SETTINGS
}

enum class GameLevel {
    EASY, HARD, SUPER
}

data class ProfileData(
    val name: String = "Gully Runner",
    val photoUri: String? = null,
    val avatarIndex: Int = 0 // For built-in avatar selections if no custom photo is chosen
)

data class GameSettings(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val particlesEnabled: Boolean = true,
    val selectedTheme: Int = 0 // 0: Forest Sunset, 1: Cyber Neon, 2: Dusty Desert
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("run_boy_run_prefs", Context.MODE_PRIVATE)

    // Screen State
    private val _currentScreen = MutableStateFlow(Screen.MENU)
    val currentScreen = _currentScreen.asStateFlow()

    // Level Selection
    private val _selectedLevel = MutableStateFlow(GameLevel.EASY)
    val selectedLevel = _selectedLevel.asStateFlow()

    // Has level been confirmed (so Play button is active)
    private val _isLevelConfirmed = MutableStateFlow(false)
    val isLevelConfirmed = _isLevelConfirmed.asStateFlow()

    // High Scores
    private val _easyHighScore = MutableStateFlow(sharedPrefs.getInt("high_score_easy", 0))
    val easyHighScore = _easyHighScore.asStateFlow()

    private val _hardHighScore = MutableStateFlow(sharedPrefs.getInt("high_score_hard", 0))
    val hardHighScore = _hardHighScore.asStateFlow()

    private val _superHighScore = MutableStateFlow(sharedPrefs.getInt("high_score_super", 0))
    val superHighScore = _superHighScore.asStateFlow()

    // Profile State
    private val _profile = MutableStateFlow(
        ProfileData(
            name = sharedPrefs.getString("profile_name", "Gully Runner") ?: "Gully Runner",
            photoUri = sharedPrefs.getString("profile_photo_uri", null),
            avatarIndex = sharedPrefs.getInt("profile_avatar_idx", 0)
        )
    )
    val profile = _profile.asStateFlow()

    // Settings State
    private val _settings = MutableStateFlow(
        GameSettings(
            soundEnabled = sharedPrefs.getBoolean("settings_sound", true),
            musicEnabled = sharedPrefs.getBoolean("settings_music", true),
            particlesEnabled = sharedPrefs.getBoolean("settings_particles", true),
            selectedTheme = sharedPrefs.getInt("settings_theme", 0)
        )
    )
    val settings = _settings.asStateFlow()

    // Current Game Score (live updated)
    private val _currentScore = MutableStateFlow(0)
    val currentScore = _currentScore.asStateFlow()

    // Is a new high score achieved in the current session
    private val _isNewHighScore = MutableStateFlow(false)
    val isNewHighScore = _isNewHighScore.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen == Screen.MENU) {
            _isLevelConfirmed.value = false // Require level confirmation when returning to menu
        }
    }

    fun selectLevel(level: GameLevel) {
        _selectedLevel.value = level
        _isLevelConfirmed.value = true // Automatically show or activate Play once level clicked
    }

    fun updateScore(newScore: Int) {
        _currentScore.value = newScore
        val currentLvl = _selectedLevel.value
        val record = getHighScoreForLevel(currentLvl)
        if (newScore > record) {
            _isNewHighScore.value = true
            saveHighScore(currentLvl, newScore)
        }
    }

    fun resetScoreSession() {
        _currentScore.value = 0
        _isNewHighScore.value = false
    }

    private fun getHighScoreForLevel(level: GameLevel): Int {
        return when (level) {
            GameLevel.EASY -> _easyHighScore.value
            GameLevel.HARD -> _hardHighScore.value
            GameLevel.SUPER -> _superHighScore.value
        }
    }

    private fun saveHighScore(level: GameLevel, score: Int) {
        val editor = sharedPrefs.edit()
        when (level) {
            GameLevel.EASY -> {
                _easyHighScore.value = score
                editor.putInt("high_score_easy", score)
            }
            GameLevel.HARD -> {
                _hardHighScore.value = score
                editor.putInt("high_score_hard", score)
            }
            GameLevel.SUPER -> {
                _superHighScore.value = score
                editor.putInt("high_score_super", score)
            }
        }
        editor.apply()
    }

    fun updateProfile(name: String, avatarIdx: Int, photoUri: String?) {
        _profile.value = ProfileData(name, photoUri, avatarIdx)
        sharedPrefs.edit().apply {
            putString("profile_name", name)
            putInt("profile_avatar_idx", avatarIdx)
            putString("profile_photo_uri", photoUri)
            apply()
        }
    }

    fun updateSettings(sound: Boolean, music: Boolean, particles: Boolean, theme: Int) {
        _settings.value = GameSettings(sound, music, particles, theme)
        sharedPrefs.edit().apply {
            putBoolean("settings_sound", sound)
            putBoolean("settings_music", music)
            putBoolean("settings_particles", particles)
            putInt("settings_theme", theme)
            apply()
        }
    }

    fun resetAllHighScores() {
        _easyHighScore.value = 0
        _hardHighScore.value = 0
        _superHighScore.value = 0
        sharedPrefs.edit().apply {
            putInt("high_score_easy", 0)
            putInt("high_score_hard", 0)
            putInt("high_score_super", 0)
            apply()
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
                return GameViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
