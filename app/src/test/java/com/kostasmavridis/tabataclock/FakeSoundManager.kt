package com.kostasmavridis.tabataclock

import com.kostasmavridis.tabataclock.audio.ISoundManager

/**
 * Silent stand-in for [ISoundManager].
 * Records which sounds were triggered so tests can assert on them.
 */
class FakeSoundManager : ISoundManager {

    private var _beepCount = 0
    private var _workCount = 0
    private var _restCount = 0
    private var _doneCount = 0
    private var _reinitialiseCount = 0

    val beepCount:         Int get() = _beepCount
    val workCount:         Int get() = _workCount
    val restCount:         Int get() = _restCount
    val doneCount:         Int get() = _doneCount
    val reinitialiseCount: Int get() = _reinitialiseCount

    override fun playBeep()             { _beepCount++ }
    override fun playWork()             { _workCount++ }
    override fun playRest()             { _restCount++ }
    override fun playDone()             { _doneCount++ }
    override fun reinitialise()         { _reinitialiseCount++ }
    override fun reinitialiseIfNeeded() { _reinitialiseCount++ }
    override fun release()              { /* no-op */ }
}
