package com.example.autoclickapp.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.example.autoclickapp.ClickTime
import com.example.autoclickapp.NAME
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
    private var countTimer: Timer? = null
    private lateinit var  handler: Handler
    private lateinit var intent: Intent
    var isPlay = false

    private val client = OkHttpClient()
    private val apiUrl = "https://timeapi.io/api/Time/current/zone?timeZone=Asia/Ho_Chi_Minh"

//    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
//    val coroutine = rememberCoroutineScope()

    override fun onBind(intent: Intent): IBinder? {
        this.intent = intent
        return null
    }

    override fun onCreate() {
        super.onCreate()
        handler = Handler(Looper.getMainLooper())
        startDragDistance = dp2px(10f)
        autoTapView = LayoutInflater.from(this).inflate(R.layout.widget, null)
        settingView = LayoutInflater.from(this).inflate(R.layout.auto_click_setting, null)
        timeView = LayoutInflater.from(this).inflate(R.layout.auto_time, null)

        _startCountTimer()

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
            handler.post {
                if(isPlay) {
                    settingView.findViewById<ImageView>(R.id.play_icon).setImageResource(R.drawable.pause)
                } else {
                    settingView.findViewById<ImageView>(R.id.play_icon).setImageResource(R.drawable.play)
                }
            }
            viewOnClick()
        }

        settingView.findViewById<ImageView>(R.id.setting_icon).setOnClickListener {
//            stopService(Intent(this, FloatingClickService::class.java))
            val displayMetrics = DisplayMetrics()
            manager.getDefaultDisplay().getMetrics(displayMetrics)
            val height = displayMetrics.heightPixels
            val width = displayMetrics.widthPixels
            Log.i("Screen",String.format("%d %d", height, width))

//            2177 1080

            autoTapView.findViewById<ImageView>(R.id.circle_icon).setImageResource(R.drawable.clicked)
            autoTapView.getLocationOnScreen(location)
            val posX = location[0] + autoTapView.right
            val posY = location[1] + autoTapView.bottom - 50
            Log.i("POSITION",String.format("%d %d", location[0] + autoTapView.right+1, location[1] + autoTapView.bottom+1))
            Log.i("POSITION",String.format("%d %d", posX, posY))

            autoClickService?.click(posX, posY)
            handler.postDelayed({
                autoTapView.findViewById<ImageView>(R.id.circle_icon).setImageResource(R.drawable.click)
            }, 10)
        }

        settingView.setOnTouchListener(TouchAndDragListener(settingParams, startDragDistance,
            { },
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

    private fun _startCountTimer() {
        countTimer = fixedRateTimer(initialDelay = 0, period = 100) {
            onCallTimeAPI()
        }
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
                    val msText =  (milliSeconds / 100) * 100
                    handler.post {
                        val fullTime = String.format("%s:%s:%s:%s", getTimeFormat(hour), getTimeFormat(minute), getTimeFormat(seconds), getMsFormat(msText))

                        timeView.findViewById<TextView>(R.id.auto_time_text).text = fullTime
                        if(isOn) {
                            autoClick(ClickTime(hour, minute, seconds, milliSeconds))
                        }
                    }
                } else {
                    println("Invalid JSON format")
                }

            }
        })
    }

    private fun getTimeFormat(time: Int) : String {
        if(time < 10) {
            return String.format("0%d", time)
        } else {
            return time.toString()
        }
    }

    private fun getMsFormat(time: Int) : String {
        if(time > 0) {
            return String.format("%d", time)
        } else {
            return "000"
        }
    }


    private fun autoClick(currentTime: ClickTime) {
        val preferences = this.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        val clickTime = ClickTime(
            preferences.getInt(getString(R.string.hour_click_time), 0),
            preferences.getInt(getString(R.string.minute_click_time), 0),
            preferences.getInt(getString(R.string.second_click_time), 0),
            preferences.getInt(getString(R.string.millisecond_click_time), 0)
        )

        if(clickTime.isSameTime(currentTime)) {
            autoTapView.findViewById<ImageView>(R.id.circle_icon).setImageResource(R.drawable.clicked)
            handler.postDelayed({
                autoTapView.getLocationOnScreen(location)
                autoClickService?.click(location[0] + autoTapView.right+1,
                    location[1] + autoTapView.bottom+1)
                autoTapView.findViewById<ImageView>(R.id.circle_icon).setImageResource(R.drawable.click)
            }, 10)
        }
    }

    private var isOn = false
    private fun viewOnClick() {
//        if (isOn) {
//            timer?.cancel()
//        } else {
//            timer = fixedRateTimer(initialDelay = 0,
//                    period = 200) {
//                autoTapView.getLocationOnScreen(location)
//                Log.i("LOCATION", location[0].toString())
//                Log.i("LOCATION", location[1].toString())
//                autoClickService?.click(location[0] + autoTapView.right+1,
//                        location[1] + autoTapView.bottom+1)
//            }
//        }
        isOn = !isOn
    }

    override fun onDestroy() {
        super.onDestroy()
        "FloatingClickService onDestroy".logd()
        countTimer?.cancel()
        manager.removeView(autoTapView)
        manager.removeView(settingView)
        manager.removeView(timeView)
    }
}