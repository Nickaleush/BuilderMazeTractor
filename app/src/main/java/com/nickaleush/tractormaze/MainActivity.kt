package com.nickaleush.tractormaze

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.NavHostFragment
import com.nickaleush.tractormaze.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarsInsets()
        configureOrientationByDestination()
    }

    private fun applySystemBarsInsets() {
        val navHost = binding.navHostFragment

        val initialLeft = navHost.paddingLeft
        val initialTop = navHost.paddingTop
        val initialRight = navHost.paddingRight
        val initialBottom = navHost.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(navHost) { view, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )

            view.updatePadding(
                left = initialLeft + systemBars.left,
                top = initialTop + systemBars.top,
                right = initialRight + systemBars.right,
                bottom = initialBottom + systemBars.bottom
            )

            WindowInsetsCompat.CONSUMED
        }
    }

    private fun configureOrientationByDestination() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navHostFragment.navController.addOnDestinationChangedListener { _, destination, _ ->
            requestedOrientation = when (destination.id) {
                R.id.splashFragment,
                R.id.onBoardingGreyFragment,
                R.id.noInternetGreyFragment -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    override fun onStop() {
        (application as App).serviceLocator.soundManager.pauseMusic()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        (application as App).serviceLocator.soundManager.resumeMusic()
    }

    private data class InitialPadding(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )
}
