package com.anselm.location.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.asLocalDate
import com.anselm.location.components.StatsCard
import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

// Custom formatter to include the day of the week and ordinal date
private val WEEKLY_FORMATTER = DateTimeFormatterBuilder()
    .appendPattern("E, MMM d Y")
    .optionalStart()
    .parseDefaulting(ChronoField.DAY_OF_MONTH, 1) // Default to 1 if no day is provided
    .optionalEnd()
    .toFormatter()

private val MONTHLY_FORMATTER = DateTimeFormatterBuilder()
    .appendPattern("MMMM YYYY")
    .optionalStart()
    .parseDefaulting(ChronoField.DAY_OF_MONTH, 1) // Default to 1 if no day is provided
    .optionalEnd()
    .toFormatter()

private val ANNUAL_FORMATTER = DateTimeFormatterBuilder()
    .appendPattern("YYYY")
    .optionalStart()
    .parseDefaulting(ChronoField.DAY_OF_MONTH, 1) // Default to 1 if no day is provided
    .optionalEnd()
    .toFormatter()

@Composable
fun StatsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        StatsCard(
            key = "AnnualStats",
            titleFormatter = {  millis: Long ->  asLocalDate(millis).format(ANNUAL_FORMATTER) },
            app.recordingManager.annualStats(),
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
        )
        StatsCard(
            key = "MonthlyStats",
            titleFormatter = {  millis: Long ->  asLocalDate(millis).format(MONTHLY_FORMATTER) },
            app.recordingManager.monthlyStats(),
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
        )
        StatsCard(
            key = "WeeklyStats",
            titleFormatter = {  millis: Long ->  asLocalDate(millis).format(WEEKLY_FORMATTER) },
            app.recordingManager.weeklyStats(),
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
        )
    }
}