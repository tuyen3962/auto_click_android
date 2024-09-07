package com.example.autoclickapp

import android.content.Context
import android.util.Log


/**
 * Created on 2018/9/28.
 * By nesto
 */
private const val TAG = "AutoClickService"
const val NAME = "AutoClick"

fun Any.logd(tag: String = TAG) {
    if (this is String) {
        Log.d(tag, this)
    } else {
        Log.d(tag, this.toString())
    }
}

fun Any.loge(tag: String = TAG) {
    if (this is String) {
        Log.e(tag, this)
    } else {
        Log.e(tag, this.toString())
    }
}

fun Context.dp2px(dpValue: Float): Int {
    val scale = resources.displayMetrics.density
    return (dpValue * scale + 0.5f).toInt()
}

typealias Action = () -> Unit