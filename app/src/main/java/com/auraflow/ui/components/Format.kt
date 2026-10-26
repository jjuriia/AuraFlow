package com.auraflow.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RU = Locale.forLanguageTag("ru-RU")
private val DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM, HH:mm", RU)

fun formatDuration(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

fun formatDate(epochMillis: Long): String =
    DATE_FORMAT.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

fun formatMoodDelta(delta: Double?): String = delta?.let { "%+.1f".format(RU, it) } ?: "—"
