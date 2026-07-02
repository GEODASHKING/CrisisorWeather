package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.example.audio.EasAlarmPlayer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CrisisWeatherRepository(private val context: Context) {
    private val TAG = "CrisisWeatherRepository"
    private val alertDao = AppDatabase.getDatabase(context).alertDao()
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    val easAlarmPlayer = EasAlarmPlayer(context)
    val alertsList = alertDao.getAllAlerts()

    // Location States
    private val _currentLatitude = MutableStateFlow(38.8951) // Default to Washington, DC
    val currentLatitude: StateFlow<Double> = _currentLatitude.asStateFlow()

    private val _currentLongitude = MutableStateFlow(-77.0364)
    val currentLongitude: StateFlow<Double> = _currentLongitude.asStateFlow()

    private val _isUsingSimulatedLocation = MutableStateFlow(true) // Default to true for ease of testing in emulator
    val isUsingSimulatedLocation: StateFlow<Boolean> = _isUsingSimulatedLocation.asStateFlow()

    private val _isFetching = MutableStateFlow(false)
    val isFetching: StateFlow<Boolean> = _isFetching.asStateFlow()

    private val _lastSyncStatus = MutableStateFlow("Ready")
    val lastSyncStatus: StateFlow<String> = _lastSyncStatus.asStateFlow()

    private var locationCallback: LocationCallback? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        // Start monitoring real location if allowed, but keep simulated as a backup
        startLocationMonitoring()
    }

    fun setSimulatedLocation(lat: Double, lon: Double) {
        _currentLatitude.value = lat
        _currentLongitude.value = lon
        _isUsingSimulatedLocation.value = true
        _lastSyncStatus.value = "Updated to simulated coordinate"
        refreshWeatherAlerts()
    }

    fun toggleLocationMode(useRealGps: Boolean) {
        if (useRealGps) {
            _isUsingSimulatedLocation.value = false
            requestSingleFreshLocation()
        } else {
            _isUsingSimulatedLocation.value = true
        }
    }

    @SuppressLint("MissingPermission")
    fun requestSingleFreshLocation() {
        if (isUsingSimulatedLocation.value) return
        
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    _currentLatitude.value = location.latitude
                    _currentLongitude.value = location.longitude
                    _lastSyncStatus.value = "GPS Location Updated"
                    refreshWeatherAlerts()
                } else {
                    _lastSyncStatus.value = "No Last Known Location"
                    // Force updates
                    requestLocationUpdatesOnce()
                }
            }.addOnFailureListener {
                _lastSyncStatus.value = "GPS Fetch Failed: ${it.message}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch GPS coordinates", e)
            _lastSyncStatus.value = "GPS Error: ${e.localizedMessage}"
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdatesOnce() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMaxUpdates(1)
                .build()

            val tempCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation
                    if (location != null) {
                        _currentLatitude.value = location.latitude
                        _currentLongitude.value = location.longitude
                        _lastSyncStatus.value = "GPS Location Updated"
                        refreshWeatherAlerts()
                    }
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, tempCallback, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting single update", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationMonitoring() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 30000)
                .setMinUpdateIntervalMillis(15000)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    if (!_isUsingSimulatedLocation.value) {
                        val location = locationResult.lastLocation
                        if (location != null) {
                            _currentLatitude.value = location.latitude
                            _currentLongitude.value = location.longitude
                            _lastSyncStatus.value = "GPS Location Updated (Periodic)"
                            refreshWeatherAlerts()
                        }
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location monitoring", e)
        }
    }

    fun refreshWeatherAlerts() {
        if (_isFetching.value) return
        
        _isFetching.value = true
        _lastSyncStatus.value = "Synchronizing with NOAA..."
        
        coroutineScope.launch {
            val lat = _currentLatitude.value
            val lon = _currentLongitude.value
            
            val liveAlerts = withContext(Dispatchers.IO) {
                WeatherApiClient.fetchActiveAlerts(lat, lon)
            }
            
            var addedCount = 0
            for (alert in liveAlerts) {
                // Insert into db to persist
                val id = alertDao.insertAlert(alert)
                if (id > 0) addedCount++
                
                // Trigger EAS alert & TTS for critical level live alerts
                if (alert.severity == "CRITICAL") {
                    withContext(Dispatchers.Main) {
                        easAlarmPlayer.playEasAlarmAndSpeak(
                            "Emergency Weather Warning. ${alert.title}. ${alert.description.take(200)}"
                        )
                    }
                }
            }
            
            _isFetching.value = false
            _lastSyncStatus.value = if (liveAlerts.isEmpty()) {
                "Sync Complete: No warnings for this area."
            } else {
                "Sync Complete: Received ${liveAlerts.size} warnings."
            }
        }
    }

    /**
     * Simulates an incoming emergency broadcast (Amber, Weather, or Hazmat).
     * Amber alerts are filtered to a 25-mile radius.
     */
    fun simulateIncomingAlert(
        type: String, // "AMBER", "WEATHER", "HAZMAT"
        title: String,
        description: String,
        alertLat: Double,
        alertLon: Double,
        severity: String = "CRITICAL"
    ) {
        coroutineScope.launch {
            val myLat = _currentLatitude.value
            val myLon = _currentLongitude.value

            // Calculate distance
            val results = FloatArray(1)
            Location.distanceBetween(myLat, myLon, alertLat, alertLon, results)
            val distanceMeters = results[0]
            val distanceMiles = distanceMeters * 0.000621371

            // Apply filter based on alert type
            if (type == "AMBER" && distanceMiles > 25.0) {
                Log.d(TAG, "Amber alert ignored. Distance: $distanceMiles miles (Exceeds 25-mile threshold)")
                _lastSyncStatus.value = "Amber Alert ignored (outside 25-mile radius: %.1f mi)".format(distanceMiles)
                return@launch
            }
            
            // Weather warnings: if it's simulating a local weather warning, it usually checks your exact coordinates
            // (We will allow weather warning simulation to always go through as if in area, but show distance)
            
            val finalAlert = Alert(
                type = type,
                title = title,
                description = description,
                timestamp = System.currentTimeMillis(),
                latitude = alertLat,
                longitude = alertLon,
                distanceMiles = distanceMiles,
                severity = severity,
                isRead = false
            )

            alertDao.insertAlert(finalAlert)
            _lastSyncStatus.value = "New $type Alert triggered (%.1f miles away)".format(distanceMiles)

            // Trigger immediate interruption play
            withContext(Dispatchers.Main) {
                val announceText = buildString {
                    append("The Emergency Alert System has issued a ")
                    when (type) {
                        "AMBER" -> append("Child Abduction Emergency, also known as an Amber Alert. ")
                        "WEATHER" -> append("Critical Weather Warning. ")
                        "HAZMAT" -> append("Hazardous Materials Alert. ")
                        else -> append("Crisis Warning. ")
                    }
                    append(title)
                    append(". ")
                    append(description)
                }
                easAlarmPlayer.playEasAlarmAndSpeak(announceText)
            }
        }
    }

    fun markAsRead(alertId: Int) {
        coroutineScope.launch {
            alertDao.markAsRead(alertId)
        }
    }

    fun deleteAlert(alert: Alert) {
        coroutineScope.launch {
            alertDao.deleteAlert(alert)
        }
    }

    fun clearAllAlerts() {
        coroutineScope.launch {
            alertDao.clearAllAlerts()
        }
    }

    fun shutdown() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        easAlarmPlayer.shutdown()
    }
}
