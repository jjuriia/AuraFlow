package com.auraflow.data.repository

import com.auraflow.data.local.db.SessionDao
import com.auraflow.data.local.db.toDomain
import com.auraflow.data.local.db.toEntity
import com.auraflow.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionRepository(private val dao: SessionDao) {
    val sessions: Flow<List<Session>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun save(session: Session): Long = dao.insert(session.toEntity())
}
