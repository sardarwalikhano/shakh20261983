package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Order(
    @Json(name = "id") val id: String,
    @Json(name = "customer_name") val customerName: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "address") val address: String,
    @Json(name = "total_price") val totalPrice: Double,
    @Json(name = "status") val status: String, // "Pending", "Preparing", "On the way", "Delivered"
    @Json(name = "items_summary") val itemsSummary: String, // Short summary of ordered items
    @Json(name = "created_at") val createdAt: String? = null
)
