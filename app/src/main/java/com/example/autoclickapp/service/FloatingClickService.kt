package com.example.autoclickapp.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.autoclickapp.R
import com.example.autoclickapp.TouchAndDragListener
import com.example.autoclickapp.dp2px
import com.example.autoclickapp.logd
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.*
import kotlin.concurrent.fixedRateTimer


/**
 * Created on 2018/9/28.
 * By nesto
 */
class FloatingClickService : Service() {
    private lateinit var manager: WindowManager
    private lateinit var autoTapView: View
    private lateinit var settingView: View
    private lateinit var timeView: View
    private lateinit var autoTapParams: WindowManager.LayoutParams
    private lateinit var settingParams: WindowManager.LayoutParams
    private lateinit var timeParams: WindowManager.LayoutParams
    private var xForRecord = 0
    private var yForRecord = 0
    private val location = IntArray(2)
    private var startDragDistance: Int = 0
    private var timer: Timer? = null
    var isPlay = false

    private val client = OkHttpClient()
    private val apiUrl = "https://timeapi.io/api/Time/current/zone?timeZone=Asia/Ho_Chi_Minh"

//    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
//    val coroutine = rememberCoroutineScope()

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        startDragDistance = dp2px(10f)
        autoTapView = LayoutInflater.from(this).inflate(R.layout.widget, null)
        settingView = LayoutInflater.from(this).inflate(R.layout.auto_click_setting, null)
        timeView = LayoutInflater.from(this).inflate(R.layout.auto_time, null)

        //setting the layout parameters
        val overlayParam =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_PHONE
                }
        autoTapParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayParam,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT)

        settingParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayParam,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT)

        timeParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayParam,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT)

        settingParams.gravity = Gravity.LEFT or Gravity.CENTER
        timeParams.gravity = Gravity.TOP or Gravity.LEFT

        //getting windows services and adding the floating view to it
        manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        manager.addView(autoTapView, autoTapParams)
        manager.addView(settingView, settingParams)
        manager.addView(timeView, timeParams)

        //adding an touchlistener to make drag movement of the floating widget
        autoTapView.setOnTouchListener(TouchAndDragListener(autoTapParams, startDragDistance,
                {  },
                { manager.updateViewLayout(autoTapView, autoTapParams) }))

        settingView.findViewById<ImageView>(R.id.play_icon).setOnClickListener {
            isPlay = !isPlay
            if(isPlay) {
                settingView.findViewById<ImageView>(R.id.play_icon).setImageResource(R.drawable.setting)
            } else {
                settingView.findViewById<ImageView>(R.id.play_icon).setImageResource(R.drawable.play)
            }
            viewOnClick()
        }

        settingView.setOnTouchListener(TouchAndDragListener(settingParams, startDragDistance,
            {

            },
            {
                if(!isOn) {
                    manager.updateViewLayout(settingView, settingParams)
                }
            }))

        timeView.setOnTouchListener(TouchAndDragListener(timeParams, startDragDistance,
            { },
            {    manager.updateViewLayout(timeView, timeParams) }))

        onCallTimeAPI()
    }

    private fun onCallTimeAPI() {
        val request = Request.Builder().url(apiUrl).build()

        client.newCall(request).enqueue(object  : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("API", e.toString())
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonObject: JsonObject? = try {
                    JsonParser.parseString(response.body()?.string()).asJsonObject
                } catch (e: Exception) {
                    null // Handle the exception, e.g., return null or log the error
                }
                if (jsonObject != null) {

                    val hour = jsonObject.get("hour").asInt
                    val minute = jsonObject.get("minute").asInt
                    val seconds = jsonObject.get("seconds").asInt
                    val milliSeconds = jsonObject.get("milliSeconds").asInt
                    val fullTime = String.format("%d:%d:%d:%d", hour, minute, seconds, milliSeconds)
                    timeView.findViewById<TextView>(R.id.auto_time_text).text = fullTime
                } else {
                    println("Invalid JSON format")
                }

            }
        })
    }

    private var isOn = false
    private fun viewOnClick() {
        if (isOn) {
            timer?.cancel()
        } else {
            timer = fixedRateTimer(initialDelay = 0,
                    period = 200) {
                autoTapView.getLocationOnScreen(location)
                Log.i("LOCATION", location[0].toString())
                Log.i("LOCATION", location[1].toString())
                autoClickService?.click(location[0] + autoTapView.right+1,
                        location[1] + autoTapView.bottom+1)
            }
        }
        isOn = !isOn
//        (autoTapView as TextView).text = if (isOn) "ON" else "OFF"

    }

    override fun onDestroy() {
        super.onDestroy()
        "FloatingClickService onDestroy".logd()
        timer?.cancel()
        manager.removeView(autoTapView)
        manager.removeView(settingView)
    }
}