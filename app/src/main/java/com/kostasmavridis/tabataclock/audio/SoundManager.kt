package com.kostasmavridis.tabataclock.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.kostasmavridis.tabataclock.R
import javax.inject.Inject

class SoundManager @Inject constructor(private val context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var beepId: Int = 0
    private var workId: Int = 0
    private var restId: Int = 0
    private var doneId: Int = 0
    private var loaded = false

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    init {
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) loaded = true
        }
        // Load sound resources — raw files must exist in res/raw/
        try {
            beepId = soundPool.load(context, R.raw.beep,      1)
            workId = soundPool.load(context, R.raw.work_start, 1)
            restId = soundPool.load(context, R.raw.rest_start, 1)
            doneId = soundPool.load(context, R.raw.done,       1)
        } catch (e: Exception) {
            Log.w("SoundManager", "Sound file missing: ${e.message}")
        }
    }

    fun playBeep()      = play(beepId,  shortVibrate = false)
    fun playWork()      = play(workId,  shortVibrate = true)
    fun playRest()      = play(restId,  shortVibrate = true)
    fun playDone()      = play(doneId,  shortVibrate = true)

    private fun play(soundId: Int, shortVibrate: Boolean) {
        if (loaded && soundId != 0) soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        if (shortVibrate) vibrate()
    }

    private fun vibrate() {
        val effect = VibrationEffect.createOneShot(150L, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(effect)
    }

    fun release() = soundPool.release()
}
