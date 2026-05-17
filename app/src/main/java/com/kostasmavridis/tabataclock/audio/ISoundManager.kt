package com.kostasmavridis.tabataclock.audio

/**
 * Contract for audio/haptic feedback during a Tabata session.
 * Decoupled from [SoundManager] so unit tests can inject a silent fake
 * without touching Android framework classes.
 */
interface ISoundManager {
    fun playBeep()
    fun playWork()
    fun playRest()
    fun playDone()
    /**
     * Tear down and rebuild the SoundPool, reloading all sounds from raw resources.
     * Must be called each time the app returns to the foreground so that native
     * AudioTrack sessions invalidated by aggressive OEM memory management
     * (e.g. Oppo/OnePlus) are transparently restored.
     */
    fun reinitialise()
    fun release()
}
