package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.data.repository.DeliveryRepository
import com.example.ui.screens.ShakhDeliveryUi
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DeliveryViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        // Instantiate the single sources of truth using simple constructor injection
        val repository = remember { DeliveryRepository() }
        val viewModel = remember { DeliveryViewModel(repository) }

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          // Render the main visual routing interface
          Box(modifier = Modifier.padding(innerPadding)) {
            ShakhDeliveryUi(viewModel = viewModel)
          }
        }
      }
    }
  }
}

