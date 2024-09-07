package com.example.autoclickapp

class ClickTime(hour: Int, minute: Int, second: Int, ms: Int) {
    val hour: Int = hour
    val minute: Int = minute
    val second: Int = second
    val ms: Int = ms

    fun isSameTime(clickTime: ClickTime) : Boolean {
        if(clickTime.hour == hour && clickTime.minute == minute && clickTime.second == second) {
            if(clickTime.ms == ms) {
                return true
            } else {
                val start = clickTime.ms
                val end = clickTime.ms + 100
                return ms > start && ms < end
            }
        }

        return false
    }

    override fun toString(): String {
        return String.format("%d:%d:%d:%d", hour, minute,second,ms)
    }
}