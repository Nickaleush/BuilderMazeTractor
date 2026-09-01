package com.nickaleush.tractormaze.feature.game

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.nickaleush.tractormaze.App
import com.nickaleush.tractormaze.R
import com.nickaleush.tractormaze.core.audio.SoundManager
import com.nickaleush.tractormaze.databinding.FragmentGameBinding
import com.nickaleush.tractormaze.games.maze.MazeGameResult
import com.nickaleush.tractormaze.games.maze.MazeGameView
import com.nickaleush.tractormaze.games.maze.MazeMaterialCounters
import kotlinx.coroutines.launch

class GameFragment : Fragment(R.layout.fragment_game), MazeGameView.Callback {

    private var _binding: FragmentGameBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: GameViewModel by viewModels {
        val app = requireActivity().application as App
        GameViewModelFactory(
            gameRepository = app.serviceLocator.gameRepository,
            soundManager = app.serviceLocator.soundManager
        )
    }

    private val soundManager: SoundManager
        get() = (requireActivity().application as App).serviceLocator.soundManager

    private var level = 1
    private var hasFinished = false
    private var isPausedByUser = false
    private var levelStarted = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentGameBinding.bind(view)
        level = requireArguments().getInt(ARG_LEVEL, 1).coerceIn(1, 45)

        binding.gameView.callback = this
        setupListeners()
        hidePausePanel()
        observeProfile()
        viewModel.loadPlayerProfile()
    }

    private fun observeProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.profile.collect { profile ->
                        if (_binding == null) return@collect
                        binding.gameView.setAppearance(
                            skinId = profile.selectedSkinId,
                            backgroundId = profile.selectedBackgroundId
                        )
                    }
                }
                launch {
                    viewModel.profileLoaded.collect { loaded ->
                        if (loaded && !levelStarted && _binding != null) {
                            levelStarted = true
                            binding.gameView.post {
                                if (_binding != null) binding.gameView.startLevel(level)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.pauseButton.setOnClickListener { showPausePanel() }
        binding.continueButton.setOnClickListener { hidePausePanel() }
        binding.restartButton.setOnClickListener {
            hidePausePanel()
            hasFinished = false
            binding.gameView.restart()
        }
        binding.homeButton.setOnClickListener {
            binding.gameView.stopGameLoop()
            findNavController().popBackStack(R.id.menuFragment, false)
        }
    }

    override fun onResume() {
        super.onResume()
        soundManager.playMusic(SoundManager.MusicTrack.Game)
        if (_binding != null && !isPausedByUser && !hasFinished) {
            binding.gameView.setPaused(false)
        }
    }

    override fun onPause() {
        if (!hasFinished) {
            soundManager.pauseMusic()
        }
        if (_binding != null && !hasFinished) {
            binding.gameView.setPaused(true)
        }
        super.onPause()
    }

    override fun onDestroyView() {
        binding.gameView.callback = null
        binding.gameView.stopGameLoop()
        _binding = null
        super.onDestroyView()
    }

    override fun onHudChanged(level: Int, counters: MazeMaterialCounters, score: Int, lives: Int) {
        if (_binding == null) return
        binding.levelTextView.text = getString(R.string.game_level_format, level)
    }

    override fun onCollectSound() {
        soundManager.playEffect(SoundManager.SoundEffect.Collect)
    }

    override fun onTurnSound() {
        soundManager.playEffect(SoundManager.SoundEffect.Turn)
    }

    override fun onCrashSound() {
        soundManager.playEffect(SoundManager.SoundEffect.Crash)
    }

    override fun onGameFinished(result: MazeGameResult) {
        if (hasFinished || _binding == null) return
        hasFinished = true
        soundManager.playEffect(if (result.passed) SoundManager.SoundEffect.Win else SoundManager.SoundEffect.Lose)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveResult(result)
            if (_binding == null) return@launch
            findNavController().navigate(
                R.id.action_gameFragment_to_resultFragment,
                bundleOf(
                    ARG_LEVEL to result.level,
                    ARG_SCORE to result.score,
                    ARG_PASSED to result.passed,
                    ARG_REWARD_COINS to result.rewardCoins,
                    ARG_MATERIALS to result.materialsCollected
                )
            )
        }
    }

    private fun showPausePanel() {
        isPausedByUser = true
        binding.gameView.setPaused(true)
        soundManager.pauseMusic()
        binding.pausePanel.isVisible = true
    }

    private fun hidePausePanel() {
        isPausedByUser = false
        binding.pausePanel.isVisible = false
        if (!hasFinished) {
            binding.gameView.setPaused(false)
            soundManager.playMusic(SoundManager.MusicTrack.Game)
        }
    }

    companion object {
        const val ARG_LEVEL = "level"
        const val ARG_SCORE = "score"
        const val ARG_PASSED = "passed"
        const val ARG_REWARD_COINS = "rewardCoins"
        const val ARG_MATERIALS = "materialsCollected"
    }
}
