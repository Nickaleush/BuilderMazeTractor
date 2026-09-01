package com.nickaleush.tractormaze.feature.startup

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nickaleush.tractormaze.App
import com.nickaleush.tractormaze.R
import com.nickaleush.tractormaze.core.audio.SoundManager
import com.nickaleush.tractormaze.databinding.OnBoardingGreyFragmentBinding

class OnBoardingGreyFragment : Fragment(R.layout.on_boarding_grey_fragment) {

    private var _binding: OnBoardingGreyFragmentBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        StartupGate.markNotificationPromptDone(requireContext())
        navigateNext()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = OnBoardingGreyFragmentBinding.bind(view)
        binding.allowButton.setOnClickListener { requestNotifications() }
        binding.skipButton.setOnClickListener {
            StartupGate.markNotificationPromptDone(requireContext())
            navigateNext()
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity().application as App).serviceLocator.soundManager
            .playMusic(SoundManager.MusicTrack.Menu)
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            StartupGate.markNotificationPromptDone(requireContext())
            navigateNext()
            return
        }

        val permission = Manifest.permission.POST_NOTIFICATIONS
        val granted = ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            StartupGate.markNotificationPromptDone(requireContext())
            navigateNext()
        } else {
            notificationPermissionLauncher.launch(permission)
        }
    }

    private fun navigateNext() {
        if (!isAdded) return
        val action = when {
            !StartupGate.isInternetAvailable(requireContext()) -> R.id.action_onBoardingGreyFragment_to_noInternetGreyFragment
            StartupGate.isGameOnboardingDone(requireContext()) -> R.id.action_onBoardingGreyFragment_to_menuFragment
            else -> R.id.action_onBoardingGreyFragment_to_onboardingFragment
        }
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
