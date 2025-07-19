package com.szopper.presentation.utils

import android.app.ActivityManager
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MemoryManager(private val context: Context) {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val memoryInfo = ActivityManager.MemoryInfo()
    
    private val _memoryPressure = MutableStateFlow(MemoryPressure.LOW)
    val memoryPressure: StateFlow<MemoryPressure> = _memoryPressure.asStateFlow()
    
    fun checkMemoryPressure(): MemoryPressure {
        activityManager.getMemoryInfo(memoryInfo)
        
        val pressure = when {
            memoryInfo.lowMemory -> MemoryPressure.CRITICAL
            memoryInfo.availMem < memoryInfo.totalMem * 0.15 -> MemoryPressure.HIGH
            memoryInfo.availMem < memoryInfo.totalMem * 0.30 -> MemoryPressure.MEDIUM
            else -> MemoryPressure.LOW
        }
        
        _memoryPressure.value = pressure
        return pressure
    }
    
    fun getMemoryInfo(): MemoryStats {
        activityManager.getMemoryInfo(memoryInfo)
        
        return MemoryStats(
            totalMemory = memoryInfo.totalMem,
            availableMemory = memoryInfo.availMem,
            usedMemory = memoryInfo.totalMem - memoryInfo.availMem,
            isLowMemory = memoryInfo.lowMemory,
            threshold = memoryInfo.threshold
        )
    }
    
    fun getOptimalPageSize(): Int {
        return when (checkMemoryPressure()) {
            MemoryPressure.CRITICAL -> 5
            MemoryPressure.HIGH -> 10
            MemoryPressure.MEDIUM -> 15
            MemoryPressure.LOW -> 20
        }
    }
    
    fun shouldReduceAnimations(): Boolean {
        return checkMemoryPressure() >= MemoryPressure.HIGH
    }
    
    fun shouldLimitImageCaching(): Boolean {
        return checkMemoryPressure() >= MemoryPressure.MEDIUM
    }
}

enum class MemoryPressure(val level: Int) {
    LOW(0),
    MEDIUM(1),
    HIGH(2),
    CRITICAL(3)
}

operator fun MemoryPressure.compareTo(other: MemoryPressure): Int {
    return this.level.compareTo(other.level)
}

data class MemoryStats(
    val totalMemory: Long,
    val availableMemory: Long,
    val usedMemory: Long,
    val isLowMemory: Boolean,
    val threshold: Long
) {
    val memoryUsagePercentage: Float
        get() = (usedMemory.toFloat() / totalMemory.toFloat()) * 100f
    
    val availableMemoryMB: Long
        get() = availableMemory / (1024 * 1024)
    
    val totalMemoryMB: Long
        get() = totalMemory / (1024 * 1024)
}

@Composable
fun rememberMemoryManager(): MemoryManager {
    val context = LocalContext.current
    return remember { MemoryManager(context) }
}

@Composable
fun MemoryAwareContent(
    lowMemoryContent: @Composable () -> Unit,
    normalContent: @Composable () -> Unit
) {
    val memoryManager = rememberMemoryManager()
    val memoryPressure by memoryManager.memoryPressure.collectAsState()
    
    LaunchedEffect(Unit) {
        memoryManager.checkMemoryPressure()
    }
    
    when (memoryPressure) {
        MemoryPressure.CRITICAL, MemoryPressure.HIGH -> lowMemoryContent()
        MemoryPressure.MEDIUM, MemoryPressure.LOW -> normalContent()
    }
}