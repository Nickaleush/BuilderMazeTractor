package com.nickaleush.tractormaze.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nickaleush.tractormaze.core.audio.SoundManager
import com.nickaleush.tractormaze.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MenuViewModel(
    private val gameRepository: GameRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val _bestScore = MutableStateFlow(0)
    val bestScore: StateFlow<Int> = _bestScore.asStateFlow()

    private val _coins = MutableStateFlow(0)
    val coins: StateFlow<Int> = _coins.asStateFlow()

    private val _selectedSkinId = MutableStateFlow("skin_loader")
    val selectedSkinId: StateFlow<String> = _selectedSkinId.asStateFlow()

    fun loadMenuData() {
        viewModelScope.launch {
            val profile = gameRepository.getProfile()
            _bestScore.value = gameRepository.getBestScore(GameRepository.GAME_ID)
            _coins.value = profile.coins
            _selectedSkinId.value = profile.selectedSkinId
            // Sync the audio engine with stored preferences before the menu
            // tries to start its background music.
            soundManager.applySettings(
                soundEnabled = profile.soundEnabled,
                musicEnabled = profile.musicEnabled,
                soundVolume = profile.soundVolume / 100f,
                musicVolume = profile.musicVolume / 100f
            )
        }
    }
}

class MenuViewModelFactory(
    private val gameRepository: GameRepository,
    private val soundManager: SoundManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MenuViewModel(gameRepository, soundManager) as T
    }
}
