package com.szopper.presentation.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

class DragDropState<T>(
    val items: List<T>,
    val onSwap: (Int, Int) -> Unit
) {
    var draggingItemIndex = mutableStateOf<Int?>(null)
    var draggingItemOffset = mutableStateOf(0f)
    private var itemHeight = 80f
    private var dragStartY = 0f
    private var currentDragY = 0f
    
    fun onDragStart(index: Int, startY: Float = 0f) {
        draggingItemIndex.value = index
        draggingItemOffset.value = 0f
        dragStartY = startY
        currentDragY = startY
    }

    fun onDragEnd() {
        draggingItemIndex.value = null
        draggingItemOffset.value = 0f
        dragStartY = 0f
        currentDragY = 0f
    }

    fun onDrag(dragAmount: Float, currentItemHeight: Float = 80f) {
        if (draggingItemIndex.value == null) return
        
        itemHeight = currentItemHeight
        
        // Update current drag position
        currentDragY += dragAmount
        
        // Calculate visual offset from start position (this follows finger precisely)
        val visualOffset = currentDragY - dragStartY
        draggingItemOffset.value = visualOffset
        
        // Check for swaps based on visual offset
        val draggingIndex = draggingItemIndex.value ?: return
        val threshold = itemHeight * 0.7f // 70% threshold for more controlled swapping
        
        when {
            visualOffset > threshold && draggingIndex < items.size - 1 -> {
                // Dragging down - swap with item below
                onSwap(draggingIndex, draggingIndex + 1)
                draggingItemIndex.value = draggingIndex + 1
                // Adjust drag start position to account for the swap
                dragStartY += itemHeight
                draggingItemOffset.value = currentDragY - dragStartY
            }
            visualOffset < -threshold && draggingIndex > 0 -> {
                // Dragging up - swap with item above  
                onSwap(draggingIndex, draggingIndex - 1)
                draggingItemIndex.value = draggingIndex - 1
                // Adjust drag start position to account for the swap
                dragStartY -= itemHeight
                draggingItemOffset.value = currentDragY - dragStartY
            }
        }
    }

    @Deprecated("Use onDrag with automatic swap detection instead")
    fun checkForSwap(index: Int, bounds: Float) {
        // Keep for backward compatibility but not used anymore
    }
}

@Composable
fun <T> rememberDragDropState(items: List<T>, onSwap: (Int, Int) -> Unit): DragDropState<T> {
    return remember(items.size) { DragDropState(items, onSwap) }
}

@Composable
fun <T> DraggableItem(
    state: DragDropState<T>,
    index: Int,
    modifier: Modifier = Modifier,
    itemHeight: Float = 80f,
    onDragStart: (() -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    content: @Composable (isDragging: Boolean) -> Unit
) {
    val isDragging = index == state.draggingItemIndex.value

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(state.items.size) { // Recompose when items change
                detectDragGesturesAfterLongPress(
                    onDragStart = { startPosition ->
                        onDragStart?.invoke()
                        state.onDragStart(index, startPosition.y) 
                    },
                    onDragEnd = { 
                        onDragEnd?.invoke()
                        state.onDragEnd() 
                    },
                    onDrag = { _, dragAmount ->
                        state.onDrag(dragAmount.y, itemHeight)
                    }
                )
            }
    ) {
        content(isDragging)
    }
}

