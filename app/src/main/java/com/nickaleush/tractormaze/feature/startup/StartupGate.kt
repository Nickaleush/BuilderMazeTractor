package com.nickaleush.tractormaze.feature.startup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.ContextCompat
import com.nickaleush.tractormaze.feature.onboarding.OnboardingFragment

object StartupGate {
    private const val KEY_NOTIFICATION_GATE_DONE = "notification_gate_done"

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun shouldShowNotificationPrompt(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) return false
        return !context.startupPrefs().getBoolean(KEY_NOTIFICATION_GATE_DONE, false)
    }

    fun markNotificationPromptDone(context: Context) {
        context.startupPrefs()
            .edit()
            .putBoolean(KEY_NOTIFICATION_GATE_DONE, true)
            .apply()
    }

    fun isGameOnboardingDone(context: Context): Boolean = context.startupPrefs()
        .getBoolean(OnboardingFragment.KEY_DONE, false)

    private fun Context.startupPrefs() = getSharedPreferences(
        OnboardingFragment.PREFS,
        Context.MODE_PRIVATE
    )
}
