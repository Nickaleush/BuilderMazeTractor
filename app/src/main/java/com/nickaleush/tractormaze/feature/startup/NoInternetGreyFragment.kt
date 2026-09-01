package com.nickaleush.tractormaze.feature.startup

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nickaleush.tractormaze.App
import com.nickaleush.tractormaze.R
import com.nickaleush.tractormaze.core.audio.SoundManager
import com.nickaleush.tractormaze.databinding.NoInternetGreyFragmentBinding

class NoInternetGreyFragment : Fragment(R.layout.no_internet_grey_fragment) {

    private var _binding: NoInternetGreyFragmentBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = NoInternetGreyFragmentBinding.bind(view)
        binding.retryButton.setOnClickListener { retry() }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity().application as App).serviceLocator.soundManager
            .playMusic(SoundManager.MusicTrack.Menu)
    }

    private fun retry() {
        if (!StartupGate.isInternetAvailable(requireContext())) {
            Toast.makeText(requireContext(), R.string.no_internet_toast, Toast.LENGTH_SHORT).show()
            return
        }
        val action = when {
            StartupGate.shouldShowNotificationPrompt(requireContext()) -> R.id.action_noInternetGreyFragment_to_onBoardingGreyFragment
            StartupGate.isGameOnboardingDone(requireContext()) -> R.id.action_noInternetGreyFragment_to_menuFragment
            else -> R.id.action_noInternetGreyFragment_to_onboardingFragment
        }
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
