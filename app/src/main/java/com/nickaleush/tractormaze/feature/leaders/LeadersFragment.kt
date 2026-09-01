package com.nickaleush.tractormaze.feature.leaders

import android.graphics.Typeface
import android.graphics.text.LineBreaker
import android.os.Build
import android.os.Bundle
import android.text.Layout
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.nickaleush.tractormaze.App
import com.nickaleush.tractormaze.R
import com.nickaleush.tractormaze.core.audio.SoundManager
import com.nickaleush.tractormaze.data.db.entity.AchievementEntity
import com.nickaleush.tractormaze.databinding.FragmentLeadersBinding
import kotlinx.coroutines.launch

class LeadersFragment : Fragment(R.layout.fragment_leaders) {

    private var _binding: FragmentLeadersBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: LeadersViewModel by viewModels {
        val app = requireActivity().application as App
        LeadersViewModelFactory(app.serviceLocator.gameRepository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLeadersBinding.bind(view)
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        observe()
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
        (requireActivity().application as App).serviceLocator.soundManager
            .playMusic(SoundManager.MusicTrack.Menu)
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.achievements.collect { render() } }
            }
        }
    }

    private fun render() {
        binding.leadersContainer.removeAllViews()
        val achievements = viewModel.achievements.value
        if (achievements.isEmpty()) {
            addText("No achievements yet. Keep playing to unlock them.")
        } else {
            achievements.forEach(::addAchievement)
        }
    }

    private fun addText(text: String) {
        binding.leadersContainer.addView(TextView(requireContext()).apply {
            this.text = text
            setTextColor(resources.getColor(R.color.tractor_text_primary, null))
            textSize = 16f

            setPadding(dp(18))
        })
    }

    private fun addAchievement(achievement: AchievementEntity) {
        val status = if (achievement.isUnlocked) {
            getString(R.string.leaders_achievement_done)
        } else {
            getString(
                R.string.leaders_achievement_progress_format,
                achievement.currentValue,
                achievement.targetValue
            )
        }

        // ---- Card: fixed 200dp height, item_bg background ----
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(if (achievement.isUnlocked) R.drawable.item_bg else R.drawable.items_disabled_bg)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(200)
            )
            params.setMargins(0, 0, 0, dp(14))
            layoutParams = params
        }

        // ---- Trophy icon (large, on the left) ----
        val trophy = ImageView(requireContext()).apply {
            setImageResource(if (achievement.isUnlocked) R.drawable.ui_achievement_icon else R.drawable.ui_lock_icon)
            val iconSize = dp(110)
            val params = LinearLayout.LayoutParams(iconSize, iconSize)
            params.marginEnd = dp(16)
            layoutParams = params
            alpha = 1f
        }

        // ---- Text column (title + description), centered ----
        val textColumn = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        // Title: large gold text, auto-shrinks to fit on one line, words never split.
        textColumn.addView(TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = achievement.title
            gravity = Gravity.CENTER_HORIZONTAL
            setTextColor(resources.getColor(R.color.tractor_button_primary, null))
            typeface = ResourcesCompat.getFont(requireContext(), R.font.science_gothic_expanded_bold) ?: Typeface.DEFAULT_BOLD
            maxLines = 1
            isSingleLine = true
            if (Build.VERSION.SDK_INT >= 23) {
                breakStrategy = LineBreaker.BREAK_STRATEGY_SIMPLE
                hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
            }
            // Autosize: shrinks from 26sp down to 12sp until it fits the width.
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                this, 12, 26, 1, TypedValue.COMPLEX_UNIT_SP
            )
        })
        // Description below the title, auto-shrinks, wraps by words (max 2 lines).
        textColumn.addView(TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = achievement.description
            gravity = Gravity.CENTER_HORIZONTAL
            setTextColor(resources.getColor(R.color.white, null))
            typeface = ResourcesCompat.getFont(requireContext(), R.font.science_gothic_expanded_bold) ?: Typeface.DEFAULT_BOLD
            setPadding(0, dp(4), 0, 0)
            maxLines = 2
            if (Build.VERSION.SDK_INT >= 23) {
                breakStrategy = LineBreaker.BREAK_STRATEGY_SIMPLE
                hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
            }
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                this, 11, 18, 1, TypedValue.COMPLEX_UNIT_SP
            )
        })
        // Optional progress / DONE line (small, dimmer), centered.
        textColumn.addView(TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = status
            gravity = Gravity.CENTER_HORIZONTAL
            setTextColor(resources.getColor(R.color.tractor_text_secondary, null))
            textSize = 14f
            setPadding(0, dp(6), 0, 0)
        })

        card.addView(trophy)
        card.addView(textColumn)
        binding.leadersContainer.addView(card)
    }

    /** dp -> px helper. */
    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}