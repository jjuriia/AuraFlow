package com.auraflow.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.auraflow.domain.audio.NatureSound
import com.auraflow.domain.plan.BreathTechnique
import com.auraflow.domain.plan.CustomPractice
import com.auraflow.domain.plan.PracticeBlock
import com.auraflow.domain.plan.PracticeStage

@Entity(tableName = "custom_practices")
data class CustomPracticeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "blocks") val blocks: String,
    @ColumnInfo(name = "technique") val technique: String,
    @ColumnInfo(name = "sound") val sound: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

fun CustomPracticeEntity.toDomain() = CustomPractice(
    id = id,
    name = name,
    blocks = blocks.split(',').mapNotNull { item ->
        val (stage, minutes) = item.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
        PracticeBlock(
            PracticeStage.entries.firstOrNull { it.name == stage } ?: return@mapNotNull null,
            minutes.toIntOrNull() ?: return@mapNotNull null
        )
    },
    technique = BreathTechnique.entries.firstOrNull { it.name == technique } ?: BreathTechnique.CALM,
    sound = NatureSound.entries.firstOrNull { it.name == sound }
)

fun CustomPractice.toEntity(createdAt: Long) = CustomPracticeEntity(
    id = id,
    name = name.trim(),
    blocks = blocks.joinToString(",") { "${it.stage.name}:${it.minutes}" },
    technique = technique.name,
    sound = sound?.name,
    createdAt = createdAt
)
