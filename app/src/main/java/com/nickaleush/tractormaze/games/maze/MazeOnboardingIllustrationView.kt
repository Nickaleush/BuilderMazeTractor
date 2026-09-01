package com.nickaleush.tractormaze.games.maze

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/** Framed tutorial picture: gameplay snippet + builder in a hard hat. */
class MazeOnboardingIllustrationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val rect = RectF()
    private val path = Path()
    private val tractorRenderer = MazeTractorRenderer(context)
    private var page = 0

    fun setPage(index: Int) {
        page = index.coerceIn(0, 2)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        paint.shader = LinearGradient(0f, 0f, 0f, h, Color.rgb(143, 202, 235), Color.rgb(214, 146, 66), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        val frameW = w * 0.72f
        val frameH = h * 0.70f
        val frameLeft = w * 0.05f
        val frameTop = h * 0.12f
        rect.set(frameLeft, frameTop, frameLeft + frameW, frameTop + frameH)
        paint.color = Color.rgb(74, 74, 74)
        canvas.drawRoundRect(rect, 28f, 28f, paint)
        rect.inset(10f, 10f)
        paint.color = Color.rgb(238, 187, 97)
        canvas.drawRoundRect(rect, 18f, 18f, paint)

        drawMiniMaze(canvas, rect)
        drawBuilder(canvas, w * 0.80f, h * 0.70f, min(w, h) * 0.18f)
    }

    private fun drawMiniMaze(canvas: Canvas, bounds: RectF) {
        val rows = 5
        val cols = 5
        val tile = min(bounds.width() / cols, bounds.height() / rows)
        val left = bounds.left + (bounds.width() - tile * cols) / 2f
        val top = bounds.top + (bounds.height() - tile * rows) / 2f
        val map = when (page) {
            0 -> listOf(".....", ".s#..", "..T..", "..#w.", "..E..")
            1 -> listOf("..E..", ".###.", "..T..", ".#.#.", "..w..")
            else -> listOf("..E..", ".s.w.", "..T..", ".o.p.", "..b..")
        }
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val l = left + c * tile
                val t = top + r * tile
                rect.set(l, t, l + tile, t + tile)
                paint.color = if (map[r][c] == '#') Color.rgb(136, 139, 139) else Color.rgb(234, 185, 94)
                canvas.drawRect(rect, paint)
                stroke.color = Color.argb(90, 96, 75, 45)
                stroke.strokeWidth = 2f
                canvas.drawRect(rect, stroke)
                when (map[r][c]) {
                    'T' -> tractorRenderer.draw(canvas, MazeSkins.resolve(null), l + tile / 2f, t + tile / 2f, tile * 0.72f, when (page) { 0 -> 90f; 1 -> 0f; else -> 180f })
                    's', 'w', 'b' -> drawMaterialDot(canvas, l + tile / 2f, t + tile / 2f, tile, map[r][c])
                    'o', 'p' -> drawDanger(canvas, l + tile / 2f, t + tile / 2f, tile, map[r][c])
                    'E' -> drawGate(canvas, l + tile / 2f, t + tile / 2f, tile)
                }
            }
        }
        if (page == 0 || page == 1) drawSwipeArrow(canvas, left + tile * 2.5f, top + tile * 0.45f, tile)
    }

    private fun drawMaterialDot(canvas: Canvas, cx: Float, cy: Float, tile: Float, ch: Char) {
        paint.color = when (ch) {
            's' -> Color.rgb(117, 117, 117)
            'w' -> Color.rgb(126, 74, 35)
            else -> Color.rgb(196, 73, 35)
        }
        canvas.drawCircle(cx, cy, tile * 0.15f, paint)
    }

    private fun drawDanger(canvas: Canvas, cx: Float, cy: Float, tile: Float, ch: Char) {
        paint.color = if (ch == 'p') Color.rgb(35, 25, 21) else Color.rgb(183, 28, 28)
        rect.set(cx - tile * 0.18f, cy - tile * 0.18f, cx + tile * 0.18f, cy + tile * 0.18f)
        canvas.drawOval(rect, paint)
    }

    private fun drawGate(canvas: Canvas, cx: Float, cy: Float, tile: Float) {
        paint.color = Color.rgb(0, 151, 167)
        rect.set(cx - tile * 0.16f, cy - tile * 0.28f, cx + tile * 0.16f, cy + tile * 0.28f)
        canvas.drawRoundRect(rect, tile * 0.04f, tile * 0.04f, paint)
    }

    private fun drawSwipeArrow(canvas: Canvas, cx: Float, cy: Float, tile: Float) {
        paint.color = Color.argb(230, 255, 255, 255)
        path.reset()
        path.moveTo(cx + tile * 0.55f, cy)
        path.lineTo(cx + tile * 0.20f, cy - tile * 0.18f)
        path.lineTo(cx + tile * 0.20f, cy + tile * 0.18f)
        path.close()
        canvas.drawPath(path, paint)
        stroke.color = Color.WHITE
        stroke.strokeWidth = tile * 0.10f
        canvas.drawLine(cx - tile * 0.50f, cy, cx + tile * 0.25f, cy, stroke)
    }

    private fun drawBuilder(canvas: Canvas, cx: Float, footY: Float, size: Float) {
        paint.color = Color.rgb(255, 204, 128)
        canvas.drawCircle(cx, footY - size * 2.2f, size * 0.45f, paint)
        paint.color = Color.rgb(255, 193, 7)
        rect.set(cx - size * 0.55f, footY - size * 2.65f, cx + size * 0.55f, footY - size * 2.25f)
        canvas.drawRoundRect(rect, size * 0.18f, size * 0.18f, paint)
        rect.set(cx - size * 0.70f, footY - size * 2.35f, cx + size * 0.70f, footY - size * 2.22f)
        canvas.drawRoundRect(rect, size * 0.07f, size * 0.07f, paint)
        paint.color = Color.rgb(55, 71, 79)
        canvas.drawCircle(cx - size * 0.16f, footY - size * 2.22f, size * 0.04f, paint)
        canvas.drawCircle(cx + size * 0.16f, footY - size * 2.22f, size * 0.04f, paint)
        paint.color = Color.rgb(30, 136, 229)
        rect.set(cx - size * 0.42f, footY - size * 1.80f, cx + size * 0.42f, footY - size * 0.70f)
        canvas.drawRoundRect(rect, size * 0.16f, size * 0.16f, paint)
        stroke.color = Color.rgb(13, 71, 161)
        stroke.strokeWidth = size * 0.08f
        canvas.drawLine(cx - size * 0.18f, footY - size * 1.76f, cx - size * 0.18f, footY - size * 0.72f, stroke)
        canvas.drawLine(cx + size * 0.18f, footY - size * 1.76f, cx + size * 0.18f, footY - size * 0.72f, stroke)
        stroke.color = Color.rgb(255, 204, 128)
        stroke.strokeWidth = size * 0.18f
        canvas.drawLine(cx - size * 0.42f, footY - size * 1.55f, cx - size * 0.72f, footY - size * 1.00f, stroke)
        canvas.drawLine(cx + size * 0.42f, footY - size * 1.55f, cx + size * 0.72f, footY - size * 1.00f, stroke)
        stroke.color = Color.rgb(55, 71, 79)
        stroke.strokeWidth = size * 0.16f
        canvas.drawLine(cx - size * 0.18f, footY - size * 0.72f, cx - size * 0.34f, footY, stroke)
        canvas.drawLine(cx + size * 0.18f, footY - size * 0.72f, cx + size * 0.34f, footY, stroke)
    }
}
