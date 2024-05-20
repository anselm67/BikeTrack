package com.anselm.location

import android.annotation.SuppressLint
import android.app.Application
import com.anselm.location.data.DataManager
import com.anselm.location.data.RecordingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LocationApplication: Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())
    val recordingManager by lazy {
        RecordingManager.getInstance(applicationContext!!.filesDir)
    }
    val dataManager by lazy { DataManager() }

    override fun onCreate() {
        super.onCreate()
        app = this
    }

    fun postOnUiThread(block: () ->Unit) {
        applicationScope.launch(Dispatchers.Main) { block() }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var app: LocationApplication
            private set
    }
}