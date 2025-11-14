package com.deu3.parking.composable

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.deu3.parking.model.Detect2Response
import android.util.Base64
import androidx.compose.runtime.remember

import com.deu3.parking.util.ApiClient
import com.deu3.parking.util.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun ReportScreen(
    firstImagePath: String?,
    secondImagePath: String?,
    violationType: List<Int>?,
    carNumber: String?,
    yoloImages: Pair<String?, String?>?,
    detect2Result: Detect2Response?,
    errorMessage: String?,
    onSubmit: (Int) -> Unit,
    onHome: () -> Unit
) {
    val selectedType = remember { mutableStateOf<Int?>(null) }
    val multipleTypes = (violationType?.size ?: 0) > 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onHome) { Text("홈으로") }
            return
        }

        Text("신고 결과", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))

        carNumber?.let {
            Text("차량번호: $it", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(Modifier.height(12.dp))

        violationType?.let {
            if (multipleTypes) {
                Text("불법 주차 유형을 선택하세요:", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    it.forEach { type ->
                        Button(
                            onClick = { selectedType.value = type },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedType.value == type)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            if (type == 5){
                                Text("교차로 모퉁이")}
                            else{
                                Text("일반")
                            }
                        }
                    }
                }
            } else {
                val onlyType = it.firstOrNull()
                Text("불법주차 유형: $onlyType", style = MaterialTheme.typography.bodyLarge)
                selectedType.value = onlyType // 자동 선택
            }
        }

        Spacer(Modifier.height(12.dp))

        if (detect2Result != null) {
            if (detect2Result.car_number_match == false) {
                Text("차량 번호 미일치", color = MaterialTheme.colorScheme.error)
            }
            if (detect2Result.within_5m == false) {
                Text("첫 번째 사진과 5미터 이상 차이남", color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PhotoSlot(imageBase64 = yoloImages?.first ?: firstImagePath, label = "1차 사진")
            PhotoSlot(imageBase64 = yoloImages?.second ?: secondImagePath, label = "2차 사진")
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                selectedType.value?.let { onSubmit(it) }
            },
            enabled = (detect2Result?.car_number_match == true
                    && detect2Result.within_5m == true
                    && selectedType.value != null),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("신고 접수", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun PhotoSlot(
    imageBase64: String?,
    label: String,
) {
    Card(
        modifier = Modifier.size(140.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (imageBase64 == null) {
                Text("이미지 없음")
            } else {
                val bitmap = remember(imageBase64) {
                    try {
                        val pureBase64 = imageBase64.substringAfter("base64,", imageBase64)
                        val imageBytes = Base64.decode(pureBase64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)?.asImageBitmap()
                    } catch (e: Exception) {null}
                }
                if (bitmap != null) {
                    Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Text("이미지 디코딩 실패", color = MaterialTheme.colorScheme.error)
                }
            }
            Text(label, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
        }
    }
}

fun submitReport(
    carNumber: String,
    latitude: Double,
    longitude: Double,
    violationType: Int,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val api = ApiClient.retrofit.create(ApiService::class.java)

    val capturedAtBody = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .toString()
        .toRequestBody("text/plain".toMediaTypeOrNull())

    val carNumberBody = carNumber.toRequestBody("text/plain".toMediaTypeOrNull())
    val latitudeBody = latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
    val longitudeBody = longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
    val violationTypeBody = violationType.toString().toRequestBody("text/plain".toMediaTypeOrNull())

    api.report(
        carNumber = carNumberBody,
        capturedAt = capturedAtBody,
        latitude = latitudeBody,
        longitude = longitudeBody,
        violationType = violationTypeBody
    ).enqueue(object : Callback<Void> {
        override fun onResponse(call: Call<Void>, response: Response<Void>) {
            if (response.isSuccessful) {
                onSuccess()
            } else {
                onError("서버 오류: ${response.code()}")
            }
        }
        override fun onFailure(call: Call<Void>, t: Throwable) {
            onError("네트워크 오류: ${t.message}")
        }
    })
}
