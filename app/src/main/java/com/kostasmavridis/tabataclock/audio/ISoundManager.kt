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
    fun release()
}
