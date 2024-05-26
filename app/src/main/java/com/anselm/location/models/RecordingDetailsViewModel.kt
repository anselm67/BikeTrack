package com.anselm.location.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.data.Recording
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class Wrapper<T>(
    val value: T
)

class RecordingDetailsViewModel(
    recording: Recording,
): ViewModel() {
    // TODO Handle errors.
    private val recordingFlow = MutableStateFlow(
        Wrapper(recording)
    )

    fun updateRecording(recording: Recording) {
        this.recordingFlow.value = Wrapper(recording)
        // Launch a coroutine to update the database.
        app.launch {
            recording.save()
        }
    }

    val recordingState = recordingFlow.asStateFlow()

    val isEditing = mutableStateOf(false)

    val columnScrollingEnabled = mutableStateOf(true)

    val showDeleteDialog = mutableStateOf(false)

    class Factory(private val recording: Recording) :
        ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return requireNotNull(value = RecordingDetailsViewModel(recording) as? T) {
                "Cannot create an instance of $modelClass"
            }
        }
    }
}