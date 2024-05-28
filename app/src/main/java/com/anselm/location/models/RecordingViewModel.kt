package com.anselm.location.models

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.LocationTracker
import com.anselm.location.TAG
import com.anselm.location.data.Entry
import com.anselm.location.data.Sample
import com.anselm.location.data.defaultSample
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.io.Closeable


class RecordingViewModel: ViewModel() {
    private var serviceConnection: TrackerConnection? = null

    val isConnected = mutableStateOf(false)
    var sampleFlow: StateFlow<Sample>? = null

    fun connect() {
        if ( serviceConnection == null ) {
            serviceConnection = TrackerConnection()
            app.connect(serviceConnection!!)
            addCloseable(serviceConnection!!)
        }
    }

    fun acceptConnection(binder: LocationTracker.TrackerBinder?) {
        sampleFlow = binder?.flow?.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = defaultSample
        )
        isConnected.value = true
    }

    fun startRecording() {
        serviceConnection?.startRecording()
    }
    fun stopRecording(): Entry? {
        return serviceConnection?.stopRecording()
    }

    inner class TrackerConnection : ServiceConnection, Closeable {
        private var binder: LocationTracker.TrackerBinder? = null

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = service as LocationTracker.TrackerBinder
            this@RecordingViewModel.acceptConnection(binder)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "onServiceDisconnected")
            close()
        }

        override fun close() {
            binder?.close()
            binder = null
            app.disconnect(this)
        }

        fun startRecording() {
            binder?.startRecording()
        }

        fun stopRecording() : Entry? {
            return binder?.stopRecording()
        }

    }

}