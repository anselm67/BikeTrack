package com.anselm.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import com.anselm.location.data.DataManager
import com.anselm.location.data.RecordingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class LocationApplication: Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())
    private val trackerServiceIntent by lazy {
        Intent(this, LocationTracker::class.java)
    }

    val recordingManager by lazy {
        RecordingManager.getInstance(applicationContext!!.filesDir)
    }
    val dataManager by lazy { DataManager() }
    val okHttpClient = OkHttpClient()

    override fun onCreate() {
        super.onCreate()
        app = this
    }

    // Really quit, killing the service if needed.
    fun quit() {
        stopService(trackerServiceIntent)
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        postOnUiThread {
            activityManager.appTasks.firstOrNull()?.finishAndRemoveTask()
        }
    }

    val allPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.FOREGROUND_SERVICE,
        Manifest.permission.FOREGROUND_SERVICE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
    )

    fun checkPermissions(): Boolean {
        return allPermissions.all {
            ActivityCompat.checkSelfPermission(
                this,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun connect(serviceConnection: ServiceConnection) {
        bindService(trackerServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun disconnect(serviceConnection: ServiceConnection) {
        unbindService(serviceConnection)
        if ( ! isRecording.value ) {
            Log.d(TAG, "isRecording false => shutdown service.")
            stopService(trackerServiceIntent)
        }
    }

    private fun postOnUiThread(block: () ->Unit) {
        applicationScope.launch(Dispatchers.Main) { block() }
    }

    fun launch(block: suspend () -> Unit) {
        applicationScope.launch(Dispatchers.Default) { block() }
    }

    fun toast(msg: String) {
        applicationScope.launch(Dispatchers.Main) {
            Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
        }
    }

    // Managed by RecordingManager.
    val isRecording = mutableStateOf(false)

    fun onRecordingChanged(recording: Boolean) {
        isRecording.value = recording
    }

    // Managed by DataManager
    val isAutoPaused = mutableStateOf(false)

    fun onAutoPausedChanged(autoPaused: Boolean) {
        isAutoPaused.value = autoPaused
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var app: LocationApplication
            private set
    }
}