package com.auraflow.domain.model

data class ChladniPattern(
    val n: Int,
    val m: Int,
    val seed: Long
)

const val COMPLETION_SHARE = 0.8

fun isCompleted(durationSec: Int, plannedSec: Int): Boolean = durationSec >= plannedSec * COMPLETION_SHARE

data class Session(
    val id: Long = 0,
    val startedAt: Long,
    val durationSec: Int,
    val planDay: Int?,
    val attentionReturns: Int,
    val moodBefore: Int,
    val moodAfter: Int,
    val pattern: ChladniPattern,
    val completed: Boolean = true,
    val title: String? = null
)
