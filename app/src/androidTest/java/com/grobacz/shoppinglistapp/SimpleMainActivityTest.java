package com.grobacz.shoppinglistapp;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class SimpleMainActivityTest {

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
    public void testAppLaunches() {
        // Wait for UI to fully load
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Check if main UI elements are displayed
        onView(withId(R.id.tabLayout)).check(matches(isDisplayed()));
        onView(withId(R.id.recyclerView)).check(matches(isDisplayed()));
        onView(withId(R.id.fabAdd)).check(matches(isDisplayed()));
    }
}