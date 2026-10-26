package com.auraflow.data.remote

import com.auraflow.data.remote.dto.QuoteDto
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

interface QuotesApi {
    @GET
    suspend fun getQuotes(@Url url: String): List<QuoteDto>

    companion object {
        private const val BASE_URL = "https://raw.githubusercontent.com/"

        fun create(): QuotesApi = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QuotesApi::class.java)
    }
}
