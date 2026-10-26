package com.auraflow.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.auraflow.domain.model.ChladniPattern
import com.auraflow.domain.model.Session

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "duration_sec") val durationSec: Int,
    @ColumnInfo(name = "plan_day") val planDay: Int?,
    @ColumnInfo(name = "attention_returns") val attentionReturns: Int,
    @ColumnInfo(name = "mood_before") val moodBefore: Int,
    @ColumnInfo(name = "mood_after") val moodAfter: Int,
    @ColumnInfo(name = "pattern_n") val patternN: Int,
    @ColumnInfo(name = "pattern_m") val patternM: Int,
    @ColumnInfo(name = "pattern_seed") val patternSeed: Long,
    @ColumnInfo(name = "completed", defaultValue = "1") val completed: Boolean = true,
    @ColumnInfo(name = "title") val title: String? = null
)

fun SessionEntity.toDomain() = Session(
    id = id,
    startedAt = startedAt,
    durationSec = durationSec,
    planDay = planDay,
    attentionReturns = attentionReturns,
    moodBefore = moodBefore,
    moodAfter = moodAfter,
    pattern = ChladniPattern(patternN, patternM, patternSeed),
    completed = completed,
    title = title
)

fun Session.toEntity() = SessionEntity(
    id = id,
    startedAt = startedAt,
    durationSec = durationSec,
    planDay = planDay,
    attentionReturns = attentionReturns,
    moodBefore = moodBefore,
    moodAfter = moodAfter,
    patternN = pattern.n,
    patternM = pattern.m,
    patternSeed = pattern.seed,
    completed = completed,
    title = title
)
