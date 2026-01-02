package com.anselm.location.data

import android.util.Log
import com.anselm.location.BuildConfig
import com.anselm.location.LocationApplication.Companion.app
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.io.IOException

private const val TAG = "com.anselm.location.Tagger"

class RecordingTagger(
    private val recording: Recording,
) {
    private var boundsChecker = BoundsChecker()
    private val localities = mutableListOf<String>()

    fun tag(onDone: () -> Unit) {
        val positions = recording.extractLatLng()
        app.launch {
            try {
                positions.forEach { latlng ->
                    tag(latlng)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Tagger API failed.", e)
                app.toast("Geocoding API failed, try again later.")
            } finally {
                onDone()
            }
        }
    }

    private fun tag(latlng: LatLng) {
        if (boundsChecker.isWithinBounds(latlng)) {
            return
        }

        val url = "https://maps.googleapis.com/maps/api/geocode/json?" +
                "latlng=${latlng.latitude},${latlng.longitude}" +
                "&result_type=locality" +
                "&key=${BuildConfig.MAPS_API_KEY}"

        val request = Request.Builder()
            .url(url)
            .tag(recording.id)
            .build()

        Log.d(TAG, url)

        app.okHttpClient.newCall(request).execute().use { response ->
            if ( ! response.isSuccessful)  {
                throw IOException("Call to $url failed (status ${response.code})")
            }
            val json = response.body.string().let {
                Json.parseToJsonElement(it)
            }
            val status = json.jsonObject["status"]?.jsonPrimitive?.content
            if (status != "OK") {
                throw IllegalStateException("Call to $url failed (status $status)")
            }
            val result = json.jsonObject["results"]
                ?.jsonArray
                ?.get(0)
            val locality = extractLocality(result)
            boundsChecker.updateBounds(extractBounds(result))

            locality?.let {
                localities.add(it)
                    recording.addTag(it)
            }
        }
    }

    private fun extractLocality(result: JsonElement?): String? {
        return result?.jsonObject
            ?.get("address_components")
            ?.jsonArray
            ?.get(0)
            ?.jsonObject
            ?.get("long_name")
            ?.jsonPrimitive
            ?.content
    }

    private fun extractBounds(result: JsonElement?): JsonObject? {
        return result?.jsonObject
            ?.get("geometry")
            ?.jsonObject
            ?.get("bounds")
            ?.jsonObject
    }
}

private class BoundsChecker {
    private var bounds: LatLngBounds? = null
    fun isWithinBounds(latlng: LatLng): Boolean {
        return if ( bounds == null ) {
            false
        } else {
            bounds!!.contains(latlng)
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
