package com.example.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.FoodItem
import com.example.data.model.Order
import com.example.data.model.Restaurant
import com.example.data.model.Story
import com.example.data.repository.ConnectionStatus
import com.example.ui.theme.*
import com.example.ui.viewmodel.DeliveryViewModel
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun ShakhDeliveryUi(viewModel: DeliveryViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isKurdish by viewModel.isKurdish.collectAsState()
    val notificationMessage by viewModel.notificationMessage.collectAsState()
    val activeStory by viewModel.activeStory.collectAsState()
    val context = LocalContext.current

    // Observe notification alerts
    LaunchedEffect(notificationMessage) {
        notificationMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ShakhTopBar(viewModel = viewModel)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith
                                fadeOut(animationSpec = tween(220))
                    },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        "home" -> HomeScreen(viewModel = viewModel)
                        "detail" -> RestaurantDetailScreen(viewModel = viewModel)
                        "cart" -> CartScreen(viewModel = viewModel)
                        "order_status" -> OrderStatusScreen(viewModel = viewModel)
                        "supabase_setting" -> SupabaseSettingScreen(viewModel = viewModel)
                        else -> HomeScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // Active Story overlay
        activeStory?.let { story ->
            ActiveStoryViewer(
                story = story,
                viewModel = viewModel,
                onClose = { viewModel.setActiveStory(null) }
            )
        }
    }
}

@Composable
fun ShakhTopBar(viewModel: DeliveryViewModel) {
    val isKurdish by viewModel.isKurdish.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    val subtotal = viewModel.cartSubtotal
    val itemsCount = cart.values.sum()

    // Determine status badge color
    val statusColor = when (connectionStatus) {
        is ConnectionStatus.ConnectedLive -> ShakhSuccess
        is ConnectionStatus.LocalOnly -> ShakhGold
        is ConnectionStatus.Configured -> ShakhBlue
        is ConnectionStatus.Connecting -> ShakhOrange
        else -> ShakhRed
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left section: Title or Back button
            if (currentScreen == "home") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, CircleShape)
                    )
                    Text(
                        text = if (isKurdish) "لوتکەی شاخ" else "Shakh Delivery",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = ShakhRed
                        )
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        when (currentScreen) {
                            "detail" -> viewModel.navigateTo("home")
                            "cart" -> viewModel.navigateTo("detail")
                            "order_status" -> viewModel.navigateTo("home")
                            "supabase_setting" -> viewModel.navigateTo("home")
                            else -> viewModel.navigateTo("home")
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Right section: Lang Toggle, Cart badge, Settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Language toggle
                TextButton(
                    onClick = { viewModel.toggleLanguage() },
                    modifier = Modifier.testTag("language_toggle")
                ) {
                    Text(
                        text = if (isKurdish) "English" else "کوردی",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ShakhOrange
                        )
                    )
                }

                // Supabase config page trigger
                IconButton(
                    onClick = { viewModel.navigateTo("supabase_setting") },
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Supabase Settings",
                        tint = if (currentScreen == "supabase_setting") ShakhRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Cart badge
                if (itemsCount > 0 && currentScreen != "cart" && currentScreen != "order_status") {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = ShakhRed) {
                                Text(text = "$itemsCount", color = Color.White)
                            }
                        }
                    ) {
                        IconButton(
                            onClick = { viewModel.navigateTo("cart") },
                            modifier = Modifier.testTag("cart_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "View Cart",
                                tint = ShakhRed
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: DeliveryViewModel) {
    val restaurants by viewModel.restaurants.collectAsState()
    val isKurdish by viewModel.isKurdish.collectAsState()
    val isRestaurantsLoading by viewModel.isRestaurantsLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val categories = listOf("All", "Mains", "Appetizers", "Desserts")
    val categoriesKu = listOf("گشتی", "خواردنی سەرەکی", "پێشخواردن", "شیرینییەکان")

    // Filter restaurants based on Search & Category
    val filteredList = restaurants.filter { res ->
        val nameMatch = res.name.contains(searchQuery, ignoreCase = true) ||
                (res.nameKu?.contains(searchQuery, ignoreCase = true) ?: false)
        val cuisineMatch = res.cuisine.contains(searchQuery, ignoreCase = true) ||
                (res.cuisineKu?.contains(searchQuery, ignoreCase = true) ?: false)

        val matchesCategory = if (selectedCategory == "All" || selectedCategory == "گشتی") {
            true
        } else {
            // Match category name or translated category
            val mappedCategory = when (selectedCategory) {
                "Mains", "خواردنی سەرەکی" -> "Traditional Kurdish" // simplistic model route mapping
                "Appetizers", "پێشخواردن" -> "Appetizers"
                "Desserts", "شیرینییەکان" -> "Desserts & Baklava"
                else -> ""
            }
            res.cuisine.contains(selectedCategory, ignoreCase = true) ||
                    res.cuisine.contains(mappedCategory, ignoreCase = true) ||
                    (res.cuisineKu?.contains(selectedCategory, ignoreCase = true) ?: false)
        }

        (nameMatch || cuisineMatch) && matchesCategory
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero visual banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(ShakhRed, ShakhOrange)
                        )
                    )
            ) {
                // Background artistic illustration
                Image(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(140.dp)
                        .offset(x = 10.dp, y = 20.dp),
                    alpha = 0.15f
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isKurdish) "دیلیڤەری زۆر خێرا" else "Fast Mountain Delivery",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isKurdish) "دیاری ناوازەی کێوەکان بۆ سەر مێزەکەت" else "Direct delicious meals from cooks to your table",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = CircleShape,
                        color = ShakhGold,
                        contentColor = ShakhTextDark
                    ) {
                        Text(
                            text = if (isKurdish) "بێ بەرامبەر لە یەکەم داواکاری" else "Free delivery on 1st order",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Stories Row Section
        item {
            StoriesRow(viewModel = viewModel)
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(text = if (isKurdish) "بۆ چێشتخانە یان خواردن بگەڕێ..." else "Search for restaurants or dishes...")
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search Icon")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_bar"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ShakhRed,
                    cursorColor = ShakhRed
                )
            )
        }

        // Horizontal Category Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories.size) { index ->
                    val catEng = categories[index]
                    val catKu = categoriesKu[index]
                    val isSelected = selectedCategory == catEng || selectedCategory == catKu

                    Surface(
                        modifier = Modifier
                            .clickable {
                                viewModel.selectCategory(if (isKurdish) catKu else catEng)
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) ShakhRed else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        border = if (!isSelected) BorderStroke(1.dp, ShakhCardStroke) else null
                    ) {
                        Text(
                            text = if (isKurdish) catKu else catEng,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                text = if (isKurdish) "سەرجەم چێشتخانەکان" else "All Restaurants",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        // List Loading State
        if (isRestaurantsLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ShakhRed)
                }
            }
        } else if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Not found",
                            tint = ShakhWarning,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isKurdish) "هیچ چێشتخانەیەک نەدۆزرایەوە!" else "No restaurants found!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredList) { restaurant ->
                RestaurantCard(
                    restaurant = restaurant,
                    isKurdish = isKurdish,
                    onClick = { viewModel.selectRestaurant(restaurant) }
                )
            }
        }
    }
}

