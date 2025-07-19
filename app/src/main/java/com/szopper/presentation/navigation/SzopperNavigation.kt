package com.szopper.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.szopper.presentation.ui.ProductListScreen
import com.szopper.presentation.sync.SyncScreen

@Composable
fun SzopperNavigation(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "product_list",
        modifier = modifier
    ) {
        composable("product_list") {
            ProductListScreen(
                onNavigateToSync = {
                    navController.navigate("sync")
                }
            )
        }
        
        composable("sync") {
            SyncScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}