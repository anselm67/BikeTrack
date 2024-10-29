package com.anselm.location.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.anselm.location.R
import com.anselm.location.data.StatsEntry
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@Composable
private fun RunningTime(timeMillis: Long) {
    Text(
        text = "%02d:%02d:%02d".format(
            *(timeMillis).toDuration(DurationUnit.MILLISECONDS)
                .toComponents { hours, minutes, seconds, _ ->
                    arrayOf(hours, minutes, seconds)
                }
        ),
        style = MaterialTheme.typography.displaySmall,
    )
}

@Composable
private fun Stats(
    titleFormatter: (Long) -> String,
    statsEntry: StatsEntry,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceAround,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                titleFormatter(statsEntry.timestamp),
                style = MaterialTheme.typography.displaySmall,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "%.2f".format(statsEntry.distance / 1000.0),
                style = MaterialTheme.typography.displaySmall,
            )
            Text(" in ")
            RunningTime(statsEntry.elapsedTime)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            NumberWithUnits(
                value = "%3.1f".format(statsEntry.climb),
                units = "Climb",
            )
            NumberWithUnits(
                value = "%3.1f".format(statsEntry.descent),
                units = "Descent",
            )
        }

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Front(titleFormatter: (Long) -> String, statsEntries: List<StatsEntry>) {
    val pagerState = rememberPagerState(
        pageCount = { statsEntries.size },
        initialPage = statsEntries.size - 1,
    )

    HorizontalPager(state = pagerState) { periodNumber ->
        val statsEntry = statsEntries[periodNumber]
        Stats(titleFormatter, statsEntry)
    }
}

@Composable
fun StatsCard(
    key: String,
    titleFormatter: (Long) -> String,
    statsEntries: List<StatsEntry>,
    modifier: Modifier = Modifier
) {
    FlipCard(
        key = key,
        modifier = modifier.fillMaxSize(),
        drawableId = R.drawable.ic_show_chart,
        front = {
            Front(titleFormatter, statsEntries)
        },
        back = {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
            ) {
                Row (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text("Back")
                }
            }
        }
    )
}