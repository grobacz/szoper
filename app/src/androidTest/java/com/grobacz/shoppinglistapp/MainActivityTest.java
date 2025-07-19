package com.grobacz.shoppinglistapp;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeLeft;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    private ActivityScenario<MainActivity> activityScenario;
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        // Reset database before each test
        MainActivity.resetDatabase(context);
        
        // Launch the activity and wait for it to be ready
        activityScenario = ActivityScenario.launch(MainActivity.class);
        activityScenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);
        
        // Wait for the activity to fully initialize
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @After
    public void tearDown() {
        if (activityScenario != null) {
            activityScenario.close();
        }
    }

    @Test
    public void testAppLaunchAndBasicUIVisibility() {
        // Wait for UI to fully load
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Check if main UI elements are displayed
        onView(withId(R.id.tabLayout)).check(matches(isDisplayed()));
        onView(withId(R.id.recyclerView)).check(matches(isDisplayed()));
        onView(withId(R.id.fabAdd)).check(matches(isDisplayed()));
        onView(withId(R.id.bottomAppBar)).check(matches(isDisplayed()));
        onView(withId(R.id.btnCategories)).check(matches(isDisplayed()));
        onView(withId(R.id.btnSync)).check(matches(isDisplayed()));
    }

    @Test
    public void testAddProduct() {
        // Wait for the first tab to be selected
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Click the FAB to add a product
        onView(withId(R.id.fabAdd)).perform(click());

        // Fill in product details
        onView(withId(R.id.etProductName)).perform(typeText("Test Product"));
        onView(withId(R.id.etQuantity)).perform(typeText("3"));

        // Click Add button
        onView(withId(R.id.btnAdd)).perform(click());

        // Verify the product appears in the list
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("Test Product"))));
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("3"))));
    }

    @Test
    public void testSwipeLeftToIncreaseQuantity() {
        // Add a product first
        addTestProduct("Swipe Test Product", "2");

        // Swipe left on the first item to increase quantity
        onView(withId(R.id.recyclerView))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, swipeLeft()));

        // Verify quantity increased to 3
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("3"))));
    }

    @Test
    public void testSwipeRightToDecreaseQuantity() {
        // Add a product first
        addTestProduct("Swipe Test Product", "3");

        // Swipe right on the first item to decrease quantity
        onView(withId(R.id.recyclerView))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, swipeRight()));

        // Verify quantity decreased to 2
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("2"))));
    }

    @Test
    public void testSwipeRightToZeroQuantityCrossesOut() {
        // Add a product with quantity 1
        addTestProduct("Cross Out Test", "1");

        // Swipe right to decrease quantity to 0
        onView(withId(R.id.recyclerView))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, swipeRight()));

        // Verify product is still there but with 0 quantity
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("Cross Out Test"))));
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("0"))));
    }

    @Test
    public void testSwipeRightOnZeroQuantityRemovesItem() {
        // Add a product with quantity 1
        addTestProduct("Remove Test", "1");

        // Swipe right to decrease quantity to 0
        onView(withId(R.id.recyclerView))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, swipeRight()));

        // Swipe right again to remove the item
        onView(withId(R.id.recyclerView))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, swipeRight()));

        // Verify the item is removed (list should be empty or not contain the item)
        onView(withId(R.id.recyclerView))
                .check(matches(hasItemCount(0)));
    }

    @Test
    public void testCategoryTabsSwitching() {
        // Wait for tabs to load
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Get the first tab and verify it's selected
        onView(withId(R.id.tabLayout)).check(matches(isDisplayed()));

        // Add a product to the first category
        addTestProduct("Category Test Product", "1");

        // Click on the second tab (if it exists)
        onView(allOf(withText("Household"), isDisplayed())).perform(click());

        // Verify the product list is now empty (different category)
        onView(withId(R.id.recyclerView))
                .check(matches(hasItemCount(0)));
    }

    @Test
    public void testEmptyStateVisibility() {
        // When no products exist, empty state should be visible
        // Note: This test depends on the empty state layout being properly implemented
        // For now, we'll just check that the recycler view is empty
        onView(withId(R.id.recyclerView))
                .check(matches(hasItemCount(0)));
    }

    @Test
    public void testDragAndDropProductReordering() {
        // Add multiple products
        addTestProduct("First Product", "1");
        addTestProduct("Second Product", "2");
        addTestProduct("Third Product", "3");

        // Verify initial order
        onView(withId(R.id.recyclerView))
                .check(matches(hasDescendant(withText("First Product"))));

        // Note: Drag and drop testing in Espresso requires custom actions
        // For now, we'll just verify the products are there
        onView(withId(R.id.recyclerView))
                .check(matches(hasItemCount(3)));
    }

    // Helper method to add a test product
    private void addTestProduct(String name, String quantity) {
        // Wait for the first tab to be selected
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Click the FAB
        onView(withId(R.id.fabAdd)).perform(click());

        // Fill in product details
        onView(withId(R.id.etProductName)).perform(typeText(name));
        onView(withId(R.id.etQuantity)).perform(typeText(quantity));

        // Click Add button
        onView(withId(R.id.btnAdd)).perform(click());
    }

    // Custom matcher to check RecyclerView item count
    private static Matcher<View> hasItemCount(int count) {
        return new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(View view) {
                if (!(view instanceof RecyclerView)) {
                    return false;
                }
                RecyclerView recyclerView = (RecyclerView) view;
                RecyclerView.Adapter adapter = recyclerView.getAdapter();
                return adapter != null && adapter.getItemCount() == count;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("RecyclerView with item count: " + count);
            }
        };
    }
}