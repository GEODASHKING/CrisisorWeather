package com.example.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Alert
import com.example.ui.components.EmergencyRadar
import com.example.ui.theme.CardSteel
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkSteel
import com.example.ui.theme.EmergencyAmber
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SignalBlue
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarningYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: CrisisWeatherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val alerts by viewModel.alertsList.collectAsState()
    val latitude by viewModel.currentLatitude.collectAsState()
    val longitude by viewModel.currentLongitude.collectAsState()
    val isSimulatedLocation by viewModel.isUsingSimulatedLocation.collectAsState()
    val isFetching by viewModel.isFetching.collectAsState()
    val syncStatus by viewModel.lastSyncStatus.collectAsState()
    
    val activeOverlayAlert = viewModel.activeCriticalAlert

    Box(modifier = modifier.fillMaxSize().background(ObsidianBlack)) {
        // Main Console Layout (Partitioned Side-by-Side for Network Tablets)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // LEFT PANEL: Dynamic Monitor Feed & Database Records (Weight 1.2f)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1.2f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // GPS BEACON HEADER
                GpsBeaconHeader(
                    latitude = latitude,
                    longitude = longitude,
                    isSimulated = isSimulatedLocation,
                    syncStatus = syncStatus,
                    isFetching = isFetching,
                    onToggleGps = { useGps -> viewModel.toggleLocationMode(!useGps) },
                    onRefresh = { viewModel.refreshWeatherAlerts() },
                    onClearAll = { viewModel.clearAllAlerts() },
                    onSetupDrawOver = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )

                // BEACON QUICK PRESETS
                LocationPresetsRow(
                    onSelectPreset = { lat, lon, name ->
                        viewModel.editLatitude = lat.toString()
                        viewModel.editLongitude = lon.toString()
                        viewModel.setSimulatedLocation()
                    }
                )

                // THE LIVE ALERT FEED
                AlertFeedSection(
                    alerts = alerts,
                    selectedAlert = viewModel.selectedAlert,
                    onSelectAlert = { viewModel.selectAlert(it) },
                    modifier = Modifier.weight(1f)
                )
            }

            // RIGHT PANEL: Diagnostic Operations & EAS Simulation Transmitter (Weight 1.0f)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1.0f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // RADAR SCANNER VIEW (Capped at 25 miles per criteria)
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSteel),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EmergencyRadar(
                            alerts = alerts,
                            myLat = latitude,
                            myLon = longitude,
                            modifier = Modifier
                                .size(220.dp)
                                .weight(1f)
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(
                            modifier = Modifier.weight(0.8f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "BEACON METRICS",
                                color = TextLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Lat: %.5f".format(latitude),
                                color = SignalBlue,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Lon: %.5f".format(longitude),
                                color = SignalBlue,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Active Alerts: ${alerts.size}",
                                color = if (alerts.isNotEmpty()) EmergencyAmber else SafeGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Filters:\n• AMBER < 25 mi\n• Weather: Area\n• Hazmat: Local",
                                color = TextMuted,
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // ALERT DETAIL VIEWER / EAS TRANSMITTER SIMULATOR (Adaptive Tab-like Card)
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSteel),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (viewModel.selectedAlert != null) {
                            AlertDetailsPane(
                                alert = viewModel.selectedAlert!!,
                                onBackToSimulator = { viewModel.selectAlert(null) }
                            )
                        } else {
                            EasTransmitterPane(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }

        // FULL SCREEN CRITICAL EAS BROADCAST INTERRUPTION OVERLAY
        AnimatedVisibility(
            visible = activeOverlayAlert != null,
            modifier = Modifier.fillMaxSize()
        ) {
            activeOverlayAlert?.let { alert ->
                EasInterruptionOverlay(
                    alert = alert,
                    onSilence = { viewModel.dismissCriticalAlert() }
                )
            }
        }
    }
}

@Composable
fun GpsBeaconHeader(
    latitude: Double,
    longitude: Double,
    isSimulated: Boolean,
    syncStatus: String,
    isFetching: Boolean,
    onToggleGps: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onClearAll: () -> Unit,
    onSetupDrawOver: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSteel),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = DangerRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CRISIS OR WEATHER",
                            color = TextLight,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "EMERGENCY OPERATIONS TABLET HUB",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onSetupDrawOver,
                        colors = ButtonDefaults.buttonColors(containerColor = CardSteel),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("admin_permissions_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = WarningYellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Grant Admin Overlay", fontSize = 11.sp, color = TextLight)
                    }

                    IconButton(
                        onClick = onClearAll,
                        modifier = Modifier
                            .background(CardSteel, RoundedCornerShape(6.dp))
                            .testTag("clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Log",
                            tint = DangerRed
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF1E2836))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // GPS coordinates / mode readouts
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isSimulated) WarningYellow else SafeGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSimulated) "SIMULATOR OVERRIDE" else "LIVE GPS BEACON ACTIVE",
                        color = if (isSimulated) WarningYellow else SafeGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Hardware GPS:", color = TextMuted, fontSize = 12.sp)
                    Switch(
                        checked = !isSimulated,
                        onCheckedChange = onToggleGps,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SafeGreen,
                            checkedTrackColor = SafeGreen.copy(alpha = 0.3f),
                            uncheckedThumbColor = WarningYellow,
                            uncheckedTrackColor = WarningYellow.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("gps_toggle")
                    )
                }
            }

            // Sync Status Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F131C), RoundedCornerShape(6.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = SignalBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = syncStatus,
                        color = TextLight,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = onRefresh,
                    enabled = !isFetching,
                    modifier = Modifier.size(24.dp).testTag("noaa_sync_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync NOAA",
                        tint = if (isFetching) TextMuted else SafeGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LocationPresetsRow(
    onSelectPreset: (Double, Double, String) -> Unit
) {
    val presets = listOf(
        Triple(32.7767, -96.7970, "Dallas (Tornado Alert)"),
        Triple(34.0522, -118.2437, "LA (Hazmat Spill)"),
        Triple(25.7617, -80.1918, "Miami (Hurricane)"),
        Triple(47.6062, -122.3321, "Seattle (Amber Alert)")
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { preset ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(CardSteel, RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF222D3D), RoundedCornerShape(6.dp))
                    .clickable { onSelectPreset(preset.first, preset.second, preset.third) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = SignalBlue,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = preset.third.split(" ")[0], // First word
                        color = TextLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun AlertFeedSection(
    alerts: List<Alert>,
    selectedAlert: Alert?,
    onSelectAlert: (Alert) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSteel),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ACTIVE CRITICAL STREAM & HISTORY",
                color = TextLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (alerts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = null,
                            tint = SafeGreen.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "NO CONFLICTS RECORDED",
                            color = SafeGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Listening continuously to emergency broadcast frequencies...",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(alerts) { alert ->
                        val isSelected = selectedAlert?.id == alert.id
                        AlertItemRow(
                            alert = alert,
                            isSelected = isSelected,
                            onSelect = { onSelectAlert(alert) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlertItemRow(
    alert: Alert,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val typeColor = when (alert.type) {
        "AMBER" -> EmergencyAmber
        "WEATHER" -> WarningYellow
        "HAZMAT" -> DangerRed
        else -> SafeGreen
    }

    val typeIcon = when (alert.type) {
        "AMBER" -> Icons.Default.Warning
        "WEATHER" -> Icons.Default.Cloud
        "HAZMAT" -> Icons.Default.Campaign
        else -> Icons.Default.Info
    }

    val cardBorder = if (isSelected) {
        BorderStrokeWrapper(2.dp, typeColor)
    } else {
        BorderStrokeWrapper(1.dp, Color(0xFF222D3D))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) CardSteel else Color(0xFF131822))
            .clickable { onSelect() }
            .border(cardBorder.width, cardBorder.color, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(typeColor.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, typeColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = alert.type,
                        color = typeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatTime(alert.timestamp),
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        if (!alert.isRead) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(DangerRed, CircleShape)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = alert.title,
                    color = TextLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (alert.type == "AMBER" || alert.type == "HAZMAT") {
                        "%.1f miles away • %s".format(alert.distanceMiles ?: 0.0, alert.description.take(50))
                    } else {
                        alert.description.take(60)
                    } + "...",
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun AlertDetailsPane(
    alert: Alert,
    onBackToSimulator: () -> Unit
) {
    val typeColor = when (alert.type) {
        "AMBER" -> EmergencyAmber
        "WEATHER" -> WarningYellow
        "HAZMAT" -> DangerRed
        else -> SafeGreen
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DECODED BROADCAST SPECIFICATION",
                color = typeColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Button(
                onClick = onBackToSimulator,
                colors = ButtonDefaults.buttonColors(containerColor = CardSteel),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValuesWrapper(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text("Close Detail", fontSize = 10.sp, color = TextLight)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
                    border = BorderStrokeWrapper(1.dp, Color(0xFF222D3D)).toBorderStroke(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("EVENT IDENTIFIER", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text(alert.title, color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("SEVERITY", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text(alert.severity, color = typeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("RANGE DISTANCE", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = if (alert.type == "WEATHER") "Local Area" else "%.2f miles".format(alert.distanceMiles ?: 0.0),
                                    color = TextLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("TIMESTAMP", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text(formatTimeFull(alert.timestamp), color = TextLight, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    Text("FULL LOG BROADCAST TEXT", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = alert.description,
                        color = TextLight,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            
            if (alert.latitude != null && alert.longitude != null) {
                item {
                    Column {
                        Text("GEODESIC VECTOR ORIGIN", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Lat: ${alert.latitude} / Lon: ${alert.longitude}",
                            color = SignalBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EasTransmitterPane(
    viewModel: CrisisWeatherViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "EAS TRANSMITTER (SIMULATOR CONSOLE)",
            color = WarningYellow,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        // Select Alert Type
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val types = listOf("AMBER", "WEATHER", "HAZMAT")
            types.forEach { type ->
                val selected = viewModel.simType == type
                val color = when (type) {
                    "AMBER" -> EmergencyAmber
                    "WEATHER" -> WarningYellow
                    else -> DangerRed
                }
                
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selected) CardSteel else Color.Transparent, RoundedCornerShape(6.dp))
                        .border(1.dp, if (selected) color else Color(0xFF222D3D), RoundedCornerShape(6.dp))
                        .clickable { viewModel.simType = type }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = { viewModel.simType = type },
                        colors = RadioButtonDefaults.colors(selectedColor = color, unselectedColor = TextMuted)
                    )
                    Text(
                        text = type,
                        color = if (selected) color else TextLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Title textfield
        OutlinedTextField(
            value = viewModel.simTitle,
            onValueChange = { viewModel.simTitle = it },
            label = { Text("Custom Alert Title (Optional)", fontSize = 11.sp) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextLight, fontSize = 12.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SignalBlue,
                unfocusedBorderColor = Color(0xFF222D3D),
                unfocusedLabelColor = TextMuted,
                focusedLabelColor = SignalBlue
            ),
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("sim_title_input")
        )

        // Description textfield
        OutlinedTextField(
            value = viewModel.simDescription,
            onValueChange = { viewModel.simDescription = it },
            label = { Text("Alert Description / Instruction (Optional)", fontSize = 11.sp) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextLight, fontSize = 12.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SignalBlue,
                unfocusedBorderColor = Color(0xFF222D3D),
                unfocusedLabelColor = TextMuted,
                focusedLabelColor = SignalBlue
            ),
            modifier = Modifier.fillMaxWidth().weight(1f).testTag("sim_desc_input")
        )

        // Distance slider (CRITICAL FOR TESTING 25 MILE LIMIT!)
        Column {
            val dist = viewModel.simDistanceMiles.toDoubleOrNull() ?: 15.0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Broadcast Distance Limit",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "%.1f Miles".format(dist),
                    color = if (viewModel.simType == "AMBER" && dist > 25.0) DangerRed else EmergencyAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            Slider(
                value = dist.toFloat(),
                onValueChange = { viewModel.simDistanceMiles = "%.1f".format(it) },
                valueRange = 1f..50f,
                colors = SliderDefaults.colors(
                    thumbColor = EmergencyAmber,
                    activeTrackColor = EmergencyAmber,
                    inactiveTrackColor = Color(0xFF222D3D)
                ),
                modifier = Modifier.height(28.dp).testTag("sim_distance_slider")
            )
            
            if (viewModel.simType == "AMBER" && dist > 25.0) {
                Text(
                    text = "⚠️ Warning: Distance exceeds 25-mile filter. This AMBER Alert will be filtered out & stored only as ignored record.",
                    color = DangerRed,
                    fontSize = 9.sp,
                    lineHeight = 11.sp
                )
            }
        }

        // Trigger Broadcast
        Button(
            onClick = { viewModel.triggerSimulation() },
            colors = ButtonDefaults.buttonColors(
                containerColor = when (viewModel.simType) {
                    "AMBER" -> EmergencyAmber
                    "WEATHER" -> WarningYellow
                    else -> DangerRed
                }
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("trigger_broadcast_button")
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "TRANSMIT SIMULATED BROADCAST",
                color = Color.Black,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun EasInterruptionOverlay(
    alert: Alert,
    onSilence: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "StripesAnimation")
    
    // Animate stripe offset to simulate a moving warning border
    val stripeOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "StripeOffset"
    )

    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FlashAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Draw diagonal hazard lines background borders
        Canvas(modifier = Modifier.fillMaxSize()) {
            val borderSize = 24.dp.toPx()
            
            // Draw top and bottom hazard stripe bands
            drawRect(
                color = Color(0xFF150A0B),
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(size.width, borderSize)
            )
            
            val numStripes = (size.width / 40f).toInt() + 4
            for (i in -2..numStripes) {
                val xStart = i * 40f + stripeOffset % 40f
                drawLine(
                    color = DangerRed.copy(alpha = 0.6f),
                    start = Offset(xStart, 0f),
                    end = Offset(xStart - 20f, borderSize),
                    strokeWidth = 12.dp.toPx()
                )
                
                drawLine(
                    color = DangerRed.copy(alpha = 0.6f),
                    start = Offset(xStart, size.height - borderSize),
                    end = Offset(xStart - 20f, size.height),
                    strokeWidth = 12.dp.toPx()
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(Color(0xFF150909), RoundedCornerShape(16.dp))
                .border(2.dp, DangerRed, RoundedCornerShape(16.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Campaign,
                contentDescription = null,
                tint = DangerRed.copy(alpha = flashAlpha),
                modifier = Modifier.size(80.dp)
            )

            Text(
                text = "EMERGENCY ALERT SYSTEM\nCRITICAL BROADCAST INTERRUPTION",
                color = DangerRed,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            HorizontalDivider(color = DangerRed, thickness = 2.dp)

            Text(
                text = "ALERT SPECIFICATION: ${alert.type}",
                color = WarningYellow,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = alert.title,
                color = TextLight,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = alert.description,
                color = TextLight.copy(alpha = 0.85f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.height(140.dp)
            )

            if (alert.distanceMiles != null && alert.type != "WEATHER") {
                Text(
                    text = "TARGET IS %.2f MILES FROM TABLET GPS BEACON".format(alert.distanceMiles),
                    color = WarningYellow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSilence,
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(60.dp)
                    .testTag("silence_overlay_button")
            ) {
                Text(
                    text = "ACKNOWLEDGE & SILENCE TRANSMISSION",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// Wrapper classes to handle differences in Compose APIs cleanly
data class BorderStrokeWrapper(val width: androidx.compose.ui.unit.Dp, val color: Color) {
    fun toBorderStroke() = androidx.compose.foundation.BorderStroke(width, color)
}

@Composable
fun PaddingValuesWrapper(horizontal: androidx.compose.ui.unit.Dp, vertical: androidx.compose.ui.unit.Dp) =
    androidx.compose.foundation.layout.PaddingValues(horizontal, vertical)

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatTimeFull(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
