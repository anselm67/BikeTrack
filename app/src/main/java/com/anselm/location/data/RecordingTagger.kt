package com.anselm.location.data

import android.util.Log
import com.anselm.location.BuildConfig
import com.anselm.location.LocationApplication.Companion.app
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

private const val TAG = "com.anselm.location.Tagger"

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
                Log.d(TAG, "Json: $json")
                val locality = json?.jsonObject?.get("results")
                    ?.jsonArray
                    ?.get(0)
                    ?.jsonObject
                    ?.get("address_components")
                    ?.jsonArray
                    ?.get(0)
                    ?.jsonObject
                    ?.get("long_name")
                    ?.jsonPrimitive
                    ?.content

                Log.d(TAG, "Json: \nLocality: $locality")
            }
        }

    })
}

fun tagRecording(recording: Recording) {
    app.launch {
        val positions = recording.extractLatLng().mapIndexed { index, it ->
            if (index % 2 == 0) "${it.latitude},${it.longitude}" else null
        }.filterNotNull()

        positions.forEach {
            tagPosition(it)
            delay(1000)
        }
    }




}