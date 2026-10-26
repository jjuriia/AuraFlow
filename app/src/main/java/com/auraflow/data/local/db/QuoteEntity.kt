package com.auraflow.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quotes")
data class QuoteEntity(

    @PrimaryKey val id: Int,
    val text: String,
    val author: String
)
