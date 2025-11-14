package com.deu3.parking.util

import com.deu3.parking.model.Detect1Response
import com.deu3.parking.model.Detect2Response
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

fun sendDetect1(
    imagePath: String, lat: Double, lng: Double,
    callback: (Detect1Response?, String?) -> Unit
) {
    val imageFile = File(imagePath)
    val reqFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), imageFile)
    val imageBody = MultipartBody.Part.createFormData("image", imageFile.name, reqFile)
    val latBody = RequestBody.create("text/plain".toMediaTypeOrNull(), lat.toString())
    val lngBody = RequestBody.create("text/plain".toMediaTypeOrNull(), lng.toString())

    ApiClient.service.detect1(latBody, lngBody, imageBody).enqueue(object : retrofit2.Callback<Detect1Response> {
        override fun onResponse(call: retrofit2.Call<Detect1Response>, response: retrofit2.Response<Detect1Response>) {
            if (response.isSuccessful) {
                callback(response.body(), null)
            } else {
                callback(null, "서버 오류")
            }
        }
        override fun onFailure(call: retrofit2.Call<Detect1Response>, t: Throwable) {
            callback(null, t.message)
        }
    })
}

fun sendDetect2(
    lat1: Double, lng1: Double, lat2: Double, lng2: Double,
    carNumber: String, imagePath: String,
    callback: (Detect2Response?, String?) -> Unit
) {
    val imageFile = File(imagePath)
    val reqFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), imageFile)
    val imageBody = MultipartBody.Part.createFormData("image", imageFile.name, reqFile)
    val lat1Body = RequestBody.create("text/plain".toMediaTypeOrNull(), lat1.toString())
    val lng1Body = RequestBody.create("text/plain".toMediaTypeOrNull(), lng1.toString())
    val lat2Body = RequestBody.create("text/plain".toMediaTypeOrNull(), lat2.toString())
    val lng2Body = RequestBody.create("text/plain".toMediaTypeOrNull(), lng2.toString())
    val carNumBody = RequestBody.create("text/plain".toMediaTypeOrNull(), carNumber)

    ApiClient.service.detect2(lat1Body, lng1Body, lat2Body, lng2Body, carNumBody, imageBody)
        .enqueue(object : retrofit2.Callback<Detect2Response> {
            override fun onResponse(call: retrofit2.Call<Detect2Response>, response: retrofit2.Response<Detect2Response>) {
                if (response.isSuccessful) {
                    callback(response.body(), null)
                } else {
                    callback(null, "서버 오류")
                }
            }
            override fun onFailure(call: retrofit2.Call<Detect2Response>, t: Throwable) {
                callback(null, t.message)
            }
        })
}