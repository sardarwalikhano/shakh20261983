package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Story(
    @Json(name = "id") val id: String,
    @Json(name = "restaurant_id") val restaurantId: String,
    @Json(name = "restaurant_name") val restaurantName: String,
    @Json(name = "restaurant_name_ku") val restaurantNameKu: String? = null,
    @Json(name = "food_name") val foodName: String,
    @Json(name = "food_name_ku") val foodNameKu: String? = null,
    @Json(name = "description") val description: String,
    @Json(name = "description_ku") val descriptionKu: String? = null,
    @Json(name = "price") val price: Double,
    @Json(name = "image_url") val imageUrl: String
) {
    fun localizedRestaurantName(isKurdish: Boolean): String {
        return if (isKurdish && !restaurantNameKu.isNullOrEmpty()) restaurantNameKu else restaurantName
    }

    fun localizedFoodName(isKurdish: Boolean): String {
        return if (isKurdish && !foodNameKu.isNullOrEmpty()) foodNameKu else foodName
    }

    fun localizedDescription(isKurdish: Boolean): String {
        return if (isKurdish && !descriptionKu.isNullOrEmpty()) descriptionKu else description
    }
}
