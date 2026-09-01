package com.nickaleush.tractormaze.feature.result

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
import com.nickaleush.tractormaze.databinding.FragmentResultBinding
import com.nickaleush.tractormaze.feature.game.GameFragment
import kotlinx.coroutines.launch

class ResultFragment : Fragment(R.layout.fragment_result) {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: ResultViewModel by viewModels {
        val app = requireActivity().application as App
        ResultViewModelFactory(app.serviceLocator.gameRepository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentResultBinding.bind(view)

        val level = requireArguments().getInt(GameFragment.ARG_LEVEL, 1)
        requireArguments().getInt(GameFragment.ARG_SCORE, 0)
        requireArguments().getInt(GameFragment.ARG_MATERIALS, 0)
        val passed = requireArguments().getBoolean(GameFragment.ARG_PASSED, false)
        val rewardCoins = requireArguments().getInt(GameFragment.ARG_REWARD_COINS, 0)

        binding.resultImageView.setImageResource(if (passed) R.drawable.maze_you_win else R.drawable.maze_game_over)
        binding.rewardTextView.text = "+ $rewardCoins"
        binding.rewardPanel.isVisible = passed
        binding.nextButton.isVisible = passed && level < 45
        binding.restartButton.isVisible = !passed || level == 45
        binding.menuButton.apply {
            (layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)?.topToBottom =
                if (binding.nextButton.isVisible) R.id.nextButton else R.id.restartButton
        }
        binding.mascotImageView.isVisible = passed
        binding.nextButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_resultFragment_to_gameFragment,
                bundleOf(GameFragment.ARG_LEVEL to (level + 1).coerceAtMost(45))
            )
        }

        binding.restartButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_resultFragment_to_gameFragment,
                bundleOf(GameFragment.ARG_LEVEL to level)
            )
        }

        binding.menuButton.setOnClickListener {
            findNavController().popBackStack(R.id.menuFragment, false)
        }

        viewModel.loadBestScore()
    }

    override fun onResume() {
        super.onResume()
        (requireActivity().application as App).serviceLocator.soundManager
            .playMusic(SoundManager.MusicTrack.Game)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
