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

    // Count how many of the 4 sounds have finished loading.
    private val loadedCount = AtomicInteger(0)
    private val totalSounds = 4
    @Volatile private var loaded = false

    // Pending play requests fired before all sounds were ready.
    // Stored as soundId ints; drained once all 4 are loaded.
    private val pendingPlays = ConcurrentLinkedQueue<() -> Unit>()
    // Set of soundIds already in pendingPlays to prevent duplicates.
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
                    // USAGE_MEDIA: routes to the Media volume slider and is
                    // correctly directed to sport Bluetooth headphones.
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0 && loadedCount.incrementAndGet() == totalSounds) {
                loaded = true
                // Drain any play() calls that arrived before we were ready.
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
     * Unconditionally tears down the current SoundPool and builds a fresh one,
     * reloading all four WAV files. Call this every time the app returns to the
     * foreground so that native AudioTrack sessions invalidated by OEM memory
     * management are transparently restored.
     *
     * The operation is cheap (<100 ms for 4 small WAVs) and the existing
     * pending-play queue handles any race between start() and onLoadComplete.
     *
     * Synchronized so it cannot race with the SoundPool internal callback thread.
     */
    @Synchronized
    override fun reinitialise() {
        // Release the existing pool (if it was already built) before creating a new one.
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
            // Queue the request — will be replayed once onLoadCompleteListener
            // fires for the last sound. Guard against duplicate entries for the
            // same soundId (e.g. rapid phase ticks before load completes).
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
