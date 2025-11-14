package com.deu3.parking.util

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.deu3.parking.composable.CameraScreen
import com.deu3.parking.composable.MainScreen
import com.deu3.parking.composable.ReportScreen
import com.deu3.parking.composable.WaitScreen
//import com.deu3.parking.model.CameraResult
import com.deu3.parking.model.Detect1Response
import com.deu3.parking.model.Detect2Response
import com.deu3.parking.composable.submitReport

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext


@Composable
fun AppNavigator() {
    val context = LocalContext.current
    val navController = rememberNavController()
    // 전체 Flow 상태 관리 (ViewModel로 대체 가능)
    var firstImagePath by remember { mutableStateOf<String?>(null) }
    var firstLatLng by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var detect1Result by remember { mutableStateOf<Detect1Response?>(null) }
    var secondImagePath by remember { mutableStateOf<String?>(null) }
    var secondLatLng by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var detect2Result by remember { mutableStateOf<Detect2Response?>(null) }
    var countdown by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var yoloImages by remember { mutableStateOf<Pair<String?, String?>?>(null) }
    var showAlreadyReportedDialog by remember { mutableStateOf(false) }

    NavHost(navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                onStartCamera = {
                    // 1차 촬영 진입
                    navController.navigate("camera/first")
                }
            )
        }
        composable(
            "camera/{type}",
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: "first"

            CameraScreen(
                isSecond = type == "second",
                onPictureTaken = { imagePath, lat, lng ->
                    if (type == "first") {
                        firstImagePath = imagePath
                        firstLatLng = lat to lng
                        // 서버로 detect1 요청
                        sendDetect1(imagePath, lat, lng) { result, error ->
                            if (result != null) {
                                detect1Result = result

                                if (result.is_violation) {
                                    showAlreadyReportedDialog = true
                                    return@sendDetect1
                                }

                                // 조건 판정
                                if (result.car_number == null){
                                    errorMessage = "자동차가 잘 보이게 다시 찍어주세요"
                                    navController.navigate("report")
                                }
                                else if(
                                    result.violation_type.firstOrNull() == -1 ||
                                    result.violation_type.firstOrNull() == 0) {
                                    errorMessage = "정상입니다"
                                    // 바로 홈으로 이동
                                    navController.navigate("report")
                                } else {
                                    // 카운트다운 5분(300초) 시작
                                    countdown = 10   //test때는 10초로 설정
                                    navController.navigate("wait_second")
                                }
                            } else {
                                errorMessage = error ?: "서버오류"
                                navController.navigate("report")
                            }
                        }
                    } else {
                        secondImagePath = imagePath
                        secondLatLng = lat to lng
                        // 서버 detect2 요청
                        sendDetect2(
                            firstLatLng?.first ?: 0.0,
                            firstLatLng?.second ?: 0.0,
                            lat, lng,
                            detect1Result?.car_number ?: "",
                            imagePath
                        ) { result, error ->
                            if (result != null) {
                                detect2Result = result
                                yoloImages = Pair(
                                    detect1Result?.yolo_result_image,
                                    result.yolo_result_image
                                )
                                navController.navigate("report")
                            } else {
                                errorMessage = error ?: "서버 오류"
                                navController.navigate("report")
                            }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("wait_second") {
            // 5분 카운트다운 화면, 끝나면 2차 촬영 이동
            WaitScreen(
                countdown = countdown,
                onTick = { countdown = countdown - 1 },
                onFinish = {
                    countdown = 0
                    errorMessage = null
                    navController.navigate("camera/second")
                }
            )
        }
        composable("report") {
            ReportScreen(
                firstImagePath = firstImagePath,
                secondImagePath = secondImagePath,
                violationType = detect1Result?.violation_type,
                carNumber = detect1Result?.car_number,
                yoloImages = yoloImages,
                detect2Result = detect2Result,
                errorMessage = errorMessage,
                onSubmit = { selectedViolationType ->
                    submitReport(
                        carNumber = detect1Result?.car_number ?: return@ReportScreen,
                        latitude = firstLatLng?.first ?: return@ReportScreen,
                        longitude = firstLatLng?.second ?: return@ReportScreen,
                        violationType = selectedViolationType,
                        onSuccess = {
                            navController.navigate("main") {
                                popUpTo("main") { inclusive = true }
                            }
                        },
                        onError = {
                            errorMessage = it
                            navController.navigate("report")
                        }
                    )
                },
                onHome = {
                    navController.navigate("main") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
    }
    if (showAlreadyReportedDialog) {
        AlertDialog(
            onDismissRequest = { showAlreadyReportedDialog = false },
            title = { Text("알림") },
            text = { Text("이미 신고된 민원입니다") },
            confirmButton = {
                TextButton(onClick = {
                    showAlreadyReportedDialog = false
                    navController.navigate("main") {
                        popUpTo("main") { inclusive = true }
                    }
                }) {
                    Text("홈")
                }
            }
        )
    }
}
