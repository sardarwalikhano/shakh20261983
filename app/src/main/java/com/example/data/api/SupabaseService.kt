package com.example.data.api

import com.example.data.model.FoodItem
import com.example.data.model.Order
import com.example.data.model.Restaurant
import com.example.data.model.Story
import retrofit2.http.*

interface SupabaseService {

    @GET("rest/v1/restaurants")
    suspend fun getRestaurants(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("select") select: String = "*"
    ): List<Restaurant>

    @GET("rest/v1/food_items")
    suspend fun getFoodItems(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("restaurant_id") restaurantIdQuery: String, // Format: eq.xyz-id
        @Query("select") select: String = "*"
    ): List<FoodItem>

    @POST("rest/v1/orders")
    @Headers("Content-Type: application/json", "Prefer: return=representation")
    suspend fun createOrder(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body order: Order
    ): List<Order>

    @GET("rest/v1/orders")
    suspend fun getOrders(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idQuery: String, // format: "eq.order-id"
        @Query("select") select: String = "*"
    ): List<Order>

    @PATCH("rest/v1/orders")
    @Headers("Content-Type: application/json", "Prefer: return=representation")
    suspend fun updateOrderStatus(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idQuery: String, // format: "eq.order-id"
        @Body updates: Map<String, String>
    ): List<Order>

    @GET("rest/v1/stories")
    suspend fun getStories(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("select") select: String = "*"
    ): List<Story>

    @POST("rest/v1/stories")
    @Headers("Content-Type: application/json", "Prefer: return=representation")
    suspend fun createStory(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body story: Story
    ): List<Story>
}
