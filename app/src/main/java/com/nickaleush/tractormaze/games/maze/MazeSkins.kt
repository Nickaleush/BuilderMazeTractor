package com.nickaleush.tractormaze.games.maze

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import com.nickaleush.tractormaze.R
import kotlin.math.min

/** Visual + gameplay-flavoured description of a purchasable tractor skin. */
data class MazeSkin(
    val id: String,
    val title: String,
    val bodyColor: Int,
    val cabinColor: Int,
    val accentColor: Int,
    val treadColor: Int,
    val speedMultiplier: Float,
    val description: String,
    @DrawableRes val gameDrawableRes: Int,
    @DrawableRes val shopDrawableRes: Int,
    /** Source image direction correction: the gameplay renderer adds this to the Pac-Man direction angle. */
    val bitmapBaseRotationDegrees: Float = 0f,
    /** Per-skin visual scale for the top-down gameplay bitmap. */
    val gameArtScale: Float = 1.0f
)

object MazeSkins {
    const val DEFAULT_SKIN_ID = "skin_loader"

    val byId: Map<String, MazeSkin> = listOf(
        MazeSkin(
            id = "skin_loader",
            title = "Classic Tractor",
            bodyColor = Color.rgb(255, 193, 7),
            cabinColor = Color.rgb(44, 62, 80),
            accentColor = Color.rgb(255, 111, 0),
            treadColor = Color.rgb(45, 45, 45),
            speedMultiplier = 1.00f,
            description = "Balanced starter tractor.",
            gameDrawableRes = R.drawable.game_skin_default,
            shopDrawableRes = R.drawable.shop_skin_default,
            bitmapBaseRotationDegrees = 0f,
            gameArtScale = 1.08f
        ),
        MazeSkin(
            id = "skin_red_truck",
            title = "Magnet Tractor",
            bodyColor = Color.rgb(255, 193, 7),
            cabinColor = Color.rgb(21, 101, 192),
            accentColor = Color.rgb(229, 57, 53),
            treadColor = Color.rgb(30, 30, 30),
            speedMultiplier = 1.04f,
            description = "Magnet rig with a little more straight-line speed.",
            gameDrawableRes = R.drawable.game_skin_magnet,
            shopDrawableRes = R.drawable.shop_skin_magnet,
            bitmapBaseRotationDegrees = 180f,
            gameArtScale = 1.12f
        ),
        MazeSkin(
            id = "skin_bulldozer",
            title = "Steel Shield",
            bodyColor = Color.rgb(30, 136, 229),
            cabinColor = Color.rgb(96, 125, 139),
            accentColor = Color.rgb(255, 193, 7),
            treadColor = Color.rgb(27, 27, 27),
            speedMultiplier = 0.96f,
            description = "Heavy armored tractor for careful turns.",
            gameDrawableRes = R.drawable.game_skin_shield,
            shopDrawableRes = R.drawable.shop_skin_shield,
            bitmapBaseRotationDegrees = 180f,
            gameArtScale = 1.12f
        ),
        MazeSkin(
            id = "skin_blue_crane",
            title = "Turbo Tractor",
            bodyColor = Color.rgb(30, 136, 229),
            cabinColor = Color.rgb(255, 111, 0),
            accentColor = Color.rgb(255, 213, 79),
            treadColor = Color.rgb(38, 50, 56),
            speedMultiplier = 1.08f,
            description = "Fast turbo model for open routes.",
            gameDrawableRes = R.drawable.game_skin_turbo,
            shopDrawableRes = R.drawable.shop_skin_turbo,
            bitmapBaseRotationDegrees = 0f,
            gameArtScale = 1.08f
        ),
        MazeSkin(
            id = "skin_mixer",
            title = "Wide Plow",
            bodyColor = Color.rgb(244, 67, 54),
            cabinColor = Color.rgb(245, 245, 220),
            accentColor = Color.rgb(158, 158, 158),
            treadColor = Color.rgb(46, 46, 46),
            speedMultiplier = 1.02f,
            description = "Wide front plow with stable handling.",
            gameDrawableRes = R.drawable.game_skin_wide_plow,
            shopDrawableRes = R.drawable.shop_skin_wide_plow,
            bitmapBaseRotationDegrees = 0f,
            gameArtScale = 1.08f
        )
    ).associateBy { it.id }

    fun resolve(id: String?): MazeSkin = byId[id] ?: byId.getValue(DEFAULT_SKIN_ID)
}

data class MazeTheme(
    val id: String,
    val title: String,
    val backgroundTop: Int,
    val backgroundBottom: Int,
    val ground: Int,
    val path: Int,
    val wall: Int,
    val gridLine: Int,
    val exit: Int
)

object MazeThemes {
    const val DEFAULT_THEME_ID = "bg_build_site"

