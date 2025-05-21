package com.anselm.location.screens

import android.util.Log
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
import com.anselm.location.models.LocalAppViewModel
import java.time.format.DateTimeFormatter
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

private val YYYY = DateTimeFormatter.ofPattern("YYYY")
private val MMYY = DateTimeFormatter.ofPattern("MM/yy")
private val YYMMDD = DateTimeFormatter.ofPattern("yy-MM-dd")

@Composable
fun StatsScreen() {
    val appViewModel = LocalAppViewModel.current
    appViewModel
        .updateTitle(title = "Your statistics")
        .setShowOnLockScreen(false)

    val annualStats = app.recordingManager.annualStats()
    val monthlyStats = app.recordingManager.monthlyStats()
    val weeklyStats = app.recordingManager.weeklyStats()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        StatsCard(
            key = "AnnualStats",
            titleFormatter = {  millis: Long ->  asLocalDate(millis).format(ANNUAL_FORMATTER) },
            annualStats,
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            xLabelFormatter = {
                val index = it.toInt()
                if (index >= 0 && index < annualStats.size)
                    YYYY.format(asLocalDate(annualStats[index].timestamp).toLocalDate())
                else
                    ""
            }
        )
        StatsCard(
            key = "MonthlyStats",
            titleFormatter = {  millis: Long ->  asLocalDate(millis).format(MONTHLY_FORMATTER) },
            monthlyStats,
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            xLabelFormatter = {
                val index = it.toInt()
                if (index >= 0 && index < monthlyStats.size)
                    MMYY.format(asLocalDate(monthlyStats[index].timestamp).toLocalDate())
                else
                    ""

            }
        )
        StatsCard(
            key = "WeeklyStats",
            titleFormatter = {  millis: Long ->  asLocalDate(millis).format(WEEKLY_FORMATTER) },
            weeklyStats,
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            xLabelFormatter = {
                val index = it.toInt()
                if (index >= 0 && index < weeklyStats.size)
                    YYMMDD.format(asLocalDate(weeklyStats[index].timestamp).toLocalDate())
                else
                    ""
            }
        )
    }
}