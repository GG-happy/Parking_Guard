package com.deu3.parking

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.deu3.parking.ui.theme.ParkingGuardTheme
import com.deu3.parking.util.AppNavigator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ParkingGuardTheme {
                var hasCameraPermission by remember { mutableStateOf<Boolean?>(null) }
                var hasLocationPermission by remember { mutableStateOf<Boolean?>(null) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
                    hasLocationPermission =
                        permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

                    if (hasCameraPermission == false || hasLocationPermission == false) {
                        finish()
                    }
                }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }

                if (hasCameraPermission == null || hasLocationPermission == null) {
                    Text("권한 확인 중...")
                } else {
                    AppNavigator()
                }
            }
        }
    }
}
