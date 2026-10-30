package com.divyanshu.chess.domain.clock

import kotlinx.coroutines.flow.Flow

/** Emits a monotonic timestamp in milliseconds, first immediately and then at a steady rate. */
fun interface ClockTicker {
    fun ticks(): Flow<Long>
}
