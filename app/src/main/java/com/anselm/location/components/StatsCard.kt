package com.anselm.location.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.anselm.location.Graph
import com.anselm.location.GraphAppearance
import com.anselm.location.MIN_SAMPLES_FOR_PLOT
import com.anselm.location.R
import com.anselm.location.data.StatsEntry

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
private fun Back(statsEntries: List<StatsEntry>, xLabelFormatter: (Float) -> String) {
    if ( statsEntries.size < MIN_SAMPLES_FOR_PLOT ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.errorContainer),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Ride some more!",
                style = MaterialTheme.typography.titleLarge,
            )
        }
    } else {
        val period = (1..statsEntries.size).map { it.toFloat() }
        val values = statsEntries.map { (it.distance / 1000).toFloat() }
        Graph(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            xValues = period,
            yValues = values,
            graphAppearance = GraphAppearance(
                graphColor = Color.Blue,
                graphAxisColor = MaterialTheme.colorScheme.primary,
                graphThickness = 3f,
                isColorAreaUnderChart = true,
                colorAreaUnderChart = Color.Green,
                isCircleVisible = false,
                circleColor = MaterialTheme.colorScheme.secondary,
                backgroundColor = MaterialTheme.colorScheme.background,
                pointWidth = 10f,
                xLabelFormatter = xLabelFormatter,
            )
        )
    }
}

@Composable
fun StatsCard(
    key: String,
    titleFormatter: (Long) -> String,
    statsEntries: List<StatsEntry>,
    modifier: Modifier = Modifier,
    xLabelFormatter: (Float) -> String
) {
    FlipCard(
        key = key,
        modifier = modifier.fillMaxSize(),
        drawableId = R.drawable.ic_show_chart,
        front = {
            Front(titleFormatter, statsEntries)
        },
        back = {
            Back(statsEntries, xLabelFormatter)
        }
    )
}