package com.racelink.app.race

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Audio + haptic for the start tree.
 *
 * Drivers can't be staring at the screen while the ambers count down, so each
 * amber gets a short beep + light tap and the green gets a louder/longer beep
 * with a strong buzz. Uses [ToneGenerator] (built-in Android tones, no asset
 * files) and the platform [Vibrator].
 */
class RaceFeedback(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    // STREAM_MUSIC so the beeps go to the speaker / Bluetooth audio sink the
    // driver is already using for navigation.
    private val tone: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, 90)
    }.getOrNull()

    fun amber() {
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP, AMBER_TONE_MS)
        vibrate(40, amplitude = 120)
    }

    fun green() {
        tone?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, GREEN_TONE_MS)
        vibrate(220, amplitude = 255)
    }

    fun finish() {
        tone?.startTone(ToneGenerator.TONE_PROP_ACK, 200)
        vibrate(80, amplitude = 200)
    }

    fun release() {
        runCatching { tone?.release() }
    }

    private fun vibrate(durationMs: Long, amplitude: Int) {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255))
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION") v.vibrate(durationMs)
        }
    }

    private companion object {
        const val AMBER_TONE_MS = 120
        const val GREEN_TONE_MS = 350
    }
}
