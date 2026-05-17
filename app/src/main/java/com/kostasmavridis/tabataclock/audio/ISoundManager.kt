package com.kostasmavridis.tabataclock.audio

interface ISoundManager {
    fun playBeep()
    fun playWork()
    fun playRest()
    fun playDone()
    /**
     * Tears down and rebuilds the SoundPool, reloading all sounds.
     * Safe to call when the timer is NOT running (e.g. app foregrounded
     * while idle). For mid-run reinitialisation use [reinitialiseIfNeeded].
     */
    fun reinitialise()
    /**
     * Same as [reinitialise] but safe to call at any time, including while
     * the timer is running. Used when returning from Settings to recover
     * AudioTrack sessions that may have been silenced by a focus change
     * during the navigation transition.
     */
    fun reinitialiseIfNeeded()
    fun release()
}
