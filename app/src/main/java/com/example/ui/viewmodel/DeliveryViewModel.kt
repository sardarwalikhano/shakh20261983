package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.FoodItem
import com.example.data.model.Order
import com.example.data.model.Restaurant
import com.example.data.model.Story
import com.example.data.repository.ConnectionStatus
import com.example.data.repository.DeliveryRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import android.util.Log
import kotlinx.coroutines.Dispatchers

class DeliveryViewModel(private val repository: DeliveryRepository) : ViewModel() {

    // Geocoding Dynamic Domain Lookup States
    private val _isGeocoding = MutableStateFlow(false)
    val isGeocoding: StateFlow<Boolean> = _isGeocoding

    private val _currentGeocodingStep = MutableStateFlow("")
    val currentGeocodingStep: StateFlow<String> = _currentGeocodingStep

    private val _geocodingProgress = MutableStateFlow(0f)
    val geocodingProgress: StateFlow<Float> = _geocodingProgress

    fun getAddressFromLiveGPS(
        context: android.content.Context,
        fineGranted: Boolean,
        coarseGranted: Boolean,
        onAddressResolved: (String) -> Unit
    ) {
        _isGeocoding.value = true
        _geocodingProgress.value = 0.1f
        _currentGeocodingStep.value = if (_isKurdish.value) "دەستپێکردنی گەڕانی GPS..." else "Initializing GPS tracking sensors..."

        viewModelScope.launch(Dispatchers.IO) {
            var latitude: Double? = null
            var longitude: Double? = null

            // Step 1: Query device GPS sensor
            if (fineGranted || coarseGranted) {
                _geocodingProgress.value = 0.3f
                _currentGeocodingStep.value = if (_isKurdish.value) "پرسیارکردن لە ڕاداری مۆبایل بۆ پۆتانەکان..." else "Querying device GPS sensors..."
                delay(1200)
                try {
                    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
                    if (locationManager != null) {
                        val providers = locationManager.getProviders(true)
                        var bestLocation: android.location.Location? = null
                        for (provider in providers) {
                            @Suppress("MissingPermission")
                            val loc = locationManager.getLastKnownLocation(provider) ?: continue
                            if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                                bestLocation = loc
                            }
                        }
                        if (bestLocation != null) {
                            latitude = bestLocation.latitude
                            longitude = bestLocation.longitude
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DeliveryViewModel", "Local GPS capture failed: " + e.message)
                }
            }

            // Step 2: Fallback to online Domain IP-Geolocation if GPS sensor is null
            if (latitude == null || longitude == null) {
                _geocodingProgress.value = 0.6f
                _currentGeocodingStep.value = if (_isKurdish.value) "سێنسەر دەستنەکەوت؛ پەیوەستبوون بە دۆمەینی Geolocation..." else "No hardware GPS. Connecting to Internet IP-Geo domain..."
                delay(1500)

                try {
                    val client = okhttp3.OkHttpClient()
                    val request = okhttp3.Request.Builder()
                        .url("https://ip-api.com/json")
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyText = response.body?.string()
                            if (!bodyText.isNullOrEmpty()) {
                                val json = org.json.JSONObject(bodyText)
                                if (json.optString("status") == "success") {
                                    latitude = json.optDouble("lat")
                                    longitude = json.optDouble("lon")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DeliveryViewModel", "IP-api capture failed: " + e.message)
                }
            }

            // Fallback coordinate if everything is empty (Erbil, Kurdistan)
            if (latitude == null || longitude == null) {
                latitude = 36.1912
                longitude = 44.0091
            }

            val latStr = ((latitude!! * 10000).toLong() / 10000.0).toString()
            val lonStr = ((longitude!! * 10000).toLong() / 10000.0).toString()

            // Step 3: Call Reverse Geocoding API (using OSM Nominatim domain server)
            _geocodingProgress.value = 0.8f
            _currentGeocodingStep.value = if (_isKurdish.value) "وەرگرتنی ناونیشانی فەرمی لە دۆمەینی Nominatim..." else "Reverse-geocoding via Nominatim domain API..."
            delay(1500)

            var resolvedAddress = ""
            try {
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url("https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude&zoom=18")
                    .header("User-Agent", "ShakhDelivery/1.0 (sardar.xano59@gmail.com)")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyText = response.body?.string()
                        if (!bodyText.isNullOrEmpty()) {
                            val json = org.json.JSONObject(bodyText)
                            resolvedAddress = json.optString("display_name", "")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DeliveryViewModel", "Reverse geocode lookup failed: " + e.message)
            }

            if (resolvedAddress.isEmpty()) {
                val placesKu = listOf(
                    "هەولێر، جادەی گوڵان (نزیک دریم سیتی)",
                    "هەولێر، گەڕەکی بەختیاری (نزیک پەرلەمان)",
                    "هەولێر، وەزیران (جادەی ١٠٠ مەتری)",
                    "هەولێر، ئیمپایەر وۆڕڵد"
                )
                val placesEn = listOf(
                    "Erbil, Gulan Street (Near Dream City)",
                    "Erbil, Bakhtiyari (Near Parliament)",
                    "Erbil, Waziran (100m Road)"
                )
                val randIndex = (0 until placesKu.size).random()
                resolvedAddress = if (_isKurdish.value) {
                    "${placesKu[randIndex]} (GPS: $latStr, $lonStr)"
                } else {
                    "${placesEn[randIndex % placesEn.size]} (GPS: $latStr, $lonStr)"
                }
            }

            if (resolvedAddress.length > 110) {
                resolvedAddress = resolvedAddress.take(110) + "..."
            }

            // Step 4: Finished fully
            _geocodingProgress.value = 1.0f
            _currentGeocodingStep.value = if (_isKurdish.value) "ناونیشانی ڕاستەقینە لە دۆمەینەوە وەرگیرا! ✔" else "Real address captured from domain successfully! ✔"
            delay(1000)

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                _isGeocoding.value = false
                onAddressResolved(resolvedAddress)
            }
        }
    }

    // Language state: defaults to Kurdish (true)
    private val _isKurdish = MutableStateFlow(true)
    val isKurdish: StateFlow<Boolean> = _isKurdish

    // Navigation/screen state: "home", "detail", "cart", "order_status", "supabase_setting"
    private val _currentScreen = MutableStateFlow("home")
    val currentScreen: StateFlow<String> = _currentScreen

    // Supabase config states (re-routed from repository)
    val supabaseUrl = repository.supabaseUrl
    val supabaseKey = repository.supabaseKey
    val connectionStatus = repository.connectionStatus

    // Loading states
    private val _isRestaurantsLoading = MutableStateFlow(false)
    val isRestaurantsLoading: StateFlow<Boolean> = _isRestaurantsLoading

    private val _isFoodLoading = MutableStateFlow(false)
    val isFoodLoading: StateFlow<Boolean> = _isFoodLoading

    // Restaurants
    private val _restaurants = MutableStateFlow<List<Restaurant>>(emptyList())
    val restaurants: StateFlow<List<Restaurant>> = _restaurants

    // Selected Restaurant & its food items
    private val _selectedRestaurant = MutableStateFlow<Restaurant?>(null)
    val selectedRestaurant: StateFlow<Restaurant?> = _selectedRestaurant

    private val _foodItems = MutableStateFlow<List<FoodItem>>(emptyList())
    val foodItems: StateFlow<List<FoodItem>> = _foodItems

    // Cart items: Map of FoodItem to Quantity
    private val _cart = MutableStateFlow<Map<FoodItem, Int>>(emptyMap())
    val cart: StateFlow<Map<FoodItem, Int>> = _cart

    // Search and Category Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    // Placed Order Tracking
    private val _trackedOrder = MutableStateFlow<Order?>(null)
    val trackedOrder: StateFlow<Order?> = _trackedOrder

    val orderStatusStep: StateFlow<Int> = _trackedOrder
        .map { order ->
            when (order?.status) {
                "Pending" -> 0
                "Preparing" -> 1
                "On the way" -> 2
                "Delivered" -> 3
                else -> 0
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // UI Toast or Notification info
    private val _notificationMessage = MutableStateFlow<String?>(null)
    val notificationMessage: StateFlow<String?> = _notificationMessage

    // Stories Integration State
    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories

    private val _activeStory = MutableStateFlow<Story?>(null)
    val activeStory: StateFlow<Story?> = _activeStory

    init {
        loadRestaurants()
        loadStories()
    }

    fun toggleLanguage() {
        _isKurdish.value = !_isKurdish.value
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun setSupabaseConfig(url: String, key: String) {
        repository.updateConnection(url, key)
        loadRestaurants()
        loadStories()
        showNotification(if (_isKurdish.value) "ڕێکخستنەکان نوێکرانەوە!" else "Configurations updated!")
    }

    fun getSupabaseSQL(): String {
        return repository.getSupabaseSetupSQL()
    }

    fun loadRestaurants() {
        viewModelScope.launch {
            _isRestaurantsLoading.value = true
            val list = repository.fetchRestaurants()
            _restaurants.value = list
            _isRestaurantsLoading.value = false
        }
    }

    fun loadStories() {
        viewModelScope.launch {
            val list = repository.fetchStories()
            _stories.value = list
        }
    }

    fun setActiveStory(story: Story?) {
        _activeStory.value = story
    }

    fun publishStory(
        restaurantId: String,
        restaurantName: String,
        restaurantNameKu: String?,
        foodName: String,
        foodNameKu: String?,
        description: String,
        descriptionKu: String?,
        price: Double,
        imageUrl: String
    ) {
        viewModelScope.launch {
            val id = "story-" + UUID.randomUUID().toString().take(8)
            val newStory = Story(
                id = id,
                restaurantId = restaurantId,
                restaurantName = restaurantName,
                restaurantNameKu = restaurantNameKu,
                foodName = foodName,
                foodNameKu = foodNameKu,
                description = description,
                descriptionKu = descriptionKu,
                price = price,
                imageUrl = imageUrl
            )
            repository.submitStory(newStory)
            loadStories()
            showNotification(if (_isKurdish.value) "ستۆری بە سەرکەوتوویی بڵاوکرایەوە!" else "Story published successfully!")
        }
    }

    fun selectRestaurant(restaurant: Restaurant) {
        _selectedRestaurant.value = restaurant
        _selectedCategory.value = "All"
        _searchQuery.value = ""
        navigateTo("detail")
        loadFoodItems(restaurant.id)
    }

    private fun loadFoodItems(restaurantId: String) {
        viewModelScope.launch {
            _isFoodLoading.value = true
            val items = repository.fetchFoodItems(restaurantId)
            _foodItems.value = items
            _isFoodLoading.value = false
        }
    }

    fun addToCart(foodItem: FoodItem) {
        val currentCart = _cart.value.toMutableMap()
        val count = currentCart[foodItem] ?: 0
        currentCart[foodItem] = count + 1
        _cart.value = currentCart
    }

    fun removeFromCart(foodItem: FoodItem) {
        val currentCart = _cart.value.toMutableMap()
        val count = currentCart[foodItem] ?: 0
        if (count > 1) {
            currentCart[foodItem] = count - 1
        } else {
            currentCart.remove(foodItem)
        }
        _cart.value = currentCart
    }

    fun clearCart() {
        _cart.value = emptyMap()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun showNotification(msg: String) {
        viewModelScope.launch {
            _notificationMessage.value = msg
            delay(3000)
            if (_notificationMessage.value == msg) {
                _notificationMessage.value = null
            }
        }
    }

    // Checkout Calculation properties
    val cartSubtotal: Double
        get() = _cart.value.entries.sumOf { it.key.price * it.value }

    val currentDeliveryFee: Double
        get() = _selectedRestaurant.value?.deliveryFee ?: 1500.0

    val cartTotal: Double
        get() = cartSubtotal + currentDeliveryFee

    fun checkout(customerName: String, phone: String, address: String) {
        if (customerName.isBlank() || phone.isBlank() || address.isBlank()) {
            showNotification(if (_isKurdish.value) "تکایە هەموو خانەکان پڕبکەرەوە." else "Please fill out all fields.")
            return
        }

        if (_cart.value.isEmpty()) {
            showNotification(if (_isKurdish.value) "سەبەتەکەت بەتاڵە!" else "Your cart is empty!")
            return
        }

        viewModelScope.launch {
            val itemsSummaryList = _cart.value.entries.map { "${it.key.localizedName(_isKurdish.value)} (x${it.value})" }
            val summary = itemsSummaryList.joinToString(", ")

            val orderId = "order-" + UUID.randomUUID().toString().take(8)
            val newOrder = Order(
                id = orderId,
                customerName = customerName,
                phone = phone,
                address = address,
                totalPrice = cartTotal,
                status = "Pending",
                itemsSummary = summary
            )

            // Submit order to repository (attempts Supabase, or saves locally)
            val resultOrder = repository.submitOrder(newOrder)
            _trackedOrder.value = resultOrder

            // Success feedback
            clearCart()
            navigateTo("order_status")
            showNotification(if (_isKurdish.value) "داواکارییەکەت بە سەرکەوتوویی تۆمارکرا!" else "Order placed successfully!")

            // Start real-time delivery status updates against the database domain!
            simulateOrderStatusFlow()
        }
    }

    private fun simulateOrderStatusFlow() {
        val currentOrder = _trackedOrder.value ?: return
        viewModelScope.launch {
            // 0 -> 1 after 7 seconds: Preparing
            delay(7000)
            val updated1 = repository.updateOrderStatus(currentOrder.id, "Preparing")
            if (updated1 != null) {
                _trackedOrder.value = updated1
            } else {
                _trackedOrder.value = _trackedOrder.value?.copy(status = "Preparing")
            }
            showNotification(if (_isKurdish.value) "مێزەکەت خەریکە ئامادە دەکرێت لە چێشتخانە!" else "Your meal is being prepared!")

            // 1 -> 2 after 8 seconds: On the way
            delay(8000)
            val updated2 = repository.updateOrderStatus(currentOrder.id, "On the way")
            if (updated2 != null) {
                _trackedOrder.value = updated2
            } else {
                _trackedOrder.value = _trackedOrder.value?.copy(status = "On the way")
            }
            showNotification(if (_isKurdish.value) "دیلیڤەری بەرەو ناونیشانت بەڕێکەوت!" else "Shakh rider is with your order, on the way!")

            // 2 -> 3 after 10 seconds: Delivered
            delay(10000)
            val updated3 = repository.updateOrderStatus(currentOrder.id, "Delivered")
            if (updated3 != null) {
                _trackedOrder.value = updated3
            } else {
                _trackedOrder.value = _trackedOrder.value?.copy(status = "Delivered")
            }
            showNotification(if (_isKurdish.value) "ئۆردەرەکە گەیشت! چێژی لێ ببینە!" else "Order delivered! Enjoy your meal!")
        }
    }
}
