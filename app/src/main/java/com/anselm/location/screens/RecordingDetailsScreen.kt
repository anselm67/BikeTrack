package com.anselm.location.screens

import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anselm.location.LocalNavController
import com.anselm.location.LocationApplication.Companion.app
import com.anselm.location.NavigationItem
import com.anselm.location.R
import com.anselm.location.components.AltitudeCard
import com.anselm.location.components.RecordingMetaData
import com.anselm.location.components.SpeedCard
import com.anselm.location.components.TimeElapsedCard
import com.anselm.location.components.YesNoDialog
import com.anselm.location.models.AppAction
import com.anselm.location.models.LocalAppViewModel
import com.anselm.location.models.RecordingDetailsViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// For handling scroll within the nested / map view
// https://stackoverflow.com/questions/70836603/how-to-scroll-zoom-a-map-inside-a-vertically-scrolling-in-jetpack-compose

@Composable
private fun DeleteAction(viewModel: RecordingDetailsViewModel) {
    IconButton(
        onClick = { viewModel.showDeleteDialog.value = true }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_trash),
            contentDescription = "Delete",
            modifier = Modifier.size(24.dp),
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RecordingMap(viewModel: RecordingDetailsViewModel) {
    val recordingWrapper by viewModel.recordingState.collectAsState()
    val recording = recordingWrapper.value
    val scope = rememberCoroutineScope()

    val latLng = recording.extractLatLng()
    val boundsBuilder = LatLngBounds.Builder()
    latLng.forEach {
        boundsBuilder.include(it)
    }
    val bounds = boundsBuilder.build()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(bounds.center, 1f)
    }
    Box (
       modifier = Modifier
           .fillMaxWidth()
           .height(300.dp)
           .border(2.dp, Color(0xffcccccc), shape = RoundedCornerShape(10.dp))
           .pointerInteropFilter(
               onTouchEvent = {
                   Log.d("com.anselm.location.Touch", "onToucheEvent: $it")
                   when (it.action) {
                       MotionEvent.ACTION_DOWN -> {
                           viewModel.columnScrollingEnabled.value = false
                           false
                       }

                       else -> {
                           true
                       }
                   }
               })
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapLoaded = {
                val update  = CameraUpdateFactory.newLatLngBounds(bounds, 100)
                scope.launch {
                    withContext(Dispatchers.Main) {
                        cameraPositionState.animate(update, 500)
                    }
                }
            }
        ) {
            Polyline(
                points = recording.extractLatLng(),
                color = Color.Red
            )
        }
    }
}

@Composable
fun RecordingDetailsScreen(recordingId: String?) {
    val appViewModel = LocalAppViewModel.current
    val navController = LocalNavController.current
    val recordingOrNull = recordingId?.let {
        app.recordingManager.load(it)
    }

    if (recordingOrNull == null) {
        // This really shouldn't happen.
        navController.navigate(NavigationItem.ViewRecordings.route)
        return
    }
    val viewModel :  RecordingDetailsViewModel
        = viewModel(factory = RecordingDetailsViewModel.Factory(recordingOrNull))

    val recordingWrapper by viewModel.recordingState.collectAsState()
    val recording = recordingWrapper.value
    val lastSample = recording.lastSample()

    appViewModel.updateApplicationState {
        it.copy(
            title = recording.title,
            actions = listOf(object : AppAction {
                @Composable
                override fun Action() {
                    DeleteAction(viewModel)
                }
            })
        )
    }.setShowOnLockScreen(false)

    val butMapModifier = Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                when (event.type) {
                    PointerEventType.Press -> {
                        viewModel.columnScrollingEnabled.value = true
                    }
                    else -> { }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState(), viewModel.columnScrollingEnabled.value)
    ) {
        if ( viewModel.showDeleteDialog.value ) {
            YesNoDialog(
                title = "Delete Recording",
                text = "Are you sure you want to delete this recording?",
                onDismiss = {
                    viewModel.showDeleteDialog.value = false
                },
                onConfirm = {
                    viewModel.showDeleteDialog.value = false
                    app.recordingManager.delete(recordingId)
                    navController.popBackStack()
                }
            )
        }
        RecordingMetaData(
            viewModel,
            modifier = butMapModifier
        )
        RecordingMap(viewModel)
        TimeElapsedCard(
            recording = recording,
            sample = lastSample,
            modifier = butMapModifier
        )
        SpeedCard(
            recording = recording,
            sample = lastSample,
            modifier = butMapModifier,
        )
        AltitudeCard(
            recording = recording,
            sample = lastSample,
            modifier = butMapModifier,
        )
    }
}