package com.anselm.location.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.data.Recording
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RecordingDetailsViewModel(
    recordingId: String,
): ViewModel() {
    // TODO Handle errors.
    private val recordingFlow = MutableStateFlow(
        app.recordingManager.load(recordingId)
    )

    fun updateRecording(recording: Recording) {
        this.recordingFlow.value = recording
        // Launch a coroutine to update the database.
        app.launch {
            recording.saveEntry()
        }
    }

    val recordingState: StateFlow<Recording> = recordingFlow.asStateFlow()
    class   Factory(private val recordingId: String) :
        ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return requireNotNull(RecordingDetailsViewModel(recordingId) as? T) {
                "Cannot create an instance of $modelClass"
            }
        }
    }
}