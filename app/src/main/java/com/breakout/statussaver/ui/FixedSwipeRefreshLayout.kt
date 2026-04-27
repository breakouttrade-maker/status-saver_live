package com.breakout.statussaver.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class FixedSwipeRefreshLayout(context: Context, attrs: AttributeSet? = null) :
    SwipeRefreshLayout(context, attrs) {

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Let children handle ACTION_DOWN (clicks/long-clicks), but keep pull-to-refresh working
        return if (ev.action == MotionEvent.ACTION_DOWN) {
            false
        } else {
            super.onInterceptTouchEvent(ev)
        }
    }
}
