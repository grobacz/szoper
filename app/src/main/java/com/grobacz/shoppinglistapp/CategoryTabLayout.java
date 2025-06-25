package com.grobacz.shoppinglistapp;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MotionEventCompat;

import com.google.android.material.tabs.TabLayout;

public class CategoryTabLayout extends TabLayout {
    private static final String TAG = "CategoryTabLayout";
    private boolean isDragging = false;
    private float startX;
    private int startTabIndex = -1;
    private OnTabDragListener dragListener;
    private View selectedTabView;
    private int tabWidth;
    private int tabCount;

    public interface OnTabDragListener {
        void onTabMoved(int fromPosition, int toPosition);
    }

    public CategoryTabLayout(@NonNull Context context) {
        super(context);
        init();
    }

    public CategoryTabLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CategoryTabLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Enable long press for drag and drop
        setOnLongClickListener(v -> {
            // Find which tab was long-pressed
            float x = getXForTab(getSelectedTabPosition());
            if (x >= 0) {
                isDragging = true;
                startX = x;
                startTabIndex = getSelectedTabPosition();
                selectedTabView = getTabAt(startTabIndex).view;
                if (selectedTabView != null) {
                    selectedTabView.setBackgroundColor(Color.parseColor("#4CAF50")); // Highlight selected tab
                }
                return true;
            }
            return false;
        });
    }

    private float getXForTab(int position) {
        View tabView = getTabAt(position).view;
        if (tabView != null) {
            int[] location = new int[2];
            tabView.getLocationOnScreen(location);
            return location[0];
        }
        return -1;
    }

    public void setOnTabDragListener(OnTabDragListener listener) {
        this.dragListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (isDragging) {
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isDragging) {
            return super.onTouchEvent(ev);
        }

        int action = ev.getActionMasked();
        tabCount = getTabCount();
        if (tabCount == 0) return false;

        tabWidth = getWidth() / tabCount;
        int currentPosition = Math.min(tabCount - 1, Math.max(0, (int) (ev.getX() / tabWidth)));

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                if (startTabIndex == -1) return false;

                // Calculate new position
                int newPosition = Math.min(tabCount - 1, Math.max(0, (int) (ev.getX() / tabWidth)));
                
                // Only trigger move if position changed
                if (newPosition != startTabIndex) {
                    if (dragListener != null) {
                        dragListener.onTabMoved(startTabIndex, newPosition);
                        startTabIndex = newPosition; // Update start position for next move
                    }
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (selectedTabView != null) {
                    selectedTabView.setBackgroundColor(Color.TRANSPARENT);
                }
                isDragging = false;
                startTabIndex = -1;
                selectedTabView = null;
                return true;
        }

        return super.onTouchEvent(ev);
    }
}
