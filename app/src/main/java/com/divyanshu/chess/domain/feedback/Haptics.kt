package com.divyanshu.chess.domain.feedback

enum class HapticEvent { MOVE, CAPTURE, CHECK, GAME_OVER }

fun interface Haptics {
    fun perform(event: HapticEvent)
}
