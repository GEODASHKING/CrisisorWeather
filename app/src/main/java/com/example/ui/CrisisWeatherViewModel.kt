package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Alert
import com.example.data.CrisisWeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CrisisWeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CrisisWeatherRepository(application)

    val alertsList: StateFlow<List<Alert>> = repository.alertsList
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentLatitude = repository.currentLatitude
    val currentLongitude = repository.currentLongitude
    val isUsingSimulatedLocation = repository.isUsingSimulatedLocation
    val isFetching = repository.isFetching
    val lastSyncStatus = repository.lastSyncStatus

    // Master-Detail selection state
    var selectedAlert by mutableStateOf<Alert?>(null)
        private set

    // Critical Interruption state
    var activeCriticalAlert by mutableStateOf<Alert?>(null)
        private set

    // Settings / Custom coordinates state
    var editLatitude by mutableStateOf("38.8951")
    var editLongitude by mutableStateOf("-77.0364")

    // Simulator Console Form State
    var simType by mutableStateOf("AMBER") // "AMBER", "WEATHER", "HAZMAT"
    var simTitle by mutableStateOf("")
    var simDescription by mutableStateOf("")
    var simDistanceMiles by mutableStateOf("15.0") // Simulator distance from tablet location
    var simBearingDegrees by mutableStateOf("45.0") // Direction from tablet

    init {
        // Observe alerts to show critical overlay when a new critical alert arrives
        viewModelScope.launch {
            alertsList.collect { alerts ->
                // If there's a new critical alert that hasn't been read/acknowledged, show the interruption screen
                val unreadCritical = alerts.firstOrNull { it.severity == "CRITICAL" && !it.isRead }
                if (unreadCritical != null && activeCriticalAlert?.id != unreadCritical.id) {
                    activeCriticalAlert = unreadCritical
                }
            }
        }
    }

    fun selectAlert(alert: Alert?) {
        selectedAlert = alert
        if (alert != null) {
            repository.markAsRead(alert.id)
        }
    }

    fun dismissCriticalAlert() {
        activeCriticalAlert?.let {
            repository.markAsRead(it.id)
        }
        activeCriticalAlert = null
        repository.easAlarmPlayer.stop()
    }

    fun setSimulatedLocation() {
        val lat = editLatitude.toDoubleOrNull() ?: 38.8951
        val lon = editLongitude.toDoubleOrNull() ?: -77.0364
        repository.setSimulatedLocation(lat, lon)
    }

    fun toggleLocationMode(useRealGps: Boolean) {
        repository.toggleLocationMode(useRealGps)
    }

    fun refreshWeatherAlerts() {
        repository.refreshWeatherAlerts()
    }

    fun triggerSimulation() {
        val titleText = simTitle.ifBlank {
            when (simType) {
                "AMBER" -> "AMBER ALERT: Silver Nissan Pathfinder"
                "WEATHER" -> "TORNADO WARNING"
                "HAZMAT" -> "HAZARDOUS MATERIAL SPILL"
                else -> "CRITICAL ALARM"
            }
        }

        val descText = simDescription.ifBlank {
            when (simType) {
                "AMBER" -> "A child abduction has occurred near the highway. Vehicle license plate: K89-PLX. Last seen heading North."
                "WEATHER" -> "A confirmed, extremely dangerous tornado is moving through the area. Take shelter immediately in an interior room or basement."
                "HAZMAT" -> "An industrial chemical leak has occurred. Residents within specified range must shelter in place, seal windows and doors, and turn off HVAC."
                else -> "Take protective action immediately."
            }
        }

        val distance = simDistanceMiles.toDoubleOrNull() ?: 15.0
        val bearing = simBearingDegrees.toDoubleOrNull() ?: 45.0

        // Calculate offset lat/lon based on distance and bearing from current location
        val startLatRad = Math.toRadians(repository.currentLatitude.value)
        val startLonRad = Math.toRadians(repository.currentLongitude.value)
        val bearingRad = Math.toRadians(bearing)
        
        // Earth radius in miles
        val earthRadiusMiles = 3958.8
        val angularDistance = distance / earthRadiusMiles

        val targetLatRad = Math.asin(
            Math.sin(startLatRad) * Math.cos(angularDistance) +
            Math.cos(startLatRad) * Math.sin(angularDistance) * Math.cos(bearingRad)
        )
        
        val targetLonRad = startLonRad + Math.atan2(
            Math.sin(bearingRad) * Math.sin(angularDistance) * Math.cos(startLatRad),
            Math.cos(angularDistance) - Math.sin(startLatRad) * Math.sin(targetLatRad)
        )

        val alertLat = Math.toDegrees(targetLatRad)
        val alertLon = Math.toDegrees(targetLonRad)

        repository.simulateIncomingAlert(
            type = simType,
            title = titleText,
            description = descText,
            alertLat = alertLat,
            alertLon = alertLon,
            severity = "CRITICAL"
        )
        
        // Reset form simple fields to avoid double click spam
        simTitle = ""
        simDescription = ""
    }

    fun clearAllAlerts() {
        repository.clearAllAlerts()
        selectedAlert = null
        activeCriticalAlert = null
    }

    override fun onCleared() {
        super.onCleared()
        repository.shutdown()
    }
}
