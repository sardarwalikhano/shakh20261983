package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Restaurant(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "name_ku") val nameKu: String? = null,
    @Json(name = "cuisine") val cuisine: String,
    @Json(name = "cuisine_ku") val cuisineKu: String? = null,
    @Json(name = "image_url") val imageUrl: String,
    @Json(name = "rating") val rating: Double = 4.5,
    @Json(name = "delivery_time") val deliveryTime: String = "25-35 min",
    @Json(name = "delivery_fee") val deliveryFee: Double = 1.5,
    @Json(name = "featured") val featured: Boolean = false
) {
    fun localizedName(isKurdish: Boolean): String {
        return if (isKurdish && !nameKu.isNullOrEmpty()) nameKu else name
    }

    fun localizedCuisine(isKurdish: Boolean): String {
        return if (isKurdish && !cuisineKu.isNullOrEmpty()) cuisineKu else cuisine
    }
}
