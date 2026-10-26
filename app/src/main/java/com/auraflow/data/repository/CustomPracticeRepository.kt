package com.auraflow.data.repository

import com.auraflow.data.local.db.CustomPracticeDao
import com.auraflow.data.local.db.toDomain
import com.auraflow.data.local.db.toEntity
import com.auraflow.domain.plan.CustomPractice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomPracticeRepository(private val dao: CustomPracticeDao) {
    val practices: Flow<List<CustomPractice>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun get(id: Long): CustomPractice? = dao.get(id)?.toDomain()

    suspend fun save(practice: CustomPractice): Long {
        val createdAt = dao.get(practice.id)?.createdAt ?: System.currentTimeMillis()
        val id = dao.upsert(practice.toEntity(createdAt))
        return if (id == -1L) practice.id else id
    }

    suspend fun delete(id: Long) = dao.delete(id)
}
