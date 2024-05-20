package com.anselm.location

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.anselm.location.data.DataManager
import com.anselm.location.data.LocationStub
import com.anselm.location.data.RecordingManager
import com.anselm.location.data.Sample
import com.anselm.location.data.defaultSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LocationApplication: Application() {
    val recordingManager by lazy {
        RecordingManager.getInstance(applicationContext!!.filesDir)
    }
    val dataManager by lazy { DataManager() }

    override fun onCreate() {
        super.onCreate()
        app = this
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var app: LocationApplication
            private set
    }
}