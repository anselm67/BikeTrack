package com.anselm.location.data

import android.util.Log
import com.anselm.location.BuildConfig
import com.anselm.location.LocationApplication.Companion.app
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

private const val TAG = "com.anselm.location.Tagger"

class BoundsChecker {
    var bounds: LatLngBounds? = null
    fun isWithinBounds(latlng: LatLng): Boolean {
        if ( bounds == null ) {
            return false
        } else {
            return bounds!!.contains(latlng)
        }
    }

    private fun asLatLng(obj: JsonObject?): LatLng? {
        val lat = obj?.get("lat")?.jsonPrimitive?.content?.toDouble()
        val lng = obj?.get("lng")?.jsonPrimitive?.content?.toDouble()
        return if ( lat != null && lng != null )
            LatLng(lat, lng)
        else
            null
    }

    fun updateBounds(geometry: JsonObject?) {
        val northEast = asLatLng(geometry?.get("northeast")?.jsonObject) ?: return
        val southWest = asLatLng(geometry?.get("southwest")?.jsonObject) ?: return
        bounds = LatLngBounds(southWest, northEast)
    }
}

private val boundsChecker = BoundsChecker()

fun tagPosition(latlngString: String) {
    val url = "https://maps.googleapis.com/maps/api/geocode/json?" +
            "latlng=$latlngString" +
            "&result_type=locality" +
            "&key=${BuildConfig.MAPS_API_KEY}"

    val request = Request.Builder()
        .url(url)
        .build()

    Log.d(TAG, url)

    app.okHttpClient.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e(TAG, "Call failed", e)
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                val json = response.body?.string()?.let {
                    Json.parseToJsonElement(it)
                }
                val result = json?.jsonObject?.get("results")
                    ?.jsonArray
                    ?.get(0)
                val locality = result
                    ?.jsonObject
                    ?.get("address_components")
                    ?.jsonArray
                    ?.get(0)
                    ?.jsonObject
                    ?.get("long_name")
                    ?.jsonPrimitive
                    ?.content
                val bounds = result
                    ?.jsonObject
                    ?.get("geometry")
                    ?.jsonObject
                    ?.get("bounds")
                boundsChecker.updateBounds(bounds?.jsonObject)

                Log.d(TAG, "Locality: $locality, bounds $bounds")
            }
        }

    })
}

fun tagRecording(recording: Recording) {
    app.launch {
        recording.extractLatLng().forEach {
            if ( ! boundsChecker.isWithinBounds(it)) {
                tagPosition("${it.latitude},${it.longitude}")
                delay(1000)
            }
        }
    }




}