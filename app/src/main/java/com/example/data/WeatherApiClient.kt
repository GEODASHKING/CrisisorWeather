package com.example.data

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object WeatherApiClient {
    private const val TAG = "WeatherApiClient"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Fetches active weather alerts for a given GPS coordinate from weather.gov NWS API.
     */
    fun fetchActiveAlerts(latitude: Double, longitude: Double): List<Alert> {
        val url = "https://api.weather.gov/alerts/active?point=$latitude,$longitude"
        Log.d(TAG, "Fetching NOAA weather alerts from: $url")
        
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "CrisisorWeatherTabletApp/1.0 (micahevanmccollum@gmail.com)")
            .header("Accept", "application/ld+json") // Request LD+JSON / GeoJSON structure
            .build()

        val alerts = mutableListOf<Alert>()
        
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch weather alerts. Code: ${response.code}")
                    return emptyList()
                }
                
                val responseBody = response.body?.string() ?: return emptyList()
                val jsonObject = JSONObject(responseBody)
                
                // Parse features
                if (jsonObject.has("features")) {
                    val features = jsonObject.getJSONArray("features")
                    for (i in 0 until features.length()) {
                        try {
                            val feature = features.getJSONObject(i)
                            val properties = feature.getJSONObject("properties")
                            
                            val id = properties.optString("id", "")
                            val event = properties.optString("event", "Weather Alert")
                            val headline = properties.optString("headline", "")
                            val description = properties.optString("description", "")
                            val instruction = properties.optString("instruction", "")
                            val severity = properties.optString("severity", "Moderate") // "Extreme", "Severe", etc.
                            val areaDesc = properties.optString("areaDesc", "")
                            
                            // Map NOAA severity to our app levels ("CRITICAL", "WARNING", "INFO")
                            val mappedSeverity = when (severity.uppercase()) {
                                "EXTREME", "SEVERE" -> "CRITICAL"
                                "MODERATE" -> "WARNING"
                                else -> "INFO"
                            }
                            
                            val fullDescription = buildString {
                                append(headline)
                                if (areaDesc.isNotEmpty()) {
                                    append("\n\nAffected Areas: ").append(areaDesc)
                                }
                                if (description.isNotEmpty()) {
                                    append("\n\nDetails:\n").append(description)
                                }
                                if (instruction.isNotEmpty()) {
                                    append("\n\nSafety Instructions:\n").append(instruction)
                                }
                            }
                            
                            alerts.add(
                                Alert(
                                    type = "WEATHER",
                                    title = event,
                                    description = fullDescription,
                                    timestamp = System.currentTimeMillis(),
                                    latitude = latitude,
                                    longitude = longitude,
                                    distanceMiles = 0.0, // Weather warnings check exact area
                                    severity = mappedSeverity
                                )
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing individual alert item at index $i", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error fetching NOAA alerts", e)
        }
        
        return alerts
    }
}
