package com.auraflow.domain.model

data class ChladniPattern(
    val n: Int,
    val m: Int,
    val seed: Long
)

data class Session(
    val id: Long = 0,

    val startedAt: Long,
    val durationSec: Int,

    val planDay: Int?,

    val attentionReturns: Int,

    val moodBefore: Int,
    val moodAfter: Int,
    val pattern: ChladniPattern
)