    val byId: Map<String, MazeTheme> = listOf(
        MazeTheme("bg_build_site", "Build Site", Color.rgb(135, 206, 235), Color.rgb(210, 146, 61), Color.rgb(151, 110, 70), Color.rgb(233, 186, 96), Color.rgb(140, 143, 143), Color.rgb(106, 88, 60), Color.rgb(0, 151, 167)),
        MazeTheme("bg_sunset_yard", "Sunset Yard", Color.rgb(255, 171, 145), Color.rgb(121, 85, 72), Color.rgb(135, 86, 57), Color.rgb(230, 169, 89), Color.rgb(122, 113, 108), Color.rgb(92, 70, 56), Color.rgb(0, 137, 123)),
        MazeTheme("bg_quarry", "Rock Quarry", Color.rgb(176, 190, 197), Color.rgb(84, 110, 122), Color.rgb(126, 115, 104), Color.rgb(205, 190, 152), Color.rgb(102, 113, 120), Color.rgb(80, 74, 68), Color.rgb(38, 166, 154)),
        MazeTheme("bg_night_shift", "Night Shift", Color.rgb(25, 39, 77), Color.rgb(35, 49, 65), Color.rgb(67, 63, 58), Color.rgb(184, 143, 84), Color.rgb(93, 99, 107), Color.rgb(52, 45, 39), Color.rgb(38, 198, 218)),
        MazeTheme("bg_winter_site", "Winter Site", Color.rgb(187, 222, 251), Color.rgb(225, 245, 254), Color.rgb(151, 167, 176), Color.rgb(232, 221, 181), Color.rgb(120, 144, 156), Color.rgb(104, 120, 128), Color.rgb(0, 172, 193)),
        MazeTheme("bg_desert_site", "Desert Site", Color.rgb(255, 213, 79), Color.rgb(188, 110, 55), Color.rgb(164, 116, 73), Color.rgb(241, 188, 109), Color.rgb(139, 117, 92), Color.rgb(111, 84, 58), Color.rgb(0, 150, 136))
    ).associateBy { it.id }

    fun resolve(id: String?): MazeTheme = byId[id] ?: byId.getValue(DEFAULT_THEME_ID)
}

/** Shared tractor renderer used by gameplay, shop previews, and onboarding. */
class MazeTractorRenderer(private val context: Context? = null) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val rect = RectF()
    private val path = Path()
    private val bitmapCache = mutableMapOf<Int, Bitmap?>()

    fun draw(canvas: Canvas, skin: MazeSkin, cx: Float, cy: Float, size: Float, rotationDegrees: Float = 0f) {
        val bitmap = bitmapFor(skin.gameDrawableRes)
        if (bitmap != null) {
            drawBitmapSkin(canvas, bitmap, skin, cx, cy, size, rotationDegrees)
            return
        }
        drawProcedural(canvas, skin, cx, cy, size, rotationDegrees)
    }

    private fun bitmapFor(@DrawableRes resId: Int): Bitmap? {
        if (context == null || resId == 0) return null
        if (!bitmapCache.containsKey(resId)) {
            bitmapCache[resId] = BitmapFactory.decodeResource(context.resources, resId)
        }
        return bitmapCache[resId]
    }

    private fun drawBitmapSkin(
        canvas: Canvas,
        bitmap: Bitmap,
        skin: MazeSkin,
        cx: Float,
        cy: Float,
        size: Float,
        rotationDegrees: Float
    ) {
        val maxSide = size * skin.gameArtScale
        val scale = min(maxSide / bitmap.width.toFloat(), maxSide / bitmap.height.toFloat())
        val targetW = bitmap.width * scale
        val targetH = bitmap.height * scale
        rect.set(cx - targetW / 2f, cy - targetH / 2f, cx + targetW / 2f, cy + targetH / 2f)
        canvas.save()
        canvas.rotate(rotationDegrees + skin.bitmapBaseRotationDegrees, cx, cy)
        canvas.drawBitmap(bitmap, null, rect, paint)
        canvas.restore()
    }

    private fun drawProcedural(canvas: Canvas, skin: MazeSkin, cx: Float, cy: Float, size: Float, rotationDegrees: Float = 0f) {
        canvas.save()
        canvas.rotate(rotationDegrees, cx, cy)
        val half = size / 2f
        val left = cx - half
        val top = cy - half
        val right = cx + half
        val bottom = cy + half

        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = skin.treadColor
        rect.set(left + size * 0.10f, top + size * 0.58f, right - size * 0.10f, bottom - size * 0.07f)
        canvas.drawRoundRect(rect, size * 0.10f, size * 0.10f, paint)
        rect.set(left + size * 0.10f, top + size * 0.10f, right - size * 0.10f, top + size * 0.36f)
        canvas.drawRoundRect(rect, size * 0.10f, size * 0.10f, paint)

        paint.color = skin.bodyColor
        rect.set(left + size * 0.18f, top + size * 0.26f, right - size * 0.18f, bottom - size * 0.18f)
        canvas.drawRoundRect(rect, size * 0.12f, size * 0.12f, paint)

        paint.color = skin.cabinColor
        rect.set(left + size * 0.25f, top + size * 0.18f, right - size * 0.25f, top + size * 0.50f)
        canvas.drawRoundRect(rect, size * 0.10f, size * 0.10f, paint)

        paint.color = Color.argb(185, 220, 245, 255)
        rect.set(left + size * 0.34f, top + size * 0.23f, right - size * 0.34f, top + size * 0.43f)
        canvas.drawRoundRect(rect, size * 0.05f, size * 0.05f, paint)

        paint.color = skin.accentColor
        path.reset()
        path.moveTo(left + size * 0.22f, top + size * 0.16f)
        path.lineTo(cx, top - size * 0.10f)
        path.lineTo(right - size * 0.22f, top + size * 0.16f)
        path.close()
        canvas.drawPath(path, paint)

        // front bucket / blade
        rect.set(left + size * 0.12f, bottom - size * 0.20f, right - size * 0.12f, bottom + size * 0.06f)
        canvas.drawRoundRect(rect, size * 0.06f, size * 0.06f, paint)

        stroke.color = Color.argb(150, 0, 0, 0)
        stroke.strokeWidth = size * 0.045f
        rect.set(left + size * 0.18f, top + size * 0.26f, right - size * 0.18f, bottom - size * 0.18f)
        canvas.drawRoundRect(rect, size * 0.12f, size * 0.12f, stroke)
        canvas.restore()
    }
}
