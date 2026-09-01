package com.nickaleush.tractormaze.feature.onboarding

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nickaleush.tractormaze.App
import com.nickaleush.tractormaze.R
import com.nickaleush.tractormaze.core.audio.SoundManager
import com.nickaleush.tractormaze.databinding.FragmentOnboardingBinding

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var page = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingBinding.bind(view)
        page = savedInstanceState?.getInt(KEY_PAGE) ?: 0
        binding.nextButton.setOnClickListener { next() }
        render()
    }

    private fun next() {
        if (page < LAST_PAGE) {
            page++
            render()
        } else {
            requireContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_DONE, true)
                .apply()
            findNavController().navigate(R.id.action_onboardingFragment_to_menuFragment)
        }
    }

    private fun render() {
        val imageRes = when (page) {
            0 -> R.drawable.onboarding_slide_1
            1 -> R.drawable.onboarding_slide_2
            else -> R.drawable.onboarding_slide_3
        }
        val textRes = when (page) {
            0 -> R.string.onboarding_text_1
            1 -> R.string.onboarding_text_2
            else -> R.string.onboarding_text_3
        }
        binding.illustrationImageView.setImageResource(imageRes)
        binding.bodyTextView.setText(textRes)
    }

    override fun onResume() {
        super.onResume()
        (requireActivity().application as App).serviceLocator.soundManager
            .playMusic(SoundManager.MusicTrack.Menu)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PAGE, page)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val PREFS = "builder_maze_prefs"
        const val KEY_DONE = "onboarding_done"
        private const val KEY_PAGE = "page"
        private const val LAST_PAGE = 2
    }
}
