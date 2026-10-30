package com.divyanshu.chess.domain.ai

enum class EvaluationKind {
    /** Material plus piece-square tables, bishop pair and a tempo bonus. */
    PST,

    /** The original engine's evaluation: material plus a bonus for pieces near the centre. */
    LEGACY,
}

/**
 * Switches for the improved search, so each technique can be turned off in a benchmark to measure what it
 * contributes. Everything is on by default, which is how the app runs.
 */
data class Features(
    val quiescence: Boolean = true,
    val transpositionTable: Boolean = true,
    val reductions: Boolean = true,
    val orderingHeuristics: Boolean = true,
    val checkExtension: Boolean = true,
    val evaluation: EvaluationKind = EvaluationKind.PST,
)
