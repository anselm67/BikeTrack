package com.anselm.location.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.data.RecordingManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class ViewRecordingsModel: ViewModel() {
    var showSearchBox by mutableStateOf(false)
    var showBottomSheet by mutableStateOf(false)
    var queryRange by mutableStateOf(RecordingManager.Query.default.rangeInKilometers)
    var queryTags by mutableStateOf(setOf<String>())

    private val queryFlow = MutableStateFlow(RecordingManager.Query.default)

    @OptIn(ExperimentalCoroutinesApi::class)
    val resultFlow = queryFlow.flatMapLatest { query ->
        flow { emit( Pair(query, app.recordingManager.list(query)) ) }
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        Pair(RecordingManager.Query.default, emptyList())
    )

    fun updateQuery() {
        queryFlow.value = RecordingManager.Query(
            queryRange.start*1000f..queryRange.endInclusive*1000f,
            queryTags
        )
    }

    fun resetQuery() {
        queryFlow.value = RecordingManager.Query.default
        queryRange = RecordingManager.Query.default.rangeInKilometers
        queryTags = RecordingManager.Query.default.tags
    }
}