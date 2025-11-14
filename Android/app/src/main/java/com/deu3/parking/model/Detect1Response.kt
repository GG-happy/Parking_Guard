package com.deu3.parking.model

data class Detect1Response(
    val car_number: String,
    val violation_type: List<Int>,
    val yolo_result_image: String,
    val is_violation: Boolean,
)
