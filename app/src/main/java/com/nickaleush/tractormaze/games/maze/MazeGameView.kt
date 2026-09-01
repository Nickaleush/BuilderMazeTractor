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
import android.graphics.Typeface
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.nickaleush.tractormaze.R
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Pac-Man-like tile movement on a construction-site maze. */
class MazeGameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface Callback {
        fun onHudChanged(level: Int, counters: MazeMaterialCounters, score: Int, lives: Int)
        fun onCollectSound()
        fun onTurnSound()
        fun onCrashSound()
        fun onGameFinished(result: MazeGameResult)
    }

    var callback: Callback? = null

    private enum class Direction(val rowDelta: Int, val colDelta: Int, val angle: Float) {
        Up(-1, 0, 0f), Right(0, 1, 90f), Down(1, 0, 180f), Left(0, -1, 270f), None(0, 0, 0f)
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val path = Path()
    private val tractorRenderer = MazeTractorRenderer(context)
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val bitmapCache = mutableMapOf<Int, Bitmap?>()
    private val hudTypeface: Typeface = ResourcesCompat.getFont(context, R.font.science_gothic_expanded_bold)
        ?: Typeface.DEFAULT_BOLD

    private var config: MazeLevelConfig = MazeLevelConfig.forLevel(1)
    private var skin: MazeSkin = MazeSkins.resolve(null)
    private var theme: MazeTheme = MazeThemes.resolve(null)

    private var tileSize = 1f
    private var boardLeft = 0f
    private var boardTop = 0f
    private var boardWidth = 0f
    private var boardHeight = 0f

    private var x = 0f
    private var y = 0f
    private var direction = Direction.Up
    private var pendingDirection = Direction.Up
    private var running = false
    private var paused = false
    private var finished = false
    private var lastFrameUptime = 0L
    private var startUptime = 0L

    private val remainingMaterials = linkedMapOf<TilePos, MaterialType>()
    private val totalByType = mutableMapOf<MaterialType, Int>()
    private val collectedByType = mutableMapOf<MaterialType, Int>()
    private var totalMaterials = 0
    private var collectedMaterials = 0
    private var score = 0
    private var lives = 1
    private var turns = 0
    private var collisions = 0

    private var downX = 0f
    private var downY = 0f

    private val loopRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            val now = SystemClock.uptimeMillis()
            if (lastFrameUptime == 0L) lastFrameUptime = now
            val dt = ((now - lastFrameUptime) / 1000f).coerceIn(0f, 0.08f)
            lastFrameUptime = now
            if (!paused && !finished) update(dt)
            postInvalidateOnAnimation()
            postOnAnimation(this)
        }
    }

    fun setAppearance(skinId: String, backgroundId: String) {
        skin = MazeSkins.resolve(skinId)
        theme = MazeThemes.resolve(backgroundId)
        invalidate()
    }

    fun startLevel(level: Int) {
        config = MazeLevelConfig.forLevel(level)
        remainingMaterials.clear()
        config.materials.forEach { remainingMaterials[it.pos] = it.type }
        totalByType.clear()
        collectedByType.clear()
        MaterialType.values().forEach { type ->
            totalByType[type] = config.materials.count { it.type == type }
            collectedByType[type] = 0
        }
        totalMaterials = remainingMaterials.size
        collectedMaterials = 0
        score = 0
        lives = 1
        turns = 0
        collisions = 0
        x = config.start.col + 0.5f
        y = config.start.row + 0.5f
        direction = chooseInitialDirection()
        pendingDirection = direction
        paused = false
        finished = false
        running = true
        lastFrameUptime = 0L
        startUptime = SystemClock.uptimeMillis()
        callback?.onHudChanged(config.level, currentCounters(), score, lives)
        removeCallbacks(loopRunnable)
        postOnAnimation(loopRunnable)
        invalidate()
    }

    fun restart() = startLevel(config.level)

    fun stopGameLoop() {
        running = false
        removeCallbacks(loopRunnable)
    }

    fun setPaused(isPaused: Boolean) {
        paused = isPaused
        if (!paused) lastFrameUptime = 0L
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val dx = event.x - downX
                val dy = event.y - downY
                val threshold = 28f * resources.displayMetrics.density
                if (max(abs(dx), abs(dy)) >= threshold) {
                    pendingDirection = if (abs(dx) > abs(dy)) {
                        if (dx > 0) Direction.Right else Direction.Left
                    } else {
                        if (dy > 0) Direction.Down else Direction.Up
                    }
                }
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return true
    }

    private fun update(dt: Float) {
        val speed = config.baseSpeedTilesPerSecond * skin.speedMultiplier
        var remainingDistance = speed * dt
        var guard = 0

        // Move center-to-center so a slow frame never skips the tile center
        // where collection, queued turning, and collision checks must happen.
        while (remainingDistance > 0f && !finished && guard < MAX_TILE_STEPS_PER_FRAME) {
            guard++

            if (isAtTileCenter()) {
                snapToTileCenter()
                if (!handleTileCenter()) return
            }

            val distanceToCenter = distanceToNextTileCenter()
            if (distanceToCenter <= CENTER_EPSILON) {
                snapToTileCenter()
                continue
            }

            val step = min(remainingDistance, distanceToCenter)
            x += direction.colDelta * step
            y += direction.rowDelta * step
            remainingDistance -= step

            if (step >= distanceToCenter - CENTER_EPSILON) {
                snapToTileCenter()
            }
        }
    }

    private fun handleTileCenter(): Boolean {
        val row = currentRow()
        val col = currentCol()
        collectAt(row, col)

        if (row == config.exit.row && col == config.exit.col && remainingMaterials.isEmpty()) {
            finishLevel(passed = true)
            return false
        }

        // Pac-Man style controls:
        // 1) a swipe queues the desired direction;
        // 2) the queued turn is applied only on a tile center and only when the next tile is free;
        // 3) if the tractor keeps moving into any blocked tile (wall, obstacle, border, closed exit), it crashes.
        // There is no silent stop state during gameplay: otherwise the tractor can get stuck at a wall and
        // the player cannot reliably understand whether it is alive, paused, or waiting for another swipe.
        if (pendingDirection != Direction.None && canMove(row, col, pendingDirection)) {
            if (pendingDirection != direction) {
                turns++
                callback?.onTurnSound()
            }
            direction = pendingDirection
        }

        if (direction == Direction.None || !canMove(row, col, direction)) {
            crash()
            return false
        }
        return true
    }

    private fun collectAt(row: Int, col: Int) {
        val pos = TilePos(row, col)
        val removed = remainingMaterials.remove(pos) ?: return
        collectedMaterials++
        collectedByType[removed] = collectedByType.getValue(removed) + 1
        score += when (removed) {
            MaterialType.Stone -> 90
            MaterialType.Wood -> 110
            MaterialType.Bricks -> 130
        } + config.level * 2
        callback?.onCollectSound()
        callback?.onHudChanged(config.level, currentCounters(), score, lives)
    }

    private fun currentCounters(): MazeMaterialCounters = MazeMaterialCounters(
        stonesCollected = collectedByType[MaterialType.Stone] ?: 0,
        stonesTotal = totalByType[MaterialType.Stone] ?: 0,
        woodCollected = collectedByType[MaterialType.Wood] ?: 0,
        woodTotal = totalByType[MaterialType.Wood] ?: 0,
        bricksCollected = collectedByType[MaterialType.Bricks] ?: 0,
        bricksTotal = totalByType[MaterialType.Bricks] ?: 0
    )

    private fun chooseInitialDirection(): Direction {
        val row = config.start.row
        val col = config.start.col
        val targets = if (remainingMaterials.isNotEmpty()) {
            remainingMaterials.keys
        } else {
            setOf(config.exit)
        }

        // Do not simply prefer Up. Some repaired/generated levels may have a safe tile above the spawn
        // that immediately ends at a wall. Start along the shortest route to an actual objective instead.
        return firstStepToNearestTarget(row, col, targets)
            ?: INITIAL_DIRECTIONS.firstOrNull { canMove(row, col, it) }
            ?: Direction.None
    }

    private fun firstStepToNearestTarget(row: Int, col: Int, targets: Set<TilePos>): Direction? {
        if (targets.isEmpty()) return null
        val start = TilePos(row, col)
        val queue = ArrayDeque<TilePos>()
        val visited = mutableSetOf(start)
        val firstStepByPos = mutableMapOf<TilePos, Direction>()
        queue.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val firstStep = firstStepByPos[current]
            if (current != start && current in targets && firstStep != null) {
                return firstStep
            }

            INITIAL_DIRECTIONS.forEach { dir ->
                val next = TilePos(current.row + dir.rowDelta, current.col + dir.colDelta)
                if (next in visited) return@forEach
                if (!canEnterForRouting(next.row, next.col)) return@forEach
                visited.add(next)
                firstStepByPos[next] = firstStep ?: dir
                queue.add(next)
            }
        }
        return null
    }

    private fun canMove(row: Int, col: Int, dir: Direction): Boolean {
        if (dir == Direction.None) return false
        val nextRow = row + dir.rowDelta
        val nextCol = col + dir.colDelta
        if (nextRow == config.exit.row && nextCol == config.exit.col && remainingMaterials.isNotEmpty()) {
            return false
        }
        return !config.isBlocked(nextRow, nextCol)
    }

    private fun canEnterForRouting(row: Int, col: Int): Boolean {
        if (!config.isInside(row, col)) return false
        if (row == config.exit.row && col == config.exit.col && remainingMaterials.isNotEmpty()) return false
        return !config.isBlocked(row, col)
    }

    private fun currentRow(): Int = floor(y).toInt().coerceIn(0, config.rows - 1)
    private fun currentCol(): Int = floor(x).toInt().coerceIn(0, config.cols - 1)

    private fun isAtTileCenter(): Boolean {
        val cx = floor(x) + 0.5f
        val cy = floor(y) + 0.5f
        return abs(x - cx) <= CENTER_EPSILON && abs(y - cy) <= CENTER_EPSILON
    }

    private fun snapToTileCenter() {
        x = floor(x) + 0.5f
        y = floor(y) + 0.5f
    }

    private fun distanceToNextTileCenter(): Float {
        val targetX = when (direction) {
            Direction.Right -> nextIncreasingCenter(x)
            Direction.Left -> nextDecreasingCenter(x)
            else -> x
        }
        val targetY = when (direction) {
            Direction.Down -> nextIncreasingCenter(y)
            Direction.Up -> nextDecreasingCenter(y)
            else -> y
        }
        return max(abs(targetX - x), abs(targetY - y))
    }

    private fun nextIncreasingCenter(value: Float): Float {
        val center = floor(value) + 0.5f
        return if (value < center - CENTER_EPSILON) center else center + 1f
    }

    private fun nextDecreasingCenter(value: Float): Float {
        val center = floor(value) + 0.5f
        return if (value > center + CENTER_EPSILON) center else center - 1f
    }

    private fun crash() {
        collisions++
        lives = 0
        callback?.onCrashSound()
        finishLevel(passed = false)
    }

    private fun finishLevel(passed: Boolean) {
        if (finished) return
        finished = true
        running = false
        removeCallbacks(loopRunnable)
        val durationSeconds = ((SystemClock.uptimeMillis() - startUptime) / 1000L).toInt().coerceAtLeast(0)
        val timeBonus = if (passed) (220 - durationSeconds * 3).coerceAtLeast(25) else 0
        val completionBonus = if (passed) 500 + config.level * 25 else 0
        val finalScore = score + timeBonus + completionBonus
        val reward = if (passed) (8 + totalMaterials + config.level / 2).coerceAtMost(80) else 0
        callback?.onGameFinished(
            MazeGameResult(
                level = config.level,
                passed = passed,
                score = finalScore,
                materialsCollected = collectedMaterials,
                totalMaterials = totalMaterials,
                rewardCoins = reward,
                turnCount = turns,
                collisionCount = collisions,
                durationSeconds = durationSeconds,
                levelPattern = config.pattern
            )
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        computeBoard()
        drawBackground(canvas)
        drawBoard(canvas)
        drawExit(canvas)
        drawMaterials(canvas)
        drawObstacles(canvas)
        drawTractor(canvas)
        drawBottomCounters(canvas)
    }

    private fun computeBoard() {
        val w = width.toFloat()
        val h = height.toFloat()
        val topInset = 96f * resources.displayMetrics.density
        val bottomInset = 86f * resources.displayMetrics.density
        val availableW = w - 8f * resources.displayMetrics.density
        val availableH = h - topInset - bottomInset
        tileSize = min(availableW / config.cols, availableH / config.rows).coerceAtLeast(1f)
        boardWidth = tileSize * config.cols
        boardHeight = tileSize * config.rows
        boardLeft = (w - boardWidth) / 2f
        boardTop = topInset + (availableH - boardHeight) / 2f
    }

    private fun drawBackground(canvas: Canvas) {
        paint.shader = null
        paint.style = Paint.Style.FILL
        val staticBackground = bitmapFor(R.drawable.maze_others_bg)
        if (staticBackground != null) {
            rect.set(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawBitmap(staticBackground, null, rect, bitmapPaint)
        } else {
            paint.color = theme.backgroundBottom
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }

    private fun drawBoard(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(210, Color.red(theme.ground), Color.green(theme.ground), Color.blue(theme.ground))
        rect.set(boardLeft - tileSize * 0.18f, boardTop - tileSize * 0.18f, boardLeft + boardWidth + tileSize * 0.18f, boardTop + boardHeight + tileSize * 0.18f)
        canvas.drawRoundRect(rect, tileSize * 0.18f, tileSize * 0.18f, paint)

        val sandBitmap = bitmapFor(R.drawable.maze_map_sand)
        val wallBitmap = bitmapFor(R.drawable.maze_map_wall)

        for (r in 0 until config.rows) {
            for (c in 0 until config.cols) {
                val left = tileLeft(c)
                val top = tileTop(r)
                rect.set(left, top, left + tileSize, top + tileSize)
                val wall = config.isWall(r, c)
                val tileBitmap = if (wall) wallBitmap else sandBitmap
                if (tileBitmap != null) {
                    canvas.drawBitmap(tileBitmap, null, rect, bitmapPaint)
                } else {
                    paint.color = if (wall) theme.wall else theme.path
                    canvas.drawRect(rect, paint)
                }

                if (wall) {
                    drawBarrier(canvas, left, top)
                }

                stroke.style = Paint.Style.STROKE
                stroke.strokeWidth = max(1f, tileSize * 0.025f)
                stroke.color = if (wall) Color.argb(55, 0, 0, 0) else Color.argb(80, 108, 82, 44)
                canvas.drawRect(rect, stroke)
            }
        }
    }

    private fun drawBarrier(canvas: Canvas, left: Float, top: Float) {
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(34, 255, 255, 255)
        rect.set(left + tileSize * 0.06f, top + tileSize * 0.08f, left + tileSize * 0.94f, top + tileSize * 0.18f)
        canvas.drawRect(rect, paint)
        paint.color = Color.argb(26, 0, 0, 0)
        rect.set(left + tileSize * 0.06f, top + tileSize * 0.78f, left + tileSize * 0.94f, top + tileSize * 0.92f)
        canvas.drawRect(rect, paint)
    }

    private fun drawRouteHints(canvas: Canvas) {
        // Route hint dots intentionally disabled: traversable tiles should show only the maze_map_sand texture.
    }

    private fun drawExit(canvas: Canvas) {
        val open = remainingMaterials.isEmpty()
        val cx = tileCenterX(config.exit.col)
        val cy = tileCenterY(config.exit.row)
        val exitRes = if (open) R.drawable.maze_exit_open else R.drawable.maze_exit_closed
        if (!drawBitmapCentered(canvas, exitRes, cx, cy, tileSize * 1.28f, tileSize * 1.28f)) {
            paint.style = Paint.Style.FILL
            paint.color = if (open) theme.exit else Color.rgb(112, 76, 56)
            rect.set(cx - tileSize * 0.32f, cy - tileSize * 0.40f, cx + tileSize * 0.32f, cy + tileSize * 0.40f)
            canvas.drawRoundRect(rect, tileSize * 0.08f, tileSize * 0.08f, paint)
            paint.color = Color.WHITE
            rect.set(cx - tileSize * 0.22f, cy - tileSize * 0.30f, cx + tileSize * 0.22f, cy + tileSize * 0.02f)
            canvas.drawRect(rect, paint)
            paint.color = if (open) Color.rgb(255, 235, 59) else Color.rgb(33, 33, 33)
            canvas.drawCircle(cx, cy + tileSize * 0.18f, tileSize * 0.07f, paint)
        }
    }

    private fun drawMaterials(canvas: Canvas) {
        remainingMaterials.forEach { (pos, type) ->
            val cx = tileCenterX(pos.col)
            val cy = tileCenterY(pos.row)
            when (type) {
                MaterialType.Stone -> drawStonePile(canvas, cx, cy)
                MaterialType.Wood -> drawWoodPile(canvas, cx, cy)
                MaterialType.Bricks -> drawBrickPile(canvas, cx, cy)
            }
        }
    }

    private fun drawStonePile(canvas: Canvas, cx: Float, cy: Float, size: Float = tileSize) {
        if (!drawBitmapCentered(canvas, R.drawable.maze_stone_model, cx, cy, size * 0.82f, size * 0.82f)) {
            val colors = intArrayOf(Color.rgb(117, 117, 117), Color.rgb(158, 158, 158), Color.rgb(97, 97, 97))
            colors.forEachIndexed { i, color ->
                paint.color = color
                canvas.drawCircle(cx + (i - 1) * size * 0.11f, cy + (i % 2) * size * 0.08f, size * 0.13f, paint)
            }
        }
    }

    private fun drawWoodPile(canvas: Canvas, cx: Float, cy: Float, size: Float = tileSize) {
        if (!drawBitmapCentered(canvas, R.drawable.maze_wood_model, cx, cy, size * 0.88f, size * 0.64f)) {
            paint.color = Color.rgb(126, 74, 35)
            stroke.color = Color.rgb(76, 43, 22)
            stroke.strokeWidth = size * 0.035f
            repeat(3) { i ->
                val yOffset = (i - 1) * size * 0.12f
                rect.set(cx - size * 0.30f, cy + yOffset - size * 0.055f, cx + size * 0.30f, cy + yOffset + size * 0.055f)
                canvas.drawRoundRect(rect, size * 0.04f, size * 0.04f, paint)
                canvas.drawRoundRect(rect, size * 0.04f, size * 0.04f, stroke)
            }
        }
    }

    private fun drawBrickPile(canvas: Canvas, cx: Float, cy: Float, size: Float = tileSize) {
        if (!drawBitmapCentered(canvas, R.drawable.maze_brick_model, cx, cy, size * 0.82f, size * 0.82f)) {
            paint.color = Color.rgb(196, 73, 35)
            stroke.color = Color.rgb(115, 45, 28)
            stroke.strokeWidth = size * 0.025f
            val bw = size * 0.18f
            val bh = size * 0.12f
            for (r in 0..1) {
                for (c in 0..2) {
                    val offset = if (r == 0) 0f else bw * 0.45f
                    val left = cx - bw * 1.35f + c * bw + offset
                    val top = cy - bh + r * bh
                    rect.set(left, top, left + bw * 0.9f, top + bh * 0.82f)
                    canvas.drawRect(rect, paint)
                    canvas.drawRect(rect, stroke)
                }
            }
        }
    }

    private fun drawObstacles(canvas: Canvas) {
        config.obstacles.forEach { obstacle ->
            val cx = tileCenterX(obstacle.pos.col)
            val cy = tileCenterY(obstacle.pos.row)
            when (obstacle.type) {
                ObstacleType.Crane -> drawCone(canvas, cx, cy)
                ObstacleType.Pit -> drawPit(canvas, cx, cy)
                ObstacleType.Barrel -> drawCone(canvas, cx, cy)
            }
        }
    }

    private fun drawCone(canvas: Canvas, cx: Float, cy: Float) {
        if (!drawBitmapCentered(canvas, R.drawable.maze_map_cone, cx, cy, tileSize * 0.76f, tileSize * 0.76f)) {
            stroke.color = Color.rgb(244, 81, 30)
            stroke.strokeWidth = tileSize * 0.08f
            canvas.drawLine(cx - tileSize * 0.18f, cy + tileSize * 0.28f, cx - tileSize * 0.18f, cy - tileSize * 0.28f, stroke)
            canvas.drawLine(cx - tileSize * 0.18f, cy - tileSize * 0.22f, cx + tileSize * 0.28f, cy - tileSize * 0.35f, stroke)
            paint.color = Color.rgb(255, 193, 7)
            rect.set(cx - tileSize * 0.30f, cy + tileSize * 0.20f, cx + tileSize * 0.28f, cy + tileSize * 0.34f)
            canvas.drawRect(rect, paint)
        }
    }

    private fun drawPit(canvas: Canvas, cx: Float, cy: Float) {
        if (!drawBitmapCentered(canvas, R.drawable.maze_map_pit, cx, cy, tileSize * 0.98f, tileSize * 0.98f)) {
            paint.color = Color.rgb(34, 24, 19)
            rect.set(cx - tileSize * 0.30f, cy - tileSize * 0.24f, cx + tileSize * 0.30f, cy + tileSize * 0.24f)
            canvas.drawOval(rect, paint)
            paint.color = Color.argb(80, 255, 255, 255)
            rect.inset(tileSize * 0.08f, tileSize * 0.08f)
            canvas.drawOval(rect, paint)
        }
    }

    private fun drawBarrel(canvas: Canvas, cx: Float, cy: Float) {
        drawCone(canvas, cx, cy)
    }

    private fun drawTractor(canvas: Canvas) {
        val cx = boardLeft + x * tileSize
        val cy = boardTop + y * tileSize
        tractorRenderer.draw(canvas, skin, cx, cy, tileSize * 0.95f, direction.angle)

        val arrowDir = if (pendingDirection != direction) pendingDirection else Direction.None
        if (arrowDir != Direction.None) {
            drawPendingArrow(canvas, cx, cy, arrowDir)
        }
    }

    private fun drawPendingArrow(canvas: Canvas, cx: Float, cy: Float, dir: Direction) {
        canvas.save()
        canvas.rotate(dir.angle, cx, cy)
        paint.color = Color.argb(190, 255, 255, 255)
        path.reset()
        path.moveTo(cx, cy - tileSize * 0.58f)
        path.lineTo(cx - tileSize * 0.14f, cy - tileSize * 0.34f)
        path.lineTo(cx + tileSize * 0.14f, cy - tileSize * 0.34f)
        path.close()
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    private fun bitmapFor(resId: Int): Bitmap? {
        if (!bitmapCache.containsKey(resId)) {
            bitmapCache[resId] = BitmapFactory.decodeResource(resources, resId)
        }
        return bitmapCache[resId]
    }

    private fun drawBitmapCentered(
        canvas: Canvas,
        resId: Int,
        cx: Float,
        cy: Float,
        targetWidth: Float,
        targetHeight: Float
    ): Boolean {
        val bitmap = bitmapFor(resId) ?: return false
        rect.set(cx - targetWidth / 2f, cy - targetHeight / 2f, cx + targetWidth / 2f, cy + targetHeight / 2f)
        canvas.drawBitmap(bitmap, null, rect, bitmapPaint)
        return true
    }

    private fun drawBottomCounters(canvas: Canvas) {
        val density = resources.displayMetrics.density
        val panelHeight = 46f * density
        val horizontalPadding = 16f * density
        val gap = 8f * density
        val panelWidth = (width - horizontalPadding * 2f - gap * 2f) / 3f
        val top = height - panelHeight - 22f * density
        val counters = currentCounters()

        drawMaterialCounterCard(
            canvas = canvas,
            index = 0,
            top = top,
            width = panelWidth,
            height = panelHeight,
            gap = gap,
            padding = horizontalPadding,
            type = MaterialType.Bricks,
            collected = counters.bricksCollected,
            total = counters.bricksTotal
        )
        drawMaterialCounterCard(
            canvas = canvas,
            index = 1,
            top = top,
            width = panelWidth,
            height = panelHeight,
            gap = gap,
            padding = horizontalPadding,
            type = MaterialType.Stone,
            collected = counters.stonesCollected,
            total = counters.stonesTotal
        )
        drawMaterialCounterCard(
            canvas = canvas,
            index = 2,
            top = top,
            width = panelWidth,
            height = panelHeight,
            gap = gap,
            padding = horizontalPadding,
            type = MaterialType.Wood,
            collected = counters.woodCollected,
            total = counters.woodTotal
        )
    }

    private fun drawMaterialCounterCard(
        canvas: Canvas,
        index: Int,
        top: Float,
        width: Float,
        height: Float,
        gap: Float,
        padding: Float,
        type: MaterialType,
        collected: Int,
        total: Int
    ) {
        val left = padding + index * (width + gap)
        val radius = height * 0.16f
        rect.set(left, top, left + width, top + height)

        val cardBitmap = bitmapFor(R.drawable.ui_items_bg)
        if (cardBitmap != null) {
            canvas.drawBitmap(cardBitmap, null, rect, bitmapPaint)
        } else {
            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(36, 91, 129)
            canvas.drawRoundRect(rect, radius, radius, paint)

            stroke.style = Paint.Style.STROKE
            stroke.strokeWidth = height * 0.12f
            stroke.color = Color.rgb(35, 47, 57)
            canvas.drawRoundRect(rect, radius, radius, stroke)
            stroke.strokeWidth = height * 0.035f
            stroke.color = Color.rgb(255, 198, 45)
            rect.inset(height * 0.08f, height * 0.08f)
            canvas.drawRoundRect(rect, radius * 0.72f, radius * 0.72f, stroke)
            rect.inset(-height * 0.08f, -height * 0.08f)
        }

        val iconSize = height * 0.82f
        val iconCx = left + width * 0.29f
        val iconCy = top + height * 0.52f
        paint.style = Paint.Style.FILL
        when (type) {
            MaterialType.Bricks -> drawBrickPile(canvas, iconCx, iconCy, iconSize)
            MaterialType.Stone -> drawStonePile(canvas, iconCx, iconCy, iconSize)
            MaterialType.Wood -> drawWoodPile(canvas, iconCx, iconCy, iconSize)
        }

        textPaint.color = Color.WHITE
        textPaint.typeface = hudTypeface
        textPaint.textSize = height * 0.40f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.setShadowLayer(2.5f, 3f, 3f, Color.rgb(113, 48, 15))
        canvas.drawText("$collected/$total", left + width * 0.68f, top + height * 0.63f, textPaint)
        textPaint.clearShadowLayer()
    }

    private companion object {
        const val CENTER_EPSILON = 0.001f
        const val MAX_TILE_STEPS_PER_FRAME = 8
        val INITIAL_DIRECTIONS = listOf(Direction.Up, Direction.Right, Direction.Left, Direction.Down)
    }

    private fun tileLeft(col: Int): Float = boardLeft + col * tileSize
    private fun tileTop(row: Int): Float = boardTop + row * tileSize
    private fun tileCenterX(col: Int): Float = boardLeft + (col + 0.5f) * tileSize
    private fun tileCenterY(row: Int): Float = boardTop + (row + 0.5f) * tileSize
}
