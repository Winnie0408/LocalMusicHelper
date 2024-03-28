package com.hwinzniej.musichelper.utils

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

class MyVibrationEffect(
    context: Context,
    private val enable: Boolean,
    private val strength: Int
) {
    private val vibrator: Vibrator = context.getSystemService(Vibrator::class.java)

    fun dialog() {
        if (enable) {
            val vibrationEffect =
                VibrationEffect.createWaveform(
                    longArrayOf(
                        (18 + (strength - 3) * 3).toLong(),
                        100,
                        (13 + (strength - 3) * 3).toLong()
                    ),
                    intArrayOf(200 + (strength - 3) * 15, 0, 120 + (strength - 3) * 15),
                    -1
                )
            vibrator.vibrate(vibrationEffect)
        }
    }

    fun click() {
        if (enable) {
            val vibrationEffect =
                VibrationEffect.createOneShot(
                    (12 + (strength - 3) * 3).toLong(),
                    180 + (strength - 3) * 15
                )
            vibrator.vibrate(vibrationEffect)
        }
    }

    fun turnOn() {
        if (enable) {
            val vibrationEffect =
                VibrationEffect.createWaveform(
                    longArrayOf(
                        (12 + (strength - 3) * 3).toLong(),
                        130,
                        (18 + (strength - 3) * 3).toLong()
                    ),
                    intArrayOf(110 + (strength - 3) * 15, 0, 180 + (strength - 3) * 15),
                    -1
                )
            vibrator.vibrate(vibrationEffect)
        }
    }

    fun turnOff() {
        if (enable) {
            val vibrationEffect =
                VibrationEffect.createWaveform(
                    longArrayOf(
                        (18 + (strength - 3) * 3).toLong(),
                        130,
                        (12 + (strength - 3) * 3).toLong()
                    ),
                    intArrayOf(180 + (strength - 3) * 15, 0, 110 + (strength - 3) * 15),
                    -1
                )
            vibrator.vibrate(vibrationEffect)
        }
    }

    fun done() {
        if (enable) {
            val vibrationEffect =
                VibrationEffect.createOneShot(
                    (20 + (strength - 3) * 3).toLong(),
                    225 + (strength - 3) * 15
                )
            vibrator.vibrate(vibrationEffect)
        }
    }

    fun dragMove() {
        if (enable) {
            val vibrationEffect =
                VibrationEffect.createOneShot(
                    (10 + (strength - 3) * 3).toLong(),
                    220 + (strength - 3) * 15
                )
            vibrator.vibrate(vibrationEffect)
        }
    }
}