package com.divyanshu.chess.data

import android.os.SystemClock
import com.divyanshu.chess.domain.clock.ClockTicker
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RealtimeTicker @Inject constructor() : ClockTicker {

    override fun ticks(): Flow<Long> = flow {
        while (true) {
            emit(SystemClock.elapsedRealtime())
            delay(TICK_MS)
        }
    }

    private companion object {
        const val TICK_MS = 100L
    }
}
