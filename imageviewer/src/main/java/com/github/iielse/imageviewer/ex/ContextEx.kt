package com.github.iielse.imageviewer.ex

import android.content.Context
import android.graphics.Point
import android.view.WindowManager

/**
 * 作者：吕振鹏
 * E-mail:lvzhenpeng@pansoft.com
 * 创建时间：2020年11月03日
 * 时间：11:44 AM
 * 版本：v1.0.0
 * 类描述：
 * 修改时间：
 */
fun Context.getWindowSize(): Point {
    val windowManager = this.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val point = Point()
    windowManager.defaultDisplay.getSize(point)
    return point
}