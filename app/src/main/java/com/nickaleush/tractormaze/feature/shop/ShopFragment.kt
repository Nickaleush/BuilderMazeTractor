package com.nickaleush.tractormaze.feature.shop

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.nickaleush.tractormaze.App
import com.nickaleush.tractormaze.R
import com.nickaleush.tractormaze.core.audio.SoundManager
import com.nickaleush.tractormaze.data.db.entity.InventoryItemEntity
import com.nickaleush.tractormaze.data.repository.GameRepository
import com.nickaleush.tractormaze.databinding.FragmentShopBinding
import com.nickaleush.tractormaze.databinding.ItemShopCardBinding
import kotlinx.coroutines.launch

class ShopFragment : Fragment(R.layout.fragment_shop) {

    private var _binding: FragmentShopBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: ShopViewModel by viewModels {
        val app = requireActivity().application as App
        ShopViewModelFactory(app.serviceLocator.gameRepository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentShopBinding.bind(view)
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.shopContainer.columnCount = 2
        observe()
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
        // Keep the menu background music playing on this screen.
        (requireActivity().application as App).serviceLocator.soundManager
            .playMusic(SoundManager.MusicTrack.Menu)
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.coins.collect { coins ->
                        binding.coinsTextView.text = getString(R.string.shop_coins_format, coins)
                    }
                }
                launch {
                    viewModel.items.collect { renderItems(it) }
                }
                launch {
                    viewModel.messages.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun renderItems(items: List<InventoryItemEntity>) {
        if (_binding == null) return
        binding.shopContainer.removeAllViews()

        // The shop screen is now a compact two-column garage grid.
        // Only tractor skins are shown here, like in the design mockup.
        items.filter { it.type == GameRepository.TYPE_SKIN }
            .forEach(::addItemCard)
    }

    private fun addItemCard(item: InventoryItemEntity) {
        val inflater = LayoutInflater.from(requireContext())
        val cardBinding = ItemShopCardBinding.inflate(
            inflater, binding.shopContainer, false
        )

        cardBinding.itemTitleTextView.text = displayTitle(item)

        val badgeRes = ShopArtwork.badgeFor(item.id)
        if (badgeRes != 0) {
            cardBinding.itemBadgeImageView.setImageResource(badgeRes)
        }

        val ownedButtonBackground = ResourcesCompat.getDrawable(resources, R.drawable.item_bg, null)
        val buyButtonBackground = ResourcesCompat.getDrawable(resources, R.drawable.bg_shop_buy_button, null)

        if (item.isUnlocked) {
            cardBinding.itemPricePlate.visibility = View.GONE
            cardBinding.itemActionButton.visibility = View.VISIBLE
            cardBinding.itemActionButton.background = ownedButtonBackground
            cardBinding.itemActionButton.text = if (item.isSelected) {
                getString(R.string.shop_button_equipped)
            } else {
                getString(R.string.shop_button_equip)
            }
            cardBinding.itemActionButton.isEnabled = !item.isSelected
            cardBinding.itemActionButton.alpha = 1f
            cardBinding.itemActionButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            cardBinding.itemActionButton.setOnClickListener {
                viewModel.buyOrSelect(item.id)
            }
            cardBinding.root.setOnClickListener(null)
        } else {
            cardBinding.itemPricePlate.visibility = View.VISIBLE
            cardBinding.itemPricePlate.background = buyButtonBackground
            cardBinding.itemActionButton.visibility = View.GONE
            cardBinding.itemPriceTextView.text = item.price.toString()
            cardBinding.root.setOnClickListener { viewModel.buyOrSelect(item.id) }
            cardBinding.itemPricePlate.setOnClickListener { viewModel.buyOrSelect(item.id) }
        }

        val marginHorizontal = dp(3)
        val marginVertical = dp(5)
        val screenWidth = resources.displayMetrics.widthPixels
        val rootHorizontalPadding = dp(12)
        val rowMargins = marginHorizontal * 4
        val cardWidth = ((screenWidth - rootHorizontalPadding - rowMargins) / 2).coerceAtLeast(dp(136))
        val cardHeight = (cardWidth * 1.42f).toInt()

        val params = GridLayout.LayoutParams().apply {
            width = cardWidth
            height = cardHeight
            setMargins(marginHorizontal, marginVertical, marginHorizontal, marginVertical + dp(3))
        }
        cardBinding.root.layoutParams = params
        binding.shopContainer.addView(cardBinding.root)
    }

    private fun displayTitle(item: InventoryItemEntity): String = when (item.id) {
        "skin_loader" -> "BASIC\nTRACTOR"
        "skin_blue_crane" -> "SPEED\nBOOST"
        "skin_mixer" -> "WIDE\nPLOW"
        "skin_red_truck" -> "MAGNET\nCAB"
        "skin_bulldozer" -> "STEEL\nSHIELD"
        else -> item.title.uppercase()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
