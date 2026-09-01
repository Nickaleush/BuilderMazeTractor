package com.nickaleush.tractormaze.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nickaleush.tractormaze.core.audio.SoundManager
import com.nickaleush.tractormaze.data.db.entity.PlayerProfileEntity
import com.nickaleush.tractormaze.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val gameRepository: GameRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val _profile = MutableStateFlow(PlayerProfileEntity())
    val profile: StateFlow<PlayerProfileEntity> = _profile.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val loaded = gameRepository.getProfile()
            _profile.value = loaded
            applyToAudio(loaded)
        }
    }

    fun setSound(enabled: Boolean) {
        viewModelScope.launch {
            gameRepository.setSoundEnabled(enabled)
            val updated = gameRepository.getProfile()
            _profile.value = updated
            applyToAudio(updated)
        }
    }

    fun setMusic(enabled: Boolean) {
        viewModelScope.launch {
            gameRepository.setMusicEnabled(enabled)
            val updated = gameRepository.getProfile()
            _profile.value = updated
            applyToAudio(updated)
        }
    }

    fun setSoundVolume(volume: Int) {
        viewModelScope.launch {
            gameRepository.setSoundVolume(volume)
            val updated = gameRepository.getProfile()
            _profile.value = updated
            applyToAudio(updated)
        }
    }

    fun setMusicVolume(volume: Int) {
        viewModelScope.launch {
            gameRepository.setMusicVolume(volume)
            val updated = gameRepository.getProfile()
            _profile.value = updated
            applyToAudio(updated)
        }
    }

    private fun applyToAudio(profile: PlayerProfileEntity) {
        soundManager.applySettings(
            soundEnabled = profile.soundEnabled,
            musicEnabled = profile.musicEnabled,
            soundVolume = profile.soundVolume / 100f,
            musicVolume = profile.musicVolume / 100f
        )
    }
}

class SettingsViewModelFactory(
    private val gameRepository: GameRepository,
    private val soundManager: SoundManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(gameRepository, soundManager) as T
    }
}
