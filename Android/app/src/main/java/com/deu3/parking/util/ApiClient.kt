package com.deu3.parking.util

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object ApiClient {
    private var okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.MINUTES)
        .readTimeout(20, TimeUnit.MINUTES)
        .writeTimeout(20, TimeUnit.MINUTES)
        .build()
    val retrofit = Retrofit.Builder()
        .baseUrl("http://220.68.8.69:8000/api/parking-guard/") // 서버 주소
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: ApiService = retrofit.create(ApiService::class.java)
}
