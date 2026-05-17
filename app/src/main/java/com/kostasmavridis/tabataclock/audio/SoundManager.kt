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
import java.util.Collections
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

class SoundManager @Inject constructor(private val context: Context) : ISoundManager {

    // Guarded by `this` — reinitialise() and release() are @Synchronized so that
    // SoundPool.release() + buildPool() on the main thread cannot race with
    // onLoadCompleteListener callbacks on the SoundPool internal thread.
    private lateinit var soundPool: SoundPool

    private var beepId: Int = 0
    private var workId: Int = 0
    private var restId: Int = 0
    private var doneId: Int = 0

    private val loadedCount = AtomicInteger(0)
    private val totalSounds = 4
    @Volatile private var loaded = false

    private val pendingPlays = ConcurrentLinkedQueue<() -> Unit>()
    private val pendingSoundIds: MutableSet<Int> = Collections.synchronizedSet(mutableSetOf())

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
        buildPool()
    }

    // ---------------------------------------------------------------------------
    // Pool lifecycle
    // ---------------------------------------------------------------------------

    private fun buildPool() {
        loadedCount.set(0)
        loaded = false
        pendingPlays.clear()
        pendingSoundIds.clear()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0 && loadedCount.incrementAndGet() == totalSounds) {
                loaded = true
                while (pendingPlays.isNotEmpty()) {
                    pendingPlays.poll()?.invoke()
                }
                pendingSoundIds.clear()
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

    /**
     * Tears down the current [SoundPool] and builds a fresh one.
     * Synchronized so it cannot race with the SoundPool internal callback thread.
     */
    @Synchronized
    override fun reinitialise() {
        try { soundPool.release() } catch (_: Exception) { }
        buildPool()
        Log.d("SoundManager", "reinitialise: SoundPool rebuilt")
    }

    // ---------------------------------------------------------------------------
    // Playback
    // ---------------------------------------------------------------------------

    override fun playBeep() = play(beepId, shortVibrate = false)
    override fun playWork() = play(workId, shortVibrate = true)
    override fun playRest() = play(restId, shortVibrate = true)
    override fun playDone() = play(doneId, shortVibrate = true)

    private fun play(soundId: Int, shortVibrate: Boolean) {
        if (soundId == 0) return
        if (loaded) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        } else {
            if (pendingSoundIds.add(soundId)) {
                pendingPlays.offer { soundPool.play(soundId, 1f, 1f, 1, 0, 1f) }
            }
        }
        if (shortVibrate) vibrate()
    }

    private fun vibrate() {
        val effect = VibrationEffect.createOneShot(150L, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(effect)
    }

    @Synchronized
    override fun release() {
        pendingPlays.clear()
        pendingSoundIds.clear()
        soundPool.release()
    }
}
