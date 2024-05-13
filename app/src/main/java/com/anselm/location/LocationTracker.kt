package com.anselm.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow


private const val CHANNEL_ID = "ForegroundServiceChannel";

class LocationTracker: Service() {
    private var fusedLocationClient: FusedLocationProviderClient? = null;

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        startLocationTracker();
        return START_STICKY;
    }

    override fun onDestroy() {
        super.onDestroy();
        stopLocationTracker();
    }

    private var locationFlow: Flow<Location>? = null;
    @SuppressLint("MissingPermission")
    private fun setupLocationFlow() {
        locationFlow = callbackFlow {
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    Log.d(TAG, "Location ${locationResult.lastLocation}");
                    locationResult.lastLocation?.let { location ->
                        trySendBlocking(location)
                            .onFailure { e -> Log.e(TAG, "Failed to send location", e) }
                    }
                }
                override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                    Log.d(TAG, "Location availability $locationAvailability");
                }
            }

            fusedLocationClient?.requestLocationUpdates(
                LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    5000
                ).build(),
                locationCallback,
                Looper.getMainLooper()
            );

            awaitClose {
                fusedLocationClient?.removeLocationUpdates(locationCallback);
            }
        }

    }

    @SuppressLint("MissingPermission")
    private fun startLocationTracker() {
        Log.d(TAG, "startLocationTracker");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
        val notification = Notification.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Biking")
            .setContentText("Location tracker is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOnlyAlertOnce(true)
            .setWhen(System.currentTimeMillis())
            .build();

        startForeground(1, notification);
    }

    private fun stopLocationTracker() {
        Log.d(TAG, "stopLocationTracker");
        fusedLocationClient = null;
        stopForeground(STOP_FOREGROUND_REMOVE);
    }
    inner class TrackerBinder : Binder() {
        fun getFlow(): Flow<Location>? = this@LocationTracker.locationFlow
    }

    override fun onBind(intent: Intent?): IBinder {
        assert( fusedLocationClient != null);
        setupLocationFlow()
        return TrackerBinder()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Location Service Channel",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

}