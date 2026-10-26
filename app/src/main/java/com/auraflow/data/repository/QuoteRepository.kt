package com.auraflow.data.repository

import android.content.res.AssetManager
import com.auraflow.data.local.db.QuoteDao
import com.auraflow.data.local.db.QuoteEntity
import com.auraflow.data.local.prefs.UserPreferences
import com.auraflow.data.remote.QuotesApi
import com.auraflow.data.remote.dto.QuoteDto
import com.auraflow.domain.model.Quote
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuoteRepository(
    private val api: QuotesApi,
    private val dao: QuoteDao,
    private val prefs: UserPreferences,
    private val assets: AssetManager,
    private val quotesUrl: String
) {
    suspend fun sync() {
        if (prefs.quotesFromNetwork()) return
        try {
            val remote = api.getQuotes(quotesUrl).toEntities()
            check(remote.isNotEmpty()) { "Файл цитат пуст" }
            dao.insertAll(remote)
            prefs.setQuotesFromNetwork(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (dao.count() == 0) dao.insertAll(readBundled())
        }
    }

    suspend fun quoteForDay(epochDay: Long): Quote? {
        val count = dao.count()
        if (count == 0) return null
        val index = Math.floorMod(epochDay, count.toLong()).toInt()
        return dao.getAt(index)?.let { Quote(it.text, it.author) }
    }

    private suspend fun readBundled(): List<QuoteEntity> = withContext(Dispatchers.IO) {
        assets.open(BUNDLED_FILE).reader(Charsets.UTF_8).use { reader ->
            Gson().fromJson(reader, Array<QuoteDto>::class.java).toList().toEntities()
        }
    }

    private fun List<QuoteDto>.toEntities(): List<QuoteEntity> = mapNotNull { dto ->
        val id = dto.id ?: return@mapNotNull null
        val text = dto.text?.trim().takeUnless { it.isNullOrEmpty() } ?: return@mapNotNull null
        QuoteEntity(id = id, text = text, author = dto.author?.trim().orEmpty())
    }

    private companion object {
        const val BUNDLED_FILE = "quotes_ru.json"
    }
}
