package com.divyanshu.chess.data

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.divyanshu.chess.domain.feedback.HapticEvent
import com.divyanshu.chess.domain.feedback.Haptics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VibratorHaptics @Inject constructor(
    @ApplicationContext context: Context,
) : Haptics {

    private val handler = Handler(Looper.getMainLooper())

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    /** The buzz is delayed so it lands with the piece, not with the tap. */
    override fun perform(event: HapticEvent) {
        val device = vibrator?.takeIf { it.hasVibrator() } ?: return
        handler.postDelayed({ device.vibrate(effectFor(event)) }, LANDING_DELAY_MS)
    }

    private fun effectFor(event: HapticEvent): VibrationEffect {
        val modern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        return when (event) {
            HapticEvent.MOVE ->
                if (modern) VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                else VibrationEffect.createOneShot(15, 90)
            HapticEvent.CAPTURE ->
                if (modern) VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                else VibrationEffect.createOneShot(35, 220)
            HapticEvent.CHECK -> VibrationEffect.createWaveform(longArrayOf(0, 40, 70, 40), -1)
            HapticEvent.GAME_OVER -> VibrationEffect.createWaveform(longArrayOf(0, 80, 90, 80, 90, 260), -1)
        }
    }

    private companion object {
        const val LANDING_DELAY_MS = 200L
    }
}
