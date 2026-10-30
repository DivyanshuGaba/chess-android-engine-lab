package com.divyanshu.chess.engine

data class UciInfo(
    val depth: Int? = null,
    val selDepth: Int? = null,
    val multiPv: Int? = null,
    val scoreCp: Int? = null,
    val scoreMate: Int? = null,
    val bound: Bound? = null,
    val nodes: Long? = null,
    val nps: Long? = null,
    val timeMs: Long? = null,
    val hashFull: Int? = null,
    val pv: List<String> = emptyList(),
) {
    enum class Bound { LOWER, UPPER }
}

data class BestMove(val move: String?, val ponder: String?)

data class UciOption(
    val name: String,
    val type: String,
    val default: String?,
    val min: Long?,
    val max: Long?,
)

object UciParser {

    private val optionRegex = Regex(
        """^option name (.+?) type (\w+)(?: default (.*?))?(?: min (-?\d+))?(?: max (-?\d+))?(?: var .*)*$""",
    )

    /** Parses a line starting with "info"; returns null for free-text "info string" lines. */
    fun parseInfo(line: String): UciInfo? {
        val tokens = line.trim().split(' ').filter { it.isNotEmpty() }
        if (tokens.firstOrNull() != "info" || tokens.getOrNull(1) == "string") return null

        var info = UciInfo()
        var i = 1
        while (i < tokens.size) {
            when (tokens[i]) {
                "depth" -> { info = info.copy(depth = tokens.intAt(i + 1)); i += 2 }
                "seldepth" -> { info = info.copy(selDepth = tokens.intAt(i + 1)); i += 2 }
                "multipv" -> { info = info.copy(multiPv = tokens.intAt(i + 1)); i += 2 }
                "nodes" -> { info = info.copy(nodes = tokens.longAt(i + 1)); i += 2 }
                "nps" -> { info = info.copy(nps = tokens.longAt(i + 1)); i += 2 }
                "time" -> { info = info.copy(timeMs = tokens.longAt(i + 1)); i += 2 }
                "hashfull" -> { info = info.copy(hashFull = tokens.intAt(i + 1)); i += 2 }
                "score" -> {
                    i += 1
                    when (tokens.getOrNull(i)) {
                        "cp" -> info = info.copy(scoreCp = tokens.intAt(i + 1))
                        "mate" -> info = info.copy(scoreMate = tokens.intAt(i + 1))
                    }
                    i += 2
                    when (tokens.getOrNull(i)) {
                        "lowerbound" -> { info = info.copy(bound = UciInfo.Bound.LOWER); i += 1 }
                        "upperbound" -> { info = info.copy(bound = UciInfo.Bound.UPPER); i += 1 }
                    }
                }
                "pv" -> return info.copy(pv = tokens.drop(i + 1))
                "currmove", "currmovenumber", "tbhits", "refutation", "currline", "cpuload" -> i += 2
                "wdl" -> i += 4
                else -> i += 1
            }
        }
        return info
    }

    fun parseBestMove(line: String): BestMove? {
        val tokens = line.trim().split(' ').filter { it.isNotEmpty() }
        if (tokens.firstOrNull() != "bestmove") return null
        val move = tokens.getOrNull(1)?.takeUnless { it == "(none)" || it == "0000" }
        val ponder = tokens.getOrNull(3)?.takeIf { tokens.getOrNull(2) == "ponder" }
        return BestMove(move, ponder)
    }

    fun parseOption(line: String): UciOption? {
        val match = optionRegex.matchEntire(line.trim()) ?: return null
        val (name, type, default, min, max) = match.destructured
        return UciOption(
            name = name,
            type = type,
            default = default.ifEmpty { null },
            min = min.toLongOrNull(),
            max = max.toLongOrNull(),
        )
    }

    /** Returns the value of an "id name ..." or "id author ..." line. */
    fun parseId(line: String, key: String): String? =
        line.trim().takeIf { it.startsWith("id $key ") }?.removePrefix("id $key ")?.trim()

    private fun List<String>.intAt(index: Int): Int? = getOrNull(index)?.toIntOrNull()

    private fun List<String>.longAt(index: Int): Long? = getOrNull(index)?.toLongOrNull()
}
