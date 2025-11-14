package com.deu3.parking.composable

//import android.Manifest
//import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.deu3.parking.util.Guide
import java.io.File

@Composable
fun CameraScreen(
    isSecond: Boolean,
    onPictureTaken: (String, Double, Double) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val guideList = listOf(
        Guide("소화전과 차량이 함께 나오도록 촬영해 주세요", "소화전"),
        Guide("횡단보도 위에 차량이 있나요?", "횡단보도"),
        Guide("인도(보도)에 주차되어 있나요?", "인도(보도)"),
        Guide("어린이보호구역 주정차 확인", "어린이보호구역"),
        Guide("교차로 모퉁이 확인", "교차로 모퉁이"),
        Guide("버스정류장 주정차 확인", "버스정류장")
    )

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var showGuide by remember { mutableStateOf(!isSecond) }
    var guideIndex by remember { mutableIntStateOf(0) }
    var flashEnabled by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }
                    imageCapture = ImageCapture.Builder()
                        .setFlashMode(if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                        .build()
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                    } catch (e: Exception) {
                        Log.e("CameraX", "카메라 바인딩 실패", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        if (showGuide) {
            Card(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(32.dp)
                    .background(Color(0xAAFFFFFF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(guideList[guideIndex].title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(guideList[guideIndex].keyword, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row {
                        IconButton(onClick = { guideIndex = (guideIndex - 1).coerceAtLeast(0) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev")
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { guideIndex = (guideIndex + 1).coerceAtMost(guideList.lastIndex) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showGuide = false }) { Text("가이드 닫기") }
                }
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { onBack() }) {
                    Icon(Icons.Default.Close, contentDescription = "뒤로가기")
                }
                IconButton(onClick = { flashEnabled = !flashEnabled }) {
                    Icon(
                        if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "플래시"
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (capturing) return@Button
                    capturing = true

                    // 위치 가져오기
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        val lat = location?.latitude ?: 0.0
                        val lng = location?.longitude ?: 0.0

                        val file = File(context.externalCacheDir, "${System.currentTimeMillis()}.jpg")
                        imageCapture?.let { capture ->
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                            capture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        capturing = false
                                        onPictureTaken(file.absolutePath, lat, lng)
                                    }
                                    override fun onError(exception: ImageCaptureException) {
                                        capturing = false
                                        Toast.makeText(context, "사진 저장 실패", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                },
                enabled = !showGuide && !capturing,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp)
            ) {
                Text("촬영", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
