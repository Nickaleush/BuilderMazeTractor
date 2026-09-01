package com.nickaleush.tractormaze.feature.shop

import androidx.annotation.DrawableRes
import com.nickaleush.tractormaze.R

/** Ready-made shop badges for tractor skins. Backgrounds still use MazePreviewView. */
object ShopArtwork {
    private val badges: Map<String, Int> = mapOf(
        "skin_loader" to R.drawable.shop_skin_default,
        "skin_red_truck" to R.drawable.shop_skin_magnet,
        "skin_bulldozer" to R.drawable.shop_skin_shield,
        "skin_blue_crane" to R.drawable.shop_skin_turbo,
        "skin_mixer" to R.drawable.shop_skin_wide_plow
    )

    @DrawableRes
    fun badgeFor(itemId: String): Int = badges[itemId] ?: 0
}
