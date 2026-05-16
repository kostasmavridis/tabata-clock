package com.kostasmavridis.tabataclock.model

/**
 * Immutable snapshot of user-configured Tabata parameters.
 *
 * Values are clamped to safe ranges in the [validated] factory so that
 * corrupted DataStore entries or programmatic construction can never produce
 * a [TabataSettings] that causes integer overflow in [totalWorkoutSecs] or
 * a runaway coroutine loop in the ViewModel.
 */
data class TabataSettings(
    val prepareSecs: Int = 10,
    val workSecs:    Int = 20,
    val restSecs:    Int = 10,
    val rounds:      Int = 8,
    val sets:        Int = 1
) {
    /** Total workout duration in seconds, excluding prepare phase. */
    fun totalWorkoutSecs(): Int = sets * rounds * (workSecs + restSecs) - restSecs

    companion object {
        // Hard limits — wider than any UI slider but safe from overflow.
        val PREPARE_RANGE = 1..300
        val WORK_RANGE    = 1..600
        val REST_RANGE    = 1..600
        val ROUNDS_RANGE  = 1..99
        val SETS_RANGE    = 1..99

        /**
         * Clamp each field to its safe range.
         * Call this when constructing [TabataSettings] from an untrusted
         * source (DataStore, test fixtures, IPC).
         */
        fun validated(
            prepareSecs: Int,
            workSecs:    Int,
            restSecs:    Int,
            rounds:      Int,
            sets:        Int
        ) = TabataSettings(
            prepareSecs = prepareSecs.coerceIn(PREPARE_RANGE),
            workSecs    = workSecs   .coerceIn(WORK_RANGE),
            restSecs    = restSecs   .coerceIn(REST_RANGE),
            rounds      = rounds     .coerceIn(ROUNDS_RANGE),
            sets        = sets       .coerceIn(SETS_RANGE)
        )
    }
}
