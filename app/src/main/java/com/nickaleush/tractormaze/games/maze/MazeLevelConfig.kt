package com.nickaleush.tractormaze.games.maze

/** Immutable tile-coordinate position. */
data class TilePos(val row: Int, val col: Int)

enum class MaterialType { Stone, Wood, Bricks }
enum class ObstacleType { Crane, Pit, Barrel }

data class MaterialSpawn(val pos: TilePos, val type: MaterialType)
data class ObstacleSpawn(val pos: TilePos, val type: ObstacleType)

data class MazeMaterialCounters(
    val stonesCollected: Int,
    val stonesTotal: Int,
    val woodCollected: Int,
    val woodTotal: Int,
    val bricksCollected: Int,
    val bricksTotal: Int
)

data class MazeLevelConfig(
    val level: Int,
    val pattern: Int,
    val rows: Int,
    val cols: Int,
    val start: TilePos,
    val exit: TilePos,
    val baseSpeedTilesPerSecond: Float,
    val map: List<String>,
    val materials: List<MaterialSpawn>,
    val obstacles: List<ObstacleSpawn>
) {
    fun isInside(row: Int, col: Int): Boolean = row in 0 until rows && col in 0 until cols

    fun isWall(row: Int, col: Int): Boolean = !isInside(row, col) || map[row][col] == WALL

    fun obstacleAt(row: Int, col: Int): ObstacleSpawn? =
        obstacles.firstOrNull { it.pos.row == row && it.pos.col == col }

    fun isBlocked(row: Int, col: Int): Boolean = isWall(row, col) || obstacleAt(row, col) != null

    fun totalOf(type: MaterialType): Int = materials.count { it.type == type }

    /**
     * Checks the level using the same grid rules as the gameplay:
     * all materials must be reachable while the exit is still closed,
     * then the exit must be reachable after the materials are collected.
     */
    fun isPlayable(): Boolean {
        if (!isInside(start.row, start.col) || !isInside(exit.row, exit.col)) return false
        if (isBlocked(start.row, start.col) || isBlocked(exit.row, exit.col)) return false
        if (!hasSafeSpawnEscape()) return false

        val reachableBeforeExitOpens = reachableFromStart(exitIsOpen = false)
        if (materials.any { it.pos !in reachableBeforeExitOpens }) return false
        val reachableAfterExitOpens = reachableFromStart(exitIsOpen = true)
        return exit in reachableAfterExitOpens
    }

    private fun hasSafeSpawnEscape(): Boolean = DIRECTIONS.any { delta ->
        val next = TilePos(start.row + delta.row, start.col + delta.col)
        next != exit && isInside(next.row, next.col) && !isBlocked(next.row, next.col)
    }

    private fun reachableFromStart(exitIsOpen: Boolean): Set<TilePos> {
        val queue = ArrayDeque<TilePos>()
        val visited = linkedSetOf<TilePos>()
        queue.add(start)
        visited.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            DIRECTIONS.forEach { delta ->
                val next = TilePos(current.row + delta.row, current.col + delta.col)
                if (next in visited) return@forEach
                if (!isInside(next.row, next.col)) return@forEach
                if (!exitIsOpen && next == exit) return@forEach
                if (isBlocked(next.row, next.col)) return@forEach
                visited.add(next)
                queue.add(next)
            }
        }
        return visited
    }

    companion object {
        const val MAX_LEVEL = 45
        private const val WALL = '#'
        private val DIRECTIONS = listOf(
            TilePos(-1, 0),
            TilePos(1, 0),
            TilePos(0, -1),
            TilePos(0, 1)
        )
        private val SPAWN_ESCAPE_DIRECTIONS = listOf(
            TilePos(-1, 0),
            TilePos(0, 1),
            TilePos(0, -1),
            TilePos(1, 0)
        )

        fun forLevel(level: Int): MazeLevelConfig {
            val clamped = level.coerceIn(1, MAX_LEVEL)
            val pattern = ((clamped - 1) % BASE_PATTERNS.size) + 1
            val base = BASE_PATTERNS[pattern - 1]
            val ramp = (clamped - 1) / (MAX_LEVEL - 1f)
            return base.copy(
                level = clamped,
                pattern = pattern,
                baseSpeedTilesPerSecond = base.baseSpeedTilesPerSecond * (1f + 0.42f * ramp)
            ).ensurePlayable()
        }

        private fun config(
            pattern: Int,
            speed: Float,
            rows: List<String>
        ): MazeLevelConfig {
            require(rows.isNotEmpty()) { "Level map cannot be empty" }
            val cols = rows.first().length
            require(rows.all { it.length == cols }) { "All map rows must have the same length" }

            var start: TilePos? = null
            var exit: TilePos? = null
            val materials = mutableListOf<MaterialSpawn>()
            val obstacles = mutableListOf<ObstacleSpawn>()
            val normalized = rows.mapIndexed { r, line ->
                buildString {
                    line.forEachIndexed { c, ch ->
                        when (ch) {
                            'S' -> {
                                start = TilePos(r, c)
                                append('.')
                            }
                            'E' -> {
                                exit = TilePos(r, c)
                                append('.')
                            }
                            's' -> {
                                materials += MaterialSpawn(TilePos(r, c), MaterialType.Stone)
                                append('.')
                            }
                            'w' -> {
                                materials += MaterialSpawn(TilePos(r, c), MaterialType.Wood)
                                append('.')
                            }
                            'b' -> {
                                materials += MaterialSpawn(TilePos(r, c), MaterialType.Bricks)
                                append('.')
                            }
                            'c' -> {
                                obstacles += ObstacleSpawn(TilePos(r, c), ObstacleType.Crane)
                                append('.')
                            }
                            'p' -> {
                                obstacles += ObstacleSpawn(TilePos(r, c), ObstacleType.Pit)
                                append('.')
                            }
                            'o' -> {
                                obstacles += ObstacleSpawn(TilePos(r, c), ObstacleType.Barrel)
                                append('.')
                            }
                            '#', '.' -> append(ch)
                            else -> append('.')
                        }
                    }
                }
            }

            return MazeLevelConfig(
                level = 1,
                pattern = pattern,
                rows = normalized.size,
                cols = cols,
                start = requireNotNull(start) { "Map must contain S" },
                exit = requireNotNull(exit) { "Map must contain E" },
                baseSpeedTilesPerSecond = speed,
                map = normalized,
                materials = materials,
                obstacles = obstacles
            ).ensurePlayable()
        }

        /**
         * Safety net for hand-authored and future generated maps.
         * If a wall/obstacle accidentally cuts off a material or the exit, this carves the
         * shortest connector corridor and removes only blockers placed on that connector.
         */
        private fun MazeLevelConfig.ensurePlayable(): MazeLevelConfig {
            val mutableMap = map.map { it.toCharArray() }.toMutableList()
            val mutableObstacles = obstacles.toMutableList()

            fun clearTile(pos: TilePos) {
                if (!isInside(pos.row, pos.col)) return
                mutableMap[pos.row][pos.col] = '.'
                mutableObstacles.removeAll { it.pos == pos }
            }

            fun currentConfig(): MazeLevelConfig = copy(
                map = mutableMap.map { String(it) },
                obstacles = mutableObstacles.toList()
            )

            fun ensureSafeSpawnEscape() {
                clearTile(start)
                clearTile(exit)

                val preferredEscape = SPAWN_ESCAPE_DIRECTIONS
                    .map { delta -> TilePos(start.row + delta.row, start.col + delta.col) }
                    .firstOrNull { pos -> isInside(pos.row, pos.col) && pos != exit }

                if (preferredEscape != null) {
                    clearTile(preferredEscape)
                    return
                }

                DIRECTIONS
                    .map { delta -> TilePos(start.row + delta.row, start.col + delta.col) }
                    .firstOrNull { pos -> isInside(pos.row, pos.col) && pos != exit }
                    ?.let(::clearTile)
            }

            ensureSafeSpawnEscape()

            var repaired = currentConfig()
            materials.forEach { material ->
                if (material.pos !in repaired.reachableFromStart(exitIsOpen = false)) {
                    carveShortestConnector(
                        level = repaired,
                        mutableMap = mutableMap,
                        mutableObstacles = mutableObstacles,
                        target = material.pos,
                        avoidClosedExit = true
                    )
                    repaired = currentConfig()
                }
            }

            if (repaired.exit !in repaired.reachableFromStart(exitIsOpen = true)) {
                carveShortestConnector(
                    level = repaired,
                    mutableMap = mutableMap,
                    mutableObstacles = mutableObstacles,
                    target = repaired.exit,
                    avoidClosedExit = false
                )
                repaired = currentConfig()
            }

            require(repaired.isPlayable()) {
                "Maze pattern ${repaired.pattern} is not playable after repair"
            }
            return repaired
        }

        private fun carveShortestConnector(
            level: MazeLevelConfig,
            mutableMap: MutableList<CharArray>,
            mutableObstacles: MutableList<ObstacleSpawn>,
            target: TilePos,
            avoidClosedExit: Boolean
        ) {
            val queue = ArrayDeque<TilePos>()
            val previous = mutableMapOf<TilePos, TilePos?>()
            queue.add(level.start)
            previous[level.start] = null

            while (queue.isNotEmpty() && target !in previous) {
                val current = queue.removeFirst()
                DIRECTIONS.forEach { delta ->
                    val next = TilePos(current.row + delta.row, current.col + delta.col)
                    if (!level.isInside(next.row, next.col)) return@forEach
                    if (next in previous) return@forEach
                    if (avoidClosedExit && next == level.exit && next != target) return@forEach
                    previous[next] = current
                    queue.add(next)
                }
            }

            var cursor: TilePos? = target
            while (cursor != null) {
                if (level.isInside(cursor.row, cursor.col)) {
                    mutableMap[cursor.row][cursor.col] = '.'
                    mutableObstacles.removeAll { it.pos == cursor }
                }
                cursor = previous[cursor]
            }
        }

        private val BASE_PATTERNS: List<MazeLevelConfig> = listOf(
            config(
                pattern = 1,
                speed = 2.05f,
                rows = listOf(
                    "....E.....",
                    ".w..#..s..",
                    ".#..#..#..",
                    "..b...#...",
                    "##..#..o..",
                    "..s.#.w#..",
                    "..#...#..b",
                    "..#.p.....",
                    ".w....#...",
                    ".....S...."
                )
            ),
            config(
                pattern = 2,
                speed = 2.18f,
                rows = listOf(
                    "..s.E..w..",
                    ".#....##..",
                    "..b.o.....",
                    "#...#.....",
                    "..#w#..b#.",
                    "..#.......",
                    ".s..p.#w..",
                    ".##...#...",
                    "..b..c....",
                    ".....S...."
                )
            ),
            config(
                pattern = 3,
                speed = 2.32f,
                rows = listOf(
                    "...wE.b...",
                    ".##.#.##..",
                    ".s..#..w..",
                    "...o...#..",
                    "##.#b#....",
                    "..w#...#s.",
                    "..#..p.#..",
                    ".b..#..c..",
                    "...##.....",
                    ".....S...."
                )
            ),
            config(
                pattern = 4,
                speed = 2.48f,
                rows = listOf(
                    "..b.E.w...",
                    "..#o#..#s.",
                    ".w#.#b.#..",
                    "..#...#...",
                    "..s.#..w..",
                    ".##.#.##..",
                    "..b...p...",
                    "#.##c##.#.",
                    "...w..s...",
                    ".....S...."
                )
            ),
            config(
                pattern = 5,
                speed = 2.65f,
                rows = listOf(
                    "w..bE..s..",
                    ".##.#.##w.",
                    ".s..o..#..",
                    "..#.#b.#..",
                    "b...#...w.",
                    ".##...##..",
                    "..w.p..s..",
                    "..#.c.#b..",
                    "s..##..w..",
                    ".....S...."
                )
            )
        )
    }
}
