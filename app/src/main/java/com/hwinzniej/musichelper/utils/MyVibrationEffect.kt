package com.hwinzniej.musichelper.utils

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

class MyVibrationEffect(
    context: Context,
    val enable: Boolean
) {
    val vibrator: Vibrator = context.getSystemService(Vibrator::class.java)

    fun dialog() {
        if (enable) {
            val vibrationEffect =
                VibrationEffect.createWaveform(
                    longArrayOf(18, 100, 13),
                    intArrayOf(200, 0, 120),
                    -1
                )
            vibrator.vibrate(vibrationEffect)
        }
    }

    fun click() {
        if (enable) {
            val vibrationEffect =
                VibrationEffect.createOneShot(12, 180)
            vibrator.vibrate(vibrationEffect)
        }
    }

    fun turnOn() {
        if (enable) {
            val vibrationEffect =
                VibrationEffect.createWaveform(
                    longArrayOf(12, 130, 18),
                    intArrayOf(110, 0, 180),
                    -1
                )
            vibrator.vibrate(vibrationEffect)
        }
    }

    fun turnOff() {
        if (enable) {
            val vibrationEffect =
                VibrationEffect.createWaveform(
                    longArrayOf(18, 130, 12),
                    intArrayOf(180, 0, 110),
                    -1
                )
            vibrator.vibrate(vibrationEffect)
        }
    }

    fun done() {
        if (enable) {
            val vibrationEffect =
                VibrationEffect.createOneShot(20, 225)
            vibrator.vibrate(vibrationEffect)
        }
    }

    fun dragMove() {
        if (enable) {
            val vibrationEffect =
                VibrationEffect.createOneShot(10, 220)
            vibrator.vibrate(vibrationEffect)
        }
    }
}