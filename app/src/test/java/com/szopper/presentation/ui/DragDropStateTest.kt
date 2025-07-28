package com.szopper.presentation.ui

import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DragDropStateTest {

    private lateinit var onSwap: (Int, Int) -> Unit
    private lateinit var dragDropState: DragDropState<String>
    private val testItems = listOf("Item 1", "Item 2", "Item 3", "Item 4")

    @Before
    fun setup() {
        onSwap = mock()
        dragDropState = DragDropState(testItems, onSwap)
    }

    @Test
    fun `onDragStart should set dragging index and reset offset`() {
        // When
        dragDropState.onDragStart(1, 100f)

        // Then
        assertEquals(1, dragDropState.draggingItemIndex.value)
        assertEquals(0f, dragDropState.draggingItemOffset.value)
    }

    @Test
    fun `onDragEnd should reset dragging state`() {
        // Given
        dragDropState.onDragStart(1, 100f)
        dragDropState.onDrag(50f, 80f)

        // When
        dragDropState.onDragEnd()

        // Then
        assertNull(dragDropState.draggingItemIndex.value)
        assertEquals(0f, dragDropState.draggingItemOffset.value)
    }

    @Test
    fun `onDrag should show visual offset without swapping below threshold`() {
        // Given
        dragDropState.onDragStart(1, 100f)

        // When - drag less than 70% of item height (40f < 56f threshold)
        dragDropState.onDrag(40f, 80f)

        // Then
        assertEquals(40f, dragDropState.draggingItemOffset.value)
        assertEquals(1, dragDropState.draggingItemIndex.value)
        // Verify no swap occurred
        verify(onSwap, org.mockito.kotlin.never()).invoke(org.mockito.kotlin.any(), org.mockito.kotlin.any())
    }

    @Test
    fun `onDrag should swap down when dragging beyond threshold`() {
        // Given
        dragDropState.onDragStart(1, 100f)

        // When - drag more than 70% of item height downward (60f > 56f threshold)
        dragDropState.onDrag(60f, 80f)

        // Then
        verify(onSwap).invoke(1, 2) // Should swap with next item
        assertEquals(2, dragDropState.draggingItemIndex.value)
        assertEquals(-20f, dragDropState.draggingItemOffset.value) // Adjusted after swap
    }

    @Test
    fun `onDrag should swap up when dragging beyond negative threshold`() {
        // Given
        dragDropState.onDragStart(2, 200f)

        // When - drag more than 70% of item height upward (-60f < -56f threshold)
        dragDropState.onDrag(-60f, 80f)

        // Then
        verify(onSwap).invoke(2, 1) // Should swap with previous item
        assertEquals(1, dragDropState.draggingItemIndex.value)
        assertEquals(20f, dragDropState.draggingItemOffset.value) // Adjusted after swap
    }

    @Test
    fun `onDrag should not swap down when at last position`() {
        // Given
        dragDropState.onDragStart(3, 300f) // Last item

        // When - try to drag down beyond threshold
        dragDropState.onDrag(60f, 80f)

        // Then - no swap should occur
        verify(onSwap, org.mockito.kotlin.never()).invoke(org.mockito.kotlin.any(), org.mockito.kotlin.any())
        assertEquals(3, dragDropState.draggingItemIndex.value)
        assertEquals(60f, dragDropState.draggingItemOffset.value)
    }

    @Test
    fun `onDrag should not swap up when at first position`() {
        // Given
        dragDropState.onDragStart(0, 0f) // First item

        // When - try to drag up beyond threshold
        dragDropState.onDrag(-60f, 80f)

        // Then - no swap should occur
        verify(onSwap, org.mockito.kotlin.never()).invoke(org.mockito.kotlin.any(), org.mockito.kotlin.any())
        assertEquals(0, dragDropState.draggingItemIndex.value)
        assertEquals(-60f, dragDropState.draggingItemOffset.value)
    }

    @Test
    fun `onDrag should handle sequential drag movements correctly`() {
        // Given
        dragDropState.onDragStart(0, 0f)

        // When - drag down to trigger swap
        dragDropState.onDrag(60f, 80f) // Should trigger swap: 0 -> 1

        // Then - verify swap occurred
        verify(onSwap).invoke(0, 1)
        assertEquals(1, dragDropState.draggingItemIndex.value)
        // Visual offset should be adjusted after swap
        assertEquals(-20f, dragDropState.draggingItemOffset.value)
    }

    @Test
    fun `onDrag should not swap when no item is being dragged`() {
        // Given - no drag started

        // When
        dragDropState.onDrag(100f, 80f)

        // Then
        verify(onSwap, org.mockito.kotlin.never()).invoke(org.mockito.kotlin.any(), org.mockito.kotlin.any())
        assertNull(dragDropState.draggingItemIndex.value)
        assertEquals(0f, dragDropState.draggingItemOffset.value) // No offset when not dragging
    }
}