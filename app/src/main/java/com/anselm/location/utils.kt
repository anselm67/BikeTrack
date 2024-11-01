package com.anselm.location

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

// Function to get the start of the week (previous Monday or current day if it's Monday)
fun startOfWeek(timestamp: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zoneId)
    return dateTime
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .toLocalDate()
        .atStartOfDay() // Set time to 00:00
        .atZone(zoneId).toInstant().toEpochMilli()
}

// Function to get the start of the month
fun startOfMonth(timestamp: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zoneId)
    return dateTime
        .with(TemporalAdjusters.firstDayOfMonth())
        .toLocalDate()
        .atStartOfDay() // Set time to 00:00
        .atZone(zoneId).toInstant().toEpochMilli()
}

// Function to get the start of the month
fun startOfYear(timestamp: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zoneId)
    return dateTime
        .with(TemporalAdjusters.firstDayOfYear())
        .toLocalDate()
        .atStartOfDay() // Set time to 00:00
        .atZone(zoneId).toInstant().toEpochMilli()
}

fun asLocalDate(timestamp: Long,  zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zoneId)
}

private val YYYYMMDDFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun asYYYYMMDD(date: LocalDate): String {
    return date.format(YYYYMMDDFormatter)
}