@Composable
fun RestaurantCard(
    restaurant: Restaurant,
    isKurdish: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("restaurant_card_${restaurant.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Image with delivery badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                AsyncImage(
                    model = restaurant.imageUrl,
                    contentDescription = restaurant.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Featured tag badge
                if (restaurant.featured) {
                    Surface(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.TopStart),
                        shape = RoundedCornerShape(8.dp),
                        color = ShakhGold,
                        contentColor = ShakhTextDark
                    ) {
                        Text(
                            text = if (isKurdish) "ناوازە" else "Featured",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Delivery Time Badge
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.BottomEnd),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = restaurant.deliveryTime,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Info details
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = restaurant.localizedName(isKurdish),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = ShakhGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${restaurant.rating}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = restaurant.localizedCuisine(isKurdish),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = if (isKurdish) "دیلیڤەری: ${restaurant.deliveryFee.toInt()} د.ع" else "Delivery: ${restaurant.deliveryFee.toInt()} IQD",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = ShakhOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun RestaurantDetailScreen(viewModel: DeliveryViewModel) {
    val restaurant by viewModel.selectedRestaurant.collectAsState()
    val foodItems by viewModel.foodItems.collectAsState()
    val isKurdish by viewModel.isKurdish.collectAsState()
    val isFoodLoading by viewModel.isFoodLoading.collectAsState()
    val cart by viewModel.cart.collectAsState()

    val currentRes = restaurant ?: return

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp) // buffer for bottom cart summary
        ) {
            // Header Image banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    AsyncImage(
                        model = currentRes.imageUrl,
                        contentDescription = currentRes.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = currentRes.localizedName(isKurdish),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentRes.localizedCuisine(isKurdish),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = ShakhGold, modifier = Modifier.size(16.dp))
                                Text(text = "${currentRes.rating}", color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Text(
                                text = if (isKurdish) "دیلیڤەری: ${currentRes.deliveryFee.toInt()} د.ع" else "Delivery: ${currentRes.deliveryFee.toInt()} IQD",
                                style = MaterialTheme.typography.bodyMedium.copy(color = ShakhOrange, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Category Divider Title
            item {
                Text(
                    text = if (isKurdish) "مینۆ" else "Menu Items",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (isFoodLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ShakhRed)
                    }
                }
            } else if (foodItems.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(text = if (isKurdish) "هیچ خواردنێک نییە لە ئێستادا" else "No food items found in this section.")
                    }
                }
            } else {
                items(foodItems) { item ->
                    val quantity = cart[item] ?: 0
                    FoodItemRow(
                        foodItem = item,
                        quantity = quantity,
                        isKurdish = isKurdish,
                        onAdd = { viewModel.addToCart(item) },
                        onRemove = { viewModel.removeFromCart(item) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 1.dp,
                        color = ShakhCardStroke.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // View Cart Floating banner
        val totalQuantity = cart.values.sum()
        if (totalQuantity > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { viewModel.navigateTo("cart") }
                    .testTag("floating_cart_banner"),
                shape = RoundedCornerShape(12.dp),
                color = ShakhRed,
                contentColor = Color.White,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        ) {
                            Text(
                                text = "$totalQuantity",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = if (isKurdish) "سەیرکردنی سەبەتە" else "View Cart",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        text = if (isKurdish) "کۆی گشتی: ${viewModel.cartSubtotal.toInt()} د.ع" else "Subtotal: ${viewModel.cartSubtotal.toInt()} IQD",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun FoodItemRow(
    foodItem: FoodItem,
    quantity: Int,
    isKurdish: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("food_item_row_${foodItem.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = foodItem.localizedName(isKurdish),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = foodItem.localizedDescription(isKurdish),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isKurdish) "${foodItem.price.toInt()} د.ع" else "${foodItem.price.toInt()} IQD",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ShakhOrange
                )
            )
        }

        // Image with actions
        Box(
            modifier = Modifier.size(90.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AsyncImage(
                model = foodItem.imageUrl,
                contentDescription = foodItem.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            )

            // Adjust buttons overlay
            if (quantity == 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 8.dp)
                        .clickable(onClick = onAdd)
                        .testTag("add_item_btn_${foodItem.id}"),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = ShakhRed,
                    border = BorderStroke(1.dp, ShakhRed.copy(alpha = 0.5f)),
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = if (isKurdish) "زیادکە" else "ADD",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ShakhCardStroke),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("remove_item_btn_${foodItem.id}")
                        ) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Decrease", tint = ShakhRed, modifier = Modifier.size(12.dp))
                        }

                        Text(
                            text = "$quantity",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = onAdd,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("add_more_btn_${foodItem.id}")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", tint = ShakhRed, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartScreen(viewModel: DeliveryViewModel) {
    val cart by viewModel.cart.collectAsState()
    val isKurdish by viewModel.isKurdish.collectAsState()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val isGeocoding by viewModel.isGeocoding.collectAsState()
    val currentGeocodingStep by viewModel.currentGeocodingStep.collectAsState()
    val geocodingProgress by viewModel.geocodingProgress.collectAsState()

    val requestPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        viewModel.getAddressFromLiveGPS(context, fineGranted, coarseGranted) { resolved ->
            address = resolved
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cart_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = if (isKurdish) "سەبەتەکەت" else "Your Cart",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (cart.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, tint = ShakhCardStroke, modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isKurdish) "تەداواکارییەکت لێرە زۆر بەتاڵە!" else "Your cart is currently empty!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.navigateTo("home") },
                            colors = ButtonDefaults.buttonColors(containerColor = ShakhRed)
                        ) {
                            Text(text = if (isKurdish) "بگەڕێوە بۆ ماڵەوە" else "Go to Home", color = Color.White)
                        }
                    }
                }
            }
        } else {
            // Cart Items
            items(cart.keys.toList()) { item ->
                val qty = cart[item] ?: 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cart_item_row_${item.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.localizedName(isKurdish), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        Text(text = if (isKurdish) "${item.price.toInt()} د.ع" else "${item.price.toInt()} IQD", style = MaterialTheme.typography.bodyMedium, color = ShakhOrange)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { viewModel.removeFromCart(item) }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Decrease", tint = ShakhRed)
                        }
                        Text(text = "$qty", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        IconButton(onClick = { viewModel.addToCart(item) }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", tint = ShakhRed)
                        }
                    }
                }
                HorizontalDivider(color = ShakhCardStroke.copy(alpha = 0.5f))
            }

            // Summary Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, ShakhCardStroke)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = if (isKurdish) "نرخی خواردنەکان:" else "Subtotal:")
                            Text(text = if (isKurdish) "${viewModel.cartSubtotal.toInt()} د.ع" else "${viewModel.cartSubtotal.toInt()} IQD", fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = if (isKurdish) "نرخی گەیاندن:" else "Delivery Fee:")
                            Text(text = if (isKurdish) "${viewModel.currentDeliveryFee.toInt()} د.ع" else "${viewModel.currentDeliveryFee.toInt()} IQD", fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(color = ShakhCardStroke)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = if (isKurdish) "کۆی گشتی:" else "Total:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                            Text(
                                text = if (isKurdish) "${viewModel.cartTotal.toInt()} د.ع" else "${viewModel.cartTotal.toInt()} IQD",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = ShakhRed)
                            )
                        }
                    }
                }
            }

            // Checkout details Form
            item {
                Text(
                    text = if (isKurdish) "زانیاری گەیاندن" else "Delivery Details",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(text = if (isKurdish) "ناو" else "Your Name") },
                        modifier = Modifier.fillMaxWidth().testTag("name_field"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ShakhRed)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(text = if (isKurdish) "ژمارەی مۆبایل" else "Phone Number") },
                        modifier = Modifier.fillMaxWidth().testTag("phone_field"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ShakhRed)
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(text = if (isKurdish) "ناونیشان (شوێن یان گەڕەک)" else "Delivery Address") },
                        modifier = Modifier.fillMaxWidth().testTag("address_field"),
                        trailingIcon = {
                            if (isGeocoding) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = ShakhRed
                                )
                            } else {
                                IconButton(onClick = {
                                    requestPermissionsLauncher.launch(
                                        arrayOf(
                                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Capture Live GPS",
                                        tint = ShakhOrange
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ShakhRed)
                    )

                    if (isGeocoding) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ShakhGold.copy(alpha = 0.12f)),
                            border = BorderStroke(1.dp, ShakhCardStroke)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = ShakhOrange
                                    )
                                    Text(
                                        text = currentGeocodingStep,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = ShakhOrange
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { geocodingProgress },
                                    modifier = Modifier.fillMaxWidth().height(4.dp),
                                    color = ShakhRed,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }

                    // Modern live location fetch trigger button
                    TextButton(
                        onClick = {
                            requestPermissionsLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.textButtonColors(contentColor = ShakhOrange)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn, 
                                contentDescription = null, 
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isKurdish) "دیاریکردنی لۆکەیشنی ڕاستەوخۆ (سیستەمی GPS) ⚡" else "Get Live GPS Location ⚡",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Submit Button
            item {
                Button(
                    onClick = { viewModel.checkout(name, phone, address) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("place_order_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ShakhRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = if (isKurdish) "ئۆردەرەکە بنێرە (Cash on Delivery)" else "Place Order (Cash on Delivery)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun OrderStatusScreen(viewModel: DeliveryViewModel) {
    val order by viewModel.trackedOrder.collectAsState()
    val statusStep by viewModel.orderStatusStep.collectAsState()
    val isKurdish by viewModel.isKurdish.collectAsState()

    val currentOrder = order ?: return

    val stepsEn = listOf("Order Placed", "Preparing in Kitchen", "Out for Delivery", "Delivered")
    val stepsKu = listOf("داواکاری تۆمارکرا", "سەرقاڵی چێکردن", "ڕێگەدایە بۆ گەیاندن", "بەسەرکەوتوویی گەیشت")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("order_status_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ShakhSuccess,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isKurdish) "بەدواداچوونی گەیاندن" else "Track Shakh Rider",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (isKurdish) "ناسنامەی داواکاری: ${currentOrder.id}" else "Order ID: ${currentOrder.id}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Stepper
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ShakhCardStroke),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (i in 0..3) {
                        val stateTitle = if (isKurdish) stepsKu[i] else stepsEn[i]
                        val isActive = statusStep >= i
                        val isCurrent = statusStep == i

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Indicator bullet
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        if (isCurrent) ShakhOrange else if (isActive) ShakhSuccess else MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isActive && !isCurrent) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                } else {
                                    Text(text = "${i + 1}", color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Column {
                                Text(
                                    text = stateTitle,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = if (isCurrent) ShakhOrange else if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                                val stepSubtitle = when (i) {
                                    0 -> if (isActive) (if (isKurdish) "تۆمارکرا: ئێستا" else "Recorded: Just now") else (if (isKurdish) "چاوەڕوانە" else "Pending")
                                    1 -> if (statusStep > 1) (if (isKurdish) "تەواوبوو" else "Finished") 
                                         else if (isCurrent) (if (isKurdish) "سەرقاڵی چێکردن" else "Cooking") 
                                         else (if (isKurdish) "چاوەڕوانە" else "Pending")
                                    2 -> if (statusStep > 2) (if (isKurdish) "تایبەت گەیشت" else "Rider arrived") 
                                         else if (isCurrent) (if (isKurdish) "مەودا: ٢.٤ کم (کات: ٨ خولەک)" else "Distance: 2.4 km (Time: 8 mins)") 
                                         else (if (isKurdish) "ڕەوانەکراو نییە هێشتا" else "Rider not dispatched yet")
                                    3 -> if (isCurrent) (if (isKurdish) "تەسلیم کرا! پیرۆزە" else "Handed over successfully!") 
                                         else (if (isKurdish) "پاشەکەوتوو" else "Pending arrival")
                                    else -> ""
                                }
                                Text(
                                    text = stepSubtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isCurrent) ShakhOrange else if (isActive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                )
                                if (isCurrent) {
                                    val tipText = when (i) {
                                        0 -> if (isKurdish) "ئۆفەرەکەت لە لیست دایە بەرەو چێشتخانە..." else "Order received by the restaurant, in queue..."
                                        1 -> if (isKurdish) "سەرقاڵی ئامادەکردنی نایابترین تامەکانین بۆت..." else "Chef is cooking your fresh mountain meal..."
                                        2 -> if (isKurdish) "دیلیڤەری بەرەو ماڵەکەت گازی لێداوە!" else "Rider is steering fast over hills to your address..."
                                        3 -> if (isKurdish) "چێژی نایاب ببینە! سوپاس بۆ داواکاریت." else "Delivered! Thank you for ordering from Shakh."
                                        else -> ""
                                    }
                                    Text(text = tipText, style = MaterialTheme.typography.bodySmall, color = ShakhOrange)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Summary details info package
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = if (isKurdish) "پوختەی داواکاری" else "Order Details", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    Text(text = currentOrder.itemsSummary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(color = ShakhCardStroke.copy(alpha = 0.4f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = if (isKurdish) "کۆ گشتی پارە (واسل):" else "Grand Total (Cash):")
                        Text(text = if (isKurdish) "${currentOrder.totalPrice.toInt()} د.ع" else "${currentOrder.totalPrice.toInt()} IQD", fontWeight = FontWeight.Bold, color = ShakhRed)
                    }
                }
            }
        }

        // Address package info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = ShakhOrange)
                        Text(text = if (isKurdish) "شوێنی گەیاندن" else "Delivery Destination", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    }
                    Text(text = "${currentOrder.customerName}\n${currentOrder.phone}\n${currentOrder.address}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Back to home btn
        item {
            OutlinedButton(
                onClick = { viewModel.navigateTo("home") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ShakhRed),
                border = BorderStroke(1.dp, ShakhRed)
            ) {
                Text(text = if (isKurdish) "بگەڕێوە بۆ چێشتخانەکان" else "Return to Restaurants")
            }
        }
    }
}

@Composable
fun SupabaseSettingScreen(viewModel: DeliveryViewModel) {
    val supabaseUrl by viewModel.supabaseUrl.collectAsState()
    val supabaseKey by viewModel.supabaseKey.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val isKurdish by viewModel.isKurdish.collectAsState()

    var inputUrl by remember { mutableStateOf(supabaseUrl) }
    var inputKey by remember { mutableStateOf(supabaseKey) }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("supabase_setting_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = if (isKurdish) "بەستنەوە بە سوپابیس (Supabase)" else "Supabase Connection Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isKurdish)
                    "دەتوانیت لێرە کورتەکلیلی پڕۆژەکەت و دۆمەینەکەت بەکاربهێنیت بۆ دیتای ڕاستەوخۆ!"
                else
                    "Configure and connect this delivery prototype dynamically to your live Postgres database instance.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Connection Status Banner
        item {
            val (statusText, statusBg, textColor) = when (connectionStatus) {
                is ConnectionStatus.ConnectedLive -> Triple(
                    if (isKurdish) "پەیوەستبوون ڕاستەوخۆ چالاکە!" else "Live Database Connection Active!",
                    ShakhSuccess.copy(alpha = 0.15f),
                    ShakhSuccess
                )
                is ConnectionStatus.LocalOnly -> Triple(
                    if (isKurdish) "دۆخی تاقیکاری لۆکاڵ (In-Memory)" else "Local Offline Sandbox Active",
                    ShakhGold.copy(alpha = 0.15f),
                    ShakhOrange
                )
                is ConnectionStatus.Configured -> Triple(
                    if (isKurdish) "کڵایێنت نوێکرایەوە، خەریکی ئەنجامدانی پەیوەستبوونە..." else "Configured. Initializing sync...",
                    ShakhBlue.copy(alpha = 0.15f),
                    ShakhBlue
                )
                is ConnectionStatus.Connecting -> Triple(
                    if (isKurdish) "خەریکی گواستنەوەی داتا..." else "Querying Supabase endpoint...",
                    ShakhOrange.copy(alpha = 0.15f),
                    ShakhOrange
                )
                is ConnectionStatus.LiveError -> Triple(
                    if (isKurdish) "هەڵە لە پەیوەستبوون: (داتا لۆکاڵ پیشان دەدرێت)" else "Sync Failed: Falling back to Local Sandbox data",
                    ShakhRed.copy(alpha = 0.15f),
                    ShakhRed
                )
                is ConnectionStatus.Error -> Triple(
                    if (isKurdish) "هەڵە لە دروستکردنی کڵایێنت" else "Client configuration error",
                    ShakhRed.copy(alpha = 0.15f),
                    ShakhRed
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = statusBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = statusText,
                        color = textColor,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (connectionStatus is ConnectionStatus.LiveError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = (connectionStatus as ConnectionStatus.LiveError).message,
                            color = ShakhRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Input Fields
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    label = { Text(text = "Supabase URL / Domain") },
                    placeholder = { Text(text = "https://daim-post.online") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ShakhRed)
                )

                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    label = { Text(text = "Supabase Public Anon Key") },
                    placeholder = { Text(text = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ShakhRed)
                )
            }
        }

        // Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        inputUrl = "https://daim-post.online"
                        viewModel.showNotification("URL reset to your default project")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ShakhOrange),
                    border = BorderStroke(1.dp, ShakhOrange)
                ) {
                    Text(text = if (isKurdish) "لینکی فەرمی پڕۆژە" else "Default URL")
                }

                Button(
                    onClick = {
                        viewModel.setSupabaseConfig(inputUrl, inputKey)
                    },
                    modifier = Modifier.weight(1f).testTag("save_config_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ShakhRed)
                ) {
                    Text(text = if (isKurdish) "سەیڤ بکە و تاقیکەرەوە" else "Save & Connect", color = Color.White)
                }
            }
        }

        // Setup Guide & Copy-Paste SQL SQL helper
        item {
            HorizontalDivider(color = ShakhCardStroke)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isKurdish) "ڕێنمایی بۆ دامەزراندنی بنکەی داتا" else "Supabase SQL Setup Script",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isKurdish)
                    "بۆ ئەوەی مۆبایلەکەت داتاکان ڕاستەوخۆ لە سوپابیس بخوێنێتەوە، تکایە ئەم کۆدە کۆپی بکە و لە بەشی SQL Editor لە ناو وێبسایتی Supabase لێی بدە:"
                else
                    "Execute this setup SQL script inside your Supabase project's SQL Editor window compile table entities and insert fallback food entries:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // SQL display block
        item {
            val sqlScript = viewModel.getSupabaseSQL()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState()),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, ShakhCardStroke)
            ) {
                Text(
                    text = sqlScript,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(sqlScript))
                    Toast.makeText(context, if (isKurdish) "کۆپی کرا لە کلیپبۆرد!" else "SQL script copied!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ShakhOrange),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(text = if (isKurdish) "کۆپی کردنی جیاکەرەوەکان (SQL)" else "Copy Table Schemas (SQL)", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun StoriesRow(viewModel: DeliveryViewModel) {
    val stories by viewModel.stories.collectAsState()
    val isKurdish by viewModel.isKurdish.collectAsState()
    var showPostDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (isKurdish) "ستۆری و خواردنی نوێ 🔥" else "Live Stories & New Food 🔥",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // First Item: Create Story button for Shopkeepers
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(onClick = { showPostDialog = true })
                        .padding(horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(ShakhOrange, ShakhRed)))
                            .padding(2.5.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Post new story",
                            tint = ShakhOrange,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isKurdish) "نوێ دابنێ" else "Post Story",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = ShakhOrange
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Other items: Active story list
            items(stories) { story ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { viewModel.setActiveStory(story) }
                        .animateContentSize()
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        ShakhRed,
                                        ShakhOrange,
                                        ShakhGold,
                                        ShakhRed
                                    )
                                )
                            )
                            .padding(2.5.dp) // Beautiful border gap
                            .background(MaterialTheme.colorScheme.background, CircleShape)
                            .padding(2.dp)
                            .clip(CircleShape)
                    ) {
                        AsyncImage(
                            model = story.imageUrl,
                            contentDescription = story.localizedFoodName(isKurdish),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = story.localizedRestaurantName(isKurdish),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(76.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showPostDialog) {
        PostStoryDialog(
            onDismiss = { showPostDialog = false },
            viewModel = viewModel
        )
    }
}

@Composable
fun PostStoryDialog(onDismiss: () -> Unit, viewModel: DeliveryViewModel) {
    val isKurdish by viewModel.isKurdish.collectAsState()
    val restaurants by viewModel.restaurants.collectAsState()

    // Form inputs state
    var selectedRestaurantId by remember { mutableStateOf(restaurants.firstOrNull()?.id ?: "res-custom") }
    var customRestaurantName by remember { mutableStateOf("") }
    
    var foodNameEn by remember { mutableStateOf("") }
    var foodNameKu by remember { mutableStateOf("") }
    var descriptionEn by remember { mutableStateOf("") }
    var descriptionKu by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var imageUrlInput by remember { mutableStateOf("https://images.unsplash.com/photo-1544025162-d76694265947?w=500&q=80") }

    val presets = listOf(
        Triple("🍢 کەباب / Kebab", "https://images.unsplash.com/photo-1544025162-d76694265947?w=500&q=80", "کەباب تێکەڵی بەتامی نوێ بۆ مێز و داواکارییەکانتان"),
        Triple("🍔 بەرگر / Burger", "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&q=80", "چیزبەرگری بەرخی دۆبڵ بە سۆسی نیشتمانی نوێ"),
        Triple("🍕 پیتزا / Pizza", "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=500&q=80", "پیتزای گەرمی کێشراو بە پەنێری لوتکەی شاخ بۆ ئێواران"),
        Triple("🍰 شیرینی / Pastry", "https://images.unsplash.com/photo-1519676867240-f03562e64548?w=500&q=80", "بەقلاوەی فستقی تازە لە فڕن دەرهێنراو بۆ ئەمڕۆ"),
        Triple("🍛 بریانی / Biryani", "https://images.unsplash.com/photo-1633945274405-b6c8069047b0?w=500&q=80", "سینی بریانی گۆشتی تازە لەگەڵ شلەی بامیە"),
        Triple("🍟 پەتاتە / Potato", "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=500&q=80", "موقەبیلات و پەتاتەی گەرمی سوورکراو بە پەنێر")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isKurdish) "بڵاوکردنەوەی ستۆری و بەرهەمی نوێ" else "Create Live Story Alert",
                fontWeight = FontWeight.Bold,
                color = ShakhRed
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isKurdish) "دوکاندار و چێشتخانەکان دەتوانن لێرە داتای نوێ دابنێن بۆ ستۆری" else "Shopkeepers/Restaurants can announce items via Live Stories:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. Restaurant Selector
                Text(
                    text = if (isKurdish) "بڵاوکەرەوە / چێشتخانە" else "Publisher / Restaurant",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    restaurants.forEach { res ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRestaurantId = res.id }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedRestaurantId == res.id),
                                onClick = { selectedRestaurantId = res.id }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = res.localizedName(isKurdish))
                        }
                    }

                    // Custom choice
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRestaurantId = "res-custom" }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = (selectedRestaurantId == "res-custom"),
                            onClick = { selectedRestaurantId = "res-custom" }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (isKurdish) "چێشتخانەی تر یان ناوی دڵخواز" else "Other / Custom Vendor")
                    }
                }

                if (selectedRestaurantId == "res-custom") {
                    OutlinedTextField(
                        value = customRestaurantName,
                        onValueChange = { customRestaurantName = it },
                        label = { Text(if (isKurdish) "ناوی چێشتخانە یان مارکێت" else "Custom House Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                HorizontalDivider(color = ShakhCardStroke)

                // 2. Food Name Inputs
                OutlinedTextField(
                    value = foodNameKu,
                    onValueChange = { foodNameKu = it },
                    label = { Text(if (isKurdish) "ناوی خواردنەکە (کوردی)" else "Food Name (Kurdish)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = foodNameEn,
                    onValueChange = { foodNameEn = it },
                    label = { Text(if (isKurdish) "ناوی خواردنەکە (ئینگلیزی)" else "Food Name (English)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 3. Description Inputs
                OutlinedTextField(
                    value = descriptionKu,
                    onValueChange = { descriptionKu = it },
                    label = { Text(if (isKurdish) "وردەکاری یان ئۆفەر (کوردی)" else "Offer Detail (Kurdish)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descriptionEn,
                    onValueChange = { descriptionEn = it },
                    label = { Text(if (isKurdish) "وردەکاری یان ئۆفەر (ئینگلیزی)" else "Offer Detail (English)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 4. Price Field
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text(if (isKurdish) "نرخی خواردنی نوێ (IQD)" else "Price of New Food (IQD)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                // 5. Preset Photo Selector (Slight touch choice)
                Text(
                    text = if (isKurdish) "دیزاین و وێنە خێراکان بۆ ستۆری" else "Select Creative Visual Preset",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presets) { preset ->
                        val isSelected = (imageUrlInput == preset.second)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) ShakhOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) ShakhOrange else ShakhCardStroke
                            ),
                            modifier = Modifier
                                .clickable {
                                    imageUrlInput = preset.second
                                    if (foodNameKu.isBlank()) {
                                        foodNameKu = preset.first.split(" ").first() + "ی نایابی نوێ"
                                        foodNameEn = "Premium New " + preset.first.split("/").last().trim()
                                    }
                                    if (descriptionKu.isBlank()) {
                                        descriptionKu = preset.third
                                        descriptionEn = "Tempting hot offer of " + preset.first.split("/").last().trim() + " is ready!"
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                ) {
                                    AsyncImage(
                                        model = preset.second,
                                        contentDescription = preset.first,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = preset.first.split("/").first(),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = imageUrlInput,
                    onValueChange = { imageUrlInput = it },
                    label = { Text(if (isKurdish) "بەستەری وێنە" else "Custom Image URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPrice = priceText.toDoubleOrNull() ?: 5000.0
                    val isCustom = (selectedRestaurantId == "res-custom")
                    
                    // Restaurant identity resolve
                    val resNameVal: String
                    val resNameKuVal: String
                    val resIdVal: String

                    if (isCustom) {
                        resIdVal = "res-custom-" + UUID.randomUUID().toString().take(6)
                        resNameVal = if (customRestaurantName.isNotBlank()) customRestaurantName else "Custom Bistro"
                        resNameKuVal = if (customRestaurantName.isNotBlank()) customRestaurantName else "چێشتخانەی تایبەت"
                    } else {
                        val matchedRes = restaurants.find { it.id == selectedRestaurantId }
                        resIdVal = selectedRestaurantId
                        resNameVal = matchedRes?.name ?: "Partner Kitchen"
                        resNameKuVal = matchedRes?.nameKu ?: "هاوبەشی ئێمە"
                    }

                    // Validation checks
                    if (foodNameKu.isBlank() && foodNameEn.isBlank()) {
                        viewModel.showNotification("Please provide a name of food!")
                        return@Button
                    }

                    val finalFoodKu = if (foodNameKu.isNotBlank()) foodNameKu else foodNameEn
                    val finalFoodEn = if (foodNameEn.isNotBlank()) foodNameEn else foodNameKu
                    val finalDescKu = if (descriptionKu.isNotBlank()) descriptionKu else descriptionEn
                    val finalDescEn = if (descriptionEn.isNotBlank()) descriptionEn else descriptionKu

                    viewModel.publishStory(
                        restaurantId = resIdVal,
                        restaurantName = resNameVal,
                        restaurantNameKu = resNameKuVal,
                        foodName = finalFoodEn,
                        foodNameKu = finalFoodKu,
                        description = finalDescEn,
                        descriptionKu = finalDescKu,
                        price = finalPrice,
                        imageUrl = imageUrlInput
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ShakhRed)
            ) {
                Text(text = if (isKurdish) "ستۆری بڵاوبکەرەوە" else "Publish Live Story", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = if (isKurdish) "پاشگەزبوونەوە" else "Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun ActiveStoryViewer(
    story: Story,
    viewModel: DeliveryViewModel,
    onClose: () -> Unit
) {
    val isKurdish by viewModel.isKurdish.collectAsState()
    val restaurants by viewModel.restaurants.collectAsState()
    
    // Auto-dismiss after 6.5 seconds simulated timer
    LaunchedEffect(story) {
        delay(6500)
        onClose()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(onClick = onClose) // tap anywhere to dismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // Top indicator progress bar animation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(1.dp))
            ) {
                var progress by remember { mutableStateOf(0f) }
                LaunchedEffect(Unit) {
                    val duration = 6500f
                    val steps = 65
                    val delayStep = (duration / steps).toLong()
                    for (i in 1..steps) {
                        delay(delayStep)
                        progress = i / steps.toFloat()
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(ShakhOrange, RoundedCornerShape(1.dp))
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Vendor header info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ShakhOrange)
                    ) {
                        AsyncImage(
                            model = story.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column {
                        Text(
                            text = story.localizedRestaurantName(isKurdish),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(ShakhSuccess, CircleShape))
                            Text(
                                text = if (isKurdish) "ئێستا چالاکە" else "New Release • Live",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // Close Button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close story",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // Main story artwork image card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(20.dp))
                    .background(Color.DarkGray)
            ) {
                AsyncImage(
                    model = story.imageUrl,
                    contentDescription = story.localizedFoodName(isKurdish),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Visual Dark gradient shadow on image bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                startY = 300f
                            )
                        )
                )

                // Announcement Text Details Overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Surface(
                        color = ShakhRed,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (isKurdish) "مژدانە / ئۆفەری نوێ" else "NEW DISH ALERT",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = story.localizedFoodName(isKurdish),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = story.localizedDescription(isKurdish),
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Price tag
                    Text(
                        text = "${story.price.toInt()} IQD",
                        color = ShakhGold,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // Direct Call to Action button: Go straight to their menu!
            val matchedRes = restaurants.find { it.id == story.restaurantId }
            Button(
                onClick = {
                    if (matchedRes != null) {
                        viewModel.selectRestaurant(matchedRes)
                    } else {
                        // Or if custom vendor, navigate to home and search or show toast
                        viewModel.navigateTo("home")
                    }
                    onClose()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ShakhRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Order option",
                        tint = Color.White
                    )
                    Text(
                        text = if (isKurdish) "سەردانی چێشتخانە بکە و داوا بکە 🛍️" else "Visit Restaurant & Order 🛍️",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
