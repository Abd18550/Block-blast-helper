package com.abd.blockassistant

object Solver {
    /**
     * Greedy suggestion: choose up to [pieces] empty cells that maximize cleared rows/cols.
     * Treats each piece as a 1x1 for MVP.
     * Returns list of Placements (order 1..n) and the set of cleared line indices after all placements.
     */
    fun computeGreedyBestCells(
        board: Array<IntArray>,
        pieces: Int = 3
    ): Pair<List<Placement>, Set<Int>> {
        val h = board.size
        val w = board.firstOrNull()?.size ?: 0
        if (h == 0 || w == 0) return emptyList<Placement>() to emptySet()

        // Work on a copy for simulation
        val sim = Array(h) { r -> board[r].clone() }
        val placements = mutableListOf<Placement>()

        repeat(pieces) { i ->
            var bestPos: Pair<Int, Int>? = null
            var bestScore = -1
            var bestCleared: Set<Int> = emptySet()

            for (r in 0 until h) {
                for (c in 0 until w) {
                    if (sim[r][c] == 1) continue
                    // simulate place 1x1
                    sim[r][c] = 1
                    val cleared = BoardExtractor.detectClearedLines(sim)
                    val score = cleared.size
                    if (score > bestScore) {
                        bestScore = score
                        bestPos = r to c
                        bestCleared = cleared
                    }
                    // revert
                    sim[r][c] = board[r][c]
                }
            }

            // If no empty cells left or no improvement, still place at first available empty to proceed
            val pos = bestPos ?: run {
                var fallback: Pair<Int, Int>? = null
                loop@ for (r in 0 until h) for (c in 0 until w) if (sim[r][c] == 0) { fallback = r to c; break@loop }
                fallback
            }

            if (pos == null) return@repeat // board full

            // Apply best
            val (r, c) = pos
            sim[r][c] = 1
            placements.add(
                Placement(
                    row = r,
                    col = c,
                    cells = listOf(0 to 0),
                    order = i + 1
                )
            )
        }

        // Final cleared after all placements
        val finalCleared = BoardExtractor.detectClearedLines(sim)
        return placements to finalCleared
    }
}
