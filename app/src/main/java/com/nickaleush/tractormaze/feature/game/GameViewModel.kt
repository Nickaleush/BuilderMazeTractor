package com.nickaleush.tractormaze.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nickaleush.tractormaze.core.audio.SoundManager
import com.nickaleush.tractormaze.data.db.entity.PlayerProfileEntity
import com.nickaleush.tractormaze.data.repository.GameRepository
import com.nickaleush.tractormaze.games.maze.MazeGameResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(
    private val gameRepository: GameRepository,
    val soundManager: SoundManager
) : ViewModel() {

    private val _profile = MutableStateFlow(PlayerProfileEntity())
    val profile: StateFlow<PlayerProfileEntity> = _profile.asStateFlow()

    private val _profileLoaded = MutableStateFlow(false)
    val profileLoaded: StateFlow<Boolean> = _profileLoaded.asStateFlow()

    fun loadPlayerProfile() {
        viewModelScope.launch {
            val loaded = gameRepository.getProfile()
            _profile.value = loaded
            soundManager.applySettings(
                soundEnabled = loaded.soundEnabled,
                musicEnabled = loaded.musicEnabled,
                soundVolume = loaded.soundVolume / 100f,
                musicVolume = loaded.musicVolume / 100f
            )
            _profileLoaded.value = true
        }
    }

    suspend fun saveResult(result: MazeGameResult) {
        gameRepository.saveMazeResult(result)
    }
}

class GameViewModelFactory(
    private val gameRepository: GameRepository,
    private val soundManager: SoundManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GameViewModel(gameRepository, soundManager) as T
    }
}
