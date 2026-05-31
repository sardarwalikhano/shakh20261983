package com.example.data.repository

import android.util.Log
import com.example.data.api.SupabaseService
import com.example.data.model.FoodItem
import com.example.data.model.Order
import com.example.data.model.Restaurant
import com.example.data.model.Story
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

class DeliveryRepository {

    // Connection configuration states (can be dynamically updated in-app or read from BuildConfig/Defaults)
    private val _supabaseUrl = MutableStateFlow("https://daim-post.online")
    val supabaseUrl: StateFlow<String> = _supabaseUrl

    private val _supabaseKey = MutableStateFlow("")
    val supabaseKey: StateFlow<String> = _supabaseKey

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.LocalOnly)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private var apiService: SupabaseService? = null

    // Fallback data cached in-memory
    private val localRestaurants = listOf(
        Restaurant(
            id = "res-1",
            name = "Kebab Restaurant Hawler",
            nameKu = "کەبابستانی هەولێر",
            cuisine = "Traditional Kurdish",
            cuisineKu = "خواردنی کوردیی ڕەسەن",
            imageUrl = "https://images.unsplash.com/photo-1544025162-d76694265947?w=500&q=80",
            rating = 4.9,
            deliveryTime = "15-25 min",
            deliveryFee = 1500.0,
            featured = true
        ),
        Restaurant(
            id = "res-2",
            name = "Hawand Fast Food",
            nameKu = "فاست فوودی هەوەند",
            cuisine = "Burgers & Pizza",
            cuisineKu = "بەرگر و پیتزا",
            imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&q=80",
            rating = 4.7,
            deliveryTime = "20-30 min",
            deliveryFee = 1000.0,
            featured = true
        ),
        Restaurant(
            id = "res-3",
            name = "Mount Shakh Sweets & Pastry",
            nameKu = "شیرینی لوتکەی شاخ",
            cuisine = "Desserts & Baklava",
            cuisineKu = "شیرینی و بەقلاوە",
            imageUrl = "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=500&q=80",
            rating = 4.8,
            deliveryTime = "25-35 min",
            deliveryFee = 2000.0,
            featured = false
        ),
        Restaurant(
            id = "res-4",
            name = "Biryani Heritage Hawler",
            nameKu = "بریانی ڕەسەنی هەولێر",
            cuisine = "Kurdish Rice & Stew",
            cuisineKu = "برنج و شلەی کوردی",
            imageUrl = "https://images.unsplash.com/photo-1633945274405-b6c8069047b0?w=500&q=80",
            rating = 4.6,
            deliveryTime = "30-40 min",
            deliveryFee = 1500.0,
            featured = false
        )
    )

    private val localFoodItems = listOf(
        // Kebab Restaurant Hawler Items
        FoodItem(
            id = "food-1",
            restaurantId = "res-1",
            name = "Shish Kebab Premium",
            nameKu = "شیش کەبابی نایاب",
            description = "Three skewers of fresh lamb kebab served with Iraqi bread, grilled onions, and tomatoes.",
            descriptionKu = "سێ شیش کەبابی بەرخی تازە پێشکەش دەکرێت لەگەڵ سەموون، پیازی برژاو و تەماتە.",
            price = 7500.0,
            imageUrl = "https://images.unsplash.com/photo-1544025162-d76694265947?w=500&q=80",
            category = "Mains",
            categoryKu = "خواردنە سەرەکییەکان"
        ),
        FoodItem(
            id = "food-2",
            restaurantId = "res-1",
            name = "Tikka Beef Skewers",
            nameKu = "شیشی تکەی گۆشت",
            description = "Tender marinated beef tikka skewers grilled to perfection over charcoal.",
            descriptionKu = "تکەی گۆشتی گوێرەکەی بەتام، لەسەر خەڵووز برژاوە مۆڵت دراوە.",
            price = 8500.0,
            imageUrl = "https://images.unsplash.com/photo-1603048588665-791ca8aea617?w=500&q=80",
            category = "Mains",
            categoryKu = "خواردنە سەرەکییەکان"
        ),
        FoodItem(
            id = "food-3",
            restaurantId = "res-1",
            name = "Kurdish Salad Mix",
            nameKu = "زەڵاتەی کوردی تێکەڵ",
            description = "Chopped cucumber, tomato, onion, sumac, fresh mint, and extra virgin olive oil.",
            descriptionKu = "خەیار، تەماتە، پیاز، سماق، نەعنای تازە و زەیتی زەیتوون.",
            price = 1500.0,
            imageUrl = "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=500&q=80",
            category = "Appetizers",
            categoryKu = "پێشخواردنەکان"
        ),
        // Hawand Fast Food Items
        FoodItem(
            id = "food-4",
            restaurantId = "res-2",
            name = "Shakh Double Cheese Burger",
            nameKu = "دۆبڵ چیزبەرگری شاخ",
            description = "Two premium beef patties, melted cheddar, pickles, house Shakh sauce on a brioche bun.",
            descriptionKu = "دوو پارچە گۆشتی بەرخی کوالیتی بەرز، پەنێری چێدەر، خەیارشور، سۆسی تایبەتی شاخ.",
            price = 6000.0,
            imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&q=80",
            category = "Mains",
            categoryKu = "خواردنە سەرەکییەکان"
        ),
        FoodItem(
            id = "food-5",
            restaurantId = "res-2",
            name = "Kurdish Hawand Pizza",
            nameKu = "پیتزای کوردی هەوەند",
            description = "Crispy crust topped with spiced minced beef kebab style, mozzarella, green peppers, and olives.",
            descriptionKu = "پیتزای بەتامی هەوەند لەگەڵ گۆشتی قیمەکراوی کەبابی و پەنێری مۆزارێلا.",
            price = 9000.0,
            imageUrl = "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=500&q=80",
            category = "Mains",
            categoryKu = "خواردنە سەرەکییەکان"
        ),
        FoodItem(
            id = "food-6",
            restaurantId = "res-2",
            name = "Loaded Cheese Fries",
            nameKu = "پەتاتەی سوورەکراو بە پەنێر",
            description = "Golden potato fries, melted warm cheese sauce, dynamic spices.",
            descriptionKu = "پەتاتەی سوورەکراوی زێڕینی بە نایابی پێشکەشکراو لەگەڵ سۆسی پەنێری گەرم.",
            price = 3000.0,
            imageUrl = "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=500&q=80",
            category = "Appetizers",
            categoryKu = "پێشخواردنەکان"
        ),
        // Sweets Items
        FoodItem(
            id = "food-7",
            restaurantId = "res-3",
            name = "Pistachio Baklava Plate",
            nameKu = "بەقلاوەی فستقی نایاب",
            description = "Freshly baked flaky layer pastry filled with selected premium pistachios and sugar syrup.",
            descriptionKu = "بەقلاوەی برژاوی نایاب پڕکراو بە فستقی حەلەبی و شیلەی نایاب.",
            price = 5000.0,
            imageUrl = "https://images.unsplash.com/photo-1519676867240-f03562e64548?w=500&q=80",
            category = "Desserts",
            categoryKu = "شیرینییەکان"
        ),
        FoodItem(
            id = "food-8",
            restaurantId = "res-3",
            name = "Kurdish Rice Pudding",
            nameKu = "مەحەلەبی برنجی کوردی",
            description = "Creamy traditional milk pudding hint of cardamom, rosewater and almonds on top.",
            descriptionKu = "مەحەلەبی کەرەستی شیری ڕەسەن بە بۆنی هێل و ئاوی گوڵ و بادەم.",
            price = 2500.0,
            imageUrl = "https://images.unsplash.com/photo-1541783245831-57d6fb0926d3?w=500&q=80",
            category = "Desserts",
            categoryKu = "شیرینییەکان"
        ),
        // Biryani Items
        FoodItem(
            id = "food-9",
            restaurantId = "res-4",
            name = "Special Chicken Biryani",
            nameKu = "برنجی بریانی تایبەت بە مریشک",
            description = "Fragrant long-grained basmati rice cooked with Kurdish spices, chicken, potatoes, and almonds.",
            descriptionKu = "برنجی بسماتی بەتامی بریانی کوردی پێشکەش کراوە لەگەڵ مریشک، پەتاتە، بادەم و مێوژ.",
            price = 6500.0,
            imageUrl = "https://images.unsplash.com/photo-1633945274405-b6c8069047b0?w=500&q=80",
            category = "Mains",
            categoryKu = "خواردنە سەرەکییەکان"
        ),
        FoodItem(
            id = "food-10",
            restaurantId = "res-4",
            name = "Kurdish Okra Stew (Bamya)",
            nameKu = "شله‌ی بامیە بە گۆشت",
            description = "Rich tomato and garlic based stew with tender local okra, served with rice.",
            descriptionKu = "شلەی بامیەی بەتامی گەرم لەگەڵ گۆشتی بەرخ، پێشکەش دەکرێت لەگەڵ برنجی سپی کوردی.",
            price = 5000.0,
            imageUrl = "https://images.unsplash.com/photo-1541518763669-27fef04b14ea?w=500&q=80",
            category = "Mains",
            categoryKu = "خواردنە سەرەکییەکان"
        )
    )

    private val localOrders = mutableListOf<Order>()

    private val localStories = mutableListOf<Story>(
        Story(
            id = "story-1",
            restaurantId = "res-1",
            restaurantName = "Kebab Restaurant Hawler",
            restaurantNameKu = "کەبابستانی هەولێر",
            foodName = "Fresh Lamb Chops (Pardax)",
            foodNameKu = "پەراسووی بەرخی تەڕ",
            description = "Get 20% off on premium grilled lamb chops today!",
            descriptionKu = "٢٠٪ داشکاندن لەسەر پەراسووی بەرخی برژاو بۆ ئەمڕۆ!",
            price = 12000.0,
            imageUrl = "https://images.unsplash.com/photo-1544025162-d76694265947?w=500&q=80"
        ),
        Story(
            id = "story-2",
            restaurantId = "res-2",
            restaurantName = "Hawand Fast Food",
            restaurantNameKu = "فاست فوودی هەوەند",
            foodName = "Crispy Chicken Tenders",
            foodNameKu = "سترێپسی مریشکی کریسپی نوێ",
            description = "Hot tender breast strips served with dipping honey mustard.",
            descriptionKu = "پارچە مریشکی کریسپی بەتامی نوێ خۆشترین سۆسی خەردەل.",
            price = 4500.0,
            imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&q=80"
        ),
        Story(
            id = "story-3",
            restaurantId = "res-3",
            restaurantName = "Mount Shakh Sweets & Pastry",
            restaurantNameKu = "شیرینی لوتکەی شاخ",
            foodName = "Warm Kunafeh with Cheese",
            foodNameKu = "کونافەی گەرمی نایاب بە پەنێر",
            description = "Fresh tray of Kunafeh is ready now! Come and taste the melting cheese.",
            descriptionKu = "سینیەکی تازەی کونافە خەریکە پڕ دەکرێ لە پەنێری کێشراو و گەرم!",
            price = 3000.0,
            imageUrl = "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=500&q=80"
        )
    )

    init {
        // Read configuration from secrets domain config / BuildConfig variables
        try {
            val liveUrl = com.example.BuildConfig.SUPABASE_URL
            if (liveUrl.isNotBlank() && !liveUrl.contains("YOUR_SUPABASE")) {
                _supabaseUrl.value = liveUrl
            }
        } catch (e: Throwable) {
            // Field not present yet or in testing
        }

        try {
            val liveKey = com.example.BuildConfig.SUPABASE_ANON_KEY
            if (liveKey.isNotBlank() && !liveKey.contains("YOUR_SUPABASE")) {
                _supabaseKey.value = liveKey
            }
        } catch (e: Throwable) {
            // Field not present yet or in testing
        }

        // Build the current configurations
        rebuildService()
    }

    fun updateConnection(url: String, key: String) {
        _supabaseUrl.value = url
        _supabaseKey.value = key
        rebuildService()
    }

    private fun rebuildService() {
        val url = _supabaseUrl.value.trim()

        if (url.isEmpty()) {
            _connectionStatus.value = ConnectionStatus.LocalOnly
            apiService = null
            return
        }

        try {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            // Prepare base URL with trailing slash
            val baseUrl = if (url.endsWith("/")) url else "$url/"

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            apiService = retrofit.create(SupabaseService::class.java)
            _connectionStatus.value = ConnectionStatus.Configured
            Log.d("DeliveryRepository", "Retrofit Service built for boundary URL: $baseUrl")
        } catch (e: Exception) {
            Log.e("DeliveryRepository", "Error setting up Supabase client: " + e.message)
            _connectionStatus.value = ConnectionStatus.Error(e.message ?: "Unknown Setup Error")
            apiService = null
        }
    }

    private fun getEffectiveKey(): String {
        val key = _supabaseKey.value.trim()
        return if (key.isEmpty() || key.contains("YOUR_SUPABASE")) "dummy-anon-key-placeholder" else key
    }

    suspend fun fetchRestaurants(): List<Restaurant> {
        val service = apiService
        if (service != null) {
            _connectionStatus.value = ConnectionStatus.Connecting
            val finalKey = getEffectiveKey()
            return try {
                val list = service.getRestaurants(apiKey = finalKey, authHeader = "Bearer $finalKey")
                _connectionStatus.value = ConnectionStatus.ConnectedLive
                list
            } catch (e: Exception) {
                Log.e("DeliveryRepository", "Error fetching live restaurants, falling back: " + e.message)
                _connectionStatus.value = ConnectionStatus.LiveError(e.message ?: "Connection Failure")
                localRestaurants
            }
        }
        return localRestaurants
    }

    suspend fun fetchFoodItems(restaurantId: String): List<FoodItem> {
        val service = apiService
        if (service != null) {
            val finalKey = getEffectiveKey()
            return try {
                service.getFoodItems(
                    apiKey = finalKey,
                    authHeader = "Bearer $finalKey",
                    restaurantIdQuery = "eq.$restaurantId"
                )
            } catch (e: Exception) {
                Log.e("DeliveryRepository", "Error fetching live food items: " + e.message)
                localFoodItems.filter { it.restaurantId == restaurantId }
            }
        }
        return localFoodItems.filter { it.restaurantId == restaurantId }
    }

    suspend fun submitOrder(order: Order): Order {
        val service = apiService
        if (service != null) {
            val finalKey = getEffectiveKey()
            return try {
                val listResult = service.createOrder(
                    apiKey = finalKey,
                    authHeader = "Bearer $finalKey",
                    order = order
                )
                if (listResult.isNotEmpty()) {
                    listResult.first()
                } else {
                    order
                }
            } catch (e: Exception) {
                Log.e("DeliveryRepository", "Error placing order to Supabase: " + e.message)
                // Add to local orders list & return it
                localOrders.add(order)
                order
            }
        } else {
            localOrders.add(order)
            return order
        }
    }

    suspend fun fetchOrderStatus(orderId: String): Order? {
        val service = apiService
        if (service != null) {
            val finalKey = getEffectiveKey()
            return try {
                // Fetch the status from the domain
                val list = service.getOrders(
                    apiKey = finalKey,
                    authHeader = "Bearer $finalKey",
                    idQuery = "eq.$orderId"
                )
                list.firstOrNull()
            } catch (e: Exception) {
                Log.e("DeliveryRepository", "Error fetching order status from Supabase: " + e.message)
                null
            }
        }
        return null
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String): Order? {
        val service = apiService
        if (service != null) {
            val finalKey = getEffectiveKey()
            return try {
                val updates = mapOf("status" to newStatus)
                val listResult = service.updateOrderStatus(
                    apiKey = finalKey,
                    authHeader = "Bearer $finalKey",
                    idQuery = "eq.$orderId",
                    updates = updates
                )
                listResult.firstOrNull()
            } catch (e: Exception) {
                Log.e("DeliveryRepository", "Error updating order status in Supabase: " + e.message)
                null
            }
        }
        return null
    }

    fun getLocalOrders(): List<Order> {
         return localOrders
    }

    suspend fun fetchStories(): List<Story> {
        val service = apiService
        if (service != null) {
            val finalKey = getEffectiveKey()
            return try {
                service.getStories(apiKey = finalKey, authHeader = "Bearer $finalKey")
            } catch (e: Exception) {
                Log.e("DeliveryRepository", "Error fetching live stories: " + e.message)
                localStories
            }
        }
        return localStories
    }

    suspend fun submitStory(story: Story): Story {
        val service = apiService
        if (service != null) {
            val finalKey = getEffectiveKey()
            return try {
                val resultList = service.createStory(
                    apiKey = finalKey,
                    authHeader = "Bearer $finalKey",
                    story = story
                )
                if (resultList.isNotEmpty()) {
                    resultList.first()
                } else {
                    story
                }
            } catch (e: Exception) {
                Log.e("DeliveryRepository", "Error saving story to Supabase: " + e.message)
                localStories.add(0, story)
                story
            }
        } else {
            localStories.add(0, story)
            return story
        }
    }

    // Helper functions to fetch SQL setups
    fun getSupabaseSetupSQL(): String {
        return """
        -- 1. Create RESTAURANTS table
        create table if not exists restaurants (
            id text primary key,
            name text not null,
            name_ku text,
            cuisine text not null,
            cuisine_ku text,
            image_url text not null,
            rating double precision default 4.5,
            delivery_time text default '25-35 min',
            delivery_fee double precision default 1000,
            featured boolean default false
        );

        -- 2. Create FOOD_ITEMS table
        create table if not exists food_items (
            id text primary key,
            restaurant_id text references restaurants(id) on delete cascade,
            name text not null,
            name_ku text,
            description text,
            description_ku text,
            price double precision not null,
            image_url text not null,
            category text not null,
            category_ku text
        );

        -- 3. Create ORDERS table
        create table if not exists orders (
            id text primary key,
            customer_name text not null,
            phone text not null,
            address text not null,
            total_price double precision not null,
            status text default 'Pending',
            items_summary text,
            created_at timestamp with time zone default timezone('utc'::text, now())
        );

        -- 4. Create STORIES table
        create table if not exists stories (
            id text primary key,
            restaurant_id text not null,
            restaurant_name text not null,
            restaurant_name_ku text,
            food_name text not null,
            food_name_ku text,
            description text,
            description_ku text,
            price double precision not null,
            image_url text not null,
            created_at timestamp with time zone default timezone('utc'::text, now())
        );

        -- Enable RLS (Row Level Security) and add Public Read Policies
        alter table restaurants enable row level security;
        alter table food_items enable row level security;
        alter table orders enable row level security;
        alter table stories enable row level security;

        -- Drop old policies if they exist to prevent duplication errors
        drop policy if exists "Allow public read restaurants" on restaurants;
        drop policy if exists "Allow public read food items" on food_items;
        drop policy if exists "Allow public insert / select orders" on orders;
        drop policy if exists "Allow public read stories" on stories;
        drop policy if exists "Allow public insert stories" on stories;

        create policy "Allow public read restaurants" on restaurants for select using (true);
        create policy "Allow public read food items" on food_items for select using (true);
        create policy "Allow public insert / select orders" on orders for all using (true);
        create policy "Allow public read stories" on stories for select using (true);
        create policy "Allow public insert stories" on stories for all using (true);

        -- Seed initial restaurants data
        insert into restaurants (id, name, name_ku, cuisine, cuisine_ku, image_url, rating, delivery_time, delivery_fee, featured)
        values 
        ('res-1', 'Kebab Restaurant Hawler', 'کەبابستانی هەولێر', 'Traditional Kurdish', 'خواردنی کوردیی ڕەسەن', 'https://images.unsplash.com/photo-1544025162-d76694265947?w=500&q=80', 4.9, '15-25 min', 1500, true),
        ('res-2', 'Hawand Fast Food', 'فاست فوودی هەوەند', 'Burgers & Pizza', 'بەرگر و پیتزا', 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&q=80', 4.7, '20-30 min', 1000, true),
        ('res-3', 'Mount Shakh Sweets & Pastry', 'شیرینی لوتکەی شاخ', 'Desserts & Baklava', 'شیرینی و بەقلاوە', 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=500&q=80', 4.8, '25-35 min', 2000, false)
        on conflict (id) do nothing;

        -- Seed initial food items
        insert into food_items (id, restaurant_id, name, name_ku, description, description_ku, price, image_url, category, category_ku)
        values
        ('food-1', 'res-1', 'Shish Kebab Premium', 'شیش کەبابی نایاب', 'Three skewers of fresh lamb kebab served with bread, grilled grilled onions.', 'سێ شیش کەبابی بەرخی تازە پێشکەش دەکرێت لەگەڵ سەموون، پەنێر', 7500, 'https://images.unsplash.com/photo-1544025162-d76694265947?w=500&q=80', 'Mains', 'خواردنە سەرەکییەکان'),
        ('food-2', 'res-1', 'Tikka Beef Skewers', 'شیشی تکەی گۆشت', 'Tender marinated beef tikka skewers grilled over charcoal.', 'تکەی گۆشتی گوێرەکەی بەتام, لەسەر خەڵووز برژاوە مۆڵت دراوە.', 8500, 'https://images.unsplash.com/photo-1603048588665-791ca8aea617?w=500&q=80', 'Mains', 'خواردنە سەرەکییەکان'),
        ('food-4', 'res-2', 'Shakh Double Cheese Burger', 'دۆبڵ چیزبەرگری شاخ', 'Two premium beef patties, melted cheddar, pickles, house Shakh sauce.', 'دوو پارچە گۆشتی بەرخی کوالیتی بەرز، پەنێری چێدەر، خەیارشور.', 6000, 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&q=80', 'Mains', 'خواردنە سەرەکییەکان'),
        ('food-7', 'res-3', 'Pistachio Baklava Plate', 'بەقلاوەی فستقی نایاب', 'Freshly baked flaky layer pastry filled with premium pistachios.', 'بەقلاوەی برژاوی نایاب پڕکراو بە فستقی حەلەبی.', 5000, 'https://images.unsplash.com/photo-1519676867240-f03562e64548?w=500&q=80', 'Desserts', 'شیرینییەکان')
        on conflict (id) do nothing;

        -- Seed initial stories data
        insert into stories (id, restaurant_id, restaurant_name, restaurant_name_ku, food_name, food_name_ku, description, description_ku, price, image_url)
        values
        ('story-1', 'res-1', 'Kebab Restaurant Hawler', 'کەبابستانی هەولێر', 'Fresh Lamb Chops (Pardax)', 'پەراسووی بەرخی تەڕ', 'Get 20% off on premium grilled lamb chops today!', '٢٠٪ داشکاندن لەسەر پەراسووی بەرخی برژاو بۆ ئەمڕۆ!', 12000, 'https://images.unsplash.com/photo-1544025162-d76694265947?w=500&q=80'),
        ('story-2', 'res-2', 'Hawand Fast Food', 'فاست فوودی هەوەند', 'Crispy Chicken Tenders', 'سترێپسی مریشکی کریسپی نوێ', 'Hot tender breast strips served with dipping honey mustard.', 'پارچە مریشکی کریسپی بەتامی نوێ خۆشترین سۆسی خەردەل.', 4500, 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&q=80'),
        ('story-3', 'res-3', 'Mount Shakh Sweets & Pastry', 'شیرینی لوتکەی شاخ', 'Warm Kunafeh with Cheese', 'کونافەی گەرمی نایاب بە پەنێر', 'Fresh tray of Kunafeh is ready now! Come and taste the melting cheese.', 'سینیەکی تازەی کونافە خەریکە پڕ دەکرێ لە پەنێری کێشراو و گەرم!', 3000, 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=500&q=80')
        on conflict (id) do nothing;
        """.trimIndent()
    }
}

sealed interface ConnectionStatus {
    object LocalOnly : ConnectionStatus
    object Configured : ConnectionStatus
    object Connecting : ConnectionStatus
    object ConnectedLive : ConnectionStatus
    data class Error(val message: String) : ConnectionStatus
    data class LiveError(val message: String) : ConnectionStatus
}
