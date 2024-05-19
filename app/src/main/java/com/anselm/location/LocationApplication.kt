package com.anselm.location

import android.annotation.SuppressLint
import android.app.Application
import com.anselm.location.data.DataManager
import com.anselm.location.data.RecordingManager

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