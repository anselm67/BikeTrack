package com.anselm.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import com.anselm.location.data.DataManager
import com.anselm.location.data.LocationStub
import com.anselm.location.data.RecordingManager
import com.anselm.location.data.Sample
import com.anselm.location.data.defaultSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LocationApplication: Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())
    private val trackerServiceIntent by lazy {
        Intent(this, LocationTracker::class.java)
    }

    val recordingManager by lazy {
        RecordingManager.getInstance(applicationContext!!.filesDir)
    }
    val dataManager by lazy { DataManager() }

    override fun onCreate() {
        super.onCreate()
        app = this
    }

    override fun onTerminate() {
        super.onTerminate()
        trackerConnections.forEach {
            it.close()
        }
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

    var isTrackerBound = mutableStateOf(false)

    val trackerConnections = mutableListOf<TrackerConnection>()

    inner class TrackerConnection : ServiceConnection {
        var binder: LocationTracker.TrackerBinder? = null
        var flow: StateFlow<Sample>? = null

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = (service as LocationTracker.TrackerBinder)
            flow = binder!!.flow
                .stateIn(
                    applicationScope,   // TODO
                    SharingStarted.Eagerly,
                    defaultSample.copy(location = LocationStub())
                )
            isTrackerBound.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "onServiceDisconnected")
            close()
        }

        fun close() {
            val isRecording = binder?.isRecording?.value == true
            binder?.close()
            binder = null
            flow = null
            isTrackerBound.value = false
            applicationContext.unbindService(this)
            trackerConnections.remove(this)
            // If no one is recording, we can shutdown the service.
            if ( ! isRecording ) {
                Log.d(TAG, "isRecording false => shutdown service.")
                stopService(trackerServiceIntent)
            }
        }
    }

    fun connect(): TrackerConnection {
        val connection = TrackerConnection()
        bindService(trackerServiceIntent, connection, Context.BIND_AUTO_CREATE)
        trackerConnections.add(connection)
        return connection
    }

    private fun postOnUiThread(block: () ->Unit) {
        applicationScope.launch(Dispatchers.Main) { block() }
    }

    fun launch(block: () -> Unit) {
        applicationScope.launch { block() }
    }

    fun toast(msg: String) {
        applicationScope.launch(Dispatchers.Main) {
            Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var app: LocationApplication
            private set
    }
}