package com.deu3.parking.model

data class Detect2Response(
    val car_number_match: Boolean,
    val within_5m: Boolean,
    val yolo_result_image: String,
)
