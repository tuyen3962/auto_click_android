package com.example.autoclickapp

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.autoclickapp.service.FloatingClickService
import com.example.autoclickapp.service.autoClickService
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.math.min


private const val PERMISSION_CODE = 110

class MainActivity : AppCompatActivity() {

    private var serviceIntent: Intent? = null
    private val client = OkHttpClient()
    private val apiUrl = "https://timeapi.io/api/Time/current/zone?timeZone=Asia/Ho_Chi_Minh"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        getSharedPreferences(getString(R.string.click_time), )
        findViewById<Button>(R.id.button).setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N
                    || Settings.canDrawOverlays(this)) {
                serviceIntent = Intent(this@MainActivity,
                        FloatingClickService::class.java)
                startService(serviceIntent)
                onBackPressed()
            } else {
                askPermission()
                shortToast("You need System Alert Window Permission to do this")
            }
        }
        findViewById<Button>(R.id.setTime).setOnClickListener {
            onCallTimeAPI()
            _onSaveTimeClick()
        }
    }

    private fun _onSaveTimeClick() {
        val hour = validateAndGetTime(R.id.hour, 24)
        val minute =validateAndGetTime(R.id.minutes, 59)
        val second =validateAndGetTime(R.id.seconds, 59)
        val millisecond =validateAndGetTime(R.id.millisecond, 999)
        if(hour == 0) return
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putInt(getString(R.string.hour_click_time), hour)
            putInt(getString(R.string.minute_click_time), minute)
            putInt(getString(R.string.second_click_time), second)
            putInt(getString(R.string.millisecond_click_time), millisecond)
            apply()
        }

        Toast.makeText(this, String.format("Cập nhật thời gian click %d:%d:%d:%d ", hour, minute, second, millisecond ), Toast.LENGTH_SHORT).show()
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
                    Log.i("TIME", fullTime)
                } else {
                    println("Invalid JSON format")
                }

            }
        })
    }

    private fun validateAndGetTime(id: Int, maxValue: Int): Int {
        val view = findViewById<EditText>(id)
        view.clearFocus()
        val timeText = view.text.toString()
        if (timeText.isNotEmpty()) {
            val time = timeText.toInt()
            if(time >= maxValue) {
                view.setText(maxValue.toString())
                return maxValue
            } else {
                return time
            }
        }

        return 0
    }

    private fun checkAccess(): Boolean {
        val string = getString(R.string.accessibility_service_id)
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val list = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        Log.e("ACCESS", list.toString())
        for (id in list) {
            if (string == id.id) {
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        val hasPermission = checkAccess()
        "has access? $hasPermission".logd()
        if (!hasPermission) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(this)) {
            askPermission()
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun askPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
        startActivityForResult(intent, PERMISSION_CODE)
    }

    override fun onDestroy() {
        serviceIntent?.let {
            "stop floating click service".logd()
            stopService(it)
        }
        autoClickService?.let {
            "stop auto click service".logd()
            it.stopSelf()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) return it.disableSelf()
            autoClickService = null
        }
        super.onDestroy()
    }

//    override fun onBackPressed() {
//        moveTaskToBack(true)
//    }
}
