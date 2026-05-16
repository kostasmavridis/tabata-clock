package com.kostasmavridis.tabataclock

import com.kostasmavridis.tabataclock.audio.SoundManager

/**
 * Silent stand-in for [SoundManager].
 * Records which sounds were triggered so tests can assert on them.
 */
class FakeSoundManager : SoundManager(TODO("Not used in fake")) {

    val beepCount   get() = _beepCount
    val workCount   get() = _workCount
    val restCount   get() = _restCount
    val doneCount   get() = _doneCount

    private var _beepCount = 0
    private var _workCount = 0
    private var _restCount = 0
    private var _doneCount = 0

    override fun playBeep() { _beepCount++ }
    override fun playWork() { _workCount++ }
    override fun playRest() { _restCount++ }
    override fun playDone() { _doneCount++ }
    override fun release()  { /* no-op */ }
}
