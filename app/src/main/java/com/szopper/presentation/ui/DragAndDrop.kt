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

    fun onDragStart(index: Int) {
        draggingItemIndex.value = index
    }

    fun onDragEnd() {
        draggingItemIndex.value = null
        draggingItemOffset.value = 0f
    }

    fun onDrag(offset: Float) {
        draggingItemOffset.value = offset
    }

    fun checkForSwap(index: Int, bounds: Float) {
        val draggingItem = draggingItemIndex.value ?: return
        if (index != draggingItem) {
            val otherItemCenter = bounds / 2
            if (draggingItemOffset.value > otherItemCenter) {
                if (index > draggingItem) {
                    onSwap(draggingItem, index)
                    draggingItemIndex.value = index
                }
            } else if (draggingItemOffset.value < -otherItemCenter) {
                if (index < draggingItem) {
                    onSwap(draggingItem, index)
                    draggingItemIndex.value = index
                }
            }
        }
    }
}

@Composable
fun <T> rememberDragDropState(items: List<T>, onSwap: (Int, Int) -> Unit): DragDropState<T> {
    return remember { DragDropState(items, onSwap) }
}

@Composable
fun <T> DraggableItem(
    state: DragDropState<T>,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable (isDragging: Boolean) -> Unit
) {
    val isDragging = index == state.draggingItemIndex.value

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { state.onDragStart(index) },
                    onDragEnd = { state.onDragEnd() },
                    onDrag = { _, dragAmount ->
                        state.onDrag(dragAmount.y)
                    }
                )
            }
    ) {
        content(isDragging)
    }
}

