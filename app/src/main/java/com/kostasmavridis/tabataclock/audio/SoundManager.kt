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
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

class SoundManager @Inject constructor(private val context: Context) : ISoundManager {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                // USAGE_MEDIA: routes to the Media volume slider and is
                // correctly directed to sport Bluetooth headphones.
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var beepId: Int = 0
    private var workId: Int = 0
    private var restId: Int = 0
    private var doneId: Int = 0

    // Count how many of the 4 sounds have finished loading.
    // Only set loaded=true when ALL four are ready to avoid playing
    // a sound whose SoundPool ID is still 0.
    private val loadedCount = AtomicInteger(0)
    private val totalSounds = 4
    @Volatile private var loaded = false

    // Pending play requests fired before all sounds were ready.
    // Each entry is a lambda that calls soundPool.play() with the correct soundId.
    private val pendingPlays = ConcurrentLinkedQueue<() -> Unit>()

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
            if (status == 0 && loadedCount.incrementAndGet() == totalSounds) {
                loaded = true
                // Drain any play() calls that arrived before we were ready.
                while (pendingPlays.isNotEmpty()) {
                    pendingPlays.poll()?.invoke()
                }
            }
        }
        try {
            beepId = soundPool.load(context, R.raw.beep,       1)
            workId = soundPool.load(context, R.raw.work_start, 1)
            restId = soundPool.load(context, R.raw.rest_start, 1)
            doneId = soundPool.load(context, R.raw.done,       1)
        } catch (e: Exception) {
            Log.w("SoundManager", "Sound file missing: ${e.message}")
        }
    }

    override fun playBeep() = play(beepId, shortVibrate = false)
    override fun playWork() = play(workId, shortVibrate = true)
    override fun playRest() = play(restId, shortVibrate = true)
    override fun playDone() = play(doneId, shortVibrate = true)

    private fun play(soundId: Int, shortVibrate: Boolean) {
        if (soundId == 0) return
        if (loaded) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        } else {
            // Queue the request — will be replayed once onLoadCompleteListener
            // fires for the last sound. Only queue one pending play per sound to
            // avoid stacking up multiple beeps if the timer fires rapidly before load.
            val alreadyQueued = pendingPlays.any { it.javaClass == soundId.javaClass }
            if (!alreadyQueued) {
                pendingPlays.offer { soundPool.play(soundId, 1f, 1f, 1, 0, 1f) }
            }
        }
        if (shortVibrate) vibrate()
    }

    private fun vibrate() {
        val effect = VibrationEffect.createOneShot(150L, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(effect)
    }

    override fun release() {
        pendingPlays.clear()
        soundPool.release()
    }
}
