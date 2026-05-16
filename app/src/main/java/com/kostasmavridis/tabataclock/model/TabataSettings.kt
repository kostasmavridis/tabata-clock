package com.kostasmavridis.tabataclock.model

data class TabataSettings(
    val prepareSecs: Int = 10,
    val workSecs: Int = 20,
    val restSecs: Int = 10,
    val rounds: Int = 8,
    val sets: Int = 1
) {
    /** Total workout duration in seconds, excluding prepare phase */
    fun totalWorkoutSecs(): Int = sets * rounds * (workSecs + restSecs) - restSecs
}
