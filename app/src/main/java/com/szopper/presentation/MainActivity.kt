package com.szopper.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.szopper.presentation.navigation.SzopperNavigation
import com.szopper.presentation.ui.theme.SzopperTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SzopperTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SzopperNavigation(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}