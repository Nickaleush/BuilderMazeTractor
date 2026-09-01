package com.nickaleush.tractormaze.feature.levels

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.nickaleush.tractormaze.App
import com.nickaleush.tractormaze.R
import com.nickaleush.tractormaze.core.audio.SoundManager
import com.nickaleush.tractormaze.databinding.FragmentLevelsBinding
import com.nickaleush.tractormaze.feature.game.GameFragment
import com.nickaleush.tractormaze.games.maze.MazeLevelConfig
import kotlinx.coroutines.launch

class LevelsFragment : Fragment(R.layout.fragment_levels) {

    private var _binding: FragmentLevelsBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: LevelsViewModel by viewModels {
        val app = requireActivity().application as App
        LevelsViewModelFactory(app.serviceLocator.gameRepository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLevelsBinding.bind(view)
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        observeProgress()
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
        (requireActivity().application as App).serviceLocator.soundManager
            .playMusic(SoundManager.MusicTrack.Menu)
    }


    private fun observeProgress() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.maxUnlockedLevel.collect { maxUnlocked ->
                    binding.levelsGridLayout.post {
                        renderLevels(maxUnlocked)
                    }
                }
            }
        }
    }

    private fun renderLevels(maxUnlockedLevel: Int) {
        if (_binding == null) return
        binding.levelsGridLayout.removeAllViews()

        val margin = resources.getDimensionPixelSize(R.dimen.level_button_margin)
        val availableWidth = binding.levelsGridLayout.width
        if (availableWidth <= 0) {
            binding.levelsGridLayout.post { renderLevels(maxUnlockedLevel) }
            return
        }
        val size = ((availableWidth - margin * 2 * COLUMN_COUNT) / COLUMN_COUNT)
            .coerceAtLeast(1)

        for (level in 1..LEVEL_COUNT) {
            val config = MazeLevelConfig.forLevel(level)
            val unlocked = level <= maxUnlockedLevel

            val button = Button(requireContext()).apply {
                textSize = 20f
                isAllCaps = false
                gravity = android.view.Gravity.CENTER
                minWidth = 0
                minHeight = 0
                minimumWidth = 0
                minimumHeight = 0
                setPadding(0, 0, 0, 0)

                typeface = ResourcesCompat.getFont(context, R.font.science_gothic_expanded_bold) ?: Typeface.DEFAULT_BOLD

                if (unlocked) {
                    text = level.toString()
                    setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    setShadowLayer(2.5f, 3f, 3f, ContextCompat.getColor(context, R.color.tractor_text_shadow))
                    alpha = 1f
                    background = ResourcesCompat.getDrawable(resources, R.drawable.maze_map_sand, null)
                } else {
                    text = ""
                    alpha = 1f
                    background = ResourcesCompat.getDrawable(resources, R.drawable.bg_level_locked, null)
                }

                contentDescription = if (unlocked) {
                    getString(
                        R.string.level_button_description,
                        level,
                        config.materials.size
                    )
                } else {
                    getString(
                        R.string.level_locked_description,
                        level,
                        level - 1
                    )
                }

                setOnClickListener {
                    if (unlocked) {
                        openLevel(level)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.level_locked_toast, level - 1),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            val params = GridLayout.LayoutParams().apply {
                width = size
                height = size
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1)
                setMargins(margin, margin, margin, margin)
            }
            binding.levelsGridLayout.addView(button, params)
        }
    }

    private fun openLevel(level: Int) {
        findNavController().navigate(
            R.id.action_levelsFragment_to_gameFragment,
            bundleOf(GameFragment.ARG_LEVEL to level)
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val LEVEL_COUNT = 45
        const val COLUMN_COUNT = 4
    }
}
