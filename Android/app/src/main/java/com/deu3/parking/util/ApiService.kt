package com.deu3.parking.util

import com.deu3.parking.model.Detect1Response
import com.deu3.parking.model.Detect2Response
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @Multipart
    @POST("detect1")
    fun detect1(
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part image: MultipartBody.Part
    ): Call<Detect1Response>

    @Multipart
    @POST("detect2")
    fun detect2(
        @Part("latitude1") latitude1: RequestBody,
        @Part("longitude1") longitude1: RequestBody,
        @Part("latitude2") latitude2: RequestBody,
        @Part("longitude2") longitude2: RequestBody,
        @Part("prev_car_number") prevCarNumber: RequestBody,
        @Part image: MultipartBody.Part
    ): Call<Detect2Response>

    @Multipart
    @POST("report")
    fun report(
        @Part("car_number") carNumber: RequestBody,
        @Part("captured_at") capturedAt: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("violation_type") violationType: RequestBody
    ): Call<Void>
}
