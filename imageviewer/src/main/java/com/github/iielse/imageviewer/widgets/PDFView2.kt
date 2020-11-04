package com.github.iielse.imageviewer.widgets

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.github.barteksc.pdfviewer.PDFView
import com.github.iielse.imageviewer.R
import com.github.iielse.imageviewer.core.Components
import com.github.iielse.imageviewer.core.Photo
import com.github.iielse.imageviewer.ex.getWindowSize
import com.github.iielse.imageviewer.utils.Config
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 作者：吕振鹏
 * E-mail:lvzhenpeng@pansoft.com
 * 创建时间：2020年11月03日
 * 时间：10:35 AM
 * 版本：v1.0.0
 * 类描述：
 * 修改时间：
 */
class PDFView2 constructor(context: Context, attrs: AttributeSet? = null)
    : PDFView(context, attrs) {

    companion object {
        private val TAG = PDFView2::class.java.simpleName
    }

    interface Listener {
        fun onDrag(view: PDFView2, fraction: Float)
        fun onRestore(view: PDFView2, fraction: Float)
        fun onRelease(view: PDFView2)
    }

    fun addListener(listener: Listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    private var isDetachedForWindow: Boolean = false
    private val scaledTouchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop * Config.SWIPE_TOUCH_SLOP }
    private val dismissEdge by lazy { height * Config.DISMISS_FRACTION }
    private var singleTouch = false
    private var fakeDragOffset = 0f
    private var lastX = 0f
    private var lastY = 0f
    private val listeners = mutableListOf<Listener>()
    private var firstTouchY = 0F
    private var firstTouchX = 0F

    fun reset() {
        Log.d(TAG, "reset: isRecycled = $isRecycled -----isDetachedForWindow = $isDetachedForWindow")
        if (isRecycled && isDetachedForWindow) {
            val photo = getTag(R.id.viewer_adapter_item_data) as Photo
            val viewHolder = getTag(R.id.viewer_adapter_item_holder) as RecyclerView.ViewHolder
            Components.requireImageLoader().load(this, photo, viewHolder)
        }
    }


    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (Config.SWIPE_DISMISS && Config.VIEWER_ORIENTATION == ViewPager2.ORIENTATION_HORIZONTAL) {
            handleDispatchTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun handleDispatchTouchEvent(event: MotionEvent?) {
        if (isZooming) {
            parent?.requestDisallowInterceptTouchEvent(true)
            return
        }

        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                firstTouchY = event.y
                firstTouchX = event.x
                singleTouch = pageCount == 1 || firstTouchY / context.getWindowSize().y >= 0.8 || firstTouchY / context.getWindowSize().y <= 0.235
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                singleTouch = false
                animate()
                        .translationX(0f).translationY(0f).scaleX(1f).scaleY(1f)
                        .setDuration(200).start()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> up()
            MotionEvent.ACTION_MOVE -> {
                val currentY = event.y
                val currentX = event.x
                val absX = abs(firstTouchX - currentX)
                val absY = abs(firstTouchY - currentY)
                if ((absX >= 40 || absY >= 40) && absX > absY) {
                    if (Config.DEBUG) Log.d("----pdf----", "X轴移动距离大于Y轴距离，可以拦截我absX = $absX,absY = $absY")
                    parent?.requestDisallowInterceptTouchEvent(false)
                } else {
                    if (Config.DEBUG) Log.d("----pdf----", "Y轴移动距离大于X轴距离，请求不要拦截我absX = $absX,absY = $absY")
                    parent?.requestDisallowInterceptTouchEvent(true)
                }

                if (singleTouch) {
                    if (lastX == 0f) lastX = event.rawX
                    if (lastY == 0f) lastY = event.rawY
                    val offsetX = event.rawX - lastX
                    val offsetY = event.rawY - lastY
                    fakeDrag(offsetX, offsetY)
                }
            }
        }
    }

    private fun fakeDrag(offsetX: Float, offsetY: Float) {
        if (fakeDragOffset == 0f) {
            if (offsetY > scaledTouchSlop) fakeDragOffset = scaledTouchSlop
            else if (offsetY < -scaledTouchSlop) fakeDragOffset = -scaledTouchSlop
        }
        if (fakeDragOffset != 0f) {
            val fixedOffsetY = offsetY - fakeDragOffset
            parent?.requestDisallowInterceptTouchEvent(true)
            val fraction = abs(max(-1f, min(1f, fixedOffsetY / height)))
            val fakeScale = 1 - min(0.4f, fraction)
            scaleX = fakeScale
            scaleY = fakeScale
            translationY = fixedOffsetY
            translationX = offsetX / 2
            listeners.toList().forEach { it.onDrag(this, fraction) }
        }
    }

    private fun up() {
        singleTouch = false
        parent?.requestDisallowInterceptTouchEvent(false)
        //singleTouch = true
        fakeDragOffset = 0f
        lastX = 0f
        lastY = 0f

        if (abs(translationY) > dismissEdge) {
            listeners.toList().forEach { it.onRelease(this) }
        } else {
            val offsetY = translationY
            val fraction = min(1f, offsetY / height)
            listeners.toList().forEach { it.onRestore(this, fraction) }

            animate()
                    .translationX(0f).translationY(0f).scaleX(1f).scaleY(1f)
                    .setDuration(200).start()
        }
    }

    override fun onDetachedFromWindow() {
        isDetachedForWindow = true
        super.onDetachedFromWindow()
        animate().cancel()
    }
}