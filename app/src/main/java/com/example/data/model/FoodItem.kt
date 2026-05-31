package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FoodItem(
    @Json(name = "id") val id: String,
    @Json(name = "restaurant_id") val restaurantId: String,
    @Json(name = "name") val name: String,
    @Json(name = "name_ku") val nameKu: String? = null,
    @Json(name = "description") val description: String,
    @Json(name = "description_ku") val descriptionKu: String? = null,
    @Json(name = "price") val price: Double,
    @Json(name = "image_url") val imageUrl: String,
    @Json(name = "category") val category: String,
    @Json(name = "category_ku") val categoryKu: String? = null
) {
    fun localizedName(isKurdish: Boolean): String {
        return if (isKurdish && !nameKu.isNullOrEmpty()) nameKu else name
    }

    fun localizedDescription(isKurdish: Boolean): String {
        return if (isKurdish && !descriptionKu.isNullOrEmpty()) descriptionKu else description
    }

    fun localizedCategory(isKurdish: Boolean): String {
        return if (isKurdish && !categoryKu.isNullOrEmpty()) categoryKu else category
    }
}
