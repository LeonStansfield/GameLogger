package com.example.gamelogger.ui.features.loggame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState

import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

@Composable
fun LocationSelectionDialog(
    initialLocation: LatLng? = null,
    onLocationSelected: (Double, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    // Default to London if no location provided, just as a starting point
    val defaultLocation = LatLng(51.5074, -0.1278)
    val startPos = initialLocation ?: defaultLocation

    var selectedLocation by remember { mutableStateOf(initialLocation) }
    var locationName by remember { mutableStateOf("") }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startPos, 10f)
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val geocoder = remember { android.location.Geocoder(context, java.util.Locale.getDefault()) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = { }, 
            bottomBar = {
                Box(
                    modifier = Modifier
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = locationName,
                            onValueChange = { locationName = it },
                            label = { Text("Location Name (e.g. Home)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                selectedLocation?.let {
                                    onLocationSelected(it.latitude, it.longitude, locationName.ifBlank { "Unknown Location" })
                                }
                            },
                            enabled = selectedLocation != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Confirm Location")
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng ->
                        selectedLocation = latLng
                        // Attempt to reverse geocode
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                                        if (addresses.isNotEmpty()) {
                                            val address = addresses[0]
                                            // Prioritize broader location names (Town/City) over specific streets/numbers
                                            val name = address.locality 
                                                ?: address.subAdminArea 
                                                ?: address.adminArea 
                                                ?: address.thoroughfare 
                                                ?: ""
                                            if (name.isNotEmpty()) {
                                                locationName = name
                                            }
                                        }
                                    }
                                } else {
                                    @Suppress("DEPRECATION")
                                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                                    if (!addresses.isNullOrEmpty()) {
                                        val address = addresses[0]
                                        // Prioritize broader location names (Town/City) over specific streets/numbers
                                        val name = address.locality 
                                            ?: address.subAdminArea 
                                            ?: address.adminArea 
                                            ?: address.thoroughfare 
                                            ?: ""
                                        if (name.isNotEmpty()) {
                                            locationName = name
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                ) {
                    selectedLocation?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "Selected Location"
                        )
                    }
                }
            }
        }
    }
}
